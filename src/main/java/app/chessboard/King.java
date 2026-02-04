package app.chessboard;

import java.util.*;

public class King extends Piece
{
    public King(String colorI)
    {
        color = colorI;
    }
    @Override
    public ArrayList<Coordinate> getMoves(Piece[][] board, Coordinate pieceCoordinate)
    {
        ArrayList<Coordinate> moves = new ArrayList<>();
        int[] xMove = {1, 1, 1, -1, -1, -1, 0, 0};
        int[] yMove = {1, -1, 0, 1, -1, 0, 1, -1};
        for (int i = 0; i < 8; ++i)
        {
            int x = pieceCoordinate.x + xMove[i];
            int y = pieceCoordinate.y + yMove[i];
            if (x >= 0 && x < 8 && y >= 0 && y < 8) {
                if (board[x][y] == null || !board[x][y].color.equals(color)) moves.add(new Coordinate(x, y));
            }
        }
        return moves;
    }
    @Override
    public King copy()
    {
        King copied = new King(this.color);
        return copied;
    }
}
