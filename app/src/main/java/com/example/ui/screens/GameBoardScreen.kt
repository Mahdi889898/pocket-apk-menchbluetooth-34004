package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.GameState
import com.example.game.MatchMode
import com.example.game.MatchPhase
import com.example.game.StickerType
import com.example.game.TeamColor
import com.example.ui.components.Dice3DControl
import com.example.ui.components.IsometricBoardView
import com.example.ui.components.PlayerCornerBadge
import com.example.ui.components.StickerAndChatSheet
import com.example.ui.theme.BluetoothBlue
import com.example.ui.theme.TempleCreamText
import com.example.ui.theme.TempleGoldBright
import com.example.ui.theme.TempleIndigoCard
import com.example.ui.theme.TempleIndigoDark
import com.example.ui.theme.TempleIndigoSurface

@Composable
fun GameBoardScreen(
    state: GameState,
    soundEnabled: Boolean,
    onRollDice: () -> Unit,
    onPawnTapped: (TeamColor, Int) -> Unit,
    onSendPhrase: (String) -> Unit,
    onSendSticker: (StickerType) -> Unit,
    onToggleSound: () -> Unit,
    onPlayAgain: () -> Unit,
    onExitToMenu: () -> Unit
) {
    var showChatSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (showChatSheet) {
            showChatSheet = false
        } else {
            showExitDialog = true
        }
    }

    val activeTurnTeam = state.currentTurn
    val canRollNow = state.phase == MatchPhase.WAITING_FOR_ROLL && state.canLocalDeviceAct()
    val isBluetoothMatch = state.matchMode == MatchMode.BLUETOOTH_HOST || state.matchMode == MatchMode.BLUETOOTH_CLIENT

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-bleed Enchanted Temple Courtyard background
        Image(
            painter = painterResource(id = R.drawable.img_temple_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle warm soft-light wash for crisp readability of all 3D tiles & characters
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55180C32),
                            Color(0x16000000),
                            Color(0x16000000),
                            Color(0x88180C32)
                        )
                    )
                )
        )

        // 2. Main Arena Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Compact HUD Bar (Back, Status Pill, Sound)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { showExitDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TempleIndigoDark.copy(alpha = 0.78f))
                        .border(1.5.dp, TempleGoldBright.copy(alpha = 0.65f), CircleShape)
                        .testTag("game_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "خروج",
                        tint = TempleGoldBright
                    )
                }

                // Live Turn Status Banner Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TempleIndigoDark.copy(alpha = 0.85f),
                    border = BorderStroke(1.5.dp, activeTurnTeam.accent),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isBluetoothMatch) {
                            Icon(
                                imageVector = Icons.Rounded.BluetoothConnected,
                                contentDescription = "بازی بلوتوثی",
                                tint = BluetoothBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = state.statusBanner,
                            color = TempleCreamText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TempleIndigoDark.copy(alpha = 0.78f))
                        .border(1.5.dp, TempleGoldBright.copy(alpha = 0.65f), CircleShape)
                        .testTag("game_sound_toggle")
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = "صدا",
                        tint = TempleGoldBright
                    )
                }
            }

            // Center Arena: 3D Isometric Board + 4 Ornate Corner Player Badges & Bubbles!
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Full 3D Isometric Diamond Board
                IsometricBoardView(
                    state = state,
                    onPawnTapped = onPawnTapped,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 22.dp)
                )

                // Top-Left Corner: GREEN (Bear - یاسر)
                state.players[TeamColor.GREEN]?.takeIf { it.isActive }?.let { slot ->
                    PlayerCornerBadge(
                        slot = slot,
                        isCurrentTurn = state.currentTurn == TeamColor.GREEN,
                        bubble = state.bubbles[TeamColor.GREEN],
                        bubbleOnRight = false, // In RTL Row, false places bubble inward toward center
                        modifier = Modifier
                            .align(Alignment.TopEnd) // In RTL, TopEnd = visual Top-Left!
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Top-Right Corner: RED (Strongman - سروی)
                state.players[TeamColor.RED]?.takeIf { it.isActive }?.let { slot ->
                    PlayerCornerBadge(
                        slot = slot,
                        isCurrentTurn = state.currentTurn == TeamColor.RED,
                        bubble = state.bubbles[TeamColor.RED],
                        bubbleOnRight = true, // In RTL Row, true places bubble inward toward center
                        modifier = Modifier
                            .align(Alignment.TopStart) // In RTL, TopStart = visual Top-Right!
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Bottom-Left Corner: YELLOW (Mallet Girl - سارا)
                state.players[TeamColor.YELLOW]?.takeIf { it.isActive }?.let { slot ->
                    PlayerCornerBadge(
                        slot = slot,
                        isCurrentTurn = state.currentTurn == TeamColor.YELLOW,
                        bubble = state.bubbles[TeamColor.YELLOW],
                        bubbleOnRight = false,
                        modifier = Modifier
                            .align(Alignment.BottomEnd) // In RTL, BottomEnd = visual Bottom-Left!
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Bottom-Right Corner: BLUE (Pirate - خسرو)
                state.players[TeamColor.BLUE]?.takeIf { it.isActive }?.let { slot ->
                    PlayerCornerBadge(
                        slot = slot,
                        isCurrentTurn = state.currentTurn == TeamColor.BLUE,
                        bubble = state.bubbles[TeamColor.BLUE],
                        bubbleOnRight = true,
                        modifier = Modifier
                            .align(Alignment.BottomStart) // In RTL, BottomStart = visual Bottom-Right!
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom Control Dock: Sticker/Chat Button + 3D Interactive Dice + Action Hint
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = TempleIndigoDark.copy(alpha = 0.88f),
                border = BorderStroke(1.5.dp, TempleGoldBright.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Quick Chat & Stickers trigger button
                    Surface(
                        onClick = { showChatSheet = !showChatSheet },
                        shape = RoundedCornerShape(18.dp),
                        color = TempleIndigoCard,
                        border = BorderStroke(1.5.dp, TempleGoldBright.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("open_chat_sticker_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubble,
                                contentDescription = "پیام و استیکر",
                                tint = TempleGoldBright,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "استیکر",
                                color = TempleCreamText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Center 3D Ivory & Gold Die
                    Dice3DControl(
                        diceValue = state.diceValue,
                        isRolling = state.phase == MatchPhase.ROLLING_DICE,
                        canRoll = canRollNow,
                        accentColor = activeTurnTeam.accent,
                        onRollClick = onRollDice,
                        size = 70.dp
                    )

                    // Right Turn Action Button / Indicator
                    val actionText = when {
                        canRollNow -> "پرتاب تاس"
                        state.phase == MatchPhase.SELECTING_PAWN && state.canLocalDeviceAct() -> "انتخاب مهره"
                        else -> state.currentPlayer()?.name ?: "صبر کنید"
                    }
                    Surface(
                        onClick = { if (canRollNow) onRollDice() },
                        enabled = canRollNow,
                        shape = RoundedCornerShape(18.dp),
                        color = if (canRollNow) activeTurnTeam.tileBottom else TempleIndigoCard.copy(alpha = 0.65f),
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (canRollNow) TempleGoldBright else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("roll_action_pill")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = actionText,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 3. Animated Sticker & Quick-Chat Sheet Overlay
        AnimatedVisibility(
            visible = showChatSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 94.dp)
        ) {
            StickerAndChatSheet(
                onSendPhrase = { phrase ->
                    onSendPhrase(phrase)
                    showChatSheet = false
                },
                onSendSticker = { sticker ->
                    onSendSticker(sticker)
                    showChatSheet = false
                },
                onDismiss = { showChatSheet = false }
            )
        }

        // 4. Victory Podium Overlay
        if (state.phase == MatchPhase.GAME_OVER) {
            VictoryOverlay(
                state = state,
                onPlayAgain = onPlayAgain,
                onBackToMenu = onExitToMenu
            )
        }

        // 5. Exit Confirmation Dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                containerColor = TempleIndigoSurface,
                title = {
                    Text(
                        text = "خروج از بازی؟",
                        color = TempleGoldBright,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Text(
                        text = "آیا مطمئن هستید می‌خواهید این دست بازی را ترک کنید و به منوی اصلی برگردید؟",
                        color = TempleCreamText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            onExitToMenu()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TempleGoldBright,
                            contentColor = TempleIndigoDark
                        ),
                        modifier = Modifier.testTag("confirm_exit_btn")
                    ) {
                        Text("بله، خروج", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("ادامه بازی", color = TempleCreamText)
                    }
                }
            )
        }
    }
}
