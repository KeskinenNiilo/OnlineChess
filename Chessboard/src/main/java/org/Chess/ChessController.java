package org.Chess;

import java.lang.reflect.Method;
import org.Chess.*;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows JS to talk to Java
public class ChessController {

    // RoomCode -> {"board": Chessboard, "white:": Boolean, "black": Boolean}
    private Map<String, MainLoopServer> rooms = new HashMap<>();
    private final Bucket bucket = Bucket.builder()
        .addLimit(Bandwidth.classic(300, Refill.intervally(300, Duration.ofMinutes(1))))
        .build();

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createRoom() {
        if(!bucket.tryConsume(1)) {
            System.out.println("Someone had too many requests (create)");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        String code = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        
        MainLoopServer game = new MainLoopServer();
        game.whiteJoined = true;
        rooms.put(code, game);

        System.out.println("room created: "+ code);
        Map<String, String> response = new HashMap<>();
        response.put("room", code);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState(@RequestParam String room) {
        if(bucket.tryConsume(1)) {
            MainLoopServer game = rooms.get(room);
            if (game == null) throw new RuntimeException("Room not found.");

            String[][] jsBoard = new String[8][8];
            for(int i = 0; i < 64; i++) {
                // 1D index -> JS [row][col]
                jsBoard[7 - (i/8)][i % 8] = game.getPieceString(game.mainBoard.boardState[i]);
            }

            Map<String, Object> state = new HashMap<>();
            state.put("board", jsBoard);
            state.put("turn", (game.mainBoard.turnMask == Methods.WHITE_MASK ? "white":"black"));
            state.put("checkMate", game.checkMate);
            return ResponseEntity.ok(state);
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build(); // error 429
        }
    }

    @PostMapping("/move")
    public Map<String, String> executeMove(@RequestBody MoveRequest request) {
        MainLoopServer game = rooms.get(request.room);
        if (game == null) return Map.of("status", "error");

        int fromIdx = (7 - request.from[0]) * 8 + request.from[1];
        int toIdx = (7 - request.to[0]) * 8 + request.to[1];

        boolean success = game.handleMove(fromIdx, toIdx);

        if (success) {
            return Map.of("status", "success");
        }else{
            return Map.of("status", "invalid");
        }
    }

    @GetMapping("/moves")
    public List<int[]> getValidMoves(@RequestParam String room, @RequestParam int x, @RequestParam int y) {
        MainLoopServer game = rooms.get(room);
        if(game == null) throw new RuntimeException("Room not found");
        
        int engineRow = 7 - x;
        int originIdx = engineRow * 8 + y;

        List<int[]> jsMoves = new ArrayList<>();
        if (game.validMovesBuffer.containsKey(originIdx)) {
            for (int targetIdx : game.validMovesBuffer.get(originIdx)) {

                int targetEngineRow = targetIdx / 8;
                int targetCol = targetIdx % 8;
                // Engine 1D -> JS [row, col]

                int targetJsRow = 7 - targetEngineRow;

                jsMoves.add(new int[]{targetJsRow, targetCol});
            }
        }
        return jsMoves;
    }
   
    @PostMapping("/join")
    public ResponseEntity<Map<String, String>> joinRoom(@RequestParam String room) {
        if(!bucket.tryConsume(1)) {
            System.out.println("Someone had too many requests (join)");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        MainLoopServer game = rooms.get(room);
        if (game == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("Status", "Error", "message", "Room doesn't exist"));
        }
        
        if (!game.whiteJoined) {
            game.whiteJoined = true;
            System.out.println("White player joined in room: " + room);
            return ResponseEntity.ok(Map.of("status", "success", "side", "white"));
            
        } else if (!game.blackJoined) {
            game.blackJoined = true;
            System.out.println("Black player joined in room: " + room);
            return ResponseEntity.ok(Map.of("status", "success", "side", "black"));
        }

        System.out.println("Someone tried to join a full room: "+ room);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("status", "error", "message", "Room is full"));
    }

    @PostMapping("/leave")
    public Map<String, String> leaveRoom(@RequestParam String room, @RequestParam String side) {
       MainLoopServer game = rooms.get(room);
        if (game == null) return Map.of("status", "error");
        
        // remove the player who left from the room
        if ("white".equalsIgnoreCase(side)) game.whiteJoined = false;
        if ("black".equalsIgnoreCase(side)) game.blackJoined = false;
        System.out.println(side + " left room: " + room);

        // If both players have left, delete the room.
        if (!game.whiteJoined && !game.blackJoined){
            rooms.remove(room); 
            System.out.println("room "+ room + " deleted (empty)");
            return Map.of("status", "room_closed");
        }

        return Map.of("status", "player_left");
    }
}

// Simple DTO for the JSON body
class MoveRequest {
    public String room;
    public int[] from;
    public int[] to;
}