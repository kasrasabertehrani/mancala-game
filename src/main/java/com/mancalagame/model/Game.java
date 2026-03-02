package com.mancalagame.model;

public class Game {

    public enum GameStatus {
        WAITING_FOR_PLAYER_2,
        PLAYER_1_TURN,
        PLAYER_2_TURN,
        GAME_OVER
    }

    private Player player1;
    private Player player2;
    private Pit[] pits;
    private GameStatus gameStatus;

    // --- 1. CONSTRUCTOR & INITIALIZATION ---

    public Game(Player player1) {
        this.player1 = player1;
        initializeGame();
    }

    private void initializeGame() {
        this.gameStatus = GameStatus.WAITING_FOR_PLAYER_2;
        this.pits = new Pit[14];
        for (int i = 0; i < 14; i++) {
            if (i == 6 || i == 13) {
                this.pits[i] = new Pit(0); // Stores start empty
            } else {
                this.pits[i] = new Pit(4); // Normal pits start with 4
            }
        }
    }

    // --- 2. LIFECYCLE METHODS ---

    public void setPlayer2(Player player2) {
        this.player2 = player2;
        // The game automatically starts when player 2 sits down!
        this.gameStatus = GameStatus.PLAYER_1_TURN;
    }

    private void switchTurn() {
        if (this.gameStatus == GameStatus.PLAYER_1_TURN) {
            this.gameStatus = GameStatus.PLAYER_2_TURN;
        } else if (this.gameStatus == GameStatus.PLAYER_2_TURN) {
            this.gameStatus = GameStatus.PLAYER_1_TURN;
        }
    }

    private void endGame() {
        this.gameStatus = GameStatus.GAME_OVER;

        // Sweep Player 1's side
        for (int i = 0; i <= 5; i++) {
            int remainingStones = pits[i].clear();
            pits[6].addStones(remainingStones);
        }

        // Sweep Player 2's side
        for (int i = 7; i <= 12; i++) {
            int remainingStones = pits[i].clear();
            pits[13].addStones(remainingStones);
        }
    }

    // --- 3. THE CORE ENGINE ---

    public void playTurn(String playerId, int pitIndex) {
        // 1. Enforce the rules
        validateMove(playerId, pitIndex);

        boolean isPlayer1 = isPlayer1(playerId);
        int myStoreIndex = isPlayer1 ? 6 : 13;
        int opponentStoreIndex = isPlayer1 ? 13 : 6;

        // 2. Pick up stones
        int stonesInHand = pits[pitIndex].clear();
        int currentIndex = pitIndex;

        // 3. The Sowing Loop
        while (stonesInHand > 0) {
            currentIndex = (currentIndex + 1) % 14;

            if (currentIndex == opponentStoreIndex) {
                continue; // Skip opponent's store
            }

            pits[currentIndex].increment();
            stonesInHand--;
        }

        // 4. Capture Rule
        if (currentIndex != myStoreIndex && isMySide(isPlayer1, currentIndex) && pits[currentIndex].getStones() == 1) {
            int oppositeIndex = 12 - currentIndex;
            if (pits[oppositeIndex].getStones() > 0) {
                int capturedStones = pits[oppositeIndex].clear() + pits[currentIndex].clear();
                pits[myStoreIndex].addStones(capturedStones);
            }
        }

        // 5. Free Turn Check
        if (currentIndex != myStoreIndex) {
            switchTurn();
        }

        // 6. End of Game Check
        checkGameOver();
    }

    // --- 4. INTERNAL VALIDATION & HELPERS ---

    private void validateMove(String playerId, int pitIndex) {
        boolean isPlayer1 = isPlayer1(playerId);

        if (this.gameStatus == GameStatus.WAITING_FOR_PLAYER_2 || this.gameStatus == GameStatus.GAME_OVER) {
            throw new IllegalStateException("Game is not in a playable state.");
        }
        if (isPlayer1 && this.gameStatus != GameStatus.PLAYER_1_TURN) {
            throw new IllegalStateException("It's not Player 1's turn!");
        }
        if (!isPlayer1 && this.gameStatus != GameStatus.PLAYER_2_TURN) {
            throw new IllegalStateException("It's not Player 2's turn!");
        }
        if (isPlayer1 && (pitIndex < 0 || pitIndex > 5)) {
            throw new IllegalArgumentException("Player 1 can only pick pits 0-5.");
        }
        if (!isPlayer1 && (pitIndex < 7 || pitIndex > 12)) {
            throw new IllegalArgumentException("Player 2 can only pick pits 7-12.");
        }
        if (this.pits[pitIndex].getStones() == 0) {
            throw new IllegalArgumentException("Cannot pick an empty pit.");
        }
    }

    private void checkGameOver() {
        boolean p1Empty = true;
        boolean p2Empty = true;

        for (int i = 0; i <= 5; i++) {
            if (pits[i].getStones() > 0) p1Empty = false;
        }
        for (int i = 7; i <= 12; i++) {
            if (pits[i].getStones() > 0) p2Empty = false;
        }

        if (p1Empty || p2Empty) {
            endGame();
        }
    }

    private boolean isPlayer1(String playerId) {
        return this.player1.getId().equals(playerId);
    }

    private boolean isMySide(boolean isPlayer1, int index) {
        return isPlayer1 ? (index >= 0 && index <= 5) : (index >= 7 && index <= 12);
    }

    // --- 5. PUBLIC GETTERS (No Setters allowed for internal state!) ---

    public int getPlayer1Score() { return pits[6].getStones(); }
    public int getPlayer2Score() { return pits[13].getStones(); }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public Pit[] getPits() { return pits; }
    public GameStatus getGameStatus() { return gameStatus; }
}