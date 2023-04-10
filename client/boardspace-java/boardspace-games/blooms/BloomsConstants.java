package blooms;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;

import online.game.BaseBoard.BoardState;


public interface BloomsConstants 
{	
	static final String BloomsVictoryCondition = "control the most area";
	static final String BloomsPlayState = "Place a stone (either color) on any empty cell";
	static final String BloomsLastState = "Pass to end the game, or continue playing";
	static final String BloomPlay1State = "Place a stone (other color) or click on Done";
	static final String BloomPlay1CaptureState = "Complete a capture";
	static final String SelectEndDescription = "Agree on the endgame condition";
	static final String CaptureWinMessage = "First to capture #1 stones wins";
	static final String ShortGoalMessage = "Capture #1";
	static final String ShortTerritory = "Territory";
	static final String SelectGoalMessage = "Select the endgame condition";
	static final String ApproveMessage = "Approve this way to end the game";
	
 enum EndgameCondition 
 	{	
	 Territory(0),
	 Capture5(5),
	 Capture10(10),
	 Capture15(15),
	 Capture20(20),
	 Capture25(25),
	 Capture30(30);
	 int ncaptured = 0;
	 EndgameCondition(int n)
	 { ncaptured = n;
	 }
 };

 enum BloomsVariation
    {
	blooms_4("blooms-4",ZfirstInCol4,ZnInCol4),
 	blooms_5("blooms-5",ZfirstInCol5,ZnInCol5),
	blooms_6("blooms-6",ZfirstInCol6,ZnInCol6),
	blooms_7("blooms-7",ZfirstInCol7,ZnInCol7);
   	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	BloomsVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static BloomsVariation findVariation(String n)
    	{
    		for(BloomsVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

// this would be a standard board with 4-per-side
    static final int[] ZfirstInCol4 = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static final int[] ZnInCol4 = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.

// this would be a standard board with 5-per-side
    static final int[] ZfirstInCol5 = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
    static final int[] ZnInCol5 =     {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
  //this would be a standard board with 6-per-side
    static final int[] ZfirstInCol6 = {  5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 };
    static final int[] ZnInCol6 =     {  6, 7, 8, 9, 10, 11, 10, 9, 8, 7, 6}; // depth of columns, ie A has 4, B 5 etc.
//
// this would be a standard board with 7-per-side
    static final int[] ZfirstInCol7 = { 6, 5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 ,6};
	static final int[] ZnInCol7 =     { 7, 8, 9, 10, 11, 12, 13, 12, 11, 10, 9, 8, 7 }; // depth of columns, ie A has 4, B 5 etc.


class StateStack extends OStack<BloomsState>
{
	public BloomsState[] newComponentArray(int n) { return(new BloomsState[n]); }
}
//
// states of the game
//
public enum BloomsState implements BoardState
{
Puzzle(PuzzleStateDescription,false,false),
Draw(DrawStateDescription,true,true),
Resign(ResignStateDescription,true,false),
Gameover(GameOverStateDescription,false,false),
Confirm(ConfirmStateDescription,true,true),
SelectEnd(SelectEndDescription,true,true),
PlayLast(BloomsLastState,true,true),
PlayFirst(BloomsPlayState,false,false),
Play(BloomsPlayState,true,true),
Play1(BloomPlay1State,true,true),
Play1Capture(BloomPlay1CaptureState,false,false);
BloomsState(String des,boolean done,boolean digest)
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
public boolean simultaneousTurnsAllowed() { return(this==BloomsState.SelectEnd); }
};

//these next must be unique integers in the Bloomstypemovespec dictionary
//they represent places you can click to pick up or drop a stone
enum BloomsId implements CellId
{
Red_Chip_Pool("R"), // positive numbers are trackable
Green_Chip_Pool("G"),
	Blue_Chip_Pool("B"), // positive numbers are trackable
Orange_Chip_Pool("O"),
BoardLocation(null),
EmptyBoard(null), Select(null),Approve(null);
String shortName = name();
public String shortName() { return(shortName); }
BloomsId(String sn) { if(sn!=null) { shortName = sn; }}
static public BloomsId find(String s)
{	
	for(BloomsId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
	return(null);
}


}
	static void putStrings()
	{
	 String bloomsStrings[] = 
			{  "Blooms",
				BloomsPlayState,
				ApproveMessage,
		        BloomsVictoryCondition,
		        BloomPlay1State,
		        BloomsLastState,
		        BloomPlay1CaptureState,
		        SelectEndDescription,
		        CaptureWinMessage,
		        ShortGoalMessage,
		        ShortTerritory,
		        SelectGoalMessage,
		        "Blooms-4",
		        "Blooms-5",
		        "Blooms-6",
		        "Blooms-7",
				
			};
		 String bloomsStringPairs[][] = 
			{   {"Blooms_family","Blooms"},
				{"Blooms-4_variation","4x board"},
				{"Blooms-5_variation","5x board"},
				{"Blooms-6_variation","6x board"},
				{"Blooms-7_variation","7x board"},
			};
		 InternationalStrings.put(bloomsStrings);
		 InternationalStrings.put(bloomsStringPairs);
			

	}
}