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
 * 周期: 光源と木漏れ日 28 秒 / 脈動・呼吸 7 秒 / ガラスのきらめき 14 秒。
 */
object NotebookShaders {

    /** 全画面の背景。紙色 + 光源からの減衰 + 木漏れ日のゆらぎ + ビネット + 粒状感。 */
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

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / iResolution;
                float span = max(iResolution.x, iResolution.y);

                // 光源からの広い減衰（ダークは灯りの溜まりを絞る）
                float dist = length(fragCoord - uLightPos) / span;
                float reach = mix(1.1, 0.75, uDark);
                float pool = exp(-(dist * dist) / (reach * reach));

                // 木漏れ日: 2層の value noise を周期 28 秒の小さな円軌道で漂わせる
                float w = iTime * 6.2831853 / 28.0;
                float2 drift = float2(cos(w), sin(w)) * 0.8;
                float n1 = valueNoise(fragCoord / 150.0 + drift);
                float n2 = valueNoise(fragCoord / 67.0 - drift * 1.3 + 11.0);
                float patchAmt = n1 * 0.6 + n2 * 0.4;
                float litSide = smoothstep(0.15, 0.9, pool);
                float dapple = smoothstep(0.45, 0.9, patchAmt) * litSide;

                float keyAmount = clamp((pool * 0.30 + dapple * 0.06) * uIntensity, 0.0, 1.0);
                float3 col = mix(uBaseColor.rgb, uKeyColor.rgb, keyAmount);

                // ビネット: 画面端を影の色へ沈める
                float vig = smoothstep(0.3, 0.85, length(uv - 0.5) * 1.4) * mix(0.12, 0.55, uDark);
                col = mix(col, uShadowColor.rgb, vig);

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

    /** カードの面。角丸マスク内に、柔らかなスペキュラ・縁のリムライト・押下のグローを載せる。 */
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

                // 柔らかなスペキュラ: 光源側へ寄せた点を中心に大きくぼかす
                float2 hc = halfSize + lightDir * halfSize * 0.6;
                float2 hd = fragCoord - hc;
                float spread = max(iResolution.x, iResolution.y) * 0.6;
                float breath = 0.94 + 0.06 * sin(iTime * 6.2831853 / 7.0);
                float spec = exp(-dot(hd, hd) / (spread * spread)) * uHighlightColor.a * breath;

                // リムライト: 内側 1.5px 程度の帯。光源側の縁ほど強い
                float band = 1.0 - smoothstep(0.0, 0.9, abs(d + 0.8));
                float2 nrm = p / max(length(p), 0.0001);
                float facing = clamp(dot(nrm, lightDir), 0.0, 1.0);
                float rim = band * (0.2 + 0.8 * facing) * uRimColor.a;

                // 押下点からの放射状のグロー
                float2 pd = fragCoord - uPressPos;
                float pr = max(iResolution.x, iResolution.y) * 0.45;
                float press = exp(-dot(pd, pd) / (pr * pr)) * uPress * 0.14;

                float3 col = uBaseColor.rgb;
                col += uHighlightColor.rgb * spec * 0.22;
                col += uRimColor.rgb * rim;
                col += uHighlightColor.rgb * press;
                col = clamp(col, 0.0, 1.0);

                float a = mask * uBaseColor.a;
                return half4(col * a, a);
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
            UniformSpec("iTime", 1),
        )
    )

    /** 柔らかい影。角丸矩形の SDF から指数減衰させる。乗算済みで返す。 */
    val SHADOW = ShaderSpec(
        name = "shadow",
        source = """
            uniform float2 iResolution;
            uniform float2 uRectCenter;
            uniform float2 uRectSize;
            uniform float uRadius;
            uniform float uBlur;
            uniform float4 uShadowColor;

            float sdRoundRect(float2 p, float2 halfSize, float r) {
                float2 q = abs(p) - halfSize + r;
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
            }

            half4 main(float2 fragCoord) {
                float2 p = fragCoord - uRectCenter;
                float r = min(uRadius, min(uRectSize.x, uRectSize.y) * 0.5);
                float d = sdRoundRect(p, uRectSize * 0.5, r);

                // 描画領域の端 2px で影を消し、切り口が見えないようにする
                float2 border = min(fragCoord, iResolution - fragCoord);
                float edge = smoothstep(0.0, 2.0, min(border.x, border.y));

                // 内側（d < 0）は max(d, 0) = 0 で減衰なし = uShadowColor.a
                float falloff = exp(-pow(max(d, 0.0) / max(uBlur, 0.001), 2.0) * 1.8);
                float a = uShadowColor.a * falloff * edge;
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
        )
    )

    /** 半透明の板（ボトムバー等）。背景のぼかしは行わない。上辺の細い光の線を持つ。 */
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

                // 上辺の細い光の線（スペキュラのエッジ）
                float band = 1.0 - smoothstep(0.0, 0.9, abs(d + 0.8));
                float topness = clamp(0.5 - 0.5 * (p.y / halfSize.y), 0.0, 1.0);
                float edgeLine = band * topness * topness;

                // 光源側からのゆるいグラデーション（周期 14 秒のごく小さなきらめき）
                float facing = clamp(dot(p / halfSize, lightDir) * 0.5 + 0.5, 0.0, 1.0);
                float shimmer = 0.9 + 0.1 * sin(iTime * 6.2831853 / 14.0);
                float sheen = facing * 0.10 * shimmer;

                // ごく弱いノイズ
                float grain = hash21(floor(fragCoord)) - 0.5;

                float3 col = uTint.rgb + uHighlightColor.rgb * (sheen + edgeLine * 0.5);
                col += grain * 0.012;
                float a = mask * clamp(uTint.a + edgeLine * uHighlightColor.a * 0.5, 0.0, 1.0);
                return half4(clamp(col, 0.0, 1.0) * a, a);
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
