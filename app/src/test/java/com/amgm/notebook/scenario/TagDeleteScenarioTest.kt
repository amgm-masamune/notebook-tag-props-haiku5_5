@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.scenario

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.ui.noteedit.PropertyOrigin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TagDeleteScenarioTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `タグ削除後ノートのタグは消えプロパティ値は自由プロパティとして残り子タグの親が減る`() = runTest {
        val app = ScenarioApp()
        app.repos.tags.save(tagOf("p", defs = listOf(defOf("price", PropertyType.Number))))
        app.repos.tags.save(tagOf("c", parents = listOf("p")))
        app.repos.notes.save(
            noteOf("n", tags = listOf("p"), props = mapOf("price" to PropertyValue.Number(300.0)))
        )

        val tagList = app.tagList()
        advanceUntilIdle()
        tagList.onDeleteTagClick(TagId("p"))
        tagList.onDeleteConfirm()
        advanceUntilIdle()

        assertEquals(listOf("c"), tagList.uiState.value.items.map { it.name })
        assertTrue(app.repos.tags.snapshot.getValue(TagId("c")).parentIds.isEmpty())

        val saved = app.repos.notes.snapshot.getValue(NoteId("n"))
        assertTrue(saved.tagIds.isEmpty())
        assertEquals(PropertyValue.Number(300.0), saved.properties[PropertyName("price")])

        // ノート編集では price が自由プロパティ行として残る
        val edit = app.noteEdit(NoteId("n"))
        advanceUntilIdle()
        val row = edit.uiState.value.propertyRows.single()
        assertEquals(PropertyName("price"), row.name)
        assertEquals(PropertyOrigin.Free, row.origin)
        assertEquals("300", row.input)
    }
}
