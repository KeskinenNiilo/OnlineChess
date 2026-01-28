// script.js

// 1. Initial Setup
const moveSound = new Audio('assets/move.wav')
const board = document.getElementById("chessboard");
let selectedSquare = null;
let pieceElements = new Map(); // Stores { "row-col": DOMElement }

// The "Source of Truth"
let gameState = [
    ["♜","♞","♝","♛","♚","♝","♞","♜"],
    ["♟","♟","♟","♟","♟","♟","♟","♟"],
    ["","","","","","","",""],
    ["","","","","","","",""],
    ["","","","","","","",""],
    ["","","","","","","",""],
    ["♙","♙","♙","♙","♙","♙","♙","♙"],
    ["♖","♘","♗","♕","♔","♗","♘","♖"]
];

const whitePieces = ["♙", "♖", "♘", "♗", "♕", "♔"];
const blackPieces = ["♟", "♜", "♞", "♝", "♛", "♚"];

let currentRoom = null;
let playerSide = "white"; 
let currentTurn = "white";

async function createRoom() {
    try {
        const response = await fetch('http://127.0.0.1:5000/create', { method: 'POST' });
        const data = await response.json();
        setupGame(data.room, "white");
    } catch (err) {
        showStatus("⚠️ Server unreachable.");
    }
}

async function joinRoom() {
    const inputField = document.getElementById('join-input');
    const code = inputField.value.toUpperCase().trim(); // Added trim()
    if (code.length < 3) return showStatus("Enter a valid code");

    console.log("Attempting to join room:", code);

    try {
        const response = await fetch(`http://127.0.0.1:5000/join?room=${code}`, { 
            method: 'POST' 
        });
        
        const data = await response.json();

        if (response.ok && data.status === "success") {
            await setupGame(code, data.side);
        } else {
            showStatus(data.message || "Room is full or doesn't exist.");
        }
    } catch (err) {
        console.error("Join error:", err);
        showStatus("⚠️ Server unreachable.");
    }
}

async function setupGame(roomCode, side) {
    currentRoom = roomCode;
    playerSide = side;

    // 1. Get the LATEST state from the server before showing the board
    try {
        const response = await fetch(`http://127.0.0.1:5000/state?room=${roomCode}`);
        const data = await response.json();
        
        if (data && data.board) {
            gameState = data.board;
            currentTurn = data.turn;
        }
    } catch (err) {
        console.error("Failed to sync initial state:", err);
    }

    // 2. Switch UI
    document.getElementById('landing-page').style.display = 'none';
    document.getElementById('game-screen').style.display = 'flex';
    document.getElementById('code-text').textContent = roomCode;

    if (playerSide === "black") applyPerspective();

    // 3. Now create the board with the synced gameState
    createBoard();
    
    // 4. Start polling for opponent moves
    setInterval(pollServer, 1500);
}

async function pollServer() {
    if (currentTurn === playerSide || !currentRoom) return;

    try {
        // Added room query parameter
        const response = await fetch(`http://127.0.0.1:5000/state?room=${currentRoom}`);
        const data = await response.json();
        
        if (data.turn === playerSide) {
            detectAndAnimateOpponentMove(data.board);
            gameState = data.board;
            currentTurn = data.turn;
        }
    } catch (e) { console.warn("Polling..."); }
}

function detectAndAnimateOpponentMove(newBoard) {
    let moveFound = { from: null, to: null };

    // Compare the current gameState with the newBoard from server
    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            const oldPiece = gameState[r][c];
            const newPiece = newBoard[r][c];

            // If a piece disappeared from here, it's the 'from'
            if (oldPiece !== "" && newPiece === "") {
                moveFound.from = [r, c];
            }
            // If a piece appeared here (and it wasn't there before), it's the 'to'
            if (oldPiece === "" && newPiece !== "") {
                moveFound.to = [r, c];
            }
            // Logic for a piece moving onto another piece (Capture)
            if (oldPiece !== "" && newPiece !== "" && oldPiece !== newPiece) {
                moveFound.to = [r, c];
            }
        }
    }

    if (moveFound.from && moveFound.to) {
        performSlide(moveFound.from[0], moveFound.from[1], moveFound.to[0], moveFound.to[1]);
    } else {
        // If we can't figure out the slide, just redraw as a fallback
        createBoard();
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

            // Create Piece if it exists in gameState
            const pieceType = gameState[row][col];
            if (pieceType) {
                createPieceElement(row, col, pieceType);
            }
        }
    }
}

function createPieceElement(row, col, type) {
    const piece = document.createElement("div");
    piece.classList.add("piece");
    piece.textContent = type;
    
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
        // Fetch from your Python mock server
        const response = await fetch(`http://127.0.0.1:5000/moves?row=${row}&col=${col}&room=${currentRoom}`, {
            signal: controller.signal
    });
        const data = await response.json();

        hideStatus();

        if (data.moves) highlightSquares(data.moves);

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
        const response = await fetch('http://127.0.0.1:5000/move', {
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
        // 1. Handle Captures
        if (pieceElements.has(targetKey)) {
            const capturedEl = pieceElements.get(targetKey);
            capturedEl.remove(); 
        }

        // 2. Move Visually
        pieceEl.style.top = `${toRow * 60}px`;
        pieceEl.style.left = `${toCol * 60}px`;

        // 3. UPDATE COORDINATES ON THE ELEMENT (Fixes the "Move once" bug)
        pieceEl.dataset.row = toRow;
        pieceEl.dataset.col = toCol;

        // 4. Update the Map
        pieceElements.delete(pieceKey);
        pieceElements.set(targetKey, pieceEl);

        // 5. Update logical state
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
        const url = `http://127.0.0.1:5000/leave?room=${currentRoom}&side=${playerSide}`;
        
        // 'keepalive: true' allows the request to outlive the page
        fetch(url, { 
            method: 'POST', 
            keepalive: true,
            mode: 'no-cors' // Use no-cors to bypass preflight checks during shutdown
        });
    }
});