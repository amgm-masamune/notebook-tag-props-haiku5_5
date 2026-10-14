package com.amgm.notebook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Material の ColorScheme に収まらないアプリ固有の色。 */
@Immutable
class NotebookExtraColors(
    /** アクセント（テアル）。小さな強調・件数に使う。 */
    val accent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    /** 表の交互行。 */
    val rowAlt: Color,
    val isDark: Boolean
)

private val LightExtra = NotebookExtraColors(
    accent = TealLight,
    accentContainer = TealSoftLight,
    onAccentContainer = OnTealSoftLight,
    rowAlt = RowAltLight,
    isDark = false
)

private val DarkExtra = NotebookExtraColors(
    accent = TealDark,
    accentContainer = TealSoftDark,
    onAccentContainer = OnTealSoftDark,
    rowAlt = RowAltDark,
    isDark = true
)

internal val LocalNotebookExtraColors = staticCompositionLocalOf { LightExtra }

/** `MaterialTheme.extraColors` でアプリ固有色を参照する。 */
val MaterialTheme.extraColors: NotebookExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNotebookExtraColors.current

private val LightColors = lightColorScheme(
    primary = InkBlueLight,
    onPrimary = OnInkBlueLight,
    primaryContainer = InkBlueSoftLight,
    onPrimaryContainer = OnInkBlueSoftLight,
    inversePrimary = InkBlueDark,
    secondary = TealLight,
    onSecondary = Color.White,
    secondaryContainer = TealSoftLight,
    onSecondaryContainer = OnTealSoftLight,
    tertiary = AmberLight,
    onTertiary = Color.White,
    tertiaryContainer = AmberSoftLight,
    onTertiaryContainer = OnAmberSoftLight,
    background = PaperLight,
    onBackground = TextLight,
    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = SurfaceDimLight,
    onSurfaceVariant = TextSubLight,
    surfaceTint = InkBlueLight,
    inverseSurface = TextLight,
    inverseOnSurface = PaperLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorSoftLight,
    onErrorContainer = OnErrorSoftLight,
    outline = OutlineLight,
    outlineVariant = HairlineLight,
    scrim = Color.Black,
    surfaceBright = SurfaceLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceLight,
    surfaceContainerHighest = SurfaceDimLight
)

private val DarkColors = darkColorScheme(
    primary = InkBlueDark,
    onPrimary = OnInkBlueDark,
    primaryContainer = InkBlueSoftDark,
    onPrimaryContainer = OnInkBlueSoftDark,
    inversePrimary = InkBlueLight,
    secondary = TealDark,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = TealSoftDark,
    onSecondaryContainer = OnTealSoftDark,
    tertiary = AmberDark,
    onTertiary = Color(0xFF3D2600),
    tertiaryContainer = AmberSoftDark,
    onTertiaryContainer = OnAmberSoftDark,
    background = PaperDark,
    onBackground = TextDark,
    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = SurfaceDimDark,
    onSurfaceVariant = TextSubDark,
    surfaceTint = InkBlueDark,
    inverseSurface = TextDark,
    inverseOnSurface = PaperDark,
    error = ErrorDark,
    onError = Color(0xFF4A0E05),
    errorContainer = ErrorSoftDark,
    onErrorContainer = OnErrorSoftDark,
    outline = OutlineDark,
    outlineVariant = HairlineDark,
    scrim = Color.Black,
    surfaceBright = SurfaceDimDark,
    surfaceDim = PaperDark,
    surfaceContainerLowest = PaperDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDimDark,
    surfaceContainerHighest = SurfaceDimDark
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(NotebookDimens.InputRadius),
    medium = RoundedCornerShape(NotebookDimens.InputRadius),
    large = RoundedCornerShape(NotebookDimens.CardRadius),
    extraLarge = RoundedCornerShape(28.dp)
)

/** ダイナミックカラーは使わず、常にブランドの固定配色を使う。ダークは端末設定に追従。 */
@Composable
fun NotebookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalNotebookExtraColors provides if (darkTheme) DarkExtra else LightExtra
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
