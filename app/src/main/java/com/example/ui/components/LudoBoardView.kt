package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LudoBoardCoordinates
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.Token
import com.example.model.TokenState
import com.example.model.ValidMove

@Composable
fun LudoBoardView(
    players: List<Player>,
    validMoves: List<ValidMove>,
    animatingTokenId: Int?,
    animatingPlayerColor: PlayerColor?,
    animatingCurrentPos: Pair<Float, Float>?,
    onTokenClick: (Token) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for movable tokens
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0F172A))
            .testTag("ludo_board_container"),
        contentAlignment = Alignment.Center
    ) {
        val boardSize = maxWidth
        val cellSize = boardSize / 15f

        // 1. Draw static Board Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLudoBoard(size = this.size)
        }

        // 2. Overlay Tokens
        // Collect all stationary tokens and group by grid coordinates for stacking offsets
        val activeTokenPositions = mutableListOf<TokenDisplayData>()

        players.forEach { player ->
            player.tokens.forEach { token ->
                val isCurrentlyAnimating = animatingTokenId == token.id && animatingPlayerColor == token.playerColor
                if (isCurrentlyAnimating && animatingCurrentPos != null) {
                    activeTokenPositions.add(
                        TokenDisplayData(
                            token = token,
                            gridPos = animatingCurrentPos,
                            isAnimating = true,
                            isValidMove = false
                        )
                    )
                } else if (!isCurrentlyAnimating) {
                    val pos = LudoBoardCoordinates.getTokenGridPosition(token)
                    val isValid = validMoves.any { it.token.id == token.id && it.token.playerColor == token.playerColor }
                    activeTokenPositions.add(
                        TokenDisplayData(
                            token = token,
                            gridPos = pos,
                            isAnimating = false,
                            isValidMove = isValid
                        )
                    )
                }
            }
        }

        // Group tokens on the same cell to calculate multi-token offsets
        val groupedByCell = activeTokenPositions.groupBy {
            // Round to 1 decimal place to group items in same cell
            "${(it.gridPos.first * 10).toInt()}_${(it.gridPos.second * 10).toInt()}"
        }

        groupedByCell.forEach { (_, tokensInCell) ->
            tokensInCell.forEachIndexed { index, tokenData ->
                val totalInCell = tokensInCell.size
                val offsetFraction = calculateTokenOffset(index, totalInCell)

                val leftOffset = (tokenData.gridPos.first + offsetFraction.first) * cellSize.value
                val topOffset = (tokenData.gridPos.second + offsetFraction.second) * cellSize.value

                Box(
                    modifier = Modifier
                        .offset(x = leftOffset.dp, y = topOffset.dp)
                        .size(cellSize)
                        .padding(if (totalInCell > 1) 1.dp else 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TokenPiece(
                        token = tokenData.token,
                        isValidMove = tokenData.isValidMove,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                        size = cellSize * if (totalInCell > 1) 0.85f else 0.95f,
                        onClick = { onTokenClick(tokenData.token) }
                    )
                }
            }
        }
    }
}

private data class TokenDisplayData(
    val token: Token,
    val gridPos: Pair<Float, Float>,
    val isAnimating: Boolean,
    val isValidMove: Boolean
)

private fun calculateTokenOffset(index: Int, total: Int): Pair<Float, Float> {
    if (total <= 1) return Pair(0f, 0f)
    return when (total) {
        2 -> if (index == 0) Pair(-0.16f, -0.16f) else Pair(0.16f, 0.16f)
        3 -> when (index) {
            0 -> Pair(-0.16f, -0.16f)
            1 -> Pair(0.16f, -0.16f)
            else -> Pair(0f, 0.18f)
        }
        else -> when (index % 4) {
            0 -> Pair(-0.18f, -0.18f)
            1 -> Pair(0.18f, -0.18f)
            2 -> Pair(-0.18f, 0.18f)
            else -> Pair(0.18f, 0.18f)
        }
    }
}

@Composable
fun TokenPiece(
    token: Token,
    isValidMove: Boolean,
    pulseScale: Float,
    pulseAlpha: Float,
    size: Dp,
    onClick: () -> Unit
) {
    val color = token.playerColor
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isValidMove,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Selection Halo Glow
        if (isValidMove) {
            Box(
                modifier = Modifier
                    .size(size * pulseScale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = pulseAlpha))
                    .border(2.dp, Color(0xFFFFD700), CircleShape)
            )
        }

        // 3D Styled Token Disc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Drop shadow
            drawCircle(
                color = Color(0x66000000),
                radius = radius * 0.95f,
                center = center + Offset(0f, radius * 0.12f)
            )

            // Outer ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.lightColor, color.primaryColor, color.darkColor),
                    center = center - Offset(radius * 0.2f, radius * 0.2f),
                    radius = radius
                ),
                radius = radius * 0.9f,
                center = center
            )

            // White rim
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.65f,
                center = center,
                style = Stroke(width = radius * 0.16f)
            )

            // Inner core badge
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, color.primaryColor),
                    center = center - Offset(radius * 0.1f, radius * 0.1f),
                    radius = radius * 0.5f
                ),
                radius = radius * 0.45f,
                center = center
            )

            // Star symbol or number
            if (token.state == TokenState.FINISHED) {
                // Crown / Star
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = radius * 0.28f,
                    center = center
                )
            }
        }
    }
}

private fun DrawScope.drawLudoBoard(size: Size) {
    val cellSize = size.width / 15f

    // 1. Draw Outer Board Canvas Background
    drawRect(
        color = Color(0xFFF8FAFC),
        size = size
    )

    // 2. Draw 4 Corner Home Bases (6x6 cells each)
    val redColor = PlayerColor.RED.primaryColor
    val greenColor = PlayerColor.GREEN.primaryColor
    val yellowColor = PlayerColor.YELLOW.primaryColor
    val blueColor = PlayerColor.BLUE.primaryColor

    // Red Yard (Top-Left: 0..5, 0..5)
    drawYardBase(0f, 0f, cellSize * 6, redColor, PlayerColor.RED)
    // Green Yard (Top-Right: 9..14, 0..5)
    drawYardBase(cellSize * 9, 0f, cellSize * 6, greenColor, PlayerColor.GREEN)
    // Yellow Yard (Bottom-Right: 9..14, 9..14)
    drawYardBase(cellSize * 9, cellSize * 9, cellSize * 6, yellowColor, PlayerColor.YELLOW)
    // Blue Yard (Bottom-Left: 0..5, 9..14)
    drawYardBase(0f, cellSize * 9, cellSize * 6, blueColor, PlayerColor.BLUE)

    // 3. Draw Track Grid cells (Lines & borders)
    val gridBorderColor = Color(0xFFCBD5E1)
    val stroke = Stroke(width = 1.5f)

    for (row in 0 until 15) {
        for (col in 0 until 15) {
            // Only draw middle cross tracks (exclude 6x6 corners and center 3x3)
            val inCorner = (col < 6 && row < 6) || (col >= 9 && row < 6) ||
                    (col < 6 && row >= 9) || (col >= 9 && row >= 9)
            val inCenter = col in 6..8 && row in 6..8

            if (!inCorner && !inCenter) {
                val left = col * cellSize
                val top = row * cellSize
                drawRect(
                    color = Color.White,
                    topLeft = Offset(left, top),
                    size = Size(cellSize, cellSize)
                )
                drawRect(
                    color = gridBorderColor,
                    topLeft = Offset(left, top),
                    size = Size(cellSize, cellSize),
                    style = stroke
                )
            }
        }
    }

    // 4. Fill Starting Cells with Player Colors
    // Red Start: (1, 6)
    drawColoredTrackCell(1, 6, cellSize, redColor, withStar = false, withArrow = true, arrowDirection = ArrowDir.RIGHT)
    // Green Start: (8, 1)
    drawColoredTrackCell(8, 1, cellSize, greenColor, withStar = false, withArrow = true, arrowDirection = ArrowDir.DOWN)
    // Yellow Start: (13, 8)
    drawColoredTrackCell(13, 8, cellSize, yellowColor, withStar = false, withArrow = true, arrowDirection = ArrowDir.LEFT)
    // Blue Start: (6, 13)
    drawColoredTrackCell(6, 13, cellSize, blueColor, withStar = false, withArrow = true, arrowDirection = ArrowDir.UP)

    // 5. Fill Home Runways (5 cells each)
    // Red Home Path: (1..5, 7)
    for (c in 1..5) drawColoredTrackCell(c, 7, cellSize, redColor, withStar = false, withArrow = false)
    // Green Home Path: (7, 1..5)
    for (r in 1..5) drawColoredTrackCell(7, r, cellSize, greenColor, withStar = false, withArrow = false)
    // Yellow Home Path: (9..13, 7)
    for (c in 9..13) drawColoredTrackCell(c, 7, cellSize, yellowColor, withStar = false, withArrow = false)
    // Blue Home Path: (7, 9..13)
    for (r in 9..13) drawColoredTrackCell(7, r, cellSize, blueColor, withStar = false, withArrow = false)

    // 6. Draw Safe Stars on 4 Track Star Cells
    // (6, 2), (12, 6), (8, 12), (2, 8)
    drawStarInCell(6, 2, cellSize, Color(0xFF475569))
    drawStarInCell(12, 6, cellSize, Color(0xFF475569))
    drawStarInCell(8, 12, cellSize, Color(0xFF475569))
    drawStarInCell(2, 8, cellSize, Color(0xFF475569))

    // Also safe stars on starting cells
    drawStarInCell(1, 6, cellSize, Color.White)
    drawStarInCell(8, 1, cellSize, Color.White)
    drawStarInCell(13, 8, cellSize, Color.White)
    drawStarInCell(6, 13, cellSize, Color.White)

    // 7. Draw Center Finish Zone (3x3 with 4 Triangles)
    val centerLeft = 6 * cellSize
    val centerTop = 6 * cellSize
    val centerRight = 9 * cellSize
    val centerBottom = 9 * cellSize
    val centerMidX = 7.5f * cellSize
    val centerMidY = 7.5f * cellSize

    // Top Triangle (Green)
    val greenPath = Path().apply {
        moveTo(centerLeft, centerTop)
        lineTo(centerRight, centerTop)
        lineTo(centerMidX, centerMidY)
        close()
    }
    drawPath(greenPath, greenColor)

    // Right Triangle (Yellow)
    val yellowPath = Path().apply {
        moveTo(centerRight, centerTop)
        lineTo(centerRight, centerBottom)
        lineTo(centerMidX, centerMidY)
        close()
    }
    drawPath(yellowPath, yellowColor)

    // Bottom Triangle (Blue)
    val bluePath = Path().apply {
        moveTo(centerRight, centerBottom)
        lineTo(centerLeft, centerBottom)
        lineTo(centerMidX, centerMidY)
        close()
    }
    drawPath(bluePath, blueColor)

    // Left Triangle (Red)
    val redPath = Path().apply {
        moveTo(centerLeft, centerBottom)
        lineTo(centerLeft, centerTop)
        lineTo(centerMidX, centerMidY)
        close()
    }
    drawPath(redPath, redColor)

    // Center Gold Crown Emblem / Hub
    drawCircle(
        color = Color.White,
        radius = cellSize * 0.7f,
        center = Offset(centerMidX, centerMidY)
    )
    drawCircle(
        color = Color(0xFFFFD700),
        radius = cellSize * 0.55f,
        center = Offset(centerMidX, centerMidY)
    )
    drawCircle(
        color = Color(0xFFB45309),
        radius = cellSize * 0.55f,
        center = Offset(centerMidX, centerMidY),
        style = Stroke(width = 2.5f)
    )

    // Board outer border
    drawRect(
        color = Color(0xFF0F172A),
        size = size,
        style = Stroke(width = 4.dp.toPx())
    )
}

private fun DrawScope.drawYardBase(
    left: Float,
    top: Float,
    yardDimension: Float,
    baseColor: Color,
    playerColor: PlayerColor
) {
    // Outer colored block
    drawRect(
        color = baseColor,
        topLeft = Offset(left, top),
        size = Size(yardDimension, yardDimension)
    )

    // White inner rounded plate
    val inset = yardDimension * 0.13f
    val innerSize = yardDimension - (inset * 2)
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(left + inset, top + inset),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
    )

    // 4 Token circles in 2x2 layout inside white plate
    val slotPositions = LudoBoardCoordinates.YARD_SLOT_CELLS[playerColor] ?: emptyList()
    val cellSize = yardDimension / 6f
    val circleRadius = cellSize * 0.65f

    for (slot in slotPositions) {
        val cx = slot.first * cellSize
        val cy = slot.second * cellSize

        // Slot disc with subtle colored gradient
        drawCircle(
            color = playerColor.containerColor,
            radius = circleRadius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = baseColor,
            radius = circleRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = baseColor.copy(alpha = 0.35f),
            radius = circleRadius * 0.45f,
            center = Offset(cx, cy)
        )
    }
}

private fun DrawScope.drawColoredTrackCell(
    col: Int,
    row: Int,
    cellSize: Float,
    color: Color,
    withStar: Boolean = false,
    withArrow: Boolean = false,
    arrowDirection: ArrowDir = ArrowDir.RIGHT
) {
    val left = col * cellSize
    val top = row * cellSize

    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(cellSize, cellSize)
    )
    drawRect(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = Offset(left, top),
        size = Size(cellSize, cellSize),
        style = Stroke(width = 1.5f)
    )

    if (withArrow) {
        val cx = left + cellSize / 2f
        val cy = top + cellSize / 2f
        val arrowSize = cellSize * 0.35f
        val arrowPath = Path()

        when (arrowDirection) {
            ArrowDir.RIGHT -> {
                arrowPath.moveTo(cx - arrowSize, cy - arrowSize)
                arrowPath.lineTo(cx + arrowSize * 0.8f, cy)
                arrowPath.lineTo(cx - arrowSize, cy + arrowSize)
                arrowPath.close()
            }
            ArrowDir.DOWN -> {
                arrowPath.moveTo(cx - arrowSize, cy - arrowSize)
                arrowPath.lineTo(cx, cy + arrowSize * 0.8f)
                arrowPath.lineTo(cx + arrowSize, cy - arrowSize)
                arrowPath.close()
            }
            ArrowDir.LEFT -> {
                arrowPath.moveTo(cx + arrowSize, cy - arrowSize)
                arrowPath.lineTo(cx - arrowSize * 0.8f, cy)
                arrowPath.lineTo(cx + arrowSize, cy + arrowSize)
                arrowPath.close()
            }
            ArrowDir.UP -> {
                arrowPath.moveTo(cx - arrowSize, cy + arrowSize)
                arrowPath.lineTo(cx, cy - arrowSize * 0.8f)
                arrowPath.lineTo(cx + arrowSize, cy + arrowSize)
                arrowPath.close()
            }
        }
        drawPath(arrowPath, Color.White.copy(alpha = 0.75f))
    }
}

private fun DrawScope.drawStarInCell(col: Int, row: Int, cellSize: Float, color: Color) {
    val cx = (col + 0.5f) * cellSize
    val cy = (row + 0.5f) * cellSize
    val outerRadius = cellSize * 0.32f
    val innerRadius = cellSize * 0.14f

    val starPath = Path()
    val numPoints = 5
    var angle = -Math.PI / 2

    for (i in 0 until numPoints * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = (cx + r * kotlin.math.cos(angle)).toFloat()
        val y = (cy + r * kotlin.math.sin(angle)).toFloat()

        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
        angle += Math.PI / numPoints
    }
    starPath.close()

    drawPath(starPath, color)
}

private enum class ArrowDir {
    UP, DOWN, LEFT, RIGHT
}
