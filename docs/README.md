# 設計書 目次

ステータス: 設計（未承認→親エージェントにより承認済み扱い）

タグ（型）とプロパティを持つ、完全ローカルの Android ノートアプリの設計書。Kotlin / Jetpack Compose (Material3) / Room / ViewModel + StateFlow、DI は手動 (`AppContainer`)。

| 文書 | 内容 |
|---|---|
| [requirements.md](requirements.md) | ユーザー原文、機能要件(FR)・非機能要件(NFR)、Markdown 対応範囲、制約 |
| [data-model.md](data-model.md) | ドメインモデル、クラス図・ER 図 (Mermaid)、継承・判定・検証ルール(番号付き)、Room スキーマ、リポジトリ IF |
| [architecture.md](architecture.md) | 層構造・依存方向、ファイル構成、画面/ナビゲーション、ViewModel(UiState・操作)、AppContainer、ビルド構成、Markdown AST、TestTag |
| [testing.md](testing.md) | テスト階層、テストファイル一覧、GitHub Actions ジョブ構成・成果物名 |
| [lighting.md](lighting.md) | 光と影の UI 基盤。ライト/ダークの光源モデル、AGSL シェーダー 5 種と uniform、API 33 未満のフォールバック、テスト方針 |

読む順序の推奨: requirements → data-model → architecture → testing。実装は domain/markdown → data → ui の順に進めると、各層を JUnit で先に固められる。

固定事項の早見:
- パッケージ: `com.amgm.notebook` 配下に `domain` / `data` / `markdown` / `ui` / `di`
- 公称型: `NoteId` / `TagId` / `PropertyName`（`@JvmInline value class`）
- 保存方針: 必須未入力は警告（保存可）、型不一致はエラー（保存拒否）
- CI 成果物: `debug-apk` / `unit-test-reports` / `android-test-reports` / `android-test-apk`
