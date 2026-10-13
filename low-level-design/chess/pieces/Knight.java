package LLD.chess.pieces;

import LLD.chess.Board;
import LLD.chess.Color;
import LLD.chess.Position;

public class Knight extends Piece {
    public Knight(Color color) {
        super(color);
    }

    @Override
    public Piece clone() {
        return new Knight(getColor());
    }

    @Override
    public boolean isValidMove(Board board, Color color, Position from, Position to) {
        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());

        return ((rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)) && 
               (board.getPieceAt(to) == null || !board.getPieceAt(to).getColor().equals(getColor()));
    }
}
