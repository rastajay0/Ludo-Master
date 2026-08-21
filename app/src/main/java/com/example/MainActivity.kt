package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.components.RulesDialog
import com.example.ui.screens.LudoGameScreen
import com.example.ui.screens.LudoSetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameScreen
import com.example.viewmodel.LudoViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LudoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    LudoApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun LudoApp(viewModel: LudoViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showSetupRules by remember { mutableStateOf(false) }

    Crossfade(targetState = uiState.screen, label = "screen_transition") { screen ->
        when (screen) {
            GameScreen.SETUP -> {
                LudoSetupScreen(
                    onStartGame = { players -> viewModel.startGame(players) },
                    onOpenRules = { showSetupRules = true }
                )
            }
            GameScreen.GAME -> {
                LudoGameScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }

    if (showSetupRules) {
        RulesDialog(onDismiss = { showSetupRules = false })
    }
}

