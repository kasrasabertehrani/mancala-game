package com.mancalagame;

import com.mancalagame.model.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {
    @Test
    void testPlayer() {
        Player player = new Player("TestPlayer");
        assertEquals("TestPlayer", player.getName());
    }
    @Test
    void testPlayerId() {
        Player player = new Player("TestPlayer");
        assertNotNull(player.getId());
    }
}
