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
package punct;

import bridge.Color;
import lib.Graphics;
import lib.G;
import lib.Random;
import lib.AR;
import lib.CellId;
import lib.DefaultId;
import online.game.BoardProtocol;
import online.game.CommonMoveStack;
import online.game.commonMove;
import online.game.hexBoard;
import online.game.replayMode;


/**
 * PunctGameBoard knows all about the game of Hex, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
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

class PunctGameBoard extends hexBoard<punctCell> implements BoardProtocol,PunctConstants
{
	private PunctState unresign;
	private PunctState board_state;
	public PunctState getState() {return(board_state); }
	public CellStack animationStack = new CellStack();
	public void setState(PunctState st) 
	{ 	unresign = (st==PunctState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    // indexes into the balls array, usually called the rack
	public PunctColor playerColors[] = new PunctColor[2];
	public PunctPiece[][] pieces = { new PunctPiece[NUMPIECES],new PunctPiece[NUMPIECES] };
    public PunctPiece [] allPieces = new PunctPiece[NUMPIECES*2];
    public punctCell rack[][] = new punctCell[2][NUMPIECES];
    private punctCell allRack[] = new punctCell[NUMPIECES*2];
    public PunctPiece playerChip[] = { null, null };
    
    // ephemeral data for scoring
    public punctBlob blobs[] = new punctBlob[2*NUMPIECES];
    public int centerScore[] = new int[2];
    public int piecesOnBoard[] = new int[2];
    public int numBlobs=0;
    public int sweep_counter=0;
    public boolean setBlobits = false;
    public boolean setDropZoneValue=false;
    public boolean setDropZoneToValue=false;
    int dropBloBits = 0;
    int playerBloBits[] = new int[2];
    public boolean clearDropZoneValue=false;
    public boolean boardChanged=false;
    public void SetDrawState() { setState(PunctState.DRAW_STATE); }
 	

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    PunctPiece pickedObject = null;
    public int pickedSourceLevel = NothingMoving;
    private char picked_col;
    private int picked_row;
    private int picked_rotation = 0;

    private CellId droppedDestCode = DefaultId.HitNoWhere;
    private char dropped_col;
    private int dropped_row;
    
    // default constructor
    public PunctGameBoard(String init,int map[])
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; //coordinates left and bottom
        Grid_Style = PUNCTGRIDSTYLE;
        setColorMap(map, 2);
        makePieces();
        doInit(init); // do the initialization 
    }
    public PunctGameBoard cloneBoard() 
	{ PunctGameBoard dup = new PunctGameBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}   
     public void copyFrom(BoardProtocol b) { copyFrom((PunctGameBoard)b); }
   // factory method for board cells, overrides the default
    public punctCell newcell(char col,int row) { return(new punctCell(col,row)); }
    // overridable method.  This is used to make an ad-hoc adjustment to the position
    // of grid cells as painted by the viewer
    public void DrawGridCoord(Graphics gc,Color color, int xpos, int ypos,int cellsize,
        String txt)
    { 	char ch = txt.charAt(0);
    	//ad hoc adjustment for the numbers on the left
    	if((ch>='2') && (ch<='8')) 
    		{ 
    		  ypos=ypos+cellsize/3; 
    		}
    	else if((ch>='A') && (ch<='Z')) 
    	{
    		ypos -= cellsize/4;
    	}
    	else
    	{
    		//xpos -= cellsize/5;
    	}
    	super.DrawGridCoord(gc,color,xpos,ypos+cellsize/2,cellsize,txt);
     }
    private void fillRacks()
    {	int map[]= getColorMap();
       	for(int player=FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
       	{    	
       		PunctPiece pp[] = pieces[player];
       		punctCell prack[] = rack[player];
       		int offset = map[player]*NUMPIECES;
       		for(int idx=0;idx<NUMPIECES;idx++)
       		{
       			prack[idx] = allRack[idx+offset];
       			pp[idx] = allPieces[idx+offset];
       		}
       	}
    }
    private void makePieces()
       {
    	int id=0;	
       	PunctColor colors[] = PunctColor.values();
       	for(int player=FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
       	{	int idx=0;
       		PunctColor pcolor = colors[player];
       	    PunctId pool = chipPoolIndex[player];
       		{PunctPiece newp = new PunctPiece(this,id,pcolor,PUNCT_INDEX,0,null);
       		 punctCell newc = allRack[id] = new punctCell(pool,idx);
       		 newc.addStack(0,newp);
       		 allPieces[id++] = newp;
       		}
       		// do not disturb this ordering, as doing do would invalidate all 
       		// saved game records and cause other disruption in the display process.
       		// also, the move generator depends on all pieces with the same typecode,subtypecode
       		// being in a group in the rack
       		for(int i=0;i<6;i++)
       		{ PunctPiece newp =  new PunctPiece(this,id,pcolor,TRI_INDEX,TOP_SUBINDEX,tri_offset);
       		  punctCell newc = allRack[id] = new punctCell(pool,idx);
       		  newc.addStack(0,newp);
       		  allPieces[id++] = newp;        		  
       		}
       		for(int i=0;i<2;i++) 
       		{
       		PunctPiece newp = new PunctPiece(this,id,colors[player],STRAIGHT_INDEX,MID_SUBINDEX,straight_offset_c);
       		punctCell newc = allRack[id] = new punctCell(pool,idx);
       		newc.addStack(0, newp);
       		allPieces[id++] = newp; 
       		}
       		for(int i=0;i<4;i++) 
       		{ PunctPiece newp =   new PunctPiece(this,id,pcolor,STRAIGHT_INDEX,TOP_SUBINDEX,straight_offset_t);
       		  punctCell newc = allRack[id] = new punctCell(pool,idx);
       		  newc.addStack(0, newp);
       		  allPieces[id++] = newp;
       		}
   			for(int i=0;i<2;i++) 
   			{ PunctPiece newp = new PunctPiece(this,id,pcolor,Y_INDEX,TOP_SUBINDEX,y_offset_t);
   			  punctCell newc = allRack[id] = new punctCell(pool,idx);
   			  newc.addStack(0,newp);
   			  allPieces[id++] = newp; 
   			}
   			for(int i=0;i<2;i++) 
   			{ PunctPiece newp = new PunctPiece(this,id,pcolor,Y_INDEX,MID_SUBINDEX,y_offset_c);
   			  punctCell newc = allRack[id] =  new punctCell(pool,idx);
   			  newc.addStack(0, newp);
   			  allPieces[id++] = newp; 
   			}
   			for(int i=0;i<2;i++) 
   			{ PunctPiece newp = new PunctPiece(this,id,pcolor,Y_INDEX,BOT_SUBINDEX,y_offset_b);
 			  punctCell newc = allRack[id] =  new punctCell(pool,idx);
 			  newc.addStack(0, newp);
   			  allPieces[id++] = newp; 
   			  idx++;
   			}
         	}
       }
    


    // standared init for Punct.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    {	
    	if(!Punct_init.equalsIgnoreCase(game)) {throw G.Error(WrongInitError,game); }
    	else
    	{
        gametype = game;
        setState(PunctState.PUZZLE_STATE);
        reInitBoard(ZfirstInCol, ZnInCol, ZfirstCol);
        whoseTurn = FIRST_PLAYER_INDEX;
        for(int i=0;i<allPieces.length;i++) { allPieces[i].reInit(); }
        numBlobs = 0;
        // initialize the UI state
        pickedObject=null;
        pickedSourceLevel=NothingMoving;
        droppedDestCode=DefaultId.HitNoWhere;
      }
    }
    public void sameboard(BoardProtocol f) { sameboard((PunctGameBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(PunctGameBoard from_b)
    {
        super.sameboard(from_b); // compares the boards
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
        for(int i=0;i<allPieces.length;i++) 
        { v^=allPieces[i].Digest(r);
        }
        v^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(PunctGameBoard from_board)
    {	super.copyFrom(from_board);
        PunctGameBoard from_b = from_board;
        
        for(int i=0;i<allPieces.length;i++)
        {	allPieces[i].copyFrom(from_b.allPieces[i]);
        }
        
        { punctCell pc = allCells;
          while(pc!=null) { pc.copyFrom(from_board.getCell(pc.col,pc.row),allPieces);  pc=pc.next; }
        }

        board_state = from_b.board_state;
        AR.copy(piecesOnBoard,from_b.piecesOnBoard);
        boardChanged=true;
         sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {
    	randomKey = key;
    	PunctColor vs[] = PunctColor.values();
    	int map[] = getColorMap();
    	playerColors[0] = vs[map[0]];
    	playerColors[1] = vs[map[1]];
    	fillRacks();
       Init_Standard(gtype);
    	playerChip = new PunctPiece[] { pieces[0][0],pieces[1][0]};
 
        piecesOnBoard[0]=piecesOnBoard[1]=0;
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }
    public void togglePlayer()
    {
    	setWhoseTurn(nextPlayer[whoseTurn]);
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
        	togglePlayer();
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
        case DRAW_STATE:
        case CONFIRM_STATE:
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	
    	return(DoneState());
    }
    void sweepC2(punctCell c,punctBlob currentBlob)
    {	if(c!=null)
    	{
     	if(setBlobits) { c.bloBits |= currentBlob.bloBit;  }
    	PunctPiece p = c.topPiece();
    	
    	if(p==null)
    	{
        if(c.adjacent2_sweep_counter!=sweep_counter)  
    		{ c.adjacent2_sweep_counter=sweep_counter;
    		  c.adjacent2 = currentBlob;
    		  currentBlob.addLib(c.col,c.row);
    		} 
        else if((c.adjacent2!=null)&&(c.adjacent2!=currentBlob))
        {//cell previously encountered at distance 2
         currentBlob.addCrossLink(c.adjacent2,CONN3_VALUE,c.centerArea);
		 c.adjacent2.addCrossLink(currentBlob,CONN3_VALUE,c.centerArea);
        }
    	}}
    }
    void sweepFromLocation(punctCell c,PunctColor color,punctBlob currentBlob,punctCell caller,int direction)
    {	if(c!=null)
    	{
    	PunctPiece p = c.topPiece();
		//System.out.println("s "+caller+":"+direction+" > "+c+p);
    	if(c.adjacent2_sweep_counter!=sweep_counter)  
    		{ c.adjacent2 = null; 
    		  c.adjacent2_sweep_counter=sweep_counter; 
     		} 

    	if (c.sweep_counter!=sweep_counter)
    	{  //first encounter with any cell
    	   c.sweep_counter = sweep_counter;
    	   if(p==null)
    			{  // empty cell adjacent to a blob, not previously encountered on this sweep
    			   currentBlob.empties++;
    			   if(setBlobits) { c.bloBits |= currentBlob.bloBit; }
     		   	   c.adjacent1 = currentBlob;	// first encounter with this cell on this sweep
     		   	   currentBlob.addLib(c.col,c.row);
      			   if(c.centerArea) { currentBlob.emptyCenterAdjacent++; }
      	  		   if(c.adjacent2!=null) 
    			   	{ currentBlob.addCrossLink(c.adjacent2,CONN2_VALUE,c.centerArea); 
    			   	  c.adjacent2.addCrossLink(currentBlob,CONN2_VALUE,c.centerArea); 
    			   	}  
	    		   for(int dir=-1;dir<=1;dir++) 
   			     	{ //sweep for second level adjacents of the 3 possible cells
	    			   sweepC2(c.exitTo(direction+dir),currentBlob);
			     	}
      			}
			else if(p.color==color)
			{
			// friendly cell adjacent to a blob
     		if(currentBlob==null) 
    		{ int oldNumBlobs = numBlobs;
    		  numBlobs++;
    		  punctBlob newBlob = new punctBlob(p,setBlobits ? (1<<numBlobs) : 0,c.col,c.row);
    		  playerBloBits[playerIndex(newBlob.color)] |= newBlob.bloBit;
    		  blobs[oldNumBlobs] = newBlob;
    		  currentBlob=newBlob;
    		}
     		c.blob=currentBlob;
     		if(setBlobits) { c.bloBits |= currentBlob.bloBit; }
     		if(p.color==currentBlob.color)
     		{	if(c.centerArea) { centerScore[playerIndex(p)]++; }
     			currentBlob.addPiece(c,p,c.col,c.row);
     			for(int dir=0;dir<6;dir++)
     			{
     			sweepFromLocation(c.exitTo(dir),color,currentBlob,c,dir);
     			}
     			}
			}
     		else if(currentBlob!=null)
     		{ // unfriendly cell adjacent to a blob
     		  currentBlob.contacts++; 
     		  for(int dir=3;dir<=3;dir++)
     		  {	punctCell nx = caller.exitTo(direction+dir);
     		  	if(nx==null) { currentBlob.halfPincers++; }
     		  	else 
     		  	{PunctPiece pp = nx.topPiece();
     		    if(pp==null) { if(nx.centerArea) { currentBlob.halfPincers++; }}
     		    else { if(pp.color==color) { currentBlob.pincers++; }}
     		  	}
     		  }
     		}
    	}
    	else if(currentBlob!=null)
    	{	// this is a recursive call, not a top level call
    	 if(setBlobits) { c.bloBits |= currentBlob.bloBit; }
    	 if(p==null)
    	 {
		   if((c.adjacent2!=null)&&(c.adjacent2!=currentBlob)) 
			   	{ currentBlob.addCrossLink(c.adjacent2,CONN2_VALUE,c.centerArea); 
			   	  c.adjacent2.addCrossLink(currentBlob,CONN2_VALUE,c.centerArea); 
			   	  currentBlob.addLib(c.col,c.row);
			   	}  
		   if((c.adjacent1!=null)&&(c.adjacent1!=currentBlob))
		   	{ currentBlob.addCrossLink(c.adjacent1,CONN1_VALUE,c.centerArea); 
		   	  c.adjacent1.addCrossLink(currentBlob,CONN1_VALUE,c.centerArea); 
		   	}
    	 }
    	 }
    	}
  
    }
    void countBlobs(boolean calcBlo)
    {	setBlobits = calcBlo;
    	if(setBlobits)
    	{	//initialize the blob bits.  This is done only in winning move analysis
    		punctCell c = allCells;
    		while(c!=null) { c.bloBits = 0; c=c.next; }
    		boardChanged=true;
    		dropBloBits = 0;
    		playerBloBits[0]=playerBloBits[1]=0;
    	}
    	
    	if(boardChanged)
    	{
       	boardChanged=false;
        numBlobs=0;
    	int pp[] = new int[2];
    	for(int player=FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
    	{PunctPiece pps[] = pieces[player];
    	 centerScore[player] = 0;
    	 sweep_counter++;
    	 for(int i=0;i<NUMPIECES;i++)
    	{	PunctPiece p = pps[i];
    		int lvl = p.level;
    		if(lvl>=0) // on the board
    		{	pp[player]++;
    			for(int j=0;j<3;j++) { sweepFromLocation(p.cells[j],playerColors[player],null,null,0); }
    		}
    	}
    	}
    	G.Assert((pp[0]==piecesOnBoard[0])&&(pp[1]==piecesOnBoard[1]),"piece count is incorrect");
    	//System.out.println(""+numBlobs+" blobs");
    	//if(numBlobs>1) 
    	//{ for(int i=0;i<numBlobs;i++) 
    	//	{ blobs[i].showLinks();
    	//	}
    	//}
    	}
    }
    
 /**
  */ 
   public boolean LineWinForPlayer(int player)
    {	
       	for(int i=0;i<numBlobs;i++)
       		{ if(blobs[i].winForColor(playerColors[player])) { return(true); }
       		}
       	return(false);
    }
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winNext=false; }	// simultaneous win is win for moving player
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(PunctState.GAMEOVER_STATE);
    }
    public boolean WinForPlayerNow(int player)
    {	if(win[player]) { return(true); }
 		countBlobs(false);
  
 		boolean linewin = LineWinForPlayer(player);
 		boolean otherLineWin = LineWinForPlayer(nextPlayer[player]);
 		
 		if(linewin != otherLineWin) { return(linewin); }			// somebody wins
 		if(linewin && otherLineWin) { return(player==whoseTurn); }	// player who moves wins
 		
      	if((piecesOnBoard[FIRST_PLAYER_INDEX]==NUMREALPIECES)
    			||(piecesOnBoard[SECOND_PLAYER_INDEX]==NUMREALPIECES))
    		{	// possible win if all pieces are on the board
    		return(centerScore[player]>centerScore[nextPlayer[player]]);
    		}
    	return(false);
     }



    public punctCell getPunctCell()
    { if((pickedObject!=null)&&(pickedSourceLevel!=POOL_LEVEL))
    	{ return(getCell(picked_col,picked_row));
    	}
      return(null);
    }
    public PunctPiece getPunctPiece()
    { if(pickedObject!=null)
    	{ return(pieces[playerIndex(pickedObject)][0]);
    	}
      return(null);
    }
    boolean nonEmptyCell(char col,int row,PunctPiece p)
    {	punctCell ac = getCell(col,row);
		return((ac!=null) && (ac.level(p)>=0));
    }
    // return true if some adjacent cell to c is not empty and not p
    boolean someAdjacent(char col,int row,PunctPiece p)
    {	return(nonEmptyCell(col,row+1,p)
    		|| nonEmptyCell(col,row-1,p)
    		|| nonEmptyCell((char)(col-1),row,p)
    		|| nonEmptyCell((char)(col+1),row,p)
    		|| nonEmptyCell((char)(col+1),row+1,p)
    		|| nonEmptyCell((char)(col-1),row-1,p));
    }
    // return -1 if the piece can't be dropped, or 0-n if dropping would land on level n of the board.
    // if filter is true, also require that the drop would be in contact with either the center
    // or another piece.  This is used to limit the number of probably-meaningless moves considered
    // by the robot.  If topFilter is true, require level>0 (dropping on top of something)
    //
    public int levelForDrop(PunctPiece p,punctCell c0,int newr,
    		boolean fromrack,boolean contactFilter,int requireDrop,boolean topFilter,boolean centerFilter)
    {	
     	char col = c0.col;
    	int row = c0.row;
    	int second[] = p.cr_offsets[0][newr];
    	int third[] = p.cr_offsets[1][newr];
    	char col1 = (char)(col+second[0]);
    	char col2 = (char)(col+third[0]);
    	int row1 = row+second[1];
    	int row2 = row+third[1];
    	punctCell c1 = getCell(col1,row1);
    	punctCell c2 = getCell(col2,row2);
    	if((c1!=null) && (c2!=null))
    	{
        if((requireDrop!=0) 
           && ((requireDrop & (c0.bloBits | c1.bloBits | c2.bloBits))==0)) { return(-1); }
        
    	int c0level = c0.level(p);
       	int c1level = c1.level(p);
    	int c2level = c2.level(p);
  
    	if(fromrack) { if(c0.isCenterArea() || c1.isCenterArea() || c2.isCenterArea()) { return(-1); }}
    	
    	if(centerFilter) 
    		{ //if centerfilter is on, pass only those moves which touch the center.  This is actually used
    		  // to pass subsequent rotations only if they touch the center
    		  if(! (c0.isCenterArea() || c1.isCenterArea() || c2.isCenterArea())) { return(-1); }
    		}
		// consider the possibility of bridges
		switch(p.typecode)
		{
		default: throw G.Error("not expecting %s",p);
		case TRI_INDEX: break;	// no bridges
		case STRAIGHT_INDEX:
			switch(p.punct_index)
			{default: throw G.Error("Not expecting subtype %s",p);
			 case TOP_SUBINDEX: // bridging a straight piece with dot at the top
				 if(c1level<c0level) { c1level=c0level; }
				break;
			 case MID_SUBINDEX:	// bridging a straight piece with dot at the bottom
				if(c0level<c1level) { c0level=c1level; }
				break;
			}
			break;
		case Y_INDEX:
			switch(p.punct_index)
			{default: throw G.Error("Not expecting subtype %s",p);
			case TOP_SUBINDEX:
			case BOT_SUBINDEX:
				if(c1level<c0level) { c1level = c0level; }
				break;
			case MID_SUBINDEX:
				if(c0level<c1level) { c0level = c1level; }
				break;
			}
		}
	   	
    	if(c0level>=0)
    	{	// dropping on a nonempty spot, it must be the same color.  Note that if we are creating
    		// a bridge, c0level might have been raised, and the cell might actually be empty
    		PunctPiece topp = c0.topPiece(p);
    		if((topp==null) || (topp.color!=p.color)) { return(-1); }
    	}
 
    	if((c0level==c1level) && (c0level==c2level))
    			{ // if dropping from the rack, must be on the board itself
    			 if(topFilter && (c0level==-1) 
    					 &&!(c0.isCenterArea() 
    					     || c1.isCenterArea() 
    					     || c2.isCenterArea()))
    			 	{ return(-1); 
    			 	}
    			 if (fromrack)
    			 {	return(c0level==-1 ? 0 : -1);
    			 }
    			 else if(contactFilter)
    			 {	if((c0level>=0) || c0.isCenterArea() || c1.isCenterArea() || c2.isCenterArea()) { return(c0level+1); }
    			    if(someAdjacent(col,row,p)
    			    		|| someAdjacent(col1,row1,p) 
    			    		|| someAdjacent(col2,row2,p)) 
    			    	{ return(c0level+1); 
    			    	}
    			    return(-1);
    			 }
    			 else
    			 { 	return(c0level+1);
    			 }
    			}
    		return(-1);
    	}
    	return(-1);
    }
    public boolean canBeDropped(PunctPiece p,punctCell c, int rot,boolean fromrack,
    		boolean contactFilter,int inDropZone,boolean topFilter,boolean centerFilter)
    {	int lvl = levelForDrop(p,c,rot,fromrack,contactFilter,inDropZone,topFilter,centerFilter);
    	return(lvl>=0);
    }
    
    public int nextValidRotation(PunctPiece p,char col,int row,int start,boolean fromrack)
    {	int dif = ((start-p.rotation)+6)%6;
    	if(dif==0) { dif = 1; }
    	for(int i=0; i<6; i++)
    	{ int nextrot = ((i*dif+start)%6);
    	  if(canBeDropped(p,getCell(col,row),nextrot,fromrack,false,0,false,false)) 
    	  	{ return(nextrot); 
    	  	}
    	}
    	return(-1);
    }
    public boolean canBeDropped(PunctPiece p,char col,int row,boolean fromrack)
    {	return(nextValidRotation(p,col,row,p.rotation,fromrack)>=0);
    }
    
    // annotate the cells that this piece can no longer move to
    void freeze(PunctPiece pc)
    {	if(!pc.frozen) 
    	{ punctCell cc = pc.cells[0];
    	  cc.countPcells(playerIndex(pc),-1); 
    	  pc.frozen=true; 
    	 }
    }
    // annotate the cells that this piece could move to
    void thaw(PunctPiece pc)
    {	if(pc.frozen) 
    	{punctCell cc = pc.cells[0];
    	 cc.countPcells(playerIndex(pc),1); 
    	 pc.frozen=false; 
    	 }
    }
    // set the contents of the board, and maintain the count of chips
    public void SetBoard(char col,int row,PunctPiece p,int newr)
    {	int second[] = p.cr_offsets[0][newr];
    	int third[] = p.cr_offsets[1][newr];
    	char col1 = (char)(col+second[0]);
    	char col2 = (char)(col+third[0]);
    	int row1 = row+second[1];
    	int row2 = row+third[1];
    	punctCell c0 = getCell(col,row);
    	int c0level=c0.nextLevel();
    	punctCell c1 = getCell(col1,row1);
    	int c1level = c1.nextLevel();
    	punctCell c2 = getCell(col2,row2);
    	int c2level = c2.nextLevel();
    	// normally c0.nextLevel() is the right thing, but in unusual circumstances,
    	// c0 can be at a lower level because it is a bridge
     	int level = Math.max(Math.max(c0level,c1level),c2level);
     	if(level>0)
     	{	// potentially freeze
     		if(c0level>0) { freeze(c0.topPiece()); }
     		if(c1level>0) { freeze(c1.topPiece()); }
     		if(c2level>0) { freeze(c2.topPiece()); }
     	}
       	c0.addStack(level,p);
       	p.cols[0]=col; p.rows[0]=row;
       	p.cells[0]=c0;
       	c1.addStack(level,p);
       	p.cols[1]=col1; p.rows[1]=row1;
       	p.cells[1]=c1;
       	c2.addStack(level,p);
       	p.cols[2]=col2; p.rows[2]=row2;
       	p.cells[2]=c2;
       	p.level = level;
       	p.rotation=newr;
       	p.frozen=true; thaw(p);	// forced thaw
       	if(setDropZoneValue) 
       		{ 
       		  if(setDropZoneToValue)
       			  { c2.bloBits |= KILLER_BLOBIT;
       			  	c1.bloBits |= KILLER_BLOBIT;
       			  	c0.bloBits |= KILLER_BLOBIT;
       			  	// this collects the blobits that will attack the winning line
       			  	dropBloBits |= c0.bloBits | c1.bloBits | c2.bloBits;
       			  }else
       			  { c2.bloBits &= ~KILLER_BLOBIT;
       			    c1.bloBits &= ~KILLER_BLOBIT;
       			    c0.bloBits &= ~KILLER_BLOBIT;
       			  }
       		  
       		}
       	
       	piecesOnBoard[playerIndex(p)]++;
       	boardChanged=true;
    }
    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	if(droppedDestCode!=DefaultId.HitNoWhere) 
    		{ 
        	droppedDestCode = DefaultId.HitNoWhere;
        	pickedSourceLevel = NothingMoving;
        	pickedObject=null;
    		}
    	else { unPickObject(); }
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
       if(droppedDestCode==PunctId.EmptyBoard)
        	{ PunctPiece p = pickedObject;
        	  removeFromBoard(p);
        	  p.level = TRANSIT_LEVEL;
        	}
        droppedDestCode = DefaultId.HitNoWhere;
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	if(pickedObject!=null)
    	{
        if(pickedSourceLevel == POOL_LEVEL)
        	{
        	pickedObject.level=pickedSourceLevel;
        	pickedObject.rotation=0;
        	}
        else  {  SetBoard(picked_col, picked_row, pickedObject,picked_rotation); }
        }
		pickedObject = null;
        pickedSourceLevel = NothingMoving;
    }
    // 
    // drop the floating object.
    //
    private void dropObject(PunctId dest, char col, int row,int localrotation)
    {
        switch (dest)
        {
 
        default:
            pickedSourceLevel = NothingMoving;
        	pickedObject=null;
         	throw G.Error("not expecting dest %s", dest);
        case Chip_Pool:
        case Black_Chip_Pool:
        case White_Chip_Pool:		// back in the pool, we don't really care where
            pickedSourceLevel = NothingMoving;
            if(pickedObject!=null) 
            {	// this can occur if there was a moving piece when we rejoined the game
            	// so the first "real" move we see is a drop
            pickedObject.rotation=localrotation;
            pickedObject.level=POOL_LEVEL;
            }
            pickedObject=null;
           break;

        case BoardLocation: // an already filled board slot.
        case EmptyBoard:
        	G.Assert(pickedObject!=null,"nothing moving");
            {
            pickedObject.rotation=localrotation;
            SetBoard(col, row, pickedObject,localrotation);
            dropped_col = col;
            dropped_row = row;
            droppedDestCode = PunctId.EmptyBoard;
             break;
            }

        }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(char col, int row)
    {
        return ((droppedDestCode == PunctId.EmptyBoard) 
        		&& (dropped_col == col) 
        		&& (dropped_row == row));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	PunctPiece ch = pickedObject;
        if ((ch!=null)
        	&& (pickedSourceLevel != NothingMoving) // if moving
            &&(droppedDestCode == DefaultId.HitNoWhere)) // and not placed
        {
            return(ch.id);
        }

        return (NothingMoving);
    }
 
    private boolean canBePicked(PunctPiece p)
    {	int loc = p.level;
    	if((board_state==PunctState.PUZZLE_STATE) || p.color==playerColors[whoseTurn])
    	if(loc>=0)
	    	{
	    	for(int i=0;i<3;i++)
	    	{	punctCell c = p.cells[i]; //(punctCell)GetBoardCell(p.cols[i],p.rows[i]);
	    		G.Assert(c.pieces[loc]==p,"P location");
	    		if(c.level()>loc) 
	    		{   
	    			return(false); 
	    		}
	    	}
	    	return(true);	// nothing on top
	    	}
    	return(false);
    }
    
    public int playerIndex(PunctPiece ch) { return((ch.color==playerColors[0]) ? 0 : 1);}
    public int playerIndex(PunctColor cl) { return((cl==playerColors[0]) ? 0 : 1); }
    
    private void removeFromBoard(PunctPiece p)
    {	int loc = p.level;
    	freeze(p);
    	piecesOnBoard[playerIndex(p)]--;
    	boardChanged=true;
    	for(int i=0;i<3;i++)
    	{
    	punctCell c = p.cells[i];//(punctCell)GetBoardCell(p.cols[i],p.rows[i]);
		G.Assert((c.pieces[loc]==p) && (c.height==loc),"P location");
		p.cols[i]='@';
		p.rows[i]=0;
		p.cells[i]=null;
		c.removeStack(loc);
		if(clearDropZoneValue) { c.bloBits |= KILLER_BLOBIT; }
		{ PunctPiece pc = c.topPiece();
		  if((pc!=null) && canBePicked(pc))  { thaw(pc); }
    	}
    	}
    }
    private void pickObject(PunctPiece p)
    {
		switch(p.level)
    	{
		case TRANSIT_LEVEL:	
			G.Assert(pickedObject==p,"something else picked");
			break;
		default:	// on the board 
			G.Assert(canBePicked(p),"Piece is on top");
			picked_row=p.rows[0];
			picked_col=p.cols[0];
			picked_rotation=p.rotation;
			pickedSourceLevel=p.level;
			pickedObject = p;
			removeFromBoard(p);
	   		p.level = TRANSIT_LEVEL;
         	droppedDestCode=DefaultId.HitNoWhere;
 			break;
		case POOL_LEVEL: 
			pickedObject = p;
	   		pickedSourceLevel = p.level;
	   		p.level = TRANSIT_LEVEL;
         	droppedDestCode=DefaultId.HitNoWhere;
        	break;
    	}
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(PunctId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	{boolean wasDest = isDest(col,row);
        	if(wasDest) { unDropObject(); }
        	else
	        	{
	        	punctCell c = getCell(col, row);
	        	PunctPiece p = c.topPiece();
	        	G.Assert(p!=null,"Nothing to pick");
	        	pickObject(p);
 	        	}
        	}
            break;

        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(char col, int row)
    {
        return ((pickedSourceLevel != NothingMoving) 
        		&& (picked_col == col) 
        		&& (picked_row == row));
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
        case DRAW_STATE:
        	if(droppedDestCode==DefaultId.HitNoWhere)
        		{ setNextStateAfterDone();	 // start the move over
        		}
        	break;
        case PLAY_STATE:
			setState(PunctState.CONFIRM_STATE);
			break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    
    private void setNextStateAfterPick()
    {	switch(board_state)
    	{
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case PLAY_STATE:
        case PUZZLE_STATE: break;
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setState(PunctState.PLAY_STATE);
        	break;
    	}
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
        case DRAW_STATE:
        	setGameOver(false,false);
			break;
		case GAMEOVER_STATE:
        	break;
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		setState(PunctState.PLAY_STATE);
    		break;
    	}

    }
    private boolean setGameOver()
    {	boolean over = false;
       	boolean win1 = WinForPlayerNow(whoseTurn);
    	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
    	if(win1 || win2) { setGameOver(win1,win2); over = true; }
   		else if((piecesOnBoard[FIRST_PLAYER_INDEX]==NUMREALPIECES)
        			||(piecesOnBoard[SECOND_PLAYER_INDEX]==NUMREALPIECES))
    			{setGameOver(false,false); // all one player's pieces placed, game is over, a draw
    			 over = true;
    			}
    	return(over);
    }
    private void doDone()
    {
        acceptPlacement();
 
        if (board_state==PunctState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	if(!setGameOver()) 
        	{setNextPlayer(); 
        	 moveNumber++;		// increment movenumber only if we change player
       	     setNextStateAfterDone();
        	}
         }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	Punctmovespec m = (Punctmovespec)mm;

        //System.out.println("E "+m+Digest());
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone();
            break;
        case MOVE_MOVE:
        	
        	punctCell c = getCell(m.from_col,m.from_row);
        	PunctPiece p = c.topPiece();
        	G.Assert((p.color==playerColors[whoseTurn])&&(p.level>=0),"not my piece on board");
			m.from_rot = p.rotation;
			m.from_level = p.level;

			pickObject(p);
			m.chip = pickedObject;
			if(replay!=replayMode.Replay)
			{
				animationStack.push(c);
				animationStack.push(getCell(m.to_col,m.to_row));
			}
			dropObject(PunctId.BoardLocation, m.to_col, m.to_row,m.rotation);
            setNextStateAfterDrop();
       	
        	break;
        case MOVE_DROPB:
			switch(board_state)
			{ case PUZZLE_STATE: acceptPlacement(); break;
			  case DRAW_STATE:
			  case CONFIRM_STATE: unDropObject(); unPickObject(); break;
			  case PLAY_STATE:
			   break;
			default:
				break;
			}
			pickObject(allPieces[m.object]);
			m.chip = pickedObject;
			m.from_row = picked_row;
			m.from_col = picked_col;
			m.from_rot = picked_rotation;
			m.from_level = pickedSourceLevel;

			dropObject(PunctId.BoardLocation, m.to_col, m.to_row,m.rotation);
			
			if(replay==replayMode.Single)
			{
				animationStack.push(pickedSourceLevel==POOL_LEVEL ? allRack[m.object] : getCell(picked_col,picked_row));
				animationStack.push(getCell(m.to_col,m.to_row));
			}
			G.Assert( (board_state==PunctState.PUZZLE_STATE) 
					|| (pickedSourceLevel!=-1) 
					|| (pickedObject.level==0), "dropped on top");
            setNextStateAfterDrop();

            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	pickObject(PunctId.BoardLocation, m.to_col, m.to_row);
        	m.chip = pickedObject;
        	setNextStateAfterPick();
            break;

        case MOVE_DROP: // drop on chip pool;
            dropObject(m.source, m.to_col, m.to_row,m.rotation);
            switch(board_state)
            {
             case CONFIRM_STATE: setState(PunctState.PLAY_STATE);
				break;
			default: 
            }
            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickObject(allPieces[m.object]);
            m.chip = pickedObject;
            setNextStateAfterPick();
 
            break;

        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            win[FIRST_PLAYER_INDEX] = win[SECOND_PLAYER_INDEX]=false;
            setState(PunctState.PLAY_STATE);
            setGameOver();
            break;

        case MOVE_RESIGN:
            setState(unresign==null?PunctState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
            acceptPlacement();
            setState(PunctState.PUZZLE_STATE);

            break;
        case MOVE_ROTATE:
        	G.Assert(pickedObject!=null,"something moving");
        	pickedObject.rotation=(pickedObject.rotation+1)%6;
        	break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        	
        default:
        	cantExecute(m);
        }

        //System.out.println("X "+m+Digest());

        return (true);
    }

    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case DRAW_STATE:
        case PLAY_STATE: 
        	return((pickedObject==null)
        			?(player==whoseTurn)
        			:((player==whoseTurn)&&(pickedSourceLevel==POOL_LEVEL)));
        case CONFIRM_STATE:
        	return(false);
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
            return (true);
        }
    }
    public boolean isInline(char fcol,int frow,char tcol,int trow)
    {	return((fcol==tcol) || (frow==trow)||((tcol-trow)==(fcol-frow)));
    }
    public boolean legalSpot(char col,int row,boolean fromrack)
    {	punctCell cell = getCell(col,row);
    	if(pickedObject==null)
    	{	// picking something up
    		PunctPiece p = cell.topPiece();
    		if(p!=null) { return(canBePicked(p)); }
    		return(false);
    	}
    	// dropping off
    	if((board_state==PunctState.PLAY_STATE)&&(pickedSourceLevel>=0))
    	{	if(!isInline(picked_col,picked_row,cell.col,cell.row)) { return(false); }
    	}
    	return(canBeDropped(pickedObject,col,row,fromrack));
    }
    public boolean LegalToHitBoard(char col, int row)
    {
        switch (board_state)
        {
		case PLAY_STATE:
		{ boolean fromrack = (pickedObject!=null)&&(pickedSourceLevel==POOL_LEVEL);
		  return(legalSpot(col,row,fromrack));
		}
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
        case DRAW_STATE:
		{	punctCell c = getCell(col,row);
			PunctPiece p = c.topPiece();
			return(p==pickedObject);
		}
		default:
			throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return(legalSpot(col,row,false));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Punctmovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
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
    public void UnExecute(Punctmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	if(whoseTurn!=m.player)
    	{	moveNumber--;
    		setWhoseTurn(m.player);
    	}
        switch (m.op)
        {
   	    default:
   	    	cantExecute(m);
        	break;

        case MOVE_DONE:
            break;
       case MOVE_MOVE:
       case MOVE_DROPB:
    	    setState(PunctState.CONFIRM_STATE);
        	pickObject(PunctId.BoardLocation,m.to_col,m.to_row);
        	dropObject(m.from_level>=0?PunctId.BoardLocation:PunctId.Chip_Pool,m.from_col,m.from_row,m.from_rot);

        	break;
        case MOVE_RESIGN:

            break;
        }
        setState(m.state);
 }

 // count the number of piece types remaining in the pool
 // for the current player.  This is use to increase the
 // number of moves considered per piece as the number
 // of choices decreases.
 public int pieceTypesRemaining()
 {	int val = 0;
	PunctPiece myPieces[] = pieces[whoseTurn];
 	PunctPiece prevPiece = myPieces[0];
 	for(int i=1;i<NUMPIECES;i++)
 	{	PunctPiece p = myPieces[i];
 		if((p.level == POOL_LEVEL)&&(!p.samePieceType(prevPiece)))
 		{ prevPiece = p;
 		  val++;
 		}
 	}
 	return(val);
 }
 // get the list of drop moves, considering the rules and various filters. 
 // the main, nonspecific filter is to simply ignore most moves. there are 
 // about 4K valid first moves!
 public CommonMoveStack  GetListOfDropMoves(CommonMoveStack  all0,
		 int nskip,				// if >1, allow 1 move and skip n-1
		 int requireDropZone)	// if nonzero, drop only onto cells with some bloBits in common
 {	CommonMoveStack all = all0;
 	if(all==null) { all = new CommonMoveStack(); }
 	PunctPiece lastRackPiece=null;
 	PunctPiece myPieces[] = pieces[whoseTurn];
 	int nmoves=0;
 	for(int pieceNum=1; pieceNum<NUMPIECES; pieceNum++)
 	{	PunctPiece piece = myPieces[pieceNum];
 		int me = piece.id;
 		switch(piece.level)
 		{
 		case TRANSIT_LEVEL: throw G.Error("shouldn't be in transit");
 		case POOL_LEVEL:
 			if(!piece.samePieceType(lastRackPiece))
 			{	lastRackPiece=piece;		// we only need to consider placing each type of piece once

	 		punctCell c = allCells;
		 		while(c!=null)
		 		{
		 			if(!c.centerArea && (c.level()==-1))
 		 			{
 		 			for(int rr = 0;rr<piece.nRotations;rr++)
 		 			{
 		 			if( (((nmoves%nskip)==0)) 
 		 				&& canBeDropped(piece,c,rr,true,false,requireDropZone,false,false) )
 		 				{
 		 				all.addElement(new Punctmovespec(
 		 				  "dropb "+me+" "+c.col+" "+c.row+" "+rr,whoseTurn)
 		 				  );
 		 				
		 			
 		 				}
		 				nmoves++;
 		 			}
 		 			}
 		 		
		 		c = c.next;
		 		}

 			}
 			break;
 		default: // on the board already
 			break;
  		}	// end of piece location switch
 	} // end of pieces loop
 	return(all);
 }
 
 // get the list of "move" moves within the board, with various options to 
 // filter out valid possibilities.  Prefiltering as many as possible
 // is what makes the robot playably fast.
 //
 public CommonMoveStack  GetListOfMoveMoves(CommonMoveStack  all0,
		 boolean contactFilter,			// if true, exclude moves which end not in contact
		 								// with either some other piece or the center
		 boolean rotationFilter,		// filter multiple rotations on the same drop location
		 boolean substitute_drops,		// if true, exclude moves for which the same 
		 								// cells could be covered by a drop move.
		 int move_require_drops)		// if nonzero, a bit mask of bloBits, include
 										// only moves which some cell has intersecting bits.
 {	CommonMoveStack all = all0;
 	if(all==null) { all = new CommonMoveStack(); }
 	PunctPiece myPieces[] = pieces[whoseTurn];
 	PunctPiece lastRackPiece = myPieces[0];
 	boolean shapeIsInRack = false;

 	for(int pieceNum=1; pieceNum<NUMPIECES; pieceNum++)
 	{	PunctPiece piece = myPieces[pieceNum];
 
 		if(substitute_drops && (piece.typecode!=lastRackPiece.typecode))
 		{	lastRackPiece = piece;
 			int pn = pieceNum;
 			shapeIsInRack=false;
 			// detect if another piece with the same shape is already in the rack
 			// if a piece with the same shape is available, we don't move when we
 			// could drop instead.
 			while(pn<NUMPIECES)
 			{ PunctPiece nextP = myPieces[pn];
 			  if(nextP.typecode!=piece.typecode) { pn=NUMPIECES; }
 			  else if(nextP.level==POOL_LEVEL) { shapeIsInRack=true; pn=NUMPIECES; }
 			  else { pn++; }
 			}
 		}
 			
 		switch(piece.level)
 		{
 		case TRANSIT_LEVEL: throw G.Error("shouldn't be in transit");
 		case POOL_LEVEL:
 			break;
 		default: // on the board already
 		if(canBePicked(piece))
 		{  punctCell center = piece.cells[0];
 		   char thiscol = center.col;
 		   int thisrow = center.row;
 		   {
 		   boolean somedropped=false;
 		   for(int rr = 0; rr<piece.nRotations; rr++)
 			{
			if((rr!=piece.rotation)
	 			&& canBeDropped(piece,center,rr,false,contactFilter,move_require_drops,false,somedropped))
	 			{ // rotating a piece in place.
				all.addElement(new Punctmovespec(thiscol,thisrow,thiscol,thisrow,rr,whoseTurn));
				somedropped=rotationFilter;
	 			}
 			}
 		   }
			// traverse in each of the 6 directions, looking for valid places to drop
			// considering the rules and the filters in effect
 		   {boolean someDropped=false;
 		    for(int direction=0;direction<6;direction++)
 			{
 			punctCell cn = center.exitTo(direction);
 			int steps = 1;
 			while(cn!=null)
 			{	boolean topFilter = shapeIsInRack && (steps>2) ;
 				for(int localrotation=0;localrotation<piece.nRotations;localrotation++)
 				{
	 			if(canBeDropped(piece,cn,localrotation,false,contactFilter,move_require_drops,topFilter,someDropped))
	 				{ 
	 				  all.addElement(new Punctmovespec(thiscol,thisrow,cn.col,cn.row,localrotation,whoseTurn));
	 				  someDropped=rotationFilter;
	 				}
 				}
 			 cn=cn.exitTo(direction);
 			 steps++;
 			 }}
  	 			
 			}
 
 		}
 		}	// end of piece location switch
 	} // end of pieces loop
 	return(all);
 }

}
