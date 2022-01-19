package mijnlieff;

import static mijnlieff.Mijnlieffmovespec.*;

import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * MijnlieffBoard knows all about the game of MijnLieff which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
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

class MijnlieffBoard 
	extends rectBoard<MijnlieffCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,MijnlieffConstants
{	static int REVISION = 101;			// 100 represents the initial version of the game
										// 101 implements the proper opening rule
	public int getMaxRevisionLevel() { return(REVISION); }

	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

	MijnlieffVariation variation = MijnlieffVariation.Mijnlieff;
	private MijnlieffState board_state = MijnlieffState.Puzzle;	
	private MijnlieffState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	private CellStack robotCell = new CellStack();
	public MijnlieffState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(MijnlieffState st) 
	{ 	unresign = (st==MijnlieffState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private MColor playerColor[]={MColor.Light,MColor.Dark};    
	public MColor getPlayerColor(int p) { return(playerColor[p]); }
	public MijnlieffChip getPlayerChip(int p) 
	{ MColor co = playerColor[p];
	  switch(co)
		{
		default: throw G.Error("Not expecting ",co);
		case Dark: return(MijnlieffChip.Dark_diagonal);
		case Light: return(MijnlieffChip.Light_orthogonal);
		}
	}
	public MijnlieffCell[] getPlayerRack(int pl) { return(rack[pl]); }
	

// this is required even though it is meaningless for Hex, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() {throw G.Error("not expected"); };	
	CellStack animationStack = new CellStack();

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MijnlieffChip pickedObject = null;
    public MijnlieffChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    private MijnlieffCell darkRack[] = new MijnlieffCell[] {
    		new MijnlieffCell(MijnlieffId.Dark_Push),
    		new MijnlieffCell(MijnlieffId.Dark_Pull),
    		new MijnlieffCell(MijnlieffId.Dark_Diagonal),
    		new MijnlieffCell(MijnlieffId.Dark_Orthogonal),
    		};
    private MijnlieffCell lightRack[] = new MijnlieffCell[] {
    		new MijnlieffCell(MijnlieffId.Light_Push),
    		new MijnlieffCell(MijnlieffId.Light_Pull),
    		new MijnlieffCell(MijnlieffId.Light_Diagonal),
    		new MijnlieffCell(MijnlieffId.Light_Orthogonal),
    		};
    
    private MijnlieffCell rack[][] = new MijnlieffCell[][] {lightRack,darkRack};
    private MijnlieffCell dummyRack[] = new MijnlieffCell[] { new MijnlieffCell(MijnlieffId.None)};
    
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}
 	private int countInDirection(MijnlieffCell from,MColor color,int direction)
 	{	int count = 0;
 		while(from!=null)
 		{
 			MijnlieffChip top = from.topChip();
 			if(top!=null && top.color==color) { count++; from = from.exitTo(direction); }
 			else { break; }
 		}
 		return(count);
 		
 	}

 	private int scoreInDirection(MijnlieffCell from,MColor color,int direction,boolean maybe4)
 	{	switch(countInDirection(from,color,direction))
 		{
 		case 0: 
 			if(maybe4)
	 			{
	 			return(scoreInDirection(from.exitTo(direction),color,direction,false));
	 			}
 			return(0); 
 
 		case 3: return(1);
 		case 4: return(2);
 		default: return(0);
 		}
 		
 	}
 	public int scoreForPlayer(int pl)
 	{	int count = 0;
 		MColor color = getPlayerColor(pl);
 		// first horizontal lines
 		MijnlieffCell aa = getCell('A',1);
 
 		{MijnlieffCell a1 = aa;
 		while(a1!=null) 
 			{ count += scoreInDirection(a1,color,CELL_RIGHT,true);
 			  a1 = a1.exitTo(CELL_UP);
 			}}
 		
 		{
 			MijnlieffCell a1 = aa;
 	 		while(a1!=null) 
 	 			{ count += scoreInDirection(a1,color,CELL_UP,true);
 	 			  a1 = a1.exitTo(CELL_RIGHT);
 	 			}}	
 		// score diagonals near a1
 		count += scoreInDirection(aa,color,CELL_UP_RIGHT,true);
 		count += scoreInDirection(aa.exitTo(CELL_UP),color,CELL_UP_RIGHT,false);
 		count += scoreInDirection(aa.exitTo(CELL_RIGHT),color,CELL_UP_RIGHT,false);
 		// score diagonals near a4
 		MijnlieffCell a4 = getCell('A',4);
 		count += scoreInDirection(a4,color,CELL_DOWN_RIGHT,true);
 		count += scoreInDirection(a4.exitTo(CELL_DOWN),color,CELL_DOWN_RIGHT,false);
 		count += scoreInDirection(a4.exitTo(CELL_RIGHT),color,CELL_DOWN_RIGHT,false);
 		
 		return(count);
		
 	}
 	public void setGameOver()
 	{
 		int score0 = scoreForPlayer(0);
 		int score1 = scoreForPlayer(1);
 		win[0] = score0>score1;
 		win[1] = score1>score0;
 		setState(MijnlieffState.Gameover);
 	}
    private MijnlieffState resetState = MijnlieffState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public MijnlieffCell newcell(char c,int r)
	{	return(new MijnlieffCell(MijnlieffId.BoardLocation,c,r));
	}
	
	// constructor 
    public MijnlieffBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map);
        doInit(init,key,players,rev); // do the initialization 
     }
    
    public String gameType() { return(G.concat(gametype," ",players_in_game," ",randomKey," ",revision)); }
    
    public MijnlieffCell lastPlay = null;
    
    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	lastPlay = null;
    	players_in_game = players;
    	win = new boolean[players];
        reInitBoard(4,4);
 		setState(MijnlieffState.Puzzle);
		variation = MijnlieffVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=MColor.Light;
		playerColor[map[1]]=MColor.Dark;
	    rack[map[0]] = lightRack;
	    rack[map[1]] = darkRack;
        animationStack.clear();
        moveNumber = 1;
        for(MijnlieffCell pr[] : rack) 
        {
        	for(MijnlieffCell c : pr)
        	{
        		c.reInit();
        		MijnlieffChip ch = c.rackLocation().chip;
        		c.addChip(ch);	// two of each
        		c.addChip(ch);
        	}
        }
         // note that firstPlayer is NOT initialized here
    }

 
    /** create a copy of this board */
    public MijnlieffBoard cloneBoard() 
	{ MijnlieffBoard dup = new MijnlieffBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((MijnlieffBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MijnlieffBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        lastPlay = getCell(from_b.lastPlay);
        copyFrom(rack,from_b.rack);
        AR.copy(playerColor,from_b.playerColor);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((MijnlieffBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MijnlieffBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
        G.Assert(sameCells(lastPlay,from_b.lastPlay),"lastPlay mismatch");

         // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are relevant to the ordinary operation
     * of the game, others are for system record keeping use; so it is important that the
     * game Digest be consistent both within a game and between games over a long period
     * of time which have the same moves. 
     * (1) Digest is used by the default implementation of EditHistory to remove moves
     * that have returned the game to a previous state; ie when you undo a move or
     * hit the reset button.  
     * (2) Digest is used after EditHistory to verify that replaying the history results
     * in the same game as the user is looking at.  This catches errors in implementing
     * undo, reset, and EditHistory
	 * (3) Digest is used by standard robot search to verify that move/unmove 
	 * returns to the same board state, also that move/move/unmove/unmove etc.
	 * (4) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (5) games where repetition is forbidden (like xiangqi/arimaa) can also use this
     * information to detect forbidden loops.
	 * (6) Digest is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     * looks for duplicate digests.  
     * (7) digests are also used in live play to detect "parroting" by running two games
     * simultaneously and playing one against the other.
     */
    public long Digest()
    { 
        // the basic digestion technique is to xor a bunch of random numbers. 
    	// many object have an associated unique random number, including "chip" and "cell"
    	// derivatives.  If the same object is digested more than once (ie; once as a chip
    	// in play, and once as the chip currently "picked up", then it must be given a
    	// different identity for the second use.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest();
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= Digest(r,rack);
		v ^= Digest(r,lastPlay);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player in state ",board_state);
        case Puzzle:
            break;
        case Play:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player in state ",board_state); }
			//$FALL-THROUGH$
        case Confirm:
        case Pass:
        case Resign:
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
    {	return(board_state.doneState());
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selecteive.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }



    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	return(win[player]);
    }


    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private MijnlieffCell unDropObject()
    {	MijnlieffCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = rv.removeTop(); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	MijnlieffCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	rv.addChip(pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(MijnlieffCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Dark_Push:
        case Dark_Pull:
        case Dark_Orthogonal:
        case Dark_Diagonal:
        case Light_Push:
        case Light_Pull:
        case Light_Orthogonal:
        case Light_Diagonal:
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
           	c.addChip(pickedObject);
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MijnlieffCell c)
    {	return(droppedDestStack.top()==c);
    }
    public MijnlieffCell getDest()
    {	return(droppedDestStack.top());
    }
   
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { MijnlieffChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
   /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    private MijnlieffCell getCell(MijnlieffId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case Dark_Push:
        		return(darkRack[0]);
        case Dark_Pull:
        		return(darkRack[1]);
        case Dark_Diagonal:
        		return(darkRack[2]);
        case Dark_Orthogonal:
        		return(darkRack[3]);
        		
        case Light_Push:
    		return(lightRack[0]);
        case Light_Pull:
    		return(lightRack[1]);
        case Light_Diagonal:
    		return(lightRack[2]);
        case Light_Orthogonal:
    		return(lightRack[3]);
    		
        case BoardLocation:
        	return(getCell(col,row));
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public MijnlieffCell getCell(MijnlieffCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(MijnlieffCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
    	lastDroppedObject = null;
        lastPicked = pickedObject = c.removeTop();
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(MijnlieffCell c)
    {	return(c==pickedSourceStack.top());
    }
    public MijnlieffCell getSource()
    {	return(pickedSourceStack.top());
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
        case FirstPlay:
			setState(MijnlieffState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private boolean boardEmpty()
    {
    	for(MijnlieffCell c = allCells; c!=null; c=c.next) { if(c.topChip()!=null) { return(false); }}
    	return(true);
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case Confirm:
    	case Pass:
    		if(!hasLegalMoves(whoseTurn)) 
    		{
    			if(rackEmpty(whoseTurn)) { setGameOver(); }
    			else { setState(MijnlieffState.Pass); }
    			break;
    		}
			//$FALL-THROUGH$
		case Puzzle:
			if(revision>=101 && boardEmpty())
				{ setState(MijnlieffState.FirstPlay); 
				  break;
				}
			//$FALL-THROUGH$
		case Play:
    		setState(MijnlieffState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {	
    	lastPlay = getDest(); 	// this will be null if the last move was a pass
        acceptPlacement();

        if (board_state==MijnlieffState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(MijnlieffState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(MijnlieffState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Mijnlieffmovespec m = (Mijnlieffmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_PASS:
        	setState(MijnlieffState.Confirm);
        	break;
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_FROM_TO:
        	{
        		MijnlieffCell from = getCell(m.source,'@',0);
        		pickObject(from);
        	}
			//$FALL-THROUGH$
		case MOVE_DROPB:
        	{
			MijnlieffChip po = pickedObject;
			MijnlieffCell dest =  getCell(MijnlieffId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				m.chip = po;
		           
	            dropObject(dest);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay!=replayMode.Replay && (po==null))
	            	{ animationStack.push(getSource());
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			MijnlieffCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.chip = pickedObject;
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState( MijnlieffState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            MijnlieffCell dest = getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else 
            	{
		        if(replay==replayMode.Live)
	        	{ lastDroppedObject = pickedObject.getAltDisplayChip(dest);
	        	  //G.print("last ",lastDroppedObject); 
	        	}      	
            	dropObject(dest); 
            
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(MijnlieffState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(MijnlieffState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?MijnlieffState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(MijnlieffState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(MijnlieffState.Gameover);
    	   break;

        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case FirstPlay:
        case Play:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
		case Resign:
		case Gameover:
		case Pass:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject.color==playerColor[player]):true);
        }
    }

    public boolean legalToHitBoard(MijnlieffCell c,Hashtable<MijnlieffCell,Mijnlieffmovespec>targets)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case FirstPlay:
		case Play:
			return((pickedObject!=null) && (targets.get(c)!=null));
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c) || c.isEmpty());
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Pass:
        	return(false);
        case Puzzle:
            return ((pickedObject==null) == (c.topChip()!=null));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Mijnlieffmovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        robotCell.push(lastPlay);
        Execute(m,replayMode.Replay);
        acceptPlacement();
       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Mijnlieffmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	MijnlieffState state = robotState.pop();
    	lastPlay = robotCell.pop();
    	
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_PASS:
        case MOVE_DONE:
            break;
            
        case MOVE_FROM_TO:
        	MijnlieffChip top = getCell(m.to_col,m.to_row).removeTop();
        	getCell(top.id,'@',0).addChip(top);
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 private boolean addRackMoves(CommonMoveStack all,MijnlieffCell rack[],MijnlieffCell to,int who)
 {	boolean some = false;
 	for(MijnlieffCell from : rack)
		{	MijnlieffChip top = from.topChip();
			if(top!=null)
			{
				if(all==null) { return(true); }
				some = true;
				all.push(new Mijnlieffmovespec(MOVE_FROM_TO,top,to,who));
			}
		}
	 return(some);
 }
 private boolean addAnyMoves(CommonMoveStack all,MijnlieffCell from[],int who)
 {	boolean some = false;
	 for(MijnlieffCell c = allCells; c!=null; c=c.next)
	 {	if(c.isEmpty()) 
	 	{
		 some |= addRackMoves(all,from,c,who);
		 if(some && all==null) { return(some); }
	 	}
	 }
	 return(some);
 }
 private boolean addEdgeMoves(CommonMoveStack all,MijnlieffCell from[],int who)
 {	boolean some = false;
	 for(MijnlieffCell c = allCells; c!=null; c=c.next)
	 {	if(c.isEmpty() && c.isEdgeCell()) 
	 	{
		 some |= addRackMoves(all,from,c,who);
		 if(some && all==null) { return(some); }
	 	}
	 }
	 return(some);
 }
 private boolean addPushMoves(CommonMoveStack all,MijnlieffCell from[],int who)
 {	boolean some = false;
	 for(MijnlieffCell c = allCells; c!=null; c=c.next)
	 {	if(c.isEmpty() && !c.isAdjacentTo(lastPlay))
	 	{
		 some |= addRackMoves(all,from,c,who);
		 if(some && all==null) { return(some); }
	 	}
	 }
	 return(some);
 }
 private boolean addPullMoves(CommonMoveStack all,MijnlieffCell from[],int who)
 {	boolean some = false;
	 for(int lim=CELL_FULL_TURN-1; lim>=0; lim--)
		 {
		 MijnlieffCell adj = lastPlay.exitTo(lim);
		 if(adj!=null && adj.isEmpty())
	 		{
			 some |= addRackMoves(all,from,adj,who);
			 if(some && all==null) { return(some); }
	 	}
	 }
	 return(some);
 }
 private boolean addOrthogonalMoves(CommonMoveStack all,MijnlieffCell from[],int who)
 {	boolean some = false;
	 for(int dir = CELL_LEFT,lim=dir+CELL_FULL_TURN; dir<lim; dir+=CELL_QUARTER_TURN)
		 {
		 MijnlieffCell adj = lastPlay;
		 while((adj=adj.exitTo(dir))!=null)
		 {
		 if(adj.isEmpty())
	 		{
			 some |= addRackMoves(all,from,adj,who);
			 if(some && all==null) { return(some); }
	 		}
		 }
	 }
	 return(some);
 }
 private boolean addDiagonalMoves(CommonMoveStack all,MijnlieffCell from[],int who)
 {	boolean some = false;
	 for(int dir = CELL_UP_LEFT,lim=dir+CELL_FULL_TURN; dir<lim; dir+=CELL_QUARTER_TURN)
		 {
		 MijnlieffCell adj = lastPlay;
		 while((adj=adj.exitTo(dir))!=null)
		 {
		 if(adj.isEmpty())
	 		{
			 some |= addRackMoves(all,from,adj,who);
			 if(some && all==null) { return(some); }
	 		}
		 }
	 }
	 return(some);
 }
 private boolean addRestrictedMoves(CommonMoveStack all,MijnlieffId restriction,int who)
 {	boolean some = false;
 	MijnlieffCell from[];
 	if(pickedObject!=null) { 
 		dummyRack[0].reInit();
 		dummyRack[0].addChip(pickedObject);
 		from = dummyRack;
 	}
 	else {
 		from = rack[who];
 	}
	switch(restriction)
	{
	case Edge:
		some |= addEdgeMoves(all,from,who);
		break;
	case None:
		some |= addAnyMoves(all,from,who);
		break;
	case Dark_Push:
	case Light_Push:
		some |= addPushMoves(all,from,who);
		break;
	case Dark_Pull:
	case Light_Pull:
		some |= addPullMoves(all,from,who);
		break;
	case Dark_Diagonal:
	case Light_Diagonal:
		some |= addDiagonalMoves(all,from,who);
		break;
	case Dark_Orthogonal:
	case Light_Orthogonal:
		some |= addOrthogonalMoves(all,from,who);
		break;
	default: throw G.Error("Not expecting ",restriction);
	}
 	return(some);
 }
 public boolean hasLegalMoves(int who)
 {	MijnlieffId restriction = lastPlay==null ? MijnlieffId.None : lastPlay.topChip().id;
 	return addRestrictedMoves(null,restriction,who);
 }
 public boolean rackEmpty(int who)
 {
	 for(MijnlieffCell c : rack[who]) { if(c.topChip()!=null) { return(false); }}
	 return(true);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	boolean first=false;
 	switch(board_state)
 	{
 	case Confirm:
 	case Resign:
 		all.push(new Mijnlieffmovespec(MOVE_DONE,whoseTurn)); 
 		break;
 	case Pass:
 		all.push(new Mijnlieffmovespec(MOVE_PASS,whoseTurn)); 
 		break;
 	case Puzzle:
 		{
 		int op = pickedObject==null ? MOVE_DROPB : MOVE_PICKB; 
 		{
 			for(MijnlieffCell c = allCells;  c!=null;    c = c.next)
 			 	{	if(c.topChip()==null)
 			 		{all.addElement(new Mijnlieffmovespec(op,c.col,c.row,whoseTurn));
 			 		}
 			 	}
 		}}
 		break;
 	case FirstPlay:
 		first=true;
		//$FALL-THROUGH$
	case Play:
 		{
 			MijnlieffId restriction = first ? MijnlieffId.Edge : lastPlay==null ? MijnlieffId.None : lastPlay.topChip().id;
 			boolean some = addRestrictedMoves(all,restriction,whoseTurn);
 			if(!some) 
 				{ all.push(new Mijnlieffmovespec(MOVE_PASS,whoseTurn)); 
 				}
 			
 		}
 		break;
 	case Gameover:
 		break;
 	default: G.Error("not expecting state ",board_state);
 	}
 	return(all);

 }
 
 public void initRobotValues()
 {
 }
public Hashtable<MijnlieffCell, Mijnlieffmovespec> getTargets() 
{
	Hashtable<MijnlieffCell, Mijnlieffmovespec> targets = new Hashtable<MijnlieffCell, Mijnlieffmovespec>();
	CommonMoveStack all = GetListOfMoves();
	for(int lim=all.size()-1; lim>=0; lim--)
	{	Mijnlieffmovespec m = (Mijnlieffmovespec)all.elementAt(lim);
		switch(m.op)
		{
		case MOVE_PICKB:
		case MOVE_DROPB:
		case MOVE_FROM_TO:
			targets.put(getCell(m.to_col,m.to_row),m);
			break;
		case MOVE_PASS:
		case MOVE_DONE:
			break;
		default: G.Error("Not expecting "+m);
		
		}
	}
	
	return(targets);
}

}
