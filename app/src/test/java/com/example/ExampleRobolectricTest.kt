package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Persian Bluetooth Mensch title`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("منچ بلوتوثی", appName)
    }

    @Test
    fun `board geometry has 40 distinct track cells and 4 home cells per team`() {
        assertEquals(40, BoardGeometry.TRACK_CELLS.size)
        assertEquals(40, BoardGeometry.TRACK_CELLS.toSet().size)
        TeamColor.entries.forEach { team ->
            assertEquals(4, BoardGeometry.homeCells(team).size)
            assertEquals(4, BoardGeometry.baseCells(team).size)
        }
    }

    @Test
    fun `rolling 6 lets base pawn enter start tile and grants bonus turn`() {
        val slots = TeamColor.entries.associateWith { team ->
            PlayerSlot(team, team.defaultPlayerName, ControllerType.LOCAL_HUMAN)
        }
        val state = MenschEngine.createInitialState(MatchMode.PASS_AND_PLAY, TeamColor.GREEN, slots)
        val movableOn5 = MenschEngine.getMovablePawnIds(state, TeamColor.GREEN, 5)
        assertTrue(movableOn5.isEmpty())

        val movableOn6 = MenschEngine.getMovablePawnIds(state, TeamColor.GREEN, 6)
        assertEquals(listOf(0, 1, 2, 3), movableOn6)

        val outcome = MenschEngine.executeMove(state, TeamColor.GREEN, 0, 6)
        assertNotNull(outcome)
        assertEquals(0, outcome!!.newPawns[TeamColor.GREEN]!![0].progress)
        assertTrue(outcome.bonusTurn)
    }
}
