// script.js

const moveSound = new Audio('assets/move.wav')
const board = document.getElementById("chessboard");
let selectedSquare = null;
let pieceElements = new Map(); // Stores { "row-col": DOMElement }

//The text below the board, showing current turn
let turnIndicator = document.getElementById("turn-indicator");

let gameOver = false;
let drawRequested = false;

const PIECE_VALUES = {
    "white_pawn": 1, "black_pawn": 1,
    "white_knight": 3, "black_knight": 3,
    "white_bishop": 3, "black_bishop": 3,
    "white_rook": 5, "black_rook": 5,
    "white_queen": 9, "black_queen": 9,
    "white_king": 0, "black_king": 0
};

// Server url
const API_URL = "https://onlinechess-ey0p.onrender.com/api";

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

// ========== MATERIAL TRACKING FUNCTIONS ==========

// Calculate material from current game state
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

// Update the material display in scoreboard
function updateMaterialDisplay() {
    const material = calculateMaterial();
    document.getElementById('whiteMaterial').textContent = material.white;
    document.getElementById('blackMaterial').textContent = material.black;
}

// ========== DRAW AND FORFEIT FUNCTIONS ==========

// Draw button handler
function declareDraw() {
    if (gameOver) {
        showStatus("Game is already over!");
        return;
    }
    
    if (currentTurn !== playerSide) {
        showStatus("It's not your turn to request a draw!");
        return;
    }
    
    if (confirm("Do you want to offer a draw to your opponent?")) {
        sendDrawOffer();
    }
}

// Send draw offer to server
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
            showStatus(result.message || "Failed to send draw offer.");
        }
    } catch (err) {
        showStatus("⚠️ Failed to send draw offer.");
    }
}

// Forfeit button handler
function forfeitGame() {
    if (gameOver) {
        showStatus("Game is already over!");
        return;
    }
    
    if (confirm("Are you sure you want to forfeit? This will count as a loss.")) {
        sendForfeit();
    }
}

// Send forfeit to server
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
            const winner = playerSide === "white" ? "black" : "white";
            showStatus(`You forfeited. ${winner.toUpperCase()} wins!`);
            
            // Disable board interactions
            board.style.pointerEvents = "none";
            
            // Update win/loss counts
            updateWinLossCounts(winner);
            
            // Show restart option
            setTimeout(() => showRestartOption(), 2000);
        } else {
            showStatus(result.message || "Failed to forfeit.");
        }
    } catch (err) {
        showStatus("⚠️ Failed to forfeit.");
    }
}

// Respond to draw offer
function respondToDraw(accept) {
    sendDrawResponse(accept);
}

// Send draw response to server
async function sendDrawResponse(accept) {
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
                
                // Disable board interactions
                board.style.pointerEvents = "none";
                
                setTimeout(() => showRestartOption(), 2000);
            } else {
                showStatus("Draw declined. Game continues!");
                drawRequested = false;
                hideDrawPopup();
            }
        }
    } catch (err) {
        showStatus("⚠️ Failed to respond to draw offer.");
    }
}


// Show draw offer popup
function showDrawPopup() {
    // Remove existing popup if any
    const existingPopup = document.getElementById('draw-popup');
    if (existingPopup) {
        existingPopup.remove();
    }
    
    const popup = document.createElement('div');
    popup.id = 'draw-popup';
    popup.className = 'popup';
    popup.innerHTML = `
        <div class="popup-content">
            <h3>Draw Offer</h3>
            <p>Your opponent offers a draw. Do you accept?</p>
            <div class="popup-buttons">
                <button id="accept-draw-btn" class="accept-btn">✓ Accept</button>
                <button id="decline-draw-btn" class="decline-btn">✗ Decline</button>
            </div>
        </div>
    `;
    document.body.appendChild(popup);
    popup.style.display = 'flex';
    
    document.getElementById('accept-draw-btn').addEventListener('click', () => {
        respondToDraw(true);
    });
    
    document.getElementById('decline-draw-btn').addEventListener('click', () => {
        respondToDraw(false);
    });
}

// Hide draw popup
function hideDrawPopup() {
    const popup = document.getElementById('draw-popup');
    if (popup) popup.style.display = 'none';
}

// Show restart option after game ends
function showRestartOption() {
    // Remove existing popup if any
    const existingPopup = document.getElementById('restart-popup');
    if (existingPopup) {
        existingPopup.remove();
    }
    
    const popup = document.createElement('div');
    popup.id = 'restart-popup';
    popup.className = 'popup';
    popup.innerHTML = `
        <div class="popup-content">
            <h3>Game Over</h3>
            <p id="game-over-message">The game has ended.</p>
            <div class="popup-buttons">
                <button id="play-again-btn" class="restart-btn">🔄 Play Again</button>
                <button id="lobby-btn" class="lobby-btn">🏠 Lobby</button>
            </div>
        </div>
    `;
    document.body.appendChild(popup);
    popup.style.display = 'flex';
    
    // Remove any existing listeners by cloning and replacing
    const playAgainBtn = document.getElementById('play-again-btn');
    const lobbyBtn = document.getElementById('lobby-btn');
    
    const newPlayAgainBtn = playAgainBtn.cloneNode(true);
    const newLobbyBtn = lobbyBtn.cloneNode(true);
    playAgainBtn.parentNode.replaceChild(newPlayAgainBtn, playAgainBtn);
    lobbyBtn.parentNode.replaceChild(newLobbyBtn, lobbyBtn);
    
    newPlayAgainBtn.addEventListener('click', () => {
        restartGame();
    });
    
    newLobbyBtn.addEventListener('click', () => {
        returnToLobby();
    });
}

async function restartGame() {
    const btn = document.getElementById('play-again-btn');
    if (!btn) return;
    
    const originalText = btn.textContent;
    btn.disabled = true;
    btn.textContent = "⌛ Waiting...";

    try {
        const response = await fetch(`${API_URL}/restart`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ room: currentRoom, side: playerSide })
        });
        
        const result = await response.json();
        
        if (result.status === "success") {
            // Reset local game state
            gameOver = false;
            drawRequested = false;
            board.style.pointerEvents = "";
            lastEventIdx = 0;
            
            // Get fresh board state
            const stateResponse = await fetch(`${API_URL}/state?room=${currentRoom}`);
            const data = await stateResponse.json();
            
            if (data && data.board) {
                gameState = data.board;
                currentTurn = data.turn;
            }
            
            // Update UI
            createBoard();
            updateTurnUI(false);
            updateMaterialDisplay();
            hideAllPopups();
            clearHighlights();
            
            // DON'T remove the popup immediately - keep "Waiting..." showing
            // The popup will be removed when the game actually restarts
            // Just keep the button disabled for now
            
        } else {
            showStatus(result.message || "Failed to restart game.");
            btn.disabled = false;
            btn.textContent = originalText;
        }
    } catch (err) {
        console.error("Restart error:", err);
        showStatus("⚠️ Failed to reach server.");
        btn.disabled = false;
        btn.textContent = originalText;
    }
}
// Return to lobby
function returnToLobby() {
    if (currentRoom && playerSide) {
        const url = `${API_URL}/leave?room=${currentRoom}&side=${playerSide}`;
        fetch(url, { 
            method: 'POST', 
            keepalive: true,
            mode: 'no-cors'
        });
    }
    
    board.style.pointerEvents = "";
    document.getElementById('game-screen').style.display = 'none';
    document.getElementById('landing-page').style.display = 'block';
    gameOver = false;
    drawRequested = false;
    currentRoom = null;
    hideAllPopups();
    document.getElementById('join-input').value = '';
    
    // Also remove any restart popup
    const popup = document.getElementById('restart-popup');
    if (popup) popup.remove();
}

// Hide all popups
function hideAllPopups() {
    const popups = document.querySelectorAll('.popup');
    popups.forEach(popup => popup.style.display = 'none');
}

// Update win/loss counts in scoreboard
function updateWinLossCounts(winner) {
    if (winner === "white") {
        const whiteWins = document.getElementById('whiteWins');
        const blackLosses = document.getElementById('blackLosses');
        whiteWins.textContent = parseInt(whiteWins.textContent || 0) + 1;
        blackLosses.textContent = parseInt(blackLosses.textContent || 0) + 1;
    } else if (winner === "black") {
        const blackWins = document.getElementById('blackWins');
        const whiteLosses = document.getElementById('whiteLosses');
        blackWins.textContent = parseInt(blackWins.textContent || 0) + 1;
        whiteLosses.textContent = parseInt(whiteLosses.textContent || 0) + 1;
    }
}

// call the server to create new room
async function createRoom() {
    const btn = document.getElementById('create-btn');
    let originalText = "";

    if(btn) {
        originalText = btn.textContent;
        btn.disabled = true;
        btn.textContent = "⌛ Waiting for server...";
    }
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000);

    try {
        const response = await fetch(`${API_URL}/create`, {
            method: 'POST',
            signal: controller.signal
        });
        clearTimeout(timeoutId);

        if(response.status === 429) {
            showStatus("⚠️ Too many requests!")
            if(btn) {
                btn.disabled = false;
                btn.textContent = originalText;
            } 
            return;
        }

        if(!response.ok) {
            throw new Error("Server starting up");
        }
        // Run setupGame with data from server.
        const data = await response.json();
        if(btn) {
            btn.disabled = false;
            btn.textContent = originalText;
        }
        setupGame(data.room, "white");

    } catch (err) {
        clearTimeout(timeoutId);

        if (err.name === 'AbortError' || err.message.includes('Failed to fetch') || err.message === "Server starting up") {
            showStatus("⌛ Server is waking up, this may take up to 60sec...");
            setTimeout(createRoom, 5000);
        } else {
            showStatus("⚠️ Server unreachable.");
        }
        
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
            // Sync the game with server
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
    gameOver = false;
    drawRequested = false;

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
    updateTurnUI(false);//update the turn UI eg. "your turn", "opponent's turn"
    updateMaterialDisplay();
    
    // Start polling for opponent moves
    setTimeout(pollServer, 1500);
}

let lastEventIdx = 0;

async function pollServer() {
    if (!currentRoom) {
        setTimeout(pollServer, 2000);
        return;
    }

    try {
        const response = await fetch(`${API_URL}/state?room=${currentRoom}`);
        
        if (response.status === 404) {
            currentRoom = null;
            showStatus("⚠️ Room was closed or timed out.");
            returnToLobby();
            return;
        }

        if (response.status === 429) {
            console.warn("Rate limit hit, slowing down...");
            setTimeout(pollServer, 3000);
            return;
        }

        const data = await response.json();

        // Check for new log events from the server
        if(data.events && data.events.length > lastEventIdx) {
            for (let i = lastEventIdx; i < data.events.length; i++) {
                const msg = data.events[i];

                // Color coding
                let color = null;
                if (msg.includes("joined")) color = "green";
                if (msg.includes("left")) color = "red";
                if (msg.includes("wants a rematch")) color = "cyan";

                addLog(msg, color);
            }
            lastEventIdx = data.events.length;
        }

        if (data.gameOver === false && gameOver === true) {
            console.log("Game restarted detected! Opponent restarted the game.");
            gameOver = false;
            drawRequested = false;
            board.style.pointerEvents = "";
            lastEventIdx = 0;

            gameState = data.board;
            currentTurn = data.turn;

            createBoard();
            updateTurnUI(false);
            updateMaterialDisplay();
            hideAllPopups();

            const btn = document.getElementById('play-again-btn');
            if (btn) {
                btn.disabled = false;
                btn.textContent = "🔄 Play Again";
            }
            
            const popup = document.getElementById('restart-popup');
            if (popup) popup.remove();
            
            setTimeout(pollServer, 1500);
            return;
        }

        // Check if game is over from server
        if (data.gameOver && !gameOver) {
            gameOver = true;
            
            if (data.winner) {
                updateWinLossCounts(data.winner);
            }
            
            board.style.pointerEvents = "none";
            setTimeout(() => showRestartOption(), 2000);
            setTimeout(pollServer, 5000);
            return;
        }
        
        // Update material display
        if (data.whiteMaterial !== undefined) {
            document.getElementById('whiteMaterial').textContent = data.whiteMaterial;
            document.getElementById('blackMaterial').textContent = data.blackMaterial;
        }
        
        // Update check status and turnUI
        const isCheck = data.inCheck || data.checkMate;
        const isCheckmate = data.checkMate;
        updateCheckStatus(isCheck, data.turn);
        updateTurnUI(isCheckmate);
        
        // Check for draw offer
        if (data.drawOffer && data.drawOfferedBy !== playerSide && !drawRequested && !gameOver) {
            showDrawPopup();
        }

        if (data.turn === playerSide && currentTurn !== playerSide && !gameOver) {
            detectAndAnimateOpponentMove(data.board);
        
            // Update game state
            gameState = data.board;
            currentTurn = data.turn;
            updateMaterialDisplay();
        } else {
            // Just update turn if board hasn't changed
            currentTurn = data.turn;
            updateTurnUI(isCheckmate);
        }
        
        // Handle checkmate
        if (isCheckmate && !gameOver) {
            gameOver = true;
            const winner = currentTurn === "white" ? "Black" : "White";
            showStatus(`CHECKMATE! ${winner} wins!`);
            updateWinLossCounts(winner.toLowerCase());
            board.style.pointerEvents = "none";
            setTimeout(() => showRestartOption(), 2000);
            return;
        }
        
    } catch (e) { 
        console.warn("Polling error:", e); 
    }

    setTimeout(pollServer, 1500);
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
            if (oldPiece !== "empty" && oldPiece !== "" && (newPiece === "empty" || newPiece === "")) {
                moveFrom = [r, c];
            }

            if (newPiece && newPiece.includes(opponentColor)) {
                moveTo = [r, c];
            }
        }
    }

    if (moveFrom && moveTo) {
        performSlide(moveFrom[0], moveFrom[1], moveTo[0], moveTo[1]);
    } else {
        // If we can't figure out the slide, just redraw as a fallback
        createBoard();
    }
}

// Handling the turn UI text
function updateTurnUI(checkMate) {
    if (checkMate) {
        const winner = (currentTurn === "white") ? "Black" : "White";
        turnIndicator.innerHTML = `<b style="color: red;">CHECKMATE! ${winner} wins!</b>`;
        return;
    } else if (gameOver) {
        return;
    } else {
        if (currentTurn === playerSide) {
            turnIndicator.innerHTML = "It's your turn.";
        } else {
            turnIndicator.innerHTML = "It's the opponent's turn.";
        }
    }
}

// Highlight the king square, if in check.
function updateCheckStatus(isCheck, serverTurn) {
    document.querySelectorAll('.square.check-warning').forEach(sq =>{
        sq.classList.remove('check-warning');
    });

    if (!isCheck) return;

    const kingType = (serverTurn === "white") ? "white_king" : "black_king";

    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            if (gameState[r][c] === kingType) {
                const kingSquare = board.querySelector(`.square[data-row='${r}'][data-col='${c}']`);
                if (kingSquare) {
                    kingSquare.classList.add('check-warning');
                }
                return; // Found it, stop searching
            }
        }
    }
}

// For black player, rotate the board 180
function applyPerspective() {
    board.style.transform = "rotate(180deg)";
    const style = document.createElement('style');
    style.innerHTML = `.piece { transform: rotate(180deg); }`;
    document.head.appendChild(style);
}

// Rendering the chessboard
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
            
            // SAdding event listener to each square
            square.addEventListener("click", () => handleSquareClick(row, col));
            board.appendChild(square);

            const javaPieceName = gameState[row][col];
            const unicodeSymbol = PIECE_MAP[javaPieceName] || "";
            if (unicodeSymbol) {
                createPieceElement(row, col, unicodeSymbol, javaPieceName);
            }
        }
    }

    updateCheckStatus(false, "");
}

// Create an element for each piece
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

// handling the piece selection
async function handlePieceClick(row, col) {
    // Game over check
    if (gameOver) {
        showStatus("Game is over! Please restart or join a new game.");
        return;
    }
    
    const piece = gameState[row][col];
    const pieceColor = whitePieces.includes(piece) ? "white" : (blackPieces.includes(piece) ? "black" : null);

    // Capture logic
    const targetSquare = board.querySelector(`.square[data-row='${row}'][data-col='${col}']`);
    if (selectedSquare && targetSquare.classList.contains("highlight")) {
        await executeMove(selectedSquare.row, selectedSquare.col, row, col);
        return; // Exit early so we don't try to "re-select" the enemy piece
    }

    // if the player tries to select a piece on opponent's turn.
    if(currentTurn !== playerSide) {
        showStatus("It's not your turn!");
        return;
    }

    // if the player tries to click opponent's piece, do nothing.
    if (pieceColor !== playerSide) {
        return;
    }


    if (selectedSquare && selectedSquare.row === row && selectedSquare.col === col) {
        clearHighlights();
        return;
    }

    clearHighlights();
    selectedSquare = { row, col };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 500);
    // fetch possible moves from server.
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

// status popuppien luominen
let statusTimeout;
function showStatus(msg) {
    const statusEl = document.getElementById('status-message');

    clearTimeout(statusTimeout);

    statusEl.innerHTML = `
        <span>${msg}</span>
        <button onclick="hideStatus()">✕</button>
    `;
    statusEl.style.display = 'flex';
    statusEl.style.opacity = '1';

    // sulje 6s kuluttua automaattisesti
    statusTimeout = setTimeout(hideStatus, 6000);
}

// status popuppien piilotus.
function hideStatus() {
    const statusEl = document.getElementById('status-message');
    statusEl.style.opacity = '0';
    setTimeout(() => {
        if(statusEl.style.opacity === '0') statusEl.style.display = 'none';
    }, 500);
}

// ruutujen klikkaamisen käsittely
async function handleSquareClick(toRow, toCol) {
    const targetSquare = board.querySelector(`.square[data-row='${toRow}'][data-col='${toCol}']`);
    
    if (targetSquare.classList.contains("highlight") && selectedSquare) {
        await executeMove(selectedSquare.row, selectedSquare.col, toRow, toCol);
    } else {
        clearHighlights();
    }
}

// siirron suoritus.
async function executeMove(fromRow, fromCol, toRow, toCol) {
    try {
        const response = await fetch(`${API_URL}/move`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                room: currentRoom,
                from: [fromRow, fromCol],
                to: [toRow, toCol] 
            })
        });
        const result = await response.json();

        if (result.status === "success") {
            performSlide(fromRow, fromCol, toRow, toCol);
            updateMaterialDisplay(); // Update material after move
        } else {
            showStatus("Server: Invalid move.");
        }
    } catch (err) {
        showStatus("⚠️ Move failed: Server unreachable.")
    }
}

// nappuloiden siirto
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

        // update piece element coordinates
        pieceEl.dataset.row = toRow;
        pieceEl.dataset.col = toCol;

        // Update the Map
        pieceElements.delete(pieceKey);
        pieceElements.set(targetKey, pieceEl);

        // promotion logic
        let pieceName = gameState[fromRow][fromCol];
        
        // If White pawn reaches row 0 OR Black pawn reaches row 7
        if (pieceName === "white_pawn" && toRow === 0) {
            pieceName = "white_queen";
            pieceEl.textContent = PIECE_MAP["white_queen"];
        } else if (pieceName === "black_pawn" && toRow === 7) {
            pieceName = "black_queen";
            pieceEl.textContent = PIECE_MAP["black_queen"];
        }

        // Update logical state
        gameState[toRow][toCol] = gameState[fromRow][fromCol];
        gameState[fromRow][fromCol] = "";

        currentTurn = (currentTurn === "white") ? "black" : "white";
        console.log("Next turn:", currentTurn);
        
        // Update material display
        updateMaterialDisplay();
    }

    clearHighlights();
}

// square highlighting
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

// log container toggle (collapsing)
function toggleLog() {
    const log = document.getElementById("log-container");
    const icon = document.getElementById("log-toggle-icon");
    log.classList.toggle("collapsed");
    icon.textContent = log.classList.contains("collapsed") ? "▲" : "▼";
}

// adding a new log entry
function addLog(message, color = null) {
    const logBody = document.getElementById("log-body");
    if (!logBody) return;
    
    const entry = document.createElement("div");
    entry.classList.add("log-entry");
    
    if (color) {
        entry.style.color = color;
    }
    
    entry.textContent = message;
    logBody.prepend(entry); // Newest on top
    
    // Keep only last 50 entries
    while (logBody.children.length > 50) {
        logBody.removeChild(logBody.lastChild);
    }
}

// leave the room on refresh.
window.addEventListener('beforeunload', () => {
    if (currentRoom && playerSide) {
        const url = `${API_URL}/leave?room=${currentRoom}&side=${playerSide}`;
        fetch(url, { 
            method: 'POST', 
            keepalive: true,
            mode: 'no-cors'
        });
    }
});
