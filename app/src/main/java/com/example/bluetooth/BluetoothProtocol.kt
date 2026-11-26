package com.example.bluetooth

import com.example.game.GameState
import com.example.game.PlayerSlot
import com.example.game.StickerType
import com.example.game.TeamColor
import org.json.JSONArray
import org.json.JSONObject

sealed interface BluetoothPacket {
    fun toWireString(): String

    data class Hello(val playerName: String, val frameId: Int) : BluetoothPacket {
        override fun toWireString(): String = JSONObject().apply {
            put("t", "HELLO")
            put("name", playerName)
            put("frame", frameId)
        }.toString()
    }

    data class Welcome(val assignedTeam: TeamColor, val hostName: String) : BluetoothPacket {
        override fun toWireString(): String = JSONObject().apply {
            put("t", "WELCOME")
            put("team", assignedTeam.ordinal)
            put("host", hostName)
        }.toString()
    }

    data class LobbySync(val slots: Map<TeamColor, PlayerSlot>) : BluetoothPacket {
        override fun toWireString(): String = JSONObject().apply {
            put("t", "LOBBY")
            val arr = JSONArray()
            TeamColor.entries.forEach { team ->
                val s = slots[team]
                if (s != null) {
                    arr.put(JSONObject().apply {
                        put("team", team.ordinal)
                        put("name", s.name)
                        put("ctrl", s.controller.name)
                        put("frame", s.frameId)
                    })
                }
            }
            put("slots", arr)
        }.toString()
    }

    data class GameSync(val stateJson: JSONObject) : BluetoothPacket {
        override fun toWireString(): String = JSONObject().apply {
            put("t", "STATE")
            put("s", stateJson)
        }.toString()
    }

    data class RollRequest(val team: TeamColor) : BluetoothPacket {
        override fun toWireString(): String = JSONObject().apply {
            put("t", "ROLL")
            put("team", team.ordinal)
        }.toString()
    }

    data class MoveRequest(val team: TeamColor, val pawnId: Int) : BluetoothPacket {
        override fun toWireString(): String = JSONObject().apply {
            put("t", "MOVE")
            put("team", team.ordinal)
            put("pawn", pawnId)
        }.toString()
    }

    data class ChatMessage(val team: TeamColor, val text: String) : BluetoothPacket {
        override fun toWireString(): String = JSONObject().apply {
            put("t", "CHAT")
            put("team", team.ordinal)
            put("msg", text)
        }.toString()
    }

    data class StickerMessage(val team: TeamColor, val sticker: StickerType) : BluetoothPacket {
        override fun toWireString(): String = JSONObject().apply {
            put("t", "STICKER")
            put("team", team.ordinal)
            put("sid", sticker.id)
        }.toString()
    }

    data object RematchRequest : BluetoothPacket {
        override fun toWireString(): String = JSONObject().apply {
            put("t", "REMATCH")
        }.toString()
    }

    companion object {
        fun parse(line: String): BluetoothPacket? = runCatching {
            val obj = JSONObject(line)
            when (obj.optString("t")) {
                "HELLO" -> Hello(
                    playerName = obj.optString("name", "دوست بلوتوثی"),
                    frameId = obj.optInt("frame", 1)
                )
                "WELCOME" -> Welcome(
                    assignedTeam = TeamColor.entries.getOrElse(obj.optInt("team", 1)) { TeamColor.RED },
                    hostName = obj.optString("host", "میزبان")
                )
                "LOBBY" -> {
                    val map = mutableMapOf<TeamColor, PlayerSlot>()
                    val arr = obj.optJSONArray("slots")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val team = TeamColor.entries.getOrNull(item.optInt("team")) ?: continue
                            val ctrl = runCatching {
                                com.example.game.ControllerType.valueOf(item.optString("ctrl", "EMPTY"))
                            }.getOrDefault(com.example.game.ControllerType.EMPTY)
                            map[team] = PlayerSlot(
                                team = team,
                                name = item.optString("name", team.defaultPlayerName),
                                controller = ctrl,
                                frameId = item.optInt("frame", team.defaultFrameId)
                            )
                        }
                    }
                    LobbySync(map)
                }
                "STATE" -> {
                    val stateObj = obj.optJSONObject("s") ?: return null
                    GameSync(stateObj)
                }
                "ROLL" -> RollRequest(
                    team = TeamColor.entries.getOrElse(obj.optInt("team", 0)) { TeamColor.GREEN }
                )
                "MOVE" -> MoveRequest(
                    team = TeamColor.entries.getOrElse(obj.optInt("team", 0)) { TeamColor.GREEN },
                    pawnId = obj.optInt("pawn", 0)
                )
                "CHAT" -> ChatMessage(
                    team = TeamColor.entries.getOrElse(obj.optInt("team", 0)) { TeamColor.GREEN },
                    text = obj.optString("msg", "")
                )
                "STICKER" -> StickerMessage(
                    team = TeamColor.entries.getOrElse(obj.optInt("team", 0)) { TeamColor.GREEN },
                    sticker = StickerType.fromId(obj.optString("sid", "bunny"))
                )
                "REMATCH" -> RematchRequest
                else -> null
            }
        }.getOrNull()

        fun fromState(state: GameState): GameSync = GameSync(state.toSyncJson())
    }
}
