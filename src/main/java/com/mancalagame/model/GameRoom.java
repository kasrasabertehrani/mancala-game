package com.mancalagame.model;

public class GameRoom {
    private String roomId;
    private Game game;

    public GameRoom(String roomId, Player host) {
        this.roomId = roomId;
        // We pass the host (Player 1) directly into the new Game!
        this.game = new Game(host);
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }
}