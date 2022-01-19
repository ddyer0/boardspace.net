package ponte;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface PonteConstants 
{	
	static final String Ponte_INIT = "ponte";	//init for standard game
    static final int White_Chip_Index = 0;
    static final int Red_Chip_Index = 1;
    static final int Bridge_Index = 2;

    static enum Bridge
    {	b_30(-2,-1,PonteId.Bridge_30),
    	b_45(-2,-2,PonteId.Bridge_45),
    	b_60(-1,-2,PonteId.Bridge_60),
    	b_90(0,-2,PonteId.Bridge_90),
    	b_120(1,-2,PonteId.Bridge_120),
    	b_135(2,-2,PonteId.Bridge_135),
    	b_150(2,-1,PonteId.Bridge_150),
    	b_180(-2,0,PonteId.Bridge_180);
    	public int otherEnddx = 0;
    	public int otherEnddy = 0;
    	public PonteId id = null;
    	Bridge(int dx,int dy,PonteId idc)
    	{	otherEnddx = dx;
    		otherEnddy = dy;
    		id = idc;
    		
    	}
    	public static Bridge find(String in)
    	{	for(Bridge b : values()) { if(b.toString().equalsIgnoreCase(in)) { return(b); }}
    		return(null);
    	}
    }
   
    //	these next must be unique integers in the dictionary
	static enum PonteId implements CellId
	{
		 
		 White_Chip_Pool("W",false),
		 Red_Chip_Pool("R",false),
		 Bridge_30("X_30",true),
		 Bridge_45("X_45",true),
		 Bridge_60("X_60",true),
		 Bridge_90("X_90",true),
		 Bridge_120("X_120",true),
		 Bridge_135("X_135",true),
		 Bridge_150("X_150",true),
		 Bridge_180("X_180",true),
		 BoardLocation(null,false),
		 BridgeEnd(null,false);
		 String shortName = null;
		 public String shortName() { return(shortName); }
		 PonteChip chip=null;
		 PonteId(String s,boolean b) 
		  	{
			shortName = s;
		  	bridge=b; 
		  	}
		 boolean bridge = false;

		 public static PonteId find(int id)
		 {	for(PonteId p : values()) { if(p.ordinal()==id) { return(p); }}
		 	throw G.Error(IdNotFoundError, id);
		 }
		 public static PonteId find(String str)
		 {	for(PonteId id : values()) { if(str.equals(id.name())) { return(id); }}
		 	throw G.Error(IdNotFoundError,str);
		 }
	}
    static final PonteId RackLocation[] =
    	{
    	PonteId.White_Chip_Pool,PonteId.Red_Chip_Pool,
    	PonteId.Bridge_30,
    	PonteId.Bridge_45,PonteId.Bridge_60,
    	PonteId.Bridge_90,PonteId.Bridge_120,
    	PonteId.Bridge_135,PonteId.Bridge_150,
    	PonteId.Bridge_180
    	};
    
    class StateStack extends OStack <PonteState>
	{
		public PonteState[] newComponentArray(int n) { return(new PonteState[n]); }
	}
	public enum PonteState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),
    	Resign(ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	PlayTilesOrBridge("Place 2 tiles or a bridge"),
    	PlayTiles("Place 2 tiles"),
    	PlaySecondTile("Place a second tile"),
    	PlayBridge("Place a bridge, or click on DONE to Pass"),
    	PlayOrSwap("Place two tiles or swap colors"),
    	ConfirmSwap(ConfirmSwapDescription),
    	PlayBridgeEnd("Place the other end of the bridge"),
    	DoNothing("You have no moves, click on DONE");
    	String description;
    	PonteState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    	public static void putStrings()
    	{
    		for(PonteState v : values())
    		{
    			InternationalStrings.put(v.description,v.description);
    		}
    	}
    }


	


}