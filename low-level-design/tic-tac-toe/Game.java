package LLD.ticTacToe;

public class Game {
    private Board board;
    private Player xPlayer;
    private Player oPlayer;
    private Player currentPlayer;
    private boolean isGameOver;
    private Player winner;

    public Game(Player xPlayer, Player oPlayer) {
        this.board = new Board();
        this.xPlayer = xPlayer;
        this.oPlayer = oPlayer;
        this.currentPlayer = xPlayer; // X always starts first
        this.isGameOver = false;
        this.winner = null;
    }

    public void placeSymbol(Position position) {
        if(isGameOver) {
            throw new IllegalStateException("Game is already over");
        }

        if(!board.isEmpty(position)) {
            throw new IllegalArgumentException("Position is already occupied");
        }
        
        board.set(position, currentPlayer.getSymbol());
        boolean isFull = board.isFull();
        if(board.isAnyRowAndColumnSame() || board.isAnyDiagonalSame() || isFull) {
            isGameOver = true;
            winner = isFull ? null : currentPlayer;
            return;
        } 

        currentPlayer = (currentPlayer == xPlayer) ? oPlayer : xPlayer;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public Player getWinner() {
        if (!isGameOver) {
            throw new IllegalStateException("Game is not over yet");
        }
        return winner;
    }

    public boolean isDraw() {
        if (!isGameOver) {
            throw new IllegalStateException("Game is not over yet");
        }
        return winner == null;
    }
}

