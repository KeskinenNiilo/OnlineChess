package org.Chess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Chessboard
{
    public Piece[][] board;
    public String turn;
    public boolean check;
    public boolean checkMate;
    public boolean staleMate;
    public ArrayList<Piece> blackCaptured;
    public ArrayList<Piece> whiteCaptured;
    public boolean error;
    public HashMap<Coordinate, Coordinate> moveHistory;
    public Chessboard()
    {
        board = new Piece[8][8];
        for (int x = 0; x < 8; ++x)
        {
            board[x][1] = new Pawn("white");
            board[x][6] = new Pawn("black");
        }
        String[] colors = {"white", "black"};
        int[] ranks = {0, 7};

        for (int i = 0; i < 2; i++) {
            int y = ranks[i];
            String c = colors[i];
            board[0][y] = new Rook(c);
            board[1][y] = new Knight(c);
            board[2][y] = new Bishop(c);
            board[3][y] = new Queen(c);
            board[4][y] = new King(c);
            board[5][y] = new Bishop(c);
            board[6][y] = new Knight(c);
            board[7][y] = new Rook(c);
        }
        
        turn = "white";
        check = false;
        checkMate = false;
        staleMate = false;
        whiteCaptured = new ArrayList<>();
        blackCaptured = new ArrayList<>();
        moveHistory = new HashMap<>();
        error = false;
    }
    
    public boolean check(Coordinate kingC, King kingP) { // check if given king is in check
        int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}, {1,0}, {-1,0}, {0,1}, {0,-1}};

        for (int i = 0; i < 8; i++) {
            for (int j = 1; j < 8; j++) {
                int x = kingC.x + (directions[i][0] * j);
                int y = kingC.y + (directions[i][1] * j);

                if (x < 0 || x >= 8 || y < 0 || y >= 8) break; 

                if (board[x][y] != null) {
                    if (!board[x][y].color.equals(kingP.color)) {
                        Piece type = board[x][y];
                        if (j == 1 && type instanceof King) return true;
                        if (i < 4) {
                            if (type instanceof Bishop || type instanceof Queen) return true;
                        } else {
                            if (type instanceof Rook || type instanceof Queen) return true;
                        }
                    }
                    break; 
                }
            }
        }

        int[][] knightMoves = {{2,1}, {2,-1}, {-2,1}, {-2,-1}, {1,2}, {1,-2}, {-1,2}, {-1,-2}};
        for (int[] m : knightMoves) {
            int x = kingC.x + m[0];
            int y = kingC.y + m[1];
            if (x >= 0 && x < 8 && y >= 0 && y < 8) {
                Piece p = board[x][y];
                if (p != null && p instanceof Knight && !p.color.equals(kingP.color)) return true;
            }
        }

        int pawnY = kingP.color.equals("white") ? 1 : -1; 
        int[] pawnX = {1, -1};
        for (int dx : pawnX) {
            int x = kingC.x + dx;
            int y = kingC.y + pawnY;
            if (x >= 0 && x < 8 && y >= 0 && y < 8) {
                Piece p = board[x][y];
                if (p != null && p instanceof Pawn && !p.color.equals(kingP.color)) return true;
            }
        }

        return false;
    }
    
    public HashMap<Coordinate, ArrayList<Coordinate>> getColorMoves(String pieceColor) // return hashmap of moves by color
    {
        HashMap<Coordinate, ArrayList<Coordinate>> moveMap = new HashMap<>();
        for (int x = 0; x < 8; ++x)
        {
            for (int y = 0; y < 8; ++y)
            {
                Piece p = board[x][y];
                if (p != null && p.color.equals(pieceColor))
                {
                    Coordinate curr = new Coordinate(x, y);
                    moveMap.put(curr, p.getMoves(board, curr));
                }
            }
        }
        return moveMap;
    }
    
    public void move(Coordinate piece, Coordinate target) { // hard move, only use for final moves
        if (board[target.x][target.y] != null)
        {
            if (board[target.x][target.y].color.equals("white")) blackCaptured.add(board[target.x][target.y]);
            else whiteCaptured.add(board[target.x][target.y]);
        }
        board[target.x][target.y] = board[piece.x][piece.y];
        board[piece.x][piece.y] = null;
        if (checkPromotion(target)) promote(target);
    }
    
    public Piece virtualMove(Coordinate piece, Coordinate target) { // helper functions for move checking
        Piece captured = board[target.x][target.y];
        board[target.x][target.y] = board[piece.x][piece.y];
        board[piece.x][piece.y] = null;
        return captured;
    }

    public void virtualUndoMove(Coordinate original, Coordinate target, Piece captured) {
        board[original.x][original.y] = board[target.x][target.y];
        board[target.x][target.y] = captured;
    }
    
    public boolean checkPromotion(Coordinate piece) { // check promotion and promote to quuen (temporary awaiting UI logic)
        if (board[piece.x][piece.y] == null) return false;
        Piece p = board[piece.x][piece.y];
        if (!(p instanceof Pawn)) return false;
        int promotionY = (p.color.equals("white")) ? 7 : 0;
        return (piece.y == promotionY);
    }
    
    public void promote(Coordinate pawn) 
    {
        board[pawn.x][pawn.y] = new Queen(board[pawn.x][pawn.y].color);
    }
    
    public Chessboard copyChessboard() // deep copy of chessboard for checking checks
    {
        Chessboard copied = new Chessboard();
        Piece[][] copiedBoard = new Piece[8][8];
        for (int i = 0; i < 8; ++i)
        {
            for (int j = 0; j < 8; ++j)
            {
                if (board[i][j] != null)
                {
                    copiedBoard[i][j] = board[i][j].copy();
                }
            }
        }
        copied.board = copiedBoard;
        copied.blackCaptured = blackCaptured;
        copied.whiteCaptured = whiteCaptured;
        copied.check = check;
        copied.checkMate = checkMate;
        copied.staleMate = staleMate;
        copied.turn = turn;
        return copied;
    }
    
    public Coordinate findKing(String kingColor) //find king in array
    {
        for (int x = 0; x < 8; ++x)
        {
            for (int y = 0; y < 8; ++y)
            {
                if (board[x][y] instanceof King && board[x][y].color.equals(kingColor))
                    return new Coordinate(x, y);
            }
        }
        return new Coordinate(-1, -1);
    }
    
    public HashMap<Coordinate, ArrayList<Coordinate>> validateAllMoves(HashMap<Coordinate, ArrayList<Coordinate>> moves, String pieceColor)
    { // validate the hashmap of moves
        HashMap<Coordinate, ArrayList<Coordinate>> validMoves = new HashMap<>();
        Set<Coordinate> pieces = moves.keySet();
        Chessboard virtual = copyChessboard();
        for (Coordinate p : pieces)
        {
            ArrayList<Coordinate> tempValidMoves = new ArrayList<>();
            for (Coordinate m : moves.get(p))
            {   
                Piece captured = virtual.virtualMove(p, m);
                Coordinate kingCoord = virtual.findKing(pieceColor);
                King k = (King) virtual.board[kingCoord.x][kingCoord.y];
                if (!virtual.check(kingCoord, k)) tempValidMoves.add(m);
                virtualUndoMove(p, m, captured);
            }
            if (!tempValidMoves.isEmpty())
            {
                validMoves.put(p, tempValidMoves);
            }
        }
        return validMoves;
    }
    
    public String[][] printBoard()
    { // return 8x8 array of strings in form of black_pawn, white_quuen etc.
        String[][] tempString = new String[8][8];
        for (int x = 0; x < 8; ++x)
        {
            for (int y = 0; y < 8; ++y)
            {
                Piece p = board[x][y];    
                if (p == null) {
                    tempString[x][y] = "empty";
                    continue;
                }
                tempString[x][y] = switch (p) {
                    case King k   -> k.color + "_king";
                    case Queen q  -> q.color + "_queen";
                    case Rook r   -> r.color + "_rook";
                    case Bishop b -> b.color + "_bishop";
                    case Knight n -> n.color + "_knight";
                    case Pawn pw  -> pw.color + "_pawn";
                    default       -> "empty";
                };
            }
        }
        return tempString;
    }
}

