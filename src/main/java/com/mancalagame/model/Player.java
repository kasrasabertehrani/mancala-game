package com.mancalagame.model;

import java.util.UUID;

public class Player {

    private String id;
    private String name;
    private int stones;

    public Player(String name, String id) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }



    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public int getStones() {return stones;}
    public void setStones(int stones) {this.stones = stones;}

    public void dropStone() {if (stones > 0) stones--;}
}
