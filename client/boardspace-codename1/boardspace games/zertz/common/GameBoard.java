package zertz.common;

import lib.*;
import online.game.*;
import zertz.common.GameConstants.ZertzState;

class StateStack extends OStack<ZertzState>
{
	public ZertzState[] newComponentArray(int sz) {
		return new ZertzState[sz];
	}

}
public class GameBoard extends hexBoard<zCell> implements BoardProtocol,GameConstants
{
    static final String[] ZERTZ_GRID_STYLE = { null, "A1", "A1" };
    static final int NEXTRACOLORS = 1; //extra ball colors (ie; undecided.  This isn't in use now.)
	private ZertzState unresign;
	private ZertzState board_state;
	public ZertzState getState() {return(board_state); }
	public void setState(ZertzState st) 
	{ 	unresign = (st==ZertzState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	CellStack animationStack = new CellStack();
	
	// this is arranged so each successive ball is worth more than the previous,
    // so getting blacks from 2 to 1 is worth more than getting greys from 4 to 3
    // and so on.  Exact numbers are complicated by the 3 ball win.
    // the basic pattern here is a fibbonachi series.
    static double[] need_weight = 
        {
            0.025, 0.018, 0.0083, 0.0041, 0.0025, 0.0015, 0.0009, 0.0005, 0.0003,
            0.0001, 0.0
        };
    static double[] minweight = { 1.0, 0.8, 0.4, 0.2, 0.1, 0.05, 0.0 };
    static double white_value = 1.2;
    static double grey_value = 1.1;
    static double black_value = 1.0;
    private boolean swapped_state = false;
    public int placementIndex = 0;
    public Zvariation variation=Zvariation.Zertz;
    public Zvariation boardSetup=Zvariation.Zertz;
    public int movingObjectIndex() { return(NothingMoving); }
 
    final int winning_total = 3; //number of balls needed to win
    public void SetDrawState() 
    	{ setState(ZertzState.DRAW_STATE); }
    //dimensions for the viewer to use
    public int[][] balls = new int[3][NCOLORS + NEXTRACOLORS]; //0 is reserve 1 is player1 2 is player2
    public zCell rack[][] = new zCell[3][NCOLORS + NEXTRACOLORS];
    
    private zCell[] Captures=null; 		// capture stack used by the robot
    private int[] CaptureColor=null;	// stack of colors of captured stones
    private ZertzState[] CaptureState=null;
    private int Capture_Index = 0;
    public int ballPlacedFromRack; // if placing a ball, where it came from
    public boolean needStart = true;
    // usually the reserve, but not always!
    public zCell ballPlaced=null;	//cell where the ball is placed
    public zCell ringRemoved=null;	//cell where ring was removed
    public zCell[] ball_location=null; //locations of balls on the board
    public int balls_on_board = 0;  //number of balls on the board
    public int balls_in_play = 0;	// number on board or captured
    public int rings_removed = 0;	// rings removed from the board
    public movespec lastMove = null;

    // factory method to create the right kind of cell
    public zCell newcell(char col,int row)
    {	return(new zCell(col,row));
    }
    public GameBoard(String init) // default constructor
    {	for(int row = 0;row<rack.length;row++)
    	{	// these are used as animation targets
    		zCell rowcell[] = rack[row];
    		ZertzId id = BallIds[row];
    		for(int col=0;col<rowcell.length;col++)
    {
    			rowcell[col] = new zCell(id,col);
    		}
    	}
        doInit(init);
    }
    public GameBoard cloneBoard() 
	{ GameBoard dup = new GameBoard(gametype); 
	  dup.copyFrom(this);
	  return(dup); 
   	}

    public void copyFrom(BoardProtocol b)
    { copyFrom((GameBoard)b); 
    }


    /*

      private interfaces which use X,Y (ie; real board coordinates)

    */
    private void InitBallsAndBoard()
    {	int totalballs = 10 + 8 + 6;
        balls[RESERVE_INDEX][zChip.WHITE_INDEX] = 6;
        balls[FIRST_PLAYER_INDEX][zChip.WHITE_INDEX] = 0;
        balls[SECOND_PLAYER_INDEX][zChip.WHITE_INDEX] = 0;
        balls[RESERVE_INDEX][zChip.GREY_INDEX] = 8;
        balls[FIRST_PLAYER_INDEX][zChip.GREY_INDEX] = 0;
        balls[SECOND_PLAYER_INDEX][zChip.GREY_INDEX] = 0;
        balls[RESERVE_INDEX][zChip.BLACK_INDEX] = 10;
        balls[FIRST_PLAYER_INDEX][zChip.BLACK_INDEX] = 0;
        balls[SECOND_PLAYER_INDEX][zChip.BLACK_INDEX] = 0;

        // under unusual circumstances, the number of balls captured in a search
        // can be more than the number of balls that exist, because players are
        // forced to start recycling their previously captured balls.
        Captures = new zCell[totalballs*2];	//the most that can be captured.
        CaptureColor = new int[totalballs*2];	// likewise
        CaptureState = new ZertzState[totalballs*2];
        Capture_Index = 0;

        setState(ZertzState.PUZZLE_STATE);
        char fill = boardSetup==Zvariation.Zertz_xx ? NoSpace : Empty;
        for(zCell c = allCells; c!=null; c=c.next)
        {	SetBoard(c,fill);
        } 

        ball_location = new zCell[totalballs+1];
        balls_on_board = 0;
        rings_removed = 0;
        balls_in_play = 0;
        swapped_state=false;
        whoseTurn = 0;
        board_state = ZertzState.PUZZLE_STATE;
    }

    // Remove a ball and remove from placed ball list
    private void RemoveBall(zCell bc)
    {
        for (int i = 0; i < balls_on_board; i++)
        {	
            if (ball_location[i]==bc)
            { //move down the last
                balls_on_board--;
                balls_in_play--;
                ball_location[i] = ball_location[balls_on_board];
                ball_location[i] = ball_location[balls_on_board];

                return;
            }
        }

        throw G.Error("Ball not found on board");
    }
    
    public zCell lastDroppedDest = null;
    // Set the contents of a board location, keep ball list up to date
    public char SetBoard(zCell c, char contents)
    {
        char old = c.contents;
        if (zChip.BallColorIndex(old) >= 0)     {   RemoveBall(c);    }
        if (zChip.BallColorIndex(contents) >= 0) {  AddBall(c);   }
        if(old==NoSpace) { if(contents==Empty) { rings_removed--; }}
        else if(old==Empty) {if(contents==NoSpace) { rings_removed++; }}
        c.contents = contents;
        lastDroppedDest = (contents==Empty)?null:c;
        return(old);
    }
    public char SetBoardPos(char col,int row,char contents)
    {	return(SetBoard(getCell(col,row),contents));
    }
    public zCell getCell(zCell from)
    {	if(from==null) { return(null); }
     	switch(from.rackLocation())
    	{
     	case EmptyBoard:	return(getCell(from.col,from.row));
     	case White:
     		return(rack[0][from.row]);
     	case Gray:
     		return(rack[1][from.row]);
     	case Black:
     		return(rack[2][from.row]);
    	default:
    		throw G.Error("Not expecting "+from.rackLocation);
    	}
    }
    void incBalls(int row,int col)
    {	int p0 = balls[row][col];
    	balls[row][col]=balls[row][col]+1;
    	int p1 = balls[row][col];
    	G.Assert(p1==p0+1,"inc error sb "+(p0+1)+" is "+p1);
    } 
    void decBalls(int row,int col)
    {	int p0 = balls[row][col];
		balls[row][col] = balls[row][col]-1;
		int p1 = balls[row][col];
		G.Assert(p1==p0-1,"dec error sb "+(p0+1)+" is "+p1);
    }
    // remove balls isolated by removing a tile, and balls captured by moving a ball.
    // captured balls are marked with lower case letters. This isn't effectient; it
    // just scans the board, but no matter because it works in human time rather than
    // robot time.
    private void RemoveCapturedBalls(replayMode replay)
    {
        for (int i = balls_on_board - 1; i >= 0; i--)
        { //run for top down so if balls are removed, we've
          // already processed them.   At one thime there was
          // a subtle bug because the ball_location array
          // was shuffled by "temporary" manipulations done 
          // by setBoardXY called from isViable.

            zCell c = ball_location[i];
            char ch = c.contents;
            int col = CapturedColorIndex(ch);

            if (col >= 0)
            {
                incBalls(whoseTurn,col);
                c.lastCaptured = placementIndex-1;
                c.lastContents = c.contents;
                
                SetBoard(c,Empty); // turn captured balls into an Empty space
                if(replay!=replayMode.Replay)
                {	zCell d = rack[whoseTurn][col];
                	animationStack.push(c);
                	animationStack.push(d);
                	d.contents = ch;
                }
            }
            else if (!IsViable(c))
            {
                int color = zChip.BallColorIndex(ch);
                incBalls(whoseTurn,color); // move to player's rack
                c.lastCaptured = placementIndex-1;
                c.lastContents = c.contents;
                SetBoard(c, NoSpace); // turn isolated spaces into nowhere
                if(replay!=replayMode.Replay)
                {	zCell d = rack[whoseTurn][color];
                	animationStack.push(c);
                	animationStack.push(d);
                	d.contents = ch;
                }
            }
        }

        return;
    }

    // count the spaces adjacent to a cell which are still on the board
    public int SpaceCount(zCell c)
    {
        int n = 0;
        char contents = c.contents;

        if (contents == NoSpace)
        {
            return (-1);
        }
        for(int i=0;i<6;i++) 
        { zCell cn = c.exitTo(i);
          char cc = (cn==null) ? NoSpace : cn.contents;
          if(cc!=NoSpace) { n++; }
        }
        return (n);
    }

    // "viable" balls have an empty space in their island
    private boolean IsViable(zCell c)
    {
        char contents = (c==null) ? NoSpace : c.contents;

        if (contents == NoSpace)
        {
            return (false);
        }

        if (contents == Empty)
        {
            return (true);
        }

        if (contents != Marker)
        { //note, we use the super function in order to skip the bookeeping
          //for ball locations.  We don't need it, and the shuffling of the
          //balllocation array that happens causes a subtle bug in detecting
          //islands.
            c.contents=Marker; // temporarily change the marker 

            for (int dir = 0; dir < CELL_FULL_TURN; dir++)
            {	zCell nc = c.exitTo(dir);
            	if(IsViable(nc))
                {
                    c.contents=contents; 	// skip bookkeeping, return to original state
                   //System.out.println("IsViable "+XtoBC(x,y)+","+YtoBC(x,y)+" true");
                    return (true);
                }
            }

            c.contents=contents; //skip bookkeeping
        }

        //System.out.println("IsViable "+XtoBC(x,y)+","+YtoBC(x,y)+" false");
        return (false);
    }

    // "viable" balls have an empty space in their island
    private boolean isViableWithout(zCell c,zCell filled,zCell removed)
    {
        char contents = ((c==null)||(c==removed)) ? NoSpace : c.contents;
        if(c!=filled)
        {
        if (contents == NoSpace)
        {
            return (false);
        }

        if (contents == Empty)
        {
            return (true);
        }}

        if (contents != Marker)
        { //note, we use the super function in order to skip the bookeeping
          //for ball locations.  We don't need it, and the shuffling of the
          //balllocation array that happens causes a subtle bug in detecting
          //islands.
            c.contents=Marker; // temporarily change the marker 

            for (int dir = 0; dir < CELL_FULL_TURN; dir++)
            {	zCell nc = c.exitTo(dir);
            	if(isViableWithout(nc,filled,removed))
                {
                    c.contents=contents; 	// skip bookkeeping, return to original state
                   //System.out.println("IsViable "+XtoBC(x,y)+","+YtoBC(x,y)+" true");
                    return (true);
                }
            }

            c.contents=contents; //skip bookkeeping
        }

        //System.out.println("IsViable "+XtoBC(x,y)+","+YtoBC(x,y)+" false");
        return (false);
    }
    
    // 
    boolean CanBeIsolated(zCell c)
    {	
        for (int dir = 0; dir < CELL_FULL_TURN; dir++)
        {	zCell nc = c.exitTo(dir);
        	char ncontents = (nc==null) ? NoSpace : nc.contents;
        	if((ncontents==Empty) && RingCanChange(nc) && !isViableWithout(c,c,nc)) { return(true); }
        }
       return (false);
    }
    // Add a ball and record location
    private final void AddBall(zCell c)
    {
        ball_location[balls_on_board] = c;
        balls_on_board++;
        balls_in_play++;
    }
    // return true if any capture is possible from cell c if it were filled
    private boolean Capture_Is_Possible_Of_Cell(zCell c)
    {	int empties = 0;
    	zCell emptyCell = null;
        for (int direction = 0; direction < 6; direction++)
        {	zCell nc = c.exitTo(direction);
            char color2 = nc==null ? NoSpace : nc.contents; // adjacent tile contents
            if(color2==Empty) { empties++; emptyCell = nc; }
            else if(color2 !=NoSpace)
            {	zCell nc2 = c.exitTo(direction+3);
                char color3 = (nc2==null) ? NoSpace : nc2.contents;
                if (color3 == Empty)
                {
                    return (true);
                }
            }
        }
        if(empties<=1 && !isViableWithout(c,c,emptyCell)) { return(true); }
        return (false);
    }
    // return true if any capture is possible from cell c if it were filled
    private boolean Capture_Is_Possible_From_Cell(zCell c, char[] colors)
    {	
        for (int direction = 0; direction < 6; direction++)
        {	zCell nc = c.exitTo(direction);
            char color2 = nc==null ? NoSpace : nc.contents; // adjacent tile contents
            int cindex = -1;
            int len = colors.length;

            for (int i = 0; i < len; i++)
            {
                if (colors[i] == color2)
                {
                    cindex = i;

                    break;
                }
            }

            if (cindex >= 0)
            {	zCell nc2 = nc.exitTo(direction);
                char color3 = (nc2==null) ? NoSpace : nc2.contents;
                if (color3 == Empty)
                {
                    return (true);
                }
            }
        }

        return (false);
    }

    /*
        Public interfaces which use Column,Row addresses
    */


    public void sameboard(BoardProtocol f) { sameboard((GameBoard)f); }

    public void sameboard(GameBoard from_b)
    {   super.sameboard(from_b);
    	G.Assert(variation==from_b.variation,"variation mismatch");
    	G.Assert(boardSetup==from_b.boardSetup,"boardSetup mismatch");
        G.Assert(balls_on_board == from_b.balls_on_board,"same balls on board");
        G.Assert(balls_in_play == from_b.balls_in_play,"same balls in play");
        G.Assert(rings_removed == from_b.rings_removed,"same rings removed");
        G.Assert(swapped_state == from_b.swapped_state,"same swapped");
        
        for (int i = 0, len = balls[0].length; i < len; i++)
        {
            if (!((balls[RESERVE_INDEX][i] == from_b.balls[RESERVE_INDEX][i]) &&
                    (balls[FIRST_PLAYER_INDEX][i] == from_b.balls[FIRST_PLAYER_INDEX][i]) &&
                    (balls[SECOND_PLAYER_INDEX][i] == from_b.balls[SECOND_PLAYER_INDEX][i])))
            {
            	throw G.Error("Ball mismatch at %s", i);
            }
        }
    }

    private long Digest_Rack(Random r, int[] rack)
    {
        long v = 0;

        for (int i = 0; i < rack.length; i++)
        {
            int ct = rack[i];

            for (int j = 0; j < 12; j++)
            {
                long val0 = r.nextLong();

                if (j < ct)
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
        Random r = new Random(64 * 1000);
        for(zCell cel = allCells; cel!=null; cel=cel.next)
            {
                long val0 = r.nextLong();
                long val1 = r.nextLong();
                long val2 = r.nextLong();
                long val3 = r.nextLong();
                switch (cel.contents)
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

                case Grey:
                    v ^= val3;
                    break;
				default:
					break;
                }
            }
        v ^= Digest_Rack(r, balls[FIRST_PLAYER_INDEX]);
        v ^= Digest_Rack(r, balls[SECOND_PLAYER_INDEX]);
        if(swapped_state) { v = ~v; }
        v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }

    /* make a copy of a board */
    public void copyFrom(GameBoard from_b)
    {	
   		super.copyFrom(from_b);
 
    	needStart = from_b.needStart;
    	swapped_state = from_b.swapped_state;
        balls_on_board = from_b.balls_on_board;
        balls_in_play = from_b.balls_in_play;
        rings_removed = from_b.rings_removed;
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        AR.copy(balls,from_b.balls);
        AR.copy(CaptureColor,from_b.CaptureColor);
        AR.copy(CaptureState,from_b.CaptureState);
        getCell(Captures,from_b.Captures);       
        getCell(ball_location,from_b.ball_location);
        copyFrom(rack,from_b.rack);

        {
        zCell bp = from_b.ballPlaced;
        if(bp!=null) { bp = getCell(bp.col,bp.row); }
        ballPlaced = bp;
    	}
        ballPlacedFromRack = from_b.ballPlacedFromRack;
        {
        zCell rr = from_b.ringRemoved;
        if(rr!=null) { rr=getCell(rr.col,rr.row); }
        ringRemoved = rr;
        }
        placementIndex = from_b.placementIndex;
        sameboard(from_b);
    }
    public void setBoardType(Zvariation v)
    {
    	boardSetup = v;
    	reInitBoard(v.firstInCol,v.nInCol,null);
    	moveNumber = 1;
        InitBallsAndBoard();
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {   Zvariation variation = Zvariation.find(gtype);
    	G.Assert(variation!=null,WrongInitError,gtype);
    	doInit(gtype,variation,variation.boardSetup,key);
    }
    public void doInit()
    {
    	doInit(gametype,variation,boardSetup,randomKey);
    }
    private void doInit(String gt,Zvariation v,Zvariation bs,long k)
    {	gametype = gt;
    	variation = v;
    	randomKey = k;
    	setBoardType(bs);
    	
    	swapped_state = false;
    	needStart = true;
    	Grid_Style = ZERTZ_GRID_STYLE;
    	drawing_style = DrawingStyle.STYLE_NOTHING;
    	placementIndex = 1;
        }
    // this override is unusual, but necessary to account for 
    // the basic board shape changing after init
    public void doInit(GameBoard b,long randomv)
    {	
    	doInit(b.gametype,b.variation,b.boardSetup,b.randomKey);
    }

    // true if two adjacent sides are free, ie if it's an edge
    public boolean twoSides(zCell c)
    {

        for (int i = 0; i < 6; i++)
        {	zCell nc = c.exitTo(i);
        	char contents = (nc==null) ? NoSpace : nc.contents;

            if (contents == NoSpace)
            {	zCell nc1 = c.exitTo(i+1);
            	char contents1 = (nc1==null) ? NoSpace : nc1.contents;
                if (contents1 == NoSpace)
                {
                    return (true);
                }
            }
        }

        return (false);
    }
    public boolean isEdgeCell(zCell c)
    {	for(int dir=0;dir<6;dir++)
    	{
    	zCell nc = c.exitTo(dir);
    	char con = (nc==null) ? NoSpace : c.contents;
    	if(con==NoSpace) { return(true); }
    	}
    	return(false);
    }
    public boolean RingNextToBall(zCell c)
    {

        for (int dir = 0; dir < 6; dir++)
        {	zCell nc = c.exitTo(dir);
            char ch = (nc==null) ? NoSpace : nc.contents;

            if (!((ch == NoSpace) || (ch == Empty)))
            {
                return (true);
            }
        }

        return (false);
    }

    // initially true if it's an edge, or if a temporary ring was removed if its the one.
    public boolean RingCanChange(zCell c)
    {
        switch (board_state)
        {
        case SETRING_STATE:
        	{
        	char sp = c.contents;
        	return((sp==NoSpace)||(sp==Empty));
        	}
        case PUZZLE_STATE:
        {
            char sp = c.contents;

            return ((sp == NoSpace) ? true : ((sp == Empty) && twoSides(c)));
        }

        case MOVE_OR_SWAP_STATE:
        case MOVE_STATE:
        case RING_STATE:
        {
            char gb = c.contents;

            return ((gb == Empty) && twoSides(c));
        }
        case SWAP_CONFIRM_STATE:
        case DONE_STATE:
        case BALL_STATE:
            return (c==ringRemoved);

        default:
            return (false);
        }
    }

    // one of the marginal conditions that is possible is that no rings
    // are mobile, in that case we have to skip "ring" state
    public boolean AnyRingCanChange()
    {	for(zCell c = allCells; c!=null; c=c.next)
    	{	if((c.contents==Empty) && twoSides(c)) { return(true); }
    	}
        return (false);
    }

    public char CaptureBetween(zCell between)
    {
       char mid = between.contents;
       int midc = zChip.BallColorIndex(mid);
       int midcap = CapturedColorIndex(mid);
       between.lastContents = mid;
       between.lastCaptured = placementIndex;
       
       if (midc >= 0)
          {
    	   return ((midcap >= 0) ? BallChars[midc]   : CapturedBallChars[midc]);
          }
 
        return (NoSpace);
    }
    public zCell MidBetween(zCell from,zCell to)
    {	for(int dir=0;dir<6;dir++)
    	{	zCell nc1 = from.exitTo(dir);
    		if(nc1!=null) 
    		{	zCell nc2 = nc1.exitTo(dir);
    			if(nc2==to) { return(nc1); }
    		}
    	}
    return(null);
    }
    public boolean CaptureOccurs(zCell from,zCell between,zCell dest,boolean orUncapture)
    {  
       if ((dest!=null) && (dest.contents == Empty))
        {
            switch (board_state)
            {
            case CAPTURE_STATE:
            case CONTINUE_STATE:
            case DONE_CAPTURE_STATE:
            {	if(between==null) { between=MidBetween(from,dest); }
                char src = from.contents;
                int color = UncapturedColorIndex(src);

                if ((between!=null) && (color >= 0))
                {
                    char mid = CaptureBetween(between);

                    // capturebetween returns the new char to place
                    // so for captures it's a captured color
                    if ((mid != NoSpace) &&
                            (orUncapture || (CapturedColorIndex(mid) >= 0)))
                    {
                        return (true);
                    }
                }
            	}
				//$FALL-THROUGH$
			case RESIGN_STATE:
            case GAMEOVER_STATE:
                return (false);

            default:
                return (true);
            }
        }

        return (false);
    }
    public boolean BallCanMove(char col,int row)
    {	return(BallCanMove(getCell(col,row)));
    }
    // true if a ball on the board can be picked up and moved
    public boolean BallCanMove(zCell c)
    {
        switch (board_state)
        {
        case PUZZLE_STATE:
            return (true);

        case MOVE_STATE:
        case BALL_STATE: // no temporary ball placed
        case GAMEOVER_STATE:
            return (false);

        case CAPTURE_STATE:
        {
            int color = zChip.BallColorIndex(c.contents);

            return ((color >= 0) && Capure_Is_Possible_From(c));
        }

        case DONE_STATE:
        case RING_STATE:
        case DONE_CAPTURE_STATE:
        case CONTINUE_STATE:
            return (c==ballPlaced);
		default:
			break;
        }

        return (false);
    }

    // move a ball from the board to a rack.  If a temporary ball was placed, only
    // that ball can be moved.
    public final void MoveBtoR(movespec m,replayMode replay)
    {	char movingBoardCol = m.from_col;
    	int movingBoardRow = m.from_row;
    	int movingRack = m.to_rack;
    	zCell c = getCell(movingBoardCol, movingBoardRow);
        char oldboard = c.contents;
        int idx = zChip.BallColorIndex(oldboard);
        if(replay==replayMode.Single)
        {
        	animationStack.push(c);
        	animationStack.push(rack[whoseTurn][idx]);
        }
        m.movedAndCaptured = idx;
        if (idx >= 0)
        {
            incBalls(movingRack,idx);
            SetBoard(c, Empty);
            
           switch (board_state)
            {
            case PUZZLE_STATE:
                break;

            case RING_STATE:
            case DONE_STATE:

                if (ballPlaced!=c)
                {
                	throw G.Error("Moving the wrong ball");
                }

                if (movingRack != ballPlacedFromRack)
                {
                	throw G.Error("Moving to the wrong rack");
                }
                setState(((boardSetup==Zvariation.Zertz_xx) && (balls_in_play==0) && (rings_removed==0) && !swapped_state)
                		? ZertzState.MOVE_OR_SWAP_STATE
                	    :  (((board_state == ZertzState.DONE_STATE)
                	    		&& ( (ringRemoved==null) ? !AnyRingCanChange() : true))
                   	    	? ZertzState.BALL_STATE 
                	    	: ZertzState.MOVE_STATE));

                break;

            default:
            	throw G.Error("Ball Not Placed");
            }


            return;
        }

        throw G.Error("Board has no ball");
    }

    // removing a ring, if no temporary ring already removed
    public void RemoveRing(char col, int row)
    {	zCell c = getCell(col,row);
        char oldc = c.contents;;
        G.Assert(oldc == Empty, "Ring was not present");

        switch (board_state)
        {
        case PUZZLE_STATE:
        case SETRING_STATE:
            break;
        case MOVE_OR_SWAP_STATE:
        case RING_STATE:
        case MOVE_STATE:
        	ringRemoved = c;
            setState((board_state == ZertzState.RING_STATE) ? ZertzState.DONE_STATE : ZertzState.BALL_STATE);

            break;

        default:
        	throw G.Error("Ring removal not allowed now, state %s",board_state);
        }
        previousLastEmptied = c.lastEmptied;
        c.lastEmptied = placementIndex;
        placementIndex++;
        SetBoard(c, NoSpace);
    }

    private int previousLastEmptied;
    // add a ring, if a temporary removal was done, only that ring can be put back
    public void AddRing(char col, int row)
    {	zCell c = getCell(col,row);
        char oldc = c.contents;
        G.Assert(oldc == NoSpace, "Ring was already present");

        switch (board_state)
        {
        case SETRING_STATE:
        case PUZZLE_STATE:
            break;

        case DONE_STATE:
        case BALL_STATE:

            if (c!=ringRemoved)
            {
            	throw G.Error("Some other ring was removed");
            }
            ringRemoved = null;
            setState(((boardSetup==Zvariation.Zertz_xx) && (rings_removed==1) && (balls_in_play==0))
            				? ZertzState.MOVE_OR_SWAP_STATE
            				: ((board_state == ZertzState.BALL_STATE) ? ZertzState.MOVE_STATE : ZertzState.RING_STATE));

            break;

        default:
        	throw G.Error("No Ring already removed");
        }
        c.lastEmptied = previousLastEmptied;
        SetBoard(c, Empty);
    }

    // move a ball from one board position to another.  If a temporary
    // ball was placed, only that ball can be moved.
    public void MoveBtoB(movespec m,replayMode replay)
    {	
    	
    	char movingBoardCol = m.from_col;
    	int movingBoardRow = m.from_row;
    	char destBoardCol = m.to_col;
    	int destBoardRow = m.to_row;
    	
        zCell oldboard = getCell(movingBoardCol, movingBoardRow);
        char oldball = oldboard.contents;
        m.movedAndCaptured = zChip.BallColorIndex(oldball);
        int oldcolor = zChip.BallColorIndex(oldball);
        zCell newdest = getCell(destBoardCol, destBoardRow);

        if (oldboard==newdest)  {  return;  }

 
        G.Assert(newdest.contents == Empty, "Destination is not empty");
        G.Assert(oldcolor >= 0, "BtoB Board has no ball");

        if(replay==replayMode.Single)
        {
    	animationStack.push(oldboard);
    	animationStack.push(newdest);
        }


        //old position contains a ball, new position is empty.
        //if doing captures, we must also be capturing a ball
        switch (board_state)
        {
        case GAMEOVER_STATE:
        	if(replay!=replayMode.Replay)
        	{	// some old damaged games continue with forced moves
            	throw G.Error("Undefined state for MoveBtoB: %s",board_state); //gameover?        		
        	}
			//$FALL-THROUGH$
        case CAPTURE_STATE:
        case DONE_CAPTURE_STATE:
        case CONTINUE_STATE:
        {	
            zCell mid = MidBetween(oldboard,newdest);
            if (mid!=null)
            { 
            char oldColor = mid.contents;
            m.movedAndCaptured |= zChip.BallColorIndex(oldColor)<<4;
            char replaceChar=CaptureBetween(mid);
            SetBoard(mid, replaceChar);
            }
            else
            {
            	throw G.Error("No Ball to capture");
            }
        }
        oldboard.lastEmptied = placementIndex;
        newdest.lastPlaced = placementIndex;
        placementIndex++;
        SetBoard(newdest, oldboard.contents);
        SetBoard(oldboard, Empty);
        ballPlaced = newdest;

        setState(Capture_Is_Possible_From_Cell(newdest, BallChars)
            ? (Capture_Is_Possible_From_Cell(newdest, CapturedBallChars)
            ? ZertzState.CONTINUE_STATE : ZertzState.CAPTURE_STATE) : ZertzState.DONE_CAPTURE_STATE);

        break;

        case RING_STATE:
        case DONE_STATE: //moving a temporary ball

            if ((movingBoardCol != ballPlaced.col) ||
                    (movingBoardRow != ballPlaced.row))
            {
            	throw G.Error("Moving the wrong ball");
            }
            SetBoard(newdest, oldboard.contents);
            SetBoard(oldboard, Empty);
            ballPlaced = newdest;

            if(ringRemoved==null)
            { setState(AnyRingCanChange()?ZertzState.RING_STATE:ZertzState.DONE_STATE);
            }
            break;

        case PUZZLE_STATE:
        	{
        	char oldc = oldboard.contents;
            SetBoard(oldboard, Empty);
            SetBoard(newdest, oldc);
        	}
            break;
        case MOVE_STATE:
        	// this shouldn't happen, but does in some very old game records.
        	if((movingBoardCol==destBoardCol)&&(movingBoardRow==destBoardRow))
        	{	// nothing to do
        		break;
        	}
			//$FALL-THROUGH$
        default:
        	throw G.Error("Undefined state for MoveBtoB: "+board_state); //gameover?
        }
    }

    // move a ball from one rack to another
    public void MoveRtoR(movespec m,replayMode replay)
    {
    	int fromRackIndex = m.from_rack;
    	int movingRackIndex = m.color;
    	int toRackIndex = m.to_rack;
        if (fromRackIndex != toRackIndex)
        {
            if (board_state != ZertzState.PUZZLE_STATE)
            {
            	throw G.Error("Rack transfers not allowed");
            }

            int oldcount = balls[fromRackIndex][movingRackIndex];
            G.Assert(oldcount > 0, "Source bin is empty");
            decBalls(fromRackIndex,movingRackIndex);
            incBalls(toRackIndex,movingRackIndex);
            if(replay==replayMode.Single)
            {
            	animationStack.push(rack[fromRackIndex][movingRackIndex]);
            	animationStack.push(rack[toRackIndex][movingRackIndex]);
            }
        }
    }

    // move a ball from a rack to the board
    public void MoveRtoB(movespec m,replayMode replay)
    {  int movingRack =  m.from_rack;
       int movingRackIndex = m.color;
       char movingBoardCol = m.to_col;
       int movingBoardRow = m.to_row;
    	ZertzState nextstate = board_state;
        int oldcount = balls[movingRack][movingRackIndex];
        zCell c = getCell(movingBoardCol, movingBoardRow);
        if(replay==replayMode.Single)
        {
        	animationStack.push(rack[movingRack][movingRackIndex]);
        	animationStack.push(c);
        }
        char oldboard = c.contents;
        m.movedAndCaptured = zChip.BallColorIndex(oldboard);
        G.Assert(oldboard == Empty, "Destination cell not empty!");
        G.Assert(oldcount > 0, "Source bin is empty");

        switch (board_state)
        {
        case BALL_STATE:
            nextstate = ZertzState.DONE_STATE;
			//$FALL-THROUGH$
		case PUZZLE_STATE:
            balls[movingRack][movingRackIndex]--;
            SetBoard(c, BallChars[movingRackIndex]);
            c.lastPlaced = placementIndex;
            placementIndex++;

            break;
        case MOVE_OR_SWAP_STATE:
        case MOVE_STATE:
            balls[movingRack][movingRackIndex]--;
            SetBoard(c,BallChars[movingRackIndex]);
            nextstate = (AnyRingCanChange() ? ZertzState.RING_STATE : ZertzState.DONE_STATE);

            break;

        default:
        	throw G.Error("Can't place new balls now, state=%s", board_state);
        }

        // change the board state after placing the new ball, watch out
        // for the condidition that no rings can be moved after the new
        // ball is placed.
        ballPlacedFromRack = movingRack;
        ballPlaced=c;

        c.lastPlaced = placementIndex;
        placementIndex++;

        setState(nextstate);
    }

 
    public void togglePlayer()
    {
        setWhoseTurn(nextPlayer[whoseTurn]);
    }
    public boolean winForPlayerNow(int pl) { return(win[pl]);}

    private void doEndGame()
    {	boolean win1 = WinForPlayerNow(whoseTurn);
    	if(win1) 
    		{ win[whoseTurn] = true; 
    		  setState(ZertzState.GAMEOVER_STATE);
    		}
    	else
    		{ setState(Capture_Is_Possible() ? ZertzState.CAPTURE_STATE : ZertzState.MOVE_STATE);

    		}
    }

    public void SetNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        case SETRING_STATE:
        	setState(ZertzState.MOVE_OR_SWAP_STATE);
        	setWhoseTurn(nextPlayer[whoseTurn]);
        	rings_removed=0;
        	balls_in_play = 0;
        	break;
        case MOVE_OR_SWAP_STATE:	// bot does this
        case SWAP_CONFIRM_STATE:
        	setState(ZertzState.MOVE_STATE);
        	setWhoseTurn(nextPlayer[whoseTurn]);
        	break;
        case PUZZLE_STATE:
            break;
        case DRAW_STATE:
        case DONE_CAPTURE_STATE:
        case DONE_STATE:
        case RESIGN_STATE:
            //System.out.println("up2 "+moveNumber);
            RemoveCapturedBalls(replay);
            moveNumber++;
            // fall through
			//$FALL-THROUGH$
		case START_STATE:		// don't capture as part of start, so toggling between start/edit will not
        						// cause balls to disappear.
            doEndGame();
            setWhoseTurn(nextPlayer[whoseTurn]);
            break;
        default:
        	throw G.Error("Move not complete, state: %s", board_state);
        }
    }

    public boolean DoneState()
    {	
        switch (board_state)
        {
        case RESIGN_STATE:
        case SWAP_CONFIRM_STATE:
        case DRAW_STATE:
        case DONE_CAPTURE_STATE:
        case DONE_STATE:
        case SETRING_STATE:
            return (true);

        default:
            return (false);
        }
    }
    // states which should be digested.  This doesn't include the "rearrange" states.
    public boolean DigestState()
    {  
       switch(board_state)
 	   {
 	   	case SWAP_CONFIRM_STATE:
 		case SETRING_STATE:
     	   	return(false);
	default:
		break;
 	   }
    		return(DoneState());
    }
    // any capture is possible starting at col,row (ie, this ball can capture)
    public boolean Capure_Is_Possible_From(zCell c)
    {
        return (Capture_Is_Possible_From_Cell(c, BallChars));
    }

    // any capture of C is possible (ie, this ball can be captured)
    public boolean Capure_Is_Possible_To(zCell c)
    {
        return (Capture_Is_Possible_Of_Cell(c));
    }

    // return true if any capture is possible (and therefore mandatory)
    public boolean Capture_Is_Possible()
    {
        for (int i = 0; i < balls_on_board; i++)
        {
            if (Capture_Is_Possible_From_Cell(ball_location[i], BallChars))
            {
                return (true);
            }
        }
        return (false);
    }



    /* return true if the player who owns the balls has won */
    public boolean WinForPlayer(int[] ball)
    {
        return ((ball[zChip.WHITE_INDEX] > winning_total) ||
        (ball[zChip.GREY_INDEX] > (winning_total + 1)) ||
        (ball[zChip.BLACK_INDEX] > (winning_total + 2)) ||
        ((ball[zChip.WHITE_INDEX] >= winning_total) &&
        (ball[zChip.GREY_INDEX] >= winning_total) &&
        (ball[zChip.BLACK_INDEX] >= winning_total)));
    }
    public double balls_to_win(int index)
    {
        int[] ball = balls[index];

        if (ball[zChip.UNDECIDED_INDEX] > 0)
        {
        	throw G.Error(ball[zChip.UNDECIDED_INDEX] + " Undecided balls remain");
        }

        double allballs = (white_value * ball[zChip.WHITE_INDEX]) +
            (black_value * ball[zChip.BLACK_INDEX]) +
            (grey_value * ball[zChip.GREY_INDEX]);
        int white_needed = winning_total - ball[zChip.WHITE_INDEX];

        //
        // note; evaluation of these winning conditions changed from 0 to -100
        // so wins will look much better than any non win.  The problem was that
        // an even trade (say, white for white) which resulted in a win didn't look
        // like an improvement(!)  Manifest in a missed win in 
        // Z-sebastian-Dumbot-2004-12-16-2225 at move 19
        //
        if (white_needed < 0)
        {
            return (-100.0);
        } // win with whites

        int grey_needed = winning_total - ball[zChip.GREY_INDEX];

        if (grey_needed < -1)
        {
            return (-100.0);
        } //win with greys

        int black_needed = winning_total - ball[zChip.BLACK_INDEX];

        if (black_needed < -2)
        {
            return (-100.0);
        } // win with blacks

        if ((black_needed <= 0) && (white_needed <= 0) && (grey_needed <= 0))
        {
            return (-100.0);
        } // tricolor win

        int tricolor_needed = white_needed +
            ((grey_needed < 0) ? 0 : grey_needed) +
            ((black_needed < 0) ? 0 : black_needed);
        white_needed += 1; //4 whites
        grey_needed += 2; //5 greys
        black_needed += 3; //6 blacks

        int mingb = (grey_needed < black_needed) ? grey_needed : black_needed;
        int minwt = (tricolor_needed < white_needed) ? tricolor_needed
                                                     : white_needed;
        int min_needed = (mingb < minwt) ? mingb : minwt;

        // whole number is the number of balls needed.  Fraction is larger the more balls
        // are needed in combinations, so positions with more "win by n" possibilities are 
        // scored lower.
        double mainfactor = min_needed + 1.0;
        double smallfactors = (minweight[min_needed] +
            need_weight[black_needed] + need_weight[grey_needed] +
            need_weight[white_needed] + need_weight[tricolor_needed] +
            (allballs / 1000.0));

        return (mainfactor - smallfactors); //small factor just for the wood
    }

    public boolean WinForPlayerNow(int ind)
    {
        return (win[ind] || WinForPlayer(balls[ind]));
    }

    /* return the index into the ball array if this is a captured ball color */
    public int CapturedColorIndex(char piece)
    {
        for (int c = 0, len = CapturedBallChars.length; c < len; c++)
        {
            if (piece == CapturedBallChars[c])
            {
                return (c);
            }
        }

        return (-1);
    }

    /* return the index into the ball array if this is a captured ball color */
    public int UncapturedColorIndex(char piece)
    {
        for (int c = 0, len = BallChars.length; c < len; c++)
        {
            if (piece == BallChars[c])
            {
                return (c);
            }
        }

        return (-1);
    }


    /* return true if the reserve rack is empty, which makes it legal
       to remove balls from your own rack to place on the board */
    public boolean ReserveIsEmpty()
    {
        int[] ball = balls[RESERVE_INDEX];

        for (int i = 0; i < NCOLORS; i++)
        {
            if (ball[i] > 0)
            {
                return (false);
            }
        }

        return (true);
    }

    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public boolean AllowSelectRack(int rackindex, int ballindex,
        int movingBallColor)
    {
        boolean allowSelect = false;

        switch (board_state)
        {
        case PUZZLE_STATE:
            allowSelect = (movingBallColor == ballindex) 
            					? true
                                : ((movingBallColor<0) && (balls[rackindex][ballindex] > 0));
             break;
        case MOVE_OR_SWAP_STATE:
        case MOVE_STATE:
        case BALL_STATE:

            // need to move a ball from the rack  
            allowSelect = (((movingBallColor >= 0)
                ? (movingBallColor == ballindex) : (balls[rackindex][ballindex] > 0)) &&
                ((rackindex == RESERVE_INDEX) &&
                (balls[rackindex][ballindex] > 0))) ||
                (ReserveIsEmpty() && (rackindex == whoseTurn));

            break;

        case RING_STATE:
        case DONE_STATE:

            // ball moved from the rack, maybe put it back
            allowSelect = (rackindex == ballPlacedFromRack) &&
                (movingBallColor == ballindex);

            break;

        default:
         }

        return (allowSelect);
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	movespec m = (movespec)mm;
        
        //G.print("E "+m+" for "+whoseTurn);
    	if(board_state==ZertzState.RESIGN_STATE)
    	{
    		// this is a bit of a crock that only the older move generation strategy
    		// in zertz needs.
    	switch(m.op)
    	{
    	case MOVE_RESIGN:
    	case MOVE_DONE: break;
    	default: setState(unresign);
    	}
    	}
        switch (m.op)
        {
        case MOVE_SETBOARD:
        	setBoardType(Zvariation.values()[m.to_row]);
        	break;
        case MOVE_BtoB:
            lastMove = new movespec(m.player, MOVE_BtoB, m.from_col,
                    m.from_row, m.to_col, m.to_row);
            MoveBtoB(m,replay);

            break;

        case MOVE_BtoR:
            lastMove = null;
            MoveBtoR(m,replay);

            break;

        case MOVE_RtoB:
            if(replay!=replayMode.Live)
            {	// repair some old, damaged games.  Note this gives the mover
            	// and extra ball!
            	if(board_state==ZertzState.DONE_STATE) { setState(ZertzState.BALL_STATE); }
            	else if(board_state==ZertzState.RING_STATE) { setState(ZertzState.RING_STATE); }
            }
            lastMove = new movespec(m.player, MOVE_RtoB,
            		m.from_rack,
                    m.color, m.to_col, m.to_row);
           MoveRtoB(m,replay);

            break;

        case MOVE_RtoR:
            lastMove = null;
            MoveRtoR(m,replay);

            break;

        case MOVE_R_PLUS:
            lastMove = null;
            AddRing(m.to_col, m.to_row);

            break;

        case MOVE_R_MINUS:
        	if(replay!=replayMode.Live)
        	{	// repair some old damaged games.  Note this lets the mover remove
        		// an extra ring!
        		if(board_state==ZertzState.DONE_STATE) { setState(ZertzState.RING_STATE); }
        		else if(board_state==ZertzState.BALL_STATE) { setState(ZertzState.MOVE_STATE); }
        	}
            lastMove = new movespec(m.player, MOVE_R_MINUS, m.from_col,
                    m.from_row);
            RemoveRing(m.from_col, m.from_row);

            break;

        case MOVE_DONE:
            // dont change lastmove
        	ringRemoved=null;
        	if(board_state==ZertzState.DRAW_STATE)
        	{ setState(ZertzState.GAMEOVER_STATE);
        	}
        	else if (board_state==ZertzState.RESIGN_STATE)
            {
                win[nextPlayer[whoseTurn]] = true;
                setState(ZertzState.GAMEOVER_STATE);
            }
            else
            {	SetNextPlayer(replay);
            }

            break;

        case MOVE_START:
            lastMove = null;
            needStart = false;
            setState(ZertzState.START_STATE);
            whoseTurn = nextPlayer[m.player];
            SetNextPlayer(replay);
            if(boardSetup==Zvariation.Zertz_xx) { setState(ZertzState.SETRING_STATE); }
            break;
        case MOVE_SWAP:
        	swapped_state = !swapped_state;
        	setState((board_state==ZertzState.SWAP_CONFIRM_STATE)
        			?ZertzState.MOVE_OR_SWAP_STATE
        			:ZertzState.SWAP_CONFIRM_STATE);
        	break;
        case MOVE_EDIT:
            lastMove = null;
            needStart = false;
            setState(ZertzState.PUZZLE_STATE);

            break;

        case MOVE_RESIGN:
            setState(unresign==null?ZertzState.RESIGN_STATE:unresign);
            break;

		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(ZertzState.GAMEOVER_STATE);
			break;
         
        default:
            cantExecute(m);
        }


        return (true);
    }

    /* Support for the robot.  Less general because we don't need the do/undo shuffle
       that indecisive human players need, but more restrictive because we need a speedy undo
    */
    /* support for the robot, remove captured balls only adjacent to x,y,
       which is the location of the removed ring.  Place captured balls
       on the capture stack.  Return the number of balls captured
     */
    private int removeCapturedAdjacent(zCell c)
    {
        int caps = 0;
        for (int dir = 0; dir < 6; dir++)
        {	zCell nc = c.exitTo(dir);
            char ch = (nc==null) ? NoSpace : nc.contents;
            int col = zChip.BallColorIndex(ch);

            if ((col >= 0) && !IsViable(nc))
            {	caps += removeCapturedAndAdjacent(nc);
            }
        }

        return (caps);
    }
    private int removeCapturedAndAdjacent(zCell nc)
    {	int caps = 0;
        char ch = (nc==null) ? NoSpace : nc.contents;
        int col = zChip.BallColorIndex(ch);
                incBalls(whoseTurn,col); // move to player's rack
                //System.out.println("+r "+col+"->"+balls[whoseTurn][col]);
                SetBoard(nc,NoSpace);
                CaptureColor[Capture_Index]=col;
                CaptureState[Capture_Index]=board_state;
                Captures[Capture_Index++] = nc; // save what was captured
                caps++;
        caps += removeCapturedAdjacent(nc); // continue eating the island
        return (caps);
    }

    // removing a ring, remove captured balls.
    public int ExpressRemoveRing(char col, int row)
    {	zCell c = getCell(col,row);
        char oldc = c.contents;
        G.Assert(oldc == Empty, "Ring was not present");

        switch (board_state)
        {
        case SETRING_STATE:
        	SetBoard(c, NoSpace);
        	return(0);
        case RING_STATE:
            SetBoard(c, NoSpace);
            int captured = removeCapturedAdjacent(c);
            setState(ZertzState.DONE_STATE);

            return (captured);

        default:
        	throw G.Error("Bad state for ExpressRemoveRing ");
        }

    }

    // move a ball from a rack to the board
    public int ExpressMoveRtoB(int movingRack, int movingRackIndex,
        char movingBoardCol, int movingBoardRow)
    {	int caps = 0;
        int oldcount = balls[movingRack][movingRackIndex];
        zCell c = getCell(movingBoardCol, movingBoardRow);
        char oldboard = c.contents;
        G.Assert(oldboard == Empty, "Destination cell not empty!");
        G.Assert(!((oldcount <= 0) && (movingRackIndex < NCOLORS)),
            "Source bin is empty");

        switch (board_state)
        {
        case MOVE_OR_SWAP_STATE:
        case MOVE_STATE:
            balls[movingRack][movingRackIndex]--; //remove from rack
            SetBoard(c,BallChars[movingRackIndex]);
            setState(ZertzState.RING_STATE);

            if (!IsViable(c))
            {	caps = removeCapturedAndAdjacent(c);
            }
  
            break;

        default:
        	throw G.Error("Illegal state for ExpressExecuteRtoB");
        }
        return(caps);
    }

    public String ballSummary(int pl)
    {
        int[] ball = balls[pl];

        return (ball[zChip.WHITE_INDEX] + "+" + ball[zChip.GREY_INDEX] + "+" +
        ball[zChip.BLACK_INDEX]);
    }

    // move a ball from one board position to another.  If a temporary
    // ball was placed, only that ball can be moved.
    public void ExpressMoveBtoB(char movingBoardCol, int movingBoardRow,
        char destBoardCol, int destBoardRow)
    {	zCell from = getCell(movingBoardCol, movingBoardRow);
    	zCell to = getCell(destBoardCol, destBoardRow);
        char oldboard = from.contents;
        int oldcolor = zChip.BallColorIndex(oldboard);
        char newdest = to.contents;
        G.Assert(newdest == Empty, "Destination is not empty");
        G.Assert(oldcolor >= 0, "Board has no ball");

        //old position contains a ball, new position is empty.
        //if doing captures, we must also be capturing a ball
        switch (board_state)
        {
        case CAPTURE_STATE:
        case CONTINUE_STATE:
        {	zCell mid = MidBetween(from,to);
            char replacechar = CaptureBetween(mid);

            if (replacechar != NoSpace)
            { // the arithmetic works exactly


                if (mid.contents == Empty)
                {
                	throw G.Error("Inconsistant midpoint capture");
                }

                int capcolor = CapturedColorIndex(replacechar);
                CaptureColor[Capture_Index] = capcolor;
                CaptureState[Capture_Index] = board_state;
                Captures[Capture_Index++] = mid;
                incBalls(whoseTurn,capcolor);

                //System.out.println("+ "+capcolor+"->"+balls[whoseTurn][capcolor]);
                SetBoard(mid, Empty);
                SetBoard(from, Empty);
                SetBoard(to, oldboard);
                ballPlaced = to;
                setState(Capture_Is_Possible_From_Cell(to, BallChars)
                    ? ZertzState.CONTINUE_STATE : ZertzState.DONE_CAPTURE_STATE);

               // System.out.println("cap "+Capture_Index);

            }
            else
            {
            	throw G.Error("No Ball to capture");
            }
        }

        break;

        case RING_STATE:
        case DONE_STATE: //moving a temporary ball
        case PUZZLE_STATE:
        case DONE_CAPTURE_STATE:default:
        	throw G.Error("Illegal state for ExpressMoveBtoB");
        }
    }

    StateStack undoState = new StateStack();
    IStack turnStack = new IStack();
    // ExpressExecute only executes steps for the robot evaluator
    // B-B moves are all captures
    // R-B moves are all legal, etc.
    //
    public void ExpressExecute(movespec m)
    { 	//System.out.println("Ex "+m);
    	undoState.push(board_state);
    	int who = whoseTurn;
        switch (m.op)
        {
        case MOVE_SWAP:
        	SetNextPlayer(replayMode.Replay);
        	swapped_state = !swapped_state;
        	setState(ZertzState.MOVE_STATE);
        	break;
        case MOVE_BtoB:
            ExpressMoveBtoB(m.from_col, m.from_row, m.to_col, m.to_row);

            break;

        case MOVE_RtoB:
        	{
            int caps = ExpressMoveRtoB(m.from_rack, m.color, m.to_col, m.to_row);
            turnStack.push(caps);
        	}
            break;

        case MOVE_R_MINUS:

            // must follow RtoB, because we check for isolation captures here
        	{
            int caps = ExpressRemoveRing(m.from_col, m.from_row);
            turnStack.push(caps);
        	}
            break;

        case MOVE_DONE:

            if ((board_state == ZertzState.RING_STATE)||(board_state==ZertzState.SETRING_STATE))
            { // in rare circumstances, we can't remove a ring.  We trust
              // the program to know when and to go directly to "done"
              // the other half (in AutoPlay) will toggle the player and advance the move.
                board_state = ZertzState.DONE_STATE;
            }

            //setWhoseTurn(nextPlayerIndex[whoseTurn]);
            //moveNumber++;
//System.out.println("up " +moveNumber);
            break;

        case MOVE_BtoR:
        case MOVE_RtoR:
        case MOVE_R_PLUS:
        case MOVE_START:
        case MOVE_EDIT:default:
        	throw G.Error("Illegal move for ExpressExecute: " + m);
        }
    	turnStack.push(who);

    }

    // replace a ring, put back captured balls.
    public void ExpressUnRemoveRing(char col, int row)
    {	
        switch (board_state)
        {
        default:
        	setState(ZertzState.RING_STATE);
			//$FALL-THROUGH$
		case SETRING_STATE:
        	zCell c = getCell(col,row);
            SetBoard(c, Empty);
            break;
        }
    }
    private void restoreCaptures(int captures)
    {
          while (captures > 0)
            {	int color = CaptureColor[--Capture_Index];
            	//ZertzState state = CaptureState[Capture_Index];
                zCell mid = Captures[Capture_Index];
                balls[whoseTurn][color]--; //remove the ball

                //System.out.println("-r "+color+"->"+balls[whoseTurn][color]);
                G.Assert(balls[whoseTurn][color] >= 0, "Captured too many");
                SetBoard(mid, BallChars[color]);
                captures--;
            }
    }
    // unmove a ball from a rack to the board
    public void ExpressUnMoveRtoB(int movingRack, int movingRackIndex, char movingBoardCol, int movingBoardRow)
    {
    	// not usually, but if no rings were removable
        incBalls(movingRack,movingRackIndex);
        SetBoardPos(movingBoardCol, movingBoardRow, Empty);
        setState(ZertzState.MOVE_STATE);
    }

    // move a ball from one board position to another, which must uncapture something
    public void ExpressUnMoveBtoB(char movingBoardCol, int movingBoardRow,
        char destBoardCol, int destBoardRow)
    {
       	zCell from = getCell(movingBoardCol, movingBoardRow);
       	zCell to = getCell(destBoardCol, destBoardRow);
        int code = CaptureColor[--Capture_Index];
        ZertzState newstate = CaptureState[Capture_Index];
        zCell mid = Captures[Capture_Index];
        int color = code % 10;
        SetBoard(from, to.contents);
        SetBoard(to, Empty);
        SetBoard(mid, BallChars[color]);
        ballPlaced = from; 	//save these for the move generator
        balls[whoseTurn][color]--;
        //System.out.println("un cap "+Capture_Index);
        //System.out.println("- "+color+"->"+balls[whoseTurn][color]);
        G.Assert(balls[whoseTurn][color] >= 0, "Captured too many");
        setState(newstate);

    }

    public void ExpressUnExecute(movespec m)
    {	
    	int undoturn = turnStack.pop();
    	if(whoseTurn!=undoturn) 
       		{ moveNumber--; 
       		 // System.out.println("Down "+moveNumber);
       		}
    
    	whoseTurn = undoturn;
        
    	switch (m.op)
        {
        case MOVE_SWAP:
        	swapped_state = !swapped_state;
        	break;
        case MOVE_BtoB:
            ExpressUnMoveBtoB(m.from_col, m.from_row, m.to_col, m.to_row);

            break;

        case MOVE_RtoB:
        	{
        	int caps = turnStack.pop();
        	restoreCaptures(caps);
            ExpressUnMoveRtoB(m.from_rack, m.color, m.to_col, m.to_row);
        	}
            break;

        case MOVE_R_MINUS:

            // must follow RtoB, because we check for isolation captures here
        	{
        	int caps =  turnStack.pop();
           	restoreCaptures(caps);
           	ExpressUnRemoveRing(m.from_col, m.from_row);
        	}

            break;

        case MOVE_DONE:

        	break;

        case MOVE_BtoR:
        case MOVE_RtoR:
        case MOVE_R_PLUS:
        case MOVE_START:
        case MOVE_EDIT:
        	throw G.Error("Illegal move for ExpressUnExecute: " + m);
		default:
			break;
        }

        whoseTurn = undoturn;
  
        board_state = undoState.pop();
    }

    public void test_eval()
    {
        int[] rack = balls[RESERVE_INDEX];

        for (int wi = 0; wi < rack[zChip.WHITE_INDEX]; wi++)
        {
            for (int gi = 0; gi < rack[zChip.GREY_INDEX]; gi++)
            {
                for (int bi = 0; bi < rack[zChip.BLACK_INDEX]; bi++)
                {
                    balls[SECOND_PLAYER_INDEX][zChip.WHITE_INDEX] = wi;
                    balls[SECOND_PLAYER_INDEX][zChip.GREY_INDEX] = gi;
                    balls[SECOND_PLAYER_INDEX][zChip.BLACK_INDEX] = bi;

                    if (!WinForPlayerNow(SECOND_PLAYER_INDEX))
                    {
                        int w = (int) ((balls_to_win(SECOND_PLAYER_INDEX) * 1000000) +
                            0.5);

                        // print these, copy and paste to emacs, sort
                        System.out.println(w + "\t" + wi + "\t" + gi + "\t" +
                            bi);
                    }
                }
            }
        }
    }


public void addCaptureMoves(CommonMoveStack  result)
{
    for(int i=0;i<balls_on_board;i++)
    {	zCell c = ball_location[i];
    	if(BallCanMove(c))
    	{
            for (int dir = 0; dir < 6; dir++)
            {	zCell nc1 = c.exitTo(dir);
            	zCell nc2 = (nc1==null) ? null : nc1.exitTo(dir);
            	if(CaptureOccurs(c,nc1,nc2,false))
            			{result.addElement(new movespec(whoseTurn, MOVE_BtoB,
                                c.col, c.row, nc2.col,nc2.row));
            			}
            }
    	}
    }
}
public void addContinueCaptures(CommonMoveStack  result)
// continue a capture in progress
{
   zCell placed = ballPlaced;
   for (int dir = 0; dir < 6; dir++)
   {
	zCell nx1 = placed.exitTo(dir);
   	zCell nx2 = nx1==null ? null : nx1.exitTo(dir);
       if (CaptureOccurs(placed,nx1,nx2, false))
       {
           result.addElement(new movespec(whoseTurn, MOVE_BtoB, 
           			placed.col,placed.row,nx2.col,nx2.row));
       }
   }
}

public void addRingMoves(CommonMoveStack  result, boolean limitrings)
{	CommonMoveStack  tresult = limitrings ? new CommonMoveStack() : null;
    for (zCell c = allCells; c!=null; c=c.next)
    {
     if (RingCanChange(c) 
    		 && ((boardSetup==Zvariation.Zertz_xx) 
    				 ? ((rings_removed<2) 
    						 ? (c.contents==Empty) 
    						 : ((c.contents==Empty) && twoSides(c)))
    				 : true))
            {
                movespec mv = new movespec(whoseTurn, MOVE_R_MINUS, c.col,c.row);

                if (limitrings && !RingNextToBall(c))
                { //note that since we're not looking deep, and since board awareness is nil,
                  //we don't need to consider all the ring possibilities
                    tresult.addElement(mv);
                }
                else
                {
                    result.addElement(mv);
                }
            }
        }

    if (limitrings)
    {   int sz = tresult.size();
    	Random r = new Random();
        if (sz > 0)
        {
            result.addElement(tresult.elementAt(
                    (int) (r.nextDouble() * sz)));
        }
    }
    if (result.size() == 0)
    { // rare case where no ring can be removed
        result.addElement(new movespec(whoseTurn, MOVE_DONE));
    }
	
}

}

/*

--
  white  grey  black
11259  2  4  5    // any ball wins
11311  3  2  5    // any ball wins
11335  3  4  2    // any ball wins
11366  2  4  4    // 1 w 1 g or 2b wins
11367  2  3  5    // 1 w 2 g or 1b wins
11410  1  4  5    // 2 w 1 g or 1b wins
11418  2  4  3    // 1w 1g or 3b wins
11418  3  2  4    // 1w 1g or 2b wins  << wrong
11435  3  1  5    // 1w 2g or 1b wins
11443  3  3  2    // 1w 2g or 4b wins
11452  3  4  1    // 1w 1g or 2b wins
11470  3  2  3    // 1w 1g or 3b wins

11474  2  3  4    // 1w 2g or 2b wins
11480  0  4  5    // 3w 1g or 1b wins
11498  3  0  5    // 1w 3g or 1b wins
11510  3  4  0
11517  1  4  4
11517  2  2  5
11518  1  3  5
11526  2  3  3
11541  2  4  2
11542  3  1  4
11560  3  3  1
11569  1  4  3
11586  2  1  5
11587  0  4  4
11588  0  3  5
11593  3  2  2
11594  3  1  3
11603  2  4  1
11605  3  0  4
11613  1  2  5
11618  3  3  0
11623  2  0  5
11635  2  4  0
11637  1  4  2
11639  0  4  3
11655  3  2  1
11656  1  1  5
11657  0  2  5
11657  3  0  3
11662  3  1  2
11673  1  4  1
11681  0  4  2
11687  1  0  5
11687  3  2  0
11694  0  1  5
11698  3  1  1
11699  1  4  0
11699  3  0  2
11711  0  4  1
11721  0  0  5
11724  3  1  0
11729  3  0  1
11733  0  4  0
11751  3  0  0
25624  2  2  4
25625  1  3  4
25649  2  3  2
25676  2  2  3
25677  1  3  3
25693  2  1  4
25695  0  3  4
25711  2  3  1
25720  1  2  4
25730  2  0  4
25743  2  3  0
25744  2  2  2
25745  1  3  2
25745  2  1  3
25747  0  3  3
25763  1  1  4
25764  0  2  4
25780  2  2  1
25781  1  3  1
25782  2  0  3
25787  2  1  2
25789  0  3  2
25794  1  0  4
25801  0  1  4
25806  2  2  0
25807  1  3  0
25817  2  1  1
25818  2  0  2
25819  0  3  1
25828  0  0  4
25839  2  1  0
25841  0  3  0
25844  2  0  1
25864  2  0  0
37772  1  2  3
37814  1  2  2
37815  1  1  3
37816  0  2  3
37844  1  2  1
37846  1  0  3
37851  1  1  2
37852  0  2  2
37853  0  1  3
37866  1  2  0
37877  1  1  1
37878  0  2  1
37878  1  0  2
37880  0  0  3
37897  1  1  0
37898  0  2  0
37902  1  0  1
37920  1  0  0
48885  0  1  2
48909  0  1  1
48910  0  0  2
48927  0  1  0
48932  0  0  1


*/
