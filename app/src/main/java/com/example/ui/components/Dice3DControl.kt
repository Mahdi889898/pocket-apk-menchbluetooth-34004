package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TeamRedAccent
import com.example.ui.theme.TempleGoldBright
import com.example.ui.theme.TempleGoldDeep
import com.example.ui.theme.TempleGoldMid

/**
 * Glossy 3D Ivory & Gold Die with tumbling roll animation and pulsing turn halo.
 */
@Composable
fun Dice3DControl(
    diceValue: Int,
    isRolling: Boolean,
    canRoll: Boolean,
    accentColor: Color,
    onRollClick: () -> Unit,
    size: Dp = 74.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "dice_fx")
    val spinAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isRolling -> 1.14f
            canRoll -> 1.04f
            else -> 0.94f
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size + 18.dp)
            .semantics { contentDescription = "تاس با عدد $diceValue" }
            .clickable(
                enabled = canRoll && !isRolling,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onRollClick
            )
            .testTag("dice_roll_button")
    ) {
        if (canRoll && !isRolling) {
            Canvas(modifier = Modifier.size(size + 18.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TempleGoldBright.copy(alpha = 0.65f * glowPulse),
                            accentColor.copy(alpha = 0.35f * glowPulse),
                            Color.Transparent
                        )
                    ),
                    radius = this.size.minDimension * 0.52f
                )
            }
        }

        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = if (isRolling) spinAngle else 0f
                }
        ) {
            drawGlossy3DDie(
                value = diceValue.coerceIn(1, 6),
                accentColor = accentColor,
                dimmed = !canRoll && !isRolling
            )
        }
    }
}

private fun DrawScope.drawGlossy3DDie(
    value: Int,
    accentColor: Color,
    dimmed: Boolean
) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(w * 0.24f, h * 0.24f)
    val depth = h * 0.08f

    // Drop shadow
    drawRoundRect(
        color = Color(0x660B0516),
        topLeft = Offset(w * 0.06f, h * 0.12f),
        size = Size(w * 0.88f, h * 0.88f),
        cornerRadius = corner
    )

    // Gold extruded bottom bevel
    drawRoundRect(
        brush = Brush.verticalGradient(colors = listOf(TempleGoldMid, TempleGoldDeep)),
        topLeft = Offset(w * 0.05f, h * 0.08f),
        size = Size(w * 0.90f, h * 0.88f),
        cornerRadius = corner
    )

    // Warm ivory glossy top face
    val faceTopLeft = Offset(w * 0.07f, h * 0.04f)
    val faceSize = Size(w * 0.86f, h * 0.84f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = if (dimmed) {
                listOf(Color(0xFFE6DEC9), Color(0xFFCBBFA0))
            } else {
                listOf(Color(0xFFFFFDF4), Color(0xFFF6E7BE))
            }
        ),
        topLeft = faceTopLeft,
        size = faceSize,
        cornerRadius = CornerRadius(w * 0.20f, h * 0.20f)
    )

    // Ornate golden rim stroke
    drawRoundRect(
        brush = Brush.linearGradient(listOf(TempleGoldBright, TempleGoldDeep, TempleGoldBright)),
        topLeft = faceTopLeft,
        size = faceSize,
        cornerRadius = CornerRadius(w * 0.20f, h * 0.20f),
        style = Stroke(width = w * 0.06f)
    )

    // Subtle specular sheen
    drawRoundRect(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = Offset(w * 0.14f, h * 0.09f),
        size = Size(w * 0.72f, h * 0.18f),
        cornerRadius = CornerRadius(w * 0.12f, h * 0.12f)
    )

    // Pips
    val cx = faceTopLeft.x + faceSize.width * 0.5f
    val cy = faceTopLeft.y + faceSize.height * 0.5f
    val dx = faceSize.width * 0.25f
    val dy = faceSize.height * 0.25f
    val pipR = faceSize.width * 0.092f

    val pips: List<Offset> = when (value) {
        1 -> listOf(Offset(cx, cy))
        2 -> listOf(Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy))
        3 -> listOf(Offset(cx - dx, cy - dy), Offset(cx, cy), Offset(cx + dx, cy + dy))
        4 -> listOf(
            Offset(cx - dx, cy - dy), Offset(cx + dx, cy - dy),
            Offset(cx - dx, cy + dy), Offset(cx + dx, cy + dy)
        )
        5 -> listOf(
            Offset(cx - dx, cy - dy), Offset(cx + dx, cy - dy),
            Offset(cx, cy),
            Offset(cx - dx, cy + dy), Offset(cx + dx, cy + dy)
        )
        else -> listOf(
            Offset(cx - dx, cy - dy), Offset(cx + dx, cy - dy),
            Offset(cx - dx, cy), Offset(cx + dx, cy),
            Offset(cx - dx, cy + dy), Offset(cx + dx, cy + dy)
        )
    }

    val pipColor = when (value) {
        6 -> accentColor
        1 -> TeamRedAccent
        else -> Color(0xFF2B1B4D)
    }

    for (p in pips) {
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = pipR * 1.12f,
            center = Offset(p.x, p.y + depth * 0.2f)
        )
        drawCircle(
            color = pipColor,
            radius = pipR,
            center = p
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.32f),
            radius = pipR * 0.35f,
            center = Offset(p.x - pipR * 0.28f, p.y - pipR * 0.28f)
        )
    }
}
