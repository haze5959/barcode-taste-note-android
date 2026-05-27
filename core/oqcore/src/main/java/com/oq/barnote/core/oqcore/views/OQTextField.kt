package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.oqcore.models.Palette

@Composable
fun OQTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    palette: Palette = Palette(),
    radius: Float = 16f,
    minLines: Int = 1
) {
    val isFocusedOrFilled = value.isNotEmpty()
    val borderColor = if (isFocusedOrFilled) palette.accent.copy(alpha = 0.4f) else palette.divider
    val borderWidth = if (isFocusedOrFilled) 1.5.dp else 1.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (minLines > 1) 160.dp else 48.dp)
            .clip(RoundedCornerShape(radius.dp))
            .background(palette.bgPrimary)
            .border(borderWidth, borderColor, RoundedCornerShape(radius.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        textStyle = androidx.compose.ui.text.TextStyle(color = palette.textPrimary),
        cursorBrush = SolidColor(palette.accent),
        minLines = minLines
    )
}
