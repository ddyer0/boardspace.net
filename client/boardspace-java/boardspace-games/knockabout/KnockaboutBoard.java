package knockabout;

import java.awt.Color;

import lib.*;
import lib.Random;
import online.game.*;
import java.util.*;


/**
 * KnockaboutBoard knows all about the game of Knockabout, which is played
 * on a 7-per-side hexagonal board. It gets a lot of logistic support from hexboard,
 * which know about the coordinate system.  
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

class KnockaboutBoard extends hexBoard<KnockaboutCell> implements BoardProtocol,KnockaboutConstants
{
	private KnockaboutState unresign;
	private KnockaboutState board_state;
	public KnockaboutState getState() {return(board_state); }
	public CellStack animationStack = new CellStack();
	public void setState(KnockaboutState st) 
	{ 	unresign = (st==KnockaboutState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	Random randomSeq = null;
	public int nextRandom() 
		{ int rv1 = randomSeq.nextInt();
		  int rv2 = randomSeq.nextInt();
		  int v = (whoseTurn==0)?rv1:rv2;
		  if(v==Integer.MIN_VALUE) { v = 0; }
		  else if(v<0) { v = -v; };
		  // always generate a pair, and give one depending on whose turn it is.
		  // this assures that the players don't have correlated randomness because
		  // they're using the same random numbers.
		  return(v); }
    public int chipsInGutter[] = new int[NUMPLAYERS];
    // factory method used to construct the cells of the board
 	public KnockaboutCell newcell(char c,int r)
	{	return(new KnockaboutCell(c,r,cell.Geometry.Hex));
	}

   
	// the score is the sum of the heights of controlled stacks
    public double ScoreForPlayer(int who,boolean print,boolean dumbot)
    {  	double sc = 0.0;
    	int diceup = 0;
    	int dicecount = 0;
    	for(KnockaboutCell c = allCells;
    		c != null;
    		c = c.next)
    	{
    		KnockaboutChip ch = c.topChip();
    		if((ch!=null) && (ch.color==playerColor[who]))
    		{	if(!c.inGutter)
    			{dicecount++;
     			}
    			diceup += ch.numberShowing;
     		}
    	}
    	sc = dicecount*20+diceup+dicecount*2;
    	if(print) {
    		System.out.println("dice: "+dicecount+":"+diceup+"/"+dicecount+"="+sc);	
    		}
     	return(sc);
    }
    

    public void SetDrawState() { setState(KnockaboutState.DRAW_STATE); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public KnockaboutChip pickedObject = null;			// the object in the air
    private KnockaboutCell pickedSourceStack[]=new KnockaboutCell[20];
    private KnockaboutCell droppedDestStack[]=new KnockaboutCell[20];
    KnockId playerColor[] = new KnockId[2];
    public KnockaboutCell rollCell = null;				// cell that needs reroll
    private int rollValue = 0;
    private int stackIndex = 0;
    private CellStack undoStack = new CellStack();
    private KnockaboutCell popUndoCell()
    	{ KnockaboutCell e = undoStack.top();
    	  undoStack.remove(undoStack.size()-1,false) ;
    	  return(e);
    	}
    public KnockaboutCell getSource() { return(pickedSourceStack[stackIndex]); }
    public boolean isSource(KnockaboutCell c)
    {	return((pickedObject!=null) 
    			?(c==pickedSourceStack[stackIndex])
    			: (stackIndex>0)&&(pickedSourceStack[0]==c));
    }
    public int movingObjectIndex()
    {	KnockaboutChip ch = pickedObject;
    	if((ch!=null)&&(droppedDestStack[stackIndex]==null))
    		{ return(ch.pieceNumber());
    		}
        return (NothingMoving);
    }

    private void finalizePlacement()	// finalize placement, no more undo
    { 	if(stackIndex>=pickedSourceStack.length) { stackIndex--; }
    	while(stackIndex>=0) 
    		{if(stackIndex<pickedSourceStack.length)
    			{ pickedSourceStack[stackIndex]=droppedDestStack[stackIndex] = null;
    			}
    		 stackIndex--;
    		}
    	stackIndex = 0;
     }   
    private int playerIndex(KnockaboutChip ch)
    {
    	return(ch.color==playerColor[0] ? 0 : 1);
    }
    // use these exclusively to the tallies of chips in the gutter are maintained 
    private void addChip(KnockaboutCell ps,KnockaboutChip po)
    {  	ps.addChip(po);
    	if(ps.inGutter) { chipsInGutter[playerIndex(po)]++; }
    }
    // use these exclusively to the tallies of chips in the gutter are maintained 
    private KnockaboutChip removeChip(KnockaboutCell ps)
    {  	KnockaboutChip po = ps.removeChip();
    	if(ps.inGutter && (po!=null)) { chipsInGutter[playerIndex(po)]--; }
    	return(po);
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	if(stackIndex>0)
    	{
    		KnockaboutCell dr = droppedDestStack[0];
    		pickedObject = dr.removeChip();
    		droppedDestStack[0] = null;
    		rollCell = null;
    		int ind = 1;
    		while(ind<stackIndex)
    		{
    			KnockaboutCell from = pickedSourceStack[ind];
    			KnockaboutCell to = droppedDestStack[ind];
    			from.addChip(to.removeChip());
    			pickedSourceStack[ind]=droppedDestStack[ind]=null;
    			ind++;
    		}
    		
    		stackIndex = 0;
    	}
     	setState(KnockaboutState.PLAY_STATE);
     }

    // lowest level of uncapture.  "removed" is the most distant cell removed by the capture
    // picked and dropped are the source and destination respectively.  This handles both
    // capture by approach and by withdrawal.

    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	KnockaboutChip po = pickedObject;
    	if(po!=null)
    	{
    	KnockaboutCell ps = pickedSourceStack[stackIndex];
    	KnockaboutChip oc = removeChip(ps);
    	addChip(ps,po);
        pickedSourceStack[stackIndex]=null;
    	pickedObject = oc;
    	}
     }

    
    private void dropOnBoard(KnockaboutCell dcell,boolean move)
    {
    	droppedDestStack[stackIndex] = dcell;
    	addChip(dcell,pickedObject);
    	pickedObject=null;
     }

    private void pickFromCell(KnockaboutCell src)
    {  	//G.print("st "+stackIndex);
    	pickedSourceStack[stackIndex] = src;
    	pickedObject = removeChip(src);
    }

  
    private KnockaboutCell[][]tempDestResource = new KnockaboutCell[4][];
    private int tempDestIndex=-1;
    public synchronized KnockaboutCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new KnockaboutCell[MAXDESTS]);
    	}
    public synchronized void returnTempDest(KnockaboutCell[]d) { tempDestResource[++tempDestIndex]=d; }

    public KnockaboutBoard(long random,String init,int[]map) // default constructor
    {  	drawing_style =  DrawingStyle.STYLE_NOTHING;//  or STYLE_CELL or STYLE_LINES
    	Grid_Style = KNOXKABOUTGRIDSTYLE; //coordinates left and bottom
    	Random r = new Random(60404);
    	setColorMap(map);
       	initBoard(ZfirstInCol, ZnInCol, null);
       	allCells.setDigestChain(r);
       	randomKey = random;
    	doInit(init,random); // do the initialization 
    	displayParameters.reverse_y = true;
    }
    public KnockaboutBoard cloneBoard() 
	{ KnockaboutBoard dup = new KnockaboutBoard(randomKey,gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((KnockaboutBoard)b); }
   // overridable method.  xpos ypos is the desired centerpoint for the text
    // 
    public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
    {	if((txt.length()<=2) && txt.charAt(1)=='1')
    		{ xpos -=cellsize/6; ypos-= cellsize/5; } 
    		else 
    		{ xpos-=cellsize/5; ypos += cellsize/8; } 
    	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
   }

    // standard starting position for the dice
    private int stdpos[][]={{'D',5,4,1},{'F',6,4,1},{'H',6,4,1},{'J',5,4,1},
    		{'E',4,6,2},{'G',5,6,2},{'I',4,6,2},
    		{'F',3,8,3},{'H',3,8,3}};
    private void Init_Standard(String game)
    {  
        gametype = game;
        setState(KnockaboutState.PUZZLE_STATE);
        rollCell = null;
        undoStack.setSize(0);
        for(KnockaboutCell c=allCells;
            c!=null;
            c=c.next)
        {	c.reInit();
        }
        AR.setValue(chipsInGutter,0);
        if (Knockabout_Standard_Init.equalsIgnoreCase(game)) 
        {	
        	//temporary, visualize all the dice
        	//for(KnockaboutCell c =(KnockaboutCell)allCells;
        	//	c !=null;
        	//	c = (KnockaboutCell)c.next)
        	//{	c.addChip(KnockaboutChip.getChip(i));
        	//	i++;
        	//	if(i>=KnockaboutChip.CANONICAL_PIECE.length) { break; }
        	//}
        	boolean swap = getColorMap()[0]!=0;
        	for(int idx=0;idx<stdpos.length;idx++)
        	{	int spec[] = stdpos[idx];
        		char col = (char)spec[0];
        		int row = spec[1];
        		KnockaboutCell cell = getCell(col,row);
        		KnockaboutCell cell2 = getCell(col,nInCol[col-'A']-row+1);
        		KnockaboutChip chip = KnockaboutChip.getChip(KnockId.WhiteDie,spec[2],spec[3]);
        		KnockaboutChip chip2 = KnockaboutChip.getChip(KnockId.BlackDie,spec[2],spec[3]);
        		addChip(cell,swap ? chip2 : chip);
        		addChip(cell2,swap ? chip : chip2);
        		
        	}
        }
        else { throw G.Error(WrongInitError,game); }
        
      whoseTurn = FIRST_PLAYER_INDEX;
    }
    public void sameboard(BoardProtocol f) { sameboard((KnockaboutBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(KnockaboutBoard from_b)
    {	super.sameboard(from_b);
    	G.Assert(AR.sameArrayContents(chipsInGutter,from_b.chipsInGutter),"chipsInGutter mismatch");
        G.Assert((rollCell==from_b.rollCell)||((rollCell!=null)&&rollCell.sameCell(from_b.rollCell)),"same roll cell");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject matches");
    }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
     * @return
     */
    public long Digest()
    {

        Random r = new Random(64 * 1000); // init the random number generator
    	long v = super.Digest();
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,getSource());
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
       return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(KnockaboutBoard from_b)
    {	super.copyFrom(from_b);
        pickedObject = from_b.pickedObject;
        AR.copy(chipsInGutter,from_b.chipsInGutter);
        stackIndex = from_b.stackIndex;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        rollCell = getCell(from_b.rollCell);
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long ran)
    {  
       Init_Standard(gtype);
       randomKey = ran;
       randomSeq = new Random(randomKey);
       pickedObject = null;
       AR.setValue(pickedSourceStack, null);
       AR.setValue(droppedDestStack, null);
       AR.setValue(win, false);
       int map[] = getColorMap();
       playerColor[map[0]] = KnockId.WhiteDie;
       playerColor[map[1]] = KnockId.BlackDie;
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
        case CONFIRM_STATE:
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
        {case RESIGN_STATE:
         case CONFIRM_STATE:
         case DRAW_STATE:
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	
    	switch(board_state)
    	{
    	default: return(DoneState());
    	}
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(KnockaboutState.GAMEOVER_STATE);
    }


    //
    // in the actual game, picks are optional; allowed but redundant.
    //
    private void setNextStateAfterDrop()
    {
     	switch(board_state)
    	{
    	default: throw G.Error("Not expected");
     	case PLAY_STATE: 
    	{ 	
    		setState(KnockaboutState.CONFIRM_STATE);
    	}
    	break;
    	}
     }
    private void setNextStateAfterPick()
    {        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case GAMEOVER_STATE:
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setState(KnockaboutState.PLAY_STATE);
        	break;
        case PLAY_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }

    private void setNextStateAfterDone(KnockaboutMovespec m)
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
       	case PUZZLE_STATE:
       	case CONFIRM_STATE:
    		if(chipsInGutter[whoseTurn]>=5) { setGameOver(false,true); }
    		else if(chipsInGutter[nextPlayer[whoseTurn]]>=5) { setGameOver(true,false); }
    		else { setState(KnockaboutState.PLAY_STATE);	// default, and makes hasLegalMoves work 
    		}
    		break;
    	}
       	if(rollCell!=null)
       	{	KnockaboutChip chip = removeChip(rollCell);
       		if(chip!=null)
       			{int newv = (rollValue % chip.numSides) + 1;
       			 KnockaboutChip newchip = KnockaboutChip.getChip(chip.color,chip.numSides,newv);
       			 m.rerolled = newchip;
       			 addChip(rollCell,newchip);
       			 rollCell = null;
       			}
      	}

    }
     
    private void doDone(KnockaboutMovespec m)
    {	
    	if(board_state==KnockaboutState.RESIGN_STATE) 
    	{ setGameOver(false,true);
    	}
    	else
    	{
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case PUZZLE_STATE: break;
    	case DRAW_STATE:
    	case CONFIRM_STATE:  
    		finalizePlacement();
       	 	setNextPlayer(); 
     		setNextStateAfterDone(m);
     		break;
    	}}
    }

    // remove the top N pieces from the captured stack and place them on dest
    public void undoCaptures(int n,KnockaboutCell dest)
    {	throw G.Error("not implemented");
    }
    // find thedirection from - to which may be a regular move or a gutter move
    public int findMoveDirection(KnockaboutCell from,KnockaboutChip chip,KnockaboutCell to)
    {	if(from.inGutter)
    		{	
    		// if we're in the gutter things are more complicated, as the "direction" may go around
    		// a corner.  There may be a more elegant way to do this, but we just search for the
    		// right direction.
    		for(int direction=0;direction<CELL_FULL_TURN;direction++)
    		{	KnockaboutCell c = from.exitTo(direction);
    			if((c!=null) && c.inGutter)
    			{
    			int activeDirection=direction;
    			for(int i=1;i<=chip.numSides;i++)
    			{
    				if(to==c) { return(direction); }		// found the target
    				KnockaboutCell next = c.exitTo(activeDirection);
    				if(next==null) { activeDirection+=1; next=c.exitTo(activeDirection); }
    				if(next==null) { activeDirection-=2; next=c.exitTo(activeDirection); }
    				G.Assert((next!=null) && next.inGutter,"staying in the gutter");
    				c = next;
    			}
    			}
    			
    		}
    		throw G.Error("gutter direction not found");
    		}
     	// if we don't start in the gutter, then the from/to moves are always in a line
    	return(findDirection(from.col,from.row,to.col,to.row));
   }
    // return true if there is still an empty space in this direction
    boolean hasEmpty(KnockaboutCell from,int dir)
    {
    	while(from!=null)
    		{ if(from.topChip()==null) { return(true); }
    		  from=from.exitTo(dir);
    		}
    	return(false);
    }
    void doBumpMove(KnockaboutCell from,KnockaboutChip chip,int dir,replayMode replay)
    {	int steps = chip.numberShowing;
    	int activeDirection = dir;
    	KnockaboutCell c = from;
    	KnockaboutChip cc = chip;
    	boolean needRoll = false;
    	if(replay!=replayMode.Replay)
    	{
    		animationStack.push(c);
    	}
     	while(steps>0)
    	{	KnockaboutCell next = c.exitTo(activeDirection);
    		if(from.inGutter)
    		{	if(next==null) { activeDirection+=1; next = c.exitTo(activeDirection); }
    			if(next==null) { activeDirection-=2; next = c.exitTo(activeDirection); }
    			G.Assert((next!=null) && next.inGutter,"staying in the gutter");
    		}
    			
    		if(next==null) { break; }
    		KnockaboutChip nextchip = next.topChip();
    		if(nextchip!=null) 
    			{ droppedDestStack[stackIndex] = c;
    			  stackIndex++;
    			  addChip(c,cc);
    			  pickedSourceStack[stackIndex] = next;
    			  cc = removeChip(next);
    		      if(replay!=replayMode.Replay)
    		    	{
    		    		animationStack.push(c);
    		    		animationStack.push(next);
    		    	}
  			  
    	 		  needRoll = true;
   			}
    		else {	steps--; }
    		c = next; 
    	}
       	if(replay!=replayMode.Replay)
    	{
    		animationStack.push(c);	// final destination
    	}

    	addChip(c,cc);
    	droppedDestStack[stackIndex]=c;
    	stackIndex++;
    	if(needRoll) { rollCell = c; }
    	pickedObject = null;
 
     }
    public boolean Execute(commonMove mm,replayMode replay)
    {	KnockaboutMovespec m = (KnockaboutMovespec)mm;
        
        //G.print("E "+m+" for "+whoseTurn+" : "+stackIndex);
        switch (m.op)
        {
        case MOVE_DONE:
        	m.undoInfo = -1;
        	// undo unfo is the original value of the rolled piece
        	if(rollCell!=null) 
        	{ KnockaboutChip top = m.piece = rollCell.topChip();
        	  m.undoInfo = top.index; 
        	}
         	doDone(m);

            break;
        case MOVE_BOARD_BOARD:
        	{
            int undoinfo = undoStack.size();	// undo info is the initial value of the unwind stack
           	KnockaboutCell from = getCell(m.from_col, m.from_row);
        	KnockaboutCell target = getCell(m.to_col, m.to_row); 
            KnockaboutChip top = target.topChip();
            pickFromCell(from);
            m.piece = pickedObject;
            if(top==null) 
            	{
            	  dropOnBoard(target,true);
            	  m.undoInfo = -1;
                  stackIndex++;
                  if(replay!=replayMode.Replay)
                  {
                  	animationStack.push(from);
                  	animationStack.push(target);
                  }
            	}
            else 
            { 	int dir = findMoveDirection(from,pickedObject,target);
            	m.piece = pickedObject;
            	doBumpMove(from,pickedObject,dir,replay);
            	rollValue = m.rollAmount;
            	m.undoInfo = undoinfo*1000+rollCell.topChip().index;
            	for(int i=0;i<stackIndex;i++)
            	{	undoStack.addElement(pickedSourceStack[i]);
            		undoStack.addElement(droppedDestStack[i]);
            	}
            	undoStack.addElement(rollCell);
            }
            setNextStateAfterDrop();
          }
        	break;
        case MOVE_DROPB:
        	{
        	G.Assert(pickedObject!=null,"something is moving");
        	KnockaboutCell from = pickedSourceStack[stackIndex];
        	KnockaboutCell target =  getCell(m.to_col, m.to_row);
        	if(from==target) { unPickObject(); }
        	else
        	{
            switch(board_state)
            {
            default:
            	throw G.Error("Not expecting state %s",board_state);
            case PUZZLE_STATE:
                dropOnBoard(target,false);
                finalizePlacement();
            	break;
            case PLAY_STATE:
              {	KnockaboutChip top = target.topChip();
              	m.piece = top;
                if(top==null) 
                	{ dropOnBoard(target,true);
                      stackIndex++;
                      if(replay==replayMode.Single)
                      {
                      	animationStack.push(from);
                      	animationStack.push(target);
                      }
                	}
                else 
                { 	int dir = findMoveDirection(from,pickedObject,target);
                	doBumpMove(from,pickedObject,dir,replay);
                	rollValue = m.rollAmount;
                }
                setNextStateAfterDrop();
              }
               break;
            }}
        	}
            break;
        case MOVE_ROLL:
           	{
           	KnockaboutCell from = getCell(m.from_col,m.from_row);
           	KnockaboutChip chip = from.topChip();
           	KnockaboutChip newc = KnockaboutChip.getChip(chip.color,chip.numSides,m.to_row);
           	removeChip(from);
           	m.piece = pickedObject;
           	addChip(from,newc);
           	
           	}
        	break;
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary pick
        	switch(board_state)
        	{	
           	case CONFIRM_STATE:
           		unDropObject();
           		break;
           	default:
           		{
           		KnockaboutCell target = getCell(m.from_col, m.from_row);
           		  pickFromCell(target);
           		  m.piece = pickedObject;
                  setNextStateAfterPick();
           		}
        	}
            break;

        	
        case MOVE_START:
            setWhoseTurn(m.player);
            finalizePlacement();
            setState(KnockaboutState.PUZZLE_STATE);
    		if(chipsInGutter[whoseTurn]>=5) { setGameOver(false,true); }
    		else if(chipsInGutter[nextPlayer[whoseTurn]]>=5) { setGameOver(true,false); }
    		else
            {	boolean win1 = WinForPlayer(whoseTurn);
            	boolean win2 = WinForPlayer(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setState(KnockaboutState.PLAY_STATE); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?KnockaboutState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
        	finalizePlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(KnockaboutState.PUZZLE_STATE);

            break;

		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        default:
        	cantExecute(m);
        }


        return (true);
    }
 
  
    // true if it's legal to drop a chip  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(KnockaboutCell fromCell,KnockaboutCell toCell)
    {	if(fromCell==toCell) { return(true); }
      	KnockaboutCell temp[] = getTempDest();
    	int n = getDestsFrom(fromCell,whoseTurn,temp,0,fromCell==pickedSourceStack[stackIndex]);
    	boolean ok = false;
    	for(int i=0;(i<n) && !ok;i++) 
    		{ if(temp[i]==toCell) 
    		{ ok = true; } 
    		}
    	returnTempDest(temp);
    	return(ok);
    }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    private boolean isDest(KnockaboutCell cell)
    {	return((stackIndex>0) && (droppedDestStack[0]==cell));
    }

    public boolean LegalToHitBoard(KnockaboutCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
 			if(pickedObject!=null)
 			{	return(LegalToDropOnBoard(pickedSourceStack[stackIndex],cell));
 			}
 			return((cell.chip!=null)
 					&& (cell.topChip().color==playerColor[whoseTurn])
 					&& (getDestsFrom(cell,whoseTurn,null,0,false)>0));
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
			return(isDest(cell));
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return((pickedObject==null)
        			?(cell.chip!=null)	// pick from a nonempty cell
        			: cell.chip==null);	// drop on any empty
        }
    }

 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(KnockaboutMovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transitions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //System.out.println("R "+m+" for "+m.player);
        if (Execute(m,replayMode.Replay))
        {	if(m.op==MOVE_BOARD_BOARD)
        	{	if(DoneState()) 
        			{  doDone(m); 
        			}
        	}
        	else if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone(m);
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
    public void UnExecute(KnockaboutMovespec m)
    {	int who = whoseTurn;
    	setWhoseTurn(m.player);
        //System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
  	    default:
   	    	cantUnExecute(m);
        	break;
	    	  
        case MOVE_DONE:
            break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			int undo = m.undoInfo;
        			if(undo==-1) 
        			{	// simple unmove
                		KnockaboutCell dest = getCell(m.to_col,m.to_row);
                		KnockaboutCell src = getCell(m.from_col,m.from_row);
                		addChip(src,removeChip(dest));
               		}
        			else
        			{// multiple bump/roll.  The intermediate steps have been pushed on the undoStack
        			int unroll = undo%1000;
        			int unstack = undo/1000;
        			KnockaboutChip replace = KnockaboutChip.getChip(unroll);
        			KnockaboutCell last = popUndoCell();
        			removeChip(last);
        			addChip(last,replace);
         			while(undoStack.size()>unstack)
        			{	KnockaboutCell dest = popUndoCell();
        				KnockaboutCell src = popUndoCell();
        				KnockaboutChip con = removeChip(dest);
        				addChip(src,con);
         			}
        			}
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        if(who!=m.player)
        	{
        	moveNumber--;
        	}
 }

// get the legal dests from "from" for player "forplayer"
// temp can be null, in which case only the number of dests is returned
 int getDestsFrom(KnockaboutCell from,int forplayer,KnockaboutCell temp[],int idx0,boolean picked)
 {	int n=idx0;
 	switch(board_state)
 	{
 	case PLAY_STATE:
 	default: 
 		KnockaboutChip fromChip = picked ? pickedObject : from.topChip();
 		int distance = fromChip.numberShowing;
 		for(int direction=0;direction<CELL_FULL_TURN;direction++)
 			{	int activeDirection = direction;
 				KnockaboutCell c = from;
				for(int dx = 0; dx<distance; dx++)
				{	KnockaboutCell next = c.exitTo(activeDirection);

					if(from.inGutter) 
 						{	// if we're playing gutter ball, we can change direction but not leave the gutter
							if(next==null)
 								{ if(dx==0) { c=null; break; }
 								  KnockaboutCell nx1 = c.exitTo(activeDirection+1);
 								  if((nx1!=null) && nx1.inGutter) { next = nx1; activeDirection++; }
 								  else { next = c.exitTo(activeDirection-1); activeDirection--; }
 								  G.Assert((next!=null)&&next.inGutter,"still in the gutter");
 								}
							c = next;
 							if(!c.inGutter) { c = null; break; }
 							if(c.topChip()!=null) { break; }
 						}
 						else 
 							{ 
 							  if(next==null) { break; }
 							  c = next;
 							  if(c.topChip()!=null) { break; }
 							}
 					}
 				if(c!=null) 
 					{ if(temp!=null) { temp[idx0]=c; }
 					  idx0++;
 					  n++;
 					}
 			};
 			break;
  	case CONFIRM_STATE:
 	case PUZZLE_STATE: 
 		break;
 	}
 	return(n);
 }

 // get a hash of moves destinations, used by the viewer
 // to know where to draw dots and also to determine what
 // is a legal move
 public Hashtable<KnockaboutCell,KnockaboutCell> getMoveDests()
 {	Hashtable<KnockaboutCell,KnockaboutCell> dd = new Hashtable<KnockaboutCell,KnockaboutCell>();
 	if(pickedObject!=null)
 	{
 	KnockaboutCell src = pickedSourceStack[stackIndex];
 	if(src!=null)
 	{	KnockaboutCell temp[] = getTempDest();
 		int n = getDestsFrom(src,whoseTurn,temp,0,true);
 		for(int i=0;i<n;i++)
 		{	KnockaboutCell c = temp[i];
 			dd.put(c,c);
 		}
 		returnTempDest(temp);
 	}}
	return(dd);
 }
 
 CommonMoveStack  GetListOfMoves(CommonMoveStack  all,boolean erupting,boolean nonerupting)
 {	
	switch(board_state)
	{
	case PLAY_STATE:
		{
		KnockaboutCell temp[] = getTempDest();
		for(KnockaboutCell c = allCells;
			c!=null;
			c=c.next)
				{
				KnockaboutChip ch = c.topChip();
				if((ch!=null) && (ch.color==playerColor[whoseTurn]))
				{	int n = getDestsFrom(c,whoseTurn,temp,0,false);
					for(int i=0;i<n;i++) 
						{ KnockaboutCell dc = temp[i];
						  int ran = randomSeq.nextInt();
						  if(ran<0) { ran = Math.abs(ran); }
						  if(ran<0) { ran = 0; }	// if original ran was Integer.MIN_VALUE, abs is still negative
						  all.addElement(new KnockaboutMovespec(MOVE_BOARD_BOARD,c.col,c.row,dc.col,dc.row,whoseTurn,ran));
						}
				}
				}
		}
		break;
	default: throw G.Error("Not expecting");
	}
  	return(all);
 }
 
}