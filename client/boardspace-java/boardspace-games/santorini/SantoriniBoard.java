/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package santorini;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;



/**
 * SantoriniBoard knows all about the game of Santorini, which is played
 * on a 5x5 board. It gets a lot of logistic support from common.rectBoard,
 * which knows about the coordinate system.  
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
 *  restrictive about what to allow in each state, and have a lot of trip wires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class SantoriniBoard extends rectBoard<SantoriniCell> implements BoardProtocol,SantoriniConstants
{
	private SantoriniState unresign;
	private SantoriniState board_state;
	public SantoriniState getState() {return(board_state); }
	public void setState(SantoriniState st) 
	{ 	unresign = (st==SantoriniState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() { throw G.Error("not expected"); }	// draws shouldn't be possible
    public SantoriniCell cube_rack[] = new SantoriniCell[2];	// cube pieces
    public SantoriniCell cylinder_rack[] = new SantoriniCell[2];	// cylinder pieces
    public SantoriniCell playerRack[][] = { cube_rack,cylinder_rack};
    public SantoriniCell reserve[] = new SantoriniCell[2];	// tiles and domes
	public CellStack animationStack = new CellStack();
    public SantoriniCell sourceCell() { return(pickedSourceStack.top()); }
    public SantoriniCell destCell() { return(droppedDestStack.top()); }
    private SantoriniCell lastManDestination = null;

    public SantoriniCell lastManDestination()
    { return(lastManDestination);
    }
    StateStack robotStateStack = new StateStack();
    CellStack robotLastMan = new CellStack();
    IStack robotInfoStack = new IStack();
    StateStack uiStateStack = new StateStack();
    public SantoriniCell gods = null;
    public boolean godSelected[] = null;
    public boolean godChosen[] = null;
    private boolean movedUp[] = {false,false};
    private boolean confirmWin[] = { false,false};
    private SantoriniCell[]lastBuild = { null,null };
    public SantoriniChip playerChip[]= { SantoriniChip.Cylinder_A,SantoriniChip.Cube_A};
    public boolean[]godSelections() 
    {
    	switch(board_state)
    	{
    	case GodSelect:
    	case GodSelectConfirm: return(godSelected);
    	case GodChoose:
    	case GodChooseConfirm: return(godChosen);
    	default: return(null);
    	}
    }
    public boolean godIsSelected(SantorId god)
    {
    	for(int lim = gods.height(); lim>=0; lim--)
    	{
    		SantoriniChip g = gods.chipAtIndex(lim);
    		if(g.id==god) { return(godSelections()[lim]); }
    	}
    	return(false);
    }
    public SantorId playerGodI[] = new SantorId[] {null,null};
    private boolean godless = false;
    public SantorId activeGod(int pl) 
    {
    	if(godless) { return(SantorId.Godless);}
    	return(playerGodI[pl]);
    }
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public SantoriniChip pickedObject = null;
    public SantoriniChip lastDropped = null;		// for chip editing
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    Variation variation = null;

	// factory method
	public SantoriniCell newcell(char c,int r)
	{	return(new SantoriniCell(c,r));
	}
    public SantoriniBoard(String init,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = SANTORINIGRIDSTYLE; //coordinates left and bottom
    	setColorMap(map, 2);	// the "colors" are cube and cylinder
        doInit(init); // do the initialization 
     }

    public SantoriniBoard cloneBoard() 
	{ SantoriniBoard dup = new SantoriniBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((SantoriniBoard)b); }

    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(Variation game)
    { 	if(variation!=null) 
    		{ boardColumns=DEFAULT_COLUMNS; 
    		  boardRows = DEFAULT_ROWS;
    		}
    	else { throw G.Error(WrongInitError,game); }
        setState(SantoriniState.PUZZLE_STATE);
        initBoard(boardColumns,boardRows); //this sets up the board and cross links
        
        // fill the board with the background tiles
        for(SantoriniCell c = allCells; c!=null; c=c.next)
        {  c.addChip(SantoriniChip.MainTile);
        }
        
        whoseTurn = FIRST_PLAYER_INDEX;
        acceptPlacement();
        lastManDestination=null;
        AR.setValue(movedUp,false);
        AR.setValue(confirmWin, false);
        AR.setValue(lastBuild, null);
    }

    public void sameboard(BoardProtocol f) { sameboard((SantoriniBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(SantoriniBoard from_b)
    {
    	super.sameboard(from_b);
        // removed these three from the test to keep the robot quiet.
        //G.Assert(stackIndex==from_b.stackIndex,"stack index matches");
        //G.Assert(cell.sameCell(pickedSourceStack,from_b.pickedSourceStack),"source stacks match");
        //G.Assert(cell.sameCell(droppedDestStack,from_b.droppedDestStack),"dest stacks match");
        
        G.Assert(SantoriniCell.sameCell(cube_rack,from_b.cube_rack),"cube racks match");
        G.Assert(SantoriniCell.sameCell(cylinder_rack,from_b.cylinder_rack),"cube racks match");
        G.Assert(pickedObject==from_b.pickedObject, "pickedObject matches");
        
        G.Assert(variation==from_b.variation,"variation mismatch");
        G.Assert(SantoriniCell.sameCell(gods,from_b.gods),"gods mismatch");
        G.Assert(AR.sameArrayContents(godSelected, from_b.godSelected),"selection mismatch");
        G.Assert(AR.sameArrayContents(godChosen, from_b.godChosen),"selection mismatch");
        G.Assert(AR.sameArrayContents(playerGodI,from_b.playerGodI),"gods mismatch");
        G.Assert(godless==from_b.godless,"godless mismatch");
        G.Assert(AR.sameArrayContents(movedUp, from_b.movedUp),"movedup mismatch");
        G.Assert(sameCells(lastBuild,from_b.lastBuild),"lastBuild mismatch");
        G.Assert(AR.sameArrayContents(confirmWin, from_b.confirmWin),"confirmWin mismatch");
        G.Assert(SantoriniCell.sameCell(lastManDestination, from_b.lastManDestination), "lastManDestination mismatch");
 
    }

    /** 
     * Digest produces a 32 bit hash of the game state.  This is used 3 different
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
       Random r = new Random(64 * 1000); // init the random number generator
	   long v = super.Digest(r);

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //

        v ^= chip.Digest(r,pickedObject);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        v ^= Digest(r,variation.ordinal());
        v ^= Digest(r,gods);
        v ^= Digest(r,godSelected);
        v ^= Digest(r,godChosen);
        v ^= Digest(r,playerGodI);
        v ^= Digest(r,godless);
        v ^= Digest(r,movedUp);
        v ^= Digest(r,lastBuild);
        v ^= Digest(r,confirmWin);
        v ^= Digest(r,lastManDestination);
        return (v);
    }
   public SantoriniCell getCell(SantorId from,char col,int row)
   {		
		switch(from)
		{
		case Cube_Rack:
			return unplacedMan(cube_rack);
		case Cylinder_Rack:
			return unplacedMan(cylinder_rack);
		case Reserve_Rack:
			return reserve[row];
		case BoardLocation:
			return getCell(col,row);
		default: throw G.Error("not extected");
		}
   }
    public SantoriniCell getCell(SantoriniCell c)
   {	if(c==null) { return(null); }
   		switch(c.rackLocation())
    	{
   		default: throw G.Error("Not expecting");
   		case GodsId:
   			return(gods);
    	case Cube_Rack:	
    		return(cube_rack[c.row]);
    	case Cylinder_Rack:
    		return(cylinder_rack[c.row]);
    	case Reserve_Rack:
    		return(reserve[c.row]);
    	case BoardLocation:
    		return(getCell(c.col,c.row));
    	}
   }
   
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(SantoriniBoard from_b)
    {	super.copyFrom(from_b);
       
    	getCell(pickedSourceStack,from_b.pickedSourceStack);
    	getCell(droppedDestStack,from_b.droppedDestStack);
    	lastManDestination = getCell(from_b.lastManDestination);
    	uiStateStack.copyFrom(from_b.uiStateStack);
        for(int i=0;i<2;i++)
	        {
	        cube_rack[i].location=getCell(from_b.cube_rack[i].location);
	    	cylinder_rack[i].location=getCell(from_b.cylinder_rack[i].location);
	        }
        pickedObject = from_b.pickedObject;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        variation = from_b.variation;
        gods.copyFrom(from_b.gods);
        AR.copy(godSelected,from_b.godSelected);
        AR.copy(godChosen,from_b.godChosen);
        AR.copy(playerGodI,from_b.playerGodI);
        godless = from_b.godless;
        AR.copy(movedUp,from_b.movedUp);
        AR.copy(confirmWin,from_b.confirmWin);
        getCell(lastBuild,from_b.lastBuild);
        sameboard(from_b);
    }
    private void initGods()
    {	gods.reInit();
    	for(SantoriniChip ch : SantoriniChip.Gods) { gods.addChip(ch); }
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key; 
    	Random r = new Random(633632);
        gametype = gtype;
    	variation = Variation.findVariation(gtype);
    	
    	animationStack.clear();
     	for(int i=0,pl=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX; i++,pl=nextPlayer[pl])
    	{
     	cube_rack[i] = new SantoriniCell(r,SantorId.Cube_Rack,i);
     	cylinder_rack[i] = new SantoriniCell(r,SantorId.Cylinder_Rack,i);
     	reserve[i] = new SantoriniCell(r,SantorId.Reserve_Rack,i);
      	}    
     	cube_rack[0].addChip(SantoriniChip.Cube_A);
     	cube_rack[1].addChip(SantoriniChip.Cube_B);
    	cylinder_rack[0].addChip(SantoriniChip.Cylinder_A);
    	cylinder_rack[1].addChip(SantoriniChip.Cylinder_B);
    	gods = new SantoriniCell(r,SantorId.GodsId,0);
    	initGods();
    	godSelected = new boolean[SantoriniChip.Gods.length];
    	godChosen = new boolean[2];
    	playerGodI = new SantorId[]{SantorId.Godless,SantorId.Godless};
    	int map[]=getColorMap();
    	playerRack[map[FIRST_PLAYER_INDEX]] = cube_rack;
    	playerRack[map[SECOND_PLAYER_INDEX]] = cylinder_rack;
    	playerChip[map[FIRST_PLAYER_INDEX]] = SantoriniChip.Cube_A;
    	playerChip[map[SECOND_PLAYER_INDEX]] = SantoriniChip.Cylinder_A;
    	for(int i=0;i<4;i++)
    	{
    	reserve[0].addChip(SantoriniChip.MainTile);
    	reserve[1].addChip(SantoriniChip.Dome);
    	}
    	Init_Standard(variation);
    	allCells.setDigestChain(r);
        moveNumber = 1;

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
        case CONFIRM_WIN_STATE:
        case CONFIRM_STATE:
        case GodSelectConfirm:
        case GodChooseConfirm:
        case MoveOpponentState:
        case MoveOrPassState:
        case Build2_State:
        case BuildAgain_State:
        case RESIGN_STATE:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(nextPlayer[whoseTurn]);
            movedUp[whoseTurn]=false;
            lastBuild[whoseTurn]=null;
            lastManDestination = null;
            confirmWin[0]=confirmWin[1]=false;
            godless = false;
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
        {case RESIGN_STATE:
         case CONFIRM_STATE:
         case CONFIRM_WIN_STATE:
         case GodSelectConfirm:
         case GodChooseConfirm:
         case Build2_State:
         case MoveOpponentState:
         case BuildAgain_State:
         case MoveOrPassState:
            return (true);

        default:
            return (false);
        }
    }


   public boolean uniformRow(int player,SantoriniCell cell,int dir)
    {	for(SantoriniCell c=cell.exitTo(dir); c!=null; c=c.exitTo(dir))
    	{    SantoriniChip cup = c.topChip();
    	     if(cup==null) { return(false); }	// empty cell
    	     if(cup.chipNumber()!=player) { return(false); }	// cell covered by the other player
    	}
    	return(true);
    }
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(SantoriniState.GAMEOVER_STATE);
    }
    private int playerHasMoves(SantoriniCell loc)
    {	int n=0;
    	SantoriniChip top = loc.topChip();
    	for(int dir=0;dir<CELL_FULL_TURN;dir++)
		{	SantoriniCell adj = loc.exitTo(dir);
			if(isDestForMan(whoseTurn,top,loc,adj,true)) { n++; }
		}    	
    	return(n);
    }
    private boolean playerCanMove(SantoriniCell loc)
    {	SantoriniChip top = loc.topChip();
    	for(int dir=0;dir<CELL_FULL_TURN;dir++)
    		{	SantoriniCell adj = loc.exitTo(dir);
    			if(isDestForMan(whoseTurn,top,loc,adj,true)) { return(true); }
    		}
    	return(false);
    }
    private boolean canBuildAndMove(int who)
    {
    	if(activeGod(who)==SantorId.Prometheus)
    	{
    		SantoriniCell rack[]=playerRack[who];
    		for(int i=0;i<rack.length;i++)
    			{	SantoriniCell c = rack[i].location;
    				boolean can = canBuildAndMove(who,c);
    				if(can) { return(true); }
    			}
    	}
    	return(false);
    }
    private boolean canBuildAndMove(int who,SantoriniCell c)
    {	int lowerCount = 0;
    	int equalCount = 0;
    	int higherCount = 0;
    	int fromH = c.tileHeight();
    	int lowest = 99;
    	int nextp = nextPlayer[who];
    	boolean slaveOfLove = isSlaveOfLove(who,c);
    	boolean hades = activeGod(nextPlayer[who])==SantorId.Hades;
       	for(int dir=0;dir<CELL_FULL_TURN;dir++)
       	{
       		SantoriniCell d = c.exitTo(dir);
       		if((d!=null)
       				&& d.topChip().isTile() 
       				&& ((activeGod(nextp)!=SantorId.Cleo) || (d!=lastBuild[nextp]))
       				&& (!slaveOfLove || isSlaveOfLove(who,d)))
       		{	// this is a place we can move to
       			int dheight = d.height();
       			if(hades && (dheight+1==fromH)) { lowerCount++; }
       			else if(dheight<fromH) { lowerCount++; }
       			else if(dheight==fromH) { equalCount++; }
       			else { higherCount++; }
       			if(dheight<lowest) { lowest = dheight; }
       		}
       	}
       	switch(lowerCount+equalCount)
       	{
       	case 0:	return(false);
       	case 1: return((higherCount>0) || (lowerCount>1) || (lowest+1<fromH));
       	case 2: return(true);
       	default: return(true);
       	}      	 	
    }
    public boolean WinForPlayerByBlocking(int player)
    {
	// if were're waiting for a move, demand that a move is available
	SantoriniCell rack[] = playerRack[nextPlayer[whoseTurn]];
	boolean manCanMove = false;
	for(int i=0;i<2 && !manCanMove;i++) 
	{ SantoriniCell loc = rack[i].location;
	  if(loc!=null)
	  {	 manCanMove = playerCanMove(loc); 
	  }
	  else { manCanMove=true; }
	}
	if(!manCanMove) { return(true); }
   	return(false);
    }
    public boolean WinForPlayerByStepping(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==SantoriniState.GAMEOVER_STATE) { return(win[player]); }
    	{
    	SantoriniCell rack[] = playerRack[player];
    	for(int i=0;i<2;i++) 
    		{ SantoriniCell loc = rack[i].location;
    		  if(loc!=null) 
    			  {
    			  if (loc.height()>4) { return(true); } 			  
    			  }
    		}}
      	return(false);
    }

    
    // look for a win for player.
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=0.0;
    	SantoriniCell rack[]=playerRack[player];
    	for(int i=0;i<rack.length;i++)
    	{	SantoriniCell c = rack[i].location;
    		if(c!=null) 
    		{ finalv += c.height()*3; 
    		  finalv += playerHasMoves(c);
    		}
    		
    	}
    	
    	//G.Error("not implemented");
    	return(finalv);
    }

    public void acceptPlacement()
    {	pickedSourceStack.clear();
    	droppedDestStack.clear();
    	uiStateStack.clear();
    	pickedObject = null;
    	//lastManDestination = null; not included
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {	SantoriniCell to = destCell();
    	if(to!=null) 
    	{	droppedDestStack.pop();
    		pickedObject = to.removeTop(); 
    		if(pickedObject.isMan()) 
    			{ movedUp[whoseTurn]=false; 
    			  lastManDestination=null;
    			  confirmWin[0]=confirmWin[1]=false;
    			}
    		else {
  			  lastBuild[whoseTurn]=null;    			
    		}
    		recordManLocation(pickedObject,null);
    		setState(uiStateStack.pop());
       		boolean unpush = (uiStateStack.top()==SantoriniState.Unpush_State);
    		if(unpush) 
    		{ 
    			uiStateStack.pop();
   	    		SantoriniCell src = sourceCell();
   	    		SantoriniChip victim = src.topChip();
   	    		if(!victim.isMan())
   	    		{	// must be push rather than swap
   	    			int dir = findDirection(src,to);
   	    			src = to.exitTo(dir);
   	    		}
   	    		SantoriniChip top = src.removeTop();
    			to.addChip(top);
    			recordManLocation(top,to);
    		}
     	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	if(pickedObject!=null)
    {	SantoriniCell src = sourceCell();
    	if(src!=null) 
    		{ pickedSourceStack.pop();
    		  if(src.onBoard) 
    		  	{ src.addChip(pickedObject); 
    		  	  recordManLocation(pickedObject,src);
    		  	} 
    		  pickedObject=null;
    		}
    	}
    }
    private boolean canDrop(SantoriniChip ch,SantoriniCell c)
    {	if(ch!=null)
    	{
    	switch(c.height())
    	{
    	default:	
    		if(activeGod(whoseTurn)==SantorId.Atlas) { return(true); }
    		return ch.isTile();
    	case 4:
    		return(ch.isDome());
    	}}
    	return(true);
    }
    // 
    // drop the floating object on the boad.
    //
    private void dropBoardCell(SantoriniCell c)
    {     	G.Assert(c.onBoard,"is on board");
    		// level 3 becomes a dome
    		c.addChip(pickedObject);
    		recordManLocation(pickedObject,c);
    		SantoriniCell s = sourceCell();
            droppedDestStack.push(c);
    		uiStateStack.push(board_state);
        	if(pickedObject.isMan())
        	{	lastManDestination = c;
        		checkForWin(whoseTurn,s,c);
        	}
        	else { 
        		lastBuild[whoseTurn] = c;
        	}
    		pickedObject = null;
    		setNextStateAfterDrop();
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(SantoriniCell c)
    {	return(destCell()==c);
    }
    public boolean isDest(char col,int ro)
    {	return(isDest(getCell(col,ro)));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    { SantoriniChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
   
    private void recordManLocation(SantoriniChip ch,SantoriniCell loc)
    {
       	for(int i=0;i<2;i++) 
   		{ if(ch==cube_rack[i].topChip()) { cube_rack[i].location=loc; }
   		  if(ch==cylinder_rack[i].topChip()) { cylinder_rack[i].location=loc; }
   		}
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickBoardCell(SantoriniCell c)
    {

        pickedSourceStack.push(c);
        pickedObject = c.topChip();
       	lastDropped = pickedObject;
       	recordManLocation(pickedObject,null);
      	c.removeTop();
    }
    
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(SantoriniCell c)
    {	return(c==pickedSourceStack.top());
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(char col, int row)
    {
        return (isSource(getCell(col,row)));
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //
    private void checkForWin(int who,SantoriniCell from,SantoriniCell to)
    {	int dheight = to.tileHeight();
    	int sheight = from.tileHeight();
    	boolean win = dheight==4;
    	switch(activeGod(who))
    	{
    	case Athena:
    		movedUp[who] =(dheight>sheight);
    		break;
    	case Pan:
    		if(sheight>=dheight+2) 
    		{ win = true;
    		}
    		break;
    	default: break;
    	}

    	if(win)
    	{
  		  confirmWin[who]=true;
    	}
    }
    private void setNextStateAfterDrop()
    {	SantoriniState estate = board_state;
    	if(estate==SantoriniState.BuildOrMoveState)
    	{	SantoriniChip top = destCell().topChip();
    		if(top.isMan()) 
    				{ estate = SantoriniState.MOVE_STATE; 
    				}
    	}
        switch (estate)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_WIN_STATE:
        case CONFIRM_STATE:
        	setNextStateAfterDone(); 
        	break;
        case MoveOpponentState:
        case MoveOnLevelState:
        case MOVE_STATE:
        	setState((confirmWin[0]||confirmWin[1])
        				?SantoriniState.CONFIRM_WIN_STATE
        				:SantoriniState.BUILD_STATE);
			break;
        case MoveOrPassState:
        case MAN1_STATE:
        	setState(SantoriniState.MAN2_STATE);
        	break;
        case BuildOrMoveState:
        	setState(SantoriniState.MoveOnLevelState);
        	lastBuild[whoseTurn] = destCell();	// prometheus build
        	 
        	break;
        case BUILD_STATE:
         	SantoriniCell destCell = destCell();
        	SantoriniChip dchip = destCell.topChip();
        	setState(SantoriniState.CONFIRM_STATE);
        	switch(activeGod(whoseTurn))
        	{
        	default: break;
        	case Dionysus:
        		if(dchip.isDome())
            		{	
        			godless = true;
            		setState(SantoriniState.MoveOpponentState);
             		}
        		break;
        	case Hercules:
        		setState(SantoriniState.Build2_State);
        		break;
        	case Hephaestus:
        		if(destCell.height()<4)
        		{
        		setState(SantoriniState.BuildAgain_State);
        		}
        		break;
        	}
        	break;
        case Build2_State:
        case BuildAgain_State:
		case MAN2_STATE:
         	setState(SantoriniState.CONFIRM_STATE);
        	break;

        case PUZZLE_STATE:
			acceptPlacement();
			lastManDestination = null;
            break;
        }
    }
    private boolean godsSelected()
    {
    	return(gods.height()==nPlayers());
    }
    private boolean godsChosen()
    {
    	return(activeGod(0)!=SantorId.Godless);
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case GodChooseConfirm:
    		{
    		boolean sel[]=godSelections();
    		int selIndex = sel[0] ? 0 : 1;
    		playerGodI[whoseTurn^1] = gods.chipAtIndex(selIndex).id;
    		playerGodI[whoseTurn] = gods.chipAtIndex(selIndex^1).id; 
    		setState(SantoriniState.MoveOrPassState);
     		}
    		break;
    	case GodSelectConfirm:
    		{
    		gods.reInit();
    		boolean sel[] = godSelections();
    		for(int i=0,lim=sel.length;i<lim;i++)
    			{	if(sel[i]) { gods.addChip(SantoriniChip.Gods[i]); }
    			}
    		}
    		setState(SantoriniState.GodChoose);
    		break;
		case CONFIRM_WIN_STATE:
			setState(SantoriniState.GAMEOVER_STATE);
			break;
    	case GAMEOVER_STATE: 
    		break;
    		
    	case PUZZLE_STATE:
    		switch(variation)
    		{
    		default: break;
    		case santorini_gods:
    			if(godsSelected())
    			{
    				if(godsChosen())
    				{
    				}
    				else { setState(SantoriniState.GodChoose); return; }
    			}
    			else 
    				{ 
    				  setState(SantoriniState.GodSelect); 
    				  return;
    				}
    		}
    		
			//$FALL-THROUGH$
			
		case Build2_State:
		case BuildAgain_State:
     	case CONFIRM_STATE:
    	case MOVE_STATE:
    	case MoveOpponentState:
    	case MoveOrPassState:
    		{
    		SantoriniCell rack[] = playerRack[whoseTurn];
    		SantoriniCell loc1 = rack[0].location;
    		SantoriniCell loc2 = rack[1].location;
    		if((loc1!=null) && (loc2!=null)) 
    			{ 	if(hasManMoves(whoseTurn)) 
    					{
    					setState(canBuildAndMove(whoseTurn)
    								? SantoriniState.BuildOrMoveState
    								: SantoriniState.MOVE_STATE);
    					}
    					else {
    					setGameOver(false,true);	// can't move, lose
    					}
    			}
    		else if((loc1!=null) || (loc2!=null)) { setState(SantoriniState.MAN2_STATE); }
    		else { setState(SantoriniState.MAN1_STATE); }
    		}
    		break;
    	}

    }
   

    
    private void doDone()
    {	acceptPlacement();
        lastManDestination = null;
        switch(board_state)
        {
        case RESIGN_STATE:
        	setGameOver(false,true);
        	break;
        case CONFIRM_WIN_STATE:
        	setGameOver(confirmWin[whoseTurn],confirmWin[nextPlayer[whoseTurn]]);
        	break;
        default:
        	setNextPlayer();
        	setNextStateAfterDone(); 
        }
    }
    
    public SantoriniCell unplacedMan(SantoriniCell rack[])
    {	boolean used1=false,used2=false;
    	SantoriniChip u1 = rack[0].topChip();
    	SantoriniChip u2 = rack[1].topChip();
    	for(SantoriniCell c = allCells; c!=null; c=c.next)
    	{	SantoriniChip top = c.topChip();
    		if(top==u1) { used1 = true; }
    		else if(top==u2) { used2 = true; }
    	}
    	if(!used1) { return(rack[0]); }
    	if(!used2) { return(rack[1]); }
    	return(null);
    }
    private int toggleSelection(int n)
    {	int nselected = 0;
    	SantoriniChip god = SantoriniChip.findGod(n);
    	boolean sel[] = godSelections();
    	for(int lim=sel.length-1; lim>=0; lim--) 
    		{ if(gods.chipAtIndex(lim)==god) { sel[lim]=!sel[lim]; }
    		  if(sel[lim]) { nselected++; }
    		}   
    	return(nselected);
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	SantoriniMovespec m = (SantoriniMovespec)mm;
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
    	if(replay==replayMode.Replay) { animationStack.clear(); }
    	boolean swap = false;
    	boolean push = false;
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_PUSH:
        	push = true;
			//$FALL-THROUGH$
		case MOVE_SWAPWITH:
        	swap = true;
			//$FALL-THROUGH$
		case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case MOVE_STATE:
        		case BuildOrMoveState:
        		case MoveOnLevelState:
        		case MoveOpponentState:
        			G.Assert((pickedObject==null),"nothing is moving");
        			SantoriniCell from = getCell(m.from_col, m.from_row);
        			SantoriniCell to = getCell(m.to_col,m.to_row); 
        			if(replay.animate)
        			{
        				animationStack.push(from);
        				animationStack.push(to);
        			}
        			pickBoardCell(from);
        			m.chip = pickedObject;
        			if(swap)
        			{	SantoriniCell destination = from;
        				if(push) 
        					{ int dir = findDirection(from,to); 
        					  destination = to.exitTo(dir);
        					}
        				SantoriniChip opponent = to.removeTop();
        				G.Assert(opponent.isMan(),"should be a man");
        				checkForWin(nextPlayer[whoseTurn],from,destination);
        				destination.addChip(opponent);
        				recordManLocation(opponent,destination);
        				if(replay.animate)
            			{
        				animationStack.push(to);
        				animationStack.push(destination);
            			}
        			}
        			dropBoardCell(to); 
        			break;
        	}
        	break;
		case MOVE_DROP_PUSH:
			push = true;
			//$FALL-THROUGH$
		case MOVE_DROP_SWAP:
			swap = true;
			//$FALL-THROUGH$
		case MOVE_DOME:
        case MOVE_DROPB:
        	{
        	boolean suppliedSource = (pickedObject==null);
			SantoriniCell dst = getCell(m.to_col, m.to_row); 
        	if(pickedObject==null)
        	{	// supply the implied object
        		SantoriniCell src = null;
        		switch(board_state)
        		{
        		case MoveOrPassState:
        		case MAN1_STATE:
        		case MAN2_STATE:
        			src = unplacedMan(playerRack[whoseTurn]);
        			break;
        		case BUILD_STATE:
        		case Build2_State:
        		case BuildOrMoveState:
        		case BuildAgain_State:
        		case PUZZLE_STATE:
            		if((m.op==MOVE_DOME) || (dst.height()>3)) 
        			{ src = reserve[1]; 
        			}
            		else 
            		{src = reserve[0];
            		}
        			break;
        		default: throw G.Error("not expecting dropb in state %s",board_state);
        		}
    		    pickedSourceStack.push(src);
    		    pickedObject = src.topChip();    
    		    m.chip=pickedObject;
        	}

			SantoriniCell src = sourceCell();

			if(src==dst)
			{
				unPickObject();				
			}
			else
			{
			SantoriniChip opponent = dst.topChip();
			if(opponent.isMan())
			{	SantoriniCell newloc = src;
				if(push)
				{
					int dir = findDirection(src,dst);
					newloc = dst.exitTo(dir);
				}
				dst.removeTop();
				checkForWin(nextPlayer[whoseTurn],dst,newloc);
				newloc.addChip(opponent);
				uiStateStack.push(SantoriniState.Unpush_State);
				recordManLocation(opponent,newloc);
 				if(replay.animate)
				{
				animationStack.push(dst);
				animationStack.push(newloc);
				}
			}
			m.chip = pickedObject;
            dropBoardCell(dst);
			if((replay.animate)
					&& (suppliedSource || (replay==replayMode.Single)))
			{
				animationStack.push(src);
				animationStack.push(dst);
			}
			}
        	}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ 	
        			unDropObject(); 
        			if(board_state==SantoriniState.BuildAgain_State)
        			{
        				unPickObject();
        				unDropObject();
        			}
        		}
        	else 
        		{ pickBoardCell(getCell(m.from_col, m.from_row));
        		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	pickedObject = null;
        	break;

        case MOVE_PICK:
			{
 			if(pickedObject!=null) { unPickObject();  }
 			SantoriniCell src = getCell(m.source,m.from_col,m.from_row);
 
		    pickedSourceStack.push(src);
		    pickedObject = src.topChip();
 			}
            break;

        case MOVE_START:
            unPickObject();
            acceptPlacement();
            lastManDestination = null;
            setWhoseTurn(m.player);
            setState(SantoriniState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerByStepping(whoseTurn);
            	int next = nextPlayer[whoseTurn];
            	boolean win2 = WinForPlayerByStepping(next) || WinForPlayerByBlocking(next);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setState(SantoriniState.PUZZLE_STATE);
            	   setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?SantoriniState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setState(SantoriniState.PUZZLE_STATE);

            break;
        case MOVE_SELECT:
        	{
        	int nselected = toggleSelection(m.from_row);
         	switch(board_state)
        	{
        	case GodSelect:
        	case GodSelectConfirm:
        		{
        		setState((nselected==2) ? SantoriniState.GodSelectConfirm : SantoriniState.GodSelect);
        		}
        		break;
        	case GodChoose:
        	case GodChooseConfirm:
	    		{
	    		setState((nselected==1) ? SantoriniState.GodChooseConfirm : SantoriniState.GodChoose);
	    		}
    		break;
        	default: throw G.Error("Not expecting state %s",board_state);
        	}}
        	break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        default:
        	cantExecute(m);
        }


        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
    	if(player>=0 && unplacedMan(playerRack[player])==null) { return(false); }

        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_WIN_STATE:
        case CONFIRM_STATE:
 		case GAMEOVER_STATE:
 		case MOVE_STATE:
 		case RESIGN_STATE:
 		case MoveOpponentState:
 		case GodSelect:
 		case GodChoose:
 		case GodChooseConfirm:
 		case MoveOnLevelState:
 		case GodSelectConfirm:
			return(false);
        case MAN1_STATE:
        case MAN2_STATE:
        case MoveOrPassState:
        	return(player==whoseTurn);
        case PUZZLE_STATE:
        case BUILD_STATE:
        case Build2_State:
        case BuildOrMoveState:
        case BuildAgain_State:
         	return(true);
        }
    }
    public boolean canForceBack(SantoriniCell from,SantoriniCell to)
    {
    	int dir = findDirection(from,to);
    	SantoriniCell next = to.exitTo(dir);
    	return((next!=null) && next.topChip().isTile());
    }
    public boolean isDestForMan(int who,SantoriniChip man,SantoriniCell from,SantoriniCell to,boolean canAscend)
    {	
		if(to!=null)
		{	int otherPlayer = nextPlayer[who];
			SantoriniChip top = to.topChip();
			SantorId myGod = activeGod(who);
			SantorId hisGod = activeGod(otherPlayer);
			boolean apollo = myGod==SantorId.Apollo;
			boolean ares = myGod==SantorId.Ares;
			boolean hades = hisGod==SantorId.Hades;
			boolean cleo = hisGod==SantorId.Cleo;
		if((top.isTile() && (!cleo ||  (to!=lastBuild[otherPlayer])))
			|| ((apollo|ares) 
					&& top.isMan() 
					&& (top.playerIndex()!=who)
					&& (!ares || canForceBack(from,to))
					&& (!apollo || canBuildFrom(to))
					)
			)
		{
	    	boolean noUp = !canAscend || movedUp[otherPlayer];
			int sheight = from.tileHeight();	// compensate if man still standing
			int dheight = to.tileHeight();
			if(hades && dheight<sheight) { return(false); }
			return(dheight<=sheight+(noUp?0:1));
		}}
		return(false);
	}
    
    public boolean LegalToHitBoard(SantoriniCell dcell,Hashtable<SantoriniCell,SantoriniMovespec>targets)
    {	
        switch (board_state)
        {
        case CONFIRM_WIN_STATE:
		case CONFIRM_STATE:
			return(isDest(dcell));
			
		case BUILD_STATE:
		case Build2_State:
		case BuildAgain_State:
		case MoveOrPassState:
		case MoveOpponentState:
		case MoveOnLevelState:
		case MAN1_STATE:
 		case MAN2_STATE:
		case BuildOrMoveState:
		case MOVE_STATE:
 			{
			return (targets.get(dcell)!=null);
 			}
 		case GodSelect:
 		case GodChoose:
 		case GodChooseConfirm:
 		case GodSelectConfirm:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
  			return(false);
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return((pickedObject==null)?(!dcell.isEmpty()):true);
        }
    }

 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(SantoriniMovespec m)
    {
        robotStateStack.push(board_state); //record the starting state. The most reliable
        robotLastMan.push(lastManDestination);
        robotLastMan.push(lastBuild[0]);
        robotLastMan.push(lastBuild[1]);
        int info = (movedUp[0] ? 1 : 0) 
        			| (movedUp[1]?2:0) 
        			| (confirmWin[0]?4:0)
        			| (confirmWin[1]?8:0)
        			| (godless ? 16 : 0);
        robotInfoStack.push(info);
        
        // to undo state transistions is to simple put the original state back.
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);    
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        pickedObject = null;
        Execute(m,replayMode.Replay);
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(SantoriniMovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);
        SantoriniState newstate = robotStateStack.pop();
        pickedObject = null;
        switch (m.op)
        {
        case MOVE_SELECT:
        	{
        	toggleSelection(m.from_row);
        	}
        	break;
        case MOVE_DOME:
        case MOVE_DROPB:
        	{
        	SantoriniCell c = getCell(m.to_col,m.to_row);
        	pickBoardCell(c);
        	acceptPlacement();
        	}
        	break;
        case MOVE_PUSH:
       		{
        	SantoriniCell c = getCell(m.to_col,m.to_row);
        	pickBoardCell(c);
        	SantoriniCell d = getCell(m.from_col,m.from_row);
        	int dir = findDirection(d,c);
        	SantoriniCell e = c.exitTo(dir);
        	SantoriniChip ch = e.removeTop();
        	G.Assert(ch.isMan(),"should be a man");
        	c.addChip(ch);
        	recordManLocation(ch,c);
        	dropBoardCell(d);
        	acceptPlacement();
        	lastManDestination = null;
        	}
       		break;
        case MOVE_SWAPWITH:
        	{
        	SantoriniCell c = getCell(m.to_col,m.to_row);
        	pickBoardCell(c);
        	SantoriniCell d = getCell(m.from_col,m.from_row);
        	SantoriniChip ch = d.removeTop();
        	G.Assert(ch.isMan(),"should be a man");
        	c.addChip(ch);
        	recordManLocation(ch,c);
        	dropBoardCell(d);
        	acceptPlacement();
        	lastManDestination = null;
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	{
        	SantoriniCell c = getCell(m.to_col,m.to_row);
        	pickBoardCell(c);
        	SantoriniCell d = getCell(m.from_col,m.from_row);
        	dropBoardCell(d);
        	acceptPlacement();
        	lastManDestination = null;
        	}
        	break;
        default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
        	switch(newstate){
        	default: throw G.Error("Not expecting state %s",newstate); 
        	case MoveOrPassState: 
        	case BuildAgain_State:
        	case CONFIRM_STATE:
        	case MoveOpponentState:
        	case CONFIRM_WIN_STATE:
        	case MAN2_STATE:
        	case Build2_State:
        		break;
        	case GodChooseConfirm:
        		playerGodI[0] = playerGodI[1] = SantorId.Godless;
        		break;
        	case GodSelectConfirm:
           		initGods();
           		break;
        	}
            break;
        case MOVE_RESIGN:
            break;
        }
        setState(newstate);
        lastBuild[1] = robotLastMan.pop();
        lastBuild[0] = robotLastMan.pop();
        lastManDestination = robotLastMan.pop();
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
        int info = robotInfoStack.pop();
        movedUp[0] = (info&1)!=0;
        movedUp[1] = (info&2)!=0;
        confirmWin[0] = (info&4)!=0;
        confirmWin[1] = (info&8)!=0;
        godless = (info&16)!=0;

 }
 
 Hashtable<SantoriniCell,SantoriniMovespec> getTargets()
 {
	 switch(board_state)
	 {
	 default: throw G.Error("Not expecting state ",board_state);
	 case PUZZLE_STATE:
	 case GAMEOVER_STATE:
	 case GodSelect:
	 case GodChoose:
	 case GodChooseConfirm:
	 case GodSelectConfirm:
	 case CONFIRM_WIN_STATE:
	 case CONFIRM_STATE:
	 case RESIGN_STATE:
		 return(null);
		 
	 case BuildOrMoveState:
	 case BUILD_STATE:
	 case Build2_State:
	 case BuildAgain_State:
	 case MAN1_STATE:
	 case MAN2_STATE:
	 case MoveOnLevelState:
	 case MoveOpponentState:
	 case MoveOrPassState:
	 case MOVE_STATE:
		 Hashtable<SantoriniCell,SantoriniMovespec> res = new Hashtable<SantoriniCell,SantoriniMovespec>();
		 CommonMoveStack moves = GetListOfUIMoves();
		 for(int lim = moves.size()-1; lim>=0; lim--)
		 {
			 SantoriniMovespec m = (SantoriniMovespec)moves.elementAt(lim);
			 switch(m.op)
			 {
			 default: throw G.Error("Not expecting move %s",m);
			 case MOVE_DONE: 
				 break;
			 case MOVE_DROP:
				 res.put(getCell(m.source,m.from_col,m.from_row),m);
				 break;
			 case MOVE_PICK:
				 res.put(getCell(m.source,m.from_col,m.from_row),m);
				 break;
			 case MOVE_BOARD_BOARD:	
			 case MOVE_SWAPWITH:
			 case MOVE_PUSH:
			 case MOVE_PICKB:
				 res.put(getCell(m.from_col,m.from_row),m);
				 break;
			 case MOVE_DROPB:
			 case MOVE_DROP_SWAP:
			 case MOVE_DROP_PUSH:
			 case MOVE_DOME:
				 res.put(getCell(m.to_col,m.to_row),m);
				 break;
			 }
		 }
		 
		 return(res);
	 }
 }
 CommonMoveStack GetListOfUIMoves()
 {	CommonMoveStack all = GetListOfMoves();
 	{
 	SantoriniCell d = (pickedObject==null) ? destCell() : null;
 	if(d!=null && d.onBoard) { all.push(new SantoriniMovespec(whoseTurn,MOVE_PICKB,d.col,d.row)); }
 	}
 	{
 	SantoriniCell s = sourceCell();
 	if(s!=null && s.onBoard) { all.push(new SantoriniMovespec(whoseTurn,MOVE_DROPB,s.col,s.row)); }
 	}
 	return(all);
 }
 private boolean isSlaveOfLove(int who,SantoriniCell c)
 {	int nextP = nextPlayer[who];
 	if(activeGod(nextP)==SantorId.Aphrodite)
 	{SantoriniChip toph = c.topChip();
 	 if(toph.isMan() && (toph.playerIndex()!=who))	{ return(true); } 	// apollo or ares push/swap
	 for(int dir=0;dir<CELL_FULL_TURN;dir++)
		{	SantoriniCell e = c.exitTo(dir);
			if(e!=null)
			{
			SantoriniChip top = e.topChip();
			if(top.isMan() && (top.playerIndex()==nextP)) { return(true); }
			}
		}
 	}
	return(false); 
 }
 
 private boolean hasManMoves(int who) { return(addManMoves(null,who,true)); }

 
 private boolean addManMoves(CommonMoveStack all,int who,SantoriniCell c,SantoriniChip ch0,
		 	boolean slaveOfLove,SantoriniCell origin,boolean canAscend)
 {	boolean some = false;
 	SantoriniChip top = (ch0==null) ? c.topChip() : ch0;
 	for(int dir=0;dir<CELL_FULL_TURN;dir++)
		{	SantoriniCell e = c.exitTo(dir);
			if((e!=origin)
				&& (origin!=null))
			{	// artemis, can't move back to the origin,
				// and shouldn't generate duplicates
				if(e!=null)
				{
				for(int lim=all.size()-1; lim>=0; lim--)
				{
					SantoriniMovespec m = (SantoriniMovespec)all.elementAt(lim);
					if((m.to_col==e.col) && (m.to_row==e.row))
					{
						e = null;
						break;
					}
				}}
			}
			if((e!=null)
				&& isDestForMan(who,top,c,e,canAscend)
				&& (!slaveOfLove || isSlaveOfLove(who,e))	
			)
			{
			boolean tile = e.topChip().isTile();
			// nominally a permitted move
			if(all==null) { return(true); }
			// apollo moves to an occupied square
			if(ch0==null)	// full moves
			{	
				int op = (tile 
							? MOVE_BOARD_BOARD 
							: (activeGod(who)==SantorId.Ares)
								? MOVE_PUSH
								: MOVE_SWAPWITH);
				SantoriniCell src = origin==null ? c : origin;
				SantoriniMovespec newmove = 
						new SantoriniMovespec(whoseTurn /*not who */,op,
								src.col,src.row,e.col,e.row);
				
				all.push(newmove);
			}
			else
			{
			int op = tile 
						? MOVE_DROPB
						: (activeGod(who)==SantorId.Ares)
							? MOVE_DROP_PUSH
							: MOVE_DROP_SWAP;
			all.addElement(new SantoriniMovespec(whoseTurn /* not who */,op,e.col,e.row));
			}
			some = true;
			}

		}
		
	 return(some);
 }
 private boolean addManMoves(CommonMoveStack all,int who,boolean canAscend)
 {	boolean some = false;
 	if(pickedObject!=null)
		{	SantoriniCell c = sourceCell();
			if(c!=null && c.onBoard)
			{
			boolean slaveOfLove = isSlaveOfLove(who,c);
			some = addManMoves(all,who,c,pickedObject,slaveOfLove,null,canAscend);
			}
		}
		else {
		SantoriniCell rack[]=playerRack[who];
		for(int i=0;i<rack.length;i++)
			{	SantoriniCell c = rack[i].location;
				if(!canAscend)
				{	// can't ascend is prometheus, having built first,
					// and we must also be adjacent to the built space.
					if(!c.isAdjacentTo(lastBuild[who])) { c = null; }
				}
				if(c!=null)
				{
				boolean slaveOfLove = isSlaveOfLove(who,c);
				some|=addManMoves(all,who,c,null,slaveOfLove,null,canAscend);
				if(some && all==null) { return(true); }
				}
			}
		}
 	
 	// artemis lets you move twice, but not stand still
 	boolean isArtemis = (activeGod(who)==SantorId.Artemis);
 	if(some && isArtemis)
 	{
 		for(int lim = all.size()-1; lim>=0; lim--)
			{
 			SantoriniMovespec m = (SantoriniMovespec)all.elementAt(lim);
 			SantoriniCell oldsrc = (pickedObject==null) 
 									? getCell(m.from_col,m.from_row)
 									: sourceCell();
 			SantoriniCell newsrc = getCell(m.to_col,m.to_row);
 			boolean slaveOfLove = isSlaveOfLove(who,newsrc);
 			addManMoves(all,who,newsrc,pickedObject,slaveOfLove,oldsrc,canAscend);
			}
 	}
	return(some);
 }
 // for promethian moves after build, there has to be a non-ascending
 // spot that also satisfies slave of love
 private boolean hasMovesAfterBuild(int who,SantoriniCell from,SantoriniCell build)
 {	boolean slaveOfLove = isSlaveOfLove(who,from);
 	int sh = from.tileHeight();
 	int nextP = nextPlayer[who];
 	SantorId nextGod = activeGod(nextP);
 	boolean hades = nextGod==SantorId.Hades;
 	boolean cleo = nextGod==SantorId.Cleo;
 	for(int dir=0;dir<CELL_FULL_TURN;dir++)
	 {	SantoriniCell d = from.exitTo(dir);
	 	if((d!=null)
	 			&& (!cleo || (d!=lastBuild[nextP]))	// space excluded by cleo
	 			&& (!slaveOfLove || isSlaveOfLove(who,d)))
	 			{
	 			SantoriniChip top = d.topChip();
	 			if(top.isTile())
	 				{
	 				int dh = d.height()+((d==build)?1:0);
		 			if(hades?(dh==sh) : (dh<=sh)) { return(true); }
	 				}
	 			}
	 }
	 return(false);
 }
 private void addPrometheanBuildMoves(CommonMoveStack all,int who)
 {	SantoriniCell rack[]=playerRack[who];
 	if(pickedObject==null)
 	{
 	for(int i=0;i<rack.length;i++)
		{	SantoriniCell c = rack[i].location;
			if(canBuildAndMove(who,c))
			{	
				for(int dir=0;dir<CELL_FULL_TURN;dir++)
				 {	SantoriniCell d = c.exitTo(dir);
				 	if(d!=null
				 			&& d.topChip().isTile()
				 			&& hasMovesAfterBuild(who,c,d))
				 	{	
				 		addBuildMove(all,d,who);
				 	}
				 }
			}
		}}
 }
 private void addBuildMove(CommonMoveStack all,SantoriniCell d,int who)
 {
	 switch(d.height())
		{
		case 4:
			all.addElement(new SantoriniMovespec(who,MOVE_DOME,d.col,d.row));
			break;
		default:
			all.addElement(new SantoriniMovespec(who,MOVE_DROPB,d.col,d.row));
			if((pickedObject==null)
				&& (activeGod(who)==SantorId.Atlas))
				{
				all.addElement(new SantoriniMovespec(who,MOVE_DOME,d.col,d.row));							
				}
	}
 }
 private void addBuildMoves(CommonMoveStack all,SantoriniCell c,int who,SantoriniCell except)
 {
	 for(int dir=0;dir<CELL_FULL_TURN;dir++)
	 {	SantoriniCell d = c.exitTo(dir);
		if((d!=null) 
				&& (d.topChip()==SantoriniChip.MainTile)
				&& (d!=except)
				&& canDrop(pickedObject,d))
			{	addBuildMove(all,d,who);			
			}
		} 
 }
 private boolean canBuildFrom(SantoriniCell c)
 {
	 for(int dir=0;dir<CELL_FULL_TURN;dir++)
	 {	SantoriniCell d = c.exitTo(dir);
		if((d!=null) 
				&& (d.topChip()==SantoriniChip.MainTile)) 
			{ return(true); 
			}
	 }
	 return(false);
 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default: throw G.Error("Not implemented for state %s",board_state);
 	case GodSelectConfirm:
 	case GodChooseConfirm:
 	case CONFIRM_STATE:
 	case CONFIRM_WIN_STATE:
 		all.push(new SantoriniMovespec(whoseTurn,MOVE_DONE));
 		break;
 	case GodChoose:
 	case GodSelect:
 		{
 		boolean sel[] = godSelections();
 		for(int lim=gods.height()-1; lim>=0; lim--)
 		{
 			if(!sel[lim]) { all.push(new SantoriniMovespec(whoseTurn,MOVE_SELECT,
 					SantoriniChip.findGodIndex(gods.chipAtIndex(lim)))); 
 			}
 		}}
 		break;
 	case MoveOrPassState:
 		all.push(new SantoriniMovespec(whoseTurn,MOVE_DONE));
		//$FALL-THROUGH$
	case MAN1_STATE:
 	case MAN2_STATE:
 		// place a man on an unoccupied space
 		for(SantoriniCell c=allCells; c!=null; c=c.next)
 		{	if(c.topChip()==SantoriniChip.MainTile)
 			{	all.addElement(new SantoriniMovespec(whoseTurn,MOVE_DROPB,c.col,c.row));
 			}
 		}
 		break;
 	case MoveOpponentState:
 		addManMoves(all,nextPlayer[whoseTurn],true);	// dionysys
 		all.push(new SantoriniMovespec(whoseTurn,MOVE_DONE));
 		break;
 	case BuildOrMoveState:
 		addPrometheanBuildMoves(all,whoseTurn);
 		addManMoves(all,whoseTurn,true);
 		break;
 	case MoveOnLevelState:
 		addManMoves(all,whoseTurn,false);
 		//G.Assert(all.size()>0,"no moves generated");
 		break;
 	case MOVE_STATE:
 		// move a man
 		addManMoves(all,whoseTurn,true);
 		break;
 	case BuildAgain_State:
 		{
 	 	 	SantoriniCell e = lastBuild[whoseTurn];
 	 	 	all.addElement(new SantoriniMovespec(whoseTurn,MOVE_DROPB,e.col,e.row));		
 	 	 	all.push(new SantoriniMovespec(whoseTurn,MOVE_DONE));
 		}
 		break;
 	case Build2_State:
 		{
 	 	SantoriniCell e = lastBuild[whoseTurn];
 	 	SantoriniCell d = lastManDestination();
 	 	G.Assert(d.topChip().isMan(),"should be a man");
 	 	addBuildMoves(all,d,whoseTurn,e);	
 	 	all.push(new SantoriniMovespec(whoseTurn,MOVE_DONE));
 		}
 		break;
 	case BUILD_STATE:
 		{
 		// build adjacent to the moved man
 		SantoriniCell c = lastManDestination();
 		addBuildMoves(all,c,whoseTurn,null);
 		}
 		break;
 	}
 	//if(all.size()==0) {throw G.Error("no moves generated"); }
 	//G.print(""+all.size());
  	return(all);
 }
}
