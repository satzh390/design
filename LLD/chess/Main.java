package LLD.chess;

public class Main {

    public static void main(String[] args) {

        Player white = new Player(1, "Alice", Color.WHITE);
        Player black = new Player(2, "Bob", Color.BLACK);

        // =========================================================
        // 1. NORMAL GAME FLOW (no checkmate)
        // =========================================================
        Game game1 = new Game(white, black);

        game1.move(new Move(white, new Position(1, 4), new Position(3, 4))); // e4
        game1.move(new Move(black, new Position(6, 4), new Position(4, 4))); // e5
        game1.move(new Move(white, new Position(0, 6), new Position(2, 5))); // Nf3
        game1.move(new Move(black, new Position(7, 1), new Position(5, 2))); // Nc6

        if (game1.getState() != GameState.ONGOING) {
            throw new AssertionError("Game should be ongoing");
        }
        System.out.println("Test1 passed: Normal game flow");

        // =========================================================
        // 2. CHECK (NOT CHECKMATE)
        // =========================================================
        Game game2 = new Game(white, black);

        game2.move(new Move(white, new Position(1, 4), new Position(3, 4))); // e4
        game2.move(new Move(black, new Position(6, 3), new Position(4, 3))); // d5
        game2.move(new Move(white, new Position(0, 3), new Position(4, 7))); // Qh5 (check)

        if (game2.getState() == GameState.ENDED) {
            throw new AssertionError("Game should NOT be ended (only check)");
        }
        System.out.println("Test2 passed: Check detected (not mate)");

        // =========================================================
        // 3. REAL CHECKMATE (Scholar's Mate)
        // =========================================================
        Game game3 = new Game(white, black);

        game3.move(new Move(white, new Position(1, 4), new Position(3, 4))); // e4
        game3.move(new Move(black, new Position(6, 4), new Position(4, 4))); // e5
        game3.move(new Move(white, new Position(0, 3), new Position(4, 7))); // Qh5
        game3.move(new Move(black, new Position(7, 1), new Position(5, 2))); // Nc6
        game3.move(new Move(white, new Position(0, 5), new Position(3, 2))); // Bc4
        game3.move(new Move(black, new Position(7, 6), new Position(5, 5))); // Nf6
        game3.move(new Move(white, new Position(4, 7), new Position(6, 5))); // Qxf7# (mate)

        if (game3.getState() != GameState.ENDED) {
            throw new AssertionError("Game should have ended with checkmate");
        }
        System.out.println("Test3 passed: Checkmate detected");

        // =========================================================
        // 4. INVALID MOVE (moving opponent piece)
        // =========================================================
        Game game4 = new Game(white, black);

        try {
            game4.move(new Move(black, new Position(1, 4), new Position(3, 4)));
            throw new AssertionError("Should not allow moving opponent piece");
        } catch (IllegalArgumentException e) {
            System.out.println("Test4 passed: Invalid move rejected");
        }

        // =========================================================
        // 5. INVALID MOVE (blocked bishop)
        // =========================================================
        Game game5 = new Game(white, black);

        try {
            game5.move(new Move(white, new Position(0, 2), new Position(3, 5))); // blocked bishop
            throw new AssertionError("Blocked path should not be allowed");
        } catch (IllegalArgumentException e) {
            System.out.println("Test5 passed: Path blocking works");
        }

        // =========================================================
        // 6. MOVE AFTER GAME ENDED
        // =========================================================
        try {
            game3.move(new Move(black, new Position(6, 4), new Position(5, 4)));
            throw new AssertionError("Move should not be allowed after game ends");
        } catch (IllegalStateException e) {
            System.out.println("Test6 passed: No moves after game end");
        }

        // =========================================================
        // 7. KNIGHT JUMP TEST
        // =========================================================
        Game game6 = new Game(white, black);

        game6.move(new Move(white, new Position(0, 1), new Position(2, 2))); // knight jump

        if (game6.getState() != GameState.ONGOING) {
            throw new AssertionError("Knight move should be valid");
        }
        System.out.println("Test7 passed: Knight jump works");

        // =========================================================
        System.out.println("✅ All tests passed");
    }
}