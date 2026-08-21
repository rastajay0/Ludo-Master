package com.example.engine

import com.example.model.*
import kotlin.random.Random

object LudoGameEngine {

    /**
     * Compute all valid moves for the given player and dice roll.
     */
    fun getValidMoves(
        player: Player,
        diceRoll: Int,
        allPlayers: List<Player>
    ): List<ValidMove> {
        val validMoves = mutableListOf<ValidMove>()

        for (token in player.tokens) {
            when (token.state) {
                TokenState.IN_YARD -> {
                    // Token can only exit yard if dice roll is 6
                    if (diceRoll == 6) {
                        // Target is starting track square (trackStep = 0)
                        val willCapture = checkCaptureAtTrackStep(player.color, 0, allPlayers)
                        validMoves.add(
                            ValidMove(
                                token = token,
                                targetState = TokenState.ON_TRACK,
                                targetTrackStep = 0,
                                targetHomeStep = -1,
                                willCapture = willCapture,
                                willFinish = false,
                                willEnterSafeZone = true // start square is safe
                            )
                        )
                    }
                }
                TokenState.ON_TRACK -> {
                    val currentStep = token.trackStep
                    val nextStep = currentStep + diceRoll

                    if (nextStep <= 50) {
                        // Remains on universal track
                        val willCapture = checkCaptureAtTrackStep(player.color, nextStep, allPlayers)
                        val globalIdx = LudoBoardCoordinates.getGlobalTrackIndex(player.color, nextStep)
                        val isSafe = LudoBoardCoordinates.SAFE_TRACK_INDICES.contains(globalIdx)
                        validMoves.add(
                            ValidMove(
                                token = token,
                                targetState = TokenState.ON_TRACK,
                                targetTrackStep = nextStep,
                                targetHomeStep = -1,
                                willCapture = willCapture,
                                willFinish = false,
                                willEnterSafeZone = isSafe
                            )
                        )
                    } else {
                        // Enters or progresses into home stretch
                        // nextStep 51 -> homeStep 0
                        // nextStep 52 -> homeStep 1
                        // ...
                        // nextStep 56 -> homeStep 5 (Goal finish!)
                        val homeStep = nextStep - 51
                        if (homeStep < 5) {
                            validMoves.add(
                                ValidMove(
                                    token = token,
                                    targetState = TokenState.IN_HOME_STRETCH,
                                    targetTrackStep = -1,
                                    targetHomeStep = homeStep,
                                    willCapture = false,
                                    willFinish = false,
                                    willEnterSafeZone = true // home stretch is safe
                                )
                            )
                        } else if (homeStep == 5) {
                            // Exact roll to finish!
                            validMoves.add(
                                ValidMove(
                                    token = token,
                                    targetState = TokenState.FINISHED,
                                    targetTrackStep = -1,
                                    targetHomeStep = 5,
                                    willCapture = false,
                                    willFinish = true,
                                    willEnterSafeZone = true
                                )
                            )
                        }
                        // If homeStep > 5, it overshoots the finish, so invalid!
                    }
                }
                TokenState.IN_HOME_STRETCH -> {
                    val nextHomeStep = token.homeStep + diceRoll
                    if (nextHomeStep < 5) {
                        validMoves.add(
                            ValidMove(
                                token = token,
                                targetState = TokenState.IN_HOME_STRETCH,
                                targetTrackStep = -1,
                                targetHomeStep = nextHomeStep,
                                willCapture = false,
                                willFinish = false,
                                willEnterSafeZone = true
                            )
                        )
                    } else if (nextHomeStep == 5) {
                        validMoves.add(
                            ValidMove(
                                token = token,
                                targetState = TokenState.FINISHED,
                                targetTrackStep = -1,
                                targetHomeStep = 5,
                                willCapture = false,
                                willFinish = true,
                                willEnterSafeZone = true
                            )
                        )
                    }
                    // If nextHomeStep > 5, cannot overshoot
                }
                TokenState.FINISHED -> {
                    // Cannot move finished tokens
                }
            }
        }

        return validMoves
    }

    /**
     * Checks if moving to a specific track step for a player results in capturing an opponent token.
     */
    fun checkCaptureAtTrackStep(
        playerColor: PlayerColor,
        targetTrackStep: Int,
        allPlayers: List<Player>
    ): Boolean {
        val targetGlobalIndex = LudoBoardCoordinates.getGlobalTrackIndex(playerColor, targetTrackStep)

        // Cannot capture in safe squares
        if (LudoBoardCoordinates.SAFE_TRACK_INDICES.contains(targetGlobalIndex)) {
            return false
        }

        for (opponent in allPlayers) {
            if (opponent.color == playerColor) continue
            for (oppToken in opponent.tokens) {
                if (oppToken.state == TokenState.ON_TRACK) {
                    val oppGlobalIndex = LudoBoardCoordinates.getGlobalTrackIndex(opponent.color, oppToken.trackStep)
                    if (oppGlobalIndex == targetGlobalIndex) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Find captured tokens if any at destination
     */
    fun findCapturedTokens(
        actingPlayerColor: PlayerColor,
        targetTrackStep: Int,
        allPlayers: List<Player>
    ): List<Token> {
        val captured = mutableListOf<Token>()
        val targetGlobalIndex = LudoBoardCoordinates.getGlobalTrackIndex(actingPlayerColor, targetTrackStep)

        if (LudoBoardCoordinates.SAFE_TRACK_INDICES.contains(targetGlobalIndex)) {
            return captured
        }

        for (opponent in allPlayers) {
            if (opponent.color == actingPlayerColor) continue
            for (oppToken in opponent.tokens) {
                if (oppToken.state == TokenState.ON_TRACK) {
                    val oppGlobal = LudoBoardCoordinates.getGlobalTrackIndex(opponent.color, oppToken.trackStep)
                    if (oppGlobal == targetGlobalIndex) {
                        captured.add(oppToken)
                    }
                }
            }
        }
        return captured
    }

    /**
     * Intelligent Bot Move Picker
     */
    fun chooseBestBotMove(
        validMoves: List<ValidMove>,
        botPlayer: Player,
        allPlayers: List<Player>,
        difficulty: BotDifficulty
    ): ValidMove? {
        if (validMoves.isEmpty()) return null
        if (validMoves.size == 1) return validMoves.first()

        if (difficulty == BotDifficulty.EASY) {
            // Easy bot picks random valid move with a slight capture preference
            return validMoves.maxByOrNull { move ->
                var score = Random.nextInt(0, 50)
                if (move.willCapture) score += 100
                if (move.willFinish) score += 80
                score
            } ?: validMoves.random()
        }

        // Medium & Hard bots evaluate tactical weights
        val scoredMoves = validMoves.map { move ->
            var score = 0

            // 1. Capturing an opponent is huge
            if (move.willCapture) {
                score += if (difficulty == BotDifficulty.HARD) 1200 else 800
            }

            // 2. Finishing token
            if (move.willFinish) {
                score += if (difficulty == BotDifficulty.HARD) 1000 else 700
            }

            // 3. Opening new token from yard when 6 is rolled
            if (move.token.state == TokenState.IN_YARD) {
                score += if (botPlayer.activeTokensCount < 2) 600 else 400
            }

            // 4. Moving into home stretch or safe zone
            if (move.willEnterSafeZone || move.targetState == TokenState.IN_HOME_STRETCH) {
                score += 350
            }

            // 5. Escaping danger if opponent is 1..6 steps behind on track
            if (move.token.state == TokenState.ON_TRACK) {
                val currentGlobal = LudoBoardCoordinates.getGlobalTrackIndex(botPlayer.color, move.token.trackStep)
                val inDanger = isSquareInDanger(currentGlobal, botPlayer.color, allPlayers)
                if (inDanger) {
                    score += 450
                }
            }

            // 6. Avoid moving into danger
            if (move.targetState == TokenState.ON_TRACK) {
                val nextGlobal = LudoBoardCoordinates.getGlobalTrackIndex(botPlayer.color, move.targetTrackStep)
                if (!LudoBoardCoordinates.SAFE_TRACK_INDICES.contains(nextGlobal)) {
                    val willBeInDanger = isSquareInDanger(nextGlobal, botPlayer.color, allPlayers)
                    if (willBeInDanger) {
                        score -= 200
                    }
                }
            }

            // 7. General forward progress: tokens furthest along the track get slight bonus
            val progress = when (move.targetState) {
                TokenState.ON_TRACK -> move.targetTrackStep
                TokenState.IN_HOME_STRETCH -> 51 + move.targetHomeStep
                TokenState.FINISHED -> 60
                else -> 0
            }
            score += progress * 4

            Pair(move, score)
        }

        return scoredMoves.maxByOrNull { it.second }?.first ?: validMoves.first()
    }

    private fun isSquareInDanger(
        targetGlobalIndex: Int,
        myColor: PlayerColor,
        allPlayers: List<Player>
    ): Boolean {
        if (LudoBoardCoordinates.SAFE_TRACK_INDICES.contains(targetGlobalIndex)) return false

        for (opp in allPlayers) {
            if (opp.color == myColor) continue
            for (oppToken in opp.tokens) {
                if (oppToken.state == TokenState.ON_TRACK) {
                    val oppGlobal = LudoBoardCoordinates.getGlobalTrackIndex(opp.color, oppToken.trackStep)
                    // Calculate clockwise distance from opp to target
                    val dist = (targetGlobalIndex - oppGlobal + 52) % 52
                    if (dist in 1..6) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
