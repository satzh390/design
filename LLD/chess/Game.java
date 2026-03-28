package LLD.chess;

import java.util.List;

import LLD.chess.validators.CheckValidator;
import LLD.chess.validators.MoveValidator;
import LLD.chess.validators.Validator;

public class Game {
    private Board board;
    private Player[] players;
    private int currentPlayerIdx;
    private GameState state;
    private List<Move> history;
    private List<Validator> validators;

    public Game(Player whitePlayer, Player blackPlayer){
        players = new Player[]{whitePlayer, blackPlayer};
        currentPlayerIdx = 0;
        board = new Board();
        state = GameState.INITIALIZED;
        validators = List.of(new MoveValidator(), new CheckValidator());
        history = new java.util.ArrayList<>();
    }


    public void move(Move move){
        if(state == GameState.ENDED){
            throw new IllegalStateException("Game has already ended");
        }

        if(players[currentPlayerIdx].getColor() != move.getPlayer().getColor()){
            throw new IllegalArgumentException("It's not the current player's turn");
        }
        
        for(Validator validator : validators){
            if(!validator.validate(board, move)){
                throw new IllegalArgumentException("Invalid move[" + validator.getClass().getSimpleName() + "]: " + move);
            }
        }

        board.movePiece(move.getFrom(), move.getTo());
        currentPlayerIdx = 1 - currentPlayerIdx; // Switch player
        history.add(move);
        if(state == GameState.INITIALIZED){
            state = GameState.ONGOING;
        }

        boolean isCheckmate = board.isCheckmate(players[currentPlayerIdx].getColor());
        if(isCheckmate){
            state = GameState.ENDED;
            System.out.println("Checkmate! " + move.getPlayer().getName() + " wins!");
        }
    }

}
