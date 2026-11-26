package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.QUICK_CHAT_PHRASES
import com.example.game.StickerType
import com.example.ui.theme.TempleCreamText
import com.example.ui.theme.TempleGoldBright
import com.example.ui.theme.TempleIndigoCard
import com.example.ui.theme.TempleIndigoDark
import com.example.ui.theme.TempleIndigoSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom hand-crafted glossy cartoon stickers inspired by the reference screenshots
 * (pouting pink bunny, funny chubby chicken, royal laugh, starry heart, fiery flame, trophy).
 */
@Composable
fun CartoonStickerArtwork(
    sticker: StickerType,
    size: Dp = 54.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        when (sticker) {
            StickerType.POUTING_BUNNY -> drawPoutingBunny()
            StickerType.FUNNY_CHICKEN -> drawFunnyChicken()
            StickerType.ROYAL_LAUGH -> drawRoyalLaugh()
            StickerType.STAR_HEART -> drawStarHeart()
            StickerType.ANGRY_FLAME -> drawAngryFlame()
            StickerType.TROPHY_GG -> drawTrophySticker()
        }
    }
}

private fun DrawScope.drawPoutingBunny() {
    val w = size.width
    val h = size.height
    val cx = w * 0.52f
    val cy = h * 0.56f

    // Droopy left ear + tall right ear (like Screenshot 1 pink bunny!)
    val leftEar = Path().apply {
        moveTo(cx - w * 0.14f, cy - h * 0.22f)
        quadraticTo(cx - w * 0.44f, cy - h * 0.36f, cx - w * 0.42f, cy - h * 0.08f)
        quadraticTo(cx - w * 0.34f, cy - h * 0.04f, cx - w * 0.16f, cy - h * 0.10f)
        close()
    }
    drawPath(leftEar, Color(0xFFFFB7DC))
    drawPath(leftEar, Color(0xFF7A385C), style = Stroke(width = w * 0.035f))

    val rightEar = Path().apply {
        moveTo(cx + w * 0.02f, cy - h * 0.25f)
        quadraticTo(cx + w * 0.24f, cy - h * 0.54f, cx + w * 0.32f, cy - h * 0.28f)
        quadraticTo(cx + w * 0.28f, cy - h * 0.12f, cx + w * 0.12f, cy - h * 0.16f)
        close()
    }
    drawPath(rightEar, Color(0xFFFFB7DC))
    drawPath(rightEar, Color(0xFF7A385C), style = Stroke(width = w * 0.035f))

    // Cute body
    drawOval(
        color = Color(0xFFFFCBE5),
        topLeft = Offset(cx - w * 0.22f, cy + h * 0.12f),
        size = Size(w * 0.42f, h * 0.30f)
    )
    drawOval(
        color = Color(0xFF7A385C),
        topLeft = Offset(cx - w * 0.22f, cy + h * 0.12f),
        size = Size(w * 0.42f, h * 0.30f),
        style = Stroke(width = w * 0.03f)
    )

    // Big round bunny head
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFE5F3), Color(0xFFFFB5DB)),
            center = Offset(cx - w * 0.06f, cy - h * 0.08f),
            radius = w * 0.34f
        ),
        radius = w * 0.29f,
        center = Offset(cx, cy - h * 0.03f)
    )
    drawCircle(
        color = Color(0xFF7A385C),
        radius = w * 0.29f,
        center = Offset(cx, cy - h * 0.03f),
        style = Stroke(width = w * 0.035f)
    )

    // Huge glossy side-glancing eyes
    val eyeY = cy - h * 0.05f
    for (dx in listOf(-w * 0.11f, w * 0.10f)) {
        drawCircle(Color.White, radius = w * 0.095f, center = Offset(cx + dx, eyeY))
        drawCircle(Color(0xFF5A2B45), radius = w * 0.095f, center = Offset(cx + dx, eyeY), style = Stroke(w * 0.025f))
        drawCircle(Color(0xFF3A1A2E), radius = w * 0.042f, center = Offset(cx + dx + w * 0.025f, eyeY - h * 0.01f))
        drawCircle(Color.White, radius = w * 0.018f, center = Offset(cx + dx + w * 0.01f, eyeY - h * 0.03f))
    }

    // Little pink nose & pouting frown
    drawCircle(Color(0xFFFF5C93), radius = w * 0.032f, center = Offset(cx + w * 0.02f, cy + h * 0.05f))
    drawArc(
        color = Color(0xFF6B2848),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - w * 0.05f, cy + h * 0.09f),
        size = Size(w * 0.14f, h * 0.08f),
        style = Stroke(width = w * 0.03f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawFunnyChicken() {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f
    val cy = h * 0.56f

    // Red comb on top of head (like Screenshot 2 funny chicken!)
    drawCircle(Color(0xFFE53935), radius = w * 0.08f, center = Offset(cx - w * 0.08f, cy - h * 0.34f))
    drawCircle(Color(0xFFE53935), radius = w * 0.07f, center = Offset(cx + w * 0.02f, cy - h * 0.37f))
    drawCircle(Color(0xFFE53935), radius = w * 0.06f, center = Offset(cx + w * 0.10f, cy - h * 0.32f))

    // Flapping chubby wings
    drawOval(
        color = Color(0xFFDF8B2E),
        topLeft = Offset(cx - w * 0.42f, cy - h * 0.02f),
        size = Size(w * 0.22f, h * 0.22f)
    )
    drawOval(
        color = Color(0xFFDF8B2E),
        topLeft = Offset(cx + w * 0.20f, cy - h * 0.02f),
        size = Size(w * 0.22f, h * 0.22f)
    )

    // Plump golden-orange pear body
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFC15E), Color(0xFFE08A28))
        ),
        topLeft = Offset(cx - w * 0.29f, cy - h * 0.30f),
        size = Size(w * 0.58f, h * 0.68f)
    )
    drawOval(
        color = Color(0xFF6B3A0B),
        topLeft = Offset(cx - w * 0.29f, cy - h * 0.30f),
        size = Size(w * 0.58f, h * 0.68f),
        style = Stroke(width = w * 0.035f)
    )

    // Big goofy wide eyes
    val eyeY = cy - h * 0.10f
    for (dx in listOf(-w * 0.10f, w * 0.10f)) {
        drawCircle(Color.White, radius = w * 0.095f, center = Offset(cx + dx, eyeY))
        drawCircle(Color(0xFF542C07), radius = w * 0.095f, center = Offset(cx + dx, eyeY), style = Stroke(w * 0.028f))
        drawCircle(Color(0xFF1F1205), radius = w * 0.038f, center = Offset(cx + dx, eyeY))
    }

    // Bright orange beak
    val beak = Path().apply {
        moveTo(cx - w * 0.08f, cy + h * 0.02f)
        lineTo(cx + w * 0.08f, cy + h * 0.02f)
        lineTo(cx, cy + h * 0.14f)
        close()
    }
    drawPath(beak, Color(0xFFFF7043))
    drawPath(beak, Color(0xFF6B3A0B), style = Stroke(width = w * 0.025f))
}

private fun DrawScope.drawRoyalLaugh() {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.56f
    val r = w * 0.34f

    // Crown on top
    val crown = Path().apply {
        moveTo(cx - r * 0.6f, cy - r * 0.85f)
        lineTo(cx - r * 0.72f, cy - r * 1.45f)
        lineTo(cx - r * 0.22f, cy - r * 1.12f)
        lineTo(cx, cy - r * 1.55f)
        lineTo(cx + r * 0.22f, cy - r * 1.12f)
        lineTo(cx + r * 0.72f, cy - r * 1.45f)
        lineTo(cx + r * 0.6f, cy - r * 0.85f)
        close()
    }
    drawPath(crown, Color(0xFFFFD84D))
    drawPath(crown, Color(0xFF9C5E00), style = Stroke(width = w * 0.03f))

    // Smiley face
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF176), Color(0xFFFFA726)),
            center = Offset(cx - r * 0.2f, cy - r * 0.2f),
            radius = r * 1.2f
        ),
        radius = r,
        center = Offset(cx, cy)
    )
    drawCircle(Color(0xFF8C4A00), radius = r, center = Offset(cx, cy), style = Stroke(w * 0.035f))

    // Happy squint eyes
    for (dir in listOf(-1f, 1f)) {
        drawArc(
            color = Color(0xFF5A2D00),
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx + dir * r * 0.35f - r * 0.18f, cy - r * 0.26f),
            size = Size(r * 0.36f, r * 0.26f),
            style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
        )
    }

    // Joyful tear drops
    drawCircle(Color(0xFF29B6F6), radius = r * 0.16f, center = Offset(cx - r * 0.75f, cy + r * 0.06f))
    drawCircle(Color(0xFF29B6F6), radius = r * 0.16f, center = Offset(cx + r * 0.75f, cy + r * 0.06f))

    // Big laughing mouth
    drawArc(
        color = Color(0xFF7A1C1C),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r * 0.52f, cy + r * 0.02f),
        size = Size(r * 1.04f, r * 0.68f)
    )
    drawArc(
        color = Color.White,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r * 0.44f, cy + r * 0.05f),
        size = Size(r * 0.88f, r * 0.22f)
    )
}

private fun DrawScope.drawStarHeart() {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.52f
    val path = Path().apply {
        moveTo(cx, cy + h * 0.34f)
        cubicTo(cx - w * 0.48f, cy + h * 0.04f, cx - w * 0.42f, cy - h * 0.36f, cx, cy - h * 0.12f)
        cubicTo(cx + w * 0.42f, cy - h * 0.36f, cx + w * 0.48f, cy + h * 0.04f, cx, cy + h * 0.34f)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(colors = listOf(Color(0xFFFF5C8D), Color(0xFFD81B60)))
    )
    drawPath(path, color = Color(0xFF7A0D35), style = Stroke(width = w * 0.035f))

    // Sparkle star
    val starPath = Path()
    val scx = cx + w * 0.22f
    val scy = cy - h * 0.22f
    for (i in 0 until 8) {
        val a = i * PI / 4.0
        val r = if (i % 2 == 0) w * 0.15f else w * 0.06f
        val x = scx + (cos(a) * r).toFloat()
        val y = scy + (sin(a) * r).toFloat()
        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
    }
    starPath.close()
    drawPath(starPath, Color(0xFFFFEA61))
}

private fun DrawScope.drawAngryFlame() {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.56f
    val r = w * 0.34f

    // Flame tips behind head
    val flame = Path().apply {
        moveTo(cx - r, cy)
        quadraticTo(cx - r * 1.15f, cy - r * 1.1f, cx - r * 0.3f, cy - r * 0.9f)
        quadraticTo(cx, cy - r * 1.65f, cx + r * 0.35f, cy - r * 0.9f)
        quadraticTo(cx + r * 1.15f, cy - r * 1.1f, cx + r, cy)
        close()
    }
    drawPath(flame, Color(0xFFFF9800))

    drawCircle(
        brush = Brush.verticalGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFC62828))),
        radius = r,
        center = Offset(cx, cy)
    )
    drawCircle(Color(0xFF6B0B0B), radius = r, center = Offset(cx, cy), style = Stroke(w * 0.035f))

    // Angry brows + eyes
    drawLine(Color(0xFF4A0808), Offset(cx - r * 0.55f, cy - r * 0.36f), Offset(cx - r * 0.10f, cy - r * 0.14f), strokeWidth = w * 0.055f, cap = StrokeCap.Round)
    drawLine(Color(0xFF4A0808), Offset(cx + r * 0.55f, cy - r * 0.36f), Offset(cx + r * 0.10f, cy - r * 0.14f), strokeWidth = w * 0.055f, cap = StrokeCap.Round)
    drawCircle(Color.White, radius = r * 0.16f, center = Offset(cx - r * 0.32f, cy - r * 0.04f))
    drawCircle(Color.White, radius = r * 0.16f, center = Offset(cx + r * 0.32f, cy - r * 0.04f))
    drawCircle(Color.Black, radius = r * 0.08f, center = Offset(cx - r * 0.28f, cy - r * 0.04f))
    drawCircle(Color.Black, radius = r * 0.08f, center = Offset(cx + r * 0.28f, cy - r * 0.04f))

    // Gritted mouth
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(cx - r * 0.42f, cy + r * 0.25f),
        size = Size(r * 0.84f, r * 0.28f)
    )
    drawRoundRect(
        color = Color(0xFF4A0808),
        topLeft = Offset(cx - r * 0.42f, cy + r * 0.25f),
        size = Size(r * 0.84f, r * 0.28f),
        style = Stroke(width = w * 0.03f)
    )
}

private fun DrawScope.drawTrophySticker() {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f

    // Handles
    drawCircle(
        color = Color(0xFFE6A100),
        radius = w * 0.14f,
        center = Offset(cx - w * 0.24f, h * 0.36f),
        style = Stroke(width = w * 0.055f)
    )
    drawCircle(
        color = Color(0xFFE6A100),
        radius = w * 0.14f,
        center = Offset(cx + w * 0.24f, h * 0.36f),
        style = Stroke(width = w * 0.055f)
    )

    // Cup bowl
    val cup = Path().apply {
        moveTo(cx - w * 0.26f, h * 0.20f)
        lineTo(cx + w * 0.26f, h * 0.20f)
        quadraticTo(cx + w * 0.24f, h * 0.58f, cx, h * 0.62f)
        quadraticTo(cx - w * 0.24f, h * 0.58f, cx - w * 0.26f, h * 0.20f)
        close()
    }
    drawPath(
        cup,
        brush = Brush.horizontalGradient(colors = listOf(Color(0xFFFFEA70), Color(0xFFF5A600), Color(0xFFFFD54F)))
    )
    drawPath(cup, Color(0xFF8C5600), style = Stroke(width = w * 0.03f))

    // Stem & pedestal
    drawRect(Color(0xFFE09600), topLeft = Offset(cx - w * 0.06f, h * 0.61f), size = Size(w * 0.12f, h * 0.15f))
    drawRoundRect(Color(0xFF5D3310), topLeft = Offset(cx - w * 0.22f, h * 0.74f), size = Size(w * 0.44f, h * 0.13f))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StickerAndChatSheet(
    onSendPhrase: (String) -> Unit,
    onSendSticker: (StickerType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("sticker_chat_sheet"),
        shape = RoundedCornerShape(26.dp),
        color = TempleIndigoSurface.copy(alpha = 0.96f),
        shadowElevation = 16.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, TempleGoldBright.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "استیکر و پیام سریع",
                    style = MaterialTheme.typography.titleLarge,
                    color = TempleGoldBright
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("close_chat_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = TempleCreamText
                    )
                }
            }

            // 6 Cartoon Stickers Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StickerType.entries.take(3).forEach { sticker ->
                    StickerButton(sticker = sticker, onClick = { onSendSticker(sticker) })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StickerType.entries.drop(3).forEach { sticker ->
                    StickerButton(sticker = sticker, onClick = { onSendSticker(sticker) })
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Quick Persian Chat Phrases
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QUICK_CHAT_PHRASES.forEachIndexed { idx, phrase ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(TempleIndigoCard)
                            .border(1.dp, TempleGoldBright.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .clickable { onSendPhrase(phrase) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("quick_chat_$idx")
                    ) {
                        Text(
                            text = phrase,
                            color = TempleCreamText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerButton(
    sticker: StickerType,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TempleIndigoDark.copy(alpha = 0.7f))
            .border(1.5.dp, TempleGoldBright.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("sticker_${sticker.id}")
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center
        ) {
            CartoonStickerArtwork(sticker = sticker, size = 48.dp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = sticker.label,
            style = MaterialTheme.typography.labelSmall,
            color = TempleCreamText
        )
    }
}
