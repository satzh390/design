package LLD.ticTacToe;

import java.util.Arrays;

public class Board {
    private final Symbol[][] board;

    public Board() {
        board = new Symbol[3][3];
        for (int i = 0; i < 3; i++) {
            Arrays.fill(board[i], Symbol.EMPTY);
        }
    }

    public Symbol get(Position position) {
        return board[position.getRow()][position.getCol()];
    }

    public void set(Position position, Symbol symbol) {
        if (!isEmpty(position)) {
            throw new IllegalArgumentException("Position is already occupied");
        }

        board[position.getRow()][position.getCol()] = symbol;
    }

    public boolean isEmpty(Position position) {
        return get(position) == Symbol.EMPTY;
    }

    public boolean isAnyRowAndColumnSame(){
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != Symbol.EMPTY && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                return true;
            }
            if (board[0][i] != Symbol.EMPTY && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
                return true;
            }
        }

        return false;
    }

    public boolean isAnyDiagonalSame() {
        if (board[0][0] != Symbol.EMPTY && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            return true;
        }
        if (board[0][2] != Symbol.EMPTY && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            return true;
        }
        return false;
    }

    public boolean isFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == Symbol.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

}
