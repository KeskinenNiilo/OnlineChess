package app.chessboard;

import java.util.ArrayList;

public class Bishop extends Piece
{
    public Bishop(String colorI)
    {
        color = colorI;
    }
    @Override
    public ArrayList<Coordinate> getMoves(Piece[][] board, Coordinate pieceCoordinate)
    {
        ArrayList<Coordinate> moves = new ArrayList<>();
        int[] xMove = {1, -1, 1, -1};
        int[] yMove = {1, 1, -1, -1};
        for (int i = 0; i < 4; ++i)
        {
            for (int j = 1; j < 8; ++j)
            {
                int x = pieceCoordinate.x + (xMove[i] * j);
                int y = pieceCoordinate.y + (yMove[i] * j);
                if (x < 0 || x >= 8 || y < 0 || y >= 8) break;
                if (board[x][y] == null) moves.add(new Coordinate(x, y));
                else
                {
                    if (board[x][y] != null && !board[x][y].color.equals(color)) moves.add(new Coordinate(x, y));
                    break;
                }
            }
        }
        return moves;
    }
    @Override
    public Bishop copy()
    {
        Bishop copied = new Bishop(this.color);
        return copied;
    }
}
