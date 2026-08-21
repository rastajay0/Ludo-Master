package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.model.PlayerColor

@Composable
fun PlayerInfoPanel(
    players: List<Player>,
    currentPlayerIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        players.forEachIndexed { index, player ->
            val isCurrentTurn = index == currentPlayerIndex
            PlayerMiniCard(
                player = player,
                isCurrentTurn = isCurrentTurn,
                modifier = Modifier
                    .weight(1f)
                    .testTag("player_card_${player.color.displayName.lowercase()}")
            )
        }
    }
}

@Composable
fun PlayerMiniCard(
    player: Player,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isCurrentTurn) player.color.primaryColor else Color.Transparent,
        animationSpec = tween(300),
        label = "border_color"
    )

    Surface(
        modifier = modifier
            .shadow(if (isCurrentTurn) 6.dp else 2.dp, RoundedCornerShape(14.dp))
            .border(
                width = if (isCurrentTurn) 2.5.dp else 1.dp,
                color = if (isCurrentTurn) borderColor else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrentTurn) player.color.containerColor else Color.White
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with color circle & rank
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(player.color.primaryColor)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = player.color.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = player.color.darkColor,
                    maxLines = 1
                )

                if (player.rank != null) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Rank",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "#${player.rank}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Token progress indicators (Home 4 dots)
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val finished = player.finishedTokensCount
                for (i in 0 until 4) {
                    val isHome = i < finished
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isHome) player.color.primaryColor else Color(0xFFCBD5E1)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (player.rank != null) "Finished" else if (player.isBot) "Bot" else "Player",
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
