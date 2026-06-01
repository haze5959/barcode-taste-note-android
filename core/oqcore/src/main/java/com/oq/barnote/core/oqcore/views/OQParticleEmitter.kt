package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * iOS `OQParticleEmitter.burstAtBottom()` 대응의 Compose 포팅.
 *
 * - CAEmitterLayer 대신 Canvas + frame loop 으로 파티클 위치/투명도 직접 계산
 * - 별/하트/원형 등 SF Symbol 대신 단순 원형 파티클로 그림 (성능 + 의존성 0)
 * - [trigger] Flow 가 emit 될 때마다 한 번씩 파티클 버스트 실행
 *
 * 사용 예 (앱 글로벌 오버레이):
 * ```
 * OQParticleEmitterHost(trigger = appController.particleBurstEvent)
 * ```
 */
@Composable
fun OQParticleEmitterHost(
    trigger: Flow<Unit>,
    modifier: Modifier = Modifier,
    heightFromBottomPx: Float = 360f,
) {
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(trigger) {
        trigger.collectLatest {
            // 사이즈가 아직 측정되기 전이면 무시 (다음 트리거에서 다시 시도).
            if (canvasSize.width <= 0 || canvasSize.height <= 0) return@collectLatest
            val origin = Offset(
                x = canvasSize.width / 2f,
                y = canvasSize.height - heightFromBottomPx,
            )
            particles = particles + spawnBurst(origin = origin)
        }
    }

    // 프레임 루프 — 파티클이 있을 때만 동작. 파티클이 비면 다음 burst 까지 idle.
    LaunchedEffect(particles.isNotEmpty()) {
        if (particles.isEmpty()) return@LaunchedEffect
        var lastTime = 0L
        while (particles.isNotEmpty()) {
            withFrameNanos { now ->
                val dt = if (lastTime == 0L) 0f else (now - lastTime) / 1_000_000_000f
                lastTime = now
                particles = particles.mapNotNull { it.advance(dt) }
            }
        }
    }

    // 입력 modifier 가 없는 Box / Canvas 는 Compose 의 hit-test 에 등록되지 않아 자동으로 pass-through.
    // 따라서 파티클이 떠 있어도 그 아래 UI 의 터치/스크롤 이벤트는 그대로 전달됩니다.
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it },
    ) {
        if (particles.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = p.radius,
                        center = Offset(p.x, p.y),
                    )
                }
            }
        }
    }
}

/** 단일 파티클 상태 (immutable; advance() 가 새 인스턴스 반환). */
private data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color,
    val lifetime: Float, // 남은 생명 (초)
    val initialLifetime: Float,
) {
    val alpha: Float get() = (lifetime / initialLifetime).coerceIn(0f, 1f)

    fun advance(dt: Float): Particle? {
        val nextLifetime = lifetime - dt
        if (nextLifetime <= 0f) return null
        // y 가속도 (중력).
        val gravity = 1400f
        return copy(
            x = x + vx * dt,
            y = y + vy * dt,
            vy = vy + gravity * dt,
            lifetime = nextLifetime,
        )
    }
}

private val DefaultColors = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFF40D9FF), // Sky Blue
    Color(0xFFFF5959), // Red
    Color(0xFF59FF8C), // Green
    Color(0xFFFF8E1A), // Orange
    Color(0xFFD980FF), // Purple
)

private fun spawnBurst(
    origin: Offset,
    count: Int = 50,
    colors: List<Color> = DefaultColors,
): List<Particle> {
    val rng = Random(System.nanoTime())
    return List(count) {
        // 위쪽 방향 (-PI/2) ± 60도 fan.
        val angle = -PI.toFloat() / 2f + (rng.nextFloat() - 0.5f) * (PI.toFloat() / 1.5f)
        val speed = 700f + rng.nextFloat() * 300f
        val lifetime = 1.0f + rng.nextFloat() * 0.7f
        Particle(
            x = origin.x,
            y = origin.y,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            radius = 6f + rng.nextFloat() * 4f,
            color = colors[rng.nextInt(colors.size)],
            lifetime = lifetime,
            initialLifetime = lifetime,
        )
    }
}
