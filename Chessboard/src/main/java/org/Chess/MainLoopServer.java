package org.Chess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    
    // ========== ADD THESE VARIABLES ==========
    public boolean gameOver;
    public String winner;
    public boolean drawOffer;
    public String drawOfferedBy;
    public boolean drawAccepted;
    
    // Material tracking
    public int whiteMaterial;
    public int blackMaterial;

    public boolean whiteReadyToRestart = false;
    public boolean blackReadyToRestart = false;

    public List<String> eventLog = new ArrayList<>();

    public void addEvent(String message) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        eventLog.add("[" + timestamp + "]" + message);
    }

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

    public int[] errorLoop() {
        return new int[]{-1, -1};
    }

    public MainLoopServer() {
        mainBoard = new Board();
        checkMate = false;
        staleMate = false;
        
        // Initialize game state
        gameOver = false;
        winner = null;
        drawOffer = false;
        drawOfferedBy = null;
        drawAccepted = false;
        
        whiteKingMoved = blackKingMoved = whiteLeftRookMoved = whiteRightRookMoved = blackLeftRookMoved = blackRightRookMoved = false;
        lastMoveOrigin = 0;
        lastMoveTarget = 0;
        
        // Calculate initial material
        calculateMaterialFromBoard();
        
        HashMap<Integer, int[]> rawMoves = Moves.allColorMoves(
                mainBoard.boardState,
                mainBoard.turnMask,
                lastMoveOrigin, lastMoveTarget,
                false, false, false
        );
        validMovesBuffer = Moves.validateAllColorMoves(mainBoard.boardState, rawMoves, mainBoard.turnMask);

        lastMoveOrigin = -1;
        lastMoveTarget = -1;
    }
    
    // ========== MATERIAL TRACKING METHODS ==========
    
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

    public boolean handleMove(int moveOrigin, int moveTarget) {
        // Don't allow moves if game is over
        if (gameOver) return false;
        
        if(!validMovesBuffer.containsKey(moveOrigin)) return false;

        int[] targets = validMovesBuffer.get(moveOrigin);
        boolean valid = false;
        for(int t : targets) if(t == moveTarget) valid = true;
        if(!valid) return false;

        // Handle capture BEFORE moving
        int capturedPiece = mainBoard.boardState[moveTarget];
        int movingPiece = mainBoard.boardState[moveOrigin];
        int pieceColor = movingPiece & Methods.COLOR_MASK;
        
        if (capturedPiece != 0) {
            int capturedValue = Methods.getPieceValue(capturedPiece & Methods.TYPE_MASK);
            if (pieceColor == Methods.WHITE_MASK) {
                blackMaterial -= capturedValue;
            } else {
                whiteMaterial -= capturedValue;
            }
        }

        updateMovementFlags(moveOrigin, moveTarget);
        Moves.move(mainBoard.boardState, moveOrigin, moveTarget);

        // Castling
        int movedPieceType = mainBoard.boardState[moveTarget] & Methods.TYPE_MASK;
        if (movedPieceType == Methods.KING) {
            int fileDiff = (moveTarget % 8) - (moveOrigin % 8);
            if (fileDiff == 2) {
                int rookOrigin = moveOrigin + 3; // kingside
                int rookTarget = moveOrigin + 1;
                Moves.move(mainBoard.boardState, rookOrigin, rookTarget);
            } else if (fileDiff == -2) {
                int rookOrigin = moveOrigin - 4; // queenside
                int rookTarget = moveOrigin - 1;
                Moves.move(mainBoard.boardState, rookOrigin, rookTarget);
            }
        }

        // En Passant
        if (movedPieceType == Methods.PAWN) {
            int fileDiff = Math.abs((moveTarget % 8) - (moveOrigin % 8));
            int rankDiff = Math.abs((moveTarget / 8) - (moveOrigin / 8));
            if (fileDiff == 1 && rankDiff == 1) { // must be diagonal by 1
                int capturedPawnIdx = moveOrigin + (moveTarget % 8) - (moveOrigin % 8);
                int capturedEnPassant = mainBoard.boardState[capturedPawnIdx];
                int moverColor = mainBoard.boardState[moveTarget] & Methods.COLOR_MASK;
                if ((capturedEnPassant & Methods.TYPE_MASK) == Methods.PAWN // check that piece to be captured is a pawn
                        && (capturedEnPassant & Methods.COLOR_MASK) != moverColor) { // and an opp
                    mainBoard.boardState[capturedPawnIdx] = 0; // remove and update material
                    if (moverColor == Methods.WHITE_MASK) {
                        blackMaterial -= Methods.PAWN_VALUE;
                    } else {
                        whiteMaterial -= Methods.PAWN_VALUE;
                    }
                }
            }
        }

        // Handle promotion
        if(Methods.checkPromotion(mainBoard.boardState, moveTarget)) {
            int oldValue = Methods.PAWN_VALUE;
            int newValue = Methods.QUEEN_VALUE;
            int valueDiff = newValue - oldValue;
            
            if (pieceColor == Methods.WHITE_MASK) {
                whiteMaterial += valueDiff;
            } else {
                blackMaterial += valueDiff;
            }
            
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
        boolean inCheck = Methods.isSquareAttacked(mainBoard.boardState, kingIdx, mainBoard.turnMask);

        if (validMovesBuffer.isEmpty()) {
            if (inCheck) {
                this.checkMate = true;
                this.gameOver = true;
                this.winner = (mainBoard.turnMask == Methods.WHITE_MASK) ? "black" : "white";
                System.out.println("Checkmate! " + winner.toUpperCase() + " wins!");
                addEvent("Checkmate!" + winner.toUpperCase() + " wins!");
            } else {
                this.staleMate = true;
                this.gameOver = true;
                this.winner = null;
                System.out.println("Stalemate detected.");
            }
        }
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
        int moveTarget = 24;

        if (!validMovesBuffer.containsKey(moveOrigin)) {
            return true;
        }
        int piece = mainBoard.boardState[moveOrigin];
        int type = piece & Methods.TYPE_MASK;
        int color = piece & Methods.COLOR_MASK;

        if (type == Methods.KING) {
            if (color == Methods.WHITE_MASK) whiteKingMoved = true;
            else blackKingMoved = true;
        }
        if (moveOrigin == 0 || moveTarget == 0) whiteLeftRookMoved = true;
        if (moveOrigin == 7 || moveTarget == 7) whiteRightRookMoved = true;
        if (moveOrigin == 56 || moveTarget == 56) blackLeftRookMoved = true;
        if (moveOrigin == 63 || moveTarget == 63) blackRightRookMoved = true;

        Moves.move(mainBoard.boardState, moveOrigin, moveTarget);

        if (Methods.checkPromotion(mainBoard.boardState, moveTarget)) {
            Methods.promote(mainBoard.boardState, moveTarget, Methods.QUEEN);
        }

        lastMoveOrigin = moveOrigin;
        lastMoveTarget = moveTarget;

        mainBoard.changeTurnMask();

        boolean isWhiteTurn = (mainBoard.turnMask == Methods.WHITE_MASK);

        HashMap<Integer, int[]> rawMoves = Moves.allColorMoves(
                mainBoard.boardState,
                mainBoard.turnMask,
                lastMoveOrigin,
                lastMoveTarget,
                isWhiteTurn ? whiteKingMoved : blackKingMoved,
                isWhiteTurn ? whiteLeftRookMoved : blackLeftRookMoved,
                isWhiteTurn ? whiteRightRookMoved : blackRightRookMoved
        );

        validMovesBuffer = Moves.validateAllColorMoves(mainBoard.boardState, rawMoves, mainBoard.turnMask);

        int kingIdx = Methods.findKing(mainBoard.boardState, mainBoard.turnMask);
        boolean inCheck = Methods.isSquareAttacked(mainBoard.boardState, kingIdx, mainBoard.turnMask);

        if (validMovesBuffer.isEmpty()) {
            if (inCheck) {
                checkMate = true;
                gameOver = true;
                winner = (mainBoard.turnMask == Methods.WHITE_MASK) ? "black" : "white";
            } else {
                staleMate = true;
                gameOver = true;
                winner = null;
            }
            return false;
        }

        return true;
    }

    public boolean endGame() {
        return false;
    }
    
    // ========== DRAW AND FORFEIT METHODS ==========
    
    public boolean offerDraw(String side) {
        if (gameOver) {
            System.out.println("Cannot offer draw - game already over");
            return false;
        }
        if (drawOffer) {
            System.out.println("Draw already offered");
            return false;
        }
        
        drawOffer = true;
        drawOfferedBy = side;
        System.out.println(side + " offers a draw");
        addEvent(side + " offers a draw");
        return true;
    }
    
    public boolean respondToDraw(String side, boolean accept) {
        if (!drawOffer) {
            System.out.println("No draw offer to respond to");
            return false;
        }
        if (side.equals(drawOfferedBy)) {
            System.out.println("Cannot respond to your own draw offer");
            return false;
        }
        
        if (accept) {
            gameOver = true;
            winner = null;
            drawAccepted = true;
            System.out.println("Draw accepted! Game is a draw.");
            addEvent("Draw accepted! It's a draw.");
        } else {
            drawOffer = false;
            drawOfferedBy = null;
            System.out.println("Draw declined. Game continues.");
            addEvent("Draw declined. Game continues.");
        }
        
        return true;
    }
    
    public boolean forfeit(String side) {
        if (gameOver) {
            System.out.println("Cannot forfeit - game already over");
            return false;
        }
        
        gameOver = true;
        winner = side.equals("white") ? "black" : "white";
        System.out.println(side + " forfeits! " + winner.toUpperCase() + " wins!");
        addEvent(side + " forfeited! " + winner.toUpperCase() + " wins!");
        return true;
    }

    public boolean requestRestart(String side) {
        if ("white".equalsIgnoreCase(side)) {
            whiteReadyToRestart = true;
        } else if ("black".equalsIgnoreCase(side)) {
            blackReadyToRestart = true;
        }

        if (whiteReadyToRestart && blackReadyToRestart) {
            restartGame();

            whiteReadyToRestart = false;
            blackReadyToRestart = false;
            return true;
        }
        return false;
    }
    
    public void restartGame() {
        // Reset board
        mainBoard = new Board();
        
        // Reset all flags
        checkMate = false;
        staleMate = false;
        gameOver = false;
        winner = null;
        drawOffer = false;
        drawOfferedBy = null;
        drawAccepted = false;
        
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteLeftRookMoved = false;
        blackLeftRookMoved = false;
        whiteRightRookMoved = false;
        blackRightRookMoved = false;
        
        lastMoveOrigin = -1;
        lastMoveTarget = -1;
        
        // Recalculate material
        calculateMaterialFromBoard();
        
        // Refresh moves
        refreshMoves();
        
        System.out.println("Game restarted!");
        addEvent("Game restarted.");
    }
    
    // ========== GETTER METHODS ==========
    
    public boolean isGameOver() {
        return gameOver;
    }
    
    public String getWinner() {
        return winner;
    }
    
    public boolean isDrawOffer() {
        return drawOffer;
    }
    
    public String getDrawOfferedBy() {
        return drawOfferedBy;
    }
    
    public int getWhiteMaterial() {
        return whiteMaterial;
    }
    
    public int getBlackMaterial() {
        return blackMaterial;
    }
    
    public int getMaterialBalance() {
        return whiteMaterial - blackMaterial;
    }
    
    public String getTurn() {
        return (mainBoard.turnMask == Methods.WHITE_MASK) ? "white" : "black";
    }
}