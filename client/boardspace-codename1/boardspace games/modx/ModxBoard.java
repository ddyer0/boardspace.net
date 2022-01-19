package modx;
/**
 * TODO: needs to be more willing to accept draws
 */
import online.game.*;

import static modx.ModxMovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
/**
 * ModxBoard knows all about the game of Modx.
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

class ModxBoard extends rectBoard<ModxCell> implements BoardProtocol,ModxConstants
{	static int REVISION = 101;			// revision numbers start at 100
	public int getMaxRevisionLevel() { return(REVISION); }
	
	
    public int boardColumns;	// size of the board
    public int boardRows;
    public void SetDrawState() {G.Error("Not expected"); }
    public int score[] = null;
    public ModxCell rack[] = null;	// the pool of chips for each player.  
    public ModxCell flat[] = null;	// the pool of flats for each player
    public ModxCell joker = null;	// common pool of neutral pieces
    public int nonJokersPlaced = 0;
    public CellStack animationStack = new CellStack();
    //
    // private variables
    //
    private int sweep_counter = 0;
    private RepeatedPositions repeatedPositions = null;		// shared with the viewer
    private ModxState board_state = ModxState.Play;	// the current board state
    private ModxState unresign = null;					// remembers the previous state when "resign"
    Variation variation = Variation.Modx;
    public ModxState getState() { return(board_state); } 
	public void setState(ModxState st) 
	{ 	unresign = (st==ModxState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    private ModxId playerColor[]={ModxId.Red_Chip_Pool,ModxId.Black_Chip_Pool};
    private ModxChip playerChip[] = { ModxChip.getChip(0),ModxChip.getChip(1) };
    public ModxChip getPlayerChip(int pl)
    {
    	return(playerChip[pl]);
    }
 	public ModxId getPlayerColor(int p) { return(playerColor[p]); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ModxChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack dropState = new StateStack();
    int lastDrawMove = 0;			// last move where a draw was offered
    int robotDepth = 0;		// current depth of robot search.  This is used to make faster wins look better
    						// than slower wins.  It's part of the board so multiple threads have independent values.
  	private StateStack robotState = new StateStack();
  	private IStack robotStack = new IStack();
  	private CellStack captureStack = new CellStack();
  	private ChipStack capturePiece = new ChipStack();
	// factory method
	public ModxCell newcell(char c,int r)
	{	return(new ModxCell(c,r));
	}
    public ModxBoard(String init,long rv,int np,RepeatedPositions rep,int map[],int rev) // default constructor
    {   repeatedPositions = rep;
    	setColorMap(map);
        doInit(init,rv,np,rev); // do the initialization 
     }


	public void sameboard(BoardProtocol f) { sameboard((ModxBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ModxBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(nonJokersPlaced==from_b.nonJokersPlaced, "nonJokersPlaced mismatch");
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(dropState.sameContents(from_b.dropState),"dropState mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(sameCells(rack, from_b.rack), "rack mismatch");
        G.Assert(sameCells(flat, from_b.flat), "flat mismatch");
        G.Assert(sameCells(joker, from_b.joker), "joker mismatch");
        G.Assert(AR.sameArrayContents(score, from_b.score), "score mismatch");
        G.Assert(sameCells(captureStack,from_b.captureStack),"capture stack mismatch");
        G.Assert(capturePiece.sameContents(from_b.capturePiece),"capturepiece mismatch");
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
        long v = super.Digest();
        v ^= Digest(r,nonJokersPlaced);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,flat);
		v ^= Digest(r,rack);
		v ^= Digest(r,joker);
		v ^= Digest(r,score);
		v ^= Digest(r,captureStack);
		v ^= capturePiece.Digest(r);
		v ^= Digest(r,revision);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public ModxBoard cloneBoard() 
	{ ModxBoard copy = new ModxBoard(gametype,randomKey,players_in_game,repeatedPositions,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((ModxBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ModxBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        nonJokersPlaced = from_b.nonJokersPlaced;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        dropState.copyFrom(from_b.dropState);
        AR.copy(playerColor,from_b.playerColor);
        board_state = from_b.board_state;
        lastDrawMove = from_b.lastDrawMove;
        unresign = from_b.unresign;
        repeatedPositions = from_b.repeatedPositions;
        copyFrom(rack,from_b.rack);
        AR.copy(score,from_b.score);
        copyFrom(flat,from_b.flat);
        copyFrom(joker,from_b.joker);
        getCell(captureStack,from_b.captureStack);
        capturePiece.copyFrom(from_b.capturePiece);
       
        sameboard(from_b);
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game,revision);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np,int rev)
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
		adjustRevision(rev);
    	Grid_Style = GRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
    	nonJokersPlaced = 0;
    	players_in_game = np;
		rack = new ModxCell[np];
		flat = new ModxCell[np];
		score = new int[np];
    	Random r = new Random(67246765);
    	captureStack.clear();
    	capturePiece.clear();
		joker = new ModxCell(r,ModxId.Joker_Pool);
		for(int n=0;n<nJokers;n++) { joker.addChip(ModxChip.Joker); }
		int map[]=getColorMap();
    	for(int i=0,pl=0;i<np; i++,pl=nextPlayer[pl])
    	{
     		{ModxCell cell = new ModxCell(r,RackLocation[i]);
       		for(int n=0;n<nChipsPerColor; n++) { cell.addChip(ModxChip.getChip(i)); }
       		rack[map[i]]=cell;
     		}
     		{
     		ModxCell flatCell = new ModxCell(r,FlatLocation[i]);
     		for(int n=0;n<nFlatsPerColor;n++) { flatCell.addChip(ModxChip.getFlat(i)); }
     		flat[map[i]]=flatCell;
     		}
     	
     	}    
     	
     	variation = Variation.findVariation(gtype);
     	switch(variation)
     	{
     	default:  throw G.Error(WrongInitError,gtype);
     	case Modx:
     		boardColumns = variation.size;
     		boardRows = variation.size;
     		reInitBoard(boardColumns,boardRows);
      		gametype = gtype;
     		break;
     	}

	    setState(ModxState.Puzzle);
	    

	    switch(variation)
	    {
	    default: break;

	    }
	    lastDrawMove = 0;
	    robotDepth = 0;
	    robotState.clear();
	    robotStack.clear();
	    whoseTurn = FIRST_PLAYER_INDEX;
		playerColor[map[FIRST_PLAYER_INDEX]]=ModxId.Red_Chip_Pool;
		playerColor[map[SECOND_PLAYER_INDEX]]=ModxId.Black_Chip_Pool;
		playerChip[map[FIRST_PLAYER_INDEX]]=ModxChip.getChip(FIRST_PLAYER_INDEX);
		playerChip[map[SECOND_PLAYER_INDEX]]=ModxChip.getChip(SECOND_PLAYER_INDEX);
		acceptPlacement();
        AR.setValue(win,false);
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    public double simpleScore(int who)
    {	// range is 0.0 to 0.8
    	return((score[who]-score[nextPlayer[who]])/40.0);
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
        case Puzzle:
            break;
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
    {	
        switch (board_state)
        {case Resign:
         case Confirm:

            return (true);

        default:
            return (false);
        }
    }
    

    //
    // declare the game over, and the winner and loser
    //
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(ModxState.Gameover);
    }
    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	// we maintain the wins in doDone so no logic is needed here.
    	if(board_state.GameOver()) { return(win[player]); }
    	return(false);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=simpleScore(player);
    	
    	return(finalv);
    }


    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        droppedDestStack.clear();
        dropState.clear();
        pickedSourceStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	ModxCell dr = droppedDestStack.pop();
    	setState(dropState.pop());
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation:
				ModxChip po = pickedObject = dr.removeTop();
				if(!ModxChip.isJoker(po)) { nonJokersPlaced--; }
				break;
			case Joker_Pool:
			case Red_Chip_Pool:
			case Black_Chip_Pool:
			case Yellow_Chip_Pool:
			case Orange_Chip_Pool:
			case Red_Flat_Pool:
			case Black_Flat_Pool:
			case Yellow_Flat_Pool:
			case Orange_Flat_Pool:	
				pickedObject = dr.removeTop();
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	ModxChip po = pickedObject;
    	if(po!=null)
    	{
    		ModxCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case BoardLocation: 
    			if(!ModxChip.isJoker(po)) { nonJokersPlaced++; }
    			ps.addChip(po);
    			break;
    		case Joker_Pool:
    		case Red_Chip_Pool:
    		case Black_Chip_Pool:
    		case Yellow_Chip_Pool:
    		case Orange_Chip_Pool:
    		case Red_Flat_Pool:
    		case Black_Flat_Pool:
    		case Yellow_Flat_Pool:
    		case Orange_Flat_Pool:
    			ps.addChip(po);
    			break;	// don't add back to the pool
    		}
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(ModxCell c)
    {   ModxChip po = pickedObject;
    	G.Assert(po!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation:
			if(!ModxChip.isJoker(po)) { nonJokersPlaced++; }
			c.addChip(po);
			break;
		case Joker_Pool:
		case Red_Chip_Pool:
		case Black_Chip_Pool:
		case Yellow_Chip_Pool:
		case Orange_Chip_Pool:
		case Red_Flat_Pool:
		case Black_Flat_Pool:
		case Yellow_Flat_Pool:
		case Orange_Flat_Pool:
			c.addChip(po);
			break;	// don't add back to the pool
		}
    	dropState.push(board_state);
       	droppedDestStack.push(c);
       	pickedObject = null;
    }
    
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(ModxCell c)
    {	ModxChip po = pickedObject;
    	G.Assert(po==null,"pickedObject should be null");
    	G.Assert(!c.isEmpty(),"should have a chip");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			po = pickedObject = c.removeTop();
			if(!ModxChip.isJoker(po)) { nonJokersPlaced--; }
			break;
		case Joker_Pool:
		case Red_Chip_Pool:
		case Black_Chip_Pool:	
		case Yellow_Chip_Pool:
		case Orange_Chip_Pool:
		case Red_Flat_Pool:
		case Black_Flat_Pool:	
		case Yellow_Flat_Pool:
		case Orange_Flat_Pool:
			pickedObject = c.removeTop();
			break;	
    	
    	}
    	pickedSourceStack.push(c);
   }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ModxCell cell)
    {	return(droppedDestStack.top()==cell);
    }
    //
    // get the last dropped dest cell
    //
    public ModxCell getDest() 
    { return(droppedDestStack.top()); 
    }

	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  Returns +100 if a king is the moving object.
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	ModxChip ch = pickedObject;
    	if(ch!=null)
    		{ int nn = ch.chipNumber();
    		  return(nn);
    		}
        return (NothingMoving);
    }
    
    public ModxCell getCell(ModxId source)
    {	return(getCell(source,'@',0));
    }
    // get a cell from a partucular source
    public ModxCell getCell(ModxId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Joker_Pool:
        	return(joker);
        case Red_Chip_Pool:
        case Black_Chip_Pool:
        case Yellow_Chip_Pool:
        case Orange_Chip_Pool:
        	return(rack[getColorMap()[source.colorIndex]]);
        case Red_Flat_Pool:
        case Black_Flat_Pool:
        case Yellow_Flat_Pool:
        case Orange_Flat_Pool:
       		return(flat[getColorMap()[source.colorIndex]]);
        }
    }
    //
    // get the local cell which is the same role as c, which might be on
    // another instance of the board
    public ModxCell getCell(ModxCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
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
        case PlaceInitialJoker:
        case ReplaceJoker:
        	if(joker.height()==0) { setState(ModxState.Confirm); }
        	break;
        case Confirm:
        	setNextStateAfterDone(); 
        	break;
        case Play:
 			setState(ModxState.Confirm);
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
    public boolean isSource(ModxCell c)
    {	return(getSource()==c);
    }
    public ModxCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    
    // the second source is the previous stage of a multiple jump
    public boolean isSecondSource(ModxCell cell)
    {	int h =pickedSourceStack.size();
    	return((h>=2) && (cell==pickedSourceStack.elementAt(h-2)));
    }

    //
    // we don't need any special state changes for picks.
    //
    private void setNextStateAfterPick()
    {
    }
    // set gameover and winner according to the current score
    private void setGameOver()
    {
    	setGameOver(score[whoseTurn]>score[nextPlayer[whoseTurn]],
				score[whoseTurn]<score[nextPlayer[whoseTurn]]);	
    }
    // either player is out of exes or flats
    private boolean outOfMarkers()
    {	return((rack[0].height()==0)
    		|| (rack[1].height()==0)
    		|| (flat[0].height()==0)
    		|| (flat[1].height()==0));
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{

    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;

       	case Puzzle:
       		if((nonJokersPlaced==0)&&(joker.height()>0)) 
       			{	setState(ModxState.PlaceInitialJoker);
       				break;
       			}
			//$FALL-THROUGH$
		case Confirm:
		case Play:
			// nextPlayer has advanced, so the player that just played
			// 
			if(outOfMarkers())
			{	setGameOver();
			}
			else {
				setState(joker.height()==0 ? ModxState.Play : ModxState.ReplaceJoker);
			}
     		break;
    	}

    }
    private int playerIndex(ModxChip ch) 
    { 	int color = ch.colorIndex();
    	return(color>=0? getColorMap()[color] : color);
    }
    
    private void scoreCellsOriginal(ModxChip chip,int sweep,replayMode replay)
    {	int index = playerIndex(chip);
    	for(ModxCell c = allCells; c!=null; c=c.next)
    	{
    	if(c.sweep_counter==sweep)
    	{	ModxChip top = c.removeTop();
    		captureStack.push(c);		// remember what was removed
    		capturePiece.push(top);
    		if(top==ModxChip.Joker)
    			{ joker.addChip(top); 
    			  if(replay!=replayMode.Replay)
    			  {	animationStack.push(c);
  			    	animationStack.push(joker);
    			  }
    			}
    		else if(top==chip)
    			{ ModxChip cover = c.topChip();
    			  rack[index].addChip(top);
    			  boolean noAction = false;
    			  // stack multiple flats on a square
    			  if(cover!=null)
    			  {	if(cover.isFlat())
    			  	{	int coveredIndex = playerIndex(cover);
    			  		if(coveredIndex==index) { noAction=true; }
    			  		else {
    			  		score[coveredIndex]--;	// discount the old
    			  		}
    			  	}  
    			  }
    			  // score and top with one of the player's flats.
    			  if(!noAction)
    			  {
    			  score[index]++;
    			  ModxCell from = flat[index];
    			  if(from.height()>0)
    			  {	  ModxChip flat = from.removeTop();
    			  	  captureStack.push(c);
    			  	  capturePiece.push(flat);
    				  c.addChip(flat);
    			  if(replay!=replayMode.Replay)
    			  {	animationStack.push(c);
    			    animationStack.push(rack[index]);
    			  }
    			  }}
    			}
    		else { G.Error("wrong color chip"); }
    	}
    	}	
    }
    
    private void scoreCellsRevised(ModxChip chip,int sweep,replayMode replay)
    {	int index = playerIndex(chip);
    	ModxCell flats = flat[index];
    	for(ModxCell c = allCells; c!=null; c=c.next)
    	{
    	if(c.sweep_counter==sweep)
    	{	int h = c.height();
    		ModxChip top = c.chipAtIndex(h-1);
    		
    		// remove jokers, no effect on score
    		if(top==ModxChip.Joker)
    			{
    			captureStack.push(c);
    			capturePiece.push(top);
    			joker.addChip(c.removeTop());
    			if(replay!=replayMode.Replay)
    			{	animationStack.push(c);
    				animationStack.push(joker); 
    			}
    			c.sweep_counter = -1;
    			}
    		else
    		if((flats.height()>0)
    			&& (h>1) 
    			&& (top==chip))
    		{
    		ModxChip cover = c.chipAtIndex(h-2);
    		int coveredIndex = playerIndex(cover);
    		if(cover.isFlat() && (coveredIndex!=index))
    		{	// it's a flat that changes the score, do it first
    			captureStack.push(c);		// remember what was removed
        		capturePiece.push(top);
        		rack[index].addChip(c.removeTop());
        		
        		captureStack.push(c);
		  		ModxChip p = flats.removeTop();
		  		capturePiece.push(p);
		  		c.addChip(p);
	
	      		score[coveredIndex]--;	// discount the old
			  	score[index]++;

		  		c.sweep_counter = -1;	//unmatch 
			  		
        		if(replay!=replayMode.Replay)
    			{
        		animationStack.push(flats);
        		animationStack.push(c);
    			}
    		}}
    	}}
    	// now run the original code to pick up the rest
    	for(ModxCell c = allCells; c!=null && (flats.height()>0); c=c.next)
    	{
    	if(c.sweep_counter==sweep)
    	{	ModxChip top = c.removeTop();
    		captureStack.push(c);		// remember what was removed
    		capturePiece.push(top);
    		rack[index].addChip(top);
    		if(top==chip)
    			{ // flats that score have already been accounted for
    			  ModxChip below = c.topChip();
    			  if(below==null)
    			  {
    			  score[index]++;
    			  ModxChip p = flats.removeTop();
    			  c.addChip(p);
    			  captureStack.push(c);
    			  capturePiece.push(p);
    			  if(replay!=replayMode.Replay)
    			  {	animationStack.push(c);
    			    animationStack.push(rack[index]);
    			    animationStack.push(flats);
    			    animationStack.push(c);
    			  }
    			 }}
    		else { G.Error("wrong color chip"); }
    	}}
    }
    
    private void removeCaptures(ModxChip chip,int sweep,replayMode replay)
    {	
    	if(revision<101)
    	{
    		scoreCellsOriginal(chip,sweep,replay);
    	}
    	else { scoreCellsRevised(chip,sweep,replay);
    	}
    }
    private void undoCaptures(int level)
    {
    	while(captureStack.size()>level)
    	{
    		ModxCell p = captureStack.pop();
    		ModxChip v = capturePiece.pop();
    		ModxCell from = getCell(v.id);
    		if(v.isFlat())
    		{	// flats were moved from the reservoir to the board
    			from.addChip(p.removeTop());
    			score[playerIndex(v)]--;
    			ModxChip oldv = p.topChip();
    			if(oldv!=null)
    			{
    				score[playerIndex(oldv)]++;
    			}
    		}
    		else 
    		{
    		// x's were move from the board to the reservoir
    		from.removeTop();
    		p.addChip(v);
    		}
    	}
    }
    private boolean doCaptures(replayMode replay)
    {	ModxChip chip = playerChip[whoseTurn];
    	int sweep = ++sweep_counter;
    	boolean hasCaptures = markScoringPatterns(chip,sweep,null);
    	if(hasCaptures)
    	{	
       		if(hasScoringPattern(ModxChip.Joker,null))
   			{
   			setGameOver(true,false);
   			}
       		else
       		{
    		removeCaptures(chip,sweep,replay);
    		if((score[whoseTurn]>=15) || (flat[whoseTurn].height()==0))
    		{
    			setGameOver();
       		}}
    	}
    	return(hasCaptures);
    }
    private void doDone(replayMode replay)
    {	
   	
     	acceptPlacement();
    	
     	doCaptures(replay);		// might be end of game if joker pattern
     	switch(board_state)
     	{
     	case Resign:
     		setGameOver(false,true);
     		break;
     	case Gameover: break;
     	default:
     		if(joker.height()==0) { setNextPlayer(); } 
        	setNextStateAfterDone();
     	}
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	ModxMovespec m = (ModxMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;

 
        case MOVE_RACK_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        		case PlaceInitialJoker:
        		case ReplaceJoker:
        			G.Assert(pickedObject==null,"something is moving");
        			ModxCell src = getCell(m.source);
        			ModxCell dest = getCell(ModxId.BoardLocation,m.col,m.row);
        			pickObject(src);
        			dropObject(dest); 
        			if(replay!=replayMode.Replay)
        			{
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
        			setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			ModxCell c = getCell(ModxId.BoardLocation, m.col, m.row);
        	G.Assert(pickedObject!=null,"something is moving");
			if(isSecondSource(c))
			{
				unPickObject();
				unDropObject();
				unPickObject();
			}
			else if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		setNextStateAfterDrop();
            		if(replay==replayMode.Single)
            			{
            			animationStack.push(getSource());
            			animationStack.push(c);
            			}
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	ModxCell c = getCell(m.col,m.row);
        	if(isDest(c))
        		{ unDropObject(); 
        		}
        	else 
        		{ pickObject(c);        		  
         		}
        	}
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	ModxCell c = getCell(m.source, m.col, m.row);
            dropObject(c);
            setNextStateAfterDrop();
            if(replay==replayMode.Single)
			{
			animationStack.push(getSource());
			animationStack.push(c);
			}}
            break;


        case MOVE_PICK:
        	{
        	ModxCell c = getCell(m.source);
            pickObject(c);
            setNextStateAfterPick();
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(ModxState.Puzzle);
            {	boolean win1 = winForPlayerNow(whoseTurn);
            	boolean win2 = winForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;
        case MOVE_RESIGN:
        	setState(unresign==null?ModxState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            // standardize "gameover" is not true
            setState(ModxState.Puzzle);
 
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
    public boolean legalToHitChips(ModxCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PlaceInitialJoker:
        case ReplaceJoker:
        	return(c == joker);

        case Confirm:
        	return(false);
        case Play: 
        	return (pickedObject==null 
        				? (c.rackLocation()==RackLocation[getColorMap()[whoseTurn]])
        				: isSource(c));
		case Gameover:
		case Resign:
			return(false);
        case Puzzle:
        	return((pickedObject==null)?true:(c.row==playerIndex(pickedObject)));
        }
    }
  

    public boolean legalToHitBoard(ModxCell cell,Hashtable<ModxCell,ModxMovespec>targets)
    {	
        switch (board_state)
        {
        case PlaceInitialJoker:
        case ReplaceJoker:
 		case Play:
 			return(isSource(cell)||targets.get(cell)!=null);
 		case Resign:
		case Gameover:
			return(false);
		case Confirm:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedObject==null) 
        			? !cell.isEmpty()
        			: !ModxChip.isXorJoker(cell.topChip());
        }
    }
   
   private boolean hasHLine(ModxCell center,ModxChip forchip,ModxCell filled)
   {	boolean prelim = (forchip.matches(center,filled)
		   					&& center.hasTwo(forchip,CELL_LEFT,filled)
		   					&& center.hasTwo(forchip,CELL_RIGHT,filled));
   		// ModxChip.Blind matches jokers only, which means "no match" when we ask for it here.
   		return((forchip==ModxChip.Blind)
   					? prelim ? false : hasHLine(center,ModxChip.getChip(whoseTurn),filled)
   					: prelim);
   }
   
   private boolean hasVLine(ModxCell center,ModxChip forchip,ModxCell filled)
   {	boolean prelim = forchip.matches(center,filled)
		   	&& center.hasTwo(forchip,CELL_UP,filled)
		    && center.hasTwo(forchip, CELL_DOWN,filled);
		// ModxChip.Blind matches jokers only, which means "no match" when we ask for it here.
   		return((forchip==ModxChip.Blind)
				? prelim ? false : hasVLine(center,ModxChip.getChip(whoseTurn),filled)
				: prelim);
   }
   
   private boolean hasDUpLine(ModxCell center,ModxChip forchip,ModxCell filled)
   {	boolean prelim = forchip.matches(center,filled)
		   	&& center.hasTwo(forchip,CELL_UP_LEFT,filled)
		    && center.hasTwo(forchip, CELL_DOWN_RIGHT,filled);
		// ModxChip.Blind matches jokers only, which means "no match" when we ask for it here.
   		return((forchip==ModxChip.Blind)
			? prelim ? false : hasDUpLine(center,ModxChip.getChip(whoseTurn),filled)
			: prelim);
   }
   private boolean hasDDownLine(ModxCell center,ModxChip forchip,ModxCell filled)
   {	boolean prelim = forchip.matches(center,filled)
		   	&& center.hasTwo(forchip,CELL_UP_RIGHT,filled)
		    && center.hasTwo(forchip, CELL_DOWN_LEFT,filled);
		// ModxChip.Blind matches jokers only, which means "no match" when we ask for it here.
   		return((forchip==ModxChip.Blind)
			? prelim ? false : hasDDownLine(center,ModxChip.getChip(whoseTurn),filled)
			: prelim);
   }
  
   private boolean hasPlus(ModxCell center,ModxChip forchip,ModxCell filled)
   {	boolean prelim = forchip.matches(center,filled)
		   && center.hasOne(forchip,CELL_UP,filled)
		   && center.hasOne(forchip,CELL_DOWN,filled)
		   && center.hasOne(forchip, CELL_LEFT,filled)
		   && center.hasOne(forchip, CELL_RIGHT,filled);
	// ModxChip.Blind matches jokers only, which means "no match" when we ask for it here.
		return((forchip==ModxChip.Blind)
		? prelim ? false : hasPlus(center,ModxChip.getChip(whoseTurn),filled)
		: prelim);
   }
   private boolean hasX(ModxCell center,ModxChip forchip,ModxCell filled)
   {	boolean prelim = (forchip.matches(center,filled))
		   && center.hasOne(forchip,CELL_UP_LEFT,filled)
		   && center.hasOne(forchip,CELL_DOWN_LEFT,filled)
		   && center.hasOne(forchip, CELL_UP_RIGHT,filled)
		   && center.hasOne(forchip, CELL_DOWN_RIGHT,filled);
	// ModxChip.Blind matches jokers only, which means "no match" when we ask for it here.
	return((forchip==ModxChip.Blind)
			? prelim ? false : hasX(center,ModxChip.getChip(whoseTurn),filled)
			: prelim);
  
   }
   
   private boolean markHLine(ModxCell center,ModxChip forchip,int sweep,ModxCell filled)
   {	if(hasHLine(center,forchip,filled))
	   	{	center.sweep_counter = sweep;
	   		center.markTwo(sweep,CELL_LEFT);
	   		center.markTwo(sweep,CELL_RIGHT);
	   		return(true);
	   	}
   		return(false);
   }
   

   private boolean markVLine(ModxCell center,ModxChip forchip,int sweep,ModxCell filled)
   {	if(hasVLine(center,forchip,filled))
	   	{	center.sweep_counter = sweep;
	   		center.markTwo(sweep,CELL_UP);
	   		center.markTwo(sweep, CELL_DOWN);
	   		return(true);
	   	}
   	return(false);
   }
   
   private boolean markDUpLine(ModxCell center,ModxChip forchip,int sweep,ModxCell filled)
   {	if(hasDUpLine(center,forchip,filled))
	   	{	center.sweep_counter = sweep;
	   		center.markTwo(sweep,CELL_UP_LEFT);
	   		center.markTwo(sweep, CELL_DOWN_RIGHT);
	   		return(true);
	   	}
   	return(false);
   }
   private boolean markDDownLine(ModxCell center,ModxChip forchip,int sweep,ModxCell filled)
   {	if(hasDDownLine(center,forchip,filled))
	   	{	center.sweep_counter = sweep;
	   		center.markTwo(sweep,CELL_UP_RIGHT);
	   		center.markTwo(sweep, CELL_DOWN_LEFT);
	   		return(true);
	   	}
   	return(false);
   }
   private boolean markPlus(ModxCell center,ModxChip forchip,int sweep,ModxCell filled)
   {	if(hasPlus(center,forchip,filled))
	   	{	center.sweep_counter = sweep;
	   		center.markOne(sweep, CELL_UP);
	   		center.markOne(sweep, CELL_DOWN);
	   		center.markOne(sweep, CELL_LEFT);
	   		center.markOne(sweep, CELL_RIGHT);
	   		return(true);
	   	}
   	return(false);
   }
   
   private boolean markX(ModxCell center,ModxChip forchip,int sweep,ModxCell filled)
   {	if(hasX(center,forchip,filled))
	   {	center.sweep_counter = sweep;
	   		center.markOne(sweep,CELL_UP_LEFT);
	   		center.markOne(sweep,CELL_DOWN_LEFT);
	   		center.markOne(sweep, CELL_UP_RIGHT);
	   		center.markOne(sweep, CELL_DOWN_RIGHT);
	   		return(true);
	   }
   		return(false);
   }
   private boolean hasScoringPattern(ModxChip forChip,ModxCell filled)
   {	for(ModxCell c = allCells; c!=null; c=c.next)
   		{
	   	if(hasScoringPattern(c,forChip,filled)) { return(true); }
   		}   
   		return(false);
   }
   private boolean hasScoringPattern(ModxCell center,ModxChip forchip,ModxCell filled)
   {	return(hasHLine(center,forchip,filled)
		   || hasVLine(center,forchip,filled)
		   || hasDUpLine(center,forchip,filled)
		   || hasDDownLine(center,forchip,filled)
		   || hasPlus(center,forchip,filled)
		   || hasX(center,forchip,filled));
   }
   
   private boolean markScoringPattern(ModxCell center,ModxChip forchip,int sweep,ModxCell filled)
   {	
   		// note using | rather than || so they are all evaluated!
   		return( markHLine(center,forchip,sweep,filled)
			   | markVLine(center,forchip,sweep,filled)
			   | markDUpLine(center,forchip,sweep,filled)
			   | markDDownLine(center,forchip,sweep,filled)
			   | markX(center,forchip,sweep,filled)
			   | markPlus(center,forchip,sweep,filled));
   }
   
   private boolean markScoringPatterns(ModxChip forchip,int sweep,ModxCell filled )
   {	
   		boolean some = false;
   		for(ModxCell c = allCells; c!=null; c=c.next)
   			{
   			some |= markScoringPattern(c,forchip,sweep,filled);
   			}
   		return(some);
   }

 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(ModxMovespec m)
    {
        //G.print("R "+m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        robotState.push(board_state);
        robotStack.push(captureStack.size());
        robotDepth++;
        if (Execute(m,replayMode.Replay))
        {	acceptPlacement();
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	if((robotDepth<=6) && (repeatedPositions.numberOfRepeatedPositions(Digest())>1))
            		{
            		// this check makes game end by repetition explicitly visible to the robot
            		setGameOver(false,false);
            		}
            //else { doDone(replayMode.Replay); }
            }
        }
    }
 

   //
   // un-execute a move.  The move should only be un-executed
   // in proper sequence, and if it was executed by the robot in the first place.
   // If you use monte carlo bots with the "blitz" option this will never be called.
   //
    public void UnExecute(ModxMovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);
    	robotDepth--;
    	undoCaptures(robotStack.pop()); 
        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
        case MOVE_DONE:
            break;
        case MOVE_RACK_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
         		case Play:
         		case PlaceInitialJoker:
         		case ReplaceJoker:
         			{
        			G.Assert(pickedObject==null,"something is moving");
        			ModxCell dest = getCell(m.col,m.row);
        			pickObject(dest);
        			ModxCell from = getCell(m.source);
       			    dropObject(from); 
       			    acceptPlacement();
         			}
        			break;
        	}
        	break;

        case MOVE_RESIGN:
        case MOVE_ACCEPT_DRAW:
        case MOVE_DECLINE_DRAW:
        case MOVE_OFFER_DRAW:
            break;
        }
  }

private void loadHash(CommonMoveStack all,Hashtable<ModxCell,ModxMovespec>hash,boolean from)
{
	for(int lim=all.size()-1; lim>=0; lim--)
	{
		ModxMovespec m = (ModxMovespec)all.elementAt(lim);
		switch(m.op)
		{
		default: break;
		case MOVE_RACK_BOARD:
			if(from) { hash.put(getCell(m.source),m); }
			else { hash.put(getCell(m.col,m.row),m); }
		}
		}
}
/**
 * getTargets() is called from the user interface to get a hashtable of 
 * cells which the mouse can legally hit.
 * 
 * Modx uses the move generator for most of the logic of where it's legal
 * for the mouse to pick up or drop something.  We start with the list of legal
 * moves, and select either the legal "from" spaces, or the legal "to" spaces.
 * 
 * The advantage of this approach is that the logic for "legal moves" whatever it
 * may be, is needed anyway to drive the robot, and by reusing the move list we
 * avoid having to duplicate that logic.
 * 
 * @return
 */
public Hashtable<ModxCell,ModxMovespec>getTargets()
{
	Hashtable<ModxCell,ModxMovespec>hash = new Hashtable<ModxCell,ModxMovespec>();
	CommonMoveStack all = new CommonMoveStack();

		switch(board_state)
		{
		default: break;
		case PlaceInitialJoker:
		case ReplaceJoker:
		case Play:
			{	addMoves(all,whoseTurn);
				loadHash(all,hash,false);
			}
			break;
		}
	return(hash);
}


public boolean hasMoves()
{
	return(addMoves(null,whoseTurn));
}



 // add normal Modx moves
 // "all" can be null
 // return true if there are any.
 public boolean addSimpleMoves(CommonMoveStack all,int who)
 {	return(addSimpleMoves(all,rack[who],who));
 }
 public boolean addSimpleMoves(CommonMoveStack all,ModxCell source,int who)
 { 
	boolean some = false;
 	for(ModxCell c = allCells; c!=null; c=c.next)
 	{	ModxChip chip = c.topChip();
 		if(!ModxChip.isXorJoker(chip))
 		{	if(all==null) { return(true); }
 			all.push(new ModxMovespec(MOVE_RACK_BOARD,source.rackLocation(),c,who));
 			some = true;
 		}
 	}
 	return(some);
 }
 
 

 // add normal Modx moves
 // "all" can be null
 // return true if there are any.
 public boolean addReplacementJokerMoves(CommonMoveStack all,int who)
 {	return(addReplacementJokerMoves(all,joker,who));
 }
 public boolean addReplacementJokerMoves(CommonMoveStack all,ModxCell source,int who)
 { 
	boolean some = false;
 	for(ModxCell c = allCells; c!=null; c=c.next)
 	{	ModxChip chip = c.topChip();
 		// ModxChip.Blind is a special matcher that is blind to 
 		// patterns containing only jokers.  This allows the instant
 		// win with 5 jokers to be constructed.
 		if(!ModxChip.isXorJoker(chip) && !hasScoringPattern(ModxChip.Blind,c))
 		{	if(all==null) { return(true); }
 			all.push(new ModxMovespec(MOVE_RACK_BOARD,source.rackLocation(),c,who));
 			some = true;
 		}
 	}
 	return(some);
 }
 
 
// place initial jokers
public boolean addInitialJokerMoves(CommonMoveStack all,int who)
{	boolean some = false;
	for(ModxCell c = allCells;c!=null; c=c.next)
	{
	ModxChip top = c.topChip();
	if(top==null) 
	{	boolean bad = false;
		for(int direction = c.geometry.n-1; direction>=0 && !bad; direction--)
		{	ModxCell adj = c.exitTo(direction);
			bad |= adj!=null && (ModxChip.isJoker(adj.topChip()));
		}
		if(!bad)
		{	if(all==null) { return(true); }
			all.push(new ModxMovespec(MOVE_RACK_BOARD,ModxId.Joker_Pool,c,who));
			some = true;
		}
	}}
	return(some);
}

 
 private boolean addDone(CommonMoveStack all,int who)
 {	if(all!=null) { all.push(new ModxMovespec(MOVE_DONE,who)); }
 	return(true);
 }

 public boolean addMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	switch(variation)
	 {
	 default: throw G.Error("Not expecting %s",variation); 
	 case Modx:
 		 // captures are mandatory
 		 switch(board_state)
 		 {
 		 default: throw G.Error("Not expecting state %s",board_state);
 		 case Confirm:
 			 some = addDone(all,whoseTurn);
 			 break;
 		 case ReplaceJoker:
 			 some = addReplacementJokerMoves(all,whoseTurn);
 			 break;
 		 case PlaceInitialJoker:
 			 some = addInitialJokerMoves(all,whoseTurn);
 			 break;
 		 case Play:
 			 if(pickedObject==null)
 			 {
 			 some = addSimpleMoves(all,whoseTurn); 
 			 }
 			 else
 			 {	// something is already moving
 			 some = addSimpleMoves(all,getSource(),whoseTurn()); 
 			 }
 			 

 			 break;

 		 }
	 }
 	return(some);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	addMoves(all,whoseTurn);
 	return(all);
 }
 

}
