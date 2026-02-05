package logic;

import app.chessboard.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows your JS to talk to Java
public class ChessController {

    // Simple in-memory storage like your 'rooms' dict in Python
    private Map<String, Chessboard> rooms = new HashMap<>();

    @PostMapping("/create")
    public Map<String, String> createRoom() {
        String code = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        rooms.put(code, new Chessboard());
        
        Map<String, String> response = new HashMap<>();
        response.put("room", code);
        return response;
    }

    @GetMapping("/state")
    public Map<String, Object> getState(@RequestParam String room) {
        Chessboard board = rooms.get(room);
        if (board == null) throw new RuntimeException("Room not found");

        // The Fix: Transpose the Java [x][y] board into a JS [row][col] board
        String[][] javaBoard = board.printBoard(); // This is [x][y]
        String[][] jsBoard = new String[8][8];    // We want [row][col]

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                // In Java, y=0 is white's backrank. In JS, row 7 is the bottom.
                // We map Java Y to JS Row, but inverted so y=0 is row 7
                jsBoard[7 - y][x] = javaBoard[x][y];
            }
        }

        Map<String, Object> state = new HashMap<>();
        state.put("board", jsBoard);
        state.put("turn", board.turn);
        return state;
    }

    @PostMapping("/move")
    public Map<String, String> executeMove(@RequestBody MoveRequest request) {
        Chessboard board = rooms.get(request.room);
        if (board == null) return Map.of("status", "error");

        // Translate JS [row, col] to Java [x, y]
        Coordinate from = new Coordinate(request.from[1], 7 - request.from[0]);
        Coordinate to = new Coordinate(request.to[1], 7 - request.to[0]);
        
        board.move(from, to);
        board.turn = board.turn.equals("white") ? "black" : "white";

        return Map.of("status", "success");
    }

    @GetMapping("/moves")
    public List<int[]> getValidMoves(@RequestParam String room, @RequestParam int x, @RequestParam int y) {
        Chessboard b = rooms.get(room);
        if (b == null) return new ArrayList<>();

        // Convert JS Row/Col back to Java X/Y
        int javaX = y;      // JS Column is Java X
        int javaY = 7 - x;  // JS Row (inverted) is Java Y

        System.out.println("DEBUG: Clicked JS Row: " + x + " Col: " + y);
        System.out.println("DEBUG: Translated to Java X: " + javaX + " Y: " + javaY);

        if (b.board[javaX][javaY] == null) {
            System.out.println("DEBUG: No piece found at Java " + javaX + "," + javaY);
            return new ArrayList<>();
        }

        String pieceType = b.board[javaX][javaY].getClass().getSimpleName();
        String pieceColor = b.board[javaX][javaY].color;
        System.out.println("DEBUG: Found " + pieceColor + " " + pieceType);

        List<Coordinate> moves = b.board[javaX][javaY].getMoves(b.board, new Coordinate(javaX, javaY));
        
        // Convert Java moves back to JS [row, col] for highlighting
        List<int[]> jsMoves = new ArrayList<>();
        for (Coordinate m : moves) {
            jsMoves.add(new int[]{7 - m.y, m.x}); 
        }
        return jsMoves;
    }
   
    @PostMapping("/join")
    public Map<String, String> joinRoom(@RequestParam String room) {
        if (!rooms.containsKey(room)) return Map.of("status", "error");
        return Map.of("status", "success", "side", "black");
    }

    @PostMapping("/leave")
    public Map<String, String> leaveRoom(@RequestParam String room, @RequestParam String side) {
        // Basic logic: If someone leaves, you could delete the room
        // or just log it for now.
        rooms.remove(room); 
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "room_closed");
        return response;
    }
}

// Simple DTO for the JSON body
class MoveRequest {
    public String room;
    public int[] from;
    public int[] to;
}