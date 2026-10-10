@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.scenario

import com.amgm.notebook.domain.error.CyclicInheritanceException
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.testing.assertThrowsInstance
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.testutil.recordEvents
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.common.UiMessage
import com.amgm.notebook.ui.tagedit.TagEditError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CyclicInheritanceScenarioTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `A_B_Cの鎖でAの親候補にBとCは出ず循環保存はメッセージで拒否される`() = runTest {
        val app = ScenarioApp()
        // UI 操作で A <- B <- C を作成する
        val specs = listOf<Pair<String, TagId?>>(
            "A" to null,
            "B" to TagId("id-1"),
            "C" to TagId("id-2")
        )
        for ((name, parent) in specs) {
            val edit = app.tagEdit(null)
            advanceUntilIdle()
            edit.onNameChange(name)
            if (parent != null) edit.onParentToggle(parent)
            edit.onSaveClick()
            advanceUntilIdle()
        }
        val aId = TagId("id-1")
        val cId = TagId("id-3")
        assertEquals(listOf("A", "B", "C"), app.repos.tags.snapshot.values.map { it.name }.sorted())

        // A の編集画面では親候補が空（A 自身・B・C は除外される）
        val aEdit = app.tagEdit(aId)
        val events = recordEvents(aEdit.events)
        advanceUntilIdle()
        assertTrue(aEdit.uiState.value.parentCandidates.isEmpty())

        // それでも C を親にして保存しようとすると CyclicInheritance メッセージで拒否
        aEdit.onParentToggle(cId)
        aEdit.onSaveClick()
        advanceUntilIdle()
        assertEquals(TagEditError.Cyclic, aEdit.uiState.value.error)
        assertEquals(listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.CyclicInheritance)), events)
        assertTrue(app.repos.tags.snapshot.getValue(aId).parentIds.isEmpty())

        // リポジトリ直接でも同じく拒否される
        assertThrowsInstance<CyclicInheritanceException> {
            runBlocking { app.repos.tags.save(tagOf("id-1", parents = listOf("id-3"), name = "A")) }
        }
    }
}
