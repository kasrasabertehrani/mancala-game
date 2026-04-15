package com.mancalagame;

import com.mancalagame.model.Pit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PitTest {
    @Test
    void testPit() {
        Pit pit = new Pit(10);
        assertEquals(10, pit.getStones());
    }
    @Test
    void testIncrement() {
        Pit pit = new Pit(10);
        pit.increment();
        assertEquals(11, pit.getStones());
    }
    @Test
    void testClear() {
        Pit pit = new Pit(10);
        assertEquals(10, pit.clear());
        assertEquals(0, pit.getStones());
    }
    @Test
    void testAddStones() {
        Pit pit = new Pit(10);
        pit.addStones(5);
        assertEquals(15, pit.getStones());
    }


}
