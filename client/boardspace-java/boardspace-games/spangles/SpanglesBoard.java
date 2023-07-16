package spangles;

import java.awt.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;



/**
 * SpanglesBoard knows all about the game of Spangles, which is played
 * on a triangular grid. It gets a lot of logistic support from 
 * common.triBoard, which knows about the coordinate system.  
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

class SpanglesBoard extends triBoard<SpanglesCell> implements BoardProtocol,SpanglesConstants
{	static int REVISION = 101;			// 101 switches to using and expandable board
	public int getMaxRevisionLevel() { return(REVISION); }
	private SpanglesState unresign;
	private SpanglesState board_state;
	public SpanglesState getState() {return(board_state); }
	public void setState(SpanglesState st) 
	{ 	unresign = (st==SpanglesState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public void SetDrawState() {throw G.Error("not expected"); };	// impossible in spangles
    //
    // private variables
    //
    private SpanglesCell occupiedCells = null;
    private int chips_on_board = 0;		// number of chips currently on the board
    private int sweep_counter=0;
	
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public SpanglesChip pickedObject = null;
    public SpanglesChip lastPicked = null;
    public SpanglesCell rack[][] = new SpanglesCell[2][2];	// dummy source for the chip pools
    public SpanglesCell pickedSource = null; 
    public SpanglesCell droppedDest = null;
    public SpanglesChip lastDroppedDest = null;	// for image adjustment logic
    public SpanglesCell lastDroppedCell = null;
    public int oneleg[] = new int[2];			// count of one legged triangles
    public SpanglesCell centerCell() { return(getCell((char)('A'+ncols/2),nrows/4)); }
    public Hashtable<SpanglesCell,SpanglesCell> movingObjectDests()
    {	int n = 0;
    	Hashtable<SpanglesCell,SpanglesCell> h = new Hashtable<SpanglesCell,SpanglesCell>();
    	if((pickedObject!=null)||(occupiedCells==null))
    	{	for(SpanglesCell c = allCells; c!=null; c=c.next)
    		{	if(c.height()>0) 
    			{
    			for(int dir = 0;dir<CELL_FULL_TURN;dir++)
    			{	SpanglesCell d = c.exitTo(dir);
    				if((d!=null) && (d.height()==0))
    				{	n++;
    					h.put(d,d);
    				}
    			}
    			}
    		}
    		if(n==0) 
    			{ SpanglesCell c = centerCell();
    			  h.put(c,c);
    			}
    	}
    	return(h);
    }
	// factory method to generate a board cell
	public SpanglesCell newcell(char c,int r)
	{	return(new SpanglesCell(c,r));
	}
    public SpanglesBoard(String init,int map[]) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = SpanglesGRIDSTYLE;
        revision = REVISION;
        setColorMap(map, 2);
        doInit(init); // do the initialization 
    }
    public SpanglesBoard cloneBoard() 
	{ SpanglesBoard dup = new SpanglesBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((SpanglesBoard)b); }

    private void Init_Standard(String game)
    {	
    	if(Spangles_INIT.equalsIgnoreCase(game)) {   }
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(SpanglesState.PUZZLE_STATE);
        reInitBoard(26,26); //this sets up a hexagonal board
         
        whoseTurn = FIRST_PLAYER_INDEX;
        chips_on_board = 0;
        occupiedCells = null;
        droppedDest = null;
        pickedSource = null;
        lastDroppedDest = null;
        oneleg[0]=oneleg[1]=0;

        // set the initial contents of the board to all empty cells
		for(SpanglesCell c = allCells; c!=null; c=c.next) { c.nextOccupied=null; c.chip=null; }
		
		//for(int i=1;i<ncols;i++)
		//{	getCell((char)('@'+i),1).addChip(SpanglesChip.getChip(i%4));
		//    if(i>1) { getCell('A',i).addChip(SpanglesChip.getChip(i%4));}
		//}
    }

    public void sameboard(BoardProtocol f) { sameboard((SpanglesBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(SpanglesBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the boards
        G.Assert(AR.sameArrayContents(oneleg,from_b.oneleg),"oneleg mismatch");
        // here, check any other state of the board to see if
        G.Assert((chips_on_board == from_b.chips_on_board),"chips On Board matches");
        G.Assert((occupiedCells==from_b.occupiedCells)
        		|| ((occupiedCells!=null) && occupiedCells.sameCell(from_b.occupiedCells)), "Boards not the same");
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
        
        v^=r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        v ^= Digest(r,pickedObject);
        return (v);
    }
    public SpanglesCell getCell(SpanglesCell c)
    {	return(c==null?null:getCell(c.col,c.row));
    }
    // this visitor method implements copying the contents of a cell on the board
    public void copyFrom(SpanglesCell c,SpanglesCell from)
    {	super.copyFrom(c,from);
    	SpanglesCell cc = c;
    	SpanglesCell fc = from;
    	cc.nextOccupied = getCell(fc.nextOccupied);
    }
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(SpanglesBoard from_b)
    {	
    	super.copyFrom(from_b);

        chips_on_board = from_b.chips_on_board;
        occupiedCells = getCell(from_b.occupiedCells);
        droppedDest = getCell(from_b.droppedDest);
        pickedSource = getCell(from_b.pickedSource);
        pickedObject = from_b.pickedObject;
        lastPicked = from_b.lastPicked;
        AR.copy(oneleg,from_b.oneleg);
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        sameboard(from_b); 
    }

    public String gameType() 
    {   
    	if(revision<101) { return gametype; }
    	// the lower case is a secret flag so the "new" parsing can be recognized
    	return(G.concat(gametype.toLowerCase()," ",players_in_game," ",randomKey," ",revision)); 
    }

    public void reInit(String d)
    {
    	revision = 0;
    	doInit(d);
    }
    public void doInitOld(String gtype)
    {
    	doInit(gtype,0,0,0);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	
    
	   StringTokenizer tok = new StringTokenizer(gtype);
	   String typ = tok.nextToken();
	   int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
	   long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
	   int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
	   doInit(typ,np,ran,rev);
    }
    public void doInit(String typ,int np,long ran,int rev)
    {  Random r = new Random(6278734);
	   adjustRevision(rev);
	   G.Assert(np==2,"players can only be 2");
   	   setIsTorus(rev<101);
       randomKey = ran;
       Init_Standard(typ);
       int map[]=getColorMap();
       for(int player=FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
        for(int i=0;i<2;i++)
        {	rack[map[player]][i]=new SpanglesCell(r,SpanglesId.ChipPool,SpanglesChip.getChip(player*2+i));
        }
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
    int playerIndex(SpanglesChip ch) { return(getColorMap()[ch.colorIndex]); }
    
    //This is used to implement the "forced move" optimization, which is particularly easy for spangles.
    // return 0 if this empty position is not a win for either player
    // 		  1 if it is a win for player 0
    //		  2 if it is a win for player 1
    //		  3 if it can be a win for either player
    //
    public int winFromPosition(SpanglesCell c)
    {	int val = 0;
    	//if((c.col=='G')&&(c.row==11)) 
    	//{G.print(""+c);
    	//}
    	if(c.chip==null)
    	{
    	for(int bias=0; bias<CELL_FULL_TURN; bias++)
    	{
    	SpanglesCell down = c.Down(bias);
    	if(down!=null && down.chip!=null)
    		{	SpanglesCell left = down.Left(bias);
    			SpanglesCell right = down.Right(bias);
    			SpanglesChip lt = left.topChip();
    			SpanglesChip rt = right.topChip();
    			if((lt==rt)&&(lt!=null)) { val |= (1<<(playerIndex(lt))); }
    		}
    	}}
    	//G.print(""+c+" "+val);
    	return(val);
    }
    public boolean WinForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	oneleg[player]=0;
    	for(SpanglesCell occ = occupiedCells; occ!=null; occ=occ.nextOccupied)
    	{
    	SpanglesChip top = occ.topChip();
     	if((top!=null) && playerIndex(top)==player)
    	{
     	   	SpanglesCell dl = occ;
        	SpanglesCell dr = occ;
        	do {		// just once around, so only smallest size wins
        	SpanglesCell down = occ.Down();
        	if(down.topChip()==null) { break; }	// must not be empty
        	dl = down.Left();
    		dr = down.Right();
    		SpanglesChip dltop = dl.topChip();
    		SpanglesChip drtop = dr.topChip();
    		if((dltop==null)&&(drtop==null)) 
    			{ if(dl.isIsolated(droppedDest)&&dr.isIsolated(droppedDest)) 
    				{ break; }
    			}
    		if((dltop!=null)
    			&& (drtop!=null))
    		{	boolean left = (playerIndex(dltop)==player);
    			boolean right = (playerIndex(drtop)==player);
    			if(left && right) { return(true); }
    			else if(left || right) { oneleg[player]++; }
    		}	
    		} while(false);
    	}
    	}
    	return(false);
     }



    
    // set the contents of a cell, and maintian the books
    public SpanglesChip SetBoard(SpanglesCell c,SpanglesChip ch)
    {	SpanglesChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(old!=null) 
    		{ chips_on_board--;
    		  SpanglesCell prev = null;
    		  SpanglesCell occ = occupiedCells;
    		  // old had contents, remove from the occupied list
    		  while(occ!=null) 
    		  { if (occ==c) 
    		  	{ if(prev!=null) { prev.nextOccupied = c.nextOccupied; c.nextOccupied=null; }
    		  	else { occupiedCells = c.nextOccupied; c.nextOccupied=null; }
    		  	 occ=null;
    		  	}
    		  else { prev = occ; occ = occ.nextOccupied; }
    		  }
    		}
     	if(ch!=null) { chips_on_board++; c.nextOccupied = occupiedCells; occupiedCells = c; }
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
    	}
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	if(droppedDest!=null) 
    	{	pickedObject = SetBoard(droppedDest,null); 
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
    public boolean isDest(SpanglesCell c)
    {	return(droppedDest==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    { SpanglesChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(SpanglesId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);

        case BoardLocation:
        	{
        	SpanglesCell c = getCell(col,row);
        	boolean wasDest = isDest(c);
        	unDropObject(); 
        	if(!wasDest)
        	{
            pickedSource = c;
            lastPicked = pickedObject = c.topChip();
         	droppedDest = null;
         	SetBoard(c,null);
        	}}
            break;

        case ChipPool:
			if(pickedObject!=null) { unPickObject();  }
			
			SpanglesCell src = rack[getColorMap()[row/2]][row&1];
			lastPicked = pickedObject = src.topChip();

            break;

        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(SpanglesCell c)
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
        	if(droppedDest==null)
        	{setNextStateAfterDone();
        	}
        	break;
        case PLAY_STATE:
			setState(SpanglesState.CONFIRM_STATE);
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
    		setState(SpanglesState.PLAY_STATE);
    		break;
    	}

    }
    private void doDone()
    {
        acceptPlacement();

        if (board_state==SpanglesState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(SpanglesState.GAMEOVER_STATE);
        }
        else
        {	if(WinForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(SpanglesState.GAMEOVER_STATE); 
        		}
        	else {setNextPlayer();
        		setNextStateAfterDone();
        	}
        }
    }
    public void setBoardCell(char col,int row,int player)
    {
      	SpanglesCell c =getCell(col,row);
		createExitCells(c);
       	int direction = col&1;
       	SpanglesChip ch =  rack[player][direction].topChip();
       	lastDroppedDest = ch;
       	lastDroppedCell = c;
       	SetBoard(c,ch);
        droppedDest = c;
        pickedObject = null;
    }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	Spanglesmovespec m = (Spanglesmovespec)mm;
 
      //System.out.println("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
       case MOVE_DONE:

         	doDone();

            break;

        case MOVE_DROPB:
			switch(board_state)
			{ case PUZZLE_STATE: acceptPlacement(); break;
			  case CONFIRM_STATE: unDropObject(); unPickObject(); break;
			  case PLAY_STATE:
			   acceptPlacement(); break;
			default:
				break;
			}
			setBoardCell(m.to_col,m.to_row,m.object);
			setNextStateAfterDrop();

            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
             // fall through
			//$FALL-THROUGH$
		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	pickObject(m.source, m.to_col, m.to_row);
        	if(board_state==SpanglesState.CONFIRM_STATE) 
        		{ setState(SpanglesState.PLAY_STATE);
        		}
            break;

        case MOVE_DROP: // drop on chip pool;
            pickedObject = null;
            //setNextStateAfterDrop();

            break;


 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            int nextp = nextPlayer[whoseTurn];
            setState(SpanglesState.PUZZLE_STATE);
            if((win[whoseTurn]=WinForPlayerNow(whoseTurn))
               ||(win[nextp]=WinForPlayerNow(nextp)))
               	{ setState(SpanglesState.GAMEOVER_STATE); 
               	}
            else {  setNextStateAfterDone(); }

            break;

       case MOVE_RESIGN:
            setState(unresign==null?SpanglesState.RESIGN_STATE:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(SpanglesState.PUZZLE_STATE);

            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(SpanglesState.GAMEOVER_STATE);
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
        case CONFIRM_STATE:
        	return(false);
        case PLAY_STATE:
        	// you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
            return (true);
        }
    }

    public boolean LegalToHitBoard(SpanglesCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case PLAY_STATE:
			return((c.chip==null)&&((chips_on_board==0)||!c.isIsolated(droppedDest)));
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
			return((c.chip==null) ? !c.isIsolated(droppedDest) : isDest(c));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
            return ((pickedObject!=null) 
            		  ? (c.topChip()==null)
            		  : (c.topChip()!=null));
        }
    }
    
    StateStack robotState = new StateStack();
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Spanglesmovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
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
    public void UnExecute(Spanglesmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
        default:
   	    	cantExecute(m);
        	break;

        case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
        	SetBoard(getCell(m.to_col,m.to_row),null);
        	break;
        case MOVE_RESIGN:
            

            break;
        }

        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
    
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	if(chips_on_board==0)
 	{	all.addElement(new Spanglesmovespec(MOVE_DROPB,(char)('A'+ncols/2),nrows/2,whoseTurn));
 	}
 	else
 	{
 		int myWinMask = 1<<whoseTurn;
 		boolean single = (occupiedCells!=null) && (occupiedCells.nextOccupied==null);
 		Spanglesmovespec oppwin = null;
 		sweep_counter++;
 		for(SpanglesCell c = occupiedCells;
 			c!=null;
 			c=c.nextOccupied)
 		{
 		G.Assert(c.topChip()!=null,"occupied");
 		for(int dir=0;dir<CELL_FULL_TURN;dir++)
 		{	SpanglesCell nx = c.exitTo(dir);
 			int nsweep = nx.sweep_counter;
 			nx.sweep_counter = sweep_counter;
 			if((nsweep!=sweep_counter) && (nx.topChip()==null))
 			{	int wins = winFromPosition(nx);
 				//wins is used to implement the forced move optimization, which is particularly easy for spangles.
 				//if we have any winning moves, return only that move.  If the opponent has any winning moves, return
 				//only that move (which is the only block for the win).  So in forced move situations, only one
 				//move is returned as the list of all possible moves.
 				Spanglesmovespec newmove = new Spanglesmovespec(MOVE_DROPB,nx.col,nx.row,whoseTurn);
 				if(wins==0)
 				{	all.addElement(newmove);
 					if(single) { return(all); }
 				}
 				else if((myWinMask&wins)!=0)
 				{	// a win for us is all we need to consider
 					all.setSize(0);
 					all.addElement(newmove);
 					return(all);
 				}
 				else if(wins!=0)
 				{	oppwin = newmove;
 				}
 			}
 		}
 		}
		if(oppwin!=null)
 		{	all.setSize(0);
 			all.addElement(oppwin);	// preventing the opponent win is all we can do
 		}
 
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
