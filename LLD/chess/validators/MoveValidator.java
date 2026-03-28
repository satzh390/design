package LLD.chess.validators;

import LLD.chess.Board;
import LLD.chess.Move;
import LLD.chess.Position;
import LLD.chess.pieces.Piece;

public class MoveValidator implements Validator {

    @Override
    public boolean validate(Board board, Move move) {
        if(!isWithinBoard(move.getFrom()) || !isWithinBoard(move.getTo())){
            return false;
        }

        Piece piece = board.getPieceAt(move.getFrom());
        if(piece == null || !piece.getColor().equals(move.getPlayer().getColor())){
            return false;
        }

        return piece.isValidMove(board, move.getPlayer().getColor(), move.getFrom(), move.getTo());
    }

    private boolean isWithinBoard(Position position){
        return position.getRow() >= 0 && position.getRow() < Board.SIZE
            && position.getCol() >= 0 && position.getCol() < Board.SIZE;
    }
    
}
