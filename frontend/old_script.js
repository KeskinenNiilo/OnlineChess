// script.js

const board = document.getElementById("chessboard");

// Unicode pieces: ♔ ♕ ♖ ♗ ♘ ♙ ♚ ♛ ♜ ♝ ♞ ♟
const initialBoard = [
    ["♜","♞","♝","♛","♚","♝","♞","♜"],
    ["♟","♟","♟","♟","♟","♟","♟","♟"],
    ["","","","","","","",""],
    ["","","","","","","",""],
    ["","","","","","","",""],
    ["","","","","","","",""],
    ["♙","♙","♙","♙","♙","♙","♙","♙"],
    ["♖","♘","♗","♕","♔","♗","♘","♖"]
];

function createBoard() {
    for (let row = 0; row < 8; row++) {
        for (let col = 0; col < 8; col++) {
            const square = document.createElement("div");
            square.classList.add("square");
            square.classList.add((row + col) % 2 === 0 ? "light" : "dark");
            square.dataset.row = row; // store row
            square.dataset.col = col; // store column

            if (initialBoard[row][col]) {
                const piece = document.createElement("div");
                piece.classList.add("piece");
                piece.textContent = initialBoard[row][col];

                // Only make pawns clickable for now
                if (piece.textContent === "♙" || piece.textContent === "♟") {
                    piece.addEventListener("click", (e) => {
                        e.stopPropagation(); // prevent triggering square clicks
                        selectPawn(row, col);
                    });
                }

                square.appendChild(piece);
            }

            board.appendChild(square);
        }
    }
}

createBoard();

//placeholder for deselecting, just click anywhere.
document.addEventListener("click", () => {
    clearHighlights();
});

function selectPawn(row, col) {
    clearHighlights(); // remove any previous highlights

    const piece = initialBoard[row][col];
    const moves = [];

    // Simple mock: white pawn
    if (piece === "♙") {
        if (row > 0 && !initialBoard[row - 1][col]) moves.push([row - 1, col]);
        if (row === 6 && !initialBoard[row - 1][col] && !initialBoard[row - 2][col]) moves.push([row - 2, col]);
    }

    // Simple mock: black pawn
    if (piece === "♟") {
        if (row < 7 && !initialBoard[row + 1][col]) moves.push([row + 1, col]);
        if (row === 1 && !initialBoard[row + 1][col] && !initialBoard[row + 2][col]) moves.push([row + 2, col]);
    }

    highlightSquares(moves);
}

function highlightSquares(moves) {
    moves.forEach(([row, col]) => {
        const square = board.querySelector(`.square[data-row='${row}'][data-col='${col}']`);
        if (square) square.classList.add("highlight");
    });
}

function clearHighlights() {
    board.querySelectorAll(".square.highlight").forEach(square => {
        square.classList.remove("highlight");
    });
}




