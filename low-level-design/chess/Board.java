package LLD.chess;

import LLD.chess.pieces.Bishop;
import LLD.chess.pieces.King;
import LLD.chess.pieces.Knight;
import LLD.chess.pieces.Pawn;
import LLD.chess.pieces.Piece;
import LLD.chess.pieces.Queen;
import LLD.chess.pieces.Rook;

public class Board implements Cloneable {
    private Piece[][] board;
    public static final int SIZE = 8;
    private Position whiteKingPosition;
    private Position blackKingPosition;

    public Board(){
        initializeBoard();
    }

    private Board(Piece[][] board){
        this.board = board;
    }

    private void initializeBoard(){
        board = new Piece[SIZE][SIZE];
        
        // initialize white pieces
        whiteKingPosition = new Position(0, 4);
        board[0][0] = new Rook(Color.WHITE);
        board[0][1] = new Knight(Color.WHITE);
        board[0][2] = new Bishop(Color.WHITE);
        board[0][3] = new Queen(Color.WHITE);
        board[0][4] = new King(Color.WHITE);
        board[0][5] = new Bishop(Color.WHITE);
        board[0][6] = new Knight(Color.WHITE);
        board[0][7] = new Rook(Color.WHITE);
        for(int i = 0; i < SIZE; i++){
            board[1][i] = new Pawn(Color.WHITE);
        }

        // initialize black pieces
        blackKingPosition = new Position(7, 4);
        board[7][0] = new Rook(Color.BLACK);
        board[7][1] = new Knight(Color.BLACK);
        board[7][2] = new Bishop(Color.BLACK);
        board[7][3] = new Queen(Color.BLACK);
        board[7][4] = new King(Color.BLACK);
        board[7][5] = new Bishop(Color.BLACK);
        board[7][6] = new Knight(Color.BLACK);
        board[7][7] = new Rook(Color.BLACK);
        for(int i = 0; i < SIZE; i++){
            board[6][i] = new Pawn(Color.BLACK);
        }
    }

    public Piece getPieceAt(Position position){
        return board[position.getRow()][position.getCol()];
    }

    private void setPieceAt(Position position, Piece piece){
        board[position.getRow()][position.getCol()] = piece;
        if(piece != null) {
            if(piece instanceof King){
                if(piece.getColor() == Color.WHITE){
                    whiteKingPosition = position;
                } else {
                    blackKingPosition = position;
                }
            }
        }
    }

    public void resetBoard(){
        initializeBoard();
    }

    public void move(Move move){
        if(!validate(move)){
            throw new IllegalArgumentException("Invalid move: " + move);
        }

        Piece piece = getPieceAt(move.getFrom());
        setPieceAt(move.getFrom(), null);
        setPieceAt(move.getTo(), piece);
    }

    private boolean validMoveAfterSimulate(Move move){
        Board tempBoard = clone();
        Piece piece = tempBoard.getPieceAt(move.getFrom());
        tempBoard.setPieceAt(move.getFrom(), null);
        tempBoard.setPieceAt(move.getTo(), piece);
        return !tempBoard.isKingInCheck(move.getPlayer().getColor());
    }

    private boolean validate(Move move) {
        if(!validPosition(move.getFrom()) || !validPosition(move.getTo())){
            return false;
        }

        Piece piece = getPieceAt(move.getFrom());
        if(piece == null || !piece.getColor().equals(move.getPlayer().getColor())){
            return false;
        }

        return piece.isValidMove(this, move.getPlayer().getColor(), move.getFrom(), move.getTo()) 
            && validMoveAfterSimulate(move);
    }


    private boolean validPosition(Position position){
        if(position.getRow() < 0 || position.getRow() >= 8 || position.getCol() < 0 || position.getCol() >= 8){
            return false;
        }

        return true;
    }

    private boolean isKingInCheck(Color color){
        Position kingPosition = color == Color.WHITE ? whiteKingPosition : blackKingPosition;
        if(kingPosition == null){
            throw new IllegalStateException("King not found for color: " + color);
        }

        for(int i = 0; i < SIZE; i++){
            for(int j = 0; j < SIZE; j++){
                Piece piece = board[i][j];
                if(piece != null && !piece.getColor().equals(color) 
                    && piece.isValidMove(this, piece.getColor(), new Position(i, j), kingPosition)){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Board clone() {
        Piece[][] newBoard = new Piece[SIZE][SIZE];
        Position newWhiteKingPos = null;
        Position newBlackKingPos = null;

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                Piece piece = this.board[i][j];
                if (piece != null) {
                    Piece clonedPiece = piece.clone(); // must deep clone position
                    newBoard[i][j] = clonedPiece;

                    if (clonedPiece instanceof King) {
                        if (clonedPiece.getColor() == Color.WHITE) {
                            newWhiteKingPos = new Position(i, j);
                        } else {
                            newBlackKingPos = new Position(i, j);
                        }
                    }
                }
            }
        }

        Board cloned = new Board(newBoard);
        cloned.whiteKingPosition = newWhiteKingPos;
        cloned.blackKingPosition = newBlackKingPos;

        return cloned;
    }

    public boolean isCheckmate(Color color) {
        if(!isKingInCheck(color)){
            return false;
        }

        // check if any move can save the king
        for(int i = 0; i < SIZE; i++){
            for(int j = 0; j < SIZE; j++){
                Piece piece = board[i][j];
                if(piece != null && piece.getColor() == color){
                    Position from = new Position(i, j);
                    for(int x = 0; x < SIZE; x++){
                        for(int y = 0; y < SIZE; y++){
                            Position to = new Position(x, y);
                            if(piece.isValidMove(this, color, from, to)){
                                Move move = new Move(new Player(0, "temp", color), from, to);
                                if(validMoveAfterSimulate(move)){
                                    return false; // found a move that can save the king
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }
}
