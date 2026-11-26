package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.bluetooth.BluetoothMenchManager
import com.example.bluetooth.BluetoothRole
import com.example.bluetooth.BluetoothUiState
import com.example.bluetooth.DiscoveredBtDevice
import com.example.game.ControllerType
import com.example.game.PlayerSlot
import com.example.game.TeamColor
import com.example.ui.theme.BluetoothBlue
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TeamGreenBottom
import com.example.ui.theme.TeamGreenSide
import com.example.ui.theme.TeamGreenTop
import com.example.ui.theme.TempleCreamText
import com.example.ui.theme.TempleGoldBright
import com.example.ui.theme.TempleIndigoCard
import com.example.ui.theme.TempleIndigoDark
import com.example.ui.theme.TempleIndigoSurface
import com.example.ui.theme.TempleMutedLavender

@Composable
fun BluetoothLobbyScreen(
    btState: BluetoothUiState,
    btManager: BluetoothMenchManager,
    lobbySlots: Map<TeamColor, PlayerSlot>,
    onStartHostRoom: () -> Unit,
    onToggleBotSlot: (TeamColor) -> Unit,
    onConnectToDevice: (String) -> Unit,
    onStartBluetoothMatch: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Host, 1 = Join
    var permissionsGranted by remember { mutableStateOf(btManager.hasAllPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it } || btManager.hasAllPermissions()
        if (permissionsGranted) {
            btManager.refreshAdapterState()
            btManager.loadPairedDevices()
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        btManager.refreshAdapterState()
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        btManager.refreshAdapterState()
    }

    LaunchedEffect(Unit) {
        permissionsGranted = btManager.hasAllPermissions()
        if (permissionsGranted) {
            btManager.refreshAdapterState()
            btManager.loadPairedDevices()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_temple_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TempleIndigoDark.copy(alpha = 0.84f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TempleIndigoCard)
                        .testTag("bt_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = TempleGoldBright
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "اتاق بازی بلوتوثی",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TempleGoldBright
                    )
                    Text(
                        text = btState.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TempleMutedLavender
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hardware & Permission Check Banner
                if (!btState.isSupported) {
                    InfoBannerCard(
                        title = "بلوتوث در دسترس نیست",
                        body = "این دستگاه فاقد سخت‌افزار بلوتوث است. می‌توانید از منوی اصلی با ربات یا روی یک گوشی بازی کنید.",
                        accent = CoralDanger
                    )
                } else if (!permissionsGranted) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = TempleIndigoSurface,
                        border = BorderStroke(1.5.dp, TempleGoldBright.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = BluetoothBlue,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = "برای پیدا کردن و اتصال به گوشی دوستان، مجوز بلوتوث لازم است.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TempleCreamText
                            )
                            Button(
                                onClick = { permissionLauncher.launch(btManager.requiredPermissions()) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BluetoothBlue,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("bt_grant_permission_btn")
                            ) {
                                Text("تایید دسترسی بلوتوث", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (!btState.isEnabled) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = TempleIndigoSurface,
                        border = BorderStroke(1.5.dp, BluetoothBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "بلوتوث گوشی خاموش است",
                                style = MaterialTheme.typography.titleLarge,
                                color = TempleGoldBright
                            )
                            Button(
                                onClick = {
                                    enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BluetoothBlue,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("bt_enable_btn")
                            ) {
                                Text("روشن کردن بلوتوث", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Role Tabs (Host vs Join)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(TempleIndigoSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LobbyTabButton(
                        title = "ساخت اتاق (میزبان)",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_bt_host")
                    )
                    LobbyTabButton(
                        title = "ورود به اتاق (جستجو)",
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            if (permissionsGranted) btManager.loadPairedDevices()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_bt_join")
                    )
                }

                if (selectedTab == 0) {
                    // HOST VIEW
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = TempleIndigoSurface.copy(alpha = 0.94f),
                        border = BorderStroke(1.5.dp, TempleGoldBright.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!permissionsGranted) {
                                            permissionLauncher.launch(btManager.requiredPermissions())
                                        } else {
                                            onStartHostRoom()
                                        }
                                    },
                                    enabled = btState.isSupported && btState.isEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (btState.isHosting) EmeraldSuccess else BluetoothBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("bt_create_room_btn")
                                ) {
                                    Icon(Icons.Rounded.BluetoothConnected, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (btState.isHosting) "اتاق باز است" else "باز کردن اتاق",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                        }
                                        discoverableLauncher.launch(intent)
                                    },
                                    enabled = btState.isSupported && btState.isEnabled && permissionsGranted,
                                    border = BorderStroke(1.5.dp, TempleGoldBright),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("bt_discoverable_btn")
                                ) {
                                    Icon(
                                        Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        tint = TempleGoldBright
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("قابل مشاهده", color = TempleGoldBright, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "بازیکنان اتاق (روی جایگاه خالی بزنید تا ربات اضافه/حذف شود):",
                                style = MaterialTheme.typography.labelLarge,
                                color = TempleMutedLavender
                            )

                            TeamColor.entries.forEach { team ->
                                val slot = lobbySlots[team]
                                if (slot != null) {
                                    LobbyPlayerSlotCard(
                                        slot = slot,
                                        canToggleBot = slot.controller == ControllerType.EMPTY || slot.controller == ControllerType.BOT,
                                        onToggleBot = { onToggleBotSlot(team) }
                                    )
                                }
                            }
                        }
                    }

                    GlossyGameButton(
                        title = "شروع بازی منچ",
                        subtitle = "همه بازیکنان متصل همزمان وارد معبد می‌شوند",
                        icon = Icons.Rounded.PlayArrow,
                        topColor = TeamGreenTop,
                        bottomColor = TeamGreenBottom,
                        rimColor = TeamGreenSide,
                        onClick = onStartBluetoothMatch,
                        testTag = "bt_start_match_btn"
                    )
                } else {
                    // JOIN VIEW
                    if (btState.isConnectedToHost) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = TempleIndigoSurface,
                            border = BorderStroke(2.dp, EmeraldSuccess),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.BluetoothConnected,
                                        contentDescription = null,
                                        tint = EmeraldSuccess
                                    )
                                    Text(
                                        text = "متصل شدید! منتظر شروع بازی توسط میزبان...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldSuccess
                                    )
                                }
                                TeamColor.entries.forEach { team ->
                                    lobbySlots[team]?.let { slot ->
                                        LobbyPlayerSlotCard(
                                            slot = slot,
                                            canToggleBot = false,
                                            onToggleBot = {}
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = TempleIndigoSurface.copy(alpha = 0.94f),
                            border = BorderStroke(1.5.dp, BluetoothBlue.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
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
                                        text = "انتخاب گوشی میزبان",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TempleGoldBright
                                    )
                                    Button(
                                        onClick = {
                                            if (!permissionsGranted) {
                                                permissionLauncher.launch(btManager.requiredPermissions())
                                            } else if (btState.isScanning) {
                                                btManager.stopDiscovery()
                                            } else {
                                                btManager.startDiscovery()
                                            }
                                        },
                                        enabled = btState.isSupported && btState.isEnabled,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BluetoothBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("bt_scan_btn")
                                    ) {
                                        if (btState.isScanning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                        } else {
                                            Icon(Icons.Rounded.Radar, contentDescription = null)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (btState.isScanning) "توقف جستجو" else "جستجو",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (btState.isConnecting) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(color = TempleGoldBright, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("در حال اتصال به اتاق میزبان...", color = TempleCreamText)
                                    }
                                }

                                if (btState.pairedDevices.isNotEmpty()) {
                                    Text(
                                        text = "دستگاه‌های جفت‌شده (Paired)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TempleMutedLavender
                                    )
                                    btState.pairedDevices.forEach { device ->
                                        BluetoothDeviceRow(
                                            device = device,
                                            onClick = { onConnectToDevice(device.address) }
                                        )
                                    }
                                }

                                if (btState.discoveredDevices.isNotEmpty()) {
                                    Text(
                                        text = "دستگاه‌های پیدا شده اطراف",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = BluetoothBlue
                                    )
                                    btState.discoveredDevices.forEach { device ->
                                        BluetoothDeviceRow(
                                            device = device,
                                            onClick = { onConnectToDevice(device.address) }
                                        )
                                    }
                                }

                                if (btState.pairedDevices.isEmpty() && btState.discoveredDevices.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 18.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.BluetoothSearching,
                                            contentDescription = null,
                                            tint = TempleMutedLavender
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "برای یافتن گوشی میزبان دکمه «جستجو» را بزنید",
                                            color = TempleMutedLavender
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(TempleGoldBright, Color(0xFFF5A600)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) TempleIndigoDark else TempleCreamText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LobbyPlayerSlotCard(
    slot: PlayerSlot,
    canToggleBot: Boolean,
    onToggleBot: () -> Unit
) {
    val statusLabel = when (slot.controller) {
        ControllerType.LOCAL_HUMAN -> "میزبان (شما)"
        ControllerType.BLUETOOTH_REMOTE -> "دوست بلوتوثی متصل"
        ControllerType.BOT -> "ربات هوشمند (برای حذف بزنید)"
        ControllerType.EMPTY -> "جایگاه خالی (برای ربات بزنید)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (slot.isActive) TempleIndigoCard else TempleIndigoDark.copy(alpha = 0.65f)
            )
            .border(
                width = 1.5.dp,
                color = if (slot.isActive) slot.team.accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = canToggleBot, onClick = onToggleBot)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("lobby_slot_${slot.team.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(slot.team.tileBottom.copy(alpha = 0.35f))
                .border(2.dp, slot.team.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = slot.team.pawnDrawable),
                contentDescription = slot.team.heroName,
                modifier = Modifier.size(42.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (slot.isActive) "${slot.name} (${slot.team.persianName})" else "تیم ${slot.team.persianName} • ${slot.team.heroName}",
                style = MaterialTheme.typography.titleMedium,
                color = if (slot.isActive) TempleCreamText else TempleMutedLavender
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = when (slot.controller) {
                    ControllerType.BLUETOOTH_REMOTE -> EmeraldSuccess
                    ControllerType.LOCAL_HUMAN -> TempleGoldBright
                    ControllerType.BOT -> BluetoothBlue
                    ControllerType.EMPTY -> TempleMutedLavender.copy(alpha = 0.7f)
                }
            )
        }

        if (slot.controller == ControllerType.BOT) {
            Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = BluetoothBlue)
        } else if (slot.controller == ControllerType.BLUETOOTH_REMOTE) {
            Icon(Icons.Rounded.BluetoothConnected, contentDescription = null, tint = EmeraldSuccess)
        } else if (canToggleBot) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = TempleMutedLavender)
        }
    }
}

@Composable
private fun BluetoothDeviceRow(
    device: DiscoveredBtDevice,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TempleIndigoCard)
            .border(1.dp, BluetoothBlue.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Bluetooth,
            contentDescription = null,
            tint = BluetoothBlue,
            modifier = Modifier.size(26.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                color = TempleCreamText
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.labelSmall,
                color = TempleMutedLavender
            )
        }
        Text(
            text = "اتصال",
            color = TempleGoldBright,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun InfoBannerCard(
    title: String,
    body: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = TempleIndigoSurface,
        border = BorderStroke(1.5.dp, accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = TempleCreamText)
        }
    }
}
