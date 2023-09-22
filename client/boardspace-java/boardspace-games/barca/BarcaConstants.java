/* copyright notice */package barca;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;

import online.game.BaseBoard.BoardState;


public interface BarcaConstants 
{	static final String BarcaVictoryCondition = "occupy 3 watering holes";
	static final String BarcaPlayState = "Move an animal";
	static final String BarcaEscapeState = "Escape! Move an animal out of danger";


 enum BarcaVariation
    {
    	barca("barca");
    	String name ;
    	// constructor
    	BarcaVariation(String n) 
    	{ name = n; 
   	}
    	// match the variation from an input string
    	static BarcaVariation findVariation(String n)
    	{
    		for(BarcaVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
	
    class StateStack extends OStack<BarcaState>
    {
    	public BarcaState[] newComponentArray(int n) { return(new BarcaState[n]); }
    }
    //
    // states of the game
    //
    public enum BarcaState implements BoardState
    {
    Puzzle(PuzzleStateDescription,false,false),
    Resign(ResignStateDescription,true,false),
    Draw(DrawStateDescription,true,true),
    Gameover(GameOverStateDescription,false,false),
    Confirm(ConfirmStateDescription,true,true),
    Play(BarcaPlayState,false,false),
    Escape(BarcaEscapeState,false,false);
    	
    BarcaState(String des,boolean done,boolean digest)
    {
    	description = des;
    	digestState = digest;
    	doneState = done;
    }
    boolean doneState;
    boolean digestState;
    String description;
    public boolean GameOver() { return(this==Gameover); }
    public String description() { return(description); }
    public boolean doneState() { return(doneState); }
    public boolean digestState() { return(digestState); }
    public boolean Puzzle() { return(this==Puzzle); }
    public boolean simultaneousTurnsAllowed() { return false; }
    };

//	these next must be unique integers in the Barcamovespec dictionary
//  they represent places you can click to pick up or drop a stone
enum BarcaId implements CellId
{
	White_Mouse("WM"),
	White_Lion("WL"),
	White_Elephant("WE"),
	Black_Mouse("BM"),
	Black_Lion("BL"),
	Black_Elephant("BE"),
	Reverse(null),
	BoardLocation(null), 
	ToggleEye(null);
	String shortName = name();
	public String shortName() { return(shortName); }
	BarcaId afraidOf;
	BarcaId(String sn)
	{ 	
		if(sn!=null) { shortName = sn; }
	}
	static public BarcaId find(String s)
	{	
		for(BarcaId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	public boolean afraidOf(BarcaId i) { 	return(i==afraidOf); }
	static {
		White_Mouse.afraidOf = Black_Lion;
		White_Lion.afraidOf = Black_Elephant;
		White_Elephant.afraidOf = Black_Mouse;
		Black_Mouse.afraidOf = White_Lion;
		Black_Lion.afraidOf = White_Elephant;
		Black_Elephant.afraidOf = White_Mouse;
	}

}
static void putStrings()
{
	final String BarcaStrings[] = 
		{  "Barca",
			BarcaPlayState,
	       BarcaEscapeState,
	       BarcaVictoryCondition
			
		};
	 final String BarcaStringPairs[][] = 
		{   {"Barca_family","Barca"},
			{"Barca_variation","Standard Barca"},
		};

	InternationalStrings.put(BarcaStrings);
	InternationalStrings.put(BarcaStringPairs);
}

}