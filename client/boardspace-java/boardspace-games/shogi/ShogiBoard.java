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
package shogi;


import java.util.Hashtable;

import lib.*;
import online.game.*;


/**
 * ShogiBoard knows all about the game of Shogi, which is played
 * on a 9x9 board. It gets a lot of logistic support from 
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
 * todo: perfect the perpetual chase/perpetual check logic.  Muller's recommendation
 * from http://www.jcraner.com/qianhong/forum/viewtopic.php?p=428#428
 * 
 * Chases are more tricky. In the following concise definition, the term 'legal move' means a move that
creates a position that does not leave the mover in check. It does not necessarily mean that the
position from which the move starts is a legal position; King capture might be possible, e.g. to
determine if you attack something with a move that delivers check at the same time.

1) A piece is chased on a given move if there exists at least one piece of the moving side that satisfies all following criteria:
   a) it can capture the chased piece with a legal move after the given move, when the same side would be allowed to move again
   b) it could not yet legally capture the chased piece before the move (when it really was his turn)
   c) it is not a King or a Pawn
   d) it can not be captured through a legal move by the chased piece when it is of equal type
   e) after making the capture, it can not be recaptured by any legal move, or
      it is a Horse or Cannon, and it attacks a Rook

2) A sequence of moves is a perpetual chase (by one side), if every move (by that side) leading
   from the previous occurrence of the position to the repeated one is a chase.

3) A perpetual chase is forbidden, if there exists at least one piece that is chased on every move of the perpetual,
   and that piece is not a Pawn that has not crossed the River yet.

4) The outcome of a repetitive game is determined as follows (in order of proiority):
   a) When both sides are perpetually checking, it is a draw, irrespective of whether they are also chasing.
   b) When one side is perpetually checking, and the other not, the checking side loses,
      even if the other side was making a forbidden perpetual chase.
   c) when both sides make a forbidden perpetual chase, it is draw, irrespective of the type of pieces chased.
   d) when only one side makes a forbidden perpetual chase, that side loses
   e) otherwise, it is draw 
 *
 */

class ShogiBoard extends rectBoard<ShogiCell> implements BoardProtocol,ShogiConstants
{	ShogiState board_state = ShogiState.Puzzle;
	private ShogiState unresign = null;
	private int lastDrawMove = 0;
	private int prevLastPicked = -1;
	private int prevLastDropped = -1;
	public ShogiState getState() { return(board_state); }
	public void setState(ShogiState st) 
	{ 	unresign = ((st==ShogiState.Resign)||(st==ShogiState.OfferDraw))?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public RepeatedPositions repeatedPositions = null;
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    private int forwardDirection[] = { CELL_DOWN, CELL_UP};
    
    public void SetDrawState() 
    { if(currentPlayState!=ShogiState.Check) { setState(ShogiState.IllegalMove); }}
    public ShogiCell rack[][] = null;
	public CellStack occupied[] = new CellStack[2];
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ShogiChip pickedObject = null;
    public ShogiChip originalDroppedObject = null;
    public ShogiChip originalPickedObject = null;
    public ShogiCell pickedSource = null;
    public ShogiCell droppedDest = null;
    public ShogiCell prevDroppedDest = null;
    public ShogiCell prevPickedSource = null;
    public CellStack animationStack = new CellStack();
    public ShogiState currentPlayState;
    private ShogiCell captured = null;
    private ShogiCell kingLocation[] = new ShogiCell[2];
	// factory method
	public ShogiCell newcell(char c,int r)
	{	return(new ShogiCell(c,r));
	}
    public ShogiBoard(String init,long key,RepeatedPositions rep) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = new String[3];
    	 Grid_Style[0]=SHOGIGRIDSTYLE[0]; //coordinates left and bottom
    	 Grid_Style[1]=SHOGIGRIDSTYLE[1]; //coordinates left and bottom
    	 Grid_Style[2]=SHOGIGRIDSTYLE[2]; //coordinates left and bottom
    	 repeatedPositions = rep;			//share the repeated positions list with the viewer
    	 doInit(init,key); // do the initialization 
     }


    int placements[][] = {
    		{'E',1,ShogiChip.PieceType.General.ordinal()},
    		{'F',1,ShogiChip.PieceType.Gold.ordinal()},
    		{'D',1,ShogiChip.PieceType.Gold.ordinal()},
    		{'A',1,ShogiChip.PieceType.Lance.ordinal()},
    		{'I',1,ShogiChip.PieceType.Lance.ordinal()},
    		{'B',1,ShogiChip.PieceType.Knight.ordinal()},
    		{'H',1,ShogiChip.PieceType.Knight.ordinal()},
    		{'C',1,ShogiChip.PieceType.Silver.ordinal()},
       		{'G',1,ShogiChip.PieceType.Silver.ordinal()},
       		{'H',2,ShogiChip.PieceType.Bishop.ordinal()},
       		{'B',2,ShogiChip.PieceType.Rook.ordinal()},
       		{'A',3,ShogiChip.PieceType.Pawn.ordinal()},
       		{'B',3,ShogiChip.PieceType.Pawn.ordinal()},
       		{'C',3,ShogiChip.PieceType.Pawn.ordinal()},
       		{'D',3,ShogiChip.PieceType.Pawn.ordinal()},
       		{'E',3,ShogiChip.PieceType.Pawn.ordinal()},
       		{'F',3,ShogiChip.PieceType.Pawn.ordinal()},
       		{'G',3,ShogiChip.PieceType.Pawn.ordinal()},
       		{'H',3,ShogiChip.PieceType.Pawn.ordinal()},
       		{'I',3,ShogiChip.PieceType.Pawn.ordinal()}

    };
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	if(Shogi_INIT.equalsIgnoreCase(game)) 
    		{ boardColumns=DEFAULT_COLUMNS; 
    		  boardRows = DEFAULT_ROWS;
    		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(ShogiState.Puzzle);
        initBoard(boardColumns,boardRows); //this sets up the board and cross links
        for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX; i++)
        {
        occupied[i].clear();
        }
        
        for(int i=0; i<placements.length; i++)
        { int loc[] = placements[i];
          ShogiCell c = getCell((char)loc[0],10-loc[1]);
          addChip(c,ShogiChip.getChip(0,loc[2]));
          ShogiCell d = getCell((char)('I'-loc[0]+'A'),loc[1]);
          addChip(d,ShogiChip.getChip(1,loc[2]));
       }
        whoseTurn = FIRST_PLAYER_INDEX;
 
    }

    public void sameboard(BoardProtocol f) { sameboard((ShogiBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ShogiBoard from_b)
    {	super.sameboard(from_b);
    	sameCells(occupied,from_b.occupied);
    	sameCells(rack,from_b.rack);
 
        // here, check any other state of the board to see if
        G.Assert((pickedObject==from_b.pickedObject),"pickedObject matches");
        G.Assert((unresign == from_b.unresign), "unresign mismatch");
        G.Assert(ShogiCell.sameCell(pickedSource,from_b.pickedSource),"same pickedSource");
        G.Assert(ShogiCell.sameCell(droppedDest,from_b.droppedDest), "same droppedDest");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

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
  		v ^= chip.Digest(r,pickedObject);
		v ^= chip.Digest(r,originalDroppedObject);
		v ^= chip.Digest(r,originalPickedObject);
		v ^= cell.Digest(r,pickedSource);
		v ^= Digest(r,rack);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);

        return (v);
    }
   
   public ShogiBoard cloneBoard() 
	{ ShogiBoard copy = new ShogiBoard(gametype,randomKey,repeatedPositions);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((ShogiBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ShogiBoard from_b)
    {	super.copyFrom(from_b);

        repeatedPositions = from_b.repeatedPositions.copy();
        pickedObject = from_b.pickedObject;
        originalDroppedObject = from_b.originalDroppedObject;
        originalPickedObject = from_b.originalPickedObject;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        lastDrawMove = from_b.lastDrawMove;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = getCell(from_b.droppedDest);
        copyFrom(rack,from_b.rack);
        getCell(occupied,from_b.occupied);
        getCell(kingLocation,from_b.kingLocation);

        if(G.debug()) { sameboard(from_b); }
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;	// not used, but for reference in this demo game
    	Random r = new Random(647637);
    	rack = new ShogiCell[2][ShogiChip.NPIECETYPES];
        AR.setValue(win,false);
        AR.setValue(kingLocation, null);
	    for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX;  pl++)
	    {	occupied[pl] = new CellStack();
	    	for(int j=0;j<ShogiChip.NPIECETYPES;j++)
	    	{  
	    	ShogiCell cell = new ShogiCell(r,'@',j,RackLocation[pl]);
	    	rack[pl][j]=cell;
	     	}    
	   }
       Init_Standard(gtype);
       allCells.setDigestChain(r);
       animationStack.clear();
       prevLastPicked = -1;
       prevLastDropped = -1;
        captured = null;
        pickedObject = null;
        originalDroppedObject = null;
        originalPickedObject = null;
        pickedSource = null;
        droppedDest = null;
        prevPickedSource = null;
        prevDroppedDest = null;
        unresign = null;
        lastDrawMove = 0;
        moveNumber = 1;
        currentPlayState = ShogiState.Puzzle;

        // note that firstPlayer is NOT initialized here
    }


    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        case IllegalMove:
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case Puzzle:
            break;
        case Confirm:
        case Draw:
        case Resign:
        case OfferDraw:
        case AcceptDraw:
        case DeclineDraw:
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
         case OfferDraw:
         case AcceptDraw:
         case DeclineDraw:
         case Draw:
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	switch(board_state)
    	{	
    	case OfferDraw:
    	case AcceptDraw:
    	case DeclineDraw:
    		return(false);
    	default:
    		return(DoneState());
    	}
    }
    
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(ShogiState.Gameover);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==ShogiState.Gameover) { return(win[player]); }
    	boolean win = (captured!=null) && (captured.topChip().pieceType == ShogiChip.PieceType.General);
    	// general captured is a loss
    	return(win);
    }
    // look for a win for player.  
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=0.0;
    	CellStack pieces = occupied[player];
    	for(int i=0,lim=pieces.size(); i<lim;i++)
    	{
    	ShogiCell c = pieces.elementAt(i);
    	ShogiChip top = c.topChip();
    	// count the wood
    	finalv += top.pieceType.standardValue;
    	}
    	for(ShogiCell c : rack[player])
    	{	ShogiChip ch = c.topChip();
    		if(ch!=null)
    		{	ShogiChip.PieceType pp = ch.pieceType.demoted;
    			finalv += pp.standardValue*c.height()*0.5;	// half credit for captured pieces
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
    {	prevDroppedDest = droppedDest;
    	prevPickedSource = pickedSource;
        pickedObject = null;
        originalDroppedObject = null;
        originalPickedObject = null;
        droppedDest = null;
        pickedSource = null;
        captured = null;
     }
    public void addChip(ShogiCell c,ShogiChip oo)
    {	if(c.onBoard)
    	{
    	G.Assert(c.topChip()==null,"currently empty");
    	int pl = oo.playerIndex;
    	occupied[pl].push(c);
    	if(oo.pieceType==ShogiChip.PieceType.General) { kingLocation[pl]=c; }
    	}
    	c.addChip(oo);
    	
    }
    public ShogiChip removeChip(ShogiCell c)
    {   G.Assert(c.topChip()!=null,"currently empty");
    	ShogiChip rem = c.removeTop();
    	if(c.onBoard)
    	{int pl = rem.playerIndex;
    	 occupied[pl].remove(c,false);
    	}
    	return(rem);
    }  
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    ShogiCell dr = droppedDest;
    if(dr!=null)
    	{
    	droppedDest = null;
    	removeChip(dr);
    	dr.lastDropped = prevLastDropped;
    	prevLastDropped = -1;
    	pickedObject = originalDroppedObject;	// in case there was a promotion
    	originalDroppedObject = null;
    	if(captured!=null) { addChip(dr,captured.removeTop()); captured = null;  }
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	ShogiChip po = originalPickedObject;
    	if(po!=null)
    	{
    	ShogiCell ps = pickedSource;
    	ps.lastPicked = prevLastPicked;
    	prevLastPicked = -1;
    	pickedSource=null;
    	pickedObject = null;
    	originalPickedObject = null;
    	addChip(ps,po);
    	}
     }
    public ShogiCell getRackCell(ShogiId rackLo,int n)
    {	switch(rackLo)
    	{
    	case Down_Chip_Pool: return(rack[SECOND_PLAYER_INDEX][n]);
    	case Up_Chip_Pool: return(rack[FIRST_PLAYER_INDEX][n]);
    	default: throw G.Error("not expecting %s",rackLo);
    	}
    }
    private ShogiCell dropOnRack(ShogiId rack,ShogiChip ob)
    {
    	ShogiCell c = getRackCell(rack,ob.pieceType.demoted.ordinal());
    	originalDroppedObject = null;
    	addChip(c,ob);
    	return(c);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ShogiCell cell)
    {	return(droppedDest==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	ShogiChip ch = pickedObject;
    	if((ch!=null)&&(droppedDest==null))
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
  

   public ShogiCell getCell(ShogiCell c) 
   { if(c==null) { return(null); }
   	 if(c.onBoard) { return(getCell(c.col,c.row)); }
   	 return(getRackCell(c.rackLocation(),c.row));
   }

   ShogiChip pickFromRack(ShogiId pl,int oi)
   {	ShogiCell c = pickedSource = getRackCell(pl,oi);
   		originalPickedObject = removeChip(c);
   		ShogiChip down = originalPickedObject.getDemoted();
   		int player = (c.rackLocation==ShogiId.Up_Chip_Pool)?FIRST_PLAYER_INDEX:SECOND_PLAYER_INDEX;
   		pickedObject = ShogiChip.getChip(player,down.pieceType);
   		return(pickedObject);
   }

    private void setNextStateAfterDrop()
    {	currentPlayState = board_state;
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Confirm:
        case Draw:
        	if(droppedDest!=null) { setNextStateAfterDone(); }
        	break;
        case Play:
        	if(!pickedSource.onBoard
        			&& (droppedDest.topChip().pieceType==ShogiChip.PieceType.Pawn)
        			)
        	{	ShogiCell king = kingLocation[nextPlayer[whoseTurn]];
        		if((king.col==droppedDest.col)
        				&& (king.row==droppedDest.row+droppedDest.topChip().forwardOneRow))
        		{
        			// pawn puts the king in check.  Illegal if it's mate
        			if(!canEscapePawnCheck(nextPlayer[whoseTurn])) 
        				{ setState(ShogiState.IllegalMove);
        				  break;
        				}
        		}
        		
        	}
        	if(attackingPosition(occupied[nextPlayer[whoseTurn]],kingLocation[whoseTurn],null,null))
        			{ // uncovered check
        			  setState(ShogiState.IllegalMove);
        			  break;
        			}
			//$FALL-THROUGH$
		case IllegalMove:
        case Check:
			setState(ShogiState.Confirm);
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
    public boolean isSource(ShogiCell c)
    {	return(pickedSource==c);
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
        case IllegalMove:
        case Confirm:
        case Draw:
        	setState(currentPlayState);
        	break;
        case Play:
        case Check:
			break;
        case Puzzle:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
        case IllegalMove:
        default: throw G.Error("Not expecting state %s",board_state);
        case OfferDraw:
        	setState(ShogiState.QueryDraw);
        	lastDrawMove = moveNumber;
        	break;
        case AcceptDraw:
        	setGameOver(false,false);
        	break;
    	case Gameover: 
    		break;

        case Draw:
        	setGameOver(true,false);	//forcing a draw by repetition is a loss
        	break;
    	case Confirm:
    	case Puzzle:
    		if(attackingPosition(occupied[nextPlayer[whoseTurn]],kingLocation[whoseTurn],null,null))
    				{ 	setState(ShogiState.Check); 
    					if(!hasMoves(whoseTurn))
    					{
    					// if this is a drop-pawn checkmate, disallow it.
    					setGameOver(false,true); 
    					}
    				break;
    				}
			//$FALL-THROUGH$
		case Play:
    	case Check:
    	case DeclineDraw:
    			{
    				setState(ShogiState.Play);
    			}
    		break;
    	}
       	currentPlayState = board_state;
    }
   
    
    private void doDone()
    {	boolean win = (captured!=null) && (captured.topChip().pieceType == ShogiChip.PieceType.General);
        acceptPlacement();
        if (board_state==ShogiState.Resign)
        {	setGameOver(false,true);
        }
        else
        {  	if(win)  { setGameOver(true,false); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
        currentPlayState = board_state;
    }


    public boolean Execute(commonMove mm,replayMode replay)
    {	ShogiMovespec m = (ShogiMovespec)mm;
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_NULL:
        	setState(ShogiState.Confirm);
			//$FALL-THROUGH$
		case MOVE_DONE:
        	if(replay.animate) 
        		{ if(captured !=null) 
        			{ m.annotation = captured.topChip(); }
        		}
         	doDone();
          	if(replay.animate) 
          		{ if(board_state==ShogiState.Check) 
          			{ m.check = true; }
          		}
            break;
        case MOVE_ONBOARD:
        	{	ShogiCell from = pickedSource = getRackCell(m.source,m.piece);
        		ShogiCell to = droppedDest = getCell(m.to_col,m.to_row);
        		ShogiChip cap = m.captures = from.removeTop();
        		ShogiChip chip = ShogiChip.getChip(m.player,cap.pieceType.demoted);
        		m.chip = chip;
        		addChip(to,chip);
        		prevLastDropped = to.lastDropped;
        		to.lastDropped = moveNumber;
        		setNextStateAfterDrop();
        		if(replay.animate)
        		{	animationStack.push(from);
        			animationStack.push(to);
        		}
        	}
        	break;
        case MOVE_PROMOTE:
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        		case Check:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			ShogiCell from = getCell(m.from_col, m.from_row);
        			ShogiCell to = getCell(m.to_col,m.to_row);
        			ShogiChip rem = to.topChip();
        			if(replay.animate)
        			{
        				animationStack.push(from);
        				animationStack.push(to);
        			}
        			if(rem!=null) 
        				{ captured = dropOnRack(RackLocation[whoseTurn],removeChip(to)); 
        				if(replay.animate)
        				  {
        					  animationStack.push(to);
        					  animationStack.push(captured);
        				  }
        				}
        			ShogiChip moving = removeChip(from);

        			pickedSource = from;
        			droppedDest = to;
        			prevLastPicked = from.lastPicked;
        			prevLastDropped = to.lastDropped;
        			from.lastPicked = moveNumber;
        			to.lastDropped = moveNumber;
                    m.chip = moving; 	// for game record

  				    if(m.op==MOVE_PROMOTE)
 				     {	moving = moving.getPromoted();
 				     }
        			addChip(to,moving);
        			m.captures = rem;

  				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
			ShogiCell to = getCell(m.to_col, m.to_row);
			ShogiChip rem = to.topChip();
			ShogiChip originalP = pickedObject;
			boolean canPromote = canPromote(pickedSource,to,pickedObject,whoseTurn);
			if(canPromote) { pickedObject = pickedObject.getPromoted(); };
			if(rem!=null) 
				{captured = dropOnRack(RackLocation[whoseTurn],removeChip(to)); 
				 if(replay.animate)
				 {
					 animationStack.push(to);
					 animationStack.push(captured);
				 }
				}
			// defer this until after the capture check
			originalDroppedObject = originalP;
  			if(replay==replayMode.Single)
			{
				animationStack.push(pickedSource);
				animationStack.push(to);
			}

            if(pickedSource==to) 
            	{ 
            	  unPickObject(); 
            	} 
            	else
            		{
            		addChip(droppedDest = getCell(m.to_col,m.to_row),pickedObject);
            		prevLastDropped = droppedDest.lastDropped;
            		droppedDest.lastDropped = moveNumber;
            		setNextStateAfterDrop();
            		}

            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	ShogiCell from = getCell(m.from_col,m.from_row);
        	if(isDest(from))
        		{ if((captured!=null)&&(replay.animate))
            		{
            		animationStack.push(captured);
            		animationStack.push(from);
            		}
        		  unDropObject(); 
              	  setState(currentPlayState);
        		}
        	else 
        		{ pickedSource = from;
        		  prevLastPicked = from.lastPicked;
        		  from.lastPicked = moveNumber;
        		  originalPickedObject = pickedObject = removeChip(from);
                  m.chip = pickedObject; 	// for game record
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
          		}
        	}
            break;

        case MOVE_DROP: // drop on chip pool;
        	
        	droppedDest = dropOnRack(m.source,pickedObject);
        	pickedObject = null;
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickFromRack(m.source,m.from_row);
            m.chip = pickedObject; 	// for game record
            setNextStateAfterPick();
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            setState(ShogiState.Puzzle);
            win[FIRST_PLAYER_INDEX]=win[SECOND_PLAYER_INDEX]=false;
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

       case MOVE_RESIGN:
    	   setState(unresign==null?ShogiState.Resign:unresign);
    	   break;
       case MOVE_EDIT:
    		acceptPlacement();
            setState(ShogiState.Puzzle);
            win[0]=win[1]=false;

            break;

        case MOVE_OFFER_DRAW:
        {	
        	ShogiState bs = board_state;
        	if(bs==ShogiState.OfferDraw)
        	{
        	setState(unresign);
        	}else
        	{
        	unresign = bs;
        	setState(ShogiState.OfferDraw);
        	}}
        	break;
        	
        case MOVE_ACCEPT_DRAW:
         	setState(board_state==ShogiState.AcceptDraw?ShogiState.QueryDraw:ShogiState.AcceptDraw);
        	break;
        	
        case MOVE_DECLINE_DRAW:
        	setState(board_state==ShogiState.DeclineDraw?ShogiState.QueryDraw:ShogiState.DeclineDraw);
        	break;
        case MOVE_FLIP:
        	{	ShogiCell dest = getCell(m.to_col,m.to_row);
        		ShogiChip top = removeChip(dest);
        		addChip(dest,top.getFlipped());
        	}
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
        	throw G.Error("Not expecting state %s", board_state);
        case Confirm:
        case Draw:
        case IllegalMove:
		case Gameover:
		case QueryDraw:
		case DeclineDraw:
		case AcceptDraw:
		case Resign:
		case OfferDraw:
			return(false);
			
		case Check:
		case Play: 
        	return(player==whoseTurn);
        case Puzzle:
        	return(true);
        }
    }
  

    public boolean LegalToHitBoard(ShogiCell cell)
    {	
        switch (board_state)
        {
 		case Play:
 		case Check:
 			if(pickedObject!=null)
 			{
 			return(isSource(cell) || (getDests().get(cell)!=null));
 			}
 			else
 			{ return(getSources().get(cell)!=null);
 			}
 			
		case Gameover:
		case QueryDraw:
		case DeclineDraw:
		case AcceptDraw:
		case OfferDraw:
		case Resign:
			return(false);
		case Confirm:
		case IllegalMove:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedObject==null?(cell.chipIndex>=0):true);
        }
    }
  public boolean canDropOn(ShogiCell cell)
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
    public int RobotExecute(ShogiMovespec m,boolean digest)
    {	int num = 0;
        m.state =  board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if(digest)
            {	long dig = Digest();
            	num = repeatedPositions.addToRepeatedPositions(dig,m);
            }         	
        	if ((m.op==MOVE_NULL) || (m.op == MOVE_DONE))
            {
            }
            else if (DoneState())
            {
                doDone();
            }
            else if(board_state==ShogiState.IllegalMove)
            {	acceptPlacement();
            	setGameOver(false,true);
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
        return(num);
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(ShogiMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_ACCEPT_DRAW:
   	    case MOVE_DECLINE_DRAW:
        case MOVE_DONE:
        case MOVE_NULL:
            break;
        case MOVE_ONBOARD:
        	{	
        	ShogiCell from = getRackCell(m.source,m.piece);
    		ShogiCell to = getCell(m.to_col,m.to_row);
    		removeChip(to);
    		addChip(from,m.captures);
        	}
        	break;

        case MOVE_PROMOTE:
        case MOVE_BOARD_BOARD:
        	// complete move, used for the robot
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Gameover:
        		case Check:
        		case Play:
        			ShogiCell from = getCell( m.from_col,m.from_row);
        			ShogiCell to = getCell(m.to_col, m.to_row);
        			ShogiChip rem = m.captures;
        			ShogiChip newchip = removeChip(to);
        			if(m.op==MOVE_PROMOTE)
        			{	newchip = newchip.getDemoted();
        			}
        			addChip(from,newchip);
        			if(rem!=null) 
        				{ addChip(to,rack[m.player][rem.pieceType.demoted.ordinal()].removeTop());
        				}
          			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        win[FIRST_PLAYER_INDEX]=win[SECOND_PLAYER_INDEX]=false; 
        setState(m.state);
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }



 //
 // this is the canonical check for "check" in a static position
 //

 private boolean attackingPosition(CellStack from,ShogiCell to,ShogiCell empty,ShogiCell filled)
 {	if(to!=null)
	{for(int i=0,lim=from.size(); i<lim; i++)
 	{
	 ShogiCell c = from.elementAt(i);
	 if(attackingPosition(c,c.topChip(),to,empty,filled))
	 	{ return(true); }
 	}}
	return(false);
 }
 boolean vacantDirection(ShogiCell from,ShogiCell to,int forward,ShogiCell empty,ShogiCell filled)
 {
	ShogiCell next = from.exitTo(forward);
	while((next!=null) && (next!=to)) 
		{ if(next==filled) { return(false); }
		  if((next!=empty)&&(next.topChip()!=null)) { return(false); }
		  next=next.exitTo(forward); 
		}
	return(true);
 }
 //
 // this is used only to check for check, and the position can only be in the opposing player's palace
 // if "empty" is not null, it's a space that has been vacated to fill "filled"
 //
 boolean attackingPosition(ShogiCell from,ShogiChip top,ShogiCell to,ShogiCell empty,ShogiCell filled)
 {	 if(from==filled) 
 		{
	 	// this corresponds to a piece that's about to be captured. so it won't be threatening anything
	 	return(false); 
 		}
 	ShogiChip.PieceType piece = top.pieceType;
	 switch(piece)
	 {
	 case Pawn:	return((to.col==from.col) && (from.row+top.forwardOneRow == to.row));
	 case Knight: return( ((to.col==from.col-1)||(to.col==from.col+1))&&(from.row+2*top.forwardOneRow == to.row));
	 case General: return( (from.col>=to.col-1)
			 				&&(from.col<=to.col+1)
			 				&& (from.row>=to.row-1)
			 				&&(from.row<=to.row+1));
	 case Promoted_Knight:
	 case Promoted_Silver:
	 case Promoted_Pawn:
	 case Promoted_Lance:
	 case Gold:
		 	return( ( ((to.row==from.row) || (to.row == from.row+top.forwardOneRow)) && (from.col>=to.col-1) && (from.col<=to.col+1))
		 			|| ((to.row==from.row-top.forwardOneRow) && (from.col==to.col))
		 			);
		 
	 case Silver:
		 	return( ((to.row == from.row+top.forwardOneRow) && (from.col>=to.col-1) && (from.col<=to.col+1))
		 			|| ((to.row==from.row-top.forwardOneRow) && ((from.col==to.col-1) || (from.col==to.col+1)))
		 			);
	 case Promoted_Bishop:
		 if((from.col>=to.col-1)
			&&(from.col<=to.col+1)
			&& (from.row>=to.row-1)
			&&(from.row<=to.row+1)) { return(true); }
		//$FALL-THROUGH$
	case Bishop:
		if(to.row-from.row == to.col-from.col)
	 	{	return(vacantDirection(from,to,(to.row>from.row)?CELL_UP_RIGHT:CELL_DOWN_LEFT,empty,filled));
	 	}
		else if(to.row-from.row == -(to.col-from.col))
		{	return(vacantDirection(from,to,(to.row<from.row)?CELL_DOWN_RIGHT:CELL_UP_LEFT,empty,filled));
		}
	 	return(false);
	 case Promoted_Rook:
		 if(  (from.col>=to.col-1)
			&&(from.col<=to.col+1)
			&&(from.row>=to.row-1)
			&&(from.row<=to.row+1)) { return(true); }
		//$FALL-THROUGH$
	case Rook: 
	 	if(to.col==from.col)
	 		{ if(to.row>from.row) 
	 			{ return(vacantDirection(from,to,CELL_UP,empty,filled)); }
	 			else { return(vacantDirection(from,to,CELL_DOWN,empty,filled)); }}
	 	else if(to.row==from.row)
	 	{	if(to.col<from.col) { return(vacantDirection(from,to,CELL_LEFT,empty,filled)); }
	 			else { return(vacantDirection(from,to,CELL_RIGHT,empty,filled)); }
	 	}
	 	return(false);
	 case Lance:
	 	if((to.col==from.col)&&(Integer.signum(to.row-from.row)==top.forwardOneRow))
	 		{	int forward = forwardDirection[top.playerIndex];
	 			return(vacantDirection(from,to,forward,empty,filled));
	 		}
	 	return(false);
	 default: throw G.Error("not expecting %s",top);

	 }
   }
 
 //return true of the piece can be promoted as part of this move
 boolean canPromote(ShogiCell from,ShogiCell d,ShogiChip ctop,int who)
 {	return(from.onBoard 
		 	&& (ctop!=null)
		 	&& ( (ctop.pieceType.promoted!=null)?promotable[who][d.row]||promotable[who][from.row]:false));
 }
 //canPromote is known to be true.  In 
 //return true if the promotion is mandatory
 boolean mustPromote(ShogiCell d,ShogiChip ctop,int who)
 {	
	 boolean mustPromote = false;
	 switch(ctop.pieceType)
			{
			case Lance: mustPromote = d.row==((who==0)?DEFAULT_ROWS:1);	// lance must promote at the border
				break;
			case Pawn: 
				mustPromote = true;	// pawn must promote if it can, there's no reason not to.
				break;
			case General:
				throw G.Error("Can't promote");
			default:break;
			}
	return(mustPromote);
 }
 //
 // add moves from c to d, 
 // knows about mandatory and optional promotion
 // multiple moves are added if both promoted and unpromoted moves are possible.
 // returns true if any moves exist
 //
 private boolean addMoveIfFree(CommonMoveStack  v,ShogiCell c,ShogiCell d,int who)
 {	boolean some = false;
	 if(d!=null)
	 	{	ShogiChip dtop = d.topChip();
	 		if((dtop==null) || (dtop.playerIndex!=who)) 
	 		{	if(v==null) { return(true); }
	 			ShogiChip ctop = c.topChip();
 				boolean canPromote = canPromote(c,d,ctop,who);
	 			boolean mustPromote = canPromote ? mustPromote(d,ctop,who) : false;

	 			if(!mustPromote) {	v.addElement(new ShogiMovespec(MOVE_BOARD_BOARD,c.col,c.row,d.col,d.row,who)); }
	 			if(canPromote) 
	 				{ v.addElement(new ShogiMovespec(MOVE_PROMOTE,c.col,c.row,d.col,d.row,who)); 
	 				}
	
	 			some = true;
	 		}
	 	}
	 return(some);
 }
 //
 // add moves starting from c in direction.
 // v can be null
 // includes possible capturing moves at the end of the line
 // returns true if any moves exist
 private boolean addLinearMoves(CommonMoveStack  v,ShogiCell c,int direction,int who)
 {	boolean some = false;
	ShogiCell d = c;
	while(d!=null) 
		{ d = d.exitTo(direction);
		  if(d!=null)
			  { some |= addMoveIfFree(v,c,d,who);
			  	if(some && (v==null)) { return(some); }
			    if(d.topChip()!=null) { d = null; }
			  }
		}
	return(some);
 }
 //
 // get all the drop moves for a player
 // v can be null, in which case no list is generated
 // return true if any drop moves exist
 //
 public boolean getDropMoves(CommonMoveStack  v,int who)
 {	boolean some = false;
 	ShogiCell drops[] = rack[who];
 	boolean shortcut = (v==null);
 	if(shortcut&&(board_state==ShogiState.Check)) { v = new CommonMoveStack(); } 
 	for(ShogiCell drop : drops)
 	{	ShogiChip top = drop.topChip();
 		if(top!=null)
 		{	some |= getDropMoves(v,top,who);
 			if(some && (v==null)) { return(true); }
 		}
 	}
 	if(board_state==ShogiState.Check)
 	{	some = filterCheckMoves(v,who,shortcut);
  	}
 	return(some);
 }
 //
 // get the available drop moves for piece "top"
 // top may be either promoted or not, but the unpromoted
 // piece will drop.  Also top may be for either player 
 // but the chip for player to move will drop.
 //
 // v can be null, in which case no list is returned
 // return true if any drop moves exist
 public boolean getDropMoves(CommonMoveStack  v,ShogiChip top,int who)
 {	boolean some = false;
	switch(top.pieceType)
	{
	case Knight:
	case Promoted_Knight:
		{
		// knights can't drop into the last two rows because they would be unable to move.
		int forbidden_row = (who!=0) ? DEFAULT_ROWS : 1;
		int forbidden_row_2 = (who!=0) ? DEFAULT_ROWS-1 : 2;
		for(ShogiCell c = allCells; c!=null; c=c.next)
		{	if((c.row!=forbidden_row) && (c.row!=forbidden_row_2) && (c.topChip()==null))
			{	if(v==null) { return(true); }
				ShogiChip.PieceType type = top.pieceType.demoted;
				v.addElement(new ShogiMovespec(MOVE_ONBOARD,c.col,c.row,type.ordinal(),who));
				some = true;
			}
		}
		}
		break;
	case Lance:
	case Promoted_Lance:
		// can't drop on the back row because it would be unable to move
		{
		int forbidden_row = (who!=0) ? DEFAULT_ROWS : 1;
		for(ShogiCell c = allCells; c!=null; c=c.next)
		{	if((c.row!=forbidden_row) && (c.topChip()==null))
			{	if(v==null) { return(true); }
				ShogiChip.PieceType type = top.pieceType.demoted;
				v.addElement(new ShogiMovespec(MOVE_ONBOARD,c.col,c.row,type.ordinal(),who));
				some = true;
			}
		}
		}
		break;
	case Pawn:
	case Promoted_Pawn:
		// can't drop on a column with another unpromoted pawn, or in the back row
		// also can't drop to cause checkmate, but that isn't handled here.
		{
		int forbidden_row = (who!=0) ? DEFAULT_ROWS : 1;
		int forbidden_column_mask = 0;	// mask of columns that contain our pawns
		for(ShogiCell c = allCells; c!=null; c=c.next)
			{	// scan the board for our pawn, build a mask
				ShogiChip topchip = c.topChip();
				if((topchip!=null) && (topchip.playerIndex==who) && (topchip.pieceType==ShogiChip.PieceType.Pawn))
					{ forbidden_column_mask |= (1<<(c.col-'A'));
					}
			}
		for(ShogiCell c = allCells; c!=null; c=c.next)
			{
			if( (c.row!=forbidden_row)	// in the back row 
					&& (((1<<(c.col-'A')) & forbidden_column_mask ) ==0) // not a forbidden column
					&& (c.topChip()==null))	// and in an empty space
				{
				if(v==null) { return(true); }
				ShogiChip.PieceType type = top.pieceType.demoted;
				v.addElement(new ShogiMovespec(MOVE_ONBOARD,c.col,c.row,type.ordinal(),who));
				some = true;
				}
			}
		}
		break;
	case Bishop:
	case Promoted_Bishop:
	case Silver:
	case Promoted_Silver:
	case Rook:
	case Promoted_Rook:
	case Gold:
	case General:	// general can be moved to and from the rack in puzzle mode

		// major pieces can drop anywhere
		for(ShogiCell c = allCells; c!=null; c=c.next)
		{	if(c.topChip()==null)
			{	if(v==null) { return(true); }
				ShogiChip.PieceType type = top.pieceType.demoted;
				v.addElement(new ShogiMovespec(MOVE_ONBOARD,c.col,c.row,type.ordinal(),who));
				some = true;
			}
		}
		break;
		
	case Promoted_Gold:		// nonexistent
	case Promoted_General:	// nonexistent
	default:	throw G.Error("Can't drop this type: "+top);
	}
	return(some);
 }

 //
 // if check is true, only check for moves that attack the king
 // if empty and filled are non-null, they're the temporary move which must escape check
 //
 boolean getListOfMoves(CommonMoveStack  v,ShogiCell c,ShogiChip top)
 {	boolean some = false;
 	if(top!=null)
 	{int who = top.playerIndex;
 	 int forward = forwardDirection[who];
	 switch(top.pieceType)
	 {
	 case Promoted_Gold:	// doesn't exist
	 default: throw G.Error("not expecting %s",top);
	 case General:
		 for(int dir = c.geometry.n-1; dir>=0; dir--)
		 {	
		 	some |= addMoveIfFree(v,c,c.exitTo(dir),who);
		 	if(some && (v==null)) { return(true); }
		 }
		 break;
	 case Pawn:
	 	{
	 		some |= addMoveIfFree(v,c,c.exitTo(forward),who);
	 		if(some && (v==null)) { return(true); }
	 	}
	 	break;
	 case Lance:
	 	{	some |= addLinearMoves(v,c,forward,who);
	 		if(some && (v==null)) { return(true); }
	 	}
	 	break;
	 case Silver:
	 	{
		 some |= addMoveIfFree(v,c,c.exitTo(forward),who);
		 if(some && (v==null)) { return(true); }
		 some |= addMoveIfFree(v,c,c.exitTo(forward-1),who);
		 if(some && (v==null)) { return(true); }
		 some |= addMoveIfFree(v,c,c.exitTo(forward+1),who);
		 if(some && (v==null)) { return(true); }
		 some |= addMoveIfFree(v,c,c.exitTo(forward+3),who);
		 if(some && (v==null)) { return(true); }
		 some |= addMoveIfFree(v,c,c.exitTo(forward+5),who);
		 if(some && (v==null)) { return(true); }
	 	}
	 	break;
	 case Knight:
	 	{
	 		ShogiCell d = c.exitTo(forward);
	 		if(d!=null)
	 		{	
	 		some |= addMoveIfFree(v,c,d.exitTo(forward+1),who);
	 		if(some && (v==null)) { return(true); }
	 		some |= addMoveIfFree(v,c,d.exitTo(forward-1),who);
	 		if(some && (v==null)) { return(true); }
	 		}
	 	}
		 break;
	 case Promoted_Silver:
	 case Promoted_Knight:
	 case Promoted_Pawn:
	 case Promoted_Lance:
	 case Gold:
	 	{
	 		some |= addMoveIfFree(v,c,c.exitTo(forward),who);
	 		if(some && (v==null)) { return(true); }
	 		some |= addMoveIfFree(v,c,c.exitTo(forward-1),who);
	 		if(some && (v==null)) { return(true); }
	 		some |= addMoveIfFree(v,c,c.exitTo(forward+1),who);
	 		if(some && (v==null)) { return(true); }
	 		some |= addMoveIfFree(v,c,c.exitTo(forward-2),who);
	 		if(some && (v==null)) { return(true); }
	 		some |= addMoveIfFree(v,c,c.exitTo(forward+2),who);
	 		if(some && (v==null)) { return(true); }
	 		some |= addMoveIfFree(v,c,c.exitTo(forward+4),who);
	 		if(some && (v==null)) { return(true); }
	 	}
	 	break;
	 case Promoted_Bishop:
		 {
		 for(int dir=forward,step=0;  step<4; step++,dir+=2)
		 	{
			 some |= addMoveIfFree(v,c,c.exitTo(dir),who);
			 if(some && (v==null)) { return(true); }
		 	}
		 }
		 // fall into regular bishop
		//$FALL-THROUGH$
	case Bishop:
	 	{
	 	for(int dir = forward+1,step=0; step<4; step++,dir+=2)
	 	{
	 		some |= addLinearMoves(v,c,dir,who);
	 		if(some && (v==null)) { return(true); }
	 	}
	 	}
	 	break;
	 case Promoted_Rook:
		 {
		 for(int dir=forward+1,step=0;  step<4; step++,dir+=2)
		 	{
			 some |= addMoveIfFree(v,c,c.exitTo(dir),who);
			 if(some && (v==null)) { return(true); }
		 	}
		 }
		 // fall into normal rook
		//$FALL-THROUGH$
	case Rook:
	 	{
	 	for(int dir = forward,step=0; step<4; step++,dir+=2)
	 	{
	 		some |= addLinearMoves(v,c,dir,who);
	 		if(some && (v==null)) { return(true); }
	 	}
	 	}
	 	break;
	 }
 	}
 	return(some);
  }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	getListOfMoves(all,whoseTurn);
 	
 	return(all);
 }
 private boolean getListOfMoves(CommonMoveStack v,CellStack mine,int who)
 {	boolean some = false;
	for(int i=0,lim=mine.size(); i<lim;  i++)
		{
		ShogiCell c = mine.elementAt(i);
		some = getListOfMoves(v,c,c.topChip());
		if((v==null)&&some) { return(true); }
		}
	return(some);
 }
 /* filter the move list v of all the moves the victim might make,
  * and remove all those that do not relieve check.
  * this is the test for checkmate.
  */
 private boolean filterCheckMoves(CommonMoveStack v,int victim,boolean shortcut)
 {	ShogiCell king = kingLocation[victim];
	for(int lim=v.size()-1; lim>=0; lim--)
		{	ShogiMovespec m = (ShogiMovespec)v.elementAt(lim);
			ShogiCell from = getCell(m.from_col,m.from_row);
			ShogiCell to = getCell(m.to_col,m.to_row);
			if(attackingPosition(occupied[nextPlayer[victim]],from==king?to:king,from,to))
			{	// still in check after this move
				v.remove(lim,false);
			}
			else if(shortcut) { return(true); }
		} 
	return(v.size()>0);
 }
 /* for the special case of a dropped pawn checking a king, see of the king can escape.
  * we don't need to consider drop moves.
  */
 public boolean canEscapePawnCheck(int victim)
 {	CommonMoveStack  v =  new CommonMoveStack(); 
    getListOfMoves(v,occupied[victim],victim);	// get the list of moves the victim might make
    return(filterCheckMoves(v,victim,true));	// remove those that don't relieve check
 }
 // if v is not null, collects the list of moves
 // who is the player to move (regardless of whose turn it is)
 // check is true if checking for game winning moves.
 // returns true if moves are possible.
 private boolean getListOfMoves(CommonMoveStack  v,int who)
 {	switch(board_state)
 	{
 	default: throw G.Error("Not expecting state %s",board_state);
 	case QueryDraw:
 		v.addElement(new ShogiMovespec(ACCEPTDRAW,who));
 		v.addElement(new ShogiMovespec(DECLINEDRAW,who));
 		break;
 	case Check:
 		{	// if we are in check, only allow moves that relieve check
 			boolean some = getListOfMoves(v,occupied[who],who);
 			some |= getDropMoves(v,who);
 			if(some)
 			{	// filter out all the moves that don't eliminate check
 				boolean shortcut = v==null;
 				if(shortcut) 
 					{ //if we didn't make a list, make one now
 					v = new CommonMoveStack(); 
 					getListOfMoves(v,occupied[who],who);
 					}
 				some = filterCheckMoves(v,who,shortcut);

 			}
 			return(some);
 		}

 	case Play:
 		
 		boolean n = getListOfMoves(v,occupied[who],who);
 		n |= getDropMoves(v,who);
 		return(n);
 	}
 	return(false);
 }
 //
 // this is the general check for stalemate, which is checkmate
 // if we're also in check.
 //
 private boolean hasMoves(int who)
 {	return(getListOfMoves(null,who) || getDropMoves(null,who));
 }
 
 //
 // used by the viewer to find where the picked piece can land.
 //
 Hashtable<ShogiCell,ShogiCell> getDests()
 {	Hashtable<ShogiCell,ShogiCell> h = new Hashtable<ShogiCell,ShogiCell>();
 	if((pickedObject!=null) && (pickedSource!=null))
 	{
	CommonMoveStack  v = new CommonMoveStack();
 	if(pickedSource.onBoard) 
 		{ getListOfMoves(v,pickedSource,pickedObject); 
 		  if(board_state==ShogiState.Check)
 		  {
 			  filterCheckMoves(v,whoseTurn,false); // remove moves that don't relieve check
 		  }
 		}
 	else 
 	{ getDropMoves(v,pickedObject,(pickedSource.rackLocation==ShogiId.Down_Chip_Pool) ? 1 : 0); 
 	  if(board_state==ShogiState.Check)
 	  {	filterCheckMoves(v,whoseTurn,false);	// remove moves that don't relieve check
 	  }
 	}
 	for(int i=0,lim=v.size(); i<lim; i++)
 	{	ShogiMovespec m = (ShogiMovespec)v.elementAt(i);
 		if((m.op==MOVE_BOARD_BOARD)||(m.op==MOVE_PROMOTE)||(m.op==MOVE_ONBOARD))
 		{	ShogiCell c = getCell(m.to_col,m.to_row);
 			h.put(c,c);
 		}
 	}
 	}
 	return(h);
	 
 }
 Hashtable<ShogiCell,ShogiCell> getSources()
 {	Hashtable<ShogiCell,ShogiCell> h = new Hashtable<ShogiCell,ShogiCell>();
 	if(pickedSource==null)
 	{
 	CommonMoveStack  v = new CommonMoveStack();
 	getListOfMoves(v,whoseTurn);
 	for(int i=0,lim=v.size(); i<lim; i++)
 	{	ShogiMovespec m = (ShogiMovespec)v.elementAt(i);
 		if((m.op==MOVE_BOARD_BOARD)||(m.op==MOVE_PROMOTE))
 		{	ShogiCell c = getCell(m.from_col,m.from_row);
 			h.put(c,c);
 		}
 	}
 	}
 	return(h);
	 
 }
public boolean canOfferDraw() {
	return (moveNumber-lastDrawMove>4)
			&& (movingObjectIndex()<0)
			&& (( board_state==ShogiState.Play) || (board_state==ShogiState.OfferDraw));
}
}
