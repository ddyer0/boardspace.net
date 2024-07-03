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
package yinsh.common;

import online.game.*;

import java.util.Hashtable;

import lib.*;


//
// Feb 14, 2006: major dogwash to switch to "cell" oriented coordinates
//

public class YinshBoard extends hexBoard<YinshCell> implements BoardProtocol,YinshConstants
{
	private YinshState unresign;
	private YinshState board_state;
	public CellStack animationStack = new CellStack();
	
	public YinshState getState() {return(board_state); }
	public void setState(YinshState st) 
	{ 	unresign = (st==YinshState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	//dimensions for the viewer to use
    public boolean blitz = false;
    public int[] chips = new int[1]; // an array for uniformity
    public int[] rings = new int[2]; // rings not yet placed on the board
    public int[] captured_rings = new int[2]; // rings captured in the game
    public YinshCell[][]ringCell = new YinshCell[2][5];
    public YinshCell poolCell[] = new YinshCell[2];
    public YinshCell ringPool[] = new YinshCell[2];
    public YinshId chipPoolIndex[] = new YinshId[2];
    public char ringCharIndex[] = new char[2];
    private char chipChar[] = new char[2];
    YinshId ringCacheIndex[] = new YinshId[2];
    YinshId ringCapturedIndex[] = new YinshId[2];
    YinshId ringIndex[] = new YinshId[2];
    String[] removeString = new String[2];
    String[] placeRingCommand = new String[2];
    String[] placeCommand = new String[2];
    int[] ringImageIndex = new int[2];
    int placementCount = 0;
    
    public char GetBoardPos(char c,int r)
    {	YinshCell cel = getCell(c,r);
    	if(cel==null) { return(Empty); }
    	return(cel.contents);
    }

    // factory method used to construct the cells of the board
    public YinshCell newcell(char c,int r)
    {	return(new YinshCell(c,r));
    }
    // these explicitly remember the place where an object was picked up
    // and where it was dropped.
    private char movingObjectChar = Empty; // char character representing the moving opject
    private YinshId movingOrigin = null; // the source (board, ring cache, chip cache) 
    private char movingFromCol = 'x'; // the column where picked
    private int movingFromRow = 0; // the row where picked
    private YinshId placedDest = null; // the place where dropped
    private char placedToCol = 'x'; // the column where dropped
    private int placedToRow = 0; // the row where dropped

    public void SetDrawState() { throw G.Error("not implemented"); } 
    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public Yinshmovespec lastMove = null; // used by robots

    // this is really part of the robot evaluator, but included here
    // for convenience, because it shares machinery with the move generator
    int[] bchips = new int[2]; // chips currently our color
    int[] moves = new int[2]; // moves our rings can make
    int[] flips = new int[2]; // chips our rings can flip

    public YinshBoard(String init,int map[]) // default constructor
    {	setColorMap(map, 2);
        doInit(init);
    }
    public YinshBoard cloneBoard() 
	{ YinshBoard dup = new YinshBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((YinshBoard)b); }
    //get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object
    public int movingObjectIndex()
    {
        if ((movingOrigin != null) // if moving
                 &&(placedDest == null)) // and not placed
        {
            switch (movingObjectChar)
            {
            default:
                movingOrigin = null;
                throw G.Error("Bad char, Origin " + movingOrigin + "=" +
                    movingObjectChar);
            case Empty:	
				// shouldn't happen, but maybe in damaged games
            	movingOrigin = null;
            	return(NothingMoving);
            case White:
                return (YinshChip.WHITE_CHIP_INDEX);

            case Black:
                return (YinshChip.BLACK_CHIP_INDEX);

            case WRing:
                return (YinshChip.WHITE_RING_INDEX);

            case BRing:
                return (YinshChip.BLACK_RING_INDEX);
            }
        }

        return (NothingMoving);
    }
 
    // true if col,row is the source of the moving object
    public boolean isSource(char col, int row)
    {
        return ((movingOrigin == YinshId.BoardLocation) && (col == movingFromCol) &&
        (row == movingFromRow));
    }

    // true if col,row is where the moving object was dropped
    public boolean isDest(char col, int row)
    {
        return ((placedDest == YinshId.BoardLocation) && (col == placedToCol) &&
        (row == placedToRow));
    }

    public boolean isRemoved(char col, int row)
    {
        return ((movingOrigin == YinshId.RemoveFive) &&
        isInLine(col, row, movingFromCol, movingFromRow, placedToCol,
            placedToRow));
    }

    private void PlaceBoardRing(int color, YinshCell c)
    { //System.out.println("Place "+color+" "+col+row);

        for (int i = 0; i < 5; i++)
        {
            if (ringCell[color][i] == null)
            {	ringCell[color][i] = c;
            	c.lastRing = color;
                return;
            }
        }

        throw G.Error("No empty space for ring");
    }

    private void RemoveBoardRing(int color, YinshCell cc)
    { // System.out.println("UnPlace "+color+" "+col+row);

        for (int i = 0; i < 5; i++)
        {	YinshCell c = ringCell[color][i];
            if (c==cc)
            {	ringCell[color][i]=null;
                return;
            }
        }
        throw G.Error("Ring " + cc + " not found");
    }

    public char SetBoard(char fromcol, int fromrow, char to)
    {	YinshCell c = getCell(fromcol,fromrow);
    	return(SetBoard(c,to));
    }
    public char SetBoard(YinshCell c,char to)
    {
    	char cc = c.contents;
        switch (cc)
        {
        default:
        	throw G.Error("Not expecting to find " + cc);
        case Empty:
            break;
        case White:
        case Black:
            break;
        case WRing:
            RemoveBoardRing(getColorMap()[FIRST_PLAYER_INDEX], c);
            break;
        case BRing:
            RemoveBoardRing(getColorMap()[SECOND_PLAYER_INDEX], c);
            break;
        }

        switch (to)
        {
        default:
        	throw G.Error("Not expecting to place " + to);
        case BRing:
            PlaceBoardRing(getColorMap()[SECOND_PLAYER_INDEX], c);
            break;
        case WRing:
            PlaceBoardRing(getColorMap()[FIRST_PLAYER_INDEX], c);
            break;
        case Black:
        case White:
            break;
        case Empty:
            break;
        }

        c.contents=to;

        return (cc);
    }

    // UnPick any previously moving object and pick up an object.
    // this also does bookkeeping for the number of rings in the pools
    // "removefive" is a somewhat special case
    private char PickObject(YinshId origin, char fromcol, int fromrow)
    {
        char cc = Empty;
        UnDropObject(); // undo previous drop
        UnPickObject(); // undo previous pick

        switch (origin)
        {
        default:
        	throw G.Error("Not a valid origin");

        case RemoveFive:
            cc = GetBoardPos(fromcol, fromrow);

            break;

        case Black_Chip_Pool:
            cc = Black;
            RemoveRing(chips, 0);

            break;

        case White_Chip_Pool:
            cc = White;
            RemoveRing(chips, 0);

            break;

        case Black_Ring_Cache:
            cc = BRing;
            RemoveRing(rings, getColorMap()[SECOND_PLAYER_INDEX]);

            break;

        case White_Ring_Cache:
            cc = WRing;
            RemoveRing(rings, getColorMap()[FIRST_PLAYER_INDEX]);

            break;

        case Black_Ring_Captured:
            cc = BRing;
            RemoveRing(captured_rings, getColorMap()[SECOND_PLAYER_INDEX]);

            break;

        case White_Ring_Captured:
            cc = WRing;
            RemoveRing(captured_rings, getColorMap()[FIRST_PLAYER_INDEX]);

            break;

        case BoardLocation:
        {	YinshCell c = getCell(fromcol,fromrow);
        	lastEmptied = c.lastEmptied;
        	c.lastEmptied = placementCount;
            cc = SetBoard(c, Empty);
        }
            break;
        }

        movingObjectChar = cc;
        movingOrigin = origin;
        movingFromCol = fromcol;
        movingFromRow = fromrow;

        return (cc);
    }
    private int lastEmptied = -1;
    // undo previous pick
    private void UnPickObject()
    {	if(movingOrigin!=null)
    	{
        switch (movingOrigin)
        {
        default:
        	throw G.Error("Not a valid source");

 
        case RemoveFive:
            RemoveFive(movingFromCol, movingFromRow, placedToCol, placedToRow,
                movingObjectChar,replayMode.Replay);

            break;

        case Black_Chip_Pool:
        case White_Chip_Pool:
            AddRing(chips, 0);

            break;

        case Black_Ring_Cache:
            AddRing(rings, getColorMap()[SECOND_PLAYER_INDEX]);

            break;

        case White_Ring_Cache:
            AddRing(rings, getColorMap()[FIRST_PLAYER_INDEX]);

            break;

        case Black_Ring_Captured:
            AddRing(captured_rings, getColorMap()[SECOND_PLAYER_INDEX]);

            break;

        case White_Ring_Captured:
            AddRing(captured_rings, getColorMap()[FIRST_PLAYER_INDEX]);

            break;

        case BoardLocation:
        	YinshCell cell = getCell(movingFromCol, movingFromRow);
        	cell.lastEmptied = lastEmptied;
        	lastEmptied = -1;
            char c = SetBoard(cell, movingObjectChar);

            switch (c)
            {
            default:
            	throw G.Error("Not expecting " + c);

            case Empty:
            case WRing:
            case BRing:
                break;

            case Black:
            case White:
                AddRing(chips, 0);
            }

            break;
        }}
        movingFromRow = 0;
        movingFromCol = '@';
        movingOrigin = null;
    }

    // flip the chips affected by a moving ring
    private void FlipMove(replayMode replay)
    {
        switch (board_state)
        {
        default:
            break;

        case DROP_RING_STATE:
        case MOVE_DONE_STATE:
            Flip(movingFromCol, movingFromRow, placedToCol, placedToRow,replay);

            break;
        }
    }

    // drop an object, ready to commit to the complete move
    private void DropObject(YinshId dest, char tocol, int torow,replayMode replay)
    {
        G.Assert(movingOrigin != null, "Something must be moving");
        placedDest = dest;
        placedToCol = tocol;
        placedToRow = torow;
        if(dest==null) {   UnPickObject(); }
        else
        {
        switch (dest)
        {
        default:
        	throw G.Error("Not a valid dest");


        case Black_Chip_Pool:
        case White_Chip_Pool:
            AddRing(chips, 0);

            break;

        case RemoveFive:
            break;

        case Black_Ring_Cache:
            AddRing(rings, getColorMap()[SECOND_PLAYER_INDEX]);

            break;

        case White_Ring_Cache:
            AddRing(rings, getColorMap()[FIRST_PLAYER_INDEX]);

            break;

        case Black_Ring_Captured:
            AddRing(captured_rings, getColorMap()[SECOND_PLAYER_INDEX]);

            break;

        case White_Ring_Captured:
            AddRing(captured_rings, getColorMap()[FIRST_PLAYER_INDEX]);

            break;

        case BoardLocation:
        	{
       		YinshCell c = getCell(tocol, torow);
       		lastPlaced = c.lastPlaced;
       		c.lastPlaced = placementCount++;
            SetBoard(c, movingObjectChar);
            FlipMove(replay);
        	}
            break;
        }}
    }
    private int lastPlaced = -1;
    
    // undo previous drop, reverting to having a moving object
    private void UnDropObject()
    {	if(placedDest!=null)
    	{
        switch (placedDest)
        {
        default:
        	throw G.Error("Not a valid dest");

        case RemoveFive:
        	return;	// preseve placed position for the remove

        case Black_Chip_Pool:
        case White_Chip_Pool:
            RemoveRing(chips, 0);

            break;

        case Black_Ring_Cache:
            RemoveRing(rings, getColorMap()[SECOND_PLAYER_INDEX]);

            break;

        case White_Ring_Cache:
            RemoveRing(rings, getColorMap()[FIRST_PLAYER_INDEX]);

            break;

        case Black_Ring_Captured:
            RemoveRing(captured_rings, getColorMap()[SECOND_PLAYER_INDEX]);

            break;

        case White_Ring_Captured:
            RemoveRing(captured_rings, getColorMap()[FIRST_PLAYER_INDEX]);

            break;

        case BoardLocation:
        {	YinshCell c = getCell(placedToCol, placedToRow);
        	c.lastPlaced = lastPlaced;
        	lastPlaced = -1;
            SetBoard(c, Empty);
            FlipMove(replayMode.Replay);
        }
            break;
        }}
        placedToRow = 0;
        placedToCol = '@';
        placedDest = null;
    }

    // commit to the pick and drop
    private void commitToMove()
    {
        movingOrigin = placedDest = null;
    	movingFromRow = 0;
    	movingFromCol = '@';
    	placedToRow = 0;
    	placedToCol = '@';
    }



    /*
       private interfaces which use X,Y (ie; real board coordinates)

    */
    private void Init_Standard(String gtype)
    {	blitz = false;
    	if(Y_INIT.equalsIgnoreCase(gtype)) {}
    	else if(YB_INIT.equalsIgnoreCase(gtype)) { blitz=true; }
    	else { throw G.Error(WrongInitError,gtype); }
        gametype = gtype;
        Grid_Style = YINSH_GRID_STYLE;
        initBoard(ZfirstInCol, ZnInCol, ZfirstCol);
        setState(YinshState.PUZZLE_STATE);

        rings[0] = rings[1] = NRINGS; // start with 5 rings each
        chips[0] = NCHIPS;
        captured_rings[0] = captured_rings[1] = 0; // and none captured

        for(YinshCell c=allCells; c!=null; c=c.next) {  c.contents=Empty;  }

        whoseTurn = FIRST_PLAYER_INDEX;
        board_state = YinshState.PLACE_RING_STATE;
    }
    public void sameboard(BoardProtocol f) { sameboard((YinshBoard)f); }

    public void sameboard(YinshBoard from_b)
    {
        super.sameboard(from_b);
        G.Assert(sameCells(ringCell,from_b.ringCell),"ringCell mismatch");

        G.Assert(AR.sameArrayContents(rings,from_b.rings),"ring mismatch");
        G.Assert(AR.sameArrayContents(captured_rings,from_b.captured_rings),"captured_rings mismatch");
  
        G.Assert(movingFromCol == from_b.movingFromCol,"movingFromCol matches");
        G.Assert(movingFromRow == from_b.movingFromRow,"movingFromRow matches");
        G.Assert(placedToCol == from_b.placedToCol,"placedToCol matches");
        G.Assert(placedToRow == from_b.placedToRow,"placedToRow matches");
        G.Assert(movingObjectChar == from_b.movingObjectChar,"movingObjectChar matches");
        G.Assert(movingOrigin == from_b.movingOrigin,"movingOrigin matches");
        G.Assert(placedDest == from_b.placedDest,"placed digest matches");
        G.Assert(placementCount == from_b.placementCount,"placementCount matches");
        G.Assert(Digest()==from_b.Digest(), "Digest matches");
    }

    private long Digest_Rings(Random r, int[] rack)
    {
        int v = 0;

        for (int j = 0; j < rack.length; j++)
        {
            int ct = rack[j];

            for (int i = 0; i < NRINGS; i++)
            {
                long val0 = r.nextLong();

                if (i < ct)
                {
                    v ^= val0;
                }

            }
        }

        return (v);
    }

    public long Digest()
    {
        long v = 0;
        int cols = ncols;
        Random r = new Random(64 * 1000);

        // note, we can't modernize this sequence to use allCells
        // without invalidating the previously generated digests.
        for (int i = 0; i < cols; i++)
        {
            char thiscol = (char) ('A' + i);
            int lastcol = nInCol[i];

            for (int thisrownum = 1, thisrow = 1 + firstRowInCol[i];
                    thisrownum <= lastcol; thisrownum++, thisrow++)
            {
            	long val0 = r.nextLong();
            	long val1 = r.nextLong();
            	long val2 = r.nextLong();

                switch (GetBoardPos(thiscol, thisrow))
                {
                case Empty:
                    v ^= val0;

                    break;

                case White:
                    v ^= val1;

                    break;

                case Black:
                    v ^= val2;

                    break;
				default:
					break;
                }
            }
        }
        v ^= r.nextLong()*chips[0];
        v ^= Digest(r,placementCount);
        v ^= Digest_Rings(r, rings);
        v ^= Digest_Rings(r, captured_rings);
        v ^= (movingFromCol*100+movingFromRow*10000+(movingOrigin==null?0:movingOrigin.ordinal()+1)*r.nextLong());
        v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }

    /* make a copy of a board */
    public void copyFrom(YinshBoard from_b)
    {	super.copyFrom(from_b);
    	AR.copy(rings,from_b.rings);
    	AR.copy(captured_rings,from_b.captured_rings);
    	AR.copy(chips,from_b.chips);
    	getCell(ringCell,from_b.ringCell);
    	board_state = from_b.board_state;
    	unresign = from_b.unresign;
    	movingFromCol = from_b.movingFromCol;
        movingFromRow = from_b.movingFromRow;
        placedToCol = from_b.placedToCol;
        placedToRow = from_b.placedToRow;
        movingObjectChar = from_b.movingObjectChar;
        movingOrigin = from_b.movingOrigin;
        placedDest = from_b.placedDest;
        placementCount = from_b.placementCount;
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {
    	randomKey = key;
        Init_Standard(gtype);
        AR.setValue(ringCell, null);

        moveNumber = 1;
        movingOrigin = placedDest = null;
        setState(YinshState.PLACE_RING_STATE);
        
        //
        int map[] = getColorMap();
        int wp = map[FIRST_PLAYER_INDEX];
        int bp = map[SECOND_PLAYER_INDEX];
        
        poolCell[wp] = new YinshCell(White);
        poolCell[bp] = new YinshCell(Black);
        ringPool[wp] = new YinshCell(WRing);
        ringPool[bp] = new YinshCell(BRing);
        chipPoolIndex[wp] = YinshId.White_Chip_Pool;
        chipPoolIndex[bp] = YinshId.Black_Chip_Pool;
        ringCharIndex[wp] = WRing;
        ringCharIndex[bp] = BRing;
        chipChar[wp] = White;
        chipChar[bp] = Black;
        ringCacheIndex[wp] = YinshId.White_Ring_Cache;
        ringCacheIndex[bp] = YinshId.Black_Ring_Cache;
        ringCapturedIndex[wp] = YinshId.White_Ring_Captured;
        ringCapturedIndex[bp] = YinshId.Black_Ring_Captured;
        ringImageIndex[wp] = YinshChip.WHITE_RING_INDEX;
        ringImageIndex[bp] = YinshChip.BLACK_RING_INDEX;
        //
        // trailing spaces in these names are significant!
        removeString[wp] = "Remove WR ";
        removeString[bp] = "Remove BR ";
        placeRingCommand[wp] = "Place WR ";
        placeRingCommand[bp] = "Place BR ";
        placeCommand[wp] =  "Place W ";
        placeCommand[bp] = "Place B ";
        placementCount = 0;

        // note that firstPlayer is NOT initialized here
    }


    public void SetNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        case PUZZLE_STATE:
            break;
        case PLACE_RING_STATE:
        	// needed for some damaged games.
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, state %s",board_state); }
			//$FALL-THROUGH$
		case PLACE_DONE_STATE:
        case MOVE_DONE_STATE:
        case CONFIRM_STATE:
        case RESIGN_STATE:
        case SELECT_LATE_REMOVE_RING_DONE_STATE:
        	setWhoseTurn(nextPlayer[whoseTurn]);
            moveNumber++;
			return;
			
		case SELECT_EARLY_REMOVE_RING_DONE_STATE: // forced to remove before move
            return;

        default:
        	throw G.Error("Move not complete, state %s",board_state);
        }
    }
    public boolean DigestState()
    {	
    	return(DoneState());
    }
    public boolean DoneState()
    {

        switch (board_state)
        {
        case PLACE_DONE_STATE:
        case MOVE_DONE_STATE:
        case SELECT_EARLY_REMOVE_RING_DONE_STATE:
        case SELECT_EARLY_REMOVE_CHIP_DONE_STATE:
        case SELECT_LATE_REMOVE_RING_DONE_STATE:
        case SELECT_LATE_REMOVE_CHIP_DONE_STATE:
        case RESIGN_STATE:
        case CONFIRM_STATE:
            return (true);

        default:
            return (false);
        }
    }

    public boolean WinForPlayerNow(int ind)
    {
        return (win[ind] 
                || (captured_rings[ind] >= (blitz ? 1 : 3)) 
                || ((board_state==YinshState.PICK_RING_STATE)
                	 && (chips[0] == 0) 
                	 && (captured_rings[ind] > captured_rings[ind ^ 1])
                	 ));
    }


    public boolean SetGameOverNow()
    {	boolean win1 = win[FIRST_PLAYER_INDEX] = WinForPlayerNow(FIRST_PLAYER_INDEX);
    	boolean win2 = win[SECOND_PLAYER_INDEX] = WinForPlayerNow(SECOND_PLAYER_INDEX);
    	boolean draw = (board_state==YinshState.PICK_RING_STATE) && (chips[0] == 0);
    	if(win1 || win2 || draw)
    	{ setState(YinshState.GAMEOVER_STATE);
    	  return(true);
    	}
    	return(false);
     }

    // true if the board location is empty
    private boolean isEmpty(char col, int row)
    {
        return (GetBoardPos(col, row) == Empty);
    }

    // true if the board is the player to move's ring
    private boolean isMyRing(char col, int row)
    {
        return (GetBoardPos(col, row) == ringCharIndex[whoseTurn]);
    }
    /*
    //
    // find the move in the legal list which removes col,row.  This is used
    // both to test if this location should be mouse sensitive, and to
    // get the full spec for the move once it is chosen by a single click.
    //
    //This original verision looked for a unique cell to designate a row to remvoe
    //but eventually was found to be inadequate for lines 7 or more
    //
    
    public Yinshmovespec moveForRemoveOld(char col, int row, boolean unique,CommonMoveStack removes)
    {
    	if(removes==null) {  removes = legalFives(null); }
        int sz = removes.size();
        int possible = 0;
        Yinshmovespec mpos = null;

        if (sz == 0)
        {
        	throw G.Error("No removes are possible");
        }

        for (int i = 0; i < sz; i++)
        {
            Yinshmovespec m = (Yinshmovespec) removes.elementAt(i);

            if (isInLine(col, row, m.from_col, m.from_row, m.to_col, m.to_row))
            {
                mpos = m;
                possible++;
            }
        }

        if (unique)
        { // if we want a unique result, complain if there were multiples

            if (possible > 1)
            {
            	throw G.Error("multiple removes are possible");
            }

            return (mpos);
        }

        // if we just want to exclude multiples, return the move only if there was just one.
        return ((possible == 1) ? mpos : null);
    }
    
*/
    private Hashtable<YinshCell,commonMove>cachedSolutions = null;
    public void clearUICache() { cachedSolutions = null; }
    
    // this improved version assigns the sensitive removal cell to
    // the end of the lines if there is overlap between several
    // co-linear lines.  It almost works, exept in the case of
    // a triangle of lines.  So the rarely used cleanup at the
    // end looks for any previously unassigned cell, which picks
    // up the endpoint that was rejected because it was shared
    // with 2 different lines.   Running this test a lot in
    // montebot games indicates it's probably good enough.
    //
    public Hashtable<YinshCell,commonMove> validateRemoves()
       	{
    	if(cachedSolutions==null)
    	{

    	CommonMoveStack removes = legalFives(null); 
    	CommonMoveStack found = new CommonMoveStack();
    	Hashtable <YinshCell,commonMove> solutions = new Hashtable<YinshCell,commonMove>();
    	found.copyFrom(removes);
    	for(YinshCell c = allCells; c!=null; c=c.next)
    	{
    		commonMove m = uniqueMoveForRemove(c.col,c.row,false,removes);   		
    		if(m!=null) { solutions.put(c,m); found.remove(m); }
    	}
    	// in all but the rarest of cases, there is at least one cell that
    	// is unique to a row to remove.  The rare case is a triangle of
    	// moves formed by moving from one corner to just beyond the last,
    	// flipping all the stones along the way.
    	while(found.size()>0)
    	{
    		Yinshmovespec m = (Yinshmovespec)found.pop();
    		YinshCell from = getCell(m.from_col,m.from_row);
    		YinshCell to = getCell(m.to_col,m.to_row);
    		int direction = findDirection(from.col,from.row,to.col,to.row);
    		boolean solved = false;
    		do {
    			if(solutions.get(from)==null) { solutions.put(from,m); solved = true; }
    			if(from==to) { break; }
    			else { from = from.exitTo(direction); }
    		} while (true);
    		G.Advise(solved,"no unique solition for ",m);
        }
    	cachedSolutions = solutions;
    }
    	return cachedSolutions;
    }
    public commonMove findMoveForRemove(char col,int row)
    {	cachedSolutions = null;
    	Hashtable<YinshCell,commonMove>solutions = validateRemoves();
    	return solutions.get(getCell(col,row)); 	
    }
    //
    // find the move in the legal list which removes col,row.  This is used
    // both to test if this location should be mouse sensitive, and to
    // get the full spec for the move once it is chosen by a single click.
    //
    public Yinshmovespec uniqueMoveForRemove(char col, int row, boolean unique,CommonMoveStack removes)
    {
    	if(removes==null) {   removes = legalFives(null); }
        int sz = removes.size();
        int possible = 0;
        Yinshmovespec mpos = null;
        boolean endline = false;
        if (sz == 0)
        {
        	throw G.Error("No removes are possible");
        }

        for (int i = 0; i < sz; i++)
        {
            Yinshmovespec m = (Yinshmovespec) removes.elementAt(i);
            char from_col = m.from_col;
            int from_row = m.from_row;
            char to_col = m.to_col;
            int to_row = m.to_row;
            if (isInLine(col, row, from_col,from_row,to_col,to_row))
            {	boolean isend = isEndLine(col,row,from_col,from_row,to_col,to_row);
            	if(mpos!=null)
            	{	if(endline && !isend) {}
            		else if(isend && !endline) 
            		{	// take over the position
            			mpos = m;
            			endline = isend;
            			possible = 1;
            		}
            		else { possible++; }
            	}
            	else
            	{
                mpos = m;
                endline = isend;
                possible++;
            	}
            }
        }

        if (unique)
        { // if we want a unique result, complain if there were multiples

            if (possible > 1)
            {
            	throw G.Error("multiple removes are possible");
            }

            return (mpos);
        }

        // if we just want to exclude multiples, return the move only if there was just one.
        return ((possible == 1) ? mpos : null);
    }
 
    //
    // this is the major state-dependant calculation to see if a board position
    // should be mouse sensitive right now.  
    //
    public boolean legalToHitBoard(char col, int row)
    {
        switch (board_state)
        {
        default:
            return (false);

        case PUZZLE_STATE:
            return ((movingObjectIndex()==NothingMoving)!=isEmpty(col, row));

        case SELECT_LATE_REMOVE_RING_STATE:
        case SELECT_EARLY_REMOVE_RING_STATE:
            return (isMyRing(col, row));

        case SELECT_EARLY_REMOVE_CHIP_STATE:
        case SELECT_LATE_REMOVE_CHIP_STATE:
        	Hashtable<YinshCell,commonMove> solutions = validateRemoves();
        	YinshCell c = getCell(col,row);
            return solutions.get(c)!= null;

        case MOVE_DONE_STATE:

            // we moved a ring, we're allowed to pick it up again
            return (((col == placedToCol) && (row == placedToRow)));

        case PLACE_DONE_STATE:
        case PLACE_RING_STATE: // placing a ring on an empty square.   
            return (isEmpty(col, row));

        case PICK_RING_STATE: // picking a ring to move
            return (isMyRing(col, row));

        case DROP_RING_STATE:

            // ready to drop a moving ring, but it must be in a line etc.
            if (isEmpty(col, row))
            {
            	CommonMoveStack  legal = legalMovesFromPosition(movingFromCol,movingFromRow, new CommonMoveStack(), -1);

                // this is complicated, so we genrate the list of all legal moves
                // from the starting position of the ring, and see if col,row is 
                // somethere among them.
                int sz = legal.size();

                for (int i = 0; i < sz; i++)
                {
                    Yinshmovespec m = (Yinshmovespec) legal.elementAt(i);

                    if ((m.to_col == col) && (m.to_row == row))
                    {
                        return (true);
                    }
                }
            }

            return (false);
        }
    }

    // true if it's legal to hit the unplayed ring cache
    public boolean legalToHitRing(int player, YinshId target)
    {
        switch (board_state)
        {
        default:
            return (false);

        case PUZZLE_STATE:
        	int mo = movingObjectIndex();
        	switch(mo)
        	{
        	case NothingMoving: 
        		switch(target)
        		{
        		case White_Ring_Cache:	return(rings[FIRST_PLAYER_INDEX]>0);
        		case Black_Ring_Cache: return(rings[SECOND_PLAYER_INDEX]>0);
        		case Black_Ring_Captured: return(captured_rings[SECOND_PLAYER_INDEX]>0);
        		case White_Ring_Captured: return(captured_rings[FIRST_PLAYER_INDEX]>0);
        		case Black_Chip_Pool: return(chips[FIRST_PLAYER_INDEX]>0);
				default:
					break;
        		}
        		return(false);
        	case YinshChip.BLACK_CHIP_INDEX:
        	case YinshChip.WHITE_CHIP_INDEX:
        		return(target==YinshId.Black_Chip_Pool);
        	case YinshChip.WHITE_RING_INDEX: 
        		return((target==YinshId.White_Ring_Cache) || (target==YinshId.White_Ring_Captured));
        	case YinshChip.BLACK_RING_INDEX: 
        		return((target==YinshId.Black_Ring_Cache)||(target==YinshId.Black_Ring_Captured));
			default:
				break;
        	}
        	return(false);

        case PICK_RING_STATE:

            switch (target)
            {
            default:
                break;

            case Black_Chip_Pool:
            case White_Chip_Pool:
                return (true);
            }

            return (false);

        case PLACE_DONE_STATE:
        	return(false);
        case PLACE_RING_STATE:

            switch (target)
            {
            default:
                return (false);

            case Black_Ring_Cache:
            case White_Ring_Cache:
                return (player == whoseTurn);
            }
        }
    }

    // bookeeping for moving rings from cache to board and back
    private void RemoveRing(int[] mrings, int idx)
    {
        G.Assert(mrings[idx] > 0, "No Rings Left");
        mrings[idx]--;
    }

    private void AddRing(int[] mrings, int idx)
    {
        mrings[idx]++;
    }

    public void SetNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("no Next from state " + board_state);

        case DROP_RING_STATE:
            setState(YinshState.MOVE_DONE_STATE);

            break;

        case PICK_RING_STATE:
            setState(YinshState.DROP_RING_STATE);

            break;

        case PLACE_DONE_STATE: // after dropping on a different board location
        case PUZZLE_STATE:
            break;

        case PLACE_RING_STATE:
            setState(YinshState.PLACE_DONE_STATE);

            break;
        }
    }

    public void SetNextStateAfterDone(replayMode replay)
    {
        switch (board_state)
        {
        case PLACE_RING_STATE:
        	//needed to replay some damaged games
        	if(replay==replayMode.Live) { throw G.Error("no next after done in state " + board_state);}
        	SetNextPlayer(replay);
        	break;
        default:
        	throw G.Error("no next after done in state " + board_state);
        case SELECT_EARLY_REMOVE_CHIP_DONE_STATE:
            setState(YinshState.SELECT_EARLY_REMOVE_RING_STATE);
            break;
            
        case SELECT_LATE_REMOVE_CHIP_DONE_STATE:
            setState(YinshState.SELECT_LATE_REMOVE_RING_STATE);
            break;

        case SELECT_EARLY_REMOVE_RING_DONE_STATE:
        {
            // removing rings before our move, a gift from the opponent
            if (SetGameOverNow())
            {
 
                break;
            }

            YinshState nextState = legalFivesExist() ? YinshState.SELECT_EARLY_REMOVE_CHIP_STATE
                                              : YinshState.PICK_RING_STATE;
            setState(nextState);
        }

        break;

        case SELECT_LATE_REMOVE_RING_DONE_STATE:

            if (SetGameOverNow())
            {

                break;
            }

			//$FALL-THROUGH$
        case CONFIRM_STATE:
		case MOVE_DONE_STATE:
        // if a 5 exists, go to remove them and a ring.  Otherwise back to picking
        {
        	YinshState nextState = legalFivesExist() ? YinshState.SELECT_LATE_REMOVE_CHIP_STATE
                                              : YinshState.PICK_RING_STATE;

            if (nextState == YinshState.PICK_RING_STATE)
            {
                SetNextPlayer(replay);

                if (legalFivesExist())
                { //it's possible for one player to create fives for the other.
                    nextState = YinshState.SELECT_EARLY_REMOVE_CHIP_STATE;
                    if(G.debug()) { validateRemoves(); }
                }
            }
            else {   if(G.debug()) { validateRemoves(); }}
            setState(nextState);
            SetGameOverNow();
        }

        break;

        case PUZZLE_STATE:
            break;

        case PLACE_DONE_STATE:
        {
            SetNextPlayer(replay);

            YinshState nextState = (rings[whoseTurn] == 0) ? YinshState.PICK_RING_STATE
                                                    : YinshState.PLACE_RING_STATE;
            setState(nextState);
        }

        break;
        }
    }

    // return the type of chip (Black, White) corresponding to 
    // the chip pool and contents of the board.  These should
    // match - black rings with black chip pool etc.
    public char ChipType(YinshId obj, char contents)
    {
        switch (obj)
        {
        default:
        	throw G.Error("Not a chip type: " + obj);

        case BoardLocation:

            switch (contents)
            {
            default:
            	throw G.Error("Bad contents " + contents);

            case BRing:
                return (Black);

            case WRing:
                return (White);
            }

        case Black_Chip_Pool:

            if (contents != BRing)
            {
            	throw G.Error("Bad contents: " + contents);
            }

            return (Black);

        case White_Chip_Pool:

            if (contents != WRing)
            {
            	throw G.Error("Bad contents: " + contents);
            }

            return (White);
        }
    }
    private int indexForContents(char col)
    {
    	if(col==WRing || col==White) { return(getColorMap()[0]); }
    	return(getColorMap()[1]);
    }
    //
    // remove or restore 5-in-a-row by bisecting the endpoints twice.  This
    // requires that the endpoints really be in a line...
    //
    public void RemoveFive(char from_col, int from_row, char to_col,
        int to_row, char setto,replayMode replay)
    {	YinshCell c = getCell(from_col,from_row);
    	int dir = findDirection(from_col,from_row,to_col,to_row);
    	for(int i=0;i<5;i++)
    	{	char oldContents = c.contents;
    		c.contents=setto;
    		if(i==4) { G.Assert((c.col==to_col)&&(c.row==to_row),"got to end of 5"); }
    		if(replay.animate)
    		{
    			animationStack.push(c);
    			animationStack.push(poolCell[indexForContents(oldContents)]);
    		}
    		c = c.exitTo(dir);
    	}
        switch (setto)
        {
        default:
        	throw G.Error("Not expecting " + setto);

        case Empty:
            chips[0] += 5;

            break;

        case White:
        case Black:
            chips[0] -= 5;

            break;
        }
    }

    public YinshCell GetBoard(char col,int row)
    {	return(getCell(col,row));
    }
    //
    // flip all the markers between from and to
    //
    public void Flip(char from_col, int from_row, char to_col, int to_row,replayMode replay)
    {
        int dir = findDirection(from_col,from_row,to_col,to_row);
        YinshCell flipStart = getCell(from_col,from_row);
        YinshCell c = flipStart.exitTo(dir);
        while((c.row!=to_row)||(c.col!=to_col))
        {	switch(c.contents)
        	{
        	case Empty: break;
        	case White: c.contents=Black; break;
        	case Black: c.contents=White; break;
        	default: throw G.Error("unexpected contents of "+c);
        	}
        	if((c.contents!=Empty) && (replay.animate))
        	{
        		animationStack.push(flipStart);
        		animationStack.push(c);
        	}
         	c=c.exitTo(dir);
        }
    }


    public void doDone(replayMode replay)
    { // make the move permanant
        commitToMove();

        if (board_state==YinshState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
            setState(YinshState.GAMEOVER_STATE);
        }
        else
        {
            SetNextStateAfterDone(replay);
        }
    }

    private void doPick(Yinshmovespec m)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Can't execute pick " + m);

        case MOVE_DONE_STATE:
            UnDropObject();
            setState(YinshState.DROP_RING_STATE);

            break;

        case PICK_RING_STATE:
        case PLACE_RING_STATE:
        case PLACE_DONE_STATE:
        case PUZZLE_STATE:
            PickObject(m.object, m.from_col, m.from_row);
        }
    }

    private void doDrop(YinshId object, char col, int row,replayMode replay)
    { // drop a floating object

        switch (board_state)
        {
        default:
        	throw G.Error("Can't execute drop");

        case PICK_RING_STATE:

            switch (object)
            {
            default:
            	throw G.Error("Can't pick from " + object);

            case Black_Chip_Pool:
            case White_Chip_Pool:
                UnPickObject();
                // note that we don't change state here.
                break;

            case BoardLocation:

                char cont = PickObject(YinshId.BoardLocation, col, row);
                		
                RemoveRing(chips, 0);
                SetBoard(col, row, ChipType(object, cont));
                SetNextStateAfterDrop();
            }

            break;

        case PUZZLE_STATE:
        case DROP_RING_STATE:
        case PLACE_DONE_STATE:
        case PLACE_RING_STATE:

            switch (object)
            {
            default:
            	throw G.Error("Can't drop on" + object);

            case Black_Ring_Cache:
            case White_Ring_Cache:
            case Black_Chip_Pool:
            case White_Chip_Pool:
            case White_Ring_Captured:
            case Black_Ring_Captured:
                DropObject(object, col, row,replay);
                break;
                
            case BoardLocation:
            
                DropObject(object, col, row,replay);
                if(replayMode.Single==replay)
                {	YinshCell from = getCell(movingFromCol, movingFromRow);
                	YinshCell to = getCell(col,row);
                	if(from!=null && to!=null)
                	{
                	animationStack.push(from);
                	animationStack.push(to);
                	}
                }
                SetNextStateAfterDrop();
                
                break;
            }
           
        }
    }

    //
    // execute a move. Mostly the move should be certified legal, but
    // some checking is done, intended to be a bug check rather than
    // a fraud prevention.
    //
    public boolean Execute(commonMove mm,replayMode replay)
    {	Yinshmovespec m = (Yinshmovespec)mm;

        //System.out.println("E "+m+" for "+whoseTurn+" "+board_state);
    	YinshId object = m.object;

        switch (m.op)
        {
        default:
        	cantExecute(m);
        	break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(YinshState.GAMEOVER_STATE);
			break;
       case MOVE_EDIT:

            // edit state is where you can just move things around at will
           commitToMove();
           setState(YinshState.PUZZLE_STATE);
            
            break;

        case MOVE_REMOVE:

            // remove a row of 5 chips, or a ring
            switch (m.object)
            {
            default:
            	throw G.Error("Can't remove " + object);

            case Black_Chip_Pool:
            case White_Chip_Pool:
                PickObject(YinshId.RemoveFive, m.from_col, m.from_row);
                DropObject(YinshId.RemoveFive, m.to_col, m.to_row,replay);
                RemoveFive(m.from_col, m.from_row, m.to_col, m.to_row, Empty,replay);

                switch (board_state)
                {
                default:
                	throw G.Error("Not expecting state %s", board_state);

                case SELECT_LATE_REMOVE_CHIP_STATE:
                    setState(YinshState.SELECT_LATE_REMOVE_CHIP_DONE_STATE);

                    break;

                case SELECT_EARLY_REMOVE_CHIP_STATE:
                    setState(YinshState.SELECT_EARLY_REMOVE_CHIP_DONE_STATE);

                    break;
                }

                break;

            case Black_Ring_Cache:
            case White_Ring_Cache:

            	YinshId dr = (m.object == YinshId.White_Ring_Cache) ? YinshId.White_Ring_Captured
                                                        : YinshId.Black_Ring_Captured;
                PickObject(YinshId.BoardLocation, m.from_col, m.from_row);
                placementCount++;
                DropObject(dr, '@', -1,replay);
                if(replay.animate)
                {
                	animationStack.push(getCell(m.from_col,m.from_row));
                	animationStack.push(ringPool[whoseTurn]);
                }
                switch (board_state)
                {
                default:
                	throw G.Error("Not expecting state %s", board_state);

                case SELECT_EARLY_REMOVE_RING_STATE:
                    setState(YinshState.SELECT_EARLY_REMOVE_RING_DONE_STATE);

                    break;

                case SELECT_LATE_REMOVE_RING_STATE:
                    setState(YinshState.SELECT_LATE_REMOVE_RING_DONE_STATE);

                    break;
                }
            }

            break;

        case MOVE_PASS:
            setState(YinshState.CONFIRM_STATE);
            break;

        case MOVE_RESIGN:
            setState(unresign==null?YinshState.RESIGN_STATE:unresign);
            break;
            

        case MOVE_PLACE:

            // place a ring in the opening sequence
        	if((movingObjectIndex()<=0) && (replay==replayMode.Live)) { replay = replayMode.Single; }
            PickObject(object, '@', -1);
            object = YinshId.BoardLocation;
            if(replay==replayMode.Single)
            {
            	animationStack.push(board_state==YinshState.PICK_RING_STATE? poolCell[whoseTurn] : ringPool[whoseTurn] );
            	animationStack.push(getCell(m.to_col, m.to_row));
            }

 			//$FALL-THROUGH$
		case MOVE_DROP:
            doDrop(object, m.to_col, m.to_row,replay);
            if(board_state==YinshState.PUZZLE_STATE)
            {
            	placedDest = null;
            	movingOrigin = null;
            }
            break;

        case MOVE_MOVE:
            PickObject(chipPoolIndex[whoseTurn], '@', -1);
            if(replay==replayMode.Single)
            {	YinshCell start = getCell(m.from_col, m.from_row);
            	animationStack.push(poolCell[whoseTurn]);
            	animationStack.push(start);
            }
            
            doDrop(YinshId.BoardLocation, m.from_col, m.from_row,replay);
            doDrop(YinshId.BoardLocation, m.to_col, m.to_row,replay);
                      
            break;

        case MOVE_PICK:

            // pick up an object
            doPick(m);

            break;

        case MOVE_DONE:
            doDone(replay);

            break;

        case MOVE_START:
            whoseTurn = m.player;
            commitToMove();
            setState((rings[m.player] > 0) ? YinshState.PLACE_RING_STATE
                                                : YinshState.PICK_RING_STATE);

            break;
        }


        return (true);
    }

    //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public boolean UnExecute(Yinshmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	YinshId object = m.object;

        switch (m.op)
        {
        default:
        	cantExecute(m);
        	break;

        case MOVE_DONE:
            break;

        case MOVE_REMOVE:

            // remove a row of 5 chips, or a ring
            switch (m.object)
            {
            default:
            	throw G.Error("Can't remove " + object);

            case Black_Chip_Pool:
            case White_Chip_Pool:
                RemoveFive(m.from_col, m.from_row, m.to_col, m.to_row,
                    (m.object == YinshId.Black_Chip_Pool) ? Black : White,replayMode.Replay);
                commitToMove();

                break;

            case Black_Ring_Cache:
            case White_Ring_Cache:

            	YinshId dr = (m.object == YinshId.White_Ring_Cache) ? YinshId.White_Ring_Captured
                                                        : YinshId.Black_Ring_Captured;
                PickObject(dr, '@', 0);
                DropObject(YinshId.BoardLocation, m.from_col, m.from_row,replayMode.Replay);
                commitToMove();
            }

            break;

        case MOVE_PASS:
            break;

        case MOVE_PLACE:

            // place a ring in the opening sequence
            PickObject(YinshId.BoardLocation, m.to_col, m.to_row);
            DropObject(m.object, '@', -1,replayMode.Replay);
            commitToMove();

            break;

        case MOVE_MOVE:
            PickObject(YinshId.BoardLocation, m.from_col, m.from_row);
            DropObject(YinshId.White_Chip_Pool, '@', -1,replayMode.Replay);
            SetBoard(m.from_col, m.from_row, SetBoard(m.to_col, m.to_row, Empty));
            Flip(m.from_col, m.from_row, m.to_col, m.to_row,replayMode.Replay);
            commitToMove();

            break;

        case MOVE_RESIGN:
  
            break;
        }

        setState(m.state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
        return (true);
    }

    // execute with autodone
    public void RobotExecute(Yinshmovespec m)
    {
        m.state = board_state; //record the starting state
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

    public boolean legalFivesExist()
    {
    	CommonMoveStack  res = legalFives(null);

        return (res.size() > 0);
    }

    // true if col,row is in the line defined by from-to.  This is dependant
    // on the coordinate system representation for the board
    public boolean isInLine(char col, int row, char from_col, int from_row,char to_col, int to_row)
    {
   	boolean val = false;
    if((from_col==to_col)&&(col==from_col)) 
		{ val = (from_row<to_row) 
			? ((row>=from_row)&&(row<=to_row))
			: ((row>=to_row)&&(row<=from_row));
		}
	else if((from_row==to_row)&&(row==to_row)) 
		{ val = (from_col<to_col) 
			? ((col>=from_col)&&(col<=to_col)) 
			: ((col>=to_col)&&(col<=from_col));
		}
	else { // z axis is the same
			int from_z = from_col-from_row;
			int to_z = to_col-to_row;
			int z = col-row;
			//System.out.println("z");
			if((from_z==to_z)&&(from_z==z))
					{
 					val = ((from_row<to_row) 
 	    					? ((row>=from_row)&&(row<=to_row))
 	    	    			: ((row>=to_row)&&(row<=from_row)));
  					}
     			}
    	return(val);
    }
    // true if col, row is either end of a line
    public boolean isEndLine(char col, int row, char from_col, int from_row,char to_col, int to_row)
    {
    	return ((col==from_col && row==from_row)
    			|| (col==to_col && row==to_row));
    }

    public CommonMoveStack  legalFives(CommonMoveStack  result)
    {
        if (result == null)
        {
            result = new CommonMoveStack();
        }
        YinshCell c = allCells;
        char match = chipChar[whoseTurn];
        while(c!=null)
        {	if(c.contents==match)
        	{
        		for(int dir = 0;dir<3; dir++)
        		{	YinshCell nc = c;
        			boolean valid = true;
         			for(int dis=1;(dis<5) && valid;dis++) 
         			{	nc = nc.exitTo(dir);
         				if((nc==null) || (nc.contents!=match)) { valid = false; }
        			}
         			if(valid) 
         			{	result.addElement(new Yinshmovespec(match,c.col,c.row,nc.col,nc.row,whoseTurn));
         			}
        		}
        	}
        c = c.next;
        }
        return (result);
    }

    public CommonMoveStack  legalMovesFromPosition(char col,int row, CommonMoveStack  result,int forplayer)
    {	return(legalMovesFromPosition(getCell(col,row),result,forplayer));
    }
    // expand result with more moves, assuming that
    // col,row was originally occupied by a ring, 
    // if result is null, just count stats for the evalutor
    public CommonMoveStack  legalMovesFromPosition(YinshCell c, CommonMoveStack  result,int forplayer)
    {	
        for (int dir = 0; dir < 6; dir++)
        {	YinshCell nc = c;
            int state = 0;
            int nflipped = 0;

            for (; state < 2; )
            {	
                 nc = nc.exitTo(dir);
                
                if (nc==null)
                {
                    state = 3;

                    break;
                }

                char contents = nc.contents;

                switch (contents)
                {
                default:
                	throw G.Error("Unexpected contents " + contents);

                case WRing:
                case BRing:
                    state = 3;

                    break;

                case White:
                case Black:
                    nflipped++; // count for the evaluator

                    switch (state)
                    {
                    case 0:
                        state = 1;

						//$FALL-THROUGH$
					case 1:
                        break;

                    case 2:
                        state = 3;

                        break; // seen a row or chips + empty

                    default:
                    	throw G.Error("state error");
                    }

                    break;

                case Empty:

                    switch (state)
                    {
                    default:
                    	throw G.Error("State error");

                    case 0:
                        break;

                    case 1:
                        state = 2; // empty after chips

                        break;

                    case 2:
                        state = 3; // second empty after chips

                        break;
                    }
                }

                if (result != null)
                {
                    if ((state == 2) || (state == 0))
                    {
                        //String msg = "Move " + col + " " + row + " " + nc.col + " " +nc.row;
                        result.addElement(new Yinshmovespec(c.col,c.row,nc.col,nc.row, whoseTurn));
                    }
                }
                else
                { // count this for the evaluator
                    flips[forplayer] += nflipped;
                    nflipped = 0;
                    moves[forplayer]++;
                }

                //if(valid)
            }
        }

        return (result);
    }

    public CommonMoveStack  legalRingRemoves(CommonMoveStack  result)
    {
        int ringsfound = 0;

        if (result == null)
        {
            result = new CommonMoveStack();
        }

        for (int i = 0; i < 5; i++)
        {	YinshCell c = ringCell[whoseTurn][i];
             if (c!=null)
            {   ringsfound++;
                result.addElement(new Yinshmovespec(removeString[whoseTurn] +
                        c.col + " " + c.row, whoseTurn));
            }
        }

        G.Assert((ringsfound + captured_rings[whoseTurn]) == 5,
            "There should be 5 rings in play");

        return (result);
    }

    public CommonMoveStack  legalIfEmpty(CommonMoveStack  result)
    {
        if (result == null)
        {
            result = new CommonMoveStack();
        }
        for(YinshCell c = allCells; c!=null; c=c.next)
        {	if(c.contents==Empty)
        	{ result.addElement(new Yinshmovespec(c.col,c.row,ringCacheIndex[whoseTurn],whoseTurn));
        	}
        }
        return (result);
    }

    // rings you can pick up and move
    public CommonMoveStack  legalRingMoves(CommonMoveStack  result, boolean countonly)
    {
        int ringsfound = 0;

        if ((result == null) && !countonly)
        {
            result = new CommonMoveStack();
        }

        for (int i = 0; i < 5; i++)
        {	YinshCell c = ringCell[whoseTurn][i];
            if (c!=null)
            {
                ringsfound++;
                legalMovesFromPosition(c, result, whoseTurn);
            }
        }

        G.Assert((ringsfound + captured_rings[whoseTurn]) == 5,
            "There should be 5 rings in play");

        return (result);
    }

    CommonMoveStack  List_Of_Legal_Moves(CommonMoveStack  v)
    {
        if (v == null)
        {
            v = new CommonMoveStack();
        }

        switch (board_state)
        {
        case PUZZLE_STATE:
        case GAMEOVER_STATE:
        case DROP_RING_STATE:default:
        	throw G.Error("No Moves for state %s", board_state);

        case SELECT_EARLY_REMOVE_RING_STATE:
        case SELECT_LATE_REMOVE_RING_STATE:
            legalRingRemoves(v);

            break;

        case PICK_RING_STATE: // pick a ring to move
            legalRingMoves(v, false);
            if(v.size()==0)
            {	// rare, no legal moves
            	v.push(new Yinshmovespec(MOVE_PASS,whoseTurn));
            }
            break;

        case PLACE_RING_STATE: // drop one of the initial rings
            legalIfEmpty(v);

            break;

        case SELECT_EARLY_REMOVE_CHIP_STATE: // select 5 to remove
        case SELECT_LATE_REMOVE_CHIP_STATE: // select 5 to remove
            legalFives(v);

            break;

        case SELECT_EARLY_REMOVE_RING_DONE_STATE:
        case SELECT_EARLY_REMOVE_CHIP_DONE_STATE:
        case SELECT_LATE_REMOVE_RING_DONE_STATE:
        case SELECT_LATE_REMOVE_CHIP_DONE_STATE:
        case PLACE_DONE_STATE:
        case MOVE_DONE_STATE:
        case RESIGN_STATE:
            v.addElement(new Yinshmovespec(MOVE_DONE, whoseTurn));

            break;
        }

        return (v);
    }

    void CountPosition()
    {
        int wrings = 0;
        int brings = 0;
        bchips[0] = bchips[1] = 0;
        moves[0] = moves[1] = 0;
        flips[0] = flips[1] = 0;
        YinshCell ce = allCells;
        while(ce!=null)
        {	char c = ce.contents;
            switch (c)
                {
                default:
                	throw G.Error("Not Expecting " + c);
                case Empty:
                    break;

                case Black:
                    bchips[SECOND_PLAYER_INDEX]++;
                    break;
                case White:
                    bchips[FIRST_PLAYER_INDEX]++;
                    break;

                case WRing:
                    wrings++;
                    legalMovesFromPosition(ce, null, FIRST_PLAYER_INDEX);

                    break;

                case BRing:
                    brings++;
                    legalMovesFromPosition(ce, null, SECOND_PLAYER_INDEX);

                    break;
                }
            
            ce=ce.next;
        }

        // since we've just ennumerated the board, make sure nothing got lost 
        G.Assert(5 == (wrings + captured_rings[getColorMap()[FIRST_PLAYER_INDEX]] +
            rings[getColorMap()[FIRST_PLAYER_INDEX]]), "There must be 5 white rings");
        G.Assert(5 == (brings + captured_rings[getColorMap()[SECOND_PLAYER_INDEX]] +
            rings[getColorMap()[SECOND_PLAYER_INDEX]]), "There must be 5 black rings");
        G.Assert((bchips[0] + bchips[1] + chips[0]) == 51,
            "There must be 51 chips");
    }
}
