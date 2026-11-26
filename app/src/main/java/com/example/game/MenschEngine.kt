package com.example.game

import kotlin.random.Random

data class MoveOutcome(
    val newPawns: Map<TeamColor, List<PawnState>>,
    val capturedPawn: PawnState? = null,
    val captureCell: GridCell? = null,
    val enteredHome: Boolean = false,
    val playerFinished: Boolean = false,
    val bonusTurn: Boolean = false,
    val stepPath: List<GridCell> = emptyList()
)

object MenschEngine {

    fun createInitialState(
        mode: MatchMode,
        localTeam: TeamColor,
        slots: Map<TeamColor, PlayerSlot>
    ): GameState {
        val pawnMap = TeamColor.entries.associateWith { team ->
            if (slots[team]?.isActive == true) {
                List(4) { id -> PawnState(id = id, team = team, progress = -1) }
            } else {
                emptyList()
            }
        }
        val firstTurn = TeamColor.entries.firstOrNull { slots[it]?.isActive == true } ?: TeamColor.GREEN
        val firstName = slots[firstTurn]?.name ?: firstTurn.defaultPlayerName
        return GameState(
            matchMode = mode,
            localTeam = localTeam,
            players = slots,
            pawns = pawnMap,
            currentTurn = firstTurn,
            diceValue = 6,
            phase = MatchPhase.WAITING_FOR_ROLL,
            movablePawnIds = emptyList(),
            consecutiveSixes = 0,
            statusBanner = "نوبت $firstName است"
        )
    }

    fun getMovablePawnIds(state: GameState, team: TeamColor, dice: Int): List<Int> {
        val teamPawns = state.pawns[team].orEmpty()
        if (teamPawns.isEmpty()) return emptyList()

        return teamPawns.filter { pawn ->
            canMovePawn(pawn, teamPawns, dice)
        }.map { it.id }
    }

    private fun canMovePawn(pawn: PawnState, friendlyPawns: List<PawnState>, dice: Int): Boolean {
        if (pawn.isInBase) {
            if (dice != 6) return false
            // Cannot exit base if a friendly pawn is already standing on start square (progress == 0)
            return friendlyPawns.none { it.id != pawn.id && it.progress == 0 }
        }

        val targetProgress = pawn.progress + dice
        if (targetProgress > BoardGeometry.MAX_PROGRESS) return false

        // Cannot land on own friendly pawn (classic Iranian Mensch rule)
        if (friendlyPawns.any { it.id != pawn.id && it.progress == targetProgress }) {
            return false
        }

        return true
    }

    fun executeMove(state: GameState, team: TeamColor, pawnId: Int, dice: Int): MoveOutcome? {
        val friendlyPawns = state.pawns[team].orEmpty()
        val pawn = friendlyPawns.firstOrNull { it.id == pawnId } ?: return null
        if (!canMovePawn(pawn, friendlyPawns, dice)) return null

        val targetProgress = if (pawn.isInBase) 0 else pawn.progress + dice

        // Build step-by-step cell list for smooth animated hopping
        val path = mutableListOf<GridCell>()
        if (pawn.isInBase) {
            path.add(BoardGeometry.cellForPawn(team, pawn.id, -1))
            path.add(BoardGeometry.cellForPawn(team, pawn.id, 0))
        } else {
            for (p in pawn.progress..targetProgress) {
                path.add(BoardGeometry.cellForPawn(team, pawn.id, p))
            }
        }

        val mutablePawns = state.pawns.mapValues { it.value.toMutableList() }.toMutableMap()
        val updatedTeamList = mutablePawns[team] ?: return null
        val idx = updatedTeamList.indexOfFirst { it.id == pawnId }
        if (idx < 0) return null
        updatedTeamList[idx] = pawn.copy(progress = targetProgress)

        // Check enemy capture on outer track (home tiles 40..43 are safe)
        var captured: PawnState? = null
        var captureCell: GridCell? = null
        val targetGlobalTrack = BoardGeometry.globalTrackIndex(team, targetProgress)
        if (targetGlobalTrack != null) {
            for (otherTeam in TeamColor.entries) {
                if (otherTeam == team) continue
                val enemyList = mutablePawns[otherTeam] ?: continue
                val enemyIdx = enemyList.indexOfFirst { enemy ->
                    BoardGeometry.globalTrackIndex(otherTeam, enemy.progress) == targetGlobalTrack
                }
                if (enemyIdx >= 0) {
                    captured = enemyList[enemyIdx]
                    captureCell = BoardGeometry.TRACK_CELLS[targetGlobalTrack]
                    enemyList[enemyIdx] = captured.copy(progress = -1)
                    break
                }
            }
        }

        val enteredHome = !pawn.isInHome && targetProgress >= BoardGeometry.TRACK_LENGTH
        val playerFinished = updatedTeamList.all { it.isInHome }
        val bonusTurn = !playerFinished && (dice == 6 || captured != null)

        return MoveOutcome(
            newPawns = mutablePawns.mapValues { it.value.toList() },
            capturedPawn = captured,
            captureCell = captureCell,
            enteredHome = enteredHome,
            playerFinished = playerFinished,
            bonusTurn = bonusTurn,
            stepPath = path
        )
    }

    fun nextActiveTurn(state: GameState, current: TeamColor): TeamColor {
        val ordered = TeamColor.entries
        val startIdx = ordered.indexOf(current)
        for (offset in 1..ordered.size) {
            val candidate = ordered[(startIdx + offset) % ordered.size]
            val slot = state.players[candidate]
            if (slot != null && slot.isActive && !slot.isFinished) {
                return candidate
            }
        }
        return current
    }

    fun countRemainingActivePlayers(state: GameState): Int {
        return state.players.values.count { it.isActive && !it.isFinished }
    }

    /**
     * Smart tactical Bot AI:
     * 1. Capture enemy pawn (highest priority, especially enemy with high progress)
     * 2. Enter safe Home stretch
     * 3. Bring pawn out of base on 6
     * 4. Escape danger (enemy 1..6 tiles behind)
     * 5. Advance furthest pawn cleanly
     */
    fun chooseBestBotMove(state: GameState, team: TeamColor, dice: Int): Int? {
        val legalIds = getMovablePawnIds(state, team, dice)
        if (legalIds.isEmpty()) return null
        if (legalIds.size == 1) return legalIds.first()

        val teamPawns = state.pawns[team].orEmpty()
        var bestId = legalIds.first()
        var bestScore = Int.MIN_VALUE

        for (pawnId in legalIds) {
            val pawn = teamPawns.first { it.id == pawnId }
            val targetProgress = if (pawn.isInBase) 0 else pawn.progress + dice
            var score = Random.nextInt(0, 6) // slight natural variety

            // Check capture potential
            val targetTrack = BoardGeometry.globalTrackIndex(team, targetProgress)
            if (targetTrack != null) {
                for (otherTeam in TeamColor.entries) {
                    if (otherTeam == team) continue
                    val victim = state.pawns[otherTeam].orEmpty().firstOrNull {
                        BoardGeometry.globalTrackIndex(otherTeam, it.progress) == targetTrack
                    }
                    if (victim != null) {
                        score += 220 + victim.progress * 3
                    }
                }
            }

            // Entering safe home stretch
            if (!pawn.isInHome && targetProgress >= BoardGeometry.TRACK_LENGTH) {
                score += 170 + (targetProgress - BoardGeometry.TRACK_LENGTH) * 15
            }

            // Exiting base on a 6
            if (pawn.isInBase && dice == 6) {
                val activeCount = teamPawns.count { !it.isInBase && !it.isInHome }
                score += if (activeCount < 2) 150 else 110
            }

            // Escaping danger from enemies behind
            val currentTrack = BoardGeometry.globalTrackIndex(team, pawn.progress)
            if (currentTrack != null && isUnderThreat(state, team, currentTrack)) {
                score += 95
            }

            // Avoid landing right in front of an enemy if possible
            if (targetTrack != null && isUnderThreat(state, team, targetTrack)) {
                score -= 55
            }

            // Prefer advancing pawns further along the track
            score += targetProgress * 2

            if (score > bestScore) {
                bestScore = score
                bestId = pawnId
            }
        }
        return bestId
    }

    private fun isUnderThreat(state: GameState, myTeam: TeamColor, trackPos: Int): Boolean {
        for (other in TeamColor.entries) {
            if (other == myTeam) continue
            val enemies = state.pawns[other].orEmpty()
            for (enemy in enemies) {
                val eTrack = BoardGeometry.globalTrackIndex(other, enemy.progress) ?: continue
                val distance = (trackPos - eTrack + BoardGeometry.TRACK_LENGTH) % BoardGeometry.TRACK_LENGTH
                if (distance in 1..6) return true
            }
        }
        return false
    }
}
