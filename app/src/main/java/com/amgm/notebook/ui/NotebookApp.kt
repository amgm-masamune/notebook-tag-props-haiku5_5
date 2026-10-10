package com.amgm.notebook.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amgm.notebook.di.AppContainer
import com.amgm.notebook.ui.navigation.NotebookBottomBar
import com.amgm.notebook.ui.navigation.NotebookNavHost
import com.amgm.notebook.ui.navigation.isTopLevelRoute
import com.amgm.notebook.ui.theme.NotebookTheme

/** [darkThemeOverride] が null なら端末設定に従う（テストからダーク/ライトを強制するための口）。 */
@Composable
fun NotebookApp(container: AppContainer, darkThemeOverride: Boolean? = null) {
    NotebookTheme(darkTheme = darkThemeOverride ?: isSystemInDarkTheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            val showBottomBar = isTopLevelRoute(currentRoute)

            // 各画面が自前の Scaffold を持つため、外側はボトムバー分の下余白だけを渡し、インセットは二重に適用しない
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomBar) NotebookBottomBar(currentRoute, navController)
                }
            ) { innerPadding ->
                val bottom = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                NotebookNavHost(
                    container = container,
                    navController = navController,
                    modifier = Modifier
                        .padding(bottom)
                        .consumeWindowInsets(bottom)
                )
            }
        }
    }
}
