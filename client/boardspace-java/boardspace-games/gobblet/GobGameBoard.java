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
package gobblet;

import java.awt.Color;

import lib.*;
import online.game.*;

/**
 * GobGameBoard knows all about the game of Gobblet, which is played
 * on a 4x4 board. It gets a lot of logistic support from 
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
class CellStack extends OStack<GobCell>
{
	public GobCell[] newComponentArray(int n) { return(new GobCell[n]); }
}
class GobGameBoard extends rectBoard<GobCell> implements BoardProtocol,GobConstants
{
    static final GobbletId[] chipPoolIndex = { GobbletId.White_Chip_Pool, GobbletId.Black_Chip_Pool };
    static final int DD_INDEX = 0;		// index into diagInfo for diagonal down
    static final int DU_INDEX = 1;		// index into diagInfo for diagonal up
    static final String[] GOBGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	static final int DEFAULT_BOARDSIZE = 4;	// 4x4 board
	
 	private GobbletState unresign;
 	private GobbletState board_state;
	public GobbletState getState() {return(board_state); }
	public void setState(GobbletState st) 
	{ 	unresign = (st==GobbletState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}	
	public CellStack animationStack = new CellStack();
    public int boardSize = DEFAULT_BOARDSIZE;	// size of the board
    public int numCups = DEFAULT_NCUPS;			// number of cup sizes
    public GobCell[][] rack = null;				// rack for the unused pieces
    public GobCell allRackCells = null;			// cells in the racks
    public GobLine colInfo[]=null;
    public GobLine rowInfo[]=null;
    public GobLine diagInfo[]=null;
    public GobCup allCups[] = new GobCup[numCups*2*(boardSize-1)];	// all the chips
    public GobCup playerChip[] = { GobCup.black,GobCup.white};
    public boolean MEMORY = false;
    public void SetDrawState() { setState(GobbletState.DRAW_STATE); }
	 // small ad-hoc adjustment to the grid positions
	 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
	 {  if(Character.isDigit(txt.charAt(0))) { xpos+=cellsize/3; }
	 	GC.Text(gc, true, xpos, ypos, -1, 0,clt, null, txt);
	 }
	 //
    // private variables
    //	
    private int chips_on_board = 0;			// number of chips currently on the board
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public GobCup pickedObject = null;
    private GobCell pickedSource = null;
    private GobCell droppedDest = null;
    
     
    public GobCell GetRackCell(int player,int idx)
    {	return(rack[player][idx]);
    }
	// factory method
	public GobCell newcell(char c,int r)
	{	return(new GobCell(c,r));
	}
    public GobGameBoard(String init,int[]map) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = GOBGRIDSTYLE; //coordinates left and bottom
    	setColorMap(map, 2);
        doInit(init); // do the initialization 
     }
    public GobGameBoard cloneBoard() 
	{ GobGameBoard dup = new GobGameBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((GobGameBoard)b); }

    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	Random r = new Random(23782746);
    	if(Gobblet_INIT.equalsIgnoreCase(game)) { MEMORY=false; boardSize=DEFAULT_BOARDSIZE; numCups=DEFAULT_NCUPS; }
     	else if(GobbletM_INIT.equalsIgnoreCase(game)) { MEMORY=true; boardSize=DEFAULT_BOARDSIZE; numCups=DEFAULT_NCUPS; }
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(GobbletState.PUZZLE_STATE);
        animationStack.clear();
        initBoard(boardSize,boardSize); //this sets up the board and cross links
        allCells.setDigestChain(r);
        
        // initialize lineinfo for scoring
        rack = new GobCell[2][numCups-1];
        rowInfo = new GobLine[numCups];
        colInfo = new GobLine[numCups];
        diagInfo = new GobLine[numCups];
        for(int i=0; i<numCups; i++)
        {	rowInfo[i]=new GobLine();
        	colInfo[i]=new GobLine();
        	if(i<2) { diagInfo[i]=new GobLine(); }
        }
        // distribute the info to the cells
        for(GobCell c=allCells; c!=null; c=c.next)
        {	c.rowInfo=rowInfo[c.row-1];
        	c.colInfo=colInfo[c.col-'A'];
        	if(c.diagonal_down) { c.diagonalDownInfo = diagInfo[DD_INDEX]; }
        	if(c.diagonal_up) { c.diagonalUpInfo = diagInfo[DU_INDEX]; }
        }
        
        whoseTurn = FIRST_PLAYER_INDEX;
        chips_on_board = 0;
        pickedObject = null;
        pickedSource = null;
        droppedDest = null;

		int cupidx=0;
		int map[]=getColorMap();
		playerChip[map[FIRST_PLAYER_INDEX]] = GobCup.white;
		playerChip[map[SECOND_PLAYER_INDEX]] = GobCup.black;
		
		for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
		{ for(int j=0; j<numCups-1; j++)
			{	GobCell nc = new GobCell(r,(char)('0'+i),j);
				nc.rackLocation = chipPoolIndex[i];
				for(int csiz = 0;csiz<numCups;csiz++)
				{	int cupn = cupidx++;
					nc.addChip(allCups[cupn]=GobCup.getCup(i,csiz));
				}
				rack[map[i]][j]= nc;
			}
		}
    }
    public void sameboard(BoardProtocol f) { sameboard((GobGameBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(GobGameBoard from_b)
    {	super.sameboard(from_b);

    	G.Assert(sameCells(rack,from_b.rack), "rack mismatch");
        G.Assert(pickedObject == from_b.pickedObject,"pickedObject matches");
        G.Assert(GobCell.sameCell(pickedSource,from_b.pickedSource),"pickedSource matches");
        G.Assert(GobCell.sameCell(droppedDest,from_b.droppedDest),"droppedDest matches");

        // here, check any other state of the board to see if
        G.Assert(chips_on_board == from_b.chips_on_board , "chips_on_board mismatch");
    }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
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
       
        v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        v ^= chip.Digest(r,pickedObject);
        v ^= cell.Digest(r,pickedSource);
        return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(GobGameBoard from_b)
    {	super.copyFrom(from_b);
        MEMORY = from_b.MEMORY;
        chips_on_board = from_b.chips_on_board;
        whoseTurn = from_b.whoseTurn;
        board_state = from_b.board_state;
        pickedObject = from_b.pickedObject;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = getCell(from_b.droppedDest);
        copyAllFrom(rack,from_b.rack);
        unresign = from_b.unresign;

        if(G.debug()) { sameboard(from_b); }
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;

       Init_Standard(gtype);
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
    	return(DigestState());
    }
   public boolean DigestState()
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
   private int playerIndex(GobCup ch) { return(getColorMap()[ch.colorIndex]);}
   
   public boolean uniformRow(int player,GobCell cell,int dir)
    {	for(GobCell c=cell.exitTo(dir); c!=null; c=c.exitTo(dir))
    	{    GobCup cup = c.topChip();
    	     if(cup==null) { return(false); }	// empty cell
    	     if(playerIndex(cup)!=player) { return(false); }	// cell covered by the other player
    	}
    	return(true);
    }
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(GobbletState.GAMEOVER_STATE);
    }
    
    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public boolean WinForPlayerNow(int player)
    {	if(board_state==GobbletState.GAMEOVER_STATE) { return(win[player]); }
    	GobCell corner = getCell('A',1);
    	GobCell lastCell = null;
    	for(GobCell cell=corner; cell!=null; cell=cell.exitTo(CELL_UP))
    	{ GobCup cup = cell.topChip();
    	  if((cup!=null) && (playerIndex(cup)==player) && uniformRow(player,cell,CELL_RIGHT)) 
    	  	{ return(true); }
    	  lastCell=cell;
   	}
    	for(GobCell cell=corner; cell!=null; cell=cell.exitTo(CELL_RIGHT))
    	{ GobCup cup = cell.topChip();
    	  if((cup!=null) && (playerIndex(cup)==player) && uniformRow(player,cell,CELL_UP)) 
    	  	{ return(true); }
    	}
    	GobCup cup = corner.topChip();
    	if((cup!=null)&&(playerIndex(cup)==player)&&uniformRow(player,corner,CELL_UP_RIGHT))
    		{ return(true); }
    	cup = lastCell.topChip();
    	if((cup!=null)&&(playerIndex(cup)==player)&&uniformRow(player,lastCell,CELL_DOWN_RIGHT)) 
    		{ return(true); }
 
    	return(false);
     }

    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot,int lvl)
    {  	GobCell corner = getCell('A',1);
    	GobCell lastCell = null;
    	double score = 0.0;
    	String msg="";
    	
       	// if he has a big cup, our small chips don't count
    	//int finalv = dumbot ? mycups : mycups-hiscups-((hisbigcups>0)?(mycups-mybigcups):0);
 
    	// score the rows
    	for(GobCell cell=corner; cell!=null; cell=cell.exitTo(CELL_UP))
    	{ GobLine info = cell.rowInfo;
    	  lineProfile(cell,info,player,CELL_RIGHT);
    	  int tscore = dumbot ? info.myCups : info.lineScore();
    	  score += tscore;
    	  if(print) { msg += " "+tscore; }
    	  lastCell=cell;
    	}
    	
    	if(print) { msg += " | "; }
    	// score the columns
    	for(GobCell cell=corner; cell!=null; cell=cell.exitTo(CELL_RIGHT))
    	{ GobLine info = cell.colInfo;
    	  lineProfile(cell,info,player,CELL_UP);
    	  int tscore = dumbot ? info.myCups : info.lineScore();
    	  score += tscore;
    	  if(print) { msg += " "+tscore; }
     	}
    	if(print) { msg += " | "; };
	  	// score the upward diagonal
    	{
	  	  GobLine info = corner.diagonalUpInfo;
	   	  lineProfile(corner,info,player,CELL_UP_RIGHT);
		  int tscore = dumbot ? info.myCups : info.lineScore();
		  score += tscore;
		  if(print) { msg += " "+tscore; }
	  	  }
  	  	// score the downward diagonal
		{
		GobLine info = lastCell.diagonalDownInfo;
		lineProfile(lastCell,info,player,CELL_DOWN_RIGHT);
		int tscore = dumbot ? info.myCups : info.lineScore();
		score += tscore;
		if(print) { msg += " "+tscore; }
		}

		// score the cup nesting and multiline confluence
		{
		String cmsg = "chips: ";
		int mlscore = 0;
 	  	int cupscore = 0;
 	  	int playerColor = getColorMap()[player];
	 	for(GobCell c = allCells; c!=null; c=c.next)
	 	  	{	int cs = c.cupScore(playerColor);
	 	  		if(cs!=0) { if(print) { cmsg += " "+cs; } cupscore += cs; }
	 	  		int mls = c.mlScore(playerColor);
	 	  		if(mls!=0) {  mlscore += mls; }
	 	  	}
	 	
	 	if(cupscore!=0)
	 	{
	 	score += cupscore*cup_weight;
	 	if(print) 
	 		{ System.out.println(cmsg); msg += " cv: "+cupscore; 
	 		}
		}
	 	if(mlscore!=0)
	 	{	score += mlscore*ml_weight;
 			if(print) { System.out.println(cmsg); msg += " ml: "+mlscore; }
	 	}
		}
		
 	  	if(print) { System.out.println((dumbot?"dev":"ev")+player+": "+msg+ " = "+score); }
  	    return(score);
    }
    

    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	if(droppedDest!=null)
    	{
        pickedObject = null;
        droppedDest = null;
        pickedSource = null;
    	}
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    GobCell dr = droppedDest;
    if(dr!=null)
    	{
    	droppedDest = null;
    	dr.removeTop(pickedObject);
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	GobCup po = pickedObject;
    	if(po!=null)
    	{
    	GobCell ps = pickedSource;
    	pickedSource=null;
    	pickedObject = null;
    	ps.addChip(po);
    	}
     }
    // 
    // drop the floating object.
    //
    private void dropObject(GobCell dest)
    {	
       G.Assert((pickedObject!=null)&&(droppedDest==null),"ready to drop");
       droppedDest = dest;
       dest.addChip(pickedObject);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(GobCell cell)
    {	return(droppedDest==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	GobCup ch = pickedObject;
    	if((ch!=null)&&(droppedDest==null))
    		{ return(ch.chipIndex);
    		}
        return (NothingMoving);
    }
   
    public GobCell getCell(GobCell c) { return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));}
    private GobCell getCell(GobbletId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);

        case BoardLocation:
        	return(getCell(col,row));
        case White_Chip_Pool:
          	return(rack[getColorMap()[FIRST_PLAYER_INDEX]][row]);
        case Black_Chip_Pool:
        	return(rack[getColorMap()[SECOND_PLAYER_INDEX]][row]);
        }
	
    }
    
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(GobCell source)
    {	G.Assert((pickedObject==null)&&(pickedSource==null),"ready to pick");
    	pickedSource = source;
    	pickedObject = source.removeChip();
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
        	if(droppedDest!=null) { setNextStateAfterDone(); }
        	break;
        case PICKED_STATE:
        case PLAY_STATE:
			setState(GobbletState.CONFIRM_STATE);
			break;

        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(char col, int row)
    {
        return ((pickedSource!=null) 
        		&& (pickedSource.col == col) 
        		&& (pickedSource.row == row));
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterPick()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setState(GobbletState.PLAY_STATE);
        	break;
        case PLAY_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    	case PICKED_STATE:
    		setState(GobbletState.PLAY_STATE);
    		break;
    	}

    }
   

    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==GobbletState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }
    public boolean canUndoPick()
    {	return((board_state==GobbletState.PUZZLE_STATE) || (!(MEMORY && (pickedSource!=null) && (pickedSource.onBoard))));
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	GobMovespec m = (GobMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
         			GobCell src = getCell(m.source, m.from_col, m.from_row);
        			GobCell dest = getCell(GobbletId.BoardLocation,m.to_col,m.to_row);
        			m.covered = dest.topChip();
                    pickObject(src);
                    m.moved = pickedObject;
                    dropObject(dest); 
                    if(replay.animate)
                    {
                    	animationStack.push(src);
                    	animationStack.push(dest);
                    }
                    setNextStateAfterDrop();
                    break;
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	{
        		GobCell src = getCell(GobbletId.BoardLocation, m.from_col, m.from_row);
        		GobCell dest = getCell(GobbletId.BoardLocation,m.to_col,m.to_row);
        		m.covered = dest.topChip();
        		pickObject(src);
        		m.moved = pickedObject;        	
        		dropObject(dest); 
                if(replay.animate)
                {
                	animationStack.push(src);
                	animationStack.push(dest);
                }
			    setNextStateAfterDrop();
         	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
			switch(board_state)
			{ default: throw G.Error("Not expecting drop in state %s",board_state);
			  case PUZZLE_STATE:  break;
		      case DRAW_STATE:
			  case CONFIRM_STATE: unDropObject(); unPickObject(); break;
			  case PLAY_STATE:
			  case PICKED_STATE:

				  break;
			}
			{
			GobCell dest = getCell(GobbletId.BoardLocation, m.to_col, m.to_row);
			m.covered = dest.topChip();
            if(replay==replayMode.Single)
            {
            	animationStack.push(pickedSource);
            	animationStack.push(dest);
            }
            dropObject(dest);
            if(pickedSource==droppedDest) 
            	{ unDropObject(); 
            	  unPickObject(); 

            	} 
            	else
            		{
            		setNextStateAfterDrop();
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(canUndoPick()?GobbletState.PLAY_STATE:GobbletState.PICKED_STATE);
        		}
        	else 
        		{ GobCell src = getCell(GobbletId.BoardLocation, m.from_col, m.from_row);
        		  pickObject(src);
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
        		  m.moved = pickedObject;
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case PLAY_STATE:
        		  		// if we pick a piece off the board, we might expose a win for the other player
        		  		// and otherwise, we are comitted to moving the piece
        		  		if(MEMORY)
    		  			{ setState(GobbletState.PICKED_STATE);
    		  			}
         		  		break;
        		  	case PUZZLE_STATE:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	GobCell dest = getCell(m.source, m.to_col, m.to_row);
        	if(replay==replayMode.Single)
        	{
        		animationStack.push(pickedSource);
            	animationStack.push(dest);	
        	}
        	m.covered = dest.topChip();
            dropObject(dest);
            setNextStateAfterDrop();
        	}
            break;

        case MOVE_PICK:
        	{
            unDropObject();
            unPickObject();
            GobCell src = getCell(m.source, m.from_col, m.from_row);
            pickObject(src);
            m.moved = pickedObject;
            setNextStateAfterPick();
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            setState(GobbletState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?GobbletState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setState(GobbletState.PUZZLE_STATE);

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
        	throw G.Error("Not expecting state %s", board_state);
         case CONFIRM_STATE:
         case DRAW_STATE:
         case PLAY_STATE: 
        	return((pickedObject==null)
        			?(player==whoseTurn)
        			:((droppedDest==null) && (pickedSource.onBoard==false)&&(player==playerIndex(pickedObject))));


        case PICKED_STATE:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }
    
    // this is the basic count for score estimation.  Count chips of your color, 
    // chips of the oppoising color, and big chips of the opponent as extra important.
    void lineProfile(GobCell centerCel,GobLine info,int forPlayer,int dir)
    {	GobCell cc=centerCel;
    	int mycups=0;
    	int hiscups=0;
    	int mybigcups=0;
    	int hisbigcups=0;
    	while(cc!=null)
    	{	GobCup c = cc.topChip();
    		int cs = (c==null) ? -1 : c.size+4*((playerIndex(c)==forPlayer)?0:1);		// size of cup or -1
    		// encode my chips as 0-3, his chips as 4-7 empty as -1
    		switch(cs)
    		{
    		default: throw G.Error("not expecting %s",cs);
    		case -1:	// empty cell
    			break;
    		case 3: mybigcups++;
				//$FALL-THROUGH$
			case 0:
    		case 1:
    		case 2: mycups++;
    			break;
    		case 7: hisbigcups++;
				//$FALL-THROUGH$
			case 4:
    		case 5:
    		case 6:
    			hiscups++;
    			break;
    		}
    		cc = cc.exitTo(dir);
    	}
    	
    	info.myBigCups = mybigcups;
    	info.myCups = mycups;
    	info.hisCups = hiscups;
    	info.hisBigCups = hisbigcups;
      }
    boolean rowOfThree(int pl,GobCell cell,int dir)
	{	// note that this is sometimes called when we know the center cell
    	// is the opposite color, so we are looking 3 as an indicator of
    	// a cup that can't be picked up.  Other times we're called with
    	// unknown contents to see if a cup can be dropped on.
		int ninrow=0;
		GobCup top = cell.topChip();
		if((top!=null)&&(playerIndex(top)==pl)) { ninrow++; }
		for(GobCell nc = cell.exitTo(dir);
		    nc!=null;
		    nc=nc.exitTo(dir))
		{ GobCup tc = nc.topChip();
		  if((tc!=null)&& (playerIndex(tc)==pl)) { ninrow++; }
		}
		for(GobCell nc =cell.exitTo(4+dir);
	    nc!=null;
	    nc=nc.exitTo(4+dir))
		{ GobCup tc = nc.topChip();
		  if((tc!=null)&& (playerIndex(tc)==pl)) { ninrow++; }
		}
		if(ninrow>=3)
		{ return(true); }
		return(false);
		}
    
    // true if it's legal to drop gobblet  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(GobCell fromCell,GobCup gobblet,GobCell toCell)
    {	GobCup cup = toCell.topChip();
    	if(MEMORY && (fromCell==toCell)) { return(false); }
		if(gobblet==null)
		{	// pick in a game is legal if it's your color
			if(cup==null) { return(false); }
			if(playerIndex(cup)==whoseTurn) {return(true); }
		}
		else
		{	// drop in a game is legal if the cell is empty or if the moving
			// cup is from the board, or if it is to block a win.
			if(cup==null) 
				{ return(true); 
				}
			if(cup.size>=gobblet.size) 
				{ return(false); }	// too big to eat
			if(fromCell.onBoard==true) 
			{ return(true); }
			
			if(playerIndex(cup)==whoseTurn) 
				{ // I have an explicit ruiling from Thierry that this should never be allowed,
				  // even considering that this might be the only way to prevent a loss.  For
				  // about the first year, the was "return true" until someone pointed it out.
				return(false); 
				}	// gobbling own piece
			// gobbling an opponent's piece, only permitted if the piece is part
			// of a row of 3
			int pl = playerIndex(cup);
			if(rowOfThree(pl,toCell,CELL_UP)) 
				{ return(true); }
			if(rowOfThree(pl,toCell,CELL_RIGHT)) 
				{ return(true); }
			if(toCell.diagonal_up && rowOfThree(pl,toCell,CELL_UP_RIGHT))
				{ return(true); }
			if(toCell.diagonal_down && rowOfThree(pl,toCell,CELL_DOWN_RIGHT)) 
				{return(true); }
		}
		return(false);

    }
    public boolean LegalToHitBoard(GobCell cell)
    {	
        switch (board_state)
        {
        case PICKED_STATE:
		case PLAY_STATE:
			return(LegalToDropOnBoard(pickedSource,pickedObject,cell));

		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	GobCup cup = cell.topChip();
        	return( (pickedObject==null) 
        				? (cup!=null)
        			    : ((cup==null)||(cup.size<pickedObject.size)));
        }
    }
  public boolean canDropOn(GobCell cell)
  {	GobCup top = cell.topChip();
  	return((pickedObject!=null)				// something moving
  			&&(pickedSource.onBoard 
  					? (cell!=pickedSource)	// dropping on the board, must be to a different cell 
  					: (cell==pickedSource))	// dropping in the rack, must be to the same cell
  			&&((top==null)	// either an empty cell or one with a smaller gobblet
  				||(top.size<pickedObject.size)));
  }
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(GobMovespec m)
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
    public void UnExecute(GobMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);

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
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			
        			pickObject(getCell(GobbletId.BoardLocation,m.to_col,m.to_row));
        			dropObject(getCell(m.source,m.from_col, m.from_row));
       			    acceptPlacement();
                    break;
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			pickObject(getCell(GobbletId.BoardLocation, m.to_col, m.to_row));
       			    dropObject(getCell(GobbletId.BoardLocation, m.from_col,m.from_row)); 
       			    acceptPlacement();
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	int sizesOn = 0;
 	GobCell rackCells[] = rack[whoseTurn];
 	int map[] = getColorMap();
 	// handle the onboard moves
 	for(int i=0;i<rackCells.length; i++)
 		{ GobCell cell = rackCells[i];
 		  GobCup cup = cell.topChip();
 		  if(cup!=null)
 		  {	int cupsize = 1<<cup.size;	// optimize by doing each cup size only once
 		  	if((sizesOn&cupsize)==0)
 		  	{	sizesOn |= cupsize;
 		  		for(GobCell dcell=allCells; dcell!=null; dcell=dcell.next)
 		  		{	if(LegalToDropOnBoard(cell,cup,dcell))
 		  			{ 	all.addElement(new GobMovespec("OnBoard "+chipColorString[map[whoseTurn]]+" "+i+" "+cup.size+" "+dcell.col+" "+dcell.row,whoseTurn));
 		  			}
 		  		}
 		  	}
 			  
 		  }
 		}
 	// handle all inboard moves
 	for(GobCell from=allCells; from!=null; from=from.next)
 	{	GobCup cup = from.topChip();
 		if((cup!=null)&&(playerIndex(cup)==whoseTurn))
 		{	
 			for(GobCell to=allCells; to!=null; to=to.next)
 			{ if((to!=from) 
 					&& LegalToDropOnBoard(from,cup,to)
 					)
 				{ 
 				all.addElement(new GobMovespec("Move "+from.col+" "+from.row+" "+cup.size+" "+to.col+" "+to.row,whoseTurn));
 				}
 			}
 		}
 	}
 	if(all.size()==0)
 	{	// no legal moves, which can happen if all the technically legal
 		// moves are suicude.  Resign
 		all.addElement(new GobMovespec(RESIGN,whoseTurn));
 	}
  	return(all);
 }
 
}
