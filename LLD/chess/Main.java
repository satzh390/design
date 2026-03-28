package LLD.chess;

public class Main {
    // create a simple game and make some moves and assert the state of the game after each move
    public static void main(String[] args) {
        Player whitePlayer = new Player(1, "Alice", Color.WHITE);
        Player blackPlayer = new Player(2, "Bob", Color.BLACK);
        Game game = new Game(whitePlayer, blackPlayer);

        try {
            game.move(new Move(whitePlayer, new Position(1, 4), new Position(3, 4))); // e2 to e4
            game.move(new Move(blackPlayer, new Position(6, 4), new Position(4, 4))); // e7 to e5
            game.move(new Move(whitePlayer, new Position(0, 6), new Position(2, 5))); // Ng1 to f3
            game.move(new Move(blackPlayer, new Position(7, 1), new Position(5, 2))); // Nb8 to c6
            game.move(new Move(whitePlayer, new Position(0, 5), new Position(3, 2))); // Bf1 to c4
            game.move(new Move(blackPlayer, new Position(7, 6), new Position(5, 5))); // Ng8 to f6
            game.move(new Move(whitePlayer, new Position(0, 3), new Position(4, 7))); // Qd1 to h5 (check)
            game.move(new Move(blackPlayer, new Position(7, 3), new Position(3, 7))); // Qd8 to h4 (check)
            game.move(new Move(whitePlayer, new Position(0, 4), new Position(1, 4))); // Ke1 to e2 (escape check)
            game.move(new Move(blackPlayer, new Position(5, 5), new Position(3, 4))); // Nf6 to e4 (check)
            game.move(new Move(whitePlayer, new Position(1, 4), new Position(0, 4))); // Ke2 to e1 (escape check)
            game.move(new Move(blackPlayer, new Position(3, 7), new Position(0, 7))); // Qh4 to h1 (checkmate)
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
