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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amgm.notebook.MainActivity
import com.amgm.notebook.NotebookApplication
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.common.UiMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 実機（エミュレータ）上の Compose UI シナリオテスト。
 * アプリ本体の AppContainer（Room 実DB）を使うため、各テストの前後にリポジトリ経由で全データを削除する。
 */
@RunWith(AndroidJUnit4::class)
class UiScenarioTest {

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

    /** リポジトリ経由で全削除する（Room の Flow が更新通知を受け取れるように DB 直操作はしない）。 */
    private fun clearAllData() {
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as NotebookApplication
        val container = app.container
        runBlocking {
            container.noteRepository.observeNotes("").first().forEach {
                container.noteRepository.delete(it.id)
            }
            container.tagRepository.getAllTags().forEach {
                container.tagRepository.delete(it.id)
            }
        }
    }

    // ---- ヘルパー ----

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

    private fun waitForText(text: String) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTagGone(tag: String) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    /** scroll=true はスクロール可能コンテナ内の要素（編集フォーム内）に使う。FAB・トップバー・ダイアログには不要。 */
    private fun click(tag: String, scroll: Boolean = false) {
        waitForTag(tag)
        val node = rule.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
    }

    /** 入力欄は常にスクロール可能なフォーム内にある。 */
    private fun input(tag: String, text: String) {
        waitForTag(tag)
        rule.onNodeWithTag(tag).performScrollTo().performTextInput(text)
    }

    private fun clickFirstWithPrefix(prefix: String, scroll: Boolean = false) {
        waitForTagPrefix(prefix)
        val node = rule.onAllNodes(tagStartingWith(prefix))[0]
        if (scroll) node.performScrollTo()
        node.performClick()
    }

    // ---- シナリオ ----

    /** シナリオA: ノート作成 → 保存 → 一覧にタイトルが表示される。 */
    @Test
    fun scenarioA_createNote_appearsInList() {
        waitForTag(TestTags.NOTE_LIST_FAB)
        click(TestTags.NOTE_LIST_FAB)

        input(TestTags.NOTE_EDIT_TITLE, "シナリオAのノート")
        input(TestTags.NOTE_EDIT_BODY, "本文テキスト")
        click(TestTags.NOTE_EDIT_SAVE)

        // 保存後に一覧へ戻る
        waitForTag(TestTags.NOTE_LIST_FAB)
        waitForText("シナリオAのノート")
        waitForTagPrefix(TestTags.NOTE_LIST_ITEM_PREFIX)
        rule.onAllNodes(tagStartingWith(TestTags.NOTE_LIST_ITEM_PREFIX))[0].assertIsDisplayed()
    }

    /** シナリオB: タグ+数値プロパティ定義 → ノートで値入力 → タグ別表に値が出る。 */
    @Test
    fun scenarioB_tagWithNumberProperty_valueShownInTable() {
        // 一覧 → タグ一覧 → 新規タグ
        click(TestTags.NOTE_LIST_TAGS_BUTTON)
        click(TestTags.TAG_LIST_FAB)

        input(TestTags.TAG_EDIT_NAME, "商品")
        click(TestTags.TAG_EDIT_ADD_DEFINITION, scroll = true)
        input(TestTags.TAG_EDIT_DEFINITION_NAME_PREFIX + "0", "価格")
        click(TestTags.TAG_EDIT_DEFINITION_TYPE_PREFIX + "0_Number", scroll = true)
        click(TestTags.TAG_EDIT_SAVE)

        // タグ一覧に戻り、タグが現れる
        waitForTagPrefix(TestTags.TAG_LIST_ITEM_PREFIX)
        waitForText("商品")

        // 一覧へ戻りノート作成
        click(TestTags.TAG_LIST_BACK)
        click(TestTags.NOTE_LIST_FAB)
        input(TestTags.NOTE_EDIT_TITLE, "りんご")
        clickFirstWithPrefix(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX, scroll = true)
        input(TestTags.NOTE_EDIT_PROPERTY_PREFIX + "価格", "1500")
        click(TestTags.NOTE_EDIT_SAVE)

        // 一覧 → タグ一覧 → タグをタップして表ビュー
        waitForTag(TestTags.NOTE_LIST_FAB)
        waitForText("りんご")
        click(TestTags.NOTE_LIST_TAGS_BUTTON)
        clickFirstWithPrefix(TestTags.TAG_LIST_ITEM_PREFIX)

        waitForTag(TestTags.TAG_TABLE)
        waitForTagPrefix(TestTags.TAG_TABLE_ROW_PREFIX)
        rule.onAllNodes(tagStartingWith(TestTags.TAG_TABLE_ROW_PREFIX))[0].assertIsDisplayed()
        waitForText("りんご")
        waitForText("1500")

        // 戻る操作でタグ一覧 → ノート一覧に戻れる
        click(TestTags.TAG_TABLE_BACK)
        waitForTag(TestTags.TAG_LIST_FAB)
        click(TestTags.TAG_LIST_BACK)
        waitForTag(TestTags.NOTE_LIST_FAB)
    }

    /** シナリオC: 型不一致の数値入力は保存が拒否され、メッセージが出て編集画面に留まる。戻ると未保存確認が出る。 */
    @Test
    fun scenarioC_typeMismatch_blocksSaveWithMessage() {
        click(TestTags.NOTE_LIST_TAGS_BUTTON)
        click(TestTags.TAG_LIST_FAB)
        input(TestTags.TAG_EDIT_NAME, "計測")
        click(TestTags.TAG_EDIT_ADD_DEFINITION, scroll = true)
        input(TestTags.TAG_EDIT_DEFINITION_NAME_PREFIX + "0", "重さ")
        click(TestTags.TAG_EDIT_DEFINITION_TYPE_PREFIX + "0_Number", scroll = true)
        click(TestTags.TAG_EDIT_SAVE)
        waitForTagPrefix(TestTags.TAG_LIST_ITEM_PREFIX)
        click(TestTags.TAG_LIST_BACK)

        click(TestTags.NOTE_LIST_FAB)
        input(TestTags.NOTE_EDIT_TITLE, "不正な値")
        clickFirstWithPrefix(TestTags.NOTE_EDIT_TAG_CHIP_PREFIX, scroll = true)
        input(TestTags.NOTE_EDIT_PROPERTY_PREFIX + "重さ", "abc")
        click(TestTags.NOTE_EDIT_SAVE)

        waitForText(UiMessage.SaveBlockedByErrors.displayText())
        // 編集画面に留まっている
        rule.onNodeWithTag(TestTags.NOTE_EDIT_SAVE).assertIsDisplayed()

        // 未保存のまま戻る → 破棄確認 → 破棄で一覧へ
        click(TestTags.NOTE_EDIT_BACK)
        click(TestTags.NOTE_EDIT_DISCARD_CONFIRM)
        waitForTag(TestTags.NOTE_LIST_FAB)
        waitForTagGone(TestTags.NOTE_EDIT_SAVE)
    }
}
