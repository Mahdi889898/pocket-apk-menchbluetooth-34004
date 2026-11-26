package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.UserProfile
import com.example.game.GameState
import com.example.game.MatchMode
import com.example.game.TeamColor
import com.example.ui.components.OrnateAvatarFrameCanvas
import com.example.ui.theme.TeamGreenBottom
import com.example.ui.theme.TempleCreamText
import com.example.ui.theme.TempleGoldBright
import com.example.ui.theme.TempleIndigoCard
import com.example.ui.theme.TempleIndigoDark
import com.example.ui.theme.TempleIndigoSurface
import com.example.ui.theme.TempleMutedLavender

@Composable
fun MatchSetupDialog(
    mode: MatchMode,
    defaultTeam: TeamColor,
    onStartMatch: (playerCount: Int, chosenTeam: TeamColor) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCount by remember { mutableIntStateOf(4) }
    var selectedTeam by remember { mutableStateOf(defaultTeam) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = TempleIndigoSurface,
            border = BorderStroke(2.dp, TempleGoldBright),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (mode == MatchMode.VS_BOTS) "تنظیم بازی با ربات" else "بازی چندنفره روی یک گوشی",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TempleGoldBright
                )

                Text(
                    text = "تعداد بازیکنان:",
                    style = MaterialTheme.typography.titleMedium,
                    color = TempleCreamText,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(2, 3, 4).forEach { count ->
                        val active = selectedCount == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (active) TempleGoldBright else TempleIndigoCard
                                )
                                .border(
                                    1.5.dp,
                                    if (active) Color.White else TempleGoldBright.copy(alpha = 0.35f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedCount = count }
                                .testTag("setup_count_$count"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count نفره",
                                color = if (active) TempleIndigoDark else TempleCreamText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Text(
                    text = "شخصیت و رنگ خود را انتخاب کنید:",
                    style = MaterialTheme.typography.titleMedium,
                    color = TempleCreamText,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TeamColor.entries.forEach { team ->
                        val active = selectedTeam == team
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (active) team.tileBottom.copy(alpha = 0.4f) else TempleIndigoDark.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (active) 2.5.dp else 1.dp,
                                    color = if (active) TempleGoldBright else Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedTeam = team }
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                                .testTag("setup_team_${team.name.lowercase()}")
                        ) {
                            Image(
                                painter = painterResource(id = team.pawnDrawable),
                                contentDescription = team.heroName,
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = team.persianName,
                                style = MaterialTheme.typography.labelMedium,
                                color = TempleCreamText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, TempleMutedLavender),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("انصراف", color = TempleCreamText)
                    }
                    Button(
                        onClick = { onStartMatch(selectedCount, selectedTeam) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TeamGreenBottom,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                            .testTag("setup_start_btn")
                    ) {
                        Text("شروع بازی!", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSettingsDialog(
    profile: UserProfile,
    onSaveProfile: (name: String, frameId: Int, preferredTeam: Int) -> Unit,
    onToggleVibration: () -> Unit,
    onToggleAutoMove: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(profile.playerName) { mutableStateOf(profile.playerName) }
    var selectedFrame by remember(profile.frameId) { mutableIntStateOf(profile.frameId) }
    var selectedTeamIdx by remember(profile.preferredTeamIndex) { mutableIntStateOf(profile.preferredTeamIndex) }

    val frameNames = listOf("زمرد طلایی", "گوش خرگوشی", "حلقه طلایی", "تاج یاقوتی")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = TempleIndigoSurface,
            border = BorderStroke(2.dp, TempleGoldBright),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "پروفایل و تنظیمات",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TempleGoldBright
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 14) nameInput = it },
                    label = { Text("نام بازیکن") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TempleGoldBright,
                        unfocusedBorderColor = TempleMutedLavender.copy(alpha = 0.6f),
                        focusedTextColor = TempleCreamText,
                        unfocusedTextColor = TempleCreamText,
                        focusedLabelColor = TempleGoldBright,
                        unfocusedLabelColor = TempleMutedLavender
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                Text(
                    text = "انتخاب قاب آواتار:",
                    style = MaterialTheme.typography.titleMedium,
                    color = TempleCreamText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val previewTeam = TeamColor.entries.getOrElse(selectedTeamIdx) { TeamColor.GREEN }
                    for (fid in 0..3) {
                        val active = selectedFrame == fid
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) TempleIndigoCard else Color.Transparent)
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) TempleGoldBright else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedFrame = fid }
                                .padding(6.dp)
                                .testTag("frame_choice_$fid")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                                Image(
                                    painter = painterResource(id = previewTeam.pawnDrawable),
                                    contentDescription = frameNames[fid],
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                )
                                OrnateAvatarFrameCanvas(frameId = fid, isActive = false, size = 56.dp)
                            }
                            Text(
                                text = frameNames[fid],
                                style = MaterialTheme.typography.labelSmall,
                                color = TempleCreamText
                            )
                        }
                    }
                }

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("لرزش هنگام پرتاب تاس و زدن مهره", color = TempleCreamText, fontSize = 13.sp)
                    Switch(
                        checked = profile.vibrationEnabled,
                        onCheckedChange = { onToggleVibration() },
                        colors = SwitchDefaults.colors(checkedThumbColor = TempleIndigoDark, checkedTrackColor = TempleGoldBright)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حرکت خودکار هنگام داشتن ۱ انتخاب", color = TempleCreamText, fontSize = 13.sp)
                    Switch(
                        checked = profile.autoMoveSinglePawn,
                        onCheckedChange = { onToggleAutoMove() },
                        colors = SwitchDefaults.colors(checkedThumbColor = TempleIndigoDark, checkedTrackColor = TempleGoldBright)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, TempleMutedLavender),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("بستن", color = TempleCreamText)
                    }
                    Button(
                        onClick = {
                            onSaveProfile(nameInput, selectedFrame, selectedTeamIdx)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TempleGoldBright,
                            contentColor = TempleIndigoDark
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("save_profile_btn")
                    ) {
                        Text("ذخیره", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun VictoryOverlay(
    state: GameState,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val championTeam = state.winnerOrder.firstOrNull() ?: state.currentTurn
    val championSlot = state.players[championTeam]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC8120826))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = TempleIndigoSurface,
            border = BorderStroke(3.dp, TempleGoldBright),
            shadowElevation = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = "قهرمان",
                    tint = TempleGoldBright,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = "قهرمان معبد!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TempleGoldBright
                )

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(championTeam.tileTop.copy(alpha = 0.6f), TempleIndigoDark)
                            )
                        )
                        .border(3.dp, TempleGoldBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = championTeam.pawnDrawable),
                        contentDescription = championSlot?.name,
                        modifier = Modifier.size(92.dp)
                    )
                }

                Text(
                    text = championSlot?.name ?: championTeam.defaultPlayerName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TempleCreamText
                )

                // Standings list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.winnerOrder.forEachIndexed { idx, team ->
                        val slot = state.players[team] ?: return@forEachIndexed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TempleIndigoCard)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    color = TempleGoldBright,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = slot.name,
                                    color = TempleCreamText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "زده: ${slot.captures} • شیش: ${slot.sixesRolled}",
                                color = TempleMutedLavender,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToMenu,
                        border = BorderStroke(1.5.dp, TempleMutedLavender),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("victory_home_btn")
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null, tint = TempleCreamText)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("منو", color = TempleCreamText, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TeamGreenBottom,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("victory_rematch_btn")
                    ) {
                        Icon(Icons.Rounded.Replay, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بازی مجدد", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
