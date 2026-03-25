package org.Chess;

import java.lang.reflect.Method;
import java.util.HashMap;

public class Board {
    public int[] boardState; // keep track of current board
    public int turnMask; // keep track of current move turn (black / white)
    Board() { // create the startin board
        int[] backRank = {Methods.ROOK, Methods.KNIGHT, Methods.BISHOP, Methods.QUEEN, Methods.KING, Methods.BISHOP, Methods.KNIGHT, Methods.ROOK};
        int[] board = new int[64];
        for (int i = 0; i < 8; ++i) {
            board[i] = backRank[i];
            board[i + 8] = Methods.PAWN;
            board[i + 56] = backRank[i] | Methods.BLACK_MASK;
            board[i + 48] = Methods.PAWN | Methods.BLACK_MASK;
        }
        boardState = board; // assign the board as the main board state
        turnMask = Methods.WHITE_MASK; // assign first move to white
    }

    public void changeTurnMask() {
        turnMask = turnMask ^ Methods.COLOR_MASK;
    } // change the turn mask with bit ops. example:
}                                                                              // b10000000(black) XOR b100000000(color mask) -> b00000000(white) and reverse etc
