package LLD.chess.pieces;

import LLD.chess.Board;
import LLD.chess.Color;
import LLD.chess.Position;

public class Pawn extends Piece {
    public Pawn(Color color) {
        super(color);
    }

    @Override
    public Piece clone() {
        return new Pawn(getColor());
    }

    @Override
    public boolean isValidMove(Board board, Color color, Position from, Position to) {
        int rowDiff = to.getRow() - from.getRow();
        int colDiff = Math.abs(to.getCol() - from.getCol());

        // Check if the move is in the correct direction
        if (getColor() == Color.WHITE && (rowDiff != 1 && !(rowDiff == 2 && from.getRow() == 1))) {
            return false;
        }

        if (getColor() == Color.BLACK && (rowDiff != -1 && !(rowDiff == -2 && from.getRow() == 6))) {
            return false;
        }

        // Check for valid move (one step forward or two steps from starting position)
        if (colDiff == 0) {
            if (board.getPieceAt(to) != null && board.getPieceAt(to).getColor().equals(getColor())) {
                return false; // Can't move forward if there's a piece
            }

            if (Math.abs(rowDiff) == 2) {
                // Check if the path is clear for two-step move
                int intermediateRow = from.getRow() + (rowDiff / 2);
                if (board.getPieceAt(new Position(intermediateRow, from.getCol())) != null) {
                    return false; // Path is blocked
                }
            }

            return true; // Valid forward move
        } 
        
        if (colDiff == 1 && Math.abs(rowDiff) == 1) {
            // Check for diagonal capture
            Piece targetPiece = board.getPieceAt(to);
            return targetPiece != null && !targetPiece.getColor().equals(getColor());
        }

        return false; // Invalid move
    }
}
