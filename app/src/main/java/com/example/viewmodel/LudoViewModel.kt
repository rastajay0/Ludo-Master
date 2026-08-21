package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectManager
import com.example.engine.LudoBoardCoordinates
import com.example.engine.LudoGameEngine
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LudoUiState(
    val screen: GameScreen = GameScreen.SETUP,
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val diceRoll: Int = 6,
    val isDiceRolling: Boolean = false,
    val consecutiveSixes: Int = 0,
    val validMoves: List<ValidMove> = emptyList(),
    val selectedToken: Token? = null,
    val gamePhase: GamePhase = GamePhase.SETUP,
    val animatingTokenId: Int? = null,
    val animatingPlayerColor: PlayerColor? = null,
    val animatingPath: List<Pair<Float, Float>> = emptyList(),
    val animatingCurrentPos: Pair<Float, Float>? = null,
    val gameLogs: List<GameLogEntry> = emptyList(),
    val statistics: MatchStatistics = MatchStatistics(),
    val settings: GameSettings = GameSettings(),
    val winner: Player? = null,
    val rankedWinners: List<Player> = emptyList(),
    val showRulesDialog: Boolean = false,
    val showGameMenu: Boolean = false,
    val celebrationActive: Boolean = false
)

enum class GameScreen {
    SETUP,
    GAME
}

class LudoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LudoUiState())
    val uiState: StateFlow<LudoUiState> = _uiState.asStateFlow()

    val soundManager = SoundEffectManager()

    private var botTurnJob: Job? = null
    private var gameStartTime: Long = 0

    init {
        // Initialize default setup
        setupDefaultPlayers(playerCount = 4, vsBot = true, botDifficulty = BotDifficulty.MEDIUM)
    }

    fun setupDefaultPlayers(
        playerCount: Int = 4,
        vsBot: Boolean = true,
        botDifficulty: BotDifficulty = BotDifficulty.MEDIUM
    ) {
        val colors = when (playerCount) {
            2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
            3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
            else -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
        }

        val players = colors.mapIndexed { index, color ->
            val isBot = if (vsBot) index > 0 else false
            val name = if (isBot) "${color.displayName} Bot" else "Player ${index + 1} (${color.displayName})"
            Player(
                color = color,
                name = name,
                isBot = isBot,
                botDifficulty = botDifficulty
            )
        }

        _uiState.update {
            it.copy(
                players = players,
                currentPlayerIndex = 0,
                gamePhase = GamePhase.SETUP
            )
        }
    }

    fun startGame(customPlayers: List<Player>) {
        botTurnJob?.cancel()
        gameStartTime = System.currentTimeMillis()

        _uiState.update {
            it.copy(
                screen = GameScreen.GAME,
                players = customPlayers,
                currentPlayerIndex = 0,
                diceRoll = 6,
                isDiceRolling = false,
                consecutiveSixes = 0,
                validMoves = emptyList(),
                selectedToken = null,
                gamePhase = GamePhase.WAITING_FOR_ROLL,
                animatingTokenId = null,
                animatingPlayerColor = null,
                gameLogs = listOf(
                    GameLogEntry(
                        message = "Match started! ${customPlayers[0].name}'s turn.",
                        playerColor = customPlayers[0].color,
                        type = LogType.NORMAL
                    )
                ),
                statistics = MatchStatistics(),
                winner = null,
                rankedWinners = emptyList(),
                celebrationActive = false
            )
        }

        checkAndTriggerBotTurn()
    }

    fun rollDice() {
        val state = _uiState.value
        if (state.gamePhase != GamePhase.WAITING_FOR_ROLL || state.isDiceRolling) return

        val currentPlayer = state.players[state.currentPlayerIndex]

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDiceRolling = true,
                    gamePhase = GamePhase.ROLLING_DICE
                )
            }

            // Dice rolling sound & visual vibration
            soundManager.playDiceRoll()
            for (i in 0 until 6) {
                _uiState.update { it.copy(diceRoll = Random.nextInt(1, 7)) }
                delay(70)
            }

            val finalRoll = Random.nextInt(1, 7)
            soundManager.playDiceLand()

            val newConsecutiveSixes = if (finalRoll == 6) state.consecutiveSixes + 1 else 0

            // 3 Sixes rule penalty check
            val penalizeThreeSixes = state.settings.threeSixesPenalty && newConsecutiveSixes == 3

            _uiState.update {
                it.copy(
                    diceRoll = finalRoll,
                    isDiceRolling = false,
                    consecutiveSixes = if (penalizeThreeSixes) 0 else newConsecutiveSixes,
                    statistics = it.statistics.copy(
                        totalRolls = it.statistics.totalRolls + 1,
                        sixesRolled = it.statistics.sixesRolled + (if (finalRoll == 6) 1 else 0)
                    )
                )
            }

            if (penalizeThreeSixes) {
                addLog("${currentPlayer.name} rolled 3 sixes in a row! Turn forfeited.", currentPlayer.color, LogType.NORMAL)
                delay(900)
                advanceToNextTurn(extraRoll = false)
                return@launch
            }

            // Calculate valid moves
            val validMoves = LudoGameEngine.getValidMoves(
                player = currentPlayer,
                diceRoll = finalRoll,
                allPlayers = _uiState.value.players
            )

            addLog("${currentPlayer.name} rolled a $finalRoll!", currentPlayer.color, LogType.ROLL)

            if (validMoves.isEmpty()) {
                _uiState.update {
                    it.copy(
                        validMoves = emptyList(),
                        gamePhase = GamePhase.WAITING_FOR_ROLL
                    )
                }
                delay(if (state.settings.fastAnimation) 400 else 750)
                advanceToNextTurn(extraRoll = false)
            } else {
                _uiState.update {
                    it.copy(
                        validMoves = validMoves,
                        gamePhase = GamePhase.SELECTING_TOKEN
                    )
                }

                if (currentPlayer.isBot) {
                    delay(if (state.settings.fastAnimation) 350 else 600)
                    val bestMove = LudoGameEngine.chooseBestBotMove(
                        validMoves = validMoves,
                        botPlayer = currentPlayer,
                        allPlayers = _uiState.value.players,
                        difficulty = currentPlayer.botDifficulty
                    )
                    bestMove?.let { executeMove(it) }
                }
            }
        }
    }

    fun onTokenClicked(token: Token) {
        val state = _uiState.value
        if (state.gamePhase != GamePhase.SELECTING_TOKEN) return

        val currentPlayer = state.players[state.currentPlayerIndex]
        if (currentPlayer.isBot) return // Human clicks only
        if (token.playerColor != currentPlayer.color) return

        val move = state.validMoves.find { it.token.id == token.id } ?: return
        executeMove(move)
    }

    private fun executeMove(move: ValidMove) {
        val state = _uiState.value
        if (state.gamePhase == GamePhase.ANIMATING_MOVE) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    gamePhase = GamePhase.ANIMATING_MOVE,
                    animatingTokenId = move.token.id,
                    animatingPlayerColor = move.token.playerColor,
                    validMoves = emptyList()
                )
            }

            val intermediateSteps = LudoBoardCoordinates.getIntermediatePositions(
                token = move.token,
                targetState = move.targetState,
                targetTrackStep = move.targetTrackStep,
                targetHomeStep = move.targetHomeStep
            )

            val stepDelay = if (state.settings.fastAnimation) 65L else 115L

            if (move.token.isInYard && move.targetState == TokenState.ON_TRACK) {
                soundManager.playTokenOut()
            }

            // Animate stepping through each grid position
            for (pos in intermediateSteps) {
                _uiState.update { it.copy(animatingCurrentPos = pos) }
                soundManager.playTokenStep()
                delay(stepDelay)
            }

            // Apply move to player state
            val capturedTokens = if (move.targetState == TokenState.ON_TRACK) {
                LudoGameEngine.findCapturedTokens(
                    actingPlayerColor = move.token.playerColor,
                    targetTrackStep = move.targetTrackStep,
                    allPlayers = _uiState.value.players
                )
            } else emptyList()

            val hasCapture = capturedTokens.isNotEmpty()
            if (hasCapture) {
                soundManager.playCapture()
                for (cap in capturedTokens) {
                    val capPlayer = _uiState.value.players.find { it.color == cap.playerColor }
                    addLog("${move.token.playerColor.displayName} captured ${capPlayer?.name ?: cap.playerColor.displayName}'s token!", move.token.playerColor, LogType.CAPTURE)
                }
            } else if (move.willFinish) {
                soundManager.playHomeEntry()
                addLog("${move.token.playerColor.displayName}'s token reached Home Goal!", move.token.playerColor, LogType.ENTER_HOME)
            } else if (move.willEnterSafeZone) {
                soundManager.playSafeStar()
            }

            // Update all tokens
            val updatedPlayers = _uiState.value.players.map { player ->
                if (player.color == move.token.playerColor) {
                    val updatedTokens = player.tokens.map { tok ->
                        if (tok.id == move.token.id) {
                            tok.copy(
                                state = move.targetState,
                                trackStep = move.targetTrackStep,
                                homeStep = move.targetHomeStep
                            )
                        } else tok
                    }
                    player.copy(tokens = updatedTokens)
                } else if (capturedTokens.any { it.playerColor == player.color }) {
                    // Send captured tokens back to yard
                    val updatedTokens = player.tokens.map { tok ->
                        if (capturedTokens.any { it.playerColor == tok.playerColor && it.id == tok.id }) {
                            tok.copy(
                                state = TokenState.IN_YARD,
                                trackStep = -1,
                                homeStep = -1
                            )
                        } else tok
                    }
                    player.copy(tokens = updatedTokens)
                } else {
                    player
                }
            }

            _uiState.update {
                it.copy(
                    players = updatedPlayers,
                    animatingTokenId = null,
                    animatingPlayerColor = null,
                    animatingCurrentPos = null,
                    statistics = it.statistics.copy(
                        totalCaptures = it.statistics.totalCaptures + capturedTokens.size,
                        totalStepsMoved = it.statistics.totalStepsMoved + intermediateSteps.size
                    )
                )
            }

            // Check Win Condition
            val movingPlayer = updatedPlayers.find { it.color == move.token.playerColor }!!
            if (movingPlayer.hasWon && movingPlayer.rank == null) {
                val currentRanked = _uiState.value.rankedWinners
                val newRank = currentRanked.size + 1
                val rankedPlayer = movingPlayer.copy(rank = newRank)
                val newRankedList = currentRanked + rankedPlayer

                val updatedPlayersWithRank = updatedPlayers.map {
                    if (it.color == rankedPlayer.color) rankedPlayer else it
                }

                soundManager.playVictory()
                addLog("🏆 ${rankedPlayer.name} finished in Rank #$newRank!", rankedPlayer.color, LogType.WIN)

                _uiState.update {
                    it.copy(
                        players = updatedPlayersWithRank,
                        rankedWinners = newRankedList,
                        winner = if (newRank == 1) rankedPlayer else it.winner,
                        celebrationActive = true
                    )
                }

                // If only 1 player remains without winning, end game
                val remainingPlayers = updatedPlayersWithRank.filter { it.rank == null }
                if (remainingPlayers.size <= 1 || newRank == 1) {
                    _uiState.update {
                        it.copy(
                            gamePhase = GamePhase.GAME_OVER,
                            celebrationActive = true
                        )
                    }
                    return@launch
                }
            }

            // Determine if extra turn is granted
            val gotSix = _uiState.value.diceRoll == 6
            val bonusFromCapture = state.settings.captureBonusRoll && hasCapture
            val bonusFromHome = state.settings.homeBonusRoll && move.willFinish
            val getsExtraRoll = gotSix || bonusFromCapture || bonusFromHome

            if (getsExtraRoll && !_uiState.value.players[_uiState.value.currentPlayerIndex].hasWon) {
                val reason = when {
                    gotSix -> "Rolled a 6! Extra roll."
                    bonusFromCapture -> "Token captured! Bonus roll."
                    else -> "Reached home! Bonus roll."
                }
                addLog("${movingPlayer.name} gets a bonus turn ($reason)", movingPlayer.color, LogType.NORMAL)
                delay(if (state.settings.fastAnimation) 300 else 500)
                _uiState.update { it.copy(gamePhase = GamePhase.WAITING_FOR_ROLL) }
                checkAndTriggerBotTurn()
            } else {
                advanceToNextTurn(extraRoll = false)
            }
        }
    }

    private fun advanceToNextTurn(extraRoll: Boolean) {
        val state = _uiState.value
        val activePlayers = state.players
        if (activePlayers.isEmpty()) return

        var nextIndex = state.currentPlayerIndex
        var attempts = 0

        do {
            nextIndex = (nextIndex + 1) % activePlayers.size
            attempts++
        } while (activePlayers[nextIndex].hasWon && attempts < activePlayers.size)

        if (attempts >= activePlayers.size) {
            // All finished
            _uiState.update { it.copy(gamePhase = GamePhase.GAME_OVER) }
            return
        }

        val nextPlayer = activePlayers[nextIndex]

        _uiState.update {
            it.copy(
                currentPlayerIndex = nextIndex,
                diceRoll = it.diceRoll,
                isDiceRolling = false,
                consecutiveSixes = 0,
                validMoves = emptyList(),
                selectedToken = null,
                gamePhase = GamePhase.WAITING_FOR_ROLL,
                statistics = it.statistics.copy(turnsCount = it.statistics.turnsCount + 1)
            )
        }

        checkAndTriggerBotTurn()
    }

    private fun checkAndTriggerBotTurn() {
        botTurnJob?.cancel()
        val state = _uiState.value
        if (state.gamePhase == GamePhase.GAME_OVER) return

        val currentPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return
        if (currentPlayer.isBot && !currentPlayer.hasWon) {
            botTurnJob = viewModelScope.launch {
                delay(if (state.settings.fastAnimation) 400 else 750)
                rollDice()
            }
        }
    }

    private fun addLog(message: String, color: PlayerColor, type: LogType) {
        _uiState.update {
            val updated = (listOf(GameLogEntry(message = message, playerColor = color, type = type)) + it.gameLogs).take(25)
            it.copy(gameLogs = updated)
        }
    }

    fun toggleSound() {
        _uiState.update {
            val newSettings = it.settings.copy(soundEnabled = !it.settings.soundEnabled)
            soundManager.setMuted(!newSettings.soundEnabled)
            it.copy(settings = newSettings)
        }
    }

    fun toggleFastSpeed() {
        _uiState.update {
            it.copy(settings = it.settings.copy(fastAnimation = !it.settings.fastAnimation))
        }
    }

    fun setRulesDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showRulesDialog = visible) }
    }

    fun setGameMenuVisible(visible: Boolean) {
        _uiState.update { it.copy(showGameMenu = visible) }
    }

    fun returnToSetup() {
        botTurnJob?.cancel()
        _uiState.update {
            it.copy(
                screen = GameScreen.SETUP,
                gamePhase = GamePhase.SETUP,
                winner = null,
                celebrationActive = false
            )
        }
    }

    fun restartCurrentGame() {
        val currentPlayers = _uiState.value.players.map { player ->
            player.copy(
                tokens = (0..3).map { Token(id = it, playerColor = player.color) },
                rank = null
            )
        }
        startGame(currentPlayers)
    }
}
