package com.example.engine

import androidx.compose.ui.geometry.Offset
import com.example.model.PlayerColor
import com.example.model.Token
import com.example.model.TokenState

object LudoBoardCoordinates {

    const val GRID_SIZE = 15

    // 52 Main Track Coordinates on 15x15 Grid (col, row)
    // Starting at Red's entry square (1, 6) and proceeding clockwise
    val TRACK_CELLS: List<Pair<Int, Int>> = listOf(
        // Segment 1: Left-top horizontal heading right (5 cells: index 0..4)
        Pair(1, 6),  // 0: Red Start Square
        Pair(2, 6),  // 1
        Pair(3, 6),  // 2
        Pair(4, 6),  // 3
        Pair(5, 6),  // 4
        // Segment 2: Top-left vertical heading UP (6 cells: index 5..10)
        Pair(6, 5),  // 5
        Pair(6, 4),  // 6
        Pair(6, 3),  // 7
        Pair(6, 2),  // 8: Safe Star
        Pair(6, 1),  // 9
        Pair(6, 0),  // 10
        // Segment 3: Top horizontal across (2 cells: index 11..12)
        Pair(7, 0),  // 11: Green Home entrance
        Pair(8, 0),  // 12
        // Segment 4: Top-right vertical heading DOWN (5 cells: index 13..17)
        Pair(8, 1),  // 13: Green Start Square
        Pair(8, 2),  // 14
        Pair(8, 3),  // 15
        Pair(8, 4),  // 16
        Pair(8, 5),  // 17
        // Segment 5: Right-top horizontal heading RIGHT (6 cells: index 18..23)
        Pair(9, 6),  // 18
        Pair(10, 6), // 19
        Pair(11, 6), // 20
        Pair(12, 6), // 21: Safe Star
        Pair(13, 6), // 22
        Pair(14, 6), // 23
        // Segment 6: Right vertical heading DOWN (2 cells: index 24..25)
        Pair(14, 7), // 24: Yellow Home entrance
        Pair(14, 8), // 25
        // Segment 7: Right-bottom horizontal heading LEFT (5 cells: index 26..30)
        Pair(13, 8), // 26: Yellow Start Square
        Pair(12, 8), // 27
        Pair(11, 8), // 28
        Pair(10, 8), // 29
        Pair(9, 8),  // 30
        // Segment 8: Bottom-right vertical heading DOWN (6 cells: index 31..36)
        Pair(8, 9),  // 31
        Pair(8, 10), // 32
        Pair(8, 11), // 33
        Pair(8, 12), // 34: Safe Star
        Pair(8, 13), // 35
        Pair(8, 14), // 36
        // Segment 9: Bottom horizontal heading LEFT (2 cells: index 37..38)
        Pair(7, 14), // 37: Blue Home entrance
        Pair(6, 14), // 38
        // Segment 10: Bottom-left vertical heading UP (5 cells: index 39..43)
        Pair(6, 13), // 39: Blue Start Square
        Pair(6, 12), // 40
        Pair(6, 11), // 41
        Pair(6, 10), // 42
        Pair(6, 9),  // 43
        // Segment 11: Left-bottom horizontal heading LEFT (6 cells: index 44..49)
        Pair(5, 8),  // 44
        Pair(4, 8),  // 45
        Pair(3, 8),  // 46
        Pair(2, 8),  // 47: Safe Star
        Pair(1, 8),  // 48
        Pair(0, 8),  // 49
        // Segment 12: Left vertical heading UP (2 cells: index 50..51)
        Pair(0, 7),  // 50: Red Home entrance
        Pair(0, 6)   // 51
    )

    // Safe Squares on the 52-step track (Indices)
    val SAFE_TRACK_INDICES: Set<Int> = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    // Star / Shield icon positions on track (the 4 non-start safe squares)
    val STAR_TRACK_INDICES: Set<Int> = setOf(8, 21, 34, 47)

    // Home Stretch 5 Runway Cells (col, row) for each color leading to center (7, 7)
    val HOME_STRETCH_CELLS: Map<PlayerColor, List<Pair<Int, Int>>> = mapOf(
        PlayerColor.RED to listOf(
            Pair(1, 7), Pair(2, 7), Pair(3, 7), Pair(4, 7), Pair(5, 7)
        ),
        PlayerColor.GREEN to listOf(
            Pair(7, 1), Pair(7, 2), Pair(7, 3), Pair(7, 4), Pair(7, 5)
        ),
        PlayerColor.YELLOW to listOf(
            Pair(13, 7), Pair(12, 7), Pair(11, 7), Pair(10, 7), Pair(9, 7)
        ),
        PlayerColor.BLUE to listOf(
            Pair(7, 13), Pair(7, 12), Pair(7, 11), Pair(7, 10), Pair(7, 9)
        )
    )

    // Home Finishing Coordinates in the center triangle
    val FINISH_GOAL_CELL: Map<PlayerColor, Pair<Float, Float>> = mapOf(
        PlayerColor.RED to Pair(6.4f, 7.0f),
        PlayerColor.GREEN to Pair(7.0f, 6.4f),
        PlayerColor.YELLOW to Pair(7.6f, 7.0f),
        PlayerColor.BLUE to Pair(7.0f, 7.6f)
    )

    // Yard slot coordinates (col, row) for 4 tokens inside each 6x6 base
    val YARD_SLOT_CELLS: Map<PlayerColor, List<Pair<Float, Float>>> = mapOf(
        PlayerColor.RED to listOf(
            Pair(1.8f, 1.8f), Pair(3.8f, 1.8f),
            Pair(1.8f, 3.8f), Pair(3.8f, 3.8f)
        ),
        PlayerColor.GREEN to listOf(
            Pair(10.8f, 1.8f), Pair(12.8f, 1.8f),
            Pair(10.8f, 3.8f), Pair(12.8f, 3.8f)
        ),
        PlayerColor.YELLOW to listOf(
            Pair(10.8f, 10.8f), Pair(12.8f, 10.8f),
            Pair(10.8f, 12.8f), Pair(12.8f, 12.8f)
        ),
        PlayerColor.BLUE to listOf(
            Pair(1.8f, 10.8f), Pair(3.8f, 10.8f),
            Pair(1.8f, 12.8f), Pair(3.8f, 12.8f)
        )
    )

    /**
     * Converts track step (0..50) of a player to the global 0..51 index on TRACK_CELLS.
     */
    fun getGlobalTrackIndex(playerColor: PlayerColor, trackStep: Int): Int {
        return (playerColor.startTrackIndex + trackStep) % 52
    }

    /**
     * Get grid (col, row) as Float Pair for rendering a token
     */
    fun getTokenGridPosition(token: Token): Pair<Float, Float> {
        return when (token.state) {
            TokenState.IN_YARD -> {
                val slots = YARD_SLOT_CELLS[token.playerColor] ?: return Pair(0f, 0f)
                slots.getOrElse(token.id) { slots[0] }
            }
            TokenState.ON_TRACK -> {
                val globalIdx = getGlobalTrackIndex(token.playerColor, token.trackStep)
                val cell = TRACK_CELLS[globalIdx]
                Pair(cell.first.toFloat(), cell.second.toFloat())
            }
            TokenState.IN_HOME_STRETCH -> {
                val stretch = HOME_STRETCH_CELLS[token.playerColor] ?: return Pair(7f, 7f)
                val idx = token.homeStep.coerceIn(0, stretch.size - 1)
                val cell = stretch[idx]
                Pair(cell.first.toFloat(), cell.second.toFloat())
            }
            TokenState.FINISHED -> {
                FINISH_GOAL_CELL[token.playerColor] ?: Pair(7.0f, 7.0f)
            }
        }
    }

    /**
     * Get step-by-step path between start position and target position for smooth animation
     */
    fun getIntermediatePositions(token: Token, targetState: TokenState, targetTrackStep: Int, targetHomeStep: Int): List<Pair<Float, Float>> {
        val list = mutableListOf<Pair<Float, Float>>()
        
        if (token.state == TokenState.IN_YARD && targetState == TokenState.ON_TRACK) {
            // Emerging from yard to start cell
            val startGlobalIdx = token.playerColor.startTrackIndex
            val cell = TRACK_CELLS[startGlobalIdx]
            list.add(Pair(cell.first.toFloat(), cell.second.toFloat()))
            return list
        }

        if (token.state == TokenState.ON_TRACK) {
            if (targetState == TokenState.ON_TRACK) {
                // Moving forward on track
                for (step in (token.trackStep + 1)..targetTrackStep) {
                    val gIdx = getGlobalTrackIndex(token.playerColor, step)
                    val c = TRACK_CELLS[gIdx]
                    list.add(Pair(c.first.toFloat(), c.second.toFloat()))
                }
            } else if (targetState == TokenState.IN_HOME_STRETCH || targetState == TokenState.FINISHED) {
                // Moving through remaining track steps to 50
                for (step in (token.trackStep + 1)..50) {
                    val gIdx = getGlobalTrackIndex(token.playerColor, step)
                    val c = TRACK_CELLS[gIdx]
                    list.add(Pair(c.first.toFloat(), c.second.toFloat()))
                }
                // Then entering home stretch
                val stretch = HOME_STRETCH_CELLS[token.playerColor] ?: emptyList()
                val maxHome = if (targetState == TokenState.FINISHED) 4 else targetHomeStep
                for (hStep in 0..maxHome) {
                    if (hStep < stretch.size) {
                        val c = stretch[hStep]
                        list.add(Pair(c.first.toFloat(), c.second.toFloat()))
                    }
                }
                if (targetState == TokenState.FINISHED) {
                    val finish = FINISH_GOAL_CELL[token.playerColor] ?: Pair(7f, 7f)
                    list.add(finish)
                }
            }
        } else if (token.state == TokenState.IN_HOME_STRETCH) {
            val stretch = HOME_STRETCH_CELLS[token.playerColor] ?: emptyList()
            if (targetState == TokenState.IN_HOME_STRETCH) {
                for (hStep in (token.homeStep + 1)..targetHomeStep) {
                    if (hStep < stretch.size) {
                        val c = stretch[hStep]
                        list.add(Pair(c.first.toFloat(), c.second.toFloat()))
                    }
                }
            } else if (targetState == TokenState.FINISHED) {
                for (hStep in (token.homeStep + 1)..4) {
                    if (hStep < stretch.size) {
                        val c = stretch[hStep]
                        list.add(Pair(c.first.toFloat(), c.second.toFloat()))
                    }
                }
                val finish = FINISH_GOAL_CELL[token.playerColor] ?: Pair(7f, 7f)
                list.add(finish)
            }
        }

        return list
    }
}
