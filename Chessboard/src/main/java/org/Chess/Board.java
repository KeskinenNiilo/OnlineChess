package org.Chess;

import java.util.HashMap;

public class Board {
    public int[] boardState;
    public int turnMask;
    public int[] unmovedPieces;
    Board() {
        int[] backRank = {Methods.ROOK, Methods.KNIGHT, Methods.BISHOP, Methods.KING, Methods.QUEEN, Methods.BISHOP, Methods.KNIGHT, Methods.ROOK};
        int[] board = new int[64];
        for (int i = 0; i < 8; ++i) {
            board[i] = backRank[i];
            board[i + 8] = Methods.PAWN;
            board[i + 56] = backRank[i] | Methods.BLACK_MASK;
            board[i + 48] = Methods.PAWN | Methods.BLACK_MASK;
        }
        boardState = board;
        turnMask = Methods.WHITE_MASK;
        unmovedPieces = new int[]{0, 3, 7, 56, 59, 63};
    }

    public void changeTurnMask() {
        turnMask = turnMask ^ Methods.COLOR_MASK;
    }
}
