package com.amgm.notebook.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.lighting.glassPanel
import com.amgm.notebook.ui.lighting.softShadow

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

/** 浮遊するボトムバーの角丸・影の強さ・画面端からの余白。 */
private val BarRadius: Dp = 28.dp
private val BarElevation: Dp = 12.dp
private val BarMargin: Dp = 12.dp

/**
 * 現在のルートに対応するタブを選択状態で描画する。切替時は各タブの状態を保存・復元する。
 * 左右と下に余白を取った浮遊型のガラス板の上に載せる。選択インジケータは主色の淡い pill。
 */
@Composable
fun NotebookBottomBar(currentRoute: String?, navController: NavHostController) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = BarMargin, end = BarMargin, bottom = BarMargin)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(cornerRadius = BarRadius, elevation = BarElevation)
                .glassPanel(cornerRadius = BarRadius)
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
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
}
