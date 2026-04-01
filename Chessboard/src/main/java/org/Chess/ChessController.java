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
        game.addEvent("-- Room created --");
        Map<String, String> response = new HashMap<>();
        response.put("room", code);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState(@RequestParam String room) {
        if(bucket.tryConsume(1)) {
            MainLoopServer game = rooms.get(room);
            if (game == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

            String[][] jsBoard = new String[8][8];
            for(int i = 0; i < 64; i++) {
                // 1D index -> JS [row][col]
                jsBoard[7 - (i/8)][i % 8] = game.getPieceString(game.mainBoard.boardState[i]);
            }

            Map<String, Object> state = new HashMap<>();
            state.put("board", jsBoard);
            state.put("turn", (game.mainBoard.turnMask == Methods.WHITE_MASK ? "white":"black"));
            state.put("checkMate", game.checkMate);
            state.put("staleMate", game.staleMate);

            // event log
            state.put("events", game.eventLog);
            
            // ========== ADD THESE NEW FIELDS ==========
            state.put("gameOver", game.isGameOver());
            state.put("winner", game.getWinner());
            state.put("drawOffer", game.isDrawOffer());
            state.put("drawOfferedBy", game.getDrawOfferedBy());
            state.put("whiteMaterial", game.getWhiteMaterial());
            state.put("blackMaterial", game.getBlackMaterial());
            
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
            System.out.println("Move successful. Room: " + request.room + " Checkmate status: " + game.checkMate);
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
            game.addEvent("White player joined.");
            return ResponseEntity.ok(Map.of("status", "success", "side", "white"));
            
        } else if (!game.blackJoined) {
            game.blackJoined = true;
            System.out.println("Black player joined in room: " + room);
            game.addEvent("Black player joined.");
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
        game.addEvent(side + " left.");

        // If both players have left, delete the room.
        if (!game.whiteJoined && !game.blackJoined){
            rooms.remove(room); 
            System.out.println("room "+ room + " deleted (empty)");
            return Map.of("status", "room_closed");
        }

        return Map.of("status", "player_left");
    }
    
    // ========== DRAW ENDPOINTS ==========
    
    @PostMapping("/draw")
    public ResponseEntity<Map<String, String>> offerDraw(@RequestBody Map<String, String> request) {
        if(!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        String room = request.get("room");
        String side = request.get("side");
        
        MainLoopServer game = rooms.get(room);
        if (game == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", "Room not found"));
        }
        
        boolean success = game.offerDraw(side);
        
        if (success) {
            System.out.println(side + " offered a draw in room: " + room);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Draw offer sent"));
        } else {
            return ResponseEntity.ok(Map.of("status", "error", "message", "Cannot offer draw"));
        }
    }
    
    @PostMapping("/draw-response")
    public ResponseEntity<Map<String, String>> respondToDraw(@RequestBody Map<String, Object> request) {
        if(!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        String room = (String) request.get("room");
        String side = (String) request.get("side");
        boolean accept = (boolean) request.get("accept");
        
        MainLoopServer game = rooms.get(room);
        if (game == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", "Room not found"));
        }
        
        boolean success = game.respondToDraw(side, accept);
        
        if (success) {
            System.out.println(side + (accept ? " accepted" : " declined") + " draw in room: " + room);
            return ResponseEntity.ok(Map.of("status", "success", "message", accept ? "Draw accepted" : "Draw declined"));
        } else {
            return ResponseEntity.ok(Map.of("status", "error", "message", "Cannot respond to draw"));
        }
    }
    
    // ========== FORFEIT ENDPOINT ==========
    
    @PostMapping("/forfeit")
    public ResponseEntity<Map<String, String>> forfeit(@RequestBody Map<String, String> request) {
        if(!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        String room = request.get("room");
        String side = request.get("side");
        
        MainLoopServer game = rooms.get(room);
        if (game == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", "Room not found"));
        }
        
        boolean success = game.forfeit(side);
        
        if (success) {
            System.out.println(side + " forfeited in room: " + room);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Game forfeited"));
        } else {
            return ResponseEntity.ok(Map.of("status", "error", "message", "Cannot forfeit"));
        }
    }
    
    // ========== RESTART ENDPOINT ==========
    
    @PostMapping("/restart")
    public ResponseEntity<Map<String, String>> restartGame(@RequestBody Map<String, String> request) {
        if(!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        String room = request.get("room");
        String side = request.get("side");
        
        MainLoopServer game = rooms.get(room);
        if (game == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", "Room not found"));
        }
        boolean restarted = game.requestRestart(side);

        
        if (restarted) {
            System.out.println("Game restarted in room: " + room);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Game restarted"));
        } else {
            // Add a log event so the other player sees someone is ready
            game.addEvent(side + " wants a rematch."); 
            return ResponseEntity.ok(Map.of("status", "waiting", "message", "Waiting for opponent"));
    }
    }
    
    // ========== MATERIAL ENDPOINT (OPTIONAL) ==========
    
    @GetMapping("/material")
    public ResponseEntity<Map<String, Object>> getMaterial(@RequestParam String room) {
        if(!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        MainLoopServer game = rooms.get(room);
        if (game == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        Map<String, Object> material = new HashMap<>();
        material.put("white", game.getWhiteMaterial());
        material.put("black", game.getBlackMaterial());
        material.put("balance", game.getMaterialBalance());
        
        return ResponseEntity.ok(material);
    }
}

// Simple DTO for the JSON body
class MoveRequest {
    public String room;
    public int[] from;
    public int[] to;
}