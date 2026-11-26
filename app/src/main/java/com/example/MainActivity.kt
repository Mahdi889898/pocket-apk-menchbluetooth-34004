package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.MatchMode
import com.example.game.TeamColor
import com.example.ui.AppScreen
import com.example.ui.MenchViewModel
import com.example.ui.screens.BluetoothLobbyScreen
import com.example.ui.screens.GameBoardScreen
import com.example.ui.screens.HomeMenuScreen
import com.example.ui.screens.MatchSetupDialog
import com.example.ui.screens.ProfileSettingsDialog
import com.example.ui.theme.MenchBluetoothTheme
import com.example.ui.theme.TempleCreamText
import com.example.ui.theme.TempleGoldBright
import com.example.ui.theme.TempleIndigoSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MenchBluetoothTheme {
                MenchBluetoothApp()
            }
        }
    }
}

@Composable
fun MenchBluetoothApp(
    viewModel: MenchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingOfflineMode by remember { mutableStateOf<MatchMode?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.screen) {
            AppScreen.HOME -> {
                HomeMenuScreen(
                    profile = uiState.profile,
                    onBluetoothPlayClick = { viewModel.openBluetoothLobby() },
                    onPlayVsBotsClick = { pendingOfflineMode = MatchMode.VS_BOTS },
                    onPassAndPlayClick = { pendingOfflineMode = MatchMode.PASS_AND_PLAY },
                    onEditProfileClick = { showProfileDialog = true },
                    onToggleSound = { viewModel.toggleSound() }
                )
            }

            AppScreen.BLUETOOTH_LOBBY -> {
                BluetoothLobbyScreen(
                    btState = uiState.bluetooth,
                    btManager = viewModel.btManager,
                    lobbySlots = uiState.lobbySlots,
                    onStartHostRoom = { viewModel.startHostingBluetoothRoom() },
                    onToggleBotSlot = { viewModel.toggleLobbyBotSlot(it) },
                    onConnectToDevice = { viewModel.connectToBluetoothHost(it) },
                    onStartBluetoothMatch = { viewModel.startBluetoothMatchAsHost() },
                    onBack = { viewModel.goHome() }
                )
            }

            AppScreen.GAME_BOARD -> {
                GameBoardScreen(
                    state = uiState.game,
                    soundEnabled = uiState.profile.soundEnabled,
                    onRollDice = { viewModel.onRollDicePressed() },
                    onPawnTapped = { team, pawnId -> viewModel.onPawnTapped(team, pawnId) },
                    onSendPhrase = { viewModel.sendQuickChat(it) },
                    onSendSticker = { viewModel.sendSticker(it) },
                    onToggleSound = { viewModel.toggleSound() },
                    onPlayAgain = { viewModel.restartCurrentMatch() },
                    onExitToMenu = { viewModel.goHome() }
                )
            }
        }

        // Offline setup modal
        pendingOfflineMode?.let { mode ->
            val defaultTeam = TeamColor.entries.getOrElse(uiState.profile.preferredTeamIndex) { TeamColor.GREEN }
            MatchSetupDialog(
                mode = mode,
                defaultTeam = defaultTeam,
                onStartMatch = { count, chosenTeam ->
                    pendingOfflineMode = null
                    viewModel.startOfflineMatch(mode = mode, playerCount = count, humanTeam = chosenTeam)
                },
                onDismiss = { pendingOfflineMode = null }
            )
        }

        // Profile & Ornate Frame picker modal
        if (showProfileDialog) {
            ProfileSettingsDialog(
                profile = uiState.profile,
                onSaveProfile = { name, frameId, teamIdx ->
                    viewModel.updateProfile(name, frameId, teamIdx)
                },
                onToggleVibration = { viewModel.toggleVibration() },
                onToggleAutoMove = { viewModel.toggleAutoMoveSingle() },
                onDismiss = { showProfileDialog = false }
            )
        }

        // Floating toast pill
        AnimatedVisibility(
            visible = uiState.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp, start = 20.dp, end = 20.dp)
        ) {
            uiState.toastMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = TempleIndigoSurface.copy(alpha = 0.96f),
                    border = BorderStroke(1.5.dp, TempleGoldBright),
                    shadowElevation = 10.dp
                ) {
                    Text(
                        text = msg,
                        color = TempleCreamText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
