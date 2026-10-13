package LLD.chess.pieces;

import LLD.chess.Board;
import LLD.chess.Color;
import LLD.chess.Position;

public class King extends Piece {
    public King(Color color) {
        super(color);
    }

    @Override
    public Piece clone() {
        return new King(getColor());
    }

    @Override
    public boolean isValidMove(Board board, Color color, Position from, Position to) {
        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());

        return (rowDiff <= 1 && colDiff <= 1) && 
               (board.getPieceAt(to) == null || !board.getPieceAt(to).getColor().equals(getColor()));
    }
}
