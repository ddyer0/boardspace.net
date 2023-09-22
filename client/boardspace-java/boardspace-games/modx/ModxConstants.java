/* copyright notice */package modx;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface ModxConstants 
{	static String VictoryCondition = "Score the most \"5\" patterns";
	static String ModxMoveDescription = "Place an X piece";
	static String PlaceJokerMoveDescription = "Place a joker";
	static String ReplaceJokerDescription = "Replace a joker to the board";
	static public String ModxStrings[] =
	{	
		ModxMoveDescription,
		PlaceJokerMoveDescription,
		ReplaceJokerDescription,
		VictoryCondition
	};
	static public String ModxStringPairs[][] = 
	{   {"Modx","Mod X"},
		{"Modx_family","Mod X"},
		{"Modx_variation","Standard Mod X"},
	};
	
	static enum Variation
	{	
		
		Modx(null,null,"modx",8);		// empty modx board size 6
		int size;
		String name;
		ModxChip banner;
		String rules;
		Variation(ModxChip b,String r,String n,int sz) 
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
	
	enum ModxId implements CellId
	{
    //	these next must be unique integers in the dictionary
	   	Red_Chip_Pool("R",0),
    	Black_Chip_Pool("B",1), // positive numbers are trackable
     	Yellow_Chip_Pool("Y",2),
    	Orange_Chip_Pool("O",3),
    	
    	Red_Flat_Pool("RF",0),
    	Black_Flat_Pool("BF",1),
    	Yellow_Flat_Pool("YF",2),
    	Orange_Flat_Pool("OF",3),
    	
    	Joker_Pool("J",-1),
    	Blind_Pool("JB",-1),		// blind joker, used for special matching
    	
        BoardLocation(null,-1),
  	;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	int colorIndex;
    	ModxId(String sn,int id) { if(sn!=null) { shortName = sn; } ; colorIndex = id; }
    	static public ModxId find(String s)
    	{	
    		for(ModxId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public ModxId get(String s)
    	{	ModxId v = find(s);
     		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
     	
	}
	static final int nFlatsPerColor = 18;
	static final int nChipsPerColor = 14;
	static final int nJokers = 5;
	
    static final ModxId RackLocation[] = { ModxId.Red_Chip_Pool,ModxId.Black_Chip_Pool,
    		ModxId.Yellow_Chip_Pool,ModxId.Orange_Chip_Pool};
    static final ModxId FlatLocation[] = { ModxId.Red_Flat_Pool,ModxId.Black_Flat_Pool,
    		ModxId.Yellow_Flat_Pool,ModxId.Orange_Flat_Pool};
    
    public class StateStack extends OStack<ModxState>
	{
		public ModxState[] newComponentArray(int n) { return(new ModxState[n]); }
	} 
    public enum ModxState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	PlaceInitialJoker(PlaceJokerMoveDescription),
    	ReplaceJoker(ReplaceJokerDescription),
    	Play(ModxMoveDescription),
    	;
    	String description;
    	ModxState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


	
    static final String Modx_SGF = "Modx"; // sgf game name
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final String ImageDir = "/modx/images/";

}