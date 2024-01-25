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
package dash;

import java.awt.Color;

import java.util.Hashtable;

import lib.*;
import online.game.*;

/**
 * DashBoard knows all about the game of Dash, which is played
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

class DashBoard extends rectBoard<DashCell> implements BoardProtocol,DashConstants
{
	private int sweep_counter = 0;
	private DashState unresign;
	private DashState board_state;
	public DashState getState() {return(board_state); }
	public void setState(DashState st) 
	{ 	unresign = (st==DashState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public int boardSize = DEFAULT_BOARDSIZE;	// size of the board
    public void SetDrawState() { setState(DashState.DRAW_STATE); }
    public DashCell tTiles[] = null;
    
    public CellStack animationStack = new CellStack();
    
    public DashColor playerColor[] = null;
    public DashChip playerChip[] = null;
    
    public DashCell pickedSource = null;
    public DashCell droppedDest = null;
    public DashChip pickedObject = null;
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    // note that these are intended to be used by and maintained by the GUI, not the robot
    //
    public DashCell flippedCell = null;
    private CommonMoveStack  pickedRiverMoves=null;
    
	// factory method
	public DashCell newcell(char c,int r)
	{	return(new DashCell(c,r));
	}
    public DashBoard cloneBoard() 
	{ DashBoard dup = new DashBoard(gametype,randomKey,players_in_game,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((DashBoard)b); }
    public DashBoard(String init,long randomv,int players,int map[])
    {	// convert a game name to the random key in a unique but specific way.
    	// this initializer determines the initial layout of the flippable tiles
    	setColorMap(map, players);
    	doInit(init,randomv);
    }
    // override method.  xpos ypos is the desired centerpoint for the text
    // dash needs two tweaks to make the grid right.  Offset to label
    // the intersections instead of the squares, and skip the last row/col
    public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
    {   char ch = txt.charAt(0);
    	boolean isDig = Character.isDigit(ch);
    	if(isDig) 
    	{ if(G.IntToken(txt)>=nrows) { return; } ;
    	  ypos -= 5*cellsize/10 ; 
    	}
    	else 
    	{ if((ch-('A'-1))>=ncols)
    		{ return; } ; 
    	  xpos += 9*cellsize/20; 
    	}
   		GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
    }


    final DashCell SetCell(char col,int row,int idx)
    {	DashCell c = getCell(col,row);
    	c.addTile(tTiles[idx].topChip());
    	return(c);
    }
    final void SetMatchingCell(char col,int row,int idx)
    {	DashCell c = getCell(col,row);
    	if(!c.chipColorMatches(tTiles[idx].topChip())) { idx ^=1; }	// flip the chip
		c.addTile(tTiles[idx].topChip());
    }
    
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game,long rk)
    { 	if(Dash_INIT.equalsIgnoreCase(game)) { boardSize=DEFAULT_BOARDSIZE+2;  }
    	else {throw  G.Error(WrongInitError,game); }
        gametype = game;
        randomKey = rk;
        setState(DashState.PUZZLE_STATE);
        initBoard(boardSize,boardSize); //this sets up the board and cross links
        int map[]=getColorMap();
        // temporary, fill the board
        char last = (char)('A'+boardSize-1);
        SetCell('A',1,DashChip.SW_tile_index);
        SetCell(last,1,DashChip.SE_tile_index).onBoard=false;
        SetCell('A',boardSize,DashChip.NW_tile_index);
        SetCell(last,boardSize,DashChip.NE_tile_index).onBoard=false;
        Random r = new Random(randomKey);
        for(int i=2; i<boardSize;i++)
        {	char col = (char)('A'+i-1);
        	SetCell('A',i,((i&1)==0)?DashChip.LA_tile_index:DashChip.LB_tile_index);
        	SetCell(last,i,((i&1)==0)?DashChip.RA_tile_index:DashChip.RB_tile_index).onBoard=false;
        	SetCell(col,1,((i&1)!=0)?DashChip.BA_tile_index:DashChip.BB_tile_index);
        	SetCell(col,boardSize,((i&1)!=0)?DashChip.TA_tile_index:DashChip.TB_tile_index).onBoard=false;
        	for(int j=2;j<boardSize;j++)
        	{	SetMatchingCell(col,j,DashChip.R0_tile_index+(Random.nextInt(r,4)));
        	}
        }
               
        playerColor = new DashColor[players_in_game];
        playerChip = new DashChip[players_in_game];
        for(int i=0;i<players_in_game; i++)
        {
        	DashColor color = playerColor[i] = DashColor.values()[map[i]];
        	playerChip[i] = color.chip;
        }
 
 		int row = 1;
    	{	int invrow = boardSize-row;
			for(int colnum=2;colnum<boardSize-2;colnum+=2)
			{	char col = (char)('A'+(boardSize-colnum-1)+((row&1)^1));
				char invcol = (char)('A'+colnum-1+(invrow&1));
        		DashCell c = getCell(col,row);
            	c.addChip(playerChip[0]);
            	c.region_size=-1;
            	
        		DashCell d = getCell(invcol,invrow);
        		d.addChip(playerChip[1]);
        		d.region_size = -1;
        	
        		if(players_in_game>=3)
        		{
        			DashCell e = getCell('A',col-'A'+1);
        			e.addChip(playerChip[2]);
        			e.region_size = -1;
        			
        			if(players_in_game>=4)
        			{
        				DashCell f = getCell((char)('A'+boardSize-2),invcol-'A'+1);
        				f.addChip(playerChip[3]);
        				f.region_size = -1;
        			}		
        			
        		}
       	}
        }
		for(DashCell c = allCells; c!=null; c=c.next)
		{	if(c.region_size<0) { mark1RegionSize(c); }
		}
        whoseTurn = FIRST_PLAYER_INDEX;
    }
    
    public void sameboard(BoardProtocol f) { sameboard((DashBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(DashBoard from_b)
    {	super.sameboard(from_b);
        // here, check any other state of the board to see if
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");
    }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
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
		//v ^= chip.Digest(r,sm_picked[sm_step]);//if(pickedObject!=null) { v^= (pickedObject.Digest()<<1); }
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(DashBoard from_b)
    {	super.copyFrom(from_b);
    	
        board_state = from_b.board_state;
        unresign = from_b.unresign;
       	pickedRiverMoves = from_b.pickedRiverMoves;
  
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rk)
    {	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
		Grid_Style = DASHGRIDSTYLE; //coordinates left and bottom
		doInit(gtype,rk,players_in_game);
    }
    public void doInit(String gtype,long rk,int np)
    {	players_in_game = np;
    	win = new boolean[np];
		randomKey = rk;
		animationStack.clear();
		Random r = new Random(63742);
    	tTiles = new DashCell[DashChip.N_STANDARD_TILES];
    	for(int i=0;i<DashChip.N_STANDARD_TILES;i++)
    	{	
    		DashCell cell = new DashCell(r,DashId.Tile);
    		tTiles[i]=cell;
    		cell.addTileNoCheck(DashChip.getChip(i));
    	}
       
       Init_Standard(gtype,randomKey);
       allCells.setDigestChain(r);
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
        {
         case CONFIRM_STATE:
         case DRAW_STATE:
         case RESIGN_STATE:
            return (true);

        default:
            return (false);
        }
    }
   public boolean DigestState()
   {	
   		switch(board_state)
	   	{case CONFIRM_STATE: 
	   		 return(true);
	   	 default: return(false);
	   	}
   }
   public boolean MandatoryDoneState()
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

    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(DashState.GAMEOVER_STATE);
    }

    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public boolean WinForPlayerNow(int player)
    {	if(board_state==DashState.GAMEOVER_STATE) { return(win[player]); }
 
     	throw G.Error("Not implemented");
     }

    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    double base_tropism_score = 0.1;
    double region_size_score = 0.02;
    //double color_tropism_score = 0.01;
    double height_multiplier[]={1.0,1.0,2.205,3.31,4.42,4.0,4.0};
    public double ScoreForPlayer(int player,boolean print,
    			double base_weight,
    			double piece_weight,
    			boolean dumbot)
    { 
    	double base_score =0;
    	double sub=0.0;
    	double reg=0.0;
    	for(DashCell c = allCells; c!=null; c=c.next)
    	{	if(c.onBoard && (c.topChip()==playerChip[player]))
    		{	double baserow = 1.0+base_tropism_score*((player!=FIRST_PLAYER_INDEX) ? c.row : (nrows-c.row));
    			sub += height_multiplier[c.chipIndex]*baserow;
    			if(!dumbot) { reg += c.chipIndex*Math.sqrt(region_size_score)*c.region_size; }
    		}
    	}
    	if(print)
    	{	System.out.println("pl "+player+" base "+base_score + " sub "+sub+" reg "+reg);
    	}
    	return(base_score+sub+reg);
    }
    

    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	pickedSource = null;
    	droppedDest = null;
    	flippedCell = null;
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	pickedObject = droppedDest.removeTop();
    	droppedDest = null;
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	pickedSource.addChip(pickedObject);
    	pickedObject = null;
    	pickedSource = null;
     }

    // 
    // drop the floating object.
    //
    private void dropObject(DashCell dest)
    {	droppedDest = dest;
    	droppedDest.addChip(pickedObject);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(DashCell cell)
    {	return( cell==droppedDest);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	DashChip ch = pickedObject;
    	if((ch!=null))
    		{ return(ch.chipNumber);
    		}
        return (NothingMoving);
    }

    public DashCell getCell(DashCell c)
    {
    	if(c!=null)
    	{
    		return(getCell(c.rackLocation(),c.col,c.row));
    	}
    	return(null);
    }
    public DashCell getCell(DashId source,char col,int row)
    {
    	switch(source)
    	{
        default:
        	throw G.Error("Not expecting source %s", source);
  	
        case BoardLocation:
     		return(getCell(col,row));
    	}
     }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(DashCell c)
    {	
    	pickedSource = c;
    	pickedObject = c.removeTop();
   }

    private void setNextStateAfterDrop(replayMode replay)
    {
       
      {
      switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case MOVE_STATE:
        	setState(DashState.CONFIRM_STATE);
        	break;
        case CONFIRM_STATE:
        case PUZZLE_STATE:
        	acceptPlacement();
            break;
        }
      }
    }


    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	
    	case GAMEOVER_STATE: 
    		break;
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
		case MOVE_STATE:
    		setState(DashState.FLIP_STATE);
    		break; 
    
    	}
   	 	acceptPlacement();

    }
 
    private void doDone(replayMode replay)
    {	
        acceptPlacement();

        if (board_state==DashState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	
        	boolean win1 = WinForPlayerNow(whoseTurn);
        	acceptPlacement();
        	win[whoseTurn] = win1;
        	if(win1) { setState(DashState.GAMEOVER_STATE); }
        	else { 
        		  setNextPlayer(); 
        		  setNextStateAfterDone(); 
        		  }
        }
    }
    private void flipCell (DashCell c)
    {
   	 G.Assert(c.chipIndex==0,"Cell is empty");
   	 invalidateRegionSize(c);
	 DashChip chip = c.removeTop();
	 DashChip newchip = tTiles[chip.chipNumber^3].topChip();
	 c.addTile(newchip);
	 markRegionSizes(c);
    }

    public void doFlip(DashMovespec m)
    {
     DashCell c = getCell(m.from_col,m.from_row);
   	 flipCell(c);
   	 switch(board_state)
   	 {	
   	 default: throw G.Error("not expecting flip in state %s",board_state);
   	 case PUZZLE_STATE:	break;
   	 case FLIP_STATE:	
   		 flippedCell = c;
   		 setState(DashState.MOVE_STATE); 
   		 acceptPlacement();
   		 break;
   	 case MOVE_STATE:
   		 flippedCell = null;
   		 setState(DashState.FLIP_STATE);
   		 break;
   	 }	 
     }
    
    public void doMove(DashMovespec m,replayMode replay)
    {	DashCell dest = getCell(m.to_col,m.to_row);
		DashCell source = getCell(m.from_col, m.from_row);
		pickObject(source);
		dropObject(dest); 
		if(replay!=replayMode.Replay)
		{
			animationStack.push(source);
			animationStack.push(dest);
		}
		setNextStateAfterDrop(replay);
    }
    public void undoMove()
    {	
    	unDropObject();
		unPickObject(); 
    }
    public int directionNumber(char ch)
    {
    	switch(ch)
    	{
     	default: throw G.Error("not expecting %s",ch);
    	case 'N':
    	case 'n': return(CELL_UP);
    	case 'S':
    	case 's': return(CELL_DOWN);
    	case 'E':
    	case 'e': return(CELL_RIGHT);
    	case 'W':
    	case 'w': return(CELL_LEFT);
    	}
    }

    public int playerIndex(DashChip ch)
    {	int ind = ch.colorIndex();
      	return((ind>=0)?getColorMap()[ind]:ind); 
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	DashMovespec m = (DashMovespec)mm;
        if(replay==replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(replay);
            break;
        case MOVE_FLIP:
          	 G.Assert((pickedObject==null),"can't flip; something is moving");
        	doFlip(m);
        	break;
        case MOVE_BOARD_BOARD:
        	// used by the robot
    		G.Assert((pickedObject==null),"something is moving");
        	switch(board_state)
        	{	default: throw G.Error("Not expecting move in state %s",board_state);
        		case MOVE_STATE:
        			doMove(m,replay);
        			acceptPlacement();
        			setState(DashState.CONFIRM_STATE);
        			break;
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
			switch(board_state)
			{ default: throw G.Error("Not expecting drop in state %s",board_state);
			  case PUZZLE_STATE:  break;
			  case CONFIRM_STATE: unDropObject(); unPickObject(); break;
			  case MOVE_STATE:

				  break;
			}
			DashCell dest = getCell(m.to_col, m.to_row);
            dropObject(dest);
            if(replay==replayMode.Single)
            {
            	animationStack.push(pickedSource);
            	animationStack.push(getCell(DashId.BoardLocation,m.to_col, m.to_row));
            }
            setNextStateAfterDrop(replay);
            
            break;

        case MOVE_PICKB:
        	{
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	DashCell src = getCell(m.from_col,m.from_row);
        	if(isDest(src))
        		{	unDropObject();
        			setState(DashState.MOVE_STATE);
        		}
        	else
        	{	pickObject(src);
        	}
        	}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	DashCell c = getCell(m.object, m.to_col, m.to_row);
        	dropObject(c);
        	setNextStateAfterDrop(replay);
        	}
        	break;

        case MOVE_PICK:
        	{
        	DashCell src = getCell(m.object, m.from_col, m.from_row);	
            pickObject(src);
        	}
            break;

        case MOVE_START:
            setWhoseTurn(m.player);
            flippedCell = null;
            setState(DashState.PUZZLE_STATE);

             {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
   
            break;

        case MOVE_RESIGN:
            setState(unresign==null?DashState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setState(DashState.PUZZLE_STATE);

            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        default:
        	cantExecute(m);
        }


        return (true);
    }

    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
			return(false);
        case PUZZLE_STATE:
        	return(true);
        }
    }
  
    // true if it's legal to drop a chip  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(DashCell fromCell,DashChip gobblet,DashCell toCell)
    {	
		return(false);

    }
    
    public boolean flippable(DashCell c)
    {	if(!c.isTileOnly()) {return(false); }
    	if(c.isEdgeCell()) { return(false); }
    	DashCell cm1 = c.exitTo(CELL_DOWN);
    	if((cm1==null) || !cm1.isTileOnly()) { return(false); }
    	DashCell cm2 = c.exitTo(CELL_LEFT);
    	if((cm2==null) || !cm2.isTileOnly()) { return(false); }
    	DashCell cm3 = c.exitTo(CELL_DOWN_LEFT);
    	if((cm3==null) || !cm3.isTileOnly()) {return(false); }
    	return(true);
     }
    public boolean LegalToHitIntersection(DashCell cell)
    {	boolean rval = false;

    	if(pickedSource==null)
    	{
     	switch(board_state)
    	{
    	case MOVE_STATE:
       		if((cell.topChip()==playerChip[whoseTurn]) 
       			&& (getRiverMovesFrom(cell,null)>0)
       				)
       			{ rval = true; }
       		break;

    	default: throw G.Error("Not expecting state %s",board_state);
    	case PUZZLE_STATE:
    		rval = cell.chipIndex>0;	// something to hit
    		break;
    	case GAMEOVER_STATE: 
    	case DRAW_STATE:
    	case RESIGN_STATE:
    		rval = false;
    		break;
    	case CONFIRM_STATE: 
    		break;
    	}
     }
    else
    {	if(cell==pickedSource) { return(true); }
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case DRAW_STATE:
    	case GAMEOVER_STATE: 
    		return(false);
    	case CONFIRM_STATE:
    		{ Hashtable<DashCell,DashCell> moves = riverMoveDests();
    			  if(moves.get(cell)!=null) { return(true); }
    			}
    			return(false);
    			
      	case PUZZLE_STATE:
    		rval = pickedSource.chipIndex<MAX_CHIP_HEIGHT*2;	// can drop anywhere
    	}
    }
   	return(rval);
   }
    // true if legal to hit this BOARD to flip it over.  The point appears at the
    // upped left corner
   public boolean LegalToHitBoard(DashCell cell)
    {	
    switch (board_state)
        {

		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw  G.Error("Not expecting state %s", board_state);
 	   case MOVE_STATE:
       case PUZZLE_STATE:
        	return((pickedObject==null) && flippable(cell));
        }
    }
  public boolean canDropOn(DashCell cell)
  {	
  	return((pickedSource!=null)				// something moving
  			&&(pickedSource.onBoard 
  					? (cell!=pickedSource)	// dropping on the board, must be to a different cell 
  					: (cell==pickedSource))	// dropping in the rack, must be to the same cell
  				);
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(DashMovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //System.out.println("Ex "+m+" "+getCell('H',4).region_size);
        if (Execute(m,replayMode.Replay))
        {	switch(m.op)
        	{
        	case MOVE_FLIP:
        	case MOVE_BOARD_BOARD:
        		if(board_state==DashState.CONFIRM_STATE) { doDone(replayMode.Replay); }
        		break;
        	case MOVE_DONE:	break;
            default:
            	if (DoneState())
            	{
                doDone(replayMode.Replay);
            	}
            	else
            	{
            		throw G.Error("Robot move should be in a done state");
            	}
        	}
        }

    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(DashMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
     	//System.out.println("un "+m+" "+getCell('H',4).region_size);
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_FLIP:
   	    	{
   	    	DashCell c = getCell(m.from_col,m.from_row);
   	   	 	flipCell(c);
   	    	}
   	    	break;
        case MOVE_DONE:
            break;
        case MOVE_BOARD_BOARD:
        	{
        		DashCell src = getCell(m.from_col,m.from_row);
        		DashCell dest = getCell(m.to_col,m.to_row);
        		droppedDest = dest;
        		pickedSource = src;
        		unDropObject();
        		unPickObject();
        	}
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
         			
        		    break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
		flippedCell = null;
        setState(m.state);
        if(m.player!=whoseTurn) 
        	{ moveNumber--; 
        	setWhoseTurn(m.player);
        	}
 }
    
// get the list of tiles that can be flipped.  If "all" is null then just get the count
 void getListOfFlipMoves(CommonMoveStack  all)
 {	
	switch(board_state)
	{
	case FLIP_STATE:
		for(DashCell c = allCells; c!=null; c=c.next)
		{	if(flippable(c)) 
 			{ if(all!=null) { all.addElement(new DashMovespec(c.col,c.row,whoseTurn)); }
 			}
		}
		break;
	default: break;
	}
 }
 //
 // dests for river moves from "from" starting at "first"
 // if from is null initialize a new sweep
 // if dests is null just count
 //
 private int legalRiverMoveDests(DashCell from,DashCell first,DashCell dests[],int n,CommonMoveStack  all)
 {	 if(from==null) 
 		{ from = first; sweep_counter++; first.sweep_counter = sweep_counter; 
 		}
 		else
 		{ from.sweep_counter = first.sweep_counter;
 		}
	 for(int step = 0,direction = CELL_DOWN_LEFT;
	 	 step<4;
	 	 direction+=2,step++)
	 	{
		 DashCell dest = from.canMoveInDirection(direction);
		 if((dest!=null) && (dest.sweep_counter!=first.sweep_counter))
		 {	if(dests!=null)
			 { dests[n]=dest;
			 }
		 	if(all!=null)
		 	{	all.addElement(new DashMovespec(first.col,first.row,dest.col,dest.row,whoseTurn));
		 	}
		 	n++;
		 	n=legalRiverMoveDests(dest,first,dests,n,all);
		 }
	 	}
	 return(n);
 }
 //
 // dests for river moves from "from" starting at "first"
 // if from is null initialize a new sweep
 // if dests is null just count
 //
 private int markRegion(DashCell from,int n,int sz)
 {	 from.sweep_counter = sweep_counter;
 	 from.region_size=sz;
	 for(int step = 0,direction = CELL_DOWN_LEFT;
	 	 step<4;
	 	 direction+=2,step++)
	 	{
		 DashCell dest = from.riverInDirection(direction);
		 if((dest!=null) && (dest.sweep_counter!=sweep_counter))
		 {	
		 	n++;
		 	n=markRegion(dest,n,sz);
		 }
	 	}
	 return(n);
 }
 // fill the "size" cell of the region
 private int fillRegionSize(DashCell from,int sz)
 {
	sweep_counter++;
	return(markRegion(from,0,sz));
 }
 // invalidate the region size of all the cells associated with "from"
 // this is done for a cell that's about to flip
 private void invalidateRegionSize(DashCell flipped)
 {	
 	DashCell from = flipped.exitTo(CELL_DOWN_LEFT);
 	invalidateRegion(from);
 	invalidateRegion(flipped.exitTo(CELL_LEFT));
 }
 private void invalidateRegion(DashCell from)
 {	boolean moved = false;
 	for(int step = 0,direction = CELL_DOWN_LEFT;
 	 		step<4;
 	 		direction+=2,step++)
 		{
		 DashCell otherCell = from.exitTo(direction);	// cell that represents the desgination
		 if(otherCell!=null)
		 {
			 DashCell river = DashBoard.riverInDirectionOfCell(from,direction,otherCell);
			 if(river!=null)
			 {	if(!moved) { moved = true; fillRegionSize(river,-1); }
			 }
			 else { fillRegionSize(otherCell,-1); }
		 }
 		}
 }
 // mark the region associated with from with its proper size
 private void mark1RegionSize(DashCell from)
 { 	int n = fillRegionSize(from,-1);
 	int nn = fillRegionSize(from,n+1);
 	G.Assert(nn==n,"same size");
 }
 private void markRegionSizes(DashCell flipped)
 {	
 	DashCell from = flipped.exitTo(CELL_DOWN_LEFT);
	 markRegion(from);	// might have formed a single cell region
	 markRegion(flipped.exitTo(CELL_LEFT));
 }
 private void markRegion(DashCell from)
 {	boolean moved = false;
	 mark1RegionSize(from);
	 for(int step = 0,direction = CELL_DOWN_LEFT;
 	 		step<4;
 	 		direction+=2,step++)
 		{
		 DashCell otherCell = from.exitTo(direction);	// cell that represents the desgination
		 if(otherCell!=null)
		 {
			 DashCell river = DashBoard.riverInDirectionOfCell(from,direction,otherCell);
			 if(river!=null)
			 {	if(!moved) { moved = true; mark1RegionSize(river); }
			 }
			 else { mark1RegionSize(otherCell); }
		 }
 		}
 }


 private int getRiverMovesFrom(DashCell c,CommonMoveStack  v)
 {	if(c.onBoard && (c.topChip()==playerChip[whoseTurn]))
 		{ return(legalRiverMoveDests(null,c,null,0,v));
 		}
 	return(0);
 }
 

 void getListOfRiverMoves(CommonMoveStack  all)
 {	
	switch(board_state)
	{
	case MOVE_STATE:
		{
		for(DashCell c = allCells; c!=null; c=c.next)
		{	if(c.chipIndex>0) 
 			{ DashChip chip = c.topChip();
  			  if(chip==playerChip[whoseTurn])
 				  {
  				  legalRiverMoveDests(null,c,null,0,all);
  				  }
 			}
		}
		}
		break;
	default: break;
	}
	
 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	getListOfFlipMoves(all);
 	getListOfRiverMoves(all);

  	return(all);
 }
 // temporary list of destination cells allocate as a resource for speed
 private DashCell[][]tempDestResource = new DashCell[6][];
 private int tempDestIndex=-1;
 public synchronized DashCell []getTempDest() 
 	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
 	  return(new DashCell[((ncols-2)*(nrows*2))/2]);
 	}
 public synchronized void returnTempDest(DashCell[]d) { tempDestResource[++tempDestIndex]=d; }
 // get the list of moves from the current picked source
 public Hashtable<DashCell,DashCell>  riverMoveDests()
 {	return(riverMoveDests(pickedSource));
 }
 public Hashtable<DashCell,DashCell>  riverMoveDests(DashCell cell)
 {	Hashtable<DashCell,DashCell>  dd = new Hashtable<DashCell,DashCell> ();
  	if((cell!=null) && cell.onBoard)
 	{	switch(board_state)
	 	{
		default: break;
		case MOVE_STATE:
		  DashCell tempDests[]=getTempDest();
 		  int dests = legalRiverMoveDests(null,cell,tempDests,0,null);
  		  for(int i=0;i<dests;i++) { dd.put(tempDests[i],tempDests[i]); }
  		  returnTempDest(tempDests);
  	  	}
 	}
 return(dd);
 }
 
 public int countInDirection(String mspec,int direction)
 {	int len = mspec.length();
 	String dname = DIRECTION_NAMES[direction];
 	char dc = dname.charAt(0);
 	int count = 0;
 	G.Assert(dname.length()==1,"primary directions only");
 	for(int i=0;i<len;i++)
 	{	if(mspec.charAt(i)==dc) { count++; }
 	}
 	return(count);
 }

  static final int colorInDirection(DashChip chip,int dir)
	{	int colorInfo[]=chip.colorInfo;
		if(colorInfo!=null)
		{	dir = (dir+8)&7;
			switch(dir)
			{	
			default: throw G.Error("Bad direction");
			case CELL_LEFT: return((colorInfo[3]<<2) | colorInfo[0]);
			case CELL_UP: return((colorInfo[0]<<2) | colorInfo[1]);
			case CELL_RIGHT: return((colorInfo[1]<<2) | colorInfo[2]);
			case CELL_DOWN: return((colorInfo[2]<<2) | colorInfo[3]);
			}
		}
		return((DashChip.N<<2) | DashChip.N);
	}
	static final int colorInInverseDirection(DashChip chip,int dir)
	{	int colorInfo[] = chip.colorInfo;
		if(colorInfo!=null)
		{	dir = (dir+8)&7;
			switch(dir)
			{	
			case CELL_LEFT: return((colorInfo[0]<<2) | colorInfo[3]);
			case CELL_UP: return((colorInfo[1]<<2) | colorInfo[0]);
			case CELL_RIGHT: return((colorInfo[2]<<2) | colorInfo[1]);
			case CELL_DOWN: return((colorInfo[3]<<2) | colorInfo[2]);
			default:
				break;
			}
		}
		return((DashChip.N<<2) | DashChip.N);
	}
	
	static final boolean connectsInDirection(DashChip chip,int dir)
	{	int colorInfo[] = chip.colorInfo;
		if(colorInfo!=null)
		{
		dir = (dir+8)&7;
		switch(dir)
		{	
		default: throw G.Error("Bad direction");
		case CELL_DOWN_LEFT: 
			boolean as1 = colorInfo[1]==colorInfo[4];
			return(as1);
		case CELL_UP_LEFT: 
			boolean as2 = colorInfo[2]==colorInfo[4];
			return(as2);
		case CELL_UP_RIGHT: 
			boolean as3 = colorInfo[3]==colorInfo[4];
			return(as3);
		case CELL_DOWN_RIGHT: 
			boolean as4 = colorInfo[0]==colorInfo[4];
			return(as4);
		}
		}
		return(false);
	}
	
	static final public DashCell riverInDirectionOfCell(DashCell thisCell,int dir,DashCell otherCell)
	{	if(otherCell!=null)
		{
		switch(dir)
		{
		default: throw G.Error("Bad Direction");
		case CELL_DOWN_LEFT:
			if(connectsInDirection(thisCell.chipAtIndex(0),dir)) 
				{ connectsInDirection(thisCell.chipAtIndex(0),dir);
				return(otherCell); 
				}
			break;
		case CELL_UP_RIGHT:
				{	
				DashCell adjCell = thisCell.exitTo(CELL_UP_RIGHT);
				if(adjCell!=null) 
					{ if(connectsInDirection(adjCell.chipAtIndex(0),dir))
						{ connectsInDirection(adjCell.chipAtIndex(0),dir);
						return(otherCell);
						}
					}
				break;
				}

		case CELL_DOWN_RIGHT:
			{	DashCell adjCell = thisCell.exitTo(CELL_RIGHT);
				if(adjCell!=null) 
					{ if(connectsInDirection(adjCell.chipAtIndex(0),dir))
						{ connectsInDirection(adjCell.chipAtIndex(0),dir);
						return(otherCell);
						}
					}
				break;
			}
		case CELL_UP_LEFT:
			{	DashCell adjCell = thisCell.exitTo(CELL_UP);
			if(connectsInDirection(adjCell.chipAtIndex(0),dir)) 
				{ connectsInDirection(adjCell.chipAtIndex(0),dir);
				return(otherCell);
				}
			break;
			}
		}}
		return(null);
	}
 }
