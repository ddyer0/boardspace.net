package chess;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface ChessConstants 
{	static String VictoryCondition = "Checkmate your opponent's king";
	static String ChessMoveDescription = "Move a piece";


	static enum Variation
	{	
		
		Chess(ChessChip.standard,null,"chess",8),		// empty chess board size 8
		Ultima(ChessChip.ultima,null,"ultima",8),		// ultima chess
		Chess960(ChessChip.chess960,null,"chess960",8);	// chess 960
		int size;
		String name;
		ChessChip banner;
		String rules;
		Variation(ChessChip b,String r,String n,int sz) 
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
	
	enum ChessPiece
	{	// standard chess
		
		Pawn(1,"Pawn"),
		Rook(5,"Rook"),
		Knight(3,"Knight"),
		Bishop(3,"Bishop"),
		Queen(9,"Queen"),
		King(0,"King"),
		// ultima chess
		Immobilizer(10,"Immobilizer"),
		Withdrawer(6,"Withdrawer"),
		Chamelion(5,"Chamelion"),
		Coordinator(5,"Coordinator"),
		LongLeaper(4,"LongLeaper"),
		CustodialPawn(2,"PinchingPawn"),
		UltimaKing(0,"King");
		double value;
		String prettyName;
		ChessPiece(double v,String p) { value = v; prettyName = p; }
		
		public static void putStrings()
		{
			for(ChessPiece p : values())
			{	InternationalStrings.put(p.prettyName);
			}
		}
	};
	enum ChessId implements CellId
	{
    //	these next must be unique integers in the dictionary
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	Black_Captured("BC"),
    	White_Captured("WC"),
        BoardLocation(null),
        ReverseViewButton(null),
        ToggleEye(null),
  	;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	ChessId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public ChessId find(String s)
    	{	
    		for(ChessId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public ChessId get(String s)
    	{	ChessId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
     	
	}
    public class StateStack extends OStack<ChessState>
	{
		public ChessState[] newComponentArray(int n) { return(new ChessState[n]); }
	} 
    public enum ChessState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),				// involuntary draw by repetition
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Play(ChessMoveDescription),
    	Check(CheckStateExplanation),
    	DrawPending(DrawOfferDescription),		// offered a draw
    	AcceptOrDecline(DrawDescription),		// must accept or decline a draw
    	AcceptPending(AcceptDrawPending),		// accept a draw is pending
       	DeclinePending(DeclineDrawPending),		// decline a draw is pending
       	Filter("Filtering legal moves"),
    	;
    	String description;
    	ChessState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


    
    static void putStrings()
    {
    		String ChessStrings[] =
    		{	"Chess",
    			"Ultima",
    			ChessMoveDescription,
    			VictoryCondition,
    		};
    		String ChessStringPairs[][] = 
    		{   {"Chess_family","Chess"},
    			{"Chess_variation","Standard Chess"},
    			{"Ultima_variation","Ultima Chess"},
    			{"Ultima_family","Ultima"},
    			{"Chess960_variation","Chess 960"},
    		};
    		InternationalStrings.put(ChessStrings);
    		InternationalStrings.put(ChessStringPairs);
    		ChessPiece.putStrings();
    }
}