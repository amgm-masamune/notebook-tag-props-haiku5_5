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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amgm.notebook.di.AppContainer
import com.amgm.notebook.ui.lighting.AmbientLightBackground
import com.amgm.notebook.ui.lighting.LocalLightState
import com.amgm.notebook.ui.lighting.rememberLightState
import com.amgm.notebook.ui.navigation.NotebookBottomBar
import com.amgm.notebook.ui.navigation.NotebookNavHost
import com.amgm.notebook.ui.navigation.isTopLevelRoute
import com.amgm.notebook.ui.theme.NotebookTheme

/** [darkThemeOverride] が null なら端末設定に従う（テストからダーク/ライトを強制するための口）。 */
@Composable
fun NotebookApp(container: AppContainer, darkThemeOverride: Boolean? = null) {
    val isDark = darkThemeOverride ?: isSystemInDarkTheme()
    NotebookTheme(darkTheme = isDark) {
        // 画面全体で光源は 1 つ。時間は画面の入口で 1 回だけ回し、配下は LocalLightState から読む
        val light = rememberLightState(isDark)
        CompositionLocalProvider(LocalLightState provides light) {
            // 背景は AmbientLightBackground が描くので、ここは透明にして文字色だけ与える
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                AmbientLightBackground(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val showBottomBar = isTopLevelRoute(currentRoute)

                    // 各画面が自前の Scaffold を持つため、外側はボトムバー分の下余白だけを渡し、インセットは二重に適用しない
                    Scaffold(
                        containerColor = Color.Transparent,
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
    }
}
