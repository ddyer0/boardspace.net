package mbrane;

import java.awt.Color;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;

import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface MbraneConstants 
{	
	static String MbraneVictoryCondition = "control the most 3x3 regions";
	static String MbranePlayState = "Place a tile on any empty cell";
	static String MbranePlayOrSwapState = "Place a tile on any empty cell, or Swap Colors";
	static String SwitchMessage = "Switch sides, and play red using the current position";
	static String ScoreStateDescription = "No moves, click on Done to resolve the score";
	static String StartResolution = "Start the resolution phase";
	static String OkResolution = "Ok, Start the resolution phase";
	static String NoResolution = "No, keep playing";
	static String ProposeResolutionDescription = "decide to end the game or not";
	static String ConfirmPlayDescription = "Click on Done to resume normal playing";
	static String ConfirmScoreDescription = "Click on Done to start final score resolution";
	static String ConfirmResolveDescription = "Click on Done to offer to end the game now";


	static Color StandardRed = new Color(131,17,51);
	
	enum MbraneColor { Red(MbraneId.PlaceRed,StandardRed), Black(MbraneId.PlaceBlack,Color.black);
		MbraneId id=null;
		Color textColor = Color.black;
		MbraneChip instances[]=null;
		MbraneColor(MbraneId c,Color co)
		{ id=c;
		  textColor = co;
		}
	};

	class StateStack extends OStack<MbraneState>
	{
		public MbraneState[] newComponentArray(int n) { return(new MbraneState[n]); }
	}
	//
    // states of the game
    //
	public enum MbraneState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	ConfirmPlay(ConfirmPlayDescription,true,true),
	ConfirmProposeResolution(ConfirmResolveDescription,true,true),
	ProposeResolution(ProposeResolutionDescription,false,false),
	ConfirmScore(ConfirmScoreDescription,true,true),
	Score(ScoreStateDescription,true,true),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSwap(ConfirmSwapDescription,true,false),
	PlayOrSwap(MbranePlayOrSwapState,false,false),
	Play(MbranePlayState,false,false),
	PlayNoResolve(MbranePlayState,false,false);
	MbraneState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the Mbranemovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum MbraneId implements CellId
	{
    	Reserve_Pool("R"), // positive numbers are trackable
    	BoardLocation(null),
    	EmptyBoard(null),
    	PlaceRed(null),
    	PlaceBlack(null),
		StartResolution(null),
		OkResolution(null),
		NoResolution(null),
		;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	MbraneId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public MbraneId find(String s)
    	{	
    		for(MbraneId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public MbraneId get(String s)
    	{	MbraneId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}

 enum MbraneVariation
    {
    	Mbrane("Mbrane"),
	 	MbraneSimple("Mbrane-simple");
    	String name ;
    	// constructor
    	MbraneVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static MbraneVariation findVariation(String n)
    	{
    		for(MbraneVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		
    		return(null);
    	}
     	
    }
    static void putStrings()
    {
    	String MbraneStrings[] = 
    		{  "Mbrane",
    			MbranePlayState,
    	       SwitchMessage,
    	       ScoreStateDescription,
    	       MbranePlayOrSwapState,
    	       MbraneVictoryCondition,
    	       ConfirmPlayDescription,
    	       StartResolution,
    	       ConfirmResolveDescription,
    	       ConfirmScoreDescription,
    	       OkResolution,
    	       NoResolution
    			
    		};

    	String MbraneStringPairs[][] = 
    		{   {"Mbrane_family","Mbrane"},
    				{"Mbrane_variation","Standard Mbrane"},
    				{"Mbrane-Simple_variation","Simplified Mbrane"},
    			 {"Mbrane-Simple","Simplified Mbrane, without influence"}
    		};
    	InternationalStrings.put(MbraneStrings);
    	InternationalStrings.put(MbraneStringPairs);
    }

}