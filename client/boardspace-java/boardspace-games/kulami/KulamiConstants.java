/* copyright notice */package kulami;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface KulamiConstants 
{	
	static String KulamiVictoryCondition = "control the most area";
	static String KulamiPlayState = "Place a marble on any empty cell";
	static String KulamiPlayOrSwapState = "Place a marble, or Swap Colors";
	static String SwitchMessage = "Switch sides, and play red using the current position";

	class StateStack extends OStack<KulamiState>
	{
		public KulamiState[] newComponentArray(int n) { return(new KulamiState[n]); }
	}
	//
    // states of the game
    //
	public enum KulamiState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSwap(ConfirmSwapDescription,true,false),
	PlayOrSwap(KulamiPlayOrSwapState,false,false),
	Play(KulamiPlayState,false,false);
	KulamiState(String des,boolean done,boolean digest)
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
		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
	};
	
    //	these next must be unique integers in the Kulamimovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum KulamiId implements CellId
	{
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	Red_Chip_Pool("W"),
    	SubBoard(null),
    	BoardLocation(null),
    	EmptyBoard(null),;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	KulamiId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public KulamiId find(String s)
    	{	
    		for(KulamiId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public KulamiId get(String s)
    	{	KulamiId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}

 enum KulamiVariation
    {
    	Kulami("Kulami"),
	 	Kulami_R("Kulami-R");
    	String name ;
    	// constructor
    	KulamiVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static KulamiVariation findVariation(String n)
    	{
    		for(KulamiVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers

    static void putStrings()
    {
    	String KulamiStrings[] = 
    		{  "Kulami",
    			KulamiPlayState,
    	       SwitchMessage,
    	       KulamiPlayOrSwapState,
    	       KulamiVictoryCondition
    			
    		};
    	 String KulamiStringPairs[][] = 
    		{   {"Kulami_family","Kulami"},
    			{"Kulami-R","Kulami Rectangular"},
    			{"Kulami_variation","8x8 board"},
    			{"Kulami-R_variation","8x10 board"},
    		};
    	 InternationalStrings.put(KulamiStrings);
    	 InternationalStrings.put(KulamiStringPairs);
    }
}