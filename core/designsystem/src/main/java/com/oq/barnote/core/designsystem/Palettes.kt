package com.oq.barnote.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
