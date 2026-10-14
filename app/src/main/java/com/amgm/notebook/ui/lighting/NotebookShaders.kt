package com.amgm.notebook.ui.lighting

/**
 * AGSL の uniform 宣言。[floatCount] は setFloatUniform に渡す float の個数
 * （float=1, float2=2, float4/half4=4 など）。
 */
data class UniformSpec(val name: String, val floatCount: Int)

/** AGSL のシェーダー定義。[source] は `half4 main(float2 fragCoord)` を持つ。 */
data class ShaderSpec(val name: String, val source: String, val uniforms: List<UniformSpec>)

/**
 * 光と影の AGSL シェーダー 5 種。すべて API 33 以上でのみ使う（[ShaderSupport] を参照）。
 *
 * 時間を使う式は、ループ長（[LightState] の 3584 秒 = 2^9 × 7）の約数の周期だけを使う。
 * これによりループの折り返しで光源や木漏れ日が跳ばない。
 * 周期: 光源 28 秒 / 脈動・呼吸 7 秒 / ガラスのきらめき 14 秒 / 木漏れ日の漂い 112 秒と 224 秒。
 */
object NotebookShaders {

    /**
     * 全画面の背景。
     * ライトは紙色の上に暖白の光を当て（光の当たる側は必ずベース以上の明るさ）、窓の光（2 枚）と木漏れ日を重ねる。
     * ダークはインクの地に上部中央の卓上灯の溜まりを加算し、四隅を沈める。
     */
    val AMBIENT = ShaderSpec(
        name = "ambient",
        source = """
            uniform float2 iResolution;
            uniform float iTime;
            uniform float2 uLightPos;
            uniform float4 uBaseColor;
            uniform float4 uKeyColor;
            uniform float4 uShadowColor;
            uniform float uIntensity;
            uniform float uGrain;
            uniform float uDark;

            float hash21(float2 p) {
                return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
            }

            float valueNoise(float2 p) {
                float2 i = floor(p);
                float2 f = fract(p);
                float2 u = f * f * (3.0 - 2.0 * f);
                float a = hash21(i);
                float b = hash21(i + float2(1.0, 0.0));
                float c = hash21(i + float2(0.0, 1.0));
                float d = hash21(i + float2(1.0, 1.0));
                return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
            }

            // 窓ガラス 1 枚。中心 c（画面座標）を基準に -28° 回転し、上下へ 0.16 せん断した平行四辺形。
            // 半幅・半高は hs（px）、縁のぼかしは片側 b（px）。
            // 返す x は被覆率（0..1）、y は符号付きの距離（外側が正、px）。
            float2 windowPane(float2 frag, float2 c, float2 hs, float b) {
                float2 q = frag - c;
                float2 r = float2(0.88295 * q.x + 0.46947 * q.y, -0.46947 * q.x + 0.88295 * q.y);
                r.x -= r.y * 0.16;
                float2 e = abs(r) - hs;
                float dist = length(max(e, 0.0)) + min(max(e.x, e.y), 0.0);
                float cover = (1.0 - smoothstep(-b, b, e.x)) * (1.0 - smoothstep(-b, b, e.y));
                return float2(cover, dist);
            }

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / iResolution;
                float wpx = iResolution.x;
                float hpx = iResolution.y;
                float diag = length(iResolution);

                // 光源からの減衰。ライトは対角線の広さ、ダークは画面高さの溜まりに絞る
                float dist = length(fragCoord - uLightPos);
                float poolRadius = mix(diag * 0.85, hpx * 0.55, uDark);
                float pool = exp(-pow(dist / poolRadius, 2.0));

                // ライト: 暖白へ最大 60% 寄せる（暖白の輝度は紙色より高いので、当たる側は明るくなる）
                float3 col = mix(uBaseColor.rgb, uKeyColor.rgb, clamp(pool * 0.35 * uIntensity, 0.0, 1.0) * (1.0 - uDark));
                // ダーク: 卓上灯の暖色を加算する
                col += uKeyColor.rgb * pool * 0.20 * uDark;

                // 窓の光（ライトのみ）: 窓ガラス 2 枚。枚幅 0.24W・高さ 0.50H・間隔 0.04W。
                // 各枚は中心 (0.30W, 0.36H) と (0.58W, 0.36H) を基準に -28° 回転し、上下へ 0.16 せん断する
                float2 hs = float2(0.12 * wpx, 0.25 * hpx);
                float wb = 0.045 * wpx;
                float2 paneA = windowPane(fragCoord, iResolution * float2(0.30, 0.36), hs, wb);
                float2 paneB = windowPane(fragCoord, iResolution * float2(0.58, 0.36), hs, wb);
                float panes = clamp(paneA.x + paneB.x, 0.0, 1.0);
                float paneDist = min(paneA.y, paneB.y);

                // 木漏れ日: 2 層の value noise（スケール W/4 と W/9）の積で、窓の内側の強さを 0.55〜1.0 倍に変調する。
                // 座標へ時間を足すので、時刻 0 でも模様は一様にならない
                float w1 = iTime * 6.2831853 / 112.0;
                float w2 = iTime * 6.2831853 / 224.0;
                float n1 = valueNoise(fragCoord / (wpx / 4.0) + float2(cos(w1), sin(w1)) * 0.5);
                float n2 = valueNoise(fragCoord / (wpx / 9.0) + float2(cos(w2), sin(w2)) * 0.7 + 11.0);
                float dapple = 0.55 + 0.45 * clamp(2.0 * n1 * n2, 0.0, 1.0);

                // 窓の外側をわずかに沈ませる（暖かい影色 #3A3226 へ 4%。ハローの範囲も含めた外側全体）
                float3 shadowWarm = float3(0.22745, 0.19608, 0.14902);
                col = mix(col, shadowWarm, 0.04 * (1.0 - panes) * (1.0 - uDark));

                // 窓の外側へ回り込む朝日の色のハロー（窓の SDF の外側で、0.12W の範囲に 0.12 から 0 へ）
                float3 morning = float3(1.0, 0.94510, 0.82353); // #FFF1D2
                float halo = 0.12 * (1.0 - smoothstep(0.0, 0.12 * wpx, max(paneDist, 0.0)));
                halo *= smoothstep(-0.5, 0.5, paneDist) * (1.0 - uDark);
                col = mix(col, morning, halo);

                // 窓の内側は金色寄りの朝日の色（#FFF1D2）へ 0.75 * 木漏れ日の変調で寄せる（ダークでは出さない）
                col = mix(col, morning, panes * dapple * 0.75 * (1.0 - uDark));

                // ダーク: 下からごく弱い寒色のフィル
                float3 fill = float3(0.118, 0.137, 0.251); // #1E2340
                float low = smoothstep(0.35, 1.0, uv.y);
                col = mix(col, fill, 0.05 * low * uDark);

                // ビネット: 暖かい影色で端を沈める。ライトは光源から遠い右下だけ少し強く、光源側は暗くしない。
                // ダークは四隅へ最大 25%
                float farness = clamp(dist / (diag * 0.9), 0.0, 1.0);
                float edgeAmt = smoothstep(0.30, 0.71, length(uv - 0.5));
                float vigMax = mix(mix(0.05, 0.07, farness), 0.25, uDark);
                float vigSide = mix(smoothstep(0.2, 0.9, farness), 1.0, uDark);
                col = mix(col, uShadowColor.rgb, edgeAmt * vigMax * vigSide);

                // 紙の粒状感（ピクセル単位の静的なハッシュ）
                float grain = hash21(floor(fragCoord)) - 0.5;
                col += grain * uGrain;

                return half4(clamp(col, 0.0, 1.0), 1.0);
            }
        """.trimIndent(),
        uniforms = listOf(
            UniformSpec("iResolution", 2),
            UniformSpec("iTime", 1),
            UniformSpec("uLightPos", 2),
            UniformSpec("uBaseColor", 4),
            UniformSpec("uKeyColor", 4),
            UniformSpec("uShadowColor", 4),
            UniformSpec("uIntensity", 1),
            UniformSpec("uGrain", 1),
            UniformSpec("uDark", 1),
        )
    )

    /**
     * カードの面。角丸マスク内に、光源方向に沿った線形の面の色、柔らかなスペキュラ、
     * 縁の 1.2px のベベル（光源側は明るく、反対側は沈める）、押下点からのグローを載せる。
     */
    val SURFACE = ShaderSpec(
        name = "surface",
        source = """
            uniform float2 iResolution;
            uniform float2 uLightPos;
            uniform float4 uBaseColor;
            uniform float4 uHighlightColor;
            uniform float4 uRimColor;
            uniform float uRadius;
            uniform float uPress;
            uniform float2 uPressPos;
            uniform float4 uPressColor;
            uniform float uDark;
            uniform float iTime;

            float sdRoundRect(float2 p, float2 halfSize, float r) {
                float2 q = abs(p) - halfSize + r;
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
            }

            half4 main(float2 fragCoord) {
                float2 halfSize = iResolution * 0.5;
                float2 p = fragCoord - halfSize;
                float r = min(uRadius, min(halfSize.x, halfSize.y));
                float d = sdRoundRect(p, halfSize, r);
                float mask = 1.0 - smoothstep(-0.5, 0.5, d);

                // 光源の向き（要素中心から見て。要素の外でもよい）
                float2 toLight = uLightPos - halfSize;
                float2 lightDir = toLight / max(length(toLight), 0.0001);

                // 縁の外向き法線（SDF の勾配を 1px 差分で求める）と、光源方向との内積
                float2 gradRaw = float2(
                    sdRoundRect(p + float2(1.0, 0.0), halfSize, r) - d,
                    sdRoundRect(p + float2(0.0, 1.0), halfSize, r) - d
                );
                float2 grad = gradRaw / max(length(gradRaw), 0.0001);
                float facing = dot(grad, lightDir);
                float facingLit = smoothstep(-0.3, 0.7, facing);
                float facingOpp = smoothstep(-0.3, 0.7, -facing);

                // 面の基本色: 光源方向に沿って線形。光源側は持ち上げ（ライトは白、ダークは卓上灯の暖色）、
                // 反対側は 4% 沈める
                float side = clamp(dot(p / halfSize, lightDir) * 0.5 + 0.5, 0.0, 1.0);
                float3 face = uBaseColor.rgb * (1.0 - 0.04 * (1.0 - side));
                face = mix(face, uHighlightColor.rgb, side * mix(0.5, 0.06, uDark));

                // スペキュラ: 光源側へ寄せた点を中心に大きくぼかし、白（ダークは暖色）へ寄せる
                float2 hc = halfSize + lightDir * halfSize * 0.6;
                float2 hd = fragCoord - hc;
                float spread = max(iResolution.x, iResolution.y) * 0.9;
                float breath = 0.94 + 0.06 * sin(iTime * 6.2831853 / 7.0);
                float spec = exp(-dot(hd, hd) / (spread * spread)) * uHighlightColor.a * breath;
                face = mix(face, uHighlightColor.rgb, spec);

                // ベベル: 縁の内側 1.2px の帯。光源側はリム色で明るく、反対側はわずかに沈める
                float band = 1.0 - smoothstep(0.0, 0.6, abs(d + 0.6));
                face = mix(face, uRimColor.rgb, band * facingLit * uRimColor.a);
                face *= 1.0 - mix(0.06, 0.03, uDark) * band * facingOpp;

                // 押下点からの放射状のグロー（半径は幅の 0.6 倍。ライトは主色、ダークはテアル）
                float2 pd = fragCoord - uPressPos;
                float pr = iResolution.x * 0.6;
                float press = exp(-dot(pd, pd) / (pr * pr)) * uPress * 0.10;
                face = mix(face, uPressColor.rgb, press);

                float a = mask * uBaseColor.a;
                return half4(clamp(face, 0.0, 1.0) * a, a);
            }
        """.trimIndent(),
        uniforms = listOf(
            UniformSpec("iResolution", 2),
            UniformSpec("uLightPos", 2),
            UniformSpec("uBaseColor", 4),
            UniformSpec("uHighlightColor", 4),
            UniformSpec("uRimColor", 4),
            UniformSpec("uRadius", 1),
            UniformSpec("uPress", 1),
            UniformSpec("uPressPos", 2),
            UniformSpec("uPressColor", 4),
            UniformSpec("uDark", 1),
            UniformSpec("iTime", 1),
        )
    )

    /**
     * 柔らかい影。ずらした影の矩形（uRectCenter / uRectSize）の SDF から指数減衰させる。乗算済みで返す。
     * 要素本体（ずらす前の矩形: uCasterCenter / uCasterSize / uCasterRadius）の内側だけを抜く。
     * 影はずらしてあるので、本体の直下で本体に覆われない帯まで抜くと背景が透けて二重の縁に見えるため。
     */
    val SHADOW = ShaderSpec(
        name = "shadow",
        source = """
            uniform float2 iResolution;
            uniform float2 uRectCenter;
            uniform float2 uRectSize;
            uniform float uRadius;
            uniform float uBlur;
            uniform float4 uShadowColor;
            uniform float2 uCasterCenter;
            uniform float2 uCasterSize;
            uniform float uCasterRadius;

            float sdRoundRect(float2 p, float2 halfSize, float r) {
                float2 q = abs(p) - halfSize + r;
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
            }

            half4 main(float2 fragCoord) {
                // ずらした影の矩形の SDF。外側は指数減衰、内側は不透明度のまま
                float2 p = fragCoord - uRectCenter;
                float r = min(uRadius, min(uRectSize.x, uRectSize.y) * 0.5);
                float d = sdRoundRect(p, uRectSize * 0.5, r);

                // 要素本体（ずらす前）の SDF。本体の内側（dc < 0）だけを抜き、縁は 1.5px でアンチエイリアスする
                float2 pc = fragCoord - uCasterCenter;
                float rc = min(uCasterRadius, min(uCasterSize.x, uCasterSize.y) * 0.5);
                float dc = sdRoundRect(pc, uCasterSize * 0.5, rc);
                float casterWeight = smoothstep(-0.75, 0.75, dc);

                // 描画領域の端 2px で影を消し、切り口が見えないようにする
                float2 border = min(fragCoord, iResolution - fragCoord);
                float edge = smoothstep(0.0, 2.0, min(border.x, border.y));

                float falloff = exp(-pow(max(d, 0.0) / max(uBlur, 0.001), 2.0) * 1.8);
                float a = uShadowColor.a * falloff * casterWeight * edge;
                return half4(uShadowColor.rgb * a, a);
            }
        """.trimIndent(),
        uniforms = listOf(
            UniformSpec("iResolution", 2),
            UniformSpec("uRectCenter", 2),
            UniformSpec("uRectSize", 2),
            UniformSpec("uRadius", 1),
            UniformSpec("uBlur", 1),
            UniformSpec("uShadowColor", 4),
            UniformSpec("uCasterCenter", 2),
            UniformSpec("uCasterSize", 2),
            UniformSpec("uCasterRadius", 1),
        )
    )

    /**
     * 半透明の板（ボトムバー等）。背景のぼかしは行わない。上辺に 1px の光の線を持ち、
     * 光源側へごく弱いグラデーションを載せる。線と板は over 合成する（premultiplied で返す）。
     */
    val GLASS = ShaderSpec(
        name = "glass",
        source = """
            uniform float2 iResolution;
            uniform float4 uTint;
            uniform float4 uHighlightColor;
            uniform float2 uLightPos;
            uniform float uRadius;
            uniform float iTime;

            float sdRoundRect(float2 p, float2 halfSize, float r) {
                float2 q = abs(p) - halfSize + r;
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
            }

            float hash21(float2 p) {
                return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
            }

            half4 main(float2 fragCoord) {
                float2 halfSize = iResolution * 0.5;
                float2 p = fragCoord - halfSize;
                float r = min(uRadius, min(halfSize.x, halfSize.y));
                float d = sdRoundRect(p, halfSize, r);
                float mask = 1.0 - smoothstep(-0.5, 0.5, d);

                float2 toLight = uLightPos - halfSize;
                float2 lightDir = toLight / max(length(toLight), 0.0001);

                // 上辺の 1px の光の線（不透明度は uHighlightColor.a）
                float band = 1.0 - smoothstep(0.0, 0.6, abs(d + 0.6));
                float topness = clamp(0.5 - 0.5 * (p.y / halfSize.y), 0.0, 1.0);
                float lineOp = clamp(band * topness * topness * uHighlightColor.a, 0.0, 1.0);

                // 光源側からのゆるいグラデーション（4%。周期 14 秒のごく小さなきらめき）
                float facing = clamp(dot(p / halfSize, lightDir) * 0.5 + 0.5, 0.0, 1.0);
                float shimmer = 0.9 + 0.1 * sin(iTime * 6.2831853 / 14.0);
                float3 tint = mix(uTint.rgb, uHighlightColor.rgb, facing * 0.04 * shimmer);

                // 線を板の上に over 合成する（premultiplied）
                float tintA = uTint.a;
                float outA = lineOp + tintA * (1.0 - lineOp);
                float3 outRGB = uHighlightColor.rgb * lineOp + tint * tintA * (1.0 - lineOp);

                // ごく弱いノイズ
                float grain = hash21(floor(fragCoord)) - 0.5;
                outRGB += grain * 0.012 * outA;

                outA = clamp(outA, 0.0, 1.0) * mask;
                outRGB = clamp(outRGB, 0.0, 1.0) * mask;
                return half4(outRGB, outA);
            }
        """.trimIndent(),
        uniforms = listOf(
            UniformSpec("iResolution", 2),
            UniformSpec("uTint", 4),
            UniformSpec("uHighlightColor", 4),
            UniformSpec("uLightPos", 2),
            UniformSpec("uRadius", 1),
            UniformSpec("iTime", 1),
        )
    )

    /** FAB や選択要素の後光。形の内側は描かず、外側へ指数減衰する光だけを描く。 */
    val GLOW = ShaderSpec(
        name = "glow",
        source = """
            uniform float2 iResolution;
            uniform float2 uRectCenter;
            uniform float2 uRectSize;
            uniform float uRadius;
            uniform float4 uGlowColor;
            uniform float uSpread;
            uniform float iTime;

            float sdRoundRect(float2 p, float2 halfSize, float r) {
                float2 q = abs(p) - halfSize + r;
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
            }

            half4 main(float2 fragCoord) {
                float2 p = fragCoord - uRectCenter;
                float r = min(uRadius, min(uRectSize.x, uRectSize.y) * 0.5);
                float d = sdRoundRect(p, uRectSize * 0.5, r);

                // 形の内側は透明にする（境界で立ち上がるので硬い縁は出ない）
                float outsideWeight = smoothstep(0.0, 1.5, d);

                // 周期 7 秒で強度を ±10% 脈動させる
                float pulse = 1.0 + 0.1 * sin(iTime * 6.2831853 / 7.0);
                float glow = exp(-max(d, 0.0) / max(uSpread, 0.001)) * pulse * outsideWeight;

                // 描画領域の端 2px で光を消し、切り口が見えないようにする
                float2 border = min(fragCoord, iResolution - fragCoord);
                float edge = smoothstep(0.0, 2.0, min(border.x, border.y));

                float a = uGlowColor.a * glow * edge;
                return half4(uGlowColor.rgb * a, a);
            }
        """.trimIndent(),
        uniforms = listOf(
            UniformSpec("iResolution", 2),
            UniformSpec("uRectCenter", 2),
            UniformSpec("uRectSize", 2),
            UniformSpec("uRadius", 1),
            UniformSpec("uGlowColor", 4),
            UniformSpec("uSpread", 1),
            UniformSpec("iTime", 1),
        )
    )

    /** 全シェーダーの一覧（テストと、名前による参照に使う）。 */
    val all: List<ShaderSpec> = listOf(AMBIENT, SURFACE, SHADOW, GLASS, GLOW)
}
