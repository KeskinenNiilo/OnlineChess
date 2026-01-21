#include <vector>
#include <map>
#include <string>
#include <memory>

struct Coord {
    int x; int y;
};

class Piece {
    public:
    std::string pieceColor;
    virtual std::vector<Coord> getMoves(std::shared_ptr<Piece> b[8][8], Coord piece) = 0;
    virtual ~Piece() {}; 
};

class Pawn : public Piece {
    public:
    Pawn(std::string color) {
        pieceColor = color;
    }
    std::vector<Coord> getMoves(std::shared_ptr<Piece> b[8][8], Coord piece) override {
        std::vector<Coord> moves;
        bool startPos = ((piece.y == 1 && pieceColor == "white") || (piece.y == 6 && pieceColor == "black"));
        if (pieceColor == "white") {
            if (piece.y + 1 < 8 && b[piece.x][piece.y + 1] == nullptr) {
                moves.push_back(Coord{piece.x, piece.y + 1});
                if (startPos && b[piece.x][piece.y + 2] == nullptr)
                    moves.push_back(Coord{piece.x, piece.y + 2});
            }
        } else {
            if (piece.y - 1 >= 0 && b[piece.x][piece.y - 1] == nullptr) {
                moves.push_back(Coord{piece.x, piece.y - 1});
                if (startPos && b[piece.x][piece.y - 2] == nullptr)
                    moves.push_back(Coord{piece.x, piece.y - 2});
            }
        }
        std::vector<Coord> eat = (pieceColor == "white") ? 
            std::vector<Coord>{{-1, 1}, {1, 1}} : 
            std::vector<Coord>{{-1, -1}, {1, -1}};

        for (Coord& offset : eat) {
            int targetX = piece.x + offset.x;
            int targetY = piece.y + offset.y;
            if (targetX >= 0 && targetX < 8 && targetY >= 0 && targetY < 8) {
                if (b[targetX][targetY] != nullptr && b[targetX][targetY]->pieceColor != this->pieceColor) {
                    // FIX: Push the target coordinate, not the relative offset
                    moves.push_back(Coord{targetX, targetY});
                }
            }
        }
        return moves;
    }
};

class Bishop : public Piece {
    public:
    Bishop(std::string color) {
        pieceColor = color;
    }
    std::vector<Coord> getMoves(std::shared_ptr<Piece> b[8][8], Coord piece) override {
        std::vector<Coord> moves;
        int xMove[4] = {1, -1, 1, -1};
        int yMove[4] = {1, 1, -1, -1};
        for (int i = 0; i < 4; ++i) {
            for (int j = 1; j < 8; ++j) {
                int x = piece.x + (xMove[i] * j);
                int y = piece.y + (yMove[i] * j);
                if (x < 0 || x >= 8 || y < 0 || y >= 8) break;
                if (b[x][y] == nullptr) moves.push_back(Coord{x, y});
                else {
                    if (b[x][y]->pieceColor != this->pieceColor) moves.push_back(Coord{x, y});
                    break;
                }
            }
        }
        return moves;
    }
};

class Rook : public Piece {
    public:
    Rook(std::string color) {
        pieceColor = color;
    }
    std::vector<Coord> getMoves(std::shared_ptr<Piece> b[8][8], Coord piece) override {
        std::vector<Coord> moves;
        int xMove[4] = {1, -1, 0, 0};
        int yMove[4] = {0, 0, 1, -1};
        for (int i = 0; i < 4; ++i) {
            for (int j = 1; j < 8; ++j) {
                int x = piece.x + (xMove[i] * j);
                int y = piece.y + (yMove[i] * j);
                if (x < 0 || x >= 8 || y < 0 || y >= 8) break;
                if (b[x][y] == nullptr) moves.push_back(Coord{x, y});
                else {
                    if (b[x][y]->pieceColor != this->pieceColor) moves.push_back(Coord{x, y});
                    break;
                }
            }
        }
        return moves;
    }
};

class Knight : public Piece {
    public:
    Knight(std::string color) {
        pieceColor = color;
    }
    std::vector<Coord> getMoves(std::shared_ptr<Piece> b[8][8], Coord piece) override {
        std::vector<Coord> moves;
        int xMove[] = {1, 1, -1, -1, 2, 2, -2, -2}; 
        int yMove[] = {2, -2, 2, -2, 1, -1, -1, 1};
        for (int i = 0; i < 8; ++i) {
            int x = piece.x + xMove[i];
            int y = piece.y + yMove[i];
            if ((x >= 0 && x < 8 && y >= 0 && y < 8) && 
            (b[x][y] == nullptr || b[x][y]->pieceColor != this->pieceColor)) moves.push_back(Coord{x, y});
        }
        return moves;
    }
};

class Chess {
    public:
    std::shared_ptr<Piece> Board[8][8];
    std::vector<int> blackCaptured; std::vector<int> whiteCaptured;
    std::string turn;
    bool whiteCheck; bool blackCheck;
    bool checkMate;
    Chess() {
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                Board[i][j] = nullptr;
            }
        }
        turn = "white";
        blackCaptured.clear();
        whiteCaptured.clear();
        whiteCheck = false;
        blackCheck = false;
        checkMate = false;
        for (int i = 0; i < 8; ++i) {
            Board[i][1] = std::make_shared<Pawn>("white");
            Board[i][6] = std::make_shared<Pawn>("black");
        }
    }
};