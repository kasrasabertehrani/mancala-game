package com.mancalagame.model;

public class Game {

    public enum GameStatus {
        WAITING_FOR_PLAYER_2,
        PLAYER_1_TURN,
        PLAYER_2_TURN,
        GAME_OVER
    }

    private GameStatus gameStatus;
    private Pit[] pits;


    public Game() {
        this.gameStatus = GameStatus.WAITING_FOR_PLAYER_2;
        this.pits = new Pit[14];
        for (int i = 0; i < 14; i++) {
            if (i == 6 || i == 13) {
                pits[i] = new Pit(0);
            } else {
                pits[i] = new Pit(4);
            }
        }
    }

    public void startGame() {
        this.gameStatus = GameStatus.PLAYER_1_TURN;
    }

    public Pit[] getPits() {
        return pits;
    }

    public void setPits(Pit[] pits) {
        this.pits = pits;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }
}