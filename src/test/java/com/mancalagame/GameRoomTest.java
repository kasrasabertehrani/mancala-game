package com.mancalagame;

import com.mancalagame.model.Game;
import com.mancalagame.model.GameRoom;
import com.mancalagame.model.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameRoomTest {

    @Test
    void testConstructor_shouldInitializeRoomIdAndGame() {
        // Given
        Player host = new Player("kasra");
        String roomId = "123";

        // When
        GameRoom gameRoom = new GameRoom(roomId, host);

        // Then
        assertEquals(roomId, gameRoom.getRoomId());
        assertNotNull(gameRoom.getGame());
        assertSame(host, gameRoom.getGame().getPlayer1());
        assertEquals(Game.GameStatus.WAITING_FOR_PLAYER_2, gameRoom.getGame().getGameStatus());
    }

    @Test
    void testGetRoomId() {
        // Given
        Player host = new Player("kasra");
        GameRoom gameRoom = new GameRoom("456", host);

        // When
        String roomId = gameRoom.getRoomId();

        // Then
        assertEquals("456", roomId);
    }

    @Test
    void testSetRoomId() {
        // Given
        Player host = new Player("kasra");
        GameRoom gameRoom = new GameRoom("123", host);

        // When
        gameRoom.setRoomId("newRoomId");

        // Then
        assertEquals("newRoomId", gameRoom.getRoomId());
    }

    @Test
    void testGetGame() {
        // Given
        Player host = new Player("kasra");
        GameRoom gameRoom = new GameRoom("123", host);

        // When
        Game game = gameRoom.getGame();

        // Then
        assertNotNull(game);
        assertSame(host, game.getPlayer1());
    }

    @Test
    void testSetGame() {
        // Given
        Player host1 = new Player("kasra");
        Player host2 = new Player("ali");
        GameRoom gameRoom = new GameRoom("123", host1);
        Game newGame = new Game(host2);

        // When
        gameRoom.setGame(newGame);

        // Then
        assertSame(newGame, gameRoom.getGame());
        assertSame(host2, gameRoom.getGame().getPlayer1());
    }

    @Test
    void testGameInRoom_canAddPlayer2AndStartGame() {
        // Given
        Player host = new Player("kasra");
        Player guest = new Player("ali");
        GameRoom gameRoom = new GameRoom("123", host);

        // When
        gameRoom.getGame().setPlayer2(guest);

        // Then
        assertSame(guest, gameRoom.getGame().getPlayer2());
        assertEquals(Game.GameStatus.PLAYER_1_TURN, gameRoom.getGame().getGameStatus());
    }

    @Test
    void testGameInRoom_canPlayTurn() {
        // Given
        Player host = new Player("kasra");
        Player guest = new Player("ali");
        GameRoom gameRoom = new GameRoom("123", host);
        gameRoom.getGame().setPlayer2(guest);

        // When
        gameRoom.getGame().playTurn(host.getId(), 0);

        // Then
        assertEquals(Game.GameStatus.PLAYER_2_TURN, gameRoom.getGame().getGameStatus());
    }
}