package com.amgm.notebook.ui.screenshot

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import com.amgm.notebook.NotebookApplication
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyDefinition
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.ui.NotebookApp
import com.amgm.notebook.ui.TestTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.LocalDate

/**
 * 主要画面のスクリーンショットを撮る（CI で PNG を回収し ci-screenshots ブランチへ載せる）。
 *
 * - 画面ごと・テーマごとに独立したテストメソッドにしてあり、1枚の失敗が他の撮影に影響しない。
 * - 撮影は [TestStorage] の `screenshots/{name}.png` に書き出し、AGP が
 *   `app/build/outputs/connected_android_test_additional_output/` へ回収する。
 * - ダーク/ライトは [NotebookApp] の darkThemeOverride で強制する。
 * - 通常の検証ではなく撮影が目的のため、アサーションは「画面の目印 testTag が出たこと」だけにする。
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val timeout = 10_000L

    private val container
        get() = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as NotebookApplication).container

    // 値クラスは lateinit 不可のため、ダミー初期値を持たせる（@Before の seedSampleData で必ず上書きされる）
    private var projectTagId: TagId = TagId("")
    private var devTagId: TagId = TagId("")
    private var readingTagId: TagId = TagId("")
    private val noteIdByTitle = mutableMapOf<String, NoteId>()

    @Before
    fun setUp() {
        clearAllData()
        rule.waitForIdle()
        seedSampleData()
    }

    @After
    fun tearDown() {
        clearAllData()
    }

    /** リポジトリ経由で全削除する（UiScenarioTest と同じ方式）。 */
    private fun clearAllData() {
        val c = container
        runBlocking {
            c.noteRepository.observeNotes("").first().forEach { c.noteRepository.delete(it.id) }
            c.tagRepository.getAllTags().forEach { c.tagRepository.delete(it.id) }
        }
    }

    /**
     * 見栄えのするサンプル: 親子タグ（プロジェクト > 開発）、読書、各型のプロパティ、
     * Markdown（見出し・リスト・引用・コード）を含む本文。
     */
    private fun seedSampleData() {
        val c = container
        val now = c.clock.instant()
        projectTagId = TagId(c.idGenerator.newId())
        devTagId = TagId(c.idGenerator.newId())
        readingTagId = TagId(c.idGenerator.newId())

        runBlocking {
            c.tagRepository.save(
                Tag(
                    id = projectTagId,
                    name = "プロジェクト",
                    parentIds = emptySet(),
                    ownPropertyDefinitions = listOf(
                        PropertyDefinition(PropertyName("状態"), PropertyType.Text, required = false),
                        PropertyDefinition(PropertyName("期限"), PropertyType.Date, required = false)
                    )
                )
            )
            c.tagRepository.save(
                Tag(
                    id = devTagId,
                    name = "開発",
                    parentIds = setOf(projectTagId),
                    ownPropertyDefinitions = listOf(
                        PropertyDefinition(PropertyName("工数"), PropertyType.Number, required = false),
                        PropertyDefinition(PropertyName("担当"), PropertyType.Text, required = false)
                    )
                )
            )
            c.tagRepository.save(
                Tag(
                    id = readingTagId,
                    name = "読書",
                    parentIds = emptySet(),
                    ownPropertyDefinitions = listOf(
                        PropertyDefinition(PropertyName("評価"), PropertyType.Number, required = false),
                        PropertyDefinition(PropertyName("読了日"), PropertyType.Date, required = false),
                        PropertyDefinition(PropertyName("完読"), PropertyType.Boolean, required = false)
                    )
                )
            )

            saveSampleNote(
                now = now, daysAgo = 0,
                title = "設計メモ",
                body = """
                    # 設計メモ

                    ## 方針
                    - タグは型として使い、プロパティ定義を親から継承する
                    - 未入力（null）と空文字は区別する

                    ## 実装の要点
                    ```kotlin
                    val schema = resolver.resolve(tagId)
                    ```
                """.trimIndent(),
                tagIds = setOf(devTagId),
                properties = mapOf(
                    PropertyName("工数") to PropertyValue.Number(8.5),
                    PropertyName("担当") to PropertyValue.Text("山田"),
                    PropertyName("優先度") to PropertyValue.Text("高")
                )
            )
            saveSampleNote(
                now = now, daysAgo = 1,
                title = "リリース計画",
                body = """
                    # リリース計画

                    ## マイルストーン
                    1. 仕様確定（10月）
                    2. 実装（11月）
                    3. 検証と公開

                    - [x] 要件レビュー
                    - [ ] 実機テスト

                    > 締切は **11月15日** 厳守
                """.trimIndent(),
                tagIds = setOf(projectTagId, devTagId),
                properties = mapOf(
                    PropertyName("状態") to PropertyValue.Text("進行中"),
                    PropertyName("期限") to PropertyValue.Date(LocalDate.of(2026, 11, 15)),
                    PropertyName("工数") to PropertyValue.Number(24.0),
                    PropertyName("担当") to PropertyValue.Text("佐藤")
                )
            )
            saveSampleNote(
                now = now, daysAgo = 2,
                title = "読んだ本メモ",
                body = """
                    # 読んだ本メモ

                    **純粋ロジック入門** を読了。

                    - 副作用を境界に寄せる
                    - 型で不正な状態を表現する

                    > 次は テストの設計 を読む
                """.trimIndent(),
                tagIds = setOf(readingTagId),
                properties = mapOf(
                    PropertyName("評価") to PropertyValue.Number(4.0),
                    PropertyName("読了日") to PropertyValue.Date(LocalDate.of(2026, 9, 20)),
                    PropertyName("完読") to PropertyValue.Bool(true)
                )
            )
            saveSampleNote(
                now = now, daysAgo = 3,
                title = "買い物リスト",
                body = """
                    # 買い物リスト

                    - 牛乳
                    - 卵 10個
                    - パン
                """.trimIndent(),
                tagIds = emptySet(),
                properties = emptyMap()
            )
            saveSampleNote(
                now = now, daysAgo = 4,
                title = "バグ調査ログ",
                body = """
                    # バグ調査ログ

                    型不一致の値が保存されてしまう件。

                    ```kotlin
                    require(value.matches(type))
                    ```

                    原因は `Null` と空文字の扱い。
                """.trimIndent(),
                tagIds = setOf(devTagId),
                properties = mapOf(
                    PropertyName("工数") to PropertyValue.Number(3.0),
                    PropertyName("担当") to PropertyValue.Text("田中")
                )
            )
        }
    }

    private suspend fun saveSampleNote(
        now: java.time.Instant,
        daysAgo: Long,
        title: String,
        body: String,
        tagIds: Set<TagId>,
        properties: Map<PropertyName, PropertyValue>
    ) {
        val id = NoteId(container.idGenerator.newId())
        noteIdByTitle[title] = id
        val updatedAt = now.minus(Duration.ofDays(daysAgo))
        container.noteRepository.save(
            Note(
                id = id,
                title = title,
                body = body,
                tagIds = tagIds,
                properties = properties,
                createdAt = updatedAt,
                updatedAt = updatedAt
            )
        )
    }

    // ---- 待機・操作ヘルパー（testTag の出現で待つ。固定の sleep は使わない） ----

    private fun tagStartingWith(prefix: String) = SemanticsMatcher("testTag starts with $prefix") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private fun waitForTag(tag: String) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTagPrefix(prefix: String) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodes(tagStartingWith(prefix)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(tag: String) {
        waitForTag(tag)
        rule.onNodeWithTag(tag).performClick()
    }

    private fun editableText(tag: String): String =
        rule.onNodeWithTag(tag).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()

    /** 既存ノートを一覧から開き、タイトルと本文が読み込まれるまで待つ。 */
    private fun openNote(title: String) {
        click(TestTags.NOTE_LIST_ITEM_PREFIX + noteIdByTitle.getValue(title).value)
        waitForTag(TestTags.NOTE_EDIT_SAVE)
        rule.waitUntil(timeoutMillis = timeout) { editableText(TestTags.NOTE_EDIT_TITLE) == title }
        rule.waitUntil(timeoutMillis = timeout) { editableText(TestTags.NOTE_EDIT_BODY).isNotEmpty() }
    }

    /**
     * アプリを指定テーマで表示し、一覧を読み込んでから [open] で目的の画面へ移動して撮影する。
     * 出力名は `{name}_{light|dark}.png`。
     */
    private fun capture(name: String, dark: Boolean, open: () -> Unit) {
        rule.setContent { NotebookApp(container, darkThemeOverride = dark) }
        waitForTag(TestTags.NOTE_LIST_FAB)
        waitForTagPrefix(TestTags.NOTE_LIST_ITEM_PREFIX)
        open()
        rule.waitForIdle()
        savePng("${name}_${if (dark) "dark" else "light"}")
    }

    private fun savePng(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        TestStorage().openOutputFile("screenshots/$name.png").use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG の圧縮に失敗: $name" }
        }
    }

    // ---- 撮影対象（ライト・ダークを独立したテストにする） ----

    /** (1) ノート一覧 */
    @Test
    fun notes_light() = capture("notes", dark = false) {
        waitForTagPrefix(TestTags.NOTE_LIST_ITEM_PREFIX)
    }

    @Test
    fun notes_dark() = capture("notes", dark = true) {
        waitForTagPrefix(TestTags.NOTE_LIST_ITEM_PREFIX)
    }

    /** (2) ノート編集（既存ノートを開いた状態） */
    @Test
    fun noteEdit_light() = capture("note_edit", dark = false) {
        openNote("設計メモ")
    }

    @Test
    fun noteEdit_dark() = capture("note_edit", dark = true) {
        openNote("設計メモ")
    }

    /** (3) ノート編集のプレビュー表示 */
    @Test
    fun notePreview_light() = capture("note_preview", dark = false) {
        openNote("リリース計画")
        click(TestTags.NOTE_EDIT_MODE_PREVIEW)
        waitForTag(TestTags.NOTE_EDIT_PREVIEW)
    }

    @Test
    fun notePreview_dark() = capture("note_preview", dark = true) {
        openNote("リリース計画")
        click(TestTags.NOTE_EDIT_MODE_PREVIEW)
        waitForTag(TestTags.NOTE_EDIT_PREVIEW)
    }

    /** (4) タグ一覧（ボトムナビの「タグ」タブ） */
    @Test
    fun tags_light() = capture("tags", dark = false) {
        click(TestTags.BOTTOM_NAV_TAGS)
        waitForTag(TestTags.TAG_LIST_FAB)
        waitForTagPrefix(TestTags.TAG_LIST_ITEM_PREFIX)
    }

    @Test
    fun tags_dark() = capture("tags", dark = true) {
        click(TestTags.BOTTOM_NAV_TAGS)
        waitForTag(TestTags.TAG_LIST_FAB)
        waitForTagPrefix(TestTags.TAG_LIST_ITEM_PREFIX)
    }

    /** (5) 表タブ（ボトムナビの「表」タブ） */
    @Test
    fun tables_light() = capture("tables", dark = false) {
        click(TestTags.BOTTOM_NAV_TABLES)
        waitForTag(TestTags.TABLE_INDEX_LIST)
    }

    @Test
    fun tables_dark() = capture("tables", dark = true) {
        click(TestTags.BOTTOM_NAV_TABLES)
        waitForTag(TestTags.TABLE_INDEX_LIST)
    }

    /** (6) タグ別表ビュー（表タブから「開発」を開く） */
    @Test
    fun tagTable_light() = capture("tag_table", dark = false) {
        openDevTagTable()
    }

    @Test
    fun tagTable_dark() = capture("tag_table", dark = true) {
        openDevTagTable()
    }

    private fun openDevTagTable() {
        click(TestTags.BOTTOM_NAV_TABLES)
        waitForTag(TestTags.TABLE_INDEX_LIST)
        click(TestTags.TABLE_INDEX_ITEM_PREFIX + devTagId.value)
        waitForTag(TestTags.TAG_TABLE)
        waitForTagPrefix(TestTags.TAG_TABLE_ROW_PREFIX)
    }
}
