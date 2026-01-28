from flask import Flask, request, jsonify
from flask_cors import CORS
import random
import string

app = Flask(__name__)
CORS(app)

rooms = {}

def get_initial_board():
    return [
        ["♜","♞","♝","♛","♚","♝","♞","♜"],
        ["♟","♟","♟","♟","♟","♟","♟","♟"],
        ["","","","","","","",""],
        ["","","","","","","",""],
        ["","","","","","","",""],
        ["","","","","","","",""],
        ["♙","♙","♙","♙","♙","♙","♙","♙"],
        ["♖","♘","♗","♕","♔","♗","♘","♖"]
    ]

@app.route('/create', methods=['POST'])
def create_room():
    code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=5))
    rooms[code] = {
        "board": get_initial_board(),
        "turn": "white",
        "players": ["white"]
    }
    return jsonify({"room": code})

@app.route('/join', methods=['POST'])
def join_room():
    room_id = request.args.get('room')
    
    if room_id not in rooms:
        return jsonify({"status": "error", "message": "Room not found"}), 404
    
    room = rooms[room_id]
    
    # Check for an empty seat
    if "white" not in room["players"]:
        room["players"].append("white")
        return jsonify({"status": "success", "side": "white"})
    elif "black" not in room["players"]:
        room["players"].append("black")
        return jsonify({"status": "success", "side": "black"})
    else:
        # Both seats are taken
        return jsonify({"status": "error", "message": "Room is full"}), 400

@app.route('/state', methods=['GET'])
def get_state():
    room_id = request.args.get('room')
    if room_id in rooms:
        return jsonify(rooms[room_id])
    return jsonify({"error": "Room not found"}), 404

@app.route('/moves', methods=['GET'])
def get_moves():
    # 1. Get Room, Row, and Col from the URL
    room_id = request.args.get('room')
    row = int(request.args.get('row'))
    col = int(request.args.get('col'))

    # 2. Check if room exists
    if room_id not in rooms:
        return jsonify({"error": "Room not found"}), 404

    # 3. Get the board for THIS specific room
    current_board = rooms[room_id]["board"]
    piece = current_board[row][col]

    # 4. Movement Logic
    white_pieces = ["♙", "♖", "♘", "♗", "♕", "♔"]
    direction = -1 if piece in white_pieces else 1

    valid_moves = [
        [row + direction, col],
        [row + (direction * 2), col]
    ]

    # 5. Boundary check
    valid_moves = [m for m in valid_moves if 0 <= m[0] <= 7]
    
    return jsonify({"moves": valid_moves})

@app.route('/move', methods=['POST'])
def execute_move():
    data = request.json
    room_id = data.get('room')
    
    if room_id in rooms:
        board = rooms[room_id]["board"]
        from_r, from_c = data['from']
        to_r, to_c = data['to']
        
        # Move the piece
        board[to_r][to_c] = board[from_r][from_c]
        board[from_r][from_c] = ""
        
        # Switch turn
        rooms[room_id]["turn"] = "black" if rooms[room_id]["turn"] == "white" else "white"
        return jsonify({"status": "success"})
    
    return jsonify({"status": "error"}), 404

@app.route('/leave', methods=['POST'])
def leave():
    # Use request.args for URL params (from sendBeacon url)
    room_id = request.args.get('room')
    side = request.args.get('side')
    
    if room_id in rooms:
        if side in rooms[room_id]["players"]:
            rooms[room_id]["players"].remove(side)
            print(f"DEBUG: {side} left room {room_id}. Current players: {rooms[room_id]['players']}")
            
        # If the room is empty, delete it
        if not rooms[room_id]["players"]:
            del rooms[room_id]
            print(f"DEBUG: Room {room_id} deleted (empty).")
            
    return jsonify({"status": "ok"})

if __name__ == '__main__':
    app.run(port=5000, debug=True)