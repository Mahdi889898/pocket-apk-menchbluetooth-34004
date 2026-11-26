package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameFeedbackManager
import com.example.bluetooth.BluetoothIncomingEvent
import com.example.bluetooth.BluetoothMenchManager
import com.example.bluetooth.BluetoothPacket
import com.example.bluetooth.BluetoothRole
import com.example.bluetooth.BluetoothUiState
import com.example.data.ProfilePreferences
import com.example.data.UserProfile
import com.example.game.ActiveBubble
import com.example.game.CaptureEvent
import com.example.game.ControllerType
import com.example.game.GameState
import com.example.game.MatchMode
import com.example.game.MatchPhase
import com.example.game.MenschEngine
import com.example.game.MovingPawnAnimation
import com.example.game.PlayerSlot
import com.example.game.StickerType
import com.example.game.TeamColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AppScreen {
    HOME,
    BLUETOOTH_LOBBY,
    GAME_BOARD
}

data class MenchUiState(
    val screen: AppScreen = AppScreen.HOME,
    val profile: UserProfile = UserProfile(),
    val bluetooth: BluetoothUiState = BluetoothUiState(),
    val lobbySlots: Map<TeamColor, PlayerSlot> = emptyMap(),
    val clientAssignedTeam: TeamColor = TeamColor.RED,
    val game: GameState = GameState(),
    val toastMessage: String? = null
)

class MenchViewModel(application: Application) : AndroidViewModel(application) {

    private val profilePrefs = ProfilePreferences(application)
    val feedback = GameFeedbackManager(application)
    val btManager = BluetoothMenchManager(application)

    private val _uiState = MutableStateFlow(MenchUiState())
    val uiState: StateFlow<MenchUiState> = _uiState.asStateFlow()

    private var turnActionJob: Job? = null
    private val bubbleClearJobs = mutableMapOf<TeamColor, Job>()
    private val peerTeamMap = mutableMapOf<String, TeamColor>()
    private var lastHandledCaptureId: Long = 0L

    init {
        viewModelScope.launch {
            profilePrefs.profileFlow.collect { profile ->
                feedback.soundEnabled = profile.soundEnabled
                feedback.vibrationEnabled = profile.vibrationEnabled
                _uiState.update { it.copy(profile = profile) }
            }
        }

        viewModelScope.launch {
            btManager.state.collect { btState ->
                _uiState.update { it.copy(bluetooth = btState) }
            }
        }

        viewModelScope.launch {
            btManager.events.collect { event ->
                handleBluetoothEvent(event)
            }
        }
    }

    // ---- Profile & Settings ----

    fun updateProfile(name: String, frameId: Int, preferredTeamIndex: Int) {
        viewModelScope.launch {
            profilePrefs.updateProfile(name, frameId, preferredTeamIndex)
        }
    }

    fun toggleSound() {
        val next = !_uiState.value.profile.soundEnabled
        viewModelScope.launch { profilePrefs.setSoundEnabled(next) }
    }

    fun toggleVibration() {
        val next = !_uiState.value.profile.vibrationEnabled
        viewModelScope.launch { profilePrefs.setVibrationEnabled(next) }
    }

    fun toggleAutoMoveSingle() {
        val next = !_uiState.value.profile.autoMoveSinglePawn
        viewModelScope.launch { profilePrefs.setAutoMoveSinglePawn(next) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
        viewModelScope.launch {
            delay(2600L)
            _uiState.update { if (it.toastMessage == msg) it.copy(toastMessage = null) else it }
        }
    }

    // ---- Navigation ----

    fun goHome() {
        turnActionJob?.cancel()
        btManager.stopDiscovery()
        btManager.stopAllConnections()
        peerTeamMap.clear()
        _uiState.update { it.copy(screen = AppScreen.HOME) }
    }

    fun openBluetoothLobby() {
        btManager.refreshAdapterState()
        val profile = _uiState.value.profile
        val defaultSlots = buildDefaultHostLobbySlots(profile)
        _uiState.update {
            it.copy(
                screen = AppScreen.BLUETOOTH_LOBBY,
                lobbySlots = defaultSlots
            )
        }
    }

    private fun buildDefaultHostLobbySlots(profile: UserProfile): Map<TeamColor, PlayerSlot> {
        val hostTeam = TeamColor.entries.getOrElse(profile.preferredTeamIndex) { TeamColor.GREEN }
        return TeamColor.entries.associateWith { team ->
            if (team == hostTeam) {
                PlayerSlot(
                    team = team,
                    name = profile.playerName,
                    controller = ControllerType.LOCAL_HUMAN,
                    frameId = profile.frameId
                )
            } else {
                PlayerSlot(
                    team = team,
                    name = team.defaultPlayerName,
                    controller = ControllerType.EMPTY,
                    frameId = team.defaultFrameId
                )
            }
        }
    }

    // ---- Offline & Local Match Launch ----

    fun startOfflineMatch(
        mode: MatchMode,
        playerCount: Int,
        humanTeam: TeamColor
    ) {
        turnActionJob?.cancel()
        btManager.stopAllConnections()
        val profile = _uiState.value.profile

        // Pick active teams symmetrically for 2 players (opposite corners) or 3/4 players
        val activeTeams: List<TeamColor> = when (playerCount.coerceIn(2, 4)) {
            2 -> {
                val opposite = TeamColor.entries[(humanTeam.ordinal + 2) % 4]
                listOf(humanTeam, opposite)
            }
            3 -> listOf(
                humanTeam,
                TeamColor.entries[(humanTeam.ordinal + 1) % 4],
                TeamColor.entries[(humanTeam.ordinal + 2) % 4]
            )
            else -> TeamColor.entries.toList()
        }

        val slots = TeamColor.entries.associateWith { team ->
            when {
                team !in activeTeams -> PlayerSlot(
                    team = team,
                    name = team.defaultPlayerName,
                    controller = ControllerType.EMPTY,
                    frameId = team.defaultFrameId
                )
                team == humanTeam -> PlayerSlot(
                    team = team,
                    name = profile.playerName,
                    controller = ControllerType.LOCAL_HUMAN,
                    frameId = profile.frameId
                )
                mode == MatchMode.PASS_AND_PLAY -> PlayerSlot(
                    team = team,
                    name = team.defaultPlayerName,
                    controller = ControllerType.LOCAL_HUMAN,
                    frameId = team.defaultFrameId
                )
                else -> PlayerSlot(
                    team = team,
                    name = team.defaultPlayerName,
                    controller = ControllerType.BOT,
                    frameId = team.defaultFrameId
                )
            }
        }

        val initialState = MenschEngine.createInitialState(
            mode = mode,
            localTeam = humanTeam,
            slots = slots
        )
        _uiState.update {
            it.copy(
                screen = AppScreen.GAME_BOARD,
                game = initialState
            )
        }
        scheduleBotIfNeeded()
    }

    // ---- Bluetooth Lobby Actions ----

    fun startHostingBluetoothRoom() {
        val profile = _uiState.value.profile
        peerTeamMap.clear()
        val slots = buildDefaultHostLobbySlots(profile)
        _uiState.update { it.copy(lobbySlots = slots) }
        btManager.startHosting()
    }

    fun toggleLobbyBotSlot(team: TeamColor) {
        if (_uiState.value.bluetooth.role != BluetoothRole.HOST && _uiState.value.bluetooth.role != BluetoothRole.NONE) return
        _uiState.update { current ->
            val slot = current.lobbySlots[team] ?: return@update current
            if (slot.controller == ControllerType.LOCAL_HUMAN || slot.controller == ControllerType.BLUETOOTH_REMOTE) {
                return@update current
            }
            val nextCtrl = if (slot.controller == ControllerType.BOT) ControllerType.EMPTY else ControllerType.BOT
            val updated = current.lobbySlots.toMutableMap().apply {
                put(team, slot.copy(controller = nextCtrl, name = team.defaultPlayerName))
            }
            if (current.bluetooth.role == BluetoothRole.HOST) {
                btManager.broadcastFromHost(BluetoothPacket.LobbySync(updated))
            }
            current.copy(lobbySlots = updated)
        }
    }

    fun connectToBluetoothHost(address: String) {
        btManager.connectToHost(address)
    }

    fun startBluetoothMatchAsHost() {
        val current = _uiState.value
        val activeSlots = current.lobbySlots.values.count { it.isActive }
        if (activeSlots < 2) {
            showToast("حداقل ۲ بازیکن (دوست بلوتوثی یا ربات) لازم است")
            return
        }
        val hostTeam = current.lobbySlots.values.firstOrNull {
            it.controller == ControllerType.LOCAL_HUMAN
        }?.team ?: TeamColor.GREEN

        val initial = MenschEngine.createInitialState(
            mode = MatchMode.BLUETOOTH_HOST,
            localTeam = hostTeam,
            slots = current.lobbySlots
        )
        _uiState.update {
            it.copy(
                screen = AppScreen.GAME_BOARD,
                game = initial
            )
        }
        btManager.broadcastFromHost(BluetoothPacket.fromState(initial))
        scheduleBotIfNeeded()
    }

    private fun handleBluetoothEvent(event: BluetoothIncomingEvent) {
        when (event) {
            is BluetoothIncomingEvent.PeerConnected -> {
                val role = _uiState.value.bluetooth.role
                if (role == BluetoothRole.CLIENT) {
                    val p = _uiState.value.profile
                    btManager.sendToHost(BluetoothPacket.Hello(p.playerName, p.frameId))
                }
            }
            is BluetoothIncomingEvent.ConnectionFailed -> {
                showToast(event.reason)
            }
            is BluetoothIncomingEvent.PeerDisconnected -> {
                val role = _uiState.value.bluetooth.role
                if (role == BluetoothRole.HOST) {
                    val team = peerTeamMap.remove(event.address)
                    if (team != null) {
                        handleHostPeerDrop(team)
                    }
                } else if (role == BluetoothRole.CLIENT) {
                    showToast("ارتباط با میزبان بلوتوث قطع شد")
                    if (_uiState.value.screen == AppScreen.GAME_BOARD) {
                        _uiState.update {
                            it.copy(game = it.game.copy(statusBanner = "ارتباط بلوتوث قطع شد"))
                        }
                    }
                }
            }
            is BluetoothIncomingEvent.PacketReceived -> {
                handleIncomingPacket(event.fromAddress, event.packet)
            }
        }
    }

    private fun handleHostPeerDrop(team: TeamColor) {
        val current = _uiState.value
        if (current.screen == AppScreen.BLUETOOTH_LOBBY) {
            val updated = current.lobbySlots.toMutableMap()
            updated[team] = PlayerSlot(
                team = team,
                name = team.defaultPlayerName,
                controller = ControllerType.EMPTY,
                frameId = team.defaultFrameId
            )
            _uiState.update { it.copy(lobbySlots = updated) }
            btManager.broadcastFromHost(BluetoothPacket.LobbySync(updated))
        } else if (current.screen == AppScreen.GAME_BOARD) {
            val players = current.game.players.toMutableMap()
            val dropped = players[team]
            if (dropped != null) {
                players[team] = dropped.copy(
                    controller = ControllerType.BOT,
                    name = "${dropped.name} (ربات)"
                )
                val updatedGame = current.game.copy(
                    players = players,
                    statusBanner = "${dropped.name} خارج شد؛ ربات ادامه می‌دهد"
                )
                _uiState.update { it.copy(game = updatedGame) }
                btManager.broadcastFromHost(BluetoothPacket.fromState(updatedGame))
                scheduleBotIfNeeded()
            }
        }
    }

    private fun handleIncomingPacket(fromAddress: String, packet: BluetoothPacket) {
        val role = _uiState.value.bluetooth.role
        when (packet) {
            is BluetoothPacket.Hello -> {
                if (role != BluetoothRole.HOST) return
                val slots = _uiState.value.lobbySlots.toMutableMap()
                val existingTeam = peerTeamMap[fromAddress]
                val openTeam = existingTeam ?: TeamColor.entries.firstOrNull {
                    val ctrl = slots[it]?.controller
                    ctrl == ControllerType.EMPTY || ctrl == ControllerType.BOT
                }
                if (openTeam == null) return

                peerTeamMap[fromAddress] = openTeam
                slots[openTeam] = PlayerSlot(
                    team = openTeam,
                    name = packet.playerName.take(14).ifBlank { openTeam.defaultPlayerName },
                    controller = ControllerType.BLUETOOTH_REMOTE,
                    frameId = packet.frameId.coerceIn(0, 3),
                    peerAddress = fromAddress
                )
                _uiState.update { it.copy(lobbySlots = slots) }
                btManager.sendToPeer(
                    fromAddress,
                    BluetoothPacket.Welcome(openTeam, _uiState.value.profile.playerName)
                )
                btManager.broadcastFromHost(BluetoothPacket.LobbySync(slots))
                showToast("${packet.playerName} وارد اتاق شد!")
            }

            is BluetoothPacket.Welcome -> {
                if (role != BluetoothRole.CLIENT) return
                _uiState.update { it.copy(clientAssignedTeam = packet.assignedTeam) }
                showToast("به اتاق ${packet.hostName} متصل شدید (${packet.assignedTeam.persianName})")
            }

            is BluetoothPacket.LobbySync -> {
                if (role != BluetoothRole.CLIENT) return
                _uiState.update { it.copy(lobbySlots = packet.slots) }
            }

            is BluetoothPacket.GameSync -> {
                if (role != BluetoothRole.CLIENT) return
                val assigned = _uiState.value.clientAssignedTeam
                val prevState = _uiState.value.game
                val synced = GameState.fromSyncJson(
                    obj = packet.stateJson,
                    localTeam = assigned,
                    previousBubbles = prevState.bubbles
                )
                if (synced.phase == MatchPhase.ROLLING_DICE && prevState.phase != MatchPhase.ROLLING_DICE) {
                    feedback.playDiceRoll()
                }
                val capId = synced.lastCapture?.eventId ?: 0L
                if (capId > lastHandledCaptureId) {
                    lastHandledCaptureId = capId
                    feedback.playCapture()
                }
                if (synced.phase == MatchPhase.GAME_OVER && prevState.phase != MatchPhase.GAME_OVER) {
                    feedback.playVictory()
                }
                _uiState.update {
                    it.copy(
                        screen = AppScreen.GAME_BOARD,
                        game = synced
                    )
                }
            }

            is BluetoothPacket.RollRequest -> {
                if (role != BluetoothRole.HOST) return
                val game = _uiState.value.game
                if (game.currentTurn == packet.team && game.phase == MatchPhase.WAITING_FOR_ROLL) {
                    performAuthoritativeRoll()
                }
            }

            is BluetoothPacket.MoveRequest -> {
                if (role != BluetoothRole.HOST) return
                val game = _uiState.value.game
                if (game.currentTurn == packet.team &&
                    game.phase == MatchPhase.SELECTING_PAWN &&
                    packet.pawnId in game.movablePawnIds
                ) {
                    performAuthoritativeMove(packet.team, packet.pawnId)
                }
            }

            is BluetoothPacket.ChatMessage -> {
                displayBubble(packet.team, text = packet.text, sticker = null)
                if (role == BluetoothRole.HOST) {
                    btManager.broadcastFromHost(packet)
                }
            }

            is BluetoothPacket.StickerMessage -> {
                displayBubble(packet.team, text = null, sticker = packet.sticker)
                if (role == BluetoothRole.HOST) {
                    btManager.broadcastFromHost(packet)
                }
            }

            BluetoothPacket.RematchRequest -> {
                if (role == BluetoothRole.HOST) {
                    restartCurrentMatch()
                }
            }
        }
    }

    // ---- In-Game Actions ----

    fun onRollDicePressed() {
        val game = _uiState.value.game
        if (game.phase != MatchPhase.WAITING_FOR_ROLL) return
        if (!game.canLocalDeviceAct()) return

        if (game.matchMode == MatchMode.BLUETOOTH_CLIENT) {
            btManager.sendToHost(BluetoothPacket.RollRequest(game.localTeam))
            return
        }
        performAuthoritativeRoll()
    }

    fun onPawnTapped(team: TeamColor, pawnId: Int) {
        val game = _uiState.value.game
        if (game.phase != MatchPhase.SELECTING_PAWN) return
        if (team != game.currentTurn || pawnId !in game.movablePawnIds) return
        if (!game.canLocalDeviceAct()) return

        if (game.matchMode == MatchMode.BLUETOOTH_CLIENT) {
            btManager.sendToHost(BluetoothPacket.MoveRequest(team, pawnId))
            return
        }
        performAuthoritativeMove(team, pawnId)
    }

    fun sendQuickChat(phrase: String) {
        val game = _uiState.value.game
        val senderTeam = when (game.matchMode) {
            MatchMode.PASS_AND_PLAY -> game.currentTurn
            else -> game.localTeam
        }
        displayBubble(senderTeam, text = phrase, sticker = null)
        when (game.matchMode) {
            MatchMode.BLUETOOTH_HOST -> btManager.broadcastFromHost(
                BluetoothPacket.ChatMessage(senderTeam, phrase)
            )
            MatchMode.BLUETOOTH_CLIENT -> btManager.sendToHost(
                BluetoothPacket.ChatMessage(senderTeam, phrase)
            )
            else -> Unit
        }
    }

    fun sendSticker(sticker: StickerType) {
        val game = _uiState.value.game
        val senderTeam = when (game.matchMode) {
            MatchMode.PASS_AND_PLAY -> game.currentTurn
            else -> game.localTeam
        }
        displayBubble(senderTeam, text = null, sticker = sticker)
        when (game.matchMode) {
            MatchMode.BLUETOOTH_HOST -> btManager.broadcastFromHost(
                BluetoothPacket.StickerMessage(senderTeam, sticker)
            )
            MatchMode.BLUETOOTH_CLIENT -> btManager.sendToHost(
                BluetoothPacket.StickerMessage(senderTeam, sticker)
            )
            else -> Unit
        }
    }

    fun restartCurrentMatch() {
        val game = _uiState.value.game
        if (game.matchMode == MatchMode.BLUETOOTH_CLIENT) {
            btManager.sendToHost(BluetoothPacket.RematchRequest)
            showToast("درخواست بازی مجدد ارسال شد")
            return
        }
        turnActionJob?.cancel()
        val resetSlots = game.players.mapValues { (_, slot) ->
            slot.copy(captures = 0, sixesRolled = 0, finishRank = null)
        }
        val fresh = MenschEngine.createInitialState(
            mode = game.matchMode,
            localTeam = game.localTeam,
            slots = resetSlots
        )
        _uiState.update { it.copy(game = fresh) }
        syncIfHost()
        scheduleBotIfNeeded()
    }

    // ---- Authoritative Game Loop (Offline & Bluetooth Host) ----

    private fun performAuthoritativeRoll() {
        turnActionJob?.cancel()
        turnActionJob = viewModelScope.launch {
            feedback.playDiceRoll()
            _uiState.update {
                it.copy(
                    game = it.game.copy(
                        phase = MatchPhase.ROLLING_DICE,
                        movablePawnIds = emptyList(),
                        statusBanner = "در حال چرخش تاس..."
                    )
                )
            }
            syncIfHost()

            // Animated dice tumble faces
            repeat(6) {
                delay(65L)
                _uiState.update {
                    it.copy(game = it.game.copy(diceValue = Random.nextInt(1, 7)))
                }
            }

            val current = _uiState.value.game
            val turnTeam = current.currentTurn
            val rolled = Random.nextInt(1, 7)

            val updatedPlayers = current.players.toMutableMap()
            updatedPlayers[turnTeam]?.let { slot ->
                if (rolled == 6) {
                    updatedPlayers[turnTeam] = slot.copy(sixesRolled = slot.sixesRolled + 1)
                }
            }

            val nextSixStreak = if (rolled == 6) current.consecutiveSixes + 1 else 0
            if (rolled == 6) feedback.playSixRolled()

            // 3 consecutive sixes penalty (standard fair Mensch rule)
            if (nextSixStreak >= 3) {
                val nextTeam = MenschEngine.nextActiveTurn(current, turnTeam)
                val nextName = updatedPlayers[nextTeam]?.name ?: nextTeam.defaultPlayerName
                _uiState.update {
                    it.copy(
                        game = it.game.copy(
                            diceValue = rolled,
                            players = updatedPlayers,
                            consecutiveSixes = 0,
                            statusBanner = "سه شیش پیاپی! نوبت $nextName"
                        )
                    )
                }
                syncIfHost()
                delay(900L)
                advanceTurnTo(nextTeam)
                return@launch
            }

            val movable = MenschEngine.getMovablePawnIds(current, turnTeam, rolled)
            if (movable.isEmpty()) {
                val nextTeam = MenschEngine.nextActiveTurn(current, turnTeam)
                val turnName = updatedPlayers[turnTeam]?.name ?: turnTeam.defaultPlayerName
                _uiState.update {
                    it.copy(
                        game = it.game.copy(
                            diceValue = rolled,
                            players = updatedPlayers,
                            consecutiveSixes = 0,
                            movablePawnIds = emptyList(),
                            statusBanner = "$turnName حرکتی ندارد!"
                        )
                    )
                }
                syncIfHost()
                delay(900L)
                advanceTurnTo(nextTeam)
                return@launch
            }

            val turnSlot = updatedPlayers[turnTeam]
            val turnName = turnSlot?.name ?: turnTeam.defaultPlayerName
            _uiState.update {
                it.copy(
                    game = it.game.copy(
                        diceValue = rolled,
                        players = updatedPlayers,
                        consecutiveSixes = nextSixStreak,
                        movablePawnIds = movable,
                        phase = MatchPhase.SELECTING_PAWN,
                        statusBanner = if (turnSlot?.controller == ControllerType.BOT) {
                            "$turnName در حال انتخاب مهره..."
                        } else {
                            "تاس $rolled آمد! یک مهره را انتخاب کنید"
                        }
                    )
                )
            }
            syncIfHost()

            // Bot auto-selects best tactical pawn
            if (turnSlot?.controller == ControllerType.BOT) {
                delay(520L)
                val bestPawn = MenschEngine.chooseBestBotMove(_uiState.value.game, turnTeam, rolled)
                if (bestPawn != null) {
                    performAuthoritativeMove(turnTeam, bestPawn)
                }
            } else if (_uiState.value.profile.autoMoveSinglePawn &&
                movable.size == 1 &&
                _uiState.value.game.canLocalDeviceAct()
            ) {
                delay(320L)
                performAuthoritativeMove(turnTeam, movable.first())
            }
        }
    }

    private fun performAuthoritativeMove(team: TeamColor, pawnId: Int) {
        turnActionJob?.cancel()
        turnActionJob = viewModelScope.launch {
            val beforeState = _uiState.value.game
            val dice = beforeState.diceValue
            val outcome = MenschEngine.executeMove(beforeState, team, pawnId, dice) ?: return@launch

            _uiState.update {
                it.copy(
                    game = it.game.copy(
                        phase = MatchPhase.ANIMATING_MOVE,
                        movablePawnIds = emptyList()
                    )
                )
            }

            // Smooth tile-by-tile hopping animation
            val steps = outcome.stepPath
            for (i in 0 until steps.size - 1) {
                val from = steps[i]
                val to = steps[i + 1]
                feedback.playPawnHop(i)
                val frames = 7
                for (f in 1..frames) {
                    val fraction = f.toFloat() / frames.toFloat()
                    _uiState.update {
                        it.copy(
                            game = it.game.copy(
                                movingPawn = MovingPawnAnimation(
                                    team = team,
                                    pawnId = pawnId,
                                    fromCell = from,
                                    toCell = to,
                                    stepFraction = fraction
                                )
                            )
                        )
                    }
                    delay(20L)
                }
            }

            val updatedPlayers = _uiState.value.game.players.toMutableMap()
            var updatedWinners = _uiState.value.game.winnerOrder
            var captureEvent: CaptureEvent? = _uiState.value.game.lastCapture

            if (outcome.capturedPawn != null && outcome.captureCell != null) {
                feedback.playCapture()
                updatedPlayers[team]?.let { attacker ->
                    updatedPlayers[team] = attacker.copy(captures = attacker.captures + 1)
                }
                captureEvent = CaptureEvent(
                    cell = outcome.captureCell,
                    victimTeam = outcome.capturedPawn.team,
                    attackerTeam = team
                )
                // Playful bot reaction when knocked out!
                val victimSlot = updatedPlayers[outcome.capturedPawn.team]
                if (victimSlot?.controller == ControllerType.BOT) {
                    if (Random.nextBoolean()) {
                        displayBubble(outcome.capturedPawn.team, text = "مهربون باش", sticker = null)
                    } else {
                        displayBubble(outcome.capturedPawn.team, text = null, sticker = StickerType.POUTING_BUNNY)
                    }
                }
            }

            if (outcome.playerFinished && team !in updatedWinners) {
                updatedWinners = updatedWinners + team
                updatedPlayers[team]?.let { finishedSlot ->
                    updatedPlayers[team] = finishedSlot.copy(finishRank = updatedWinners.size)
                }
                feedback.playVictory()
            }

            val postMoveState = _uiState.value.game.copy(
                pawns = outcome.newPawns,
                players = updatedPlayers,
                winnerOrder = updatedWinners,
                lastCapture = captureEvent,
                movingPawn = null
            )

            // Check if match is finished (when 1 or 0 active unfinished players remain)
            val remaining = MenschEngine.countRemainingActivePlayers(postMoveState)
            if (remaining <= 1) {
                // Add the last remaining player to final rank list
                val lastUnfinished = postMoveState.players.values.firstOrNull { it.isActive && !it.isFinished }
                val finalWinners = if (lastUnfinished != null && lastUnfinished.team !in updatedWinners) {
                    updatedWinners + lastUnfinished.team
                } else {
                    updatedWinners
                }
                val finalPlayers = updatedPlayers.toMutableMap()
                if (lastUnfinished != null) {
                    finalPlayers[lastUnfinished.team] = lastUnfinished.copy(finishRank = finalWinners.size)
                }
                val championName = finalWinners.firstOrNull()?.let { finalPlayers[it]?.name } ?: "قهرمان"
                _uiState.update {
                    it.copy(
                        game = postMoveState.copy(
                            players = finalPlayers,
                            winnerOrder = finalWinners,
                            phase = MatchPhase.GAME_OVER,
                            statusBanner = "$championName قهرمان معبد شد!"
                        )
                    )
                }
                syncIfHost()
                return@launch
            }

            // Decide next turn
            val nextTeam = if (outcome.bonusTurn) team else MenschEngine.nextActiveTurn(postMoveState, team)
            val nextName = updatedPlayers[nextTeam]?.name ?: nextTeam.defaultPlayerName
            val banner = when {
                outcome.capturedPawn != null -> "ایول! جایزه زدن مهره برای $nextName"
                outcome.bonusTurn -> "جایزه تاس ۶! دوباره نوبت $nextName"
                outcome.enteredHome -> "آفرین! مهره وارد خانه امن شد"
                else -> "نوبت $nextName است"
            }

            _uiState.update {
                it.copy(
                    game = postMoveState.copy(
                        currentTurn = nextTeam,
                        phase = MatchPhase.WAITING_FOR_ROLL,
                        consecutiveSixes = if (nextTeam == team) postMoveState.consecutiveSixes else 0,
                        statusBanner = banner
                    )
                )
            }
            syncIfHost()
            scheduleBotIfNeeded()
        }
    }

    private fun advanceTurnTo(nextTeam: TeamColor) {
        val current = _uiState.value.game
        val nextName = current.players[nextTeam]?.name ?: nextTeam.defaultPlayerName
        _uiState.update {
            it.copy(
                game = it.game.copy(
                    currentTurn = nextTeam,
                    phase = MatchPhase.WAITING_FOR_ROLL,
                    movablePawnIds = emptyList(),
                    consecutiveSixes = 0,
                    statusBanner = "نوبت $nextName است"
                )
            )
        }
        syncIfHost()
        scheduleBotIfNeeded()
    }

    private fun scheduleBotIfNeeded() {
        val game = _uiState.value.game
        if (game.phase != MatchPhase.WAITING_FOR_ROLL) return
        if (game.matchMode == MatchMode.BLUETOOTH_CLIENT) return
        val currentSlot = game.currentPlayer() ?: return
        if (currentSlot.controller != ControllerType.BOT) return

        turnActionJob?.cancel()
        turnActionJob = viewModelScope.launch {
            delay(650L)
            if (_uiState.value.screen == AppScreen.GAME_BOARD &&
                _uiState.value.game.phase == MatchPhase.WAITING_FOR_ROLL &&
                _uiState.value.game.currentTurn == currentSlot.team
            ) {
                performAuthoritativeRoll()
            }
        }
    }

    private fun displayBubble(team: TeamColor, text: String?, sticker: StickerType?) {
        feedback.playStickerPop()
        val bubble = ActiveBubble(team = team, text = text, sticker = sticker)
        _uiState.update { current ->
            current.copy(
                game = current.game.copy(
                    bubbles = current.game.bubbles + (team to bubble)
                )
            )
        }
        bubbleClearJobs[team]?.cancel()
        bubbleClearJobs[team] = viewModelScope.launch {
            delay(3200L)
            _uiState.update { current ->
                current.copy(
                    game = current.game.copy(
                        bubbles = current.game.bubbles - team
                    )
                )
            }
        }
    }

    private fun syncIfHost() {
        if (_uiState.value.game.matchMode == MatchMode.BLUETOOTH_HOST) {
            btManager.broadcastFromHost(BluetoothPacket.fromState(_uiState.value.game))
        }
    }

    override fun onCleared() {
        super.onCleared()
        turnActionJob?.cancel()
        btManager.release()
    }
}
