package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ActiveBubble
import com.example.game.ControllerType
import com.example.game.PlayerSlot
import com.example.game.TeamColor
import com.example.ui.theme.BluetoothBlue
import com.example.ui.theme.FramePinkBunny
import com.example.ui.theme.FramePinkDark
import com.example.ui.theme.FramePurpleGem
import com.example.ui.theme.FrameTealGem
import com.example.ui.theme.TempleGoldBright
import com.example.ui.theme.TempleGoldDeep
import com.example.ui.theme.TempleGoldMid
import com.example.ui.theme.TempleIndigoDark

/**
 * Ornate player avatar badge matching the 4 distinct corner styles from the screenshots:
 * - Frame 0: Ornate Teal & Gold Jeweled Frame (یاسر style)
 * - Frame 1: Cute Pink Bunny-Ear Headphones Frame (سروی style)
 * - Frame 2: Gleaming Gold Ring Frame (خسرو style)
 * - Frame 3: Royal Baroque Gold & Purple Gem Frame (sara style)
 */
@Composable
fun PlayerCornerBadge(
    slot: PlayerSlot,
    isCurrentTurn: Boolean,
    bubble: ActiveBubble?,
    bubbleOnRight: Boolean,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "turn_glow").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val avatarBlock: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(84.dp)
            ) {
                // Active turn glowing aura
                if (isCurrentTurn) {
                    Canvas(modifier = Modifier.size(84.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    TempleGoldBright.copy(alpha = 0.65f * pulse.value),
                                    slot.team.accent.copy(alpha = 0.35f * pulse.value),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension * 0.52f
                        )
                    }
                }

                // Character portrait circle inside ornate frame
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    slot.team.tileTop.copy(alpha = 0.45f),
                                    TempleIndigoDark
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = slot.team.pawnDrawable),
                        contentDescription = slot.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .offset(y = 5.dp)
                    )
                }

                // Ornate frame overlay matching screenshots
                OrnateAvatarFrameCanvas(
                    frameId = slot.frameId,
                    isActive = isCurrentTurn,
                    size = 82.dp
                )

                // Small corner status badge (Bluetooth / Bot / Trophy Rank)
                if (slot.finishRank != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(TempleGoldBright)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "مقام",
                            tint = TempleIndigoDark,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                } else if (slot.controller == ControllerType.BLUETOOTH_REMOTE || slot.controller == ControllerType.BOT) {
                    val isBt = slot.controller == ControllerType.BLUETOOTH_REMOTE
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isBt) BluetoothBlue else Color(0xFF5D4691))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBt) Icons.Rounded.BluetoothConnected else Icons.Rounded.SmartToy,
                            contentDescription = if (isBt) "بازیکن بلوتوثی" else "ربات",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Dark translucent pill name plate matching screenshots ("یاسر", "سروی", "خسرو", "sara")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isCurrentTurn) slot.team.tileSide.copy(alpha = 0.92f)
                        else Color(0xB8140C26)
                    )
                    .border(
                        width = if (isCurrentTurn) 1.5.dp else 1.dp,
                        color = if (isCurrentTurn) TempleGoldBright else Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = slot.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    val bubbleBlock: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = bubble != null,
            enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 450f)) + fadeIn(),
            exit = scaleOut(tween(180)) + fadeOut(tween(180))
        ) {
            if (bubble != null) {
                FloatingReactionBubble(bubble = bubble)
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (bubbleOnRight) {
            avatarBlock()
            bubbleBlock()
        } else {
            bubbleBlock()
            avatarBlock()
        }
    }
}

@Composable
private fun FloatingReactionBubble(bubble: ActiveBubble) {
    if (bubble.sticker != null) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFEAE2F8)),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CartoonStickerArtwork(sticker = bubble.sticker, size = 54.dp)
            }
        }
    } else if (!bubble.text.isNullOrBlank()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEAE2F8))
        ) {
            Text(
                text = bubble.text,
                color = Color(0xFF1F1633),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
fun OrnateAvatarFrameCanvas(
    frameId: Int,
    isActive: Boolean,
    size: Dp = 76.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width * 0.5f
        val cy = this.size.height * 0.5f
        val r = this.size.minDimension * 0.39f

        when (frameId % 4) {
            0 -> drawTealGoldOrnateFrame(cx, cy, r)
            1 -> drawPinkBunnyHeadphoneFrame(cx, cy, r)
            2 -> drawClassicGoldRingFrame(cx, cy, r)
            else -> drawRoyalPurpleJewelFrame(cx, cy, r)
        }

        if (isActive) {
            drawCircle(
                color = TempleGoldBright,
                radius = r + 5.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.2.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawTealGoldOrnateFrame(cx: Float, cy: Float, r: Float) {
    // Gold double ring + teal crystal flourishes at top/sides
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(TempleGoldBright, TempleGoldDeep, TempleGoldBright, TempleGoldMid, TempleGoldBright)
        ),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = r * 0.17f)
    )
    // Teal accent wings
    for (dir in listOf(-1f, 1f)) {
        drawArc(
            color = FrameTealGem,
            startAngle = if (dir < 0) 145f else -35f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - r * 1.07f, cy - r * 1.07f),
            size = Size(r * 2.14f, r * 2.14f),
            style = Stroke(width = r * 0.13f, cap = StrokeCap.Round)
        )
    }
    // Top diamond crown gem
    val gem = Path().apply {
        moveTo(cx, cy - r * 1.24f)
        lineTo(cx + r * 0.18f, cy - r * 0.98f)
        lineTo(cx, cy - r * 0.78f)
        lineTo(cx - r * 0.18f, cy - r * 0.98f)
        close()
    }
    drawPath(gem, FrameTealGem)
    drawPath(gem, TempleGoldBright, style = Stroke(width = r * 0.06f))
}

private fun DrawScope.drawPinkBunnyHeadphoneFrame(cx: Float, cy: Float, r: Float) {
    // Pink bunny ears on top of the frame (like سروی in Screenshot 1!)
    for (dir in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(cx + dir * r * 0.28f, cy - r * 0.90f)
            quadraticTo(cx + dir * r * 0.65f, cy - r * 1.55f, cx + dir * r * 0.78f, cy - r * 1.25f)
            quadraticTo(cx + dir * r * 0.85f, cy - r * 0.95f, cx + dir * r * 0.56f, cy - r * 0.74f)
            close()
        }
        drawPath(ear, FramePinkBunny)
        drawPath(ear, FramePinkDark, style = Stroke(width = r * 0.06f))
    }

    // Soft glowing pink headband circle
    drawCircle(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFD6EC), FramePinkBunny)),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = r * 0.16f)
    )

    // Left & right pink headphone earcups
    for (dir in listOf(-1f, 1f)) {
        drawRoundRect(
            color = Color(0xFFFFB5DF),
            topLeft = Offset(cx + dir * r * 0.96f - r * 0.18f, cy - r * 0.24f),
            size = Size(r * 0.36f, r * 0.52f)
        )
        drawRoundRect(
            color = FramePinkDark,
            topLeft = Offset(cx + dir * r * 0.96f - r * 0.18f, cy - r * 0.24f),
            size = Size(r * 0.36f, r * 0.52f),
            style = Stroke(width = r * 0.06f)
        )
    }
}

private fun DrawScope.drawClassicGoldRingFrame(cx: Float, cy: Float, r: Float) {
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFF3B0), TempleGoldMid, TempleGoldDeep, TempleGoldBright),
            start = Offset(cx - r, cy - r),
            end = Offset(cx + r, cy + r)
        ),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = r * 0.18f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = r * 0.92f,
        center = Offset(cx, cy),
        style = Stroke(width = r * 0.04f)
    )
}

private fun DrawScope.drawRoyalPurpleJewelFrame(cx: Float, cy: Float, r: Float) {
    // Ornate baroque gold filigree ring + top royal purple amethyst gem (like sara in Screenshot 2!)
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(TempleGoldBright, TempleGoldDeep, Color(0xFFFFEE88), TempleGoldMid, TempleGoldBright)
        ),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = r * 0.20f)
    )
    // Decorative corner beads
    for (angle in listOf(45f, 135f, 225f, 315f)) {
        val rad = Math.toRadians(angle.toDouble())
        val bx = cx + (kotlin.math.cos(rad) * r * 1.06f).toFloat()
        val by = cy + (kotlin.math.sin(rad) * r * 1.06f).toFloat()
        drawCircle(TempleGoldBright, radius = r * 0.11f, center = Offset(bx, by))
    }
    // Top royal purple diamond jewel
    val gem = Path().apply {
        moveTo(cx, cy - r * 1.28f)
        lineTo(cx + r * 0.22f, cy - r * 0.98f)
        lineTo(cx, cy - r * 0.74f)
        lineTo(cx - r * 0.22f, cy - r * 0.98f)
        close()
    }
    drawPath(gem, FramePurpleGem)
    drawPath(gem, TempleGoldBright, style = Stroke(width = r * 0.07f))
}
