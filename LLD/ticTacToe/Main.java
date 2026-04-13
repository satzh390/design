package LLD.ticTacToe;

public class Main {
    public static void main(String[] args) {
        Player player1 = new Player("Alice", Symbol.X);
        Player player2 = new Player("Bob", Symbol.O);

        Game game = new Game(player1, player2);
        // Simulate a game
        game.placeSymbol(new Position(0, 0)); // Alice places X at (0, 0)
        game.placeSymbol(new Position(1, 1)); // Bob places O at (1, 1)
        game.placeSymbol(new Position(0, 1)); // Alice places X at (0, 1)
        game.placeSymbol(new Position(1, 0)); // Bob places O at (1, 0)
        game.placeSymbol(new Position(0, 2)); // Alice places X at (0, 2) - Alice wins  

        if (game.isGameOver()) {
            if (game.isDraw()) {
                System.out.println("The game is a draw!");
            } else {
                System.out.println("The winner is: " + game.getWinner().getName());
            }
        }

        // simulate draw game
        Game drawGame = new Game(player1, player2);

        drawGame.placeSymbol(new Position(0, 0)); // X
        drawGame.placeSymbol(new Position(0, 1)); // O
        drawGame.placeSymbol(new Position(0, 2)); // X

        drawGame.placeSymbol(new Position(1, 1)); // O
        drawGame.placeSymbol(new Position(1, 0)); // X
        drawGame.placeSymbol(new Position(1, 2)); // O

        drawGame.placeSymbol(new Position(2, 1)); // X
        drawGame.placeSymbol(new Position(2, 0)); // O
        drawGame.placeSymbol(new Position(2, 2)); // X
        if (drawGame.isGameOver()) {
            if (drawGame.isDraw()) {
                System.out.println("The game is a draw!");
            } else {
                System.out.println("The winner is: " + drawGame.getWinner().getName());
            }
        }
    }
}
