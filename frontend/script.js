// script.js

const moveSound = new Audio('assets/move.wav')
const board = document.getElementById("chessboard");
let selectedSquare = null;
let pieceElements = new Map(); // Stores { "row-col": DOMElement }

//The text below the board, showing current turn
let turnIndicator = document.getElementById("turn-indicator");

// Server url
const API_URL = "http://localhost:8080/api";

// Mapping the pieces from java names to unicode for the frontend
const PIECE_MAP = {
    "white_king": "♔", "white_queen": "♕", "white_rook": "♖",
    "white_bishop": "♗", "white_knight": "♘", "white_pawn": "♙",
    "black_king": "♚", "black_queen": "♛", "black_rook": "♜",
    "black_bishop": "♝", "black_knight": "♞", "black_pawn": "♟",
    "empty": ""
};

const whitePieces = ["white_pawn", "white_rook", "white_knight", "white_bishop", "white_queen", "white_king"];
const blackPieces = ["black_pawn", "black_rook", "black_knight", "black_bishop", "black_queen", "black_king"];
let gameState = [];


let currentRoom = null;
let playerSide = "white"; 
let currentTurn = "white";

// call the server to create new room
async function createRoom() {
    try {
        const response = await fetch(`${API_URL}/create`, { method: 'POST' });

        if(response.status === 429) {
            showStatus("⚠️ Too many requests!")
            return;
        }

        if(!response.ok) {
            showStatus("⚠️ Failed to create room.")
            return;
        }

        const data = await response.json();
        setupGame(data.room, "white");

    } catch (err) {
        showStatus("⚠️ Server unreachable.");
    }
}

// call the server to join a room
async function joinRoom() {
    const inputField = document.getElementById('join-input');
    const code = inputField.value.toUpperCase().trim();

    console.log("Attempting to join room:", code);

    try {
        const response = await fetch(`${API_URL}/join?room=${code}`, { 
            method: 'POST' 
        });

        if (response.status === 429) {
            showStatus("⚠️ Too many requests.")
            return;
        }
        
        const data = await response.json();

        if (response.ok && data.status === "success") {
            await setupGame(code, data.side);
        } else {
            showStatus(data.message); //returns either room full, or doesn't exist.
        }
    } catch (err) {
        console.error("Join error:", err);
        showStatus("⚠️ Server unreachable.");
    }
}

async function setupGame(roomCode, side) {
    if (!roomCode) return;

    currentRoom = roomCode;
    playerSide = side;

    // Sync the board with server
    try {
        const response = await fetch(`${API_URL}/state?room=${roomCode}`);

        if (response.status === 429) {
            showStatus("⚠️ Too many requests.");
            return;
        }

        const data = await response.json();
        
        if (data && data.board) {
            gameState = data.board;
            currentTurn = data.turn;
        }
    } catch (err) {
        console.error("Failed to sync initial state:", err);
    }

    // Switch UI
    document.getElementById('landing-page').style.display = 'none';
    document.getElementById('game-screen').style.display = 'flex';
    document.getElementById('code-text').textContent = roomCode;

    // If the player's side is black, the board is rotated 180
    if (playerSide === "black")applyPerspective();

    
    createBoard();// Create the board with the synced gameState
    updateTurnUI();//update the turn UI eg. "your turn", "opponent's turn"
    
    // Start polling for opponent moves
    setTimeout(pollServer, 1500);
}

async function pollServer() {
    if (!currentRoom) {
	setTimeout(pollServer, 2000);
	return
	}

    try {
        //get the state of current room
        const response = await fetch(`${API_URL}/state?room=${currentRoom}`);

        if (response.status === 429) {
            console.warn("Rate limit hit, slowing down...");
            return;
        }

        const data = await response.json();
        
        // If the data got from the server is equal to player's side,
        // animate the opponent's move, refresh the gamestate and change turn
        if (data.turn === playerSide && currentTurn !== playerSide) {
            detectAndAnimateOpponentMove(data.board);
            gameState = data.board;
            currentTurn = data.turn;

            setTimeout(() => {
            createBoard();
            updateTurnUI();
            }, 400);
        } else {
            currentTurn = data.turn;
            updateTurnUI();
        }

        if (data.checkMate || data.staleMate) {
            showStatus(data.checkMate ? "Checkmate" : "Stalemate");
            return;
        }
    } catch (e) { console.warn("Polling error:", e); }

    setTimeout(pollServer, 2000);
}

function detectAndAnimateOpponentMove(newBoard) {
    let moveFrom = null;
    let moveTo = null;
    const opponentColor = (playerSide === "white") ? "black" : "white";

    // Compare the current gameState with the newBoard from server
    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            const oldPiece = gameState[r][c];
            const newPiece = newBoard[r][c];

            if (oldPiece == newPiece) continue;

            // If a piece disappeared from here, it's the 'from'.
            if (oldPiece !== "empty" && oldPiece !== "" && (newPiece === "empty" ||  newPiece === "")) {
                moveFrom = [r, c];
            }

            if(newPiece.includes(opponentColor)) {
                moveTo = [r, c];
            }
        }
    }

    if (moveFrom && moveTo) {
        performSlide(moveFrom[0], moveFrom[1], moveTo[0], moveTo[1]);
    } else {
        // If we can't figure out the slide, just redraw as a fallback
        createBoard(newBoard);
    }
}

function updateTurnUI() {
    if (currentTurn === playerSide) {
        turnIndicator.innerHTML = "It's your turn."
    } else {
        turnIndicator.innerHTML = "It's the opponent's turn."
    }
}

function applyPerspective() {
        board.style.transform = "rotate(180deg)";
        const style = document.createElement('style');
        style.innerHTML = `.piece { transform: rotate(180deg); }`;
        document.head.appendChild(style);
    }

// 2. Creation Logic
function createBoard() {
    board.innerHTML = ""; // Clear board
    pieceElements.clear(); // Clear the map

    for (let row = 0; row < 8; row++) {
        for (let col = 0; col < 8; col++) {
            // Create Square
            const square = document.createElement("div");
            square.classList.add("square", (row + col) % 2 === 0 ? "light" : "dark");
            square.dataset.row = row;
            square.dataset.col = col;
            
            // Squares handle moving to a destination
            square.addEventListener("click", () => handleSquareClick(row, col));
            board.appendChild(square);

            const javaPieceName = gameState[row][col];
            const unicodeSymbol = PIECE_MAP[javaPieceName] || "";
            // Create Piece if it exists in gameState
            //const pieceType = gameState[row][col];
            if (unicodeSymbol) {
                createPieceElement(row, col,unicodeSymbol, javaPieceName);
            }
        }
    }
}

function createPieceElement(row, col, unicode, javaName) {
    const piece = document.createElement("div");
    piece.classList.add("piece");
    piece.textContent = unicode;
    piece.dataset.javaName = javaName;
    
    // Store current coordinates on the element
    piece.dataset.row = row;
    piece.dataset.col = col;

    piece.style.top = `${row * 60}px`;
    piece.style.left = `${col * 60}px`;

    piece.addEventListener("click", (e) => {
        e.stopPropagation();
        // Always use the LATEST coordinates from the dataset
        handlePieceClick(parseInt(piece.dataset.row), parseInt(piece.dataset.col));
    });

    board.appendChild(piece);
    pieceElements.set(`${row}-${col}`, piece);
}

// 3. Selection Logic (The "GET" phase)
async function handlePieceClick(row, col) {
    const piece = gameState[row][col];
    const pieceColor = whitePieces.includes(piece) ? "white" : (blackPieces.includes(piece) ? "black" : null);

    // New capture logic
    const targetSquare = board.querySelector(`.square[data-row='${row}'][data-col='${col}']`);
    if (selectedSquare && targetSquare.classList.contains("highlight")) {
        await executeMove(selectedSquare.row, selectedSquare.col, row, col);
        return; // Exit early so we don't try to "re-select" the enemy piece
    }


    if(currentTurn !== playerSide) {
        showStatus("It's not your turn!");
        return;
    }

    if (pieceColor !== playerSide) {
        return;
    }
    const statusEl = document.getElementById('status-message');

    if (selectedSquare && selectedSquare.row === row && selectedSquare.col === col) {
        clearHighlights();
        return;
    }

    clearHighlights();
    selectedSquare = { row, col };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 500); // 500ms limit

    try {
        const url = `${API_URL}/moves?room=${currentRoom}&x=${row}&y=${col}`;
        const response = await fetch(url);
        const data = await response.json();

        hideStatus();

        if (data && Array.isArray(data)) {
            highlightSquares(data);
        }


        clearTimeout(timeoutId);
    } catch (err) {
        showStatus("⚠️ Connection Error: Cannot fetch moves.");
        selectedSquare=null;
    }
}

function showStatus(msg) {
    const statusEl = document.getElementById('status-message');

    statusEl.innerHTML = `
        <span>${msg}</span>
        <button onclick="hideStatus()">✕</button>
        `;
        statusEl.style.display = 'flex';
        statusEl.style.opacity = '1';
}

function hideStatus() {
    const statusEl = document.getElementById('status-message');
    statusEl.style.opacity = '0';

    setTimeout(() => {
        if(statusEl.style.opacity === '0') statusEl.style.display = 'none';
    }, 500);
}

// 4. Movement Logic (The "POST" phase)
async function handleSquareClick(toRow, toCol) {
    const targetSquare = board.querySelector(`.square[data-row='${toRow}'][data-col='${toCol}']`);
    
    if (targetSquare.classList.contains("highlight") && selectedSquare) {
        await executeMove(selectedSquare.row, selectedSquare.col, toRow, toCol);
    } else {
        clearHighlights();
    }
}

async function executeMove(fromRow, fromCol, toRow, toCol) {
    try {
        const response = await fetch(`${API_URL}/move`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({room: currentRoom,
                 from: [fromRow, fromCol],
                  to: [toRow, toCol] 
            })
        });
        const result = await response.json();

        if (result.status === "success") {
            performSlide(fromRow, fromCol, toRow, toCol);
        } else {
            showStatus("Server: Invalid move.");
        }
    } catch (err) {
        showStatus("⚠️ Move failed: Server unreachable.")
    }
}

function performSlide(fromRow, fromCol, toRow, toCol) {
    const pieceKey = `${fromRow}-${fromCol}`;
    const targetKey = `${toRow}-${toCol}`;
    const pieceEl = pieceElements.get(pieceKey);

    if (pieceEl) {

        moveSound.currentTime = 0; 
        moveSound.play().catch(e => console.error("Audio play failed:", e));
        // Handle Captures
        if (pieceElements.has(targetKey)) {
            const capturedEl = pieceElements.get(targetKey);
            capturedEl.remove(); 
        }

        // Move Visually
        pieceEl.style.top = `${toRow * 60}px`;
        pieceEl.style.left = `${toCol * 60}px`;

        // UPDATE COORDINATES ON THE ELEMENT (Fixes the "Move once" bug)
        pieceEl.dataset.row = toRow;
        pieceEl.dataset.col = toCol;

        // Update the Map
        pieceElements.delete(pieceKey);
        pieceElements.set(targetKey, pieceEl);

        // Update logical state
        gameState[toRow][toCol] = gameState[fromRow][fromCol];
        gameState[fromRow][fromCol] = "";

        currentTurn = (currentTurn === "white") ? "black" : "white";
        console.log("Next turn:", currentTurn);
    }

    

    clearHighlights();
}

// 5. Helpers
function highlightSquares(moves) {
    moves.forEach(([r, c]) => {
        const sq = board.querySelector(`.square[data-row='${r}'][data-col='${c}']`);
        if (sq) sq.classList.add("highlight");
    });
}

function clearHighlights() {
    selectedSquare = null;
    board.querySelectorAll(".square.highlight").forEach(sq => sq.classList.remove("highlight"));
}

window.addEventListener('beforeunload', () => {
    if (currentRoom && playerSide) {
        const url = `${API_URL}/leave?room=${currentRoom}&side=${playerSide}`;
        
        // 'keepalive: true' allows the request to outlive the page
        fetch(url, { 
            method: 'POST', 
            keepalive: true,
            mode: 'no-cors' // Use no-cors to bypass preflight checks during shutdown
        });
    }
});