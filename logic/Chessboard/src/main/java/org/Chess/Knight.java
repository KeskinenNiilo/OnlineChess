package org.Chess;

import java.util.ArrayList;

public class Knight extends Piece
{
    public Knight(String colorI)
    {
        color = colorI;
    }
    @Override
    public ArrayList<Coordinate> getMoves(Piece[][] board, Coordinate pieceCoordinate)
    {
        ArrayList<Coordinate> moves = new ArrayList<>();
        int[] xMove = {1, 1, -1, -1, 2, 2, -2, -2};
        int[] yMove = {2, -2, 2, -2, 1, -1, 1, -1};
        for (int i = 0; i < 8; ++i)
        {
            int x = pieceCoordinate.x + xMove[i];
            int y = pieceCoordinate.y + yMove[i];
            if ((x >= 0 && x < 8 && y >= 0 && y < 8) && (board[x][y] == null || !board[x][y].color.equals(color)))
               moves.add(new Coordinate(x, y));
        }
        return moves;
    }
    @Override
    public Knight copy()
    {
        Knight copied = new Knight(this.color);
        return copied;
    }
}
