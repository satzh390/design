package LLD.chess.pieces;

import LLD.chess.Board;
import LLD.chess.Color;
import LLD.chess.Position;

public class Queen extends Piece {

    public Queen(Color color) {
        super(color);
    }

    @Override
    public Piece clone() {
        return new Queen(getColor());
    }

    @Override
    public boolean isValidMove(Board board, Color color, Position from, Position to) {
        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());

        boolean isStraightMove = (rowDiff == 0 || colDiff == 0);
        boolean isDiagonalMove = (rowDiff == colDiff);

        // validate path is clear for straight move
        if(isStraightMove){ 
            int rowDirection = Integer.compare(to.getRow(), from.getRow());
            int colDirection = Integer.compare(to.getCol(), from.getCol());

            for(int i=1; i<Math.max(rowDiff, colDiff); i++){
                int intermediateRow = from.getRow() + i * rowDirection;
                int intermediateCol = from.getCol() + i * colDirection;
                if(board.getPieceAt(new Position(intermediateRow, intermediateCol)) != null){
                    return false;
                }
            }
        }

        // validate path is clear for diagonal move
        if(isDiagonalMove){
            int rowDirection = Integer.compare(to.getRow(), from.getRow());
            int colDirection = Integer.compare(to.getCol(), from.getCol());

            for(int i=1; i<rowDiff; i++){
                int intermediateRow = from.getRow() + i * rowDirection;
                int intermediateCol = from.getCol() + i * colDirection;
                if(board.getPieceAt(new Position(intermediateRow, intermediateCol)) != null){
                    return false;
                }
            }
        }

        return (isStraightMove || isDiagonalMove) && 
               (board.getPieceAt(to) == null || !board.getPieceAt(to).getColor().equals(getColor()));
    }
    
}
