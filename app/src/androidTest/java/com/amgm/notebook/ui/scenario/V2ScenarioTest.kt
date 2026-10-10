package com.amgm.notebook.ui.scenario

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v2 機能（H1自動同期・タグ作成導線・表からのノート追加・タグのネスト表示・ノート紐付けプロパティ）の
 * 実機（エミュレータ）シナリオテスト。DB のクリーンアップは UiScenarioTest と同じ方式。
 */
@RunWith(AndroidJUnit4::class)
class V2ScenarioTest {

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

    /** チップの選択状態（Selected もしくは ToggleableState.On のどちらで表現されていても可）。 */
    private val isChipSelected = SemanticsMatcher("is selected or toggled on") { node ->
        node.config.getOrNull(SemanticsProperties.Selected) == true ||
            node.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On
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

    /** 編集欄（OutlinedTextField）の現在のテキスト。 */
    private fun editableText(tag: String): String =
        rule.onNodeWithTag(tag).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()

    private fun waitForEditableTextContains(tag: String, expected: String) {
        waitForTag(tag)
        rule.waitUntil(timeoutMillis = timeout) { editableText(tag).contains(expected) }
    }

    private fun xOf(tag: String): Float {
        waitForTag(tag)
        return rule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.x
    }

    /** ノート一覧から: タグ一覧 → 新規タグ → 保存 → ノート一覧へ戻る。definition は (プロパティ名, 型キー)。 */
    private fun createTagViaUi(
        name: String,
        definition: Pair<String, String>? = null,
        linkTargetTagId: String? = null,
        selectFirstParent: Boolean = false
    ) {
        click(TestTags.NOTE_LIST_TAGS_BUTTON)
        click(TestTags.TAG_LIST_FAB)
        input(TestTags.TAG_EDIT_NAME, name)
        if (selectFirstParent) {
            waitForTagPrefix(TestTags.TAG_EDIT_PARENT_CHIP_PREFIX)
            rule.onAllNodes(tagStartingWith(TestTags.TAG_EDIT_PARENT_CHIP_PREFIX))[0]
                .performScrollTo().performClick()
        }
        if (definition != null) {
            click(TestTags.TAG_EDIT_ADD_DEFINITION, scroll = true)
            input(TestTags.TAG_EDIT_DEFINITION_NAME_PREFIX + "0", definition.first)
            click(TestTags.TAG_EDIT_DEFINITION_TYPE_PREFIX + "0_" + definition.second, scroll = true)
            if (linkTargetTagId != null) {
                click(TestTags.TAG_EDIT_DEFINITION_LINK_TARGET_PREFIX + "0_" + linkTargetTagId, scroll = true)
            }
        }
        click(TestTags.TAG_EDIT_SAVE)
        waitForTagPrefix(TestTags.TAG_LIST_ITEM_PREFIX)
        waitForText(name)
        click(TestTags.TAG_LIST_BACK)
        waitForTag(TestTags.NOTE_LIST_FAB)
    }

    /** ノート一覧から: 新規ノート → タイトル（と任意のタグ選択）→ 保存 → ノート一覧へ戻る。 */
    private fun createNoteViaUi(title: String, tagIdToSelect: String? = null) {
        click(TestTags.NOTE_LIST_FAB)
        input(TestTags.NOTE_EDIT_TITLE, title)
        if (tagIdToSelect != null) {
            click(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX + tagIdToSelect, scroll = true)
        }
        click(TestTags.NOTE_EDIT_SAVE)
        waitForTagGone(TestTags.NOTE_EDIT_SAVE)
        waitForTag(TestTags.NOTE_LIST_FAB)
        waitForText(title)
    }

    // ---- シナリオ ----

    /** 1: タイトルと本文 h1 の自動同期。トグル ON で追従し、OFF にすると追従しない。 */
    @Test
    fun scenario1_h1AutoSync_followsTitleOnlyWhenEnabled() {
        click(TestTags.NOTE_LIST_FAB)
        input(TestTags.NOTE_EDIT_TITLE, "最初の題")
        waitForEditableTextContains(TestTags.NOTE_EDIT_BODY, "# 最初の題")

        replaceText(TestTags.NOTE_EDIT_TITLE, "次の題")
        waitForEditableTextContains(TestTags.NOTE_EDIT_BODY, "# 次の題")
        assertTrue(!editableText(TestTags.NOTE_EDIT_BODY).contains("最初の題"))

        // OFF にしてタイトルを変えても見出しは変わらない
        click(TestTags.NOTE_EDIT_AUTO_SYNC_H1, scroll = true)
        replaceText(TestTags.NOTE_EDIT_TITLE, "三つ目の題")
        // タイトル反映の完了を待ってから本文を確認
        rule.waitUntil(timeoutMillis = timeout) { editableText(TestTags.NOTE_EDIT_TITLE).contains("三つ目の題") }
        rule.waitForIdle()
        val body = editableText(TestTags.NOTE_EDIT_BODY)
        assertTrue("見出しは変わらないはず: $body", body.contains("# 次の題"))
        assertTrue("新タイトルは本文に入らないはず: $body", !body.contains("三つ目の題"))
    }

    /** 2: ノート編集の「タグを作成」→ タグ編集で保存 → ノート編集に戻り新タグのチップが出る。 */
    @Test
    fun scenario2_createTagFromNoteEdit_chipAppears() {
        click(TestTags.NOTE_LIST_FAB)
        input(TestTags.NOTE_EDIT_TITLE, "導線テスト")
        click(TestTags.NOTE_EDIT_CREATE_TAG, scroll = true)

        input(TestTags.TAG_EDIT_NAME, "新規導線タグ")
        click(TestTags.TAG_EDIT_SAVE)

        waitForTag(TestTags.NOTE_EDIT_SAVE)
        waitForTagPrefix(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX)
        waitForText("新規導線タグ")
        rule.onAllNodes(tagStartingWith(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX))[0].assertIsDisplayed()
    }

    /** 3: タグ別表の「ノートを追加」→ 該当タグが選択済みの新規ノート → 保存で表に行が出る。 */
    @Test
    fun scenario3_addNoteFromTagTable_tagPreselected() {
        createTagViaUi("商品", definition = "価格" to "Number")
        val tagId = tagIdOf("商品")

        click(TestTags.NOTE_LIST_TAGS_BUTTON)
        click(TestTags.TAG_LIST_ITEM_PREFIX + tagId)
        waitForTag(TestTags.TAG_TABLE)
        click(TestTags.TAG_TABLE_ADD_NOTE)

        waitForTag(TestTags.NOTE_EDIT_TITLE)
        waitForTag(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX + tagId)
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.TestTag, TestTags.NOTE_EDIT_TAG_CHIP_PREFIX + tagId)
                    and isChipSelected
            ).fetchSemanticsNodes().isNotEmpty()
        }

        input(TestTags.NOTE_EDIT_TITLE, "表から追加したノート")
        click(TestTags.NOTE_EDIT_SAVE)

        waitForTag(TestTags.TAG_TABLE)
        waitForTagPrefix(TestTags.TAG_TABLE_ROW_PREFIX)
        waitForText("表から追加したノート")
    }

    /** 4: 親子タグ。子の行が親の行より右（インデント）に表示される。 */
    @Test
    fun scenario4_nestedTags_childIndentedUnderParent() {
        createTagViaUi("乗り物")
        createTagViaUi("自動車", selectFirstParent = true)
        val parentId = tagIdOf("乗り物")
        val childId = tagIdOf("自動車")

        click(TestTags.NOTE_LIST_TAGS_BUTTON)
        waitForTag(TestTags.TAG_LIST_ITEM_PREFIX + parentId)
        waitForTag(TestTags.TAG_LIST_ITEM_PREFIX + childId)
        rule.onNodeWithTag(TestTags.TAG_LIST_ITEM_PREFIX + parentId).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.TAG_LIST_ITEM_PREFIX + childId).assertIsDisplayed()
        waitForText("親: 乗り物")

        val parentX = xOf(TestTags.TAG_LIST_ITEM_PREFIX + parentId)
        val childX = xOf(TestTags.TAG_LIST_ITEM_PREFIX + childId)
        assertTrue("子($childX)は親($parentX)より右にあるはず", childX > parentX)
    }

    /** 5: NoteLink プロパティ。ピッカーの初期絞り込みは対象タグ。1件選んで確定、保存後も残る。 */
    @Test
    fun scenario5_noteLinkProperty_pickAndPersist() {
        createTagViaUi("人物")
        val personTagId = tagIdOf("人物")
        createNoteViaUi("田中", personTagId)
        createNoteViaUi("鈴木", personTagId)
        createTagViaUi("会議", definition = "出席者" to "NoteLink", linkTargetTagId = personTagId)
        val meetingTagId = tagIdOf("会議")
        val tanakaId = noteIdOf("田中")
        val suzukiId = noteIdOf("鈴木")

        click(TestTags.NOTE_LIST_FAB)
        input(TestTags.NOTE_EDIT_TITLE, "定例会議")
        click(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX + meetingTagId, scroll = true)
        click(TestTags.NOTE_EDIT_LINK_PICK_PREFIX + "出席者", scroll = true)

        waitForTag(TestTags.NOTE_PICKER)
        waitForTag(TestTags.NOTE_PICKER_ITEM_PREFIX + tanakaId)
        waitForTag(TestTags.NOTE_PICKER_ITEM_PREFIX + suzukiId)
        // 初期絞り込みは「人物」
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodes(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TestTag,
                    TestTags.NOTE_PICKER_FILTER_TAG_PREFIX + personTagId
                ) and isChipSelected
            ).fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithTag(TestTags.NOTE_PICKER_ITEM_PREFIX + tanakaId).performClick()
        click(TestTags.NOTE_PICKER_CONFIRM)
        waitForTagGone(TestTags.NOTE_PICKER)

        val chipTag = TestTags.NOTE_EDIT_LINK_CHIP_PREFIX + "出席者_" + tanakaId
        waitForTag(chipTag)
        assertTrue(
            rule.onAllNodesWithTag(TestTags.NOTE_EDIT_LINK_CHIP_PREFIX + "出席者_" + suzukiId)
                .fetchSemanticsNodes().isEmpty()
        )

        click(TestTags.NOTE_EDIT_SAVE)
        waitForTagGone(TestTags.NOTE_EDIT_SAVE)
        waitForTag(TestTags.NOTE_LIST_FAB)
        waitForText("定例会議")

        // 開き直してもチップが残っている
        click(TestTags.NOTE_LIST_ITEM_PREFIX + noteIdOf("定例会議"))
        waitForTag(chipTag)
        rule.onNodeWithTag(chipTag).performScrollTo().assertIsDisplayed()
    }

    /** 6: ピッカーの絞り込みを「すべて」にすると、タグの無いノートも候補に出る。 */
    @Test
    fun scenario6_pickerFilterAll_showsUntaggedNotes() {
        createTagViaUi("人物")
        val personTagId = tagIdOf("人物")
        createNoteViaUi("山田", personTagId)
        createNoteViaUi("タグなしメモ")
        createTagViaUi("会議", definition = "出席者" to "NoteLink", linkTargetTagId = personTagId)
        val meetingTagId = tagIdOf("会議")
        val yamadaId = noteIdOf("山田")
        val untaggedId = noteIdOf("タグなしメモ")

        click(TestTags.NOTE_LIST_FAB)
        input(TestTags.NOTE_EDIT_TITLE, "臨時会議")
        click(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX + meetingTagId, scroll = true)
        click(TestTags.NOTE_EDIT_LINK_PICK_PREFIX + "出席者", scroll = true)

        waitForTag(TestTags.NOTE_PICKER_ITEM_PREFIX + yamadaId)
        assertTrue(
            rule.onAllNodesWithTag(TestTags.NOTE_PICKER_ITEM_PREFIX + untaggedId).fetchSemanticsNodes().isEmpty()
        )

        click(TestTags.NOTE_PICKER_FILTER_ALL)
        waitForTag(TestTags.NOTE_PICKER_ITEM_PREFIX + untaggedId)
        rule.onNodeWithTag(TestTags.NOTE_PICKER_ITEM_PREFIX + untaggedId).assertIsDisplayed()
    }
}
