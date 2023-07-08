package proteus;

import bridge.Color;

import java.util.*;
import online.game.*;
import lib.*;
import lib.Random;
import static proteus.ProteusMovespec.*;
/**
 * ProteusBoard knows all about the game of Proteus.
 * It gets a lot of logistic support from game.rectBoard, 
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

class ProteusBoard extends rectBoard<ProteusCell> implements BoardProtocol,ProteusConstants
{	public static int REVISION = 101;  	// revision 101 fixes the tile 
	public int getMaxRevisionLevel() { return(REVISION); }
	
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

    public int boardColumns;	// size of the board
    public int boardRows;
    public void SetDrawState() { setState(ProteusState.Draw); }
    public ProteusCell rack[] = null;
    public CellStack animationStack = new CellStack();
    
    public ProteusCell blackChips[] = new ProteusCell[ProteusChip.BlackChips.length];	// home for the black pieces
    public ProteusCell whiteChips[] = new ProteusCell[ProteusChip.WhiteChips.length];	// home for the white pieces
    public ProteusCell playerChips[][] = { blackChips,whiteChips };
    public ProteusCell originalTiles[] = new ProteusCell[ProteusChip.MainChips.length];	// original home for the 
	public PieceColor playerColors[] = { PieceColor.Player_Black, PieceColor.Player_White };
    //
    // private variables
    //
    private ProteusState board_state = ProteusState.Placement;	// the current board state
    private ProteusState unresign = null;					// remembers the previous state when "resign"
    Variation variation = Variation.Proteus;
    public ProteusState getState() { return(board_state); } 
	public void setState(ProteusState st) 
	{ 	unresign = (st==ProteusState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	
    public Move move = Move.none;
    public Trade trade = Trade.none;
    public Goal goal = Goal.none;
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ProteusChip pickedObject = null;
    private IStack levelStack = new IStack();
    private StateStack stateStack = new StateStack();
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    
	// factory method
	public ProteusCell newcell(char c,int r)
	{	return(new ProteusCell(c,r));
	}
    public ProteusBoard(String init,long rv,int np,int map[],int rev) // default constructor
    {   setColorMap(map, np);
        doInit(init,rv,np,rev); // do the initialization 
     }


	public void sameboard(BoardProtocol f) { sameboard((ProteusBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ProteusBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(trade == from_b.trade,"trade mismatch");
        G.Assert(goal == from_b.goal,"goal mismatch");
        G.Assert(move == from_b.move,"move mismatch");
        		
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are germane to the ordinary operation
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

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);

		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,whiteChips);
		v ^= Digest(r,blackChips);
		v ^= Digest(r,originalTiles);
		v ^= Digest(r,goal.ordinal());
		v ^= Digest(r,move.ordinal());
		v ^= Digest(r,trade.ordinal());
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public ProteusBoard cloneBoard() 
	{ ProteusBoard copy = new ProteusBoard(gametype,randomKey,players_in_game,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((ProteusBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ProteusBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        copyFrom(originalTiles,from_b.originalTiles);
        copyFrom(whiteChips,from_b.whiteChips);
        copyFrom(blackChips,from_b.blackChips);
        stateStack.copyFrom(from_b.stateStack);
        levelStack.copyFrom(from_b.levelStack);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        goal = from_b.goal;
        trade = from_b.trade;
        move = from_b.move;
        sameboard(from_b);
    }
    public void doInit(String gtype,long rv)
    {
       	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : rv;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np,int rev)
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = GRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
    	gametype = gtype;
    	players_in_game = np;
		rack = new ProteusCell[2];
    	Random r = new Random(67246765);
    	adjustRevision(rev);
    	variation = Variation.findVariation(gtype);
    	boardRows = boardColumns = 3;
    	initBoard(3,3);
        allCells.setDigestChain(r);
	    setState(ProteusState.Puzzle);
	    int map[] = getColorMap();
	    playerColors[map[FIRST_PLAYER_INDEX]] = PieceColor.Player_Black;
	    playerColors[map[SECOND_PLAYER_INDEX]] = PieceColor.Player_White;
	    playerChips[map[FIRST_PLAYER_INDEX]] = blackChips;
	    playerChips[map[SECOND_PLAYER_INDEX]] = whiteChips;
	    
	    
	    for(int i=0;i<ProteusChip.MainChips.length;i++)
	    {	ProteusCell c = originalTiles[i] = new ProteusCell(r,ProteusId.MainChips,i);
	    	c.addChip(ProteusChip.MainChips[i]);
	    }
	    for(int i=0;i<ProteusChip.WhiteChips.length;i++)
	    {	ProteusCell w = whiteChips[i] = new ProteusCell(r,ProteusId.WhiteChips,i);
	    	w.addChip(ProteusChip.WhiteChips[i]);
	    }
	    for(int i=0;i<ProteusChip.BlackChips.length;i++)
	    {	ProteusCell b = blackChips[i] = new ProteusCell(r,ProteusId.BlackChips,i);
	    	b.addChip(ProteusChip.BlackChips[i]);
	    }
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		stateStack.clear();
		levelStack.clear();
		pickedObject = null;
		goal = Goal.none;
		trade = Trade.none;
		move = Move.none;
        AR.setValue(win,false);
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }


    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        case Puzzle:
            break;
        default:
        case Gameover:
        	if(replay!=replayMode.Replay)
        	{
        	throw G.Error("Move not complete, can't change the current player %s",board_state);
        	}
			//$FALL-THROUGH$
        case ConfirmPlacement:
        case Confirm:
        case Draw:
        case Resign:
        case Pass:
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
        {case Resign:
         case Confirm:
         case ConfirmPlacement:
         case Pass:
         case Draw:
            return (true);

        default:
            return (false);
        }
    }

    //
    // we forbid repetition moves, so test when entering normal "play" state
    //
    public boolean DigestState()
    {	
    	switch(board_state)
    	{
    	case Play:
    		return (true);
        default:
            return (false);
        }
    }

    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(ProteusState.Gameover);
    }
    
    public boolean checkWinByColor(int player)
    {
    	int nmatch = 0;
		PieceColor someColor = null;
		PieceColor targetChip = playerColors[player];
		for(ProteusCell c = allCells; c!=null; c=c.next)
			{	ProteusChip ch = c.topChip();
				if((ch!=null) && (ch.pieceColor==targetChip))
				{	ProteusChip tile = c.chipAtIndex(0);
					if(nmatch==0)
					{ nmatch++;
					  someColor = tile.pieceColor;
					}
					else if (tile.pieceColor==someColor) { nmatch++; }
					else { return(false); }
				}
			}
		return(nmatch==3);
    }
    public boolean checkWinByShape(int player)
    {
    	int nmatch = 0;
		Shape someShape = null;
		PieceColor targetChip = playerColors[player];
		for(ProteusCell c = allCells; c!=null; c=c.next)
			{	ProteusChip ch = c.topChip();
				if((ch!=null) && (ch.pieceColor==targetChip))
				{	ProteusChip tile = c.chipAtIndex(0);
					if(nmatch==0)
					{ nmatch++;
					  someShape = tile.shape;
					}
					else if (tile.shape==someShape) { nmatch++; }
					else { return(false); }
				}
			}
		return(nmatch==3);
    }
    private boolean checkWinBy3(PieceColor targetChip,ProteusCell seed,int direction)
    {	for(int i=0;i<3;i++)
    	{	if(seed==null) { return(false); }
    		ProteusChip top = seed.topChip();
    		if((top==null) || (top.pieceColor!=targetChip)) { return(false); }
    		seed = seed.exitTo(direction);
    	}
    	return(true);
    }
    public boolean checkWinBy3(int player)
    {	PieceColor targetChip = playerColors[player];
    	ProteusCell seed = getCell('A',1);
    	
    	if(checkWinBy3(targetChip,seed,CELL_UP())) { return(true); }
    	if(checkWinBy3(targetChip,seed,CELL_RIGHT())) { return(true); }
    	if(checkWinBy3(targetChip,seed,CELL_UP_RIGHT())) { return(true); }
    	
    	ProteusCell col = seed.exitTo(CELL_RIGHT());
    	if(checkWinBy3(targetChip,col,CELL_UP())) { return(true); }
    	col = col.exitTo(CELL_RIGHT());
    	if(checkWinBy3(targetChip,col,CELL_UP())) { return(true); }
    	if(checkWinBy3(targetChip,col,CELL_UP_LEFT())) { return(true); }
    	
    	ProteusCell row = seed.exitTo(CELL_UP());
    	if(checkWinBy3(targetChip,row,CELL_RIGHT())) { return(true); }
    	row = row.exitTo(CELL_UP());
    	if(checkWinBy3(targetChip,row,CELL_RIGHT())) { return(true); }
    	
    	return(false);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==ProteusState.Gameover) { return(win[player]); }
    	switch(goal)
    	{	
    	case none:	
    		return(false);
    	case color:
    		return(checkWinByColor(player) && !checkWinByColor(nextPlayer[player]));
    	case shape:
    		return(checkWinByShape(player) && !checkWinByShape(nextPlayer[player]));
    	case three:
    		return(checkWinBy3(player) && !checkWinBy3(nextPlayer[player]));
    	default: throw G.Error("Not expecting goal %s",goal);
    	}   	
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot)
    {  	double finalv=0.0;
    	G.Error("not implemented");
    	return(finalv);
    }


    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        droppedDestStack.clear();
        pickedSourceStack.clear();
        levelStack.clear();
        stateStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	ProteusCell dr = droppedDestStack.pop();
    	int lvl = levelStack.pop();
    	setState(stateStack.pop());
    	if(lvl==-1) {  	pickedObject = dr.removeTop(); }
    	else { pickedObject = dr.removeChipAtIndex(lvl);}
    	
    	if(pickedObject.isTile() && (board_state==ProteusState.Play))
    	{
    	// undo the swap
    	ProteusCell c = getSource();
    	ProteusChip ch = c.removeChipAtIndex(0);
    	dr.insertChipAtIndex(0,ch);
    	}
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	ProteusChip po = pickedObject;
    	if(po!=null)
    	{
    		ProteusCell ps = pickedSourceStack.pop();
    		int lvl = levelStack.pop();
    		setState(stateStack.pop());
    		if(lvl==-1) { ps.addChip(po); }
    		else { ps.insertChipAtIndex(lvl,po); }
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(ProteusCell c,int lvl)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	
    	if(lvl==-1) { c.addChip(pickedObject); }
    	else { c.insertChipAtIndex(lvl,pickedObject); }
    	pickedObject = null;
    	levelStack.push(lvl);
    	stateStack.push(board_state);
    	droppedDestStack.push(c);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ProteusCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    public ProteusCell getDest() { return(droppedDestStack.top()); }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	ProteusChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    
    public ProteusCell getCell(ProteusId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        case BoardTile:
        	return(getCell(col,row));
        case MainChips:
        	return(originalTiles[row]);
        case WhiteChips:
       		return(whiteChips[row]);
        case BlackChips:
       		return(blackChips[row]);
        }
    }
    public ProteusCell getCell(ProteusCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(ProteusCell c,int lvl)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
		if(lvl==-1)  { pickedObject = c.removeTop(); }
		else { pickedObject = c.removeChipAtIndex(lvl); } 
		levelStack.push(lvl);
		stateStack.push(board_state);
    	pickedSourceStack.push(c);
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        case Gameover:
        	// needed for a few damaged games
        	if(replay==replayMode.Replay) { break; }
			//$FALL-THROUGH$
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Draw:
        	setNextStateAfterDone(); 
        	break;
        case Play:
        	setState(ProteusState.Confirm);
        	break;
        case Placement:
			setState(ProteusState.ConfirmPlacement);
			break;
        case Confirm:
        	setState(ProteusState.Play);
        	break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(ProteusCell c)
    {	return(getSource()==c);
    }
    public ProteusCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterPick(replayMode replay)
    {
        switch (board_state)
        {
        case Gameover:
        	// paper over damaged games
        	if(replay==replayMode.Replay) { break;}
			//$FALL-THROUGH$
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case ConfirmPlacement:
        case Draw:
        	setState(ProteusState.Placement);
        	break;
        case Confirm:
        	setState(ProteusState.Play);
        	break;
        case Placement:
			break;
        case Puzzle:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;

        case Draw:
        	setGameOver(false,false);
        	break;
    	case Confirm:
    		setState(ProteusState.Play);
    		break;
    	case Puzzle:
    	case ConfirmPlacement:
    	case Pass:
    	case Placement:
    		if(placementComplete())
    			{ setState(ProteusState.Play); }
    		else {
    			setState(hasPlacementMoves(whoseTurn) ? ProteusState.Placement: ProteusState.Pass); 
    			}
    		break;
    	}
    }
    private boolean placementComplete()
    {	int tot = 0;
    	for(ProteusCell c = allCells; c!=null; c=c.next)
    	{ int h = c.height();
    	  if(h==0) { return(false); }
    	  tot+= h;
    	}
    	boolean ready = tot==15;
    	if(ready) 
    		{ G.Assert(goal!=Goal.none,"goal is not set");
    		  G.Assert(trade!=Trade.none,"trade is not set");
    		  G.Assert(move!=Move.none,"move is not set");
    		}
    	return(ready);	// all cells occupied and 6 of them stacked.
    }
    private void activate(ProteusChip destTile)
    {
    	Goal tgoal = destTile.goal;
    	if(tgoal!=null) { goal = tgoal; } 
    	Trade ttrade = destTile.trade;
    	if(ttrade!=null) { trade = ttrade; }
    	Move tmove = destTile.move;
    	if(tmove!=null) { move = tmove; }	
    }
    public boolean shapeIsActive(ProteusChip shape)
    {	return((shape!=null) && ((shape.goal==goal) || (shape.trade==trade) || (shape.move==move)));
    }
    //
    // this is where we change the rules of the game based on the activated tile
    //
    private void changeRules(ProteusCell src,ProteusCell dest)
    {	
    	ProteusChip destTile = dest.chipAtIndex(0);
    	boolean destIsActive = (dest.onBoard && (dest.height()==2) && dest.chipAtIndex(1).shape==destTile.shape);
    			
    	if((src!=null) && src.onBoard && (src.height()==2))
		{
		ProteusChip srcTile = src.chipAtIndex(0);
		ProteusChip spiece = src.chipAtIndex(1);
			// special case, if to non-activate tiles would both be activated, then neither is activated
			if(srcTile.shape==spiece.shape)
			{	// activated a source file
				if(destIsActive
						&& ((revision<101) || (srcTile.pieceColor==destTile.pieceColor))
						&& !shapeIsActive(srcTile) 
						&& !shapeIsActive(destTile)
						
						) 
					{ return; }
				activate(srcTile);
			}
		}
    	
    	if(destIsActive) {	activate(destTile);	}
    }
    
    private void doDone(replayMode replay)
    {	
    	ProteusCell dest = getDest();
    	if(dest!=null)
    	{
    	// if this is a trade, we need to know the source too
    	ProteusCell src = (levelStack.top()==0) ? getSource() : null;
    	changeRules(src,dest); 
    	}
        acceptPlacement();
        switch(board_state)
        {
        case Resign:setGameOver(false,true); break;
        case Puzzle: break;
        default:
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(replay); setNextStateAfterDone(); }
        }}
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	ProteusMovespec m = (ProteusMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_PASS:
		case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_FROM_TO:
           		{
    			G.Assert(pickedObject==null,"something is moving");
    			ProteusCell dest = getCell(m.dest,m.to_col,m.to_row);
    			ProteusCell src = getCell(m.source, m.from_col, m.from_row);		
                pickObject(src,-1);
    			m.chip = pickedObject;
    			dropObject(dest,-1); 
                setNextStateAfterDrop(replay);
                if(replay!=replayMode.Replay)
    				{
                	animationStack.push(src);
                	animationStack.push(dest);
    				}
           		}
        	break;
        case MOVE_TRADE:
        	{
    			G.Assert(pickedObject==null,"something is moving");
    			ProteusCell src = getCell(ProteusId.BoardLocation, m.from_col, m.from_row);
    			ProteusCell dest = getCell(ProteusId.BoardLocation,m.to_col,m.to_row);
    			pickObject(src,0);
    			m.chip = pickedObject;
    			dropObject(dest,0);
    			pickObject(dest,1);
    			m.chip2 = pickedObject;
    			dropObject(src,0);
    			if(replay!=replayMode.Replay)
    			{
    				animationStack.push(src);
    				animationStack.push(dest);
    				animationStack.push(dest);
    				animationStack.push(src);
    			}
			    setNextStateAfterDrop(replay);
    			break;
        	}
        case MOVE_DROPB:
			{
			ProteusCell c = getCell(ProteusId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			if(replay==replayMode.Single)
			{
				animationStack.push(getSource());
				animationStack.push(c);
			}
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		ProteusCell src = getSource();
            		boolean isTile =pickedObject.isTile(); 
            		if(isTile && (board_state==ProteusState.Play))
            		{	ProteusChip ch = c.removeChipAtIndex(0);	// do the swap
            			src.insertChipAtIndex(0,ch);
            			dropObject(c,0);
            		}
            		else {
            		dropObject(c,-1);
            		}
            		if(board_state==ProteusState.Puzzle) { doDone(replay); }
            		else { setNextStateAfterDrop(replay); }
            		}
			}
            break;

        case MOVE_PICKT:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ 
        		unDropObject();       		
        		}
        	else 
        		{ pickObject(getCell(ProteusId.BoardLocation, m.from_col, m.from_row),0);
        		 m.chip = pickedObject;
         		}
 	
        	break;
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(ProteusState.Placement);
        		}
        	else 
        		{ pickObject(getCell(ProteusId.BoardLocation, m.from_col, m.from_row),-1);
        		  m.chip = pickedObject;
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	ProteusCell dest = getCell(m.source, m.to_col, m.to_row);
            dropObject(dest,pickedObject.isTile() ? 0 : -1);
            setNextStateAfterDrop(replay);
        	}

            break;

        case MOVE_PICK:
        	{
        	ProteusCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c,-1);
            m.chip = pickedObject;
            setNextStateAfterPick(replay);
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(ProteusState.Puzzle);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	setState(unresign==null?ProteusState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(ProteusState.Puzzle);
 
            break;
            
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }

 
        return (true);
    }
    public boolean legalToHitTiles(ProteusCell c,Hashtable<ProteusCell,ProteusMovespec>targets)
    {
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case ConfirmPlacement:
    		return(isDest(c));
    	case Placement:
    		return(targets.get(c)!=null);
    	case Puzzle: 
    		if(pickedObject==null) { return(c.topChip()!=null); }
    		if(pickedObject.isTile()) { return(c.topChip()==null); }
    		return(false);
       	case Confirm: 
       	case Gameover:
       	case Play:
       	case Pass:
       	case Resign:
       	case Draw:
       		return(false);
    	}
    }
    
    // legal to hit the chip storage area
    public boolean legalToHitChips(int player,ProteusCell c,Hashtable<ProteusCell,ProteusMovespec>targets)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case Confirm:
         case Pass:
        	 return(false);
		 case ConfirmPlacement:
         case Draw:
         case Placement: 
        	return(targets.get(c)!=null);
		case Gameover:
		case Play:
		case Resign:
			return(false);
        case Puzzle:
        	return((pickedObject==null)!=(c.topChip()==null));
        }
    }
  

    public boolean legalToHitBoard(ProteusCell cell,Hashtable<ProteusCell,ProteusMovespec>targets)
    {	
        switch (board_state)
        {
 		case Placement:
 		case Play:
 			if((pickedObject!=null)&&isSource(cell)) { return(true); }
 			return(targets.get(cell)!=null);

		case Gameover:
		case Pass:
		case Resign:
			return(false);
		case Confirm:
		case ConfirmPlacement:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	if(pickedObject==null) { return(cell.topChip()!=null); }
        	if(pickedObject.isTile()) { return(cell.topChip()==null); }
        	return(cell.height()==1);
        }
    }
  public boolean canDropOn(ProteusCell cell)
  {		ProteusCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(ProteusMovespec m)
    {
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("R "+m);
        if (Execute(m,replayMode.Replay))
        {
            if ((m.op == MOVE_DONE)||(m.op==MOVE_PASS))
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
    public void UnExecute(ProteusMovespec m)
    {
        	throw G.Error("Not expecting unexecute");
    }
 private boolean addTilePlacementMoves(CommonMoveStack all,ProteusCell from,ProteusChip top,int who)
 {	boolean some = false;
 	for(ProteusCell to = allCells; to!=null; to=to.next)
 	{	if(to.topChip()==null)
 		{	if(all==null) { return(true); }
 			all.push(new ProteusMovespec(MOVE_FROM_TO,from,to,who));
 			some = true;
 		}
 	}
 	return(some);
 }
 private boolean addTilePlacementMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	for(ProteusCell c : originalTiles)
 	{	ProteusChip top = c.topChip();
 		if(top!=null)
 		{	some |= addTilePlacementMoves(all,c,top,who);
 			if(some&&(all==null)) { return(some); }
 		}
 	}
 	return(some);
 }

 private ChipStack tempDestStack = new ChipStack();
 private ChipStack tempSrcStack = new ChipStack();
 
 // the rule is that each of goal,trade,move has to be activated exactly once
 // at the end of the placement phase.  Any move which makes that impossible is
 // illegal.  Figuring out exactly what can still be done analytically is complicated
 // so we just try all possibilities and return true if we succeed.  This isn't effecient,
 // but the absolute numbers are so small it won't matter.
 //
 private boolean canPlaceAndStillActivate(ChipStack destStack,ChipStack srcStack,
		 	ProteusChip from,ProteusChip target,int acode)
 {	
	 if(target.shape==from.shape)
	 	{	// activation
		 	int bit = 0;
		 	// we expect each target to have one of these three
			if(target.goal!=null) { bit |= 4; }
			if(target.trade!=null) { bit |= 2; }
			if(target.move!=null) { bit |= 1; }
	     	if((bit&acode)!=0) { return(false);} 	// already activated, this is illegal
	     	acode |= bit;
	 	}    
	 	if(srcStack.size()==0) 
	 		{ return(acode==7); 
	 		}
	 	
	   	return(recursivelyPlace(destStack,srcStack,acode));
 }
 // destStack consists of all the tiles that haven't been stuffed with a piece yet
 // srcStack is all the pieces that haven't been placed yet.
 // acode is a bit mask representing which of the 3 variables have been activated 7=all
 //
 private boolean recursivelyPlace(ChipStack destStack,ChipStack srcStack,int acode)
 {	if(srcStack.size()==0) { return(false); }
 	ProteusChip from = srcStack.pop();
 	for(int lim=destStack.size()-1; lim>=0; lim--)
 		{ ProteusChip dest = destStack.remove(lim,true);	// preserve the order of the stack during the descent
 		  boolean val = canPlaceAndStillActivate(destStack,srcStack,from,dest,acode);
 		  destStack.insertElementAt(dest,lim);
 		  if(val) 
 		  	{ srcStack.push(from);
 		  	  return(val); 
 		  	}
 		  }
 	srcStack.push(from);
 	return(false);
 
 }
 
 private boolean addPiecePlacementMoves(CommonMoveStack all,ProteusCell from,ProteusChip top,int who)
 {	boolean some = false;
 	// bit mask representing which variatble features have been activated.
 	//G.print("\np "+from+" "+top+" "+who);
 	int acode = ((move==Move.none) ? 0 : 1)
 				 | ((trade==Trade.none) ? 0 : 2) 
 				 | ((goal==Goal.none) ? 0 : 4);
 	// make a stack of all the unfilled tiles
 	tempDestStack.clear();
  	for(ProteusCell c = allCells; c!=null; c=c.next) { if(c.height()==1) { tempDestStack.push(c.topChip()); }}
 	for(ProteusCell c : originalTiles) { if(c.height()==1) { tempDestStack.push(c.topChip()); }}
 	// make a stack of all the unplaced pieces
 	tempSrcStack.clear();
 	for(ProteusCell c : blackChips) { if(c.height()==1) { tempSrcStack.push(c.topChip()); }}
 	for(ProteusCell c : whiteChips) { if(c.height()==1) { tempSrcStack.push(c.topChip()); }}
 	// make sure the piece we're placing isn't in the stack
 	tempSrcStack.remove(top,false);

 	for(ProteusCell to = allCells; to!=null; to=to.next)
 	{	if(to.height()==1)
 		{
 		// pieces can't be placed if the function is already activated and doing so would activate a new tile.
 		//ProteusChip tile = to.chipAtIndex(0);
 		//if((tile.shape==top.shape) && functionIsActivated(tile)) {}
 		//else 
 		ProteusChip target = to.topChip();
 		int idx = tempDestStack.indexOf(target);
 		tempDestStack.remove(idx,false);
 		boolean canPlace = canPlaceAndStillActivate(tempDestStack,tempSrcStack,top,target,acode);
 		//G.print("Can "+tempDestStack+tempSrcStack+top+" "+target+" "+acode+" = "+canPlace);
		if(canPlace)
 			{
 			if(all==null) { return(true); }
 			some = true;
 			ProteusMovespec m = new ProteusMovespec(MOVE_FROM_TO,from,to,who);
 			all.push(m); 
 			}
 		//tempDestStack.insertElementAt(target,idx);
 		tempDestStack.push(target);
 		}
	}
 	tempSrcStack.push(top);
	
 	return(some);
 }

 private boolean addPiecePlacementMoves( CommonMoveStack all,int who)
 {	boolean some = false;
 	for(ProteusCell c : playerChips[who])
 	{	ProteusChip top = c.topChip();
 		if(top!=null)
 		{	some |= addPiecePlacementMoves(all,c,top,who);
 			if(some && (all==null)) { return(some); } 
 		}
 	}
 	return(some);
 }
 private ProteusMovespec testMove(RepeatedPositions repeats,ProteusMovespec m)
 {	if(repeats!=null)
 	{
	ProteusChip picked = pickedObject;
 	ProteusCell pickedSource = null;
 	ProteusState midState = board_state;
 	int level = 0;
 	Goal oldGoal = goal;
	Trade oldTrade = trade;
	Move oldMove = move;
	ProteusState state = board_state;
	int who = whoseTurn;
	int number = moveNumber;
	long oldDigest = Digest();
	//ProteusBoard clone = cloneBoard();
	if(picked!=null) {  pickedSource = getSource(); level = levelStack.top();  unPickObject(); midState = board_state; }

	Execute(m,replayMode.Replay);
	doDone(replayMode.Replay);
 	long dig = Digest();
 	whoseTurn = who;
 	goal = oldGoal;
 	trade = oldTrade;
 	move = oldMove;
 	moveNumber = number;
 	AR.setValue(win,false);
 	switch(m.op)
 	{
        case MOVE_PASS:
        	break;
        case MOVE_TRADE:
        	{
        		ProteusCell dest = getCell(m.to_col,m.to_row);
           		ProteusCell src = getCell(m.from_col,m.from_row);
           		ProteusChip move = dest.removeChipAtIndex(0);
           		dest.insertChipAtIndex(0,src.removeChipAtIndex(0));
           		src.insertChipAtIndex(0,move);
        	}
        	break;
        case MOVE_FROM_TO:
           		{
           		ProteusCell dest = getCell(m.to_col,m.to_row);
           		ProteusCell src = getCell(m.from_col,m.from_row);
           		src.addChip(dest.removeTop());
           		}
        	break;

       	default: G.Error("not expecting %s",m);
 	}
	
 	if(picked!=null) { board_state = midState; pickObject(pickedSource,level); }
 		
	board_state = state;

	//sameboard(clone);
 	G.Assert(oldDigest==Digest(),"digest changed");
 	if(repeats.numberOfRepeatedPositions(dig)>0)
 	{
 		m = null;
 	}
 	}
	 return(m);
 }
 private boolean addPieceMoves( RepeatedPositions repeats,CommonMoveStack all,ProteusCell from,ProteusChip top,int who)
 {	boolean some = false;
 	if(!top.isTile())
	 {switch(move)
	 {
	 case none:	
	 default: throw G.Error("Not expecting movement style %s",move);
	 
	 case king:
		 for(int direction=0;direction<from.geometry.n;direction++)		// move to an adjacent cell
		 {	ProteusCell dest = from.exitTo(direction);
		 	if((dest!=null) && (dest.height()==1))			// to an empty
		 	{	
		 		ProteusMovespec m = testMove(repeats,new ProteusMovespec(MOVE_FROM_TO,from,dest,who));
		 		if(m!=null) 
		 			{ if(all==null) { return(true); }
		 			  all.push(m); 
		 			}
		 		some = true;
		 	}
		 }
		break;
	 case rook:
		 for(ProteusCell dest = allCells; dest!=null; dest=dest.next)
		 {	if( (dest!=from)
			    && ((dest.row==from.row) || (dest.col==from.col))		// move to the same row or column
			    && (dest.height()==1))									// to an empty
		 	{	ProteusMovespec m = testMove(repeats,new ProteusMovespec(MOVE_FROM_TO,from,dest,who));
		 		if(m!=null)
		 			{
		 			if(all==null) { return(true); }
		 			all.push(m);
		 			some = true;
		 			}
		 	} 
		 }
		 break;
		 
	 case bishop:
		 for(ProteusCell dest = allCells; dest!=null; dest=dest.next)
		 {	if( (dest!=from)
			    && ((dest.row!=from.row) && (dest.col!=from.col))		// move to a different row and column
			    && (dest.height()==1))									// to an empty
		 	{	
			 	ProteusMovespec m = testMove(repeats,new ProteusMovespec(MOVE_FROM_TO,from,dest,who));
			 	if(m!=null)
			 	{
			 	if(all==null) { return(true); }
		 		all.push(m);
		 		some = true;
			 	}
		 	} 
		 }
		 break;
	 }}
 	return(some);
 }
 private void addPieceMoves( RepeatedPositions repeats,CommonMoveStack all,int who)
 {	for(ProteusCell c = allCells; c!=null; c=c.next)
 	{	if(c.height()==2)
 		{	ProteusChip top = c.topChip();
 			if(top.pieceColor==playerColors[who])
 			{	addPieceMoves(repeats,all,c,top,who);
 			}
 		}
 	} 
 }
 private void addTileMoves( RepeatedPositions repeats,CommonMoveStack all,ProteusCell from,ProteusChip moving,int who)
 {	
	if(moving.isTile())
	{
	ProteusChip top = null;
 
 	if(trade==Trade.polarity)
 	{	top = from.topChip();
 		if((top==null)||top.isTile()) { return; }	// no moves from an unfilled tile
 	}
	if(top==null) { top = moving; }
	for(ProteusCell dest = allCells; dest!=null; dest=dest.next)
 	{	if(dest!=from)
 		{	
 			switch(trade)
 			{
 			default:
 			case none: throw G.Error("Not expecting trade condition %s",trade);
 			
 			case color:
	 			{	ProteusChip ch = dest.chipAtIndex(0);
					if(ch.pieceColor==top.pieceColor)
					{	ProteusMovespec m = testMove(repeats,new ProteusMovespec(MOVE_TRADE,from,dest,who));
						if(m!=null) { all.push(m); }
					}
				}
	 				break;
 				
 			case shape:
 				{	ProteusChip ch = dest.chipAtIndex(0);
 					if(ch.shape==top.shape)
 					{
 					ProteusMovespec m = testMove(repeats,new ProteusMovespec(MOVE_TRADE,from,dest,who));
 					if(m!=null) { all.push(m); }
 					}
 				}
 				break;
 				
 			case polarity:
 				{	if(dest.height()==2)
 					{
 					ProteusChip t2 = dest.topChip();
 					if(t2.pieceColor!=top.pieceColor)
 					{
 						ProteusMovespec m = testMove(repeats,new ProteusMovespec(MOVE_TRADE,from,dest,who));
 						if(m!=null) { all.push(m); }
 					}
 					}
 				}
 				break;
 			}
 		}
 	}
 	}
 }
 private void addTileMoves( RepeatedPositions repeats,CommonMoveStack all,int who)
 {	for(ProteusCell c = allCells; c!=null; c=c.next)
 	{	
 		ProteusChip target = c.chipAtIndex(0);
 		addTileMoves(repeats,all,c,target,who);
 	} 
 }
 
 public Hashtable<ProteusCell,ProteusMovespec> getTargets( RepeatedPositions repeats)
 {	return((pickedObject==null) ? getSources(repeats,false) : getDests(repeats));
 }
 
 public Hashtable<ProteusCell,ProteusMovespec> getPieceTargets( RepeatedPositions repeats)
 {	return((pickedObject==null) ? getSources(repeats,true) : getDests(repeats));
 }
 
 public Hashtable<ProteusCell,ProteusMovespec> getDests( RepeatedPositions repeats)
 {	Hashtable<ProteusCell,ProteusMovespec> moves = new Hashtable<ProteusCell,ProteusMovespec>();
	 if(pickedObject!=null)
	 {
		 switch(board_state)
		 {
		 default: throw G.Error("Not expecting state %s",board_state);
		 case Gameover:
		 case Puzzle:
		 case Confirm:
		 case ConfirmPlacement:
			 break;
		 case Placement:
		 case Play:
		 	{	CommonMoveStack all = GetListOfMoves(repeats,getSource(),pickedObject,whoseTurn);
		 		while(all.size()>0)
		 		{
		 			ProteusMovespec m = (ProteusMovespec)all.pop();
 					switch(m.op)
 					{
 					default: break;
 					case MOVE_FROM_TO:
 					case MOVE_TRADE:
 						{	ProteusCell c = getCell(m.dest,m.to_col,m.to_row);
 							moves.put(c,m);
 						}
 					}
		 		}
		 	}
		 }
	 }
	 return(moves);
 }
 
 public Hashtable<ProteusCell,ProteusMovespec> getSources( RepeatedPositions repeats,boolean piecesOnly)
 {	Hashtable<ProteusCell,ProteusMovespec> moves = new Hashtable<ProteusCell,ProteusMovespec>();
 	if(pickedObject==null)
 	{
 	switch(board_state)
 	{	case Puzzle:
 		case Confirm:
 		case Gameover:
 		case Draw:
 		case ConfirmPlacement:
 		case Pass:
 		case Resign:
 			break;
 		default: throw G.Error("Not expecting state %s",board_state);
 		case Play:
 		case Placement:
 			{
 				CommonMoveStack all = GetListOfMoves(repeats);
 				while(all.size()>0)
 				{	ProteusMovespec m = (ProteusMovespec)all.pop();
 					switch(m.op)
 					{
 					default: break;
 					case MOVE_TRADE:
 						if(piecesOnly) { break; }
						//$FALL-THROUGH$
					case MOVE_FROM_TO:
 						{
 						ProteusCell c = getCell(m.source,m.from_col,m.from_row);
 						moves.put(c,m);
 						}
 				}
 			}
 
 	}}}
 	return(moves);
 }
 boolean hasPlacementMoves(int who)
 {
	 return(addTilePlacementMoves(null,who) || addPiecePlacementMoves(null,who));
 }
 CommonMoveStack  GetListOfMoves( RepeatedPositions repeats,ProteusCell from,ProteusChip moving,int who)
 {
	 CommonMoveStack all = new CommonMoveStack();
	 switch(board_state)
	 {
	 default:throw G.Error("Not implemented");
	 case Placement:
		 if(moving.isTile()) { addTilePlacementMoves(all,from,moving,who); }
		 else { addPiecePlacementMoves(all,from,moving,who); }
		 break;
	 case Play:
		 if(moving.isTile()) { addTileMoves(repeats,all,from,moving,who); }
		 else { addPieceMoves(repeats,all,from,moving,who); }
	 }
	 return(all);
 }
 CommonMoveStack  GetListOfMoves( RepeatedPositions repeats)
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default: throw G.Error("Not implemented");
	case ConfirmPlacement:
 	case Confirm:
 		all.push(new ProteusMovespec(MOVE_DONE,whoseTurn)); 
 		break;
 	case Play:
 		addTileMoves(repeats,all,whoseTurn);
 		addPieceMoves(repeats,all,whoseTurn);
 		break;
 	case Placement:
 		addTilePlacementMoves(all,whoseTurn);
 		addPiecePlacementMoves(all,whoseTurn);
 		break;
 	case Pass:
 		all.push(new ProteusMovespec(MOVE_PASS,whoseTurn)); 
 		break;

 	}
 	return(all);
 }
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ xpos -= cellsize/40;
	 	}
 		else
 		{ 
 		  ypos += cellsize/30;
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }
}
