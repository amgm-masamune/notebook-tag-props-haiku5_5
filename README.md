# ノート（Notebook）

タグ（型）とプロパティを持つ、完全ローカルの Android ノートアプリ。Kotlin / Jetpack Compose (Material3) / Room / ViewModel + StateFlow、DI は手動（`AppContainer`）。

- パッケージ: `com.amgm.notebook`（`domain` / `data` / `markdown` / `ui` / `di`）
- minSdk 26 / compileSdk 35、Gradle 8.9、AGP 8.7.3、Kotlin 2.0.21、JDK 17

## ビルドとテスト

検証の正は GitHub Actions です（ローカル環境によっては Google Maven 等に到達できないため、CI 推奨）。ローカルで実行できる場合:

```
./gradlew :app:testDebugUnitTest :app:assembleDebug   # JVM テストと debug APK
./gradlew :app:connectedDebugAndroidTest              # エミュレータ/実機が必要
```

## スクリーンショット

CI が API 34 のエミュレータで撮影した画面です（ライト / ダーク）。

| 画面 | ライト | ダーク |
|---|---|---|
| ノート一覧 | <img src="https://raw.githubusercontent.com/amgm-masamune/notebook-tag-props-haiku5_5/ci-screenshots/98bc21c/api34/notes_light.png" width="240"> | <img src="https://raw.githubusercontent.com/amgm-masamune/notebook-tag-props-haiku5_5/ci-screenshots/98bc21c/api34/notes_dark.png" width="240"> |
| ノート（プレビュー） | <img src="https://raw.githubusercontent.com/amgm-masamune/notebook-tag-props-haiku5_5/ci-screenshots/98bc21c/api34/note_preview_light.png" width="240"> | <img src="https://raw.githubusercontent.com/amgm-masamune/notebook-tag-props-haiku5_5/ci-screenshots/98bc21c/api34/note_preview_dark.png" width="240"> |
| タグ一覧 | <img src="https://raw.githubusercontent.com/amgm-masamune/notebook-tag-props-haiku5_5/ci-screenshots/98bc21c/api34/tags_light.png" width="240"> | <img src="https://raw.githubusercontent.com/amgm-masamune/notebook-tag-props-haiku5_5/ci-screenshots/98bc21c/api34/tags_dark.png" width="240"> |
| タグ別表 | <img src="https://raw.githubusercontent.com/amgm-masamune/notebook-tag-props-haiku5_5/ci-screenshots/98bc21c/api34/tag_table_light.png" width="240"> | <img src="https://raw.githubusercontent.com/amgm-masamune/notebook-tag-props-haiku5_5/ci-screenshots/98bc21c/api34/tag_table_dark.png" width="240"> |

## 光と影（シェーダー）

画面全体に光源は 1 つだけあります。ライトは「窓辺の朝の光」、ダークは「夜の卓上灯」です。背景の光と木漏れ日、カードの面と縁、影、板の光の線は AGSL（RuntimeShader）で描きます。API 33 未満では、同じ光源の向きをグラデーションと標準の影で近似したフォールバック表示になります。仕様と検証方法は [docs/lighting.md](docs/lighting.md) を参照してください。

## CI（`.github/workflows/ci.yml`）

push / pull_request / workflow_dispatch で実行。成果物は Actions の実行画面（Summary 下部の Artifacts）から取得できます（保持 14 日）。

`android-test` は API 30 と API 34 のエミュレータでマトリクス実行します。API 30 は API 33 未満のフォールバック表示の確認用です。撮影したスクリーンショットは `ci-screenshots` ブランチの `{sha7}/api{level}/{画面}_{light|dark}.png` に保存され、コミットのコメントに画像リンクが投稿されます。失敗したときは、各 job のコメントに Gradle エラーの行も載ります。

| 成果物 | 内容 | job |
|---|---|---|
| `unit-test-reports` | JVM テストの HTML レポートと JUnit XML | unit-test |
| `debug-apk` | debug APK | unit-test |
| `android-test-reports` | 実機テスト（エミュレータ）の HTML レポートと XML | android-test |
| `android-test-apk` | 計装テスト APK | android-test |

## CI 成果物の上書きインストール（署名鍵・バージョン）

- デバッグ APK は、CI で固定の署名鍵（GitHub Secrets）で署名します。毎回変わる既定のデバッグ鍵で署名すると、前回の成果物の上に上書きできません。
- `versionCode` は CI の実行番号（`GITHUB_RUN_NUMBER`）になるため、新しい成果物ほど大きい番号になります。
- 必要な Secrets（リポジトリの Settings → Secrets and variables → Actions）:
  - `ANDROID_KEYSTORE_BASE64`: keystore ファイルを base64 にしたもの
  - `ANDROID_KEYSTORE_PASSWORD` / `ANDROID_KEY_ALIAS` / `ANDROID_KEY_PASSWORD`
- 鍵の作成例（一度だけ。鍵ファイルは安全な場所に保管し、リポジトリにはコミットしない）:

```
keytool -genkeypair -v -keystore notebook-ci.keystore -alias notebook -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=notebook"
base64 -w0 notebook-ci.keystore > notebook-ci.keystore.b64
```

- 鍵を変えた後の最初の更新だけは、端末の既存アプリをアンインストールしてから入れ直す必要があります（署名が変わるため）。アンインストールするとアプリ内のデータ（ノート）も消えます。

## ドキュメント

設計書は [docs/](docs/README.md) を参照（requirements / data-model / architecture / testing / lighting）。
