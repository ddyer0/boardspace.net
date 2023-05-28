package kingscolor;


import static kingscolor.KingsColormovespec.*;

import java.awt.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;
/**
 * Kings color, as a chess-like game, has a check/checkmate mechanic in the actual moves,
 * but for convenience and speed, the robot allows check/checkmate to be emergent properties.
 * 
 * @author ddyer
 *
 */
class KingsColorBoard 
	extends hexBoard<KingsColorCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,KingsColorConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	boolean PREFILTER = true;
    static final String[] GRIDSTYLE =  { null, "A1", "A1" }; // left and bottom numbers

	KingsColorVariation variation = KingsColorVariation.kingscolor;
	private KingsColorState board_state = KingsColorState.Puzzle;	
	private StateStack robotState = new StateStack();
	public KingsColorState getState() { return(board_state); }
	private GridColor kingsColor[] = new GridColor[2];
	private KingsColorCell kingLocation[] = new KingsColorCell[2];
	private CellStack occupiedCells[] = { new CellStack(),new CellStack() };
	KingsColorCell captured [] = new KingsColorCell[2];
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(KingsColorState st) 
	{ 	
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	
	// changeRecord is a lightweight stack of undo info
	// to reverse changes in PieceType identities.  Integers
	// encode the location and original type of the PieceType
	// The top element of the stack is the number of undo steps for the next move.
	//
	private IStack changeRecord = new IStack();
	
	private int encodeCapture(KingsColorCell c,KingsColorChip type)
	{
		return(encodeChange(c,type)|(1<<24));
	}
	private int encodeChange(KingsColorCell c,KingsColorChip type)
	{
		return((c.col<<16)+(c.row<<8)+type.chipNumber());
	}
	private KingsColorChip revertChange(int enc)
	{
		char col = (char)((enc>>16)&0xff);
		int row = (enc>>8)&0xff;
		KingsColorChip chip = KingsColorChip.getChip(enc&0xff);
		KingsColorCell c = getCell(col,row);
		SetBoard(c,chip);
		return(chip);
	}
	private void revertChanges()
	{
		int n = changeRecord.pop();
		while(n-- > 0)
		{
			revertChange(changeRecord.pop());
		}
	}
	private void revertCaptures()
	{
		int n = changeRecord.topz(0);	// top cell, or 0 if the stack is empty
		if((n&(1<<24))!=0)
		{
			KingsColorChip chip = revertChange(changeRecord.pop());
			int ind = playerIndex(chip.colorId);
			KingsColorChip p = captured[ind].removeTop();
			G.Assert(p==chip,"capture mismatch at undo");
		}
	}
    private ColorId playerColor[]={ColorId.White,ColorId.Black};    
    public int playerIndex(ColorId id) { return(id==playerColor[0] ? 0 : 1); }
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public KingsColorChip getPlayerChip(int p) { return(KingsColorChip.getChip(playerColor[p],PieceType.King)); }
	public ColorId getPlayerColor(int p) { return(playerColor[p]); }
	public KingsColorChip getCurrentPlayerChip() { return(getPlayerChip(whoseTurn)); }

// this is required even if it is meaningless for this game, but possibly important
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
    public KingsColorChip pickedObject = null;
    public KingsColorChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    private KingsColorState resetState = KingsColorState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public KingsColorCell newcell(char c,int r)
	{	return(new KingsColorCell(ColorId.BoardLocation,c,r));
	}
	
	// constructor 
    public KingsColorBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
        captured[0] = new KingsColorCell(ColorId.Captured,'@',0);
        captured[1] = new KingsColorCell(ColorId.Captured,'@',1);
        
        doInit(init,key,players,rev); // do the initialization 
        autoReverseYNormal();		// reverse_y based on the color map
    }
    
    public String gameType() { return(G.concat(gametype," ",players_in_game," ",randomKey," ",revision)); }
    

    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }

    public boolean reInitBoard(int []first,int []n,int[] od)
    {
    	if(super.reInitBoard(first, n, od))
    	{
     	GridColor colors[] = GridColor.values();
     	reInit(occupiedCells);
		for(KingsColorCell c = allCells; c!=null; c=c.next)
		{	// this is somewhat ad-hoc to accomplish a tri color hexagonal board
			char col = c.col;
			int row =  c.row;
			
			c.gridColor = colors[triColor(col,row)];	
			// mark the cells that wall up the king.  For reasons that are not 
			// quite clear, these letters and numbers don't correspond to the
			// letters and numbers on the drawn grid
			c.wall = ((col=='L') && ((row>=4) && (row<=7)))
					|| ((col=='K') && ((row==8)||(row==4)))
					|| ((col=='J') && ((row==9)||(row==5)))
					|| ((col=='I') && ((row==6) || (row==10)))
					|| ((col=='H') && ((row>=7) && (row<=10)))
					
					|| ((col=='B') && ((row>=2) && (row<=5)))
					|| ((col=='C') && ((row==6)||(row==2)))
					|| ((col=='D') && ((row==6)||(row==2)))
					|| ((col=='E') && ((row==6)||(row==2)))
					|| ((col=='F') && ((row<=6)&&(row>=3)));
			if(c.wall) { c.castleOwner =  col<='F' ? ColorId.White : ColorId.Black; }
		}
		return(true);
		}
    	return(false);
    }
    
    // place the initial pieces on the board
    private void placePieces(char col,int row,ColorId color)
    {	
    	KingsColorCell k = getCell(col,row);
    	int index = playerIndex(color);
    	k.castle = true;
    	k.castleOwner = color;
    	k.addChip(KingsColorChip.getChip(color,PieceType.King));
    	kingLocation[index]=k;
    	kingsColor[index] = k.gridColor;
     	KingsColorChip bishop = KingsColorChip.getChip(color,PieceType.Bishop);
    	KingsColorChip rook = KingsColorChip.getChip(color,PieceType.Rook);
    	// surround the king with bishops
    	for(int direction = 0; direction<CELL_FULL_TURN; direction++)
    	{
    		KingsColorCell d = k.exitTo(direction);
    		d.addChip(bishop);
    		d.castle = true;
    		d.castleOwner = color;
    	}
    	// place rooks at the left and right.  Incidentally they
    	// are also on the king's color, as required.
    	KingsColorCell lr = k.exitTo(CELL_DOWN_LEFT).exitTo(CELL_UP_LEFT);
    	lr.addChip(rook);
     	lr.castle = true;
    	lr.castleOwner = color;
    	KingsColorCell rr = k.exitTo(CELL_DOWN_RIGHT).exitTo(CELL_UP_RIGHT);
    	rr.addChip(rook);
    	rr.castle = true;
    	rr.castleOwner = color;
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
 		setState(KingsColorState.Puzzle);
		variation = KingsColorVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		changeRecord.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case kingscolor:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
		}
		int map[] = getColorMap();
		placePieces('J',7,playerColor[map[1]]);
		placePieces('D',4,playerColor[map[0]]);
 		
		buildOccupied();
		
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    reInit(captured);
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int ind = playerIndex(ColorId.White);
		playerColor[map[ind]]=ColorId.White;
		playerColor[map[ind^1]]=ColorId.Black;
	    // set the initial contents of the board to all empty cells
	    
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public KingsColorBoard cloneBoard() 
	{ KingsColorBoard dup = new KingsColorBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((KingsColorBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(KingsColorBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        changeRecord.copyFrom(from_b.changeRecord);
        getCell(occupiedCells,from_b.occupiedCells);
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(captured,from_b.captured);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        getCell(kingLocation,from_b.kingLocation);
        AR.copy(playerColor,from_b.playerColor);
  
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((KingsColorBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(KingsColorBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(changeRecord.sameContents(from_b.changeRecord), "changerecord mismatch");
        G.Assert(sameCells(kingLocation,from_b.kingLocation),"kinglocation mismatch");
        G.Assert(sameContents(captured,from_b.captured),"captured mismatch");
        G.Assert(occupiedCells[0].size()==from_b.occupiedCells[0].size(), "occupied mismatch");
        G.Assert(occupiedCells[1].size()==from_b.occupiedCells[1].size(), "occupied mismatch");
        
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
        long v = super.Digest(r);
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= Digest(r,kingLocation);
		v ^= Digest(r,captured);
		v ^= changeRecord.Digest(r);
		v ^= Digest(r,occupiedCells[0].size());
		v ^= Digest(r,occupiedCells[1].size());
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
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }



    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	boolean win = false;
    	return(win);
    }
    public double staticEval(int who)
    {	// simple wood-only static evaluation for UCT searches
    	// the main point of this is to punish pointless sacrifices
    	double count = captured[who^1].height()-captured[who].height();
    	return(count/10);   	
    }

    // set the contents of a cell, and maintain the books
    // return true if the king color changes
    public boolean SetBoard(KingsColorCell c,KingsColorChip ch)
    {	KingsColorChip old = c.topChip();
       	if(old!=null) 
       		{ int index = playerIndex(old.colorId);
       		  KingsColorCell rem = occupiedCells[index].remove(c,false);
       		  rem.removeTop();		// if this ever gets out of sync an error will occur, deliberately
       		  if(old.pieceType==PieceType.King) 
       		  	{ 
           		kingLocation[index]= null; 
           		//if(getName().equals("main")) 
           		//	{ G.print("king null"); }
       		  	}
       		}
       	if(ch!=null) 
       		{ c.addChip(ch); 
       		  int index = playerIndex(ch.colorId);
       		  occupiedCells[index].push(c);
       		  if(ch.pieceType==PieceType.King)
       		  {	  
       		      GridColor oldcolor = kingsColor[index];
       			  GridColor newcolor = kingsColor[index] = c.gridColor;
       			  kingLocation[index] = c;
             		//if(getName().equals("main")) 
           			//{ G.print("king "+c); }
      			  return(oldcolor!=newcolor);
       		  }
       		}
    	return(false);
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
    private KingsColorCell unDropObject()
    {	revertChanges();    	
    	KingsColorCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = rv.topChip();
    	SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	revertCaptures();
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	KingsColorCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    private void smallSwitch(KingsColorCell c,KingsColorChip top)
    {	PieceType id = top.pieceType;
    	switch(id) {
    	default: throw G.Error("Not expecting %s",id);
    	case Rook:
    		if(!(c.castle && (c.castleOwner!=top.colorId)))	// not going to become a queen
    		{
    		int index = playerIndex(top.colorId);
        	GridColor kc = kingsColor[index];
        	if(kc!=c.gridColor)
        	{	changeRecord.push(encodeChange(c,top));
        		SetBoard(c,KingsColorChip.getChip(top.colorId,PieceType.Bishop));
        	}}
			//$FALL-THROUGH$
		case Bishop:
    		if(c.castle && (c.castleOwner!=top.colorId))	// becoming a queen
    		{	changeRecord.push(encodeChange(c,top));
    			SetBoard(c,KingsColorChip.getChip(top.colorId,PieceType.Queen));
    		}
    		break;
    	case King:
    	case Queen:
    		break;
    	}
    }
    
    // king has changed color, bishops on that color become rooks,
    // rooks not on that color become bishops
    private void bigSwitch(KingsColorChip king)
    {
    	int index = playerIndex(king.colorId);
    	GridColor kc = kingsColor[index];
    	ColorId kingid = king.colorId;
    	CellStack occupied = occupiedCells[index];
    	for(int lim=occupied.size()-1; lim>=0; lim--)
    	{	KingsColorCell c = occupied.elementAt(lim);
    		KingsColorChip top = c.topChip();
    		if(top.colorId==kingid)
    		{
    			switch(top.pieceType)
    			{
    			default: throw G.Error("Not expecting type %s", top.pieceType);
    			case Queen:	// queens are stable
    			case King: 	// kings are stable
    				break;
    			
    			case Bishop:
    				// maybe become rook if on the king color
    				if(kc==c.gridColor)
    				{	changeRecord.push(encodeChange(c,top));
    					SetBoard(c,KingsColorChip.getChip(kingid,PieceType.Rook));
    				}
    				break;
    			case Rook:
    				// maybe become bishop if not on king color
    				if(kc!=c.gridColor)
    				{	changeRecord.push(encodeChange(c,top));
    					SetBoard(c,KingsColorChip.getChip(kingid,PieceType.Bishop));
    				}
    				
    			}
    		}
    	}
    }
    // 
    // drop the floating object.
    //
    private void dropObject(KingsColorCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Black:
        case White:		// back in the pool, we don't really care where
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
        	{
        	KingsColorChip po = pickedObject;
            pickedObject = null;
            // first record the capture, if any
            KingsColorChip top = c.topChip();
            if(top!=null) 
            	{ changeRecord.push(encodeCapture(c,top)); 
            	  int ind = playerIndex(top.colorId);
            	  captured[ind].addChip(top);
            	}
            int level = changeRecord.size();
          	boolean newcolor = SetBoard(c,po);
            if(newcolor)
            	{
            		bigSwitch(po);
            	}
            else {
            	smallSwitch(c,po);
            }
        	changeRecord.push(changeRecord.size()-level);
        	}

            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(KingsColorCell c)
    {	return(droppedDestStack.top()==c);
    }
    public KingsColorCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { KingsColorChip ch = pickedObject;
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
    private KingsColorCell getCell(ColorId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        	return(getCell(col,row));
 
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public KingsColorCell getCell(KingsColorCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(KingsColorCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.topChip();
         	lastDroppedObject = null;
			SetBoard(c,null);
        	}
            break;

        case Black:
        case White:
        	lastPicked = pickedObject = c.topChip();
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(KingsColorCell c)
    {	return(c==pickedSourceStack.top());
    }
    public KingsColorCell getSource()
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
        case Check:
			setState(KingsColorState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    		if(kingLocation[whoseTurn]==null)
    		{	// king actually captured, this won't happen in the real game,
    			// but it is the normal endgame position in robot lookahead
    			setState(KingsColorState.Gameover);
    			win[whoseTurn^1] = true;
    		}
    		else 
    		{
    		KingsColorState next = kingInCheck(kingLocation[whoseTurn],playerColor[whoseTurn])
					? KingsColorState.Check
					: KingsColorState.Play;
    			setState(next);	
	    		if(next==KingsColorState.Check)
	    		{	// check if there is any escape
	    			CommonMoveStack raw = GetListOfMoves(whoseTurn);
	    			CommonMoveStack all = filterCheckMoves(raw,whoseTurn);

	    			if(all.size()==0) 
	    				{ 	
	    					win[whoseTurn^1] = true;	// win, otherwise stalemate
	    					setState(KingsColorState.Gameover); 
	    				}
	    		}
	    		else if ((robot==null) && !hasMoves()) { setState(KingsColorState.Gameover); }
	    		
	    	}
	    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==KingsColorState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(KingsColorState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(KingsColorState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }

	
    public boolean Execute(commonMove mm,replayMode replay)
    {	KingsColormovespec m = (KingsColormovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_STALEMATE:
        	setState(KingsColorState.Gameover);
        	break;
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_FROM_TO:
        case MOVE_CAPTURE:
        	{
        		KingsColorCell from = getCell(m.from_col,m.from_row);
        		if(pickedObject==null) { pickObject(from); }
        		else { G.Assert(from==getSource(),"picked wrong source"); }
        	}
			//$FALL-THROUGH$
		case MOVE_DROPB:
        	{
			KingsColorChip po = pickedObject;
			KingsColorCell dest =  getCell(ColorId.BoardLocation,m.to_col,m.to_row);
			if((replay==replayMode.Single)
					|| ((replay==replayMode.Live) && (m.op!=MOVE_DROPB)))
				{
				animationStack.push(getSource());
				animationStack.push(dest);
				}
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				m.chip = po;
		   
				if( (replay!=replayMode.Replay) && (dest.topChip()!=null))
				{
					animationStack.push(dest);
					animationStack.push(captured[whoseTurn^1]);
				}
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

 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			KingsColorCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.chip = pickedObject;
 			}}
            break;

 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(KingsColorState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(KingsColorState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	if(board_state==KingsColorState.Resign) { setState(stateStack.pop()); }
    	   else { stateStack.push(board_state); setState(KingsColorState.Resign); }
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(KingsColorState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(KingsColorState.Gameover);
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
        case Play:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
            return (true);
        }
    }

    public boolean legalToHitBoard(KingsColorCell c,Hashtable<KingsColorCell,KingsColormovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
		case Check:
			return(targets.get(c)!=null || isDest(c) || isSource(c));
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
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
    private void TestExecute(KingsColormovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("E "+m);
        Execute(m,replayMode.Replay);
        acceptPlacement();
       
    }
 
    public void RobotExecute(KingsColormovespec m)
    {	TestExecute(m);
    	if(kingInCheck(kingLocation[whoseTurn],playerColor[whoseTurn]))
    		{
    		// moved into check, or failed to escape it
    		win[whoseTurn^1]=true;
    		setState(KingsColorState.Gameover);
    		}
    	else if(DoneState())
    		{ doDone(replayMode.Replay); 
    		}      
    }

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(KingsColormovespec m)
    {
        //G.print("U "+m);
    	KingsColorState state = robotState.pop();
         switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
        case MOVE_STALEMATE:
            break;
            
        case MOVE_FROM_TO:
        case MOVE_CAPTURE:
        	{
        	KingsColorCell from = getCell(m.from_col,m.from_row);
        	KingsColorCell to = getCell(m.to_col,m.to_row);
        	revertChanges();
        	KingsColorChip top = to.topChip();
        	SetBoard(to,null);
        	SetBoard(from,top);
        	revertCaptures();   	
        	}
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
    private boolean bishopCaptureTarget(KingsColorCell from,int direction,ColorId targetColor)
    {
    	KingsColorCell dest = from;
    	while( (dest=dest.exitTo(direction))!=null)
    	{	// intermediate cell doesn't matter if it is occupied
     		if((dest = dest.exitTo(direction+1))==null) { break; }
    		else
    		{
    			KingsColorChip top = dest.topChip();
    			if(top!=null)
    			{	// if we hit a bishop or a queen of the desired color, it would be able to see us.
    				if(top.colorId==targetColor) { break; }	// hit something of our color
    				switch(top.pieceType)
    				{
    				case Bishop: 
    				case Queen: return(true);
    				default: break;
    				}
    				break;	// any other piece blocks the line of sight
    			}
    		}
    	}
    	return(false);
    }
    private boolean rookCaptureTarget(KingsColorCell from,int direction,ColorId targetColor)
    {
    	KingsColorCell dest = from;
    	while((dest=dest.exitTo(direction))!=null) 
    	{
    		KingsColorChip top = dest.topChip();
    		if(top!=null)
    		{	
    			if(top.colorId==targetColor) { break; }
    			else
    			{
     			switch(top.pieceType)	
    			{
    			case Rook:
    			case Queen:
    				return(true);
    			default: break;
    			}
    			}
    			break;	// any other piece blocks the line of sight
    		}
    	}
    	return(false);
    }
  
    private boolean rookCaptureTarget(KingsColorCell from,ColorId targetColor)
    {
    	for(int direction=0; direction<CELL_FULL_TURN; direction++)
    	{
    		if(rookCaptureTarget(from,direction,targetColor)) { return(true); }
    	}
    	return(false);
    }
    
    private boolean bishopCaptureTarget(KingsColorCell from,ColorId targetColor)
    {
    	for(int direction=0; direction<CELL_FULL_TURN; direction++)
    	{
    		if(bishopCaptureTarget(from,direction,targetColor)) { return(true); }
    	}
    	return(false);
    }
    // return true if the king on "from" is in check from some PieceType of color "targetColor"
    private boolean kingInCheck(KingsColorCell from,ColorId targetColor)
    {	if(from!=null)
    	{
    	return(bishopCaptureTarget(from,targetColor) || rookCaptureTarget(from,targetColor));
    	}
    	return(false);
    }
    
    private boolean recordMove(CommonMoveStack all,KingsColorCell from,KingsColorCell to,KingsColorChip fromChip,KingsColorChip toChip,int player)
    {	
    	if(to.marked) 
    		{ // the robot uses this to exclude the castle during the opening
    		return(false);
    		} 
    	if(PREFILTER && (board_state==KingsColorState.Check))
    	{
    		// if we are in check, only record moves that escape from check
    		KingsColorChip top = from.topChip();
    		SetBoard(from,null);
    		SetBoard(to,fromChip);
    		boolean check = kingInCheck(kingLocation[player],playerColor[player]);
    		SetBoard(to,toChip);
    		SetBoard(from,top);		// restore the actual top chip
    		if(check) { return(false); }
    	}
    	if(all==null) { return(true); }
    	all.push(new KingsColormovespec(toChip==null?MOVE_FROM_TO:MOVE_CAPTURE,from,to,player));
    	return(true);
    }
    // any unobstructed move in a diagonal direction, which is effectively a knight move on this diagonal grid
    private boolean addDiagonalMoves(CommonMoveStack all,int direction,KingsColorCell from,KingsColorChip chip,int who)
    {	boolean some = false;
    	GridColor color = from.gridColor;
    	KingsColorCell dest = from;
    	while((dest!=null) && (dest=dest.exitTo(direction))!=null)
    	{	if((dest=dest.exitTo(direction+1))!=null)
    		{
       		G.Assert((color==dest.gridColor),"wrong color");
       		KingsColorChip dtop = dest.topChip();
    		if(dtop!=null) 
    		{ 
    			if( ((((from.wall && dest.castle)||(from.castle && dest.wall))
    					&& (from.castleOwner==dest.castleOwner))
    					||(dtop.pieceType==PieceType.King))
    					&& (dtop.colorId!=chip.colorId)
    					)
    			{	// capture between pieces on the wall and pieces in the same castle
    				some |= recordMove(all,from,dest,chip,dtop,who);
    			}
    			break;
    		}
    		some |= recordMove(all,from,dest,chip,dtop,who);
    		}
    	}
    
    	return(some);
    }
    private boolean addDiagonalMoves(CommonMoveStack all,KingsColorCell from,KingsColorChip chip,int who)
    {	boolean some = false;
   	for(int direction = CELL_UP,limit=CELL_UP+CELL_FULL_TURN; direction<limit; direction++)
   	{	some |= addDiagonalMoves( all,direction, from, chip, who);
   		if(some && all==null) { break; }
   	}	
   	return(some);
    }
 // any unobstructed move in a direction, except bishops have to stay on the same color
 private boolean addRookMoves(CommonMoveStack all,int direction,KingsColorCell from,KingsColorChip chip,int who)
 {	boolean some = false;
 	KingsColorCell dest = from;
 	while((dest=dest.exitTo(direction))!=null)
 	{
 		KingsColorChip dtop = dest.topChip();
 		if(dtop!=null) 
 			{
 			if(((((from.wall && dest.castle)||(from.castle && dest.wall))
					&& (from.castleOwner==dest.castleOwner))
					||(dtop.pieceType==PieceType.King))
					&& (dtop.colorId!=chip.colorId)
					)
 			{	// capture between pieces on the wall and pieces in the same castle
 				some |= recordMove(all,from,dest,chip,dtop,who);
			}
 			break;
 			}
 		some |= recordMove(all,from,dest,chip,dtop,who);
 	}
 
 	return(some);
 }
 private boolean addRookMoves(CommonMoveStack all,KingsColorCell from,KingsColorChip chip,int who)
 {	boolean some = false;
	for(int direction = CELL_UP,limit=CELL_UP+CELL_FULL_TURN; direction<limit; direction++)
	{	some |= addRookMoves( all,direction, from, chip, who);
		if(some && all==null) { break; }
	}	
	return(some);
 }
 private boolean addDiagonalKingMove(CommonMoveStack all,KingsColorCell from,KingsColorChip chip,KingsColorCell left,int who)
 {	boolean some = false;
	 KingsColorChip ltop = left.topChip();
	 if(!left.wall && ((ltop==null) || (ltop.colorId!=chip.colorId)))
	 	{ // unlike other pieces, the king can capture anything inside the castle
		  some |= recordMove(all,from,left,chip,ltop,who);
	 	}
	 return(some);
 }
 private boolean addDiagonalKingMove(CommonMoveStack all,int direction,KingsColorCell origin,KingsColorCell from,KingsColorChip chip,int who)
 {	boolean some = false;
 	// try both the left and right step
 	KingsColorCell ortho = from.exitTo(direction);
	 if(!ortho.wall) {
		 {
		 addDiagonalKingMove(all,origin,chip,ortho.exitTo(direction+1),who);
		 }
	 }
	 return(some);
 }
 private boolean addKingMoves(CommonMoveStack all,KingsColorCell from,KingsColorChip chip,int who)
 {	boolean some = false;
	for(int direction = CELL_UP,limit=CELL_UP+CELL_FULL_TURN; direction<limit; direction++)
	{	// king moves are not quite right yet, "knight's move"
		some |= addDiagonalKingMove(all,direction,from,from,chip,who);
		
		KingsColorCell adj = from.exitTo(direction);
		if(!adj.wall)
		{	KingsColorChip dtop = adj.topChip();
			if((dtop==null) || (dtop.colorId!=chip.colorId))
				{
				// unlike other pieces, the king can capture anything inside the castle
				some |= recordMove(all,from,adj,chip,dtop,who);
				some |= addDiagonalKingMove(all,direction,from,adj,chip,who);
				}
		}
	}
 	return(some);
 }
 // add moves from a particular cell with top as the top chip.
 // this is called when the user has picked up a piece as well as
 // when the board is stable.
 private boolean addMoves(CommonMoveStack all,KingsColorCell from,KingsColorChip top,int who)
 {	boolean some = false;
 	switch(top.pieceType)
			 {
			 default: throw G.Error("Not expecting %s",top);
			 case King:	
				 // kings can move in any direction, and also perform a "knight move", but must
				 // remain inside the castle.
				 some |= addKingMoves(all,from,top,who);
				 break;
			 case Bishop:
				 // bishops can move in diagonal directions
				 some |= addDiagonalMoves(all,from,top,who);
				 break;
			 case Rook:
				 // rooks move in orthogonal directions
				 some |= addRookMoves(all,from,top,who);
				 break;
				 
			 case Queen:
				 // queens move in either type of direction
				 some |= addDiagonalMoves(all,from,top,who);
				 if(some && (all==null)) { break; }
				 some |= addRookMoves(all,from,top,who);
				 break;
			 }

	 return(some);
 }
 	public commonMove getRandomMove(Random r)
 	{
 		CellStack occupied = occupiedCells[whoseTurn];
 		int sz = occupied.size();
 		int ind = r.nextInt(sz);
 		int loop = 0;
 		while(loop++ < 3)
 		{
 		if(ind>=sz) { ind = 0; }
 		KingsColorCell c = occupied.elementAt(ind++);
 		CommonMoveStack all = new CommonMoveStack();
 		addMoves(all,c,c.topChip(),whoseTurn);
 		int allsize = all.size();
 		if(allsize>0)
 			{
 			return(all.elementAt(r.nextInt(allsize)));
 			}
 		}
 		return(null);	// give up
 	}
 // build a move list, 
 // return true if there are some
 // all may be null
 private boolean addMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	CellStack occupied = occupiedCells[who];
 	for(int lim = occupied.size()-1; lim>=0; lim--)
	 {	KingsColorCell c = occupied.elementAt(lim);
	 	KingsColorChip top = c.topChip();
	 	if(top.colorId==playerColor[who])
		 {	some |= addMoves(all,c,top,who);
		    if(some && all==null) { break; }
		 }
	 }
	 return(some);
 }

 CommonMoveStack  GetListOfMoves()
 {
	 return GetListOfMoves(whoseTurn);
 }
 private boolean hasMoves()
 {
	 return getListOfMoves(null,whoseTurn);
 }
 CommonMoveStack GetListOfMoves(int who)
 {
	 return GetListOfMoves(who,null,null);
 }
 CommonMoveStack  GetListOfMoves(int who,KingsColorCell from,KingsColorChip move)
 {	CommonMoveStack all = new CommonMoveStack();
 	getListOfMoves(all,from,move,who);
 	return(all);
 }
 private boolean getListOfMoves(CommonMoveStack all,int who)
 {
	return(getListOfMoves(all,getSource(),pickedObject,who));
 }
 private boolean getListOfMovesAsPlay(CommonMoveStack all,KingsColorCell from,KingsColorChip movingChip,int who)
 {	boolean some = false;
 	if(movingChip==null) { some |= addMoves(all,who); }
 	else if(movingChip.colorId==playerColor[who]) { some |= addMoves(all,from,movingChip,who); }
 	return(some);
 }
 private boolean getListOfMoves(CommonMoveStack all,KingsColorCell from,KingsColorChip movingChip,int who)
 {	boolean some = false;
 	switch(board_state)
 	{
 	case Gameover:
 		break;
 	case Puzzle:
 		{int op = movingChip==null ? MOVE_DROPB : MOVE_PICKB; 	
 			for(KingsColorCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	if(c.topChip()==null)
 			 		{
 			 		if(all!=null) { all.addElement(new KingsColormovespec(op,c.col,c.row,who)); }
 			 		some = true;
 			 		}
 			 	}
 		}
 		break;
 	case Play:
 	case Check:
 		some |= getListOfMovesAsPlay(all,from,movingChip,who);
  		break;
 	case Confirm:
 	case Resign:
 		if(all!=null) { all.push(new KingsColormovespec(MOVE_DONE,who)); }
 		some = true;
 		break;
 		
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(some);
 }
 
//
// this is called on a copy of the real board, so there is no
// possibility that it will be corrupted.
//
 private CommonMoveStack filterCheckMoves(CommonMoveStack all,int who)
 {	
	 switch(board_state)
		 {
		 case Puzzle:
		 case Gameover:
		 case Resign:
		 case Confirm: 
			 break;
		 default:
			 
			boolean picked = pickedObject!=null;
		 	if(picked) { unPickObject(); }
		 	acceptPlacement();
		 	long dig = Digest();
		 	int revertWT = whoseTurn;
		 	KingsColorBoard original = cloneBoard();
		 	original.sameboard(this);
			for(int lim = all.size()-1; lim>=0; lim--)
			{
				KingsColormovespec m = (KingsColormovespec)all.elementAt(lim);
				TestExecute(m);
				if(kingInCheck(kingLocation[who],playerColor[who]))
					{ all.remove(lim,false); 
					}
				UnExecute(m);
				whoseTurn = revertWT;
				long redig = Digest();
				if(dig!=redig)
				{
					original.sameboard(this);
					G.Assert(dig==redig,"unwound");
				}
				
		}}
	
	return(all);
 }
 

KingsColorBoard targetBoard = null;
 /**
  *  get the board cells that are valid targets right now.  This has to be filtered
  *  for check so the user isn't allowed to even think about illegal moves that 
  *  either uncover a check or fail to resolve one.  The move generator does some
  *  temporary modificiation of the board (very undesirable, but for effeciency)
  *  so the whole process is run on a copy of the board.
  * @return
  */
 public Hashtable<KingsColorCell, KingsColormovespec> getTargets(int who) 
 {	if(targetBoard==null) { targetBoard = cloneBoard(); } else { targetBoard.copyFrom(this); }
 	return targetBoard.getTargetsInternal(this,who);
 }
 private Hashtable<KingsColorCell, KingsColormovespec>getTargetsInternal(KingsColorBoard parent,int who)
 {
	CommonMoveStack all = GetListOfMoves(who,getSource(),pickedObject);
	return filterTargets(parent,all,who,pickedObject);
 }
 /* get targets as though it were someone's move
 * The move generator does some
 *  temporary modificiation of the board (very undesirable, but for effeciency)
 *  so the whole process is run on a copy of the board.
 *  */
 public Hashtable<KingsColorCell, KingsColormovespec> getTargets(int who,KingsColorCell from,KingsColorChip move) 
 {	if(targetBoard==null) { targetBoard = cloneBoard(); } else { targetBoard.copyFrom(this); }
 	return targetBoard.getTargetsInternal(this,who,targetBoard.getCell(from),move);
 }
 private Hashtable<KingsColorCell, KingsColormovespec> getTargetsInternal(KingsColorBoard parent,int who,KingsColorCell from,KingsColorChip move)
 {	CommonMoveStack all = new CommonMoveStack();
 	getListOfMovesAsPlay(all,from,move,who);
 	return filterTargets(parent,all,who,move);
 }
 
 private Hashtable<KingsColorCell, KingsColormovespec> filterTargets(KingsColorBoard parent,CommonMoveStack raw,int who,KingsColorChip move)
 {
 	Hashtable<KingsColorCell,KingsColormovespec> targets = new Hashtable<KingsColorCell,KingsColormovespec>();
 	CommonMoveStack all = filterCheckMoves(raw,who);
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	KingsColormovespec m = (KingsColormovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_PICKB:
 			targets.put(parent.getCell(m.from_col,m.from_row), m);
 			break;
 		case MOVE_DROPB:
 			targets.put(parent.getCell(m.to_col,m.to_row), m);
 			break;
 		case MOVE_CAPTURE:
 		case MOVE_FROM_TO:
 			if(move!=null)
 			{
 				targets.put(parent.getCell(m.to_col,m.to_row), m);
 			}
 			else {
 				targets.put(parent.getCell(m.from_col,m.from_row), m);
 			}
 			break;
 		case MOVE_DONE:
 			break;

 		default: G.Error("Not expecting "+m);
 		
 		}
 	}
 	
 	return(targets);
 }
 	public void initRobotValues(KingsColorPlay r,boolean nocastle)
 	{
 		robot = r;
 		ColorId owner = nocastle ? playerColor[whoseTurn^1] : null;
 		for(KingsColorCell c = allCells; c!=null; c=c.next)
 		{
 			c.marked = c.castleOwner==owner;
 		}
 		
 	}
 	public KingsColorPlay robot = null;
	public boolean p1(String msg)
	{
		if(G.p1(msg) && (robot!=null))
		{	String dir = "g:/share/projects/boardspace-html/htdocs/kingscolor/kingscolorgames/robot/";
			robot.saveCurrentVariation(dir+msg+".sgf");
			return(true);
		}
		return(false);
	}
	

	 // small ad-hoc adjustment to the grid positions
	 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
	 {   int tlen = txt.length();
	 	 boolean digits = (tlen>2 || txt.charAt(tlen-1)>'1');
	 	 if(reverseY())
	 	 {
	 		if(!digits)
	 		{ xpos -= cellsize/3;
	 		}
	 	 }
	 	 else if(digits)
		 	{ 
		 		xpos -= cellsize/2;	 		
		 	}
	 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
	 }
	 public void buildOccupied()
	 {
		 reInit(occupiedCells);
		 for(KingsColorCell c = allCells; c!=null; c=c.next)
		 {
			 KingsColorChip top = c.topChip();
			 if(top!=null)
			 {
				 occupiedCells[playerIndex(top.colorId)].push(c);
			 }
		 }
	 }
	 public void checkOccupied()
	 {	int n = 0;
	 	for(KingsColorCell c = allCells; c!=null; c=c.next)
		 {	KingsColorChip top = c.topChip();
		 	if(top!=null)
		 		{
		 		n++;
		 		int ind = playerIndex(top.colorId);
		 		G.Assert(occupiedCells[ind].contains(c),"cell %s not present",c);
		 		}
		 }
		 G.Assert(n==occupiedCells[0].size()+occupiedCells[1].size(),"extra occupied cells");
	 }
	 
}
