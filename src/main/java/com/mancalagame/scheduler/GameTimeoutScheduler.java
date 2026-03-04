package com.mancalagame.scheduler;

import com.mancalagame.model.GameRoom;
import com.mancalagame.service.GameService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameTimeoutScheduler {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameTimeoutScheduler(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 5000)
    public void checkTimeouts() {
        List<GameRoom> timedOutRooms = gameService.processTimeouts();

        for (GameRoom room : timedOutRooms) {
            messagingTemplate.convertAndSend("/topic/room/" + room.getRoomId(), room.getGame());
        }
    }
}
