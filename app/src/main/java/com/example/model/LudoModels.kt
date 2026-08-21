package com.example.model

import androidx.compose.ui.graphics.Color

enum class PlayerColor(
    val id: Int,
    val displayName: String,
    val primaryColor: Color,
    val darkColor: Color,
    val lightColor: Color,
    val containerColor: Color,
    val startTrackIndex: Int,       // Index on universal 52-step track
    val homeEntryTrackIndex: Int,   // Track index where player turns into home stretch
    val yardGridRow: Int,           // Yard top-left row in 15x15 grid
    val yardGridCol: Int            // Yard top-left col in 15x15 grid
) {
    RED(
        id = 0,
        displayName = "Red",
        primaryColor = Color(0xFFE53935),
        darkColor = Color(0xFFB71C1C),
        lightColor = Color(0xFFFFCDD2),
        containerColor = Color(0xFFFFEBEE),
        startTrackIndex = 0,
        homeEntryTrackIndex = 50,
        yardGridRow = 0,
        yardGridCol = 0
    ),
    GREEN(
        id = 1,
        displayName = "Green",
        primaryColor = Color(0xFF43A047),
        darkColor = Color(0xFF1B5E20),
        lightColor = Color(0xFFC8E6C9),
        containerColor = Color(0xFFE8F5E9),
        startTrackIndex = 13,
        homeEntryTrackIndex = 11,
        yardGridRow = 0,
        yardGridCol = 9
    ),
    YELLOW(
        id = 2,
        displayName = "Yellow",
        primaryColor = Color(0xFFFDD835),
        darkColor = Color(0xFFF57F17),
        lightColor = Color(0xFFFFF9C4),
        containerColor = Color(0xFFFFFDE7),
        startTrackIndex = 26,
        homeEntryTrackIndex = 24,
        yardGridRow = 9,
        yardGridCol = 9
    ),
    BLUE(
        id = 3,
        displayName = "Blue",
        primaryColor = Color(0xFF1E88E5),
        darkColor = Color(0xFF0D47A1),
        lightColor = Color(0xFFBBDEFB),
        containerColor = Color(0xFFE3F2FD),
        startTrackIndex = 39,
        homeEntryTrackIndex = 37,
        yardGridRow = 9,
        yardGridCol = 0
    )
}

enum class TokenState {
    IN_YARD,
    ON_TRACK,
    IN_HOME_STRETCH,
    FINISHED
}

data class Token(
    val id: Int,                        // 0..3 for each player
    val playerColor: PlayerColor,
    val state: TokenState = TokenState.IN_YARD,
    val trackStep: Int = -1,            // 0..50 steps from player's starting cell
    val homeStep: Int = -1              // 0..4 for home stretch squares, 5 for FINISHED
) {
    val isHome: Boolean get() = state == TokenState.FINISHED
    val isInYard: Boolean get() = state == TokenState.IN_YARD
    val isActive: Boolean get() = state == TokenState.ON_TRACK || state == TokenState.IN_HOME_STRETCH
}

enum class BotDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Normal"),
    HARD("Master")
}

data class Player(
    val color: PlayerColor,
    val name: String,
    val isBot: Boolean = false,
    val botDifficulty: BotDifficulty = BotDifficulty.MEDIUM,
    val tokens: List<Token> = (0..3).map { Token(id = it, playerColor = color) },
    val rank: Int? = null               // 1st, 2nd, 3rd finish place
) {
    val hasWon: Boolean get() = tokens.all { it.state == TokenState.FINISHED }
    val finishedTokensCount: Int get() = tokens.count { it.state == TokenState.FINISHED }
    val activeTokensCount: Int get() = tokens.count { it.isActive }
    val yardTokensCount: Int get() = tokens.count { it.isInYard }
}

enum class GamePhase {
    SETUP,
    WAITING_FOR_ROLL,
    ROLLING_DICE,
    SELECTING_TOKEN,
    ANIMATING_MOVE,
    GAME_OVER
}

data class ValidMove(
    val token: Token,
    val targetState: TokenState,
    val targetTrackStep: Int,
    val targetHomeStep: Int,
    val willCapture: Boolean = false,
    val willFinish: Boolean = false,
    val willEnterSafeZone: Boolean = false
)

data class GameLogEntry(
    val id: Long = System.currentTimeMillis() + (0..1000).random(),
    val message: String,
    val playerColor: PlayerColor,
    val type: LogType = LogType.NORMAL
)

enum class LogType {
    NORMAL,
    ROLL,
    CAPTURE,
    ENTER_HOME,
    WIN
}

data class MatchStatistics(
    val totalRolls: Int = 0,
    val sixesRolled: Int = 0,
    val totalCaptures: Int = 0,
    val totalStepsMoved: Int = 0,
    val turnsCount: Int = 0,
    val gameDurationSeconds: Long = 0
)

data class GameSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val fastAnimation: Boolean = false,
    val threeSixesPenalty: Boolean = true,
    val captureBonusRoll: Boolean = true,
    val homeBonusRoll: Boolean = true
)
