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
package trax;


import java.util.Vector;

import online.game.*;
import lib.*;

/**
 *  Initial release sep 2005
 */

class TraxGameBoard  extends BaseBoard implements BoardProtocol,TraxConstants
{
	private TraxState unresign;
	private TraxState board_state;
	public TraxState getState() {return(board_state); }
	public void setState(TraxState st) 
	{ 	unresign = (st==TraxState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	boolean verify = false;
	int tilesOnBoard = 0;
	boolean blitz = false;
	boolean robotBoard = false;
	int left = 0;
	int top = 0;
	int right = 0;
	int bottom = 0;
	final int ncols=52;
	final int nrows=50;
	char realboard[][]=new char[nrows][ncols];
	int scoreboard[][]=new int[nrows][ncols];
	int sweep=0;
	boolean scorevalid[]={false,false};
	@SuppressWarnings("unchecked")
	Vector<lineinfo> lines[] = new Vector[2];
	

			
    public void SetDrawState() { }	// this is required even though it is meaningless for trax 
	// the peculiar coordinate system for trax, the left,top tile is A1, which shifts as
	// new tiles are added in col "@" or row 0
	final int colToX(char col)
	{	return(left+(col-'A'));
	}
	final int rowToY(int row)
	{	return(row+top-1);
	}
	final char XtoCol(int x)
	{	return((char)(x-left+'A'));
	}
	final int YtoRow(int y)
	{	return(y-top+1);
	}
	
     //
    // private variables
    //
	public int playerChar[] = new int[2];
	public int getPlayerColor(int p) { return(playerChar[p]); }
	

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    char pickedObject = (char) 0;
	private int lastPickedIndex=-1;		// index for last picked obect
    private TraxId pickedSource = TraxId.NoWhere;
    private TraxId droppedDest = TraxId.NoWhere;
    private int picked_x;
    private int picked_y;
    private int dropped_x;
    private int dropped_y;
    
    // this is used in puzzle state to keep placing more chips of the same
    // color as the last chip placed.
	public int lastPicked() { return(lastPickedIndex); }
	char getBoardXY(int x,int y)
	{	return(realboard[y][x]);
	}
	char getBoardXYe(int x,int y)
	{	if((x<0) || (y<0) || (x>=ncols) || (y>=nrows)) { return(Empty); }
		return(getBoardXY(x,y));
	}

	private boolean nonemptyIsAdjacent(char col,int row)
	{	int x = colToX(col);
		int y = rowToY(row);
		return(nonEmptyIsAdjacentXY(x,y));
	}
	private boolean nonEmptyIsAdjacentXY(int x,int y)
	{
		return((getBoardXYe(x-1,y)!=Empty)
			|| (getBoardXYe(x+1,y)!=Empty)
			|| (getBoardXYe(x,y+1)!=Empty)
			|| (getBoardXYe(x,y-1)!=Empty));
	}
	private boolean emptyIsAdjacent(char col,int row)
	{	int x = colToX(col);
		int y = rowToY(row);
		return((getBoardXYe(x-1,y)==Empty)
			|| (getBoardXYe(x+1,y)==Empty)
			|| (getBoardXYe(x,y+1)==Empty)
			|| (getBoardXYe(x,y-1)==Empty));
	}
	private void setBoardXY(int x,int y,char ch)
	{	realboard[y][x]=ch;
		scorevalid[0]=scorevalid[1]=false;
	}
	private boolean sideMatches(char match,char[] colorset,char color)
	{	
		if(match==Empty) { return(true); } 	// empty side ok
		char matchcolor = colorset[match-'0'];
		return(matchcolor==color);
	}
	
	// return true if this placement will match colors appropritely, and at least one
	// side is adjacent to a nonempty space.
	private boolean colorsMatchxy(int x,int y,char tile)
	{	if(tile==Empty) { return(true); }
		int tileIndex = tile-'0';
		boolean ok = true;
		{ char tile1 = getBoardXYe(x-1,y);
		  if(tile1!=Empty) { ok &= sideMatches(tile1,EastColors,WestColors[tileIndex]); }
		}
		{ char tile1 = getBoardXYe(x,y-1);
		  if(tile1!=Empty) {  ok &= sideMatches(tile1,SouthColors,NorthColors[tileIndex]); }
		}
		{ char tile1 = getBoardXYe(x+1,y);
		  if(tile1!=Empty) { ok &= sideMatches(tile1,WestColors,EastColors[tileIndex]); }
		}
		{ char tile1 = getBoardXYe(x,y+1);
		  if(tile1!=Empty) {  ok &= sideMatches(tile1,NorthColors,SouthColors[tileIndex]); }
		}
		return(ok);
}
	// 
	// return the next tile which is ok at this xy position
	//
	private char nextColorMatchxy(int x,int y,char tile)
	{	char nextcolor = tile;
		for(int i=0;i<6;i++)
		{ 
		  if(colorsMatchxy(x,y,nextcolor)) { return(nextcolor); }
		  nextcolor = (char)(nextcolor+1);
		  if(nextcolor>='6') { nextcolor = '0'; }
		}
		return(Empty);
	}
	private final int nColorMatchXY(int x,int y)
	{	char nextcolor = '0';
		int nmatch = 0;
		for(int i=0;i<6;i++)
		{ 
		  if(colorsMatchxy(x,y,nextcolor))
		  	{ nmatch++;  
		  	  //System.out.println("Match for "+nextcolor+" at "+x+","+y);
		  	}
		  nextcolor = (char)(nextcolor+1);
		  
		}
		return(nmatch);
	}

	public final int nColorMatch(char col,int row)
	{	return(nColorMatchXY(colToX(col),rowToY(row)));
	}	//
	// return the next chip starting at "tile" that is acceptable in this position
	//
	public char nextColorMatch(char col,int row,char tile)
	{	int x = colToX(col);
		int y = rowToY(row);
		// for testing - nColorMatchxy(x,y,chip);
		if(tilesOnBoard==0)
		{ switch(tile)
			{
			default: throw G.Error("oops");
			case '5': tile='0'; break;
			case '1': case '2': case '3': tile='4'; break;
			case '0': case '4': break;
			}
			return(tile);
		}
		else if((tilesOnBoard==1)&&(GetBoardPos(col,row)!=Empty)) 
		{ //special case for replacing the first tile
			switch(tile)
			{ default: throw G.Error("oops");
			  case '0': case '1': tile='4'; break;
			  case '2': case '3': case '4': case '5': tile='0';
			}
			return(tile);
		}
		return(nextColorMatchxy(x,y,tile));
	}
    public TraxGameBoard(String init,int map[]) // default constructor
    {	setColorMap(map, 2);
         doInit(init); // do the initialization 
    }

    public TraxGameBoard cloneBoard() 
	{ TraxGameBoard dup = new TraxGameBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TraxGameBoard)b); }

    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard()
    {
        gametype = Trax_INIT;
        setState(TraxState.PUZZLE_STATE);
        whoseTurn = FIRST_PLAYER_INDEX;
        tilesOnBoard = 0;
        AR.copy(playerChar,getColorMap());
		lines[0] = new Vector<lineinfo>();
		lines[1] = new Vector<lineinfo>();
		for(int row=0; row<nrows;row++)
			{ for(int col=0;col<ncols;col++) { setBoardXY(col,row,Empty); }
			}
		
        // set the initial contents of the board to all empty cells
     }

    public void sameboard(BoardProtocol f) { sameboard((TraxGameBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TraxGameBoard from_b)
    {	super.sameboard(from_b);
    	G.Assert(AR.sameArrayContents(playerChar,from_b.playerChar),"Player chars mismatch");
        G.Assert((tilesOnBoard == from_b.tilesOnBoard), "tilesOnBoard matches");
     }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
     * @return
     */
    public long Digest()
    {
       Random r = new Random(64 * 1000); // init the random number generator
       long v = 0;
        for(int x=left; x<right;x++)
        { for(int y=top; y<bottom; y++)
        	{
        	int ch = getBoardXY(x,y)-'0';
         	for(int i=0;i<6;i++) 
        		{long v1 = r.nextLong(); if(i==ch) { v^=v1; }
        		}
           	//if(ch>=0) { G.print("x "+x+","+y+" "+ch+" "+v);}
       	}
        }
        return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TraxGameBoard from_b)
    {	super.copyFrom(from_b);
    	tilesOnBoard = from_b.tilesOnBoard;
    	board_state = from_b.board_state;
    	unresign = from_b.unresign;
        left=from_b.left;
        top = from_b.top;
        right = from_b.right;
        bottom = from_b.bottom;
        blitz = from_b.blitz;
        dropped_x = from_b.dropped_x;
        dropped_y = from_b.dropped_y;
        droppedDest = from_b.droppedDest;
        fillPath.clear();
        for(int x = left; x<right; x++) 
	       { for (int y=top; y<bottom; y++) 
	        { setBoardXY(x,y,from_b.getBoardXY(x,y)); 
	        }
	       }
        
        AR.copy(playerChar,from_b.playerChar);
        AR.copy(scorevalid,from_b.scorevalid);

        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;
        if (Trax_INIT.equalsIgnoreCase(gtype))
        {
            Init_Standard();
        }
        else
        {
        	throw G.Error(WrongInitError, gtype);
        }
        droppedDest= TraxId.NoWhere;
        pickedSource=TraxId.NoWhere;
        dropped_x = 0;
        dropped_y = 0;
        moveNumber = 1;
        tilesOnBoard=0;
        fillPath.clear();
        blitz = false;

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



 /**
  * In our implementation, the letter side(a-k) is black
  * and the number side (1-11) is white.  Either player can be playing either color.
  * @param ind
  * @return
  */ 
    public boolean WinForPlayerNow(int pl)
    {
    	if(pl==whoseTurn) 
    	{ return(WinConditionForPlayer(pl)); 
    	}
    	//otherwise
    	{ return(WinConditionForPlayer(pl)
    			 &&!WinConditionForPlayer(nextPlayer[pl]));
    	}
    }
	public boolean WinConditionForPlayer(int player)
	{	if(win[player]) { return(true); }
		Vector<lineinfo> plines = lines[player];
		scorevalid[player]=false;
		plines.removeAllElements();
		sweep++;
		boolean hasloop = false;
		int maxline = -1;
		int color = getPlayerColor(player);
		//lineinfo bestline = null;
		for(int x=left; x<right;x++)
		{ for(int y=top;y<bottom;y++)
		{	if((realboard[y][x]!=Empty) && (scoreboard[y][x]!=sweep))
			{ lineinfo l = new lineinfo(this,color,x,y);
			  int len = l.currentSpan();
			  hasloop |= l.loop;
			  if(len>maxline)
			  	{
				  maxline = len;
				  //bestline = l;
			  	}
			  plines.addElement(l);
			  //System.out.println(l);
			}
		}
		}
		if(verify)
		{	
			for(int x=left; x<right;x++)
			{ for(int y=top;y<bottom;y++)
			{	lineinfo hitline = null;
			    char cc = realboard[y][x];
				if((cc!=Empty))
				{ for(int i=0;i<plines.size();i++)
					{ lineinfo elem = plines.elementAt(i);
					  if(elem.containsXY(x,y)) { G.Assert(hitline==null,"already has a home"); hitline=elem; }
					}
				G.Assert(hitline!=null,"Has no home "); 
				}
			}
			}
			
		}

		scorevalid[player]=true;
		//System.out.println("Score Player "+player+" "+maxline+" "+hasloop+ " "+bestline);
		return(hasloop || (maxline>=10));
	}
	boolean onWinningLine(int player,char col,int row)
	{	if(WinForPlayerNow(player))
		{	Vector<lineinfo> plines = lines[player];
			int tx = colToX(col);
			int ty = rowToY(row);
			for(int i=0;i<plines.size(); i++)
			{	lineinfo pline=plines.elementAt(i);
				if(pline.loop || (pline.currentSpan()>=10))
				{	if(pline.containsXY(tx,ty)) { return(true); }
				}
			}
		}
		return(false);
	}
  int winner()
  {	return(WinForPlayerNow(FIRST_PLAYER_INDEX) 
   	                    ? FIRST_PLAYER_INDEX
   	                    : WinForPlayerNow(SECOND_PLAYER_INDEX) 
   	                         ? SECOND_PLAYER_INDEX
   	                         : -1);
  }
  boolean onWinningLine(char col,int row)
  {	return(onWinningLine(FIRST_PLAYER_INDEX,col,row) 
		  || onWinningLine(SECOND_PLAYER_INDEX,col,row));
  }

    /* return true if the game is over 
     * */
    public boolean GameOver()
    {
        return (board_state==TraxState.GAMEOVER_STATE);
    }
    
    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {
        pickedObject = (char) 0;
        droppedDest = TraxId.NoWhere;
        pickedSource = TraxId.NoWhere;
     }
    public int SetBoard(char col, int row, char c)
    { 	int x = 0;
    	int y = 0;
    	if(tilesOnBoard==0)
    	{	if((col=='@') && (row==0))
    		{	left = ncols/2; right=left+1;
    			top = nrows/2; bottom=top+1;
    			x = left;
    			y = top;
                dropped_x = x;
                dropped_y = y;
     			setBoardXY(x,y,Empty);
    		}
    		else 
    		{ throw G.Error("First move must be @0"); 
    		}
    	}
    	else
    	{	x = colToX(col);
    		y = rowToY(row);
    		
    	}
    	if((x>=0)&&(y>=0)&&(x<ncols)&&(y<nrows))
    	{	G.Assert(colorsMatchxy(x,y,c),"colors match");
    		{
    		char oldc = getBoardXY(x,y);
            dropped_x = x;
            dropped_y = y;
            setBoardXY(x,y,c);
    		left = Math.min(left,x);
    		right= Math.max(right,x+1);
    		top = Math.min(top,y);
    		bottom = Math.max(bottom,y+1);
    		//System.out.println("Set "+x+","+y+" "+"box "+left+"-"+right+" "+top+"-"+bottom);
    		if(c!=Empty) 
    			{ if(oldc==Empty) 
    				{ tilesOnBoard++; } }
    		else { if(oldc!=Empty) 
    				{ tilesOnBoard--; }
    				//maintain all four edges of the bounding box, which are needed for the scoring
    				if(col=='A') 
    				{	// maintain the correct left bound
    					boolean ok=false;
    					for(int i=top;i<bottom;i++)
    					{ if(realboard[i][left]!=Empty) { ok=true; break; }
    					}
  					  if(!ok) { left++; }
    				}
    				if((x+1)==right)
    				{	// maintin the correct right bounding box
    					boolean ok= false;
    					for(int i=top;i<bottom;i++)
    					{ if(realboard[i][x]!=Empty) { ok=true; break; }
    					}
    					if(!ok) { right--; }
    				}
    				if(row==1)
    				{	// maintain the correct top bound
    					boolean ok=false;
    					for(int j=left;j<right;j++)
    					{ if(realboard[top][j]!=Empty) { ok=true; break; }
 
    					}
   					  if(!ok) { top++; }
    				}
    				if((y+1)==bottom)
    				{	// maintain the correct bottom bound
    					boolean ok=false;
    					for(int j=left;j<right; j++)
    					{ if(realboard[y][j]!=Empty) { ok = true; break; }
    					}
    					if(!ok) { bottom--; }
    				}
    				if(tilesOnBoard==0) { left = top = 0; }
     			}
   		}
 
    	}
    	return(x<<16|y);
    }
    public char GetBoardPos(char col,int row)
    { return(getBoardXYe(colToX(col),rowToY(row)));
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {	
        switch (droppedDest)
        {
        default:
            break;

        case EmptyBoard:
            SetBoard(XtoCol(dropped_x),YtoRow(dropped_y), Empty);
    	}
        droppedDest = TraxId.NoWhere;
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {
        switch (pickedSource)
        {
        case BoardLocation:
        	if(tilesOnBoard==0) { SetBoard('@',0,pickedObject); }
        	else { SetBoard(XtoCol(picked_x), YtoRow(picked_y), pickedObject);}
            break;
		default:
			break;
        }
		pickedObject = (char)0;
        pickedSource = TraxId.NoWhere;
    }
    // 
    // drop the floating object.
    //
    private void dropObject(TraxId dest, char col, int row)
    {
        switch (dest)
        {
        case BoardLocation: // an already filled board slot.
        default:
            pickedSource = TraxId.NoWhere;
            throw G.Error("not expecting dest %s", dest);
        case hitTile0:
        case hitTile1:		// back in the pool, we don't really care where
        case hitTile2:
        case hitTile3:		// back in the pool, we don't really care where
        case hitTile4:
        case hitTile5:		// back in the pool, we don't really care where
            pickedSource = TraxId.NoWhere;
            break;

        case EmptyBoard:
            switch (pickedSource)
            {

            case hitTile0:
            case hitTile1:		// back in the pool, we don't really care where
            case hitTile2:
            case hitTile3:		// back in the pool, we don't really care where
            case hitTile4:
            case hitTile5:		// back in the pool, we don't really care where
            case BoardLocation:
            {  SetBoard(col,row, pickedObject);
               droppedDest = TraxId.EmptyBoard;
            }
            break;
			default:
				break;
            }
        }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(char col, int row)
    {	int x = colToX(col);
    	int y = rowToY(row);
        return ((droppedDest == TraxId.EmptyBoard) 
        		&& (dropped_x == x) 
        		&& (dropped_y == y));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {
        if ((pickedSource != TraxId.NoWhere) // if moving
                 &&(droppedDest == TraxId.NoWhere)) // and not placed
        {
            switch (pickedObject)
            {
            default:
				pickedSource = null;
				throw G.Error("Bad char, Origin " + pickedSource + "=" +
                    pickedObject);
            case 'e': return(-1); 
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5': return(pickedObject-'0');

          }
        }

        return (NothingMoving);
    }
 
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TraxId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);

        case BoardLocation:
        	{boolean wasDest = isDest(col,row);
        	unDropObject(); 
        	if(!wasDest)
        	{
        	int x = colToX(col);
        	int y = rowToY(row);
        	pickedObject = getBoardXY(x,y);
        	pickedSource = source;
        	droppedDest=TraxId.NoWhere;
            picked_x = x;
            picked_y = y;
			lastPickedIndex=movingObjectIndex();
			SetBoard(col,row,Empty);
        	}}
            break;
        case hitSlash:
        case hitBack:
        case hitPlus:	// trax notation style, only one will fit
        	{ char nmatch='0'-1;
        	  int loops=0;
        	  while(nmatch!=Empty)
        	  {	nmatch = nextColorMatch(col,row,(char)(nmatch+1));
        	  	if(((source==TraxId.hitPlus)&& ((nmatch=='0')||(nmatch=='1')))
        	  		|| ((source==TraxId.hitSlash) && ((nmatch=='2')||(nmatch=='4')))
        	  		|| ((source==TraxId.hitBack) && ((nmatch=='3')||(nmatch=='5'))))
        	  	
        	  		{ pickedObject = nmatch; 
        	  		  lastPickedIndex = nmatch-'0';
        	  		  pickedSource = MATCHTILES[lastPickedIndex];
        	  		  break;
        	  		}
        	  	if(loops++>3) { throw G.Error("stuck looking for "+source); }
        	  	}
        	  }
        	break;

        case hitTile0:
        case hitTile1:		
        case hitTile2:
        case hitTile3:		
        case hitTile4:
        case hitTile5:		
			{  pickedObject =  source.shortName.charAt(0);
			   lastPickedIndex = pickedObject-'0';
			   pickedSource = source;
			}

            break;

         }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(char col, int row)
    {	int x = colToX(col);
    	int y = rowToY(row);
        return ((pickedSource == TraxId.BoardLocation) 
        			&& (picked_x == x)
        			&& (picked_y == y));
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
        case ILLEGAL_MOVE_STATE:
        case CONFIRM_STATE:
        	switch(droppedDest)
        	{
        	case NoWhere: setNextStateAfterDone();	break; // start the move over
        	case EmptyBoard: break;	// stay in confirm state
			default:
				break;
        	}
        	break;	// stay in confirm state
        case PLAY_STATE:
        	switch(droppedDest)
        	{
        	case EmptyBoard:
        		setState(TraxState.CONFIRM_STATE);
        		break;
        	default:
			break;
        	}
        	break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterPick()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case PLAY_STATE:
        	break;
        case ILLEGAL_MOVE_STATE:
        case CONFIRM_STATE:
 			setState(TraxState.PLAY_STATE);
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
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
        case ILLEGAL_MOVE_STATE:
    		setState(TraxState.PLAY_STATE);
			break;
		case PLAY_STATE:
    		break;
    	}

    }
    

    private boolean doForcedXY1(Traxmovespec pm,Traxmovespec m,int x,int y)
    {	char ch = getBoardXY(x,y);
    	if(ch==Empty)
    	{	int nc = nColorMatchXY(x,y);
    		switch(nc)
    		{
    		case 0: return(false);
    		case 1: 
	    		{	char col = XtoCol(x);
	    			int row = YtoRow(y);
	    			char mat = nextColorMatch(col,row,'0');
	    			scoreboard[y][x]=sweep;		// mark the forced spaces
	    			int cell = SetBoard(col,row,mat);
	    			fillPath.push(cell);
	    			return(doForcedXY0(pm,m,x,y));
	    		}
	    	default: ;	
    		}
    	}
       	return(true);
    }
    private boolean doForcedXY0(Traxmovespec pm,Traxmovespec m,int x, int y)
    {   	return( doForcedXY1(pm,m,x+1,y)
    		&& doForcedXY1(pm,m,x-1,y)
    		&& doForcedXY1(pm,m,x,y+1)
    		&& doForcedXY1(pm,m,x,y-1));
    }
    private boolean doForcedXY(Traxmovespec m,int x,int y,boolean testonly)
    {	sweep++;
    	Traxmovespec tm = testonly ? null : m;
    	int size = fillPath.size();
    	boolean fo = doForcedXY0(tm,tm,x,y);
    	if(testonly || !fo)
    	{	if(m!=null)
    		{	unwind_Undo(size);
    		}
    		else
    		{
    		for(int xx=left;xx<right;xx++) 
    		{ for(int yy=top;yy<bottom;yy++) 
    			{ if(scoreboard[yy][xx]==sweep) 
    				{ setBoardXY(xx,yy,Empty);
    			      tilesOnBoard--;
    				}
    			}
    		}}
    		dropped_x = x;
    		dropped_y = y;
    		droppedDest=TraxId.EmptyBoard;
    	}
    	return(fo);
    }
    public boolean isWinner(int player)
    {	return(win[player]);
    }
    
    private boolean doDone(Traxmovespec m,boolean save_undo)
    {
        acceptPlacement();

        if (board_state==TraxState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(TraxState.GAMEOVER_STATE);
        }
        else
        {	if(doForcedXY(save_undo ? m : null,dropped_x,dropped_y,false)) 
        	{
        	if(WinForPlayerNow(whoseTurn))
        		{ win[whoseTurn]=true; setState(TraxState.GAMEOVER_STATE); }	// prefer the moving player as the winner 
        	else if(WinForPlayerNow(nextPlayer[whoseTurn]))
        		{ win[nextPlayer[whoseTurn]]=true; setState(TraxState.GAMEOVER_STATE);  
        		}
        	else {setNextPlayer();
        		  setNextStateAfterDone();
        	}
        	}
        	else if(save_undo)
        	{ // a robot move that is illegal.  This is "normal" because the 
        	  // robot doesn't filter them out in advance. The bot will naturally
        	  // avoid this move.
        	  setState(TraxState.GAMEOVER_STATE);
        	  win[nextPlayer[whoseTurn]]=true;
        	  win[whoseTurn]=false;
        	  return(false);
        	}
        	else
        	{setState(TraxState.ILLEGAL_MOVE_STATE);
        	 // illegal move attempted
        		//G.Error("Illegal move");
        	}
        }
        return(true);
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	Traxmovespec m = (Traxmovespec)mm;
        //G.print("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(m,false);

            break;
        case MOVE_MOVE:
        case MOVE_DROPB:
        case MOVE_ROTATEB:
        {	int xx = colToX(m.to_col);
        	int yy = rowToY(m.to_row);
			switch(board_state)
			{ case PUZZLE_STATE: acceptPlacement(); break;
			  case ILLEGAL_MOVE_STATE:
			  case CONFIRM_STATE: 
				  int oleft = left;
				  int otop = top;
				  unDropObject();
				  unPickObject(); 
				  if(tilesOnBoard==0)
				  	{	left = oleft;
				  		top = otop;
				  	}
				  break;
			  case PLAY_STATE:
			   acceptPlacement(); break;
			default:
				break;
			}
			char newcol = XtoCol(xx);
			int newrow = YtoRow(yy);
			pickObject(m.source, newcol,newrow);
			if(tilesOnBoard==0)
			{
				newcol = '@';
				newrow = 0;
			}
            dropObject(TraxId.EmptyBoard, newcol,newrow);
            setNextStateAfterDrop();
        }
        break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	pickObject(TraxId.BoardLocation, m.to_col, m.to_row);
        	setNextStateAfterPick();
            break;

        case MOVE_DROP: // drop on chip pool;
            dropObject(m.source, m.to_col, m.to_row);
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
            if(board_state!=TraxState.PUZZLE_STATE)
            	{unDropObject();
            	}
            droppedDest = TraxId.NoWhere;
            unPickObject();
            pickObject(m.source, m.to_col, m.to_row);
           	setNextStateAfterPick();
            break;

 
        case MOVE_START:
            setWhoseTurn(m.player);
            unPickObject();
            acceptPlacement();
            setState(TraxState.PUZZLE_STATE);
            setNextStateAfterDone();

            break;

        case MOVE_RESIGN:
            setState(unresign==null?TraxState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
        	setState(TraxState.PUZZLE_STATE);

            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(TraxState.GAMEOVER_STATE);
			break;

        default:
        	cantExecute(m);
        }


        return (true);
    }

    public boolean LegalToHitChips(boolean ourmove)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PLAY_STATE: return(ourmove);
        case RESIGN_STATE:
        case ILLEGAL_MOVE_STATE:
        case CONFIRM_STATE:
       	case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return(true);
        }
    }

    public boolean LegalToHitBoard(char col, int row)
    {
        switch (board_state)
        {
		case PLAY_STATE:
			return(GetBoardPos(col,row)!=Empty)
        			? isDest(col,row)
                			: ((nColorMatch(col,row)>0)
                					&& ((tilesOnBoard==0) || nonemptyIsAdjacent(col,row))
                					);
		case RESIGN_STATE:
		case GAMEOVER_STATE:
			return(false);
		case ILLEGAL_MOVE_STATE:
		case CONFIRM_STATE:
			return(isDest(col,row));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return((GetBoardPos(col,row)!=Empty)
        			? ((pickedObject==(char)0) && emptyIsAdjacent(col,row))
        			: ((nColorMatch(col,row)>0)
        					&& ((tilesOnBoard==0) || nonemptyIsAdjacent(col,row))
        					));
        }
    }
    
    StateStack robotState = new StateStack();
    IStack fillPath = new IStack();
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Traxmovespec m)
    {	robotBoard = true;
       	robotState.push(board_state);
       	int size = fillPath.size();
        // to undo state transistions is to simple put the original state back.
        // G.print("R "+m);
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        
        if (Execute(m,replayMode.Replay))
        {	//robotUndo.push(m.undo_info);

            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	
                doDone(m,true);
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
           	if(!blitz)
        	{
        	fillPath.push(size);
        	}

        }
    }
    
  void unwind_Undo(int finalsize)
  {  while(fillPath.size()>finalsize)
		{
	  	int cell = fillPath.pop();
	  	int x = cell>>16;
    	int y = cell&0xffff;
		setBoardXY(x,y,Empty);
		tilesOnBoard--;
		}
  }

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Traxmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	//G.print("U "+m);
        switch (m.op)
        {
   	    default:
   	    	cantExecute(m);
        	break;

        case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
        	unwind_Undo(fillPath.pop());
        	win[whoseTurn]=win[nextPlayer[whoseTurn]]=false;
        	SetBoard(((m.to_col=='@')?'A':m.to_col),
        			 ((m.to_row==0) ? 1 : m.to_row),Empty);
       	break;
        case MOVE_RESIGN:
 
            break;
        }

        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {
        	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
    
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	if(tilesOnBoard==0)
 	{	all.addElement(new Traxmovespec("dropb 0 @ 0",whoseTurn));
 		all.addElement(new Traxmovespec("dropb 4 @ 0",whoseTurn));
 	}
 	else
 	{
 	for(int x = left-1; x<=right; x++)
 		{ for(int y=top-1; y<=bottom; y++)
 			{	char ch = getBoardXY(x,y);
 				if((ch==Empty)&& nonEmptyIsAdjacentXY(x,y))
 				{	char col = XtoCol(x);
 					int row = YtoRow(y);
 					for(int i=0;i<6;i++)
 					{	// try all 6 possible tiles
 						char thisch = (char)('0'+i);
 						if(colorsMatchxy(x,y,thisch))
 						{ all.addElement(new Traxmovespec(MOVE_DROPB,i,col,row,whoseTurn));
 						}
 					}
 				}
 			}
 		}
 	}
 	return(all);
 }
 }
