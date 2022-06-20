package proteus;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface ProteusConstants 
{	static String VictoryCondition = "Satisfy the current victory condition";
	static String LineGoal = "ProteusLineGoal";
	static String ColorGoal = "ProteusColorGoal";
	static String ShapeGoal = "ProteusShapeGoal";
	static String NoGoal = "No goal has been set";
	static String ColorTrade = "ProteusColorTrade";
	static String ShapeTrade = "ProteusShapeTrade";
	static String PlayerTrade = "ProteusPlayerTrade";
	static String AdjacentMove = "ProteusAdjacentMove";
	static String SameMove = "ProteusSameMove";
	static String DifferentMove = "ProteusDifferentMove";
	static String NoMove = "No piece movement rule yet";
	static String NoWin = "No goal has been set";
	static String NoTrade = "No tile swap rule yet";
	
	public static void putStrings()
	{
	String ProteusStrings[] =
	{	"Proteus",
		VictoryCondition,
		NoWin,
		NoMove,
		NoTrade,
		"Move a piece or a swap two tiles",	
		NoGoal,
		"Place a tile or one of your pieces",
	   	"Win:",
       	"goal",
      	"Swap Tiles:",
      	"swap rule",
    	"Move Pieces:",
    	"piece movement",

	};
	String ProteusStringPairs[][] = 
	{   {"Proteus_family","Proteus"},
		{"Proteus_variation","Standard Proteus"},
		{LineGoal,"Win\nSame line"},
		{ColorGoal,"Win\nSame color"},
		{ShapeGoal,"Win\nSame shape"},
		{ColorTrade,"Swap Tiles\nsame color"},
		{ShapeTrade,"Swap Tiles\nsame shape"},
		{PlayerTrade,"Swap Tiles\nDifferent player"},
		{AdjacentMove,"Move Pieces\nAdjacent square"},
		{SameMove,"Move Pieces\nSame row/column"},
		{DifferentMove,"Move Pieces\nDifferent row/column"},
		
	};
		InternationalStrings.put(ProteusStrings);
		InternationalStrings.put(ProteusStringPairs);
	}
	static enum Variation
	{
		Proteus;
		Variation() {}
		static Variation findVariation(String n)
    	{
			for(Variation s : values()) { if(s.name().equalsIgnoreCase(n)) { return(s); }}
    		
    		return(null);
    	}
	};
	enum Goal { none(NoWin),
			    three(LineGoal),
			    color(ColorGoal),
			    shape(ShapeGoal);
			    String desc;
			Goal(String s) { desc = s; }
			};
	enum Move { none(NoMove),
		 king(AdjacentMove),
		 rook(SameMove),
		 bishop(DifferentMove);
		String desc;
		Move(String d) { desc = d; }
		};
	enum Trade { none(NoTrade),
		polarity(PlayerTrade),
		color(ColorTrade),
		shape(ShapeTrade);
		String desc;
		Trade(String d) { desc = d; };
	}
	
	// tiles and player chips have a shape.
	enum Shape { circle,
		triangle,
		square };
	// tiles have a color,
	enum PieceColor{  
		Move_Red,
		Trade_Blue,
		Goal_Yellow,
		Player_Black,	// players use a color too, but not really in the same universe
		Player_White };
	
	enum ProteusId implements CellId
	{
    //	these next must be unique integers in the dictionary
    	BlackChips("B"), // positive numbers are trackable
    	WhiteChips("W"),
    	BoardTile("BD"),
        BoardLocation("BD"),
        MainChips("M"),
  	;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	ProteusId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public ProteusId find(String s)
    	{	
    		for(ProteusId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public ProteusId get(String s)
    	{	ProteusId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
     	
	}
	class StateStack extends OStack <ProteusState>
	{
		public ProteusState[] newComponentArray(int n) { return(new ProteusState[n]); }
	}    
    public enum ProteusState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	ConfirmPlacement(ConfirmStateDescription),
    	Play("Move a piece or a swap two tiles"),
    	Pass(PassStateDescription),
    	Placement("Place a tile or one of your pieces");
    	
    	String description;
    	ProteusState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }



}