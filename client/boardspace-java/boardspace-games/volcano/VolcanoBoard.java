package volcano;


import java.awt.Color;
import java.awt.Point;

import java.util.Hashtable;

import online.game.*;

import lib.*;


/**
 * VolcanoBoard knows all about the game of Volcano, which is played
 * on a 5x5 rectangular board or a 4x hexagonal board. It gets a lot of logistic 
 * support from common.rectBoard and hexboard, which know about the
 * coordinate system.  
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

class VolcanoBoard extends BaseBoard implements BoardProtocol,VolcanoConstants
{	gBoard<VolcanoCell> rboard=null;		// the generic hex/square board object
	private VolcanoState unresign;
	private VolcanoState board_state;
	public VolcanoState getState() {return(board_state); }
	public void setState(VolcanoState st) 
	{ 	unresign = (st==VolcanoState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	// with some care to use cell.geometry instead of 8 or 6, all the logic
	// for the game works the same.  The 
	public gBoard<VolcanoCell> getRealBoard() { return(rboard); }
    public Point encodeCellPosition(int x,int y,double cellsize) 
    	{ return(rboard.encodeCellPosition(x,y,cellsize)); }
    public Point decodeCellPosition(int x,int y,double cellsize) 
    	{ return(rboard.decodeCellPosition(x,y,cellsize)); }
    CellStack animationStack = new CellStack();
    boolean hexBoard = false;
	int colorsToWin = 5;
    public VolcanoCell captures[] = new VolcanoCell[NUMPLAYERS];	// pieces captured per player
    public int capture_counts[][][] = new int[NUMPLAYERS][Pyramid.nColors][Pyramid.nSizes];
    public int nonuniform = 0;
    public int capture_size[] = new int[Pyramid.nSizes];
    public boolean gameOver[] = new boolean[NUMPLAYERS]; 
    public int colorSet[] = new int[NUMPLAYERS];
    public int scoreForPlayer(int who)
    {	int cc[][] = capture_counts[who];
    	int count_size[] = capture_size;
     	int matched_nests = 0;
     	int unmatched_nests = 0;
     	int other = 0;
        int nColors=0;
    	boolean all_colors = true;
     	// count the full nests, accumulate the leftover pieces
     	for(int size=0;size<count_size.length;size++) { count_size[size]=0; }
     	for(int color=0;color<colorsToWin;color++)
     	{	// 5 colors for a square grid, 5 for a hexagonal grid
     		int sizes[]=cc[color];
     		int mincount=sizes[0];
     		int tcount = mincount;
     		for(int size=1;size<sizes.length;size++) 
     			{int ss = sizes[size];
     			 tcount+=ss;
     			 mincount=Math.min(ss,mincount); 
     			}
     		if(tcount>0) 
     			{ nColors++; 
     			}
     			else 
     			{ all_colors = false; 
     			}
     		matched_nests+= mincount;
     		for(int size=0;size<sizes.length;size++) { count_size[size]+=sizes[size]-mincount; }
     	}
    	int mincount = count_size[0];
    	int allcount = mincount;
    	for(int size=1;size<count_size.length;size++) 
    		{allcount+=count_size[size];
    		 mincount=Math.min(count_size[size],mincount); 
    		}
    	
    	// count how many different sizes are in the "other" category.
    	nonuniform=0;
    	for(int i=0;i<count_size.length;i++)
    	{	if(count_size[i]>mincount) { nonuniform++; }
    	}
    	
    	unmatched_nests += mincount;
    	other = allcount - mincount*count_size.length;
    	capture_size[0] = matched_nests;
    	capture_size[1] = unmatched_nests;
    	capture_size[2] = other;
    	gameOver[who] = all_colors;
    	colorSet[who] = nColors;
     	return(matched_nests*7+unmatched_nests*5+other);
    }
    //
    // private variables
    //
    

    public void SetDrawState() { setState(VolcanoState.DRAW_STATE); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public Pyramid pickedObject = null;			// the object in the air
    private VolcanoCell pickedSource[] = new VolcanoCell[5];
    private VolcanoCell droppedDest[]=new VolcanoCell[5];
    private VolcanoCell lastErupted[]=new VolcanoCell[5];
    private int restPath[]=new int[5];
    public int stackIndex = 0;
    private VolcanoCell getSource() { return(pickedSource[stackIndex]); }
    private int[] extend_copy_int(int[]from)
    { 	int c[]=new int[Math.max(stackIndex,from.length)+10];
       	for(int i=0;i<from.length;i++) { c[i]=from[i]; }
       	return(c);
   }
    private VolcanoCell[]extend_copy(VolcanoCell[]from)
    {	VolcanoCell c[]=new VolcanoCell[Math.max(stackIndex,from.length)+10];
    	for(int i=0;i<from.length;i++) { c[i]=from[i]; }
    	return(c);
    }
    private void extend_move_stack()
    {	stackIndex++;
    	if(stackIndex>=pickedSource.length)
    	{	pickedSource=extend_copy(pickedSource);
    		droppedDest=extend_copy(droppedDest);
    		lastErupted=extend_copy(lastErupted);
    		restPath=extend_copy_int(restPath);
    	}
    }
    public int movingObjectIndex()
    {	Pyramid ch = pickedObject;
    	if((ch!=null)&&(droppedDest[stackIndex]==null))
    		{ return(ch.pieceNumber());
    		}
        return (NothingMoving);
    }
    
    // add a chip to the board, perform bookkeeping
    private Pyramid addChip(VolcanoCell c,Pyramid chip)
    {	c.addChip(chip);	
       	return(chip);
    }
    // add a chip to the board, perform bookkeeping
    private Pyramid removeChip(VolcanoCell c)
    {	Pyramid chip = c.removeTop();
       	return(chip);
    }  
    /** get all the cells in the current move stack.  This is used by the
     * viewer to present a move trail
     */
    public Hashtable<VolcanoCell,String> stackOrigins()
    {	Hashtable<VolcanoCell,String> h = new Hashtable<VolcanoCell,String>();
    	for(int i=0;i<=stackIndex;i++)
    		{ VolcanoCell c = pickedSource[i]; 
    			if(c!=null) 
    			{ h.put(c,""+(i+1)); 
    			}
    			VolcanoCell d = droppedDest[i];
    		  	if(d!=null)
    		  	{ h.put(d,""+(i+2));
    		  	}
    		}
    	return(h);
    }
    private void finalizePlacement()	// finalize placement, no more undo
    {	while(stackIndex>=0)
    	{   pickedObject = null;
        	droppedDest[stackIndex]  = null;
        	pickedSource[stackIndex]  = null;
        	lastErupted[stackIndex]  = null;
        	stackIndex--;
    	}
    	stackIndex = 0;
    }   
    
    // undo the eruption which followed a pick/drop
    private void undoEruption()
    {
    	VolcanoCell cap = lastErupted[stackIndex];
    	if(cap!=null)
    	{
    	VolcanoCell src = pickedSource[stackIndex];
    	VolcanoCell dest = droppedDest[stackIndex];
    	lastErupted[stackIndex]=null;
    	int direction = rboard.findDirection(cap.col,cap.row,dest.col,dest.row);
    	while(cap!=dest)
    		{
    		Pyramid p = cap.removeTop();
    		src.addChip(p);
    		cap = cap.exitTo(direction);
    		}
    	}
    }
    // get a Hashtable of cells where the top will be captured.
    // for the user interface
    public Hashtable<VolcanoCell,VolcanoCell> getCaptures()
    {	Hashtable<VolcanoCell,VolcanoCell> h = new Hashtable<VolcanoCell,VolcanoCell>();
	    if(stackIndex>0)
		{
		VolcanoCell cap = lastErupted[stackIndex-1];
		if(cap!=null)
		{
		VolcanoCell dest = droppedDest[stackIndex-1];
		lastErupted[stackIndex]=null;
		int direction = rboard.findDirection(cap.col,cap.row,dest.col,dest.row);
		while(cap!=dest)
			{
			if(cap.sameTopSize()) 
				{  h.put(cap,cap);
				}
			cap = cap.exitTo(direction);
			}
		}
		}	
	    return(h);
    }
    private void addToCaptures(int who,Pyramid p)
    {	captures[who].addChip(p);
    	capture_counts[who][p.colorIndex][p.sizeIndex]++;
    }
    private void removeFromCaptures(int who)
    {	VolcanoCell cap = captures[who];
    	Pyramid p = cap.removeTop();
    	pickedSource[stackIndex] = cap;
    	pickedObject = p;
    	capture_counts[who][p.colorIndex][p.sizeIndex]--;
     }
    private Pyramid removeFromCapturesSimple(int who)
    {	VolcanoCell cap = captures[who];
    	Pyramid p = cap.removeTop();
    	capture_counts[who][p.colorIndex][p.sizeIndex]--;
    	return(p);
    }
    // perform the actual captures, and encode the undo infomation as an integer.
    // there will never be more than 4 steps, so this is not hard.
    private int doCaptures()
    {	int undoInfo = 0;
     	int steps=0;
    	if(stackIndex>0)
    	{
    	VolcanoCell cap = lastErupted[stackIndex-1];	//  the end of the eruption
    	if(cap!=null)
    	{
    	VolcanoCell dest = droppedDest[stackIndex-1];
    	lastErupted[stackIndex]=null;
    	int direction = rboard.findDirection(dest.col,dest.row,cap.col,cap.row);
    	do
    	{	dest = dest.exitTo(direction);
    		if(dest.sameTopSize()) 
    			{  Pyramid p = dest.removeTop();
    			   addToCaptures(whoseTurn,p);
    			   undoInfo = undoInfo*10+1;
    			}
    			else 
    			{ undoInfo = undoInfo*10+2; 
    			}
    		steps++;
    		} while(cap!=dest);
    	}}
    	undoInfo = undoInfo*10+steps;
     	return(undoInfo);
    }
    private void undoEruption(VolcanoCell from,VolcanoCell to,int info)
    {
    	int direction = rboard.findDirection(from.col,from.row,to.col,to.row);
     	int steps = info%10;
     	info = info / 10;
     	VolcanoCell next = to;
     	while(steps-- > 0) { next = next.exitTo(direction); }
     	direction += next.geometry.n/2;	// reverse direction
    	while(info!=0)
    	{	int op = info%10;
    		info = info/10;
    		switch(op)
    		{
    		case 1:	// undo a capture
    			{
    			Pyramid p = removeFromCapturesSimple(whoseTurn);
    			from.addChip(p);
    			}
    			break;
    		case 2: // undo a distribution
    			{
    			Pyramid p = next.removeTop();
    			from.addChip(p);
    			}
    			break;
    		default: throw G.Error("bad unwind op "+op);
    		}
    		next = next.exitTo(direction);
    	}
    	from.addChip(to.removeTop());
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {	undoEruption();
    	VolcanoCell dr = droppedDest[stackIndex];
    	if(dr!=null)
    	{
    	droppedDest[stackIndex] =  null;
    	pickedObject = removeChip(dr);
    	}
    }
    // lowest level of uncapture.  "removed" is the most distant cell removed by the capture
    // picked and dropped are the source and destination respectively.  This handles both
    // capture by approach and by withdrawal.

    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	Pyramid po = pickedObject;
    	if(po!=null)
    	{
    	VolcanoCell ps = pickedSource[stackIndex];
       	addChip(ps,po);
        pickedSource[stackIndex]=null;
    	pickedObject = null;
    	}
     }
    
    private void dropOnBoard(VolcanoCell dcell)
    {
    	droppedDest[stackIndex] = dcell;
    	addChip(dcell,pickedObject);
    	pickedObject = null;
    }
    // 
    // drop the floating object.
    //
    private void dropObject(VolcanoId dest, char col, int row)
    {
       G.Assert((pickedObject!=null)&&(droppedDest[stackIndex]==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case First_Player_Captures:
        	addToCaptures(FIRST_PLAYER_INDEX,pickedObject);
        	break;
        case Second_Player_Captures:
        	addToCaptures(SECOND_PLAYER_INDEX,pickedObject);
        	break;
        case BoardLocation: // an already filled board slot.
        	dropOnBoard(getCell(col,row));
         	break;
        }
    }
    private void pickFromCell(VolcanoCell src)
    {  	pickedSource[stackIndex] = src;
    	pickedObject = removeChip(src);
    }

    private void pickObject(VolcanoId source, char col, int row)
    {	G.Assert((pickedObject==null)&&(pickedSource[stackIndex]==null),"ready to pick");
    
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case First_Player_Captures:
        	removeFromCaptures(FIRST_PLAYER_INDEX);
            break;
        case Second_Player_Captures:
        	removeFromCaptures(SECOND_PLAYER_INDEX);
        	break;
        case BoardLocation:
         	{
         	pickFromCell(getCell(col,row));
         	break;
         	}
                	
       }
   }

   
    private VolcanoCell[][]tempDestResource = new VolcanoCell[4][];
    private int tempDestIndex=-1;
    public synchronized VolcanoCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new VolcanoCell[MAXDESTS]);
    	}
    public synchronized void returnTempDest(VolcanoCell[]d) { tempDestResource[++tempDestIndex]=d; }
    public VolcanoCell getCell(char col,int row)
    {
    	return(rboard.getCell(col,row));
    }

    public VolcanoBoard(long random,String init) // default constructor
    {  	doInit(init,random); // do the initialization 
    }
    public VolcanoBoard cloneBoard() 
	{ VolcanoBoard dup = new VolcanoBoard(randomKey,gametype); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((VolcanoBoard)b); }
   // overridable method.  xpos ypos is the desired centerpoint for the text
    // 
    public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
    {   if(Character.isDigit(txt.charAt(0))) { xpos+=cellsize/3; }
    	else { ypos-= cellsize/4; }
    	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
    }

    private void Init_Standard(String game,long key)
    { 	Random r = new Random(737836);
    	boolean israndomized = (Volcano_R_INIT.equalsIgnoreCase(game)||Volcano_HR_INIT.equalsIgnoreCase(game));
    	boolean isRectBoard = (Volcano_INIT.equalsIgnoreCase(game) || Volcano_R_INIT.equalsIgnoreCase(game));
    	boolean isHexBoard = (Volcano_H_INIT.equalsIgnoreCase(game) || Volcano_HR_INIT.equalsIgnoreCase(game));
        if(isRectBoard)
        	{ rboard = new VolcanoRBoard(DEFAULT_BOARDSIZE);
        	  hexBoard = false;
        	  colorsToWin = 5;
        	}
        else if (isHexBoard)
        {	rboard = new VolcanoHBoard();
        	hexBoard = true;
        	colorsToWin=6;
        }
        else { throw G.Error(WrongInitError,game); }
        
        gametype = game;
        randomKey = key;
        setState(VolcanoState.PUZZLE_STATE);
        for(int i=0;i<captures.length;i++) 
        	{ captures[i]=new VolcanoCell(r,i,Capture_Cell[i]); 
        	}
        for(int i=0;i<pickedSource.length;i++) 
        {	pickedSource[i]=null;
        	droppedDest[i]=null;
        	lastErupted[i]=null;
        	restPath[i]=0;
        }
        pickedObject = null;
        stackIndex = 0;
        rboard.allCells.setDigestChain(r);
 		if(israndomized)
		{
		// randomize the color sequences
		Random seed = new Random(randomKey);
		char fcol = 'A';
		char lcol = (char)('A'+rboard.ncols-1);
		int ncols = lcol-fcol+1;
		
		for(int i=0;i<999;i++)
		{	int r1 = seed.nextInt();	//note, don't mess with this random sequence, it will invalidate saved games.
			int r2 = seed.nextInt();
			int r3c = Random.nextInt(seed,ncols);
			int r4c =  Random.nextInt(seed,ncols);
			char col1 = (char)(fcol+r3c);
			char col2 = (char)(fcol+r4c);
			int frow1 = rboard.firstRowInColumn(col1);
			int lrow1 = rboard.lastRowInColumn(col1);
			int frow2 = rboard.firstRowInColumn(col2);
			int lrow2 = rboard.lastRowInColumn(col2);
			int row1 = frow1+Math.abs(r1%(lrow1-frow1+1));
			int row2 = frow2+Math.abs(r2%(lrow2-frow2+1));
			VolcanoCell c1 = getCell(col1,row1);
			VolcanoCell c2 = getCell(col2,row2);
			if((c1.chipIndex>0) && (c2.chipIndex>0)) { c1.swapContents(c2); }
		}}
 		
     whoseTurn = FIRST_PLAYER_INDEX;
      
        
    }
    public void sameboard(BoardProtocol f) { sameboard((VolcanoBoard)f); }
   /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(VolcanoBoard from_b)
    {	super.sameboard(from_b);
    
        for (int i = 0; i < captures.length; i++)
        {
 			G.Assert(captures[i].sameCell(from_b.captures[i]),"captures matches");
        }
        
        for(VolcanoCell c = rboard.allCells,d=from_b.rboard.allCells;
        	c!=null;
        	c=c.next,d=d.next)
        {	G.Assert(c.sameCell(d),"cells match");
        }
        
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject matches");
 
        // here, check any other state of the board to see if
        G.Assert((stackIndex == from_b.stackIndex) , "stackIndex not the same");
        
        for(int i=0;i<=stackIndex;i++)
        {
            G.Assert(VolcanoCell.sameCell(pickedSource[i],from_b.pickedSource[i]),"pickedSource matches");
            G.Assert(VolcanoCell.sameCell(droppedDest[i],from_b.droppedDest[i]),"droppedDest matches");
        }
    }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
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
        
		for(VolcanoCell c = rboard.allCells; c!=null; c=c.next)
		{	v ^= c.Digest(r);
		}
		for(int i=0;i<captures.length;i++) { v ^= captures[i].Digest(r); }
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,getSource());
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		v ^= r.nextLong()*stackIndex;
       return (v);
    }
    private VolcanoCell getCell(VolcanoCell c)
    {	if(c==null) { return(null); }
    	if(c.onBoard) { return( getCell(c.col,c.row)); }
    	return(captures[c.row]);
    	
    }
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(VolcanoBoard from_board)
    {	super.copyFrom(from_board);
        VolcanoBoard from_b = from_board;
 
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        pickedObject = from_b.pickedObject;
        
 		for(int i=0;i<win.length;i++) 
		{  
		   captures[i].copyFrom(from_b.captures[i]);
		   int to_cc[][] = capture_counts[i];
		   int from_cc[][] = from_b.capture_counts[i];
		   // copy the capture counts
		   for(int j=0;j<to_cc.length;j++) 
		   {	int to_cr[]=to_cc[j];
		   		int from_cr[]=from_cc[j];
		   		for(int k=0;k<to_cr.length;k++) { to_cr[k]=from_cr[k]; }
		   }
		}
		
		for(VolcanoCell dest = rboard.allCells,src=from_b.rboard.allCells;
			dest!=null; 
			dest=dest.next,src=src.next)
			{
			dest.copyFrom(src);
			}
        stackIndex = from_b.stackIndex-1;
        extend_move_stack();
        for(int i=0;i<=stackIndex;i++)
        {	lastErupted[i]=getCell(from_b.lastErupted[i]);
        	pickedSource[i]=getCell(from_b.pickedSource[i]);
        	droppedDest[i]=getCell(from_b.droppedDest[i]);
        }
        
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {
       Init_Standard(gtype,key);
       animationStack.clear();
       for(int i=0;i<NUMPLAYERS;i++) 
       	{ 
		   int to_cc[][] = capture_counts[i];
		   // copy the capture counts
		   for(int j=0;j<to_cc.length;j++) 
		   {	int to_cr[]=to_cc[j];
		   		for(int k=0;k<to_cr.length;k++) { to_cr[k]=0; }
		   }
       	}
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
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case PUZZLE_STATE:
            break;
    	case PLAY_STATE:
    	case GAMEOVER_STATE:
    		// some damaged early games need this
    		if(replay==replayMode.Live) { throw G.Error("not expecting nextPlayer in state %s",board_state); }
			//$FALL-THROUGH$
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
    {	if(pickedObject!=null) { return(false); }
    	switch(board_state)
    	{
    	case PLAY_STATE: return(true);
    	default: return(DoneState());
    	}
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(VolcanoState.GAMEOVER_STATE);
    }


    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public double ScoreForPlayer(int player,boolean print,boolean dumbot)
    {  	int sc = scoreForPlayer(player);
    	double pyramid_score = 0.0;
    	int gc=0;
    	int pv=0;
    	double nc = dumbot ? 0.0 : (colorSet[player]/10.0);		// 0 - 0.4
    	double nu = dumbot ? 0.0 : ((nonuniform>1) ? 0.25 : 0.0);
    	for(VolcanoCell c=rboard.allCells;
    	    c!=null;
    	    c=c.next)
    	{	Pyramid p = c.topChip();
    		if((p!=null) && (p.colorIndex==Pyramid.BLACK_PYRAMID)) { gc += c.geometry.n; pv += c.linkCount; }
    	}
       	if(player==whoseTurn)
       		{pyramid_score = (double)pv/(2*gc+1);	//0-0.5
       		 sc += pyramid_score;
       		}
        if((board_state==VolcanoState.GAMEOVER_STATE)&&(player==whoseTurn)&&!win[player]) 
        { sc -= 100;	// avoid draws 
        }
 
       	if(print)
    	{ String dbp = dumbot ? "" : " Colors: "+nc;
    	  if((board_state==VolcanoState.GAMEOVER_STATE) && (!win[player])) { dbp+= " draw=-100"; }
    	  if(nu>0) { dbp += " multi "+nu; }
    	  System.out.println("Solid Nests: "+capture_size[0]
    	                     +" Mixed Nests: "+capture_size[1]
    	                     +" Other: "+capture_size[2]
    	                     +" Caps: "+pyramid_score
    	                     + dbp
    	                     +" = "+sc);
    	}
      	return(sc+pyramid_score+nc+nu);
    }
    


    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterPick(replayMode replay)
    {
        switch (board_state)
        {
        case GAMEOVER_STATE:
        	if(replay!=replayMode.Live) { return; }
			//$FALL-THROUGH$
		default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setState(VolcanoState.PLAY_STATE);
        	break;
        case PLAY_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,true);
        	break;
    	case PLAY_STATE:
    		// some damaged early games need this
    		if(replay==replayMode.Live) { throw G.Error("not expecting done in state %s",board_state); }

			//$FALL-THROUGH$
		case CONFIRM_STATE:
    	case PUZZLE_STATE:
     		setState(VolcanoState.PLAY_STATE);
    		break;
    	}

    }
    private boolean doEndGame()
    {	boolean over = false;
		int score1 = scoreForPlayer(whoseTurn);	// as a side effect, notice gameover
		over = gameOver[whoseTurn];
		if(over)
		  {	int score2 = scoreForPlayer(nextPlayer[whoseTurn]);
		  	setGameOver(score1>score2,score2>score1);
		  }
		return(over);
	}
    private int doDone(replayMode replay)
    {	int undo=-1;
    	if(board_state==VolcanoState.RESIGN_STATE) 
    	{ setGameOver(false,true);
    	}
    	else
    	{
    	switch(board_state)
    	{
    	default: throw G.Error("not expecting done in state %s",board_state);
    		
    	case PUZZLE_STATE: break;
    	case PLAY_STATE:
    	case GAMEOVER_STATE:
    		// some damaged early games need this
    		if(replay==replayMode.Live) { throw G.Error("not expecting done in state %s",board_state); }
			//$FALL-THROUGH$
		case DRAW_STATE:
    	case CONFIRM_STATE:  
    		undo = doCaptures();
    		finalizePlacement();
    		boolean over = (undo>0)	&& doEndGame();
    		if(!over)
    		{
    		setNextPlayer(replay); 
    		setNextStateAfterDone(replay);
    		}
    	}}
       return(undo);
    }
    public boolean doEruption(VolcanoCell from,VolcanoCell to,replayMode replay)
    {	int dir = rboard.findDirection(from.col,from.row,to.col,to.row);
    	int steps = 0;
    	VolcanoCell next = to;
    	VolcanoCell finaldest = null;
    	while ( (from.topChip()!=null) 
    			&&((next=next.exitTo(dir))!=null) 
    			&& !hasVolcanoCap(next))
    	{	Pyramid chip = from.removeTop();
    		steps++;
    		next.addChip(chip);
    		finaldest = next;
    		if(replay!=replayMode.Replay)
    		{
    			animationStack.push(from);
    			animationStack.push(next);
    		}
    	} ;
    	lastErupted[stackIndex]=finaldest;
    	return(steps>0);
    }
    
     public boolean Execute(commonMove mm,replayMode replay)
    {	VolcanoMovespec m = (VolcanoMovespec)mm;
    	if(replay==replayMode.Replay) { animationStack.clear(); }
        m.state = board_state;		// record starting state for undo, also for EditHistory
        m.stackInfo = stackIndex;	// used by robot unwind
       //G.print("E "+m+" for "+whoseTurn+" : "+stackIndex);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(replay);

            break;
        case MOVE_BOARD_BOARD:
        	{
        	VolcanoCell from = getCell(m.from_col, m.from_row);
        	VolcanoCell to = getCell(m.to_col, m.to_row); 
        	pickFromCell(from);
        	dropOnBoard(to);
        	if(replay!=replayMode.Replay)
        	{
        		animationStack.push(from);
        		animationStack.push(to);
        	}
        	if(doEruption(from,to,replay)) { setState(VolcanoState.CONFIRM_STATE); }
        	extend_move_stack();
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
        	VolcanoCell from = pickedSource[stackIndex];
        	VolcanoCell target =  getCell(m.to_col, m.to_row);
        	if(from==target) { unPickObject(); }
        	else
        	{
            dropOnBoard(target);
            if(replay==replayMode.Single)
            {
            	animationStack.push(from);
            	animationStack.push(target);
            }
            switch(board_state)
            {
            default:
            	throw G.Error("Not expecting drop in state %s",board_state);
            case GAMEOVER_STATE:
            	if(replay==replayMode.Live) { throw G.Error("Not expecting drop in state %s",board_state);}
           	
				//$FALL-THROUGH$
			case PUZZLE_STATE:
            	finalizePlacement();
            	break;
            case PLAY_STATE:
            	{
            	VolcanoCell dest = droppedDest[stackIndex];
            	if(doEruption(from,dest,replay))
            	{ setState(VolcanoState.CONFIRM_STATE);
            	}
            	extend_move_stack();
            	}
            }}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary pick
        	switch(board_state)
        	{	
           	case CONFIRM_STATE:
           		stackIndex--;
           		unDropObject();
           		break;
           	default:
        		pickObject(VolcanoId.BoardLocation, m.from_col, m.from_row);
        	}
        	setNextStateAfterPick(replay);
        	 
            break;
         case MOVE_DROP: // drop on chip pool;
            dropObject(m.source, m.to_col, m.to_row);
            finalizePlacement();
            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            // note that this doesn't do what it should, it picks
            // in stack order only, rather than the one asked.
            // TODO: make volcano picking from the racks work correctly
            pickObject(m.source, m.from_col, m.from_row);
            setNextStateAfterPick(replay);
            break;

 
        case MOVE_START:
            setWhoseTurn(m.player);
            finalizePlacement();
            setState(VolcanoState.PLAY_STATE);
            doEndGame();
            
            break;

        case MOVE_RESIGN:
            setState(unresign==null?VolcanoState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
        	finalizePlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(VolcanoState.PUZZLE_STATE);

            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }


        return (true);
    }

    // legal to hit the chip reserve area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        case PLAY_STATE: 
        case RESIGN_STATE:
  		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return(true);
        }
    }
  
    // true if it's legal to drop gobblet  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(VolcanoCell fromCell,Pyramid gobblet,VolcanoCell toCell)
    {	if(fromCell==toCell) { return(true); }
      	VolcanoCell temp[] = getTempDest();
    	int n = getDestsFrom(fromCell,temp,true,true);
    	boolean ok = false;
    	for(int i=0;(i<n) && !ok;i++) { if(temp[i]==toCell) { ok = true; } }
    	returnTempDest(temp);
    	return(ok);
    }
    public boolean hasVolcanoCap(VolcanoCell c)
    {	Pyramid p = c.topChip();
    	return((p!=null) 
    			&& (p.colorIndex==Pyramid.BLACK_PYRAMID) 
    			&& (p.sizeIndex==Pyramid.SMALL_PYRAMID)); 
    }
    private boolean isCurrentDest(VolcanoCell c)
    {  	Hashtable<VolcanoCell,VolcanoCell> h = getMoveDests();
    	return(h.get(c)!=null);
     }
    private boolean isCurrentSource(VolcanoCell c)
    {	return(c==pickedSource[stackIndex]);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    private boolean isDest(VolcanoCell cell)
    {	return((stackIndex>0)&&(droppedDest[stackIndex-1]==cell));
    }

    public boolean LegalToHitBoard(VolcanoCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
			return((pickedObject==null) 
					? hasVolcanoCap(cell) 
					: (isCurrentSource(cell)||isCurrentDest(cell)));

		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return((pickedObject==null)
        			?(cell.chipIndex>=0)	// pick from a nonempty cell
        			: true);				// drop on any cell
        }
    }
    public boolean canDropOn(VolcanoCell cell)
    {	
    	return((pickedObject!=null)				// something moving
    			&&(pickedSource[stackIndex].onBoard 
    					? (cell!=pickedSource[stackIndex])	// dropping on the board, must be to a different cell 
    					: (cell==pickedSource[stackIndex]))	// dropping in the rack, must be to the same cell
    				);
    }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(VolcanoMovespec m)
    {
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
       // System.out.println("R "+m+" for "+m.player);
        if (Execute(m,replayMode.Replay))
        {	if(m.op==MOVE_BOARD_BOARD)
        	{	if(DoneState()) { m.undoInfo = doDone(replayMode.Replay); }
        		else { m.undoInfo=0; }
        	}
        	else if (m.op == MOVE_DONE)
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
    public void UnExecute(VolcanoMovespec m)
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
        			if(m.undoInfo!=0) { stackIndex--; }
        			undoEruption(getCell(m.from_col,m.from_row),getCell(m.to_col,m.to_row),m.undoInfo);
         			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        stackIndex = m.stackInfo;
        if(who!=m.player)
        	{
        	moveNumber--;
        	}
 }
 boolean canEruptTo(VolcanoCell c,int direction)
 {	VolcanoCell d = c.exitTo(direction);
 	return((d==null)?false:!hasVolcanoCap(d));
 }
 
 // return the final resting place index of the piece currently in stack index idx
 int finalRest(int idx)
 {	VolcanoCell target = pickedSource[idx];
 	int rest = idx;
 	while(idx<stackIndex)
 	{ if(pickedSource[idx]==target) 
 		{ target=droppedDest[idx]; 
 		  rest = idx;
 		}
 	 idx++; 
    }
  return(rest);
 }
 // mark all the stack indexes traversed by the piece currently in the index initial
 void markPath(int initial)
 {	VolcanoCell target = pickedSource[initial];
 	int idx = initial;
 	while(idx<stackIndex)
 	{ if(pickedSource[idx]==target) 
 		{ restPath[idx]=initial;
 		  target=droppedDest[idx];
 		}
  	 idx++; 
    }
 }

// determine if moving "from" to "to" would be a repetition that should be disallowed.
// This is a multistep process to allow returning to the same position when the global
// situation is not a complete repetition
 boolean isRepetition(VolcanoCell from,VolcanoCell to)
 { 	for(int i=0;i<stackIndex;i++) { restPath[i]=-1; }	// clear the tracking list
 	for(int i=0;i<stackIndex;i++)
	 {VolcanoCell sc = pickedSource[i];
	  VolcanoCell sf = droppedDest[finalRest(i)];
	  boolean lloop = ((sc==to)&& (sf==from));		// if this index starts at to and leads into from
	  if((sc==sf)||lloop) 
	  	{ markPath(i); 
	  	  if(lloop)
	  	  {	// locally we have a loop
	  		boolean loopok = false;
	  	 	for(int j=i;(j<stackIndex) && !loopok;j++)
	  	 	{	if( (restPath[j]==-1)
	  	 			&& (restPath[finalRest(j)]==-1)) 	// could be a lead-in to a loop
	  	 			{loopok = true; 
	  	 			}
	  	 	}
	  	 	if(!loopok) { return(true); }
	  	  }
	  	  }
	  	}
 	return(false);
 }
 	//	 && (pickedSource[stackIndex-1]==to)
	//	 && (droppedDest[stackIndex-1]==from)) { return(true); }
	//VolcanoCell target = to;
   	//for(int i=0;i<stackIndex;i++)		// up to the last move
 	//{	if(pickedSource[i]==target) 	// found our cell as a source?
 	//		{ target=droppedDest[i]; 	// now looking for this cell as a source
 	//		  if((target==from))			// got back to the target as a source  
 	//		  	{ return(true); 
 	//		  	}
 	//		}
 	//		else
 	//}
 	//return(false);
 //}
 int getDestsFrom(VolcanoCell from,VolcanoCell temp[],boolean erupting,boolean nonerupting)
 {	int n=0;
 	boolean haspiece = (from.chipIndex>=((from==pickedSource[stackIndex])?0:1));
 	erupting &= haspiece;
 	if(erupting || nonerupting)
 	{
 	for(int i=0;i<from.geometry.n;i++)
 	{
 	VolcanoCell c = from.exitTo(i);
 	if((c!=null)
 		&& !hasVolcanoCap(c)
 		&& ( (erupting && canEruptTo(c,i))
 			 || (nonerupting 
 					 && (!haspiece || !canEruptTo(c,i))
 					 && !isRepetition(from,c)
 			 ))
 		)
 		{
  		if(temp!=null) { temp[n]=c; }
 		n++;
 		}
 		
 	}}
 	return(n);
 }
 
 // get a hash of moves destinations, used by the viewer
 // to know where to draw dots and also to determine what
 // is a legal move
 public Hashtable<VolcanoCell,VolcanoCell> getMoveDests()
 {	Hashtable<VolcanoCell,VolcanoCell> dd = new Hashtable<VolcanoCell,VolcanoCell>();
 	VolcanoCell src = pickedSource[stackIndex];
 	if((src!=null)&&(pickedObject!=null))
 	{	VolcanoCell temp[] = getTempDest();
 		int n = getDestsFrom(src,temp,true,true);
 		for(int i=0;i<n;i++)
 		{	VolcanoCell c = temp[i];
 			dd.put(c,c);
 		}
 		returnTempDest(temp);
 	}
	return(dd);
 }
 
 CommonMoveStack  GetListOfMoves(CommonMoveStack  all,boolean erupting,boolean nonerupting)
 {	
	VolcanoCell temp[] = getTempDest();
	for(VolcanoCell c = rboard.allCells;
		c!=null;
		c=c.next)
	{	if(hasVolcanoCap(c))
		{	int n = getDestsFrom(c,temp,erupting,nonerupting);
			for(int i=0;i<n;i++)
			{	VolcanoCell d = temp[i];
				all.addElement(new VolcanoMovespec(c.col,c.row,d.col,d.row,whoseTurn));
			}
		}
	}
	returnTempDest(temp);
  	return(all);
 }
 
}