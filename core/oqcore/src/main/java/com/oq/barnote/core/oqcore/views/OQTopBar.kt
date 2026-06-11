package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.oqcore.models.Palette

/** [OQTopBar] 의 좌측 네비게이션 아이콘 종류. */
enum class OQTopBarNav { Back, Close, None }

/**
 * 가장 흔한 상단 바 패턴 — 좌측 네비 아이콘(뒤로/닫기) + 가운데 정렬 타이틀 + 우측 슬롯.
 *
 * BarNote 의 여러 화면(Settings / ReservationSettings / UserNoteList / UserDetail / Subscription 및
 * AddNote / EditNote / AddProduct 닫기 헤더)이 거의 동일한 Row(아이콘 + weight Spacer + Text +
 * weight Spacer + 대칭 Spacer)를 중복 정의하던 것을 통합. 도메인 무관이라 oqcore 에 둡니다.
 *
 * 색상은 [Palette] 로 주입 (oqcore 가 designsystem 에 의존하지 않으므로). 앱은 `barNotePalette()` 전달.
 *
 * @param trailing 우측 영역. null 이면 타이틀이 가운데 오도록 좌측 아이콘과 동일 폭의 Spacer 를 둡니다.
 *                 아이콘 버튼 등을 넣으려면 RowScope 슬롯에 직접 구성하세요.
 */
@Composable
fun OQTopBar(
    title: String,
    onNavClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: Palette = Palette(),
    navIcon: OQTopBarNav = OQTopBarNav.Back,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val navVector = when (navIcon) {
            OQTopBarNav.Back -> Icons.AutoMirrored.Filled.ArrowBack
            OQTopBarNav.Close -> Icons.Filled.Close
            OQTopBarNav.None -> null
        }
        if (navVector != null) {
            Icon(
                imageVector = navVector,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier
                    .size(NAV_TOUCH_SIZE)
                    .clip(CircleShape)
                    .clickable(onClick = onNavClick)
                    .padding(12.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(NAV_TOUCH_SIZE))
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.weight(1f))

        if (trailing != null) {
            trailing()
        } else {
            // 타이틀을 정확히 가운데 두기 위해 좌측 아이콘과 동일 폭의 대칭 Spacer.
            Spacer(modifier = Modifier.size(NAV_TOUCH_SIZE))
        }
    }
}

// 최소 터치 타깃 48dp(Material). 아이콘 시각 크기는 내부 padding(12dp)으로 24dp.
// oqcore 는 designsystem 에 의존하지 않아 리터럴 사용.
private val NAV_TOUCH_SIZE = 48.dp
