package org.Chess;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import org.Chess.Methods;

public class MainLoopServer {
    public Board mainBoard;
    public boolean whiteJoined = false;
    public boolean blackJoined = false;
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

    public String getPieceString(int pieceValue) {
        if (pieceValue == 0) return "empty";

        String color = (pieceValue & Methods.COLOR_MASK) == Methods.WHITE_MASK ? "white" : "black";
        int type = pieceValue & Methods.TYPE_MASK;

        return switch (type) {
            case Methods.KING  -> color + "_king";
            case Methods.QUEEN -> color + "_queen";
            case Methods.ROOK  -> color + "_rook";
            case Methods.BISHOP -> color + "_bishop";
            case Methods.KNIGHT -> color + "_knight";
            case Methods.PAWN   -> color + "_pawn";
            default             -> "empty";
        };
    }

    public int[] errorLoop() { // get a correct move and return in form [origin, target]
        // here get the correct loop
        return new int[]{-1, -1}; // temp, replace
    }

    public MainLoopServer() {
        mainBoard = new Board();
        checkMate = false;
        staleMate = false;
        whiteKingMoved = blackKingMoved = whiteLeftRookMoved = whiteRightRookMoved = blackLeftRookMoved = blackRightRookMoved = false;
        lastMoveOrigin = 0;
        lastMoveTarget = 0;
        HashMap<Integer, int[]> rawMoves = Moves.allColorMoves(
            mainBoard.boardState,
            mainBoard.turnMask,
            lastMoveOrigin, lastMoveTarget,
            false, false, false
        );
        validMovesBuffer = Moves.validateAllColorMoves(mainBoard.boardState, rawMoves, mainBoard.turnMask);
        // convert and send valid moves to white

        lastMoveOrigin = -1;
        lastMoveTarget = -1;
    }

    public boolean handleMove(int moveOrigin, int moveTarget) {
        if(!validMovesBuffer.containsKey(moveOrigin)) return false;

        int[] targets = validMovesBuffer.get(moveOrigin);
        boolean valid = false;
        for(int t : targets) if(t == moveTarget) valid = true;
        if(!valid) return false;

        updateMovementFlags(moveOrigin, moveTarget);
        Moves.move(mainBoard.boardState, moveOrigin, moveTarget);

        if(Methods.checkPromotion(mainBoard.boardState, moveTarget)) {
            Methods.promote(mainBoard.boardState, moveTarget, Methods.QUEEN);
        }

        lastMoveOrigin = moveOrigin;
        lastMoveTarget = moveTarget;
        mainBoard.changeTurnMask();

        refreshMoves();
        checkGameOver();
        return true;
    }

    private void refreshMoves() {
        boolean isWhiteTurn = (mainBoard.turnMask == Methods.WHITE_MASK);

        int effectiveTarget = (lastMoveTarget == -1) ? 0 : lastMoveTarget;
        int effectiveOrigin = (lastMoveOrigin == -1) ? 0 : lastMoveOrigin;

        HashMap<Integer, int[]> rawMoves = Moves.allColorMoves(
            mainBoard.boardState,
            mainBoard.turnMask,
            effectiveOrigin,
            effectiveTarget,
            isWhiteTurn ? whiteKingMoved : blackKingMoved,
            isWhiteTurn ? whiteLeftRookMoved : blackLeftRookMoved,
            isWhiteTurn ? whiteRightRookMoved : blackRightRookMoved
        );
        validMovesBuffer = Moves.validateAllColorMoves(mainBoard.boardState, rawMoves, mainBoard.turnMask);

        System.out.println("Turn: " + (isWhiteTurn ? "White" : "Black") + " | Moves found: " + validMovesBuffer.size());
    }

    private void checkGameOver() {
        int kingIdx = Methods.findKing(mainBoard.boardState, mainBoard.turnMask);
        // You need to check if the square is attacked by the OPPOSITE color
        int opponentMask = (mainBoard.turnMask == Methods.WHITE_MASK) ? Methods.BLACK_MASK : Methods.WHITE_MASK;
        
        boolean inCheck = Methods.isSquareAttacked(mainBoard.boardState, kingIdx, mainBoard.turnMask); 
        // ^ Wait: Inside isSquareAttacked, it calculates oppMask internally. 
        // But your call passes turnMask as the 'colorMask'. 
        // This is actually okay ONLY IF isSquareAttacked is written to treat the 3rd param as 'Friend color'.
    }

    private void updateMovementFlags(int moveOrigin, int moveTarget) {
        int piece = mainBoard.boardState[moveOrigin];
        int type = piece & Methods.TYPE_MASK;
        int color = piece & Methods.COLOR_MASK;
        if (type == Methods.KING) {
            if(color == Methods.WHITE_MASK) whiteKingMoved = true;
            else blackKingMoved = true;
        }
        if (moveOrigin == 0 || moveTarget == 0) whiteLeftRookMoved = true;
        if (moveOrigin == 7 || moveTarget == 7) whiteRightRookMoved = true;
        if (moveOrigin == 56 || moveTarget == 56) blackLeftRookMoved = true;
        if (moveOrigin == 63 || moveTarget == 63) blackRightRookMoved = true;
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
}

