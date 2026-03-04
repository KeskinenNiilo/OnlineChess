package org.Chess;

import org.jetbrains.annotations.NotNull;

public class Methods {
    public static final int TYPE_MASK = 0b00000111; // piece bits
    public static final int COLOR_MASK = 0b10000000; // 128 -> if bit, piece black
    public static final int BLACK_MASK = 0b10000000;
    public static final int WHITE_MASK = 0b00000000;
    public static final int PAWN = 1;
    public static final int KNIGHT = 2;
    public static final int ROOK = 3;
    public static final int BISHOP = 4;
    public static final int QUEEN = 5;
    public static final int KING = 6;

    //Material values
    public static final int PAWN_VALUE = 1;
    public static final int KNIGHT_VALUE = 3;
    public static final int BISHOP_VALUE = 3;
    public static final int ROOK_VALUE = 5;
    public static final int QUEEN_VALUE = 9;
    public static final int KING_VALUE = 0;


    //Get Piece value
    public static int getPieceValue(int pieceType) {
        return switch (pieceType) {
            case PAWN -> PAWN_VALUE;
            case KNIGHT -> KNIGHT_VALUE;
            case BISHOP -> BISHOP_VALUE;
            case ROOK -> ROOK_VALUE;
            case QUEEN -> QUEEN_VALUE;
            case KING -> KING_VALUE;
            default -> 0;
        };
    }


    public static boolean inCheck(int[] board, int kingIdx) { // check if in check
        return isSquareAttacked(board, kingIdx, board[kingIdx] & COLOR_MASK);
    }

    public static boolean isSquareAttacked(int[] board, int targetIdx, int colorMask) {
        int targetFile = targetIdx % 8;
        int targetRow = targetIdx / 8;
        int oppMask = (colorMask == WHITE_MASK) ? BLACK_MASK : WHITE_MASK;

        // Knight Threat
        for (int moveDir : Moves.KNIGHT_OFFSETS) { // possible knight -> check
            int threat = targetIdx + moveDir; // place we are checking for a knight
            if (threat >= 0 && threat < 64) { // bounds check
                int threatPiece = board[threat]; // threat piece
                int threatFileDiff = Math.abs((threat % 8) - targetFile); // check that the place check inst wrapping around
                if (threatFileDiff == 1 || threatFileDiff == 2) {
                    if ((threatPiece & COLOR_MASK) == oppMask && (threatPiece & TYPE_MASK) == KNIGHT)
                        return true;
                }
            }
        }

        // Rook Threat and Queen Threat
        for (int moveDir : Moves.ROOK_DIRS) { // possible rook or queen -> check
            for (int step = 1; step < 8; ++step) { // sliding check
                int threat = targetIdx + moveDir * step;
                if (threat < 0 || threat >= 64) break; // bounds check
                int threatPiece = board[threat];
                if (moveDir == -1 || moveDir == 1) { // check which row are checking and check that it isnt wrapping around
                    if (threat / 8 != targetRow) break;
                }
                if (threatPiece != 0) {
                    if ((threatPiece & COLOR_MASK) == oppMask && ((threatPiece & TYPE_MASK) == ROOK || (threatPiece & TYPE_MASK) == QUEEN))
                        return true;
                    break; // if piece in square but not opp or right piece -> next
                }
            }
        }

        // Bishop Threat and Queen Threat
        for (int moveDir : Moves.BISHOP_DIRS) { // possible bishop or queen -> check
            for (int step = 1; step < 8; ++step) {
                int threat = targetIdx + moveDir * step;
                if (threat < 0 || threat >= 64) break; // bounds check
                int threatPiece = board[threat];
                int threatFile = threat % 8;
                int threatRow = threat / 8;
                if (Math.abs(threatFile - targetFile) != step ||
                        Math.abs(threatRow - targetRow) != step)
                    break;
                if (threatPiece != 0) {
                    if ((threatPiece & COLOR_MASK) == oppMask && ((threatPiece & TYPE_MASK) == BISHOP || (threatPiece & TYPE_MASK) == QUEEN))
                        return true;
                    break; // if piece in square but not opp or right piece -> next
                }
            }
        }

        // Pawn Threat
        // if colorMask is WHITE, we look for BLACK pawns "ahead" of us.
        int[] pawnThreat = (oppMask == BLACK_MASK) ? new int[]{7, 9} : new int[]{-9, -7};
        for (int moveDir : pawnThreat) {
            int threat = targetIdx + moveDir;
            if (threat < 0 || threat >= 64) continue; // bounds check
            int threatFile = threat % 8; // must be 1 file over
            if (Math.abs(threatFile - targetFile) != 1) continue;
            int threatPiece = board[threat];
            if (threatPiece != 0 && (threatPiece & COLOR_MASK) == oppMask && (threatPiece & TYPE_MASK) == PAWN)
                return true;
        }

        // King Threat
        for (int moveDir : Moves.KING_OFFSETS) {
            int threat = targetIdx + moveDir;
            if (threat < 0 || threat >= 64) continue; // bounds check
            int threatFile = threat % 8; // must be 0 or 1 file over
            if (Math.abs(threatFile - targetFile) > 1) continue;
            int threatPiece = board[threat];
            if (threatPiece != 0 && (threatPiece & COLOR_MASK) == oppMask && (threatPiece & TYPE_MASK) == KING)
                return true;
        }

        return false; // nothing found
    }

    public static int findKing(int[] board, int colorMask) { // find king
        if (colorMask == COLOR_MASK) { // if king black
            for (int i = 63; i >= 0; --i)
                if ((board[i] & COLOR_MASK) == colorMask && (board[i] & TYPE_MASK) == KING) return i;
        } else {
            for (int i = 0; i < 64; ++i)
                if ((board[i] & COLOR_MASK) == colorMask && (board[i] & TYPE_MASK) == KING) return i;
        }
        return -1;
    }

    public static boolean checkPromotion(int[] board, int targetIdx) { // check promotion from last move
        return ((targetIdx >= 56 && targetIdx < 64 && (board[targetIdx] & TYPE_MASK) == PAWN && (board[targetIdx] & COLOR_MASK) == WHITE_MASK)
                || (targetIdx >= 0 && targetIdx < 8 && (board[targetIdx] & TYPE_MASK) == PAWN && (board[targetIdx] & COLOR_MASK) == BLACK_MASK));
    }

    public static void promote(int[] board, int targetIdx, int promotePiece) { // promote
        if (checkPromotion(board, targetIdx)) board[targetIdx] = (board[targetIdx] & COLOR_MASK) | promotePiece;
    }

    public static int enPassant(int[] board, int lastMoveOriginIdx, int lastMoveTargetIdx) {
        int victimPiece = board[lastMoveTargetIdx];
        if ((victimPiece & TYPE_MASK) != PAWN) return -1;
        if (Math.abs(lastMoveTargetIdx - lastMoveOriginIdx) != 16) return -1;
        return (lastMoveOriginIdx + lastMoveTargetIdx) / 2;
    }
}