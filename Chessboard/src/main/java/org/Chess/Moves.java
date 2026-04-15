package org.Chess;

import java.util.HashMap;

public class Moves {

    // make static arrays for piece movements to reduce assigning variables
    public static final int[] KNIGHT_OFFSETS = {-17, -15, -10, -6, 6, 10, 15, 17};
    public static final int[] BISHOP_DIRS = {7, -7, 9, -9};
    public static final int[] ROOK_DIRS = {8, -8, 1, -1};
    public static final int[] QUEEN_DIRS = {8, -8, 1, -1, 7, -7, 9, -9};
    public static final int[] KING_OFFSETS = {8, -8, 1, -1, 7, -7, 9, -9};
    public static final int[] PAWN_CAPTURE = {7, 9};

    // copy buffer array into the right size of array, return -1 if array is empty
    public static int[] finishMoves(int[] buffer, int count) {
        if (count == 0) return new int[]{-1};
        int[] result = new int[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
    }

    // slide movement method for queen, rook, bishop
    private static int slide(int[] board, int startIdx, int piece, int[] directions, int max,
                             int[] buffer, int bufferIdx) {
        int startFile = startIdx % 8; // check file
        int startRank = startIdx / 8; // and rank integrity

        for (int dir : directions) {
            for (int i = 1; i < 8; i++) { // sliding movement
                int target = startIdx + (dir * i);
                if (target < 0 || target >= 64) break; // check bounds

                int file = target % 8; // target square file
                int rank = target / 8; // and rank
                int fileDist = Math.abs(file - startFile);
                int rankDist = Math.abs(rank - startRank);

                if (!(fileDist == 0 || rankDist == 0 || fileDist == rankDist)) break; // check file and rank integrity

                if (board[target] == 0) { // if square is empty
                    buffer[bufferIdx++] = target;
                } else {
                    if ((board[target] & Methods.COLOR_MASK) != (piece & Methods.COLOR_MASK)) { // check if it is opponent with a color mask
                        buffer[bufferIdx++] = target;
                    }
                    break;
                }
            }
        }
        return bufferIdx;
    }

    // moves for pawn, this is longer because pawn has so many different edge cases
    public static int[] PawnMoves(int[] board, int pawnIdx, int lastMoveOriginIdx, int lastMoveTargetIdx) {
        int[] movesBuffer = new int[5]; // 1 forward, 2 forward, 2 captures, 1 en passant
        int movesBufferIdx = 0;
        int piece = board[pawnIdx];
        boolean pawnWhite = (piece & Methods.COLOR_MASK) == Methods.WHITE_MASK;
        int moveDir = pawnWhite ? 1 : -1;
        int file = pawnIdx % 8;

        int oneStep = pawnIdx + (moveDir * 8);
        if (oneStep >= 0 && oneStep < 64 && board[oneStep] == 0) {
            movesBuffer[movesBufferIdx++] = oneStep;

            boolean startPos = (pawnWhite && pawnIdx >= 8 && pawnIdx <= 15) ||
                    (!pawnWhite && pawnIdx >= 48 && pawnIdx <= 55);
            int twoStep = pawnIdx + (moveDir * 16);
            if (startPos && board[twoStep] == 0) {
                movesBuffer[movesBufferIdx++] = twoStep;
            }
        }

        for (int offset : PAWN_CAPTURE) {
            int targetIdx = pawnIdx + (moveDir * offset);
            if (targetIdx >= 0 && targetIdx < 64) {
                if (Math.abs((targetIdx % 8) - file) == 1) {
                    int targetPiece = board[targetIdx];
                    if (targetPiece != 0 &&
                            (targetPiece & Methods.COLOR_MASK) != (piece & Methods.COLOR_MASK)) {
                        movesBuffer[movesBufferIdx++] = targetIdx;
                    }
                }
            }
        }
        return finishMoves(movesBuffer, movesBufferIdx);
    }

    public static int[] KnightMoves(int[] board, int knightIdx) {
        int[] movesBuffer = new int[8];
        int movesBufferIdx = 0;
        int piece = board[knightIdx];
        int startFile = knightIdx % 8;

        for (int move : KNIGHT_OFFSETS) {
            int target = knightIdx + move;
            if (target >= 0 && target < 64) {
                int fileDiff = Math.abs((target % 8) - startFile);
                if (fileDiff == 1 || fileDiff == 2) { // check file diff, knight moves in L - pattern
                    if (board[target] == 0 ||
                            (board[target] & Methods.COLOR_MASK) != (piece & Methods.COLOR_MASK)) {
                        movesBuffer[movesBufferIdx++] = target;
                    }
                }
            }
        }
        return finishMoves(movesBuffer, movesBufferIdx); // make buffer array the right size
    }

    public static int[] BishopMoves(int[] board, int bishopIdx) {
        int[] movesBuffer = new int[13];
        int movesBufferIdx = 0; // dynamically assign array index
        int piece = board[bishopIdx];
        movesBufferIdx = slide(board, bishopIdx, piece, BISHOP_DIRS, 13, movesBuffer, movesBufferIdx); // sliding check
        return finishMoves(movesBuffer, movesBufferIdx);
    }

    public static int[] RookMoves(int[] board, int rookIdx) {
        int[] movesBuffer = new int[14];
        int movesBufferIdx = 0;
        int piece = board[rookIdx];
        movesBufferIdx = slide(board, rookIdx, piece, ROOK_DIRS, 14, movesBuffer, movesBufferIdx);
        return finishMoves(movesBuffer, movesBufferIdx);
    }

    public static int[] QueenMoves(int[] board, int queenIdx) {
        int[] movesBuffer = new int[27];
        int movesBufferIdx = 0;
        int piece = board[queenIdx];
        movesBufferIdx = slide(board, queenIdx, piece, QUEEN_DIRS, 27, movesBuffer, movesBufferIdx);
        return finishMoves(movesBuffer, movesBufferIdx);
    }

    public static int[] KingMoves(int[] board, int kingIdx, boolean kingMoved,
                                  boolean leftRookMoved, boolean rightRookMoved, int colorMask) {
        int[] movesBuffer = new int[10]; // 8 normal + 2 castling
        int movesBufferIdx = 0;
        int piece = board[kingIdx];
        int startFile = kingIdx % 8;

        // Normal king moves
        for (int offset : KING_OFFSETS) {
            int target = kingIdx + offset;
            if (target >= 0 && target < 64 && Math.abs((target % 8) - startFile) <= 1) {
                if (board[target] == 0 || (board[target] & Methods.COLOR_MASK) != (piece & Methods.COLOR_MASK)) {
                    movesBuffer[movesBufferIdx++] = target;
                }
            }
        }

        // Castling
        if (!kingMoved && !Methods.isSquareAttacked(board, kingIdx, colorMask)) {
            if (!rightRookMoved) { // Kingside
                int f = kingIdx + 1;
                int g = kingIdx + 2;
                int rookSquare = kingIdx + 3;
                if (board[f] == 0 && board[g] == 0
                        && board[rookSquare] != 0
                        && (board[rookSquare] & Methods.TYPE_MASK) == Methods.ROOK
                        && (board[rookSquare] & Methods.COLOR_MASK) == colorMask
                        && !Methods.isSquareAttacked(board, f, colorMask)  // king cannot pass through check
                        && !Methods.isSquareAttacked(board, g, colorMask)) {
                    movesBuffer[movesBufferIdx++] = g; // king castle square
                }
            }

            // Queenside
            if (!leftRookMoved) {
                int d = kingIdx - 1;
                int c = kingIdx - 2;
                int b = kingIdx - 3;
                int rookSquare = kingIdx - 4;
                if (board[d] == 0 && board[c] == 0 && board[b] == 0
                        && board[rookSquare] != 0
                        && (board[rookSquare] & Methods.TYPE_MASK) == Methods.ROOK
                        && (board[rookSquare] & Methods.COLOR_MASK) == colorMask
                        && !Methods.isSquareAttacked(board, d, colorMask)  // king cannot pass through check
                        && !Methods.isSquareAttacked(board, c, colorMask)) {
                    movesBuffer[movesBufferIdx++] = c; // king castle square
                }
            }
        }

        return finishMoves(movesBuffer, movesBufferIdx);
    }


    public static int move(int[] board, int pieceIdx, int targetIdx) {
        int piece = board[pieceIdx];
        int capturedPiece = board[targetIdx];

        board[targetIdx] = piece;
        board[pieceIdx] = 0;
        return capturedPiece; // return captured piece to keep track of move functions
    }

    public static void undoMove(int[] board, int pieceIdx, int targetIdx, int capturedPiece) { // undo the move
        int piece = board[targetIdx];


        board[pieceIdx] = piece;
        board[targetIdx] = 0; // put empty in square

        board[targetIdx] = capturedPiece; // normal capture
    }

    public static boolean checkAfterMove(int[] board, int pieceIdx, int targetIdx, int colorMask) {
        int piece = board[pieceIdx];
        int pieceType = piece & Methods.TYPE_MASK;

        int epCapturedIdx = -1; // detect en passant
        int epCapturedPiece = 0;
        if (pieceType == Methods.PAWN) {
            int fileDiff = Math.abs((targetIdx % 8) - (pieceIdx % 8));
            int rankDiff = Math.abs((targetIdx / 8) - (pieceIdx / 8));
            if (fileDiff == 1 && rankDiff == 1 && board[targetIdx] == 0) {
                epCapturedIdx = pieceIdx + (targetIdx % 8) - (pieceIdx % 8); // the capture
                epCapturedPiece = board[epCapturedIdx];
                board[epCapturedIdx] = 0; // temporarily remove for check detection
            }
        }

        int target = move(board, pieceIdx, targetIdx);
        int kingIdx = Methods.findKing(board, colorMask);
        boolean check = Methods.inCheck(board, kingIdx);
        undoMove(board, pieceIdx, targetIdx, target);

        if (epCapturedIdx >= 0) { // restore en passant
            board[epCapturedIdx] = epCapturedPiece;
        }

        return check;
    }

    public static HashMap<Integer, int[]> allColorMoves(int[] board, int colorMask, int lastMoveOriginIdx, int lastMoveTargetIdx,
                                                        boolean kingMoved, boolean leftRookMoved, boolean rightRookMoved) { // get all possible moves by color
        HashMap<Integer, int[]> moves = new HashMap<>();
        for (int i = 0; i < 64; ++i) {
            int piece = board[i];
            if (piece == 0 || (piece & Methods.COLOR_MASK) != colorMask)
                continue; // check if piece is not 0 and is the right color
            int[] possibleMoves = switch (piece & Methods.TYPE_MASK) {
                case Methods.PAWN -> Moves.PawnMoves(board, i, lastMoveOriginIdx, lastMoveTargetIdx);
                case Methods.KNIGHT -> Moves.KnightMoves(board, i);
                case Methods.ROOK -> Moves.RookMoves(board, i);
                case Methods.BISHOP -> Moves.BishopMoves(board, i);
                case Methods.QUEEN -> Moves.QueenMoves(board, i);
                case Methods.KING -> Moves.KingMoves(board, i, kingMoved, leftRookMoved, rightRookMoved, colorMask);
                default -> null;
            };
            if (possibleMoves != null) moves.put(i, possibleMoves); // if there are possible moves
        }
        return moves;
    }

    public static HashMap<Integer, int[]> validateAllColorMoves(int[] board, HashMap<Integer, int[]> moves, int colorMask) { // return a new hashmap of valid moves
        HashMap<Integer, int[]> validMoves = new HashMap<>(); // new hashmap for valid moves
        for (int pieceIdx : moves.keySet()) { // validate all moves for piece
            if (moves.get(pieceIdx).length == 1 && moves.get(pieceIdx)[0] == -1) continue; // check that we have a valid move to check
            int[] moveBuffer = new int[moves.get(pieceIdx).length]; // create new correct size array
            int movesBufferIdx = 0;
            for (int targetIdx : moves.get(pieceIdx)) {
                if (!checkAfterMove(board, pieceIdx, targetIdx, colorMask)) { // check if king is NOT in check after move -> valid move
                    moveBuffer[movesBufferIdx] = targetIdx;
                    ++movesBufferIdx;
                }
            }
            if (movesBufferIdx > 0) { // if there are valid moves
                validMoves.put(pieceIdx, finishMoves(moveBuffer, movesBufferIdx));
            }
        }
        return validMoves;
    }
}