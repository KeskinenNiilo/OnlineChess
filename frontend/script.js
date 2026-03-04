// script.js

const moveSound = new Audio('assets/move.wav')
const board = document.getElementById("chessboard");
const PIECE_VALUES = {
    "white_pawn": 1, "black_pawn": 1,
    "white_knight": 3, "black_knight": 3,
    "white_bishop": 3, "black_bishop": 3,
    "white_rook": 5, "black_rook": 5,
    "white_queen": 9, "black_queen": 9,
    "white_king": 0, "black_king": 0
};
let drawRequested = false; // Track if a draw has been requested
let gameOver = false; // Track if game is over
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
    if (playerSide === "black") applyPerspective();

    
    createBoard();// Create the board with the synced gameState
    updateTurnUI();//update the turn UI eg. "your turn", "opponent's turn"
    
    // ADD THIS LINE - Update material display
    updateMaterialDisplay();
    
    // Start polling for opponent moves
    setTimeout(pollServer, 1500);
}

async function pollServer() {
    if (currentTurn === playerSide || !currentRoom) return;

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
        if (data.turn === playerSide) {
            detectAndAnimateOpponentMove(data.board);
            gameState = data.board;
            currentTurn = data.turn;
            updateTurnUI();
            
            // ADD THIS LINE - Update material after opponent's move
            updateMaterialDisplay();
        } else {
            //ensuring the correct turn is displayed
            updateTurnUI();
        }
    } catch (e) { console.warn("Polling..."); }

    setTimeout(pollServer, 2000);
}

function detectAndAnimateOpponentMove(newBoard) {
    let moveFound = { from: null, to: null };

    // Compare the current gameState with the newBoard from server
    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            const oldPiece = gameState[r][c];
            const newPiece = newBoard[r][c];

            if (oldPiece == newPiece) continue;

            // If a piece disappeared from here, it's the 'from'.
            if (oldPiece !== "empty" && oldPiece !== "" && (newPiece === "empty" ||  newPiece === "")) {
                moveFound.from = [r, c];
            }

            if(newPiece !== "empty" && newPiece !== "") {
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
        
        // ADD THIS LINE - Update material after move
        updateMaterialDisplay();
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


//calculate material from gameState
function calculateMaterial() {
    let whiteTotal = 0;
    let blackTotal = 0;
    
    for (let row = 0; row < 8; row++) {
        for (let col = 0; col < 8; col++) {
            const piece = gameState[row][col];
            if (piece && piece !== "empty" && piece !== "") {
                const value = PIECE_VALUES[piece] || 0;
                if (whitePieces.includes(piece)) {
                    whiteTotal += value;
                } else if (blackPieces.includes(piece)) {
                    blackTotal += value;
                }
            }
        }
    }
    
    return { white: whiteTotal, black: blackTotal };
}

// update the material display
function updateMaterialDisplay() {
    const material = calculateMaterial();
    
    // Update the HTML elements with your EXISTING IDs
    document.getElementById('whiteMaterial').textContent = material.white;
    document.getElementById('blackMaterial').textContent = material.black;
    
    // Optional: Log the balance to console since you don't have a balance element in HTML
    const balance = material.white - material.black;
    if (balance > 0) {
        console.log(`White advantage: +${balance}`);
    } else if (balance < 0) {
        console.log(`Black advantage: ${balance}`);
    } else {
        console.log("Material equal");
    }
}
// fetch material from server (if you implement the backend endpoint)
async function fetchMaterialFromServer() {
    if (!currentRoom) return;
    
    try {
        const response = await fetch(`${API_URL}/material?room=${currentRoom}`);
        const data = await response.json();
        
        if (data) {
            document.getElementById('white-material').textContent = data.white;
            document.getElementById('black-material').textContent = data.black;
            
            const balanceElement = document.getElementById('material-balance');
            if (data.balance > 0) {
                balanceElement.textContent = '+' + data.balance;
                balanceElement.className = 'white-advantage';
            } else if (data.balance < 0) {
                balanceElement.textContent = data.balance;
                balanceElement.className = 'black-advantage';
            } else {
                balanceElement.textContent = '0';
                balanceElement.className = 'equal';
            }
        }
    } catch (err) {
        console.warn("Failed to fetch material from server, using local calculation");
        updateMaterialDisplay(); // Fallback to local calculation
    }
}

// Function to handle draw button click
function declareDraw() {
    if (gameOver) {
        showStatus("Game is already over!");
        return;
    }
    
    if (currentTurn !== playerSide) {
        showStatus("It's not your turn to request a draw!");
        return;
    }
    
    // Show confirmation dialog
    if (confirm("Do you want to offer a draw to your opponent?")) {
        sendDrawOffer();
    }
}

// Function to send draw offer to server
async function sendDrawOffer() {
    try {
        const response = await fetch(`${API_URL}/draw`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                room: currentRoom,
                side: playerSide
            })
        });
        
        const result = await response.json();
        
        if (result.status === "success") {
            drawRequested = true;
            showStatus("Draw offer sent! Waiting for opponent...");
        } else {
            showStatus("Failed to send draw offer.");
        }
    } catch (err) {
        showStatus("⚠️ Failed to send draw offer.");
    }
}

// Function to handle forfeit button click
function forfeitGame() {
    if (gameOver) {
        showStatus("Game is already over!");
        return;
    }
    
    // Show confirmation dialog
    const confirmForfeit = confirm("Are you sure you want to forfeit? This will count as a loss.");
    
    if (confirmForfeit) {
        sendForfeit();
    }
}

// Function to send forfeit to server
async function sendForfeit() {
    try {
        const response = await fetch(`${API_URL}/forfeit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                room: currentRoom,
                side: playerSide
            })
        });
        
        const result = await response.json();
        
        if (result.status === "success") {
            gameOver = true;
            showStatus(`You forfeited. ${playerSide === "white" ? "Black" : "White"} wins!`);
            
            // Update win/loss counts
            updateWinLossCounts(playerSide === "white" ? "black" : "white");
            
            // Show restart option after 2 seconds
            setTimeout(() => {
                showRestartOption();
            }, 2000);
        } else {
            showStatus("Failed to forfeit.");
        }
    } catch (err) {
        showStatus("⚠️ Failed to forfeit.");
    }
}

// Function to handle draw response from opponent
async function respondToDraw(accept) {
    try {
        const response = await fetch(`${API_URL}/draw-response`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                room: currentRoom,
                side: playerSide,
                accept: accept
            })
        });
        
        const result = await response.json();
        
        if (result.status === "success") {
            if (accept) {
                gameOver = true;
                showStatus("Draw accepted! Game is a draw.");
                
                // Update win/loss counts (draw doesn't affect wins/losses)
                // You might want to add a draws counter if you want
                
                // Show restart option after 2 seconds
                setTimeout(() => {
                    showRestartOption();
                }, 2000);
            } else {
                showStatus("Draw declined. Game continues!");
                drawRequested = false;
            }
        }
    } catch (err) {
        showStatus("⚠️ Failed to respond to draw offer.");
    }
}

// Function to show draw popup when opponent requests draw
function showDrawPopup() {
    // Create popup if it doesn't exist
    let popup = document.getElementById('draw-popup');
    if (!popup) {
        popup = document.createElement('div');
        popup.id = 'draw-popup';
        popup.className = 'popup';
        popup.innerHTML = `
            <div class="popup-content">
                <h3>Draw Offer</h3>
                <p>Your opponent offers a draw. Do you accept?</p>
                <div class="popup-buttons">
                    <button onclick="respondToDraw(true)" class="accept-btn">✓ Accept</button>
                    <button onclick="respondToDraw(false)" class="decline-btn">✗ Decline</button>
                </div>
            </div>
        `;
        document.body.appendChild(popup);
    }
    popup.style.display = 'flex';
}

// Function to show restart option after game ends
function showRestartOption() {
    // Create restart popup if it doesn't exist
    let popup = document.getElementById('restart-popup');
    if (!popup) {
        popup = document.createElement('div');
        popup.id = 'restart-popup';
        popup.className = 'popup';
        popup.innerHTML = `
            <div class="popup-content">
                <h3>Game Over</h3>
                <p id="game-over-message">The game has ended.</p>
                <div class="popup-buttons">
                    <button onclick="restartGame()" class="restart-btn">🔄 Play Again</button>
                    <button onclick="returnToLobby()" class="lobby-btn">🏠 Lobby</button>
                </div>
            </div>
        `;
        document.body.appendChild(popup);
    }
    
    // Update message based on result
    const messageEl = document.getElementById('game-over-message');
    if (messageEl) {
        // You can customize this message based on how the game ended
        messageEl.textContent = "The game has ended. What would you like to do?";
    }
    
    popup.style.display = 'flex';
}

// Function to restart the game
async function restartGame() {
    try {
        const response = await fetch(`${API_URL}/restart`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                room: currentRoom
            })
        });
        
        const result = await response.json();
        
        if (result.status === "success") {
            // Reset game state
            gameOver = false;
            drawRequested = false;
            
            // Get fresh board state
            const stateResponse = await fetch(`${API_URL}/state?room=${currentRoom}`);
            const data = await stateResponse.json();
            
            if (data && data.board) {
                gameState = data.board;
                currentTurn = data.turn;
            }
            
            // Recreate board
            createBoard();
            updateTurnUI();
            updateMaterialDisplay();
            
            // Hide popups
            hideAllPopups();
            
            showStatus("Game restarted!");
        } else {
            showStatus("Failed to restart game.");
        }
    } catch (err) {
        showStatus("⚠️ Failed to restart game.");
    }
}

// Function to return to lobby
function returnToLobby() {
    // Hide game screen and show landing page
    document.getElementById('game-screen').style.display = 'none';
    document.getElementById('landing-page').style.display = 'block';
    
    // Reset game state
    gameOver = false;
    drawRequested = false;
    currentRoom = null;
    
    // Hide all popups
    hideAllPopups();
    
    // Clear join input
    document.getElementById('join-input').value = '';
}

// Helper function to hide all popups
function hideAllPopups() {
    const popups = document.querySelectorAll('.popup');
    popups.forEach(popup => {
        popup.style.display = 'none';
    });
}

// Function to update win/loss counts
function updateWinLossCounts(winner) {
    if (winner === "white") {
        const whiteWins = document.getElementById('whiteWins');
        const blackLosses = document.getElementById('blackLosses');
        whiteWins.textContent = parseInt(whiteWins.textContent) + 1;
        blackLosses.textContent = parseInt(blackLosses.textContent) + 1;
    } else if (winner === "black") {
        const blackWins = document.getElementById('blackWins');
        const whiteLosses = document.getElementById('whiteLosses');
        blackWins.textContent = parseInt(blackWins.textContent) + 1;
        whiteLosses.textContent = parseInt(whiteLosses.textContent) + 1;
    }
}

// Modify pollServer to check for draw offers
async function pollServer() {
    if (gameOver) return; // Don't poll if game is over
    
    if (currentTurn === playerSide || !currentRoom) return;

    try {
        const response = await fetch(`${API_URL}/state?room=${currentRoom}`);

        if (response.status === 429) {
            console.warn("Rate limit hit, slowing down...");
            return;
        }

        const data = await response.json();
        
        // Check if there's a draw offer
        if (data.drawOffer && data.drawOffer !== playerSide && !drawRequested) {
            showDrawPopup();
        }
        
        // Check if game is over
        if (data.gameOver) {
            gameOver = true;
            if (data.winner) {
                showStatus(`${data.winner} wins!`);
                updateWinLossCounts(data.winner);
            } else if (data.draw) {
                showStatus("Game ended in a draw!");
            }
            setTimeout(() => {
                showRestartOption();
            }, 2000);
        }
        
        // If the data got from the server is equal to player's side,
        // animate the opponent's move, refresh the gamestate and change turn
        if (data.turn === playerSide) {
            detectAndAnimateOpponentMove(data.board);
            gameState = data.board;
            currentTurn = data.turn;
            updateTurnUI();
            updateMaterialDisplay();
        } else {
            updateTurnUI();
        }
    } catch (e) { console.warn("Polling..."); }

    setTimeout(pollServer, 2000);
}