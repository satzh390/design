package LLD.chess.validators;

import LLD.chess.Board;
import LLD.chess.Move;

public class CheckValidator implements Validator {

    @Override
    public boolean validate(Board board, Move move) {
        Board tempBoard = board.clone();
        tempBoard.movePiece(move.getFrom(), move.getTo());
        return !tempBoard.isKingInCheck(move.getPlayer().getColor());
    }
    
}
