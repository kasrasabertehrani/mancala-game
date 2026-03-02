package com.mancalagame.model;

public class GameRoom {
    private String roomId;
    private Player player1;
    private Player player2;
    private Game game; // The room now cleanly owns the game

    public GameRoom(String roomId, Player player1) {
        this.roomId = roomId;
        this.player1 = player1;
        this.game = new Game(); // This now automatically sets up the pits!
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }
}