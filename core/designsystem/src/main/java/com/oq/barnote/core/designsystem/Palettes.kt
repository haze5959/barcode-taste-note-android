package com.oq.barnote.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.oq.barnote.core.oqcore.models.Palette

/**
 * BarNote 앱 전역 컬러 팔레트.
 * iOS `Palette.btnPalette` 에 대응.
 *
 * `colorResource` 는 `@Composable` 컨텍스트가 필요하므로 함수 형태로 제공합니다.
 * 다크 모드는 `values-night/colors.xml` 의 컬러가 자동으로 적용됩니다.
 */
@Composable
@ReadOnlyComposable
fun barNotePalette(): Palette = Palette(
    accent = colorResource(R.color.accent_color),
    accentSecondary = colorResource(R.color.accent_secondary),
    surfacePrimary = colorResource(R.color.surface_primary),
    surfaceSecondary = colorResource(R.color.surface_secondary),
    textPrimary = colorResource(R.color.text_primary),
    textSecondary = colorResource(R.color.text_secondary),
    divider = colorResource(R.color.divider),
    bgPrimary = colorResource(R.color.background_primary),
    disabledButton = colorResource(R.color.disabled_button),
    disabledText = colorResource(R.color.disabled_text),
)

/**
 * BarNote 앱 전역 Material3 [ColorScheme].
 *
 * Material 컴포넌트(AlertDialog / DropdownMenu / CircularProgressIndicator 등)는 색을 명시하지
 * 않으면 이 ColorScheme 을 따른다. Material 기본값(보라 primary + 라이트 surface)을 그대로 두면
 * 다크모드에서 다이얼로그/메뉴가 밝게 나오므로, 디자인 시스템 토큰(values/values-night 자동 전환)으로
 * 매핑한다. 다이얼로그/메뉴/바텀시트가 쓰는 `surfaceContainer*` 까지 surface 로 묶어 일관성 유지.
 */
@Composable
@ReadOnlyComposable
fun barNoteColorScheme(): ColorScheme {
    val accent = colorResource(R.color.accent_color)
    val accentContainer = colorResource(R.color.accent_secondary)
    val bg = colorResource(R.color.background_primary)
    val surface = colorResource(R.color.surface_primary)
    val surfaceVariant = colorResource(R.color.surface_secondary)
    val onSurface = colorResource(R.color.text_primary)
    val onSurfaceVariant = colorResource(R.color.text_secondary)
    val outline = colorResource(R.color.divider)
    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accentContainer,
        onPrimaryContainer = onSurface,
        secondary = accent,
        onSecondary = Color.White,
        tertiary = accent,
        onTertiary = Color.White,
        background = bg,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        surfaceTint = Color.Transparent,
        outline = outline,
        outlineVariant = outline,
    )
}
