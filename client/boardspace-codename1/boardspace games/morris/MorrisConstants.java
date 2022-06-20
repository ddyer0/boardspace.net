package morris;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface MorrisConstants 
{	static String VictoryCondition = "Reduce your opponent to 2 men";
	static String MorrisPlaceDescription = "Place a man on the board";
	static String MorrisCaptureDescription = "Take one of your opponent's men";
	static String Morris9Rules = "9 men morris rules";
	static String MorrisMoveDescription = "MorrisMove a man";
	static public String MorrisStrings[] =
	{	"Morris",
		MorrisPlaceDescription,
		MorrisCaptureDescription,
		VictoryCondition
	};
	static public String MorrisStringPairs[][] = 
	{   {"Morris_family","Nine Men Morris"},
		{MorrisMoveDescription,"Move a man"},

		{"Morris_variation","Standard 9 Men Morris"},
		{"Morris-9","Nine Men Morris"},
		{"Morris-9_variation","Standard 9 Men Morris"},
		{Morris9Rules,"move to an adjacent space, try to make a row of 3"},
	};
	
	static enum Variation
	{	
		Morris_9(MorrisChip.american,Morris9Rules,"morris-9",7,false);
		int size;
		String name;
		MorrisChip banner;
		String rules;
		Variation(MorrisChip b,String r,String n,int sz,boolean fly) 
			{banner = b;
			 rules = r;
			 name = n; 
			 size = sz; 
			}
		static Variation findVariation(String n)
    	{
			for(Variation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		
    		return(null);
    	}
	};
	static int [][] AddLinks_9 = 
		{ 
		{'A',1,'D',1},
		{'A',1,'A',4},
		{'A',7,'A',4},
		{'A',7,'D',7},
		
		{'G',1,'D',1},
		{'G',1,'G',4},
		{'G',7,'G',4},
		{'G',7,'D',7},
		
		{'B',6,'D',6},
		{'D',6,'F',6},
		{'B',2,'D',2},
		{'D',2,'F',2},
		
		{'B',2,'B',4},
		{'B',4,'B',6},
		{'F',2,'F',4},
		{'F',4,'F',6},
		
		};
	static int [][] RemoveCells_9 = 
		{
		{'D',4},
		{'C',1},
		{'C',2},
		{'C',6},
		{'C',7},
		{'B',1},
		{'B',3},
		{'B',5},
		{'B',7},
		{'A',3},
		{'A',2},
		{'A',6},
		{'A',5},
		
		{'E',1},
		{'E',2},
		{'E',6},
		{'E',7},
		{'F',1},
		{'F',3},
		{'F',5},
		{'F',7},
		{'G',3},
		{'G',2},
		{'G',6},
		{'G',5},
		};
	
	enum MorrisId implements CellId
	{
    //	these next must be unique integers in the dictionary
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	Black_Captured("BC"),
    	White_Captured("WC"),
    	Display(null),
        BoardLocation(null),
   	;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	MorrisId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public MorrisId find(String s)
    	{	
    		for(MorrisId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public MorrisId get(String s)
    	{	MorrisId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
     	
	}
    static final int White_Chip_Index = 0;
    static final int Black_Chip_Index = 1;
    static final MorrisId RackLocation[] = { MorrisId.White_Chip_Pool,MorrisId.Black_Chip_Pool};
    static final MorrisId CaptureLocation[] = { MorrisId.White_Captured, MorrisId.Black_Captured };

    public class StateStack extends OStack<MorrisState>
	{
		public MorrisState[] newComponentArray(int n) { return(new MorrisState[n]); }
	}
    public enum MorrisState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),				// involuntary draw by repetition
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Play(MorrisMoveDescription),
    	Place(MorrisPlaceDescription),
    	Capture(MorrisCaptureDescription),
     	DrawPending(DrawOfferDescription),		// offered a draw
    	AcceptOrDecline(DrawDescription),		// must accept or decline a draw
    	AcceptPending(AcceptDrawPending),		// accept a draw is pending
       	DeclinePending(DeclineDrawPending),		// decline a draw is pending
    	;
    	String description;
    	MorrisState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


	
    static final String Morris_SGF = "Morris"; // sgf game name
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final String ImageDir = "/morris/images/";

}