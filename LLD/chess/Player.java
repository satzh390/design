package LLD.chess;

public class Player {
    private Color color;
    private String name;
    private int id;

    public Player(int id, String name, Color color){
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public Color getColor(){
        return color;
    }

    public String getName(){
        return name;
    }

    public int getId(){
        return id;
    }
}
