package gounki;

import java.awt.Color;

import online.game.*;

import java.util.*;
import lib.*;
import lib.Random;


/**
 * GounkiBoard knows all about the game of Gounki, which is played
 * on a 7x7 board. It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
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

class GounkiBoard extends rectBoard<GounkiCell> implements BoardProtocol,GounkiConstants
{
	static final int DEFAULT_COLUMNS = 8;	// 8x6 board
	static final int DEFAULT_ROWS = 12;
	static final int STACKHEIGHTLIMIT = 6;	// this height limit is used when packing descriptions for undo.
	static final String[] GOUNKIGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	static final GounkiId RackLocation[] = { GounkiId.White_Chip_Pool,GounkiId.Black_Chip_Pool};
   	static final int White_Chip_Index = 0;
	static final int Black_Chip_Index = 1;

	private GounkiState unresign;
 	private GounkiState board_state;
	public GounkiState getState() {return(board_state); }
	public CellStack animationStack = new CellStack();
	public void setState(GounkiState st) 
	{ 	unresign = (st==GounkiState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() { setState(GounkiState.DRAW_STATE); }
    public GounkiCell rack[][] = new GounkiCell[2][GounkiChip.N_CHIP_TYPES];
    public int chipsOnBoard[] = new int[2];
     
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public GounkiChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack =  new CellStack();
    private CommonMoveStack dropMoveStack = new CommonMoveStack();
    //
    // caches of all the moves available when the first chip is picked up in PLAY_STATE
    private CommonMoveStack deployMoves = new CommonMoveStack();
    private CommonMoveStack moveMoves = new CommonMoveStack();
    private int deploymentSize = 0;		// number of chips yet to deploy
    
    public CellStack cellsForCurrentMove=new CellStack();
    public CellStack cellsForPrevMove=new CellStack();
    

    private StateStack pickedStateStack = new StateStack();
    private int forward_left[] = new int[2];
    private int forward_right[] = new int[2];
    private int forward[] = new int[2];
	// factory method
	public GounkiCell newcell(char c,int r)
	{	return(new GounkiCell(c,r));
	}
    public GounkiBoard(String init,long rv,int map[]) // default constructor
    {   setColorMap(map, 2);
        doInit(init,rv); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
     }

    public int gridFirstRowInColumn(char ch)
    {
    	return(3);
    }
    public int gridLastRowInColumn(char ch)
    {
    	return(10);
    }
    
    public void DrawGridCoord(Graphics gc, Color clr,int xpos, int ypos, int cellsize,String txt)
    {  	if(Character.isDigit(txt.charAt(0)))
    		{ txt = ""+ (G.IntToken(txt)-2);
    		}
    	super.DrawGridCoord(gc,clr,xpos,ypos,cellsize,txt);
    }
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	if(Gounki_INIT.equalsIgnoreCase(game)) 
    		{ boardColumns=DEFAULT_COLUMNS; 
    		  boardRows = DEFAULT_ROWS;
    		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(GounkiState.PUZZLE_STATE);
        initBoard(boardColumns,boardRows); //this sets up the board and cross links
        acceptPlacement();
        cellsForCurrentMove.clear();
        cellsForPrevMove.clear();
        chipsOnBoard[0] = 0;
        chipsOnBoard[1] = 0;
        deploymentSize = 0;
        for(int rownum=2; rownum<=3;rownum++)
        {	int row = rownum+1;
        	for(int colnum=0;colnum<boardColumns;colnum++)
        	{ char col = (char)('A'+colnum);
        	  int set = (colnum/2)%3;
        	  getCell(col,row).addChip(GounkiChip.getChip(0,set*2+((1^colnum^rownum)&1)));
        	  getCell(col,boardRows-rownum).addChip(GounkiChip.getChip(1,set*2+((colnum^rownum)&1)));
        	  chipsOnBoard[0]++;
        	  chipsOnBoard[1]++;
        	}
        }
        whoseTurn = FIRST_PLAYER_INDEX;

    }
    public void sameboard(BoardProtocol f) { sameboard((GounkiBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(GounkiBoard from_b)
    {	super.sameboard(from_b);
    	G.Assert(AR.sameArrayContents(chipsOnBoard,from_b.chipsOnBoard),"chipsOnBoard mismatch");
    	G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
        G.Assert(deploymentSize==from_b.deploymentSize, "deployment mismatch");
         
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

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
		v ^= chipsOnBoard[0]*r.nextLong();
		v ^= chipsOnBoard[1]*r.nextLong();
		v ^= deploymentSize*r.nextLong();
		v ^= Digest(r,rack);
        // for most games, we should also digest whose turn it is
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);

        return (v);
    }
   public GounkiBoard cloneBoard() 
	{ GounkiBoard copy = new GounkiBoard(gametype,randomKey,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((GounkiBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(GounkiBoard from_b)
    {	super.copyFrom(from_b);
        
    	copyAllFrom(rack,from_b.rack);
        deploymentSize = from_b.deploymentSize;
        AR.copy(chipsOnBoard,from_b.chipsOnBoard);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        pickedObject = from_b.pickedObject;
        deployMoves.copyFrom(from_b.deployMoves);
        moveMoves.copyFrom(from_b.moveMoves);
        dropMoveStack.copyFrom(from_b.dropMoveStack);
        getCell(cellsForCurrentMove,from_b.cellsForCurrentMove);
        getCell(cellsForPrevMove,from_b.cellsForPrevMove);
        pickedStateStack.copyFrom(from_b.pickedStateStack);
       
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;	// not used, but for reference in this demo game
    	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = GOUNKIGRIDSTYLE; //coordinates left and bottom
    	int map[]=getColorMap();
    	for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX; pl++)
    	{ for(int i=0;i<GounkiChip.N_CHIP_TYPES; i++)
    		{
    		GounkiCell cell = new GounkiCell();
    		cell.rackLocation=RackLocation[pl];
    		cell.row = i;
     		rack[map[pl]][i]=cell;
     	}} 
    	Init_Standard(gtype);
        moveNumber = 1;
        forward_left[map[0]] = CELL_UP_LEFT;
        forward_right[map[0]] = CELL_UP_RIGHT;
        forward[map[0]] = CELL_UP;
        forward_left[map[1]] = CELL_DOWN_RIGHT;
        forward_right[map[1]] = CELL_DOWN_LEFT;
        forward[map[1]] = CELL_DOWN;
 
        // note that firstPlayer is NOT initialized here
    }

    //
    // change whose turn it is, increment the current move number
    //
    private void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
            G.Error("Move not complete, can't change the current player %s",board_state);
            break;
        case PUZZLE_STATE:
            break;
        case DEPLOY2_STATE:
        	// allow some damaged games to continue
        	if(replay==replayMode.Live) { G.Error("Move not complete, can't change the current player %s",board_state);}
			//$FALL-THROUGH$
		case CONFIRM_STATE:
        case DEPLOY_STATE:
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
         case DEPLOY_STATE:
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(GounkiState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==GounkiState.GAMEOVER_STATE) { return(win[player]); }
    	if((chipsOnBoard[player]>0) && (chipsOnBoard[nextPlayer[player]]==0)) 
    		{ return(true); /* opponent has no chips left */
    		}
    	// the board is 12x8 
    	int []map = getColorMap();
    	GounkiCell ch = getCell('A',(map[player]==0)?boardRows-1:2);
    	while(ch!=null) 
    		{ GounkiChip top = ch.topChip(); if((top!=null)&&(playerIndex(top)==player))
    			{ return(true);
    			} 
    		  ch = ch.exitTo(CELL_RIGHT);
    		}
    	return(false);
    }
    // estimate the value of the position
    public synchronized double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot)
    {  	double finalv=chipsOnBoard[player]*100;					// 100 points per chip baseline
    	GounkiCell[] myrack = rack[player];
    	finalv += myrack[GounkiChip.ROUND_CHIP_INDEX].height()*33;		// make rounds less valuable
    	// score for advancing toward the goal.
    	int map[] = getColorMap();
    	for(GounkiCell c = allCells; c!=null; c=c.next)
    	{	GounkiChip top = c.topChip();
    		if((top!=null)&&(playerIndex(top)==player))
    		{	finalv += (map[player]==0)?(c.row-1):(boardRows-c.row);
    		}
    	}
    	return(finalv);
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	CellStack prev = cellsForPrevMove;
    	cellsForPrevMove = cellsForCurrentMove;
    	cellsForCurrentMove = prev;
    	prev.clear();
        pickedObject = null;
        droppedDestStack.clear();
        pickedSourceStack.clear();
        pickedStateStack.clear();
        dropMoveStack.clear();
        moveMoves.clear();
        deployMoves.clear();
        deploymentSize = 0;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    GounkiCell src = pickedSourceStack.top();
    GounkiCell dr = droppedDestStack.pop();
    GounkiMovespec m = (GounkiMovespec)dropMoveStack.pop();
    int undoInfo = m.undoInfo;
    GounkiState state = m.state;
    int undoInfo2 = undoInfo/100;
   switch(m.op)
    {	case MOVE_DROPB:	
        	{
        	int size = undoInfo2%STACKHEIGHTLIMIT;
        	int capinfo = undoInfo2/STACKHEIGHTLIMIT;
         	moveStack(dr,src,size);
    		pickedObject = src.removeTop();
    		undoCaptures(dr,capinfo);
    		deploymentSize = 0;
    	    switch(dr.rackLocation())
        	{
        	default: throw G.Error("Not expecting dest %d",dr.rackLocation);
        	case BoardLocation:	chipsOnBoard[playerIndex(pickedObject)]--;
        		break;
        	case Black_Chip_Pool:
        	case White_Chip_Pool:
        		break;
        	}
        	}
    		break;
    	case MOVE_DEPLOY:
    		undoDeploy(m);
    		deploymentSize = 0;
    		break;
    	case MOVE_DEPLOYSTEP:
    		pickedSourceStack.pop();	// remove the source too.  This is unusual
    		deploymentSize++;
    		int height = undoInfo2%STACKHEIGHTLIMIT;
    		int stack = undoInfo2/STACKHEIGHTLIMIT;
    		while(height-->0) { dr.removeTop(); }
    		constructStack(src,stack);
    		
    		break;
default:
	break;
    }
    setState(state);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	GounkiChip po = pickedObject;
    	GounkiCell c = pickedSourceStack.pop();
    	c.addChip(po);
    	setState(pickedStateStack.pop());
    	pickedObject = null;
    	switch(c.rackLocation())
    	{
    	default: throw G.Error("Not expecting source %d", c.rackLocation);
    	case BoardLocation:
    		chipsOnBoard[playerIndex(po)]++;
    		break;
    	case White_Chip_Pool:
    	case Black_Chip_Pool:

    	}
     }

    public GounkiCell getCell(GounkiId dest,char col,int row)
    {
        switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case BoardLocation: // an already filled board slot.
        	return(getCell(col,row));
        case Black_Chip_Pool:		// back in the pool
        	return(rack[SECOND_PLAYER_INDEX][row]);
        case White_Chip_Pool:		// back in the pool
           	return(rack[FIRST_PLAYER_INDEX][row]);
        }
    }
    // 
    // drop the floating object.
    //
    private void dropObject(GounkiCell c,GounkiMovespec m)
    {  
       G.Assert((pickedObject!=null),"ready to drop");
       GounkiChip po = pickedObject;
       pickedObject = null;
       c.addChip(po);
       droppedDestStack.push(c);
       dropMoveStack.push(m);
       switch (c.rackLocation())
        {
        default: throw G.Error("Not expecting dest %d",c.rackLocation);
        case BoardLocation: // an already filled board slot.
        	chipsOnBoard[playerIndex(po)]++;
        	break;
        case Black_Chip_Pool:		// back in the pool
        case White_Chip_Pool:		// back in the pool
             break;
        }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(GounkiCell cell)
    {	return(droppedDestStack.top()==cell);
    }
    public boolean isSource(GounkiCell cell)
    {	return(pickedSourceStack.top()==cell);
    }
    public GounkiCell getSource() { return(pickedSourceStack.top()); }
    public GounkiCell getDest() { return(droppedDestStack.top()); }
    
	// get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	if(pickedObject!=null)
    		{ return(pickedObject.chipNumber());
    		}
        return (NothingMoving);
    }
  
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(GounkiCell c)
    {	G.Assert((pickedObject==null),"ready to pick");
    	pickedObject = c.removeTop();
    	pickedSourceStack.push(c);
    	pickedStateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting source %d",c.rackLocation);
        case BoardLocation:
         	{
        	chipsOnBoard[playerIndex(pickedObject)]--;
         	break;
         	}
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	break;
         }
   }


    private void setNextStateAfterDone(replayMode replay)
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state %s",board_state);
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
        case DEPLOY2_STATE:
        	// allow some damaged games to continue
        	if(replay==replayMode.Live) { throw G.Error("Not expecting after Done state %s",board_state); };
			//$FALL-THROUGH$
		case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case DEPLOY_STATE:
    	case PLAY_STATE:
    		setState(GounkiState.PLAY_STATE);
    		break;
    	}

    }
       
    private void doDone(replayMode replay)
    {	
        acceptPlacement();

        if (board_state==GounkiState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(replay); setNextStateAfterDone(replay); }
        }
    }
    public int doCaptures(GounkiCell to,replayMode replay)
    {	
    	int desc = 0;
    	GounkiChip top = to.topChip();
    	if((top!=null) && (playerIndex(top)!=whoseTurn()))
    	{	desc = stackDescription(to);
    		while(to.height()>0)
    		{	top = to.removeTop();
    			int player = playerIndex(top);
     			chipsOnBoard[player]--;
    			GounkiCell dest = rack[player][top.chipTypeIndex()];
    			dest.addChip(top);
    			if(replay!=replayMode.Replay)
    			{	
    				animationStack.push(to);
    				animationStack.push(dest);
    			}
    		}
    	}
    	return(desc);
    }
    public int playerIndex(GounkiChip ch) { return(getColorMap()[ch.getColorIndex()]);}
    
    // use the standard stack coding to construct the stack,
    // but remove the chips from rack instead of just creating them.
    public int undoCaptures(GounkiCell to,int stackCode)
    {	int sz = stackCode%STACKHEIGHTLIMIT;
    	int contents = stackCode/STACKHEIGHTLIMIT;
    	while(sz-- > 0)
    	{	GounkiChip ch = GounkiChip.getChip(contents%GounkiChip.NCHIPTYPES);
			int player = playerIndex(ch);
    		contents = contents/GounkiChip.NCHIPTYPES;
    		rack[player][ch.chipTypeIndex()].removeChipOfType(ch);
    		to.addChip(ch);
    		chipsOnBoard[player]++;
    	}
    	return(sz);
    } 
    public void moveStack(GounkiCell from,GounkiCell to,int depth)
    {	GounkiChip ch = from.removeTop();
    	if(depth-- > 1) { moveStack(from,to,depth); }
    	to.addChip(ch);
    }
    private boolean isSquareDeployment(GounkiCell from,GounkiCell to)
    {	return((from.col==to.col)||(from.row==to.row));
    }
    
    // build a description of the current stack for undo to use
    private int stackDescription(GounkiCell src)
    {	int code = 0;
    	int size = src.height();
       	for(int i=size-1; i>=0; i--)
    	{	GounkiChip ch = src.chipAtIndex(i);
    		int num  = ch.chipNumber();
    		code = code*GounkiChip.NCHIPTYPES + num;
    	}
       	return(code*STACKHEIGHTLIMIT+size);
    }
    public int doDeploy(GounkiCell src,GounkiCell d1,GounkiCell d2,GounkiCell d3,replayMode replay)
    {	int code = stackDescription(src);
    	int size = src.height();
    	G.Assert((size>=2) && ((size==3) || (d3==null)), "inconsistent deployment");
    	// deploy 2 or 3 chips.  If the row or column is the same, it's a square deployment
    	// prefer to remove from the bottom of the stack first
    	// and in any case preserve the identity of the chips
    	cellsForCurrentMove.push(src);
    	cellsForCurrentMove.push(d1);
    	cellsForCurrentMove.push(d2);
    	d1.addChip(src.removeChipOfType(isSquareDeployment(src,d1),0));
    	d2.addChip(src.removeChipOfType(isSquareDeployment(d1,d2),0));
    	if(replay!=replayMode.Replay)
    	{
    		animationStack.push(src);
    		animationStack.push(d1);
    		animationStack.push(d1);
    		animationStack.push(d2);
    	}

    	if(d3!=null) 
    	{	d3.addChip(src.removeChipOfType(isSquareDeployment(d2,d3),0));
    		cellsForCurrentMove.push(d3);
    	   	if(replay!=replayMode.Replay)
        	{
        		animationStack.push(d2);
        		animationStack.push(d3);
        	}
    	}
    	return(code);
    }
    // construct a stack according to the code
    public void constructStack(GounkiCell src,int stackCode)
    {	int code = stackCode/STACKHEIGHTLIMIT;
		int size = stackCode%STACKHEIGHTLIMIT;
		while(src.height()>0) { src.removeTop(); }
	   	while(size-->0)
    	{	src.addChip(GounkiChip.getChip(code%GounkiChip.NCHIPTYPES));
    		code = code/GounkiChip.NCHIPTYPES;
    	}
    }
    // undo deployment, restoring the orignal stack order
    public void undoDeploy(GounkiCell src,GounkiCell d1,GounkiCell d2,GounkiCell d3,int stackCode)
    {	
    	// deploy 2 or 3 chips.  If the row or column is the same, it's a square deployment
    	// prefer to remove from the bottom of the stack first
    	// and in any case preserve the identity of the chips
    	d1.removeTop();
    	d2.removeTop();
    	if(d3!=null) 
    	{	d3.removeTop();
    	}
    	// build a code describing the original stack order
    	constructStack(src,stackCode);

    }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	GounkiMovespec m = (GounkiMovespec)mm;

        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_DEPLOYSTEP:
        	{
        	GounkiCell src = getSource();
        	GounkiCell dest = getDest();
        	GounkiCell dest2 = getCell(m.to_col,m.to_row);
        	if(replay!=replayMode.Replay)
        	{	for(int lim=dest.height();lim>0;lim--)
        		{
        		animationStack.push(dest);
        		animationStack.push(dest2);
        		}
        	}
         	int undo = stackDescription(dest);	// describe the current stack for undo
        	int startingHeight = deploymentSize--;
        	//G.print("Deploy "+deploymentSize+" "+m);
        	int d2height = dest2.height();
        	boolean prev_is_square = isSquareDeployment(src,dest);	// the user took a first step, in either diagonal or square direction
        	moveStack(dest,dest2,startingHeight);					// move the whole stack to the next space in the deployment
        	GounkiChip ch = dest2.removeChipOfType(prev_is_square,d2height);	// extract the one needed by the previous step move
        	m.undoInfo = ((undo*STACKHEIGHTLIMIT)+deploymentSize)*100;
        	m.state = board_state;
        	dest.addChip(ch);										// put it back on the intermediate square
        	pickedSourceStack.push(dest);
        	droppedDestStack.push(dest2);
        	cellsForCurrentMove.push(dest2);
        	dropMoveStack.push(m);
        	setState(GounkiState.DEPLOY2_STATE);
        	boolean finalDeploy = (deploymentSize==1) || (getDests().size()==0);
        	// if we're down to a single chip we're done, or if this is a bounce that ends the dest here
        	if(finalDeploy) { setState(GounkiState.CONFIRM_STATE); } 
        	}
        	break;
        case MOVE_DEPLOY:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
    			case PLAY_STATE:
    				GounkiCell src = getCell(GounkiId.BoardLocation, m.from_col, m.from_row);
    				m.chip = src.topChip();
        			GounkiCell d1 = getCell(GounkiId.BoardLocation,m.to_col,m.to_row);
        			GounkiCell d2 = getCell(GounkiId.BoardLocation,m.to_col2,m.to_row2);
        			GounkiCell d3 = (m.to_col3>='A')?getCell(GounkiId.BoardLocation,m.to_col3,m.to_row3):null;
    				m.undoInfo += 100*doDeploy(src,d1,d2,d3,replay);
    				setState(GounkiState.CONFIRM_STATE);
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
        			G.Assert((pickedObject==null),"nothing should be moving");
        			GounkiCell src = getCell(GounkiId.BoardLocation, m.from_col, m.from_row);
        			GounkiCell dest = getCell(GounkiId.BoardLocation,m.to_col,m.to_row);
        			int siz = src.height();
                   	if(replay!=replayMode.Replay)
                	{
                		animationStack.push(src);
                		animationStack.push(dest);
                	}
         			m.undoInfo += 100*(doCaptures(dest,replay)*STACKHEIGHTLIMIT+siz);		// save the number of captures 
         			m.chip = src.topChip();
        			moveStack(src,dest,siz);
        			cellsForCurrentMove.push(src);
        			cellsForCurrentMove.push(dest);
       			
       				setState(GounkiState.CONFIRM_STATE);
       			 	break;
        	}
        	break;
        case MOVE_DROPB:
        	{
        	G.Assert(pickedObject!=null,"something is moving");
        	GounkiCell c = getCell(m.source, m.to_col, m.to_row);
        	if(isSource(c)) 
        		{ unPickObject(); 
      		  	  if(cellsForCurrentMove.size()>0) { cellsForCurrentMove.pop(); }
        		}
        	else 
        	{ GounkiCell src = getSource();
          	  switch(board_state)
         	  {
         	  default: 
         	  case PUZZLE_STATE:
         		  dropObject(c,m);
         		  cellsForCurrentMove.push(c);
         		  acceptPlacement();
         		  break;
         	  case PLAY_STATE:
         	  	{ 
              	  src.addChip(pickedObject);
            	  chipsOnBoard[whoseTurn]++;
            	  pickedObject = null;
            	  int siz = src.height();
	              if(replay!=replayMode.Replay)
	            	{	for(int lim=src.height();lim>0;lim--)
	            		{
	            		if(replay==replayMode.Single)
	            		{
	            		animationStack.push(src);
	            		animationStack.push(c);
	            		}}
	            	}
            	  m.undoInfo = 100*((doCaptures(c,replay)*STACKHEIGHTLIMIT)+siz);		// save the number of captures 
            	  m.state = board_state;
            	  moveStack(src,c,siz);
            	  droppedDestStack.push(c);
            	  dropMoveStack.push(m);
            	  cellsForCurrentMove.push(c);
            	  deploymentSize = siz;
            	  

         		  if(hasDeployMovesStarting(src,c))
	        	  {	// if this is a stack, and it moved only one space
	        		  setState(hasMoveMovesStarting(src,c)?GounkiState.DEPLOY_STATE:GounkiState.DEPLOY_ONLY_STATE);
	        	  }
         		  else { setState(GounkiState.CONFIRM_STATE); }
         	  }}
        	}}

            break;
        case MOVE_PICKB:
        	{
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	GounkiCell c = getCell(m.from_col,m.from_row);
        	if(isDest(c))
        		{ unDropObject(); 
        		  if(cellsForCurrentMove.size()>0) { cellsForCurrentMove.pop(); }
        		}
        	else 
        		{ pickObject(c);
        		  m.chip = pickedObject;
     		   	  cellsForCurrentMove.push(c);
     		   	  if(board_state==GounkiState.PLAY_STATE)
        		  {int typecode = c.stackType(pickedObject);
        		   moveMoves.clear();
        		   deployMoves.clear();
        		   getMoveMoves(typecode,moveMoves,c,whoseTurn,false);
        		   getDeployMoves(typecode,deployMoves,c,whoseTurn);
        		  }
         		}
        	}
            break;

        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            if(pickedObject!=null) { unPickObject(); }
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(replay); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?GounkiState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
    		setState(GounkiState.PUZZLE_STATE);

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
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting undoInfo %s", board_state);
         case CONFIRM_STATE:
         case DRAW_STATE:
         case PLAY_STATE: 
         case DEPLOY_STATE:
         case DEPLOY2_STATE:
         case DEPLOY_ONLY_STATE:
		 case GAMEOVER_STATE:
		 case RESIGN_STATE:
			return(false);
         case PUZZLE_STATE:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }
  
    public boolean LegalToHitBoard(GounkiCell c,Hashtable<GounkiCell,GounkiMovespec>dests)
    {	
        switch (board_state)
        {
        case DEPLOY2_STATE:
        case DEPLOY_ONLY_STATE:
        case DEPLOY_STATE:
        	if(pickedObject==null)
        	{	if(isDest(c)) { return(true); }
        		if(dests.get(c)!=null) { return(true); }
        		return(false);
        	}
        	else
        	{	
        	}
        	return(false);
 		case PLAY_STATE:
 			if(pickedObject==null) 
 				{ if(isDest(c)) { return(true); }
 				  GounkiChip ch = c.topChip(); 
 				  return((ch!=null) && (playerIndex(ch)==whoseTurn));
 				}
 			else 
 			{	if(isSource(c)) { return(true); }
 				return(dests.get(c)!=null);
 			}
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	return(pickedObject==null?(c.chipIndex>=0):true);
        }
    }

 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(GounkiMovespec m)
    {	m.undoInfo = 0;
        m.state = board_state; //record the starting undoInfo. The most reliable
        // to undo undoInfo transistions is to simple put the original undoInfo back.
        //G.print("R "+m);
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

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
    private void undoDeploy(GounkiMovespec m)
    {
		GounkiCell src = getCell(GounkiId.BoardLocation, m.from_col, m.from_row);
		GounkiCell d1 = getCell(GounkiId.BoardLocation,m.to_col,m.to_row);
		GounkiCell d2 = getCell(GounkiId.BoardLocation,m.to_col2,m.to_row2);
		GounkiCell d3 = (m.to_col3>='A')?getCell(GounkiId.BoardLocation,m.to_col3,m.to_row3):null;
		undoDeploy(src,d1,d2,d3,m.undoInfo/100);
    }

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(GounkiMovespec m)
    {
        //G.print("U "+m);

        switch (m.op)
        {
        default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
            break;
        case MOVE_DEPLOY:
        	{	undoDeploy(m);
				acceptPlacement();
			}
        	break;
        case MOVE_BOARD_BOARD:
        	{	
        		GounkiCell src = getCell(GounkiId.BoardLocation, m.from_col, m.from_row);
    			GounkiCell d1 = getCell(GounkiId.BoardLocation,m.to_col,m.to_row);
    			int undo = m.undoInfo/100;
    			int cap = undo/STACKHEIGHTLIMIT;
    			int siz = undo%STACKHEIGHTLIMIT;
    			moveStack(d1,src,siz);
    			undoCaptures(d1,cap);
       		    acceptPlacement();
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 void moveInDirection
 	(int startingHeight,	// height of the starting stack
 	 int steps,				// max steps to take
 	 GounkiCell cc,			// the original starting cell
 	 GounkiCell next,		// the current starting cell
 	 int dir,				// the direction to step
 	 int bounceDir,			// complementary direction to bounce, or -1
 	 CommonMoveStack all,	// list of moves being built
 	 int who)				// player that is moving
 {	GounkiCell dest = next.exitTo(dir);
 	// if we stepped off the board, consider bouncing.  This is actually effective
 	// only for rounds, and only if we've already take a step in the current direction
 	if((dest==null) && (bounceDir>=0) && (cc!=next)) 
 		{ dir = bounceDir;
 		  bounceDir=-1; 
 		  dest = next.exitTo(dir); 
 		}
 	// don't allow "move" moves into the first and last rows.  It's never
 	// necessary to move into these rows, and avoiding them means we don't
 	// have to check them for win or consider them in searches.
 	if((dest!=null) && (dest.row!=1) && (dest.row!=boardRows))
 		{ // the starting cell is vacant
 		  GounkiChip top = (dest==cc) ? null : dest.topChip();
 		  if((top==null)					// empty 
 				  || (playerIndex(top)!=who)	// a capturing move
 				  || (((dest.height()+startingHeight))<=3)	// stacking move that won't be too tall
 				  )
 		  	{ all.push(new GounkiMovespec(cc.col,cc.row,dest.col,dest.row,who));
 		  	}
 		  if((top==null) && (steps>1) && (dest.topChip()==null))
 		  	{ moveInDirection(startingHeight,steps-1,cc,dest,dir,bounceDir,all,who); 
 		  	}
 		}
 }
 private boolean canDeployOn(GounkiCell next,int who,GounkiCell d1,GounkiCell d2)
 {
	 if(next==null) { return(false); }
	 GounkiChip top = next.topChip();
	 if(top==null) { return(true); }
	 if(playerIndex(top)==who) { return(next.height()<=2-((d1==next)?1:0)-((d2==next)?1:0)); }
	 return(false);
 }
 //
 // generate deployment moves.  step in the initial direction and bounce.
 // if the new cell is empty, continue recursively in the same direction
 // until we've used all the chips of the current type.  Then switch types
 // and continue.  At the bottom, when fully deployed, generate the actual move.
 //
 void deployStepInDirection(
		 int rounds,	// multiplier * rounds in the stack
		 int squares,	// multiplier * squares in the stack
		 int chip,		// the chip we're deploying
		 GounkiCell s,	// source cell,
		 GounkiCell f,	// from here originally in this direction
		 GounkiCell f1,	// from here
		 GounkiCell d1,	// first dest
		 GounkiCell d2,	// second dest
		 GounkiCell d3,	// third dest
		 int direction,	// the direction we're moving
		 int bouncedir,	// the bounce direction or -1
		 CommonMoveStack all,
		 int who)
 {	GounkiCell next = f1.exitTo(direction);
 	if((next==null) && (bouncedir>=0) && (f1!=f)) { next = f1.exitTo(bouncedir); direction = bouncedir; bouncedir=-1; }
 	if((next!=null) && (next==s)	// squares can bounce back to where they came from
 				? true				// the source cell is empty
 				: canDeployOn(next,who,d1,d2))
	{	// deployment is possible in this direction. Set the next dest in line
 		if(d1==null) { d1 = next; }
 		else if(d2==null) { d2 = next; }
 		else if(d3==null) { d3 = next; }
 		else { throw G.Error("Not expected"); }
 		
 		if(chip==rounds)
 			{	// last round, switch deploytype
 				if(squares==0) 
 				{	// end of the line, make a move
 					all.push(new GounkiMovespec(s,d1,d2,d3,who));
 				}
 				else
 				{	// switch to squares, try each of the three new directions
 					deployStepInDirection(0,squares,GounkiCell.squareCode,s,next,next,d1,d2,d3,forward[who],-1,all,who);
 					deployStepInDirection(0,squares,GounkiCell.squareCode,s,next,next,d1,d2,d3,CELL_LEFT,CELL_RIGHT,all,who);
 					deployStepInDirection(0,squares,GounkiCell.squareCode,s,next,next,d1,d2,d3,CELL_RIGHT,CELL_LEFT,all,who);
				}
 			}
 			else if(chip==squares)
 			{	// last square, switch deploy type
 				if(rounds==0)
 				{	// end of the line, make a move
					all.push(new GounkiMovespec(s,d1,d2,d3,who));
				}
 				else
 				{	// switch to rounds, try both of the new directions and allow bounces
					deployStepInDirection(rounds,0,GounkiCell.roundCode,s,next,next,d1,d2,d3,forward_left[who],forward_right[who],all,who);
					deployStepInDirection(rounds,0,GounkiCell.roundCode,s,next,next,d1,d2,d3,forward_right[who],forward_left[who],all,who);
				}
 			}
 			else if(chip==GounkiCell.roundCode)
 			{	// continue deploying rounds in the same direction
 				rounds -= chip;
				deployStepInDirection(rounds,squares,chip,s,f,next,d1,d2,d3,direction,bouncedir,all,who);
			}
 			else if(chip==GounkiCell.squareCode)
 			{	// continue deploying squares in the same direction
 				squares -= chip;
 				deployStepInDirection(rounds,squares,chip,s,f,next,d1,d2,d3,direction,bouncedir,all,who);
 			}
 			else { throw G.Error("not expected"); }
 		}
 }
 
 //
 // generate deploy moves with a certain number of rounds and squares in the stack at c
 //
 void deployStack(int rounds,int squares,int initialchip,GounkiCell c,CommonMoveStack all,int who)
 {	
 	switch(initialchip)
	 {
	 	default: throw G.Error("Not expecting type code %s",initialchip);
	 	case GounkiCell.roundCode:
	 		deployStepInDirection(rounds,squares,initialchip,c,c,c,null,null,null,
	 				forward_left[who],forward_right[who],all,who);
	 		deployStepInDirection(rounds,squares,initialchip,c,c,c,null,null,null,
	 				forward_right[who],forward_left[who],all,who);
	 		break;
	 	case GounkiCell.squareCode:
 			deployStepInDirection(rounds,squares,initialchip,c,c,c,null,null,null,
 				forward[who],-1,all,who);
			deployStepInDirection(rounds,squares,initialchip,c,c,c,null,null,null,
	 				CELL_RIGHT,CELL_LEFT,all,who);
			deployStepInDirection(rounds,squares,initialchip,c,c,c,null,null,null,
	 				CELL_LEFT,CELL_RIGHT,all,who);
		break;
	 }
 }
 // true if there are deploy moves that start with from-to as the first step
 private boolean hasMoveMovesStarting(GounkiCell from,GounkiCell to)
 {	for(int lim=moveMoves.size()-1;lim>=0;lim--)
 	{
	 GounkiMovespec m = (GounkiMovespec)moveMoves.elementAt(lim);
	 if((m.from_col==from.col) && (m.from_row==from.row) && (m.to_col==to.col) && (m.to_row==to.row))
	 {	return(true);
	 }
 	} 
 	return(false);
 } 
 // true if there are deploy moves that start with from-to as the first step
 private boolean hasDeployMovesStarting(GounkiCell from,GounkiCell to)
 {	for(int lim=deployMoves.size()-1;lim>=0;lim--)
 	{
	 GounkiMovespec m = (GounkiMovespec)deployMoves.elementAt(lim);
	 if((m.from_col==from.col) && (m.from_row==from.row) && (m.to_col==to.col) && (m.to_row==to.row))
	 {	return(true);
	 }
 	} 
 	return(false);
 }
 public CommonMoveStack deployMovesContaining(GounkiCell dest,GounkiCell to)
 {	CommonMoveStack val = new CommonMoveStack();
 	for(int lim=deployMoves.size()-1;lim>=0;lim--)
 	{
	 GounkiMovespec m = (GounkiMovespec)deployMoves.elementAt(lim);
	 if( (((m.to_col==dest.col) && (m.to_row==dest.row))
			 || ((m.to_col2==dest.col) && (m.to_row2==dest.row)))
			 &&( ((m.to_col2==to.col) && (m.to_row2==to.row))
					 || ((m.to_col3==to.col) && (m.to_row3==to.row))))
	 {	val.push(m);
	 }
 	} 
 	return(val);
 }

 public Hashtable<GounkiCell,GounkiMovespec> getDests()
 {	Hashtable<GounkiCell,GounkiMovespec>val = new Hashtable<GounkiCell,GounkiMovespec>();
 	if(pickedObject!=null)
	 {switch(board_state)
	 	{
	 	default: break;
	 	case PLAY_STATE:
	 		// include the first step of all deployment moves.  In unusual circumstances
	 		// you can start a deploy by making a stack more than 3 high
	 	 	for(int lim=deployMoves.size()-1; lim>=0; lim--)
	 	 	{	GounkiMovespec m = (GounkiMovespec)deployMoves.elementAt(lim);
	 	 		GounkiCell d = getCell(GounkiId.BoardLocation,m.to_col,m.to_row);
	 	 		val.put(d,m);
	 	 	}
	 	 	for(int lim=moveMoves.size()-1; lim>=0; lim--)
	 	 	{	GounkiMovespec m = (GounkiMovespec)moveMoves.elementAt(lim);
	 	 		GounkiCell d = getCell(GounkiId.BoardLocation,m.to_col,m.to_row);
	 	 		val.put(d,m);
	 	 	}
	 	 	break;
	 	 }
	 }
 	else
 	{
 		switch(board_state)
 		{
 		default: break;
	 	case DEPLOY2_STATE:
	 	 	for(int lim=deployMoves.size()-1; lim>=0; lim--)
	 	 	{	GounkiMovespec m = (GounkiMovespec)deployMoves.elementAt(lim);
	 	 		GounkiCell d = getCell(GounkiId.BoardLocation,m.to_col,m.to_row);
	 	 		GounkiCell d2 = getCell(GounkiId.BoardLocation,m.to_col2,m.to_row2);
	 	 		if((d.topChip()!=null) && isDest(d2))
	 	 		{	if(m.to_col3>='A')
	 	 			{	GounkiCell d3 = getCell(GounkiId.BoardLocation,m.to_col3,m.to_row3);
	 	 				val.put(d3,m);
	 	 			}
	 	 		}
	 	 	}
	 	 	break;
	 	case DEPLOY_ONLY_STATE:
	 	case DEPLOY_STATE:
	 	 	for(int lim=deployMoves.size()-1; lim>=0; lim--)
	 	 	{	GounkiMovespec m = (GounkiMovespec)deployMoves.elementAt(lim);
	 	 		GounkiCell d = getCell(GounkiId.BoardLocation,m.to_col,m.to_row);	
	 	 		if(isDest(d))
	 	 		{	GounkiCell d2 = getCell(GounkiId.BoardLocation,m.to_col2,m.to_row2);
	 	 			val.put(d2,m);
	 	 			if(m.to_col3>='A')
	 	 			{	GounkiCell d3 = getCell(GounkiId.BoardLocation,m.to_col3,m.to_row3);
	 	 				val.put(d3,m);
	 	 			}
	 	 		}
	 	 	}	
	 	 	break;
 		}
 	}
 	return(val);
 }
 
 // get the moves that are simple moves or captures
 void getMoveMoves(int typecode,CommonMoveStack all,GounkiCell c,int who,boolean robot)
 {
 	switch(typecode)
	 {
	 case GounkiCell.emptyCode:	break;
	 
	 case GounkiCell.roundCode*3:	// one round
	 case GounkiCell.roundCode*2:	// one round
	 case GounkiCell.roundCode:	// one round
		 {
		 int steps = typecode/GounkiCell.roundCode;
		 moveInDirection(steps,steps,c,c,forward_left[who],forward_right[who],all,who);
		 moveInDirection(steps,steps,c,c,forward_right[who],forward_left[who],all,who);
		 }
		break;
		
	 case GounkiCell.squareCode*3:
	 case GounkiCell.squareCode*2:
	 case GounkiCell.squareCode:
	 	{	int steps = typecode/GounkiCell.squareCode;
	 		moveInDirection(steps,steps,c,c,forward[who],-1,all,who);
	 		moveInDirection(steps,steps,c,c,CELL_LEFT,robot?-1:CELL_RIGHT,all,who);
	 		moveInDirection(steps,steps,c,c,CELL_RIGHT,robot?-1:CELL_LEFT,all,who);
	 	}
	 	break;
	 case GounkiCell.squareCode+GounkiCell.roundCode:	// 1 square + 1 round
	 	moveInDirection(2,1,c,c,forward[who],-1,all,who);
	 	moveInDirection(2,1,c,c,CELL_LEFT,robot?-1:CELL_RIGHT,all,who);
	 	moveInDirection(2,1,c,c,CELL_RIGHT,robot?-1:CELL_LEFT,all,who);
		moveInDirection(2,1,c,c,forward_left[who],forward_right[who],all,who);
		moveInDirection(2,1,c,c,forward_right[who],forward_left[who],all,who);
		break;
	 case GounkiCell.squareCode+2*GounkiCell.roundCode:	// 1 square + 2 rounds
		 // move as either 2 square or 1 circle
	 	moveInDirection(3,1,c,c,forward[who],-1,all,who);
	 	moveInDirection(3,1,c,c,CELL_LEFT,robot?-1:CELL_RIGHT,all,who);
	 	moveInDirection(3,1,c,c,CELL_RIGHT,robot?-1:CELL_LEFT,all,who);
		moveInDirection(3,2,c,c,forward_left[who],forward_right[who],all,who);
		moveInDirection(3,2,c,c,forward_right[who],forward_left[who],all,who);
		break;
	 case GounkiCell.squareCode*2+GounkiCell.roundCode:	// 2 square + 1 round
		 // move as either 1 square or 2 circle
	 	moveInDirection(3,2,c,c,forward[who],-1,all,who);
	 	moveInDirection(3,2,c,c,CELL_LEFT,robot?-1:CELL_RIGHT,all,who);
	 	moveInDirection(3,2,c,c,CELL_RIGHT,robot?-1:CELL_LEFT,all,who);
		moveInDirection(3,1,c,c,forward_left[who],forward_right[who],all,who);
		moveInDirection(3,1,c,c,forward_right[who],forward_left[who],all,who);
		break;
	 default:
	 }
 	}
 
 // get the moves that are deploying stacks
 void getDeployMoves(int typecode,CommonMoveStack all,GounkiCell c,int who)
 {
	 	switch(typecode)
		 {
		 case GounkiCell.emptyCode:	break;
		 
		 case GounkiCell.roundCode*3:	// one round
		 case GounkiCell.roundCode*2:	// one round
			 deployStack(typecode,0,GounkiCell.roundCode,c,all,who);
			//$FALL-THROUGH$
		case GounkiCell.roundCode:	// one round
			 break;
			
		 case GounkiCell.squareCode*3:
		 case GounkiCell.squareCode*2:
			 deployStack(0,typecode,GounkiCell.squareCode,c,all,who);
			//$FALL-THROUGH$
		case GounkiCell.squareCode:
		 	break;
		 case GounkiCell.squareCode+GounkiCell.roundCode:	// 1 square + 1 round
			deployStack(GounkiCell.roundCode,GounkiCell.squareCode,GounkiCell.squareCode,c,all,who);
			deployStack(GounkiCell.roundCode,GounkiCell.squareCode,GounkiCell.roundCode,c,all,who);
			break;
		 case GounkiCell.squareCode+2*GounkiCell.roundCode:	// 1 square + 2 rounds
			deployStack(GounkiCell.roundCode*2,GounkiCell.squareCode,GounkiCell.squareCode,c,all,who);
			deployStack(GounkiCell.roundCode*2,GounkiCell.squareCode,GounkiCell.roundCode,c,all,who);
			break;
		 case GounkiCell.squareCode*2+GounkiCell.roundCode:	// 2 square + 1 round
			deployStack(GounkiCell.roundCode,GounkiCell.squareCode*2,GounkiCell.squareCode,c,all,who);
			deployStack(GounkiCell.roundCode,GounkiCell.squareCode*2,GounkiCell.roundCode,c,all,who);
			break;
		 default:
		 } }
 

 CommonMoveStack  getListOfMoves(CommonMoveStack all0,int who,boolean robot)
 {	CommonMoveStack all = all0;
 	if(all==null) { all = new CommonMoveStack(); }
 	switch(board_state)
 	{
 	default: break;
 	case PLAY_STATE:
	 	for(GounkiCell c = allCells; c!=null; c=c.next)
	 	{	GounkiChip ch = c.topChip();
	 		if((ch!=null) && (playerIndex(ch)==who))
	 				{
	 				int typecode = c.stackType(pickedObject);
	 				getMoveMoves(typecode,all,c,who,robot);
	 		 		getDeployMoves(typecode,all,c,who);
	 				}
	 	}
	 	break;
 	}
 	return(all);
 }
 CommonMoveStack  GetListOfMoves()
 {	return(getListOfMoves(null,whoseTurn,true));
 }
 
}
