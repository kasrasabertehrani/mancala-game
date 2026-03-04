package com.mancalagame.controller;

import com.mancalagame.model.GameRoom;
import com.mancalagame.payload.PlayPitCommand;
import com.mancalagame.payload.ReconnectRequest;
import com.mancalagame.service.GameService;
import com.mancalagame.service.SessionTracker;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionTracker sessionTracker;

    public GameController(GameService gameService, SimpMessagingTemplate messagingTemplate, SessionTracker sessionTracker) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.sessionTracker = sessionTracker;
    }

    @MessageMapping("/game.join")
    public void joinGame(@Payload ReconnectRequest request, SimpMessageHeaderAccessor headerAccessor) {
        // Track this player's session so we can detect disconnects
        sessionTracker.trackSession(headerAccessor.getSessionId(), request.getPlayerId(), request.getRoomId());

        // Start activity tracking for this room
        gameService.startActivityTracking(request.getRoomId());
    }

    @MessageMapping("/game.move")
    public void makeMove(@Payload PlayPitCommand command, SimpMessageHeaderAccessor headerAccessor) {

        if (command.getRoomId() == null || command.getPlayerId() == null) {
            throw new IllegalArgumentException("Room ID and Player ID are required.");
        }

        // 1. Memorize this player's session ID in case they disconnect later
        String sessionId = headerAccessor.getSessionId();
        sessionTracker.trackSession(sessionId, command.getPlayerId(), command.getRoomId());

        // 2. Make the move normally
        GameRoom updatedRoom = gameService.makeMove(
                command.getRoomId(),
                command.getPlayerId(),
                command.getPitIndex()
        );

        // 3. Broadcast the update
        messagingTemplate.convertAndSend("/topic/room/" + updatedRoom.getRoomId(), updatedRoom.getGame());
    }

    @MessageMapping("/game.reconnect")
    public void reconnect(@Payload ReconnectRequest request, SimpMessageHeaderAccessor headerAccessor) {
        // 1. Re-register their brand new WebSocket session in our memory using their secret ID
        sessionTracker.trackSession(headerAccessor.getSessionId(), request.getPlayerId(), request.getRoomId());

        // 2. Tell the Service to stop the forfeit timer and resume the game!
        GameRoom updatedRoom = gameService.handlePlayerReconnect(request.getRoomId(), request.getPlayerId());

        // 3. Broadcast the resumed game state to both players
        if (updatedRoom != null) {
            messagingTemplate.convertAndSend("/topic/room/" + updatedRoom.getRoomId(), updatedRoom.getGame());
        }
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Exception ex) {
        return ex.getMessage();
    }
}