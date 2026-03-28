package LLD.chess;

public class Position implements Cloneable {
    private int row;
    private int col;

    public Position(int row, int col){
        this.row = row;
        this.col = col;
    }

    public int getRow(){
        return row;
    }

    public int getCol(){
        return col;
    }

    @Override
    public Position clone() {
        return new Position(getRow(), getCol());
    }

    public String toString(){
        return "(" + row + ", " + col + ")";
    }
}
