package com.mancalagame.service;

import com.mancalagame.model.Game;
import com.mancalagame.model.GameRoom;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final RoomService roomService;

    // gameId -> Instant when a player disconnected
    private final Map<String, Instant> disconnectTimestamps = new ConcurrentHashMap<>();

    // gameId -> Instant of last move
    private final Map<String, Instant> lastActivityTimestamps = new ConcurrentHashMap<>();

    private static final Duration RECONNECT_GRACE = Duration.ofSeconds(30);
    private static final Duration INACTIVITY_TIMEOUT = Duration.ofMinutes(5);

    public GameService(RoomService roomService) {
        this.roomService = roomService;
    }

    public GameRoom makeMove(String roomId, String playerId, int pitIndex) {
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        synchronized (room) {
            room.getGame().playTurn(playerId, pitIndex);
            lastActivityTimestamps.put(roomId, Instant.now());

            if (room.getGame().getGameStatus() == Game.GameStatus.GAME_OVER) {
                cleanupTimestamps(roomId);
            }

            return room;
        }
    }

    public GameRoom handlePlayerDisconnect(String roomId, String playerId) {
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) return null;

        synchronized (room) {
            Game game = room.getGame();
            if (game.getGameStatus() != Game.GameStatus.GAME_OVER
                    && game.getGameStatus() != Game.GameStatus.PLAYER_DISCONNECTED) {
                game.handleDisconnect(playerId);
                disconnectTimestamps.put(roomId, Instant.now());
            }
            return room;
        }
    }

    public GameRoom handlePlayerReconnect(String roomId, String playerId) {
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) return null;

        synchronized (room) {
            Game game = room.getGame();
            game.handleReconnect(playerId);
            disconnectTimestamps.remove(roomId);
            lastActivityTimestamps.put(roomId, Instant.now());
            return room;
        }
    }

    /**
     * Returns list of rooms that timed out (reconnect or inactivity).
     * The caller is responsible for broadcasting.
     */
    public List<GameRoom> processTimeouts() {
        Instant now = Instant.now();
        List<GameRoom> timedOutRooms = new ArrayList<>();

        // 1. Reconnect grace period expired
        new ArrayList<>(disconnectTimestamps.keySet()).forEach(roomId -> {
            Instant disconnectedAt = disconnectTimestamps.get(roomId);
            if (disconnectedAt != null && Duration.between(disconnectedAt, now).compareTo(RECONNECT_GRACE) > 0) {
                GameRoom room = roomService.getRoom(roomId);
                if (room != null && room.getGame().getGameStatus() == Game.GameStatus.PAUSED_FOR_RECONNECT) {
                    synchronized (room) {
                        room.getGame().forfeit(room.getGame().getDisconnectedPlayerId());
                        timedOutRooms.add(room);
                    }
                    cleanupTimestamps(roomId);
                    roomService.removeRoom(roomId);
                }
            }
        });

        // 2. Inactivity timeout
        new ArrayList<>(lastActivityTimestamps.keySet()).forEach(roomId -> {
            Instant lastMove = lastActivityTimestamps.get(roomId);
            if (lastMove != null && Duration.between(lastMove, now).compareTo(INACTIVITY_TIMEOUT) > 0) {
                GameRoom room = roomService.getRoom(roomId);
                if (room != null
                        && room.getGame().getGameStatus() != Game.GameStatus.GAME_OVER
                        && room.getGame().getGameStatus() != Game.GameStatus.PLAYER_DISCONNECTED
                        && room.getGame().getGameStatus() != Game.GameStatus.PAUSED_FOR_RECONNECT) {
                    synchronized (room) {
                        String idlePlayer = (room.getGame().getGameStatus() == Game.GameStatus.PLAYER_1_TURN)
                                ? room.getGame().getPlayer1().getId()
                                : room.getGame().getPlayer2().getId();
                        room.getGame().forfeit(idlePlayer);
                        timedOutRooms.add(room);
                    }
                    cleanupTimestamps(roomId);
                    roomService.removeRoom(roomId);
                }
            }
        });

        return timedOutRooms;
    }

    private void cleanupTimestamps(String roomId) {
        disconnectTimestamps.remove(roomId);
        lastActivityTimestamps.remove(roomId);
    }
}
