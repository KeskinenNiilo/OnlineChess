package org.Chess;

import java.util.Objects;

public class Coordinate
{
    public int x;
    public int y;
    Coordinate(int xI, int yI)
    {
       this.x = xI;
       this.y = yI;
    }
   @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return x == that.x && y == that.y;
    }
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
    @Override
    public String toString()
    {
        return "(" + x + "," + y + ")";
    }   
    public String toChessString()
    {
        if (x < 0 || x > 7 || y < 0 || y > 7) return "Coordinate out of bounds";
        char[] xChar = {'A','B','C','D','E','F','G','H'};
        return xChar[x] + "" + (y + 1);
    }
}
