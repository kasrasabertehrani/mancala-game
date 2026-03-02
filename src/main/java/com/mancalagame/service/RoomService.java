package com.mancalagame.service;

import com.mancalagame.model.GameRoom;
import com.mancalagame.model.Player;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RoomService {

    // Our in-memory "database" mapping Room IDs to GameRooms
    private final ConcurrentHashMap<String, GameRoom> activeRooms = new ConcurrentHashMap<>();
    private final AtomicInteger roomCounter = new AtomicInteger(1);

    // 1. Create a new room
    public GameRoom createRoom(Player host) {

        // Grab the current number and increment it for the next person
        String simpleRoomId = String.valueOf(roomCounter.getAndIncrement());

        GameRoom newRoom = new GameRoom(simpleRoomId, host);
        activeRooms.put(simpleRoomId, newRoom);

        return newRoom;
    }

    // 2. Let a second player join
    public GameRoom joinRoom(String roomId, Player player2) {
        GameRoom room = activeRooms.get(roomId);

        // FIX 1: We must fetch the game from the room to check Player 2
        if (room != null && room.getGame().getPlayer2() == null) {

            // FIX 2: This custom setter automatically changes the GameStatus to PLAYER_1_TURN!
            room.getGame().setPlayer2(player2);
        }
        return room;
    }

    // 3. Retrieve a room (used when a player makes a move)
    public GameRoom getRoom(String roomId) {
        return activeRooms.get(roomId);
    }
}