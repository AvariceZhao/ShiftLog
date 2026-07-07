package com.clockin.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.AmberSoft
import com.clockin.app.ui.theme.CyanSecondary
import com.clockin.app.ui.theme.CyanSoft
import com.clockin.app.ui.theme.StatusNormal
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val DURATION_MS = 3000L
private const val LIFE_SCALE = 0.58f

private enum class BurstStyle {
    PEONY,
    CHRYSANTHEMUM,
    RING,
    WILLOW,
    PALM,
    CRACKLE,
}

private data class Spark(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val lifeMs: Float,
    val gravity: Float,
    val drag: Float,
    val trail: Float,
    val glitter: Boolean,
    val delayMs: Float = 0f,
    val bloom: Boolean = false,
)

private data class Burst(
    val startMs: Long,
    val xRatio: Float,
    val yRatio: Float,
    val palette: List<Color>,
    val sparks: List<Spark>,
    val flashScale: Float,
    val style: BurstStyle,
    val hasRocket: Boolean,
)

@Composable
fun FireworksOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember {
        listOf(
            AmberPrimary,
            AmberSoft,
            CyanSecondary,
            CyanSoft,
            StatusNormal,
            Color(0xFFFF6B9D),
            Color(0xFFB388FF),
            Color(0xFFFFE066),
            Color(0xFFFF8C42),
            Color(0xFF7FE7FF),
            Color(0xFFFF4D6D),
            Color(0xFFE0B0FF),
            Color(0xFFFF2E63),
            Color(0xFF00E5FF),
            Color(0xFFFFD93D),
            Color(0xFF9B5DE5),
            Color(0xFFF15BB5),
            Color(0xFF00F5D4),
        )
    }
    val bursts = remember(palette) { buildBursts(palette) }

    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            elapsedMs = (frame - start) / 1_000_000
            if (elapsedMs >= DURATION_MS) {
                onFinished()
                break
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val fadeOut = ((DURATION_MS - elapsedMs).toFloat() / 350f).coerceIn(0f, 1f)

        for (burst in bursts) {
            if (elapsedMs < burst.startMs - if (burst.hasRocket) 380L else 0L) continue
            val age = (elapsedMs - burst.startMs).toFloat()
            val center = Offset(width * burst.xRatio, height * burst.yRatio)
            if (burst.hasRocket && age < 0f) {
                drawRocket(burst, age, center, height, fadeOut)
            }
            if (age >= 0f) {
                drawBurst(burst, age, center, fadeOut)
            }
        }
    }
}

private fun buildBursts(palette: List<Color>): List<Burst> {
    data class BurstConfig(
        val x: Float,
        val y: Float,
        val scale: Float,
        val style: BurstStyle,
    )

    // 前三波逐个错开（偶尔最多 2 发同刻），第四波压轴齐放
    val timed = listOf(
        // 第一波
        0L to BurstConfig(0.22f, 0.26f, 1.15f, BurstStyle.PEONY),
        240L to BurstConfig(0.78f, 0.20f, 1.10f, BurstStyle.CHRYSANTHEMUM),
        480L to BurstConfig(0.48f, 0.34f, 1.20f, BurstStyle.PALM),
        // 第二波
        720L to BurstConfig(0.18f, 0.38f, 1.05f, BurstStyle.RING),
        960L to BurstConfig(0.82f, 0.28f, 1.10f, BurstStyle.WILLOW),
        1200L to BurstConfig(0.52f, 0.18f, 1.12f, BurstStyle.CRACKLE),
        // 第三波（此处 2 发同刻，其余分开）
        1440L to BurstConfig(0.30f, 0.22f, 1.18f, BurstStyle.PEONY),
        1680L to BurstConfig(0.68f, 0.40f, 1.08f, BurstStyle.CHRYSANTHEMUM),
        1680L to BurstConfig(0.36f, 0.44f, 1.0f, BurstStyle.CRACKLE),
        1920L to BurstConfig(0.58f, 0.16f, 1.15f, BurstStyle.PALM),
        // 第四波压轴齐放
        2280L to BurstConfig(0.50f, 0.24f, 1.70f, BurstStyle.PEONY),
        2280L to BurstConfig(0.26f, 0.20f, 1.40f, BurstStyle.CHRYSANTHEMUM),
        2280L to BurstConfig(0.74f, 0.30f, 1.40f, BurstStyle.PALM),
        2280L to BurstConfig(0.40f, 0.46f, 1.20f, BurstStyle.WILLOW),
        2280L to BurstConfig(0.60f, 0.14f, 1.20f, BurstStyle.RING),
        2280L to BurstConfig(0.16f, 0.32f, 1.10f, BurstStyle.CRACKLE),
        2280L to BurstConfig(0.84f, 0.38f, 1.10f, BurstStyle.CRACKLE),
        2280L to BurstConfig(0.50f, 0.36f, 1.50f, BurstStyle.CHRYSANTHEMUM),
    )

    return timed.mapIndexed { index, (startMs, config) ->
        val rng = Random(31 + index * 113)
        val burstPalette = List(7) { palette[rng.nextInt(palette.size)] }
        Burst(
            startMs = startMs,
            xRatio = config.x,
            yRatio = config.y,
            palette = burstPalette,
            sparks = createSparks(rng, burstPalette, config.scale, config.style),
            flashScale = config.scale,
            style = config.style,
            hasRocket = false,
        )
    }
}

private fun createSparks(
    rng: Random,
    palette: List<Color>,
    scale: Float,
    style: BurstStyle,
): List<Spark> {
    val sparks = ArrayList<Spark>(700)

    fun add(
        count: Int,
        speedMin: Float,
        speedMax: Float,
        sizeMin: Float,
        sizeMax: Float,
        lifeMin: Float,
        lifeMax: Float,
        trail: Float,
        gravityMin: Float = 0.00045f,
        gravityMax: Float = 0.00095f,
        dragMin: Float = 0.0001f,
        dragMax: Float = 0.00018f,
        glitter: Boolean = false,
        bloom: Boolean = false,
        delayMs: Float = 0f,
        spreadJitter: Float = 0.32f,
        fullRandomAngle: Boolean = false,
    ) {
        for (i in 0 until count) {
            val angle = if (fullRandomAngle) {
                rng.nextFloat() * (Math.PI * 2).toFloat()
            } else {
                (i.toFloat() / count) * (Math.PI * 2).toFloat() +
                    (rng.nextFloat() - 0.5f) * spreadJitter
            }
            sparks += Spark(
                angle = angle,
                speed = speedMin + rng.nextFloat() * (speedMax - speedMin),
                size = sizeMin + rng.nextFloat() * (sizeMax - sizeMin),
                color = palette[rng.nextInt(palette.size)],
                lifeMs = (lifeMin + rng.nextFloat() * (lifeMax - lifeMin)) * LIFE_SCALE,
                gravity = gravityMin + rng.nextFloat() * (gravityMax - gravityMin),
                drag = dragMin + rng.nextFloat() * (dragMax - dragMin),
                trail = trail,
                glitter = glitter,
                delayMs = delayMs * LIFE_SCALE,
                bloom = bloom,
            )
        }
    }

    val s = scale
    when (style) {
        BurstStyle.PEONY -> {
            add(140, 2.4f * s, 5.2f * s, 3.2f, 6.2f, 1000f, 1600f, 0.62f, bloom = true)
            add(90, 3.8f * s, 6.8f * s, 2.4f, 4.4f, 850f, 1300f, 0.75f)
            add(56, 5.0f * s, 7.4f * s, 2.0f, 3.4f, 700f, 1050f, 0.8f, spreadJitter = 0.06f)
            add(70, 1.4f * s, 3.6f * s, 1.4f, 2.8f, 1200f, 1800f, 0.25f, glitter = true, fullRandomAngle = true)
            add(48, 1.0f * s, 2.4f * s, 1.6f, 3.0f, 1400f, 2000f, 0.95f, gravityMin = 0.0007f, gravityMax = 0.0012f)
            // Secondary crackle wave
            add(80, 1.6f * s, 4.2f * s, 1.2f, 2.6f, 700f, 1100f, 0.35f, glitter = true, delayMs = 420f, fullRandomAngle = true)
        }
        BurstStyle.CHRYSANTHEMUM -> {
            add(160, 2.8f * s, 6.4f * s, 2.6f, 5.0f, 950f, 1500f, 0.7f, bloom = true)
            add(72, 4.6f * s, 7.2f * s, 2.0f, 3.6f, 750f, 1150f, 0.82f, spreadJitter = 0.05f)
            add(64, 2.0f * s, 4.0f * s, 1.8f, 3.2f, 1100f, 1700f, 0.9f)
            add(90, 1.2f * s, 3.5f * s, 1.1f, 2.2f, 1100f, 1750f, 0.2f, glitter = true, fullRandomAngle = true)
            add(60, 1.5f * s, 3.8f * s, 1.3f, 2.4f, 650f, 1000f, 0.4f, delayMs = 380f, glitter = true, fullRandomAngle = true)
        }
        BurstStyle.RING -> {
            add(100, 4.8f * s, 6.0f * s, 2.4f, 4.0f, 900f, 1300f, 0.78f, bloom = true, spreadJitter = 0.04f)
            add(80, 5.2f * s, 6.4f * s, 1.8f, 3.0f, 800f, 1150f, 0.7f, spreadJitter = 0.03f)
            add(50, 3.2f * s, 4.8f * s, 1.4f, 2.6f, 1000f, 1400f, 0.3f, glitter = true, spreadJitter = 0.1f)
            add(40, 1.0f * s, 2.2f * s, 1.2f, 2.4f, 600f, 950f, 0.35f, delayMs = 320f, glitter = true, fullRandomAngle = true)
        }
        BurstStyle.WILLOW -> {
            add(110, 2.0f * s, 4.6f * s, 2.2f, 4.2f, 1400f, 2100f, 1.0f, gravityMin = 0.0009f, gravityMax = 0.0015f, bloom = true)
            add(80, 1.4f * s, 3.4f * s, 1.6f, 3.0f, 1500f, 2300f, 1.05f, gravityMin = 0.0010f, gravityMax = 0.0016f)
            add(50, 3.0f * s, 5.0f * s, 1.8f, 3.2f, 1000f, 1500f, 0.85f)
            add(70, 0.8f * s, 2.6f * s, 1.1f, 2.2f, 1600f, 2400f, 0.25f, glitter = true, gravityMin = 0.0008f, gravityMax = 0.0014f, fullRandomAngle = true)
        }
        BurstStyle.PALM -> {
            add(48, 3.6f * s, 7.2f * s, 3.0f, 5.8f, 1100f, 1700f, 0.95f, bloom = true, spreadJitter = 0.4f)
            add(36, 2.8f * s, 5.6f * s, 2.4f, 4.6f, 1200f, 1800f, 1.0f, gravityMin = 0.0007f, gravityMax = 0.0012f)
            add(120, 1.8f * s, 4.4f * s, 1.6f, 3.2f, 900f, 1400f, 0.55f, fullRandomAngle = true)
            add(80, 1.2f * s, 3.2f * s, 1.2f, 2.4f, 1100f, 1700f, 0.3f, glitter = true, fullRandomAngle = true)
            add(55, 1.4f * s, 3.6f * s, 1.2f, 2.2f, 650f, 1000f, 0.35f, delayMs = 360f, glitter = true, fullRandomAngle = true)
        }
        BurstStyle.CRACKLE -> {
            add(70, 2.2f * s, 5.0f * s, 2.0f, 3.8f, 700f, 1100f, 0.45f, bloom = true, fullRandomAngle = true)
            add(120, 1.0f * s, 3.4f * s, 1.2f, 2.6f, 500f, 900f, 0.2f, glitter = true, fullRandomAngle = true)
            add(90, 1.4f * s, 4.0f * s, 1.1f, 2.2f, 450f, 800f, 0.25f, delayMs = 180f, glitter = true, fullRandomAngle = true)
            add(90, 1.2f * s, 3.6f * s, 1.1f, 2.0f, 450f, 800f, 0.25f, delayMs = 360f, glitter = true, fullRandomAngle = true)
            add(80, 1.0f * s, 3.0f * s, 1.0f, 1.8f, 400f, 750f, 0.2f, delayMs = 540f, glitter = true, fullRandomAngle = true)
        }
    }

    return sparks
}

private fun DrawScope.drawRocket(
    burst: Burst,
    age: Float,
    center: Offset,
    height: Float,
    fadeOut: Float,
) {
    val launchProgress = ((age + 380f) / 380f).coerceIn(0f, 1f)
    val startY = height * (center.y / height + 0.42f).coerceAtMost(0.92f)
    val y = startY + (center.y - startY) * launchProgress
    val x = center.x + sin(launchProgress * 8f) * 4f
    val head = Offset(x, y)
    val tail = Offset(x, y + 42f * (1f - launchProgress * 0.35f))
    val alpha = (0.35f + 0.65f * launchProgress) * fadeOut
    val color = burst.palette.first()

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = alpha * 0.55f),
                Color.White.copy(alpha = alpha * 0.9f),
            ),
            start = tail,
            end = head,
        ),
        start = tail,
        end = head,
        strokeWidth = 3.2f,
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = 3.5f,
        center = head,
    )
}

private fun DrawScope.drawBurst(
    burst: Burst,
    age: Float,
    center: Offset,
    fadeOut: Float,
) {
    val flashLife = 160f * burst.flashScale
    if (age < flashLife) {
        val flashT = 1f - age / flashLife
        val flashColor = burst.palette.first()
        val radius = (36f + age * 2.4f) * burst.flashScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f * flashT * fadeOut),
                    flashColor.copy(alpha = 0.65f * flashT * fadeOut),
                    flashColor.copy(alpha = 0.18f * flashT * fadeOut),
                    Color.Transparent,
                ),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.82f * flashT * fadeOut),
            radius = (10f + age * 0.42f) * burst.flashScale,
            center = center,
        )
        // Outer bloom ring
        drawCircle(
            color = burst.palette.getOrElse(1) { flashColor }.copy(alpha = 0.28f * flashT * fadeOut),
            radius = radius * 1.15f,
            center = center,
        )
    }

    for (spark in burst.sparks) {
        val localAge = age - spark.delayMs
        if (localAge < 0f) continue

        val life = 1f - (localAge / spark.lifeMs).coerceIn(0f, 1f)
        if (life <= 0f) continue

        val travel = spark.speed * localAge * (1f - spark.drag * localAge)
        val x = center.x + cos(spark.angle) * travel
        val y = center.y + sin(spark.angle) * travel + localAge * localAge * spark.gravity
        val pos = Offset(x, y)
        val alpha = life * fadeOut
        val shimmer = if (spark.glitter) {
            0.45f + 0.55f * ((sin(localAge * 0.055f + spark.angle * 11f) + 1f) * 0.5f)
        } else {
            1f
        }

        if (spark.trail > 0.2f && localAge > 12f) {
            val trailAge = localAge * (1f - spark.trail * 0.58f)
            val trailTravel = spark.speed * trailAge * (1f - spark.drag * trailAge)
            val trailPos = Offset(
                center.x + cos(spark.angle) * trailTravel,
                center.y + sin(spark.angle) * trailTravel + trailAge * trailAge * spark.gravity,
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        spark.color.copy(alpha = 0f),
                        spark.color.copy(alpha = alpha * 0.8f * shimmer),
                        Color.White.copy(alpha = alpha * 0.4f * shimmer),
                    ),
                    start = trailPos,
                    end = pos,
                ),
                start = trailPos,
                end = pos,
                strokeWidth = spark.size * life * 1.45f,
                cap = StrokeCap.Round,
            )
        }

        if (spark.bloom && life > 0.35f) {
            drawCircle(
                color = spark.color.copy(alpha = alpha * 0.22f * shimmer),
                radius = spark.size * (1.8f + life),
                center = pos,
            )
        }

        drawCircle(
            color = spark.color.copy(alpha = alpha * 0.95f * shimmer),
            radius = spark.size * (0.55f + life * 0.75f),
            center = pos,
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.7f * shimmer),
            radius = spark.size * 0.45f * life,
            center = pos,
        )
    }
}
