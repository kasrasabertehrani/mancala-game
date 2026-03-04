package com.mancalagame.payload;

public class ReconnectRequest {
    private String roomId;
    private String playerId;

    public ReconnectRequest() {}

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
}