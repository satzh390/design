package LLD.chess.pieces;

import LLD.chess.Board;
import LLD.chess.Color;
import LLD.chess.Position;

public abstract class Piece implements Cloneable {
    private Color color;

    public Piece(Color color){
        this.color = color;
    }

    public Color getColor(){
        return color;
    }

    public abstract boolean isValidMove(Board board, Color color, Position from, Position to);

    public abstract Piece clone();
}
