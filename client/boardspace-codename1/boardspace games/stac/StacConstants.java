package stac;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface StacConstants 
{	static String VictoryCondition = "Claim 4 stacks of 3 discs";
	static String MoveMeepleDescription = "Move your meeple";
	static String MoveOrCarryDescription = "Move your meeple, or Carry a disk";
	static String DropDiscDescription = "Drop the Disc";
	static String StacStrings[] =
	{	"Stac",
		MoveMeepleDescription,
		MoveOrCarryDescription,
		DropDiscDescription,
		VictoryCondition
	};
	static String StacStringPairs[][] = 
	{   {"Stac_family","Stac"},
		{"Stac_variation","Standard Stac"},
	};
	
	static enum Variation
	{
		Stac("stac",5);
		int size;
		String name;
		Variation(String n,int sz) {name = n;  size = sz; }
		static Variation findVariation(String n)
    	{
			for(Variation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
	};
	
	enum StacId implements CellId
	{
    //	these next must be unique integers in the dictionary
    	Red_Chip_Pool("Red"),
    	Blue_Chip_Pool("Blue"),
        BoardLocation(null),
        ChipLocation(null),
        ReverseViewButton(null);
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	StacId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public StacId find(String s)
    	{	
    		for(StacId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public StacId get(String s)
    	{	StacId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
     	
	}
    static final int Red_Chip_Index = 0;
    static final int Blue_Chip_Index = 1;
    static final StacId RackLocation[] = { StacId.Red_Chip_Pool,StacId.Blue_Chip_Pool};
    
    class StateStack extends OStack <StacState>
	{
		public StacState[] newComponentArray(int n) { return(new StacState[n]); }
	}  
    public enum StacState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Play(MoveMeepleDescription),
    	Carry(MoveOrCarryDescription),
    	CarryDrop(DropDiscDescription),
       	DrawPending(DrawOfferDescription),
        AcceptOrDecline(DrawDescription),
    	AcceptPending(AcceptDrawPending),
    	RepetitionPending(AcceptDrawPending),
    	DeclinePending(DeclineDrawPending);
    	String description;
    	StacState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


	
    static final String Stac_SGF = "Stac"; // sgf game number allocated for hex
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final String ImageDir = "/stac/images/";

}