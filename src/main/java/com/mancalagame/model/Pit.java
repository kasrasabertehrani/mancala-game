package com.mancalagame.model;

public class Pit {
    private int stones;

    public Pit(int startingStones) {
        this.stones = startingStones;
    }

    public int getStones() {
        return stones;
    }

    // Adds exactly one stone (used during the sowing loop)
    public void increment() {
        this.stones++;
    }

    // Adds multiple stones (used during a capture or the final sweep)
    public void addStones(int amount) {
        this.stones += amount;
    }

    // Empties the pit and tells you how many stones you just picked up
    public int clear() {
        int pickedUp = this.stones;
        this.stones = 0;
        return pickedUp;
    }
}