package com.oq.barnote.core.oqcore.ui.component

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.oq.barnote.core.oqcore.utils.OQLog

/**
 * `res/raw` 의 영상을 컨트롤 없이 **음소거 + 무한 루프**로 재생하는 뷰. iOS `OQLoopingVideoView` 대응.
 *
 * - 온보딩처럼 사용자 조작이 필요 없는 짧은 루프 영상용.
 * - SurfaceView 기반 [android.widget.VideoView] 대신 [TextureView] 를 쓴다 — SurfaceView 는 별도
 *   surface 라 Compose 의 `clip(RoundedCornerShape)` / `Dialog` 안에서 라운드 코너·합성이 깨지는 반면,
 *   TextureView 는 일반 뷰처럼 뷰 계층에 렌더돼 정상 클리핑된다. (온보딩은 Dialog 안이라 특히 중요)
 * - 노출 노드 크기에 맞춰 영상이 채워지므로, 호출부에서 원본 비율로 [Modifier.aspectRatio] 를 잡으면
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
    val player = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose { runCatching { player.release() } }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                        runCatching {
                            player.reset()
                            player.setDataSource(
                                ctx,
                                Uri.parse("android.resource://${ctx.packageName}/$rawResId"),
                            )
                            player.setSurface(Surface(surface))
                            player.isLooping = true
                            player.setVolume(0f, 0f)
                            player.setOnPreparedListener { it.start() }
                            player.prepareAsync()
                        }.onFailure { OQLog.w("OQLoopingVideoView 재생 실패: ${it.message}") }
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) = Unit

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
            }
        },
    )
}
