package com.mancalagame;

import com.mancalagame.model.Game;
import com.mancalagame.model.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GameTest {

    @Test
    void testGameConstructor() {
        Player player1 = new Player("kasra");
        Game game = new Game(player1);
        assertSame(player1, game.getPlayer1());
        assertNull(game.getPlayer2());
        assertNotNull(game.getBoard());
        assertEquals(Game.GameStatus.WAITING_FOR_PLAYER_2, game.getGameStatus());
        assertNull(game.getDisconnectedPlayerId());
    }

    @Test
    void testSetPlayer2() {
        Player player1 = new Player("kasra");
        Player player2 = new Player("ali");
        Game game = new Game(player1);
        game.setPlayer2(player2);
        assertSame(player2, game.getPlayer2());
        assertEquals(Game.GameStatus.PLAYER_1_TURN, game.getGameStatus());
    }



    @Test
    void playTurn_shouldSwitchFromPlayer1ToPlayer2_whenLastStoneNotInStore() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // PLAYER_1_TURN

        // When (pit 0 has 4 stones -> lands at pit 4, not store)
        game.playTurn(p1.getId(), 0);

        // Then
        assertEquals(Game.GameStatus.PLAYER_2_TURN, game.getGameStatus());
    }

    @Test
    void playTurn_shouldSwitchFromPlayer2ToPlayer1_whenLastStoneNotInStore() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);

        // Move once to reach PLAYER_2_TURN
        game.playTurn(p1.getId(), 0);

        // When (pit 7 has 4 stones -> lands at pit 11, not store)
        game.playTurn(p2.getId(), 7);

        // Then
        assertEquals(Game.GameStatus.PLAYER_1_TURN, game.getGameStatus());
    }

    @Test
    void playTurn_shouldKeepTurn_whenLastStoneEndsInOwnStore() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // PLAYER_1_TURN

        // When (pit 2 has 4 stones -> lands at store index 6)
        game.playTurn(p1.getId(), 2);

        // Then: switchTurn() is skipped
        assertEquals(Game.GameStatus.PLAYER_1_TURN, game.getGameStatus());
    }

    @Test
    void playTurn_shouldTriggerEndGame_whenOneSideIsEmpty_andSweepRemainingStones() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // PLAYER_1_TURN

        // Force player 2 side to be empty (pits 7..12)
        for (int i = 7; i <= 12; i++) {
            game.getBoard().getPits()[i].clear();
        }

        // When: player 1 makes any valid move
        game.playTurn(p1.getId(), 0);

        // Then: endGame() has been executed
        assertEquals(Game.GameStatus.GAME_OVER, game.getGameStatus());

        // Remaining stones on both sides are swept to stores
        for (int i = 0; i <= 5; i++) {
            assertEquals(0, game.getBoard().getPits()[i].getStones());
        }
        for (int i = 7; i <= 12; i++) {
            assertEquals(0, game.getBoard().getPits()[i].getStones());
        }

        // With this setup, all player-1 side stones end up in player-1 store
        assertEquals(24, game.getPlayer1Score());
        assertEquals(0, game.getPlayer2Score());
    }

    // --- HANDLE DISCONNECT TESTS ---

    @Test
    void handleDisconnect_shouldSetGameOver_whenPlayer2IsNull() {
        // Given
        Player p1 = new Player("kasra");
        Game game = new Game(p1);

        // When: player 1 disconnects before player 2 joins
        game.handleDisconnect(p1.getId());

        // Then: game ends immediately
        assertEquals(Game.GameStatus.GAME_OVER, game.getGameStatus());
    }

    @Test
    void handleDisconnect_shouldPauseGame_whenBothPlayersActive() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // PLAYER_1_TURN

        // When: player 1 disconnects mid-game
        game.handleDisconnect(p1.getId());

        // Then
        assertEquals(Game.GameStatus.PAUSED_FOR_RECONNECT, game.getGameStatus());
        assertEquals(p1.getId(), game.getDisconnectedPlayerId());
    }

    @Test
    void handleDisconnect_shouldPreservePreviousStatus() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // PLAYER_1_TURN
        game.playTurn(p1.getId(), 0); // now PLAYER_2_TURN

        // When: player 2 disconnects
        game.handleDisconnect(p2.getId());

        // Then: previous status is saved
        assertEquals(Game.GameStatus.PAUSED_FOR_RECONNECT, game.getGameStatus());
        assertEquals(p2.getId(), game.getDisconnectedPlayerId());
    }

    // --- HANDLE RECONNECT TESTS ---

    @Test
    void handleReconnect_shouldRestorePreviousStatus_whenCorrectPlayer() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);
        game.playTurn(p1.getId(), 0); // PLAYER_2_TURN
        game.handleDisconnect(p2.getId()); // PAUSED_FOR_RECONNECT, previousStatus = PLAYER_2_TURN

        // When: player 2 reconnects
        game.handleReconnect(p2.getId());

        // Then
        assertEquals(Game.GameStatus.PLAYER_2_TURN, game.getGameStatus());
        assertNull(game.getDisconnectedPlayerId());
    }

    @Test
    void handleReconnect_shouldDoNothing_whenGameNotPaused() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // PLAYER_1_TURN

        // When: someone tries to reconnect while game is active
        game.handleReconnect(p1.getId());

        // Then: no change
        assertEquals(Game.GameStatus.PLAYER_1_TURN, game.getGameStatus());
        assertNull(game.getDisconnectedPlayerId());
    }

    @Test
    void handleReconnect_shouldDoNothing_whenWrongPlayerReconnects() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Player p3 = new Player("unknown");
        Game game = new Game(p1);
        game.setPlayer2(p2);
        game.handleDisconnect(p1.getId()); // PAUSED_FOR_RECONNECT, disconnectedPlayerId = p1

        // When: wrong player tries to reconnect
        game.handleReconnect(p3.getId());

        // Then: no change
        assertEquals(Game.GameStatus.PAUSED_FOR_RECONNECT, game.getGameStatus());
        assertEquals(p1.getId(), game.getDisconnectedPlayerId());
    }

    // --- FORFEIT TESTS ---

    @Test
    void forfeit_shouldSetGameStatusAndDisconnectedId() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);

        // When: player 1 forfeits
        game.forfeit(p1.getId());

        // Then
        assertEquals(Game.GameStatus.PLAYER_DISCONNECTED, game.getGameStatus());
        assertEquals(p1.getId(), game.getDisconnectedPlayerId());
    }

    @Test
    void forfeit_player2() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);

        // When: player 2 forfeits
        game.forfeit(p2.getId());

        // Then
        assertEquals(Game.GameStatus.PLAYER_DISCONNECTED, game.getGameStatus());
        assertEquals(p2.getId(), game.getDisconnectedPlayerId());
    }

    // --- GET WINNER TESTS ---

    @Test
    void getWinner_shouldReturnPlayer2_whenPlayer1Forfeits() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);
        game.forfeit(p1.getId()); // PLAYER_DISCONNECTED

        // When
        String winner = game.getWinner();

        // Then
        assertEquals(p2.getId(), winner);
    }

    @Test
    void getWinner_shouldReturnPlayer1_whenPlayer2Forfeits() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);
        game.forfeit(p2.getId()); // PLAYER_DISCONNECTED

        // When
        String winner = game.getWinner();

        // Then
        assertEquals(p1.getId(), winner);
    }

    @Test
    void getWinner_shouldReturnNull_whenGameNotOver() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // PLAYER_1_TURN (game is ongoing)

        // When
        String winner = game.getWinner();

        // Then
        assertNull(winner);
    }


    // ... existing code ...
    @Test
    void getWinner_shouldReturnPlayer1_whenPlayer1HasMoreStones() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);

        // Force game over with p1 winning
        for (int i = 7; i <= 12; i++) {
            game.getBoard().getPits()[i].clear();
        }
        game.playTurn(p1.getId(), 0); // triggers endGame

        // When
        String winner = game.getWinner();

        // Then
        assertEquals(p1.getId(), winner);
    }

    @Test
    void getWinner_shouldReturnPlayer2_whenPlayer2HasMoreStones() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);

        // Force game over with p2 winning by emptying p1's pits
        // and giving p2 more stones in their store
        for (int i = 0; i <= 5; i++) {
            game.getBoard().getPits()[i].clear();
        }
        // Add stones to player 2's store to ensure p2 wins
        game.getBoard().getPits()[13].addStones(10);

        // Make a valid move from p2's side to trigger game over check
        // First we need to switch turn to p2
        game.getBoard().getPits()[0].addStones(4); // restore pit 0
        game.playTurn(p1.getId(), 0); // p1 plays, now p2's turn

        // Empty p1's pits again for game over
        for (int i = 0; i <= 5; i++) {
            game.getBoard().getPits()[i].clear();
        }
        game.playTurn(p2.getId(), 7); // triggers endGame

        // When
        String winner = game.getWinner();

        // Then
        assertEquals(p2.getId(), winner);
    }

    // ... existing code ...
    @Test
    void getWinner_shouldReturnDraw_whenScoresAreEqual() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);

        // Clear all pits to force game over
        for (int i = 0; i <= 5; i++) {
            game.getBoard().getPits()[i].clear();
        }
        for (int i = 7; i <= 12; i++) {
            game.getBoard().getPits()[i].clear();
        }

        // Set scores in stores - player 2 needs 1 more because the 1 stone
        // we add to pit 0 will be swept to player 1's store during endGame
        game.getBoard().getPits()[6].addStones(24);  // Player 1 store
        game.getBoard().getPits()[13].addStones(25); // Player 2 store (1 extra to compensate)

        // Add one stone to make a valid move and trigger game over
        // This stone lands in pit 1, then gets swept to player 1's store (total: 25)
        game.getBoard().getPits()[0].addStones(1);
        game.playTurn(p1.getId(), 0); // triggers endGame, sweeps pit 1 (1 stone) to p1 store

        // When
        String winner = game.getWinner();

        // Then
        assertEquals("DRAW", winner);
    }

    // --- VALIDATE MOVE TESTS ---
// ... existing code ...

    // --- VALIDATE MOVE TESTS ---
// ... existing code ...

    // --- VALIDATE MOVE TESTS ---

    @Test
    void validateMove_shouldThrow_whenPlayerIdIsNull() {
        // Given
        Player p1 = new Player("kasra");
        Game game = new Game(p1);
        game.setPlayer2(new Player("ali"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            game.playTurn(null, 0);
        });
    }

    @Test
    void validateMove_shouldThrow_whenUnknownPlayer() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Player p3 = new Player("unknown");
        Game game = new Game(p1);
        game.setPlayer2(p2);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            game.playTurn(p3.getId(), 0);
        });
    }

    @Test
    void validateMove_shouldThrow_whenGameNotPlayable_WAITING_FOR_PLAYER_2() {
        // Given
        Player p1 = new Player("kasra");
        Game game = new Game(p1); // Status: WAITING_FOR_PLAYER_2

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            game.playTurn(p1.getId(), 0);
        });
    }

    @Test
    void validateMove_shouldThrow_whenGameNotPlayable_GAME_OVER() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);

        // Force GAME_OVER
        for (int i = 7; i <= 12; i++) {
            game.getBoard().getPits()[i].clear();
        }
        game.playTurn(p1.getId(), 0); // GAME_OVER

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            game.playTurn(p1.getId(), 0);
        });
    }

    @Test
    void validateMove_shouldThrow_whenNotPlayersTurn_P1() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // P1_TURN
        game.playTurn(p1.getId(), 0); // Switch to P2_TURN

        // When & Then (p1 tries to play when it's p2's turn)
        assertThrows(IllegalStateException.class, () -> {
            game.playTurn(p1.getId(), 0);
        });
    }

    @Test
    void validateMove_shouldThrow_whenNotPlayersTurn_P2() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // P1_TURN

        // When & Then (p2 tries to play when it's p1's turn)
        assertThrows(IllegalStateException.class, () -> {
            game.playTurn(p2.getId(), 7);
        });
    }

    @Test
    void validateMove_shouldThrow_whenPitNotOnCorrectSide_P1() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // P1_TURN

        // When & Then (p1 tries to pick pit 7, which is p2's side)
        assertThrows(IllegalArgumentException.class, () -> {
            game.playTurn(p1.getId(), 7);
        });
    }

    @Test
    void validateMove_shouldThrow_whenPitNotOnCorrectSide_P2() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2);
        game.playTurn(p1.getId(), 0); // P2_TURN

        // When & Then (p2 tries to pick pit 0, which is p1's side)
        assertThrows(IllegalArgumentException.class, () -> {
            game.playTurn(p2.getId(), 0);
        });
    }

    @Test
    void validateMove_shouldThrow_whenPickingEmptyPit() {
        // Given
        Player p1 = new Player("kasra");
        Player p2 = new Player("ali");
        Game game = new Game(p1);
        game.setPlayer2(p2); // P1_TURN

        // Empty pit 0
        game.getBoard().getPits()[0].clear();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            game.playTurn(p1.getId(), 0);
        });
    }



}
