package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserProfile
import com.example.game.TeamColor
import com.example.ui.components.OrnateAvatarFrameCanvas
import com.example.ui.theme.BluetoothBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TeamGreenBottom
import com.example.ui.theme.TeamGreenSide
import com.example.ui.theme.TeamGreenTop
import com.example.ui.theme.TeamYellowBottom
import com.example.ui.theme.TeamYellowSide
import com.example.ui.theme.TeamYellowTop
import com.example.ui.theme.TempleCreamText
import com.example.ui.theme.TempleGoldBright
import com.example.ui.theme.TempleGoldDeep
import com.example.ui.theme.TempleGoldMid
import com.example.ui.theme.TempleIndigoDark
import com.example.ui.theme.TempleIndigoSurface

@Composable
fun HomeMenuScreen(
    profile: UserProfile,
    onBluetoothPlayClick: () -> Unit,
    onPlayVsBotsClick: () -> Unit,
    onPassAndPlayClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onToggleSound: () -> Unit
) {
    val floatAnim by rememberInfiniteTransition(label = "hero_float").animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val chosenTeam = TeamColor.entries.getOrElse(profile.preferredTeamIndex) { TeamColor.GREEN }

    Box(modifier = Modifier.fillMaxSize()) {
        // Enchanted Temple Courtyard full-bleed background
        Image(
            painter = painterResource(id = R.drawable.img_temple_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle atmospheric gradient so hero art & glossy title pop vividly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xA6190E36),
                            Color(0x521B1138),
                            Color(0xCC160C2E)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Player Profile Pill + Sound Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = TempleIndigoSurface.copy(alpha = 0.88f),
                    border = BorderStroke(1.5.dp, TempleGoldBright.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .clickable(onClick = onEditProfileClick)
                        .testTag("profile_badge_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(chosenTeam.tileBottom.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = chosenTeam.pawnDrawable),
                                    contentDescription = profile.playerName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            OrnateAvatarFrameCanvas(
                                frameId = profile.frameId,
                                isActive = false,
                                size = 44.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = profile.playerName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TempleCreamText
                            )
                            Text(
                                text = "تغییر نام و قاب",
                                style = MaterialTheme.typography.labelSmall,
                                color = TempleGoldBright
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "ویرایش پروفایل",
                            tint = TempleGoldBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TempleIndigoSurface.copy(alpha = 0.88f))
                        .border(1.5.dp, TempleGoldBright.copy(alpha = 0.6f), CircleShape)
                        .testTag("home_sound_toggle")
                ) {
                    Icon(
                        imageVector = if (profile.soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = "تغییر وضعیت صدا",
                        tint = TempleGoldBright
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Glossy Royal Title Emblem
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = TempleIndigoDark.copy(alpha = 0.78f),
                border = BorderStroke(2.5.dp, TempleGoldBright),
                shadowElevation = 14.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "منچ بلوتوثی",
                        style = MaterialTheme.typography.displayLarge,
                        color = TempleGoldBright,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "معبد افسانه‌ای • چندنفره بدون اینترنت",
                        style = MaterialTheme.typography.labelLarge,
                        color = TempleCreamText.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Showcase of all 4 3D Cartoon Heroes standing together!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .height(175.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Soft golden glow pedestal under heroes
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    TeamGreenTop.copy(alpha = 0.35f),
                                    TempleGoldBright.copy(alpha = 0.55f),
                                    TeamRedTopAlpha(),
                                    BluetoothBlue.copy(alpha = 0.45f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    HeroShowcaseItem(
                        drawableRes = R.drawable.img_pawn_bear,
                        contentDesc = "خرس سبز",
                        offsetY = floatAnim,
                        sizeDp = 96
                    )
                    HeroShowcaseItem(
                        drawableRes = R.drawable.img_pawn_strongman,
                        contentDesc = "پهلوان قرمز",
                        offsetY = -floatAnim,
                        sizeDp = 104
                    )
                    HeroShowcaseItem(
                        drawableRes = R.drawable.img_pawn_girl,
                        contentDesc = "دختر چکش‌دار زرد",
                        offsetY = floatAnim * 0.8f,
                        sizeDp = 104
                    )
                    HeroShowcaseItem(
                        drawableRes = R.drawable.img_pawn_pirate,
                        contentDesc = "دزد دریایی آبی",
                        offsetY = -floatAnim * 0.8f,
                        sizeDp = 96
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Primary Glossy 3D Game Mode Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlossyGameButton(
                    title = "بازی بلوتوثی با دوستان",
                    subtitle = "اتصال مستقیم ۲ تا ۴ گوشی بدون نیاز به اینترنت",
                    icon = Icons.Rounded.Bluetooth,
                    topColor = Color(0xFF38C6FF),
                    bottomColor = Color(0xFF0E76E5),
                    rimColor = Color(0xFF084B9C),
                    onClick = onBluetoothPlayClick,
                    testTag = "btn_bluetooth_play"
                )

                GlossyGameButton(
                    title = "بازی با ربات هوشمند",
                    subtitle = "رقابت سریع ۲، ۳ یا ۴ نفره با حریف‌های حرفه‌ای",
                    icon = Icons.Rounded.SmartToy,
                    topColor = TeamGreenTop,
                    bottomColor = TeamGreenBottom,
                    rimColor = TeamGreenSide,
                    onClick = onPlayVsBotsClick,
                    testTag = "btn_vs_bots"
                )

                GlossyGameButton(
                    title = "چندنفره روی یک گوشی",
                    subtitle = "بازی دوستانه نوبتی با خانواده روی همین صفحه",
                    icon = Icons.Rounded.Groups,
                    topColor = TeamYellowTop,
                    bottomColor = TeamYellowBottom,
                    rimColor = TeamYellowSide,
                    textColor = TempleIndigoDark,
                    onClick = onPassAndPlayClick,
                    testTag = "btn_pass_and_play"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun TeamRedTopAlpha(): Color = Color(0x66FF4B3E)

@Composable
private fun HeroShowcaseItem(
    drawableRes: Int,
    contentDesc: String,
    offsetY: Float,
    sizeDp: Int
) {
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = contentDesc,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(sizeDp.dp)
            .offset(y = offsetY.dp)
    )
}

@Composable
fun GlossyGameButton(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    topColor: Color,
    bottomColor: Color,
    rimColor: Color,
    textColor: Color = Color.White,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(rimColor)
            .clickable(onClick = onClick)
            .padding(bottom = 5.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
                .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = textColor.copy(alpha = 0.88f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
