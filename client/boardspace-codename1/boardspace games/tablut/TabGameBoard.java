package tablut;

import java.util.Hashtable;
import java.util.StringTokenizer;

import lib.*;
import online.game.*;

/**
 * TabGameBoard knows all about the game of Tablut, which is played
 * on a square board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class TabGameBoard extends rectBoard<TabCell> implements BoardProtocol,TabConstants
{	public static int REVISION = 101;		// revision 101 fixes the "no move" bug
	public int getMaxRevisionLevel() { return(REVISION); }
	private TablutState unresign;
	private TablutState board_state;
	CellStack animationStack = new CellStack();
	public TablutState getState() {return(board_state); }
	public void setState(TablutState st) 
	{ 	unresign = (st==TablutState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    
    // these four options determine the character of the game.
    public boolean Flagship_Wins_In_Corner = false;			// flagship wins only in the corner
    public boolean Flagship_Owns_Center = false;			// only king can occupy the center
    public boolean Flagship_Can_Capture = false;			// flagship can capture
    public boolean Flagship_Four_Sided_Capture = false;		// flagship captured on 4 sides only

    public boolean getOptionValue(TabId code)
    {
    	switch(code)
    	{
    	default: throw G.Error("not expecting");
    	case CornerWin: return(Flagship_Wins_In_Corner);
    	case FlagShipCaptures: return(Flagship_Can_Capture);
    	case ExclusiveCenter: return(Flagship_Owns_Center);
    	case FourSideCaptures: return(Flagship_Four_Sided_Capture);
    	}
    }
    public void setOptionValue(TabId code,TabId val)
    {
    	switch(code)
    	{
    	default: throw G.Error("not expecting");
    	case CornerWin: Flagship_Wins_In_Corner=val==TabId.True; break;
    	case FlagShipCaptures: Flagship_Can_Capture=val==TabId.True; break;
    	case ExclusiveCenter: Flagship_Owns_Center=val==TabId.True; break;
    	case FourSideCaptures: Flagship_Four_Sided_Capture=val==TabId.True;break;
    	}
    	SetCellProperties();
    }

    public String gameType()
    {	String val = gametype +" rev "+revision;
    	for(int i=0; i<optionNames.length; i++)
    	{	val += " "+optionNames[i].shortName+" "+getOptionValue(optionNames[i]);
    	}
    	return(val+" "+ENDOPTIONS);
     }

    public void parseOptions(StringTokenizer tok)
    {	Flagship_Wins_In_Corner = false;
    	Flagship_Owns_Center = false;
    	Flagship_Can_Capture = true;
    	Flagship_Four_Sided_Capture = false;	// standardize
    	boolean hasRev = false;
    	boolean hasSome = false;
    	while(tok.hasMoreTokens())
    	{	String m = tok.nextToken();
    		if(m.equalsIgnoreCase("rev")) { revision = G.IntToken(tok); hasRev = true; }
    		else if(! (".end.".equals(m) || ENDOPTIONS.equals(m))) 
    		{
    		TabId b = TabId.get(tok.nextToken());
    		hasSome = true;
    		setOptionValue(TabId.get(m),b);
    		}
    	}
    	if(canCaptureCompatibility) { Flagship_Can_Capture = true; }
    	if(captureCompatibility2) { Flagship_Owns_Center = true; }
    	if(hasSome && !hasRev) { revision = 100; }
    }
    //
    // private variables
    //
    private boolean changedOptions=false;
    public boolean changedOptions() { boolean c = changedOptions; changedOptions=false; return(c); }
    
    private int nRows=11;
    private int nCols=11;
     
    public TabChip playerChip[] = { TabChip.GoldShip,TabChip.SilverShip};
 	public TabChip getPlayerChip(int p) { return(playerChip[p]); }
    public void SetDrawState() 
    	{ setState(TablutState.DRAW_STATE); }
     
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public TabChip pickedObject = null;
	private TabChip lastPicked=null;		// index for last picked obect
	private TabCell pickedSource = null; 
	private TabCell droppedDest = null;
	
    private TabCell goldPool = null;	// dummy source for the chip pools
    private TabCell silverPool = null;
    public TabCell goldFlagPool = null;
    private TabCell pools[] = { silverPool,goldPool,goldFlagPool };
    public TabCell playerChipPool(int pl) 
    	{	return((playerChip[pl]==TabChip.SilverShip) ? silverPool : goldPool);
    	}
    public TabCell flagShipLocation = null;
    public int gold_ships = 0;
    public int silver_ships = 0;
    public int totalSweepScore = 0;
    public int open_ranks = 0;
    public TabChip lastDroppedDest = null;	// for image adjustment logic

    
    // this is used in puzzle state to keep placing more chips of the same
    // color as the last chip placed.
	public TabChip lastPicked() { return(pickedObject==null?lastPicked:pickedObject); }
	
	
    // temporary list of destination cells allocate as a resource for speed
    private TabCell[][]tempDestResource = new TabCell[6][];
    private int tempDestIndex=-1;
    public synchronized TabCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new TabCell[nCols+nRows]);
    	}
    public synchronized void returnTempDest(TabCell[]d) 
    	{ tempDestResource[++tempDestIndex]=d; 
    	}

	// factory method to generate a board cell
	public TabCell newcell(char c,int r)
	{	return(new TabCell(c,r));
	}
    public TabGameBoard(String init,int map[]) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = TABGRIDSTYLE;
        setColorMap(map);
        revision = REVISION;
        doInit(init); // do the initialization 
    }
    public TabGameBoard cloneBoard() 
	{ TabGameBoard dup = new TabGameBoard(gameType(),getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TabGameBoard)b); }
 
    // set the cell properties appropriate for the options
    // this determines which cells are wins for gold, and
    // which cells are interdicted to non-flagships.
    public void SetCellProperties()
	{
 	int span = nCols>9 ? 3:(nCols>7? 2 : 1);
	int center = nCols/2;
	changedOptions=true;
    for(TabCell c=allCells; c!=null; c=c.next)
	{ int col = c.col-'A';
	  int row = c.row-1;
	  if((row==0) 
				|| (row==(nRows-1))
				|| (col==0)
				|| (col==(nCols-1))) 
			  	{ c.winForGold=!Flagship_Wins_In_Corner; 
			  	}
	  if((col==center)&&(row==center)) { c.flagArea = Flagship_Owns_Center; c.centerSquare=true; }
	  else if(((col==0)||(col==(ncols-1)))
      		  && ((row==0)||(row==nRows-1))) 
	  	{ c.flagArea = Flagship_Wins_In_Corner; c.winForGold=true;
	  	}
	  else if((col>=(center-span))
      		  &&(col<=(center+span))
      		  &&(row<=(center+span))
      		  &&(row>=(center-span))) 
	  	{ c.centerArea = true; 
	  	}
	}
}
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String gamespec)
    {	StringTokenizer tok = new StringTokenizer(gamespec);
    	String game = tok.nextToken();
    	if(Tablut_11_INIT.equalsIgnoreCase(game)) { nCols = nRows = 11; }
    	else if(Tablut_9_INIT.equalsIgnoreCase(game)) { nCols = nRows = 9; }
    	else if(Tablut_7_INIT.equalsIgnoreCase(game)) { nCols = nRows = 7; }
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        parseOptions(tok);
        setState(TablutState.PUZZLE_STATE);
        initBoard(nCols,nRows); //this sets up a hexagonal board
        whoseTurn = FIRST_PLAYER_INDEX;
        gold_ships = silver_ships = 0;
        flagShipLocation = goldFlagPool;
        pickedSource = null;
        lastDroppedDest = null;
        int map[]=getColorMap();
        playerChip[map[0]]=TabChip.GoldShip;
        playerChip[map[1]]=TabChip.SilverShip;
		
		int span = nCols>9 ? 3:(nCols>7? 2 : 1);
		int center = nCols/2;

        // set the initial contents of the board to all empty cells
		for(TabCell c = allCells; c!=null; c=c.next)
		{  c.reInit();
		}
		for(int col=center-span;col<=center+span;col++)
		{	SetBoard((char)(col+'A'),center+1,TabChip.GoldShip);
			SetBoard((char)('A'+center),col+1,TabChip.GoldShip);
		}
		for(int col=center-span+1; col<=center+span-1;col++)
		{	SetBoard('A',col+1,TabChip.SilverShip);
			SetBoard((char)('A'+nCols-1),col+1,TabChip.SilverShip);
			SetBoard((char)('A'+col),1,TabChip.SilverShip);
			SetBoard((char)('A'+col),nCols,TabChip.SilverShip);
		}
		SetBoard('B',center+1,TabChip.SilverShip);
		SetBoard((char)('A'+nCols-2),center+1,TabChip.SilverShip);
		SetBoard((char)('A'+center),2,TabChip.SilverShip);
		SetBoard((char)('A'+center),nCols-1,TabChip.SilverShip);
		SetBoard((char)('A'+center),center+1,TabChip.GoldFlag);
  
		SetCellProperties();
  
    }
    public void sameboard(BoardProtocol f) { sameboard((TabGameBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TabGameBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the boards

        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        
        G.Assert(pickedObject==from_b.pickedObject, "pickedObject matches");
        G.Assert(flagShipLocation.sameCell(from_b.flagShipLocation),"flagship location");
        // here, check any other state of the board to see if
        G.Assert(gold_ships == from_b.gold_ships,"gold ships differ "+gold_ships+" "+from_b.gold_ships);
        G.Assert(silver_ships == from_b.silver_ships,"silver ships differ "+silver_ships+" "+from_b.silver_ships);
        G.Assert(Flagship_Wins_In_Corner == from_b.Flagship_Wins_In_Corner,
        	"flagship wins differs "+Flagship_Wins_In_Corner+" "+from_b.Flagship_Wins_In_Corner);
        G.Assert(Flagship_Owns_Center == from_b.Flagship_Owns_Center,
        	"flagship owns differs "+Flagship_Owns_Center+" "+from_b.Flagship_Owns_Center);
        G.Assert(Flagship_Can_Capture == from_b.Flagship_Can_Capture,
        	"flagship capture differs "+Flagship_Can_Capture+" "+from_b.Flagship_Can_Capture);
        G.Assert(Flagship_Four_Sided_Capture == from_b.Flagship_Four_Sided_Capture,
        	"flagship fourside differs "+Flagship_Four_Sided_Capture+" "+from_b.Flagship_Four_Sided_Capture);
    }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
     * @return
     */
    public long Digest()
    {
    	Random r = new Random(64 * 1000); // init the random number generator
    	long v = super.Digest(r);
        v ^= playerChip[0].Digest();
        v ^= r.nextLong()*((Flagship_Wins_In_Corner?1:0) 
        					| (Flagship_Owns_Center?2:0)
        					| (Flagship_Can_Capture?4:0)
        					| (Flagship_Four_Sided_Capture?8:0));


 		v ^= chip.Digest(r,pickedObject);
        v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TabGameBoard from_b)
    {	super.copyFrom(from_b);

        gold_ships = from_b.gold_ships;
        silver_ships = from_b.silver_ships;
        pickedObject = from_b.pickedObject;
        
        AR.copy(playerChip,from_b.playerChip);

        {TabCell c = from_b.flagShipLocation;
         if(c==from_b.goldFlagPool) { flagShipLocation=goldFlagPool; }
          else 
        	{flagShipLocation =getCell(c.col,c.row); 
        	}
        }
        Flagship_Wins_In_Corner = from_b.Flagship_Wins_In_Corner;
        Flagship_Owns_Center = from_b.Flagship_Owns_Center;
        Flagship_Can_Capture = from_b.Flagship_Can_Capture;
        Flagship_Four_Sided_Capture = from_b.Flagship_Four_Sided_Capture;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest= getCell(from_b.droppedDest);
        sameboard(from_b);
    }
    public TabCell getCell(TabCell c)
    {	if(c==null) { return null; }
		switch(c.rackLocation())
		{
		case GoldShipLocation:
			return goldPool;
		case SilverShipLocation:
			return silverPool;
		default:
			if(c.onBoard) { return getCell(c.col,c.row); }
			throw G.Error("getCell not handled %s",c);
		}

    }
    public void doInit() { doInit(gameType(),randomKey,revision); }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	doInit(gtype,key,revision);
    }
    public void doInit(String gtype,long key,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	Random r = new Random(645462);
        goldPool = new TabCell(r,TabChip.GoldShip,TabId.GoldShipLocation);	// dummy source for the chip pools
        silverPool = new TabCell(r,TabChip.SilverShip,TabId.SilverShipLocation);
        goldFlagPool = new TabCell(r,TabChip.GoldFlag,TabId.GoldFlagLocation);
        pools[0]=silverPool;
        pools[1]=goldPool;
        pools[2]=goldFlagPool;
        Init_Standard(gtype);
        allCells.setDigestChain(r);
        moveNumber = 1;
        droppedDest = null;
        pickedSource = null;
        lastPicked = null;
        pickedObject = null;
        

        // note that firstPlayer is NOT initialized here
    }

 
    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case PUZZLE_STATE:
            break;
        case REARRANGE_GOLD_STATE:
        case REARRANGE_SILVER_STATE:
        case CONFIRM_SWAP_STATE:
        case CONFIRM_STATE:
        case CONFIRM_NOSWAP_STATE:
        case DRAW_STATE:
        case RESIGN_STATE:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(nextPlayer[whoseTurn]);
            return;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	
        switch (board_state)
        {
        case RESIGN_STATE:
        case REARRANGE_GOLD_STATE:
        case REARRANGE_SILVER_STATE:
        case CONFIRM_SWAP_STATE:
        case CONFIRM_STATE:
        case CONFIRM_NOSWAP_STATE:
        case DRAW_STATE: 
            return (true);

        default:
            return (false);
        }
    }
   // states which should be digested.  This doesn't include the "rearrange" states.
   public boolean DigestState()
   {   
   	   switch(board_state)
	   {
       case REARRANGE_GOLD_STATE:
       case REARRANGE_SILVER_STATE:
    	   	return(false);
	default:
		break;
	   }
   		return(DoneState());
   }


 /**
  * In our implementation, the letter side(a-k) is black
  * and the number side (1-11) is white.  Either player can be playing either color.
  * @param ind
  * @return
  */ 
    public boolean WinForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	if(playerChip[player]==TabChip.SilverShip)
    	{return((flagShipLocation==goldFlagPool) || NoMovesForPlayer(nextPlayer[player]));
    	}
    	else if(playerChip[player]==TabChip.GoldShip)
    	{return(flagShipLocation.winForGold || NoMovesForPlayer(nextPlayer[player]));
    	}
    	else 
    	{ throw G.Error("Not expecting playerchip "+playerChip[player]);
    	}
     }


    public TabChip  SetBoard(char col,int row,TabChip newc)
    {	TabCell c = getCell(col,row);
    	return(SetBoard( c, newc));
    }
    // set the contents of a cell, and maintain the books
    public TabChip SetBoard(TabCell c,TabChip newc)
    {	TabChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(old==TabChip.GoldShip) { gold_ships--; }
    	else if(old==TabChip.SilverShip) { silver_ships--; }
    	else if((old==null)||(old==TabChip.GoldFlag)) {}
    	else { throw G.Error("Unexepected old chip "+old);}

    	c.chip=newc;
    	
       	if(newc==TabChip.GoldShip) { gold_ships++; }
    	else if(newc==TabChip.SilverShip) { silver_ships++; }
    	else if(newc==TabChip.GoldFlag) { flagShipLocation=c; }
    	else if(newc==null) {}
    	else { throw G.Error("Unexepected new chip "+newc);}
    	}
    	return(old);
    }
    //
    // accept the current placements as permanant
    //
    public void acceptPlacement()
    {	if(droppedDest!=null)
    	{
        droppedDest = null;
        pickedSource = null;
    	pickedObject=null;
    	}
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	if(droppedDest!=null) 
    	{	pickedObject = droppedDest.topChip();
    		SetBoard(droppedDest,null); 
    		droppedDest = null;
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	if(pickedSource!=null) 
    		{ SetBoard(pickedSource,pickedObject); 
    		  pickedSource = null;
    		}
    pickedObject = null;
    }

    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TabCell c)
    {	return(droppedDest==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    { 	TabChip ch = pickedObject;
    	if((ch!=null) && (pickedSource!=null) && (droppedDest==null))
    	{	return(pickedObject.index);
    	}
      	return (NothingMoving);
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickFromBoard(char col, int row)
    {
       	TabCell c = getCell(col,row);
       	boolean wasDest = isDest(c);
       	unDropObject(); 
       	if(!wasDest)
       	{
        pickedSource = c;
       	pickedObject = c.topChip();
       	droppedDest = null;
		lastPicked=pickedObject;
		SetBoard(c,null);
       	}
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(TabCell c)
    {	return(c==pickedSource);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_STATE:
        case CONFIRM_NOSWAP_STATE:
        	if(droppedDest==null)
        	{setNextStateAfterDone();
        	}
        	break;
        case PLAY_STATE:
        	setState(TablutState.CONFIRM_STATE);
        	break;
        case PLAY_OR_SWAP_STATE:
			setState(TablutState.CONFIRM_NOSWAP_STATE);
			break;
        case PUZZLE_STATE:
        case REARRANGE_GOLD_STATE:
        case REARRANGE_SILVER_STATE:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case DRAW_STATE:
    		setState(TablutState.GAMEOVER_STATE);	//end as a draw
    		break;
    	case CONFIRM_SWAP_STATE: 
    	case CONFIRM_NOSWAP_STATE:
    	case CONFIRM_STATE:
     	case PLAY_STATE:
    		setState(TablutState.PLAY_STATE);
    		break;
     	case REARRANGE_GOLD_STATE:
     		setState(TablutState.REARRANGE_SILVER_STATE);
     		break;
     	case REARRANGE_SILVER_STATE:
     		setState(TablutState.PLAY_OR_SWAP_STATE);
     		break;
       	case PUZZLE_STATE:
    		setState(TablutState.PLAY_STATE);
    		break;
   	}

    }
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==TablutState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(TablutState.GAMEOVER_STATE);
        }
        else
        {	if(WinForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true; 
        		  setState(TablutState.GAMEOVER_STATE); 
        		}
        	else 
        	{	setNextPlayer();
        		setNextStateAfterDone();
        	}
        }
    }

void doSwap()
{ 
	TabChip ch = playerChip[0];
	playerChip[0]=playerChip[1];
	playerChip[1]=ch;
	switch(board_state)
	{	
	default: throw G.Error("Not expecting state %s",board_state);
	case PLAY_OR_SWAP_STATE:
		  setState(TablutState.CONFIRM_SWAP_STATE);
		  break;
	case CONFIRM_SWAP_STATE:
		  setState(TablutState.PLAY_OR_SWAP_STATE);
		  break;
	case GAMEOVER_STATE:
	case PUZZLE_STATE: break;
	}

}
	private void pickFromPool(TabId src)
	{   for(int i=0;i<pools.length;i++)
		{ TabCell pi=pools[i];
		  if(pi.rackLocation==src) 
		  { pickedSource = pi;
		    pickedObject = lastPicked = pickedSource.topChip();
		    return;
		  }
		}
		throw G.Error("location "+src+" not found");
        
	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Tabmovespec m = (Tabmovespec)mm;

        //G.print("E "+m+" for "+whoseTurn+" "+Digest());
        switch (m.op)
        {
        case MOVE_SETOPTION:
        	setOptionValue(m.source,m.object);
        	break;
		case MOVE_SWAP:	// swap colors with the other player
			doSwap();
			break;
        case MOVE_DONE:
        	doCaptures(droppedDest,replay);	// save capture info fo undo
         	doDone();
            break;
        case MOVE_MOVE:	// used by the robots
        	pickFromBoard(m.from_col, m.from_row);
			//$FALL-THROUGH$
		case MOVE_DROPB:
			switch(board_state)
			{ 
			case PUZZLE_STATE: 
			case REARRANGE_GOLD_STATE:
			case REARRANGE_SILVER_STATE:
				acceptPlacement(); 
				pickFromPool(m.object);
				break;
			  case CONFIRM_NOSWAP_STATE:
			  case CONFIRM_STATE: 
				 unDropObject(); 
				 unPickObject(); 
				 break;
			  case PLAY_STATE:
			  case PLAY_OR_SWAP_STATE:  break;
			default:
				break;
			}
          	TabCell c = getCell(m.to_col, m.to_row);
          	if((m.op==MOVE_MOVE) ? (replay!=replayMode.Replay) : (replay==replayMode.Single))
          	{
          		animationStack.push(pickedSource);
          		animationStack.push(c);
          	}
          	if((revision>=101) && (c==pickedSource)) { unPickObject(); } 
          	else 
          	{ 
           	SetBoard(c,pickedObject);
            droppedDest = c;
            pickedObject = null;
            setNextStateAfterDrop();
          	}

            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	pickFromBoard(m.from_col, m.from_row);
        	switch(board_state)
        	{
        	case CONFIRM_SWAP_STATE:
        		// this occurs in some damaged games. 
        		doSwap(); 
				//$FALL-THROUGH$
        	case DRAW_STATE:
        	case CONFIRM_STATE: setState(TablutState.PLAY_STATE); break;
        	case CONFIRM_NOSWAP_STATE: setState(TablutState.PLAY_OR_SWAP_STATE); break;
			default:
				break;
        	}
            break;

        case MOVE_DROP: // drop on a chip pool;
        	pickedSource = null;
        	pickedObject = null;
            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickFromPool(m.source);
 
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            unDropObject();
            unPickObject();
            setState(TablutState.PUZZLE_STATE);

            if((win[whoseTurn]=WinForPlayerNow(whoseTurn))
               ||(win[nextPlayer[whoseTurn]]=WinForPlayerNow(nextPlayer[whoseTurn])))
               	{ setState(TablutState.GAMEOVER_STATE); }
            else if(moveNumber>3) { setState(TablutState.PLAY_STATE); }
            else if(playerChip[whoseTurn]==TabChip.GoldShip)
            {setState(TablutState.REARRANGE_GOLD_STATE);
            }
            else if(playerChip[whoseTurn]==TabChip.SilverShip)
            {setState(TablutState.REARRANGE_SILVER_STATE);
            }
 
            break;

       case MOVE_RESIGN:
            setState(unresign==null?TablutState.RESIGN_STATE:unresign);	
            break;
        case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(TablutState.PUZZLE_STATE);

            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(TablutState.GAMEOVER_STATE);
			break;

        default:
        	cantExecute(m);
        }

        //G.print("X "+m+" for "+whoseTurn+" "+Digest());
        return (true);
    }

 
    public boolean LegalToHitChips(TabCell cell)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case REARRANGE_SILVER_STATE:
        	return(cell.rackLocation==TabId.SilverShipLocation);
        case REARRANGE_GOLD_STATE:
        	return(cell.rackLocation==TabId.GoldShipLocation);
        case CONFIRM_STATE:
        case PLAY_OR_SWAP_STATE:
        case PLAY_STATE: return(false);
		case CONFIRM_SWAP_STATE:
		case CONFIRM_NOSWAP_STATE:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
		case DRAW_STATE:
			return(false);
        case PUZZLE_STATE:
            return (true);
        }
    }
    boolean contentsOwnedBy(int player,TabCell cell)
    {	if(cell!=null)
    	{
    	TabChip cc = cell.topChip();
    	if(cc!=null) { return(cc.colorType==playerChip[player].colorType); }
    	}
    	return(false);
    }
    boolean contentsAssistCapture(int player,TabCell cell)
    {
    	if((cell!=null) 
    			&& cell.flagArea
    			&& (!captureCompatibility || ((cell.chip==null) || (cell.chip.colorType==TabChip.GoldColor)))
    			) { return(true); }	// only the flagship can enter
    	return(contentsOwnedBy(player,cell));
    }
    boolean captureCompatibility = false;
    boolean captureCompatibility2 = false;
    boolean canCaptureCompatibility = false;
    public void setCaptureCompatibility(String date)
    {
    	BSDate dd = new BSDate(date);
    	BSDate fr = new BSDate("Nov 1 2009");
    	BSDate fr1 = new BSDate("feb 1 2007" /* "Feb 1 2009"*/);
    	BSDate fr2 = new BSDate("Apr 1 2009");
    
    	canCaptureCompatibility = dd.before(fr);
     	captureCompatibility2 = dd.before(fr2) && !dd.before(fr1);
       	captureCompatibility = dd.before(fr);
   }
    int dropCaptures(TabCell source,TabCell []temp)
    {	int idx=0;
    	if((source.chip!=TabChip.GoldFlag)||Flagship_Can_Capture)
    	{
    	for(int dir = 1;dir<source.nAdjacentCells(); dir+=2)
    	{	TabCell adj = source.exitTo(dir);
    		if(contentsOwnedBy(nextPlayer[whoseTurn],adj))
    		{
    		TabCell opp = adj.exitTo(dir);
    		if(contentsAssistCapture(whoseTurn,opp)
    				&& ((opp.chip!=TabChip.GoldFlag)||Flagship_Can_Capture))
    		{ 	if(Flagship_Four_Sided_Capture && (adj.chip==TabChip.GoldFlag)) 
    			{	//  the other two sides have to be surround too
    				TabCell adj1 = adj.exitTo(dir+2);
    				if((adj1==null) 
    						|| contentsAssistCapture(whoseTurn,adj1)
    						|| (captureCompatibility && adj1.centerSquare )
    						)
    				{
    					TabCell adj2 = adj.exitTo(dir+6);
    					if((adj2==null)
    							|| contentsAssistCapture(whoseTurn,adj2)
    							|| (captureCompatibility && adj2.centerSquare )
    							)
    					{ temp[idx++]=adj;
    					}
    				}
    			}
    			else
    			{	if(!( captureCompatibility
    					&& opp.flagArea 
    					&& (adj.chip.colorType==TabChip.GoldColor)))
    			{
    				temp[idx++]=adj;;
    			}
    			else if (captureCompatibility
    						&& opp.centerSquare 
    						&& ((opp.chip!=null) && (opp.chip.colorType==TabChip.SilverColor))
    						)
    				{	temp[idx++]=adj;;
    				}
    			
    			}
    		}
    	}}
    	}
    	return(idx);
    }
    // this returns a mask of captured pieces (up to 4 of them)
    // for the benefit of undos
    int doCaptures(TabCell source,replayMode replay)
    {	
    	int mask = 0;
    	if(source!=null)
    	{
    	TabCell tempDests[]=getTempDest();
    	int caps = dropCaptures(source,tempDests);
    	for(int i=0;i<caps;i++) 
    	{	TabCell cap = tempDests[i];
    		int dx = 1+ (cap.col-source.col);
    		int dy = 1+ (cap.row-source.row);
       		TabChip ch = SetBoard(cap,null);
       		if(replay!=replayMode.Replay)
       		{
       			animationStack.push(cap);
       			animationStack.push(playerChipPool(nextPlayer[whoseTurn]));
       		}
       		if(ch==TabChip.GoldFlag) { flagShipLocation=goldFlagPool; }
       		switch(dx+dy*3)
    		{ default: throw G.Error("not expecting %s",(dx+dy*3));
    		  case (-1+1)+(0+1)*3:	mask |= (ch.index+1); break;
    		  case (1+1)+(0+1)*3:	mask |= ((ch.index+1)<<8); break;
    		  case (0+1)+(-1+1)*3:  mask |= ((ch.index+1)<<16); break;
    		  case (0+1)+(1+1)*3:	mask |= ((ch.index+1)<<24); break;
    		}
   		
    	}
    	returnTempDest(tempDests);
    	}
    	return(mask);
    }
    void undoCaptures(TabCell cell,int mask)
    {	if(mask!=0)
    	{
    	int v0 = (mask & 0xff);
    	int v1 = (mask>>8)& 0xff;
    	int v2 = (mask>>16) & 0xff;	// dx = 1 dy = 0
    	int v3 = (mask>>24) & 0xff;
    	if(v0!=0) { SetBoard((char)(cell.col-1),cell.row,TabChip.getChip(v0-1)); }
    	if(v1!=0) { SetBoard((char)(cell.col+1),cell.row,TabChip.getChip(v1-1)); }
    	if(v2!=0) { SetBoard(cell.col,cell.row-1,TabChip.getChip(v2-1)); }
    	if(v3!=0) { SetBoard(cell.col,cell.row+1,TabChip.getChip(v3-1)); }
    	}
    }
    // add legal moves to temp, if temp is null return 1 or 0 if there are moves or not
    int legalDests(TabCell source,TabChip src,TabCell []temp)
    {	int idx=0;
    	for(int dir = 1;dir<source.nAdjacentCells(); dir+=2)
    	{	TabCell adj = source.exitTo(dir);
    		while(adj!=null)
    		{
    		if(adj.chip==null) 
    			{ // we can traverse flagship-only areas, but can't land on them
    			  if((src==TabChip.GoldFlag) || !adj.flagArea) 
    			  	{  if(temp!=null) 
    			  		{ temp[idx++] = adj;
    			  		} 
    			  			else 
    			  		{ return(1);	// act as an "are there any" test 
    			  		}
    			  	}
    			  adj=adj.exitTo(dir); 
    			}
    		else  { adj=null; }
    		}
    	}
    	return(idx);
    }
    public Hashtable<TabCell,TabCell> movingObjectDests()
    {	Hashtable<TabCell,TabCell> dd = new Hashtable<TabCell,TabCell>();
    	int mo = movingObjectIndex();
    	if(mo>=0)
    	{TabCell tempDests[]=getTempDest();
   		  int dests = legalDests(pickedSource,pickedObject,tempDests);
   		  for(int i=0;i<dests;i++) { dd.put(tempDests[i],tempDests[i]); }
   	      returnTempDest(tempDests);
    	}
    return(dd);
    }
    public Hashtable<TabCell,TabCell> droppedObjectCaptures()
    {	Hashtable<TabCell,TabCell> dd = new Hashtable<TabCell,TabCell>();
    	switch(board_state)
    	{
    	case CONFIRM_STATE:
    	if((droppedDest!=null)&&(pickedObject==null))
    	{ TabCell tempDests[]=getTempDest();
   		  int dests = dropCaptures(droppedDest,tempDests);
   		  for(int i=0;i<dests;i++) { dd.put(tempDests[i],tempDests[i]); }
   	      returnTempDest(tempDests);
    	}
    	break;
    	default: break;
    	}
    return(dd);
    }
    public boolean LegalToHitBoard(TabCell c)
    {	if(c==null) { return(false); }
    	boolean cellContentsOk = (pickedSource==null) 
    							? ((c==pickedSource)||contentsOwnedBy(whoseTurn,c))
    							: (c.chip==null);
	switch (board_state)
        {
 		case PLAY_STATE:
		case PLAY_OR_SWAP_STATE:
			return(cellContentsOk 
					&& ((pickedSource==null)?true:(c==pickedSource)||(movingObjectDests().get(c)==c)));
		
		case CONFIRM_SWAP_STATE:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case DRAW_STATE:
		case CONFIRM_NOSWAP_STATE:
		case CONFIRM_STATE:
			return(isDest(c));
		case REARRANGE_GOLD_STATE:
			return(cellContentsOk && c.centerArea);
		case REARRANGE_SILVER_STATE:
			return(cellContentsOk && !c.centerArea);
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
            return (true);
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Tabmovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	if(m.op==MOVE_MOVE) 
            	{ m.capture_mask = doCaptures(getCell(m.to_col,m.to_row),replayMode.Replay);
            	} else { m.capture_mask=0; }
                doDone();
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Tabmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
            break;

        case MOVE_SWAP:
        	setState(m.state);
        	doSwap();
        	break;
        case MOVE_MOVE:
        	{
        	TabCell s = getCell(m.to_col,m.to_row);
        	TabCell d = getCell(m.from_col,m.from_row);
        	undoCaptures(s,m.capture_mask);
        	SetBoard(d,s.topChip());
        	SetBoard(s,null);
        	pickedSource = null;
        	droppedDest=null;
        	}
        	break;
        case MOVE_RESIGN:
             break;
        }

        setState(m.state);
        if(whoseTurn!=m.player)
        {
        	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 boolean NoMovesForPlayer(int player)
 {
	for(TabCell cell = allCells; cell!=null; cell=cell.next)
		{	if(contentsOwnedBy(player,cell))
				{
				if(legalDests(cell,cell.topChip(),null)>0) { return(false); }
				}
		}
	 return(true);
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case PLAY_OR_SWAP_STATE:
 		all.addElement(new Tabmovespec(SWAP,whoseTurn));
 		// and all the other moves too
		//$FALL-THROUGH$
	case PLAY_STATE:
 		{	TabCell temp[] = getTempDest();
 		    if(playerChip[whoseTurn]==TabChip.SilverShip)
 		    {	int minships = Flagship_Four_Sided_Capture ? 3 : 2;
 		    	if(silver_ships<minships)
 		    	{	all.addElement(new Tabmovespec(RESIGN,whoseTurn));
 		    		return(all);
 		    	}
 		    }
 			for(TabCell cell = allCells; cell!=null; cell=cell.next)
 			{	if(contentsOwnedBy(whoseTurn,cell))
 				{
 				int num = legalDests(cell,cell.topChip(),temp);
 				for(int i=0; i<num; i++) 
 				{ TabCell dest = temp[i];
 				  all.addElement(new Tabmovespec(MOVE_MOVE,cell.col,cell.row,dest.col,dest.row,whoseTurn));
 				}
 				}
 			}
  			returnTempDest(temp);
 		}
 		break;
 	default: 
 	}
 	return(all);
 }
 // classify the squares by the number of rook moves to get to the edge
 public void classify()
 {
	for(TabCell cell = allCells; cell!=null; cell=cell.next) 
		{ cell.sweep_score = cell.sweep_counter=cell.winForGold?0:ncols;		// win cells 0 others -1
		}
	boolean progress = true;
	open_ranks = 0;
	for(int generation=0; progress; generation++)
	{	progress=false;
	    totalSweepScore = 0;
		for(TabCell cell = allCells; cell!=null; cell=cell.next) 
		{ totalSweepScore += cell.sweep_score;
		  if((cell.sweep_counter==generation) && (cell.chip==null))
			{	// tag movable cells as gen +1
				for(int direction = 1;direction<cell.nAdjacentCells(); direction+=2)
				{	TabCell nx = cell.exitTo(direction);
					int distance = cell.sweep_score+1;
					
					while((nx!=null) && (nx.sweep_counter>=(generation+1)))
					{	//extend along the line, stop after we hit something.
						nx.sweep_counter = generation+1;
						nx.sweep_score = distance++;
						progress=true;
						nx = (nx.chip==null) 
								? nx.exitTo(direction)
								: null;
					}

				}
			}
		}
		
	}
	
 }
}
