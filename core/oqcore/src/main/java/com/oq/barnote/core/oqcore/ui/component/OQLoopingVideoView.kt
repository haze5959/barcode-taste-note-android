package com.oq.barnote.core.oqcore.ui.component

import android.net.Uri
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * `res/raw` 의 영상을 컨트롤 없이 **음소거 + 무한 루프**로 재생하는 뷰. iOS `OQLoopingVideoView` 대응.
 *
 * media3 **ExoPlayer + [TextureView]** ([Player.setVideoTextureView]) 구성:
 * - ExoPlayer 가 디코더 출력(컬러 포맷/스트라이드/비표준 해상도)을 안정적으로 surface 에 렌더한다.
 *   `MediaPlayer` + raw `Surface` 직접 렌더로 했을 때 발생하던 화면 깨짐(garbled)을 방지하기 위함.
 * - SurfaceView 가 아닌 TextureView 라, Compose 의 `clip(RoundedCornerShape)` / `Dialog` 안에서도
 *   정상적으로 합성/클리핑된다 (온보딩이 Dialog 안이라 중요).
 * - 노출 노드 크기에 맞춰 렌더되므로, 호출부에서 영상 원본 비율로 [Modifier.aspectRatio] 를 잡으면
 *   왜곡 없이 표시된다.
 *
 * @param rawResId `R.raw.*` 영상 리소스 id.
 */
@Composable
fun OQLoopingVideoView(
    @RawRes rawResId: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // recomposition 사이 유지 + 화면 이탈 시 release.
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(
                MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$rawResId")),
            )
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> TextureView(ctx).also { player.setVideoTextureView(it) } },
    )
}
