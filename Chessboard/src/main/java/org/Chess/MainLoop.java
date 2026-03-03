package org.Chess;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Handler;

public class MainLoop {
    public Board mainBoard;
    public boolean checkMate;
    public boolean staleMate;
    public HashMap<Integer, int[]> validMovesBuffer;
    public int lastMoveOrigin;
    public int lastMoveTarget;
    public boolean whiteKingMoved;
    public boolean blackKingMoved;
    public boolean whiteLeftRookMoved;
    public boolean blackLeftRookMoved;
    public boolean whiteRightRookMoved;
    public boolean blackRightRookMoved;

    public int[] errorLoop() { // get a correct move and return in form [origin, target]
        // here get the correct loop
        return new int[]{-1, -1}; // temp, replace
    }

    public void startGame() {
        mainBoard = new Board();
        checkMate = false;
        staleMate = false;
        whiteKingMoved = blackKingMoved = whiteLeftRookMoved = whiteRightRookMoved = blackLeftRookMoved = blackRightRookMoved = false;
        lastMoveOrigin = -1;
        lastMoveTarget = -1;
        HashMap<Integer, int[]> rawMoves = Moves.allColorMoves(mainBoard.boardState, mainBoard.turnMask, lastMoveOrigin, lastMoveTarget, false, false, false);
        validMovesBuffer = Moves.validateAllColorMoves(mainBoard.boardState, rawMoves, mainBoard.turnMask);
        // convert and send valid moves to white
    }

    public boolean updateGameLoop() {
        int moveOrigin = 8;
        int moveTarget = 24; // THESE ARE TEMPS, get real moves from ui

        if (!validMovesBuffer.containsKey(moveOrigin)) { // validate moves
            // get real moves
            return true;
        }
        int piece = mainBoard.boardState[moveOrigin]; // get type and color
        int type = piece & Methods.TYPE_MASK;
        int color = piece & Methods.COLOR_MASK;

        if (type == Methods.KING) { // king moved
            if (color == Methods.WHITE_MASK) whiteKingMoved = true;
            else blackKingMoved = true;
        }
        if (moveOrigin == 0 || moveTarget == 0) whiteLeftRookMoved = true; // rook moved or captured
        if (moveOrigin == 7 || moveTarget == 7) whiteRightRookMoved = true;
        if (moveOrigin == 56 || moveTarget == 56) blackLeftRookMoved = true;
        if (moveOrigin == 63 || moveTarget == 63) blackRightRookMoved = true;

        Moves.move(mainBoard.boardState, moveOrigin, moveTarget); // move

        if (Methods.checkPromotion(mainBoard.boardState, moveTarget)) { // promotion check
            Methods.promote(mainBoard.boardState, moveTarget, Methods.QUEEN); // replace methods queen with getting promotion tupe
        }

        lastMoveOrigin = moveOrigin; // move history
        lastMoveTarget = moveTarget;

        mainBoard.changeTurnMask(); // change turn

        boolean isWhiteTurn = (mainBoard.turnMask == Methods.WHITE_MASK); // generate moves

        HashMap<Integer, int[]> rawMoves = Moves.allColorMoves( // get raw moves
                mainBoard.boardState,
                mainBoard.turnMask,
                lastMoveOrigin,
                lastMoveTarget,
                isWhiteTurn ? whiteKingMoved : blackKingMoved,
                isWhiteTurn ? whiteLeftRookMoved : blackLeftRookMoved,
                isWhiteTurn ? whiteRightRookMoved : blackRightRookMoved
        );

        validMovesBuffer = Moves.validateAllColorMoves(mainBoard.boardState, rawMoves, mainBoard.turnMask); // validate

        // check game state
        int kingIdx = Methods.findKing(mainBoard.boardState, mainBoard.turnMask);
        boolean inCheck = Methods.isSquareAttacked(mainBoard.boardState, kingIdx, mainBoard.turnMask);

        if (validMovesBuffer.isEmpty()) { // checkMate and stalemate
            if (inCheck) checkMate = true;
            else staleMate = true;
            return false;
        }

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

