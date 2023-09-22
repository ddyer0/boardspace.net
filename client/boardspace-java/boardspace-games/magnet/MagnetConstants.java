/* copyright notice */package magnet;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface MagnetConstants 
{	
	static String MagnetVictoryCondition = "Capture the opponent king";
	static String PlayState = "Place the magnet on any space that causes movement";
	static String SetupState = "Place your pieces on the starting spaces";
	static String SelectState = "Select which piece reaches the magnet";
	static String PromoteState = "Promote any of the pieces which moved";
	static String FirstSelectState = "Select which piece moves";
	static String NormalStartDescription = "Ready to start synchronous play";
	static String WaitForStartDescription = "Wait for the other player to finish setup";
	static String PlaceRandom = "Place the pieces randomly";
	class StateStack extends OStack<MagnetState>
	{
		public MagnetState[] newComponentArray(int n) { return(new MagnetState[n]); }
	}
	//
    // states of the game
    //
	public enum MagnetState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Confirm_Synchronous_Setup(ConfirmStateDescription,true,true),
	Confirm_Setup(ConfirmStateDescription,true,true),
	FirstSelect(FirstSelectState,false,false),
	Play(PlayState,false,false),
	Setup(SetupState,false,false),
	Synchronous_Setup(SetupState,false,false),
	WaitForStart(WaitForStartDescription,false,false),
	NormalStart(NormalStartDescription,false,false),
	Promote(PromoteState,true,true),
	Select(SelectState,false,false);
	MagnetState(String des,boolean done,boolean digest)
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
	public boolean simultaneousTurnsAllowed()
		{ switch(this)
			{
			case Confirm_Setup:
			case Setup:
			case WaitForStart:
			case NormalStart: return(true);
			default: return(false);
			}
		}
	};
	
	enum ChipId 
	{
		Red_Front_4("RF4"),
		Red_Front_3("RF3"),
		Red_Front_2("RF2"),
		Red_Front_1("RF1"),
		Blue_Front_4("BF4"),
		Blue_Front_3("BF3"),
		Blue_Front_2("BF2"),
		Blue_Front_1("BF1"),
		
		Red_Back_1("RB1"),
		Blue_Back_1("BB1"),
		
		Red_Back_2_2("RB2_2"),
		Red_Back_2x_2("RB2_2x"),
		Red_Back_2_1("RB2_1"),
		Red_Back_2x_1("RB2_1x"),

		Blue_Back_2_2("BB2_2"),
		Blue_Back_2x_2("BB2_2x"),
		Blue_Back_2_1("BB2_1"),
		Blue_Back_2x_1("BB2_1x"),

		Red_Back_3x_1("RB3x_1"),
		Red_Back_3x_2("RB3x_2"),
		Red_Back_3x_3("RB3x_3"),
		Red_Back_3_1("RB3_1"),
		Red_Back_3_2("RB3_2"),
		Red_Back_3_3("RB3_3"),
		
		Blue_Back_3x_1("BB3x_1"),
		Blue_Back_3x_2("BB3x_2"),
		Blue_Back_3x_3("BB3x_3"),
		Blue_Back_3_1("BB3_1"),
		Blue_Back_3_2("BB3_2"),
		Blue_Back_3_3("BB3_3"),
		
		Red_Back_4_1("RB4_1"),
		Red_Back_4_2("RB4_2"),
		Red_Back_4_3("RB4_3"),
		Red_Back_4_4("RB4_4"),
		
		Blue_Back_4_1("BB4_1"),
		Blue_Back_4_2("BB4_2"),
		Blue_Back_4_3("BB4_3"),
		Blue_Back_4_4("BB4_4"),
		
		Magnet("T");
		String shortName = null;
		ChipId(String n) { shortName = n; }
	}
    //	these next must be unique integers in the Magnetmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum MagnetId implements CellId
	{	
		
    	Blue_Chip_Pool("BR"), // positive numbers are trackable
    	Red_Chip_Pool("RR"),
    	Blue_Captures("BC"),
    	Red_Captures("RC"),
    	Magnet("M"),
    	ReverseViewButton(null),
    	BoardLocation("B"),
    	EphemeralDone(null),
    	RandomRect(null),
    	UnSelect(null),
    	;
		String shortName = name();
		public String shortName() { return(shortName); }
		MagnetId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public MagnetId find(String s)
    	{	
    		for(MagnetId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public MagnetId get(String s)
    	{	MagnetId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
//this would be a standard magnet-magnet board with 6-per-side
static int[] ZfirstInCol = {  5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 };
static int[] ZnInCol =     {  6, 7, 8, 9, 10, 11, 10, 9, 8, 7, 6}; // depth of columns, ie A has 4, B 5 etc.
//

 enum MagnetVariation
    {
    	magnet("magnet",ZfirstInCol,ZnInCol);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	MagnetVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static MagnetVariation findVariation(String n)
    	{
    		for(MagnetVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		
    		return(null);
    	}
     	
    }

     
    static void putStrings()
    {
    	String MagnetStrings[] = 
    		{  "Magnet",
    			PlayState,
    			PlaceRandom,
    			PromoteState,
    			SelectState,
    			WaitForStartDescription,
    	       MagnetVictoryCondition,
    	       NormalStartDescription,
    	       FirstSelectState,
    	       SetupState,
    			
    		};
    	String MagnetStringPairs[][] = 
    		{   {"Magnet_family","Magnet"},
    			{"Magnet_variation","Standard Magnet"},
    		};
    	InternationalStrings.put(MagnetStrings);
    	InternationalStrings.put(MagnetStringPairs);

    }

}