package micropul;

import bridge.*;

import java.util.*;

import online.game.*;
import lib.*;
import lib.Random;

/**
 * MicropulBoard knows all about the game of Micropul 
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

class MicropulBoard extends squareBoard<MicropulCell> implements BoardProtocol,MicropulConstants
{	
	static int BOARDCOLUMNS = 19;
	static int BOARDROWS = 19;
	static final String[] MicropulGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

	
	private MicropulState unresign;
	private MicropulState board_state;
	public MicropulState getState() {return(board_state); }
	public void setState(MicropulState st) 
	{ 	unresign = (st==MicropulState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    //
    // private variables
    //
	public CellStack animationStack = new CellStack();
	
	public void SetDrawState() {throw G.Error("Draw not expected"); };	
    private int chips_on_board = 0;			// number of chips currently on the board
    public int extraTurns = 0;				// extra turns for the current player
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MicropulChip pickedObject = null;
    public int pickedRotation = 0;
    public MicropulChip lastPicked = null;
    public MicropulCell core = null;
    public MicropulCell rack[][] = new MicropulCell[2][6];		// face up chips
    public MicropulCell supply[] = new MicropulCell[2];		// face down chips
    public MicropulCell jewels[] = new MicropulCell[2];			// unplayed jewels
    public MicropulChip playerChip[] = { MicropulChip.red ,MicropulChip.blue };
    public MicropulCell pickedSource = null; 
    public MicropulCell droppedDest = null;
    public MicropulChip lastDroppedDest = null;	// for image adjustment logic
	private MicropulCell occupiedCells = null;
 
	public int jewelOwner(MicropulChip chip)
	{
		return(chip==playerChip[0] ? 0 : 1);
	}
	// jewel scorekeeping 
	public int claimedMicropul[] = new int[2];	// accumulate results
	public int unclaimedMicropul[] = new int[2];
	public int claimExtensions[] = new int[2];
	// temporary for counting
	public int micropulCells = 0;				// temp use in the scan
	public int micropulExtensions = 0;			// empty cells adjacent to claimed micropul
	public int sweep_counter = 0;
	
    public Hashtable<MicropulCell,MicropulCell> movingObjectDests()
    {	
		Hashtable<MicropulCell,MicropulCell> h = new Hashtable<MicropulCell,MicropulCell>();
    	if(pickedObject!=null)
		{
    	switch(pickedSource.rackLocation())
    	{
    	default: break;
    	case Rack:
    	{	for(MicropulCell c = allCells; c!=null; c=c.next)
    		{	if(c.height()>0) 
    			{
    			for(int dir = 0;dir<CELL_FULL_TURN;dir++)
    			{	MicropulCell d = c.exitTo(dir);
    				if((d!=null) && (d.height()==0))
    				{	
    					h.put(d,d);
    				}
    			}
    			}
    		}
    	}}}
    	return(h);
    }
	// factory method to generate a board cell
	public MicropulCell newcell(char c,int r)
	{	return(new MicropulCell(c,r));
	}
    public MicropulBoard(long randomv,String init,int []map) // default constructor
    {	setColorMap(map, 2);
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = MicropulGRIDSTYLE;
        isTorus=true;
        doInit(init,randomv); // do the initialization 
    }

    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game,long key)
    {	Random r = new Random(645462);
    	if(Micropul_INIT.equalsIgnoreCase(game)) {  initBoard(BOARDCOLUMNS,BOARDROWS);  }
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(MicropulState.PUZZLE_STATE);
        allCells.setDigestChain(r);
        core = new MicropulCell(r,MicroId.Core,0);	// dummy source for the chip pools
        //initBoard(firstcol, ncol, null); //this sets up the board
         
        whoseTurn = FIRST_PLAYER_INDEX;
        chips_on_board = 0;
        extraTurns = 0;
        droppedDest = null;
        pickedSource = null;
        pickedObject = null;
        lastDroppedDest = null;
        randomKey = key;

        // set the initial contents of the board to all empty cells
		for(MicropulCell c = allCells; c!=null; c=c.next) 
			{ c.reInit(); }
	  occupiedCells = null;
	  core.reInit();
	  core.masked = true;
	  
	  SetBoard(getCell('J',10),MicropulChip.getChip(0),0); // the starting tile
	  
	  for(int i=1;i<MicropulChip.nChips;i++)
	       {	core.addChip(MicropulChip.getChip(i));
	       }
      if(randomKey!=0)
       {
       // swap pairs to randomize
       Random v = new Random(randomKey);
       int h = core.height();
       for(int i=0;i<999;i++)
       {
    	   int n1 = Random.nextInt(v,h);
    	   int n2 = i%h;
    	   MicropulChip c1 = core.chipAtIndex(n1);
    	   MicropulChip c2 = core.chipAtIndex(n2);
    	   core.setChipAtIndex(n1,c2);
    	   core.setChipAtIndex(n2,c1);
      }}
      //
      // draw the initial tiles
        int map[] = getColorMap();
  		for(int j=0;j<2;j++)
  		{ 
  		  supply[j]=new MicropulCell(r,MicroId.Supply,j);
  		  supply[j].masked = true;
  		  jewels[j] = new MicropulCell(r,MicroId.Jewels,j);
  		   MicropulChip jc = MicropulChip.getJewel(map[j]);
  		   playerChip[j] = jc;
  		   jewels[j].addChip(jc);
  		   jewels[j].addChip(jc);
  		   jewels[j].addChip(jc);
    	  for(int i=0;i<6;i++)
    	  { MicropulChip cc = core.removeTop();
    	  	MicropulCell cell = rack[j][i]=new MicropulCell(r,MicroId.Rack,i,j);
    	  	cell.addChip(cc,0);
    	  }
      }
		//for(int i=1;i<ncols;i++)
		//{	getCell((char)('@'+i),1).addChip(CheChip.getChip(i%3));
		//    if(i>1) { getCell('A',i).addChip(CheChip.getChip(i%3));}
		//}
    }

    void clearJewels()
    {	for(MicropulCell c = occupiedCells; c!=null; c=c.nextOccupied)
    	{ c.clearJewels();
    	}
    	for(int i=0;i<2;i++)
    	{ claimedMicropul[i]=0;
    	  unclaimedMicropul[i] = 0;
    	  claimExtensions[i] = 0;
     	}
    }
    void sweepJewels()
    {
    	clearJewels();
    	for(MicropulCell c = occupiedCells; c!=null; c=c.nextOccupied)
    	{
    		MicropulChip chip = c.topChip();
    		if(chip.isJewel())
    		{ c.markJewels(this);
    		  
    		}
    	}
    }
 
    public void sameboard(BoardProtocol f) { sameboard((MicropulBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MicropulBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the boards

        G.Assert(core.sameCell(from_b.core),"core matches");
        G.Assert(extraTurns==from_b.extraTurns,"Extra Turns match");      
        G.Assert(sameCells(supply,from_b.supply),"Supply matches");
        G.Assert(sameCells(jewels,from_b.jewels),"Jewels match");
        G.Assert(sameCells(rack,from_b.rack),"rack match");
        G.Assert((occupiedCells==from_b.occupiedCells) || ((occupiedCells!=null) && occupiedCells.sameCell(from_b.occupiedCells)),"same occupied");
        G.Assert(pickedObject==from_b.pickedObject, "pickedObject matches");
        // here, check any other state of the board to see if
        G.Assert(chips_on_board == from_b.chips_on_board , "chips_on_board not the same");
        
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used 3 different
     * ways in the system.
     * (1) This is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     *  looks for duplicate digests.  
     * (2) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (3) Digests are used by the search machinery as a check on the robot's winding/unwinding
     * of the board position, this is mainly a debug/development function, but a very useful one.
     * @return
     */
    public long Digest()
    {
 
        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
       	long v = super.Digest(r);
               
        v ^= (r.nextLong()*extraTurns);
        v ^= Digest(r,rack);
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,pickedSource);
		v ^= r.nextLong()*(board_state.ordinal()*10 +  whoseTurn);

		//G.print("digest "+v);
        return (v);
    }
    public MicropulBoard cloneBoard() 
    	{ MicropulBoard dup = new MicropulBoard(randomKey,gametype,getColorMap()); 
    	  dup.copyFrom(this);
    	  return(dup); 
       	}
    public void copyFrom(BoardProtocol b) { copyFrom((MicropulBoard)b); }
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MicropulBoard from_b)
    {	super.copyFrom(from_b);
        copyFrom(core,from_b.core);
        copyFrom(supply,from_b.supply);
        copyFrom(jewels,from_b.jewels);
        copyFrom(rack,from_b.rack);
        
        chips_on_board = from_b.chips_on_board;
        extraTurns = from_b.extraTurns;
        pickedObject = from_b.pickedObject;
        droppedDest = getCell(from_b.droppedDest);
        pickedSource = getCell(from_b.pickedSource);
        
        lastPicked = null;
        {
            MicropulCell oo = from_b.occupiedCells;
            MicropulCell ll = getCell(oo);
            occupiedCells = ll;
            while(oo!=null)
            	{ oo = oo.nextOccupied;
            	  ll = ll.nextOccupied = getCell(oo);
             	}
            }
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        sameboard(from_b); 
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {

       Init_Standard(gtype,key);
       // temp for debugging
       //int ii=0;
       //for(int col=0;col<8;col++)
       //{ for(int row=1; row<=6;row++)
       //{	SetBoard(getCell((char)('A'+col),row),MicropulChip.getChip(ii++));
       //}
       //}
        moveNumber = 1;
        extraTurns = 0;

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
        case CONFIRM_STATE:
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
        case CONFIRM_STATE:
            return (true);

        default:
            return (false);
        }
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(DoneState());
    }

    boolean hasNoChips(int pl)
    {	if(supply[pl].height()>0) { return(false); }
    	MicropulCell cp[] = rack[pl];
    	for(int i=0;i<cp.length;i++) { if(cp[i].height()>0) { return(false); }}
    	return(true);
    }
    
    public int scoreForPlayer(int player)
    {	int val = supply[player].height()*2;
    	val += claimedMicropul[player];
    	for(int i=0;i<6;i++) { val += rack[player][i].height(); }
    	return(val);
    }
    public boolean WinForPlayerNow(int player)
    {	if(win[player]) { return(true); }
     	if(hasNoChips(nextPlayer[player])) { return(true);}
    	if(core.height()>0) { return(false); }
    	sweepJewels();
    	return(scoreForPlayer(player)>scoreForPlayer(nextPlayer[player]));
     }

    public double dumbotEval(int who,boolean print)
    {   sweepJewels();
    	double supplyweight = core.height();	// decrease the value of supply near the start
    	double val = scoreForPlayer(who)*100;			// actual score
    	val += 100*unclaimedMicropul[who]/(claimExtensions[who]+1);		// dowgraded potential
    	val += 50*jewels[who].height();				// discourage trading jewels for less than 5 points
    	val -= supply[who].height()*supplyweight;		// make supply the same as rack early in the game
    	return(val);
    }
    public double smartbotEval(int who, boolean print)
    {   sweepJewels();
    	double supplyweight = core.height();	// decrease the value of supply near the start
    	double val = scoreForPlayer(who)*100;			// actual score
    	val += 100*unclaimedMicropul[who]/(claimExtensions[who]+1);		// dowgraded potential
    	val -= supply[who].height()*supplyweight;		// make supply the same as rack early in the game
    	return(val);
    }
    // set the contents of a cell, and maintian the books
    // ch=null means remove an element.
    public MicropulChip SetBoard(MicropulCell c,MicropulChip ch,int rot)
    {	MicropulChip old = c.topChip();
    	boolean oldtop = old!=null;
    	if(oldtop && (ch==null) ) { c.removeTop(); }
       	if(ch!=null) { c.addChip(ch,rot); }
       	
    	if(c.onBoard) 
    	{
    	// maintain the onboad count and occupuied chips list
    	boolean newtop = c.topChip()!=null;
    	if(oldtop!=newtop)
    	{
    	if(oldtop)
    	{	// old was occupied, new is not
    		chips_on_board--; 
    		MicropulCell prev = null;
    		MicropulCell curr = occupiedCells;
    		while(curr!=null)
    		{	if(curr==c)
    		  	{ // remove from occupiedCells list
    			  if(prev==null) 
    		  			{ occupiedCells = curr.nextOccupied; curr.nextOccupied=null;}
    		  			else 
    		  			{ prev.nextOccupied = curr.nextOccupied; curr.nextOccupied = null; }
    		  	 }
    		  prev = curr;
    		  curr = curr.nextOccupied;
    		  }
    		}
    	else { chips_on_board++; 
     		   c.nextOccupied = occupiedCells;
     		   occupiedCells = c; 
     		 }
    	}}
  	
    	return(old);
    }
    //
    // accept the current placements as permanant
    //
    public void acceptPlacement()
    {	if(droppedDest!=null)
    	{
    	switch(droppedDest.rackLocation())
    	{	
    	case Core:
    	case Supply: droppedDest.masked = true;
    		break;
    	default: droppedDest.masked = false;
    	}
        droppedDest = null;
        pickedSource = null;
        pickedObject = null;
    	}
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	if(droppedDest!=null) 
    	{	pickedObject = SetBoard(droppedDest,null,0); 
    		droppedDest = null;
     	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	if(pickedSource!=null) 
    		{ SetBoard(pickedSource,pickedObject,pickedRotation); 
    		  pickedObject = null;
    		  pickedSource = null;
    		}
    }
    // 
    // drop the floating object.
    //
    private void dropObject(MicroId dest, int pla, int row)
    {
       MicropulCell c = null;
       switch (dest)
        {
         default:
        	 throw G.Error("not expecting dest %s", dest);
        case Jewels:
        	c = jewels[pla];
        	break;
        case Rack:
        	c = rack[pla][row];
        	break;
        case Supply:
        	c = supply[pla];
        	break;
        case Core:
        	c = core;
            break;
         }
      if(isSource(c)) { unPickObject(); }
      else
      {
      if(pickedObject!=null)
		{ c.addChip(pickedObject,pickedRotation);
		  c.masked = pickedSource.masked;
	      droppedDest = c;
		  pickedObject = null;
		}}
     
     }

    public void dropOnBoard(char col, int row,int rot)
    {
      	MicropulCell c = getCell(col,row);
      	if(pickedObject.legalToPlaceMicropul(c,rot))
      	{
       	SetBoard(c,pickedObject,pickedRotation);
       	c.rotation[c.chipIndex] = rot;
        lastDroppedDest = pickedObject;
        droppedDest = c;
        pickedObject = null;
      	}
      	else
      	{ throw G.Error("Not a legal placement"); 
      	}
	
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MicropulCell c)
    {	return(droppedDest==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    { 	MicropulChip ch = pickedObject;
    	MicropulCell ps = pickedSource;
    	if((ps!=null) && (droppedDest==null) && ch!=null)
    	{	int number = ch.chipNumber();
    		// encode chip id and rotation and mask status
    		int rot = ps.masked ? 4 : ps.rotation[0];
      		return(number*100+rot); 
    	}
      	return (NothingMoving);
    }
    
    public MicropulCell getCell(MicropulCell c) 
    { 	if(c==null) { return(null); }
    	if(c.onBoard) { return(getCell(c.col,c.row)); }
    	
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("not expecting source %s", c.rackLocation);
        case Supply:
        	 return(supply[c.player]);
        case Jewels:
        	 return(jewels[c.player]);
        case Rack:
        	return(rack[c.player][c.row]);
        case Core:
        	return(core);
        }

    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(MicroId source, int player, int row)
    {	MicropulCell src = null;
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case Supply:
        	 src = supply[player];
        	 break;
        case Jewels:
        	 src = jewels[player];
        	 break;
        case Rack:
        	src=rack[player][row];
        	break;
        case Core:
        	src = core;
        }
        if(isDest(src))
        { unDropObject(); 
        }
        else
        {
        acceptPlacement();
        pickedRotation = src.topRotation();
		lastPicked = pickedObject = src.removeTop();
		pickedSource = src;
        }
    }
    
    private void pickFromBoard(char col,int row)
    {
       	MicropulCell c = getCell(col,row);
       	boolean wasDest = isDest(c);
       	unDropObject(); 
       	if(!wasDest)
       	{
        pickedSource = c;
        pickedRotation = c.topRotation();
        lastPicked = pickedObject = c.topChip();
       	droppedDest = null;
		if(c.topChip()!=null) { SetBoard(c,null,0); }
       	}	
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(MicropulCell c)
    {	return(c==pickedSource);
    }
    
    private void setNextStateAfterPick()
    {
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting drop in state %s", board_state);
    	case CONFIRM_STATE: setState(MicropulState.PLAY_STATE); break;
    	case PUZZLE_STATE: break;
    	}
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //
    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_STATE:
        	if(droppedDest==null)
        	{setNextStateAfterDone();
        	}
        	break;
        case GAMEOVER_STATE:
        	if(replay==replayMode.Live)
        	{	// some damaged games continue
        		throw G.Error("Not expecting drop in state %s", board_state);
        	}
			//$FALL-THROUGH$
        case PLAY_STATE:
			if(droppedDest!=null) { setState(MicropulState.CONFIRM_STATE);}
			break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		setState(MicropulState.PLAY_STATE);
    		break;
    	}

    }
    private void doDone(replayMode replay)
    {	MicropulCell dest = droppedDest;
        acceptPlacement();
        
        if (board_state==MicropulState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(MicropulState.GAMEOVER_STATE);
        }
        else
        if(dest!=null)
        {
        MicropulChip top = dest.topChip();
        if(top.isJewel()) { ; }
        else
        {
        int pull = top.countActivations(dest);
        while((pull-- > 0) && (core.height()>0))
        {	int rot = core.topRotation();
        	MicropulChip xx = core.removeTop();
        	supply[whoseTurn].addChip(xx,rot);
        	if(replay!=replayMode.Replay)
        	{
        	animationStack.push(core);
        	animationStack.push(supply[whoseTurn]);
        	}
        }
        extraTurns += top.countNewturns(dest);
        }
        
        {
        switch(dest.rackLocation())
        {
        case Core:
        case Supply: break;
        default: dest.masked = false;
        }
        {	if(WinForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setNextPlayer(); 
        		  setState(MicropulState.GAMEOVER_STATE); 
        		}
            else if(WinForPlayerNow(nextPlayer[whoseTurn]))
        		{ win[nextPlayer[whoseTurn]]=true;
        		  setNextPlayer(); 
       		      setState(MicropulState.GAMEOVER_STATE); 
        		}
            else if(core.height()==0) 
            	{ setNextPlayer(); 
        		  setState(MicropulState.GAMEOVER_STATE); 
        		}
        	else {
        		if(extraTurns>0) { extraTurns--; } else { setNextPlayer(); }
        		setNextStateAfterDone();
        	}
        }
        }
        
        }
        
   
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Micropulmovespec m = (Micropulmovespec)mm;

        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
       case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_DROPB:
            dropOnBoard(m.to_col, m.to_row,m.rotation);
            if(replay==replayMode.Single)
            {
            	animationStack.push(pickedSource);
            	animationStack.push(droppedDest);
            }
            setNextStateAfterDrop(replay);
            break;

        case MOVE_PICK:
            pickObject(m.source,m.player,m.from_row);
            m.chip = pickedObject;
            if(board_state==MicropulState.CONFIRM_STATE) { setState(MicropulState.PLAY_STATE); }
            break;
 
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	pickFromBoard(m.from_col, m.from_row);
        	m.chip = pickedObject;
        	setNextStateAfterPick();
            break;

        case MOVE_DROP: // drop on chip pool, rack, or supply;
            dropObject(m.dest, m.player, m.to_row);
            setNextStateAfterDrop(replay);
            break;

        case MOVE_MOVE:	// computer move
        	m.undoInfo = core.height()*100 + extraTurns;
        	if(m.source==MicroId.BoardLocation) 
        		{ 
        		  pickFromBoard(m.from_col,m.from_row);
        		}
        		else
        		{ pickObject(m.source,m.player,m.from_row);
        		}
        	m.chip = pickedObject;
        	if(m.dest==MicroId.BoardLocation)
        	{	dropOnBoard(m.to_col, m.to_row,m.rotation);
        	}
        	else
        	{
        		dropObject(m.dest, m.player, m.to_row);
        	}
            if(replay!=replayMode.Replay)
            {
            	animationStack.push(pickedSource);
            	animationStack.push(droppedDest);
            }
 
        	setNextStateAfterDrop(replay);
        	break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            int nextp = nextPlayer[whoseTurn];
            setState(MicropulState.PUZZLE_STATE);
            if((win[whoseTurn]=WinForPlayerNow(whoseTurn))
               ||(win[nextp]=WinForPlayerNow(nextp)))
               	{ setState(MicropulState.GAMEOVER_STATE); 
               	}
            else {  setNextStateAfterDone(); }

            break;

       case MOVE_RESIGN:
            setState(unresign==null?MicropulState.RESIGN_STATE:unresign);
            break;
            // and be like reset
        case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(MicropulState.PUZZLE_STATE);

            break;
        case MOVE_PASS:
        	setState(MicropulState.CONFIRM_STATE);
        	break;
        case MOVE_ROTATE:
        	{
        	MicropulCell c = getCell(m.to_col,m.to_row);
        	c.rotation[0] = m.rotation;
        	}
        	break;
        case MOVE_RRACK:
        	{
       		MicropulCell c = rack[m.player][m.to_row];
        	c.rotation[0] = m.rotation;
        	}
        	break;
        case MOVE_GAMEOVERONTIME:
     	   win[whoseTurn] = true;
     	   setState(MicropulState.GAMEOVER_STATE);
     	   break;
        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);

        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitCore()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        case PLAY_STATE:
        case RESIGN_STATE:
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	if(pickedObject!=null) { return(!pickedObject.isJewel()); }
            return (true);
        }
    }
    // legal to hit the visble tile area
    public boolean LegalToHitRack(int player,MicropulCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        	return(isDest(c));
        case PLAY_STATE:
        	// you can pick up a stone in the storage area
        	// but it's really optional
        	if(player!=whoseTurn) { return(false); }
        	if(pickedObject!=null) { return(isSource(c)||(pickedSource.rackLocation==MicroId.Supply)); } 
        	return(true);
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
        	if(pickedObject!=null) { return(!pickedObject.isJewel()); }      	
            return (true);
        }
    }
    // legal to hit the reserve tile area
    public boolean LegalToHitSupply(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        	if(player!=whoseTurn) { return(false); }
        	return((pickedObject!=null) && (pickedSource.rackLocation==MicroId.Supply));
        case PLAY_STATE:
        	// you can pick up a stone in the storage area
        	// but it's really optional
        	if(pickedObject!=null) { return((pickedSource.rackLocation==MicroId.Supply)&&(whoseTurn==player)); }
        	return((supply[whoseTurn].height()>0) && (player==whoseTurn));
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
            return (true);
        }
    }
    
    // legal to hit the Jewel tile area
    public boolean LegalToHitJewels(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        	return((pickedObject!=null)&&pickedObject.isJewel());
        	
        case PLAY_STATE:
        	// you can pick up a stone in the storage area
        	if((pickedObject!=null)&&!pickedObject.isJewel()) { return(false); }
        	// but it's really optional
        	return(player==whoseTurn);
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
        	if(pickedObject!=null) 
        		{ return(jewelOwner(pickedObject)==player); 	// can only hit the matching color
        		}
            return (true);
        }
    }
    public boolean legalToPlaceMicropul(MicropulCell c,MicropulChip chip,int rota)
    {	boolean some_adj = false;
    	boolean some_attach = false;
    	for(int dir=0;dir<CELL_FULL_TURN;dir++)
    	{
    		MicropulCell adj = c.exitTo(dir);		// adjacent in some direction
    		MicropulChip top = adj.bottomChip();
    		if(top!=null) 
    			{ some_adj = true;
    			  int ldr1 = (4+dir-rota)%4;
    			  int odr1 = (dir+2-1+4-adj.rotation[0])%4;
    			  int ldr2 = (4+ldr1-1)%4;
    			  int odr2 = (odr1+1)%4;
    			  //G.print("dir "+dir+":"+rota+" ldir1 "+ldr1+"<>"+odr1+" "+ldr2+" "+ldr2+"<>"+odr2);
     			  if(!chip.pipsCompatible(ldr1,top,odr1)) { return(false); }
     			  if(!chip.pipsCompatible(ldr2,top,odr2)) { return(false); }
    			  if(!some_attach) { some_attach |= chip.pipsAttach(ldr1,top,odr1); }
    			  if(!some_attach) { some_attach |= chip.pipsAttach(ldr2,top,odr2); }
    			}
    	}
    	//if(some_attach) { G.print("ok"); } else { G.print("allowed"); }
    	return(some_attach || !some_adj);
    }
    public boolean hasLegalRotation(MicropulCell c,MicropulChip chip)
    {	if(chip.isJewel()) { return(true); }
    	for(int i=0;i<CELL_FULL_TURN;i++)
    	{	if(chip.legalToPlaceMicropul(c,i)) { return(true); }
    	}
    	return(false);
    }
    public boolean LegalToHitBoard(MicropulCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case PLAY_STATE:
			if(pickedObject!=null)
			{
			switch(pickedSource.rackLocation())
			{
			default: throw G.Error("Not expecting source %s",pickedSource);
			case Jewels: return(c.bottomChip()!=null);
			case Supply: return(false);
			case Rack:
			case BoardLocation:
				return((c.topChip()==null) && hasLegalRotation(c,pickedObject));
			}}
			{
			return((c.topChip()==null)
					? (c.topChip()!=null)
		            : isDest(c));
			}
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
			return(isDest(c) || (c.topChip()==null));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
			if(pickedObject!=null)
			{
			if(pickedObject.isJewel()) { return(c.bottomChip()!=null); }
			return((c.topChip()==null) && hasLegalRotation(c,pickedObject));
			}
			return((c.topChip()!=null));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Micropulmovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("rx: "+m);
        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone(replayMode.Replay);
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
    public void UnExecute(Micropulmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
   	    default:
   	    	throw G.Error("Can't un execute %s", m);

        case MOVE_DONE:
        case MOVE_PASS:
            break;
            
        case MOVE_MOVE:
        {	int coreheight = m.undoInfo/100;
        	extraTurns = m.undoInfo%100;
        	// put tiles back in the core
        	while(coreheight>core.height())
        	{	core.addChip(supply[m.player].removeTop());
        	}
        	if(m.dest==MicroId.BoardLocation) 
    		{ 
    		  pickFromBoard(m.to_col,m.to_row);
    		}
    		else
    		{ pickObject(m.dest,m.player,m.to_row);
    		}
	    	if(m.source==MicroId.BoardLocation)
	    	{	dropOnBoard(m.from_col, m.from_row,m.rotation);
	    	}
	    	else
	    	{
	    		dropObject(m.source, m.player, m.from_row);
	    	}
	        }
        	pickedSource = droppedDest = null;
        	pickedObject = null;
	    	break;
        	
        case MOVE_RESIGN:
            break;
        }

        setState(m.state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
    
    CommonMoveStack GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	int sweep = ++sweep_counter;
 	int who = whoseTurn;
 	// add "place a chip" moves
 	for(MicropulCell c = occupiedCells; c!=null; c=c.nextOccupied)
 	{
 		for(int dir=0; dir<4; dir++)
 		{	MicropulCell nx = c.exitTo(dir);
 			if((nx.sweep_counter!=sweep) && (nx.topChip()==null))
 			{	MicropulCell rackCells[] = rack[who];
 				nx.sweep_counter=sweep;
 				for(int idx = 0;idx<rackCells.length;idx++)
 				{	MicropulCell rc = rackCells[idx];
 					MicropulChip chip = rc.topChip();
 					if(chip!=null)
 					{	// see if this chip can be dropped on this empty cell with each possible rotation
 						for(int rot = 0; rot<4; rot++)
 						{
 						if(chip.legalToPlaceMicropul(nx,rot))	
 						{
 							all.addElement(new Micropulmovespec(who,
 									MicroId.Rack,'@',idx,
 									MicroId.BoardLocation,nx.col,nx.row,
 									rot));
 						}
 						}
 						
 					}
 				}
 				
 			}
 		}
 	}
 	
 	// add move from supply moves
 	{
 	MicropulCell sup = supply[who];
 	MicropulChip top = sup.topChip();
 	if(top!=null)
 		{
 		MicropulCell rr[] = rack[who];
 		for(int idx=0;idx<rr.length;idx++)
 			{
 			MicropulCell rc = rr[idx];
 			if(rc.topChip()==null)
 				{
 				all.addElement(new Micropulmovespec(who,
 						MicroId.Supply,'@',0,
 						MicroId.Rack,'@',idx,
 						0));
 				break;	// only one is needed
 				}
 			}
 		}
 	}
 	
 	// add move from jewel moves
 	{
 	 	MicropulCell sup = jewels[who];
 	 	MicropulChip top = sup.topChip();
 	 	sweepJewels();
		if(top!=null)
		{
			for(MicropulCell c = occupiedCells; c!=null; c=c.nextOccupied)
		 	{	if(!c.topChip().isJewel())
		 		{
		 		for(int rot=0; rot<4; rot++)
		 		{	if(c.topChip().legalToPlaceJewel(c,rot))
		 			{
		 			all.addElement(new Micropulmovespec(who,
		 					MicroId.Jewels,'@',0,
		 					MicroId.BoardLocation,c.col,c.row,
		 					rot));
		 			c.markJewels(this,top,rot);		// mark so we won't reuse this group
		 			break;
		 			}
		 		}
		 		}
		 	}
		}
		clearJewels();	// clear so no one uses the bogus info we placed
 	}
 	if(all.size()==0) 
 		{ all.addElement(new Micropulmovespec(RESIGN,who)); 
 		}
 	return(all);
 }
 
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ xpos-=2;
	 	}
 	GC.Text(gc, false, xpos-2, ypos, -1, 0,clt, null, txt);
 }

}
