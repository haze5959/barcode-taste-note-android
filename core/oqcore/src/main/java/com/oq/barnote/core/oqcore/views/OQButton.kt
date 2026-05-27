package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.oqcore.models.Palette

@Composable
fun OQButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Palette().accent,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, color),
        contentPadding = PaddingValues(15.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color
        )
    ) {
        Text(text)
    }
}

@Composable
fun OQFillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: Palette = Palette(),
    radius: Float = 0f,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(radius.dp),
        contentPadding = PaddingValues(15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.accent,
            contentColor = palette.accentSecondary,
            disabledContainerColor = palette.disabledButton,
            disabledContentColor = palette.disabledText
        )
    ) {
        Text(text)
    }
}

enum class OQRoundedButtonStyleType {
    Accent, TextPrimary, TextSecondary
}

@Composable
fun OQRoundedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OQRoundedButtonStyleType = OQRoundedButtonStyleType.TextPrimary,
    palette: Palette = Palette(),
    radius: Float = 0f,
    cornerWidth: Float = 1f,
    enabled: Boolean = true
) {
    val baseColor = when (style) {
        OQRoundedButtonStyleType.Accent -> palette.accent
        OQRoundedButtonStyleType.TextPrimary -> palette.textPrimary
        OQRoundedButtonStyleType.TextSecondary -> palette.textSecondary
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(radius.dp),
        border = BorderStroke(cornerWidth.dp, if (enabled) baseColor else palette.disabledButton),
        contentPadding = PaddingValues(15.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (enabled) baseColor else palette.disabledText
        )
    ) {
        Text(text)
    }
}

@Composable
fun OQCapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAccent: Boolean = true,
    palette: Palette = Palette(),
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(50), // Capsule
        border = if (!isAccent) BorderStroke(1.dp, palette.divider) else null,
        contentPadding = PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isAccent) palette.accent else palette.surfacePrimary,
            contentColor = if (isAccent) Color.White else palette.textSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isAccent) 6.dp else 2.dp
        )
    ) {
        Text(text)
    }
}
