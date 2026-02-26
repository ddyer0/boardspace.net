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
    
    TODO: allow "done" and change the prompt when no moves are possible.
    TODO: make the robot smarter about trades, not offer completely extra pieces
    TODO: suppress the wandering picked piece when the bot is pondering the drop
    
 */
package plateau.common;

import lib.Graphics;

import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import lib.AR;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.Tokenizer;
import lib.Random;
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
	static int REVISION = 101;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	private int totalPoints = 0;
	public int minimumTradeOffer[] = new int[2];
	private PlateauState unresign;
	private PlateauState board_state;
	public int placementIndex = -1;
	public boolean useDelays = false;
	public int delay_count = 0;
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
    
    public String gameType() { return(G.concat(gametype," ",2," ",randomKey," ",revision)); }

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
    pstack cellArray[] = null;
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
    pstack[] pickedStack;		// the stack that was picked
    pstack[] copyOriginalStack; // copy of the stack before pick/drop
    pstack[] movingStack; 		// the temporary stack that was split off
    piece droppedPiece = null; 	// the bottom piece on the stack piece we just dropped
    private pstack the_flipped_stack = null; // the board stack we flipped
    private piece the_flipped_piece = null;
    private long onBoardTimer = 0;
    public int[] captivePoints = new int[2];
    public int[] captiveCount = new int[2];
    public int[] boardCount = new int[2];
    private int turnsSinceCapture = 0;
    public PlateauBoard(String init,long k,int map[],int rev) // default constructor
    {  
    	setColorMap(map, 2);
        doInit(init,k,2,rev);
    }
    public PlateauBoard cloneBoard() 
	{ PlateauBoard dup = new PlateauBoard(gametype,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol p)
    {	copyFrom((PlateauBoard)p);
    }
    public piece getPiece(int n)
    {
        return (pieces[n]);
    }
    public piece getPiece(piece p)
    {
    	if(p==null) { return null; }
    	return getPiece(p.piecenumber);
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

    public pstack getStack(int n)
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
    public int pieceTotal(pstack[] rr)
    {
        int sum = 0;

        for (int i = 0; i < rr.length; i++)
        {
            sum += rr[i].size();
        }

        return (sum);
    }
    // draw text between the bar and trade with information about the trade
    public void DrawExchangeSummary(Graphics gc, int idx, Rectangle r,NumberMenu numberMenu)
    {
        pstack[] tr = trade[idx];
        int tot = pointTotal(tr);
        InternationalStrings s = G.getTranslations();
        int left =G.Left(r);
        int top = G.Top(r);
        int width = G.Width(r);
        int height = G.Height(r);
        if (tot > 0)
        {
            String msg = s.get(PointsMessage,tot);
            GC.Text(gc,false,left,top,width,height, Color.black, null,
                msg);
        }
        if((idx==whoseTurn) && (board_state==PlateauState.EXCHANGE_STATE || board_state==PlateauState.EXCHANGE_DONE_STATE))
        {
        int offer = minimumTradeOffer[idx];
        if(offer>=0)
        {
        	GC.TextRight(gc,left,top,width,height, Color.black, null,s.get(MinPointsMessage,offer));
        }}

    }

    // draw the main reserve rack
    public void DrawRack(Graphics gc, Rectangle rect, int index,
        HitPoint highlight,NumberMenu numberMenu)
    {
    	drawPstacks(gc, index>=0?rack[index]:null, rect, highlight, "",numberMenu);
 
    }

    // constructors for basic board setup
    private piece newpiece(PieceType p,int idx, int index, long rv)
    {
        piece pp = new piece(this,p,idx, index,rv);
        pieces[index] = pp;

        return (pp);
    }

    private pstack newpstack(int origin, int idx, int owner,char col,int row)
    {	
        pstack ps = new pstack(this, origin, idx, owner,col,row);
        stacks[idx] = ps;

        return (ps);
    }

    private void Init_Standard(Variation v)
    {
        int stacknumber = 0;
        int piecenumber = 0;
        totalPoints = 0;
        variation = v;
        int counts[] = variation.pieceCounts;
        if(ncols!=v.boardSize)
        {
        ncols = nrows = v.boardSize;
        board = new pstack[nrows][ncols];
        cellArray = new pstack[nrows*ncols];
        stacks = new pstack[(nrows * ncols) + (6 * NPIECETYPES)];
        movingStackOrigin = new pstack[nrows]; 	// the stack that was picked/dropped
        copyOriginalStack = new pstack[nrows]; 	// copy of the stack before pick/drop
        pickedStack = new pstack[nrows];
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
        
        allCells = null;
        int cellIdx = 0;
        for (int col = ncols-1; col >=0 ; col--)
        {
            for (int row = 0; row < nrows; row++)
            {
                pstack ps = newpstack(BOARD_ORIGIN, stacknumber++, -1,(char)('A'+row),col+1);
                board[row][ncols-col-1] = ps;
                cellArray[cellIdx++] = ps;
                ps.next = allCells;
                allCells = ps;
            }
        }
 
        
        Random pr = new Random(1035356);
        for (int idx = FIRST_PLAYER_INDEX; idx <= SECOND_PLAYER_INDEX; idx++)
        {	totalPoints = 0;
            for (int j = 0; j < NPIECETYPES; j++)
            {
                bar[idx][j] = newpstack(CAPTIVE_ORIGIN, stacknumber++, idx,'@',j);
                trade[idx][j] = newpstack(TRADE_ORIGIN, stacknumber++, idx,'@',j);
            }

            pstack s = rack[idx][PieceType.Mute.index] = newpstack(RACK_ORIGIN, stacknumber++, idx,'@',0);
            for (int n = 0; n < counts[PieceType.Mute.index]; n++)
            {	totalPoints += PieceType.Mute.value;
                newpiece(PieceType.Mute,idx, piecenumber++, pr.nextLong()).addToStack(s);
            } //4 mutes

            s = rack[idx][PieceType.Red.index] = newpstack(RACK_ORIGIN, stacknumber++, idx,'@',0);

            for (int n = 0; n < counts[PieceType.Red.index]; n++)
            {	totalPoints += PieceType.Red.value;
                newpiece(PieceType.Red,idx, piecenumber++, pr.nextLong()).addToStack(s); // 2 reds
            }

 
            s = rack[idx][PieceType.Blue.index] = newpstack(RACK_ORIGIN, stacknumber++, idx,'@',0);

            for (int n = 0; n < counts[PieceType.Blue.index]; n++)
            {	totalPoints += PieceType.Blue.value;
                newpiece(PieceType.Blue,idx, piecenumber++, pr.nextLong()).addToStack(s);// 2 blues
            }
            
            s = rack[idx][PieceType.RedMask.index] = newpstack(RACK_ORIGIN,    stacknumber++, idx,'@',0);
            for (int n = 0; n < counts[PieceType.BlueMask.index]; n++)
            {	totalPoints += PieceType.RedMask.value;
            	newpiece(PieceType.RedMask,idx, piecenumber++, pr.nextLong()).addToStack(s);
            }

            s = rack[idx][PieceType.BlueMask.index] = newpstack(RACK_ORIGIN, stacknumber++, idx,'@',0);
            
            for (int n = 0; n < counts[PieceType.BlueMask.index]; n++)
            {
            totalPoints += PieceType.BlueMask.value;
            newpiece(PieceType.BlueMask,idx, piecenumber++, pr.nextLong()).addToStack(s);
            }
            
            s = rack[idx][PieceType.Twister.index] = newpstack(RACK_ORIGIN, stacknumber++, idx,'@',0);
            for (int n = 0; n < counts[PieceType.Twister.index]; n++)
            {
            totalPoints += PieceType.Twister.value;
            newpiece(PieceType.Twister,idx, piecenumber++, pr.nextLong()).addToStack(s);
            }

            s = rack[idx][PieceType.Ace.index] = newpstack(RACK_ORIGIN, stacknumber++, idx,'@',0);
            for (int n = 0; n < counts[PieceType.Ace.index]; n++)
            {
            totalPoints += PieceType.Ace.value;
            newpiece(PieceType.Ace,idx, piecenumber++, pr.nextLong()).addToStack(s);
            }
        }

        whoseTurn = FIRST_PLAYER_INDEX;
    }
    int ncap = 0;
    private double sumValues(pstack pieces[])
    {	
    	double v = 0;
    	for(pstack s : pieces)
    	{
    		for(int lim=s.size()-1; lim>=0; lim--)
    		{	ncap++;
    			v += s.elementAt(lim).pieceType.value;
    		}
    	}
    	return v;
    }
    private double captureScale()
    {	// downgrade value of height if we're not capturing
    	return turnsSinceCapture/4.0;
    }
    public double simpleScore(int who,boolean second)
    {	ncap = 0;
    	double v = sumValues(bar[who]) + sumValues(trade[who]);
    	int tallest = 1;
    	for(pstack c = allCells; c!=null; c=c.next)
    	{
    		if(c.topOwner()==who && c.size()>tallest && c.elementAt(0).unobstructed())
    		{
    			tallest = c.size();
    		}
    	}
    	double tallmax = (second ? 4 : 5)+captureScale();
    	double cappoints = Math.sqrt(Math.min(ncap,6)/6.0);
    	double tallpoints = Math.sqrt(Math.min(tallmax,tallest)/tallmax);
    	return Math.max(cappoints,Math.max(v/totalPoints,tallpoints));
    }
    public void revealAll()
    {
        anonymized_for = -1;

        for (int i = 0; i < pieces.length; i++)
        {
            pieces[i].revealAll();
        }
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

        for(int lim=cellArray.length-1; lim>=0; lim--)
        {
        	pstack bs = cellArray[lim];

        	if (bs.topOwner() == ind)
                {
                    int h = bs.takeOffHeight();
                    onboard += h;
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
    public void doInit(String gtype,long key)
    {
    	Tokenizer tok = new Tokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? tok.intToken() : players_in_game;
    	long ran = tok.hasMoreTokens() ? tok.longToken() : key;
    	int rev = tok.hasMoreTokens() ? tok.intToken() : revision;
    	doInit(typ,ran,np,rev);
    }

    public void doInit(String gtype, long key, int np, int rev)
    {	randomKey = key;
    	adjustRevision(rev);
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
        turnsSinceCapture = 0;
        placementIndex = 1;
        AR.setValue(minimumTradeOffer,0);
        setState(PlateauState.ONBOARD2_STATE);

        // note that firstPlayer is NOT initialized here
    }

    public void  SetDrawState()
    {	setState(PlateauState.DRAW_STATE);
    }

    /** return true if x,y is a valid board position(i.e. if x,y is on the board */
    private final boolean validBoardPos(int inX, int inY)
    {	//G.Assert(inX<'A' && inY<'A',"invalid input");
        return (!((inX < 0) || (inX >= ncols) || (inY < 0) || (inY >= nrows)));
    }

    public final pstack getBoardXY(int x, int y)
    {
        if (!validBoardPos(x, y))
        {
        	throw G.Error("Get invalid %s,%s" , x , y);
        }

        return (board[x][y]);
    }

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

        return (getBoardXY(col - 'A', row - 1));
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
        v ^= Digest(r,revision);
        v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
       return (v);
    }
    public pstack getStack(pstack from)
    {	if(from==null) { return null;}
    	if(from.isCopy) { return new pstack(from,pieces); }
    	return getStack(from.stackNumber);
    }
    public void getStack(pstack to[],pstack from[])
    {
    	//G.Assert(to.length==from.length,"size mismatch");
    	for(int i=0;i<to.length;i++)
    	{
    		to[i] =getStack(from[i]);
    	}
    }
    /* make a copy of a board.  The robot needds this */
    public void copyFrom(PlateauBoard from_b)
    {	
    	from_b.checkStacks();
    	super.copyFrom(from_b);

        // copy the pieces
        for(int i=0;i<pieces.length;i++) 
    	{ pieces[i].copyFrom(from_b.pieces[i]); 
    	}

        // copy the board cells
        for (int row = 0; row < nrows; row++)
        {
            for (int col = 0; col < ncols; col++)
            {
                board[col][row].copyFrom(from_b.board[col][row]);
            }
        }
        // copy the racks and bars
        for (int i = 0; i < NPIECETYPES; i++)
        {	
            rack[FIRST_PLAYER_INDEX][i].copyFrom(from_b.rack[FIRST_PLAYER_INDEX][i]);
            rack[SECOND_PLAYER_INDEX][i].copyFrom(from_b.rack[SECOND_PLAYER_INDEX][i]);
            bar[FIRST_PLAYER_INDEX][i].copyFrom(from_b.bar[FIRST_PLAYER_INDEX][i]);
            bar[SECOND_PLAYER_INDEX][i].copyFrom(from_b.bar[SECOND_PLAYER_INDEX][i]);
            trade[FIRST_PLAYER_INDEX][i].copyFrom(from_b.trade[FIRST_PLAYER_INDEX][i]);
            trade[SECOND_PLAYER_INDEX][i].copyFrom(from_b.trade[SECOND_PLAYER_INDEX][i]);
        }
        //
        // the rest are either duplicates of the real board state, or shared pointers.
        getStack(movingStackOrigin,from_b.movingStackOrigin);
        getStack(pickedStack,from_b.pickedStack);
        getStack(copyOriginalStack,from_b.copyOriginalStack);
        getStack(movingStack,from_b.movingStack);
        droppedPiece = getPiece(from_b.droppedPiece);
        AR.copy(minimumTradeOffer,from_b.minimumTradeOffer);
        the_flipped_stack = getStack(from_b.the_flipped_stack);
        the_flipped_piece = getPiece(from_b.the_flipped_piece);
        moveStep = from_b.moveStep;
        board_state = from_b.board_state;
        turnsSinceCapture = from_b.turnsSinceCapture;
        unresign = from_b.unresign;
        useDelays = from_b.useDelays;
        delay_count = from_b.delay_count;
        if(G.debug()) { sameboard(from_b); }
        checkStacks();     
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
    		||((board_state==PlateauState.CAPTIVE_SHUFFLE_STATE) 
    				&& (pointTotal(trade[whoseTurn])>minimumTradeOffer[whoseTurn])));
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	// simultaneous win is not a consideration
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(PlateauState.GAMEOVER_STATE);
    }
    public int numberOfPrisoners(int player)
    {
        pstack[] captures = bar[player];
        int ncaps = 0;
        for (int i = 0; i < captures.length; i++)
        {
            ncaps += captures[i].size();
        }
        return ncaps;
    }
    /* return true if the player has won */
    public boolean WinForPlayerNow(int player)
    {
        if (win[player])
        {
            return (true);
        }

        // check for win by capturing 6
        int ncaps = numberOfPrisoners(player);
 
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
    {	switch(board_state)
    	{
    	case GAMEOVER_STATE:
    		return true;
    	case PLAY_STATE:
    	case PLAY_NOEXCHANGE_STATE:
    		return (WinForPlayerNow(FIRST_PLAYER_INDEX) ||
    	            WinForPlayerNow(SECOND_PLAYER_INDEX));
    	default: return false;
    	}

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
      
        if (!robotBoard && (mv!=null) && mv.origin == BOARD_ORIGIN)
        {	// robot doesn't move back
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

                        setNextStateAfterDrop(which,(level == DO_NOT_CAPTURE));
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
                    movingStackOrigin[moveStep] = which;
                    copyOriginalStack[moveStep] = new pstack(which,pieces);
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
            piece p = getPiece(str.intToken());
            realorig = p.mystack;
            p.anonymize();
            p.setTopKnown(whoseTurn);
            p.setBottomKnown(whoseTurn);
            orig = new pstack(realorig,pieces);
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
        pickedStack[0] = new pstack(res,pieces);
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
            case PLAY_NOEXCHANGE_STATE:
            	 
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
        case PLAY_NOEXCHANGE_STATE:
        case PLAY_STATE: // initial states, ignore dropped stack from previous gestures
            movingStackOrigin[0] = pp.mystack;
            copyOriginalStack[0] = new pstack(pp.mystack,pieces);
			//$FALL-THROUGH$
		case ONBOARD2_DONE_STATE:
        	{
            pstack newstack = pp.makeNewStack(); //split the stack
            pickedStack[0] = new pstack(this,PICKED_ORIGIN).addCopyStack(newstack);
            newstack.origin = movingStackOrigin[0].origin;	//preserve the original location
            movingStack[0] = newstack; //make a new stack, and don't change movingStackOrigin
            moveStep = 0;
        	}
            break;

        case ONBOARD_DONE_STATE:
        {
            // here we pick a single piece, but maybe from inside a stack
            moveStep = 0;
            copyOriginalStack[0] = new pstack(pp.mystack,pieces);
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

            pstack st = movingStack[moveStep] = pp.makeNewStack(); // get the whole stack on top
            pickedStack[moveStep] = new pstack(st,pieces);
            break;

        default:
        	throw G.Error("Pick not handled in state %s", board_state);
        }

        pstack res = movingStack[moveStep];
        setNextStateAfterPick(res,replay);

        return (res);
    }

    public void handleExchange(String pieceSpec,replayMode replay)
    {
    	Tokenizer tok = new Tokenizer(pieceSpec,",");
    	while(tok.hasMoreTokens())
    	{
    		piece p = pieces[tok.intToken()];
    		pstack s = p.mystack;
    		int ind = p.pieceType.index;
    		//pstack d = bar[p.owner^1][ind];
    		//G.Assert(s==d,"expected %s is %s",s,d);
    		pstack dest = trade[p.owner^1][ind];
    		s.removeElement(p);
    		p.addToStack(dest);
    	}
    }
    public void checkStacks()
    {	/*
    	for(pstack s : stacks)
    	{	G.Assert(s.b==this,"my board");
    		for(int lim = s.size()-1; lim>=0; lim--)
    		{
    			piece p = s.elementAt(lim);
    			G.Assert(p.mystack==s,"unexpected mystack %s expected %s",p.mystack,s);
    		}
    	}
    	*/
    }
    public void doDone(replayMode replay)
    {
    	placementIndex++;
        setNextStateAfterDone(replay);
    }
    //
    // actually execute a move.  Make changes in the board, select the next state and the next
    // player.  For the most part, this assumes that only legal moves will be executed, but
    // some nonsensical moves trigger error states to indicate likely bugs.
    //
    public boolean Execute(commonMove mm,replayMode replay)
    {	plateaumove m = (plateaumove)mm;
        //System.out.println("E "+m);
    	checkStacks();
        m.startingState = board_state;
        switch (m.op)
        {
        case ROBOT_DELAY:
        	delay_count = m.level;
        	break;
        case MOVE_FLIP:
        {
           piece mov = pieces[m.pick];
           handleFlip(mov);
           if(!robotBoard)
           { 	pstack myoldstack = mov.mystack;
        	   	pstack p = new pstack(myoldstack.b, myoldstack.origin, myoldstack.owner);
        	   	p.addElement(mov);
        	   	m.display = new pstack(p,null);
           }
        }

        break;
        case ROBOT_FLIP:
        	{	
        	pstack stack = locusStack(m.locus);
        	piece mov = stack.topElement();
        	handleFlip(mov);
	           if(!robotBoard)
	           { 	pstack p = new pstack(stack.b, stack.origin, stack.owner);
	        	   	p.addElement(mov);
	        	   	m.display = new pstack(p,null);
	           }
        	}
        break;

        case MOVE_RACKPICK:
	        {
	        	//
	        	// now suck up the pieces required by the onboard.
	        	//
	            pickStack(m.pieces, m.realColors); // pick the stack and set up the movingStack stuff
	            if(!robotBoard) { m.display = new pstack(pickedStack[0],null); }
	            setState(PlateauState.ONBOARD_DROP_STATE);
	        }
        	break;
        case MOVE_ONBOARD:
        {	// new strategy 10/2007, start an onboard by "drop off" to cancel any 
        	// pick in progress.
        	turnsSinceCapture++;
        	if(moveStep==0) { handleDrop(movingStackOrigin[0],DO_NOT_CAPTURE); }
        	//
        	// now suck up the pieces required by the onboard.
        	//
            pickStack(m.pieces, m.realColors); // pick the stack and set up the movingStack stuff
            if(!robotBoard) { m.display = new pstack(pickedStack[0],null); }
            pstack destination = locusStack(m.locus);
            m.setDrop(destination.stackNumber);
            pstack moving = movingStack[0];
            piece top = moving.topPiece();
            top.resetColor(m.pubColors);
            handleDrop(destination, m.level);
            // 11/2016 remember the piece we're dropping
            m.pick = droppedPiece.piecenumber;
            break;
        }
        case MOVE_EXCHANGE:
        	// move a collection of pieces to the exchange rack
        	handleExchange(m.pieces,replay);
        	switch(board_state)
        	{
        	case CAPTIVE_SHUFFLE_STATE:
        		break;
        	case PLAY_NOEXCHANGE_STATE:
        	case PLAY_STATE:
        		setState(PlateauState.CAPTIVE_SHUFFLE_STATE);
        		break;
        	case EXCHANGE_STATE:
        		setState(PlateauState.EXCHANGE_DONE_STATE);
        		break;
        	default: G.Error("Not expected");
        	}
        	if(!robotBoard) { m.display = new RackIcon(trade[whoseTurn]); }
        	break;
        case MOVE_DROP:
        {
        	turnsSinceCapture++;
        	pstack which = (m.destStack() == -1) ? movingStackOrigin[0]
                                            : stacks[m.destStack()];
            handleDrop(which, m.level);
            // 11/2016 remember the piece we're dropping
            m.pick = droppedPiece.piecenumber;
            if(!robotBoard)
            {
            	if("T".equals(which.locus())) { m.display = new RackIcon(trade[whoseTurn]); }
            	else { m.display = new pstack(which,null); }
            }
            break;
        }
        case ROBOT_PICK:
        	{
        	pstack ps = locusStack(m.locus);
        	piece pp =  ps.elementAt(m.level);
            //G.Assert(ps.contains(pp),"should be contained");
            m.setDrop(ps.stackNumber); // save the actual origin stack for editing
            handlePick(pp,replay);
            if(!robotBoard)
            {
            m.display = new pstack(pickedStack[moveStep],null);
            }
            lastPickedIndex = ps.lastPicked;
            ps.lastPicked = placementIndex;
        	}
        	break;
        case MOVE_PICK:
        {
            piece pp = pieces[m.pick];
            pstack ps = pp.mystack;
            //G.Assert(ps.contains(pp),"should be contained");
            m.setDrop(ps.stackNumber); // save the actual origin stack for editing
            handlePick(pp,replay);
            if(!robotBoard)
            {
            m.display = new pstack(pickedStack[moveStep],null);
            }
            lastPickedIndex = ps.lastPicked;
            ps.lastPicked = placementIndex;
        }

        break;

        case MOVE_DONE:
        	doDone(replay);

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
        checkStacks();
        m.state_after_execute = board_state;
        return (true);
    }


    public void test_eval()
    {
    }

    public pstack takeOffStack()
    {
        return ((moveStep >= 0) ? movingStackOrigin[0] : null);
    }
    public boolean isEdgeCell(pstack c)
    {
    	return isEdgeSquare(c.colNum(),c.rowNum());
    }
    public boolean isEdgeSquare(int x, int y)
    {
        return ((x == 0) || (x == (ncols - 1)) 
        		|| ((y == 0) || (y == (nrows - 1))));
    }

    public boolean LegalDestination(pstack dest)
    { // return true if we can stop here to pick up, drop or pin

        pstack from = copyOriginalStack[0];
        int height = from.takeOffHeight();
        Face color = from.realTopColor();
        int idx = dest.col() - from.col();
        int idy = dest.row() - from.row();
        int adx = Math.abs(idx);
        int ady = Math.abs(idy);

        if (moveStep > 0)
        { // we must be further away and in the same direction as the
          // previous step.

            pstack pfrom = copyOriginalStack[moveStep];
            int pdx = pfrom.col() - from.col();
            int pdy = pfrom.row() - from.row();

            if (color == Face.Orange)
            {
                int odx = Math.abs(dest.col() - pfrom.col());
                int ody = Math.abs(dest.row() - pfrom.row());

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
        if(adx==0 && ady==0) { return true; }	// drop on same spot
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

        boolean val = p.realTopColor()!=Face.Blank && !p.elementAt(0).unobstructed();

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
            case PLAY_NOEXCHANGE_STATE:
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
                    else if(p.stackNumber==copyOriginalStack[moveStep].stackNumber) 
                    	{ return ontop; 
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
			case PLAY_NOEXCHANGE_STATE:
                return ((p.topOwner() == whoseTurn) && chip.unobstructed()); //pick only unobstructed pieces

            case ONBOARD2_DROP_STATE: // drop an initial stack on the nonempty board
                return ((p.size() == 0) && isEdgeCell(p));

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

	        case PLAY_NOEXCHANGE_STATE:
	        case GAMEOVER_STATE:
	        default:
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

	        case PLAY_NOEXCHANGE_STATE:
	        case GAMEOVER_STATE:
	        default:
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
        case PLAY_NOEXCHANGE_STATE:
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

                if (pp.pieceType.index != i)
                {	stack.remove(j);
                    pp.addToStack(st[pp.pieceType.index]);
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

        case EXCHANGE_STATE:
        	if(robotBoard)
        	{	// in live play, a refused exchange reverts to presenting prisoers, which can be withdrawn
        		// in robot games, the prisoners get withdrawn and revert to play mode.
        		pstack tr[] = trade[whoseTurn^1];
        		pstack ra[] = bar[whoseTurn^1];
        		for(int i=0;i<tr.length;i++)
        		{
        			pstack from = tr[i];
        			if(from.size()>0)
        			{
        				ra[i].dropStack(from);
        			}
        		}
        		setState(PlateauState.PLAY_STATE);
        	}
        	break;
        case EXCHANGE_DONE_STATE:

            // move the prisoners in the exchange area to back to the racks
        	int offered = pointTotal(trade[whoseTurn]);
            if (offered == 0)
            {	 // this is a trade refusal
            	if(revision>=101) { minimumTradeOffer[whoseTurn^1] = pointTotal(trade[whoseTurn^1])+1; }
            	if(revision>=101 && pointTotal(bar[whoseTurn^1])==0)	// nothing else to add
            	{
            	// robot proposed an exchange that was refused
            	pstack tr[]= trade[whoseTurn^1];
            	pstack ba[] = bar[whoseTurn^1];
            	for(int i=0;i<tr.length;i++)
            	{
            	ba[i].dropStack(tr[i]);
            	}
            	setState(PlateauState.PLAY_NOEXCHANGE_STATE);
            	}
            	else
            	{
                setState(PlateauState.CAPTIVE_SHUFFLE_STATE);
            	}
                SetNextPlayer();
            }
            else
            {
                for (int pl = FIRST_PLAYER_INDEX; pl <= SECOND_PLAYER_INDEX;
                        pl++)
                {
                    pstack[] tr = trade[pl];
                    pstack[] ra = rack[pl^1];
                    resort(tr, (pl == anonymized_for));

                    for (int i = 0; i < ra.length; i++)
                    {
                        ra[i].dropStack(tr[i]);
                    }
                }
                AR.setValue(minimumTradeOffer,0);
                setState(PlateauState.PLAY_STATE);

                // note we do NOT change players here.
            }

            break;

        case CAPTIVE_SHUFFLE_STATE:
            SetNextPlayer();
            minimumTradeOffer[whoseTurn] = largestSubsetBelow(pointTotal(trade[whoseTurn^1]),whoseTurn);
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
                    {	turnsSinceCapture = 0;
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
                        bar[whoseTurn][victim.pieceType.index].dropStack(news);
                        AR.setValue(minimumTradeOffer,0);
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
		case FLIPPED_STATE:
		case PLAY_STATE:
		case PLAY_NOEXCHANGE_STATE:
			// allow damaged games to continue, and also some very rare cases where no legal moves are possible
			setFlipped(null);
			SetNextPlayer();
			setState(PlateauState.PLAY_STATE);
			break;
		default:
        	throw G.Error("No preparation for done, state =%s", board_state);
        }
    }

    private boolean exchangeIsLegal()
    {
        int offer = pointTotal(trade[whoseTurn]);
        int ask = pointTotal(trade[whoseTurn^1]);
        int tot = largestSubsetBelow(ask, whoseTurn);

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
            case PLAY_NOEXCHANGE_STATE:
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
                            int dx = Math.abs(dest.col() - original.col());
                            int dy = Math.abs(dest.row() - original.row());

                            if ((dx + dy) != 3)
                            {
                                nextState = PlateauState.PLAY_UNDONE_STATE;
                            }
                        }
                       // else if(canMove(copyOriginalStack[0],dest))
                       // {
                       // 	nextState = PlateauState.PLAY_CAPTURE_STATE;
                       // }
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

            case PLAY_NOEXCHANGE_STATE:
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
            case PLAY_NOEXCHANGE_STATE:
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
            	if(replay.isReplay)
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
            case PLAY_NOEXCHANGE_STATE:
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
                pstack contents = getBoardXY(x, y);
                drawPstack(gc, contents, cx, cy, xstep * 2, ystep * 2, highlight);
                numberMenu.saveSequenceNumber(contents,cx+xstep,cy+ystep	);
                //GC.Text(gc,true,cx,cy,xstep,ystep,Color.black,null,""+contents.stackNumber);
            }
        }
    }
	boolean robotBoard = false;
	public void RobotExecute(commonMove m) {
	       Execute(m,replayMode.Replay);
	       
	       if(whoseTurn==m.player){
	       switch(board_state)
	       {
			case ONBOARD2_DONE_STATE:
			case ONBOARD_DONE_STATE:
			case EXCHANGE_DONE_STATE:
			case DRAW_STATE:
				doDone(replayMode.Replay);
				break;
			default: break;
	       }}
	     
		
	}
	public void UnExecute(commonMove m0) {
		
    	G.Error("Not expected");
	}
	public void initRobotValues(PlateauPlay plateauPlay) 
	{
		robotBoard = true;
		useDelays = plateauPlay.useDelays;
	}
	public boolean winForPlayerNow(int i) 
	{
		return win[i];
	}
    // add one exchange moves for all the chips on the bar
    private void addExchangeMoves(CommonMoveStack all,int who)
    {	boolean canBeDone = (board_state==PlateauState.CAPTIVE_SHUFFLE_STATE);
    	pstack b[] = bar[who];
    	int offer = pointTotal(trade[who]);
    	int minacceptable = largestSubsetBelow(offer, who^1);
    	for(int lim=b.length-1; lim>=0; lim--)
    	{
    		pstack src = b[lim];
    		int ss = src.size();
    		if(ss>0)
    		{
    		piece p = src.topPiece();
    		int newvalue = p.pieceType.value;
    		int newmin = largestSubsetBelow(offer+newvalue,who^1);
    		
    		if(newmin>minacceptable)	// don't add pieces which do nothing
    		{
    		if((newmin-minacceptable)>=newvalue-1) { canBeDone = false; }	// if there's a good trade available, don't be done
    		String str = ""+p.piecenumber;
    		all.push(new plateaumove(MOVE_EXCHANGE,str,who));
    		}
    		
    		}
    	}
    	if(canBeDone)
    	{
 			addOptionalDone(all);
    	}
    }
    
    // add one exchange moves for all the chips on the bar
    private commonMove randomExchangeMove(Random r,int who)
    {	
    	pstack b[] = bar[who];
    	int size = b.length;
    	int offset = r.nextInt(size);
    	int offer = pointTotal(trade[who]);
    	int minacceptable = largestSubsetBelow(offer, who^1);
    	for(int i = 0;i<size;i++)
    	{	
    		pstack src = b[(i+offset)%size];
    		int ss = src.size();
    		if(ss>0)
    		{
    		piece p = src.topPiece();
       		int newvalue = p.pieceType.value;
    		int newmin = largestSubsetBelow(offer+newvalue,who^1);
    		
    		if(newmin>minacceptable)	// don't add pieces which do nothing
    		{
    		String str = ""+p.piecenumber;
    		return new plateaumove(MOVE_EXCHANGE,str,who);
    		}}
    		}
    	boolean canBeDone = (board_state==PlateauState.CAPTIVE_SHUFFLE_STATE);
    	if(canBeDone) { return getOptionalDone(); }
    	return null;
    }
    //
    // moves when pieces have been picked from the rack which can't be dropped on the board
    //
    private void addRackDropMoves(CommonMoveStack all,int who)
    {
    	pstack rs[] = rack[who];
    	for(pstack d : rs)
    	{	all.push(new plateaumove("Drop " + d.stackNumber+" 100 " + " " + d.locus(),who));
    	}
    }
    
    // onboarding a piece
    private void addBoardDropMoves(CommonMoveStack all,pstack cell,int who)
    {	
		int opponent = who^1;
		if(cell.topOwner()!=opponent)
		{
		int sz = cell.size();
		pstack st = movingStack[moveStep];
		while(sz>=0)
		{
			String str = "Onboard "+cell.locus()+" "+sz +" "+st.allRealColors()+" "+st.pieces();
			all.push(new plateaumove(str,who));
			sz--;
			if(sz>=0) { 
				piece next = cell.elementAt(sz);
				if(next.owner!=who) { sz = -1; }
			}
		}}
    }
    // onboarding a piece
    private void addBoardDropMoves(CommonMoveStack all,int who)
    {	pstack loc = winningDropLocation(who);
    	if(loc!=null)
    	{
        	pstack st = movingStack[moveStep];
    		winningMove = new plateaumove("Onboard " + loc.locus()+" "+DO_NOT_CAPTURE+" "+st.allRealColors()+" "+st.pieces(),who);
    	}
    	else
    	{
    	for(int lim=cellArray.length-1; lim>=0; lim--)
    	{
    		pstack cell = cellArray[lim];
    		addBoardDropMoves(all,cell,who);
    	}}
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
        for (int i = 0,permutations=(1 << v.size()); i < permutations; i++)
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
    	int minimumPieces = 999;
    	int offer = pointTotal(trade[nextPlayer[whoseTurn]]);
    	int minacceptable = largestSubsetBelow(offer, whoseTurn);
    	int minvalue = 9999;
    	
    	// manage the overpayment options by using the fewest pieces with the lowest value   	
    	for(int i=1;i<(1<<v.size());i++)
    	{
    		int subtotal = v.sumSubset(i);
    		if(subtotal>minacceptable)
    		{	
        		int nones = G.bitCount(i);
        		if(nones<minimumPieces)
        		{	// never give more pieces than necessary, even if possible
        			minimumPieces = nones;
        			minvalue = subtotal;
        		}
        		else {
        			minvalue = Math.min(subtotal,minvalue);
        		}
    		}}
    	
    	for(int i=1;i<(1<<v.size());i++)
    	{
    		int subtotal = v.sumSubset(i);
      		if((subtotal==minacceptable) || (subtotal>minacceptable && (G.bitCount(i)==minimumPieces) && (subtotal<=minvalue)))
        		{
    			String str = v.pickSubset(i);
    			all.push(new plateaumove(MOVE_EXCHANGE,str,who));
    			if(subtotal<=offer) { underpay++; }
        		}
    		}
    	
    	if(underpay==0) 
    	{	// if all the proposed payments were more than was offered, we can refuse
    		all.push(new plateaumove(MOVE_DONE,who));
    	}
    }
    
    private boolean addOrangeFinalMoves(CommonMoveStack all,int who,pstack mid,int dx,int dy)
    {
    	int x = mid.colNum()+dx;
    	int y = nrows-mid.rowNum()-1+dy;
    	if(validBoardPos(x,y))
    	{	if(all!=null)
    		{
    		pstack fin = getBoardXY(x,y);
     		plateaumove newMove = new plateaumove(MOVE_DROP,fin,TOP_CAPTURE,who);
    		all.push(newMove);
    		int sz = fin.size();
    		if((sz>=5) && fin.elementAt(sz-5).unobstructed())
    		{
    			winningMove = newMove;
    		}
    		}
    		return(true);
    	}
    	return(false);
    }
  
    private commonMove randomOrangeFinalMoves(int who,pstack mid,int dx,int dy)
    {
    	int x = mid.colNum()+dx;
    	int y = nrows-mid.rowNum()-1+dy;
    	if(validBoardPos(x,y))
    	{	
    		pstack fin = getBoardXY(x,y);
     		return new plateaumove(MOVE_DROP,fin,TOP_CAPTURE,who);
    	}
    	return(null);
    }
    
    private commonMove randomOrangeFinalMoves(boolean onlyCaptures,int who,pstack mid,int dx,int dy)
    {
    	int x = mid.colNum()+dx;
    	int y = nrows-mid.rowNum()-1+dy;
    	if(validBoardPos(x,y))
    	{	
    		pstack fin = getBoardXY(x,y);
    		if(!onlyCaptures || fin.containsOwner(who^1))
    		{
    			plateaumove newMove = new plateaumove(MOVE_DROP,fin,TOP_CAPTURE,who);
    			return newMove;
    		}
    	}
    	return null;
    }
    private boolean addOrangeIntermediateMoves(CommonMoveStack all,int who,pstack from,pstack moving,int dx,int dy)
    {	boolean some = false;
    	if(winningMove!=null) { return true;}
    	int x = from.colNum()+dx;
    	int y = nrows-from.rowNum()-1+dy;
    	int movingHeight = moving.size();
    	if(validBoardPos(x,y))
    	{	boolean valid = false;
    		pstack midCell = getBoardXY(x,y);
    		// add jump to the final, without an intermediate stop
     		if(dy==0)
    		{
    			valid |= addOrangeFinalMoves(all,who,from,dx*2,+1);
    			if(valid && (all==null)) { return(true); }
    			valid |= addOrangeFinalMoves(all,who,from,dx*2,-1);
    			if(valid && (all==null)) { return(true); }
    		}
    		else
    		{
    			valid |= addOrangeFinalMoves(all,who,from,+1,dy*2);
    			if(valid && (all==null)) { return(true); }
    			valid |= addOrangeFinalMoves(all,who,from,-1,dy*2);
    			if(valid && (all==null)) { return(true); }
    		}
    		
    		if(valid)	// the end point is on the board, so we can potentially drop in the intermediate cell
    		{	
    				if( (midCell.size()==0)
    						||(midCell.topOwner()==who)
    						||from.containsColor(from.size()-movingHeight,from.size()-1))
    				{	if(midCell.size()>0 || movingHeight>1)
    					{	
     	   				// must be taking a non-mute to pin
    					if(all==null) { return(true); }
    					plateaumove newMove = new plateaumove(MOVE_DROP,midCell,TOP_CAPTURE,who);
         				all.push(newMove);
    					}
    				}
    			
    		}
    	}
    	return(some);
    }
    
    private commonMove randomOrangeIntermediateMove(Random r,boolean onlyCaptures,int who,pstack from,pstack moving,int dx,int dy)
    {	
    	int x = from.colNum()+dx;
    	int y = nrows-from.rowNum()-1+dy;
    	int movingHeight = moving.size();
    	int direction = r.nextBoolean() ? 1 : -1;
    	if(validBoardPos(x,y))
    	{	
    		pstack midCell = getBoardXY(x,y);
    		// add jump to the final, without an intermediate stop
    		if(onlyCaptures || r.nextDouble()<0.5)
    		{
     		if(dy==0)
    		{
    			commonMove m = randomOrangeFinalMoves(onlyCaptures,who,from,dx*2,direction);
    			if(m==null) { m = randomOrangeFinalMoves(onlyCaptures,who,from,dx*2,-direction); }
    			if(m!=null) { return m; }
    		}
    		else
    		{	commonMove m = randomOrangeFinalMoves(onlyCaptures,who,from,direction,dy*2);
    			if(m==null) { m = randomOrangeFinalMoves(onlyCaptures,who,from,-direction,dy*2); }
    			if(m!=null) { return m; }
    		}}
     		
    		if(!onlyCaptures)	// the end point is on the board, so we can potentially drop in the intermediate cell
    		{	boolean valid = dx==0
    						? validBoardPos(x+1,y+dy*2)||validBoardPos(x-1,y+dy*2)
    						: validBoardPos(x+dx*2,y+1) || validBoardPos(x+dx*2,y-1);
    				if( valid
    					&& ((midCell.size()==0)
    						||(midCell.topOwner()==who)
    						||from.containsColor(from.size()-movingHeight,from.size()-1)))
    				{	if(midCell.size()>0 || movingHeight>1)
    					{	
     	   				// must be taking a non-mute to pin
     					plateaumove newMove = new plateaumove(MOVE_DROP,midCell,TOP_CAPTURE,who);
         				return newMove;
    					}
    				}
    			
    		}
    	}
    	return(null);
    }
    
    private boolean addOrangeContinuationMoves(CommonMoveStack all,int who,pstack cell,int dx,int dy)
    {	boolean some = false; 
    	if(winningMove!=null) { return true;}
    	if(dx==0)
		{
    	if(addOrangeFinalMoves(all,who,cell,1,dy)) { if(all==null) { return(true); } some=true; }   
		some |= addOrangeFinalMoves(all,who,cell,-1,dy);
		}
		else {
			if(addOrangeFinalMoves(all,who,cell,dx,1)) { if(all==null) { return(true); } some=true; }
			some |= addOrangeFinalMoves(all,who,cell,dx,-1);					
		}
    	return(some);
    }
    private commonMove randomOrangeContinuationMoves(Random r,int who,pstack cell,int dx,int dy)
    {	 
    	int direction = r.nextBoolean() ? 1 : -1;
    	if(dx==0)
		{
    	commonMove m = randomOrangeFinalMoves(who,cell,direction,dy);
    	if(m!=null) { return m; }
    	return randomOrangeFinalMoves(who,cell,-direction,dy);
		}
		else {
			commonMove m = randomOrangeFinalMoves(who,cell,dx,direction);
			if(m!=null) { return m; }
			return randomOrangeFinalMoves(who,cell,dx,-direction);	
		}
    }

    private boolean addNormalMoves(CommonMoveStack all,int who,pstack from,int mindistance,int maxdistance,int dx,int dy,piece top,pstack moving)
    {	int movingHeight = moving.size();
    	int ox = from.colNum();
    	int oy = nrows-from.rowNum()-1;
     	boolean some = false;
		{
			// move a stack of "movingHeight" chipStack, maxDistance or less
		  	boolean canMoveFurther = false;			// we can make intermediate stops only if we can move further with this stack

		  	for(int distance = maxdistance; distance>=mindistance; distance--)
			{
    		int x = ox + distance*dx;
    		int y = oy + distance*dy;
    		if(validBoardPos(x,y))
    		{	pstack target = getBoardXY(x,y);
    			int targetHeight = target.size();
    			int targetOwner = target.topOwner();
    			if(	(targetHeight==0) 				// destination is empty
    				|| (targetOwner==who) 	// or destination is ours
    				|| !top.isMute() 				// or our top is colored
    				|| (canMoveFurther && moving.containsColor(0,movingHeight))
    				)
    			{	if(distance>0 || (!top.isMute() && stompCapture(target)))	// this stompcapture may not be completely right
    				{
    				if(all==null) { return(true); }
    				plateaumove newmove = new plateaumove(MOVE_DROP,target,TOP_CAPTURE,who);
    				all.push(newmove);
    				some = true;
    				int sz = target.size();
    				if(movingHeight>=6 || (sz+movingHeight>=6 && target.elementAt(sz+movingHeight-6).unobstructed()))
    				{
    					winningMove = newmove;
    				}
    				if(!top.isMute())
    					{	int captureHeight = target.captureDepth(who,movingHeight);
    						if(captureHeight>0 && (captureHeight+numberOfPrisoners(who)>=6))
    						{
    							winningMove = newmove;
    						}
    					}
    				canMoveFurther = true;
    				}
    			}
    			
    		}
    	}
		}
		return(some);
    }
    // when picking a random intermediate stop which requires a move-on, determine that the move-on
    // is actually legal.  We are moving a mute-topped stack which needs a friendly or empty landing
    private boolean canMoveFurther(int who,pstack from,int mindistance,int maxdistance, int dx, int dy)
    {
    	int ox = from.colNum();
    	int oy = nrows-from.rowNum()-1;
    	for(int i=mindistance; i<maxdistance;i++)
    	{
    		int x = ox+dx*i;
    		int y = oy+dy*i;
    		if(validBoardPos(x,y))
    		{
    			pstack target = getBoardXY(x,y);
    			if((target.size()==0) || (target.topOwner()==who)) { return true; }
    		}
    	}
    	return false;
    }
    private commonMove randomNormalMove(Random r,boolean onlyCaptures,int who,pstack from,int mindistance,int maxdistance,int dx,int dy,piece top,pstack moving)
    {	int movingHeight = moving.size();
    	int ox = from.colNum();
    	int oy = nrows-from.rowNum()-1;
   
    	if(onlyCaptures && top.isMute()) { return null; }
   
    	{
			// move a stack of "movingHeight" chipStack, maxDistance or less
		  	int span = maxdistance - mindistance;
		  	int offset = span>0?r.nextInt(span) : 0;
		  	for(int d = 0;d<span;d++)
			{
		  	int distance = mindistance+(d+offset)%span;
    		int x = ox + distance*dx;
    		int y = oy + distance*dy;
    		if(validBoardPos(x,y))
    		{	pstack target = getBoardXY(x,y);
    			int targetHeight = target.size();
    			int targetOwner = target.topOwner();
    			boolean cando = onlyCaptures 
    								? target.containsOwner(who^1)
    								: (targetHeight==0) 				// destination is empty
    			    				|| (targetOwner==who) 			// or destination is ours
    			    				|| !top.isMute() 				// or our top is colored
    			    				|| (moving.containsColor(0,movingHeight)
    			    						&& canMoveFurther(who,target,1,maxdistance-distance, dx, dy)
    			    						);
    			if(cando)
    			{	if(distance>0 || (!top.isMute() && stompCapture(target)))	// this stompcapture may not be completely right
    				{
    				return new plateaumove(MOVE_DROP,target,TOP_CAPTURE,who);
    				}
    			}
    			
    		}
    	}
		}
		return(null);
    }


    // add drop moves for the first move of the game
    private void addInitialDropMoves(CommonMoveStack all,int who)
    {	boolean first = (who == FIRST_PLAYER_INDEX);
		for(pstack cell = allCells; cell!=null; cell=cell.next)
			{
			if((cell.size()==0) && isEdgeCell(cell) && (first ? cell.row()==1 : true))
				{
				pstack st = movingStack[moveStep];
				String str = "Onboard "+cell.locus()+" "+TOP_CAPTURE+" "+st.allRealColors()+" "+st.pieces();
				all.push(new plateaumove(str,who));
				}
			}
    }
    
    
    private boolean addFlipMoves(CommonMoveStack all,pstack cell,int who)
    {	piece top = cell.topElement();
		if( top!=null && top.owner==who && !top.isMonoColor())
		{	
			if(all==null) { return(true); }
			all.push(new plateaumove(ROBOT_FLIP,top,cell.locus(),top.topColorString(),who));
			return true;
		}
		return false;
    }

    private commonMove randomFlipMove(Random r,int who)
    {	int size = cellArray.length;
    	int offset = r.nextInt(size);
    	for(int i=0;i<size;i++)
    	{
    		pstack cell = cellArray[(i+offset)%size];
        	piece top = cell.topElement();
        	if(top!=null && top.owner==who && (!top.allKnown()  || !top.isMonoColor()))
        	{   return new plateaumove(ROBOT_FLIP,top,cell.locus(),top.topColorString(),who);
        	}
    	}
    	return null;
    }
  
    private commonMove randomInitialBoardPickMove(Random r,boolean mute,int who)
    {	int size = cellArray.length;
    	int offset = r.nextInt(size);
    	for(int i=0;i<size;i++)
    	{
    		pstack cell = cellArray[(i+offset)%size];
    		piece top = cell.topElement();
    		if(top!=null 
    				&& (top.owner==who) 
    				// this is only mutes or only nonmutes
    				&& (mute == (top.realTopColor()==Face.Blank)))
    		{
    			return randomInitialBoardPickMove(r,cell,who);
    		}
    	}
    	return null;
    }

    private boolean addFlipMoves(CommonMoveStack all,int who)
    {	boolean some = false;
    	for(int lim=cellArray.length-1; lim>=0; lim--)
     	{
    		pstack cell = cellArray[lim];
    		// will check that owner is correct
    		some |= addFlipMoves(all,cell,who);
    		if(some && (all==null)) { return(true); }
     	}
    	return some;
    }
    // special logic to predict if a mute will be able to move if picked
    private boolean canMove(pstack cell,int distance)
    {	Face color = cell.realTopColor();
    	for(int direction = 0;direction<8;direction++)
    	{
    		pstack c = cell;
    		for(int i=0;i<distance && c!=null;i++)
    		{
    			c = exitTo(c,direction);
    			if((c!=null) 
    					&& ((c.size()==0)
    							|| (c.topOwner()==whoseTurn)
    							|| (color!=Face.Blank)))
    				{ return true; }
    		}
    	}
    	return false;
    }
    
    // special logic to predict if a mute will be able to move if picked
    private boolean canMove(pstack from,pstack to,Face top)
    {	
    	int toCol = to.colNum();
    	int toRow = to.rowNum();
    	
    	int dx = toCol-from.colNum();
    	int dy = toRow-from.rowNum();
    	if(dx==0 && dy==0) { return false; }
    	if(top==Face.Orange)
    	{	// the intermediate point was only selected if the endpoint exists
    		return (Math.abs(dx)<2) && (Math.abs(dy)<2 );
    	}
    	else
    	{
    	int distance = from.takeOffHeight();
    	int remainingDistance = distance-Math.max(Math.abs(dx),Math.abs(dy));
    	int sdx = G.signum(dx);
    	int sdy = G.signum(dy);
    	int opponent = whoseTurn^1;
    	while(remainingDistance>0)
    	{
    		remainingDistance--;
    		toCol += sdx;
    		toRow += sdy;
    		int targetRow = nrows-toRow-1;
    		pstack nextCell = validBoardPos(toCol,targetRow) ? getBoardXY(toCol,targetRow) : null;
    		if(nextCell==null) {return false; }
    		if((top!=Face.Blank) || (nextCell.topOwner()!=opponent)) { return true; }
    	}
    	}
    	return false;
    }
    
    private boolean addIntermediatePickMoves(CommonMoveStack all,pstack cell,int who)
    {  	boolean some = false;
    	Face top = cell.realTopColor();
    	boolean mobile = canMove(copyOriginalStack[0],cell,top);
    	if(mobile)
    	{
    	int takeoff = cell.takeOffHeight();
    	int sz=cell.size();
    	boolean colorNeeded = takeoff<sz
    							&& (cell.elementAt(sz-takeoff-1).owner!=who);
    	// we do this from bottom to top, so prev is the exposed piece after the pick
    	piece prev = null;
   		pstack mov = pickedStack[moveStep-1];
   		for(int h = sz-takeoff; h<sz; h++)
    	{	piece p = cell.elementAt(h);
     		if(mov.size()!=(sz-h))	// don't include moving exactly what we dropped
    		{
    		if(!colorNeeded || prev==null || (prev.realTopColor()!=Face.Blank))
    		{	// intermediate moves have to leave color
    	    	if(all==null) { return true; }
    			all.push(new plateaumove(ROBOT_PICK,p,h,cell,who));
    			some = true;
    		}}
    		prev = p;
    	}
    	}
    	return some;
    }
    
    private boolean addInitialBoardPickMoves(CommonMoveStack all,pstack cell,int who)
    {
    	int takeoff = cell.takeOffHeight();
    	int sz=cell.size();
    	boolean some = false;
    	boolean canMove = canMove(cell,takeoff);
    	if(canMove)
    	{
    	for(int h = sz-takeoff; h<sz; h++)
    	{	piece p = cell.elementAt(h);
    		if(all==null) { return true; }
    		all.push(new plateaumove(ROBOT_PICK,p,h,cell,who));
    		some = true;
    	}}
    	return some;
    }
  
    private commonMove randomIntermediatePickMove(Random r,pstack cell,int who)
    {	Face top = cell.realTopColor();
    	boolean mobile = canMove(copyOriginalStack[0],cell,top);
    	if(mobile)
    	{
    	int takeoff = cell.takeOffHeight();
    	int sz=cell.size();
    	boolean colorNeeded = takeoff<sz
    							&& ((takeoff==0) || ((cell.elementAt(sz-takeoff-1).owner!=who)));
    	boolean canStay = top!=Face.Blank && top!=Face.Orange;
    	int offset = r.nextInt(takeoff+(canStay ? 1 : 0));
    	if(offset==takeoff)  { 
    		return getOptionalDone();
    	}
    	int first = sz-takeoff;
    	pstack mov = pickedStack[moveStep-1];
    	for(int idx = 0;idx<takeoff;idx++)
    	{	int h = first+(idx+offset)%takeoff;
    		if(mov.size()!=(sz-h))	// don't include moving exactly what we dropped
    		{
    		piece p = cell.elementAt(h);
     		if(!colorNeeded || (h==0) || (cell.elementAt(h-1).realTopColor()!=Face.Blank))
    		{	// intermediate moves have to leave color
    			return (new plateaumove(ROBOT_PICK,p,h,cell,who));
     		}}
    	}}
    	// can't move
    	return new plateaumove(MOVE_DONE,who);
    }
    private commonMove randomInitialBoardPickMove(Random r,pstack cell,int who)
    {
    	int takeoff = cell.takeOffHeight();
    	int sz=cell.size();
    	int offset = r.nextInt(takeoff);
    	int first = sz-takeoff;
    	boolean canStartMove = canMove(cell,takeoff);
    	if(canStartMove)
    	{
    		int h = first+offset;
    		piece p = cell.elementAt(h);
    		return (new plateaumove(ROBOT_PICK,p,h,cell,who));
     	}
    	return null;
    }
    
    private boolean addInitialBoardPickMoves(CommonMoveStack all,int who)
    {	boolean some = false;
    	for(int lim=cellArray.length-1; lim>=0; lim--)
     		{
    		pstack cell = cellArray[lim];
    		if(cell.topOwner()==who)
    			{
    			some |= addInitialBoardPickMoves(all,cell,who);
    			if(some && (all==null)) { return(true); }
    			}
    		}
    	
    	return(some);
    }
    
    private commonMove randomRackPickMove(Random r,int who)
    {	
    	pstack cells[] = rack[who];
    	int size = cells.length;
    	int offset = r.nextInt(size);
    	for(int i=0;i<size;i++)
    	{
    		pstack cell = cells[(i+offset)%size];
    		if(cell.size()>0)
    		{	piece p = cell.topElement();
    			Face top = p.realTopColor();
    			Face bottom =p.realBottomColor();
       			if(top==Face.Orange) { top = bottom; bottom = Face.Orange; }
       			if(top==bottom || r.nextDouble()<0.5)
    			{
    				String colors = top.shortName+bottom.shortName;
    				return new plateaumove(MOVE_RACKPICK,p,colors,who);
    			}
       			if(bottom!=Face.Orange)
       			{
       			String colors = bottom.shortName+top.shortName;
    			return new plateaumove(MOVE_RACKPICK,p,colors,who);
       			}
    		}
    	}
    	return null;
    }

    static int CELL_LEFT = 0;
    static int CELL_QUARTER_TURN = 2;
    static int CELL_FULL_TURN = 8;
    pstack getCellOrNull(int x, int y)
    {
    	if(x<0 || y<0 || x>=ncols || y>= nrows) { return null; }
    	return board[x][y];
    }
    pstack exitTo(pstack c,int direction)
    {	int x = c.colNum();
    	int y = nrows-c.rowNum()-1;
    	switch((direction+CELL_FULL_TURN)%CELL_FULL_TURN)
    	{
    	default: throw G.Error("Not expected");
    	case 0:	return getCellOrNull(x-1,y);
    	case 1: return getCellOrNull(x-1,y+1);
    	case 2: return getCellOrNull(x,y+1);
    	case 3: return getCellOrNull(x+1,y+1);
    	case 4: return getCellOrNull(x+1,y);
    	case 5: return getCellOrNull(x+1,y-1);
    	case 6: return getCellOrNull(x,y-1);
    	case 7: return getCellOrNull(x-1,y-1);
    	}
    }
   

    private void addContinuationDropMoves(CommonMoveStack all,int who,pstack cell,int distance,piece top,pstack moving,int dx, int dy)
    {	Face topcolor = top.realTopColor();
		switch(topcolor)
			{
			default: throw G.Error("Not expecting "+topcolor);
			case Orange:
				addOrangeContinuationMoves(all,who,cell,dx,dy);
				break;
			case Blue:
			case Red:
			case Blank:
				addNormalMoves(all,who,cell,1,distance,dx,dy,top,moving);
				break;
			}
	}

    private commonMove randomContinuationDropMove(Random r,int who,pstack cell,int distance,piece top,pstack moving,int dx, int dy)
    {	Face topcolor = top.realTopColor();
		switch(topcolor)
			{
			default: throw G.Error("Not expecting "+topcolor);
			case Orange:
				return randomOrangeContinuationMoves(r,who,cell,dx,dy);
				
			case Blue:
			case Red:
			case Blank:
				return randomNormalMove(r,false,who,cell,1,distance,dx,dy,top,moving);

			}
	}
    // drop after an intermediate drop in play_drop_state
    private void addContinuationDropMoves(CommonMoveStack all,int who)
    {
    	pstack orig = copyOriginalStack[0]; // where we all started, determines distance
    	pstack step = copyOriginalStack[moveStep];
    	pstack moving = movingStack[moveStep];
    	piece top = moving.topElement();
		int xsofar =step.col()-orig.col();
    	int ysofar = step.row()-orig.row();
    	int dissofar = Math.max(Math.abs(xsofar),Math.abs(ysofar));
    	int dx = Integer.signum(xsofar);
    	int dy = -Integer.signum(ysofar);	// calculate y directi
    	int takeoff = orig.takeOffHeight();
    	
    	addContinuationDropMoves(all,who,step,takeoff-dissofar,top,moving,dx,dy);
      	
    }
    // drop after an intermediate drop in play_drop_state
    private commonMove randomContinuationDropMove(Random r,int who)
    {
    	pstack orig = copyOriginalStack[0]; // where we all started, determines distance
    	pstack step = copyOriginalStack[moveStep];
    	pstack moving = movingStack[moveStep];
    	piece top = moving.topElement();
		int xsofar =step.col()-orig.col();
    	int ysofar = step.row()-orig.row();
    	int dissofar = Math.max(Math.abs(xsofar),Math.abs(ysofar));
    	int dx = Integer.signum(xsofar);
    	int dy = -Integer.signum(ysofar);	// calculate y directi
    	int takeoff = orig.takeOffHeight();
    	
    	return randomContinuationDropMove(r,who,step,takeoff-dissofar,top,moving,dx,dy);
      	
    }
   
    // find a stack were can add 1 more to and win, if it exists
    private pstack winningDropLocation(int who)
    {
    	int last = cellArray.length;
    	for(int i=0;i<last;i++)
    	{
    		pstack cell = cellArray[i];
    		int sz = cell.size();
    		if((sz>=5)
    			&& (cell.topOwner()==who)
    			&& (cell.elementAt(sz-5).unobstructed()))
    		{	
    			for(pstack r :rack[who])
    			{	// and verify they have something to drop
    				if(r.size()>0) { return cell; }
    			}
    		}
    	}
    	return null;
    }
    
    private commonMove winningDropMove(piece p,Face top,Face bottom,int who)
    {	pstack cell = winningDropLocation(who);
    	if(cell!=null)
    	{	String colors = top.shortName+bottom.shortName;
			String pnum = ""+p.piecenumber;
    		return new plateaumove(cell.locus(),DO_NOT_CAPTURE,colors,colors,pnum,who);
    	}
    	return null;
    }
    
    private commonMove randomDropMove(Random r,piece p,Face top,Face bottom,int who)
    {	
    	commonMove m = winningDropMove(p,top,bottom,who);
    	if(m!=null) { return m; }
    	
    	int opponent = who^1;
		String colors = top.shortName+bottom.shortName;
		int last = cellArray.length;
		int first = r.nextInt(last);
		for(int idx = 0; idx<last; idx++)
		{
			pstack cell = cellArray[(idx+first)%last];
			
	  		if(cell.topOwner()!=opponent)
    		{
    		int height = cell.size();
 			String pnum = ""+p.piecenumber;
			String loc = cell.locus();
			
			if(height==0 || r.nextDouble()<0.5) 
				{ return new plateaumove(loc,0,colors,colors,pnum,who);
				}
			return new plateaumove(loc,height-1,colors,colors,pnum,who);
    		}
    	}
		throw G.Error("there must be a drop move");
    }
    private commonMove randomDropMove(Random r,int who)
    {	pstack ps = movingStack();
    	piece p = ps.topElement();
    	Face top = p.realTopColor();
    	Face bottom = p.realBottomColor();
     	return randomDropMove(r,p,top,bottom,who);
     }

    private commonMove getOptionalDone()
    {	if(useDelays)
    	{
    	if(delay_count==0) { return new plateaumove(ROBOT_DELAY,1,whoseTurn); }
    	delay_count--;
    	if(delay_count<=0) { return new plateaumove(MOVE_DONE,whoseTurn); }
    	}
    	return new plateaumove(MOVE_DONE,whoseTurn);
    }
    private void addOptionalDone(CommonMoveStack all)
    {
    	all.push(getOptionalDone());
    }
    private void addDoneMove(CommonMoveStack all,int who)
    {
    	all.push(new plateaumove(MOVE_DONE,who));
    }
	
	private void buildOnboardPickMoves(CommonMoveStack all,piece p,Face top, Face bottom,int who)
	{	String colors = top.shortName+bottom.shortName;
		if(winningMove!=null) { return; }
		plateaumove topMove = new plateaumove(MOVE_RACKPICK,p,colors,who);
		all.push(topMove);		// on top
	
	}  
	private plateaumove winningOnboardMove(pstack win,int who)
	{
		for(pstack p : rack[who])
		{
			if(p.size()>0)
			{	piece st = p.topElement();
				String str = "Onboard "+win.locus()+" "+DO_NOT_CAPTURE +" "+st.realColorString()+" "+st.piecenumber;
				winningMove = new plateaumove(str,who);
				return winningMove;
			}
		}
		return null;

	}
	private plateaumove winningOnboardDropMove(int who)
	{
    	pstack win = winningDropLocation(who);
    	if(win!=null)
    	{
    		return winningOnboardMove(win,who);
     	}
    	return null;
	}
	// pick from rack to deploy to the board
    private void addOnboardPickMoves(CommonMoveStack all,int who)
    {	pstack pieces[] = rack[who];
    	int typeMap = 0;
    	for(pstack stack : pieces)
    	{	
    		for(int lvl=0; lvl<stack.size() ; lvl++)
    		{
    			piece p = stack.elementAt(lvl);
    			int pieceBit = (1<<p.pieceType.index);
    			if((pieceBit & typeMap)==0)
    			{
    				typeMap |= pieceBit;
    				Face top = p.realTopColor();
    				Face bottom = p.realBottomColor();
    				// never deploy the orange as visible
    				if(top!=Face.Orange) 
    					{ 
    					buildOnboardPickMoves(all,p,top,bottom,who); 
    					}
    				if(!p.isMonoColor() && (bottom!=Face.Orange)) 
    					{ buildOnboardPickMoves(all,p,bottom,top,who); 
    					}
    			}
    		}
    	}
    }

    // dropoffs after an initial pick, in play_drop_state
    private void addInitialDrops(CommonMoveStack all,int who)
    {  	

    	pstack orig = copyOriginalStack[0]; // where we all started, determines distance
    	pstack moving = movingStack[moveStep];
    	piece top = moving.topElement();
    	addInitialDrops(all,who,orig,orig.takeOffHeight(),top,moving,0);    	
    }
    // dropoffs after an initial pick, in play_drop_state
    private commonMove randomDropFromBoardMove(Random r,boolean onlyCaptures,int who)
    {  	

    	pstack orig = copyOriginalStack[0]; // where we all started, determines distance
    	pstack moving = movingStack[moveStep];
    	piece top = moving.topElement();
    	return randomInitialDrop(r,onlyCaptures,who,orig,orig.takeOffHeight(),top,moving,0);    	
    }
    // dropoffs after an initial pick, in play_drop_state
    static int orthogonalDirections[][] = {{1,0},{-1,0},{0,1},{0,-1}};
    static int diagonalDirections[][] = {{1,1},{1,-1},{-1,1},{-1,-1}};
    static int allDirections[][] = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    
    private commonMove randomInitialDrop(Random r,boolean onlyCaptures,int who,pstack cell,int distance,piece top,pstack moving,int opponentHeight)
    {	Face topcolor = top.realTopColor();
    	switch(topcolor)
			{
			default: throw G.Error("Not expecting "+topcolor);
			case Orange:
				{	int offset = r.nextInt(4);
					for(int i=0;i<4;i++)
					{	int pair[] = orthogonalDirections[(i+offset)%4];
						commonMove some = randomOrangeIntermediateMove(r,onlyCaptures,who,cell,moving,pair[0],pair[1]);
						if(some!=null) { return some; }
					}
				}
				break;
			case Blue:
				{
				int offset = r.nextInt(4);
				for(int i=0;i<4;i++)
				{	int pair[] = diagonalDirections[(i+offset)%4];
					commonMove some = randomNormalMove(r,onlyCaptures,who,cell,0,distance,pair[0],pair[1],top,moving);
					if(some!=null) { return some; }
				}}
				break;
			case Blank:
				if(onlyCaptures) { return null; }
				{
				int offset = r.nextInt(8);
				for(int i=0;i<8;i++)
				{
				int pair[] = allDirections[(i+offset)%8];
				commonMove some = randomNormalMove(r,false,who,cell,0,distance,pair[0],pair[1],top,moving);	
				if(some!=null) { return some; }
				}
				}
				break;
				
			case Red:
				{
				int offset = r.nextInt(4);
				for(int i=0;i<4;i++)
				{	int pair[] = orthogonalDirections[(i+offset)%4];
					commonMove some = randomNormalMove(r,onlyCaptures,who,cell,0,distance,pair[0],pair[1],top,moving);
					if(some!=null) { return some; }
				}}
				break;
			}
		return(null);
    }

    // dropoffs after an initial pick, in play_drop_state 
    private boolean addInitialDrops(CommonMoveStack all,int who,pstack cell,int distance,piece top,pstack moving,int opponentHeight)
    {	Face topcolor = top.realTopColor();
    	boolean some = false;
    	if(winningMove!=null) { return true; }
		switch(topcolor)
			{
			default: throw G.Error("Not expecting "+topcolor);
			case Orange:
				{
					some |= addOrangeIntermediateMoves(all,who,cell,moving,1,0);
					if(some && (all==null)) { return(true); }
					some |= addOrangeIntermediateMoves(all,who,cell,moving,-1,0);
					if(some && (all==null)) { return(true); }
					some |= addOrangeIntermediateMoves(all,who,cell,moving,0,1);
					if(some && (all==null)) { return(true); }
					some |= addOrangeIntermediateMoves(all,who,cell,moving,0,-1);
					if(some && (all==null)) { return(true); }
				}
				break;
			case Blue:
				some |= addNormalMoves(all,who,cell,0,distance,1,1,top,moving);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,0,distance,1,-1,top,moving);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,0,distance,-1,1,top,moving);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,0,distance,-1,-1,top,moving);
				if(some && (all==null)) { return(true); }
				break;
			case Blank:
				some |= addNormalMoves(all,who,cell,0,distance,1,1,top,moving);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,0,distance,1,-1,top,moving);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,0,distance,-1,1,top,moving);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,0,distance,-1,-1,top,moving);
				if(some && (all==null)) { return(true); }
				// and add red moves
				//$FALL-THROUGH$
			case Red:
				some |= addNormalMoves(all,who,cell,0,distance,1,0,top,moving);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,0,distance,-1,0,top,moving);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,0,distance,0,1,top,moving);
				if(some && (all==null)) { return(true); }
				some |= addNormalMoves(all,who,cell,0,distance,0,-1,top,moving);
				if(some && (all==null)) { return(true); }
			
			}
		return(some);
	}
    
 private void buildInitialMoves(CommonMoveStack all,Hashtable<pstack,pstack>covered,
		 	int who,
    		piece first,String firstTopColor,String firstBottomColor,
    		piece second,String secondTopColor,String secondBottomColor)
    {		
    	for(pstack cell = allCells; cell!=null; cell=cell.next)
    		{
    		if((cell.size()==0) && isEdgeCell(cell) && covered.get(cell)==null)
    		{	
    			all.push(new plateaumove( cell.locus(),
    										100, 
    										firstTopColor+firstBottomColor+secondTopColor+secondBottomColor,
     										firstTopColor+firstBottomColor,
    										second.piecenumber+","+first.piecenumber,
    										who));
    		}
    	    	
    	}
    }	
 
private void addCoveredSpaces(Hashtable<pstack,pstack>covered,pstack from,int dx,int dy,int movingHeight)
{
	int ox = from.colNum();
	int oy = nrows-from.rowNum()-1;
	boolean done = false;
	for(int distance = 1; !done && distance<movingHeight; distance++)
	{
		int x = ox + distance*dx;
		int y = oy + distance*dy;
		if(validBoardPos(x,y))
    		{	pstack target = getBoardXY(x,y);
    			covered.put(target,target);
    		}
		else {	done = true;
		}
	}
}
private void addCoveredSpaces(Hashtable<pstack,pstack>covered,pstack start,int [][]directions,int distance)
{
		for(int dir[] : directions)
		{
			addCoveredSpaces(covered,start,dir[0],dir[1],distance);
		}
}
	
private void addCoveredSpaces(Hashtable<pstack,pstack>covered,pstack start,int who)
{
	int own = start.topOwner();
	if(own==(who^1))
	{
		Face top = start.realTopColor();
		switch(top)
		{
		default:
		case Orange: break;	// don't worry about twisters
		case Blank: break;
		case Red:
			addCoveredSpaces(covered,start,orthogonalDirections,3);
			break;
		case Blue:
			addCoveredSpaces(covered,start,diagonalDirections,3);
			break;
			
		}
	}
}
//
// build spaces that are directly covered by red or blue tops
// this is a big hammer to prevent playing the initial stack
// where it can just be captured.
//
private Hashtable<pstack,pstack> buildCoveredSpaces(int whoseTurn)
{
	Hashtable<pstack,pstack>covered = new Hashtable<pstack,pstack>();
	for(int lim=cellArray.length-1; lim>=0; lim--)
	{
		pstack p = cellArray[lim];
		addCoveredSpaces(covered,p,whoseTurn);
	}
	return covered;
}
private void addInitialMoves(CommonMoveStack all,int whoseTurn)
{	pstack pieces[] = rack[whoseTurn];
	int ntypes = pieces.length;
	Hashtable<pstack,pstack> covered = buildCoveredSpaces(whoseTurn);
	for(int i1=0;i1<ntypes;i1++)
	{	pstack firstPiece = pieces[i1];
		piece firstTop = firstPiece.topElement();
		Face firstTopFace = firstTop.realTopColor();
		Face firstBottomFace =firstTop.realBottomColor();
		for(int i2 = i1; i2<ntypes; i2++)
		{
			pstack secondPiece = pieces[i2];
				piece secondTop = firstPiece!=secondPiece
						? secondPiece.topElement()
						: (secondPiece.size()>1) ? secondPiece.elementAt(0) : null;
			
				if(secondTop!=null)
				{		
				String firstTopColor = firstTop.realTopColorString();
				String firstBottomColor = firstTop.realBottomColorString();
				String secondTopColor = secondTop.realTopColorString();
				String secondBottomColor = secondTop.realBottomColorString();
				if(firstTopFace!=Face.Orange)
				{
				buildInitialMoves(all,covered,whoseTurn,firstTop,firstTopColor,firstBottomColor,
							secondTop,secondTopColor,secondBottomColor);
				if(!secondTop.isMonoColor())
				{	buildInitialMoves(all,covered,whoseTurn,firstTop,firstTopColor,firstBottomColor,
						secondTop,secondBottomColor,secondTopColor);
				}
				}
				if(!firstTop.isMonoColor() && (firstBottomFace!=Face.Orange))
				{
					buildInitialMoves(all,covered,whoseTurn,firstTop,firstBottomColor,firstTopColor,
							secondTop,secondTopColor,secondBottomColor);
					if(!secondTop.isMonoColor())
					{
						buildInitialMoves(all,covered,whoseTurn,firstTop,firstBottomColor,firstTopColor,
    							secondTop,secondBottomColor,secondTopColor);
					}
				}
				}
 			}
		}
	}
	private boolean shouldStartExchange()
	{
		return (pointTotal(bar[whoseTurn])>=Math.max(5,minimumTradeOffer[whoseTurn]))
				&& (pieceTotal(bar[whoseTurn^1])>3)
				&& (pointTotal(bar[whoseTurn^1])>3);
	}
	/*
	 * the nature of moves for plateau is that winning moves are easy to recognise, but random
	 * playouts can find a lot of other things to do instead.  The strategy here is to recognise
	 * winning moves, and if present, return only the winning move instead of the whole menagerie.
	 */
	plateaumove winningMove = null;
	public CommonMoveStack GetListOfMoves(Random robotRandom,boolean includeRedundentFlip) 
	{	CommonMoveStack all = new CommonMoveStack();
		winningMove = null;
		switch(board_state)
		{
		default:throw  G.Error("Not expecting "+board_state);
		case RESIGN_STATE:
			addDoneMove(all,whoseTurn);
			break;
		case ONBOARD2_STATE:
			addInitialMoves(all,whoseTurn);
			break;
		
		case CAPTIVE_SHUFFLE_STATE:
			addExchangeMoves(all,whoseTurn);
			break;
			
		case EXCHANGE_STATE:		
			addExchangeResponseMoves(all,whoseTurn);
			break;
			
		case PLAY_DROP_STATE:
			if(moveStep==0) { addInitialDrops(all,whoseTurn); }
			else { addContinuationDropMoves(all,whoseTurn); }
			break;
			
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
			addBoardDropMoves(all,whoseTurn);
			break;
		case PLAY_DONE_STATE:
		case PLAY_CAPTURE_STATE:
			addOptionalDone(all);
			//$FALL-THROUGH$
		case PLAY_UNDONE_STATE:
			addIntermediatePickMoves(all,movingStackOrigin[moveStep],whoseTurn);
			break;
		case PLAY_STATE:	
		case PLAY_NOEXCHANGE_STATE:
			winningMove = winningOnboardDropMove(whoseTurn);
			if(winningMove==null)
			{
			pstack losing = winningDropLocation(whoseTurn^1);
			addFlipMoves(all,whoseTurn); 
			
			if(losing==null)
			{
			addOnboardPickMoves(all,whoseTurn);
			if(winningMove==null 
					&& (board_state!=PlateauState.PLAY_NOEXCHANGE_STATE)
					&& shouldStartExchange()
					) 
				{ 	// start an exchange if its vaguely sensible
					addExchangeMoves(all,whoseTurn); 
				}
			}
			addInitialBoardPickMoves(all,whoseTurn);
			if(all.size()==0)
			{	if(losing!=null) { all.push(new plateaumove(MOVE_RESIGN,whoseTurn)); }
				else { 	addOptionalDone(all); }
			}}
			break;

		case FLIPPED_STATE:
			if(winningMove==null) { addInitialBoardPickMoves(all,the_flipped_stack,whoseTurn); }
			if(all.size()==0) 
			{	addDoneMove(all,whoseTurn);
			}
		}
		if(winningMove!=null)
			{ all.clear(); 
			  all.push(winningMove); 
			}
		
		// this is a check for debugging getRandomMove, which is complicated
		// and largely duplicates the whole algorithm.  verify that the random
		// move is one of the non-random moves
		/*
		Random r = new Random(112);
		for(int i=0;i<all.size();i++)
		{
			plateaumove m = (plateaumove)getRandomMove(r);
			if(m!=null)
			{	boolean some = false;
				for(int lim = all.size()-1; !some && lim>=0; lim--)
				{
					plateaumove a = (plateaumove)all.elementAt(lim);
					some |= m.Same_Move_P(a);
				}
				if(!some)
				{
					G.print("Mismatch ",m);
				}
			
			}
		}
	*/
		return all;
	}
	public CommonMoveStack GetListOfMoves() {
		CommonMoveStack all = GetListOfMoves(new Random(),true);
		return all;
	}
	
	public commonMove getRandomMove(Random r)
	{
		switch(board_state)
		{
		default: return null;
		case ONBOARD_DROP_STATE:
			return randomDropMove(r,whoseTurn);
			
		case PLAY_DROP_STATE:
			// drop after pick on the board
			if(moveStep==0)
			{
			if(r.nextInt()<0.9)	// greatly prefer capture moves
				{
				commonMove m = randomDropFromBoardMove(r,true,whoseTurn);
				if(m!=null) { return m; }
				}
			return randomDropFromBoardMove(r,false,whoseTurn);
			}
			
			return randomContinuationDropMove(r,whoseTurn);
			
		case FLIPPED_STATE:
			{
			commonMove m = randomInitialBoardPickMove(r,the_flipped_stack,whoseTurn);
			if(m==null) { return new plateaumove(MOVE_DONE,whoseTurn); }
			return m;
			}
		case PLAY_CAPTURE_STATE:
			if(r.nextDouble()<0.75) { return getOptionalDone(); }	
			return randomIntermediatePickMove(r,movingStackOrigin[moveStep],whoseTurn);
			
		case PLAY_DONE_STATE:	
			if(r.nextDouble()<0.2) { return getOptionalDone(); }	
			return randomIntermediatePickMove(r,movingStackOrigin[moveStep],whoseTurn);
			
		case PLAY_UNDONE_STATE:
			// we have to continue moving
			return randomIntermediatePickMove(r,movingStackOrigin[moveStep],whoseTurn);
					
		case PLAY_STATE:
		case PLAY_NOEXCHANGE_STATE:
			{
			plateaumove m = winningOnboardDropMove(whoseTurn);
			if(m!=null) { return m; }
			}
			pstack losing = winningDropLocation(whoseTurn^1);

			if(r.nextDouble()<(losing==null ? 0.5 : 0.8))
				{ commonMove m = randomInitialBoardPickMove(r,false,whoseTurn);	// potential captures
				  if(m!=null) { return m; }
				}
			if(r.nextDouble()<0.3)
			{ commonMove m = randomFlipMove(r,whoseTurn);
			  if(m!=null) { return m; }
			}
			if(losing!=null || r.nextDouble()<0.5)
			{ commonMove m = randomInitialBoardPickMove(r,true,whoseTurn);	// potential non captures
			  if(m!=null) { return m; }
			}
			
			if((board_state!=PlateauState.PLAY_NOEXCHANGE_STATE) 
					&& (losing==null)
					&& shouldStartExchange()
					&& r.nextDouble()<0.1)
			{
				commonMove m = randomExchangeMove(r,whoseTurn);
				if(m!=null) { return m; }
			}
			if(losing==null)
			{
			return randomRackPickMove(r,whoseTurn);
			}
			return null;
			
		}
	}
	public void randomizeHiddenState(Random r,int forPlayer)
	{	int opponent = forPlayer^1;
		PieceStack known = new PieceStack();
		PieceStack unknown = new PieceStack();
		PieceStack available = new PieceStack();
		PieceStack compatible = new PieceStack();
		int typemask = 0;
		for(pstack rp : rack[opponent])
			{ for(int lim = rp.size()-1; lim>=0; lim--)
				{piece sp = rp.elementAt(lim);
				 int bit = 1<<sp.pieceType.ordinal();
				 if((typemask & bit) == 0)
				 {
				 available.push(sp);
				 typemask |= bit;
				 }
				}
			}
		for(int lim=cellArray.length-1; lim>=0; lim--)
		{
			pstack s= cellArray[lim];
			for(int idx = s.size()-1; idx>=0; idx--)
			{
				piece p= s.elementAt(idx);
				G.Assert(p.mystack==s,"should be mystack");
				if(p.owner==opponent)
				{
					available.remove(p);
					if(p.topKnown(forPlayer)
							&& p.bottomKnown(forPlayer))
					{
						known.push(p);
						available.remove(p);
					}
					else
					{
						unknown.push(p);
					}
				}
			}
		}
		//G.print("\nrandomize. unknown "+unknown);
		for(int lim=unknown.size()-1; lim>=0; lim--)
		{	
			piece unknownPiece = unknown.elementAt(lim);
			compatible.clear();
			for(int av = available.size()-1; av>=0; av--)
			{
				piece candidate = available.elementAt(av);
				if(candidate.compatibleWith(unknownPiece,forPlayer))
				{	candidate.compatibleWith(unknownPiece,forPlayer);
					compatible.push(candidate);
				}
			}
			if(compatible.size()>0)
			{
			int sz = compatible.size();
			int rindex = r.nextInt(sz+1);
			if(rindex<sz)	// allow for not swapping!
			{
			piece replacement = compatible.elementAt(rindex);
			//G.print("swap ",unknownPiece,"@",unknownPiece.mystack," for ",replacement);
			// now adjust the visibility of the swapped piece
			boolean ktop = unknownPiece.topKnown(forPlayer);			
			boolean kbot = unknownPiece.bottomKnown(forPlayer);
			boolean oktop = unknownPiece.topKnown(opponent);		
			boolean okbot = unknownPiece.bottomKnown(opponent);
			if(ktop) 
				{ Face top = unknownPiece.realTopColor();
				  if (replacement.realTopColor()!=top)
					{//G.print("flip "+replacement);
					  replacement.flip(); 
					}
				  G.Assert(top==replacement.realTopColor(),"should match top");
				}
			else if(kbot)
				{	Face bot = unknownPiece.realBottomColor();
					if(replacement.realBottomColor()!=bot)
					{ //G.print("flip "+replacement);
					  replacement.flip();
					}
					G.Assert(replacement.realBottomColor()==bot,"should match bottom");
				}
		//	if( !ktop && !kbot && r.nextBoolean() ) { replacement.flip(); }
			
			// replacement top or bottom now matches 
			replacement.anonymize();
			if(ktop) { replacement.setTopKnown(forPlayer); }
			if(kbot) { replacement.setBottomKnown(forPlayer); }
			if(oktop) { replacement.setTopKnown(opponent); }
			if(okbot) { replacement.setBottomKnown(opponent); }
			unknownPiece.revealAll();		
			
			unknownPiece.mystack.swap(unknownPiece,replacement);
			available.remove(replacement);
			available.push(unknownPiece);
			}
			}
			
		}
	}
}