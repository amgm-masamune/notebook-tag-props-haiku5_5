package com.amgm.notebook.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amgm.notebook.di.AppContainer
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.common.noteEditViewModelFactory
import com.amgm.notebook.ui.common.noteListViewModelFactory
import com.amgm.notebook.ui.common.tagEditViewModelFactory
import com.amgm.notebook.ui.common.tableIndexViewModelFactory
import com.amgm.notebook.ui.common.tagListViewModelFactory
import com.amgm.notebook.ui.common.tagTableViewModelFactory
import com.amgm.notebook.ui.noteedit.NoteEditScreen
import com.amgm.notebook.ui.noteedit.NoteEditViewModel
import com.amgm.notebook.ui.notelist.NoteListScreen
import com.amgm.notebook.ui.notelist.NoteListViewModel
import com.amgm.notebook.ui.tagedit.TagEditScreen
import com.amgm.notebook.ui.tagedit.TagEditViewModel
import com.amgm.notebook.ui.tableindex.TableIndexScreen
import com.amgm.notebook.ui.tableindex.TableIndexViewModel
import com.amgm.notebook.ui.taglist.TagListScreen
import com.amgm.notebook.ui.taglist.TagListViewModel
import com.amgm.notebook.ui.tagtable.TagTableScreen
import com.amgm.notebook.ui.tagtable.TagTableViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun NotebookNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Routes.NOTE_LIST,
        modifier = modifier.fillMaxSize(),
        // 画面遷移は fade + 小さなスライド。戻るときは逆向き
        enterTransition = {
            fadeIn(tween(220)) + slideInHorizontally(tween(260)) { fullWidth -> fullWidth / 12 }
        },
        exitTransition = {
            fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { fullWidth -> -fullWidth / 16 }
        },
        popEnterTransition = {
            fadeIn(tween(220)) + slideInHorizontally(tween(260)) { fullWidth -> -fullWidth / 16 }
        },
        popExitTransition = {
            fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { fullWidth -> fullWidth / 12 }
        }
    ) {
        composable(Routes.NOTE_LIST) {
            val vm: NoteListViewModel = viewModel(
                key = Routes.NOTE_LIST,
                factory = noteListViewModelFactory(container)
            )
            EventEffect(vm.events, navController, snackbarHostState, scope)
            NoteListScreen(vm, snackbarHostState)
        }
        composable(
            route = Routes.NOTE_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_NOTE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Routes.ARG_TAG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val noteId = entry.arguments?.getString(Routes.ARG_NOTE_ID)?.takeIf { it.isNotBlank() }?.let { NoteId(it) }
            val initialTagId = entry.arguments?.getString(Routes.ARG_TAG_ID)?.takeIf { it.isNotBlank() }?.let { TagId(it) }
            val vm: NoteEditViewModel = viewModel(
                key = "${Routes.NOTE_EDIT}/${noteId?.value}/${entry.id}",
                factory = noteEditViewModelFactory(container, noteId, initialTagId)
            )
            EventEffect(vm.events, navController, snackbarHostState, scope)
            NoteEditScreen(vm, snackbarHostState)
        }
        composable(Routes.TABLES) {
            val vm: TableIndexViewModel = viewModel(
                key = Routes.TABLES,
                factory = tableIndexViewModelFactory(container)
            )
            EventEffect(vm.events, navController, snackbarHostState, scope)
            TableIndexScreen(vm, snackbarHostState)
        }
        composable(Routes.TAG_LIST) {
            val vm: TagListViewModel = viewModel(
                key = Routes.TAG_LIST,
                factory = tagListViewModelFactory(container)
            )
            EventEffect(vm.events, navController, snackbarHostState, scope)
            TagListScreen(vm, snackbarHostState)
        }
        composable(
            route = Routes.TAG_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_TAG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val tagId = entry.arguments?.getString(Routes.ARG_TAG_ID)?.takeIf { it.isNotBlank() }?.let { TagId(it) }
            val vm: TagEditViewModel = viewModel(
                key = "${Routes.TAG_EDIT}/${tagId?.value}/${entry.id}",
                factory = tagEditViewModelFactory(container, tagId)
            )
            EventEffect(vm.events, navController, snackbarHostState, scope)
            TagEditScreen(vm, snackbarHostState)
        }
        composable(
            route = Routes.TAG_TABLE,
            arguments = listOf(
                navArgument(Routes.ARG_TAG_ID) { type = NavType.StringType }
            )
        ) { entry ->
            val tagId = entry.arguments?.getString(Routes.ARG_TAG_ID)?.takeIf { it.isNotBlank() }?.let { TagId(it) }
            if (tagId != null) {
                val vm: TagTableViewModel = viewModel(
                    key = "${Routes.TAG_TABLE}/${tagId.value}/${entry.id}",
                    factory = tagTableViewModelFactory(container, tagId)
                )
                EventEffect(vm.events, navController, snackbarHostState, scope)
                TagTableScreen(vm, snackbarHostState)
            }
        }
    }
}

/** ViewModel の UiEvent を NavController / Snackbar に反映する。 */
@Composable
private fun EventEffect(
    events: Flow<UiEvent>,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is UiEvent.Navigate -> navController.navigate(event.destination.route)
                is UiEvent.NavigateBack -> navController.popBackStack()
                is UiEvent.ShowMessage -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message.displayText())
                }
            }
        }
    }
}
