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
package quinamid;

import online.game.*;
import lib.*;


/**
 * QuinamidBoard knows all about the game of Quinamid, which is played
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
 *  In general, the undoInfo of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit undoInfo variable.  All the transitions specified
 *  by moves are mediated by the undoInfo.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each undoInfo, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class QuinamidBoard extends rectBoard<QuinamidCell> implements BoardProtocol,QuinamidConstants
{	// robot parameters
	private QuinamidState unresign;
	private QuinamidState board_state;
	public QuinamidState getState() {return(board_state); }
	public CellStack animationStack = new CellStack();
	public void setState(QuinamidState st) 
	{ 	unresign = (st==QuinamidState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	double FRIENDLY_ASSISTANCE_BONUS = 0.2;	// bonus for n friendly pieces in a row that might win
    double UNBALANCE_PENALTY = 1.0;			// penalty for having more control in one direction than others.
    double COMPLETE_ROW_WEIGHT = 1.5;		// weight for the complete-row part of the calculation
    double VISIBLE_CHIP_BONUS = 4.0;		// weight for currently visible chips
    double UPPER_LEVEL_BONUS = 1.0;			// increasing weight on higher levels of the board

	/**
	QuinamidBoard has a dual representation; a conventional "flat" 6x6t board which 
	represents the visible cells of the stack, and a stack of "edge boards", one for
	each layer, including the base layer.  Cells in the visible board are paired with
	cells in one of the edge boards, but of course, some cells in the edge board are
	not paired, because they are currently invisible.
	
	Edge boards use a one dimensional "racetrack" to represent their cells, plus row and column
	offsets from the board below, which are always 0 or 1, and a rotation which is 0-3 quarter turns.
	
	When any of the edge boards are moved, the links between currently visible cells and 
	edge cells are re-established, and all the currently visible cells contents are copied
	into the flat board.  Likewise, when any cell in the flat board's contents are changed,
	the change is mirrored into the paired EdgeBoard cell.
 	*/
	class EdgeBoard 
	{	int size;		// square size of this board (2-6)
		int col_offset;	// offset from the next lower board (0-1)
		int row_offset;	// offset from the next lower board (0-1)
		int level;		// stack level of this board the level of this board in the stack
		int rot;		// clockwise quarter turn rotations of this board 
		long randomv;	// base digest value for this board
		// this one dimensional array represents a walk around the edge of a 
		// board that's size x size.  So the contents will be (4*(size-1)) cells long
		QuinamidCell contents[] = null;
		EdgeBoard higherBoard = null;		// the board on top of this one, or null
		int chipCount[] = new int[2];
		
		private EdgeBoard(int sz,int lvl,EdgeBoard up)
		{	contents = new QuinamidCell[4*(sz-1)];
			size = sz;
			level = lvl;
			higherBoard = up;
			rot = 0;
			Random r = new Random(1000*sz);
			randomv = r.nextLong();
			for(int lim=contents.length-1; lim>=0; lim--)
			{	contents[lim] = new QuinamidCell(r,BoardCodes[lvl],(char)('a'+level),lim);
			}
			col_offset = 0;
			row_offset = 0;
		}
		public void doInit()
		{	col_offset = 0;
			row_offset = 0;
			rot = 0;
			chipCount[0]=chipCount[1]=0;
			for(int lim = contents.length-1; lim>=0; lim--) 
				{ contents[lim].chip=null;
				  contents[lim].upLink=null; 
				} 
		}
		public void sameBoard(EdgeBoard from_b)
		{	G.Assert(col_offset==from_b.col_offset,"col offset matches");
			G.Assert(row_offset==from_b.row_offset,"row offset matches");
			G.Assert(level==from_b.level, "level matches");
			G.Assert(size==from_b.size,"size matches");
			G.Assert(rot==from_b.rot, "rotation matches");
			G.Assert(chipCount[0]==from_b.chipCount[0], "chipCount on mismatch");
			G.Assert(chipCount[1]==from_b.chipCount[1], "chipCount on mismatch");
			for(int idx = contents.length-1; idx>=0; idx--) { G.Assert(contents[idx].sameCell(from_b.contents[idx]), "stacked board cell matches"); }
		}
		public void copyFrom(EdgeBoard from_b)
		{	col_offset=from_b.col_offset;
			row_offset=from_b.row_offset;
			rot=from_b.rot;
			chipCount[0]=from_b.chipCount[0];
			chipCount[1]=from_b.chipCount[1];
			for(int idx = contents.length-1; idx>=0; idx--) { contents[idx].copyFrom(from_b.contents[idx]); }
		}

		// true of this board can shift in the indicated direction.
		public boolean canShiftWithArrow(MovementZone zone)
		{	if(level==0) { return(false); }		// bottom board can't shift
			if((board_state==QuinamidState.FIRST_PLAY_STATE)||(board_state==QuinamidState.PLAY_OR_SWAP_STATE)) { return(false); }
			if((board_state!=QuinamidState.PUZZLE_STATE) && (level==lastMovedBoard)) 
				{ return(false);	// most recently moved board can't move again during a game 
				}
			if(nextMovedBoard!=-1)
			{
				if((this.level==nextMovedBoard) && (zone.reverseOpcode==nextMovedDirection)) 
					{ return(true); }	// undo
				return(false);
			}
			else if(board_state==QuinamidState.CONFIRM_STATE) { return(false); }
			
			switch(zone)
			{
			case Move_Up: return(row_offset==0);
			case Move_Down: return(row_offset!=0);
			case Move_Left: return(col_offset!=0);
			case Move_Right: return(col_offset==0);
			default: return(false);
			}
		}
		// true of this board can rotate
		public boolean canRotateWithArrow(QIds direction)
		{	if(level==0) { return(false); }	// bottom board can't rotate
			switch(board_state)
			{
			case FIRST_PLAY_STATE:
			case PLAY_OR_SWAP_STATE:
				return(false);
				
			case PUZZLE_STATE: break;
			case CONFIRM_STATE: 
				if(nextMovedBoard!=-1) 
				{	// can't move again in the same direciton
					switch(nextMovedDirection)
					{
					case RotateCW:
					case RotateCCW:
						if((this.level!=nextMovedBoard) || (direction==nextMovedDirection)) { return(false); }
						return(true);
					default: return(false); 
					}
				}
				else { return(false); }
			default: if(level==lastMovedBoard) { return(false); }	// can't move again
			}
			return(true);
		}
		private void setOffsets(int col,int row) 
			{ col_offset = col; row_offset = row; 
			}
		private void setRowOffset(int newv) { row_offset = newv; }
		private void setColOffset(int newv) { col_offset = newv; }
		
		// change the rotation of this board and rigidly all the boards above it.
		private void changeRotation(int dif,boolean changeOffsets)
			{ rot = (rot+4+dif)%4; 
			  if(changeOffsets)
			  {	int xx = col_offset;
			  	int yy = row_offset;
			  	if(dif>0)
			  		{ xx = 1-xx; } 	// clockwise rotation
			  		else 
			  		{	yy = 1-yy;	// counterclockwise rotation
			  		}
			  	row_offset = xx;
			  	col_offset = yy;
			  	
			  }
			  if(higherBoard!=null) 
			  	{ higherBoard.changeRotation(dif,true);
			  	}
			}
		public long Digest(Random r)
		{ long v = r.nextLong()*(randomv+(col_offset*10+row_offset*100+rot));
		  for(int lim=contents.length-1; lim>=0; lim--)
		  	{ v ^= contents[lim].Digest(r);
		  	}
		  
		  return(v);
		}
		private void clearUpLinks()
		{	for(int lim=contents.length-1; lim>=0; lim--) { contents[lim].upLink=null; }
			if(higherBoard!=null) { higherBoard.clearUpLinks(); }
		}
		/* link "partner" from the flat board with a cell in some Edge board
		 * 
		 * */
		private QuinamidCell setUpLink(QuinamidCell partner,char col0,int row0)
		{	char col = (char)(col0-col_offset);	// offset by this board's offsets
			int row = row0-row_offset;
			if(! ((row>=1)&&(row<=size)&&(col>='A')&&(col<=(char)('A'+size-1))))
				{ return(null); }					// not inside this board
			if((higherBoard!=null)
					&& (higherBoard.setUpLink(partner,col,row)!=null))
				{ return(partner.upLink); }			// it's on a higher board
			// not on a higher board, and inside this board, so there must be a cell here we
			// should link to, at some position around the contents racetrack
			int index = 0;
			if(row==1) { index = (col-'A'); }	// in row 1, linkto some column
			else if(row==size)		// in the last row
					   { index = (size-1)*3+('A'-col); }	
			else if(col=='A') 		// in the first column
				{ index = (size-1)*4+1-row; }
			else if(col==(char)('A'+size-1)) 	// in the last column
				{ index = (size-1)+row-1; }
			else { return(null); }
			
			index = (index + rot*(size-1))%contents.length;
			QuinamidCell local = contents[index];
			local.upLink = partner;
			partner.upLink = local;
			return(local);
		}
	}

	// counting routines for robot scoring
	int friendly=0;
	int neutral=0;
	public boolean uniform_or_empty_distance(QuinamidCell anchor,int dir,QuinamidChip forChip)
	{	QuinamidCell c = anchor;
		while (c!=null) 
				{ QuinamidChip ch = c.chip;
				  if(ch==null) { neutral++; }
				  else if(ch==forChip) { friendly++; }
				  else { break; }
				  c = c.exitTo(dir); 
				};
			if((friendly+neutral)==4)
				{ c = anchor.exitTo(dir+CELL_HALF_TURN);
				  if(c!=null)
				  	{ QuinamidChip ch = c.chip;
				  	  if(ch==null) { neutral++; }
				  	  else if(ch==forChip) { friendly++; }
				  	}}
			return((friendly+neutral)>=5);
		}
	
	double scoreRow(QuinamidCell anchor,int dir,QuinamidChip forChip)
		{	friendly = 0;
			neutral = 0;
    		if(uniform_or_empty_distance(anchor,CELL_UP,forChip)) { return(friendly*FRIENDLY_ASSISTANCE_BONUS); }
    		return(0);
		}

	public String initialPosition = "A1A1A1A1A1";
	public EdgeBoard stackedBoards[] = new EdgeBoard[NLEVELS];
    public void SetDrawState() 
    	{ setState(QuinamidState.DRAW_STATE); }
    public QuinamidCell redChips = null;
    public QuinamidCell blueChips = null;
    public QuinamidCell rack[] = null;		// chip pools per player
    //
    // private variables
    //
    public int chips_on_board[] = new int[2];			// number of chips currently on the board
	int playerColors[] = new int[2];
	QuinamidChip playerChip[] = { QuinamidChip.red,QuinamidChip.blue};
	int firstPlayer = 0;
	boolean swapPending = false;
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public QuinamidChip pickedObject = null;
    private QuinamidCell pickedSource = null;
    private QuinamidCell droppedDest = null;
    private int lastMovedBoard = -1;
    private QIds lastMovedDirection = null;
    private int nextMovedBoard = -1;
    private QIds nextMovedDirection =null;
    
	// factory method
	public QuinamidCell newcell(char c,int r)
	{	return(new QuinamidCell(c,r));
	}
    public QuinamidBoard(String init,String initialP,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = QUINAMIDGRIDSTYLE; //coordinates left and bottom
    	EdgeBoard lower = null;
    	setColorMap(map, 2);
     	for(int lvl = NLEVELS-1; lvl>=0;lvl--)
    	{	stackedBoards[lvl] = lower = new EdgeBoard(DEFAULT_COLUMNS-lvl,lvl,lower);
    	}
        initBoard(DEFAULT_COLUMNS,DEFAULT_COLUMNS); //this sets up the board and cross links
        doInit(init,initialP); // do the initialization for the main board
    }
    private int playerIndex(QuinamidChip ch) { return(playerColors[ch.colorIndex]);}
    public void addChip(QuinamidCell c,QuinamidChip ch)
    {	
    	if(c.onBoard)
    		{
    		QuinamidCell partner = c.upLink;
    		G.Assert(partner!=null && partner.upLink==c, "linked cell ok");
    	   	G.Assert(partner.chip == c.chip, "contents ok before change");
    	   	int pi = playerIndex(ch);
    	   	chips_on_board[pi]++;
    	   	partner.addChip(ch);
    	   	stackedBoards[partner.col-'a'].chipCount[pi]++;
    		}
   	   	c.addChip(ch);
    }
    public QuinamidChip removeTop(QuinamidCell c)
    {	QuinamidChip top = c.removeTop();
    	if(c.onBoard)
    	{
    	QuinamidCell partner = c.upLink;
    	G.Assert(partner!=null && partner.upLink==c, "linked cell ok");
    	G.Assert(partner.chip == top, "contents ok before change");
    	partner.removeTop();
	   	int pi = playerIndex(top);
    	chips_on_board[pi]--;
	   	stackedBoards[partner.col-'a'].chipCount[pi]--;
    	}
    	return(top);
    }
    
    // call to set the uplinks from the flat board to the edge boards
    // when the relationship has changed.
    private void setUpLinks()
    {
        stackedBoards[0].clearUpLinks();
        for(QuinamidCell c=allCells; c!=null; c=c.next)
        {	stackedBoards[0].setUpLink(c,c.col,c.row);
        	c.chip = c.upLink.chip;	// pull the current value
        	//G.print(""+c);
        }
    }
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	if(Quinamid_INIT.equalsIgnoreCase(game)) 
    		{ 
    		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(QuinamidState.PUZZLE_STATE);
        char col = 'A';
        char row = '1';
        for(int lvl=0; lvl<NLEVELS; lvl++)
        	{ 	char nextCol = initialPosition.charAt(lvl*2);
        		char nextRow = initialPosition.charAt(lvl*2+1);
        		stackedBoards[lvl].doInit();
        		stackedBoards[lvl].setOffsets(nextCol-col,nextRow-row);
        		col = nextCol;
        		row = nextRow;
        	}
        for(QuinamidCell c = allCells; c!=null; c=c.next) { c.reInit(); }
        setUpLinks();
        whoseTurn = FIRST_PLAYER_INDEX;
		for(QuinamidCell c=allCells; c!=null; c=c.next) { c.chip = null; }
		AR.copy(playerColors,getColorMap());
		swapPending = false;
        playerChip[playerColors[FIRST_PLAYER_INDEX]] = QuinamidChip.red;
        playerChip[playerColors[SECOND_PLAYER_INDEX]] = QuinamidChip.blue;
        AR.setValue(chips_on_board, 0);


    }
	public void sameboard(BoardProtocol f) { sameboard((QuinamidBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(QuinamidBoard from_b)
    {
    	super.sameboard(from_b);
    	
    	G.Assert(AR.sameArrayContents(playerColors,from_b.playerColors),"playerColors mismatch");
    	G.Assert(firstPlayer==from_b.firstPlayer,"firstPlayer mismatch");
    	G.Assert(AR.sameArrayContents(chips_on_board,from_b.chips_on_board),"chip count matches");
        G.Assert(swapPending==from_b.swapPending,"swapPending matches");
        for(int lvl=0; lvl<stackedBoards.length; lvl++) { stackedBoards[lvl].sameBoard(from_b.stackedBoards[lvl]); }
        G.Assert(pickedObject==from_b.pickedObject, "pickedObject doesn't match");
        G.Assert(QuinamidCell.sameCell(pickedSource,from_b.pickedSource), "pickedSource doesn't match");
        G.Assert(lastMovedBoard==from_b.lastMovedBoard,"lastMovedBoard doesn't match");
        G.Assert(nextMovedBoard==from_b.nextMovedBoard,"nextMovedBoard doesn't match");
        
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

    /** 
     * Digest produces a 32 bit hash of the game undoInfo.  This is used 3 different
     * ways in the system.
     * (1) This is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * undoInfo of the game, and a midpoint undoInfo of the game. Other site machinery
     *  looks for duplicate digests.  
     * (2) Digests are also used as the game is played to look for draw by repetition.  The undoInfo
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (3) Digests are used by the search machinery as a check on the robot's winding/unwinding
     * of the board position, this is mainly a debug/development function, but a very useful one.
     * @return
     */
   public long Digest()
    {
       Random r = new Random(64 * 1000); // init the random number generator
       long v = 0;

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        
        for (int i = 0; i < win.length; i++)
        {
        	v ^= chips_on_board[i]*r.nextLong();;
        	v ^= playerColors[i]*r.nextLong();
        }
	
		for(QuinamidCell c = allCells; c!=null; c=c.next)
		{	v ^= c.Digest(r);
		}
		
		for(int lvl = 0; lvl<stackedBoards.length; lvl++) { v ^= stackedBoards[lvl].Digest(r); }
        // for most games, we should also digest whose turn it is
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,pickedSource);
		v ^= ((firstPlayer*100)+(swapPending?2:1))*r.nextLong();
		v ^= r.nextLong()*(lastMovedBoard*2000+board_state.ordinal()*10+whoseTurn);
        return (v);
    }
   public QuinamidBoard cloneBoard() 
	{ QuinamidBoard copy = new QuinamidBoard(gametype,initialPosition,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((QuinamidBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(QuinamidBoard from_b)
    {	super.copyFrom(from_b);
    	swapPending = from_b.swapPending;
       	AR.copy(playerColors,from_b.playerColors);
       	AR.copy(chips_on_board,from_b.chips_on_board);
       	firstPlayer = from_b.firstPlayer;

        for(int lvl=0; lvl<stackedBoards.length; lvl++) { stackedBoards[lvl].copyFrom(from_b.stackedBoards[lvl]); }
        setUpLinks();

        pickedSource = getCell(from_b.pickedSource);
        pickedObject = from_b.pickedObject;
        lastMovedBoard = from_b.lastMovedBoard;
        lastMovedDirection = from_b.lastMovedDirection;
        nextMovedBoard = from_b.nextMovedBoard;
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        sameboard(from_b);
    }
    public void doInit() { doInit(gametype,initialPosition); }
    public void doInit(String qtype,long key) { randomKey = key; doInit(qtype,initialPosition); }
    /* initialize a board back to initial empty undoInfo */
    public void doInit(String gtype,String initialP)
    {	Random r = new Random(6025467);
    	initialPosition = initialP;
    	rack = new QuinamidCell[2];
    	firstPlayer = 0;
    	redChips = new QuinamidCell(r,QIds.Red_Chip_Pool);
    	blueChips = new QuinamidCell(r,QIds.Blue_Chip_Pool);
    	redChips.addChip(QuinamidChip.red);
    	blueChips.addChip(QuinamidChip.blue);
    	rack[0] = redChips;
    	rack[1] = blueChips;
    	    
        Init_Standard(gtype);
        allCells.setDigestChain(r);
        moveNumber = 1;
        lastMovedBoard = -1;
        nextMovedDirection = null;
        lastMovedDirection = null;
        nextMovedBoard = -1;

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
        	throw G.Error("Move not complete, can't change the current player %s",board_state);
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


   // count the number of cells with the same value with a specified 
   // starting point and direciton. 
   public boolean uniformRow(int player,QuinamidCell cell,int dir)
    {	for(QuinamidCell c=cell.exitTo(dir); c!=null; c=c.exitTo(dir))
    	{    QuinamidChip cup = c.topChip();
    	     if(cup==null) { return(false); }	// empty cell
    	     if(cup.chipNumber()!=player) { return(false); }	// cell covered by the other player
    	}
    	return(true);
    }
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(QuinamidState.GAMEOVER_STATE);
    }
    
    // Quinamid is generally interested
    // in 5 of 6, so we start with row 1 and either find 5 in some
    // direction, or 4 in that direction plus one in the opposite.
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	QuinamidChip forChip = QuinamidChip.getChip(playerColors[player]);
    	
    	// try columns up from 2, 
    	for(char col='A'; col<=LAST_COLUMN;col++)
    	{	QuinamidCell anchor = getCell(col,2);
    		if(anchor.winningLine(CELL_UP,forChip)) { return(true); }
    	}
    	
    	// try rows right from B
    	for(int row=1; row<=DEFAULT_COLUMNS; row++)
    	{	QuinamidCell anchor = getCell('B',row);
    		if(anchor.winningLine(CELL_RIGHT, forChip)) { return(true); }
    	}
       	// there are only a few diagnals that matter
    	if(getCell('B',1).winningLine(CELL_UP_RIGHT,forChip)) { return(true); }
       	if(getCell('A',2).winningLine(CELL_UP_RIGHT,forChip)) { return(true); }
       	if(getCell('A',5).winningLine(CELL_DOWN_RIGHT,forChip)) { return(true); }
       	if(getCell('B',6).winningLine(CELL_DOWN_RIGHT,forChip)) { return(true); }
       	// finalll the two main diagonals
       	if(getCell('B',2).winningLine(CELL_UP_RIGHT,forChip)) { return(true); }
       	if(getCell('B',5).winningLine(CELL_DOWN_RIGHT,forChip)) { return(true); }

    	return(false);
    }
    
    // Quinamid is generally interested
    // in 5 of 6, so we start with row 1 and either find 5 in some
    // direction, or 4 in that direction plus one in the opposite.
    public double rowCompleteScore(QuinamidChip forChip)
    {	// return true if the conditions for a win exist for player right now
    	double val = 0.0;
    	// try columns up from 2, 
    	for(char col='A'; col<=LAST_COLUMN;col++)
    	{	QuinamidCell anchor = getCell(col,2);
    		val += scoreRow(anchor,CELL_UP,forChip);
    		
    	}
    	double d1Lines = val;
   	
    	// try rows right from B
    	for(int row=1; row<=DEFAULT_COLUMNS; row++)
    	{	QuinamidCell anchor = getCell('B',row);
    		val += scoreRow(anchor,CELL_RIGHT, forChip);
    	}
    	double d2Lines = val-d1Lines;
    	
       	// there are only a few diagnals that matter
    	val += scoreRow(getCell('B',1),CELL_UP_RIGHT,forChip);
    	val += scoreRow(getCell('A',2),CELL_UP_RIGHT,forChip);
       	val += scoreRow(getCell('B',2),CELL_UP_RIGHT,forChip);
  	
    	double d3Lines = val-d2Lines;
    	
    	val += scoreRow(getCell('A',5),CELL_DOWN_RIGHT,forChip);
    	val += scoreRow(getCell('B',6),CELL_DOWN_RIGHT,forChip);
       	val += scoreRow(getCell('B',5),CELL_DOWN_RIGHT,forChip);
       	
       	double d4Lines = val-d3Lines;
       	
       	
       	// penalty for having some directions more powerful than others.
       	val -= Math.abs(d1Lines-(d2Lines+d3Lines+d4Lines)/2)*UNBALANCE_PENALTY;
       	val -= Math.abs(d2Lines-(d1Lines+d3Lines+d4Lines)/2)*UNBALANCE_PENALTY;
       	val -= Math.abs(d3Lines-(d1Lines+d2Lines+d4Lines)/4)*UNBALANCE_PENALTY;
       	val -= Math.abs(d4Lines-(d1Lines+d2Lines+d3Lines)/4)*UNBALANCE_PENALTY;
       	// finalll the two main diagonals

    	return(val);
    }
    // look for a win for player.  
    public double ScoreForPlayer(int player,boolean print,boolean dumbot)
    {  	int color = playerColors[player];
    	double finalv=rowCompleteScore(QuinamidChip.getChip(color))*COMPLETE_ROW_WEIGHT;
    	finalv += VISIBLE_CHIP_BONUS*chips_on_board[player];		// bonus for visible chips
    	for(int i=0;i<NLEVELS;i++)
    		{	EdgeBoard bd = stackedBoards[i];
    			finalv += bd.chipCount[player]*(i+UPPER_LEVEL_BONUS);	// higher levels are worth more.
    		}
    	return(finalv);
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        droppedDest = null;
        pickedSource = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    QuinamidCell dr = droppedDest;
    if(dr!=null)
    	{
    	droppedDest = null;
    	pickedObject = removeTop(dr);
    	}
    }
    // 
    // undo the pick, getting back to base undoInfo for the move
    //
    private void unPickObject()
    {	QuinamidChip po = pickedObject;
    	if(po!=null)
    	{
    	QuinamidCell ps = pickedSource;
    	pickedSource=null;
    	pickedObject = null;
    	addChip(ps,po);
    	}
     }
    // 
    // drop the floating object.
    //
    private void dropObject(QuinamidCell c)
    {
       G.Assert((pickedObject!=null)&&(droppedDest==null),"ready to drop");
       droppedDest = c; 
       switch (c.rackLocation())
        {
        default: throw G.Error("Not expecting dest %s",c);
        case BoardLocation: // an already filled board slot.
        	addChip(c,pickedObject);
        	pickedObject = null;
        	break;
        case Blue_Chip_Pool:		// back in the pool
        case Red_Chip_Pool:		// back in the pool
        	acceptPlacement();
            break;
        }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(QuinamidCell cell)
    {	return(droppedDest==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	QuinamidChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    
    public QuinamidCell getCell(QuinamidCell c)
    {	if(c==null) { return(null); }
    	return(getCell(c.rackLocation(),c.col,c.row));
    }
    public QuinamidCell getCell(QIds rack,char col,int row)
    {
    	switch (rack)
    	{
    	default:
    		throw G.Error("not expecting source %s", rack);
    	case BoardLocation:
     		return(getCell(col,row));
    	case Red_Chip_Pool:
    		return(redChips);
    	case Blue_Chip_Pool:
	    	return(blueChips);
    	}
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(QuinamidCell c)
    {	G.Assert((pickedObject==null)&&(pickedSource==null),"ready to pick");
    	pickedSource = c;
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting source %s", c);
        case BoardLocation:
         	{
        	pickedObject = removeTop(c);
         	break;
         	}
        case Blue_Chip_Pool:
        case Red_Chip_Pool:
        	{
       		pickedObject = c.topChip();
        	}
        }
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in board_state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	if(droppedDest!=null) { setNextStateAfterDone(); }
        	break;
        case PLAY_STATE:
        case PLAY_OR_SWAP_STATE:
        case FIRST_PLAY_STATE:
			setState(QuinamidState.CONFIRM_STATE);
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
        	throw G.Error("Not expecting pick in board_state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setState(QuinamidState.PLAY_STATE);
        	break;
        case PLAY_STATE:
        case PLAY_OR_SWAP_STATE:
        case FIRST_PLAY_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting board_state %s",board_state);
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
     	case PLAY_STATE:
     		setNextPlayState();
    		break;
    	}

    }
    private void setNextPlayState()
    {
    	if((chips_on_board[0]==0) && (chips_on_board[1]==0))
    	{
    		setState(QuinamidState.FIRST_PLAY_STATE);
    	}
    	else if((chips_on_board[firstPlayer]==1)
    			&&(chips_on_board[nextPlayer[firstPlayer]]==0)
    			&& (playerColors[0]==getColorMap()[0])	// not already swapped
    			)
    	{
    		setState(QuinamidState.PLAY_OR_SWAP_STATE);
    	}
    	else { setState(QuinamidState.PLAY_STATE); }
    	swapPending = false;
    }

    private void doDone()
    {	
        acceptPlacement();
    	lastMovedBoard = nextMovedBoard;
    	lastMovedDirection = nextMovedDirection;
    	nextMovedBoard= -1;
    	nextMovedDirection=null;

        if (board_state==QuinamidState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer();  setNextStateAfterDone(); }
        }
    }


    private void doShiftOrRotate(int source,QIds shiftcode,boolean undo)
    {	G.Assert(undo || (source!=lastMovedBoard), "can't move the same board twice");
     	EdgeBoard movee = stackedBoards[source];
     	switch(shiftcode)
    	{	
    	case RotateCW:	movee.changeRotation(undo?1:-1,false); break;	// rotate in opposite direction to undo
    	case RotateCCW: movee.changeRotation(undo?-1:1,false); break;
    	case MoveLeft:	movee.setColOffset(undo?1:0); break;
    	case MoveRight: movee.setColOffset(undo?0:1); break;
    	case MoveUp: movee.setRowOffset(undo?0:1); break;
    	case MoveDown: movee.setRowOffset(undo?1:0); break;
    	default: throw G.Error("Not expecting move code "+shiftcode);
    	}
    	setUpLinks();
    	switch(board_state)
    	{
    	case PUZZLE_STATE:
    		break;
    	case CONFIRM_STATE:
    		nextMovedBoard=-1;
    		nextMovedDirection=null;
    		setState(QuinamidState.PLAY_STATE);
    		break;
    	case PLAY_OR_SWAP_STATE:	// not currently allowed, but if achieved.
    	case PLAY_STATE:
    		nextMovedBoard = source;
    		nextMovedDirection = shiftcode; 
    		setState(QuinamidState.CONFIRM_STATE);
			break;
		default:
			break;
    	}
    }
    void doSwap()
    {
    	int c = playerColors[0];
    	playerColors[0] = playerColors[1];
    	playerColors[1] = c;
    	swapPending = !swapPending;
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	QuinamidMovespec m = (QuinamidMovespec)mm;
 
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_SWAP:
        	doSwap();
        	setState(swapPending ? QuinamidState.CONFIRM_STATE : QuinamidState.PLAY_OR_SWAP_STATE);
        	break;
        case MOVE_DONE:
         	doDone();
            break;
        case MOVE_ROTATE:
        case MOVE_SHIFT:
        	doShiftOrRotate(m.to_row,m.shift,false);
        	break;
        case MOVE_DROPB:
        	{
        	G.Assert(pickedObject!=null,"something is moving");
        	QuinamidCell dest = getCell(QIds.BoardLocation, m.to_col, m.to_row);
            dropObject(dest);
            if(replay==replayMode.Single)
            {
            	animationStack.push(pickedSource);
            	animationStack.push(dest);
            }
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
        	if(isDest(getCell(m.to_col,m.to_row)))
        		{ unDropObject(); 
        		  setState(QuinamidState.PLAY_STATE);
        		}
        	else 
        		{ QuinamidCell src = getCell(QIds.BoardLocation, m.to_col, m.to_row);
        		  pickObject(src);
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case PLAY_STATE:
        		  		// if we pick a piece off the board, we might expose a win for the other player
        		  		// and otherwise, we are comitted to moving the piece
         		  		break;
        		  	case PUZZLE_STATE:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	QuinamidCell dest = getCell(m.source, m.to_col, m.to_row);
            dropObject(dest);           
            if(board_state!=QuinamidState.PUZZLE_STATE) { setNextPlayState(); }
        	}
            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickObject(getCell(m.source, m.to_col, m.to_row));
            setNextStateAfterPick();
            break;

        case MOVE_RACK_BOARD:
        	{
        	QuinamidCell src = getCell(m.source,m.to_col,m.to_row);
        	QuinamidCell dest = getCell(QIds.BoardLocation,m.to_col,m.to_row);
        	pickObject(src);
        	dropObject(dest);
        	
			if(replay!=replayMode.Replay)
			{
				animationStack.push(src);
				animationStack.push(dest);
			}

        	setNextStateAfterDrop();
        	}
        	break;
        case MOVE_START:
            setWhoseTurn(firstPlayer = m.player);
            lastMovedBoard = -1;
            acceptPlacement();
            unPickObject();
            // standardize the gameover undoInfo.  Particularly importing if the
            // sequence in a game is resign/start
            setState(QuinamidState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?QuinamidState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            lastMovedBoard = -1;
            setState(QuinamidState.PUZZLE_STATE);
 
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
        	throw G.Error("Not expecting board_state %s", board_state);
         case CONFIRM_STATE:
        	 	if(swapPending) { return(false); }
			//$FALL-THROUGH$
         case DRAW_STATE:
         case PLAY_STATE: 
         case PLAY_OR_SWAP_STATE:
         case FIRST_PLAY_STATE:
        	return((pickedObject==null)
        			?((nextMovedBoard==-1) && (player==whoseTurn))
        			:((droppedDest==null) 
        					&& (pickedSource.onBoard==false)
        					&&(player==pickedObject.chipNumber())));


		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(player==pickedObject.chipNumber()));
        }
    }

    public boolean LegalToHitBoard(QuinamidCell cell)
    {	
        switch (board_state)
        {
        case FIRST_PLAY_STATE:
        case PLAY_OR_SWAP_STATE:
 		case PLAY_STATE:
			return((pickedObject!=null) && (cell.chip==null));
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
			return((pickedObject==null) && (droppedDest==cell));
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting board_state %s", board_state);
        case PUZZLE_STATE:
        	return((pickedObject==null)!=(cell.chip==null));
        }
    }
  public boolean canDropOn(QuinamidCell cell)
  {	
  	return((pickedObject!=null)				// something moving
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
    public void RobotExecute(QuinamidMovespec m)
    {	m.state = board_state;
        m.undoInfo = (swapPending?100000:0)+lastMovedBoard+1; //record the starting undoInfo. The most reliable
        // to undo undoInfo transitions is to simple put the original undoInfo back.
        
        //G.print("R "+m);
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
    public void UnExecute(QuinamidMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
        //G.print("U "+m);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

   	    case MOVE_SWAP:
   	    	doSwap();
   	    	break;
        case MOVE_ROTATE:
        case MOVE_SHIFT:
	    	doShiftOrRotate(m.to_row,m.shift,true);
	    	break;      	
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case FIRST_PLAY_STATE:
        		case PLAY_OR_SWAP_STATE:
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			QuinamidCell src = getCell(QIds.BoardLocation,m.to_col,m.to_row);
        			QuinamidCell dest =getCell(m.source,m.to_col, m.to_row);
        			pickObject(src);
        			dropObject(dest);
       			    acceptPlacement();
                    break;
        	}
        	break;
         case MOVE_RESIGN:
            break;
        }
        int undo = m.undoInfo;
        swapPending = (undo/100000)!=0;
        undo = undo%100000;
        setState(m.state);
        nextMovedBoard = -1;
        nextMovedDirection=null;
        lastMovedBoard = (undo%1000)-1;
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	for(QuinamidCell c = allCells; c!=null; c=c.next)
 	{	if(c.chip==null) 
 			{ all.push(new QuinamidMovespec(MOVE_RACK_BOARD,rack[playerColors[whoseTurn]].rackLocation(),c.col,c.row,whoseTurn));
 			}
 		if(board_state==QuinamidState.PLAY_OR_SWAP_STATE) { all.push(new QuinamidMovespec(MOVE_SWAP,whoseTurn)); }
 		if(board_state==QuinamidState.PLAY_STATE)
 		{
 		for(int lvl=1; lvl<NLEVELS; lvl++)
 		{	if(lvl!=lastMovedBoard)
 			{	EdgeBoard edge = stackedBoards[lvl];
 				// 2 shift moves and 2 rotate moves per edge board, except for
 				// the one blocked by the Ko rule
 				all.push(new QuinamidMovespec(MOVE_SHIFT,
 									lvl,
 									(edge.col_offset==0)?QIds.MoveRight:QIds.MoveLeft,whoseTurn));
 				all.push(new QuinamidMovespec(MOVE_SHIFT,
							lvl,
							(edge.row_offset==0)?QIds.MoveUp:QIds.MoveDown,whoseTurn));
				
 				all.push(new QuinamidMovespec(MOVE_ROTATE,
							lvl,
							QIds.RotateCCW,whoseTurn));
 				all.push(new QuinamidMovespec(MOVE_ROTATE,
							lvl,
							QIds.RotateCW,whoseTurn)); 				
			}
 		}}
 	}
  	return(all);
 }
 
}
