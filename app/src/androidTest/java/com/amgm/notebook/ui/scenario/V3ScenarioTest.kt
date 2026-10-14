package com.amgm.notebook.ui.scenario

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amgm.notebook.MainActivity
import com.amgm.notebook.NotebookApplication
import com.amgm.notebook.ui.TestTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v3 機能（ボトムナビ・表タブ・値の状態 null/空文字・タグ取り外し時のプロパティ整理・H1トグル）の
 * 実機（エミュレータ）シナリオテスト。DB のクリーンアップは UiScenarioTest と同じ方式。
 */
@RunWith(AndroidJUnit4::class)
class V3ScenarioTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val timeout = 10_000L

    @Before
    fun setUp() {
        clearAllData()
        rule.waitForIdle()
    }

    @After
    fun tearDown() {
        clearAllData()
    }

    private val container
        get() = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as NotebookApplication).container

    private fun clearAllData() {
        val c = container
        runBlocking {
            c.noteRepository.observeNotes("").first().forEach { c.noteRepository.delete(it.id) }
            c.tagRepository.getAllTags().forEach { c.tagRepository.delete(it.id) }
        }
    }

    // ---- データ参照ヘルパー（リポジトリ経由・読み取りのみ） ----

    private fun tagIdOf(name: String): String = runBlocking {
        container.tagRepository.getAllTags().first { it.name == name }.id.value
    }

    private fun noteIdOf(title: String): String = runBlocking {
        container.noteRepository.observeNotes("").first().first { it.title == title }.id.value
    }

    // ---- UI ヘルパー ----

    private fun tagStartingWith(prefix: String) = SemanticsMatcher("testTag starts with $prefix") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private fun waitForTag(tag: String) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTagGone(tag: String) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForText(text: String) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(tag: String, scroll: Boolean = false) {
        waitForTag(tag)
        val node = rule.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
    }

    private fun input(tag: String, text: String) {
        waitForTag(tag)
        rule.onNodeWithTag(tag).performScrollTo().performTextInput(text)
    }

    private fun replaceText(tag: String, text: String) {
        waitForTag(tag)
        rule.onNodeWithTag(tag).performScrollTo().performTextReplacement(text)
    }

    /** 編集欄の現在のテキスト。 */
    private fun editableText(tag: String): String =
        rule.onNodeWithTag(tag).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()

    private fun waitForEditableText(tag: String, expected: String) {
        waitForTag(tag)
        rule.waitUntil(timeoutMillis = timeout) { editableText(tag) == expected }
    }

    private fun waitForEditableTextContains(tag: String, expected: String) {
        waitForTag(tag)
        rule.waitUntil(timeoutMillis = timeout) { editableText(tag).contains(expected) }
    }

    /**
     * ボトムナビの「タグ」タブから新規タグを作成し、「ノート」タブでノート一覧へ戻る。
     * definition は (プロパティ名, 型キー)。
     */
    private fun createTagViaTabs(name: String, definition: Pair<String, String>? = null) {
        click(TestTags.BOTTOM_NAV_TAGS)
        click(TestTags.TAG_LIST_FAB)
        input(TestTags.TAG_EDIT_NAME, name)
        if (definition != null) {
            click(TestTags.TAG_EDIT_ADD_DEFINITION, scroll = true)
            input(TestTags.TAG_EDIT_DEFINITION_NAME_PREFIX + "0", definition.first)
            click(TestTags.TAG_EDIT_DEFINITION_TYPE_PREFIX + "0_" + definition.second, scroll = true)
        }
        click(TestTags.TAG_EDIT_SAVE)
        waitForTag(TestTags.TAG_LIST_FAB)
        waitForText(name)
        click(TestTags.BOTTOM_NAV_NOTES)
        waitForTag(TestTags.NOTE_LIST_FAB)
    }

    /** ノート一覧から新規ノートを開き、タイトルを入力して指定タグ（複数可）を付ける。保存はしない。 */
    private fun startNewNote(title: String, vararg tagIds: String) {
        click(TestTags.NOTE_LIST_FAB)
        input(TestTags.NOTE_EDIT_TITLE, title)
        tagIds.forEach { click(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX + it, scroll = true) }
    }

    private fun saveAndWaitForList(title: String) {
        click(TestTags.NOTE_EDIT_SAVE)
        waitForTagGone(TestTags.NOTE_EDIT_SAVE)
        waitForTag(TestTags.NOTE_LIST_FAB)
        waitForText(title)
    }

    /** ノート一覧から既存ノートを開き直す（タイトルの読み込み完了まで待つ）。 */
    private fun openExistingNote(title: String) {
        click(TestTags.NOTE_LIST_ITEM_PREFIX + noteIdOf(title))
        waitForTag(TestTags.NOTE_EDIT_SAVE)
        waitForEditableText(TestTags.NOTE_EDIT_TITLE, title)
    }

    // ---- シナリオ ----

    /** a: ボトムバーが見え、表タブ → 一覧 → 表ビュー、タグタブ → タグ一覧に切り替えられる。 */
    @Test
    fun scenarioA_bottomNav_tabsSwitchAndTableIndexNavigates() {
        waitForTag(TestTags.BOTTOM_NAV)
        rule.onNodeWithTag(TestTags.BOTTOM_NAV_NOTES).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.BOTTOM_NAV_TAGS).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.BOTTOM_NAV_TABLES).assertIsDisplayed()

        // タグが無い間、表タブは空表示
        click(TestTags.BOTTOM_NAV_TABLES)
        waitForTag(TestTags.TABLE_INDEX_EMPTY)

        createTagViaTabs("商品", definition = "価格" to "Number")
        val tagId = tagIdOf("商品")

        // 表タブに作成済みタグが並び、押すと表ビューへ
        click(TestTags.BOTTOM_NAV_TABLES)
        waitForTag(TestTags.TABLE_INDEX_LIST)
        waitForTag(TestTags.TABLE_INDEX_ITEM_PREFIX + tagId)
        rule.onNodeWithTag(TestTags.TABLE_INDEX_ITEM_PREFIX + tagId).assertIsDisplayed()
        click(TestTags.TABLE_INDEX_ITEM_PREFIX + tagId)
        waitForTag(TestTags.TAG_TABLE)
        // 表ビューはトップレベルではないのでボトムバーは出ない
        waitForTagGone(TestTags.BOTTOM_NAV)

        // 戻ると表タブの一覧に戻り、ボトムバーも復活する
        click(TestTags.TAG_TABLE_BACK)
        waitForTag(TestTags.TABLE_INDEX_LIST)
        waitForTag(TestTags.BOTTOM_NAV)

        // タグタブでタグ一覧が出る
        click(TestTags.BOTTOM_NAV_TAGS)
        waitForTag(TestTags.TAG_LIST_FAB)
        waitForTag(TestTags.TAG_LIST_ITEM_PREFIX + tagId)

        // ノートタブでノート一覧に戻る
        click(TestTags.BOTTOM_NAV_NOTES)
        waitForTag(TestTags.NOTE_LIST_FAB)
    }

    /**
     * b: タグを付けただけの値は未入力（プレースホルダ）。入力して全部消すと空文字で保持され、
     * タグを外した後も自由プロパティとして残る。
     */
    @Test
    fun scenarioB_nullThenEmptyText_emptyTextSurvivesTagRemoval() {
        createTagViaTabs("メモ帳", definition = "メモ" to "Text")
        val tagId = tagIdOf("メモ帳")
        val propTag = TestTags.NOTE_EDIT_PROPERTY_PREFIX + "メモ"
        val removeTag = TestTags.NOTE_EDIT_PROPERTY_REMOVE_PREFIX + "メモ"

        // タグを付けただけ → 欄は空でプレースホルダ「未入力」
        startNewNote("空文字テスト", tagId)
        waitForTag(propTag)
        assertEquals("", editableText(propTag))
        waitForText("未入力")

        // 入力して全部消す（Text("") になる）
        input(propTag, "あいう")
        waitForEditableText(propTag, "あいう")
        replaceText(propTag, "")
        waitForEditableText(propTag, "")
        saveAndWaitForList("空文字テスト")

        // 開き直しても欄は存在し、空欄
        openExistingNote("空文字テスト")
        waitForTag(propTag)
        assertEquals("", editableText(propTag))
        // タグ由来の項目なので削除ボタン（自由プロパティ専用）はまだ無い
        waitForTagGone(removeTag)

        // タグを外す → 空文字の項目は自由プロパティとして残る
        click(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX + tagId, scroll = true)
        waitForTag(removeTag)
        waitForTag(propTag)
        saveAndWaitForList("空文字テスト")

        // 保存して開き直しても自由プロパティとして残っている
        openExistingNote("空文字テスト")
        waitForTag(propTag)
        waitForTag(removeTag)
        waitForText("自由プロパティ")
        assertEquals("", editableText(propTag))
    }

    /** c: A(文字列「メモ」)とB(数値「価格」)を付け、メモだけ入力。B を外すと null の「価格」は消え、「メモ」は残る。 */
    @Test
    fun scenarioC_removeTag_nullPropertyDropped_filledPropertyKept() {
        createTagViaTabs("メモ類", definition = "メモ" to "Text")
        createTagViaTabs("値段類", definition = "価格" to "Number")
        val tagA = tagIdOf("メモ類")
        val tagB = tagIdOf("値段類")
        val memoTag = TestTags.NOTE_EDIT_PROPERTY_PREFIX + "メモ"
        val priceTag = TestTags.NOTE_EDIT_PROPERTY_PREFIX + "価格"

        startNewNote("取り外しテスト", tagA, tagB)
        waitForTag(memoTag)
        waitForTag(priceTag)

        // 「メモ」にだけ値を入れ、「価格」は未入力のまま
        input(memoTag, "ほげ")
        waitForEditableText(memoTag, "ほげ")
        assertEquals("", editableText(priceTag))

        // B を外す → 価格(null)は消え、メモは残る
        click(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX + tagB, scroll = true)
        waitForTagGone(priceTag)
        waitForTag(memoTag)
        assertEquals("ほげ", editableText(memoTag))
        saveAndWaitForList("取り外しテスト")

        // 保存後に開き直しても同じ（価格なし・メモあり）
        openExistingNote("取り外しテスト")
        waitForTag(memoTag)
        waitForEditableText(memoTag, "ほげ")
        assertTrue(rule.onAllNodesWithTag(priceTag).fetchSemanticsNodes().isEmpty())
    }

    /** d: H1 同期トグル。ON では見出しがタイトルに追従し、OFF にすると追従しない（testTag で操作）。 */
    @Test
    fun scenarioD_h1Toggle_followsTitleOnlyWhenEnabled() {
        click(TestTags.NOTE_LIST_FAB)
        input(TestTags.NOTE_EDIT_TITLE, "最初の題")
        waitForEditableTextContains(TestTags.NOTE_EDIT_BODY, "# 最初の題")

        replaceText(TestTags.NOTE_EDIT_TITLE, "次の題")
        waitForEditableTextContains(TestTags.NOTE_EDIT_BODY, "# 次の題")
        assertTrue(!editableText(TestTags.NOTE_EDIT_BODY).contains("最初の題"))

        click(TestTags.NOTE_EDIT_AUTO_SYNC_H1, scroll = true)
        replaceText(TestTags.NOTE_EDIT_TITLE, "三つ目の題")
        waitForEditableTextContains(TestTags.NOTE_EDIT_TITLE, "三つ目の題")
        rule.waitForIdle()
        val body = editableText(TestTags.NOTE_EDIT_BODY)
        assertTrue("見出しは変わらないはず: $body", body.contains("# 次の題"))
        assertTrue("新タイトルは本文に入らないはず: $body", !body.contains("三つ目の題"))
    }
}
