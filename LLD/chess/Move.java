package LLD.chess;

public class Move {
    private Position from;
    private Position to;
    private Player player;

    public Move(Player player, Position from, Position to){
        if(player == null || from == null || to == null){
            throw new IllegalArgumentException("All constructor arguments are required");
        }
        
        this.player = player;
        this.from = from;
        this.to = to;
    }

    public Player getPlayer(){
        return player;
    }

    public Position getFrom(){
        return from;
    }

    public Position getTo(){
        return to;
    }

    public String toString(){
        return player.getName() + ": " + from + " to " + to;
    }
}
