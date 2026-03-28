package LLD.chess.pieces;

import LLD.chess.Board;
import LLD.chess.Color;
import LLD.chess.Position;

public class Rook extends Piece {
    public Rook(Color color) {
        super(color);
    }

    @Override
    public Piece clone() {
        return new Rook(getColor());
    }

    @Override
    public boolean isValidMove(Board board, Color color, Position from, Position to) {
        if (from.getRow() != to.getRow() && from.getCol() != to.getCol()) {
            return false; // Rook moves only in straight lines
        }

        // Check if the path is clear
        int rowDirection = Integer.compare(to.getRow(), from.getRow());
        int colDirection = Integer.compare(to.getCol(), from.getCol());
        int steps = Math.max(Math.abs(to.getRow() - from.getRow()), Math.abs(to.getCol() - from.getCol())); 
        for (int i = 1; i < steps; i++) {
            int intermediateRow = from.getRow() + i * rowDirection;
            int intermediateCol = from.getCol() + i * colDirection;
            if (board.getPieceAt(new Position(intermediateRow, intermediateCol)) != null) {
                return false; // Path is blocked
            }
        }

        // Check if the destination square is empty or occupied by an opponent's piece
        return board.getPieceAt(to) == null || !board.getPieceAt(to).getColor().equals(getColor());
    }
}
