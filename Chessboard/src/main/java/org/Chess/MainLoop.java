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


    
    private void calculateMaterialFromBoard() {
        whiteMaterial = 0;
        blackMaterial = 0;
        
        for (int i = 0; i < 64; i++) {
            int piece = mainBoard.boardState[i];
            if (piece == 0) continue;
            
            int pieceType = piece & Methods.TYPE_MASK;
            int pieceColor = piece & Methods.COLOR_MASK;
            int pieceValue = Methods.getPieceValue(pieceType);
            
            if (pieceColor == Methods.WHITE_MASK) {
                whiteMaterial += pieceValue;
            } else {
                blackMaterial += pieceValue;
            }
        }
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
        // Calculate initial material
        calculateMaterialFromBoard();
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
        // Update material BEFORE the move (capture logic)
        int capturedPiece = mainBoard.boardState[moveTarget];
        if (capturedPiece != 0) {
            int capturedValue = Methods.getPieceValue(capturedPiece & Methods.TYPE_MASK);
            if (color == Methods.WHITE_MASK) {
                blackMaterial -= capturedValue; // White captured black piece
            } else {
                whiteMaterial -= capturedValue; // Black captured white piece
            }
        }

        if (type == Methods.KING) { // king moved
            if (color == Methods.WHITE_MASK) whiteKingMoved = true;
            else blackKingMoved = true;
        }
        if (moveOrigin == 0 || moveTarget == 0) whiteLeftRookMoved = true; // rook moved or captured
        if (moveOrigin == 7 || moveTarget == 7) whiteRightRookMoved = true;
        if (moveOrigin == 56 || moveTarget == 56) blackLeftRookMoved = true;
        if (moveOrigin == 63 || moveTarget == 63) blackRightRookMoved = true;

        Moves.move(mainBoard.boardState, moveOrigin, moveTarget); // move

        // Handle promotion material update
        if (Methods.checkPromotion(mainBoard.boardState, moveTarget)) {
            int oldValue = Methods.PAWN_VALUE; // Pawn was worth 1
            int newValue = Methods.QUEEN_VALUE; // Promoting to queen (9)
            int valueDiff = newValue - oldValue;
            
            if (color == Methods.WHITE_MASK) {
                whiteMaterial += valueDiff;
            } else {
                blackMaterial += valueDiff;
            }
            
            Methods.promote(mainBoard.boardState, moveTarget, Methods.QUEEN);
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

    // Getter methods for frontend
    public int getWhiteMaterial() {
        return whiteMaterial;
    }
    
    public int getBlackMaterial() {
        return blackMaterial;
    }
    
    public int getMaterialBalance() {
        return whiteMaterial - blackMaterial;
    }
}

