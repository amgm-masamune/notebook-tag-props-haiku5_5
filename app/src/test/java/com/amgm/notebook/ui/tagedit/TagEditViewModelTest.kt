@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.ui.tagedit

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.rules.EffectiveSchema
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.testing.InMemoryRepositories
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.testutil.SequentialIdGenerator
import com.amgm.notebook.testutil.recordEvents
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.common.UiMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TagEditViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val ids = SequentialIdGenerator()

    private fun TestScope.createViewModel(repos: InMemoryRepositories, tagId: TagId? = null): TagEditViewModel {
        val vm = TagEditViewModel(tagId, repos.tags, ids)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `新規の初期状態では全タグが親候補`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("a"))
        repos.tags.save(tagOf("b"))
        val vm = TagEditViewModel(null, repos.tags, ids)
        assertFalse(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.isNew)
        assertEquals("", s.name)
        assertEquals(listOf("a", "b"), s.parentCandidates.map { it.name })
        assertTrue(s.definitions.isEmpty())
        assertNull(s.error)
    }

    @Test
    fun `既存タグの読込`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("p", name = "親"))
        repos.tags.save(
            tagOf(
                "c",
                name = "子",
                parents = listOf("p"),
                defs = listOf(defOf("n", PropertyType.Number, required = true), defOf("t"))
            )
        )
        val vm = TagEditViewModel(TagId("c"), repos.tags, ids)
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.isNew)
        assertFalse(s.isLoading)
        assertEquals("子", s.name)
        assertEquals(setOf(TagId("p")), s.selectedParentIds)
        assertEquals(
            listOf(
                PropertyDefinitionDraft("n", PropertyType.Number, true),
                PropertyDefinitionDraft("t", PropertyType.Text, false)
            ),
            s.definitions
        )
    }

    @Test
    fun `親候補から自分と子孫が除外される`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("a"))
        repos.tags.save(tagOf("b", parents = listOf("a")))
        repos.tags.save(tagOf("c", parents = listOf("b")))
        repos.tags.save(tagOf("d"))
        val vm = createViewModel(repos, TagId("a"))
        assertEquals(listOf("d"), vm.uiState.value.parentCandidates.map { it.name })

        val vmB = createViewModel(repos, TagId("b"))
        assertEquals(listOf("a", "d"), vmB.uiState.value.parentCandidates.map { it.name })
    }

    @Test
    fun `親トグルで継承スキーマが更新される`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("p", defs = listOf(defOf("price", PropertyType.Number, required = true))))
        repos.tags.save(tagOf("q", defs = listOf(defOf("price", PropertyType.Text))))
        val vm = createViewModel(repos)

        vm.onParentToggle(TagId("p"))
        var schema = vm.uiState.value.inheritedSchema
        assertEquals(listOf("price"), schema.definitions.map { it.definition.name.value })
        assertEquals(TagId("p"), schema.definitions.single().definedBy)
        assertTrue(schema.conflicts.isEmpty())

        vm.onParentToggle(TagId("q"))
        schema = vm.uiState.value.inheritedSchema
        assertTrue(schema.definitions.isEmpty())
        assertEquals(PropertyName("price"), schema.conflicts.single().name)

        vm.onParentToggle(TagId("p"))
        vm.onParentToggle(TagId("q"))
        assertTrue(vm.uiState.value.selectedParentIds.isEmpty())
        assertEquals(EffectiveSchema.EMPTY, vm.uiState.value.inheritedSchema)
    }

    @Test
    fun `定義の追加と編集と削除と並べ替え`() = runTest {
        val vm = createViewModel(InMemoryRepositories())

        vm.onAddDefinition()
        vm.onAddDefinition()
        vm.onAddDefinition()
        assertEquals(3, vm.uiState.value.definitions.size)
        assertEquals(PropertyDefinitionDraft(""), vm.uiState.value.definitions.first())

        vm.onDefinitionChange(0, PropertyDefinitionDraft("a", PropertyType.Number, true))
        vm.onDefinitionChange(1, PropertyDefinitionDraft("b"))
        vm.onDefinitionChange(2, PropertyDefinitionDraft("c", PropertyType.Date))
        assertEquals(listOf("a", "b", "c"), vm.uiState.value.definitions.map { it.name })

        vm.onMoveDefinition(2, 0)
        assertEquals(listOf("c", "a", "b"), vm.uiState.value.definitions.map { it.name })
        vm.onMoveDefinition(0, 5) // 範囲外は無視
        assertEquals(listOf("c", "a", "b"), vm.uiState.value.definitions.map { it.name })

        vm.onRemoveDefinition(1)
        assertEquals(listOf("c", "b"), vm.uiState.value.definitions.map { it.name })
        vm.onRemoveDefinition(9) // 範囲外は無視
        assertEquals(2, vm.uiState.value.definitions.size)
    }

    @Test
    fun `名前が空だと保存できない`() = runTest {
        val repos = InMemoryRepositories()
        val vm = createViewModel(repos)
        val events = recordEvents(vm.events)

        vm.onNameChange("   ")
        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(TagEditError.BlankName, vm.uiState.value.error)
        assertTrue(events.isEmpty())
        assertTrue(repos.tags.snapshot.isEmpty())

        vm.onNameChange("x")
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `定義名の空と重複は保存できない`() = runTest {
        val repos = InMemoryRepositories()
        val vm = createViewModel(repos)
        vm.onNameChange("タグ")
        vm.onAddDefinition()
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(TagEditError.BlankDefinitionName, vm.uiState.value.error)

        vm.onDefinitionChange(0, PropertyDefinitionDraft("dup"))
        vm.onAddDefinition()
        vm.onDefinitionChange(1, PropertyDefinitionDraft(" dup ", PropertyType.Number))
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(TagEditError.DuplicateDefinitionName("dup"), vm.uiState.value.error)
        assertTrue(repos.tags.snapshot.isEmpty())
    }

    @Test
    fun `保存に成功すると採番されたIDで登録されNavigateBackする`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("p"))
        val vm = createViewModel(repos)
        val events = recordEvents(vm.events)

        vm.onNameChange("  書籍  ")
        vm.onParentToggle(TagId("p"))
        vm.onAddDefinition()
        vm.onDefinitionChange(0, PropertyDefinitionDraft("price", PropertyType.Number, true))
        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(
            listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.Saved), UiEvent.NavigateBack),
            events
        )
        val saved = repos.tags.snapshot.getValue(TagId("id-1"))
        assertEquals("書籍", saved.name)
        assertEquals(setOf(TagId("p")), saved.parentIds)
        val def = saved.ownPropertyDefinitions.single()
        assertEquals(PropertyName("price"), def.name)
        assertEquals(PropertyType.Number, def.type)
        assertTrue(def.required)
    }

    @Test
    fun `既存タグの保存は同じIDで更新される`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("t", name = "旧"))
        val vm = createViewModel(repos, TagId("t"))
        vm.onNameChange("新")
        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(1, repos.tags.snapshot.size)
        assertEquals("新", repos.tags.snapshot.getValue(TagId("t")).name)
    }

    @Test
    fun `循環する親を選んで保存するとCyclicエラーになり登録されない`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("a"))
        repos.tags.save(tagOf("b", parents = listOf("a")))
        val vm = createViewModel(repos, TagId("a"))
        val events = recordEvents(vm.events)

        // 候補には出ないが、直接トグルされた場合もリポジトリが拒否する
        vm.onParentToggle(TagId("b"))
        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(TagEditError.Cyclic, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSaving)
        assertEquals(listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.CyclicInheritance)), events)
        assertTrue(repos.tags.snapshot.getValue(TagId("a")).parentIds.isEmpty())
    }

    @Test
    fun `自己参照も循環として拒否される`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("a"))
        val vm = createViewModel(repos, TagId("a"))
        vm.onParentToggle(TagId("a"))
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(TagEditError.Cyclic, vm.uiState.value.error)
    }

    @Test
    fun `重複名はDuplicateNameエラーになる`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("t", name = "Book"))
        val vm = createViewModel(repos)
        val events = recordEvents(vm.events)

        vm.onNameChange("book")
        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(TagEditError.DuplicateName, vm.uiState.value.error)
        assertEquals(listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.DuplicateTagName)), events)
        assertEquals(1, repos.tags.snapshot.size)
    }

    @Test
    fun `存在しないIDはTagNotFoundで戻る`() = runTest {
        val vm = TagEditViewModel(TagId("none"), InMemoryRepositories().tags, ids)
        val events = recordEvents(vm.events)
        advanceUntilIdle()
        assertEquals(
            listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.TagNotFound), UiEvent.NavigateBack),
            events
        )
    }

    @Test
    fun `戻る操作はNavigateBackを発行する`() = runTest {
        val vm = createViewModel(InMemoryRepositories())
        val events = recordEvents(vm.events)
        vm.onBackClick()
        assertEquals(listOf<UiEvent>(UiEvent.NavigateBack), events)
    }
}
