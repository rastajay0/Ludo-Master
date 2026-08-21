package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BotDifficulty
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.ui.components.RulesDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoSetupScreen(
    onStartGame: (List<Player>) -> Unit,
    onOpenRules: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playerCount by remember { mutableStateOf(4) }
    var gameMode by remember { mutableStateOf(0) } // 0 = Pass & Play, 1 = Vs Computer
    var botDifficulty by remember { mutableStateOf(BotDifficulty.MEDIUM) }

    // Colors active based on player count
    val activeColors = when (playerCount) {
        2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
        3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
        else -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
    }

    // Player configurations
    var playerConfigs by remember(playerCount, gameMode, botDifficulty) {
        mutableStateOf(
            activeColors.mapIndexed { idx, color ->
                val isBot = if (gameMode == 1) idx > 0 else false
                val defaultName = if (isBot) "${color.displayName} AI" else "Player ${idx + 1}"
                PlayerSetupItem(
                    color = color,
                    name = defaultName,
                    isBot = isBot,
                    difficulty = botDifficulty
                )
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("setup_screen"),
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LUDO MASTER",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenRules,
                        modifier = Modifier.testTag("setup_rules_button")
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
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Hero Game Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1E293B)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 4 Colored token dots preview
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            PlayerColor.RED,
                            PlayerColor.GREEN,
                            PlayerColor.YELLOW,
                            PlayerColor.BLUE
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(color.primaryColor)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Classic Board Game",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Roll, capture & race home!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Number of Players
            SectionCard(title = "NUMBER OF PLAYERS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(2, 3, 4).forEach { count ->
                        val selected = playerCount == count
                        FilterChip(
                            selected = selected,
                            onClick = { playerCount = count },
                            label = {
                                Text(
                                    text = "$count Players",
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("select_${count}_players"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = if (selected) Color(0xFF3B82F6) else Color(0xFF334155)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Game Mode
            SectionCard(title = "GAME MODE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val modes = listOf("Pass & Play", "Vs Computer (AI)")
                    modes.forEachIndexed { index, modeTitle ->
                        val selected = gameMode == index
                        FilterChip(
                            selected = selected,
                            onClick = { gameMode = index },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.Groups else Icons.Default.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = modeTitle,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("select_mode_$index"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF059669),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1),
                                iconColor = Color(0xFF94A3B8)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = if (selected) Color(0xFF10B981) else Color(0xFF334155)
                            )
                        )
                    }
                }

                // AI Difficulty selector when VS AI is selected
                AnimatedVisibility(visible = gameMode == 1) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "AI Difficulty:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BotDifficulty.entries.forEach { diff ->
                                val selected = botDifficulty == diff
                                FilterChip(
                                    selected = selected,
                                    onClick = { botDifficulty = diff },
                                    label = { Text(diff.label, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF7C3AED),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color(0xFFCBD5E1)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = if (selected) Color(0xFF8B5CF6) else Color(0xFF334155)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Players List Customization
            SectionCard(title = "PARTICIPANTS") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    playerConfigs.forEachIndexed { idx, config ->
                        PlayerConfigRow(
                            item = config,
                            onNameChange = { newName ->
                                playerConfigs = playerConfigs.toMutableList().apply {
                                    this[idx] = this[idx].copy(name = newName)
                                }
                            },
                            onToggleBot = {
                                playerConfigs = playerConfigs.toMutableList().apply {
                                    val current = this[idx]
                                    val newBot = !current.isBot
                                    val newName = if (newBot) "${current.color.displayName} AI" else "Player ${idx + 1}"
                                    this[idx] = current.copy(isBot = newBot, name = newName)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start Game CTA Button
            Button(
                onClick = {
                    val finalPlayers = playerConfigs.map { config ->
                        Player(
                            color = config.color,
                            name = config.name.ifBlank { config.color.displayName },
                            isBot = config.isBot,
                            botDifficulty = config.difficulty
                        )
                    }
                    onStartGame(finalPlayers)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .testTag("start_game_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START MATCH",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E293B)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PlayerConfigRow(
    item: PlayerSetupItem,
    onNameChange: (String) -> Unit,
    onToggleBot: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(item.color.primaryColor)
                    .border(2.dp, Color.White, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Player Name text field
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = item.color.primaryColor,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Human / Bot toggle button
            IconButton(
                onClick = onToggleBot,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item.isBot) Color(0xFF334155) else Color(0xFF2563EB))
            ) {
                Icon(
                    imageVector = if (item.isBot) Icons.Default.SmartToy else Icons.Default.Person,
                    contentDescription = if (item.isBot) "Bot" else "Human",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class PlayerSetupItem(
    val color: PlayerColor,
    val name: String,
    val isBot: Boolean,
    val difficulty: BotDifficulty
)
