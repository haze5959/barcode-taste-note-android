package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.oqcore.views.SkeletonView

/**
 * 검색 자동완성 패널. 검색바 바로 아래에 overlay 로 띄움.
 * iOS `SearchView.autocompletePanel` 에 대응.
 *
 * @param query 강조 표시할 매칭 쿼리.
 * @param suggestions 자동완성 후보. null/empty 면 미표시.
 * @param isLoading true 면 4행 SkeletonView 표시.
 */
@Composable
fun AutocompletePanel(
    query: String,
    suggestions: List<String>?,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val divider = colorResource(R.color.divider)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Dimens.Radius, shape = RoundedCornerShape(Dimens.Radius))
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .border(
                width = 0.5.dp,
                color = divider.copy(alpha = 0.4f),
                shape = RoundedCornerShape(Dimens.Radius),
            ),
    ) {
        if (isLoading) {
            repeat(4) { SkeletonRow() }
        } else if (!suggestions.isNullOrEmpty()) {
            suggestions.forEachIndexed { index, suggestion ->
                SuggestionRow(
                    text = suggestion,
                    query = query,
                    accent = accent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { onSelect(suggestion) },
                )
                if (index < suggestions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = Dimens.Spacing),
                        color = divider.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Spacing, vertical = Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        SkeletonView(
            modifier = Modifier.size(Dimens.MiniIconSize),
            cornerRadius = 4.dp,
        )
        SkeletonView(
            modifier = Modifier
                .weight(1f)
                .height(14.dp),
            cornerRadius = 4.dp,
        )
    }
}

@Composable
private fun SuggestionRow(
    text: String,
    query: String,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Spacing, vertical = Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(Dimens.MiniIconSize),
        )
        Text(
            text = highlightMatch(text = text, query = query, accent = accent),
            style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.CallMade,
            contentDescription = null,
            tint = textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(Dimens.MiniIconSize - 4.dp),
        )
    }
}

/** 쿼리와 매칭되는 부분만 accent + bold 로 강조한 AnnotatedString 반환. */
private fun highlightMatch(text: String, query: String, accent: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val matchStart = text.indexOf(query, ignoreCase = true)
    if (matchStart < 0) return AnnotatedString(text)
    val matchEnd = matchStart + query.length

    return buildAnnotatedString {
        append(text.substring(0, matchStart))
        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
            append(text.substring(matchStart, matchEnd))
        }
        append(text.substring(matchEnd))
    }
}
