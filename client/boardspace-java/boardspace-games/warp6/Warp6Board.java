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
package warp6;


import java.awt.Color;
import java.awt.Rectangle;
import java.util.Hashtable;

import lib.*;
import online.game.*;
import warp6.Warp6Chip.ChipColor;

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

class Warp6Board extends squareBoard<Warp6Cell> implements BoardProtocol,Warp6Constants
{	// warp6 cells have specied locations which form a spiral
	private Warp6State unresign;
	private Warp6State board_state;
	
	public Warp6State getState() {return(board_state); }
	public void setState(Warp6State st) 
	{ 	
	unresign = (st==Warp6State.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public double cellToY00(char colchar, int thisrow)
	{	Warp6Cell c = getCell(thisrow);
		return((1.0-c.ypos)*displayParameters.HEIGHT-displayParameters.HEIGHT/2);
	}
	public double cellToX00(char colchar, int thisrow)
	{	Warp6Cell c = getCell(thisrow);
		return(c.xpos*displayParameters.WIDTH-displayParameters.WIDTH/2);
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
    // factory method used to construct the cells of the board
 	public Warp6Cell newcell(char c,int r)
	{	return(new Warp6Cell(c,r,cell.Geometry.Square));
	}

	// the score is the sum of the heights of controlled stacks
    public double ScoreForPlayer(int who,boolean print,boolean dumbot)
    {  	double sc = 0.0;
    	int diceup = 0;
    	int dicecount = 0;
    	double distance = 0;
    	double warpable = 0;
    	double pairs = 0;
    	double chips_counted = 0;
    	int row = NUMPOINTS;
    	for(int i=0;i<chipsInWarp[who];i++)
    	{	Warp6Chip ch = warp[who][i].topChip();
    		distance += ((NUMPOINTS+50.0)/ch.numSides);
    		chips_counted++;
    	}
    	
     	for(Warp6Cell c = allCells;
    		c != null;
    		c = c.next)
    	{	G.Assert(c.row<=row,"row declines");	// verify that the underlying row ordering didn't change
    		row = c.row;
    		Warp6Chip ch = c.topChip();
    		Warp6Cell down = c.exitTo(CELL_RIGHT);
    		if(down!=null)
    				{	Warp6Chip downch = down.topChip();
    					if(downch!=null)
    					{
    					if(downch.color==playerColor[who]) { warpable += 6; }
    					else { warpable+=3; }
    					}
    				}
    		if((ch!=null) && (ch.color==playerColor[who]))
    		{	dicecount++;
    			chips_counted++;
    			diceup += ch.numberShowing;
    			double incr = (c.row+ch.numSides-1.0)/ch.numSides;
    			if(chips_counted>WARP_DICE_TO_WIN)
    			{ // fewer points for the latter dice, they won't go in most likely
    			  incr /= (0.5+(chips_counted-WARP_DICE_TO_WIN));
    			}
    			distance += incr;
        		// score for pairs of chips.  Good for self-self bad for self-other
    			Warp6Cell prevCell = getCell(row+ch.numberShowing);
    			if(prevCell!=null)
    			{
    			Warp6Chip prevChip = prevCell.topChip();

    			if(prevChip!=null)
    			{
    			if(prevChip.color==playerColor[who])
    				{ 
    				  pairs += (NUMPOINTS-row)/10.0;  
    				}
    				else
    				{
    				  pairs += (NUMPOINTS-row)/20.0; 
    				}
    			}
    			}
    		}
    	}
    	sc = warpable+distance;
    	sc += pairs; 
    	sc += diceup/20.0;
     	if(print) {
    		String pa = " pair:"+pairs+" up:"+(diceup/20.0);
    		System.out.println("dice: "+dicecount+":"+warpable+"/"+dicecount+pa+"="+sc);	
    		}
     	return(sc);
    }
    
    public CellStack animationStack = new CellStack();
    public void SetDrawState() { setState(Warp6State.DRAW_STATE);  }
	public Warp6Cell rack[][] = new Warp6Cell[NUMPLAYERS][NPIECES];
	public Warp6Cell warp[][] = new Warp6Cell[NUMPLAYERS][NPIECES];
    public int chipsInWarp[] = new int[NUMPLAYERS];
    public int chipsInRack[] = new int[NUMPLAYERS];
    public ChipColor playerColor[] = new ChipColor[NUMPLAYERS];
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public Warp6Chip pickedObject = null;			// the object in the air
    private Warp6Cell pickedSourceStack[]=new Warp6Cell[20];
    private Warp6Cell droppedDestStack[]=new Warp6Cell[20];
    public Warp6Cell lastDropped = null;
    public Warp6Cell rollCell = null;				// cell that needs reroll
    private int rollValue = 0;
    private int stackIndex = 0;
    public Warp6Movespec lastMove = null;
    private Warp6Cell getSource() { return((pickedObject==null)?null:pickedSourceStack[stackIndex]); }
    public boolean isSource(Warp6Cell c)
    {	return((pickedObject!=null) 
    			?(c==pickedSourceStack[stackIndex])
    			: (stackIndex>0)&&(pickedSourceStack[0]==c));
    }
    public int movingObjectIndex()
    {	Warp6Chip ch = pickedObject;
    	if((ch!=null)&&(droppedDestStack[stackIndex]==null))
    		{ return(ch.pieceNumber());
    		}
        return (NothingMoving);
    }
    public ChipColor movingObjectColor()
    {	if((pickedObject!=null)&&(droppedDestStack[stackIndex]==null))
    		{ return(pickedObject.color);
    		}
        return (null);
    }
    private void finalizePlacement()	// finalize placement, no more undo
    { 	if(stackIndex>=pickedSourceStack.length) { stackIndex--; }
    	while(stackIndex>=0) 
    		{if(stackIndex<pickedSourceStack.length)
    			{ pickedSourceStack[stackIndex]=droppedDestStack[stackIndex] = null;
    			}
    		 stackIndex--;
    		}
    	pickedObject = null;
    	lastMove = null;
    	stackIndex = 0;
     }   
    // use these exclisively to the tallies of chips in the gutter are maintained 
    private void addChip(Warp6Cell ps,Warp6Chip po)
    {  	ps.addChip(po);
    	G.Assert(po!=null,"something to add");
    	switch(ps.rackLocation())
    	{
    	default: throw G.Error("not expecting");
    	case BoardLocation: break;
    	case FirstPlayerRack: 
    		chipsInRack[getColorMap()[FIRST_PLAYER_INDEX]]++;
    		break;
    	case SecondPlayerRack: 
    		chipsInRack[getColorMap()[SECOND_PLAYER_INDEX]]++; 
    		break;
    	case FirstPlayerWarp: 
    		chipsInWarp[getColorMap()[FIRST_PLAYER_INDEX]]++; break;
    	case SecondPlayerWarp: 
    		chipsInWarp[getColorMap()[SECOND_PLAYER_INDEX]]++; break;
    	}
    }
    // use these exclisively to the tallies of chips in the gutter are maintained 
    private Warp6Chip removeChip(Warp6Cell ps)
    {  	Warp6Chip po = ps.removeChip();
    	if(po!=null)
    	{
			switch(ps.rackLocation())
			{
			default: throw G.Error("not expecting");
			case BoardLocation: break;
			case FirstPlayerRack: chipsInRack[getColorMap()[FIRST_PLAYER_INDEX]]--; break;
			case SecondPlayerRack: chipsInRack[getColorMap()[SECOND_PLAYER_INDEX]]--; break;
		   	case FirstPlayerWarp: chipsInWarp[getColorMap()[FIRST_PLAYER_INDEX]]--; break;
		   	case SecondPlayerWarp: chipsInWarp[getColorMap()[SECOND_PLAYER_INDEX]]--; break;
			}
    	}
    	return(po);
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	Warp6Cell dr = droppedDestStack[stackIndex];
    	if(dr!=null)
    	{
    	if(pickedObject==null) { pickedObject = removeChip(dr); }
    	droppedDestStack[stackIndex] =  null;
    	lastDropped = null;
    	}
    }
    private void undoRollMove(Warp6Movespec m)
	{
       	Warp6Cell from = getCell(m.source,m.from_row);
       	Warp6Chip chip = from.topChip();
       	int nextn = chip.numberShowing+((m.op!=MOVE_ROLLUP)?1:-1);
       	Warp6Chip newc = Warp6Chip.getChip(chip.color,chip.numSides,nextn);
       	removeChip(from);
       	addChip(from,newc);
    }
    
    // lowest level of uncapture.  "removed" is the most distant cell removed by the capture
    // picked and dropped are the source and destination respectively.  This handles both
    // capture by approach and by withdrawal.

    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	Warp6Chip po = pickedObject;
    	if(po!=null)
    	{
    	Warp6Cell ps = pickedSourceStack[stackIndex];
    	Warp6Chip oc = removeChip(ps);
    	addChip(ps,po);
        pickedSourceStack[stackIndex]=null;
    	pickedObject = oc;
    	}
     }

    
    private void dropOnBoard(Warp6Cell dcell,Warp6Chip ob)
    {
    	droppedDestStack[stackIndex] = dcell;
    	addChip(dcell,ob);
    	pickedObject=null;
    	lastDropped = dcell;
     }
    private void dropOnCell(Warp6Cell dcell)
    {	droppedDestStack[stackIndex] = dcell;
    	addChip(dcell,pickedObject);
    	pickedObject = null;
    	lastDropped = null;
    }
    private void pickFromCell(Warp6Cell src)
    {  	//G.print("st "+stackIndex);
    	pickedSourceStack[stackIndex] = src;
    	pickedObject = removeChip(src);
    }

  
    public Warp6Cell getCell(int row)
    {
    	return(getCell('A',row));
    }
    public void initBoard(int cols, int rows)
    {
    	super.initBoard(cols,rows);
    	for(int i=0;i<WarpPoints.length;i++)
    	{	double spec[] = WarpPoints[i];
    		int link = (int)(spec[3]);
     		Warp6Cell c = getCell(i+1);
     		
     		// create the "warp down" links
       		if(link>0)
    		{
    		Warp6Cell linkCell = getCell(link+1);
    		c.addLink(CELL_RIGHT,linkCell);
    		}
       		// set the cell position
    		c.xpos = spec[1]/100;
    		c.ypos = spec[2]/100;
    	}
    }
    public Warp6Board(long random,String init,int map[]) // default constructor
    {  	drawing_style =  DrawingStyle.STYLE_NOTHING;//  or STYLE_CELL or STYLE_LINES
    	Grid_Style = KNOXKABOUTGRIDSTYLE; //coordinates left and bottom
    	setColorMap(map, 2);
       	initBoard(1,WarpPoints.length);
       	randomKey = random;
    	doInit(init,random); // do the initialization 
    }
    public Warp6Board cloneBoard() 
	{ Warp6Board dup = new Warp6Board(randomKey,gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((Warp6Board)b); }
   // overridable method.  xpos ypos is the desired centerpoint for the text
    // 
    public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
    {	if((txt.length()<=2) && txt.charAt(1)=='1') { ypos-= cellsize/2; } else { ypos += 2*cellsize/3; } 
    	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
   }

    private void Init_Standard(String game)
    {  
        gametype = game;
        setState(Warp6State.PUZZLE_STATE);
        rollCell = null;
        lastMove = null;
        for(Warp6Cell c=allCells;
            c!=null;
            c=c.next)
        {	c.reInit();
        }
        for(int i=0;i<NUMPLAYERS;i++) { chipsInRack[i]=chipsInWarp[i]=0; win[i]=false; }
        if (Warp6_Standard_Init.equalsIgnoreCase(game)) 
        {	
        }
        else { throw G.Error(WrongInitError,game); }
        
      whoseTurn = FIRST_PLAYER_INDEX;
    }
    public void sameboard(BoardProtocol f) { sameboard((Warp6Board)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(Warp6Board from_b)
    {	super.sameboard(from_b);
    	G.Assert(AR.sameArrayContents(chipsInWarp,from_b.chipsInWarp), "chipsInWarp mismatch");
    	G.Assert(AR.sameArrayContents(chipsInRack,from_b.chipsInRack), "chipsInRack mismatch");
    	G.Assert(sameCells(rack,from_b.rack), "rack mismatch");
    	G.Assert(sameCells(warp,from_b.warp), "warp mismatch");
        G.Assert(stackIndex == from_b.stackIndex,"stackIndex matches");
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
    	long v = super.Digest(r);

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        v ^= Digest(r,rack);
        v ^= Digest(r,warp);
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,getSource());
		v^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
       return (v);
    }
    public Warp6Cell getCell(Warp6Cell c)
    {	return((c==null) 
    			? null
    			: getCell(c.rackLocation(),c.row));
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(Warp6Board from_b)
    {	super.copyFrom(from_b);
    
    	lastMove = from_b.lastMove;
        pickedObject = from_b.pickedObject;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        copyFrom(rack,from_b.rack);
        copyFrom(warp,from_b.warp);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        AR.copy(chipsInRack,from_b.chipsInRack);
        AR.copy(chipsInWarp,from_b.chipsInWarp);
        rollCell = getCell(from_b.rollCell);
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long ran)
    {  Random r = new Random(743462);
       Init_Standard(gtype);
       allCells.setDigestChain(r);
       randomKey = ran;
       randomSeq = new Random(randomKey);
       AR.setValue(win, false);
       pickedObject = null;
       AR.setValue(pickedSourceStack,null);
       AR.setValue(droppedDestStack,null);
   	
       moveNumber = 1;
        // note that firstPlayer is NOT initialized here
       int sum[]=new int[NUMPLAYERS];
       int map[]=getColorMap();
       playerColor[map[FIRST_PLAYER_INDEX]] = ChipColor.white;
       playerColor[map[SECOND_PLAYER_INDEX]] = ChipColor.yellow;
       do
       {
       for(int i=0;i<NUMPLAYERS;i++)
       { ChipColor color = ChipColor.values()[i];
       	 Warp6Cell rr[] = rack[map[i]];
         for(int j=0;j<NPIECES;j++) 
         {	warp[map[i]][j]=new Warp6Cell(r,playerWarpLocation[i],j);
         }
         {
       	 int j=0;
       	 sum[i]=0;
       	 chipsInRack[i]=NPIECES;
       	 while(j<2)
       	 	{ Warp6Cell c = rr[j] = new Warp6Cell(r,playerRackLocation[i],j);
       	 	  int up = nextRandom()%8+1;
       	 	  sum[i]+=up;
       	 	  c.addChip(Warp6Chip.getChip(color,8,up));
       	 	  j++;
       	 	}
       	 while(j<5)
       	 {Warp6Cell c = rr[j] = new Warp6Cell(r,playerRackLocation[i],j);
       	  int up = nextRandom()%6+1;
       	  sum[i]+=up;
  	 	  c.addChip(Warp6Chip.getChip(color,6,up));
   	 	  j++;
       	 }
       	 while(j<9)
       	 {Warp6Cell c = rr[j] = new Warp6Cell(r,playerRackLocation[i],j);
       	  int up = nextRandom()%4+1;
       	  sum[i]+=up;
  	 	  c.addChip(Warp6Chip.getChip(color,4,up));
   	 	  j++;
       	 }}

       }
       }
       // impose a little fairness in the starting rolls.  Retry is the numbers are too different
       while(Math.abs(sum[0]-sum[1]) > 9);
       lastDropped = null;
       animationStack.clear();

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
        case DRAW_STATE:
         case CONFIRM_STATE:
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(Warp6State.GAMEOVER_STATE);
    }




    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	
    	case GAMEOVER_STATE: 
    		break;

       	case PUZZLE_STATE:
       	case CONFIRM_STATE:
    		if(chipsInWarp[whoseTurn]>=WARP_DICE_TO_WIN) 
    			{ setGameOver(true,false); }
    		else if(chipsInWarp[nextPlayer[whoseTurn]]>=WARP_DICE_TO_WIN) 
    			{ setGameOver(false,true); }
    		else if(chipsInRack[whoseTurn]>0) { setState(Warp6State.PLACE_STATE); }
    		else { setState(Warp6State.PLAY_STATE);	// default, and makes hasLegalMoves work 
    		}
    		break;
    	}
       	if(rollCell!=null)
       	{	Warp6Chip chip = removeChip(rollCell);
       		if(chip!=null)
       			{int newv = (rollValue % chip.numSides) + 1;
       			 addChip(rollCell,Warp6Chip.getChip(chip.color,chip.numSides,newv));
       			 rollCell = null;
       			}
      	}

    }
     
    private void doDone()
    {	
    	if(board_state==Warp6State.RESIGN_STATE) 
    	{ setGameOver(false,true);
    	}
    	else
    	{
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case PUZZLE_STATE: break;
    	case CONFIRM_STATE:  
    		finalizePlacement();
       	 	setNextPlayer(); 
     		setNextStateAfterDone();
     		break;
    	}}
    }

    // remove the top N pieces from the captured stack and place them on dest
    public void undoCaptures(int n,Warp6Cell dest)
    {	throw G.Error("not implemented");
    }

    // return true if there is still an empty space in this direction
    boolean hasEmpty(Warp6Cell from,int dir)
    {
    	while(from!=null)
    		{ if(from.topChip()==null) { return(true); }
    		  from=from.exitTo(dir);
    		}
    	return(false);
    }

    public Warp6Cell getCell(WarpId source,int row)
    {	switch(source)
    	{
    	default: throw G.Error("Not expecting source %s",source);
    	case BoardLocation: return(getCell(row));
    	case SecondPlayerRack: return(rack[getColorMap()[SECOND_PLAYER_INDEX]][row]);
    	case FirstPlayerRack: return(rack[getColorMap()[FIRST_PLAYER_INDEX]][row]);
    	case FirstPlayerWarp: return(warp[getColorMap()[FIRST_PLAYER_INDEX]][row]);
    	case SecondPlayerWarp: return(warp[getColorMap()[SECOND_PLAYER_INDEX]][row]);
    	}
    }
    int playerIndex(Warp6Chip ch) { return((ch.color==playerColor[0]) ? 0 : 1); }
    
    public void dropAndWarp(Warp6Movespec m,Warp6Chip chip,Warp6Cell target,replayMode replay)
    { 
      int roll = m.rollAmount;
  	  Warp6Chip top = target.topChip();
  	  rollCell = null;
  	  while(top!=null)
      {	
      	Warp6Cell next = target.exitTo(CELL_RIGHT);
      	if(replay!=replayMode.Replay)
      	{
      		animationStack.push(target);
      		animationStack.push(next);
      	}
      	G.Assert(target!=null,"there's a warp");
      	
      	target = next;
      	rollCell = target; 
	    rollValue = roll;
      	top = target.topChip();
      }
  	  
      if(target.exitTo(CELL_UP)==null) 
      	{	// reached the hub
    	  int owner = playerIndex(chip);
    	  int index = chipsInWarp[owner];
    	  Warp6Cell c = warp[owner][index];
    	  rollCell = null;
    	  dropOnCell(c);
    	  if(replay!=replayMode.Replay)
        	{
        		animationStack.push(target);
        		animationStack.push(c);
        	}
      	}
  	  else
  	  {	dropOnBoard(target,chip);
  	    
  	  }
      Warp6Cell rest = droppedDestStack[stackIndex];
      m.undoInfo = rest.rackLocation().ordinal()*10000+rest.row*10+chip.numberShowing;
    }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	Warp6Movespec m = (Warp6Movespec)mm;
        if(replay==replayMode.Replay) { animationStack.clear(); }
        //G.print("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:
        	m.undoInfo = -1;
        	// undo unfo is the original value of the rolled piece
        	if(rollCell!=null) { Warp6Chip top = rollCell.topChip(); if(top!=null) { m.undoInfo = top.index; }}
         	doDone();

            break;
        case MOVE_ONBOARD:
        	{
        	Warp6Cell src = getCell(m.source,m.from_row);
        	Warp6Cell dest = getCell(m.to_row);
        	pickFromCell(src);
        	m.die = pickedObject;
        	dropOnBoard(dest,pickedObject);
        	if(replay!=replayMode.Replay)
        	{	animationStack.push(src);
        		animationStack.push(dest);
        	}
        	setState(Warp6State.CONFIRM_STATE);
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	{
           	Warp6Cell from = getCell(m.from_row);
        	Warp6Cell target = getCell(m.to_row); 
             pickFromCell(from);
            if(replay!=replayMode.Replay) 
    		{   animationStack.push(from);
    			animationStack.push(target); 
    		}
            m.die = pickedObject;
            dropAndWarp(m,pickedObject,target,replay);
         
            // encode the cell and original die
            
            setState(Warp6State.CONFIRM_STATE);
          }
        	break;
        case MOVE_DROPB:
        	{
        	G.Assert(pickedObject!=null,"something is moving");
        	Warp6Cell from = pickedSourceStack[stackIndex];
        	Warp6Cell target =  getCell(m.to_row);
        	if(from==target) { unPickObject(); }
        	else
        	{
        	if(replay!=replayMode.Replay)
        	{	// single step only, not in live play or full replay
        		animationStack.push((replay==replayMode.Live)?target:from);
        		animationStack.push(target);
        	}
            switch(board_state)
            {
            default:
            	throw G.Error("Not expecting state %s",board_state);
            case PUZZLE_STATE:
            	dropAndWarp(m,pickedObject,target,replay);
                finalizePlacement();
            	break;
            case PLAY_STATE:
            case PLACE_STATE:
              {	m.die = pickedObject;
            	dropAndWarp(m,pickedObject,target,replay);
            	setState(Warp6State.CONFIRM_STATE);
              }
               break;
            }}
        	}
            break;
        case MOVE_ROLLUP:
        case MOVE_ROLLDOWN:
           	{
           	Warp6Cell from = getCell(m.source,m.from_row);
           	Warp6Chip chip = from.topChip();
           	m.die = chip;
           	int nextn = chip.numberShowing+((m.op==MOVE_ROLLUP)?1:-1);
           	Warp6Chip newc = Warp6Chip.getChip(chip.color,chip.numSides,nextn);
           	removeChip(from);
           	addChip(from,newc);
           	switch(board_state)
           	{
           	case PUZZLE_STATE: break;
           	case CONFIRM_STATE:
           		{
               	// undo of a previous roll move
               	pickedSourceStack[0]=droppedDestStack[0]=null;
               	lastMove=null;
               	setState(Warp6State.PLAY_STATE);
               	}
           		break;
           	case PLAY_STATE:
           		{
               	pickedSourceStack[0]=droppedDestStack[0]=from;
               	lastMove = m;
               	setState(Warp6State.CONFIRM_STATE);
               	}
				break;
			default:
				break;
           	}

           	}
        	break;
        case MOVE_PICK:
        	{
        	G.Assert(pickedObject==null,"nothing should be moving");
        	
        	Warp6Cell ss = getCell(m.source,m.from_row);
        	if(isDest(ss)) { unDropObject(); }
        	else 
        	{ 	finalizePlacement();	// accept any moves
        		pickFromCell(ss); }
        		m.die = pickedObject;
        	}
        	break;
        case MOVE_DROP:
        	{ 
        	G.Assert(pickedObject!=null,"something should be moving");
        	Warp6Cell from = pickedSourceStack[stackIndex];
        	Warp6Cell target =  getCell(m.source,m.to_row);
        	if(from==target) { unPickObject(); }
        	else {  m.die = pickedObject;
        			dropOnCell(target); 
        			finalizePlacement(); 
        	}
        	}
        	break;
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary pick
        	Warp6Cell target = getCell( m.from_row);
        	{
        	switch(board_state)
        	{	
           	case CONFIRM_STATE:
           		if(isDest(target))
           		{ unDropObject();
           		  Warp6Cell src = pickedSourceStack[0];
           		  if((src!=null)
           			  &&(board_state==Warp6State.CONFIRM_STATE)
           			  &&((src.rackLocation==WarpId.FirstPlayerRack)||(src.rackLocation==WarpId.SecondPlayerRack)))
           			  {setState(Warp6State.PLACE_STATE);
           			  }
           			  else
           			  {
           			  setNextStateAfterDone();
           			  }
           		  break;
           		}
				//$FALL-THROUGH$
			default:
           		{
           		  pickFromCell(target);
           		  m.die = pickedObject;
           		}
        	}}
            break;

        	
        case MOVE_START:
            setWhoseTurn(m.player);
            finalizePlacement();
            setState(Warp6State.PUZZLE_STATE);
            setNextStateAfterDone();
            break;

        case MOVE_RESIGN:
            setState(unresign==null?Warp6State.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
        	finalizePlacement();
            setState(Warp6State.PUZZLE_STATE);

            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }


        return (true);
    }
 
    public void SetDisplayRectangle(Rectangle r) { super.SetDisplayRectangle(r);  displayParameters.CELLSIZE = G.Width(r)/20; } 
  
    // true if it's legal to drop a chip  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(Warp6Cell fromCell,Warp6Cell toCell)
    {	if(fromCell==toCell) { return(true); }
    	Warp6Cell to = getDestFrom(fromCell,whoseTurn,fromCell==pickedSourceStack[stackIndex]);
    	return(to==toCell);
    }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    private boolean isDest(Warp6Cell cell)
    {	return((stackIndex>=0) && (droppedDestStack[0]==cell));
    }
    
    public boolean LegalToHitChips(int pl,Warp6Cell ch)
    {	Warp6Chip top = ch.topChip();
    	boolean pdok = (pickedObject==null)
    					?(top!=null)
						:(top==null);
    	switch(board_state)
    	{
    	case PUZZLE_STATE: return(pdok);
    	case PLACE_STATE: return((pl==whoseTurn)&& pdok); 
    	case CONFIRM_STATE: return(isDest(ch));
    		
    	default: return(false);
    	}
    }
    // return the first unoccupued cell in the board spiral
    public Warp6Cell firstEmptySlot()
    {
    	for(int i=1;i<nrows;i++)
    	{
    	Warp6Cell c = getCell(i);
    	if(c.topChip()==null) { return(c); }
    	}
    	return(null);	
    }
    public boolean isFirstEmptySlot(Warp6Cell cell)
    {	return(firstEmptySlot()==cell);
    }
    public boolean LegalToHitBoard(Warp6Cell cell)
    {	
        switch (board_state)
        {
        case PLACE_STATE:
        	if(pickedObject!=null) { return(isFirstEmptySlot(cell)); }
        	return(false);
 		case PLAY_STATE:
 			if(pickedObject!=null)
 			{	return(LegalToDropOnBoard(pickedSourceStack[stackIndex],cell));
 			}
 			return((cell.chip!=null)
 					&& (cell.topChip().color==playerColor[whoseTurn]));
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
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
    public void RobotExecute(Warp6Movespec m)
    {	//int enter = moveNumber;
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transitions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("R "+m+" for "+m.player+" "+whoseTurn+" "+moveNumber);
        if (Execute(m,replayMode.Replay))
        {	if(DoneState()) 
        		{doDone(); 
        		}
         	else if (m.op == MOVE_DONE)
            {
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
        //G.print("X "+m+" for "+m.player+" "+whoseTurn+" "+moveNumber);
        //G.Assert((moveNumber-1)== enter,"move up");
                       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Warp6Movespec m)
    {	int who = whoseTurn;
    	//int enter = moveNumber;
    	setWhoseTurn(m.player);
        //G.print("U "+m+" for "+m.player+" "+who+" "+moveNumber);

        switch (m.op)
        {
        default:
   	    	cantUnExecute(m);
        	break;
	    	  
        case MOVE_DONE:
            break;
        case MOVE_BOARD_BOARD:
        	switch(m.state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        		{
        	           // encode the cell and original die
                    int undo = m.undoInfo;// rest.rackLocation*10000+rest.row*10+po.numberShowing;
                    int rackloc = undo/10000;
                    int row = (undo%10000)/10;
                    int die = undo%10;
                    Warp6Cell dest = getCell(allIds[rackloc],row);
                    pickFromCell(dest);
                    Warp6Chip po = pickedObject;
                    Warp6Chip newpo = pickedObject = Warp6Chip.getChip(po.color,po.numSides,die);
                    dropOnBoard(getCell(m.from_row),newpo);
         			break;
        		}
        	}
        	break;
        case MOVE_ONBOARD:
        	pickFromCell(getCell(m.to_row));
        	dropOnCell(rack[m.player][m.from_row]);
        	break;
        case MOVE_ROLLUP:
        case MOVE_ROLLDOWN:
        	undoRollMove(m);
     		break;
       	
        case MOVE_RESIGN:
            break;
        }
        rollCell = null;
        setState(m.state);
        if(who!=m.player)
        	{
        	moveNumber--;
        	}
        //G.print("V "+m+" for "+m.player+" "+whoseTurn+" "+moveNumber);
        //G.Assert((moveNumber+1)== enter,"move down");
 
 }

// get the legal dests from "from" for player "forplayer"
// temp can be null, in which case only the number of dests is returned
 Warp6Cell getDestFrom(Warp6Cell from,int forplayer,boolean picked)
 {	
 	switch(board_state)
 	{
 	case PLAY_STATE:
  		{
 		Warp6Chip fromChip = picked ? pickedObject : from.topChip();
 		int distance = fromChip.numberShowing;
 		Warp6Cell next = from;
 		while((distance-- > 0) && (next !=null)) 
 			{ next = from.exitTo(CELL_UP);
 			  if(next!=null) { from = next; }
 			}
 		}
 			break;
  	case CONFIRM_STATE:
 	case PUZZLE_STATE: 
	default:
 		break;
 	}
 	return(from);
 }

 // get a hash of moves destinations, used by the viewer
 // to know where to draw dots and also to determine what
 // is a legal move
 public Hashtable<Warp6Cell,Warp6Cell> getMoveDests()
 {	Hashtable<Warp6Cell,Warp6Cell> dd = new Hashtable<Warp6Cell,Warp6Cell>();
 	if(pickedObject!=null)
 	{
 	Warp6Cell src = pickedSourceStack[stackIndex];
 	if((src!=null)&&(src.onBoard))
 	{	
 		Warp6Cell next = getDestFrom(src,whoseTurn,true);
 		dd.put(next,next);
 	}}
	return(dd);
 }
 
 CommonMoveStack  GetListOfMoves(CommonMoveStack  all)
 {	
	switch(board_state)
	{
	case PLACE_STATE:
		{	Warp6Cell dest = firstEmptySlot();
			Warp6Cell rc[] = rack[whoseTurn];
			for(int i=0;i<rc.length;i++)
			{	Warp6Cell src = rc[i];
				Warp6Chip top = src.topChip();
				if(top!=null) 
				{ int ran = randomSeq.nextInt(0xfffffff);
				  all.addElement(new Warp6Movespec(MOVE_ONBOARD,src.rackLocation(),i,dest.row,whoseTurn,ran));
				}
			}
		
		}
		break;
	case PLAY_STATE:
		{
		for(Warp6Cell c = allCells;
			c!=null;
			c=c.next)
			{
				Warp6Chip ch = c.topChip();
				if((ch!=null) && (ch.color==playerColor[whoseTurn]))
				{	Warp6Cell de = getDestFrom(c,whoseTurn,false);
					int ran = randomSeq.nextInt(0xfffffff);
					if(ch.canAdd()) 
						{ all.addElement(new Warp6Movespec(MOVE_ROLLUP,WarpId.BoardLocation,c.row,whoseTurn));
						}
					if(ch.canSub())
						{ all.addElement(new Warp6Movespec(MOVE_ROLLDOWN,WarpId.BoardLocation,c.row,whoseTurn));
						}
				    all.addElement(new Warp6Movespec(MOVE_BOARD_BOARD,c.row,de.row,whoseTurn,ran));
				}
		}}
		break;
	default: throw G.Error("Not expecting");
	}
  	return(all);
 }
 
}