package app.chessboard;

import java.util.ArrayList;

public class Queen extends Piece
{
    public Queen(String colorI) {
        color = colorI;
    }
    @Override
    public ArrayList<Coordinate> getMoves(Piece[][] board, Coordinate pieceCoordinate)
    {
        ArrayList<Coordinate> moves = new ArrayList<>();
        int xMoves[] = {1, 1, -1, -1, 1, -1, 0, 0};
        int yMoves[] = {-1, 1, -1, 1, 0, 0, 1, -1};
        for (int i = 0; i < 8; ++i)
        {
            for (int j = 1; j < 8; ++j)
            {
                int x = pieceCoordinate.x + (xMoves[i] * j);
                int y = pieceCoordinate.y + (yMoves[i] * j);
                if (x < 0 || x >= 8 || y < 0 || y >= 8) break;
                if (board[x][y] == null) moves.add(new Coordinate(x, y));
                else
                {
                    if (!board[x][y].color.equals(color)) moves.add(new Coordinate(x, y));
                    break;
                }
            }
        }
        return moves;
    }
    @Override
    public Queen copy()
    {
        Queen copied = new Queen(this.color);
        return copied;
    }
}
