package LLD.chess.validators;

import LLD.chess.Board;
import LLD.chess.Move;

public interface Validator {
    public boolean validate(Board board, Move move);
}
