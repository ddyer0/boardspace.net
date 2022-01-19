package gipf;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

public class GipfBoard extends hexBoard<GipfCell> implements BoardProtocol,GipfConstants
{   //dimensions for the viewer to use
	
	// revision 100 applies to some very old games.
	// revision 101 reduces the count of chips by 1
	public static final int REVISION = 101;
	public int getMaxRevisionLevel() { return(REVISION); }
	
	
	static final int N_STANDARD_CHIPS = 18;
	static final int N_BASIC_CHIPS = 15;
	static final int MAXDESTS = 30;
	static final String GIPF_GRID_STYLE[] = { "1", null, "A" };
	static int[] ZfirstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 }; // these are indexes into the first ball in a column, ie B1 has index 2
	static int[] ZnInCol = { 5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.


	public boolean hasExplicitRevision = false;
	private GipfState board_state = GipfState.PUZZLE_STATE;
	private GipfState unresign = null;
	public GipfState getState() { return(board_state); }
	public CellStack animationStack = new CellStack();
	public GipfChip playerChip[] = { GipfChip.getChip(0),GipfChip.getChip(1)};
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(GipfState st) 
	{ 	unresign = (st==GipfState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public static int PRESSTACK = 100000;
	public static int REMOVESTACK = 100;
    public int firstPlayer = -1; // changed as the game starts
    public boolean standard_setup = false;
    public boolean tournament_setup = false;
    public boolean tournament_setup_done = false;
    public GipfCell reserve[]=new GipfCell[2];
    public GipfCell captures[]=new GipfCell[2];
    public int initial_height = 0;
    public int gipfCount[] = new int[2];				// count of gipfs ever comitted
    public int currentGipfCount[] = new int[2];			// count of gipfs currently on the board
    public int nongipfCount[] = new int[2];				// count of nongipfs ever committed
    private int sweep_counter = 0;
    private GipfCell pickedSource[] = new GipfCell[10];
    private GipfCell droppedDest[] = new GipfCell[10];
    private IStack robotPreservationStack = new IStack(); 
    private IStack preservationStack = new IStack();
    private int dropUndo[] = new int[10];
    private int stackIndex = 0;
    public GipfChip pickedObject = null;
    public int pickedHeight = 0;
    public int movingObjectIndex() 
    	{ GipfChip ch = pickedObject;
    	  return((ch==null)?NothingMoving:ch.pieceNumber()); 
    	}
  

    // not a draw state for us, but an end of game state
    public void SetDrawState()
    	{ setState(GipfState.DRAW_STATE); }
    
    public GipfCell newcell(char r,int c) 
    {	return(new GipfCell(r,c,cell.Geometry.Hex));
    }

    private GipfCell removalStack=null;
    private CellStack removalCells=new CellStack();
    
 
    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public Gipfmovespec lastMove = null;

    public GipfBoard(String init,int map[],int rev) // default constructor
    {
        drawing_style=DrawingStyle.STYLE_NOTHING; //STYLE_NO_EDGE_LINES; // drawing style for gipf
        //Grid_Style = GIPF_GRID_STYLE;
        Random r = new Random(63472);
        removalStack = new GipfCell(r,GipfId.NoHit,-11);
        for(int i=0;i<reserve.length;i++)
        {	reserve[i]=new GipfCell(r,PlayerReserve[i],i);
        	captures[i]=new GipfCell(r,PlayerCaptures[i],nextPlayer[i]);
        	gipfCount[i]=0;
        	nongipfCount[i]=0;
        	currentGipfCount[i]=0;
        }
        setColorMap(map);
        doInit(init,randomKey,rev); // do the initialization 
        allCells.setDigestChain(r);
    }

    public GipfBoard cloneBoard() 
	{ GipfBoard dup = new GipfBoard(gametype,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	} 
    public void copyFrom(BoardProtocol b) { copyFrom((GipfBoard)b); }

    /*

      private interfaces which use X,Y (ie; real board coordinates)

    */
    static private int[][] startPos = 
    	{{'B',2,1},{'B',5,0},
    	{'E',2,0},{'E',8,1},
    	{'H',2,1},{'H',5,0} };
    private int playerIndex(GipfChip ch) { return(getColorMap()[ch.colorIndex]); }
    private void check_piece_count()
    {	int height=0;
    	int cg0 = 0;
    	int cg1 = 0;
    	for(GipfCell c=allCells; c!=null; c=c.next) 
    		{ int hh = c.height();
    		  height+= hh; 
    		  if(hh==2) 
    		  	{ GipfCell cc = c;
    		  	  if(playerIndex(cc.topChip())==0) { cg0++; } else { cg1++; }} 
    		}
    	int rh0 = reserve[0].height();
    	int rh1 = reserve[1].height();
    	int ch0 = captures[0].height();
    	int ch1 = captures[1].height();
    	height += rh0+rh1+ch0+ch1;
     	height += pickedHeight;
    	G.Assert(initial_height==height,"piece counts match");
    	G.Assert(cg0==currentGipfCount[0],"gipf 0 matches");
    	G.Assert(cg1==currentGipfCount[1],"gipf 1 matches"); 
    }
    private void InitBallsAndBoard()
    {	int map[] = getColorMap();
        setState(GipfState.PUZZLE_STATE);
        initial_height=0;
        reInit(reserve);
        reInit(captures);
        AR.setValue(gipfCount,0);
        AR.setValue(currentGipfCount,0);
        AR.setValue(nongipfCount,0);
        for(int i=0;i<reserve.length;i++) 
        	{ int lim = (standard_setup|tournament_setup)
        					?N_STANDARD_CHIPS-(revision<101 ? 0 : 1)
        					:N_BASIC_CHIPS-(revision<101 ? 0 : 1);
            switch(effective_date_algorithm)
            {
            default: throw G.Error("Not expected");
            case 0: break;
            case 1: lim = N_BASIC_CHIPS;
            	break;
            case 2: lim = standard_setup?N_STANDARD_CHIPS-1:N_BASIC_CHIPS-1;
        	break;
            case 5: lim = standard_setup?N_STANDARD_CHIPS-1:N_BASIC_CHIPS-2;
        	break;
            case 3: lim = standard_setup?N_STANDARD_CHIPS-1:N_BASIC_CHIPS-1;
            		if(tournament_setup) { lim-=3; }
            	break;
            case 4: lim = standard_setup?N_STANDARD_CHIPS-1:N_BASIC_CHIPS-1;
    			if(tournament_setup) { lim-=1; }
    			break;
           // case 3: 
            	// lim = standard_setup?N_STANDARD_CHIPS-1:N_BASIC_CHIPS-1;
            	// if(tournament_setup) { lim += 0; }
            	// else if(!standard_setup) { lim--; }
            //	break;
            }
            //G.print("Da "+effective_date_algorithm+" = "+lim);
        	//       	  if(tournament_setup && tournament_error_date) 
 //       	  	{ lim=N_BASIC_CHIPS-2;
 //       	  	  if(!tournament_error_date2) { lim+=1; }
 //       	  	}
			  initial_height += lim+1;
        	  while(reserve[i].chipIndex<lim) { reserve[i].addChip(GipfChip.getChip(map[i])); }
       	}
        playerChip[map[0]] = GipfChip.getChip(0);
        playerChip[map[1]] = GipfChip.getChip(1);
        removalStack.reInit();
        removalCells.clear();
        whoseTurn = firstPlayer;
       if(!tournament_setup)
        {
        for(int i=0;i<startPos.length;i++)
        {	int []row=startPos[i];
        	char col = (char)row[0];
        	int ro = row[1];
        	int pl = row[2];
        	getCell(col,ro).addChip(reserve[pl].removeTop());
        	if(standard_setup) 
        		{ 
        		  getCell(col,ro).addChip(reserve[pl].removeTop()); 
        		  gipfCount[pl]++; 
        		  currentGipfCount[pl]++; 
        		  }
        	else { nongipfCount[pl]++; }
        }}
        board_state = (firstPlayer >= 0) ? GipfState.DONE_STATE : GipfState.PUZZLE_STATE;
        pickedHeight = 0;
        pickedObject=null;
        stackIndex=0;
        check_piece_count();
    }

    private void Init_Standard(String type)
    {
        gametype = type;
        standard_setup = Gipf_Standard_Init.equalsIgnoreCase(type);
        tournament_setup = Gipf_Tournament_Init.equalsIgnoreCase(type);
        initBoard(ZfirstInCol, ZnInCol, null);
        while(stackIndex>=0) 
        	{ pickedSource[stackIndex]=droppedDest[stackIndex]=null;
        	  dropUndo[stackIndex]=0;
        	stackIndex--; 
        	}
        stackIndex=0;
        preservationStack.clear();
        // remove edge-edge links
        for(GipfCell c=allCells; c!=null; c=c.next)
        {	if(c.isEdgeCell())
        	{	
        		for(int i=0;i<c.nAdjacentCells();i++)
        		{
        			GipfCell a = c.exitTo(i);
        		if((a!=null) && a.isEdgeCell()) { c.unCrossLinkTo(a); }
        		}
        	}
        }
        InitBallsAndBoard();
    }
    public void sameboard(BoardProtocol f) { sameboard((GipfBoard)f); }

    public void sameboard(GipfBoard from_b)
    {
        super.sameboard(from_b);

       // G.Assert(GipfCell.sameCell(pickedSource,from_b.pickedSource),"source stack matches");
       // G.Assert(cell.sameCell(droppedDest,from_b.droppedDest),"dest stack matches");
      //  G.Assert(G.sameArrayContents(dropUndo,from_b.dropUndo),"undo matches");
        G.Assert(AR.sameArrayContents(gipfCount,from_b.gipfCount),"gipf count matches");
        G.Assert(AR.sameArrayContents(currentGipfCount,from_b.currentGipfCount),"gipf count matches");
        G.Assert(AR.sameArrayContents(nongipfCount,from_b.nongipfCount),"non gipf count matches");
        G.Assert(stackIndex==from_b.stackIndex, "stackIndex matches");
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
            //G.Assert(G.same
        for (int i = 0; i < 2; i++)
        {	if (!captures[i].sameCell(from_b.captures[i]))
        		{throw G.Error("Captures mismatch at %s",i);
        		}
            if (!reserve[i].sameCell(from_b.reserve[i]))
            {
            	throw G.Error("Reserve mismatch at %s", i);
            }
        }
        
        G.Assert(pickedObject==from_b.pickedObject, "pickedObject matches");
        G.Assert(Digest()==from_b.Digest(), "Digest matches");
        
    }



    public long Digest()
    {
    	long v = 0;
        Random r = new Random(61 * 11465);
        for(GipfCell c=allCells; c!=null; c=c.next)
        {	v ^= c.Digest(r);
        }
        for(int i=0;i<2;i++) { v ^= (reserve[i].Digest(r)); v^=(captures[i].Digest(r)); }
		for(int i=0;i<2;i++) { v^= r.nextLong()^currentGipfCount[i]; }
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,getSource());
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }
    // get a hash of moves destinations, used by the viewer
    // to know where to draw dots and also to determine what
    // is a legal move
    public Hashtable<GipfCell,GipfCell> getMoveDests()
    {	Hashtable<GipfCell,GipfCell> dd = new Hashtable<GipfCell,GipfCell>();
    	GipfCell src = pickedSource[stackIndex];
    	
    	if((src!=null)&&(pickedObject!=null))
    	{	
    		getDestsFrom(src,whoseTurn,dd,true);
    		
    	}
   	return(dd);
    }
    public Hashtable<GipfCell,GipfCell> getCaptures()
    {	Hashtable<GipfCell,GipfCell> dd = new Hashtable<GipfCell,GipfCell>();
    	if(markForRemoval(whoseTurn))
    	{
    	for(GipfCell c=allCells; c!=null; c=c.next)
    	{	if((c.rowcode!=0) 
    			&& !c.preserved 
    			&& ((c.topChip()!=null)||(c.isEdgeCell())))
    		{ dd.put(c,c); 
    		}
    	}}
    	return(dd);
    }
    public boolean hasGipfCaptures() 
    { 	if(markForRemoval(whoseTurn))
    	{	for(GipfCell c = allCells;
    			c!=null;
    			c=c.next)
    		{	if((c.rowcode!=0) && (c.isGipf())) { return(true); } 
    		}
    	}
    	return(false);
    }
    public void getDestsFrom(GipfCell src,int pl,Hashtable<GipfCell,GipfCell>d,boolean picked)
    {	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case DONE_STATE:
    	case RESIGN_STATE:
    	case PUZZLE_STATE: break;
    	case PLACE_STATE:
        case PRECAPTURE_OR_START_GIPF_STATE:    	
        case PRECAPTURE_OR_START_NORMAL_STATE:
    	case PLACE_GIPF_STATE:
    		for(GipfCell c=allCells; c!=null; c=c.next)
    		{	if(c.isEdgeCell()) { d.put(c, c); }
    		}
    		break;
    	case SLIDE_STATE:
    	case SLIDE_GIPF_STATE:
    		for(int dir = 0;dir<CELL_FULL_TURN; dir++)
    		{	GipfCell c = src.exitTo(dir);
    			if((c!=null)&&(c.emptyCellInDirection(dir))) { d.put(c,c); }
    		}
    		break;
    	}
    	
    }
    public boolean legalToHitBoard(GipfCell c)
    {
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case RESIGN_STATE:
    	case GAMEOVER_STATE: return(false);
    	
        case PRECAPTURE_OR_START_GIPF_STATE:
        case PRECAPTURE_OR_START_NORMAL_STATE:
        	if(c.isEdgeCell() 
        			&& ((reserve[whoseTurn].height()>0) 
        				|| (pickedObject!=null)
        				|| capturesPending()))
        		{ return(true); }
			//$FALL-THROUGH$
		case PRECAPTURE_STATE:
    	case DESIGNATE_CAPTURE_STATE:
     	case DESIGNATE_CAPTURE_OR_DONE_STATE:
    		G.Assert(markForRemoval(whoseTurn),"must be capturing");
    		switch(c.rowcode)
    		{	case 0:	
    			default: return(c.isGipf());
    			case 1:
    			case 2:
    			case 4: return(true);
    		}
    		
    	case DONE_CAPTURE_STATE:
    		if((pickedObject==null)
    			&& (c.isGipf())
    			&& (c.rowcode!=0))
    		{ return(true); 
    		}
    		//
    		// normally we would allow a second click on the same cell, but
    		// that would require unwinding a very delicate state.  In this
    		// case we just punt and depend on undo to clean up and start over.
    		//
    		return(false);
    	case DONE_STATE: 
    	case DRAW_STATE:
    		if(pickedObject==null) 
    			{ return((stackIndex>0) && (c==droppedDest[stackIndex-1])); }
    		return(false); 
    	case SLIDE_STATE:
    	case SLIDE_GIPF_STATE:
    		{	GipfCell src = (pickedObject!=null) 
    							?pickedSource[stackIndex]
    							:droppedDest[stackIndex-1];
    		    if(c==src) { return(true); }
    			if(c.isAdjacentTo(src)
    				&& c.emptyCellInDirection(findDirection(src.col,src.row,c.col,c.row))
    				) { return(true); }
    		}
			//$FALL-THROUGH$
		case PLACE_STATE:
     	case PLACE_GIPF_STATE:
    		return(c.isEdgeCell() && (c.height()==0));
    	case PUZZLE_STATE: 
    		GipfChip ch = c.topChip();
    		return((pickedObject==null)
    				?ch!=null
    				:ch!=null 
    					? (((c.height()+pickedHeight)<=2) 
    							&& (ch.colorIndex==pickedObject.colorIndex))
    					: true);
    	}
    }

    // legal to hit the chip reserve area
    public boolean legalToHitChips(GipfCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case SLIDE_STATE:
        case SLIDE_GIPF_STATE:
        	return((pickedObject!=null)
        			&& ((whoseTurn==FIRST_PLAYER_INDEX) 
        				? (c.rackLocation==GipfId.First_Player_Reserve)
        				: (c.rackLocation==GipfId.Second_Player_Reserve)));
        case PRECAPTURE_OR_START_GIPF_STATE:
        case PRECAPTURE_OR_START_NORMAL_STATE:
        case PLACE_STATE: 
        case PLACE_GIPF_STATE:
        {	GipfChip ch = c.topChip();
        	return((ch!=null) 
        			&& ((c.rackLocation==GipfId.First_Player_Reserve)
        					||(c.rackLocation==GipfId.Second_Player_Reserve))
        			&& (playerIndex(ch)==whoseTurn));
        }
        case DONE_STATE:
        case DRAW_STATE:
        case DONE_CAPTURE_STATE:
        case DESIGNATE_CAPTURE_OR_DONE_STATE:
        case PRECAPTURE_STATE:
        case DESIGNATE_CAPTURE_STATE:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
        	return(pickedObject==null?(c.chipIndex>=0):pickedObject.colorIndex==c.colorIndex);
        }
    }
    public GipfCell getCell(GipfCell c)
    {	if(c==null) { return(null); }
    	switch(c.rackLocation())
    	{	default: throw G.Error("not expecting %s",c);
    		case BoardLocation: return(getCell(c.col,c.row));
    		case First_Player_Reserve: return(reserve[0]);
    		case Second_Player_Reserve: return(reserve[1]);
    		case First_Player_Captures: return(captures[0]);
    		case Second_Player_Captures: return(captures[1]);
    	}
    }
    /* make a copy of a board */
    public void copyFrom(GipfBoard from_b)
    {	super.copyFrom(from_b);
        firstPlayer = from_b.firstPlayer;
        pickedHeight = from_b.pickedHeight;
        pickedObject = from_b.pickedObject;
        board_state = from_b.board_state;
        tournament_setup_done = from_b.tournament_setup_done;
        unresign = from_b.unresign;
        AR.copy(dropUndo,from_b.dropUndo);
        AR.copy(gipfCount,from_b.gipfCount);
        AR.copy(currentGipfCount,from_b.currentGipfCount);
        AR.copy(nongipfCount,from_b.nongipfCount);
      
        copyFrom(reserve,from_b.reserve);
        copyFrom(captures,from_b.captures);
        
        getCell(pickedSource,from_b.pickedSource);
        getCell(droppedDest,from_b.droppedDest);

        preservationStack.copyFrom(from_b.preservationStack);

        stackIndex = from_b.stackIndex;
  
        sameboard(from_b);
    }
    
    int effective_date_algorithm = 0;
    public void setRevisionDate(String date)
    {	effective_date_algorithm = 0;
    	
		BSDate dd = new BSDate(date);
		BSDate tt = new BSDate("Jan 21 2009");
		if(dd.before(new BSDate("jan 16 2009 17:00"))) { effective_date_algorithm = 1; }
		else if(dd.before(new BSDate("feb 07 2009 04:17"))) { effective_date_algorithm=2; }		
		else if(dd.before(new BSDate("feb 07 2009 04:20"))) { effective_date_algorithm=5; }
		else if(dd.before(new BSDate("feb 07 2009 05:00"))) { effective_date_algorithm=2; }		
		else if(dd.before(new BSDate("feb 07 2009 14:30"))) { effective_date_algorithm=5; }
		else if(dd.before(new BSDate("apr 19 2009 21:26"))) { effective_date_algorithm=2; }
		else if(dd.before(new BSDate("apr 19 2009 21:29"))) { effective_date_algorithm=5; }
		else if(dd.before(new BSDate("may 03 2009"))) { effective_date_algorithm=2; }
		else if(dd.before(new BSDate("may 04 2009"))) { effective_date_algorithm=4; }
		
		//if(!dd.before(tt)) { effective_date_algorithm++; }
		//G.print("Da "+effective_date_algorithm);
		//if(!dd.before(new BSDate("apr 21 2009"))) { effective_date_algorithm++; }
		//
		if(!hasExplicitRevision) 
		{ if(dd.before(tt)) { revision = 100; } else { revision=101; }
		}
    }
    public void doInit(String gtype,long key)
    {
    	doInit(gtype,key,revision);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int rev)
    {	
    	adjustRevision(rev);
    	randomKey = key;
        if (Gipf_Standard_Init.equalsIgnoreCase(gtype)
        	|| Gipf_Tournament_Init.equalsIgnoreCase(gtype)
        	|| Gipf_Init.equalsIgnoreCase(gtype))
        {
            Init_Standard(gtype);
        }
        else
        {
        	throw G.Error(WrongInitError,gtype);
        }
        tournament_setup_done = false;
        whoseTurn = 0;
        moveNumber = 1;
        // note that firstPlayer is NOT initialized here
    }


    public void setFirstPlayer(int who)
    {
        firstPlayer = who;
        setState((firstPlayer >= 0) ? GipfState.DONE_STATE : GipfState.PUZZLE_STATE);
    }

    public void SetNextPlayer()
    {
        switch (board_state)
        {
        case PUZZLE_STATE:
            break;
        case DONE_CAPTURE_STATE:
        case DESIGNATE_CAPTURE_OR_DONE_STATE:
        case DONE_STATE:
        case RESIGN_STATE:
        case DRAW_STATE:
            moveNumber++;
            setWhoseTurn(nextPlayer[whoseTurn]);
            return;

        default:
        	throw G.Error("Move not complete");
        }
    }

    public boolean DoneState()
    {	
        switch (board_state)
        {
        case RESIGN_STATE:
        case DONE_CAPTURE_STATE:
        case DESIGNATE_CAPTURE_OR_DONE_STATE:
        case DRAW_STATE:
        case DONE_STATE:
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	switch(board_state)
    	{	case DONE_STATE: return(true);
	default:
		break; 
     	}
    	return(false);
    }
    public boolean WinForPlayerNow(int ind)
    {
    	if(board_state==GipfState.GAMEOVER_STATE) { return(win[ind]); }
    	return(false);
    }

    public GipfCell pickSource(GipfId src)
    {
    	switch(src)
    	{
    	default: throw G.Error("not expecting %s",src);
    	case First_Player_Reserve: return(reserve[FIRST_PLAYER_INDEX]);
    	case Second_Player_Reserve: return(reserve[SECOND_PLAYER_INDEX]);
    	case First_Player_Captures: return(captures[FIRST_PLAYER_INDEX]); 
    	case Second_Player_Captures: return(captures[SECOND_PLAYER_INDEX]); 
    	}
    }
    public void pickFromCell(GipfCell c)
    {	pickedSource[stackIndex] = c;
    	pickedObject = c.removeTop();
    	pickedHeight++;
    }
    public void pickFromRack(GipfCell c)
    {	pickFromCell(c);
    	pickedSource[stackIndex]=c;
    	if((board_state==GipfState.PLACE_GIPF_STATE)||(board_state==GipfState.PRECAPTURE_OR_START_GIPF_STATE))
    		{ pickFromCell(c); 
    		}
    }
    public void dropOnRack(GipfCell c)
    {  	while(pickedHeight>0) { pickedHeight--; c.addChip(pickedObject); }
    	dropUndo[stackIndex]=removalCells.size()*100;
    	droppedDest[stackIndex] = null;
    	pickedObject = null;
    }
    public void dropOnBoard(GipfCell dest)
    {	droppedDest[stackIndex] = dest;
    	dropUndo[stackIndex]=removalCells.size()*100;
    	dropOnBoardCell(dest);
    }
    public void dropOnBoardCell(GipfCell dest)
    {	G.Assert(dest.onBoard,"is on board");
    	while(pickedHeight>0) 
    		{ pickedHeight--; 
    		  dest.addChip(pickedObject);
    		  int player = playerIndex(pickedObject);
    		  if(dest.isGipf()) 
    		  	{ currentGipfCount[player]++;
    		  	  gipfCount[player]++;
    		  	  nongipfCount[player]--;
    		  	}
    		  else { nongipfCount[player]++; }
    		}
    	pickedObject = null;
    }
    public void pickFromBoard(GipfCell src)
    {
    	pickedSource[stackIndex] = src;
    	pickFromBoardCell(src);
    }
    public void pickFromBoardCell(GipfCell src)
    {	G.Assert(src.onBoard,"is a board cell");
    	pickedHeight = src.height();
    	while(src.topChip()!=null)
    	{	boolean gipf = src.isGipf();
    		GipfChip ch = pickedObject = src.removeTop();
    		int player = playerIndex(ch);
    		if(gipf) { nongipfCount[player]++; currentGipfCount[player]--; gipfCount[player]--; }
    		else { nongipfCount[player]--; }
    	}
    }
    public boolean isDest(char col,int row)
    {	GipfCell c = getCell(col,row);
    	return((stackIndex>0)&&(c==droppedDest[stackIndex-1]));
    }
    public void unDropObject()
    {
    	if(droppedDest[stackIndex]!=null) 
    	{ 	int undo = dropUndo[stackIndex]%PRESSTACK;
    		int presinfo = dropUndo[stackIndex]/PRESSTACK;
    		int dist = undo%REMOVESTACK;
    		int uncap = undo/REMOVESTACK;
    		if(uncap<removalCells.size())
    		{
    			undoCaptures(uncap,whoseTurn);
    		}
    		if(dist>0) 
    			{ undoSlide(droppedDest[stackIndex],pickedSource[stackIndex],dist);
    			  if(presinfo<preservationStack.size())  
    			  	{ discardPreservation(preservationStack);
    			  	  //restorePreservation(preservationStack); 
    			  	}
    			  pickFromBoardCell(pickedSource[stackIndex]);
     			}
    		else
    		{	
    			pickFromBoardCell(droppedDest[stackIndex]);
     		}
    		
    		
    	}
    	else
    	{	pickFromBoardCell(droppedDest[stackIndex]);
    	}
    	
    	droppedDest[stackIndex] = null;
    	dropUndo[stackIndex]=0;
    }
    public GipfCell getSource()
    {
    	return(pickedSource[stackIndex]);
    }
    public GipfCell getDest()
    {	if(stackIndex<1) { return(null); }
    	return(droppedDest[stackIndex-1]);
    }
    public void unPickObject()
    {	GipfCell c = pickedSource[stackIndex];
    	if(c!=null) 
    		{ if(c.onBoard) { dropOnBoardCell(c); } else { dropOnRack(c); }
    		}
    		else if(pickedObject!=null)
    		{ throw G.Error("picked object has no source"); 
    		}
    	pickedObject = null;
    	pickedSource[stackIndex] = null;
   	
    }
    private void finalizePlacement()
    {  	while(stackIndex>=0) 
    	{ pickedSource[stackIndex]=droppedDest[stackIndex]=null;
    	  dropUndo[stackIndex]=removalCells.size()*100;
    	  stackIndex--; }
    	stackIndex=0;
    }

    // move a stack, bottom first
    private void moveStack(GipfCell from,GipfCell to)
    {	GipfChip ch = from.removeTop();
    	if(from.chipIndex>=0) { moveStack(from,to); }
    	to.addChip(ch);
    	from.preserved = false;
    }
    // do the slide portion, return the number of stones moved
    private int doSlide(GipfCell from,GipfCell to,int dir,replayMode replay,boolean animateFirst)
    {	int val = 1;
    	if(to.topChip()!=null) { val += doSlide(to,to.exitTo(dir),dir,replay,true); }
    	//G.Assert(!to.isEdgeCell(),"not edge1");
    	moveStack(from,to); 
    	if(replay!=replayMode.Replay && animateFirst)
    	{
    		animationStack.push(from);
    		animationStack.push(to);
    		if(to.height()>1)
    		{	animationStack.push(from);
    			animationStack.push(to);
    		}
    	}
    	return(val);
    }
 
    // return true if something is available for removal
    private boolean markForRemoval(int pl)
    {	boolean val = false;
    	for(GipfCell c =allCells; c!=null; c=c.next)
    	{	c.rowcode = 0;
    	}
    	for(int dir = 0;dir<3;dir++)
    	{	int dircode = (1<<dir);
    	for(GipfCell c = allCells; c!=null; c=c.next)
    	{	
    		if(!c.isEdgeCell() && ((c.rowcode&dircode)==0))
    		{	boolean nr = c.rowOfNInDirection(4,getColorMap()[pl],dir);
    			if(nr)
    				{ c.markRow(dir,dircode);
    				  c.markRow(dir+3,dircode);
    				  val = true;
    				}
    		}
    	}
    	}
    	return(val);
    }

    private boolean allPreserved()
    {
    	for(GipfCell c = allCells; c!=null; c=c.next)
    		{ if((c.topChip()!=null) && (c.rowcode!=0) && !c.preserved) 
    			{ return(false); 
    			}
    		}
    	return(true);
    }

    // returns true if everything is preserved by default
    private boolean markForPreservation(int pl)
    {	boolean val = standard_setup || tournament_setup;
    	int n = 0;
    	if(val)
    	{
    	   	for(GipfCell c = allCells; c!=null; c=c.next)
        	{	if(c.preserved)
        			{ 
        			preservationStack.push(moveNumber*10000+(c.col-'A')*100+c.row);
        			n++;
        			c.preserved = false;
        			}
        		if(c.rowcode!=0)
        		{
        		if(c.isGipf())
        		{
        		// we have a gipf cell marked for deletion, preserve it if it is ours
        		// or if it is the opponents in a multiple row
        		GipfChip top = c.topChip();
        		if(playerIndex(top)==pl)
        			{ 
        			c.preserved=true; 
        			preservationStack.push(moveNumber*10000+(c.col-'A')*100+c.row);
        			n++;
        			}
        		else { switch(c.rowcode)
        			{
        			default: c.preserved = true;	// multiple rows
        				preservationStack.push(moveNumber*10000+(c.col-'A')*100+c.row);
        				n++;
        				break;
        			case 1:
        			case 2:
        			case 4: break;
        			}
        		}
        		}
        		else if(c.topChip()!=null)
        			{ val=false; }
        		}
        	}
     	}
    	preservationStack.push(n);
    	return(val);
    }
    private boolean simpleRemoval()
    {	for(GipfCell c = allCells; c!=null; c=c.next)
    	{
    	switch(c.rowcode)
    	{
    	default: 
    		return(false);	// cell has more than one remove direction
    	case 4:
    	case 2:
    	case 1:
    	case 0: 	break;
    	}
    	}
    	return(true);
    }
    private void captureStack(GipfCell c,replayMode replay)
    {	
    	int pl = -1;
    	c.preserved = false;
 		if(c.isGipf()) { currentGipfCount[playerIndex(c.topChip())]--; }
 		while(c.chipIndex>=0)
    	{	GipfChip top = c.removeTop();
    		removalStack.addChip(top);
    		removalCells.push(c);
    		pl = playerIndex(top);
    		GipfCell dest = (pl==whoseTurn) ? reserve[whoseTurn] : captures[whoseTurn];
    		dest.addChip(top);
    		if(replay!=replayMode.Replay)
    		{
    			animationStack.push(c);
    			animationStack.push(dest);
    		}
    	}
    }
    private boolean capturesPending()
    {	if(markForRemoval(whoseTurn))
    	{
    	for(GipfCell c = allCells; c!=null; c=c.next)
    	{	if((c.rowcode!=0) 
    			&& !c.preserved
    			&& (c.topChip()!=null)
    			&& (playerIndex(c.topChip())==whoseTurn)) 
    		{ return(true); }
    	}}
    	return(false);
    }
    private void doCaptures(replayMode replay)
    {	if(markForRemoval(whoseTurn))
    	{	G.Assert(      (board_state==GipfState.DESIGNATE_CAPTURE_OR_DONE_STATE)
    					|| (board_state==GipfState.PRECAPTURE_OR_START_NORMAL_STATE)
    					|| (board_state==GipfState.PRECAPTURE_OR_START_GIPF_STATE)
    					|| simpleRemoval(),
    					"must be simple");
    		for(GipfCell c = allCells; c!=null; c=c.next)
    		{
     		if((c.rowcode!=0) && !c.preserved)
    		 {
      		 captureStack(c,replay);
     		 }
     		c.preserved=false;
    		}
    		
    	}
    }
    private void doRowCaptures(GipfCell c,int dir,replayMode replay)
    {	if(c!=null)
    	{GipfChip top = c.topChip();
    	if((top!=null)&&(c.rowcode!=0)) 
    	{ 
    	  if(c.isGipf()) 
    		{ if(!c.preserved) 
    			{ 
    			  captureStack(c,replay);
    			}
    		}
    	else{
    	  captureStack(c,replay);
    	}
    	}
    	// note, we iterate over the entire row in this direction, so the case where
    	// we designated a row by clickon on an edge will work as indended.
    	doRowCaptures(c.exitTo(dir),dir,replay); 
    	}
    }
    private void setGameOver(boolean winCurrent,boolean winNext)
    {  	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	finalizePlacement();
    	setState(GipfState.GAMEOVER_STATE);
    }
    public boolean canTogglePlacementMode()
    {	return(	(tournament_setup && !tournament_setup_done)
    			&& (gipfCount[whoseTurn]>0)
    			&& ((nongipfCount[0]==0)||(nongipfCount[1]==0)));	
    }
    public boolean canTogglePlacementMode(int who)
    {	return(	(tournament_setup && !tournament_setup_done)
    			&& ((board_state==GipfState.PLACE_STATE) || (board_state==GipfState.PLACE_GIPF_STATE)||(board_state==GipfState.PRECAPTURE_OR_START_GIPF_STATE))
    			&& (who==whoseTurn)
    			&& (gipfCount[whoseTurn]>0)
    			&& (nongipfCount[who]==0));	
    }
    private boolean placingGipfPieces()
    {
    	return((tournament_setup && !tournament_setup_done) 
		&& (nongipfCount[whoseTurn]==0));	
    }
    public boolean placingGipfPieces(int who)
    {
    	return((tournament_setup && !tournament_setup_done) 
    			&& ((who!=whoseTurn) 
    					? (nongipfCount[who]==0) 
    					: ((board_state==GipfState.PLACE_GIPF_STATE)||(board_state==GipfState.PRECAPTURE_OR_START_GIPF_STATE))));	
    }
    
    // set the first state of a new turn
    private void setFirstState()
    {	setState(GipfState.PUZZLE_STATE);
    	if(markForRemoval(whoseTurn))
	    	{
 	    	setState(markForPreservation(whoseTurn)
	    					? (placingGipfPieces()
	    							?GipfState.PRECAPTURE_OR_START_GIPF_STATE
	    							:GipfState.PRECAPTURE_OR_START_NORMAL_STATE)
	    					: GipfState.PRECAPTURE_STATE);
	    	
	    	}
	    	else if(placingGipfPieces())
	    	{
	    	setState(GipfState.PLACE_GIPF_STATE);
	    	}
	    	else
	    	{
	        setState(GipfState.PLACE_STATE);
	    	}
        if(((board_state==GipfState.PLACE_GIPF_STATE)||(board_state==GipfState.PLACE_STATE)) 
        		&& (reserve[whoseTurn].chipIndex<0))
        {	setGameOver(false,true);
        }
        if((standard_setup || tournament_setup))
        	{if((gipfCount[whoseTurn]>0)&&(currentGipfCount[whoseTurn]==0))
        		{ setGameOver(false,true); }
        	 int next = nextPlayer[whoseTurn];
        	 if((gipfCount[next]>0)&&(currentGipfCount[next]==0)) 
        	 	{ setGameOver(true,false); }
        	}
    }
    // set the state appropriately after a slide move
    private void setLastState()
    {
 		if(markForRemoval(whoseTurn))
		{	

    		setState(markForPreservation(whoseTurn)
    				? GipfState.DESIGNATE_CAPTURE_OR_DONE_STATE
    				: (simpleRemoval()?GipfState.DONE_CAPTURE_STATE:GipfState.DESIGNATE_CAPTURE_STATE));
   		}
		else
		{ 
		  setState(GipfState.DONE_STATE);
		}
    }
    public void clearAllPreserved()
    {
    	for(GipfCell c = allCells; c!=null; c=c.next) { c.preserved = false; }
    }
    public void doDone(replayMode replay)
    {	
    	finalizePlacement();
    	tournament_setup_done = (nongipfCount[0]>0) && (nongipfCount[1]>0);
        // dont change lastmove
        if (board_state==GipfState.RESIGN_STATE)
        {
            win[(whoseTurn == FIRST_PLAYER_INDEX) ? SECOND_PLAYER_INDEX
                                                  : FIRST_PLAYER_INDEX] = true;
            setState(GipfState.GAMEOVER_STATE);
        }
        else
        {	
        	switch(board_state)
        	{
        	default: throw G.Error("Not expecting state %s",board_state);
        	case DESIGNATE_CAPTURE_OR_DONE_STATE:
        	case DONE_CAPTURE_STATE: 
        		doCaptures(replay);
				//$FALL-THROUGH$
			case DONE_STATE:
                SetNextPlayer();
                clearAllPreserved();
                setFirstState();
        		break;
        	case DRAW_STATE:
        		SetNextPlayer();
        		setGameOver(false,false);
        		break;
        	}

        }
	
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Gipfmovespec m = (Gipfmovespec)mm;
        //G.print("E "+mm);
        //check_piece_count();
        switch (m.op)
        {
        case MOVE_DONE:
        	doDone(replay);

            break;

 
        case MOVE_START:
        	whoseTurn = m.player;
        	setFirstState();
            break;
        case MOVE_EDIT:
        	setState(GipfState.PUZZLE_STATE);
        	setWhoseTurn(FIRST_PLAYER_INDEX);
        	finalizePlacement();
        	break;
        case MOVE_PICK:
        	pickFromRack(pickSource(m.source));
        	break;
        case MOVE_DROP:
        	dropOnRack(pickSource(m.source));
        	switch(board_state)
        	{
        	case SLIDE_STATE:	setState(GipfState.PLACE_STATE); break;
        	case SLIDE_GIPF_STATE: setState(GipfState.PLACE_GIPF_STATE); break;
        	case PUZZLE_STATE: finalizePlacement(); break;
        	default: break;
        	}
        	break;
        case MOVE_SLIDE:
        	{ GipfCell src = getCell(m.from_col,m.from_row);
       		  GipfCell from = reserve[whoseTurn];
    		  GipfChip pick = from.removeTop();
    		  src.addChip(pick);
       		if(board_state==GipfState.PLACE_GIPF_STATE) 
    		{	src.addChip(reserve[whoseTurn].removeTop());
    			gipfCount[whoseTurn]++;
    			currentGipfCount[whoseTurn]++;
    		}
    		else { nongipfCount[whoseTurn]++; }
        	}
			//$FALL-THROUGH$
		case MOVE_SLIDEFROM:
        	{	int psize = preservationStack.size();        		
        		GipfCell dest = getCell(m.to_col,m.to_row);
        		GipfCell src = getCell(m.from_col,m.from_row);
        		
        		int distance = doSlide(src,dest,
        				findDirection(src.col,src.row,dest.col,dest.row),replay,true);
        		m.undoinfo = psize*PRESSTACK+removalCells.size()*REMOVESTACK+distance;
        		setLastState();	// and mark for preservation
        	}
        	break;
        case MOVE_DROPB:
        	{
        	GipfCell dest = getCell(m.to_col,m.to_row);
        	switch(board_state)
        	{
        	default: throw G.Error("Not expecting state %s",board_state);
        	case SLIDE_GIPF_STATE:
         	case SLIDE_STATE:
        		if(dest.isEdgeCell()) 
        			{ if(pickedObject==null) { stackIndex--; unDropObject();  dropOnBoard(dest); stackIndex++; } 
        				else { 	pickedSource[stackIndex]=null; stackIndex--; dropOnBoard(dest); stackIndex++; }
        			}
        			else 
        			{ int psize =  preservationStack.size();
        			  boolean animateFirst = pickedObject==null; 
        			  if(animateFirst) { pickFromBoard(droppedDest[stackIndex-1]); }
        			  GipfCell from = pickedSource[stackIndex];
        			  unPickObject(); 
         			  int cap = removalCells.size()*REMOVESTACK;
         			  if(replay!=replayMode.Replay && animateFirst)
         			  {
         			  animationStack.push(from);
         			  animationStack.push(dest);
         			  }
        			  dropUndo[stackIndex] = m.undoinfo = psize*PRESSTACK+cap+doSlide(from,dest,
        					  findDirection(from.col,from.row,dest.col,dest.row),replay,animateFirst); 
        			  droppedDest[stackIndex]=dest;
        			  pickedSource[stackIndex]=from;
        			  stackIndex++;
        			  setLastState();
 
          			}
        		break;
        	case PUZZLE_STATE: 
        		if(pickedObject!=null)
        			{ dropOnBoard(dest);
        			  finalizePlacement();
        			}
        		break;
        	case PRECAPTURE_OR_START_GIPF_STATE:
        	case PRECAPTURE_OR_START_NORMAL_STATE:
        		if(pickedObject!=null) { unPickObject(); }
        		dropUndo[stackIndex]=preservationStack.size()*PRESSTACK+removalCells.size()*REMOVESTACK;
        		pickedSource[stackIndex]=droppedDest[stackIndex]=dest;
        		stackIndex++;
        		doCaptures(replay);		// might capture some gipf pieces
				//$FALL-THROUGH$
			case PLACE_STATE: 
        	case PLACE_GIPF_STATE:
        		G.Assert(dest.isEdgeCell(),"can only drop on edges");
        		boolean animateFirst = pickedObject==null;
        		if(animateFirst) { pickFromRack(reserve[whoseTurn]); }
        		if(replay!=replayMode.Replay && animateFirst)
        		{
        			animationStack.push(getSource());
        			animationStack.push(dest);
        		}
        		dropOnBoard(dest);
        		setState((dest.height()==2)?GipfState.SLIDE_GIPF_STATE:GipfState.SLIDE_STATE); 
        		stackIndex++;
        		break;
        	
        	}
        	}
        	break;
        case MOVE_PICKB:
        	{
        	GipfCell c = getCell(m.from_col,m.from_row);
        	switch(board_state)
        	{
        	default: throw G.Error("Not expecting state %s",board_state);
        	case DONE_STATE:
        	case DRAW_STATE:
        	case DONE_CAPTURE_STATE:
        	case DESIGNATE_CAPTURE_OR_DONE_STATE:
        	case DESIGNATE_CAPTURE_STATE:
        		G.Assert(c==droppedDest[stackIndex-1],"is dest");
        		stackIndex--;
        		unDropObject();
        		setState((pickedHeight==2)?GipfState.SLIDE_GIPF_STATE:GipfState.SLIDE_STATE);
        		break;
        	case SLIDE_GIPF_STATE:
         	case PLACE_STATE:	// unusual, piece already placed on the edge
        	case SLIDE_STATE:	
        		pickFromBoard(c);
        		break;
           	case PUZZLE_STATE: 
        		pickFromBoard(c);
        		break;
        	}}
        	break;
        case MOVE_RESIGN:
        	unPickObject();
        	setState((unresign==null) ? GipfState.RESIGN_STATE : unresign);
            break;
        case MOVE_STANDARD:
        	{
        	G.Assert(canTogglePlacementMode(),"shouldn't try to switch modes here");
        	switch(board_state)
        	{
        	default: throw G.Error("Not expecting state %s",board_state);
        	case PLACE_GIPF_STATE: setState(GipfState.PLACE_STATE); break;
        	case PLACE_STATE: setState(GipfState.PLACE_GIPF_STATE); break;
        	case PRECAPTURE_OR_START_NORMAL_STATE: setState(GipfState.PRECAPTURE_OR_START_GIPF_STATE); break;
        	case PRECAPTURE_OR_START_GIPF_STATE: setState(GipfState.PRECAPTURE_OR_START_NORMAL_STATE); break;
        	}}
        	break;
        case MOVE_PRESERVE:
        	{
        		GipfCell c = getCell(m.from_col,m.from_row);
        		if(c.isGipf())
        		{
        		c.preserved = !c.preserved;	
        		}
        	}
        	break;
        case MOVE_REMOVE:
        	m.undoinfo = preservationStack.size()*PRESSTACK+removalCells.size()*REMOVESTACK+0;
        	markForRemoval(whoseTurn);
        	GipfCell c = getCell(m.from_col,m.from_row);
        	if(c.isGipf())
        	{	c.preserved = false;	// no longer preserved
        	}
        	else
        	{
        	int direction = 0;
        	switch(c.rowcode)
        	{
        	default: throw G.Error("Not expecting row code %s",c.rowcode);
        	case 1: 	direction = 0; break;
        	case 2:		direction = 1; break;
        	case 4: 	direction = 2; break;
        	}
        	pickedSource[stackIndex] = droppedDest[stackIndex] = c;
        	dropUndo[stackIndex]=removalCells.size()*100;
        	stackIndex++;
        	doRowCaptures(c.exitTo(direction),direction,replay);
        	doRowCaptures(c,direction+3,replay);
        	{	boolean mark = markForRemoval(whoseTurn);
        			if((board_state==GipfState.PRECAPTURE_STATE)
        					||(board_state==GipfState.PRECAPTURE_OR_START_GIPF_STATE)
        					||(board_state==GipfState.PRECAPTURE_OR_START_NORMAL_STATE))
        			{	if(mark)
        				{ setState(markForPreservation(whoseTurn)
        						?(placingGipfPieces()?GipfState.PRECAPTURE_OR_START_GIPF_STATE:GipfState.PRECAPTURE_OR_START_NORMAL_STATE)
        						:GipfState.PRECAPTURE_STATE );
        				  
        				}
        			else {
        				clearAllPreserved();
        				setState(GipfState.PLACE_STATE);
        			}
        			}
        			else 
        			{ // post captures
        				setState(mark 
        						? (simpleRemoval() 
        							?GipfState.DONE_CAPTURE_STATE: 
        											(allPreserved()?GipfState.DESIGNATE_CAPTURE_OR_DONE_STATE:GipfState.DESIGNATE_CAPTURE_STATE))
        						:GipfState.DONE_CAPTURE_STATE);
        			}
        	}}
        	break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        	
        default:
        	cantExecute(m);
        }

        //check_piece_count();
        return (true);
    }
    public void discardPreservation(IStack pStack)
    {
    	int n = pStack.pop();
        while(n-- > 0)
      	 {	int code = pStack.pop();
   	 		int row = code%100;
   	 		char col = (char)(((code%10000)/100)+'A');
   	 		GipfCell c = getCell(col,row);
   	 		c.preserved = false;
      	 }
    }
    public void restorePreservation(IStack pStack)
    {	clearAllPreserved();
        { int n = pStack.pop();
        while(n-- > 0)
      	 {	int code = pStack.pop();
      	 	int row = code%100;
      	 	char col = (char)(((code%10000)/100)+'A');
      	 	GipfCell c = getCell(col,row);
      	 	c.preserved = true;
      	 }
      }
    }
    public void savePreservation(IStack pStack)
    {	int n=0;
    	for(GipfCell c=allCells;c!=null;c=c.next) 
    	{ if(c.height()>0) {  if(c.preserved) {n++;  pStack.push((c.col-'A')*REMOVESTACK+c.row); }}
    	}
    	pStack.push(n);
   }

   /** assistance for the robot.  In addition to executing a move, the robot
   requires that you be able to undo the execution.  The simplest way
   to do this is to record whatever other information is needed before
   you execute the move.  It's also convenient to automatically supply
   the "done" confirmation for any moves that are not completely self
   executing.
   */
   public void RobotExecute(Gipfmovespec m)
   {   //G.print("R "+m);
       m.undostate = board_state; //record the starting state. The most reliable
       // to undo state transistions is to simple put the original state back.
       savePreservation(robotPreservationStack);
       robotPreservationStack.push(preservationStack.size());
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
           else if(board_state==GipfState.PRECAPTURE_OR_START_NORMAL_STATE) {}
           else if(board_state==GipfState.PRECAPTURE_OR_START_GIPF_STATE) {}
           else if(board_state==GipfState.PRECAPTURE_STATE) {}		// capture before moving
           else if(board_state==GipfState.DESIGNATE_CAPTURE_STATE) {}	// designate after slide
           else if(board_state==GipfState.PLACE_STATE) {}		// place after early capture
           else if(board_state==GipfState.PLACE_GIPF_STATE) {}	// place gipf after early capture
           else
           {
        	   throw G.Error("Robot move should be in a done state");
           }
       }
   }

private void undoCaptures(int uncap,int who)
{
 		while(removalCells.size()>uncap)
	   		{	GipfCell c = removalCells.pop();
	   			GipfChip ch = removalStack.removeTop();
	   			if(playerIndex(ch)==who)
	   			{	reserve[who].removeTop();
	   			}
	   			else 
	   			{	captures[who].removeTop();
	   			}
	   			c.addChip(ch);
	   			if(c.isGipf()) { currentGipfCount[playerIndex(ch)]++; }
	   		}	
}
	private void undoSlide(GipfCell to,GipfCell from,int distance)
	{ 	int dir = findDirection(from.col,from.row,to.col,to.row);
		while(distance>0)
  	   		{	distance--;
  	   			moveStack(to,from);
   	   			from = to;
  	   			to = to.exitTo(dir);
  	   		}
 	}
	private void undoDrop(GipfCell from,Gipfmovespec m)
	{	int who = m.player;
		switch(m.undostate)
	   		{	default: throw G.Error("Not expecting state %s",m.undostate);
   			case PRECAPTURE_OR_START_NORMAL_STATE:
	   			case PRECAPTURE_OR_START_GIPF_STATE:
	   				if(from.isGipf()) 
	   					{ currentGipfCount[who]--; gipfCount[who]--; } 
	   					else { nongipfCount[who]--;} ;
	   				break;
	   			case PLACE_STATE:	nongipfCount[who]--; break;
	   			case PLACE_GIPF_STATE: gipfCount[who]--; currentGipfCount[who]--; break;
	   		}
	   		while(from.chipIndex>=0) 
	   			{ 	GipfChip ch = from.removeTop();
	   				reserve[who].addChip(ch);
	   			}
	}
  //
   // un-execute a move.  The move should only be unexecuted
   // in proper sequence.  This only needs to handle the moves
   // that the robot might actually make.
   //
   public void UnExecute(Gipfmovespec m)
   {	int who = m.player;
      	//G.print("U "+m+" for "+whoseTurn);
   	   check_piece_count();
       switch (m.op)
       {
       case MOVE_DROPB:
    	   {	
    	   GipfCell from = getCell(m.from_col,m.from_row);
    	   undoDrop(from,m);
    	   }
    	   break;
       case MOVE_START:
       case MOVE_PICK:
       case MOVE_DROP:
       case MOVE_PRESERVE:
       case MOVE_EDIT: // robot never does these
  	    default:
  	    	cantUnExecute(m);
        	break;
  	   case MOVE_REMOVE:
  		   undoCaptures((m.undoinfo%PRESSTACK)/REMOVESTACK,who);
  		   break;
  	   case MOVE_SLIDEFROM:
  	   case MOVE_SLIDE:
  	   	{	int uinfo = m.undoinfo%PRESSTACK;
  	   		int distance = uinfo%REMOVESTACK;
  	   		int uncap = uinfo/REMOVESTACK;
   	   		undoCaptures(uncap,who);
  	   		
  	   		GipfCell from = getCell(m.from_col,m.from_row);
  	   		GipfCell to = getCell(m.to_col,m.to_row);
  	   		undoSlide(to,from,distance);
   	   		if(m.op==MOVE_SLIDE)
   	   		{
  	   		undoDrop(from,m);
   	   		}
  	   	}
  	   		break;
       case MOVE_DONE:
       case MOVE_STANDARD:	// change in state only, so nothing else to do
       case MOVE_RESIGN:
           break;
       }
       setState(m.undostate);
       if(whoseTurn!=who)
       {	moveNumber--;
       	setWhoseTurn(who);
       }
       check_piece_count();
       tournament_setup_done = (nongipfCount[0]>0) && (nongipfCount[1]>0);
       int ps = robotPreservationStack.pop();
       while(preservationStack.size()>ps) { preservationStack.pop(); }
       restorePreservation(robotPreservationStack);

}   

   // look for a win for player.  
   public double ScoreForPlayer(int player,boolean print,boolean dumbot)
   {  	double finalv=reserve[player].chipIndex*5+captures[player].chipIndex*20+currentGipfCount[player]*40;
   		if(tournament_setup) 
   		{	finalv -= Math.max(0,(currentGipfCount[player]-3))*50;	// penalty for too many gipf pieces
   		}
   		if(!dumbot)
   		{	int threes = 0;
   			int forks = 0;
   			for(GipfCell c = allCells;
   				c!=null;
   				c=c.next)
   			{	// count x a x and x x x 
   				if(!c.isEdgeCell())
   				{	for(int dir=0;dir<3;dir++)
   					{
   					GipfCell left = c.exitTo(dir);
   					GipfChip ltop = left.topChip();
   					if(ltop!=null)
   					{
	   				GipfCell right = c.exitTo(dir);
   					GipfChip rtop = right.topChip();
   					if((rtop!=null) && (playerIndex(ltop)==player) && (playerIndex(rtop)==player))
   					{	forks++;
   						GipfChip ctop = c.topChip();
   						if((ctop!=null) && (playerIndex(ctop)==player)) { threes++; } 
   					}
   					}
   					}
   				}
   			}
   			finalv += 2*forks + 3*threes;
   		}
   		if(print) 
   			{ System.out.println("sum "+finalv); }
   		return(finalv);
   }
   public String playerState(int pl)
   {	return(""+(reserve[pl].chipIndex+1)+"x"+(captures[pl].chipIndex+1));
   }
   
   private void addSlideMoves(CommonMoveStack all,GipfCell c,int player,int op)
   {
	   for(int dir = 0;dir<CELL_FULL_TURN; dir++)
  		{	GipfCell d = c.exitTo(dir);
  			if((d!=null)
  				&& d.emptyCellInDirection(dir)
  				&& ((d.chipIndex>=0) || (d.sweep_counter!=sweep_counter))
  				)
  				{ d.sweep_counter = sweep_counter;
  				  Gipfmovespec mm = new Gipfmovespec(player,op,c.col,c.row,d.col,d.row);
  				  all.addElement(mm);
  		}   	}
   }
   
   public CommonMoveStack  GetListOfMoves()
   {	CommonMoveStack v = new CommonMoveStack();
   		switch(board_state)
   		{
   		default: throw G.Error("not implemented");
   		case SLIDE_GIPF_STATE:
   		case SLIDE_STATE:
   			sweep_counter++;
   			addSlideMoves(v,getDest(),whoseTurn,MOVE_SLIDEFROM);
   			break;
   		case PRECAPTURE_STATE:
   		case DESIGNATE_CAPTURE_STATE:
   		{	
   			markForRemoval(whoseTurn);
   			for(GipfCell c=allCells; c!=null; c=c.next)
   			{	if((c.topChip()!=null) && !c.isGipf())
   				{
   				int sweep = c.rowcode;
   				int dir = -1;
   				switch(sweep)
   				{
   				default: break;
   				case 1:	dir = 0; break;
   				case 2: dir = 1; break;
   				case 4: dir = 2; break;
   				}
   				if(dir>=0)
   				{	v.addElement(new Gipfmovespec(whoseTurn,MOVE_REMOVE,c.col,c.row));
   				c.exitTo(dir).markRow(dir,-1);
   				c.markRow(dir+3,-1);
   				}}
  				}
   			}
   			break;
   		case PLACE_GIPF_STATE:
   			// add the option to switch to regular
   			if((reserve[whoseTurn].height()>0) && canTogglePlacementMode()) 
   				{ v.addElement(new Gipfmovespec(whoseTurn,MOVE_STANDARD)); }
			//$FALL-THROUGH$
		case PRECAPTURE_OR_START_NORMAL_STATE:
		case PRECAPTURE_OR_START_GIPF_STATE:
			// in this state, there is a row of GIPF pieces which could be removed.
			if(reserve[whoseTurn].height()==0) { new Gipfmovespec(whoseTurn,MOVE_RESIGN,'A',1); break; }
			//$FALL-THROUGH$
		case PLACE_STATE:
   		{	
   			sweep_counter++;
   			G.Assert(reserve[whoseTurn].height()>0,"must be pieces");
   			for(GipfCell c=allCells; c!=null; c=c.next)
   			{	if(c.isEdgeCell())
   				{
   				addSlideMoves(v,c,whoseTurn,MOVE_SLIDE);
   				
   			}}
    		}
   		}
	   	return(v);
   }

}
