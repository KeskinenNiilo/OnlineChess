package app.chessboard;

import java.util.ArrayList;

public class Pawn extends Piece
{
    public Pawn(String colorI)
    {
        this.color = colorI;
    }
    @Override
    public ArrayList<Coordinate> getMoves(Piece[][] board, Coordinate pieceCoordinate)
    {
        boolean startPos = ((pieceCoordinate.y == 1 && color.equals("white")) || (pieceCoordinate.y == 6 && color.equals("black")));
        ArrayList<Coordinate> moves = new ArrayList<>();
        if (color.equals("white"))
        {
            if (pieceCoordinate.y + 1 < 8 && board[pieceCoordinate.x][pieceCoordinate.y + 1] == null)
            {
                moves.add(new Coordinate(pieceCoordinate.x, pieceCoordinate.y + 1));
                if (startPos && board[pieceCoordinate.x][pieceCoordinate.y + 2] == null)
                    moves.add(new Coordinate(pieceCoordinate.x, pieceCoordinate.y + 2));
            }
        }
        else
        {
            if (pieceCoordinate.y -1 >= 0 && board[pieceCoordinate.x][pieceCoordinate.y - 1] == null)
            {
                moves.add(new Coordinate(pieceCoordinate.x, pieceCoordinate.y  - 1));
                if (startPos && board[pieceCoordinate.x][pieceCoordinate.y - 2] == null)
                    moves.add(new Coordinate(pieceCoordinate.x, pieceCoordinate.y - 2));
            }
        }
        Coordinate[] eat = new Coordinate[2];
        eat[0] = (color.equals("white")) ? new Coordinate(-1, 1) : new Coordinate(-1, -1);
        eat[1] = (color.equals("white")) ? new Coordinate(1, 1) : new Coordinate(1, -1);
        for (Coordinate offset : eat)
        {
            int x = pieceCoordinate.x + offset.x;
            int y = pieceCoordinate.y + offset.y;
            if (x >= 0 && x < 8 && y >= 0 && y < 8) 
            {
                if (board[x][y] != null && !board[x][y].color.equals(color))
                {
                    moves.add(new Coordinate(x, y));
                }
            }
        }
        return moves;
    }   
    @Override
    public Pawn copy()
    {
        Pawn copied = new Pawn(this.color);
        return copied;
    }
}
