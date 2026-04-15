package com.mancalagame;

import com.mancalagame.model.Board;
import com.mancalagame.model.Pit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {
    @Test
    void testBoardInitialization() {
        Board board = new Board();
        // Check that there are 14 pits
        assertEquals(14, board.getPits().length);
        // Check that the first 6 pits for player 1 have 4 stones each
        for (int i = 0; i < 6; i++) {
            assertEquals(4, board.getPits()[i].getStones());
        }
        // Check that the first 6 pits for player 2 have 4 stones each
        for (int i = 7; i < 13; i++) {
            assertEquals(4, board.getPits()[i].getStones());
        }
        // Check that the Mancala pits start with 0 stones
        assertEquals(0, board.getPits()[6].getStones()); // Player 1's Mancala
        assertEquals(0, board.getPits()[13].getStones()); // Player 2's Mancala
    }

    @Test
    void testSowStones_forPlayer1() {
        Board board = new Board();
        int stonesSown = board.sowStones(0, 13);
        assertEquals(4, stonesSown);
        assertEquals(0, board.getPits()[0].getStones());
        for (int i = 1; i < 5; i++) {
            assertEquals(5, board.getPits()[i].getStones());
        }
    }
    @Test
    void testSowStones_WhenPassingOpponentStoreIndex() {
        Board board = new Board();

        // Make the board simple and controlled
        for (Pit pit : board.getPits()) {
            pit.clear();
        }

        // Put enough stones in pit 5 to pass over index 13
        board.getPits()[5].addStones(10);

        int lastIndex = board.sowStones(5, 13);

        // Opponent store must still be empty
        assertEquals(0, board.getStonesAt(13));

        // The move should not end on the opponent store
        assertNotEquals(13, lastIndex);
    }

    @Test
    void testIsOnSide(){
        Board board = new Board();
        assertTrue(board.isOnSide(true, 0));
        assertFalse(board.isOnSide(true, 7));
        assertTrue(board.isOnSide(false, 8));
        assertFalse(board.isOnSide(false, 5));

        assertFalse(board.isOnSide(true, 13));
        assertTrue(board.isOnSide(true, 0));
        assertFalse(board.isOnSide(true, 6));

        assertFalse(board.isOnSide(false, 13));
        assertFalse(board.isOnSide(false, 0));
        assertFalse(board.isOnSide(false, 6));

        assertFalse(board.isOnSide(true, -1));
        assertFalse(board.isOnSide(false, -1));
        assertFalse(board.isOnSide(true, 14));
        assertFalse(board.isOnSide(false, 14));
    }

    @Test
    void testIsSideEmpty(){
        Board board = new Board();
        // Make the board simple and controlled
        for(int i = 1; i < 6; i++){
            board.getPits()[i].clear();
        }
        assertTrue(board.isSideEmpty(1,5));
        assertFalse(board.isSideEmpty(7,12));

    }

    @Test
    void testSweepRemaining(){
        Board board = new Board();
        board.sweepRemaining(1,5,6);
        assertEquals(20, board.getPits()[6].getStones());
    }

    @Test
    public void testSuccessfulCapture() {
        Board board = new Board();
        int lastIndex = 2; // Player 1's side
        int myStoreIndex = 6; // Player 1's store
        int oppositeIndex = 12 - lastIndex; // Index 10

        for (Pit pit : board.getPits()) {
            pit.clear();
        }

        // 1. ARRANGE: Set up the specific capture scenario
        board.getPits()[lastIndex].addStones(1); // The capturing stone
        board.getPits()[oppositeIndex].addStones(5); // Stones to steal
        // Store is already at 0 because of setUp()

        // 2. ACT: Trigger the capture logic
        board.attemptCapture(true, lastIndex, myStoreIndex);

        // 3. ASSERT: Verify the stones moved correctly
        assertEquals(0, board.getPits()[lastIndex].getStones(), "Capturing pit should be empty");
        assertEquals(0, board.getPits()[oppositeIndex].getStones(), "Opponent's pit should be empty");
        assertEquals(6, board.getPits()[myStoreIndex].getStones(), "Store should have 6 stones (1 + 5)");
    }

    @Test
    public void testFailedCapture_PitNotEndingAtOne() {
        Board board = new Board();
        int lastIndex = 2;
        int myStoreIndex = 6;
        int oppositeIndex = 12 - lastIndex;

        for (Pit pit : board.getPits()) {
            pit.clear();
        }

        // 1. ARRANGE: Make the landing pit have more than 1 stone
        board.getPits()[lastIndex].addStones(2);
        board.getPits()[oppositeIndex].addStones(5);

        // 2. ACT
        board.attemptCapture(true, lastIndex, myStoreIndex);

        // 3. ASSERT: Verify nothing was captured
        assertEquals(2, board.getPits()[lastIndex].getStones(), "Capturing pit should still have 2 stones");
        assertEquals(5, board.getPits()[oppositeIndex].getStones(), "Opponent's pit should remain untouched");
        assertEquals(0, board.getPits()[myStoreIndex].getStones(), "Store should still be empty");
    }

    @Test
    public void testFailedCapture_LandedOnOpponentSide() {
        Board board = new Board();
        int lastIndex = 9; // A pit on Player 2's side
        int myStoreIndex = 6; // Player 1's store

        for (Pit pit : board.getPits()) {
            pit.clear();
        }
        // 1. ARRANGE
        board.getPits()[lastIndex].addStones(1);

        // 2. ACT: Player 1 tries to capture while landing on Player 2's side
        board.attemptCapture(true, lastIndex, myStoreIndex);

        // 3. ASSERT
        assertEquals(1, board.getPits()[lastIndex].getStones(), "Stone should remain untouched because it is the wrong side");
        assertEquals(0, board.getPits()[myStoreIndex].getStones(), "Store should remain empty");
    }

    @Test
    void testFailedCapture_LastIndexIsMyStore() {
        Board board = new Board();

        for (Pit pit : board.getPits()) {
            pit.clear();
        }

        int myStoreIndex = 6;
        board.getPits()[myStoreIndex].addStones(3);

        board.attemptCapture(true, myStoreIndex, myStoreIndex);

        assertEquals(3, board.getPits()[myStoreIndex].getStones(), "Store should remain unchanged");
    }
    @Test
    void testFailedCapture_OppositePitEmpty() {
        Board board = new Board();

        for (Pit pit : board.getPits()) {
            pit.clear();
        }

        int lastIndex = 2;      // player 1 side
        int myStoreIndex = 6;
        int oppositeIndex = 12 - lastIndex; // 10

        board.getPits()[lastIndex].addStones(1);   // landing pit has exactly 1
        // opposite stays 0

        board.attemptCapture(true, lastIndex, myStoreIndex);

        assertEquals(1, board.getPits()[lastIndex].getStones(), "No capture: landing pit should remain 1");
        assertEquals(0, board.getPits()[oppositeIndex].getStones(), "Opposite pit should remain 0");
        assertEquals(0, board.getPits()[myStoreIndex].getStones(), "Store should remain unchanged");
    }
    @Test
    void testGetStoreIndex() {
        Board board = new Board();

        assertEquals(6, board.getStoreIndex(true), "Player 1 store index should be 6");
        assertEquals(13, board.getStoreIndex(false), "Player 2 store index should be 13");
    }

    @Test
    void testPlayerScores_InitialState() {
        Board board = new Board();

        assertEquals(0, board.getPlayer1Score(), "Player 1 score should start at 0");
        assertEquals(0, board.getPlayer2Score(), "Player 2 score should start at 0");
    }

    @Test
    void testPlayerScores_AfterAddingToStores() {
        Board board = new Board();

        // Arrange: put stones directly into stores
        board.getPits()[6].addStones(7);   // Player 1 store
        board.getPits()[13].addStones(5);  // Player 2 store

        // Assert
        assertEquals(7, board.getPlayer1Score(), "Player 1 score should reflect store stones");
        assertEquals(5, board.getPlayer2Score(), "Player 2 score should reflect store stones");
    }

}
