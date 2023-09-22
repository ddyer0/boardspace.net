/* copyright notice */package goban;

import lib.G;
import lib.InternationalStrings;
import lib.NameProvider;
import lib.OStack;
import lib.PopupManager;
import lib.CellId;
import online.game.BaseBoard.BoardState;

import online.game.commonMove;

public interface GoConstants 
{	static String VictoryCondition = "Surround the most territory";
	static String PlayDescription = "Place a stone on the board";
	static String ScoreDescription = "Designate dead stones, click on Done when finished";
	static String Play1Description = "Place a stone on the board, or pass to begin scoring";
	static String Score2Description = "Designate dead stones, or click on Done to end the game";
	static String UndoRequest = "please undo the last move";
	static String StartScoreMessage = "Begin Scoring";
	static String ResumePlayMessage = "Resume Playing";
	static String ConfirmHandicapStateDescription = "Click on Done to confirm your handicap";
	static String SetHandicapAction = "Set Handicap";
	static String NoHandicapAction = "No Handicap";
	static String SetKomiAction = "Set Komi";
	static String NoKomiAction = "No Komi";
	static String YesUndoMessage = "Yes, Undo";
	static String DenyUndoMessage = "Don't Undo";
	static String NumberView = "Show move numbers";
	static String NumberViewExplanation = "Show move numbers on top of stones";
	static String KomiPoints = "#1 points";
	static String ScoringFailedMessage = "Automatic scoring failed, click for details";
	/**
	 * NumberingMode encapsulates most of the behavior associated
	 * with choosing how move numbers are displayed on stones.  This
	 * enum also serves as a singleton class with a selected value
	 * with the ability to display a menu of choices.
	 */
	enum NumberingMode implements NameProvider
	{ None, All, Last, Last_5, Last_Branch, From_Here;
		public String getName() { return(toString()); }
		static PopupManager menu = null;
		static NumberingMode selected = None;
		static commonMove starting = null;
		static GoViewer viewer = null;
		// generate a pop-up menu of the choices
		static void showMenu(GoViewer v,int x,int y)
		{
			if(menu==null) { menu=new PopupManager(); }
			viewer = v;
			menu.newPopupMenu(v,v.deferredEvents);
			menu.addMenuItem(values());
			menu.show(x,y);
		}
		// handle the user clicking on one of the choices
		static boolean selectMenu(Object target)
		{
			if(menu!=null)
			{	if(menu.selectMenuTarget(target))
				{
				doSelection((NumberingMode)menu.rawValue);
				menu = null;
				return(true);	// we handled it
				}
			}
			return(false);
		}
		
		static void doSelection(NumberingMode sel)
		{
			selected = sel;
			switch(selected)
			{
			default: break;
			case From_Here:	
				{
				int pos = viewer.reviewMode() ? viewer.getReviewPosition() : viewer.History.size()-1;
				starting = viewer.History.elementAt(pos);
				}
				break;
			case Last_Branch:
				{
				int pos = viewer.reviewMode() ? viewer.getReviewPosition() : viewer.History.size()-1;
				while(--pos>0)
				{	commonMove m = viewer.History.elementAt(pos);
					if(m.nVariations()>1) { starting = m; break; }
				}
				}
				break;
				
			}
		}
		public static void putStrings()
		{	
			InternationalStrings.put(G.getNames(values()));
			
		}
	}
	
	static enum Variation
	{
		Go_19("go-19",19,GoChip.board_19,
				new int[][]{{2,'D',4,'P',16},
					 	  {3,'D',4,'P',16,'D',16},
						  {4,'D',4,'P',16,'D',16,'P',4},
					 	  {5,'D',4,'P',16,'D',16,'P',4,'J',10},
						  {6,'D',4,'P',16,'D',16,'P',4,'D',10,'P',10},
					 	  {7,'D',4,'P',16,'D',16,'P',4,'D',10,'P',10,'J',10},
					 	  {8,'D',4,'P',16,'D',16,'P',4,'D',10,'P',10,'J',4,'J',16},
					 	  {9,'D',4,'P',16,'D',16,'P',4,'D',10,'P',10,'J',4,'J',16,'J',10}
					 	  }),
		Go_13("go-13",13,GoChip.board_13,new int[][]{{2,'D',4,'J',10},
			 	  {3,'D',4,'J',10,'D',10},
				  {4,'D',4,'J',10,'D',10,'J',4},
			 	  {5,'D',4,'J',10,'D',10,'J',4,'G',7},
				  {6,'D',4,'J',10,'D',10,'J',4,'D',7,'J',7},
			 	  {7,'D',4,'J',10,'D',10,'J',4,'D',7,'J',7,'G',7},
			 	  {8,'D',4,'J',10,'D',10,'J',4,'D',7,'J',7,'G',4,'G',10},
			 	  {9,'D',4,'J',10,'D',10,'J',4,'D',7,'J',7,'G',4,'G',10,'G',7}
			 	  }),
		Go_11("go-11",11,GoChip.board_11,new int[][]{{2,'D',4,'H',8},
		 	  {3,'D',4,'H',8,'D',8},
			  {4,'D',4,'H',8,'D',8,'H',4},
		 	  {5,'D',4,'H',8,'D',8,'H',4,'F',6}}),
		Go_9("go-9",9,GoChip.board_9,new int[][]{{2,'D',4,'F',6},
		 	  {3,'D',4,'F',6,'D',6},
			  {4,'D',4,'F',6,'D',6,'F',4}});
		int size;
		String name;
		GoChip image;
		int handicaps[][] = null;
		Variation(String n,int sz,GoChip im,int[][]h) {name = n;  size = sz; image = im; handicaps=h; }
		public int[][]getHandicapValues() { return(handicaps); }
		static Variation findBySize(int n)
		{
			for(Variation s : values()) { if(s.size==n) { return(s); }}
			return(null);
		}
		static Variation findVariation(String n)
    	{	
			for(Variation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		
    		return(null);
    	}
	};
	enum ConnectCode { All,Double,None };

	// kind of space for endgame classification
	enum Kind
	{	
		Empty('.',true,false,false,null,null),	// not classified
		Liberty('L',true,false,false,null,null),	// liberties of black or white
		Dame('1',true,false,false,null,null),		// dame, exact fate indeterminate
		OutsideDame('2',true,false,false,null,null),	// dame to be fill by either black or white
		FillWhite('f',true,false,false,null,GoChip.white),	// empty cell that is filled by white
		FillBlack('g',true,false,false,null,GoChip.black),	// empty cell that is filled by black
		ReservedForWhite('F',true,false,false,null,GoChip.white),	// empty cell will be filled by white
		ReservedForBlack('G',true,false,false,null,GoChip.black),	// empty cell will be filled by black
		BlackTerritory('9',true,false,false,null,GoChip.black),	// empty spaces that are black territory
		WhiteTerritory('8',true,false,false,null,GoChip.white),	// empty spaces that are white territory
		BlackSnapbackTerritory('7',true,false,false,null,GoChip.black),	// empty spaces that are black territory
		WhiteSnapbackTerritory('6',true,false,false,null,GoChip.white),	// empty spaces that are white territory
		FalseEye('5',true,false,false,null,GoChip.white),	// false eye
		Black('B',false,true,false,GoChip.black,GoChip.black),		// black stones
		White('W',false,false,true,GoChip.white,GoChip.white),		// white stones
		DeadBlack('b',false,true,false,GoChip.black,GoChip.black),	// black stones that are dead
		DeadWhite('w',false,false,true,GoChip.white,GoChip.white),	// white stones that are dead
		RemovedBlack('c',false,true,false,GoChip.black,GoChip.black),	// black stones actually removed
		RemovedWhite('d',false,false,true,GoChip.white,GoChip.white),	// white stones actually removed
		SafeWhite('X',false,false,true,GoChip.white,GoChip.white),	// safe white stones	
		SafeBlack('C',false,true,false,GoChip.black,GoChip.black),	// safe black stones
		SekiBlack('S',false,false,true,GoChip.black,GoChip.black),	// black stones safe in a seki
		SekiWhite('s',false,false,true,GoChip.white,GoChip.white),	// white stones safe in a seki
		BlackAndEmpty('t',true,true,false,null,null),	// mix of black and empty spaces
		WhiteAndEmpty('x',true,false,true,null,null),	// mix of white and empty spaces

		WhiteDame('3',true,false,false,null,GoChip.white),	// dame that may be white territory
		BlackDame('4',true,false,false,null,GoChip.black);	// dame that may be black territory
		boolean includeEmpty;
		boolean includeBlack;
		boolean includeWhite;
		char ccode;
		GoChip chip;
		GoChip fillChip;
		Kind opposideKind;
	
		public boolean chipIsScanned(GoChip ch)
		{
			if((this==BlackDame) && (ch==GoChip.white)) { return(true); }
			if((this==WhiteDame) && (ch==GoChip.black)) { return(true); }
			return(chipIsIncluded(ch));
		}
		public boolean chipIsIncluded(GoChip ch)
		{
			if(ch==null) return(includeEmpty);
			if(ch==GoChip.black) return(includeBlack);
			if(ch==GoChip.white) return(includeWhite);
			G.Error("Not expecting %s",ch);
			return(false);
		}
		Kind(char cc,boolean ie,boolean ib,boolean iw,GoChip c,GoChip f)
		{	ccode = cc;				// code for compact 19x19 position
			chip = c;				// matching color for actual contents
			fillChip = f;			// matching color for filling/owning
			includeEmpty = ie;
			includeBlack = ib;
			includeWhite = iw;
		};
		public GoChip getIntronColor()
		{
			switch(this)
			{
			default: throw G.Error("Undefined for %s",this);
			case BlackAndEmpty: return(GoChip.black);
			case WhiteAndEmpty: return(GoChip.white);
			}
		}
		public boolean isSafeLiberty(GoChip ch)
		{	
			switch(this)
			{	
			case Dame: return(true); 
			case WhiteSnapbackTerritory:
			case WhiteTerritory:
			case WhiteDame:
			case BlackSnapbackTerritory:
			case BlackTerritory:
			case BlackDame:
				return(fillChip==ch);
			default: return(false);
			}
		}
		public Kind getBorderKind()
		{
			switch(this)
			{
			default: throw G.Error("Undefined for %s",this);
			case RemovedBlack: return(Kind.White);
			case RemovedWhite: return(Kind.Black);
			case BlackAndEmpty: return(Kind.White);
			case WhiteAndEmpty: return(Kind.Black);
			}
		}
		public boolean isNowConnected(GoChip ch)
		{
			if(fillChip==ch)
			{
			switch(this)
			{
			default: break;
			case ReservedForBlack:
			case FillBlack:
			case ReservedForWhite:
			case FillWhite:
				return(true); 
			}}
			return(false);
		}
		
		public boolean isNowEmptyOrConnected(GoChip ch)
		{
			if(isNowEmpty()) { return(true); }
			return(isNowConnected(ch));
		}
		public boolean isNowEmpty()
		{
			switch(this)
			{
			default: throw G.Error("not expecting %s",this);
			case BlackTerritory:
			case WhiteTerritory:
			case FalseEye:
			case BlackDame: 
			case WhiteDame:
			case Dame:
			case OutsideDame:
			case BlackSnapbackTerritory:
			case WhiteSnapbackTerritory:
			case RemovedBlack:
			case RemovedWhite:
			case ReservedForWhite:
			case ReservedForBlack:
				return(true);
			case DeadBlack:
			case DeadWhite:
			case SafeWhite:
			case SafeBlack:
			case White:
			case Black:
			case FillWhite:
			case FillBlack:
			case SekiBlack:
			case SekiWhite:
				return(false);
			}
		}
		public Kind getOppositeKind()
		{
			switch(this)
			{
			case DeadWhite: return(Kind.DeadBlack);
			case DeadBlack: return(Kind.DeadWhite);
			case RemovedWhite: return(Kind.RemovedBlack);
			case RemovedBlack: return(Kind.RemovedWhite);
			case Black:	return(Kind.White); 
			case White: return(Kind.Black);
			case BlackTerritory: return(Kind.WhiteTerritory);
			case WhiteTerritory: return(Kind.BlackTerritory);
			case SekiBlack: return(Kind.SekiWhite);
			case SekiWhite: return(Kind.SekiBlack);
			case BlackDame: return(Kind.WhiteDame);
			case WhiteDame: return(Kind.BlackDame);
			case SafeWhite: return(Kind.SafeBlack);
			case SafeBlack: return(Kind.SafeWhite);
			case BlackSnapbackTerritory: return(Kind.WhiteSnapbackTerritory);
			case WhiteSnapbackTerritory: return(Kind.BlackSnapbackTerritory);
			case FillWhite: return(Kind.FillBlack);
			case FillBlack: return(Kind.FillWhite);
			case ReservedForWhite: return(Kind.ReservedForBlack);
			case ReservedForBlack: return(Kind.ReservedForWhite);
			case FalseEye:
			default: return(this);
			
			}
		}
		public Kind getSekiKind()
		{
			switch(this)
			{
			case Black:
			case DeadBlack:
			case WhiteTerritory:
			case SekiBlack:
			case RemovedBlack:
			case FillBlack:
			case SafeBlack: return(Kind.SekiBlack);
			case White:
			case DeadWhite:
			case BlackTerritory:
			case SekiWhite:
			case RemovedWhite:
			case FillWhite:
			case SafeWhite: return(Kind.SekiWhite);
			default: throw G.Error("not expecting %s",this);
			}
		}
		public Kind getSafeKind()
		{	switch(this)
			{
			case Black:
			case DeadBlack:
			case RemovedBlack:
			case BlackTerritory:
			case FillBlack:
			case SafeBlack: return(Kind.SafeBlack);
			case White:
			case DeadWhite:
			case WhiteTerritory:
			case RemovedWhite:
			case FillWhite:
			case SafeWhite: return(Kind.SafeWhite);
			default: throw G.Error("not expecting %s",this);
			}
		}
		public Kind getRemovedKind()
		{
			switch(this)
			{case DeadBlack:
			 case SafeBlack:
			 case SekiBlack:
			 case RemovedBlack:
			 case Black: return(Kind.RemovedBlack);
			 case DeadWhite:
			 case SafeWhite:
			 case SekiWhite:
			 case RemovedWhite:
			 case White: return(Kind.RemovedWhite);
			 default: throw G.Error("not expecting %s",this);
			}
		}
		public Kind getDeadKind()
		{
			switch(this)
			{case DeadBlack:
			 case SafeBlack:
			 case SekiBlack:
			 case RemovedBlack:
			 case Black: return(Kind.DeadBlack);
			 case DeadWhite:
			 case SafeWhite:
			 case SekiWhite:
			 case RemovedWhite:
			 case White: return(Kind.DeadWhite);
			 default: throw G.Error("not expecting %s",this);
			}
		}
		public boolean isDead()
		{
			switch(this)
			{
			case RemovedWhite:
			case RemovedBlack:
			case DeadWhite:
			case DeadBlack:
				return(true);
			default: return(false);
			}
		}
		public Kind getTerritoryKind()
		{	switch(this)
			{
			case White:
			case SafeWhite:
			case BlackAndEmpty:
			case WhiteTerritory:
			case SekiWhite:
			case DeadWhite:
			case RemovedWhite:
			case FillWhite:
				return(Kind.WhiteTerritory);
			case Black:
			case SafeBlack:
			case WhiteAndEmpty:
			case BlackTerritory:
			case SekiBlack:
			case DeadBlack:
			case RemovedBlack:
			case FillBlack:
				return(Kind.BlackTerritory);
			default: throw G.Error("not expecting %s",this);
			}
		}
		
		public Kind getFillKind()
		{
			switch(this)
			{
			case Black:
			case SafeBlack:
			case DeadBlack:
			case SekiBlack:
				return(Kind.FillBlack);
			case White:
			case SafeWhite:
			case DeadWhite:
			case SekiWhite:
				return(Kind.FillWhite);
			default: throw G.Error("not expecting %s",this);
			}
		}
		static public Kind getKind(char c)
		{
			for(Kind v : values()) { if(v.ccode==c) { return(v); }}
			return(null);
		}
	
		public boolean isSafeTerritory()
		{
			switch(this)
			{
			case FalseEye:
			default: return(false); 
			case WhiteTerritory: return(true);
			case BlackTerritory: return(true);
			}
		}
	};
	

	enum GoId implements CellId
	{
    //	these next must be unique integers in the dictionary
    	Black_Chip_Pool("B",0), // positive numbers are trackable
    	White_Chip_Pool("W",1),
        BoardLocation(null,-1),
        ResumePlay(null,-1),
        ResumeScoring(null,-1),
        DoneScoring(null,-1),
        ReverseViewButton(null,-1),
        NumberViewButton(null,-1),
        HitUndoButton(null,-1),
        HitDenyUndoButton(null,-1),
        HitUndoActionButton(null,-1),
        HitSetHandicapButton(null,-1),
        HitSetKomiButton(null,-1),
        HitClassifyButton(null,-1),
        HitClassifyAllButton(null,-1),
        HitShowScoring(null,-1),
  	;
    	String shortName = name();
    	GoChip chip;
    	public String shortName() { return(shortName); }
    	int colorIndex = -1;
    	GoId(String sn,int id) { if(sn!=null) { shortName = sn; colorIndex = id; }}
    	static public GoId find(String s)
    	{	
    		for(GoId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public GoId get(String s)
    	{	GoId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
     	
	}
    public class StateStack extends OStack<GoState>
	{
		public GoState[] newComponentArray(int n) { return(new GoState[n]); }
	} 
    public enum GoState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	ConfirmHandicapState(ConfirmHandicapStateDescription),
    	Play(PlayDescription),
    	Score(ScoreDescription),
    	Score2(Score2Description),
    	Play1(Play1Description);
    	String description;
    	GoState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    	public boolean isScoring()
    	{
    		switch(this)
    		{
    		default: return(false);
    		case Score:
    		case Score2:
    			return(true);
    		}
    	}
    	public boolean isScoringOrOver() { return((this==Gameover) || isScoring()); }
    }


	
   

	static void putStrings()
	{
		String GoStrings[] =
			{	"Go",
				ScoringFailedMessage,
				YesUndoMessage,
				DenyUndoMessage,
				PlayDescription,
				Play1Description,
				ScoreDescription,
				Score2Description,
				UndoRequest,
				SetHandicapAction,
				NoHandicapAction,
				SetKomiAction,
				NoKomiAction,
				KomiPoints,
				VictoryCondition,
				StartScoreMessage,
				ResumePlayMessage,
				NumberViewExplanation,
				ConfirmHandicapStateDescription,
				NumberView,

			};
		 String GoStringPairs[][] = 
			{   {"Go_family","Go"},
				{"Go-19_variation","19x19 board"},
				{"Go-19","Go"},
				{"Go-9_variation","9x9 board"},
				{"Go-9","Go 9x9"},
				{"Go-13_variation","13x13 board"},
				{"Go-13","Go 13x13"},
				{"Go-11_variation","11x11 board"},
				{"Go-11","Go 11x11"},
				
			};
		 InternationalStrings.put(GoStrings);
		 InternationalStrings.put(GoStringPairs);
		 NumberingMode.putStrings();

	}
}