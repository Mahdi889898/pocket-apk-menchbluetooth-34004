package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.game.BoardGeometry
import com.example.game.GameState
import com.example.game.GridCell
import com.example.game.MatchPhase
import com.example.game.PawnState
import com.example.game.TeamColor
import com.example.ui.theme.TempleGoldBright
import com.example.ui.theme.TempleGoldDeep
import com.example.ui.theme.TileCreamBottom
import com.example.ui.theme.TileCreamShadow
import com.example.ui.theme.TileCreamSide
import com.example.ui.theme.TileCreamTop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private data class RenderTile(
    val cell: GridCell,
    val topColor: Color,
    val bottomColor: Color,
    val sideColor: Color,
    val isStartFor: TeamColor? = null,
    val isHomeSlot: Boolean = false
)

private data class PositionedPawn(
    val pawn: PawnState,
    val screenPos: Offset,
    val jumpOffsetPx: Float,
    val isMovable: Boolean,
    val depthKey: Float
)

@Composable
fun IsometricBoardView(
    state: GameState,
    onPawnTapped: (TeamColor, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "board_pulse")
    val pulsePhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val bobPhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    val burstProgress = remember { Animatable(1f) }
    val captureId = state.lastCapture?.eventId
    LaunchedEffect(captureId) {
        if (captureId != null) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(1f, tween(durationMillis = 700, easing = LinearEasing))
        }
    }

    // Build all board tiles sorted back-to-front by isometric depth (col + row)
    val allTiles = remember {
        val list = mutableListOf<RenderTile>()
        val startMap = TeamColor.entries.associateBy { BoardGeometry.startCell(it) }

        // 40 track tiles
        for (cell in BoardGeometry.TRACK_CELLS) {
            val startTeam = startMap[cell]
            if (startTeam != null) {
                list.add(
                    RenderTile(
                        cell = cell,
                        topColor = startTeam.tileTop,
                        bottomColor = startTeam.tileBottom,
                        sideColor = startTeam.tileSide,
                        isStartFor = startTeam
                    )
                )
            } else {
                list.add(
                    RenderTile(
                        cell = cell,
                        topColor = TileCreamTop,
                        bottomColor = TileCreamBottom,
                        sideColor = TileCreamSide
                    )
                )
            }
        }

        // 16 home stretch tiles + 16 base yard tiles
        for (team in TeamColor.entries) {
            for (cell in BoardGeometry.homeCells(team)) {
                list.add(
                    RenderTile(
                        cell = cell,
                        topColor = team.tileTop,
                        bottomColor = team.tileBottom,
                        sideColor = team.tileSide,
                        isHomeSlot = true
                    )
                )
            }
            for (cell in BoardGeometry.baseCells(team)) {
                list.add(
                    RenderTile(
                        cell = cell,
                        topColor = team.tileTop,
                        bottomColor = team.tileBottom,
                        sideColor = team.tileSide
                    )
                )
            }
        }

        list.sortedBy { it.cell.col + it.cell.row }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("isometric_board")
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Compute isometric tile dimensions so all 11x11 tiles fit cleanly with lush temple margins
        // Horizontal span of active cells: (col - row) ranges from -8 to +8 -> 16 half-diagonals + 2.2 padding
        // Vertical span of active cells: (col + row) ranges from 2 to 18 -> 16 half-diagonals + 2.8 padding
        val tileHalfWFromWidth = widthPx / 18.2f
        val tileHalfHFromHeight = heightPx / 19.6f
        val tileHalfW = minOf(tileHalfWFromWidth, tileHalfHFromHeight / 0.78f)
        val tileHalfH = tileHalfW * 0.78f
        val tileExtrude = tileHalfH * 0.42f

        val originX = widthPx * 0.5f
        val originY = heightPx * 0.51f

        val canSelectNow = state.phase == MatchPhase.SELECTING_PAWN && state.canLocalDeviceAct()

        // Compute destination preview cells for movable pawns
        val previewTargetCells: Set<GridCell> = remember(
            state.currentTurn,
            state.movablePawnIds,
            state.diceValue,
            state.pawns,
            canSelectNow
        ) {
            if (!canSelectNow) emptySet()
            else {
                val teamPawns = state.pawns[state.currentTurn].orEmpty()
                state.movablePawnIds.mapNotNull { id ->
                    val p = teamPawns.firstOrNull { it.id == id } ?: return@mapNotNull null
                    val nextProgress = if (p.isInBase) 0 else p.progress + state.diceValue
                    BoardGeometry.cellForPawn(state.currentTurn, p.id, nextProgress)
                }.toSet()
            }
        }

        // Compute positioned pawns with smooth interpolation for the currently hopping pawn
        val positionedPawns: List<PositionedPawn> = remember(
            state.pawns,
            state.movingPawn,
            state.movablePawnIds,
            state.currentTurn,
            canSelectNow,
            originX,
            originY,
            tileHalfW,
            tileHalfH
        ) {
            val result = mutableListOf<PositionedPawn>()
            val cellOccupancy = mutableMapOf<GridCell, Int>()

            // Count how many pawns share a cell (for subtle spread if ever needed)
            for (team in TeamColor.entries) {
                for (pawn in state.pawns[team].orEmpty()) {
                    val cell = BoardGeometry.cellForPawn(team, pawn.id, pawn.progress)
                    cellOccupancy[cell] = (cellOccupancy[cell] ?: 0) + 1
                }
            }
            val cellSeen = mutableMapOf<GridCell, Int>()

            for (team in TeamColor.entries) {
                for (pawn in state.pawns[team].orEmpty()) {
                    val anim = state.movingPawn
                    val isThisMoving = anim != null && anim.team == team && anim.pawnId == pawn.id
                    val pos: Offset
                    val jumpPx: Float
                    val depth: Float

                    if (isThisMoving && anim != null) {
                        val f = anim.stepFraction.coerceIn(0f, 1f)
                        val col = anim.fromCell.col + (anim.toCell.col - anim.fromCell.col) * f
                        val row = anim.fromCell.row + (anim.toCell.row - anim.fromCell.row) * f
                        pos = BoardGeometry.toScreenOffset(col, row, originX, originY, tileHalfW, tileHalfH)
                        jumpPx = (sin(f * PI).toFloat()) * tileHalfH * 1.45f
                        depth = col + row + 0.35f
                    } else {
                        val cell = BoardGeometry.cellForPawn(team, pawn.id, pawn.progress)
                        val baseOffset = BoardGeometry.toScreenOffset(cell, originX, originY, tileHalfW, tileHalfH)
                        val totalHere = cellOccupancy[cell] ?: 1
                        val idxHere = cellSeen[cell] ?: 0
                        cellSeen[cell] = idxHere + 1
                        val spread = if (totalHere > 1) {
                            val shift = (idxHere - (totalHere - 1) / 2f) * tileHalfW * 0.35f
                            Offset(shift, shift * 0.25f)
                        } else {
                            Offset.Zero
                        }
                        pos = baseOffset + spread
                        jumpPx = 0f
                        depth = (cell.col + cell.row).toFloat()
                    }

                    val movable = canSelectNow &&
                        team == state.currentTurn &&
                        pawn.id in state.movablePawnIds

                    result.add(
                        PositionedPawn(
                            pawn = pawn,
                            screenPos = pos,
                            jumpOffsetPx = jumpPx,
                            isMovable = movable,
                            depthKey = depth
                        )
                    )
                }
            }
            result.sortedBy { it.depthKey }
        }

        // Layer 1: Isometric 3D Beveled Tiles, Owl Pedestal, Selection Rings & Capture Burst
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(positionedPawns, canSelectNow) {
                    detectTapGestures { tapOffset ->
                        if (!canSelectNow) return@detectTapGestures
                        val movable = positionedPawns.filter { it.isMovable }
                        if (movable.isEmpty()) return@detectTapGestures

                        val hitRadius = maxOf(tileHalfW * 2.15f, 52.dp.toPx())
                        var bestPawn: PositionedPawn? = null
                        var bestDist = Float.MAX_VALUE
                        for (candidate in movable) {
                            // Check center of character body (slightly above tile center)
                            val bodyCenter = Offset(
                                candidate.screenPos.x,
                                candidate.screenPos.y - tileHalfH * 0.75f
                            )
                            val dBody = hypot(tapOffset.x - bodyCenter.x, tapOffset.y - bodyCenter.y)
                            val dTile = hypot(tapOffset.x - candidate.screenPos.x, tapOffset.y - candidate.screenPos.y)
                            val dist = minOf(dBody, dTile)
                            if (dist < hitRadius && dist < bestDist) {
                                bestDist = dist
                                bestPawn = candidate
                            }
                        }
                        bestPawn?.let { onPawnTapped(it.pawn.team, it.pawn.id) }
                    }
                }
        ) {
            // Draw subtle glowing stone board aura beneath cross
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38FFF1B8),
                        Color(0x142A1752),
                        Color.Transparent
                    ),
                    center = Offset(originX, originY),
                    radius = tileHalfW * 9.5f
                ),
                radius = tileHalfW * 9.5f,
                center = Offset(originX, originY)
            )

            // Draw all tiles back-to-front; draw Center Owl Pedestal at depth 10
            var owlDrawn = false
            for (tile in allTiles) {
                val depth = tile.cell.col + tile.cell.row
                if (!owlDrawn && depth > 10) {
                    val centerPos = BoardGeometry.toScreenOffset(
                        BoardGeometry.CENTER_CELL, originX, originY, tileHalfW, tileHalfH
                    )
                    drawOwlPedestal(centerPos, tileHalfW * 1.48f, tileHalfH * 1.48f)
                    owlDrawn = true
                }

                val center = BoardGeometry.toScreenOffset(tile.cell, originX, originY, tileHalfW, tileHalfH)
                drawIsometricBlockTile(
                    center = center,
                    halfW = tileHalfW * 0.90f,
                    halfH = tileHalfH * 0.90f,
                    extrude = tileExtrude,
                    topColor = tile.topColor,
                    bottomColor = tile.bottomColor,
                    sideColor = tile.sideColor,
                    drawStartStar = tile.isStartFor != null,
                    isTargetPreview = tile.cell in previewTargetCells,
                    pulse = pulsePhase.value
                )
            }
            if (!owlDrawn) {
                val centerPos = BoardGeometry.toScreenOffset(
                    BoardGeometry.CENTER_CELL, originX, originY, tileHalfW, tileHalfH
                )
                drawOwlPedestal(centerPos, tileHalfW * 1.48f, tileHalfH * 1.48f)
            }

            // Draw pawn contact shadows and movable selection rings under characters
            for (item in positionedPawns) {
                val shadowScale = (1f - item.jumpOffsetPx / (tileHalfH * 3.5f)).coerceIn(0.45f, 1f)
                val shadowW = tileHalfW * 1.02f * shadowScale
                val shadowH = tileHalfH * 0.62f * shadowScale

                if (item.isMovable) {
                    val ringRadius = tileHalfW * (0.95f + 0.16f * pulsePhase.value)
                    drawOval(
                        color = TempleGoldBright.copy(alpha = 0.85f),
                        topLeft = Offset(item.screenPos.x - ringRadius, item.screenPos.y - ringRadius * 0.72f),
                        size = Size(ringRadius * 2f, ringRadius * 1.44f),
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                    drawOval(
                        color = Color.White.copy(alpha = 0.35f + 0.25f * pulsePhase.value),
                        topLeft = Offset(item.screenPos.x - ringRadius * 0.82f, item.screenPos.y - ringRadius * 0.58f),
                        size = Size(ringRadius * 1.64f, ringRadius * 1.16f)
                    )
                }

                drawOval(
                    color = Color(0x661C0D04),
                    topLeft = Offset(item.screenPos.x - shadowW * 0.5f, item.screenPos.y - shadowH * 0.35f),
                    size = Size(shadowW, shadowH)
                )
            }

            // Dramatic capture burst comic rays!
            val capture = state.lastCapture
            if (capture != null && burstProgress.value < 0.99f) {
                val burstCenter = BoardGeometry.toScreenOffset(capture.cell, originX, originY, tileHalfW, tileHalfH)
                drawCaptureBurst(burstCenter, tileHalfW * 2.6f, burstProgress.value)
            }
        }

        // Layer 2: Crisp 3D Cartoon Character Sprites sorted back-to-front
        val pawnSizePx = tileHalfW * 2.55f
        val pawnSizeDp = with(density) { pawnSizePx.toDp() }

        for (item in positionedPawns) {
            val idleBobPx = if (item.isMovable) bobPhase.value * tileHalfH * 0.42f else 0f
            val totalLift = item.jumpOffsetPx + idleBobPx
            val spriteScale = if (item.isMovable) 1.06f + 0.05f * bobPhase.value else 1.0f

            // Anchor feet right onto tile center
            val leftPx = (item.screenPos.x - pawnSizePx * 0.5f).roundToInt()
            val topPx = (item.screenPos.y - pawnSizePx * 0.84f - totalLift).roundToInt()

            Image(
                painter = painterResource(id = item.pawn.team.pawnDrawable),
                contentDescription = "${item.pawn.team.heroName} ${item.pawn.id + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset { IntOffset(leftPx, topPx) }
                    .size(pawnSizeDp)
                    .graphicsLayer {
                        scaleX = spriteScale
                        scaleY = spriteScale
                    }
            )
        }
    }
}

private fun DrawScope.drawIsometricBlockTile(
    center: Offset,
    halfW: Float,
    halfH: Float,
    extrude: Float,
    topColor: Color,
    bottomColor: Color,
    sideColor: Color,
    drawStartStar: Boolean,
    isTargetPreview: Boolean,
    pulse: Float
) {
    val top = Offset(center.x, center.y - halfH)
    val right = Offset(center.x + halfW, center.y)
    val bottom = Offset(center.x, center.y + halfH)
    val left = Offset(center.x - halfW, center.y)

    // 1. Soft ground shadow below tile
    val shadowPath = buildRoundedDiamondPath(
        center = Offset(center.x, center.y + extrude + 2.5f),
        halfW = halfW * 1.03f,
        halfH = halfH * 1.03f,
        cornerFactor = 0.22f
    )
    drawPath(shadowPath, color = TileCreamShadow)

    // 2. Extruded 3D side base (left-bottom and right-bottom skirts)
    val sidePath = Path().apply {
        moveTo(left.x, left.y)
        lineTo(bottom.x, bottom.y)
        lineTo(right.x, right.y)
        lineTo(right.x, right.y + extrude)
        lineTo(bottom.x, bottom.y + extrude)
        lineTo(left.x, left.y + extrude)
        close()
    }
    drawPath(
        path = sidePath,
        brush = Brush.horizontalGradient(
            colors = listOf(sideColor, sideColor.copy(alpha = 0.88f), sideColor),
            startX = left.x,
            endX = right.x
        )
    )

    // Bottom extruded rounded base cap so corners stay plump & rounded
    val baseCap = buildRoundedDiamondPath(
        center = Offset(center.x, center.y + extrude),
        halfW = halfW,
        halfH = halfH,
        cornerFactor = 0.22f
    )
    drawPath(baseCap, color = sideColor)

    // 3. Glossy rounded top face
    val topFace = buildRoundedDiamondPath(
        center = center,
        halfW = halfW,
        halfH = halfH,
        cornerFactor = 0.22f
    )
    drawPath(
        path = topFace,
        brush = Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = top.y,
            endY = bottom.y
        )
    )

    // 4. Crisp specular top bevel rim
    drawPath(
        path = topFace,
        color = Color.White.copy(alpha = 0.42f),
        style = Stroke(width = 1.4f, join = StrokeJoin.Round, cap = StrokeCap.Round)
    )

    // Inner subtle highlight oval on top half for toy-like 3D gloss
    drawOval(
        color = Color.White.copy(alpha = 0.22f),
        topLeft = Offset(center.x - halfW * 0.42f, center.y - halfH * 0.62f),
        size = Size(halfW * 0.84f, halfH * 0.46f)
    )

    // 5. Optional start-tile subtle star emblem
    if (drawStartStar) {
        drawMiniStar(
            center = center,
            outerRadius = halfH * 0.44f,
            color = Color.White.copy(alpha = 0.72f)
        )
    }

    // 6. Destination preview pulse highlight
    if (isTargetPreview) {
        drawPath(
            path = topFace,
            color = TempleGoldBright.copy(alpha = 0.55f + 0.35f * pulse),
            style = Stroke(width = 3.2f, join = StrokeJoin.Round)
        )
    }
}

private fun buildRoundedDiamondPath(
    center: Offset,
    halfW: Float,
    halfH: Float,
    cornerFactor: Float
): Path {
    val pts = listOf(
        Offset(center.x, center.y - halfH), // Top
        Offset(center.x + halfW, center.y), // Right
        Offset(center.x, center.y + halfH), // Bottom
        Offset(center.x - halfW, center.y)  // Left
    )
    val path = Path()
    for (i in pts.indices) {
        val prev = pts[(i + 3) % 4]
        val curr = pts[i]
        val next = pts[(i + 1) % 4]

        val pStart = Offset(
            curr.x + (prev.x - curr.x) * cornerFactor,
            curr.y + (prev.y - curr.y) * cornerFactor
        )
        val pEnd = Offset(
            curr.x + (next.x - curr.x) * cornerFactor,
            curr.y + (next.y - curr.y) * cornerFactor
        )
        if (i == 0) path.moveTo(pStart.x, pStart.y) else path.lineTo(pStart.x, pStart.y)
        path.quadraticTo(curr.x, curr.y, pEnd.x, pEnd.y)
    }
    path.close()
    return path
}

private fun DrawScope.drawMiniStar(center: Offset, outerRadius: Float, color: Color) {
    val innerRadius = outerRadius * 0.46f
    val path = Path()
    for (i in 0 until 10) {
        val angle = -PI / 2.0 + i * PI / 5.0
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r * 0.80).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

/**
 * Draws the iconic sculpted golden Owl Medallion Pedestal in the center of the diamond cross
 * (matching the sculpted golden owl on the circular stone plinth seen in both reference screenshots).
 */
private fun DrawScope.drawOwlPedestal(center: Offset, rx: Float, ry: Float) {
    val plinthDepth = ry * 0.38f

    // Ground shadow
    drawOval(
        color = Color(0x661A0E05),
        topLeft = Offset(center.x - rx * 1.08f, center.y - ry * 0.72f + plinthDepth),
        size = Size(rx * 2.16f, ry * 1.72f)
    )

    // Stepped reddish-terracotta stone base rim
    drawOval(
        color = Color(0xFF9C544B),
        topLeft = Offset(center.x - rx, center.y - ry * 0.8f + plinthDepth),
        size = Size(rx * 2f, ry * 1.6f)
    )
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF6B8A8E), Color(0xFF42595E)),
            startY = center.y - ry,
            endY = center.y + ry
        ),
        topLeft = Offset(center.x - rx * 0.96f, center.y - ry * 0.85f + plinthDepth * 0.4f),
        size = Size(rx * 1.92f, ry * 1.54f)
    )

    // Top circular bronze/gold coin face
    val topCenterY = center.y - ry * 0.14f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFE57F), Color(0xFFD69E2E), Color(0xFF8C5B12)),
            center = Offset(center.x - rx * 0.2f, topCenterY - ry * 0.2f),
            radius = rx * 1.15f
        ),
        topLeft = Offset(center.x - rx * 0.86f, topCenterY - ry * 0.72f),
        size = Size(rx * 1.72f, ry * 1.44f)
    )

    // Outer embossed gold ring
    drawOval(
        color = Color(0xFFFFF3B0),
        topLeft = Offset(center.x - rx * 0.82f, topCenterY - ry * 0.68f),
        size = Size(rx * 1.64f, ry * 1.36f),
        style = Stroke(width = 2.2f)
    )

    // Sculpted Golden Owl Silhouette inside center medallion
    val owlScale = rx * 0.56f
    val owlCenter = Offset(center.x, topCenterY + ry * 0.05f)

    // Owl body & wings
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFDA66), Color(0xFFB57614))
        ),
        topLeft = Offset(owlCenter.x - owlScale * 0.68f, owlCenter.y - owlScale * 0.42f),
        size = Size(owlScale * 1.36f, owlScale * 1.15f)
    )
    // Wing arches
    drawArc(
        color = Color(0xFF7D4B08),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(owlCenter.x - owlScale * 0.55f, owlCenter.y - owlScale * 0.12f),
        size = Size(owlScale * 1.1f, owlScale * 0.72f),
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )

    // Owl head + ear tufts
    val headY = owlCenter.y - owlScale * 0.42f
    val earPath = Path().apply {
        moveTo(owlCenter.x - owlScale * 0.55f, headY)
        lineTo(owlCenter.x - owlScale * 0.68f, headY - owlScale * 0.42f)
        lineTo(owlCenter.x - owlScale * 0.22f, headY - owlScale * 0.22f)
        lineTo(owlCenter.x + owlScale * 0.22f, headY - owlScale * 0.22f)
        lineTo(owlCenter.x + owlScale * 0.68f, headY - owlScale * 0.42f)
        lineTo(owlCenter.x + owlScale * 0.55f, headY)
        close()
    }
    drawPath(earPath, color = Color(0xFFD69824))

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFE580), Color(0xFFC48218)),
            center = Offset(owlCenter.x, headY),
            radius = owlScale * 0.6f
        ),
        radius = owlScale * 0.52f,
        center = Offset(owlCenter.x, headY)
    )

    // Big wise Owl Eyes
    val eyeOffset = owlScale * 0.22f
    val eyeR = owlScale * 0.19f
    for (dir in listOf(-1f, 1f)) {
        val ex = owlCenter.x + dir * eyeOffset
        drawCircle(color = Color(0xFF6B3F05), radius = eyeR * 1.15f, center = Offset(ex, headY))
        drawCircle(color = Color(0xFFFFF4C2), radius = eyeR * 0.85f, center = Offset(ex, headY))
        drawCircle(color = Color(0xFF3A1F03), radius = eyeR * 0.45f, center = Offset(ex, headY))
        drawCircle(
            color = Color.White,
            radius = eyeR * 0.18f,
            center = Offset(ex - eyeR * 0.2f, headY - eyeR * 0.2f)
        )
    }

    // Golden beak
    val beak = Path().apply {
        moveTo(owlCenter.x - owlScale * 0.11f, headY + owlScale * 0.08f)
        lineTo(owlCenter.x + owlScale * 0.11f, headY + owlScale * 0.08f)
        lineTo(owlCenter.x, headY + owlScale * 0.28f)
        close()
    }
    drawPath(beak, color = Color(0xFF8C4A00))
}

private fun DrawScope.drawCaptureBurst(center: Offset, maxRadius: Float, progress: Float) {
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val radius = maxRadius * (0.25f + 0.75f * progress)
    for (i in 0 until 10) {
        val angle = i * (2.0 * PI / 10.0) + progress * 0.6
        val start = Offset(
            center.x + (cos(angle) * radius * 0.35f).toFloat(),
            center.y + (sin(angle) * radius * 0.35f).toFloat()
        )
        val end = Offset(
            center.x + (cos(angle) * radius).toFloat(),
            center.y + (sin(angle) * radius).toFloat()
        )
        drawLine(
            color = if (i % 2 == 0) TempleGoldBright.copy(alpha = alpha) else Color(0xFFFF6D5A).copy(alpha = alpha),
            start = start,
            end = end,
            strokeWidth = (6f * (1f - progress * 0.5f)).coerceAtLeast(1.5f),
            cap = StrokeCap.Round
        )
    }
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.65f),
        radius = radius * 0.45f,
        center = center,
        style = Stroke(width = 4f)
    )
}
