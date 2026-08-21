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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamePhase
import com.example.model.Player
import com.example.model.PlayerColor

@Composable
fun DiceControlView(
    currentPlayer: Player,
    diceValue: Int,
    isRolling: Boolean,
    gamePhase: GamePhase,
    consecutiveSixes: Int,
    onRollClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHumanTurn = !currentPlayer.isBot
    val canRoll = isHumanTurn && gamePhase == GamePhase.WAITING_FOR_ROLL && !isRolling

    // Rolling animation rotation and scale
    val infiniteTransition = rememberInfiniteTransition(label = "dice_roll_anim")
    val rollAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "roll_angle"
    )
    val rollScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(140, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "roll_scale"
    )

    // Pulse animation for waiting roll button
    val pulseButtonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btn_pulse"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        currentPlayer.color.containerColor,
                        Color.White,
                        currentPlayer.color.containerColor
                    )
                )
            )
            .border(2.5.dp, currentPlayer.color.primaryColor, RoundedCornerShape(24.dp))
            .testTag("dice_control_panel"),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player info and status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Color Avatar Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(currentPlayer.color.primaryColor)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPlayer.isBot) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Bot",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = currentPlayer.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentPlayer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (currentPlayer.isBot) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE2E8F0)
                            ) {
                                Text(
                                    text = "AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    val statusText = when {
                        isRolling -> "Rolling dice..."
                        gamePhase == GamePhase.SELECTING_TOKEN -> if (isHumanTurn) "Tap a glowing token to move" else "AI selecting move..."
                        gamePhase == GamePhase.ANIMATING_MOVE -> "Moving token..."
                        gamePhase == GamePhase.GAME_OVER -> "Game Completed!"
                        !isHumanTurn -> "AI turn in progress..."
                        else -> "Tap the dice or button to roll!"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (gamePhase == GamePhase.SELECTING_TOKEN) currentPlayer.color.darkColor else Color(0xFF64748B),
                        fontWeight = if (gamePhase == GamePhase.SELECTING_TOKEN) FontWeight.Bold else FontWeight.Medium
                    )

                    if (consecutiveSixes > 0) {
                        Text(
                            text = "Sixes: $consecutiveSixes/3 (Extra turn!)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Interactive 3D Dice Face
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(if (isRolling) rollScale else if (canRoll) pulseButtonScale else 1f)
                    .rotate(if (isRolling) rollAngle else 0f)
                    .shadow(10.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(
                        width = if (canRoll) 3.dp else 1.5.dp,
                        color = if (canRoll) currentPlayer.color.primaryColor else Color(0xFFCBD5E1),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        enabled = canRoll,
                        onClick = onRollClick
                    )
                    .testTag("interactive_dice"),
                contentAlignment = Alignment.Center
            ) {
                DiceFace(
                    value = diceValue,
                    pipColor = currentPlayer.color.primaryColor,
                    isRolling = isRolling
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Roll Button
            Button(
                onClick = onRollClick,
                enabled = canRoll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentPlayer.color.primaryColor,
                    disabledContainerColor = if (isRolling || gamePhase == GamePhase.SELECTING_TOKEN)
                        currentPlayer.color.primaryColor.copy(alpha = 0.6f)
                    else Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .scale(if (canRoll) pulseButtonScale else 1f)
                    .testTag("roll_dice_button"),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Roll",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when {
                        isRolling -> "..."
                        gamePhase == GamePhase.SELECTING_TOKEN -> "MOVE"
                        !isHumanTurn -> "AI"
                        else -> "ROLL"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun DiceFace(
    value: Int,
    pipColor: Color,
    isRolling: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize().padding(8.dp)) {
        val w = size.width
        val h = size.height
        val pipRadius = w * 0.11f

        val left = w * 0.22f
        val midX = w * 0.5f
        val right = w * 0.78f

        val top = h * 0.22f
        val midY = h * 0.5f
        val bottom = h * 0.78f

        val pipPositions = when (value.coerceIn(1, 6)) {
            1 -> listOf(Offset(midX, midY))
            2 -> listOf(Offset(left, top), Offset(right, bottom))
            3 -> listOf(Offset(left, top), Offset(midX, midY), Offset(right, bottom))
            4 -> listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom))
            5 -> listOf(Offset(left, top), Offset(right, top), Offset(midX, midY), Offset(left, bottom), Offset(right, bottom))
            6 -> listOf(Offset(left, top), Offset(right, top), Offset(left, midY), Offset(right, midY), Offset(left, bottom), Offset(right, bottom))
            else -> listOf(Offset(midX, midY))
        }

        pipPositions.forEach { center ->
            // Subtle 3D pip shadow
            drawCircle(
                color = Color(0x33000000),
                radius = pipRadius,
                center = center + Offset(0f, 1.5.dp.toPx())
            )
            // Pip body
            drawCircle(
                color = if (isRolling) Color(0xFF475569) else pipColor,
                radius = pipRadius,
                center = center
            )
        }
    }
}
