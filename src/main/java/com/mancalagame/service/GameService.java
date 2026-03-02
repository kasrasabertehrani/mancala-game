package com.mancalagame.service;

import com.mancalagame.model.GameRoom;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private final RoomService roomService;

    public GameService(RoomService roomService) {
        this.roomService = roomService;
    }

    public GameRoom makeMove(String roomId, String playerId, int pitIndex) {
        // 1. Find the room
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        // 2. Tell the Game to execute the move!
        room.getGame().playTurn(playerId, pitIndex);

        // 3. Return the updated room
        return room;
    }
}