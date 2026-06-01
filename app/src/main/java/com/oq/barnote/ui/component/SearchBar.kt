package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R

/**
 * 단순 검색 바. 좌측 magnifyingglass 아이콘 + TextField + 우측 clear 버튼.
 * iOS `SearchView` 내의 검색바 영역에 대응.
 */
@Composable
fun SearchBar(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val surfaceSecondary = colorResource(R.color.surface_secondary)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfaceSecondary)
            .padding(horizontal = Dimens.Padding, vertical = Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(Dimens.MiniIconSize),
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = textPrimary),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                    )
                }
                innerTextField()
            },
        )

        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier
                    .size(Dimens.MiniIconSize)
                    .clickable { onValueChange("") },
            )
        }
    }
}
