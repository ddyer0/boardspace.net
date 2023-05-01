package ordo;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;

import online.game.BaseBoard.BoardState;

public interface OrdoConstants 
{	static String VictoryCondition = "Capture all, Disconnect your opponent, or reach the opposite side";
	static String VictoryConditionX = "Ordo-X: Capture all or reach the opposite side";
	static String SecondPlayDescription = "Make a second move, forward only";
	static String FirstPlayDescription= "Make a move, forward or sideways";
	static String ReconnectDescription = "Reconnect your pieces";
	static String RetainDescription = "Designate one group to keep";
	
	static enum Variation
	{	
		Ordo("ordo",10,8),
		OrdoX("ordox",10,8);
		int cols;
		int rows;
		String name;
		Variation(String n,int szx,int szy) 
			{
			 name = n; 
			 cols = szx;
			 rows = szy;
			}
		static Variation findVariation(String n)
    	{
			for(Variation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
	};
	
    
    class StateStack extends OStack<OrdoState>
	{
		public OrdoState[] newComponentArray(int n) { return(new OrdoState[n]); }
	} 

public enum OrdoState implements BoardState
{	Puzzle(PuzzleStateDescription),
	Draw(DrawStateDescription),				// involuntary draw by repetition
	Resign( ResignStateDescription),
	Gameover(GameOverStateDescription),
	Confirm(ConfirmStateDescription),
	OrdoPlay(FirstPlayDescription),
	OrdoPlay2(SecondPlayDescription),	// for ordox, second move
	OrdoRetain(RetainDescription),
	Reconnect(OrdoConstants.ReconnectDescription),
	DrawPending(DrawOfferDescription),		// offered a draw
	AcceptOrDecline(DrawDescription),		// must accept or decline a draw
	AcceptPending(AcceptDrawPending),		// accept a draw is pending
   	DeclinePending(DeclineDrawPending),		// decline a draw is pending
	;
	String description;
	OrdoState(String des)
	{	description = des;
	}
	public String getDescription() { return(description); }
	public boolean GameOver() { return(this==Gameover); }
	public boolean Puzzle() { return(this==Puzzle); }
	public boolean simultaneousTurnsAllowed() { return(false); }
}
public enum OrdoId implements CellId
{
//	these next must be unique integers in the dictionary
	Black_Chip_Pool("B"), // positive numbers are trackable
	White_Chip_Pool("W"),
    BoardLocation(null),
    ReverseViewButton(null),
    ToggleEye(null),
;
	String shortName = name();
	public String shortName() { return(shortName); }
	OrdoId(String sn) { if(sn!=null) { shortName = sn; }}
	static public OrdoId find(String s)
	{	
		for(OrdoId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}

 	
}

static void putStrings()
{
	// there should be a line in masterstrings.java which causes
	// these to be included in the upload/download process for 
	// translation.  Also a line in the viewer init process to
	// add them for debugging purposes.
		String CheckerStrings[] =
			{	
			"Ordo",
			VictoryCondition,
			SecondPlayDescription,
			FirstPlayDescription,
			ReconnectDescription,
			RetainDescription,
			VictoryConditionX,
			
		};
	// there should be a line in masterstrings.java which causes
	// these to be included in the upload/download process for 
	// translation.  Also a line in the viewer init process to
	// add them for debugging purposes.
		String CheckerStringPairs[][] = 
		{   {"Ordo_family","Ordo"},
			{"Ordo_variation","Standard Ordo"},
			{"OrdoX_variation","Extended Ordo"},
		};
		InternationalStrings.put(CheckerStrings);
		InternationalStrings.put(CheckerStringPairs);

}
}