package com.mancalagame.model;

public class Game {

    public enum GameStatus {
        WAITING_FOR_PLAYER_2,
        PLAYER_1_TURN,
        PLAYER_2_TURN,
        GAME_OVER,
        PLAYER_DISCONNECTED,
        PAUSED_FOR_RECONNECT
    }

    private final Player player1;
    private Player player2;
    private final Board board;
    private GameStatus gameStatus;
    private String disconnectedPlayerId;
    private GameStatus previousStatus;

    // --- 1. CONSTRUCTOR & INITIALIZATION ---

    public Game(Player player1) {
        this.player1 = player1;
        this.board = new Board();
        this.gameStatus = GameStatus.WAITING_FOR_PLAYER_2;
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
        board.sweepRemaining(Board.PLAYER_1_PIT_START, Board.PLAYER_1_PIT_END, Board.PLAYER_1_STORE);
        board.sweepRemaining(Board.PLAYER_2_PIT_START, Board.PLAYER_2_PIT_END, Board.PLAYER_2_STORE);
    }

    // --- 3. THE CORE ENGINE ---

    public void playTurn(String playerId, int pitIndex) {
        validateMove(playerId, pitIndex);

        boolean isPlayer1 = isPlayer1(playerId);
        int myStoreIndex = board.getStoreIndex(isPlayer1);
        int opponentStoreIndex = board.getStoreIndex(!isPlayer1);

        int lastIndex = board.sowStones(pitIndex, opponentStoreIndex);

        board.attemptCapture(isPlayer1, lastIndex, myStoreIndex);

        if (lastIndex != myStoreIndex) {
            switchTurn();
        }

        checkGameOver();
    }

    private void checkGameOver() {
        boolean p1Empty = board.isSideEmpty(Board.PLAYER_1_PIT_START, Board.PLAYER_1_PIT_END);
        boolean p2Empty = board.isSideEmpty(Board.PLAYER_2_PIT_START, Board.PLAYER_2_PIT_END);

        if (p1Empty || p2Empty) {
            endGame();
        }
    }

    public void handleDisconnect(String playerId) {
        if (player2 == null) {
            this.gameStatus = GameStatus.GAME_OVER;
            return;
        }

        this.previousStatus = this.gameStatus;
        this.gameStatus = GameStatus.PAUSED_FOR_RECONNECT;
        this.disconnectedPlayerId = playerId;
    }

    public void handleReconnect(String playerId) {
        if (gameStatus != GameStatus.PAUSED_FOR_RECONNECT) return;
        if (!playerId.equals(disconnectedPlayerId)) return;

        this.gameStatus = this.previousStatus;
        this.disconnectedPlayerId = null;
        this.previousStatus = null;
    }

    public void forfeit(String playerId) {
        this.gameStatus = GameStatus.PLAYER_DISCONNECTED;
        this.disconnectedPlayerId = playerId;
    }

    public String getWinner() {
        if (gameStatus == GameStatus.PLAYER_DISCONNECTED) {
            // The person who DID NOT quit is the winner.
            return isPlayer1(disconnectedPlayerId) ? player2.getId() : player1.getId();
        }

        if (gameStatus != GameStatus.GAME_OVER) {
            return null;
        }

        int p1Score = board.getPlayer1Score();
        int p2Score = board.getPlayer2Score();

        if (p1Score > p2Score) {
            return player1.getId();
        } else if (p2Score > p1Score) {
            return player2.getId();
        } else {
            return "DRAW";
        }
    }


    // --- 4. INTERNAL VALIDATION & HELPERS ---

    private void validateMove(String playerId, int pitIndex) {
        if (playerId == null || (!isPlayer1(playerId) && (player2 == null || !player2.getId().equals(playerId)))) {
            throw new IllegalArgumentException("Unknown player.");
        }

        boolean isPlayer1 = isPlayer1(playerId);

        if (this.gameStatus == GameStatus.WAITING_FOR_PLAYER_2
                || this.gameStatus == GameStatus.GAME_OVER
                || this.gameStatus == GameStatus.PLAYER_DISCONNECTED
                || this.gameStatus == GameStatus.PAUSED_FOR_RECONNECT) {
            throw new IllegalStateException("Game is not in a playable state.");
        }
        if (isPlayer1 && this.gameStatus != GameStatus.PLAYER_1_TURN) {
            throw new IllegalStateException("It's not Player 1's turn!");
        }
        if (!isPlayer1 && this.gameStatus != GameStatus.PLAYER_2_TURN) {
            throw new IllegalStateException("It's not Player 2's turn!");
        }
        if (!board.isOnSide(isPlayer1, pitIndex)) {
            throw new IllegalArgumentException(
                isPlayer1 ? "Player 1 can only pick pits 0-5." : "Player 2 can only pick pits 7-12."
            );
        }
        if (board.getStonesAt(pitIndex) == 0) {
            throw new IllegalArgumentException("Cannot pick an empty pit.");
        }
    }

    private boolean isPlayer1(String playerId) {
        return this.player1.getId().equals(playerId);
    }

    // --- 5. PUBLIC GETTERS ---

    public int getPlayer1Score() { return board.getPlayer1Score(); }
    public int getPlayer2Score() { return board.getPlayer2Score(); }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public Board getBoard() { return board; }
    public GameStatus getGameStatus() { return gameStatus; }
    public String getDisconnectedPlayerId() {
        return disconnectedPlayerId;
    }
}

