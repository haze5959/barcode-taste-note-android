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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * iOS `OQParticleEmitter.burstAtBottom()` (CAEmitterLayer) 대응의 Compose 포팅.
 *
 * CAEmitterLayer 대신 Canvas + frame loop 으로 파티클을 직접 시뮬레이션합니다.
 * iOS 셀 파라미터에 맞춰:
 * - 모양: 별(5각)/스파클(4각) Path — iOS SF Symbol(star/sparkle 등) 근사 (단색 원형 대비 입체감)
 * - 회전: [Particle.angularVelocity] (iOS `cell.spin`)
 * - 소멸하며 축소: alpha 와 함께 크기 축소 (iOS `cell.scaleSpeed = -0.15`)
 * - 물리: yAcceleration 500 (iOS `cell.yAcceleration = 500`), 위쪽 ±60° fan (iOS `emissionRange = .pi/1.5`)
 * - [trigger] Flow 가 emit 될 때마다 한 번씩 버스트
 *
 * 사용 예: `OQParticleEmitterHost(trigger = appController.particleBurstEvent)`
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

    // 입력 modifier 가 없는 Box / Canvas 는 hit-test 에 등록되지 않아 자동 pass-through —
    // 파티클이 떠 있어도 아래 UI 의 터치/스크롤 이벤트는 그대로 전달됩니다.
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it },
    ) {
        if (particles.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    // 소멸하며 축소 (iOS scaleSpeed -0.15 근사): alpha 가 줄면 크기도 함께 축소.
                    val outer = p.radius * (0.55f + 0.45f * p.alpha)
                    drawPath(
                        path = starPath(
                            cx = p.x,
                            cy = p.y,
                            outerRadius = outer,
                            innerRadius = outer * 0.45f,
                            points = p.points,
                            rotationRad = p.rotation,
                        ),
                        color = p.color.copy(alpha = p.alpha),
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
    val points: Int,            // 별 꼭짓점 수 (4=스파클 / 5=별)
    val rotation: Float,        // 현재 회전각 (rad)
    val angularVelocity: Float, // 회전 속도 (rad/s) — iOS cell.spin
    val lifetime: Float,        // 남은 생명 (초)
    val initialLifetime: Float,
) {
    val alpha: Float get() = (lifetime / initialLifetime).coerceIn(0f, 1f)

    fun advance(dt: Float): Particle? {
        val nextLifetime = lifetime - dt
        if (nextLifetime <= 0f) return null
        val gravity = 500f // iOS cell.yAcceleration = 500
        return copy(
            x = x + vx * dt,
            y = y + vy * dt,
            vy = vy + gravity * dt,
            rotation = rotation + angularVelocity * dt,
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
        // 위쪽 방향 (-PI/2) ± 60도 fan (iOS emissionRange = .pi / 1.5).
        val angle = -PI.toFloat() / 2f + (rng.nextFloat() - 0.5f) * (PI.toFloat() / 1.5f)
        val speed = 650f + rng.nextFloat() * 500f // iOS velocity 900 ± 250
        val lifetime = 0.9f + rng.nextFloat() * 0.8f
        Particle(
            x = origin.x,
            y = origin.y,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            radius = 10f + rng.nextFloat() * 7f, // iOS pointSize 32 * scale ~0.5 근사
            color = colors[rng.nextInt(colors.size)],
            points = if (rng.nextBoolean()) 4 else 5,
            rotation = rng.nextFloat() * (2f * PI.toFloat()),
            angularVelocity = (rng.nextFloat() - 0.5f) * 12f, // iOS spin 5 ± spinRange
            lifetime = lifetime,
            initialLifetime = lifetime,
        )
    }
}

/**
 * 중심 (cx,cy) 기준 [points] 꼭짓점 별 모양 Path. 외곽/내곽 반지름을 교대로 잇고 [rotationRad] 만큼 회전.
 * points=5 → 별, points=4 → 스파클.
 */
private fun starPath(
    cx: Float,
    cy: Float,
    outerRadius: Float,
    innerRadius: Float,
    points: Int,
    rotationRad: Float,
): Path {
    val path = Path()
    val total = points * 2
    val step = PI.toFloat() / points
    for (i in 0 until total) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        // 시작점은 위(-PI/2) 방향, rotation 적용.
        val a = rotationRad - PI.toFloat() / 2f + i * step
        val px = cx + cos(a) * r
        val py = cy + sin(a) * r
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}
