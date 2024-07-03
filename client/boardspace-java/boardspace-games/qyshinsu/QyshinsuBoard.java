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
package qyshinsu;

import java.awt.Color;
import java.util.Hashtable;
import lib.*;
import online.game.*;


/**
 * QyshinsuBoard knows all about the game of Qyshinsu, which is played
 * on a 12x1 circular board. It gets a lot of logistic support from 
 * common.circBoard, which knows about the circular coordinate system.  
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

class QyshinsuBoard extends circBoard<QyshinsuCell> implements BoardProtocol,QyshinsuConstants
{
	private QyshinsuState unresign;
	private QyshinsuState board_state;
	public QyshinsuState getState() {return(board_state); }
	public void setState(QyshinsuState st) 
	{ 	unresign = (st==QyshinsuState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public QyshinsuCell rack[][] = new QyshinsuCell[2][QyshinsuChip.N_STANDARD_CHIPS];
    private QIds playerQids[] = new QIds[2];
    public CellStack animationStack = new CellStack();
    // because of the feedback involved in the rules, the most recent move is an
    // important part of the game state.  We could represent this with just the "most recent"
    // but because the robot wants to move/unmove when it seartches, it's convenient to 
    // represent the whole history as a stack.
	private CellStack vlastMove[] = new CellStack[2];	// stack of board cells involved in moves
	private ChipStack vlastChip[] = new ChipStack[2];	// stack of chips involved in moves
    
	private void addLast(int who,QyshinsuCell c,QyshinsuChip m)
    {  	
    	vlastMove[who].push(c);
    	vlastChip[who].push(m);
    }
    private void removeLast(int who)
    {	CellStack v1 = vlastMove[who];
    	ChipStack v2 = vlastChip[who];
   		v1.pop();
   		v2.pop();
     }

    // get the last move for a player
    public QyshinsuCell getLastMove(int who) 
    	{ 
    	  return((vlastMove[who].top()));
    	}
    // get the chip last moved by a player
    public QyshinsuChip getLastChip(int who) 
		{ return((vlastChip[who].top()));
		}
    
 
    // called from Execute when the a third repetition is detected.
    public void SetDrawState() { setState(QyshinsuState.DRAW_STATE); }
   
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public QyshinsuChip pickedObject = null;
    public QyshinsuCell pickedSource = null;
    public QyshinsuCell droppedDest = null;
    
    public QyshinsuCell getCell(char col,int row)
    {	if(row<=0) { row+=boardRows; }
		else if(row>boardRows) { row-=boardRows; }
    	return(super.getCell(col,row));
    }
    public QyshinsuCell getCell(QyshinsuCell c)
    {
    	return c==null ? null : getCell(c.rackLocation(),c.col,c.row);
    }
	// factory method
	public QyshinsuCell newcell(char c,int r)
	{	return(new QyshinsuCell(c,r));
	}
    public QyshinsuBoard(String init,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = QYSHINDUGRIDSTYLE; //coordinates left and bottom
    	setColorMap(map, 2);
        doInit(init); // do the initialization 
     }
    public QyshinsuBoard cloneBoard() 
	{ QyshinsuBoard dup = new QyshinsuBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}   
    public void copyFrom(BoardProtocol b) { copyFrom((QyshinsuBoard)b); }
    // a temporary stack resource for various move generation and checking functions
    private QyshinsuCell[][]tempDestResource = new QyshinsuCell[4][];
    private int tempDestIndex=-1;
    private synchronized QyshinsuCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new QyshinsuCell[MAXDESTS]);
    	}
    private synchronized void returnTempDest(QyshinsuCell[]d) { tempDestResource[++tempDestIndex]=d; }
 

    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	if(Qyshinsu_INIT.equalsIgnoreCase(game)) 
    		{ boardColumns=DEFAULT_COLUMNS; 
    		  boardRows = DEFAULT_ROWS;
    		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(QyshinsuState.PUZZLE_STATE);
        initBoard(boardColumns,boardRows); //this sets up the board and cross links
        
         
        whoseTurn = FIRST_PLAYER_INDEX;
        for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX; i++)
        {
        vlastMove[i] = new CellStack();
        vlastChip[i] = new ChipStack();
        }
        pickedSource = null;
        droppedDest = null;
        pickedObject = null;

    }

    public void sameboard(BoardProtocol f) { sameboard((QyshinsuBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(QyshinsuBoard from_b)
    {	super.sameboard(from_b);
        
    	G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
       	G.Assert(sameCells(vlastMove,from_b.vlastMove),"vlastMove mismatch");
       	G.Assert(sameContents(vlastChip,from_b.vlastChip),"vlastMove mismatch");
      	G.Assert(pickedObject==from_b.pickedObject,"pickedObject matches");
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
 
        for(int i=0;i<vlastMove.length;i++)
		{	v ^= cell.Digest(r,getLastMove(i));
			v ^= chip.Digest(r,getLastChip(i));
		}
		v ^= chip.Digest(r,pickedObject);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);

        return (v);
    }
   


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(QyshinsuBoard from_b)
    {	super.copyFrom(from_b);
    	pickedObject = from_b.pickedObject;
 		for(int i=0;i<2;i++) 
		{  
		   vlastChip[i].clear();
		   vlastMove[i].clear();
		   for(int j=0,sz=from_b.vlastChip[i].size();
		   		j<sz;
		   		j++)
		   {	vlastChip[i].push(from_b.vlastChip[i].elementAt(j));
		   }
		   for(int j=0,sz=from_b.vlastMove[i].size();
	   		j<sz;
	   		j++)
		   {	QyshinsuCell cc = from_b.vlastMove[i].elementAt(j);
		   		QyshinsuCell loc = getCell(cc);
		   		vlastMove[i].push( loc);
		   }
		}
 		copyFrom(rack,from_b.rack);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        pickedSource = getCell(from_b.pickedSource);
        sameboard(from_b);
    }
 
    public QyshinsuChip playerChip[] = new QyshinsuChip[2];
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;
    	Random r = new Random(837587);
    	int map[]=getColorMap();
    	for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX; pl++)
    	{
    	for(int i=0;i<QyshinsuChip.N_STANDARD_CHIPS; i++,pl=nextPlayer[pl])
    	{
    	int n = ((pl==FIRST_PLAYER_INDEX) ? QyshinsuChip.BLACK_CHIP_INDEX :QyshinsuChip.WHITE_CHIP_INDEX) +i;
    	QyshinsuChip chip =QyshinsuChip.getChip(n);
    	QyshinsuCell cell = new QyshinsuCell(r,'@',i);
    	cell.rackLocation = poolLocation[pl];
    	cell.onBoard=false;
    	rack[map[pl]][i]=cell;
    	playerQids[map[pl]]=poolLocation[pl];
    	cell.addChip(chip);
    	cell.addChip(chip);
    	}
    	}
    	playerChip[0] = rack[0][QyshinsuChip.N_STANDARD_CHIPS-1].topChip();
    	playerChip[1] = rack[1][QyshinsuChip.N_STANDARD_CHIPS-1].topChip();
        Init_Standard(gtype);
        allCells.setDigestChain(r);
        animationStack.clear();
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

    public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
    {	
    	GC.Text(gc, false, xpos+cellsize/4, ypos-cellsize, -1, 0,clt, null, txt);
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


    private void setGameOver(boolean winCurrent,boolean winNext)
    {	
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(QyshinsuState.GAMEOVER_STATE);
    }


    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public double ScoreForPlayer(int player,boolean print,boolean dumbot)
    {  	double finalv=0.0;
    	for(QyshinsuCell c=allCells;
    		c!=null;
    		c = c.next)
    	{	QyshinsuChip ch = c.topChip();
    		if((ch!=null) && (playerIndex(ch)==player))
    		{	finalv += 10.0;			// 10 points for each stone
    		}
    	}
    	
    	return(finalv);
    }
    private int playerIndex(QyshinsuChip ch) { return(getColorMap()[ch.colorIndex()]);}
    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement(replayMode replay)
    {	QyshinsuCell c = ((droppedDest!=null) && droppedDest.onBoard && (droppedDest.col=='A')) 
    						? droppedDest
    						: ((pickedSource!=null) && pickedSource.onBoard && (pickedSource.col=='A')) 
    							? pickedSource
    							: null;
    	if(whoseTurn>=0)
    	{
    	if(board_state!=QyshinsuState.PUZZLE_STATE) 
    		{ 
    		if(replay.animate)
    		{
    			QyshinsuCell dest = vlastMove[nextPlayer[whoseTurn]].top();
    			QyshinsuChip top = vlastChip[nextPlayer[whoseTurn]].top();
    			if((dest!=null)&&(top!=null)&&(dest.col=='A')&&(dest.topChip()==null))
    			{	QyshinsuCell home = rack[playerIndex(top)][top.typeIndex];
    				animationStack.push(dest);
    				animationStack.push(home);
    			}
    			
    		}
    		addLast(whoseTurn,c,(droppedDest!=null)?droppedDest.topChip():null); 
    		}
 	    if(droppedDest!=null) 
	    	{ 
	    	  if(droppedDest.col=='B') 
	    	  {	droppedDest.removeTop();
	    	  	QyshinsuCell dest = rack[playerIndex(pickedObject)][pickedObject.typeIndex];
	    	  	dest.addChip(pickedObject);
	    	  }
	    	}
    	}
        pickedObject = null;
        droppedDest = null;
        pickedSource = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    QyshinsuCell dr = droppedDest;
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
    {	QyshinsuChip po = pickedObject;
    	if(po!=null)
    	{
    	QyshinsuCell ps = pickedSource;
    	pickedSource=null;
    	pickedObject = null;
    	ps.addChip(po);
    	}
     }
    // 
    // drop the floating object.
    //
    private QyshinsuCell dropObject(QIds dest, char col, int row)
    {
       G.Assert((pickedObject!=null)&&(droppedDest==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case BoardLocation: // an already filled board slot.
        	droppedDest = getCell(col,row);
        	break;
        case First_Player_Pool:		// back in the pool
           	droppedDest = rack[getColorMap()[FIRST_PLAYER_INDEX]][row];
           	break;
        case Second_Player_Pool:		// back in the pool
           	droppedDest = rack[getColorMap()[SECOND_PLAYER_INDEX]][row];
            break;
        }
   	droppedDest.addChip(pickedObject);    
   	return(droppedDest);
 }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(QyshinsuCell cell)
    {	return(droppedDest==cell);
    }
    public boolean isSource(QyshinsuCell cell)
    {
    	return(pickedSource==cell);
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	QyshinsuChip ch = pickedObject;
    	if((ch!=null)&&(droppedDest==null))
    		{ return(ch.pieceNumber());
    		}
        return (NothingMoving);
    }
    public QyshinsuCell getCell(QIds source,char col,int row)
    {
    	switch(source)
    	{
    	default: throw G.Error("source %s not expected",source);
    	case BoardLocation:
    		return  getCell(col,row);
    	case Second_Player_Pool:
    		return rack[getColorMap()[SECOND_PLAYER_INDEX]][row];
    	case First_Player_Pool:
    		return rack[getColorMap()[FIRST_PLAYER_INDEX]][row];
    	}
    }
 
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private QyshinsuCell pickObject(QIds source, char col, int row)
    {	G.Assert((pickedObject==null)&&(pickedSource==null),"ready to pick");
    	QyshinsuCell c = pickedSource = getCell(source,col,row);
    	pickedObject = c.removeTop();
    	return c;
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	if(droppedDest!=null) { setNextStateAfterDone(); }
        	break;
        case PLAY_STATE:
			setState(QyshinsuState.CONFIRM_STATE);
			break;

        case PUZZLE_STATE:
			acceptPlacement(replay);
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
        	setState(QyshinsuState.PLAY_STATE);
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
    	case PUZZLE_STATE:
    	case CONFIRM_STATE:
    		setState(QyshinsuState.PLAY_STATE);
			//$FALL-THROUGH$
		case PLAY_STATE:
    		{
    			int nm = nLegalMoves();
    			if(nm==0) 
    				{ setGameOver(false,true); 
    				}
  			
    		}
    		break;
    	}

    }
   

    
    private void doDone(replayMode replay)
    {	
        acceptPlacement(replay);

        if (board_state==QyshinsuState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	setNextPlayer();
        	setNextStateAfterDone(); 	// checks for endgame
        }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	QyshinsuMovespec m = (QyshinsuMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        //G.print("e "+mm);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(replay);
            break;
        case MOVE_REMOVE:
        	switch(board_state)
        	{
        	default: throw G.Error("Not expecting robot remove in state %s",board_state);
        	case PLAY_STATE:
    			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
    			QyshinsuCell src = pickObject(QIds.BoardLocation,m.from_col,m.from_row);
                m.object = pickedObject;
    			QyshinsuCell dest = dropObject(QIds.BoardLocation,'B',m.from_row);
    			if(replay.animate) 
    			{
    				animationStack.push(src);
    				animationStack.push(dest);
    			}
    			setNextStateAfterDrop(replay);
        	}
        	break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot add in state %s",board_state);
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
                    QyshinsuCell src = pickObject(playerQids[whoseTurn], m.from_col, m.from_row);
                    m.object = pickedObject;
                    QyshinsuCell dest = dropObject(QIds.BoardLocation,m.to_col,m.to_row);
        			if(replay.animate) 
        			{
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
                    setNextStateAfterDrop(replayMode.Replay);
                    break;
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

				  break;
			}
			m.from_col = '@';
			m.from_row = pickedObject.typeIndex;	// save for viewer
            dropObject(QIds.BoardLocation, m.to_col, m.to_row);
            if(pickedSource==droppedDest) 
            	{ unDropObject(); 
            	  unPickObject(); 

            	} 
            	else
            		{
            		if(replay==replayMode.Single)
            		{
            			animationStack.push(pickedSource);
            			animationStack.push(droppedDest);
            		}
            		setNextStateAfterDrop(replay);
            		}

            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(QyshinsuState.PLAY_STATE);
        		}
        	else 
        		{ pickObject(QIds.BoardLocation, m.from_col, m.from_row);
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
                m.object = pickedObject;
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
            dropObject(m.source, m.to_col, m.to_row);
            setNextStateAfterDrop(replay);

            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickObject(m.source, m.from_col, m.from_row);
            m.object = pickedObject;
            setNextStateAfterPick();
            break;


        case MOVE_START:
            if(droppedDest!=null) { acceptPlacement(replay); }
    		unPickObject();
            //acceptPlacement();
            //unPickObject();
            setWhoseTurn(m.player);
            setState(QyshinsuState.PUZZLE_STATE);
            setNextStateAfterDone(); 	// checks for endgame
            break;

        case MOVE_RESIGN:
            setState(unresign==null?QyshinsuState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
            if(droppedDest!=null) { acceptPlacement(replay); }
      		unPickObject();
   		  	setState(QyshinsuState.PUZZLE_STATE);

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
         case CONFIRM_STATE:
         case DRAW_STATE:
         case PLAY_STATE: 
        	return((pickedObject==null)
        			?(player==whoseTurn)
        			:((droppedDest==null) 
        					&& (player==playerIndex(pickedObject))));

        case RESIGN_STATE:
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }

    // get a hash of moves destinations or sources, used by the viewer
    // to know where to draw dots
    public Hashtable<QyshinsuCell, QyshinsuCell> getMoveCells(boolean getsources)
    {	Hashtable<QyshinsuCell, QyshinsuCell> dd = new Hashtable<QyshinsuCell, QyshinsuCell>();
    	if(board_state!=QyshinsuState.PUZZLE_STATE)
    	{
       	if((pickedSource!=null) && (pickedSource.onBoard))
    	{	char nextcol = (pickedSource.col=='A') ? 'B' : 'A';
    		QyshinsuCell alt = getCell(nextcol,pickedSource.row);
    		dd.put(alt,alt);
    		dd.put(pickedSource,pickedSource);
    	}
       	else
       	{
       	QyshinsuCell temp[] = getTempDest();
       	int n = legalDests(whoseTurn,temp,0,getsources,droppedDest);
       	if(n<11)
       	{
    	for(int i=0;i<n;i++)
    	{	QyshinsuCell c = temp[i];
    		if((pickedSource==null)
				||((c!=getLastMove(whoseTurn))
					||(pickedObject!=getLastChip(whoseTurn))))
    		dd.put(c,c);
    	}}
    	returnTempDest(temp);
    	}
    	}
      	return(dd);
    } 
    //
    // this is the guts of the legal move generator/checker.
    //
    int legalDests(int who,		// the player
    			QyshinsuCell tempDests[],	// gets the list of dest cells, or can be NULL
    			int idx,					// initial index of the temp cell stack
    			boolean bsource,			// if true, look got stone that can be picked up
    										// otherwise, for cells where stones can be dropped
    			QyshinsuCell ignore)		// if not null, ignore cells in this board position
    										// this is used to keep temporary chips from confusing the old stone
    {	int next = nextPlayer[who];
       	QyshinsuCell comp = getLastMove(next);
        if(comp==null)
        {
        // add or remove anywhere, mainly on the first move of the game
        for(int i=1;i<=boardRows;i++)
        {
	        QyshinsuCell c = getCell('A',i);
	        QyshinsuChip top = (c==ignore) ? null : c.topChip();
	        if(bsource
	        	? ((top!=null)&&(playerIndex(top)==who)) 
	        	: (top==null)) 
	        			
	        	{
	        	if(tempDests!=null) { tempDests[idx]=c; }
	        	idx++;
	        	}
        }
        }
        else {
        int distance = getLastChip(next).typeIndex;
        QyshinsuChip top = comp.topChip();
        if (distance==0)	// old stone
        {
        if (top==null)
        	{	// remove the old stone
        	int old_idx = idx;
        	if(bsource)
        		{QyshinsuCell left = null;
        		int left_distance = 99;
        		for(int i=1;i<11;i++)
        		{  int row = comp.row+i;
        		   QyshinsuCell c1 = getCell(comp.col,row);
        		   QyshinsuChip ch = (c1==ignore) ? null : c1.topChip();
        		   if((ch!=null) && (playerIndex(ch)==who)) 
        		   {  if(tempDests!=null) { tempDests[idx]=c1; }
	      			  idx++;
	      			  left = c1;
	      			  left_distance = i;
	      			  break;
        		   }
        		}
        		for(int i=1;i<11;i++)
        		{  int row = comp.row-i;
        		   QyshinsuCell c1 = getCell(comp.col,row);
        		   if(c1!=left)
        		   {
        		   QyshinsuChip ch = (c1==ignore) ? null : c1.topChip();
        		   if((ch!=null) && (playerIndex(ch)==who)) 
        		   {  if(i<left_distance) { idx=old_idx; }	// shorter distance, forget the first
        		   	  if(i<=left_distance)
        		   		  { if(tempDests!=null) { tempDests[idx]=c1; }
        		   		  idx++;
        		   		  }
        		   	  break;
        		   	}
        		   }
        		}}
        	}
        else
        	{	// add the old stone
    		if(!bsource)
    	    {
    		QyshinsuCell left = null;
    		int left_distance = 0;
    		for(int i=1;i<12;i++)
    		{  int row = comp.row+i;
    		   QyshinsuCell c1 = getCell(comp.col,row);
    		   QyshinsuChip ch = (c1==ignore) ? null : c1.topChip();
    		   if(ch==null) 
    		   {  if(tempDests!=null) { tempDests[idx]=c1; }
    		   	  left = c1;
    		   	  left_distance = i;
      			  idx++;
      			  break;
    		   }
    		}
    		for(int i=1;i<11;i++)
    		{  int row = comp.row-i;
    		   QyshinsuCell c1 = getCell(comp.col,row);
    		   if(c1!=left)
    		   {
    		   QyshinsuChip ch = (c1==ignore) ? null : c1.topChip();
    		   if(ch==null) 
    		   {  if(i<left_distance) { idx--; }	// shorter distance, ignore the first
    		   	  if(i<=left_distance) 
    		   		  {if(tempDests!=null) { tempDests[idx]=c1; }
    		   		  idx++;
    		   		  }
    		   		  break;
    		   		  }}
    		}}
       	}
        }
        else
       	{	// add or remove a numbered stone
        	for(int i=-distance; i<=distance; i+=2*distance)
       		{
       		int row = comp.row+i;
       		QyshinsuCell c1 = getCell(comp.col,row);
       		QyshinsuChip c1top = (c1==ignore) ? null : c1.topChip();
       		if(bsource 
       				? ((c1top!=null) && (playerIndex(c1top)==who))
       				: (c1top==null))
	      			{ if(tempDests!=null) { tempDests[idx]=c1; }
	      			  idx++;
	       			}
       		}
        }}
       	return(idx);
    }
    boolean legalAddMove(int who,QyshinsuCell c)
    {	QyshinsuCell temp[] = getTempDest();
    	boolean val=false;
    	int n = legalDests(who,temp,0,false,droppedDest);
    	for(int i=0;i<n;i++) { if(temp[i]==c) { val=true; break; }}
    	returnTempDest(temp);
    	return(val);
    }
    boolean legalPickMove(int who,QyshinsuCell c)
    {	QyshinsuCell temp[] = getTempDest();
		boolean val=false;
		int n = legalDests(who,temp,0,true,droppedDest);
		for(int i=0;i<n;i++) { if(temp[i]==c) { val=true; break; }}
		returnTempDest(temp);
		return(val);

    }
    public boolean LegalToHitBoard(QyshinsuCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
 			if(pickedObject!=null)
 			{	
 				if(pickedSource.onBoard)
 						{ QyshinsuCell alt = getCell('B',pickedSource.row);
 						  if(cell==alt) { return(true); }
 						}
 						else
 						{// picked a board cell, drop on the edge except for Ko rule
 							QyshinsuChip lastchip = getLastChip(whoseTurn);
 							QyshinsuCell lastcell = getLastMove(whoseTurn);
 							if((lastcell !=cell ) || (lastchip != pickedObject))
 							{	return(legalAddMove(whoseTurn,cell));
 							}
 						}
 				return(false);
         	}
 			return(legalPickMove(whoseTurn,cell));

		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return(pickedObject==null?(cell.chipIndex>=0):(cell.chipIndex==-1));
        }
    }
  public boolean canDropOn(QyshinsuCell cell)
  {	
  	return((pickedObject!=null)				// something moving
  			&&(pickedSource.onBoard 
  					? (cell.row==pickedObject.typeIndex)	// dropping on the board, must be to a different cell 
  					: (cell==pickedSource))	// dropping in the rack, must be to the same cell
  				);
  }
  // number of chips of a certain type on the board, which has
  // to be related to the number in the rack
  public int chipsOfTypeOnBoard(int type)
  {	int fp = rack[FIRST_PLAYER_INDEX][type].chipIndex +1 ;
  	int sp = rack[SECOND_PLAYER_INDEX][type].chipIndex +1 ;
  	return(4-fp-sp);
  }
  public boolean canPickKoFromChip(QyshinsuChip ch)
  {	QyshinsuChip c = getLastChip(whoseTurn);
  	if(ch==c)
  	{	// potentially a Ko situation, placing the same chip again
  		QyshinsuCell temp[] = getTempDest();
  		QyshinsuCell last = getLastMove(whoseTurn);
  		int n = legalDests(whoseTurn,temp,0,false,null);
  		boolean val = false;
  		while(--n >=0)
  		{	if(last!=temp[n]) { val = true; break; }
  		}
   		returnTempDest(temp);
   		return(val);
  	}
	return(true);
  }
  public boolean canPickFrom(QyshinsuCell cell)
  {	
  	return((pickedObject==null)					// nothing moving
  			&&(chipsOfTypeOnBoard(cell.row)<2)	// not two already on board
  			&& canPickKoFromChip(cell.topChip())
 			);
  }
  private StateStack stateStack = new StateStack();
  
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(QyshinsuMovespec m)
    {
    	stateStack.push(board_state);
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

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
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(QyshinsuMovespec m)
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
        			pickObject(QIds.BoardLocation,m.to_col,m.to_row);
        			dropObject(playerQids[playerIndex(pickedObject)],'@', m.from_row);
       			    acceptPlacement(replayMode.Replay);
       			    removeLast(whoseTurn);
       			    removeLast(m.player);
                    break;
        	}
        	break;
        case MOVE_REMOVE:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
       			    pickObject(poolLocation[m.player],'@',m.to_row); 
        			dropObject(QIds.BoardLocation, m.from_col, m.from_row);
      			    acceptPlacement(replayMode.Replay);
     			    removeLast(whoseTurn);
       			    removeLast(m.player);
       			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(stateStack.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 int getListOfMovesFor(int who,CommonMoveStack  all)
 {	int nmoves = 0;
 	
 	switch(board_state)
 	{
 	default: throw G.Error("Not expecting state %s",board_state);
 	case PLAY_STATE:
	{	QyshinsuCell temp[] = getTempDest();
 		int n = legalDests(who,temp,0,true,null);
	 	while(--n>=0)
	 		{	QyshinsuCell c = temp[n];
	 			QyshinsuChip chip = c.topChip();
	 			if(all!=null) 
	 			{ all.addElement(new QyshinsuMovespec(MOVE_REMOVE,who,playerQids[who],c.col,c.row,'@',chip.typeIndex));
	 			}
	 			nmoves++;
	 		}
	 		// now do drop moves
	 	{	QyshinsuCell sources[] = rack[who];
	 		QyshinsuCell last = getLastMove(who);
	 		QyshinsuChip lastChip = getLastChip(who);
	 		n = legalDests(who,temp,0,false,null);
	 		if(n==12) { n = 1; }
	 		for(int i=0;i<sources.length;i++)
	 		{	QyshinsuCell c = sources[i];
	 			QyshinsuChip p = c.topChip();
	 			if((p!=null) && canPickFrom(c))
	 				{ 
	 				if(all!=null)
	 				{
	 				for(int j=0;j<n; j++)
	 				{	QyshinsuCell d = temp[j];
	 					if((p!=lastChip)||(d!=last))
	 					{
	 					all.addElement(new QyshinsuMovespec(MOVE_RACK_BOARD,who,playerQids[who],
	 							'@',p.typeIndex,d.col,d.row));
	 					}
	 				}}
	 				nmoves += n;
	 				}
	 		}
	 		
	 		returnTempDest(temp); 
	 	}
	}}
	 return(nmoves);
 }
 public int nLegalMoves()
 {
	return(getListOfMovesFor(whoseTurn,null)); 
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	getListOfMovesFor(whoseTurn,all);

  	return(all);
 }
 
}