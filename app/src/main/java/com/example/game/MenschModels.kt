package com.example.game

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.TeamBlueAccent
import com.example.ui.theme.TeamBlueBottom
import com.example.ui.theme.TeamBlueSide
import com.example.ui.theme.TeamBlueTop
import com.example.ui.theme.TeamGreenAccent
import com.example.ui.theme.TeamGreenBottom
import com.example.ui.theme.TeamGreenSide
import com.example.ui.theme.TeamGreenTop
import com.example.ui.theme.TeamRedAccent
import com.example.ui.theme.TeamRedBottom
import com.example.ui.theme.TeamRedSide
import com.example.ui.theme.TeamRedTop
import com.example.ui.theme.TeamYellowAccent
import com.example.ui.theme.TeamYellowBottom
import com.example.ui.theme.TeamYellowSide
import com.example.ui.theme.TeamYellowTop
import org.json.JSONArray
import org.json.JSONObject

enum class TeamColor(
    val persianName: String,
    val heroName: String,
    val defaultPlayerName: String,
    val defaultFrameId: Int,
    @DrawableRes val pawnDrawable: Int,
    val tileTop: Color,
    val tileBottom: Color,
    val tileSide: Color,
    val accent: Color
) {
    GREEN(
        persianName = "سبز",
        heroName = "خرس مهربون",
        defaultPlayerName = "یاسر",
        defaultFrameId = 0,
        pawnDrawable = R.drawable.img_pawn_bear,
        tileTop = TeamGreenTop,
        tileBottom = TeamGreenBottom,
        tileSide = TeamGreenSide,
        accent = TeamGreenAccent
    ),
    RED(
        persianName = "قرمز",
        heroName = "پهلوان معبد",
        defaultPlayerName = "سروی",
        defaultFrameId = 1,
        pawnDrawable = R.drawable.img_pawn_strongman,
        tileTop = TeamRedTop,
        tileBottom = TeamRedBottom,
        tileSide = TeamRedSide,
        accent = TeamRedAccent
    ),
    BLUE(
        persianName = "آبی",
        heroName = "دزد دریایی",
        defaultPlayerName = "خسرو",
        defaultFrameId = 2,
        pawnDrawable = R.drawable.img_pawn_pirate,
        tileTop = TeamBlueTop,
        tileBottom = TeamBlueBottom,
        tileSide = TeamBlueSide,
        accent = TeamBlueAccent
    ),
    YELLOW(
        persianName = "زرد",
        heroName = "دختر چکش‌دار",
        defaultPlayerName = "سارا",
        defaultFrameId = 3,
        pawnDrawable = R.drawable.img_pawn_girl,
        tileTop = TeamYellowTop,
        tileBottom = TeamYellowBottom,
        tileSide = TeamYellowSide,
        accent = TeamYellowAccent
    )
}

enum class StickerType(val id: String, val label: String) {
    POUTING_BUNNY("bunny", "خرگوش اخمو"),
    FUNNY_CHICKEN("chicken", "مرغ بامزه"),
    ROYAL_LAUGH("laugh", "خنده شاهانه"),
    STAR_HEART("heart", "عالی بود"),
    ANGRY_FLAME("angry", "عصبانی!"),
    TROPHY_GG("trophy", "برد با منه");

    companion object {
        fun fromId(id: String): StickerType = entries.firstOrNull { it.id == id } ?: POUTING_BUNNY
    }
}

val QUICK_CHAT_PHRASES = listOf(
    "مهربون باش",
    "ایول!",
    "وای نه!",
    "تاس شیش بیار!",
    "شانس آوردی!",
    "بزن بریم!",
    "دمت گرم!",
    "زود باش!"
)

data class PawnState(
    val id: Int, // 0..3 within team
    val team: TeamColor,
    val progress: Int = -1 // -1 = base, 0..39 = track, 40..43 = home
) {
    val isInBase: Boolean get() = progress < 0
    val isInHome: Boolean get() = progress >= BoardGeometry.TRACK_LENGTH
    val isOnTrack: Boolean get() = progress in 0 until BoardGeometry.TRACK_LENGTH
}

enum class ControllerType {
    LOCAL_HUMAN,
    BOT,
    BLUETOOTH_REMOTE,
    EMPTY
}

data class PlayerSlot(
    val team: TeamColor,
    val name: String,
    val controller: ControllerType,
    val frameId: Int = team.defaultFrameId,
    val peerAddress: String? = null,
    val captures: Int = 0,
    val sixesRolled: Int = 0,
    val finishRank: Int? = null // 1 = winner, 2 = 2nd, etc.
) {
    val isActive: Boolean get() = controller != ControllerType.EMPTY
    val isFinished: Boolean get() = finishRank != null
}

enum class MatchPhase {
    WAITING_FOR_ROLL,
    ROLLING_DICE,
    SELECTING_PAWN,
    ANIMATING_MOVE,
    GAME_OVER
}

enum class MatchMode {
    VS_BOTS,
    PASS_AND_PLAY,
    BLUETOOTH_HOST,
    BLUETOOTH_CLIENT
}

data class ActiveBubble(
    val team: TeamColor,
    val text: String? = null,
    val sticker: StickerType? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class CaptureEvent(
    val cell: GridCell,
    val victimTeam: TeamColor,
    val attackerTeam: TeamColor,
    val eventId: Long = System.currentTimeMillis()
)

data class MovingPawnAnimation(
    val team: TeamColor,
    val pawnId: Int,
    val fromCell: GridCell,
    val toCell: GridCell,
    val stepFraction: Float
)

data class GameState(
    val matchMode: MatchMode = MatchMode.VS_BOTS,
    val localTeam: TeamColor = TeamColor.GREEN,
    val players: Map<TeamColor, PlayerSlot> = emptyMap(),
    val pawns: Map<TeamColor, List<PawnState>> = emptyMap(),
    val currentTurn: TeamColor = TeamColor.GREEN,
    val diceValue: Int = 6,
    val phase: MatchPhase = MatchPhase.WAITING_FOR_ROLL,
    val movablePawnIds: List<Int> = emptyList(),
    val consecutiveSixes: Int = 0,
    val statusBanner: String = "نوبت پرتاب تاس",
    val winnerOrder: List<TeamColor> = emptyList(),
    val bubbles: Map<TeamColor, ActiveBubble> = emptyMap(),
    val lastCapture: CaptureEvent? = null,
    val movingPawn: MovingPawnAnimation? = null
) {
    fun currentPlayer(): PlayerSlot? = players[currentTurn]

    fun canLocalDeviceAct(): Boolean {
        val slot = currentPlayer() ?: return false
        return when (matchMode) {
            MatchMode.VS_BOTS -> slot.controller == ControllerType.LOCAL_HUMAN
            MatchMode.PASS_AND_PLAY -> slot.controller == ControllerType.LOCAL_HUMAN
            MatchMode.BLUETOOTH_HOST -> slot.team == localTeam && slot.controller == ControllerType.LOCAL_HUMAN
            MatchMode.BLUETOOTH_CLIENT -> slot.team == localTeam
        }
    }

    fun toSyncJson(): JSONObject {
        val obj = JSONObject()
        obj.put("turn", currentTurn.ordinal)
        obj.put("dice", diceValue)
        obj.put("phase", phase.name)
        obj.put("sixes", consecutiveSixes)
        obj.put("banner", statusBanner)

        val movableArr = JSONArray()
        movablePawnIds.forEach { movableArr.put(it) }
        obj.put("movable", movableArr)

        val winnersArr = JSONArray()
        winnerOrder.forEach { winnersArr.put(it.ordinal) }
        obj.put("winners", winnersArr)

        val playersArr = JSONArray()
        TeamColor.entries.forEach { color ->
            val p = players[color]
            if (p != null) {
                val pObj = JSONObject()
                pObj.put("team", color.ordinal)
                pObj.put("name", p.name)
                pObj.put("ctrl", p.controller.name)
                pObj.put("frame", p.frameId)
                pObj.put("cap", p.captures)
                pObj.put("s6", p.sixesRolled)
                pObj.put("rank", p.finishRank ?: -1)
                playersArr.put(pObj)
            }
        }
        obj.put("players", playersArr)

        val pawnsArr = JSONArray()
        TeamColor.entries.forEach { color ->
            val list = pawns[color].orEmpty()
            val arr = JSONArray()
            list.forEach { arr.put(it.progress) }
            pawnsArr.put(arr)
        }
        obj.put("pawns", pawnsArr)

        if (lastCapture != null) {
            val cObj = JSONObject()
            cObj.put("col", lastCapture.cell.col)
            cObj.put("row", lastCapture.cell.row)
            cObj.put("v", lastCapture.victimTeam.ordinal)
            cObj.put("a", lastCapture.attackerTeam.ordinal)
            cObj.put("id", lastCapture.eventId)
            obj.put("capture", cObj)
        }
        return obj
    }

    companion object {
        fun fromSyncJson(
            obj: JSONObject,
            localTeam: TeamColor,
            previousBubbles: Map<TeamColor, ActiveBubble>
        ): GameState {
            val turn = TeamColor.entries.getOrElse(obj.optInt("turn", 0)) { TeamColor.GREEN }
            val dice = obj.optInt("dice", 1).coerceIn(1, 6)
            val phase = runCatching { MatchPhase.valueOf(obj.optString("phase", "WAITING_FOR_ROLL")) }
                .getOrDefault(MatchPhase.WAITING_FOR_ROLL)
            val sixes = obj.optInt("sixes", 0)
            val banner = obj.optString("banner", "")

            val movable = mutableListOf<Int>()
            val movArr = obj.optJSONArray("movable")
            if (movArr != null) {
                for (i in 0 until movArr.length()) movable.add(movArr.optInt(i))
            }

            val winners = mutableListOf<TeamColor>()
            val winArr = obj.optJSONArray("winners")
            if (winArr != null) {
                for (i in 0 until winArr.length()) {
                    TeamColor.entries.getOrNull(winArr.optInt(i))?.let { winners.add(it) }
                }
            }

            val playerMap = mutableMapOf<TeamColor, PlayerSlot>()
            val pArr = obj.optJSONArray("players")
            if (pArr != null) {
                for (i in 0 until pArr.length()) {
                    val pObj = pArr.optJSONObject(i) ?: continue
                    val team = TeamColor.entries.getOrNull(pObj.optInt("team")) ?: continue
                    val rawCtrl = runCatching { ControllerType.valueOf(pObj.optString("ctrl", "EMPTY")) }
                        .getOrDefault(ControllerType.EMPTY)
                    val rank = pObj.optInt("rank", -1).takeIf { it > 0 }
                    playerMap[team] = PlayerSlot(
                        team = team,
                        name = pObj.optString("name", team.defaultPlayerName),
                        controller = rawCtrl,
                        frameId = pObj.optInt("frame", team.defaultFrameId),
                        captures = pObj.optInt("cap", 0),
                        sixesRolled = pObj.optInt("s6", 0),
                        finishRank = rank
                    )
                }
            }

            val pawnMap = mutableMapOf<TeamColor, List<PawnState>>()
            val pawnsArr = obj.optJSONArray("pawns")
            if (pawnsArr != null) {
                for (tIdx in 0 until minOf(pawnsArr.length(), TeamColor.entries.size)) {
                    val team = TeamColor.entries[tIdx]
                    val arr = pawnsArr.optJSONArray(tIdx)
                    val list = mutableListOf<PawnState>()
                    if (arr != null) {
                        for (pId in 0 until arr.length()) {
                            list.add(PawnState(id = pId, team = team, progress = arr.optInt(pId, -1)))
                        }
                    }
                    pawnMap[team] = list
                }
            }

            val capObj = obj.optJSONObject("capture")
            val captureEvent = capObj?.let {
                CaptureEvent(
                    cell = GridCell(it.optInt("col", 5), it.optInt("row", 5)),
                    victimTeam = TeamColor.entries.getOrElse(it.optInt("v", 0)) { TeamColor.RED },
                    attackerTeam = TeamColor.entries.getOrElse(it.optInt("a", 0)) { TeamColor.GREEN },
                    eventId = it.optLong("id", 0L)
                )
            }

            return GameState(
                matchMode = MatchMode.BLUETOOTH_CLIENT,
                localTeam = localTeam,
                players = playerMap,
                pawns = pawnMap,
                currentTurn = turn,
                diceValue = dice,
                phase = phase,
                movablePawnIds = movable,
                consecutiveSixes = sixes,
                statusBanner = banner,
                winnerOrder = winners,
                bubbles = previousBubbles,
                lastCapture = captureEvent
            )
        }
    }
}
