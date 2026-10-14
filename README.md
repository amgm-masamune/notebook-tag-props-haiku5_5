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

## CI（`.github/workflows/ci.yml`）

push / pull_request / workflow_dispatch で実行。成果物は Actions の実行画面（Summary 下部の Artifacts）から取得できます（保持 14 日）。

| 成果物 | 内容 | job |
|---|---|---|
| `unit-test-reports` | JVM テストの HTML レポートと JUnit XML | unit-test |
| `debug-apk` | debug APK | unit-test |
| `android-test-reports` | 実機テスト（エミュレータ）の HTML レポートと XML | android-test |
| `android-test-apk` | 計装テスト APK | android-test |

## ドキュメント

設計書は [docs/](docs/README.md) を参照（requirements / data-model / architecture / testing）。
