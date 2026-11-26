package com.example

import com.example.game.BoardGeometry
import com.example.game.ControllerType
import com.example.game.MatchMode
import com.example.game.MenschEngine
import com.example.game.PlayerSlot
import com.example.game.TeamColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun boardGeometry_has40UniqueTrackCellsAndHomeCells() {
        assertEquals(40, BoardGeometry.TRACK_CELLS.size)
        assertEquals(40, BoardGeometry.TRACK_CELLS.toSet().size)
        TeamColor.entries.forEach { team ->
            assertEquals(4, BoardGeometry.homeCells(team).size)
            assertEquals(4, BoardGeometry.baseCells(team).size)
        }
    }

    @Test
    fun rollingSix_allowsExitingBaseAndGrantsBonusTurn() {
        val slots = TeamColor.entries.associateWith { team ->
            PlayerSlot(team, team.defaultPlayerName, ControllerType.LOCAL_HUMAN)
        }
        val state = MenschEngine.createInitialState(MatchMode.PASS_AND_PLAY, TeamColor.GREEN, slots)

        assertTrue(MenschEngine.getMovablePawnIds(state, TeamColor.GREEN, 4).isEmpty())
        assertEquals(listOf(0, 1, 2, 3), MenschEngine.getMovablePawnIds(state, TeamColor.GREEN, 6))

        val outcome = MenschEngine.executeMove(state, TeamColor.GREEN, 0, 6)
        assertNotNull(outcome)
        assertEquals(0, outcome!!.newPawns[TeamColor.GREEN]!![0].progress)
        assertTrue(outcome.bonusTurn)
    }

    @Test
    fun landingOnEnemyPawn_capturesAndSendsBackToBase() {
        val slots = TeamColor.entries.associateWith { team ->
            PlayerSlot(team, team.defaultPlayerName, ControllerType.LOCAL_HUMAN)
        }
        val initial = MenschEngine.createInitialState(MatchMode.PASS_AND_PLAY, TeamColor.GREEN, slots)
        // Put Green pawn 0 at progress 8 (track index 8) and Red pawn 0 at progress 0 (track index 10)
        val greenPawns = initial.pawns[TeamColor.GREEN]!!.toMutableList().apply {
            this[0] = this[0].copy(progress = 8)
        }
        val redPawns = initial.pawns[TeamColor.RED]!!.toMutableList().apply {
            this[0] = this[0].copy(progress = 0)
        }
        val state = initial.copy(
            pawns = initial.pawns + (TeamColor.GREEN to greenPawns) + (TeamColor.RED to redPawns)
        )

        val outcome = MenschEngine.executeMove(state, TeamColor.GREEN, 0, 2)
        assertNotNull(outcome)
        assertEquals(TeamColor.RED, outcome!!.capturedPawn?.team)
        assertEquals(-1, outcome.newPawns[TeamColor.RED]!![0].progress)
        assertTrue(outcome.bonusTurn)
    }
}
