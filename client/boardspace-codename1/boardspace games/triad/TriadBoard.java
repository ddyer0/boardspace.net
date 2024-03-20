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
package triad;

import java.util.Hashtable;
import bridge.Color;
import lib.*;
import online.game.*;
import triad.TriadChip.ChipColor;


/**
 * TriadBoard knows all about the game of Hex, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves. 
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

class TriadBoard extends hexBoard<TriadCell> implements BoardProtocol,TriadConstants
{	
	private TriadState unresign;
 	private TriadState board_state;
 	CellStack animationStack = new CellStack();
	public TriadState getState() {return(board_state); }
	public void setState(TriadState st) 
	{ 	unresign = (st==TriadState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    //
    // private variables
    //
	private int chips_on_board[] = new int[3];			// number of chips currently on the board
    private TriadChip playerChip[]= new TriadChip[3];
    private TriadId playerColorPool[] = new TriadId[3];
    
    private TriadCell blue = new TriadCell(TriadId.Blue_Chip_Pool,TriadChip.BlueStone);
    private TriadCell green = new TriadCell(TriadId.Green_Chip_Pool,TriadChip.GreenStone);
    private TriadCell red =	new TriadCell(TriadId.Red_Chip_Pool,TriadChip.RedStone);
    private TriadCell playerCell[] = { red,	green, blue};
    
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public TriadChip getPlayerChip(int p) { return(playerChip[p]); }
	public TriadCell getPlayerCell(int p) { return(playerCell[p]); }
	public TriadId getPlayerColor(int p) { return(playerColorPool[p]); }

	// this is called when 3 reps are detected.  Out to be extremely rare in Triad
	public void SetDrawState() { setState(TriadState.DRAW_STATE); };	
	
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public TriadChip pickedObject = null;
    public TriadChip lastPicked = null;
    private TriadCell pickedSource = null; 
    private TriadCell droppedDest = null;
    private TriadCell bunnyDrop = null;	// cell dropped on for the bunny
    private CellStack captureCellStack = new CellStack();
    private ChipStack captureChipStack = new ChipStack();
    private int captureIndex = 0;	// capture stack at the start of the move
    public int bunny_player = -1;
    public int candidate_player = -1;
    public TriadChip lastDroppedObject = null;	// for image adjustment logic

 	// factory method to generate a board cell
	public TriadCell newcell(char c,int r)
	{	TriadCell ce = new TriadCell(c,r);
		ce.color = ChipColor.values()[(c-'A'+r)%3];
		return(ce);
	}
   public TriadBoard(String init,int map[]) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = TRIADGRIDSTYLE;
        players_in_game = 3;
        win = new boolean[3];
        setColorMap(map, 3);
        doInit(init); // do the initialization 
    }
    public TriadBoard cloneBoard() 
	{ TriadBoard dup = new TriadBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TriadBoard)b); }

    /**
     * return a score for the player in a multiplayer game. 
     */
    public int scoreForPlayer(int p)
    {	G.Assert(board_state==TriadState.GAMEOVER_STATE,"gameover");
    	return(chips_on_board[p]);
    }
    

    // precompute which border cell decorations needs to be drawn 
    // this is peculiar to the way we draw the borders of the board
    // not a general game requirement.
    private void setBorderDirections()
    {	for(TriadCell c = allCells;
    		c!=null;
    		c = c.next)
    	{	c.setBorderMask(c.borderDirectionMask());
    	}
    }

    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    {	int[] firstcol = null;
    	int[] ncol = null;
    	if(Triad_INIT.equalsIgnoreCase(game)) { firstcol = ZfirstInCol; ncol = ZnInCol; }
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(TriadState.PUZZLE_STATE);
        reInitBoard(firstcol, ncol, ZInCol); //this sets up a hexagonal board
        
      	setBorderDirections();	// mark the border cells for use in painting
        
        whoseTurn = FIRST_PLAYER_INDEX;
        bunny_player = -1;
        candidate_player = -1;
        AR.setValue(chips_on_board,0);
        AR.setValue(win, false);
        int map[]=getColorMap();
        playerChip[map[0]] = TriadChip.RedStone;
        playerCell[map[0]] = red;
        playerChip[map[1]] = TriadChip.GreenStone;
        playerCell[map[1]] = green;
        playerChip[map[2]] = TriadChip.BlueStone;
        playerCell[map[2]] = blue;
        playerColorPool[map[0]] = TriadId.Red_Chip_Pool;
        playerColorPool[map[1]] = TriadId.Green_Chip_Pool;
        playerColorPool[map[2]] = TriadId.Blue_Chip_Pool;

        captureCellStack.clear();
        captureChipStack.clear();
        droppedDest = null;
        bunnyDrop = null;
        pickedSource = null;
        lastDroppedObject = null;

       // set the initial contents of the board to all empty cells
		for(TriadCell c = allCells; c!=null; c=c.next) { c.chip=null; }
		for(int i=0;i<redStart.length;i++)
		{	int rrow[]=redStart[i];
			SetBoard(getCell((char)rrow[0],rrow[1]),TriadChip.RedStone);
			int grow[]=greenStart[i];
			SetBoard(getCell((char)grow[0],grow[1]),TriadChip.GreenStone);
			int brow[]=blueStart[i];
			SetBoard(getCell((char)brow[0],brow[1]),TriadChip.BlueStone);
			
		}
    }
    public void sameboard(BoardProtocol f) { sameboard((TriadBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TriadBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the boards

        G.Assert(AR.sameArrayContents(chips_on_board,from_b.chips_on_board),"chip count matches");       

        // here, check any other state of the board to see if
        G.Assert(candidate_player==from_b.candidate_player,"candidate matches");
        G.Assert(bunny_player==from_b.bunny_player,"bunny matches");
        G.Assert(pickedObject==from_b.pickedObject,"pickedobject matches");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

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
    	long v = 0;
 
        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        for(int i=0;i<3;i++)
        	{ long r1 = r.nextLong();
        	  long r2 = r.nextLong();
        	  long r3 = r.nextLong();
        	  v += chips_on_board[i]*r1;
        	  if(i==bunny_player) { v += r2; }
        	  if(i==candidate_player) { v += r3; }
        	}

        // note we can't modernize this without invalidating all the existing
		// digests.
		for(TriadCell c=allCells; c!=null; c=c.next)
		{	v ^= c.Digest(r);
		}
		v ^= TriadChip.Digest(r,pickedObject);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        // for most games, we should also digest whose turn it is
		//int v0 = r.nextLong();
		//int v1 = r.nextLong();
		//v ^= whoseTurn==0 ? v0 : v1;	// player to mvoe
        return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TriadBoard from_board)
    {
        TriadBoard from_b = from_board;
        super.copyFrom(from_b);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        bunny_player = from_b.bunny_player;
        candidate_player = from_b.candidate_player;
        bunnyDrop = null;
        lastPicked = null;
        pickedObject = from_b.pickedObject;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = (from_b.droppedDest);
        AR.copy(chips_on_board,from_b.chips_on_board);
        copyFrom(playerCell,from_board.playerCell);	// needed so screen location is propagated

        sameboard(from_b); 
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	
    	randomKey = key;
       Init_Standard(gtype);
        moveNumber = 1;
        captureCellStack.clear();
        captureChipStack.clear();
        captureIndex = 0;

        // note that firstPlayer is NOT initialized here
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
        case CONFIRM_END_STATE:
        case DRAW_STATE:
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
    private boolean hasLegalMoves(int who)
    {
    	for(TriadCell c = allCells; c!=null; c=c.next)
    	{
    	TriadChip top = c.topChip();
    	if(top==playerChip[who])
    		{
    		// check 6 possible move directions
    		for(int dir=0;dir<6;dir++)
    		{	TriadCell adj = c.exitTo(dir);
    			while((adj!=null) && (adj.topChip()==null))
    			{// unobstructed directional move
    			if(adj.color !=playerChip[who].color)
    			{	// possible landing spot.  We don't need to check for max captures
    				// since if no captures are possible, a noncapture move is accepted.
    			  return(true); 
    			}
    			adj = adj.exitTo(dir);
    			}
    		}
    		}
    	}
    	return(false);
    }


    public boolean GameOverNow()
    {	for(int i=0;i<3;i++) { if(chips_on_board[i]==0) { return(true); }}
    	if(chips_on_board[whoseTurn]>3) { return(false); }
    	return(!hasLegalMoves(whoseTurn));
    }

    
    // set the contents of a cell, and maintian the books
    public TriadChip SetBoard(TriadCell c,TriadChip ch)
    {	TriadChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(old!=null) { chips_on_board[playerForChip(old)]--; }
     	if(ch!=null) { chips_on_board[playerForChip(ch)]++; }
    	}
       	c.chip = ch;
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
        bunnyDrop = null;
    	}
     }
    private void undoCaptures(int level)
    {	while(captureCellStack.size()>level)
		{	TriadCell c = captureCellStack.pop();
			TriadChip ch = captureChipStack.pop();
			SetBoard(c,ch);
		}
    	captureIndex = level;
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {	switch(board_state)
    	{
    	case CONFIRM_STATE:
    		if(bunnyDrop!=null)
    		{	pickedObject = SetBoard(bunnyDrop,null);
    			bunnyDrop = null;
    		}
    		break;
    	default:
	    	if(droppedDest!=null) 
	    	{	pickedObject = droppedDest.topChip();
	    		SetBoard(droppedDest,null); 
	    		droppedDest = null;
	    		undoCaptures(captureIndex);

	    	}}
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
    // drop the floating object.
    //
    private void dropObject(TriadId m)
    {
       switch (m)
        {
        default:
        	throw G.Error("not expecting dest %s", m);
        case Red_Chip_Pool:
        case Blue_Chip_Pool:
        case Green_Chip_Pool:		// back in the pool, we don't really care where
        	pickedObject = null;
        	bunnyDrop = null;
            break;
        }
     }
    private void dropObject(TriadCell c)
    {
       	SetBoard(c,pickedObject);
       	switch(board_state)
       	{
       	case DROP_STATE: bunnyDrop = c;
       		break;
       	default: droppedDest = c;
       	}
        lastDroppedObject = pickedObject;
        pickedObject = null;
	
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TriadCell c)
    {	switch(board_state)
    	{
     	case CONFIRM_STATE: return(c==bunnyDrop); 
     	case CONFIRM_END_STATE:
    	default: return(droppedDest==c);
    	}
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    { TriadChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
    private TriadCell getCell(TriadId source, char from_col, int from_row) 
    {
 		switch(source) {
 		case BoardLocation:
 		case EmptyBoard: return getCell(from_col,from_row);
 		case Blue_Chip_Pool: return blue;
 		case Red_Chip_Pool: return red;
 		case Green_Chip_Pool: return green;
 		default: throw G.Error("Not expecting %s",source);
 		}
 	}
    private TriadCell getCell(TriadChip c)
    {
    	for(TriadCell d :  playerCell) { if(d.topChip()==c) { return d; }}
    	return null;
    }

	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TriadCell src)
    {
        switch (src.rackLocation())
        {
        default:
        	throw G.Error("Not expecting source %s", src);
         case BoardLocation:
        	{
        	boolean wasDest = isDest(src);
        	unDropObject(); 
        	if(!wasDest)
        	{
            pickedSource = src;
            lastPicked = pickedObject = src.topChip();
         	bunnyDrop = null;
         	droppedDest = null;
			SetBoard(src,null);
        	}}
            break;
        case Blue_Chip_Pool:
        	lastPicked = pickedObject = TriadChip.BlueStone;
       		break;
        case Red_Chip_Pool:
			lastPicked = pickedObject = TriadChip.RedStone;
            break;

        case Green_Chip_Pool:
            lastPicked = pickedObject = TriadChip.GreenStone;
            break;
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(TriadCell c)
    {	return(c==pickedSource);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //
    private int thirdPlayer(int who,int can)
    {	for(int i=0;i<3;i++) { if((i!=who)&&(i!=can)) { return(i); }}
   		throw G.Error("can't happen");
    }
    private int playerForColor(ChipColor co)
    {
    	for(int i=0;i<3;i++) { if(co==playerChip[i].color) { return(i); }}
    	throw G.Error("no one has color "+co);
    }
    private int playerForChip(TriadChip co)
    {
    	for(int i=0;i<3;i++) { if(co==playerChip[i]) { return(i); }}
    	throw G.Error("no one has chip "+co);
    }
    private void setNextStateAfterDrop(replayMode replay)
    {  

        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_END_STATE:
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setNextStateAfterDone();
        	break;
        case PLAY_STATE:
 			setState(TriadState.DROP_STATE);
 			candidate_player = playerForColor(droppedDest.color);
 			bunny_player = thirdPlayer(whoseTurn,candidate_player);
        	doCaptures(droppedDest,replay);
        	if(GameOverNow()) 
            {	setState(TriadState.CONFIRM_END_STATE);
            }
			break;
        case DROP_STATE:
        	setState(TriadState.CONFIRM_STATE);
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
    	case GAMEOVER_STATE: break;
    	case DRAW_STATE:
    		setState(TriadState.GAMEOVER_STATE);
      		candidate_player = -1;
        	bunny_player = -1;
    		break;
    	case CONFIRM_STATE:
    	case CONFIRM_END_STATE:
    		candidate_player = -1;
        	bunny_player = -1;
    		setState(GameOverNow() ? TriadState.GAMEOVER_STATE: TriadState.PLAY_STATE); 
    		break;
    	case PUZZLE_STATE:
    		candidate_player = -1;
        	bunny_player = -1;
        	setState(GameOverNow() ? TriadState.GAMEOVER_STATE: TriadState.PLAY_STATE); 
    		break;
    	}

    }
    // get the pending captures for the user interface
    public Hashtable<TriadCell,TriadCell> getCaptures()
    {	Hashtable<TriadCell,TriadCell> h = new Hashtable<TriadCell,TriadCell>();
    	switch(board_state)
    	{
    	case DROP_STATE:
    	case CONFIRM_STATE:
    	case CONFIRM_END_STATE:
    		int cap = captureCellStack.size()-1;
    		while(cap>captureIndex)
    		{	TriadCell c = captureCellStack.elementAt(cap);
    			h.put(c,c);
    			cap--;
    		}
    		break;
    	default: ;
    	}
    	return(h);
    }
    
    // do the captures for this move, store undo unformation on the capture stack
    private void doCaptures(TriadCell dest,replayMode replay)
    {	TriadChip top = dest.topChip();
    	if(top!=null)
    	{
    	for(int dir = 0; dir<6; dir++)
	    	{
    		TriadCell adj = dest.exitTo(dir);
    		if(adj!=null)
    			{ TriadChip adjTop = adj.topChip();
    			  if((adjTop!=null)&&(adjTop!=top))
    			  {
    			  captureChipStack.push(adjTop);
    			  captureCellStack.push(adj);
    			  SetBoard(adj,null);
    			  if(replay!=replayMode.Replay)
    			  {
    				  animationStack.push(adj);
    				  animationStack.push(getCell(adjTop));
    			  }
    			  }
    			}
	    	}
	    }
    }
    private void doDone()
    {
       	captureIndex = captureCellStack.size();
        if(GameOverNow()) 
        {	setState(TriadState.GAMEOVER_STATE);
        }
        else
        {
        	whoseTurn = candidate_player;
        	candidate_player = bunny_player = -1;
        	captureIndex = captureCellStack.size();
        	moveNumber++;
            acceptPlacement();
        	setNextStateAfterDone();
        }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	TriadMovespec m = (TriadMovespec)mm;

        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_MOVE:
        	{
        	// point to point movement for the robot
        	TriadCell src = getCell(m.source, m.from_col, m.from_row);
        	TriadCell dest = getCell(m.to_col,m.to_row);
        	pickObject(src);
        	dropObject(dest);
        	if(replay!=replayMode.Replay)
        	{
        		animationStack.push(src);
        		animationStack.push(dest);
        	}
        	setNextStateAfterDrop(replay);
        	}
        	break;
        case MOVE_DROPB:
			//pickObject(m.object, m.to_col, m.to_row);
 			switch(board_state)
			{ case PUZZLE_STATE: acceptPlacement(); break;
			default:
				break;
			}
 			TriadCell c = getCell(m.to_col,m.to_row);
 			if((board_state==TriadState.PLAY_STATE) && isSource(c)) { unPickObject(); }
 			else
 			{
 			boolean prepick = pickedObject!=null;
 			TriadCell src = getCell(m.source, m.to_col, m.to_row);
 			TriadCell dest = getCell(m.to_col,m.to_row);
			pickObject(src);
            dropObject(dest);
            if((replay==replayMode.Single) || (replay==replayMode.Live && !prepick))
            {
            	animationStack.push(src);
            	animationStack.push(dest);
            }
            setNextStateAfterDrop(replay);
            m.from_row = candidate_player;		// save for the robot
            m.from_col = (char)('A'+bunny_player);
 			}
            break;

        case MOVE_PICK:
           	pickObject(getCell(m.source, m.to_col, m.to_row));
            break;  
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
         	switch(board_state)
        	{
        	case PUZZLE_STATE:
               	pickObject(getCell(m.source, m.to_col, m.to_row));
        		break;
        	case PLAY_STATE:
               	pickObject(getCell(m.source, m.to_col, m.to_row));
               	break;
        	case CONFIRM_END_STATE:
        	case DROP_STATE:
               	unDropObject();
               	setState(TriadState.PLAY_STATE);
        		bunny_player = -1;
        		candidate_player = -1;
        		break;
        	case CONFIRM_STATE:
               	unDropObject();
        		setState(TriadState.DROP_STATE);
        		break;
        	default: ;
        	}
            break;

        case MOVE_DROP: // drop on chip pool;
            dropObject(m.source);
            //setNextStateAfterDrop();

            break;


 
        case MOVE_START:
            setWhoseTurn(m.player);
            bunny_player = -1;
            candidate_player = -1;
            captureIndex = captureCellStack.size();
            acceptPlacement();
            unPickObject();
            setState(TriadState.PUZZLE_STATE);
            setNextStateAfterDone();
            break;

        case MOVE_EDIT:
            bunny_player = -1;
            candidate_player = -1;
            captureIndex = captureCellStack.size();
            acceptPlacement();
            setState(TriadState.PUZZLE_STATE);

            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(TriadState.GAMEOVER_STATE);
			break;

        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);

        return (true);
    }


    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case DROP_STATE:
        	return(player==bunny_player);
        case CONFIRM_STATE:
        case CONFIRM_END_STATE:
        case DRAW_STATE:
        case PLAY_STATE:
        	// you can pick up a stone in the storage area
        	// but it's really optional
        	return(false);
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }
    public Hashtable<TriadCell,TriadCell> getSources()
    {
    	return(getSources(whoseTurn));
    }
    public Hashtable<TriadCell,TriadCell> getSources(int pl)
    {	Hashtable<TriadCell,TriadCell> h = new Hashtable<TriadCell,TriadCell>();
    	if(board_state==TriadState.PLAY_STATE)
    	{
    	CommonMoveStack  all = getListOfMoves(pl,true);
    	for(int i=0;i<all.size();i++)
    	{	TriadMovespec m = (TriadMovespec)all.elementAt(i);
    		if(m.op==MOVE_MOVE)
    		{	TriadCell c = getCell(m.from_col,m.from_row);
    			h.put(c,c);
    		}
    	}}
    	return(h);
    }
    public Hashtable<TriadCell,TriadCell> getDests()
    {	return(getDests(whoseTurn,pickedSource));
     }
    public Hashtable<TriadCell,TriadCell> getDests(int pl,TriadCell from)
    {	Hashtable<TriadCell,TriadCell> h = new Hashtable<TriadCell,TriadCell>();
    	if((from!=null)&&(board_state==TriadState.PLAY_STATE))
    	{
    	CommonMoveStack  all = getListOfMoves(pl,true);
    	for(int i=0;i<all.size();i++)
    	{	TriadMovespec m = (TriadMovespec)all.elementAt(i);
    		if((m.op==MOVE_MOVE) && (m.from_col==from.col) && (m.from_row==from.row))
    		{	TriadCell c = getCell(m.to_col,m.to_row);
    			h.put(c,c);
    		}
    	}}
    	return(h);
    }
    public boolean LegalToHitBoard(TriadCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case PLAY_STATE:
			{
			if(pickedObject==null)
			{
			Hashtable<TriadCell,TriadCell> sources = getSources(whoseTurn);
			return(sources.get(c)!=null);
			}
			if(isSource(c)) { return(true); }
			Hashtable<TriadCell,TriadCell> dests = getDests(whoseTurn,pickedSource);
			return(dests.get(c)!=null);
			}
		case GAMEOVER_STATE:
			return(false);
		case CONFIRM_STATE:
		case CONFIRM_END_STATE:
		case DRAW_STATE:
			return(isDest(c));
		case DROP_STATE:
			return(isDest(c)||(c.topChip()==null));
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
    public void RobotExecute(TriadMovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        m.captureIndex = captureIndex;
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("E "+m+" "+m.search_clock);
        if (Execute(m,replayMode.Replay))
        {
            if ((m.op == MOVE_DONE)||(m.op==MOVE_MOVE))
            {
            }
            else if (DoneState())
            {
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
    public void UnExecute(TriadMovespec m)
    {
    	//G.print("U "+m+" "+m.search_clock);
        switch (m.op)
        {
   	    default:
   	    	throw G.Error("Can't un execute %s", m);
        case MOVE_DONE:
            break;
        case MOVE_MOVE:
        	{
        	TriadChip ch = getCell(m.to_col,m.to_row).removeTop();
        	getCell(m.from_col,m.from_row).addChip(ch);
        	undoCaptures(m.captureIndex);
        	pickedObject = null;
        	pickedSource = droppedDest = null;
        	candidate_player = bunny_player = -1;
        	}
        	break;
        case MOVE_DROPB:
        	SetBoard(getCell(m.to_col,m.to_row),null);
        	candidate_player = m.from_row;	// saved by execute
        	bunny_player = (m.from_col-'A');
        	bunnyDrop = null;
        	break;
         }

        setState(m.state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
    
int getListOfMovesFrom(TriadCell from,TriadChip top,int who,TriadCell to,int direction,CommonMoveStack  all,int capture_size)
{	
	while((to!=null)
			&&(to.topChip()==null))
    {
	if(top.color!=to.color)
		{
		int nadj = 0;
		for(int dir = 0;dir<6;dir++) 
			{	TriadCell adj = to.exitTo(dir);
				TriadChip adjTop = (adj==null)?null:adj.topChip();
				if((adjTop!=null)&&(adjTop!=top)) { nadj ++; }
			}
	 		if(nadj>capture_size)
	    		{	// new max capture size, throw away the smaller captures
	 			capture_size = nadj;
	    		all.setSize(0); 
	    		}
	    	if(nadj==capture_size)
	    		{	
	    			all.addElement(new TriadMovespec(MOVE_MOVE,from.col,from.row,to.col,to.row,who));
	    		}
		}
    	to = to.exitTo(direction);
    	}
    	return(capture_size);
    }
    

 int getListOfMovesFrom(TriadCell from,TriadChip top,int who,CommonMoveStack  all,int capture_size)
 {	
 	for(int dir=0;dir<6;dir++)
 	{	capture_size = getListOfMovesFrom(from,top,who,from.exitTo(dir),dir,all,capture_size);
 	}
 	return(capture_size);
 }

 int getListOfMoves(CommonMoveStack  all,int who,boolean picked)
 {	int size=0;
 	for(TriadCell c = allCells;
 		c!=null;
 		c =c.next)
 	{
 	TriadChip top = (picked&&c==pickedSource) ? pickedObject : c.topChip();
 	if(top==playerChip[who])
 		{ size = getListOfMovesFrom(c,top,who,all,size); 
 		}
 	}
 	return(size);
 }
 CommonMoveStack  getListOfMoves(int pl,boolean picked)
 {	CommonMoveStack  all = new CommonMoveStack();
	getListOfMoves(all,pl,picked);
	return(all);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
	 {
 	case PLAY_STATE:
 		getListOfMoves(all,whoseTurn,false);
 		if(all.size()==0) 
 			{ all.addElement(new TriadMovespec(MOVE_PASS,whoseTurn));
 			}
 		break;
 	case CONFIRM_END_STATE:
	case DRAW_STATE:
		all.addElement(new TriadMovespec(MOVE_DONE,whoseTurn));
		break;
	case DROP_STATE:
		{	// bunny drop
		TriadId bunny = playerColorPool[bunny_player];
		for(TriadCell c = allCells; c!=null; c=c.next)
		{	if(c.topChip()==null) { all.addElement(new TriadMovespec(MOVE_DROPB,bunny,c.col,c.row,whoseTurn));}
		}
		}
		break;
	default:
		break;
	 }
 	G.Assert(all.size()>0,"some moves");
 	return(all);
	 
  }
 
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ xpos-=2;
	 	}
 	GC.Text(gc, false, xpos-2, ypos, -1, 0,clt, null, txt);
 }

public boolean WinForPlayer(int player)
{	if(board_state==TriadState.GAMEOVER_STATE)
	{
	int best = scoreForPlayer(player);
	int second = -1;
	for(int i=0;i<3;i++)
	{	if(i!=player)
			{int sc = scoreForPlayer(i);
			 if(sc>second) { second=sc; }
			}
	}
	return(best>second);
	}
	return(false);
}

public void getScores(TriadMovespec m)
{	for(int i=0;i<3; i++) { m.playerScores[i] = scoreForPlayer(i); }
}
double dumbotEval(int player,boolean print)
{	// note we don't need "player" here because the blobs variable
	// contains all the information, and was calculated for the player
	double val = chips_on_board[player]*100;

	return(val);
}
}