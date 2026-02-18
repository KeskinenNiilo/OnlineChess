package org.Chess;

import java.util.HashMap;
import java.util.logging.Handler;

public class MainLoop {
    public Board mainBoard;
    public boolean checkMate;
    public boolean staleMate;
    public HashMap<Integer, int[]> validMovesBuffer;
    public int lastMoveOrigin;
    public int lastMoveTarget;


    public int[] errorLoop() { // get a correct move and return in form [origin, target]
        // here get the correct loop
        return new int[]{-1, -1}; // temp, replace
    }

    public void startGame() {
        mainBoard = new Board();
        checkMate = false;
        staleMate = false;
        lastMoveOrigin = -1;
        lastMoveTarget = -1;
        HashMap<Integer, int[]> rawMoves = Moves.allColorMoves(mainBoard.boardState, mainBoard.turnMask, lastMoveOrigin, lastMoveTarget);
        validMovesBuffer = Moves.validateAllColorMoves(mainBoard.boardState, rawMoves, mainBoard.turnMask);
        // convert and send valid moves to white
    }

    public boolean updateGameLoop() {
        // receive move from frontend
        int moveOrigin = 8; // convert move in fronend to index form and make it variable moveOrigin and moveTarget
        int moveTarget = 24;
        if (!validMovesBuffer.containsKey(moveOrigin) || validMovesBuffer.get(moveOrigin) == null) {
            int[] validMoves = errorLoop();
            moveOrigin = validMoves[0];
            moveTarget = validMoves[1];
        }
        lastMoveOrigin = moveOrigin;
        lastMoveTarget = moveTarget;
        Moves.move(mainBoard.boardState, moveOrigin, moveTarget); // move in board
        Methods.promote(mainBoard.boardState, moveTarget, Methods.QUEEN); // replace Methods.QUEEN with piece int got from frontend
        mainBoard.changeTurnMask(); // change turn
        boolean check = Methods.inCheck(mainBoard.boardState, Methods.findKing(mainBoard.boardState, mainBoard.turnMask)); // check for check
        HashMap<Integer, int[]> rawMoves = Moves.allColorMoves(mainBoard.boardState, mainBoard.turnMask, lastMoveOrigin, lastMoveTarget);
        validMovesBuffer = Moves.validateAllColorMoves(mainBoard.boardState, rawMoves, mainBoard.turnMask); // uudet movet
        if (check && validMovesBuffer.isEmpty()) {
            checkMate = true;
            return false;
        }
        else if (!check && validMovesBuffer.isEmpty()) {
            staleMate = true;
            return false;
        }
        // send valid moves and check state to frontend
        return true;
    }

    public boolean endGame() {
        // code to display end game screen and return true if restart game is called
        return false;
    }

    public void mainGameLoop() {
        boolean gameRunning = true;
        while (gameRunning) {
            startGame();
            while (updateGameLoop());
            gameRunning = endGame();
        }
    }
}

