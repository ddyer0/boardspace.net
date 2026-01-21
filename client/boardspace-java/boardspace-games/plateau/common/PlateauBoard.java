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
package plateau.common;

import java.awt.*;

import java.util.*;

import lib.Graphics;
import lib.AR;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.Tokenizer;
import online.game.*;

/** the board class contains the actual representation of the board, all of the
logic for state transitions and gestures, and most of the logic for drawing
pictures

the other structures which co-depend on the board are "pstack" representing a stack
of pieces, and "piece" representing a single piece.  Common constants are contained
in the "PlateauConstants" class.

The drawing routines are all dual puropse, to draw the object with
appropriate ornamentation, and to note what if anything is being hit
by the mouse.  This integrates the process of drawing with the process
of detecting what is being pointed at; while drawing something, you notice
that the mouse is pointing at it, and make notes which help drive the
state machine.
*/
public class PlateauBoard extends BaseBoard implements BoardProtocol,PlateauConstants
{   int nrows = 0;
	int ncols = 0;
	private PlateauState unresign;
	private PlateauState board_state;
	public int placementIndex = -1;
	public PlateauState getState() {return(board_state); }
	public void setState(PlateauState st) 
	{ 	unresign = (st==PlateauState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    //dimensions for the viewer to use
    private pstack[][] board; //the board is a 2d array of pstacks
    public pstack getCell(char col,int row)
    {
    	return board[col-'A'][row-1];
    }
    private int anonymized_for = -1;
    double lineStrokeWidth = 1;
    // stacks are sized NPIECETYPES so they can be sorted with each type of
    // piece on its own stack.  But that is mostly an arbitrary choice.
    private pstack[][] rack = new pstack[2][NPIECETYPES]; // reserve pieces
    private pstack[][] bar = new pstack[2][NPIECETYPES]; // captured pieces
    private pstack[][] trade = new pstack[2][NPIECETYPES]; // prisoner exchange pieces
    private Variation variation = Variation.Plateau;
    public pstack[] getPlayerRack(int n) { return rack[n]; }
    pstack allCells = null;
    //
    // these arrays are indexed by piece number and stack number respectively.  All
    // communication with the server and other players uses these indexes as proxies
    // for the objects themselves.  Everything depends on all the observers for a game
    // agreeing on what these piece and stack numbers mean.
    //
    private piece[] pieces;  // all pieces, each one has an index number
    private pstack[] stacks; // all permanant stacks

    // these are not private for the convenience of the robot
    int moveStep = -1; // the pick/drop step within a move
    pstack[] movingStackOrigin; // the stack that was picked/dropped
    pstack[] copyOriginalStack; // copy of the stack before pick/drop
    pstack[] movingStack; // the temporary stack that was split off
    piece droppedPiece = null; // the bottom piece on the stack piece we just dropped
    private pstack the_flipped_stack = null; // the board stack we flipped
    private piece the_flipped_piece = null;
    private long onBoardTimer = 0;
    public int[][] stackHeight = new int[2][7];
    public int[] captivePoints = new int[2];
    public int[] captiveCount = new int[2];
    public int[] boardCount = new int[2];
    public int sequence = 1000;

    public PlateauBoard(String init) // default constructor
    {
        doInit(init);
    }
    public PlateauBoard cloneBoard() 
	{ PlateauBoard dup = new PlateauBoard(gametype); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol p)
    {	copyFrom((PlateauBoard)p);
    }
    public piece GetPiece(int n)
    {
        return (pieces[n]);
    }
    public int movingObjectIndex()
    {
        pstack h = movingStack();

        if (h != null)
        {
            return ((h.topOwner() * 10000) + (h.size() * 1000) +
            h.realTopColor().ordinal());
        }
        return(NothingMoving);
    }
    public void anonymize(int owner)
    {
        anonymized_for = owner;

        if (owner >= 0)
        {
            for (int i = 0; i < pieces.length; i++)
            {
                piece p = pieces[i];

                if (p.owner != owner)
                {
                    p.anonymize();
                }
            }
        }
    }

    public pstack GetStack(int n)
    {
        return (stacks[n]);
    }

    void setFlipped(pstack p)
    {
        pstack oldf = the_flipped_stack;
        the_flipped_stack = p;
        the_flipped_piece = (p == null) ? null : p.topPiece();

        if (oldf != null)
        {
            oldf.revealTop();
        }
    }

    private void dropPiece(piece p)
    {	// this is used to mark the "hop" piece, and also to determine if we're moving
    	// forward or backward.  
        droppedPiece = p;
        onBoardTimer = 0;

        // System.out.println("Dropped "+droppedPiece);
    }

    private boolean checkOnboardFloat()
    {
        long now = G.Date();
 
        if (onBoardTimer == 0)
        {
            onBoardTimer = now;

            return (true);
        }

        if ((onBoardTimer + floatTime) > now)
        {
            return (true);
        }

        return (false);
    }
    
 
    public pstack movingStack() // tell the graphics engine about the piece in motion
    {	
        return (((moveStep >= 0)&&(moveStep<movingStack.length)) ? movingStack[moveStep] : null);
    }

    // used to help size the stack displays  
    int stackWidth(Rectangle r)
    {
        return (G.Width(r) / NPIECETYPES);
    }

    // draw an row of pstacks, note mouse hits
    private void drawPstacks(Graphics gc, pstack[] mystack, Rectangle rect,
        HitPoint highlight, String msg,NumberMenu numberMenu)
    {
        if (gc != null)
        {
            GC.setColor(gc,background_color);
            gc.fillRoundRect(G.Left(rect), G.Top(rect), G.Width(rect), G.Height(rect), 5, 5);
            GC.setColor(gc,Color.black);
            gc.drawRoundRect(G.Left(rect), G.Top(rect), G.Width(rect), G.Height(rect), 5, 5);
            GC.Text(gc, true, G.Left(rect), G.Top(rect), G.Width(rect), G.Height(rect),
                Color.black, null, msg);
        }
        if(mystack!=null)
        {
        int npieces = mystack.length;
        int pwidth = G.Width(rect) / npieces;
        int px = G.Left(rect) + ((G.Width(rect) - (pwidth * npieces)) / 2);
        int py = G.Top(rect);
        for (int i = 0; i < npieces; i++)
        {	pstack stack = mystack[i];
            drawPstack(gc, stack, px, py, pwidth, G.Height(rect), highlight);
            numberMenu.saveSequenceNumber(stack,px,py);
            px += pwidth;
        }}
    }

    // draw a single stack
    public void drawPstack(Graphics gc, pstack stack, int px, int py,
        int pwidth, int pheight, HitPoint highlight)
    {	stack.drawnSize = pwidth;
        stack.Draw(gc, px, py, pwidth, pheight, 4, highlight);
    }

    // draw the captives
    public void DrawBar(Graphics gc, Rectangle rect, int index,
        HitPoint highlight, String msg,NumberMenu numberMenu)
    {
        drawPstacks(gc, bar[index], rect, highlight, msg,numberMenu);
    }

    // draw the trade bar
    public void DrawTrade(Graphics gc, Rectangle rect, int index,
        HitPoint highlight, String msg,NumberMenu numberMenu)
    {
        drawPstacks(gc, trade[index], rect, highlight, msg,numberMenu);
    }

    public int pointTotal(pstack[] rr)
    {
        int sum = 0;

        for (int i = 0; i < rr.length; i++)
        {
            sum += rr[i].pointTotal();
        }

        return (sum);
    }

    // draw text between the bar and trade with information about the trade
    public void DrawExchangeSummary(Graphics gc, int idx, Rectangle r,NumberMenu numberMenu)
    {
        pstack[] tr = trade[idx];
        int tot = pointTotal(tr);

        if (tot > 0)
        {
            String msg = "" + tot + " points; ";
            GC.Text(gc, false, G.Left(r), G.Top(r), G.Width(r), G.Height(r), Color.black, null,
                msg);
        }
    }

    // draw the main reserve rack
    public void DrawRack(Graphics gc, Rectangle rect, int index,
        HitPoint highlight,NumberMenu numberMenu)
    {
    	drawPstacks(gc, index>=0?rack[index]:null, rect, highlight, "",numberMenu);
 
    }

    // constructors for basic board setup
    private piece newpiece(PieceType p,int idx, int index, int val,long rv)
    {
        piece pp = new piece(p,idx, index, val,rv);
        pieces[index] = pp;

        return (pp);
    }

    private pstack newpstack(int origin, int idx, int owner)
    {
        pstack ps = new pstack(this, origin, idx, owner);
        stacks[idx] = ps;

        return (ps);
    }

    private void Init_Standard(Variation v)
    {
        int stacknumber = 0;
        int piecenumber = 0;
        variation = v;
        int counts[] = variation.pieceCounts;
        if(ncols!=v.boardSize)
        {
        ncols = nrows = v.boardSize;
        board = new pstack[nrows][ncols];
        stacks = new pstack[(nrows * ncols) + (6 * NPIECETYPES)];
        movingStackOrigin = new pstack[nrows]; 	// the stack that was picked/dropped
        copyOriginalStack = new pstack[nrows]; 	// copy of the stack before pick/drop
        movingStack = new pstack[nrows]; 		// the temporary stack that was split off

        int npieces = 0;
        for(int i=0;i<counts.length;i++) { npieces += counts[i]; }
        pieces = new piece[2*npieces];
        }
        //
        // note that this sequence can't be changed without invalidating
        // of the saved games in the world.
        //
        allCells = null;
        for (int col = 0; col < ncols; col++)
        {
            for (int row = 0; row < nrows; row++)
            {
                pstack ps = newpstack(BOARD_ORIGIN, stacknumber++, -1);
                ps.col_a_d = row;
                ps.row_1_4 = col;
                board[row][col] = ps;
                ps.next = allCells;
                allCells = ps;
            }
        }
        
        Random pr = new Random(1035356);
        for (int idx = FIRST_PLAYER_INDEX; idx <= SECOND_PLAYER_INDEX; idx++)
        {
            for (int j = 0; j < NPIECETYPES; j++)
            {
                bar[idx][j] = newpstack(CAPTIVE_ORIGIN, stacknumber++, idx);
                trade[idx][j] = newpstack(TRADE_ORIGIN, stacknumber++, idx);
            }

            pstack s = rack[idx][PieceType.Mute.index] = newpstack(RACK_ORIGIN,
                        stacknumber++, idx);
            for (int n = 0; n < counts[PieceType.Mute.index]; n++)
            {
                newpiece(PieceType.Mute,idx, piecenumber++, PieceType.Mute.index,pr.nextLong()).addToStack(s);
            } //4 mutes

            s = rack[idx][PieceType.Red.index] = newpstack(RACK_ORIGIN, stacknumber++, idx);

            for (int n = 0; n < counts[PieceType.Red.index]; n++)
            {
                newpiece(PieceType.Red,idx, piecenumber++, PieceType.Red.index,pr.nextLong()).addToStack(s); // 2 reds
            }

 
            s = rack[idx][PieceType.Blue.index] = newpstack(RACK_ORIGIN, stacknumber++,
                        idx);

            for (int n = 0; n < counts[PieceType.Blue.index]; n++)
            {
                newpiece(PieceType.Blue,idx, piecenumber++, PieceType.Blue.index,pr.nextLong()).addToStack(s);// 2 blues
            }
            
            s = rack[idx][PieceType.RedMask.index] = newpstack(RACK_ORIGIN,
                    stacknumber++, idx);
            for (int n = 0; n < counts[PieceType.BlueMask.index]; n++)
            {
            	newpiece(PieceType.RedMask,idx, piecenumber++, PieceType.RedMask.index,pr.nextLong()).addToStack(s);
            }

            s = rack[idx][PieceType.BlueMask.index] = newpstack(RACK_ORIGIN,
                        stacknumber++, idx);
            
            for (int n = 0; n < counts[PieceType.BlueMask.index]; n++)
            {
            newpiece(PieceType.BlueMask,idx, piecenumber++, PieceType.BlueMask.index,pr.nextLong()).addToStack(s);
            }
            
            s = rack[idx][PieceType.Twister.index] = newpstack(RACK_ORIGIN,
                        stacknumber++, idx);
            for (int n = 0; n < counts[PieceType.Twister.index]; n++)
            {
            newpiece(PieceType.Twister,idx, piecenumber++, PieceType.Twister.index,pr.nextLong()).addToStack(s);
            }

            s = rack[idx][PieceType.Ace.index] = newpstack(RACK_ORIGIN, stacknumber++, idx);
            for (int n = 0; n < counts[PieceType.Ace.index]; n++)
            {
            newpiece(PieceType.Ace,idx, piecenumber++, PieceType.Ace.index,pr.nextLong()).addToStack(s);
            }
        }

        whoseTurn = FIRST_PLAYER_INDEX;
    }

    public void revealAll()
    {
        anonymized_for = -1;

        for (int i = 0; i < pieces.length; i++)
        {
            pieces[i].revealAll();
        }
    }

    public piece findPiece(int who, int pieceType)
    {
        pstack[] stack = rack[who];

        for (int i = 0; i < stack.length; i++)
        {
            piece pp = stack[i].findPiece(pieceType);

            if (pp != null)
            {
                return (pp);
            }
        }

        return (null);
    }

    // produce a summary of pieces captured or deployed
    public String pieceSummary(int ind)
    {
        basicStats(ind);

        return ("" + captiveCount[ind] + "(" + captivePoints[ind] + ")" +
        boardCount[ind]);
    }

    // count some basic stats for display and use in the evaluator
    public void basicStats(int ind)
    {
        int onboard = 0;
        int point = 0;
        int tot = 0;
        pstack[] mybar = bar[ind]; //opponent pieces captured
        pstack[] mytrade = trade[ind]; //opponent pieces being traded

        // count the pieces on the board and the height of the stacks
        int[] boardstack = stackHeight[ind];
        int len = boardstack.length - 1;

        for (int i = 0; i <= len; i++)
        {
            boardstack[i] = 0;
        }

        for (int x = 0; x < ncols; x++)
        {
            for (int y = 0; y < nrows; y++)
            {
                pstack bs = board[x][y];

                if (bs.topOwner() == ind)
                {
                    int h = bs.takeOffHeight();
                    boardstack[Math.min(len, h)]++;
                    onboard += h;
                }
            }
        }

        // count the captured pieces and point totals
        for (int i = 0; i < mybar.length; i++)
        {
            tot += mybar[i].size();
            tot += mytrade[i].size();
            point += mybar[i].pointTotal();
            point += mytrade[i].pointTotal();
        }

        boardCount[ind] = onboard;
        captivePoints[ind] = point;
        captiveCount[ind] = tot;
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;
    	Variation v = Variation.find(gtype);
    	if(v!=null)
    	{
    		gametype = gtype;
            Init_Standard(v);
        }
        else
        {
        	throw G.Error(WrongInitError, gtype);
        }
        setFlipped(null);
        moveNumber = 1;
        moveStep = -1;
        placementIndex = 1;
        setState(PlateauState.ONBOARD2_STATE);

        // note that firstPlayer is NOT initialized here
    }

    public void  SetDrawState()
    {	setState(PlateauState.DRAW_STATE);
    }

    /** return true if x,y is a valid board position(i.e. if x,y is on the board */
    private final boolean validBoardPos(int inX, int inY)
    {
        return (!((inX < 0) || (inX >= ncols) || (inY < 0) || (inY >= nrows)));
    }

    public final pstack GetBoardXY(int x, int y)
    {
        if (!validBoardPos(x, y))
        {
        	throw G.Error("Get invalid %s,%s" , x , y);
        }

        return (board[x][y]);
    }

    // remove balls isolated by removing a tile, and balls captured by moving a ball.
    // captured balls are marked with lower case letters. This isn't effectient; it
    // just scans the board, but no matter because it works in human time rather than
    // robot time.

    /*
        Public interfaces which use Column,Row addresses
    */

    // get the contents of a board position, addressed as A1 B2 etc.
    public pstack GetBoardStack(char col, int row)
    {
        if (!((col >= 'A') && (col < ('A' + ncols)) && (row > 0) &&
                (row <= nrows)))
        {
        	throw G.Error("Get invalid " + col + " " + row);
        }

        return (GetBoardXY(col - 'A', row - 1));
    }
    public void sameboard(BoardProtocol f) { sameboard((PlateauBoard)f); }

    // compare two boards.  Used to check the validity of a copy
    public void sameboard(PlateauBoard from_b)
    {
        for (int row = 0; row < nrows; row++)
        {
            for (int col = 0; col < ncols; col++)
            {
                pstack mp = board[col][row];
                pstack fp = from_b.board[col][row];

                if (!(mp.equals(fp)))
                {
                    boolean v = mp.equals(fp);
                    throw G.Error("board mismatch at " + col + " " + row + " = " + v);
                }
            }
        }

        for (int i = 0, len = rack[0].length; i < len; i++)
        {
            if (!((rack[FIRST_PLAYER_INDEX][i].equals(
                        from_b.rack[FIRST_PLAYER_INDEX][i])) &&
                    (rack[SECOND_PLAYER_INDEX][i].equals(
                        from_b.rack[SECOND_PLAYER_INDEX][i])) &&
                    (bar[FIRST_PLAYER_INDEX][i].equals(
                        from_b.bar[FIRST_PLAYER_INDEX][i])) &&
                    (bar[SECOND_PLAYER_INDEX][i].equals(
                        from_b.bar[SECOND_PLAYER_INDEX][i])) &&
                    (trade[FIRST_PLAYER_INDEX][i].equals(
                        from_b.trade[FIRST_PLAYER_INDEX][i])) &&
                    (trade[SECOND_PLAYER_INDEX][i].equals(
                        from_b.trade[SECOND_PLAYER_INDEX][i]))))
            {
            	throw G.Error("Rack mismatch at %s", i);
            }
        }
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch"); 
    }

    // hash digest the contents of a rack
    private long Digest_Rack(Random r, pstack[] myract)
    {
    	long v = 0;

        for (int i = 0; i < myract.length; i++)
        {
            v ^= myract[i].Piece_Digest();
        }

        return (v+r.nextLong());
    }

    // hash digest the entire board.  This is used to detect
    // duplicate games (against the possibility of replaying
    // the same winning game over and over to gain points.
    public long Digest()
    {	
    	long v = 0;
        Random r = new Random(64 * 1000);
        for (int thiscol = 0; thiscol < ncols; thiscol++)
        {
            for (int thisrow = 0; thisrow < nrows; thisrow++)
            {	long sv = r.nextLong();
                pstack contents = board[thiscol][thisrow];
                long dig = contents.Digest(r);
                v ^= sv+dig;
            }
        }
        v ^= Digest_Rack(r, rack[FIRST_PLAYER_INDEX]);
        v ^= Digest_Rack(r, rack[SECOND_PLAYER_INDEX]);
        v ^= Digest_Rack(r, bar[FIRST_PLAYER_INDEX]);
        v ^= Digest_Rack(r, bar[SECOND_PLAYER_INDEX]);
        v ^= Digest_Rack(r, trade[FIRST_PLAYER_INDEX]);
        v ^= Digest_Rack(r, trade[SECOND_PLAYER_INDEX]);
        v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
       return (v);
    }

    /* make a copy of a board.  The robot needds this */
    public void copyFrom(PlateauBoard from_b)
    {	super.copyFrom(from_b);

        for (int row = 0; row < nrows; row++)
        {
            for (int col = 0; col < ncols; col++)
            {
                board[col][row].copyFrom(from_b.board[col][row]);
            }
        }
        
        for (int i = 0; i < NPIECETYPES; i++)
        {	
            rack[FIRST_PLAYER_INDEX][i].copyFrom(from_b.rack[FIRST_PLAYER_INDEX][i]);
            rack[SECOND_PLAYER_INDEX][i].copyFrom(from_b.rack[SECOND_PLAYER_INDEX][i]);
            bar[FIRST_PLAYER_INDEX][i].copyFrom(from_b.bar[FIRST_PLAYER_INDEX][i]);
            bar[SECOND_PLAYER_INDEX][i].copyFrom(from_b.bar[SECOND_PLAYER_INDEX][i]);
            trade[FIRST_PLAYER_INDEX][i].copyFrom(from_b.trade[FIRST_PLAYER_INDEX][i]);
            trade[SECOND_PLAYER_INDEX][i].copyFrom(from_b.trade[SECOND_PLAYER_INDEX][i]);
        }
        
        
        for(int i=0;i<pieces.length;i++) 
        	{ pieces[i].placedPosition = from_b.pieces[i].placedPosition; 
        	}
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        if(G.debug()) { sameboard(from_b); }
               
    }


    public void SetNextPlayer()
    {
        setWhoseTurn((whoseTurn == FIRST_PLAYER_INDEX) ? SECOND_PLAYER_INDEX
                                                       : FIRST_PLAYER_INDEX);
        moveNumber++;
        moveStep = -1;
    }

    // override the usual digeststate method, so we don't try to digest
    // while shuffling pieces in the trade area.
    public boolean DigestState()
    {	
        switch (board_state)
        {
        case RESIGN_STATE:
        case ONBOARD2_DONE_STATE:
        case ONBOARD_DONE_STATE:
        case PLAY_DONE_STATE:
        case PLAY_CAPTURE_STATE:
        case DRAW_STATE:
            return (true);

        default:
            return (false);
        }
    }
    // return true if it's legal to click on the "done" button
    public boolean DoneState()
    {	return(DigestState() 
    		||(board_state==PlateauState.EXCHANGE_DONE_STATE)
    		||(board_state==PlateauState.CAPTIVE_SHUFFLE_STATE));
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	// simultaneous win is not a consideration
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(PlateauState.GAMEOVER_STATE);
    }
    /* return true if the player has won */
    public boolean WinForPlayerNow(int player)
    {
        if (win[player])
        {
            return (true);
        }

        // check for win by capturing 6
        int ncaps = 0;
        pstack[] captures = bar[player];

        for (int i = 0; i < captures.length; i++)
        {
            ncaps += captures[i].size();
        }

        if (ncaps >= 6)
        {
            return (true);
        }

        // check for win by stacking 6
        for (int x = 0; x < ncols; x++)
        {
            pstack[] bx = board[x];

            for (int y = 0; y < nrows; y++)
            {
                pstack stack = bx[y];
                int sz = stack.size();

                if (sz >= 6)
                {
                    if (stack.containsStackOfSix(player))
                    {
                        return (true);
                    }
                }
            }
        }

        return (false);
    }

    /* return true if the game is over */
    public boolean GameOver()
    {
        if (board_state == PlateauState.GAMEOVER_STATE)
        {
            return (true);
        }

        if (board_state == PlateauState.PLAY_STATE)
        {
            return (WinForPlayerNow(FIRST_PLAYER_INDEX) ||
            WinForPlayerNow(SECOND_PLAYER_INDEX));
        }

        return (false);
    }
    int lastDroppedIndex = -1;
    int lastFlippedIndex = -1;
    int lastPickedIndex = -1;
    // three different types of drop gestures.  
    // moving a piece on the board and dropping on top of some stack
    // onboarding a piece and dropping inside an existing stack
    // dropping a piece in transit "nowhere" to send it back where it came from
    // 
    public void handleDrop(pstack which, int level)
    {
        boolean complete = false;

        if (moveStep < 0)  {  return;  }
        if(which==null) { return; }
        pstack mv = movingStack[moveStep];
      
        if ( (mv!=null) && mv.origin == BOARD_ORIGIN)
        {	
            for (int i = moveStep; i >= 0; i--)
            { // check for moving backward, undoing previous movesteps

                if (which == movingStackOrigin[i])
                { // move forward, restoring stacks to their original contents

                    while (moveStep >= i)
                    {
                        pstack dest = movingStackOrigin[moveStep];
                        pstack moving = movingStack[moveStep];
                        dropPiece(moving.elementAt(0));
                        dest.dropStack(dest.size(), moving);
 
                        if (moveStep > 0)
                        {
                            pstack cp = copyOriginalStack[moveStep];
                            piece mp = dest.elementAt(cp.size());
                            movingStack[moveStep - 1] = (mp == null) ? null
                                                                     : mp.makeNewStack();
                        }

                        movingStack[moveStep--] = null;
                    }

                    if (i == 0)
                    {
                        pstack wasflipped = the_flipped_stack;

                        if ((level == -1) && (wasflipped != null))
                        {
                            piece p = wasflipped.topPiece();
                            p.flip();
                            setFlipped(null);
                        }

                        setNextStateAfterDrop(which,(level == 99));
                        complete = true;	// complete means we've undone the whole move
                    }
                }
            }
        }

        if (!complete)
        { // moving ahead rather than back

            if (moveStep < 0)
            {
                moveStep = 0;
            }

            {
                pstack oldStack = movingStack[moveStep];

                if (oldStack != null)
                {
                    movingStack[moveStep] = null;
                    dropPiece(oldStack.elementAt(0));
                }
                	 

                if ((mv!=null) && (mv.origin == BOARD_ORIGIN) &&
                        (which != movingStackOrigin[0]))
                {	
                    moveStep++;
                    G.Assert(moveStep<movingStackOrigin.length,"Move Step out of range");
                    movingStackOrigin[moveStep] = which;
                    copyOriginalStack[moveStep] = new pstack(which);
                    movingStack[moveStep] = null;
                }

                // this is to reveal the underlying piece from the
                // initial 2-stack, when the top piece is flipped then
                // moved.  We defer the reveal until the stack is dropped.
                if ((oldStack!=null) && oldStack.topPiece() == the_flipped_piece)
                {
                    int h = oldStack.size();

                    if (h >= 2)
                    {
                        piece under = oldStack.elementAt(h - 2);
                        under.revealTop();
                    }
                }

                if ((level > 0) && (level < which.size()))
                { // reveal the top of the piece we're placing the new piece on top of.

                    piece oldp = which.elementAt(level - 1);

                    if (oldp != null)
                    {
                        oldp.revealTop();
                    }
                }

               if(oldStack!=null)
               	{ which.dropStack(level, oldStack); 
               	  lastDroppedIndex = which.lastDropped;
               	  which.lastDropped = placementIndex;
               	  placementIndex++;
               	}
            }

            setNextStateAfterDrop(which,(level == -1));
        }
    }

    public pstack locusStack(String str)
    {
        for (int x = 0; x < ncols; x++)
        {
            for (int y = 0; y < nrows; y++)
            {
                pstack ps = board[x][y];

                if (str.equals(ps.locus()))
                {
                    return (board[x][y]);
                }
            }
        }

        throw G.Error("Undefined locus: %s", str);
    }

    public String encodeColors(String colors)
    {
        String res = "" + sequence++ + "-" + colors;

        return (res);
    }

    // pick a stack designated by a number string, and optionally flip and validate
    // the stack according to the specified colors.  This is used only for onboard moves.
    private pstack pickStack(String pstring, String colors)
    { // list of piece numbers separated by commas

        Tokenizer str = new Tokenizer(pstring.replace(',', ' '));
        pstack res = null;
        pstack realorig=null;
        pstack orig=null;
        while (str.hasMoreTokens())
        {
            piece p = GetPiece(str.intToken());
            realorig = p.mystack;
            orig = new pstack(realorig);
            //don't alter these variables, which are only for the use of the GUI.
            //if these are changed here, the sequence pick from rack/drop on board/pick from board/drop off doesn't work
            //movingStackOrigin[0] = p.mystack;
            //copyOriginalStack[0]= new pstack(p.mystack);
            
            pstack news = p.makeNewSingleStack();

            if (res == null)
            {
                res = news;
            }
            else
            {
                res.dropStack(news);
            }
        }

        {
            int clen = (colors == null) ? 0 : colors.length();
            int height = res.size();

            while ((clen > 0) && (height-- > 0))
            {
                String top = colors.substring(0, 1).toUpperCase();
                piece p = res.elementAt(height);
                clen--;

                if (!p.realTopColorString().equals(top))
                {
                    p.flip();
                }

                if (!(p.realTopColorString().equals(top)))
                {
                	throw G.Error("Piece Top Color doesn't match: %s %s",p, top);
                }

                if (clen > 0)
                {
                    clen--;

                    String bot = colors.substring(1, 2).toUpperCase();

                    if (clen > 0)
                    {
                        colors = colors.substring(2);
                    }
                    String bc = p.realBottomColorString();
                    if (!bot.equals(bc))
                    {
                    	throw G.Error("Piece Bottom Color %s doesn't match %s",bc,bot);
                    }
                }
            }
        }

        res.topPiece().revealTop();

        movingStack[0] = res; //split the stack
        movingStackOrigin[0]=realorig;
        copyOriginalStack[0]=orig;
        moveStep = 0;

        return (res);
    }

    public void setNextStateAfterFlip(pstack stack)
    {
        switch (stack.origin)
        {
        case BOARD_ORIGIN:

            switch (board_state)
            {
            
            case PUZZLE_STATE:
                break;
            case EXCHANGE_STATE:
            	setFlipped(stack);		// allow the prisoners to be inspected
            	break;
            case PLAY_STATE:
                setState(PlateauState.FLIPPED_STATE);
                setFlipped(stack);

                break;

            case FLIPPED_STATE:
                setState(PlateauState.PLAY_STATE);
                setFlipped(null);

                break;

            default:
            	throw G.Error("Not allowed to flip in state %s", board_state);
            }

			break;
		default:
            break; //other flips have no consequence
        }
    }

    void handleFlip(piece mov)
    {
        pstack stack = mov.mystack;
        dropPiece(mov);
        mov.flip();
        if(stack.lastFlipped == placementIndex)
        {
        	stack.lastFlipped = lastFlippedIndex;
        }
        else
        {
        	lastFlippedIndex = stack.lastFlipped;
        	stack.lastFlipped = placementIndex;
        }
        setNextStateAfterFlip(stack);
    }

    pstack handlePick(piece pp,replayMode replay)
    {
        		 
        switch (board_state)
        {
        case PUZZLE_STATE:
        case EXCHANGE_STATE:
        case EXCHANGE_DONE_STATE:
        case CAPTIVE_SHUFFLE_STATE:
        case ONBOARD2_STATE:
        case FLIPPED_STATE:
        case PLAY_STATE: // initial states, ignore dropped stack from previous gestures
            movingStackOrigin[0] = pp.mystack;
            copyOriginalStack[0] = new pstack(pp.mystack);
			//$FALL-THROUGH$
		case ONBOARD2_DONE_STATE:
        {
            pstack newstack = pp.makeNewStack(); //split the stack
            newstack.origin = movingStackOrigin[0].origin;	//preserve the original location
            movingStack[0] = newstack; //make a new stack, and don't change movingStackOrigin
            moveStep = 0;
         }
            break;

        case ONBOARD_DONE_STATE:
        {
            // here we pick a single piece, but maybe from inside a stack
            moveStep = 0;
            copyOriginalStack[0] = new pstack(pp.mystack);
            pstack newstack = pp.makeNewSingleStack();
            newstack.origin = movingStackOrigin[0].origin;
            movingStack[0] = newstack; //make a new stack, and don't change movingStackOrigin
        }
            break;

        case PLAY_UNDONE_STATE:
        case PLAY_DONE_STATE:
        case PLAY_CAPTURE_STATE:
        case DRAW_STATE:

            if ((pp == droppedPiece) && (moveStep > 0))
            {	// if we're moving backward, back up the move stack
                moveStep--;
            }

            movingStack[moveStep] = pp.makeNewStack(); // get the whole stack on top

            break;

        default:
        	throw G.Error("Pick not handled in state %s", board_state);
        }

        pstack res = movingStack[moveStep];
        setNextStateAfterPick(res,replay);

        return (res);
    }

    //
    // actually execute a move.  Make changes in the board, select the next state and the next
    // player.  For the most part, this assumes that only legal moves will be executed, but
    // some nonsensical moves trigger error states to indicate likely bugs.
    //
    public boolean Execute(commonMove mm,replayMode replay)
    {	plateaumove m = (plateaumove)mm;
       //  System.out.println("E "+m);
    	m.undostate = getState();		// needed by edithistory

        switch (m.op)
        {
        case MOVE_FLIP:
        {
           piece mov = pieces[m.pick];
           handleFlip(mov);
        }

        break;

        case MOVE_ONBOARD:
        {	// new strategy 10/2007, start an onboard by "drop off" to cancel any 
        	// pick in progress.
        	if(moveStep==0) { handleDrop(movingStackOrigin[0],99); }
        	//
        	// now suck up the pieces required by the onboard.
        	//
            pickStack(m.pieces, m.realColors); // pick the stack and set up the movingStack stuff

            pstack destination = locusStack(m.locus);
            m.setDrop(destination.stacknumber);
            pstack moving = movingStack[0];
            piece top = moving.topPiece();
            top.resetColor(m.pubColors);
            top.placedPosition = (destination.stacknumber<<8)+m.level;
            if(moving.size()>1) 
        	{ piece bot = moving.elementAt(0);
        	  bot.placedPosition=(destination.stacknumber<<8)+m.level-1;
        	}
            handleDrop(destination, m.level);
            // 11/2016 remember the piece we're dropping
            m.pick = droppedPiece.piecenumber;

            break;
        }

        case MOVE_DROP:
        {
            pstack which = (m.drop() == -1) ? movingStackOrigin[0]
                                            : stacks[m.drop()];
            handleDrop(which, m.level);
            pstack from = movingStackOrigin[0];
            if((from.origin==RACK_ORIGIN) && (which.origin==BOARD_ORIGIN))
            {

            droppedPiece.placedPosition = (which.stacknumber<<8)+m.level;
  			if(which.size()>1)
 				{
 				piece bot = which.elementAt(1);
 	        	bot.placedPosition=(which.stacknumber<<8)+m.level-1;
 				}
            }
            else if((from.origin==BOARD_ORIGIN)&&(which.origin==RACK_ORIGIN))
            {
            	droppedPiece.placedPosition=0;
            }
            // 11/2016 remember the piece we're dropping
            m.pick = droppedPiece.piecenumber;
            break;
        }

        case MOVE_FROMTO:

            pstack pickedstack = locusStack(m.locus);
            piece top = pickedstack.topPiece();

            if (m.flip)
            {
                handleFlip(top);
            }

            piece pickeditem = pickedstack.elementAt(pickedstack.size() -
                    m.level);
            m.pick = pickeditem.piecenumber; // remember as in the MOVE_PICK
 
            pstack picked = handlePick(pickeditem,replay); // this splits the stack into two
            
            lastPickedIndex = picked.lastPicked;
            picked.lastPicked = placementIndex;

            
            m.setDrop(pickedstack.stacknumber); // remember the origin stack

            // the color spect is for unflipped colors
            if (m.flip)
            {
                top.flip();
            }

            G.Assert(m.realColors.equals(picked.allColors()),
                "Colors should match");

            if (m.flip)
            {
                top.flip();
            }

            pstack dropped = locusStack(m.tolocus);
            handleDrop(dropped, 99); // drop on top

            break;

        case MOVE_PICK:
        {
            piece pp = pieces[m.pick];
            pstack ps = pp.mystack;
            m.setDrop(ps.stacknumber); // save the actual origin stack for editing
            handlePick(pp,replay);
            lastPickedIndex = ps.lastPicked;
            ps.lastPicked = placementIndex;
        }

        break;

        case MOVE_DONE:
        	placementIndex++;
            setNextStateAfterDone(replay);

            break;

        case MOVE_EDIT:
            setState(PlateauState.PUZZLE_STATE);
            break;

        case MOVE_START:
            whoseTurn = m.player;
            setState((moveNumber < 2) ? PlateauState.ONBOARD2_STATE : PlateauState.PLAY_STATE);
            moveStep = -1;

            break;

        case MOVE_RESIGN:
           setState(unresign==null?PlateauState.RESIGN_STATE:unresign);
           break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        default:
        	cantExecute(m);
        }

        m.state_after_execute = board_state;
        return (true);
    }

    // express execute is used by the robot.  Two differences
    // from execute are that PICK moves don't occur, so the state
    // machine has to be tweaked appropriately, and also
    // must save information to be used by ExpressUnexecute
    public void ExpressExecute(plateaumove m)
    {
    	PlateauState state = getState();
    	if (m.player < 0)
        {
            m.player = whoseTurn;
        }

        switch (m.op)
        {
        case MOVE_ONBOARD:

            switch (state)
            {
            case PLAY_STATE:
                setState(PlateauState.PLAY_DROP_STATE);
                Execute(m,replayMode.Replay);
                setNextStateAfterDone(replayMode.Replay);

                break;

            case ONBOARD2_STATE:
                setState(PlateauState.ONBOARD2_DROP_STATE);
                Execute(m,replayMode.Replay);
                setNextStateAfterDone(replayMode.Replay);

                break;

            default:
            	throw G.Error("Move not handled: %s",m);
            }

            break;

        case MOVE_FROMTO:
        case MOVE_FLIP:
        case MOVE_RESIGN:
            Execute(m,replayMode.Replay);
            // and fall into done
			//$FALL-THROUGH$
		case MOVE_DONE:
            setNextStateAfterDone(replayMode.Replay);

            break;

        default:
        	throw G.Error("Move not handled: %s", m);
        }
        m.state_after_execute = board_state;
        
   }

    public void ExpressUnExecute(plateaumove m)
    {	//G.print("Un "+m);
    	whoseTurn = m.player;
    	setState(m.undostate);
        switch (m.op)
        {
        case MOVE_ONBOARD:

            // put them back in the rack
            pstack p = pickStack(m.pieces, m.realColors);
            rack[whoseTurn][0].dropStack(p);
            moveNumber--;
            moveStep=-1;

            break;

        case MOVE_RESIGN:

            break;

        case MOVE_DONE:
           moveNumber--;

            break;

        case MOVE_FROMTO:
        {	
            piece pickedpiece = pieces[m.pick]; // the target piece we picked
            pstack picked = handlePick(pickedpiece,replayMode.Replay); // picked is the transit stack
            pstack drop = stacks[m.drop()]; //this remembers the origin stack

            if (m.flip)
            {
                picked.topPiece().flip();
            }

            G.Assert(m.realColors.equals(picked.allColors()),
                "Colors should match");
            handleDrop(drop, 99); // drop on top
            moveNumber--;
            break;
        }

        default:
        	throw G.Error("Unexecute move not handled %s", m);
        }

        setState(m.undostate);

    }

    public void test_eval()
    {
    }

    public pstack takeOffStack()
    {
        return ((moveStep >= 0) ? movingStackOrigin[0] : null);
    }

    public boolean isEdgeSquare(int x, int y)
    {
        return ((x == 0) || (x == (ncols - 1)) || (y == 0) ||
        (y == (nrows - 1)));
    }

    public boolean LegalDestination(pstack dest)
    { // return true if we can stop here to pick up, drop or pin

        pstack from = copyOriginalStack[0];
        int height = from.takeOffHeight();
        Face color = from.realTopColor();
        int idx = dest.col_a_d - from.col_a_d;
        int idy = dest.row_1_4 - from.row_1_4;
        int adx = Math.abs(idx);
        int ady = Math.abs(idy);

        if (moveStep > 0)
        { // we must be further away and in the same direction as the
          // previous step.

            pstack pfrom = copyOriginalStack[moveStep];
            int pdx = pfrom.col_a_d - from.col_a_d;
            int pdy = pfrom.row_1_4 - from.row_1_4;

            if (color == Face.Orange)
            {
                int odx = Math.abs(dest.col_a_d - pfrom.col_a_d);
                int ody = Math.abs(dest.row_1_4 - pfrom.row_1_4);

                if ((odx > 1) || (ody > 1))
                {
                    return (false);
                } // flipping to a different dogleg
            }
            else
            {
                if ((Integer.signum(pdx) != Integer.signum(idx)))
                {
                    return (false);
                }

                if ((Integer.signum(pdy) != Integer.signum(idy)))
                {
                    return (false);
                }
            }

            int apdx = Math.abs(pdx);
            int apdy = Math.abs(pdy);

            if (adx < apdx)
            {
                return (false);
            } //move closer   

            if (ady < apdy)
            {
                return (false);
            }
        }

        switch (color)
        {
        default:
        	throw G.Error("Face color %s not handled",color);

        case Blank:
            return ((Math.max(adx, ady) <= height) &&
            ((adx == ady) || (adx == 0) || (ady == 0)));

        case Red:
            return (((adx == 0) || (ady == 0)) && ((adx + ady) <= height));

        case Blue:
            return ((adx == ady) && (adx <= height));

        case Orange:
            return (((adx == 0) || (ady == 0)) ? ((adx + ady) <= 1)
                                               : ((adx == 1) ? (ady == 2)
                                                             : ((ady == 1)
            ? (adx == 2) : false)));
        }
    }

    public boolean legalPin(piece chip)
    { // would it be legal to pick this chip, in the context
      // of a continued move.   True if at the end of the line,
      // or the bottom of the moving stack, or if the piece left
      // behind shows color.  Chip is already known to be one
      // of the original stack

        int droppedindex = chip.height();

        if (droppedindex <= 0) // not in the dropped stack, or at the bottom
        {
            return (true);
        }

        piece p = chip.mystack.elementAt(droppedindex - 1); //next piece under it

        if (!p.isMute() || (copyOriginalStack[0].indexOf(p) < 0))
        {
            return (true);
        } // not being deposited

        // still need to consider a piece picked up along the way
        return (false);
    }

    public boolean stompCapture(pstack p)
    {
        if (p.size() == 0)
        {
            return (false);
        }

        boolean val = !p.elementAt(0).unobstructed();

        //  if(val) 
        //    { System.out.println("Stomp");
        //    }
        return (val);
    }

    public boolean LegalToHit(pstack p, piece chip, boolean ontop)
    { //legalto pick up or drop on or in a stack.  The board state
      //encodes what we're doing - initial pick, looking to drop, or looking to confirm
      //
      // generally, there is a base state, you pick something up, you drop it, 
      // then you either pick it back up, or click done.
      //

        switch (p.origin)
        {
        case RACK_ORIGIN:

            switch (board_state)
            {
            case ONBOARD2_STATE:
            case PLAY_STATE:
            case RACK_DROP_STATE:

                if (p.owner != whoseTurn)
                {
                    return (false); // can't pick up the opponents pieces
                }

                return (true); // initial states, ok to pick from the rack

            case RACK2_DROP_STATE: // picked from the rack, drop to the rack
            case ONBOARD2_DROP_STATE: // picked from the rack, dropped to the rack (instead of the board)
            case ONBOARD_DROP_STATE: // picked from teh rack, dropped to the rack (instead of the board)
            case PUZZLE_STATE:
                return (true);

            case FLIPPED_STATE:
            case EXCHANGE_STATE:
            case EXCHANGE_DONE_STATE:
            case CAPTIVE_SHUFFLE_STATE:
            case PLAY_DROP_STATE:
            case PLAY_DONE_STATE:
            case PLAY_CAPTURE_STATE:
            case ONBOARD2_DONE_STATE:
            case PLAY_UNDONE_STATE:
            case GAMEOVER_STATE:
            case DRAW_STATE:
            case RESIGN_STATE:
            case ONBOARD_DONE_STATE: // can't pick up a second element
                return (false);

            default:
            	throw G.Error("LegalToHit not handled for rack origin %s" ,  board_state);

                //return(false);
            }

        case BOARD_ORIGIN:

            switch (board_state)
            {
            case GAMEOVER_STATE:
            case RESIGN_STATE:
            case RACK2_DROP_STATE:
            case RACK_DROP_STATE:
                return (false);

            case PLAY_DROP_STATE:

                // dropping a stack taken from the board.  We can drop it inside one
                // of our own stacks, or on top of an enemy stack if we show color
                if (LegalDestination(p))
                {
                    int owner = p.topOwner();

                    if (owner == -1)
                    {
                        return (true);
                    }
                    else if (owner == whoseTurn)
                    {
                        return (ontop);
                    }
                    else if (movingStack[moveStep].containsColor())
                    {
                        return (ontop);
                    }
                }

                return (false);

            case PLAY_UNDONE_STATE:
            case PLAY_DONE_STATE:
            case PLAY_CAPTURE_STATE:
            case DRAW_STATE:

                if (!ontop &&
                        (p.indexOf(copyOriginalStack[0].topElement()) >= 0) // contains the original top piece
                         &&(p.topOwner() == whoseTurn) && chip.unobstructed())
                {
                    piece originaltop = copyOriginalStack[moveStep].topElement();
                    boolean isapin = (originaltop == null) ? false
                                                           : (originaltop.owner != chip.owner);

                    if (isapin)
                    {
                        return (legalPin(chip));
                    }

                    return (true);
                }

                return (false);

            case ONBOARD2_DONE_STATE:
            case ONBOARD_DONE_STATE: // moved a piece from the rack to the board, we can pick it up again
                return (!ontop && (chip == droppedPiece));

            case FLIPPED_STATE:

                if (p != the_flipped_stack)
                {
                    return (false);
                }

				//$FALL-THROUGH$
			case PLAY_STATE:
                return ((p.topOwner() == whoseTurn) && chip.unobstructed()); //pick only unobstructed pieces

            case ONBOARD2_DROP_STATE: // drop an initial stack on the nonempty board
                return ((p.size() == 0) && isEdgeSquare(p.row_1_4, p.col_a_d));

            case EXCHANGE_STATE:
            case EXCHANGE_DONE_STATE:
            case CAPTIVE_SHUFFLE_STATE:
            case ONBOARD2_STATE:
                return (false);

            case ONBOARD_DROP_STATE:

                // dropping from the rack onto a stack on the board, which is
                // legal if its' one of your stacks
                if (p.size() == 0)
                {
                    return (true);
                }
                return(chip.unsandwiched(ontop,whoseTurn));
            	//
                //if (p.topOwner() == whoseTurn)
                //{
                //    return (true);
                //}

               // return (false);

            case PUZZLE_STATE:
                return (true);

            default:
            	throw G.Error("LegalToHit not handled for board state %s",  board_state);
            }

        case TRADE_ORIGIN:

            switch (board_state)
            {
            case EXCHANGE_STATE:
            case EXCHANGE_DONE_STATE: // in exchange state, you can rearrange the offered pieces
            case CAPTIVE_SHUFFLE_STATE:

                if (p.owner != whoseTurn)
                {
                    return (false);
                }

 				//$FALL-THROUGH$
			case PUZZLE_STATE:
                return (true);

            case GAMEOVER_STATE:default:
                return (false);
            }

        default:
        	throw G.Error("Origin not handled");

        case CAPTIVE_ORIGIN:

            switch (board_state)
            {
            case EXCHANGE_STATE:
            case EXCHANGE_DONE_STATE:
            case CAPTIVE_SHUFFLE_STATE:
            case PLAY_STATE:

                if (p.owner != whoseTurn)
                {
                    return (false);
                }

				//$FALL-THROUGH$
			case PUZZLE_STATE:
                return (true);

            case GAMEOVER_STATE:default:
                return (false);
            }
        }
    }

    public boolean isOnboarded(piece p)
    {
        return ((p != null) && (p == droppedPiece));
    }

    public boolean isFloatingPiece(piece p, boolean above)
    {
        switch (board_state)
        {
        case DRAW_STATE:
        case PLAY_DONE_STATE:
        case PLAY_CAPTURE_STATE:

            // this is to float the top piece if it was flipped, to reveal the 
            // possibly hidden piece under it. Thi is particularly for the 
            // bottom piece in the original onboard pair.
            return ((p == the_flipped_piece) && checkOnboardFloat());

        case PLAY_STATE:
        case FLIPPED_STATE:

            if (above)
            {
                return (false); //after a capture
            }

			//$FALL-THROUGH$
		case ONBOARD_DONE_STATE:
            return (isOnboarded(p) && checkOnboardFloat());

        default:
            return (false);
        }
    }

    /** given a piece in a stack which has just been landed on,
     * determine if this partucular piece is captured.  This depends
     * on the thickness of enemy pieces on top.
     * Test cases:
     * (1) simple drop one or two pieces captures 1 or 2
     * (2) simple pin does not capture
     * (3) pin + drop an additional piece captures 2
     * (4) dropping on a "sandwich" propogates the capture past the intermediate pieces
     */
    public boolean isCaptured(piece p)
    {
        if (board_state==PlateauState.PLAY_CAPTURE_STATE)
        {
            pstack original = p.mystack;
            Face topcolor = original.realTopColor();
            int topowner = original.topOwner();

            if ((topcolor != Face.Blank) // top of stack is a colored face
                     &&(topowner != p.owner) // top of stack is not our piece
                     &&(original.indexOf(droppedPiece) >= 0)) // we contain the final dropped piece
            {
                int capturedepth = original.captureDepth();
                int topindex = original.size() - capturedepth;

                while ((topindex-- > 0) && (capturedepth > 0))
                {
                    piece victim = original.elementAt(topindex);

                    if (victim == p)
                    {
                        return (true);
                    }

                    if (victim.owner == p.owner)
                    {
                        capturedepth--;
                    }
                }
            }
        }

        return (false);
    }

    // resort a row of stacks into the original ordering, with 
    private void resort(pstack[] st, boolean anon)
    {
        for (int i = 0; i < st.length; i++)
        {
            pstack stack = st[i];

            for (int j = stack.size() - 1; j >= 0; j--)
            {
                piece pp = stack.elementAt(j);
                pp.flipup();

                //if(pp.pieceType<0) 
                //{  G.Error("Already anon");
                //}
                if (pp.realPieceType != i)
                {
                    if (pp.realPieceType < 0)
                    {
                    	throw G.Error("Invalid piece");
                    }

                    pp.addToStack(st[pp.realPieceType]);
                }
            }
        }

        if (anon)
        {
            for (int i = 0; i < st.length; i++)
            {
                pstack stack = st[i];

                for (int j = stack.size() - 1; j >= 0; j--)
                {
                    piece pp = stack.elementAt(j);
                    pp.anonymize();
                }
            }
        }
    }
    // set the next state following an execute of a "done'.
    // this has to dovetail perfectly whith the decisions
    // by setNextStateAfterDrop to enter one of the "Done"
    // states.
    private void setNextStateAfterDone(replayMode replay)
    {
    	PlateauState state = board_state;


        onBoardTimer = -1; // kill it

        switch (state)
        {
        case RESIGN_STATE:
        	setGameOver(false,true);
            break;

        case EXCHANGE_DONE_STATE:

            // move the prisoners in the exchange area to back to the racks
            if (pointTotal(trade[whoseTurn]) == 0)
            { // this is a trade refusal
                setState(PlateauState.CAPTIVE_SHUFFLE_STATE);
                SetNextPlayer();
            }
            else
            {
                for (int pl = FIRST_PLAYER_INDEX; pl <= SECOND_PLAYER_INDEX;
                        pl++)
                {
                    pstack[] tr = trade[pl];
                    pstack[] ra = rack[(pl == FIRST_PLAYER_INDEX)
                        ? SECOND_PLAYER_INDEX : FIRST_PLAYER_INDEX];
                    resort(tr, (pl == anonymized_for));

                    for (int i = 0; i < ra.length; i++)
                    {
                        ra[i].dropStack(tr[i]);
                    }
                }

                setState(PlateauState.PLAY_STATE);

                // note we do NOT change players here.
            }

            break;

        case CAPTIVE_SHUFFLE_STATE:
            SetNextPlayer();
            setState(exchangeIsLegal() ? PlateauState.EXCHANGE_DONE_STATE : PlateauState.EXCHANGE_STATE);

            break;
        
        case ONBOARD2_DONE_STATE:
            setState((whoseTurn == FIRST_PLAYER_INDEX) ? PlateauState.ONBOARD2_STATE
                                                            : PlateauState.PLAY_STATE);
            SetNextPlayer();

            break;
        case DRAW_STATE:
        	SetNextPlayer();
        	setGameOver(false,false);
        	break;
        case PLAY_DONE_STATE:
        case PLAY_UNDONE_STATE:
        case PLAY_CAPTURE_STATE:
           if (moveStep >= 0)
            {	
                pstack p = copyOriginalStack[moveStep];
                // to this top down so stack order is not affected
                for (int i = 0; i <p.size(); i++)
                {
                    piece victim = p.elementAt(i);

                    if (isCaptured(victim))
                    {
                        if (i > 0)
                        { // reveal the top of the next piece down.  This is important
                          // if capturing the top half of the initial 2 stack
                            piece topp = p.elementAt(i - 1);
                            topp.revealTop();
                        }

                        pstack news = victim.makeNewSingleStack();
                        {
                         victim.revealAll();
                         victim.flipup();
                        }
                        bar[whoseTurn][victim.pieceType].dropStack(news);
                        onBoardTimer = 0; //reset the timer so it pops
                    }
                }

                setFlipped(null);
            }
			//$FALL-THROUGH$
		case ONBOARD_DONE_STATE:
            boolean win1 = WinForPlayerNow(whoseTurn);
            boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            if(win1 || win2) { setGameOver(win1,win2); moveNumber++; }
            else
            { setState(PlateauState.PLAY_STATE);
              SetNextPlayer();
            }
            break;
		case PLAY_STATE:
			if(replay==replayMode.Replay)
			{	// allow damaged games to continue
				SetNextPlayer();
				break;
			}
			//$FALL-THROUGH$
		default:
        	throw G.Error("No preparation for done, state =%s", board_state);
        }
    }

    public int subsetTotal(int ask, int who)
    {
        pstack v = new pstack(this, UNKNOWN_ORIGIN);
        int max = 0;

        for (int i = 0; i < bar[who].length; i++)
        {
            v.addCopyStack(bar[who][i]);
            v.addCopyStack(trade[who][i]);
        }

        for (int i = 0; i < (1 << v.size()); i++)
        {
            int ss = v.sumSubset(i);

            if ((ss <= ask) && (ss > max))
            {
                max = ss;
            }
        }

        return (max);
    }

    private boolean exchangeIsLegal()
    {
        int offer = pointTotal(trade[whoseTurn]);
        int ask = pointTotal(trade[(whoseTurn == FIRST_PLAYER_INDEX)
                ? SECOND_PLAYER_INDEX : FIRST_PLAYER_INDEX]);
        int tot = subsetTotal(ask, whoseTurn);

        return (offer >= tot);
    }

    private void setNextStateAfterDrop(pstack stack,boolean atlevel)
    {	int origin = stack.origin;
    	boolean stomp =   stompCapture(stack);
    	boolean atstart = atlevel || ((stack == movingStackOrigin[0]) && !stomp);
        switch (origin)
        {
        case BOARD_ORIGIN:

            switch (board_state)
            {
            case ONBOARD2_STATE: // permitted for the robot, which doesn't do picks
            case ONBOARD2_DROP_STATE:
                setState(PlateauState.ONBOARD2_DONE_STATE);

                break;

            case PLAY_STATE: // permitted for the robot, which doesn't do picks
            case ONBOARD_DROP_STATE:
                setState(PlateauState.ONBOARD_DONE_STATE);

                break;
            case PLAY_DROP_STATE:

                if (atstart)
                {
                    moveStep = -1;
                    setState((the_flipped_stack != null) ? PlateauState.FLIPPED_STATE
                                                              : PlateauState.PLAY_STATE);
                }
                else
                {
                	PlateauState nextState = stomp ? PlateauState.PLAY_CAPTURE_STATE : PlateauState.PLAY_DONE_STATE;

                    if (moveStep > 0)
                    {
                        pstack dest = copyOriginalStack[moveStep];
                        Face new_color = movingStackOrigin[moveStep].realTopColor();
                        int old_owner = dest.topOwner();
                        int nextplayer = (whoseTurn == FIRST_PLAYER_INDEX)
                            ? SECOND_PLAYER_INDEX : FIRST_PLAYER_INDEX;

                        // check for player explicitly, -1 will be for an empty stack
                        if ((old_owner == nextplayer) &&
                                (new_color == Face.Blank))
                        {
                            nextState = PlateauState.PLAY_UNDONE_STATE;
                        }
                        else if (new_color == Face.Orange)
                        {
                            pstack original = movingStackOrigin[0];
                            int dx = Math.abs(dest.col_a_d - original.col_a_d);
                            int dy = Math.abs(dest.row_1_4 - original.row_1_4);

                            if ((dx + dy) != 3)
                            {
                                nextState = PlateauState.PLAY_UNDONE_STATE;
                            }
                        }
                    }
                    else if (moveStep < 0)
                    {
                        moveStep = 0;
                    }

                    setState(nextState);
                }

                break;

            case PUZZLE_STATE:
                moveStep = -1;

                break;

            default:
            	throw G.Error("Shouldn't have dropped on the board in %s",board_state);
            }

            break;

        case RACK_ORIGIN:

            switch (board_state)
            {
            case ONBOARD2_DROP_STATE:
            case RACK2_DROP_STATE:
                moveStep = -1;
                setState(PlateauState.ONBOARD2_STATE);

                break;

            case PLAY_DROP_STATE:
            case ONBOARD2_STATE:
                break;

            case ONBOARD_DROP_STATE:
            case RACK_DROP_STATE:
                moveStep = -1;
                setState(PlateauState.PLAY_STATE);

                break;

            case PUZZLE_STATE:
                moveStep = -1;

                break;

            default:
            	throw G.Error("Should't have dropped on the rack in %s" ,board_state);
            }

            break;

        case TRADE_ORIGIN:

            switch (board_state)
            {
            case EXCHANGE_STATE:
            case EXCHANGE_DONE_STATE:
                setState((exchangeIsLegal()) ? PlateauState.EXCHANGE_DONE_STATE
                                                  : PlateauState.EXCHANGE_STATE);

                break;

            case CAPTIVE_SHUFFLE_STATE:
            case PUZZLE_STATE:
                break;

            default:
            	throw G.Error("Should't have dropped on trade in %s", board_state);
            }

            break;

        case CAPTIVE_ORIGIN:

            switch (board_state)
            {
            case PUZZLE_STATE:
                break;

            case EXCHANGE_STATE:
            case EXCHANGE_DONE_STATE:
                setState((exchangeIsLegal()) ? PlateauState.EXCHANGE_DONE_STATE
                                                  : PlateauState.EXCHANGE_STATE);

                break;

            case CAPTIVE_SHUFFLE_STATE:
            {
                pstack[] caps = trade[whoseTurn];
                int sum = 0;

                for (int i = 0; i < caps.length; i++)
                {
                    sum += caps[i].size();
                }

                if (sum == 0)
                {
                    setState(PlateauState.PLAY_STATE);
                }
            }

            break;

            default:
            	throw G.Error("Should't have dropped on bar in %s", board_state);
            }

            break;

        default:
        	throw G.Error("Origin not handled: %s", origin);
        }
    }

    private void setNextStateAfterPick(pstack st,replayMode replay)
    {
        dropPiece(null);

        switch (st.origin)
        {
        case RACK_ORIGIN:

            switch (board_state)
            {
            case PLAY_UNDONE_STATE:
            case PLAY_DONE_STATE:
            case PLAY_CAPTURE_STATE:
            case DRAW_STATE:
                setState(PlateauState.PLAY_DROP_STATE);

                break;
            case ONBOARD_DONE_STATE:
            	 setState(PlateauState.ONBOARD_DROP_STATE);
            	 break;
            case ONBOARD2_DONE_STATE:
            case ONBOARD2_STATE: // picked up a stack 

                if (st.size() == 2)
                {
                    setState(PlateauState.ONBOARD2_DROP_STATE);
                }
                else
                {
                    setState(PlateauState.RACK2_DROP_STATE);
                }

                break;

            case PLAY_STATE:

                if (st.size() == 1)
                {
                    setState(PlateauState.ONBOARD_DROP_STATE);
                }
                else
                {
                    setState(PlateauState.RACK_DROP_STATE);
                }

                break;

            case PUZZLE_STATE:
                break;

            case FLIPPED_STATE:

                if (the_flipped_stack != null)
                {
                    piece flipped_piece = the_flipped_stack.topPiece();

                    if (flipped_piece.isMonoColor())
                    {
                        if (st.size() == 1)
                        {
                            setState(PlateauState.ONBOARD_DROP_STATE);
                        }
                        else
                        {
                            setState(PlateauState.RACK_DROP_STATE);
                        }

                        setFlipped(null);

                        break;
                    }
                }
				//$FALL-THROUGH$
			default:
            	throw G.Error("No Pick state transition from rack in %s", board_state);
            }
            
            break;

        case BOARD_ORIGIN:

            switch (board_state)
            {
            case PLAY_DONE_STATE:
            case PLAY_CAPTURE_STATE:
            case PLAY_UNDONE_STATE:
            case DRAW_STATE:
            {
                // reveal the piece we uncovered by splitting a stack
                // which is important for the original 2-stack
                piece p = movingStackOrigin[moveStep].topPiece();

                if (p != null)
                {
                    p.revealTop();
                }
            }

            setState(PlateauState.PLAY_DROP_STATE);

            break;

            case ONBOARD_DONE_STATE: // pick from the board after dropping from the rack
                setState(PlateauState.ONBOARD_DROP_STATE);

                break;

            case ONBOARD2_DONE_STATE:
                setState(PlateauState.ONBOARD2_DROP_STATE);

                break;
            case PUZZLE_STATE:
                break;

            case FLIPPED_STATE:

            // fall through
            case PLAY_STATE:

                // reveal the top of the moving flipped piece.  We need this
                // even if not in flipped state, since we can flip uniform pieces
                movingStack[moveStep].topPiece().revealTop();

                // reveal top of the piece left behind
                piece p = movingStackOrigin[moveStep].topPiece();

                if (p != null)
                {
                    p.revealTop();
                }

                setState(PlateauState.PLAY_DROP_STATE);

                break;

            case EXCHANGE_DONE_STATE:
            	// some damaged games add pieces from the board to the exchange area
            	if(replay==replayMode.Replay)
            	{
            		break;
            	}
				//$FALL-THROUGH$
			default:
            	throw G.Error("No Pick Transition from board in %s", board_state);
            }

            break;

        case TRADE_ORIGIN:

            switch (board_state)
            {
            case EXCHANGE_DONE_STATE:
                setState(PlateauState.EXCHANGE_STATE);

                break;

            case CAPTIVE_SHUFFLE_STATE:
            case EXCHANGE_STATE:
            case PUZZLE_STATE:
                break;

            default:
            	throw G.Error("Pick from trade not handled in %s", board_state);
            }

            break;

        case CAPTIVE_ORIGIN:

            switch (board_state)
            {
            case EXCHANGE_DONE_STATE:
                setState(PlateauState.EXCHANGE_STATE);

				break;
			case EXCHANGE_STATE:
                break;

            case CAPTIVE_SHUFFLE_STATE:
            case PLAY_STATE:
                setState(PlateauState.CAPTIVE_SHUFFLE_STATE);

				break;
			case PUZZLE_STATE:
                break;

            default:
            	throw G.Error("Pick from bar not handled in %s", board_state);
            }

            break;

        default:
        	throw G.Error("Pick Origin not handled: %s", st.origin);
        }
    }

    /* draw the board and the stacks on it. */
    public void DrawBoard(Graphics gc, Rectangle R, HitPoint highlight,
        boolean grid,NumberMenu numberMenu)
    {
        int ystep = G.Height(R) / ((nrows * 2) + 2); // 1/2 y square size
        int xstep = G.Width(R) / ((ncols * 2) + 2); // 1/2 x square size
        int xmin = G.Left(R) + xstep; // 1/2 square all the way around
        int ymin = G.Top(R) + ystep;
        int xmax = xmin + (2 * ncols * xstep); // last x
        int ymax = ymin + (2 * nrows * ystep); // last y
        int gridx = xstep / 2;

        if (gc != null)
        {
            GC.fillRect(gc,background_color,G.Left(R), G.Top(R), G.Width(R), G.Height(R));
            GC.setColor(gc,Color.black); // draw the horizontal grid lines

            for (int row = 0; row <= nrows; row++)
            {
                int thisy = ymin + (2 * row * ystep);
                GC.drawLine(gc,xmin, thisy, xmax, thisy);

                if (grid && (row < nrows))
                {
                    GC.Text(gc, false, xmin - gridx,
                        (thisy + ystep) - (gridx / 2), gridx, gridx,
                        Color.black, null, "" + (nrows - row));
                }
            }

            for (int col = 0; col <= ncols; col++) // vertical grid lines
            {
                int thisx = xmin + (col * 2 * xstep);
                GC.drawLine(gc,thisx, ymin, thisx, ymax);

                if (grid && (col < ncols))
                {
                    GC.Text(gc, true, thisx, ymax + 2, xstep * 2, gridx,
                        Color.black, null, "" + (char) ('A' + col));
                }
            }
        }

        // enumerate the squares, painting the stacks, watching a square
        // that contains the mouse.
        for (int x = 0; x < ncols; x++)
        {
            int cx = xmin + (x * xstep * 2);

            for (int y = 0; y < nrows; y++)
            {
                int cy = ymin + (y * ystep * 2);
                pstack contents = GetBoardXY(x, y);
                drawPstack(gc, contents, cx, cy, xstep * 2, ystep * 2, highlight);
                numberMenu.saveSequenceNumber(contents,cx+xstep,cy+ystep	);
            }
        }
    }
	public void randomizeHiddenState(Random robotRandom, int playerIndex) {
		
	}
	public CommonMoveStack GetListOfMoves() {
		// TODO Auto-generated method stub
		return null;
	}
	
	StateStack robotState = new StateStack();
	boolean robotBoard = false;
	public void RobotExecute(commonMove m) {
	       robotState.push(board_state); //record the starting state. The most reliable
	        Execute(m,replayMode.Replay); 		
		
	}
	public void UnExecute(commonMove m0) {
		plateaumove m = (plateaumove)m0;
        //System.out.println("U "+m+" for "+whoseTurn);
    	PlateauState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
            break;

        case MOVE_RESIGN:
            break;
        }
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }		
	}
	public void initRobotValues(PlateauPlay plateauPlay) 
	{
		robotBoard = true;
	}
	public boolean winForPlayerNow(int i) 
	{
		return win[i];
	}
    // add one exchange move.  If this is the first, we'll enter exchange state
    // and add subsequent exchange moves.  
    private void addExchangeMoves(CommonMoveStack all,Random r,int who)
    {	
    	pstack b[] = bar[who];
    	int np = 0;
    	for(int lim=b.length-1; lim>=0; lim--)
    	{
    		pstack src = b[lim];
    		np += src.size();
    	}
    	if(np>0)
    	{
    	int nth = r==null?-1:r.nextInt(np);	// if no random, get them all
    	for(int lim=b.length-1; lim>=0; lim--)
    	{
    		pstack src = b[lim];
    		int ss = src.size();
    		if(ss>0)
    		{
    		if(np>nth) 
    		{	piece p = src.topPiece();
    			String str = ""+p.piecenumber;
    			all.push(new plateaumove(MOVE_EXCHANGE,str,who));
    			if(nth>=0) { return; }	
    		}}
    	}}    	
    }
    private void addRackDropMoves(CommonMoveStack all,int who)
    {
    	pstack rs[] = rack[who];
    	for(pstack d : rs)
    	{	all.push(new plateaumove("Drop " + d.stacknumber+" 100 " + " " + d.locus(),who));
    	}
    }
    // return a temporary stack which contains all the
    // captives we have available.
    private pstack allTradeGoods(int who)
    {
    	 pstack v = new pstack(this, UNKNOWN_ORIGIN);
         // make v the union of bar and trade
         for (int i = 0; i < bar[who].length; i++)
         {
             v.addCopyStack(bar[who][i]);
             v.addCopyStack(trade[who][i]);
         }	
         return(v);
    }

    // given "ask" is the total being exchanged, determine how many points
    // we have to offer.  We have to offer a "price is right" total which
    // is as large as possible without exceeding the asked total.
    public int largestSubsetBelow(int ask, int who)
    {
       pstack v = allTradeGoods(who);

        // find the highest value subset whole value is <= ask.  This is 
        // the smallest value we can offer in exchange
        int max = 0;
        for (int i = 0; i < (1 << v.size()); i++)
        {   int ss = v.sumSubset(i);
            if ((ss <= ask) && (ss > max))
            {    max = ss;
            }
        }

        return (max);
    }
 
    // respond with a complete set all at once.
    private void addExchangeResponseMoves(CommonMoveStack all,int who)
    {	pstack v = allTradeGoods(who);
    	int underpay = 0;
    	int offer = pointTotal(trade[nextPlayer[whoseTurn]]);
    	int minacceptable = largestSubsetBelow(offer, whoseTurn);
    	for(int i=1;i<(1<<v.size());i++)
    	{
    		int subtotal = v.sumSubset(i);
    		if(subtotal>=minacceptable)
    		{	String str = v.pickSubset(i);
    			all.push(new plateaumove(MOVE_EXCHANGE,str,who));
    			if(subtotal<=offer) { underpay++; }
    		}
    	}
    	if(underpay==0) 
    	{	// if all the proposed payments were more than was offered, we can refuse
    		all.push(new plateaumove(MOVE_DONE,who));
    	}
    }
    
    private double cellOnboardWeight(pstack cell,int opponentStackHeight,Face top,int extraHeight)
    {
  		return(0);
    }
    private boolean addOrangeFinalMoves(CommonMoveStack all,int who,int movingHeight,pstack mid,int dx,int dy,int opponentHeight)
    {
    	char x = (char)(mid.col_a_d+dx);
    	int y = mid.row_1_4+dy;
    	if(validBoardPos(x,y))
    	{	if(all!=null)
    		{
    		pstack fin = getCell(x,y);
    		double weight = cellOnboardWeight(mid,opponentHeight,Face.Orange,movingHeight);
    		plateaumove newMove = new plateaumove(MOVE_FROMTO,mid.locus(),(mid.size()-movingHeight),mid.allColors(movingHeight),weight,fin.locus(),who);
    		all.push(newMove);
    		}
    		return(true);
    	}
    	return(false);
    }
    private boolean addOrangeIntermediateMoves(CommonMoveStack all,int who,pstack from,int movingHeight,int dx,int dy,int opponentHeight)
    {	boolean some = false;
    	char x = (char)(from.col_a_d+dx);
    	int y = from.row_1_4+dy;
    	if(validBoardPos(x,y))
    	{	boolean valid = false;
    		pstack midCell = getCell(x,y);
    		if(dy==0)
    		{
    			valid |= addOrangeFinalMoves(all,who,movingHeight,from,dx*2,+1,opponentHeight);
    			if(valid && (all==null)) { return(true); }
    			valid |= addOrangeFinalMoves(all,who,movingHeight,from,dx*2,-1,opponentHeight);
    			if(valid && (all==null)) { return(true); }
    		}
    		else
    		{
    			valid |= addOrangeFinalMoves(all,who,movingHeight,from,+1,dy*2,opponentHeight);
    			if(valid && (all==null)) { return(true); }
    			valid |= addOrangeFinalMoves(all,who,movingHeight,from,-1,dy*2,opponentHeight);
    			if(valid && (all==null)) { return(true); }
    		}
    		if(valid)	// the endpoint is on the board, so we can potentially drop in the intermediate cell
    		{	
    			int height = from.size()-1;
    			for(int h = movingHeight; h>0; h--)
				{
    				if((midCell.size()==0)||(midCell.topOwner()==who)||from.containsColor(h,height))
    				{
        				// must be taking a non-mute to pin
    					if(all==null) { return(true); }
    					double weight = cellOnboardWeight(midCell,opponentHeight,Face.Blank,movingHeight);	
         				all.push(new plateaumove(MOVE_FROMTO,from.locus(),(from.size()-movingHeight),from.allColors(movingHeight),weight,midCell.locus(),who));
    				}
				}
    			
    		}
    	}
    	return(some);
    }
    private boolean addOrangeContinuationMoves(CommonMoveStack all,int who,pstack cell,int movingHeight,int dx,int dy,int opponentHeight)
    {	boolean some = false; 
    	if(dx==0)
		{
    	if(addOrangeFinalMoves(all,who,movingHeight,cell,1,dy,opponentHeight)) { if(all==null) { return(true); } some=true; }   
		some |= addOrangeFinalMoves(all,who,movingHeight,cell,-1,dy,opponentHeight);
		}
		else {
			if(addOrangeFinalMoves(all,who,movingHeight,cell,dx,1,opponentHeight)) { if(all==null) { return(true); } some=true; }
			some |= addOrangeFinalMoves(all,who,movingHeight,cell,dx,-1,opponentHeight);					
		}
    	return(some);
    }

    private boolean addNormalMoves(CommonMoveStack all,int who,pstack from,int maxdistance,int dx,int dy,piece top,int movingHeight,int opponentHeight)
    {	
    	int fromHeight = from.size();
    	int ox = from.col_a_d;
    	int oy = from.row_1_4;
     	boolean some = false;
		{
			// move a stack of "movingHeight" chipStack, maxDistance or less
		  	boolean canMoveFurther = false;			// we can make intermediate stops only if we can move further with this stack

		  	for(int distance = maxdistance; distance>0; distance--)
			{
    		char x = (char)(ox + distance*dx);
    		int y = oy + distance*dy;
    		if(validBoardPos(x,y))
    		{	pstack target = getCell(x,y);
    			int targetHeight = target.size();
    			
    			if(	(targetHeight==0) 				// destination is empty
    				|| (target.topOwner()==who) 	// or destination is ours
    				|| !top.isMute() 				// or our top is colored
    				|| (canMoveFurther && from.containsColor(fromHeight-movingHeight,fromHeight-1))
    				)
    			{	if(all==null) { return(true); }
    				canMoveFurther = true;
    				int level = from.size()-movingHeight;
    				String ll = from.locus();
    				String tl = target.locus();
    				String colors = from.allColors(movingHeight);
    				double weight = cellOnboardWeight(target,opponentHeight,top.realTopColor(),movingHeight);	
    				all.push(new plateaumove(MOVE_FROMTO,ll,level,colors,weight,tl,who));
    				some = true;
    			}
    		}
    	}
		}
		return(some);
    }
    boolean isEdgeCell(pstack c)
    {
    	return c.col_a_d == 'A' 
    			|| c.col_a_d == (char)('A'+ncols-1)
    			|| c.row_1_4 == 1
    			|| c.row_1_4 == nrows;
    }
    private void addInitialDropMoves(CommonMoveStack all,int who)
    {	boolean first = (who == FIRST_PLAYER_INDEX);

		for(pstack cell = allCells; cell!=null; cell=cell.next)
			{
			if((cell.size()==0) && isEdgeCell(cell) && (first ? cell.row_1_4==1 : true))
				{
				pstack st = movingStack[moveStep];
				String str = "Onboard "+cell.locus()+" 100 "+st.allColors()+" "+st.pieces();
				all.push(new plateaumove(str,who));
				}
			}
    	//return ((p.size() == 0) && isEdgeSquare(p.row_1_4, p.col_a_d));
    }
    
    private boolean addInboardMoves(CommonMoveStack all,int who,boolean includeFlip,boolean includeRedundentFlip)
    {	boolean some = false;
    	int opponentHeight = maximumStackHeight(nextPlayer[who]);
    	CellStack candidates = buildOnboardTargets(who);
    	for(int lim=candidates.size()-1; lim>=0; lim--)
     		{
    		pstack cell = candidates.elementAt(lim);
    		if(cell.topOwner()==who)
    			{
    			some |= addInboardMoves(all,who,includeFlip,includeRedundentFlip,cell,opponentHeight);
    			if(some && (all==null)) { return(true); }
    			}
    		}
    	
    	return(some);
    }
    /**
     * build a stack of targets for onboard moves, annotate the
     * stacks with red_thread blue orange_threat blank_threat for the number of threats
     * we issue, and  incoming_threats for the opponents that could be menacing
     * @param who
     * @return
     */
    private CellStack buildOnboardTargets(int who)
    {	CellStack targets = new CellStack();
    	G.Error("not");
    	return(targets);
    }

    private boolean addInboardMoves(CommonMoveStack all,int who,boolean includeFlip,boolean includeReduddentFlip,pstack cell,int opponentHeight)
    {	boolean some = false;
    	int takeoff = cell.takeOffHeight();
    	int height = Math.min(3,takeoff);	// max distance to move
    	
    	if(height>0)
		{    	
		piece top = cell.topElement();
		Face topFace =  top.realTopColor();
		if(cell.stompCapture())
			{	if(all==null) { return(true); }
				double weight = cellOnboardWeight(cell,opponentHeight,topFace,takeoff);
				all.push(new plateaumove(MOVE_FROMTO,cell.locus(),(cell.size()-takeoff),cell.allColors(takeoff),weight,cell.locus(),who));
			}
    	some |= addInboardMoves(all,who,includeFlip,includeReduddentFlip,cell,height,top,opponentHeight);
    	if(some && (all==null)) { return(some); }
		}
    	return(some);
    }
    
    private boolean addInboardMoves(CommonMoveStack all,int who,boolean includeFlip,boolean includeRedudnentFlip,pstack cell,int distance,piece top,int opponentHeight)
    {	boolean some = false;
		if(includeFlip && (includeRedudnentFlip || ( top.bottomIsUnknown()) || !top.isMonoColor()))
			{	// Flip 23 A2 B
				if(all==null) { return(true); }
				all.push(new
				 plateaumove("Flip "+top.piecenumber+" "+cell.locus()+" "+top.topColorString(),who));
			}

		for(int movingHeight=1; movingHeight<=cell.takeOffHeight(); movingHeight++)
			{ some|= addInboardMoves(all,who,cell,distance,top,movingHeight,opponentHeight);
			  if(some && (all==null)) { return(some); }
			}
		return(some);

    }
    private void addContinuationMoves(CommonMoveStack all,int who,pstack cell,int distance,piece top,int movingHeight,int dx, int dy,int opponentHeight)
    {	Face topcolor = top.realTopColor();
		switch(topcolor)
			{
			default: throw G.Error("Not expecting "+topcolor);
			case Orange:
				addOrangeContinuationMoves(all,who,cell,movingHeight,dx,dy,opponentHeight);
				break;
			case Blue:
			case Red:
			case Blank:
				addNormalMoves(all,who,cell,distance,dx,dy,top,movingHeight,opponentHeight);
				break;
			}
	}
    // dropoffs after an initial pick
    private void addContinuationMoves(CommonMoveStack all,int who)
    {
    	pstack orig = copyOriginalStack[0]; // where we all started, determines distance
    	pstack step = copyOriginalStack[moveStep];
    	pstack moving = movingStack[moveStep];
    	piece top = moving.topElement();
    	int xsofar =step.col_a_d-orig.col_a_d;
    	int ysofar = step.row_1_4-orig.row_1_4;
    	int dissofar = Math.max(Math.abs(xsofar),Math.abs(ysofar));
    	int dx = Integer.signum(xsofar);
    	int dy = Integer.signum(ysofar);
    	addContinuationMoves(all,who,movingStack[moveStep],orig.takeOffHeight()-dissofar,top,moving.size(),dx,dy,0);
    	
    }
    private void addDropMoves(CommonMoveStack all,int who)
    {	pstack ps = movingStack();
    	piece p = ps.topElement();
    	Face top = p.realTopColor();
    	Face bottom = p.realBottomColor();
    	// never deploy the orange showing.
    	if(p.realTopColor()!=Face.Orange) 
    		{ buildOnboardMoves(all,p,top,bottom,0,who);
    		}
    	if(!p.isMonoColor() && (p.realBottomColor()!=Face.Orange)) 
    		{ buildOnboardMoves(all,p,bottom,top,0,who); 
    		}
    }
    
    private void buildOnboardMoves(CommonMoveStack all,piece p,Face top,Face bottom,int opponentStackHeight,int who)
    {	int opponent = nextPlayer[who];
		String colors = top.shortName+bottom.shortName;
   		for(pstack cell = allCells; cell!=null; cell=cell.next)
    		{
    		if(cell.topOwner()!=opponent)
    		{
    		int height = cell.size();
    		double w = cellOnboardWeight(cell,opponentStackHeight,top,1);

     			{
    			String pnum = ""+p.piecenumber;
    			String loc = cell.locus();
    			plateaumove topMove = new plateaumove(loc,100,colors,w,colors,pnum,who);
    			all.push(topMove);		// on top
    			double insideW = cellOnboardWeight(cell,opponentStackHeight,top,1);
     			for(int lim = height-1;lim>=0;lim--)
    				{
     				if(cell.elementAt(lim).owner==who)
     					{
     					plateaumove newmove = new plateaumove(loc,lim,colors,insideW,colors,pnum,who);
     					all.push(newmove);
     					}
     				else { lim=0; }	// reached an opponent piece
    				}
    			}
    		}
    	}
    }

    private void addDoneMove(CommonMoveStack all,int who)
    {
    	all.push(new plateaumove(MOVE_DONE,who));
    }
    private void addOnboardMoves(CommonMoveStack all,int who)
    {	pstack pieces[] = rack[who];
    	int typeMap = 0;
    	int opponentStackHeight = maximumStackHeight(nextPlayer[who]);
    	CellStack candidates = buildOnboardTargets(who);
    	for(pstack stack : pieces)
    	{	
    		for(int lvl=0; lvl<stack.size() ; lvl++)
    		{
    			piece p = stack.elementAt(lvl);
    			int pieceBit = (1<<p.realPieceType);
    			if((pieceBit & typeMap)==0)
    			{
    				typeMap |= pieceBit;
    				Face top = p.realTopColor();
    				Face bottom = p.realBottomColor();
    				// never deploy the orange as visible
    				if(top!=Face.Orange) 
    					{ buildOnboardMoves(all,candidates,p,top,bottom,opponentStackHeight,who); }
    				if(!p.isMonoColor() && (bottom!=Face.Orange)) 
    					{ buildOnboardMoves(all,candidates,p,bottom,top,opponentStackHeight,who); }
    			}
    		}
    	}
    }

    // dropoffs after an initial pick
    private void addInBoardDrops(CommonMoveStack all,int who)
    {
    	pstack orig = copyOriginalStack[0]; // where we all started, determines distance
    	pstack moving = movingStack[moveStep];
    	piece top = moving.topElement();
    	addInboardMoves(all,who,orig,orig.takeOffHeight(),top,moving.size(),0);    	
    }
 
    
    int maximumStackHeight(int player)
    {	int maxh = 0;
        // check for win by stacking 6
    	for(pstack stack = allCells; stack!=null; stack=stack.next)
    	{
    		int s = stack.controlledStackHeight(player);
             if(s>maxh) { maxh=s; }
    	}
    	return(maxh);
    }
    private void buildOnboardMoves(CommonMoveStack all,CellStack candidates,piece p,Face top, Face bottom,
			int opponentStackHeight,int who)
{	int opponent = nextPlayer[who];
	String colors = top.shortName+bottom.shortName;
	for(int clim = candidates.size()-1; clim>=0; clim--)
	{	pstack cell = candidates.elementAt(clim); 
		if(cell.topOwner()!=opponent)
		{
		int height = cell.size();
		double w = cellOnboardWeight(cell,opponentStackHeight,top,1);
		String pnum = ""+p.piecenumber;
		String loc = cell.locus();
		plateaumove topMove = new plateaumove(loc,100,colors,w,colors,pnum,who);
		all.push(topMove);		// on top
		if(height>0)
		{
		Face oldTop = cell.topFace();
		double oldW = cellOnboardWeight(cell,opponentStackHeight,oldTop,1);
			for(int lim = height-1;lim>=0;lim--)
			{
				if(cell.elementAt(lim).owner==who)
					{
					plateaumove newmove = new plateaumove(loc,lim,colors,oldW,colors,pnum,who);
					all.push(newmove);
					}
				else { lim=0; }	// reached an opponent piece
			}
		}
		}
	}
}  


    private boolean addInboardMoves(CommonMoveStack all,int who,pstack cell,int distance,piece top,int movingHeight,int opponentHeight)
    {	Face topcolor = top.realTopColor();
    	boolean some = false;
		switch(topcolor)
			{
			default: throw G.Error("Not expecting "+topcolor);
			case Orange:
				{
					some |= addOrangeIntermediateMoves(all,who,cell,movingHeight,1,0,opponentHeight);
					if(some && (all==null)) { return(true); }
					some |= addOrangeIntermediateMoves(all,who,cell,movingHeight,-1,0,opponentHeight);
					if(some && (all==null)) { return(true); }
					some |= addOrangeIntermediateMoves(all,who,cell,movingHeight,0,1,opponentHeight);
					if(some && (all==null)) { return(true); }
					some |= addOrangeIntermediateMoves(all,who,cell,movingHeight,0,-1,opponentHeight);
					if(some && (all==null)) { return(true); }
				}
				break;
			case Blue:
				some |= addNormalMoves(all,who,cell,distance,1,1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,distance,1,-1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,distance,-1,1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,distance,-1,-1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				break;
			case Blank:
				some |= addNormalMoves(all,who,cell,distance,1,1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,distance,1,-1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,distance,-1,1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,distance,-1,-1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				// and add red moves
				//$FALL-THROUGH$
			case Red:
				some |= addNormalMoves(all,who,cell,distance,1,0,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,distance,-1,0,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,distance,0,1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,distance,0,-1,top,movingHeight,opponentHeight);
				if(some && (all==null)) { return(true); }
			
			}
		return(some);
	}
 private void buildInitialMoves(CommonMoveStack all,int who,
    		piece first,String firstTopColor,String firstBottomColor,
    		piece second,String secondTopColor,String secondBottomColor)
    {		
    	for(pstack cell = allCells; cell!=null; cell=cell.next)
    		{
    		if((cell.size()==0) && isEdgeCell(cell))
    		{	
    			all.push(new plateaumove( cell.locus(),
    										100, 
    										firstTopColor+firstBottomColor+secondTopColor+secondBottomColor,
    										1.0,
    										firstTopColor+firstBottomColor,
    										second.piecenumber+","+first.piecenumber,
    										who));
    		}
    	    	
    	}
    }	
private void addInitialMoves(CommonMoveStack all,int whoseTurn)
{	pstack pieces[] = rack[whoseTurn];
	int ntypes = pieces.length;
	for(int i1=0;i1<ntypes;i1++)
	{	pstack firstPiece = pieces[i1];
		piece firstTop = firstPiece.topElement();
		Face firstTopFace = firstTop.realTopColor();
		for(int i2 = i1; i2<ntypes; i2++)
		{
			pstack secondPiece = pieces[i2];
			piece secondTop = secondPiece.topElement();
			
			if((firstPiece!=secondPiece) || (firstPiece.size()>1))
			{	String firstTopColor = firstTop.realTopColorString();
				String firstBottomColor = firstTop.realBottomColorString();
				String secondTopColor = secondTop.realTopColorString();
				String secondBottomColor = secondTop.realBottomColorString();
				if(firstTopFace!=Face.Orange)
				{
				buildInitialMoves(all,whoseTurn,firstTop,firstTopColor,firstBottomColor,
							secondTop,secondTopColor,secondBottomColor);
				}
				if(!firstTop.isMonoColor())
				{
					buildInitialMoves(all,whoseTurn,firstTop,firstBottomColor,firstTopColor,
							secondTop,secondTopColor,secondBottomColor);
					if(!secondTop.isMonoColor())
					{
						buildInitialMoves(all,whoseTurn,firstTop,firstBottomColor,firstTopColor,
    							secondTop,secondBottomColor,secondTopColor);
					}
				}
				if(!secondTop.isMonoColor())
				{	buildInitialMoves(all,whoseTurn,firstTop,firstTopColor,firstBottomColor,
						secondTop,secondBottomColor,secondTopColor);
				}
				}
 			}
		}
}
	public CommonMoveStack GetListOfMoves(Random robotRandom,boolean includeRedundentFlip) 
	{	CommonMoveStack all = new CommonMoveStack();
		switch(board_state)
		{
		default:throw  G.Error("Not expecting "+board_state);
		
		case EXCHANGE_STATE:
			addExchangeResponseMoves(all,whoseTurn);
			break;
		case PLAY_DROP_STATE:
			if(moveStep==0) { addInBoardDrops(all,whoseTurn); }
			else { addContinuationMoves(all,whoseTurn); }
			break;
		case PLAY_DONE_STATE:
		case PLAY_CAPTURE_STATE:
		case ONBOARD2_DONE_STATE:
		case ONBOARD_DONE_STATE:
		case EXCHANGE_DONE_STATE:
		case DRAW_STATE:
			addDoneMove(all,whoseTurn);
			break;
		case ONBOARD2_DROP_STATE:
			addInitialDropMoves(all,whoseTurn);
			break;
		case RACK_DROP_STATE:
		case RACK2_DROP_STATE:
			addRackDropMoves(all,whoseTurn);
			break;
		case ONBOARD_DROP_STATE:
			addDropMoves(all,whoseTurn);
			break;
		case ONBOARD2_STATE:
			addInitialMoves(all,whoseTurn);
			break;
		case FLIPPED_STATE:
			G.Assert(the_flipped_stack.topOwner()==whoseTurn,"wrong flipper");
			addInboardMoves(all,whoseTurn,false,false,the_flipped_stack,maximumStackHeight(nextPlayer[whoseTurn]));
			if(all.size()==0)	// if the robot flipped something, it's possible to be completely blocked.
				{ all.push(new plateaumove(MOVE_PASS,whoseTurn)); 
				}
			break;
		case CAPTIVE_SHUFFLE_STATE:
			addExchangeMoves(all,robotRandom,whoseTurn);
			all.push(new plateaumove(MOVE_DONE,whoseTurn));
			break;
		case PLAY_STATE:
			{
			addOnboardMoves(all,whoseTurn);
			addInboardMoves(all,whoseTurn,true,false);
			addExchangeMoves(all,robotRandom,whoseTurn);
			}
			if(all.size()==0) 
			{	all.push(new plateaumove(MOVE_PASS,whoseTurn)); 
			}
		}
		return all;
	}

}