package com.example.game

import androidx.compose.ui.geometry.Offset

data class GridCell(val col: Int, val row: Int)

/**
 * Classic 40-tile Iranian Mensch (منچ) geometry on an 11x11 cross grid,
 * rendered in a 45-degree isometric diamond projection matching the temple courtyard board.
 *
 * Orientation after isometric projection (isoX = col - row, isoY = col + row):
 * - GREEN (Bear): Top-Left arm (1..4, 5), Base at Top (1..2, 1..2), Start at (0, 4)
 * - RED (Strongman): Top-Right arm (5, 1..4), Base at Right (8..9, 1..2), Start at (6, 0)
 * - BLUE (Pirate): Bottom-Right arm (6..9, 5), Base at Bottom (8..9, 8..9), Start at (10, 6)
 * - YELLOW (Mallet Girl): Bottom-Left arm (5, 6..9), Base at Left (1..2, 8..9), Start at (4, 10)
 * - Center Owl Pedestal: (5, 5)
 */
object BoardGeometry {

    const val TRACK_LENGTH = 40
    const val HOME_LENGTH = 4
    const val MAX_PROGRESS = TRACK_LENGTH + HOME_LENGTH - 1 // 43

    val CENTER_CELL = GridCell(5, 5)

    // 40-tile clockwise outer perimeter track starting at Green's start (0, 4)
    val TRACK_CELLS: List<GridCell> = listOf(
        // 0..4: Green top edge toward center
        GridCell(0, 4), GridCell(1, 4), GridCell(2, 4), GridCell(3, 4), GridCell(4, 4),
        // 5..9: Red left edge outward + Red tip
        GridCell(4, 3), GridCell(4, 2), GridCell(4, 1), GridCell(4, 0), GridCell(5, 0),
        // 10..14: Red start (6,0) & right edge toward center
        GridCell(6, 0), GridCell(6, 1), GridCell(6, 2), GridCell(6, 3), GridCell(6, 4),
        // 15..19: Blue top edge outward + Blue tip
        GridCell(7, 4), GridCell(8, 4), GridCell(9, 4), GridCell(10, 4), GridCell(10, 5),
        // 20..24: Blue start (10,6) & bottom edge toward center
        GridCell(10, 6), GridCell(9, 6), GridCell(8, 6), GridCell(7, 6), GridCell(6, 6),
        // 25..29: Yellow right edge outward + Yellow tip
        GridCell(6, 7), GridCell(6, 8), GridCell(6, 9), GridCell(6, 10), GridCell(5, 10),
        // 30..34: Yellow start (4,10) & left edge toward center
        GridCell(4, 10), GridCell(4, 9), GridCell(4, 8), GridCell(4, 7), GridCell(4, 6),
        // 35..39: Green bottom edge outward + Green tip
        GridCell(3, 6), GridCell(2, 6), GridCell(1, 6), GridCell(0, 6), GridCell(0, 5)
    )

    fun startTrackIndex(team: TeamColor): Int = when (team) {
        TeamColor.GREEN -> 0
        TeamColor.RED -> 10
        TeamColor.BLUE -> 20
        TeamColor.YELLOW -> 30
    }

    fun startCell(team: TeamColor): GridCell = TRACK_CELLS[startTrackIndex(team)]

    fun homeCells(team: TeamColor): List<GridCell> = when (team) {
        TeamColor.GREEN -> listOf(GridCell(1, 5), GridCell(2, 5), GridCell(3, 5), GridCell(4, 5))
        TeamColor.RED -> listOf(GridCell(5, 1), GridCell(5, 2), GridCell(5, 3), GridCell(5, 4))
        TeamColor.BLUE -> listOf(GridCell(9, 5), GridCell(8, 5), GridCell(7, 5), GridCell(6, 5))
        TeamColor.YELLOW -> listOf(GridCell(5, 9), GridCell(5, 8), GridCell(5, 7), GridCell(5, 6))
    }

    fun baseCells(team: TeamColor): List<GridCell> = when (team) {
        TeamColor.GREEN -> listOf(GridCell(1, 1), GridCell(2, 1), GridCell(1, 2), GridCell(2, 2))
        TeamColor.RED -> listOf(GridCell(8, 1), GridCell(9, 1), GridCell(8, 2), GridCell(9, 2))
        TeamColor.BLUE -> listOf(GridCell(8, 8), GridCell(9, 8), GridCell(8, 9), GridCell(9, 9))
        TeamColor.YELLOW -> listOf(GridCell(1, 8), GridCell(2, 8), GridCell(1, 9), GridCell(2, 9))
    }

    fun globalTrackIndex(team: TeamColor, progress: Int): Int? {
        if (progress !in 0 until TRACK_LENGTH) return null
        return (startTrackIndex(team) + progress) % TRACK_LENGTH
    }

    fun cellForPawn(team: TeamColor, pawnIndex: Int, progress: Int): GridCell {
        return when {
            progress < 0 -> baseCells(team)[pawnIndex.coerceIn(0, 3)]
            progress < TRACK_LENGTH -> TRACK_CELLS[(startTrackIndex(team) + progress) % TRACK_LENGTH]
            else -> homeCells(team)[(progress - TRACK_LENGTH).coerceIn(0, HOME_LENGTH - 1)]
        }
    }

    /**
     * Converts a continuous grid coordinate (col, row) into 2D isometric canvas coordinates.
     * tileHalfW = horizontal half-diagonal of one diamond tile
     * tileHalfH = vertical half-diagonal of one diamond tile
     */
    fun toScreenOffset(
        col: Float,
        row: Float,
        originX: Float,
        originY: Float,
        tileHalfW: Float,
        tileHalfH: Float
    ): Offset {
        val isoX = (col - row) * tileHalfW
        val isoY = (col + row - 10f) * tileHalfH
        return Offset(originX + isoX, originY + isoY)
    }

    fun toScreenOffset(
        cell: GridCell,
        originX: Float,
        originY: Float,
        tileHalfW: Float,
        tileHalfH: Float
    ): Offset = toScreenOffset(cell.col.toFloat(), cell.row.toFloat(), originX, originY, tileHalfW, tileHalfH)
}
