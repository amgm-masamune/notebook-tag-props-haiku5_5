package com.amgm.notebook.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.amgm.notebook.ui.TestTags

/** トップレベルの 3 タブ。 */
enum class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    Notes(Routes.NOTE_LIST, "ノート", Icons.Default.Edit, TestTags.BOTTOM_NAV_NOTES),
    Tags(Routes.TAG_LIST, "タグ", Icons.Default.Star, TestTags.BOTTOM_NAV_TAGS),
    Tables(Routes.TABLES, "表", Icons.Default.Menu, TestTags.BOTTOM_NAV_TABLES)
}

/** ボトムバーを表示するルートか（編集画面・表ビュー等では非表示）。 */
fun isTopLevelRoute(route: String?): Boolean = route != null && route in Routes.TOP_LEVEL

/**
 * 現在のルートに対応するタブを選択状態で描画する。切替時は各タブの状態を保存・復元する。
 * 背景はサーフェス、選択インジケータは主色の淡い pill、上端に細い罫線。
 */
@Composable
fun NotebookBottomBar(currentRoute: String?, navController: NavHostController) {
    val scheme = MaterialTheme.colorScheme
    Column {
        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
        NavigationBar(
            containerColor = scheme.surface,
            contentColor = scheme.onSurfaceVariant,
            tonalElevation = 0.dp,
            modifier = Modifier.testTag(TestTags.BOTTOM_NAV)
        ) {
            TopLevelTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = {
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = scheme.primary,
                        selectedTextColor = scheme.primary,
                        indicatorColor = scheme.primaryContainer,
                        unselectedIconColor = scheme.onSurfaceVariant,
                        unselectedTextColor = scheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag(tab.testTag)
                )
            }
        }
    }
}
