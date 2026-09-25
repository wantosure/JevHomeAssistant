package com.jev.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jev.assistant.ui.theme.BlueNeon
import com.jev.assistant.ui.theme.CyanAccent
import com.jev.assistant.ui.theme.EmeraldAccent
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingAuraVisualizer(
    modifier: Modifier = Modifier,
    isListening: Boolean,
    isDeciding: Boolean,
    rms: Float,
    isSuccess: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraTransition")

    // 基础呼吸动效
    val breatheAnimation by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheAnim"
    )

    // 旋转动效（用于决策与流光）
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isDeciding) 1800 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAnim"
    )

    // 涟漪扩散动效
    val waveRipple by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAnim"
    )

    val primaryColor = when {
        isSuccess -> EmeraldAccent
        isDeciding -> Color(0xFFB388FF) // 决策时呈现深邃极光紫
        isListening -> CyanAccent
        else -> Color(0xFF4A5568)
    }

    val secondaryColor = when {
        isSuccess -> CyanAccent
        isDeciding -> BlueNeon
        isListening -> BlueNeon
        else -> Color(0xFF1E293B)
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 4.2f

            // 能量增幅 (rms)
            val dynamicScale = if (isListening) (breatheAnimation + (rms * 1.5f).coerceAtMost(0.8f)) else 0.8f
            val coreRadius = baseRadius * dynamicScale

            // 1. 背景光晕 (Outer Glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isListening) 0.35f else 0.1f),
                        secondaryColor.copy(alpha = if (isListening) 0.15f else 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 2.2f
                ),
                radius = baseRadius * 2.2f,
                center = center
            )

            // 2. 动态扩散波纹 (Speech Ripple)
            if (isListening) {
                val rippleRadius = coreRadius + (baseRadius * 1.2f * waveRipple)
                val rippleAlpha = (1f - waveRipple).coerceIn(0f, 1f) * 0.5f
                drawCircle(
                    color = primaryColor.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 3. 旋转流光粒子轨道 (Orbiting Plasma Ring)
            val ringRadius = coreRadius * 1.25f
            val radAngle = Math.toRadians(rotationAngle.toDouble())
            val pointCount = 12
            for (i in 0 until pointCount) {
                val stepAngle = radAngle + (i * (2 * Math.PI / pointCount))
                val px = center.x + (ringRadius * cos(stepAngle)).toFloat()
                val py = center.y + (ringRadius * sin(stepAngle)).toFloat()
                val alpha = ((i + 1).toFloat() / pointCount) * (if (isDeciding) 0.9f else 0.5f)
                val dotRadius = if (isDeciding) 4.5.dp.toPx() else 3.dp.toPx()

                drawCircle(
                    color = primaryColor.copy(alpha = alpha),
                    radius = dotRadius,
                    center = Offset(px, py)
                )
            }

            // 4. 核心发光能量球 (Core Energy Sphere)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 5. 核心细线发光环
            drawCircle(
                color = primaryColor.copy(alpha = 0.8f),
                radius = coreRadius * 0.9f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
