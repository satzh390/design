package LLD.chess;

public class Main {
    // create a simple game and make some moves and assert the state of the game after each move
    public static void main(String[] args) {
        Player whitePlayer = new Player(1, "Alice", Color.WHITE);
        Player blackPlayer = new Player(2, "Bob", Color.BLACK);
        Game game = new Game(whitePlayer, blackPlayer);

        game.move(new Move(whitePlayer, new Position(1, 4), new Position(3, 4))); // e2 to e4
        game.move(new Move(blackPlayer, new Position(6, 4), new Position(4, 4))); // e7 to e5
        game.move(new Move(whitePlayer, new Position(0, 6), new Position(2, 5))); // Ng1 to f3
        game.move(new Move(blackPlayer, new Position(7, 6), new Position(5, 5))); // Ng8 to f6
        game.move(new Move(whitePlayer, new Position(0, 5), new Position(3, 2))); // Bf1 to c4
        game.move(new Move(blackPlayer, new Position(7, 5), new Position(4, 2))); // Bf8 to c5
        game.move(new Move(whitePlayer, new Position(0, 4), new Position(1, 4))); // Ke1 to e2
        game.move(new Move(blackPlayer, new Position(7, 4), new Position(6, 4))); // Ke8 to e7
        game.move(new Move(whitePlayer, new Position(3, 2), new Position(6, 5))); // Bc4 to f7 (checkmate)
        if(game.getState() != GameState.ENDED){
            throw new AssertionError("Game should have ended with checkmate");
        }
        System.out.println("All moves executed successfully and game ended with checkmate as expected.");

        // invalid move after game ended
        try {
            game.move(new Move(blackPlayer, new Position(6, 4), new Position(5, 4))); // e7 to e6
            assert false : "Move should not be allowed after game has ended";
        } catch (IllegalStateException e) {
            System.out.println("Caught expected exception when trying to move after game ended: " + e.getMessage());
        }

        // initialize a new game and make an invalid move (moving opponent's piece)
        game = new Game(whitePlayer, blackPlayer);  
        try {
            game.move(new Move(blackPlayer, new Position(1, 4), new Position(3, 4))); // Black tries to move White's pawn
            assert false : "Move should not be allowed when it's not the player's turn";
        } catch (IllegalArgumentException e) {
            System.out.println("Caught expected exception when trying to move opponent's piece: " + e.getMessage());
        }

        // make a valid move and check game state
        game.move(new Move(whitePlayer, new Position(1, 4), new Position(3, 4))); // e2 to e4
        if(game.getState() != GameState.ONGOING){
            throw new AssertionError("Game state should be ongoing after first move");
        }
        System.out.println("First move executed successfully and game state is ongoing as expected.");
    }
}
