package org.Chess;

import java.util.*;

public class ChessMainLoop {
    public Chessboard mainBoard;
    public HashMap<Coordinate, ArrayList<Coordinate>> currentMovesBuffer;
    public Coordinate movePiece;
    public Coordinate moveTarget;
    public boolean check;
    public boolean checkMate;
    public boolean staleMate;
    public boolean gameRunning;

    public boolean chessMainLoop()
    {
        boolean validMove = false;
        int nonValidMoves = 0;
        while (!validMove) {
            // get the move from client
            movePiece = new Coordinate(-1, -1); // temp, change to real read
            moveTarget = new Coordinate(0, 0); //
            if (currentMovesBuffer.containsKey(movePiece)) {
                if (currentMovesBuffer.get(movePiece).contains(moveTarget)) {
                    validMove = true;
                }
            }
            if (!validMove) nonValidMoves++;
            // if (nonValidMoves > some amount) end game
            // if (!validMove), get return an error to client and get a new move
        }
        mainBoard.move(movePiece, moveTarget); // move in chessboard
        if (mainBoard.checkPromotion(moveTarget)) mainBoard.promote(moveTarget);
        mainBoard.changeTurn();
        check = mainBoard.check(mainBoard.findKing(mainBoard.turn), new King(mainBoard.turn)); // check if king is in check
        currentMovesBuffer = mainBoard.getColorMoves(mainBoard.turn);
        if (check && currentMovesBuffer.isEmpty())
        {
            checkMate = true;
            return false;
        }
        else if (!check && currentMovesBuffer.isEmpty())
        {
            staleMate = true;
            return false;
        }
        return true;
    }

    public void startGame()
    {
        mainBoard = new Chessboard();
        currentMovesBuffer = mainBoard.getColorMoves("white");
        gameRunning = true;
        // code to send pre-existing moves to white;
    }

    public void gameEnd()
    {
        // code to check if it was staleMate or checkMate
        // and to start a new game
    }

    public void mainLoop()
    {
        startGame();
        while (gameRunning)
        {
            gameRunning = chessMainLoop();
        }
        gameEnd();
    }
}
