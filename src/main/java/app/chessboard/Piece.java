package app.chessboard;

import java.util.ArrayList;

public abstract class Piece
{
    public String color;
    public abstract ArrayList<Coordinate> getMoves(Piece[][] board, Coordinate pieCoordinate);
    public abstract Piece copy();
}
