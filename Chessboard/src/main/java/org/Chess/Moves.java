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
        int[] movesBuffer = new int[4]; // movesBuffer.length -> all possible moves
        int movesBufferIdx = 0; // track index to insert into array
        int piece = board[pawnIdx]; // int value of piece
        boolean pawnWhite = (piece & Methods.COLOR_MASK) == Methods.WHITE_MASK; // check color
        int moveDir = pawnWhite ? 1 : -1;                        // and determine what way it goes
        int file = pawnIdx % 8;

        int oneStep = pawnIdx + (moveDir * 8); // 1 square ahead
        if (oneStep >= 0 && oneStep < 64 && board[oneStep] == 0) {
            movesBuffer[movesBufferIdx++] = oneStep;

            boolean startPos = (pawnWhite && pawnIdx >= 8 && pawnIdx <= 15) || // check if pawn hasnt moved from starting square
                    (!pawnWhite && pawnIdx >= 48 && pawnIdx <= 55);
            int twoStep = pawnIdx + (moveDir * 16);
            if (startPos && board[twoStep] == 0) { // if true -> pawn can move 2 squares ahead
                movesBuffer[movesBufferIdx++] = twoStep;
            }
        }

        for (int offset : PAWN_CAPTURE) { // capture move logic
            int targetIdx = pawnIdx + (moveDir * offset);
            if (targetIdx >= 0 && targetIdx < 64) {
                if (Math.abs((targetIdx % 8) - file) == 1) {
                    int targetPiece = board[targetIdx];
                    if (targetPiece != 0 &&
                            (targetPiece & Methods.COLOR_MASK) != (piece & Methods.COLOR_MASK)) { // check if opp exists in square
                        movesBuffer[movesBufferIdx++] = targetIdx;
                    }
                }
            }
        }

        int epSquare = Methods.enPassant(board, lastMoveOriginIdx, lastMoveTargetIdx); // check for en passant square
        if (epSquare != -1) {
            for (int offset : PAWN_CAPTURE) {
                int targetIdx = pawnIdx + (moveDir * offset);
                if (targetIdx == epSquare && Math.abs((targetIdx % 8) - file) == 1) {
                    movesBuffer[movesBufferIdx++] = targetIdx;
                }
            }
        }
        return finishMoves(movesBuffer, movesBufferIdx); // return an array of correct size
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
        return finishMoves(movesBuffer, movesBufferIdx);
    }

    public static int[] BishopMoves(int[] board, int bishopIdx) {
        int[] movesBuffer = new int[13];
        int movesBufferIdx = 0;
        int piece = board[bishopIdx];
        movesBufferIdx = slide(board, bishopIdx, piece, BISHOP_DIRS, 13, movesBuffer, movesBufferIdx);
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
        int[] movesBuffer = new int[10];
        int movesBufferIdx = 0;
        int piece = board[kingIdx];
        int startFile = kingIdx % 8;
        for (int offset : KING_OFFSETS) {
            int target = kingIdx + offset;
            if (target >= 0 && target < 64 && Math.abs((target % 8) - startFile) <= 1) {
                if (board[target] == 0 || (board[target] & Methods.COLOR_MASK) != (piece & Methods.COLOR_MASK)) {
                    movesBuffer[movesBufferIdx++] = target;
                }
            }
        }

        if (!kingMoved && !Methods.isSquareAttacked(board, kingIdx, colorMask)) { // castling
            int rank = (kingIdx / 8) * 8;
            if (!rightRookMoved && board[rank + 5] == 0 && board[rank + 6] == 0) { // kingside castle
                if (!Methods.isSquareAttacked(board, rank + 5, colorMask) &&
                        !Methods.isSquareAttacked(board, rank + 6, colorMask)) {
                    movesBuffer[movesBufferIdx++] = rank + 6;
                }
            }
            if (!leftRookMoved && board[rank + 1] == 0 && board[rank + 2] == 0 && board[rank + 3] == 0) { // quuenside castle
                if (!Methods.isSquareAttacked(board, rank + 2, colorMask) &&
                        !Methods.isSquareAttacked(board, rank + 3, colorMask)) {
                    movesBuffer[movesBufferIdx++] = rank + 2;
                }
            }
        }
        return finishMoves(movesBuffer, movesBufferIdx);
    }


    public static int move(int[] board, int pieceIdx, int targetIdx) {
        int piece = board[pieceIdx];
        int capturedPiece = board[targetIdx];

        if ((piece & Methods.TYPE_MASK) == Methods.PAWN) {
            int fileDiff = Math.abs((targetIdx % 8) - (pieceIdx % 8));
            if (fileDiff != 0 && capturedPiece == 0) { // Moving diagonally to an empty square
                int victimIdx = (targetIdx / 8 == 5) ? targetIdx - 8 : targetIdx + 8;
                capturedPiece = board[victimIdx]; // Capture the pawn behind
                board[victimIdx] = 0;             // Remove it from the board!
            }
        }

        if ((piece & Methods.TYPE_MASK) == Methods.KING && Math.abs(targetIdx - pieceIdx) == 2) { // castling
            int rank = (pieceIdx / 8) * 8;
            if (targetIdx == rank + 6) { // kingside
                board[rank + 5] = board[rank + 7];
                board[rank + 7] = 0;
            } else if (targetIdx == rank + 2) { // queenside
                board[rank + 3] = board[rank];
                board[rank] = 0;
            }
        }
        board[targetIdx] = piece;
        board[pieceIdx] = 0;
        System.out.println("move run");
        return capturedPiece;
    }

    public static void undoMove(int[] board, int pieceIdx, int targetIdx, int capturedPiece) { // undo the move
        int piece = board[targetIdx];
        if ((piece & Methods.TYPE_MASK) == Methods.KING && Math.abs(targetIdx - pieceIdx) == 2) {
            int rank = (pieceIdx / 8) * 8;
            if (targetIdx == rank + 6) { // undo kingside
                board[rank + 7] = board[rank + 5];
                board[rank + 5] = 0;
            } else if (targetIdx == rank + 2) { // undo queenside
                board[rank] = board[rank + 3];
                board[rank + 3] = 0;
            }
        }
        board[pieceIdx] = piece;
        board[targetIdx] = 0; // put empty in square
        if ((piece & Methods.TYPE_MASK) == Methods.PAWN && capturedPiece != 0) { // en passant edgecase
            int fileDiff = Math.abs((targetIdx % 8) - (pieceIdx % 8));
            if (fileDiff != 0) {
                int victimIdx = (targetIdx / 8 == 5) ? targetIdx - 8 : targetIdx + 8;
                board[victimIdx] = capturedPiece;
                System.out.println("undoMove run");
                return;
            }
        }
        board[targetIdx] = capturedPiece; // normal capture
    }

    public static boolean checkAfterMove(int[] board, int pieceIdx, int targetIdx, int colorMask) { // check if move puts own king in check
        int target = move(board, pieceIdx, targetIdx);
        int kingIdx = Methods.findKing(board, colorMask); // find king since its possible king may have moved place
        boolean check = Methods.inCheck(board, kingIdx);
        undoMove(board, pieceIdx, targetIdx, target);
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
            System.out.println("allColorMoves run");
            if (possibleMoves != null) moves.put(i, possibleMoves);
        }
        return moves;
    }

    public static HashMap<Integer, int[]> validateAllColorMoves(int[] board, HashMap<Integer, int[]> moves, int colorMask) { // return a new hashmap of valid moves
        HashMap<Integer, int[]> validMoves = new HashMap<>();
        for (int pieceIdx : moves.keySet()) { // validate all moves for piece
            if (moves.get(pieceIdx).length == 1 && moves.get(pieceIdx)[0] == -1) continue;
            int[] moveBuffer = new int[moves.get(pieceIdx).length];
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