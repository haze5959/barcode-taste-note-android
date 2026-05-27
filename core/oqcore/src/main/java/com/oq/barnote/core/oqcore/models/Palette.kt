package com.oq.barnote.core.oqcore.models

import androidx.compose.ui.graphics.Color

data class Palette(
    val accent: Color = Color(0xFF6200EE), // Default Android accent-like
    val accentSecondary: Color = Color(0xFF03DAC6),
    val surfacePrimary: Color = Color.White,
    val surfaceSecondary: Color = Color.White,
    val textPrimary: Color = Color.Black,
    val textSecondary: Color = Color.Gray,
    val divider: Color = Color.Gray,
    val bgPrimary: Color = Color.White,
    val disabledButton: Color = Color.Gray.copy(alpha = 0.2f),
    val disabledText: Color = Color.Gray.copy(alpha = 0.6f)
)
