package LLD.chess.pieces;

import LLD.chess.Board;
import LLD.chess.Color;
import LLD.chess.Position;

public class Bishop extends Piece {
    public Bishop(Color color) {
        super(color);
    }

    @Override
    public Piece clone() {
        return new Bishop(getColor());
    }

    @Override
    public boolean isValidMove(Board board, Color color, Position from, Position to) {
        // validate move is diagonal
        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());
        if(rowDiff != colDiff){
            return false;
        }

        // validate path is clear
        int rowDirection = (to.getRow() - from.getRow()) / rowDiff;
        int colDirection = (to.getCol() - from.getCol()) / colDiff;

        for(int i=1; i<rowDiff - 1; i++){
            int intermediateRow = from.getRow() + i * rowDirection;
            int intermediateCol = from.getCol() + i * colDirection;
            if(board.getPieceAt(new Position(intermediateRow, intermediateCol)) != null){
                return false;
            }
        }

        // validate destination is not occupied by own piece
        if(board.getPieceAt(to) != null && board.getPieceAt(to).getColor().equals(getColor())){
            return false;
        }

        return true;
    }
}
