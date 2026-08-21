package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.LudoBoardCoordinates
import com.example.engine.LudoGameEngine
import com.example.model.BotDifficulty
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.Token
import com.example.model.TokenState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ludo Master", appName)
    }

    @Test
    fun `test token exiting yard requires 6`() {
        val redPlayer = Player(
            color = PlayerColor.RED,
            name = "Player 1",
            tokens = listOf(Token(id = 0, playerColor = PlayerColor.RED, state = TokenState.IN_YARD))
        )

        val movesRoll5 = LudoGameEngine.getValidMoves(redPlayer, 5, listOf(redPlayer))
        assertTrue(movesRoll5.isEmpty())

        val movesRoll6 = LudoGameEngine.getValidMoves(redPlayer, 6, listOf(redPlayer))
        assertEquals(1, movesRoll6.size)
        assertEquals(TokenState.ON_TRACK, movesRoll6[0].targetState)
        assertEquals(0, movesRoll6[0].targetTrackStep)
    }

    @Test
    fun `test token capture outside safe zone`() {
        val redToken = Token(id = 0, playerColor = PlayerColor.RED, state = TokenState.ON_TRACK, trackStep = 1)
        val redPlayer = Player(color = PlayerColor.RED, name = "Red", tokens = listOf(redToken))

        // Green token on track at step 0 (which is global index 13)
        // Red token at step 13 is global index (0 + 13) = 13
        val greenToken = Token(id = 0, playerColor = PlayerColor.GREEN, state = TokenState.ON_TRACK, trackStep = 0)
        val greenPlayer = Player(color = PlayerColor.GREEN, name = "Green", tokens = listOf(greenToken))

        val allPlayers = listOf(redPlayer, greenPlayer)

        // Red at step 1 rolls 12 -> reaches step 13 (global index 13, Green's start square)
        // Green's start square is a safe square, so no capture
        val capturedOnSafe = LudoGameEngine.findCapturedTokens(PlayerColor.RED, 13, allPlayers)
        assertTrue(capturedOnSafe.isEmpty())

        // Non safe square: Red at step 1 rolls 4 -> reaches step 5 (global index 5, not safe)
        // Place Green at global index 5 (which is Green's track step (5 - 13 + 52) % 52 = 44)
        val greenTokenOn5 = Token(id = 1, playerColor = PlayerColor.GREEN, state = TokenState.ON_TRACK, trackStep = 44)
        val greenPlayerWithToken5 = Player(color = PlayerColor.GREEN, name = "Green", tokens = listOf(greenTokenOn5))

        val captured = LudoGameEngine.findCapturedTokens(PlayerColor.RED, 5, listOf(redPlayer, greenPlayerWithToken5))
        assertEquals(1, captured.size)
        assertEquals(PlayerColor.GREEN, captured[0].playerColor)
    }
}
