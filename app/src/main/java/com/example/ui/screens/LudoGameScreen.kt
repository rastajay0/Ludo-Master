package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamePhase
import com.example.ui.components.*
import com.example.viewmodel.LudoUiState
import com.example.viewmodel.LudoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoGameScreen(
    viewModel: LudoViewModel,
    uiState: LudoUiState,
    modifier: Modifier = Modifier
) {
    var showRestartDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val currentPlayer = uiState.players.getOrNull(uiState.currentPlayerIndex) ?: return

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_screen"),
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LUDO MASTER",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.testTag("exit_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Fast Speed toggle
                    IconButton(
                        onClick = { viewModel.toggleFastSpeed() },
                        modifier = Modifier.testTag("toggle_speed_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = if (uiState.settings.fastAnimation) Color(0xFFF59E0B) else Color(0xFF94A3B8)
                        )
                    }

                    // Sound Mute/Unmute
                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier.testTag("toggle_sound_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.settings.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound",
                            tint = if (uiState.settings.soundEnabled) Color.White else Color(0xFF94A3B8)
                        )
                    }

                    // Restart Match
                    IconButton(
                        onClick = { showRestartDialog = true },
                        modifier = Modifier.testTag("restart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = Color.White
                        )
                    }

                    // Rules
                    IconButton(
                        onClick = { viewModel.setRulesDialogVisible(true) },
                        modifier = Modifier.testTag("game_rules_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Rules",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Player Status Overview Cards
            PlayerInfoPanel(
                players = uiState.players,
                currentPlayerIndex = uiState.currentPlayerIndex
            )

            // 2. Main Ludo Board Component
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                contentAlignment = Alignment.Center
            ) {
                LudoBoardView(
                    players = uiState.players,
                    validMoves = uiState.validMoves,
                    animatingTokenId = uiState.animatingTokenId,
                    animatingPlayerColor = uiState.animatingPlayerColor,
                    animatingCurrentPos = uiState.animatingCurrentPos,
                    onTokenClick = { token -> viewModel.onTokenClicked(token) }
                )
            }

            // 3. Move/Event Ticker Log
            GameLogSheet(gameLogs = uiState.gameLogs)

            // 4. Dice & Player Turn Controls
            DiceControlView(
                currentPlayer = currentPlayer,
                diceValue = uiState.diceRoll,
                isRolling = uiState.isDiceRolling,
                gamePhase = uiState.gamePhase,
                consecutiveSixes = uiState.consecutiveSixes,
                onRollClick = { viewModel.rollDice() }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Rules Dialog Modal
    if (uiState.showRulesDialog) {
        RulesDialog(onDismiss = { viewModel.setRulesDialogVisible(false) })
    }

    // Victory Celebration Dialog
    if (uiState.celebrationActive && uiState.winner != null) {
        WinnerCelebrationDialog(
            winner = uiState.winner,
            rankedWinners = uiState.rankedWinners,
            allPlayers = uiState.players,
            statistics = uiState.statistics,
            onPlayAgain = { viewModel.restartCurrentGame() },
            onSetupNewGame = { viewModel.returnToSetup() },
            onDismiss = { /* Keep active or modal */ }
        )
    }

    // Restart Confirmation Dialog
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restart Match?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to restart this Ludo game from the beginning?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        viewModel.restartCurrentGame()
                    }
                ) {
                    Text("Restart", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Exit to Menu Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Return to Menu?", fontWeight = FontWeight.Bold) },
            text = { Text("Leave the current match and go back to game setup?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.returnToSetup()
                    }
                ) {
                    Text("Exit to Menu", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
