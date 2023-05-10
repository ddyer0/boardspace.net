package online.game;

import java.awt.Point;

import lib.AR;
import lib.Digestable;
import lib.G;
import lib.IStack;
import lib.Plog;
import lib.Random;
/**
 * this is the recommended lowest level class for Boardspace boards.  
 * It contains only a few things that ought to be common to any game
 * board implementation.   Subclasses of this class add functions for 
 * various types of board geometry, and subclasses of those are generally
 * the actual classes for particular games.
 * <p>
 * The visible interface which is eventually implemented by a game
 * board is (@link BoardProtocol)
 * @author ddyer
 * @see gBoard
 * @see hexBoard
 * @see rectBoard
 * @see triBoard
 * @see squareBoard
 * @see circBoard
 */
public abstract class BaseBoard implements Opcodes,Digestable
{	public Object clone() { throw G.Error("Do not call"); }
    /**
     * the index of the current player
     */
	private String name = "main";
	public String getName() { return(name); }
	public void setName(String s) { name = s; }
    /**
     * return true if we're an upside down view
     * @return
     */
	public boolean reverseView() { return false; }

	@SuppressWarnings("deprecation")
	public String toString() { return("<"+getClass().getName()+" "+name+">"); }
	
	public int whoseTurn = -1; 		// player index who is to move next
	public int players_in_game = 2; // 2-6 players are supported
	private int colorMap[] = AR.intArray(players_in_game);
	public int[] getColorMap() { setColorMap(players_in_game); return(colorMap); }
	public void setColorMap(int map[])
	{	 colorMap = map==null ? AR.intArray(players_in_game) : map; 
	}
	public void setColorMap(int n) 
	{
		if((colorMap==null) || (colorMap.length<n)) { colorMap = AR.intArray(n); } 
	}
	public int nPlayers() { return(players_in_game); }
	public boolean permissiveReplay = false;
    /**
     * set the current player
     * @param who
     */
	public void setWhoseTurn(int who)
    {	
        whoseTurn = (who<0)?0:who;
    }
	/**
	 * the current move number
	 */
    public int moveNumber = -1;		// move number within the game for public display
    /** true if player[index] has won.  In multiplayer games this is replaced
     * in the board initialization with a longer array instead of 2.
     */
    public boolean win[] = new boolean[2];
    /**
     * return true if the player has won "now".  This is used in scoring 2 player games.
     * @param pl
     * @return true if this player has won
     */
    public boolean WinForPlayer(int pl) { return(win[pl]); }
    /**
     * the type of the current game, which is printed into game records
     * and used to create duplicates boards in the "same" mode.
     */
    public String gametype = null; 			// remembers the current variation
     
    /** in games with some random initialization, this is the integer used to see the
     * random number sequence.   This is normally supplied by the lobby when launching
     * the game.
     */
    public long randomKey = 0;

    public final void doInit(String typ)
    	{ 
    	  doInit(typ,randomKey); 
    	}

    public void doInit() { doInit(gametype,randomKey); }
    
    public final void doInit(String game,int rv) 
    { throw G.Error("can't exist, rv must be long"); 
    }
    /**
     * an mandatory method.  For games which always have 2 players, this
     * would be the normal "init to start" method.  Games with more players
     * or more initialization parameters must override this because this
     * two-argument method is called by clone() when copying a board
     * @param game
     * @param rv
     */
    public abstract void doInit(String game,long rv);

    /** return the current game move number.
     * this is mostly for show, but part of the robot move/unmove logic
     * requires that this number be unwound correctly.  Also, changes in
     * move number key line and column changes in the visible game log.
     * @return an integer
     */
	public int moveNumber() { return(moveNumber); }
	/** 
	 * return the index of the player whose turn it is.  Note that this
	 * is not strictly related to the position of players in other arrays.
	 * It's just the board's internal index.
	 * @return an integer index representing the current player
	 */
	public int whoseTurn() { return(whoseTurn); }
	/**
	 * return true if the board's current state should allow hitting the "done"
	 * key.  This method is normally overridden by the game's specific board class.
	 * @return true if the "done" button ought to be active
	 */
	public boolean DoneState() { return(false); }
	/** 
	 * true true if the board's current state should be digested and saved
	 * to check for repetitions.
	 * @return a hash code representing the board state
	 */
	public boolean DigestState() { return(DoneState()); }	// if this state should be digested.  Normally the same as DoneState()

	/**
	 * copy all relevant fields of this board from from_b
	 * @param from_b
	 */
	public void copyFrom(BaseBoard from_b)
	{	G.Assert(from_b != this, "can clone from myself");
		name = "copy "+from_b.name;
		revision = from_b.revision;
		clientRevisionLevel = from_b.clientRevisionLevel;
		players_in_game = from_b.players_in_game;
		colorMap = AR.copy(from_b.colorMap);
		doInit(from_b);
		whoseTurn = from_b.whoseTurn;
		moveNumber = from_b.moveNumber;
		permissiveReplay = from_b.permissiveReplay;
		AR.copy(win,from_b.win);
	}
	
	public long Digest(Random r)
	{
		long v = Digest(r,moveNumber);
		v ^= Digest(r,whoseTurn);
		v ^= Digest(r,revision);
		return v;
	}
	
	public void doInit(BaseBoard b)
	{
		doInit(b.gametype,b.randomKey);
	}
	/**
	 * assert that all relevant fields of this board are the same as from_b
	 * @param from_b
	 */
	public void sameboard(BaseBoard from_b)
	{	G.Assert(win.length==nPlayers(), "wrong size win array, set it up in doInit");
		G.Assert((gametype==from_b.gametype) || gametype.equals(from_b.gametype),"gametype matches");
		G.Assert(revision==from_b.revision,"revision mismatch local %s from %s",revision,from_b.revision);
		G.Assert(clientRevisionLevel==from_b.clientRevisionLevel,"clientRevision mismatch");
		G.Assert(randomKey==from_b.randomKey,"randomkey mismatch");
		G.Assert(whoseTurn==from_b.whoseTurn,"whoseTurn mismatch");
		G.Assert(moveNumber==from_b.moveNumber,"moveNumber mismatch");
    	G.Assert(getState()==from_b.getState(),"state mismatch, is %s expected %s",getState(),from_b.getState());
		G.Assert(AR.sameArrayContents(win,from_b.win), "win mismatch");
	}
	public interface BoardState
	{	
		static final String ConfirmSwapDescription = "Click on Done to confirm swapping colors";
		static final  String ResignStateDescription = "Click on Done to confirm your resignation";
		static final  String PassStateDescription = "You have no legal moves.  Click on Done to pass";
		static final  String PuzzleStateDescription = "Rearrange things any way you want to";
		static final  String DrawStateDescription = "Click on Done to end the game as a Draw";
		static final  String OfferDrawStateDescription = "Click on Done to offer a draw";
		static final  String AcceptDrawStateDescription = "Click Done to accept a draw";
		static final  String DeclineDrawStateDescription = "Click Done to decline a draw";
		static final  String OfferedDrawStateDescription = "You have been offered a draw - Accept or Decline";
		static final  String ConfirmStateDescription = "Click on Done to confirm this move";
		static final  String ConfirmPassDescription = "Click on Done to confirm passing";
		static final  String DrawDescription = "A draw is offered.  Accept or Decline";
		static final  String DrawOfferDescription = "Click on Done to offer a draw";
		static final  String AcceptDrawPending = "Click on Done to accept a draw";
		static final  String DeclineDrawPending = "Click on Done to decline a draw";
		static final  String GameOverStateDescription = "Game Over";
	    static final String PlaceTileMessage = "Place a tile";
	    static final String CheckStateExplanation = "Escape from check";

	    static final  String IllegalRepetitionStateDescription = "Illegal due to repetition, try something else or resign";
		static String ConfirmNewRolesDescription = "Click on Done to confirm the new role assignments";

		public boolean GameOver();
		public boolean Puzzle();
		public int ordinal();
		public boolean simultaneousTurnsAllowed();
		
		public static  final  String[] StateStrings = {
				ConfirmSwapDescription,
				GameOverStateDescription,
				CheckStateExplanation,
				PlaceTileMessage,
				IllegalRepetitionStateDescription,
				AcceptDrawPending,
				DeclineDrawPending,
				ResignStateDescription,
				PassStateDescription,
				PuzzleStateDescription,
				DrawStateDescription,
				OfferDrawStateDescription,
				AcceptDrawStateDescription,
				DeclineDrawStateDescription,
				ConfirmStateDescription,
				ConfirmPassDescription,
				OfferedDrawStateDescription,
				DrawDescription,
				DrawOfferDescription,
				ConfirmNewRolesDescription,
		};
	};
	

    public boolean GameOver() 
    { 	BoardState state = getState();
    	return(state.GameOver()); 
    }
    /**
     * get the board state, 
     */
    public abstract BoardState getState();



    /**
 	* Override this method to encode a cell position within the board. We optionally encode "within the board" coordinates separately so the
	* pointing can take into account features such as zoom, rotate, and perspective.
	* these are normally provided by gBoard, but for a few classes they are
	* not.  In those cases, the entire getBoardCoords and getScreenCoords
	* methods would normally be overridden, making these unnecessary.
	* Otherwise, these are necessary because the board itself is usually scalable
	* in size, so raw coordinates need to be scaled.
    * @param x
     * @param y
     * @param cellsize
     * @return a point representing the mouse position
     * @see #decodeCellPosition
     */
	public Point encodeCellPosition(int x,int y,double cellsize)
	{	return( new Point((int)(x/cellsize),(int)(y/cellsize)));
	}
	/**
	 * Override this method to decode an encoded board position.  
	 *
	 * @param x
	 * @param y
	 * @param cellsize
	 * @return a new "point" representing the mouse position 
	 * @see #encodeCellPosition
	 */
	public Point decodeCellPosition(int x,int y,double cellsize)
	{	return(new Point((int)(x*cellsize),(int)(y*cellsize)));
	}
	/**
	 * 
	 * this is a default-default that will probably not be used by anyone,
	 * but if it is used it should be only to affect the default position
	 * of statically placed sprites.
	 * @return an integer
	 */
	public int cellSize() { return(20); }
	
	public void setPermissiveReplay(boolean tf) { permissiveReplay = tf; }
    public long Digest(Random r,Digestable c) { return(c==null)?0:c.Digest(r); }
    public long Digest(Random r,int n) { return(r.nextLong()^n); }
    public long Digest(Random r,long n) { return(r.nextLong()^n); }
    public long Digest(Random r,boolean n) { return(r.nextLong()*(n?0:1)); }
    public long Digest(Random r,boolean n[]) { long v=0; for(boolean b : n) { v ^= Digest(r,b); } return(v); }
 
    
    public boolean sameContents(IStack a,IStack b)
    {
 	   return(a==null ? b==null : a.sameContents(b));
    }

   /**
    * Digest an array of cells in an alternate use
    * @param r
    * @param c
    * @return a long representing the alternate state of the array of cells
    */
   public long Digest(Random r,Digestable c[])
   {
	   long val = 0;
  		for(int i=0,lim=c.length;  i<lim; i++)
  		{
  			val ^= Digest(r,c[i]);
  		}
	   return(val);
   }

   public long Digest(Random r,Digestable[][]ar)
   {
	   long v=0;
	   for(Digestable[]aa : ar) { v ^= Digest(r,aa); }
	   return(v);
   }

   public long Digest(Random r,int[]arr)
   {
	   long v = 0;
	   for(int lim = arr.length-1; lim>=0; lim--) { v ^= r.nextLong()*arr[lim]; }
	   return(v);
   }
   		
	public void cantExecute(commonMove m)
	{	throw G.Error("Can't execute %s", m);
	}
 
	public void cantUnExecute(commonMove m)
	{	
		throw G.Error("Can't unExecute %s", m);
	}
	
	/*
	 * this scheme to downgrade revision to accomodate clients that are slightly out of date.
	 * requires a delicate dance.  Each client registers their maximum version as part of the
	 * primary registration of the player.   The check for restarting a game results in either
	 * a game record to be restored, which contains the version, or a not found, which means
	 * go ahead.  Before going, call checkClientRevision() to see if we need to downgrade
	 * the support level.  
	 * 
	 * This whole madness is needed for complex games like euphoria and viticulture, where there
	 * is a long tail of small bugs to be fixed.
	 * 
	 * To make this work, an individual game must implement getMaxRevisionLevel() to return the
	 * current maximum revision level, and in its doInit method must downgrade the revision
	 * level to getClientRevisionLevel() if necessary.  And (of course) do the right things
	 * if the revision is downgraded.
	 * 
	 * This is not quite a bulletproof scheme, because restarting a game with an older client
	 * might force a downgrade in the middle of a game.
	 * 
	 */
	private int clientRevisionLevel = -1;	// the lowest support level of all clients reporting
	public int revision = 0;				// the current active revision level
	
	// override this method in the game to be return(REVISION);
	public int getMaxRevisionLevel() { return(0); }	// default revision, most older and most simple games don't use it.
	public int getActiveRevisionLevel() { return(revision); }

	// called by the game doInit(..) to check for the need to downgrade
	public final int getClientRevisionLevel() { return(clientRevisionLevel); }
	
	// called by game initialization as each client checks in
	public void setClientRevisionLevel(int n) 
	{	Plog.log.addLog(G.uniqueName()," Set rev ",n," was ",clientRevisionLevel);
		if(n>=0)
			{ 
			  if(clientRevisionLevel==0) { clientRevisionLevel = n; }
		      else if(n<clientRevisionLevel) 
		      	{ 
		    	  if(started) { Plog.log.addLog(G.uniqueName()," already started, reduce to ",n); }
		    	  clientRevisionLevel = n;
		      	} 
			}
	}
	
	// call this when replaying a new game, so the client revision level won't be a factor
	public void resetClientRevision() 
	{ 	clientRevisionLevel = -1;
		started = false; 
	}
	
	// call this from the doInit with a new revision level
	public void adjustRevision(int n)
	{ revision = (clientRevisionLevel>=0 && clientRevisionLevel<n) ? clientRevisionLevel : n;
	}
	boolean started = false;
	// called from game initialization before the actual game starts.
	public boolean checkClientRevision()
	{	//G.print(G.uniqueName()+" Check rev from ",revision," to ",clientRevisionLevel);
		started = true;
		//Log.addLog("checkClientRevision");
		if(clientRevisionLevel>=0 && revision>=0 && clientRevisionLevel<revision) 
			{ Plog.log.addLog(G.uniqueName()," Reinit to change revision from ",revision," to ",clientRevisionLevel);
			  revision = clientRevisionLevel;
			  doInit(); 
			  return(true);
			}
		return(false);
	}
	public boolean canResign()
	{
		return(players_in_game<=2);
	}
}
