@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.scenario

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.rules.ValidationIssue
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.ui.noteedit.PropertyOrigin
import com.amgm.notebook.ui.tagedit.PropertyDefinitionDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MultiTagConflictScenarioTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `型が異なる同名プロパティを持つ2タグを親にすると衝突が見え子で再定義すると解消する`() = runTest {
        val app = ScenarioApp()
        app.repos.tags.save(tagOf("a", defs = listOf(defOf("x", PropertyType.Number))))
        app.repos.tags.save(tagOf("b", defs = listOf(defOf("x", PropertyType.Text), defOf("y"))))

        // 両方を親に持つタグ c を作成する
        val cEdit = app.tagEdit(null)
        advanceUntilIdle()
        cEdit.onNameChange("c")
        cEdit.onParentToggle(TagId("a"))
        cEdit.onParentToggle(TagId("b"))
        val inherited = cEdit.uiState.value.inheritedSchema
        assertEquals(listOf("y"), inherited.definitions.map { it.definition.name.value })
        assertEquals(PropertyName("x"), inherited.conflicts.single().name)
        cEdit.onSaveClick()
        advanceUntilIdle()
        val cId = TagId("id-1")

        // ノート編集に警告が出て、x は定義済みプロパティとして扱われない
        val noteEdit = app.noteEdit(null)
        advanceUntilIdle()
        noteEdit.onTagToggle(cId)
        val s = noteEdit.uiState.value
        assertTrue(s.issues.any { it is ValidationIssue.Conflict && it.conflict.name == PropertyName("x") })
        assertEquals(listOf("y"), s.propertyRows.map { it.name.value })

        // 子 c で x を再定義して解消する
        val cReedit = app.tagEdit(cId)
        advanceUntilIdle()
        cReedit.onAddDefinition()
        cReedit.onDefinitionChange(0, PropertyDefinitionDraft("x", PropertyType.Text, false))
        cReedit.onSaveClick()
        advanceUntilIdle()

        val noteEdit2 = app.noteEdit(null)
        advanceUntilIdle()
        noteEdit2.onTagToggle(cId)
        val resolved = noteEdit2.uiState.value
        assertTrue(resolved.issues.none { it is ValidationIssue.Conflict })
        val xRow = resolved.propertyRows.single { it.name.value == "x" }
        assertEquals(PropertyType.Text, xRow.type)
        assertEquals(PropertyOrigin.Defined(cId), xRow.origin)
    }
}
