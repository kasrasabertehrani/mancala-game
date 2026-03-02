package com.mancalagame.model;

import java.util.UUID;

public class Player {

    private String id;
    private String name;

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
}
