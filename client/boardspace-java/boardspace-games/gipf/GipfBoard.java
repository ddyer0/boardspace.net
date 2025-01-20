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
package gipf;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

public class GipfBoard extends hexBoard<GipfCell> implements BoardProtocol,GipfConstants
{   //dimensions for the viewer to use
	
	// revision 100 applies to some very old games.
	// revision 101 reduces the count of chips by 1
	// revision 102 fixes the endgame rules for standard and tournament games
	public static final int REVISION = 102;
	public int getMaxRevisionLevel() { return(REVISION); }
	
	
	static final int N_STANDARD_CHIPS = 18;
	static final int N_BASIC_CHIPS = 15;
	static final int MAXDESTS = 30;
	static final String GIPF_GRID_STYLE[] = { "1", null, "A" };


	public boolean hasExplicitRevision = false;
	private GipfState board_state = GipfState.PUZZLE_STATE;
	private StateStack stateStack = new StateStack();
	private StateStack robotStateStack = new StateStack();
	private GipfState tamskState = null;
	private GipfState unresign = null;
	public GipfState getState() { return(board_state); }
	public CellStack animationStack = new CellStack();
	public static final GipfChip standardChip[] = {GipfChip.White, GipfChip.Black};
	public GipfChip playerChip[] = {GipfChip.White, GipfChip.Black};
	public GColor playerColor[] = { GColor.W, GColor.B };
	private GipfCell edgeCells[] = null;
	public int gipfsOnBoard(int who)
	{
		int n = 0;
		GipfCell cap = captures[nextPlayer[who]];
		for(int lim = cap.height()-1; lim>=0; lim--)
				{
				if(cap.chipAtIndex(lim).potential==Potential.None) { n++; }
				}
		return 3-n;
	}
	public boolean swappedColors()
	{
		return playerChip[0]!=standardChip[0];
	}
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(GipfState st) 
	{ 	unresign = (st==GipfState.RESIGN_STATE)?board_state:null;
		board_state = st;
		switch(st)
		{
		case GAMEOVER_STATE: 
			break;
		case PLACE_TAMSK_FIRST_STATE:
		case PLACE_TAMSK_LAST_STATE:
			tamskState = st;
			//$FALL-THROUGH$
		default:
			AR.setValue(win,false); 	// make sure "win" is cleared
		}
	}
	public static int PRESSTACK = 100000;
	public static int REMOVESTACK = 100;
    public int firstPlayer = -1; // changed as the game starts
 
    public int lastPlacedIndex = -1;
    
    public boolean standard_setup = false;
    public boolean tournament_setup = false;
    public boolean tournament_setup_done = false;
    private GipfCell TamskCenter = null;
    public GipfCell rack[][] = new GipfCell[2][Potential.values().length];
    public GipfCell captures[]=new GipfCell[2];
    public int initial_height = 0;
    public int currentGipfCount[] = new int[2];					// count of double pieces currently on the board
    public int currentSingleCount[] = new int[2];				// count of single pieces with potential=None
    public int singlePiecesPlayed[] = new int[2];				// count of nongipfs ever committed
    public int doublePiecesPlayed[] = new int[2];				// count of gipfs ever committed
    private int sweep_counter = 0;
    private CellStack pickedSource = new CellStack();
    private int ignorePlease = 0;
    private CellStack droppedDest = new CellStack();
    private IStack robotPreservationStack = new IStack(); 
    private IStack preservationStack = new IStack();
    private IStack dropUndo = new IStack();
    public GipfChip pickedObject = null;
    public int pickedHeight = 0;
    public int movingObjectIndex() 
    	{ GipfChip ch = pickedObject;
    	  return((ch==null)?NothingMoving:ch.pieceNumber()); 
    	}
  
    public void addChip(GipfCell c,GipfChip ch)
    {	
    	c.addChip(ch);
    	if(c.onBoard)
    	{
    	switch(c.height())
    	{
    	case 1:
    		if(ch.potential==Potential.None) { currentSingleCount[playerIndex(ch)]++; }
    		break;
    	case 2:
    		{
    		GipfChip ch0 = c.chipAtIndex(0);
     		if(ch0==ch) { currentGipfCount[playerIndex(ch)]++; }
     		if(ch0.potential==Potential.None) { currentSingleCount[playerIndex(ch0)]--; }
    		}
    		break;
    	case 3:	// 3 exactly, might have been a gipf
     		{
     			GipfChip ch0 = c.chipAtIndex(0);
     			GipfChip ch1 = c.chipAtIndex(1);
     			if(ch0==ch1) { currentGipfCount[playerIndex(ch0)]--; }
     		}
     		break;
     	default: // more than 3, no way
    	}
    	}
    }
    public GipfChip removeChip(GipfCell c)
    {	GipfChip ch = c.removeTop();
    	if(c.onBoard)
    	{
    	switch(c.height())
    	{
    	case 0:
    		if(ch.potential==Potential.None) { currentSingleCount[playerIndex(ch)]--; }
    		break;
    	case 1:
    		{
    		GipfChip ch0 = c.chipAtIndex(0);
    		if(ch0.potential==Potential.None) { currentSingleCount[playerIndex(ch)]++; }
    		if(ch0==ch) { currentGipfCount[playerIndex(ch0)]--; }
    		}
    		break;
    	case 2:
    		{
    		GipfChip ch0 = c.chipAtIndex(0);
    		GipfChip ch1 = c.chipAtIndex(1);
     		if(ch0==ch1) { currentGipfCount[playerIndex(ch0)]++; }
    		}
    		break;
     	default:	// 3 or more
     		break;
    	}
    	}
    	return ch;
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
        for(int i=0;i<2;i++)
        {	for(Potential p : Potential.values())
        		{
        		GipfCell c = new GipfCell(r,PlayerReserve[i],p.ordinal());
        		c.potential = p;
        		rack[i][p.ordinal()]=c;
        		
        		}
        	captures[i]=new GipfCell(r,PlayerCaptures[i],nextPlayer[i]);
        	doublePiecesPlayed[i]=0;
        	singlePiecesPlayed[i]=0;
        	currentGipfCount[i]=0;
        }
        setColorMap(map, 2);
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
    
    private int playerIndex(GipfChip ch) { return(ch.color==playerColor[0] ? 0 : 1); }
    
    private void check_piece_count()
    {	int height=0;
    	int singles0 = 0;
    	int singles1 = 0;
    	int gipf0 = 0;
    	int gipf1 = 0;
    	for(GipfCell c=allCells; c!=null; c=c.next) 
    		{ int hh = c.height();
    		  height+= hh; 
    		  switch(hh)
    		  {
    		  case 0:
    		  default: 
    			  break;
    		  case 1:
    		  	{	
    		  		GipfChip top = c.topChip();
    		  		if(top.potential==Potential.None)
    		  		{
    		  			if(playerIndex(top)==0) { singles0++; } else { singles1++; }
    		  		}
    		  	}
    		  	break;
    		  case 2:
    		  	{ GipfChip ch0 = c.chipAtIndex(0);
    		  	  GipfChip ch1 = c.chipAtIndex(1);
    		  	  if(ch0==ch1)
    		  	  {
    		  		  if(playerIndex(ch0)==0) { gipf0++; } else { gipf1++; }
    		  	  }
    		  	}
    		  }
    		}
    	for(int player = 0;player<2; player++)
    	{	int sum = 0;
    		for(GipfCell c : rack[player])
    		{
    			sum += c.height();
    		}
    		height += sum;
    	}
    	int ch0 = captures[0].height();
    	int ch1 = captures[1].height();
    	height += ch0+ch1;
     	height += pickedHeight;
    	p1(initial_height==height,"piece counts match");
    	p1(gipf0==currentGipfCount[0],"gipf 0 matches");
    	p1(gipf1==currentGipfCount[1],"gipf 1 matches"); 
    	p1(singles0==currentSingleCount[0],"singles 0 matches");
    	p1(singles1==currentSingleCount[1],"singles 1 matches"); 
    }
    
    private void InitBallsAndBoard()
    {	int map[] = getColorMap();
        setState(GipfState.PUZZLE_STATE);
        initial_height=0;
        reInit(rack);
        reInit(captures);
        AR.setValue(doublePiecesPlayed,0);
        AR.setValue(currentGipfCount,0);
        AR.setValue(currentSingleCount,0);
        AR.setValue(singlePiecesPlayed,0);
        playerChip[map[0]] = standardChip[0];
        playerChip[map[1]] = standardChip[1];
        playerColor[map[0]] = GColor.W;
        playerColor[map[1]] = GColor.B;
        pickedHeight = 0;
        pickedObject=null;
        Potential monoculture[] = {null,null};	// hack to allow using only one type of potential
        if(variation==Variation.Gipf_Matrx)
        {
        for(int i=0;i<2;i++)
        	{
        	for(Potential p : Potential.values())
        		{
        		int n = p==Potential.None ? 3 : (monoculture[i]==null ? 6 : 30);
        		int norm = p.ordinal();
        		if(p==Potential.None || monoculture[i]==null || p==monoculture[i])
        		{
        		for(int j=0;j<n;j++) 
        			{ rack[i][norm].addChip(GipfChip.Chips[i][norm]); 
        			  initial_height++; 
        			}
        		}}
        	}
        }
        else
        {
        int norm = Potential.None.ordinal();
        for(int i=0;i<2;i++) 
        	{ 
        	int lim = (standard_setup|tournament_setup)
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
        	  while(rack[i][norm].chipIndex<lim)
        	  	{ rack[i][norm].addChip(standardChip[map[i]]); }
       	}
       if(!tournament_setup)
        {
        for(int i=0;i<startPos.length;i++)
        {	int []row=startPos[i];
        	char col = (char)row[0];
        	int ro = row[1];
        	int pl = row[2];
        	GipfCell c = getCell(col,ro);
        	addChip(c,rack[pl][norm].removeTop());
        	if(standard_setup) 
        		{ 
        		  addChip(c,rack[pl][norm].removeTop()); 
        		  doublePiecesPlayed[pl]++; 
        		  }
        	else { singlePiecesPlayed[pl]++; }
        }}}
        board_state = (firstPlayer >= 0) ? GipfState.DONE_STATE : GipfState.PUZZLE_STATE;
        TamskCenter= getCell('E',5);
        dropUndo.clear();        
        removalStack.reInit();
        removalCells.clear();
        whoseTurn = firstPlayer;
        pickedSource.clear();
        droppedDest.clear();
        tamskState = null;
        check_piece_count();
    }
    public Variation variation = Variation.Gipf;
    private void Init_Standard(Variation type)
    {
        gametype = type.name;
        variation = type;
        standard_setup = false;
        tournament_setup = false;
        switch(type)
        {
        default:
        	throw G.Error("Not expecting variation %s",type);
        case Gipf_Standard:	
        	standard_setup = true; 
        	break;
        case Gipf_Tournament:
        	tournament_setup = true; 
        	break;
        case Gipf_Matrx:
        case Gipf: 
        	break;
        }
        reInitBoard(variation.fcols,variation.ncols, null);
        
        preservationStack.clear();
        // remove edge-edge links
        CellStack ecells = new CellStack();
        for(GipfCell c =allCells; c!=null; c=c.next)
        {	if(c.isEdgeCell())
        	{for(int i=0;i<c.nAdjacentCells();i++)
        		{
        			GipfCell a = c.exitTo(i);
        		if((a!=null) && a.isEdgeCell()) { c.unCrossLinkTo(a); }
        		}
        	ecells.push(c);
        	c.centerRank = 0;
        	}
        	else { c.centerRank = -1; }
         }
        int n=0;
        while(setCenterRank(n)>0) { n++; }
        edgeCells = ecells.toArray();
        InitBallsAndBoard();
    }
    // centerRank is the distance from the edge
    private int setCenterRank(int target) 
    {	int n = 0;
    	for(GipfCell c = allCells; c!=null; c=c.next)
    	{	if(c.centerRank==target)
    		{
    			for(int dir=0;dir<c.geometry.n;dir++)
    			{
    				GipfCell adj = c.exitTo(dir);
    				if(adj!=null && adj.centerRank==-1 ) { adj.centerRank = target+1; n++; }
    			}
    		}
    	}
    	return n;
    }
    public void sameboard(BoardProtocol f) { sameboard((GipfBoard)f); }

    public void sameboard(GipfBoard from_b)
    {
        super.sameboard(from_b);

       // p1(GipfCell.sameCell(pickedSource,from_b.pickedSource),"source stack matches");
       // p1(cell.sameCell(droppedDest,from_b.droppedDest),"dest stack matches");
      //  p1(G.sameArrayContents(dropUndo,from_b.dropUndo),"undo matches");
        p1(AR.sameArrayContents(doublePiecesPlayed,from_b.doublePiecesPlayed),"gipf played count matches");
        p1(AR.sameArrayContents(currentGipfCount,from_b.currentGipfCount),"gipf count matches");
        p1(AR.sameArrayContents(singlePiecesPlayed,from_b.singlePiecesPlayed),"single played matches");
        p1(AR.sameArrayContents(currentSingleCount,from_b.currentSingleCount),"single count matches");
        p1(unresign==from_b.unresign,"unresign mismatch");
        p1(sameCells(captures,from_b.captures),"captures mismatch");
        p1(sameCells(rack,from_b.rack),"rack mismatch");        
        p1(pickedObject==from_b.pickedObject, "pickedObject matches");
        p1(tamskState==from_b.tamskState,"tamskState mismatch");
        p1(Digest()==from_b.Digest(), "Digest matches");
        
    }



    public long Digest()
    {
    	long v = 0;
        Random r = new Random(61 * 11465);
        //G.print();
        for(GipfCell c=allCells; c!=null; c=c.next)
        {	v ^= c.Digest(r);
        	//G.print(c," ",v);
        }
        //G.print("d1 ",v);
        v ^= Digest(r,rack);
        v ^= Digest(r,captures);
        v ^= Digest(r,currentGipfCount);
        v ^= Digest(r,currentSingleCount);
        // G.print("d3 ",v);
		v ^= chip.Digest(r,pickedObject);
        //G.print("d4 ",v);
		//v ^= cell.Digest(r,getSource());
        //G.print("d5 ",v);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		v ^= Digest(r,tamskState==null ? GipfState.PUZZLE_STATE : tamskState);
		v ^= Digest(r,ignorePlease);
        //G.print("d6 ",v);
        return (v);
    }
    // get a hash of moves destinations, used by the viewer
    // to know where to draw dots and also to determine what
    // is a legal move
    public Hashtable<GipfCell,GipfCell> getMoveDests()
    {	Hashtable<GipfCell,GipfCell> dd = new Hashtable<GipfCell,GipfCell>();
    
    	if(pickedObject!=null)
    	{
    	GipfCell src = pickedSource.top();    	
    	getDestsFrom(src,whoseTurn,dd,true);	
    	}
    	else
    	{
    	if(markForRemoval(whoseTurn))
    	{
    		for(GipfCell c = allCells; c!=null; c=c.next)
    		{
    			if(c.rowcode>0) { dd.put(c,c); }
    		}
    	}
    	}
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
    public boolean edgeCaptureMode()
    {
    	return board_state.edgeCaptureMode();
    }
    public void getDestsFrom(GipfCell src,int pl,Hashtable<GipfCell,GipfCell>d,boolean picked)
    {	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case DONE_STATE:
    	case RESIGN_STATE:
    	case PUZZLE_STATE: break;
    	case PLACE_TAMSK_FIRST_STATE:
    	case PLACE_TAMSK_LAST_STATE:
    	case MOVE_POTENTIAL_STATE:
    	case PLACE_OR_MOVE_POTENTIAL_STATE:
    		if(src!=null) { getPotentialDestsFrom(src,picked ? pickedObject : null,d,pl); }
    		else { 
    			getPotentialSources(d,pl);
    		}
    		break;
    	//$FALL-THROUGH$
    	case PLACE_STATE:
        case PRECAPTURE_OR_START_GIPF_STATE:    	
        case PRECAPTURE_OR_START_NORMAL_STATE:
    	case PLACE_GIPF_STATE:
    	case PLACE_POTENTIAL_STATE:
    		for(GipfCell c : edgeCells)
    		{ d.put(c, c);
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
    public boolean hasPlayablePieces(int who)
    {	if(rack[who][Potential.None.ordinal()].height()>0) { return true; }
    	if(variation==Variation.Gipf_Matrx)
    	{
    		for(GipfCell c : rack[who])
    		{
    			if(c.height()>=2) { return true; }
    		}
    	}
    	return false;
    }
  
    public boolean legalToHitBoard(GipfCell c,Hashtable<GipfCell,GipfCell>dests)
    {	if((pickedObject==null) && c==getDest()) { return true; }
    	if((pickedObject!=null) && (c==getSource())) { return true; }
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case PLACE_TAMSK_LAST_STATE:
    	case PLACE_TAMSK_FIRST_STATE:
    		if(pickedObject==null)
    		{
    			return c==TamskCenter;
    		}
    		else
    		{	return (c==TamskCenter)||c.isEdgeCell();
    			
    		}
    	case RESIGN_STATE:
    	case GAMEOVER_STATE: return(false);
    	
        case PRECAPTURE_OR_START_GIPF_STATE:
        case PRECAPTURE_OR_START_NORMAL_STATE:
        	if(c.isEdgeCell() 
        			&& (hasPlayablePieces(whoseTurn)
        				|| (pickedObject!=null)
        				|| capturesPending()))
        		{ return(true); }
			//$FALL-THROUGH$
		case PRECAPTURE_STATE:
		case MANDATORY_PRECAPTURE_STATE:
		case MANDATORY_CAPTURE_STATE:
    	case DESIGNATE_CAPTURE_STATE:
     	case DESIGNATE_CAPTURE_OR_DONE_STATE:
    		//p1(markForRemoval(whoseTurn),"must be capturing");
    		switch(c.rowcode)
    		{	case 0:	
    			default: 
    				return(c.isGipf() && dests.get(c)!=null);
    			case 1:
    			case 2:
    			case 4: return(true);
    		}
    		
    	case DONE_CAPTURE_STATE:
    		if((pickedObject==null)
    			&& (((c.isGipf())	&& (c.rowcode!=0))
    					|| (c==getDest())))
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
    			{ return(c==getDest()); }
    		return(false); 
    	case SLIDE_STATE:
    	case SLIDE_GIPF_STATE:
    		{	GipfCell src = (pickedObject!=null) 
    							?getSource()
    							:getDest();
    		    if(c==src) { return(true); }
    		    if(c==TamskCenter && stateStack.size()>0)
    		    {
    		    	switch(stateStack.top())
    		    	{
    		    	default: break;
    		    	case PLACE_TAMSK_FIRST_STATE:
    		    	case PLACE_TAMSK_LAST_STATE: return true;
    		    	}
    		    }
    			if(src!=null && c.isAdjacentTo(src)
    				&& c.emptyCellInDirection(findDirection(src.col,src.row,c.col,c.row))
    				) { return(true); }
    		}
			//$FALL-THROUGH$
		case PLACE_STATE:
     	case PLACE_GIPF_STATE:
    		return(c.isEdgeCell() && (c.height()==0));
		case PLACE_POTENTIAL_STATE:
		case PLACE_OR_MOVE_POTENTIAL_STATE:
			if(c.isEdgeCell()) { return pickedObject!=null && (c.height()==0); }
			//$FALL-THROUGH$
		case MOVE_POTENTIAL_STATE:		
			if(c==droppedDest.top()) { return true; }
			else { return (c==pickedSource.top()) 
							|| pickedObject==null 
								? hasPotentialMoves(c,whoseTurn)
								: dests.get(c)!=null;
			}
			// insist they picked up something
			
		case PUZZLE_STATE: 
    		GipfChip ch = c.topChip();
    		return((pickedObject==null)
    				?ch!=null
    				:ch!=null 
    					? (((c.height()+pickedHeight)<=2) 
    							&& (ch.color==pickedObject.color))
    					: true);
    	}
    }

    // legal to hit the chip reserve area
    public boolean legalToHitChips(GipfCell c)
    {	if((pickedObject==null)
    		&& (c==droppedDest.top())
    		&& ((c.rackLocation()==GipfId.First_Player_Captures)
    				|| (c.rackLocation()==GipfId.Second_Player_Captures)))
    		{
    		 return true;
    		}
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PLACE_TAMSK_FIRST_STATE:
        case PLACE_TAMSK_LAST_STATE:
        	if(pickedObject==null) { return false; }
        	else return c==captures[nextPlayer[whoseTurn]];
        case PLACE_OR_MOVE_POTENTIAL_STATE:
        case PLACE_POTENTIAL_STATE:
        	return pickedObject==null 
        		? c.rackLocation().color==playerColor[whoseTurn]
        			&& c.rackLocation().isReserve()
         			&& c.row!=Potential.None.ordinal() 
        			&& c.height()>=2
        		: c.row==pickedObject.potential.ordinal();
        case SLIDE_STATE:
        case SLIDE_GIPF_STATE:
        {	GipfId loc = c.rackLocation();
        	if((tamskState!=null)
        		&& (pickedObject!=null)
        		&& (loc==captures[nextPlayer[whoseTurn]].rackLocation()))
					{
        			return true;
					}
        	return((pickedObject!=null)
        			&& loc==rack[whoseTurn][0].rackLocation())
        			&& c.row==pickedObject.potential.ordinal();
        }
        case PRECAPTURE_OR_START_GIPF_STATE:
        case PRECAPTURE_OR_START_NORMAL_STATE:
        case PLACE_STATE: 
        case PLACE_GIPF_STATE:
        {	GipfChip ch = c.topChip();
        	return((ch!=null) 
        			&& (c.rackLocation().isReserve())
        			&& (c.row==Potential.None.ordinal() || c.height()==0)
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
		case MANDATORY_PRECAPTURE_STATE:
		case MANDATORY_CAPTURE_STATE:
		case MOVE_POTENTIAL_STATE:
			return(false);
        case PUZZLE_STATE:
        	return(pickedObject==null
        		?(c.chipIndex>=0)
        		: swappedColors() 
        			? pickedObject.color!=c.rackLocation().color 
        			: pickedObject.color==c.rackLocation().color);
        }
    }
    public GipfCell getCell(GipfCell c)
    {	if(c==null) { return(null); }
    	switch(c.rackLocation())
    	{	default: throw G.Error("not expecting %s",c);
    		case BoardLocation: return(getCell(c.col,c.row));
    		case First_Player_Reserve: return(rack[0][c.row]);
    		case Second_Player_Reserve: return(rack[1][c.row]);
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
        tamskState = from_b.tamskState;
        variation = from_b.variation;
        tournament_setup_done = from_b.tournament_setup_done;
        unresign = from_b.unresign;
        dropUndo.copyFrom(from_b.dropUndo);
        AR.copy(doublePiecesPlayed,from_b.doublePiecesPlayed);
        AR.copy(currentGipfCount,from_b.currentGipfCount);
        AR.copy(singlePiecesPlayed,from_b.singlePiecesPlayed);
        AR.copy(currentSingleCount,from_b.currentSingleCount);
        copyFrom(rack,from_b.rack);
        copyFrom(captures,from_b.captures);
        stateStack.copyFrom(from_b.stateStack);
        getCell(pickedSource,from_b.pickedSource);
        getCell(droppedDest,from_b.droppedDest);
        preservationStack.copyFrom(from_b.preservationStack);
        ignorePlease = from_b.ignorePlease;
         
        lastPlacedIndex = from_b.lastPlacedIndex;
        if(G.debug()) {  sameboard(from_b); }
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
    	Variation var = Variation.find(gtype);
    	
        if (var!=null)
        {
            Init_Standard(var);
        }
        else
        {
        	throw G.Error(WrongInitError,gtype);
        }
        tournament_setup_done = false;
        stateStack.clear();
        whoseTurn = 0;
        moveNumber = 1;
        lastPlacedIndex = -1;
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
        case MANDATORY_CAPTURE_STATE:
        case PLACE_TAMSK_LAST_STATE:
        case DRAW_STATE:
            moveNumber++;
            setWhoseTurn(nextPlayer[whoseTurn]);
            return;

        default:
        	throw G.Error("Move not complete");
        }
    }
    public boolean mandatoryDoneState()
    {
    	switch(board_state)
    	{
	    case DONE_CAPTURE_STATE:
	    case RESIGN_STATE:
	    case DRAW_STATE:
	    case DONE_STATE:	
	    	return true;
	    default: 
	    	return false;
    	}
    }
    public boolean DoneState()
    {	if(mandatoryDoneState()) { return true; }
    
        switch (board_state)
        {
	    case DESIGNATE_CAPTURE_OR_DONE_STATE:
	    	return true;
        case MANDATORY_CAPTURE_STATE:
        	return !allPreserved();
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

    public GipfCell pickSource(GipfId src,int row)
    {
    	switch(src)
    	{
    	default: throw G.Error("not expecting %s",src);
    	case First_Player_Reserve: return(rack[FIRST_PLAYER_INDEX][row]);
    	case Second_Player_Reserve: return(rack[SECOND_PLAYER_INDEX][row]);
    	case First_Player_Captures: return(captures[FIRST_PLAYER_INDEX]); 
    	case Second_Player_Captures: return(captures[SECOND_PLAYER_INDEX]); 
    	}
    }
    public void pickFromCell(GipfCell c)
    {	pickedSource.push(c);
    	pickedObject = c.removeTop();
    	pickedHeight++;
    }
    public void pickFromRack(GipfCell c)
    {	pickFromCell(c);
    	switch(board_state)
    	{
    	default: break;
    	case PLACE_POTENTIAL_STATE:
    	case PLACE_OR_MOVE_POTENTIAL_STATE:
    	case PLACE_GIPF_STATE:
    	case PRECAPTURE_OR_START_GIPF_STATE:
    		// pick a stack of 2
    		pickedHeight++;
    		c.removeTop();
    	}
    }
    public void dropOnRack(GipfCell c)
    {  	while(pickedHeight>0) { pickedHeight--; c.addChip(pickedObject);  }
    	dropUndo.push(removalCells.size()*100);
    	droppedDest.push(c);
    	pickedObject = null;
    }
    public void dropOnBoard(GipfCell dest)
    {	if(dest==null) 
    		{ p1("cant be null2");}
    	droppedDest.push(dest);
    	dropUndo.push(removalCells.size()*100);
    	dropOnBoardCell(dest);
    	
		dest.previousLastPlaced = dest.lastPlaced;
		dest.lastPlaced = lastPlacedIndex;
		lastPlacedIndex++;
		
    }
    public void dropOnBoardCell(GipfCell dest)
    {	p1(dest.onBoard,"is on board");
    	while(pickedHeight>0) 
    		{ pickedHeight--; 
    		  addChip(dest,pickedObject);
    		}
    	pickedObject = null;
    }
    public void pickFromBoard(GipfCell src,boolean single)
    {
    	pickedSource.push(src);
    	pickFromBoardCell(src,single);
    	
    }
    public void pickFromBoardCell(GipfCell src,boolean single)
    {	p1(src.onBoard,"is a board cell");
    	int n =single ? 1 : src.height();
    	pickedHeight=0;
    	while(pickedHeight<n)
    	{	pickedHeight++;
    		pickedObject = removeChip(src);   		
    	}
    	src.lastContents = src.previousLastContents;
		src.lastEmptied = src.previousLastEmptied;
		src.lastPlaced = src.previousLastPlaced;

    }
    public boolean isDest(char col,int row)
    {	GipfCell c = getCell(col,row);
    	return droppedDest.top()==c;
    }
    private void unDropObject()
    {	GipfCell dr = droppedDest.pop();
    	if(dr.onBoard)
    	{
    	int dundo = dropUndo.pop();
		int undo = dundo%PRESSTACK;
		int presinfo = dundo/PRESSTACK;
		int dist = undo%REMOVESTACK;
		int uncap = undo/REMOVESTACK;
		if(uncap<removalCells.size())
		{
			undoCaptures(uncap,whoseTurn);
		}
		if(dist>0) 
			{ GipfCell s = pickedSource.top();
			  undoSlide(dr,s,dist);
			  if(presinfo<preservationStack.size())  
			  	{ discardPreservation(preservationStack);
			  	  //restorePreservation(preservationStack); 
    			  	}
			  	pickFromBoardCell(s,false);
     			}
    		else
    		{	
    			pickFromBoardCell(dr,false);
     		}
    		dr.lastPlaced = dr.previousLastPlaced;
    		lastPlacedIndex--;
    	}
    	else
    	{	
    		pickFromRack(dr);
    		pickedSource.pop();
    	}
  
    }
    public GipfCell getSource()
    {
    	return(pickedSource.top());
    }
    public GipfCell getDest()
    {	return droppedDest.top();
    }
    
    public void unPickObject()
    {	GipfCell c = pickedSource.pop();
    	if(c!=null) 
    		{ if(c.onBoard) 
    			{ dropOnBoardCell(c); 
   				  c.lastEmptied = c.previousLastEmptied;
    			} 
    			else 
    			{ 	while(pickedHeight>0) { pickedHeight--; c.addChip(pickedObject);  }
    			}
    		}
    		else if(pickedObject!=null)
    		{ throw G.Error("picked object has no source"); 
    		}
    	pickedObject = null;
    }
    private void finalizePlacement()
    {  	droppedDest.clear();
    	pickedSource.clear();
    	dropUndo.clear();
    	pickedObject = null;
    	ignorePlease = 0;
    	stateStack.clear();
    }

     // move a stack, bottom first
    private void moveStack(GipfCell from,GipfCell to)
    {	GipfChip ch = from.removeTop();
    	from.lastEmptied = lastPlacedIndex;
    	if(from.chipIndex>=0) { moveStack(from,to); }
    	to.addChip(ch);


    	to.lastPlaced = lastPlacedIndex++;
    	from.preserved = false;
    }

    // move a stack, bottom first
   private void unMoveStack(GipfCell from,GipfCell to)
   {	GipfChip ch = from.removeTop();
   		from.lastEmptied = from.previousLastEmptied;
   		from.lastPlaced = from.previousLastPlaced; 	
   		lastPlacedIndex--;
   		if(from.chipIndex>=0) { unMoveStack(from,to); }
   		to.addChip(ch);
   		from.preserved = false;
   }
   
   	private int doSlide(GipfCell from,GipfCell to,int dir,replayMode replay,boolean animateFirst)
   	{     
   		if(from.isGipf()) { doublePiecesPlayed[whoseTurn]++;}
   			else { singlePiecesPlayed[whoseTurn]++; }
   		return doSlideInternal(from,to,dir,replay,animateFirst);
   	}
    // do the slide portion, return the number of stones moved
    private int doSlideInternal(GipfCell from,GipfCell to,int dir,replayMode replay,boolean animateFirst)
    {	int val = 1;
    	if(to.topChip()!=null) { val += doSlideInternal(to,to.exitTo(dir),dir,replay,true); }
    	//p1(!to.isEdgeCell(),"not edge1");
    	moveStack(from,to); 
    	if(replay.animate && animateFirst)
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
    private void clearMarks()
    {
    	for(GipfCell c =allCells; c!=null; c=c.next)
    	{	c.rowcode = 0;
    	}
    }
    // return true if something is available for removal
    private boolean markForRemoval(int pl)
    {	boolean val = false;
    	clearMarks();
    	switch(board_state)
    	{
    	case PLACE_TAMSK_FIRST_STATE:
    	case PLACE_TAMSK_LAST_STATE:
    		break;
    	default:
    	if(tamskState!=GipfState.PLACE_TAMSK_FIRST_STATE)
    	{
    	for(int dir = 0;dir<3;dir++)
    	{	int dircode = (1<<dir);
    	for(GipfCell c = allCells; c!=null; c=c.next)
    	{	
    		if(!c.isEdgeCell() && ((c.rowcode&dircode)==0))
    		{	boolean nr = c.rowOfNInDirection(4,playerChip[pl],dir);
    			if(nr)
    				{ c.markRow(dir,dircode);
    				  c.markRow(dir+3,dircode);
    				  val = true;
    				}
    		}
    	}
    	}}}
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
    {	boolean val = standard_setup || tournament_setup || (variation==Variation.Gipf_Matrx);
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
    	boolean stop = false;
    	while(c.chipIndex>=0 && !stop)
    	{	GipfChip top = removeChip(c);
    		removalStack.addChip(top);
    		removalCells.push(c);
    		pl = playerIndex(top);
    		GipfCell dest = (pl==whoseTurn) ? rack[whoseTurn][top.potential.ordinal()]: captures[whoseTurn];
    		dest.addChip(top);
    		c.lastContents = top;
    		c.previousLastCaptured = c.lastCaptured;
    		c.lastCaptured = lastPlacedIndex;
    		lastPlacedIndex++;
    		if(replay.animate)
    		{
    			animationStack.push(c);
    			animationStack.push(dest);
    		}
    		switch(top.potential)
    		{
    		case Dvonn:
    		case Punct:
    			// capturing the a dvonn or punct stack only captures the top
    			stop = (c.topChip()!=top);
    			break;
    		default: 
    			break;
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
    	{	
    		p1((board_state==GipfState.DESIGNATE_CAPTURE_OR_DONE_STATE)
    					|| (board_state==GipfState.MANDATORY_CAPTURE_STATE)
    					|| (board_state==GipfState.PRECAPTURE_OR_START_NORMAL_STATE)
    					|| (board_state==GipfState.PRECAPTURE_OR_START_GIPF_STATE)
    					|| simpleRemoval(),
    					"must be simple %s",board_state);
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
    			&& (doublePiecesPlayed[whoseTurn]>0)
    			&& ((singlePiecesPlayed[0]==0)||(singlePiecesPlayed[1]==0)));	
    }
    public boolean canTogglePlacementMode(int who)
    {	return(	(tournament_setup && !tournament_setup_done)
    			&& ((board_state==GipfState.PLACE_STATE) || (board_state==GipfState.PLACE_GIPF_STATE)||(board_state==GipfState.PRECAPTURE_OR_START_GIPF_STATE))
    			&& (who==whoseTurn)
    			&& (doublePiecesPlayed[whoseTurn]>0)
    			&& (singlePiecesPlayed[who]==0));	
    }
    private boolean placingGipfPieces()
    {
    	return((tournament_setup && !tournament_setup_done) 
		&& (singlePiecesPlayed[whoseTurn]==0));	
    }
    public boolean placingGipfPieces(int who)
    {
    	return((tournament_setup && !tournament_setup_done) 
    			&& (singlePiecesPlayed[who]==0));	
    }
    private boolean hasPlayablePotentials(int who)
    {
    	for(GipfCell c : rack[who])
    	{
    		if((c.row!=Potential.None.ordinal()) && c.height()>=2) { return true; }
    	}
    	return false;
    }
    private boolean enterTamskState()
    {
    	if(TamskCenter.isGipf())
		{
		GipfChip top = TamskCenter.topChip();
		return ((top.potential==Potential.Tamsk)
				&& (top.color==playerColor[whoseTurn]));
		}
		return false;
    }
    // set the first state of a new turn
    private void setFirstState()
    {	setState(GipfState.PUZZLE_STATE);
    	if(enterTamskState())
    	{	clearMarks();
    		setState(GipfState.PLACE_TAMSK_FIRST_STATE);
    	}
    	else 
    	{
    	setState(GipfState.PUZZLE_STATE);
    	{ if(markForRemoval(whoseTurn))
	    	{
 	    	setState(markForPreservation(whoseTurn)
	    					? (placingGipfPieces()
	    							?GipfState.PRECAPTURE_OR_START_GIPF_STATE
	    							: ((variation==Variation.Gipf_Matrx) && allPreserved())
	    								? GipfState.MANDATORY_PRECAPTURE_STATE
	    								: GipfState.PRECAPTURE_OR_START_NORMAL_STATE)
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
        		&& (rack[whoseTurn][Potential.None.ordinal()].chipIndex<0))
        {	
        	if(variation==Variation.Gipf_Matrx)
        	{	boolean potentialMoves = hasPotentialMoves(whoseTurn);
        		if(currentSingleCount[whoseTurn]==0) { setGameOver(false,true); }	// all single gipfs captured
        		else if(hasPlayablePotentials(whoseTurn)) 
        			{ setState(potentialMoves ? GipfState.PLACE_OR_MOVE_POTENTIAL_STATE : GipfState.PLACE_POTENTIAL_STATE); 
        			}
        		else if(potentialMoves) { setState(GipfState.MOVE_POTENTIAL_STATE ); }
        		else { setGameOver(false,true); }
        	}
        	else
        	{
        	// out of moves
        	setGameOver(false,true);
        	}
        }
        if((standard_setup || tournament_setup))
        	{if((doublePiecesPlayed[whoseTurn]>0)&&(currentGipfCount[whoseTurn]==0))
        		{ setGameOver(false,true); }
        	 int next = nextPlayer[whoseTurn];
        	 if((doublePiecesPlayed[next]>0)&&(currentGipfCount[next]==0)) 
        	 	{ setGameOver(true,false); }
        	}}}
    }
    // set the state appropriately after a slide move
    private void setLastState()
    {
    	if(enterTamskState())	// a tamsk potential is now on the center
    	{	clearMarks();
    		if(tamskState!=GipfState.PLACE_TAMSK_FIRST_STATE)
    		{
    		setState(GipfState.PLACE_TAMSK_LAST_STATE);
    		}
    	}
    	else if(tamskState==GipfState.PLACE_TAMSK_FIRST_STATE)
    	{	tamskState = null;
    		setFirstState();
    	}
    	else 
    	{ setState(GipfState.PUZZLE_STATE);
    	  if(markForRemoval(whoseTurn))
			{ markForPreservation(whoseTurn);
    		  if((variation==Variation.Gipf_Matrx) && allPreserved())
    		  {
    			  setState(GipfState.MANDATORY_CAPTURE_STATE);
    		  }
    		  else
    		  {
	    		setState(markForPreservation(whoseTurn)
	    				? GipfState.DESIGNATE_CAPTURE_OR_DONE_STATE
	    				: (simpleRemoval()?GipfState.DONE_CAPTURE_STATE:GipfState.DESIGNATE_CAPTURE_STATE));
    		  }
    		}
			else
			{ 
			  setState(GipfState.DONE_STATE);
			}}
    }
    public void clearAllPreserved()
    {
    	for(GipfCell c = allCells; c!=null; c=c.next) { c.preserved = false; }
    }
    public void doDone(replayMode replay)
    {	
    	finalizePlacement();
    	tamskState = null;
    	lastPlacedIndex++;
    	tournament_setup_done = (singlePiecesPlayed[0]>0) && (singlePiecesPlayed[1]>0);
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
        	case MANDATORY_CAPTURE_STATE:
        	case PLACE_TAMSK_LAST_STATE:
        	case DONE_CAPTURE_STATE: 
        		doCaptures(replay);
        		if((variation==Variation.Gipf_Matrx)
        				&& markForRemoval(whoseTurn))
        			{
         			markForPreservation(whoseTurn);
        			if(allPreserved())
        				{
        				setState(GipfState.MANDATORY_CAPTURE_STATE);
        				break;
        				}
        			}
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
    private void moveToRack(GipfCell c)
    {
    	dropOnRack(c);
    	switch(board_state)
    	{
    	case PLACE_TAMSK_LAST_STATE:
    		setLastState();
    		break;
    	case PLACE_TAMSK_FIRST_STATE:
    		tamskState=null;
    		setFirstState();
    		break;
    	case SLIDE_STATE:	 
        	finalizePlacement();
        	setState(GipfState.PLACE_STATE); break;
    	case SLIDE_GIPF_STATE: 
        	finalizePlacement();
        	setState(GipfState.PLACE_GIPF_STATE); break;
    	case PUZZLE_STATE: 
    	default: 
        	finalizePlacement();
        	break;
    	}
    }
    private void doSlideFrom(Gipfmovespec m,replayMode replay)
    {
    	int psize = preservationStack.size();        		
		GipfCell dest = getCell(m.to_col,m.to_row);
		GipfCell src = getCell(m.from_col,m.from_row);
		// the forever counts are only changed by the slide
		
		int distance = doSlide(src,dest,
				findDirection(src.col,src.row,dest.col,dest.row),replay,true);
		m.undoinfo = psize*PRESSTACK+removalCells.size()*REMOVESTACK+distance;
		setLastState();	// and mark for preservation
    }
    private void doPlacement(Gipfmovespec m,GipfCell dest,replayMode replay)
    {
		boolean animateFirst = pickedObject==null;
		if(animateFirst) { pickFromRack(rack[whoseTurn][m.from_potential.ordinal()]); }
		if(replay.animate && animateFirst)
		{	if(pickedHeight==2)
			{
			animationStack.push(getSource());
			animationStack.push(dest);
			}
			animationStack.push(getSource());
			animationStack.push(dest);
		}
		stateStack.push(board_state);
		m.from_potential = pickedObject.potential;
		dropOnBoard(dest);
		if(dest.isEdgeCell())
		{
		setState((dest.height()==2)?GipfState.SLIDE_GIPF_STATE:GipfState.SLIDE_STATE); 
		}
		else 
		{
			setLastState();
		}	
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Gipfmovespec m = (Gipfmovespec)mm;
        //G.print("E "+mm+ " " + moveNumber+" "+board_state+" "+removalCells.size());
        //check_piece_count();
        switch (m.op)
        {
        case MOVE_DONE:
        	doDone(replay);
            break;
 
        case MOVE_START:
        	finalizePlacement();
        	whoseTurn = m.player;
        	setFirstState();
            break;
        case MOVE_EDIT:
        	setState(GipfState.PUZZLE_STATE);
        	finalizePlacement();
        	break;
        case MOVE_PICK:
        	{
        	GipfCell c = pickSource(m.source,m.from_row);
        	if(c==getDest())
	        	{	setState(stateStack.pop());	// pop first so we'll guess correctly how many to pick
	        		unDropObject();
	        		
	        	}
        	else 
	        	{	stateStack.push(board_state);
	        		pickFromRack(c);
	        	}
        	}
        	break;
        case MOVE_DROP:
        	{
        	GipfCell c = pickSource(m.source,m.from_row);
        	if(c==getSource())
	        	{
	        	unPickObject();	
	        	setState(stateStack.pop());
	        	}
        	else
	        	{
	        	switch(board_state)
	        	{
	        	default: break;
	        	case SLIDE_STATE:
	        	case SLIDE_GIPF_STATE:
	        		// we dropped on the edge but balked
	        		setState(stateStack.pop());
	        		break;
	        	}
	        	stateStack.push(board_state); 
	        	moveToRack(c);
	        	}
        	}
	        break;
 		case MOVE_PSLIDE:
        case MOVE_SLIDE:
        	{ GipfCell src = getCell(m.from_col,m.from_row);
        	  int index = m.from_potential.ordinal();
       		  GipfCell from = rack[whoseTurn][index];
    		  GipfChip pick = removeChip(from);
    		  addChip(src,pick);
    		  switch(board_state)
	    		{
	    		case PLACE_GIPF_STATE:
	    		case PLACE_POTENTIAL_STATE:
	    		case PLACE_OR_MOVE_POTENTIAL_STATE:
	    			{	
	    				addChip(src,removeChip(from));
	     			}
	    			break;
	    		default:
	    			}
	        	}
			//$FALL-THROUGH$
			case MOVE_SLIDEFROM:
        	doSlideFrom(m,replay);
        		break;
			case MOVE_TAMSK:
	  		   // tamsk slide
				{
				GipfCell from = getCell(m.from_col,m.from_row);
				pickFromBoard(TamskCenter,true);
				dropOnBoard(from);
				doSlideFrom(m,replay);
				}
				break;

	       case MOVE_PDROPB:
	       case MOVE_DROPB:
        	{
        	GipfCell dest = getCell(m.to_col,m.to_row);
        	switch(board_state)
        	{
        	default: throw G.Error("Not expecting state %s",board_state);
        	case SLIDE_GIPF_STATE:
         	case SLIDE_STATE:
        		if(dest.isEdgeCell()) 
        			{ if(pickedObject==null) 
        				{unDropObject();  
        				 m.from_potential = pickedObject.potential;
        				 dropOnBoard(dest);  
        				 // this can be the same cell in some old game records
        				 // this is a hack to ignore it if it is
        				 ignorePlease++;
        				 } 
        				else
        					{ 
        					// picked a new edge cell to start from
        					droppedDest.pop();
        					m.from_potential = pickedObject.potential;
         					dropOnBoard(dest); 
        					setState((dest.height()==2)?GipfState.SLIDE_GIPF_STATE:GipfState.SLIDE_STATE); 
        					}
        			}
        			else if (dest==TamskCenter)
        			{	droppedDest.pop();
        				m.from_potential=null;
        				pickedSource.pop();	// we dropped on an edge cell, then completed the undo
        				unPickObject();
        				setState(stateStack.pop());
        			}
        			else 
        			{ int psize =  preservationStack.size();
    				  m.from_potential=null;
        			  boolean animateFirst = pickedObject==null; 
        			  if(animateFirst) { pickFromBoard(droppedDest.top(),false); }
        			  GipfCell from = pickedSource.top();
        			  unPickObject(); 
         			  int cap = removalCells.size()*REMOVESTACK;
         			  if(replay.animate && animateFirst)
         			  {
         			  animationStack.push(from);
         			  animationStack.push(dest);
         			  }
        			  dropUndo.push( m.undoinfo = psize*PRESSTACK+cap+doSlide(from,dest,
        					  findDirection(from.col,from.row,dest.col,dest.row),replay,animateFirst)); 
        			  droppedDest.push(dest);
        			  pickedSource.push(from);
        			  stateStack.push(board_state);
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
        		{
        		if(pickedObject!=null) { unPickObject(); }
        		dropUndo.push(preservationStack.size()*PRESSTACK+removalCells.size()*REMOVESTACK);
        		pickedSource.push(getCell(m.from_col,m.from_row));
        		droppedDest.push(dest);
        		doCaptures(replay);		// might capture some gipf pieces
        		if((variation==Variation.Gipf_Matrx) && markForRemoval(whoseTurn))
        		{	// in the matrx game, all rows of 4 have to be resolved
        			setFirstState();
        			break;
        		}
        		doPlacement(m,dest,replay);
        		}
        		break;
				//$FALL-THROUGH$
			case PLACE_STATE: 
			case PLACE_POTENTIAL_STATE:
			case PLACE_GIPF_STATE:
			case PLACE_OR_MOVE_POTENTIAL_STATE:
    		case MOVE_POTENTIAL_STATE:
			case PLACE_TAMSK_LAST_STATE:
			case PLACE_TAMSK_FIRST_STATE:
        		{
        		if(dest==getSource())
        		{
        			unPickObject();
        		}
        		else
        		{
        		doPlacement(m,dest,replay);
        		}
        		}
        		break;
        	}}
        	break;
        case MOVE_PICKB:
        	{
        	GipfCell c = getCell(m.from_col,m.from_row);
        	switch(board_state)
        	{
        	default: throw G.Error("Not expecting state %s",board_state);
        	case PLACE_OR_MOVE_POTENTIAL_STATE:
        	case PLACE_TAMSK_FIRST_STATE:
        	case PLACE_TAMSK_LAST_STATE:  
        	case MOVE_POTENTIAL_STATE:
        	case PRECAPTURE_STATE:
        		if(c==getDest()) { 
        			unDropObject();
        			setState(stateStack.pop());
        		}
        		else
        		{
        		stateStack.push(board_state);
        		pickFromBoard(c,true);
        		}
        		break;
        	case DONE_STATE:
        	case DRAW_STATE:
        	case DONE_CAPTURE_STATE:
        	case DESIGNATE_CAPTURE_OR_DONE_STATE:
        	case DESIGNATE_CAPTURE_STATE:
        		p1(c==droppedDest.top(),"is dest");
        		unDropObject();
        		setState(stateStack.pop());
        		break;
        	case SLIDE_GIPF_STATE:
         	case PLACE_STATE:	// unusual, piece already placed on the edge
        	case SLIDE_STATE:	
        		pickFromBoard(c,false);
        		// and do not unset the state
        		// setState(stateStack.pop());
        		break;
           	case PUZZLE_STATE: 
           		stateStack.push(board_state);
        		pickFromBoard(c,false);
        		break;
        	}}
        	break;
        case MOVE_RESIGN:
        	if(pickedObject!=null) { unPickObject(); }
        	setState((unresign==null) ? GipfState.RESIGN_STATE : unresign);
            break;
        case MOVE_STANDARD:
        	{
        	p1(canTogglePlacementMode(),"shouldn't try to switch modes here");
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
        	case 0:
        	case -1:
        		break;
        	case 7:
        	case 3:
        	case 1: 	direction = 0; break;
        	case 6:
        	case 2:		direction = 1; break;
        	case 5:
        	case 4: 	direction = 2; break;
        	}
        	dropUndo.push(removalCells.size()*100);
        	doRowCaptures(c.exitTo(direction),direction,replay);
        	doRowCaptures(c,direction+3,replay);
        	{	boolean mark = markForRemoval(whoseTurn);
        		if((board_state==GipfState.PRECAPTURE_STATE)
        				||(board_state==GipfState.MANDATORY_PRECAPTURE_STATE)
        				||(board_state==GipfState.PRECAPTURE_OR_START_GIPF_STATE)
        				||(board_state==GipfState.PRECAPTURE_OR_START_NORMAL_STATE))
        			{	
        			if(variation!=Variation.Gipf_Matrx)
        			{
	        			if(mark)
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
        			{
    					clearAllPreserved();
    					if((variation!=Variation.Gipf_Matrx) && (revision<102))
    					{
    					// not completely correct - if you captured the last gipf piece
    					// you win immediately.  Almost the same, but you actually might
    					// be unable to make a normal move at this point.
    					setState(GipfState.PLACE_STATE);	
    					}
    					else
    					{
    					setFirstState();
    					}
    				}
        			}
        			else 
        			{ // post captures
        				if(mark)
        				{
        				   setState(allPreserved()
							? GipfState.DESIGNATE_CAPTURE_OR_DONE_STATE
							: simpleRemoval()
								? GipfState.DONE_CAPTURE_STATE
								: GipfState.DESIGNATE_CAPTURE_STATE);
        				}
        				else 
        					{ setState(GipfState.DONE_CAPTURE_STATE);
        					}
        			}
        	}}
        	break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
		case MOVE_PUNCT:
		case MOVE_DVONN:
		case MOVE_YINSH:
		case MOVE_ZERTZ:
			{
			GipfCell from = getCell(m.from_col,m.from_row);
			GipfCell to = getCell(m.to_col,m.to_row);
			p1(from.isGipf(),"should be a gipf");
			m.undoinfo = removalCells.size()*REMOVESTACK+0;
			pickFromBoard(from,true);
			dropOnBoard(to);
			if(replay.animate) 
				{
				animationStack.push(from);
				animationStack.push(to);
				}	
			setLastState();
			}
			break;
        default:
        	cantExecute(m);
        }
       //G.print("X "+mm+" "+tournament_setup_done+" "+singlePiecesPlayed[0]+" "+singlePiecesPlayed[1]);
         
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
   {    //G.print("R "+m+" "+moveNumber);
   		robotStateStack.push(tamskState);
   		robotStateStack.push(board_state); //record the starting state. The most reliable
       // to undo state transistions is to simple put the original state back.
       savePreservation(robotPreservationStack);
       robotPreservationStack.push(preservationStack.size());
       p1(m.player == whoseTurn, "whoseturn doesn't agree");
       if (Execute(m,replayMode.Replay))
       {
           if (m.op == MOVE_DONE)
           {
           }
           else if (mandatoryDoneState())
           { //G.print("autodone "+board_state+" "+whoseTurn);
             doDone(replayMode.Replay);
             //G.print("after auto "+board_state+" "+whoseTurn);
           }           
       }
   }

private void undoCaptures(int uncap,int who)
{
	while(removalCells.size()>uncap)
   		{	GipfCell c = removalCells.pop();
   			GipfChip ch = removalStack.removeTop();
   			int index = playerIndex(ch);
   			if(index==who)
   			{	rack[who][ch.potential.ordinal()].removeTop();
   			}
   			else 
   			{	captures[who].removeTop();
   			}
   			addChip(c,ch);
   			c.lastCaptured = c.previousLastCaptured;
   		}	
}
	private void undoSlide(GipfCell to,GipfCell from,int distance)
	{ 	int dir = findDirection(from.col,from.row,to.col,to.row);
		if(to.isGipf()) { doublePiecesPlayed[whoseTurn]--; }
			else { singlePiecesPlayed[whoseTurn]--;}
		while(distance>0)
  	   		{	distance--;
  	   			unMoveStack(to,from);
   	   			from = to;
  	   			to = to.exitTo(dir);
  	   		}
		// the forever counts are only changed by a slide move
		}
	
	private void undoDrop(GipfCell from)
	{	
		while(from.chipIndex>=0) 
			{ 	
			GipfChip ch = removeChip(from);
			rack[playerIndex(ch)][ch.potential.ordinal()].addChip(ch);
			}
	}
  //
   // un-execute a move.  The move should only be unexecuted
   // in proper sequence.  This only needs to handle the moves
   // that the robot might actually make.
   //
   public void UnExecute(Gipfmovespec m)
   {	int who = m.player;
    	   //check_piece_count();
   	   setState(robotStateStack.pop());
   	   tamskState = robotStateStack.pop();
       if(whoseTurn!=who)
       {	moveNumber--;
       	setWhoseTurn(who);
       }
       //G.print("U "+m+" "+moveNumber);
      
   	   boolean single = false;
       switch (m.op)
       {
       case MOVE_DROPB:
    	   {	
    	   GipfCell from = getCell(m.from_col,m.from_row);
    	   undoDrop(from);
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
  		 
  	   case MOVE_DVONN:
  	   case MOVE_PUNCT:
  		   single = true;
  		   //$FALL-THROUGH$
  	   case MOVE_YINSH:
  	   case MOVE_ZERTZ:
  	   		{
  	   		int uinfo = m.undoinfo%PRESSTACK;
  	   		int uncap = uinfo/REMOVESTACK;
   	   		undoCaptures(uncap,who);
 	   		GipfCell from = getCell(m.from_col,m.from_row);
 	   		GipfCell to = getCell(m.to_col,m.to_row);
 	   		pickFromBoardCell(to,single);
 	   		dropOnBoardCell(from);
  	   		}
  		   break;
  	   case MOVE_TAMSK:
  	   case MOVE_SLIDEFROM:
  	   case MOVE_PSLIDE:
  	   case MOVE_SLIDE:
  	   	{	int uinfo = m.undoinfo%PRESSTACK;
  	   		int distance = uinfo%REMOVESTACK;
  	   		int uncap = uinfo/REMOVESTACK;
   	   		undoCaptures(uncap,who);
  	   		
  	   		GipfCell from = getCell(m.from_col,m.from_row);
  	   		GipfCell to = getCell(m.to_col,m.to_row);
  	   		undoSlide(to,from,distance);

  	   		switch(m.op)
  	   		{
  	   		case MOVE_SLIDEFROM:
  	   			break;
  	   		case MOVE_SLIDE:
  	   		case MOVE_PSLIDE:
  	   			undoDrop(from);
  	   			break;
  	   		case MOVE_TAMSK:
  	   			pickFromBoard(from,true);
  	   			dropOnBoard(TamskCenter);
  	   			break;
  	   		default:
  	   			G.Error("Not expecting %s",m.op);
   	   		}
  	   	}
  	   		break;
       case MOVE_DONE:
       case MOVE_STANDARD:	// change in state only, so nothing else to do
       case MOVE_RESIGN:
           break;
       }

       //check_piece_count();
       tournament_setup_done = (singlePiecesPlayed[0]>0) && (singlePiecesPlayed[1]>0);
       int ps = robotPreservationStack.pop();
       while(preservationStack.size()>ps) { preservationStack.pop(); }
       restorePreservation(robotPreservationStack);
}   

   // look for a win for player.  
   public double ScoreForPlayer(int player,boolean print,boolean dumbot,boolean testbot)
   {  	GipfCell normal = rack[player][Potential.None.ordinal()];
		boolean matrx = variation==Variation.Gipf_Matrx;
	    double finalv= captures[player].chipIndex*20;
	    GColor myColor = playerColor[player];
	    int gipfs = normal.height();
	    int centerRank = 0;
	    if(matrx)
	    {
		    int reserves = 0;
	    	for(GipfCell r : rack[player])
	    	{
	    		if(r.potential!=Potential.None)
	    		{
	    			int n = r.height()/2;	// round down
	    			reserves += n;
	    		}
	    	}
	    	finalv += reserves*5;
	    }
	    else
	    {
	    	finalv += gipfs*5+currentGipfCount[player]*40;
	    }
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
   				{	GipfChip ctop = c.topChip();
   					for(int dir=0;dir<3;dir++)
   					{
   					GipfCell left = c.exitTo(dir);
   					GipfChip ltop = left.topChip();
   					if(ltop!=null)
   					{
	   				GipfCell right = c.exitTo(dir);
   					GipfChip rtop = right.topChip();
   					if((rtop!=null) && (ltop.color==myColor) && (rtop.color==myColor))
   					{	forks++;
   						if((ctop!=null) && (ctop.color==myColor)) { threes++; } 
   					}
   					}
   					}
   				}
   			}
   		
   		finalv += 2*forks + 3*threes;
   		 
   		}
   		if(matrx)
		{
		for(GipfCell c = allCells;
   				c!=null;
   				c=c.next)
   			{
   				GipfChip ctop = c.topChip();
   				if(ctop!=null && ctop.potential==Potential.None && ctop.color==myColor) 
						{ gipfs++;
						  centerRank += c.centerRank;
						}
   			}
		finalv += gipfs*10;
		// penalty for gipfs toward the center
		finalv -= centerRank*2;
 		}
   		if(print) 
   			{ System.out.println("sum "+finalv); }
   		return(finalv);
   }
   public String playerState(int pl)
   {	return(""+(rack[pl][Potential.None.ordinal()].chipIndex+1)+"x"+(captures[pl].chipIndex+1));
   }
   
   private void addSlideMoves(CommonMoveStack all,GipfCell c,int player,int op)
   {
	   for(int dir = 0;dir<CELL_FULL_TURN; dir++)
  		{	GipfCell d = c.exitTo(dir);
  			if((d!=null)
  				&& ((d.topChip()==null)
  					? (d.sweep_counter!=sweep_counter)
  					: d.emptyCellInDirection(dir)))
   				{ // sweep counter avoids effectively duplicate moves
  				  // where you enter an empty cell from either of 2 edges
  				  d.sweep_counter = sweep_counter;
  				  Gipfmovespec mm = new Gipfmovespec(op,c,d,player);
  				  all.addElement(mm);
  		}   	}
   }
   public boolean hasPotentialMoves(int who)
   {
	   if((variation==Variation.Gipf_Matrx) && currentGipfCount[who]>0)
		{ 
		   return addPotentialMoves(null,who);
		}
	   return false;
   }
   /**
    * zertz potentials jump over at least one piece and end on the first empty space in a line
    * 
    * @param all
    * @param from
    * @param who
    * @return
    */
   public boolean addZertzMoves(CommonMoveStack all,GipfCell from,int who)
   {	boolean some = false;
	   	for(int direction = 0; direction<from.geometry.n && (all!=null || !some); direction++)
	   	{	GipfCell adj = from;
	   		int steps = 0;
	   		while( ((adj=adj.exitTo(direction))!=null)
	   				&& !adj.isEdgeCell() 
	   				&& adj.topChip()!=null)
	   		{	steps++;
	   		}
	   		if(steps>0 && adj!=null && !adj.isEdgeCell())
	   		{	some = true;
	   			if(all!=null) { all.push(new Gipfmovespec(MOVE_ZERTZ,from,adj,who)); }
	   		}
	   	}
   		return some;
   }
   /**
    * yinsh potentials deploy across vacant spaces
    * 
    * @param all
    * @param from
    * @param who
    * @return
    */
   public boolean addYinshMoves(CommonMoveStack all,GipfCell from,int who)
   {	boolean some = false;
	   	for(int direction = 0; direction<from.geometry.n && (all!=null || !some); direction++)
	   	{	GipfCell adj = from;
	   		while((adj=adj.exitTo(direction))!=null && !adj.isEdgeCell() && adj.topChip()==null)
	   		{	some = true;
	   			if(all!=null) { all.push(new Gipfmovespec(MOVE_YINSH,from,adj,who)); }
	   		}
	   	}
   		return some;
   }
   
   public boolean addPunctDvonnMoves(CommonMoveStack all,GipfCell from,int who)
   {	boolean some = false;
   		GipfChip top = from.topChip();
	   	for(int direction = 0; direction<from.geometry.n && (all!=null || !some); direction++)
	   	{	GipfCell adj = from;
	   		while((adj=adj.exitTo(direction))!=null && !adj.isEdgeCell())
	   		{
	   			GipfChip atop = adj.topChip();
	   			if(atop!=null)
	   			{
	   				if((atop.potential==top.potential) && (atop.color!=top.color))
	   				{
	   				some = true;
	   				if(all!=null)
	   				{	// same type
	   					all.push(new Gipfmovespec(atop.potential==Potential.Dvonn ? MOVE_DVONN : MOVE_PUNCT,from,adj,who)); }
	   				}
	   			break;
	   			}
	   		}
	   	}
   		return some;
   }
   private boolean addTamskMoves(CommonMoveStack all,int who)
   {
	   if(all==null) { return true; }
	   if(TamskCenter.isGipf())
	   {	GipfChip ch = TamskCenter.topChip();
	   		if ((ch.potential==Potential.Tamsk)
			   && (ch.color==playerColor[who]))
	   		{	sweep_counter++;
	   			for(GipfCell c : edgeCells)
	   			{
	   				addSlideMoves(all,c,whoseTurn,MOVE_TAMSK);
	   			}
	   		}
	   		return true;
	   }
	   return false;
   }
   
   public boolean addPotentialMoves(CommonMoveStack all,int who)
   {
		
		boolean some = false;
		for(GipfCell c=allCells; c!=null && (all!=null || !some); c=c.next)
		   {	some |= addPotentialMovesFrom(all, c,null,who);
		   }
		return some;
   }
   public boolean addPotentialSlideMoves(CommonMoveStack all,int who)
   {   boolean some = false;
	   for(GipfCell c : rack[whoseTurn])
	   {
		   if((c.potential!=Potential.None)
				   && c.height()>=2)
		   { if(all==null) { return true; }
			 some |= addPotentialSlideMovesFrom(all,c,who);  
		   }
	   }
	   return some;
   }
   private boolean addPotentialSlideMovesFrom(CommonMoveStack all,GipfCell from,int who)
   {
	   boolean some = false;
	   sweep_counter++;
	   for(GipfCell c : edgeCells)
	   {
		   some |= addPotentialSlideMovesFrom(all,from,c,who);
	   }
	   return some;
   }
   private boolean addPotentialSlideMovesFrom(CommonMoveStack all,GipfCell potential,GipfCell c,int who)
   {   boolean some = false;
	   for(int dir = 0;dir<CELL_FULL_TURN && (all!=null || !some); dir++)
  		{	GipfCell d = c.exitTo(dir);
  			if((d!=null)
  				&& ((d.topChip()==null)
  						? (d.sweep_counter!=sweep_counter)
  						: d.emptyCellInDirection(dir)))
  				{ // sweep counter avoids effectively duplicate moves
				  // where you enter an empty cell from either of 2 edges
  				  d.sweep_counter = sweep_counter;
  				  if(all!=null)
  				  {
  				  Gipfmovespec mm = new Gipfmovespec(potential.potential,c,d,who);
  				  all.addElement(mm);
  				  }
  				  some = true;
  				}
  		}
	   return some;
  	}
   public void getPotentialDestsFrom(GipfCell src,GipfChip chip,Hashtable<GipfCell,GipfCell>d,int who)
   {
	   CommonMoveStack all = new CommonMoveStack();
	   addPotentialMovesFrom(all,src,chip,who);
	   for(int lim=all.size()-1; lim>=0; lim--)
	   {
		   Gipfmovespec m = (Gipfmovespec)all.elementAt(lim);
		   switch(m.op)
		   {
		   default: throw G.Error("not expecing op %s",m.op);
		   case MOVE_ZERTZ:
		   case MOVE_PUNCT:
		   case MOVE_TAMSK:
		   case MOVE_DVONN:
		   case MOVE_YINSH:
		   	{
			   GipfCell e = getCell(m.to_col,m.to_row);
			   d.put(e,e);
		   	}
		   }
	   }
   }
   public void getPotentialSources(Hashtable<GipfCell,GipfCell>d,int who)
   {
	   for(GipfCell c = allCells;c!=null; c=c.next)
	   {
		   if(addPotentialMovesFrom(null,c,null,who))
		   {
			   d.put(c,c);
		   }
	   }
   }
   public boolean hasPotentialMoves(GipfCell c,int who)
   {
	   return addPotentialMovesFrom(null,c,null,who);
   }
   public boolean addPotentialMovesFrom(CommonMoveStack all,GipfCell src,GipfChip chip,int who)
   {	boolean some = false;
   		GipfChip top = chip==null ? src.topChip() : chip;
   		if(chip!=null || src.isGipf())
   		{	
   			GColor myColor = playerColor[who];
   			if(top.color == myColor)
			   {
				   switch(top.potential)
				   {
				   default:
				   case None:		
					   // stack of ordinary pieces
					   throw G.Error("this shouldn't occur in matrx");
				   case Tamsk:
					   // trickey case
					   if(src==TamskCenter) { some |= addTamskMoves(all,who); }
					   break;
				   case Zertz:
					   	some |= addZertzMoves(all,src,who);
					   	break;
				   case Yinsh:
					   	some |= addYinshMoves(all,src,who);
					   	break;
				   case Punct:
				   case Dvonn:	// these are the same
					   some |= addPunctDvonnMoves(all,src,who);
					   break;
				   }
			   }
		   }
	   	return some;
   }
   private void addNormalSlideMoves(CommonMoveStack all,int who)
   {
	   sweep_counter++;
	   for(GipfCell c : edgeCells)
	   {	
		   addSlideMoves(all,c,whoseTurn,MOVE_SLIDE);				
	   }
   }
   public void addRemovalMoves(CommonMoveStack v,int dir, GipfCell c,int who)
   {
	   if(dir>=0)
	   	{	v.addElement(new Gipfmovespec(MOVE_REMOVE,c.col,c.row,whoseTurn));
			c.exitTo(dir).markRow(dir,-1);
			c.markRow(dir+3,-1);
	   	}
   }
   private void addRemovalMoves(CommonMoveStack v,GipfCell cc,int dir,int who)
   {
	   if(dir>=0)
	   {	int edir = dir;
	   		GipfCell c = cc;
	   		while(!c.isEdgeCell()) { c=c.exitTo(edir); }
	   		// rowcode is a bit mask of direction codes, we only want the
	   		// bot to click on codes that correspond to a single direction
	   		if(G.bitCount(c.rowcode)!=1)
	   		{
	   			edir = dir+3;
	   			c = c.exitTo(edir);
	   			while(!c.isEdgeCell()) { c=c.exitTo(edir); }
	   		}
	   		p1(G.bitCount(c.rowcode)==1,"should be a unique row");
	   		v.addElement(new Gipfmovespec(MOVE_REMOVE,c.col,c.row,whoseTurn));
	   		// mark the elements of the row so we won't add them again
	   		cc.markRow(dir,-1);
	   		cc.markRow(dir+3,-1);
	   		
	   }
   }

   private void addAllToggleGipfMoves(CommonMoveStack v,int whoseTurn)
   {
	   markForRemoval(whoseTurn);
	   boolean some = false;
	   for(GipfCell c=allCells; c!=null; c=c.next)
	   {
		   if((c.rowcode>0)  && c.isGipf())
		   {   if(c.preserved)
		   		{
			     v.addElement(new Gipfmovespec(MOVE_REMOVE,c.col,c.row,whoseTurn));
		   		}
		   		else 
		   		{ // a removed gipf
		   		  some = true; 
		   		}
		   }
	   }
	   if(some)
	   {
				addRemovalMoves(v,whoseTurn);
	   }
   }
   
   private void addRemovalMoves(CommonMoveStack v,int who)
   {  
	   for(GipfCell c=allCells; c!=null; c=c.next)
			{	if((c.topChip()!=null) && (!c.isGipf()||!c.preserved))
				{
				int sweep = c.rowcode;
				switch(sweep)
				{
				default: throw G.Error("Unexpected sweep value %s",sweep);
				case -1:
				case 0: break;
				case 7:
					addRemovalMoves(v,c,2,who);
				//$FALL-THROUGH$
			case 3:
					addRemovalMoves(v,c,1,who);
				//$FALL-THROUGH$
			case 1:	
					addRemovalMoves(v,c,0,who);
					break;
				case 6:
					addRemovalMoves(v,c,2,who);
				//$FALL-THROUGH$
			case 2: 
					addRemovalMoves(v,c,1,who);
					break;
				case 5:
					addRemovalMoves(v,c,0,who);
				//$FALL-THROUGH$
			case 4: 
					addRemovalMoves(v,c,2,who);
					break;
				}
				}
			}
   }
   private void addNormalPlacementMoves(CommonMoveStack v,int who)
   {
	   if(variation==Variation.Gipf_Matrx) 
			{
			if(rack[who][Potential.None.ordinal()].height()==0)
				{
				addPotentialMoves(v,who);		
				addPotentialSlideMoves(v,who);
				}
				else {
				addNormalSlideMoves(v,who);
				}
			}
			else
			{
  			addNormalSlideMoves(v,who);
   		}
   }

   public CommonMoveStack  GetListOfMoves()
   {	CommonMoveStack v = new CommonMoveStack();
   		switch(board_state)
   		{
   		default: 
   			p1("unimplemented "+board_state);
   			throw G.Error("not implemented for state %s",board_state);
   		case DESIGNATE_CAPTURE_OR_DONE_STATE:
   		case DONE_CAPTURE_STATE:
   			if(variation==Variation.Gipf_Matrx)
   			{
   				addAllToggleGipfMoves(v,whoseTurn);
   			}
			//$FALL-THROUGH$
		case DONE_STATE:
   			v.push(new Gipfmovespec(MOVE_DONE,whoseTurn));
   			break;
   		case SLIDE_GIPF_STATE:
   		case SLIDE_STATE:
   			sweep_counter++;
   			addSlideMoves(v,getDest(),whoseTurn,MOVE_SLIDEFROM);
   			break;
   		case MANDATORY_CAPTURE_STATE:
   		case MANDATORY_PRECAPTURE_STATE:
   			// only in Matrx
   			addAllToggleGipfMoves(v,whoseTurn);
   			break;
   			
		case PRECAPTURE_OR_START_NORMAL_STATE:
		case PRECAPTURE_OR_START_GIPF_STATE:
			addNormalPlacementMoves(v,whoseTurn);
			//$FALL-THROUGH$
		case PRECAPTURE_STATE:
   		case DESIGNATE_CAPTURE_STATE:
   			markForRemoval(whoseTurn);
   			if(variation==Variation.Gipf_Matrx) { addAllToggleGipfMoves(v,whoseTurn); }
   			addRemovalMoves(v,whoseTurn);			
   			break;
   		case PLACE_GIPF_STATE:
   			// add the option to switch to regular
   			if((rack[whoseTurn][Potential.None.ordinal()].height()>0) && canTogglePlacementMode()) 
   				{ v.addElement(new Gipfmovespec(MOVE_STANDARD,whoseTurn)); }
			//$FALL-THROUGH$
		case PLACE_STATE:
   			addNormalPlacementMoves(v,whoseTurn);
   			break;
		case MOVE_POTENTIAL_STATE:
			addPotentialMoves(v,whoseTurn);	
			break;
		case PLACE_OR_MOVE_POTENTIAL_STATE:
			addPotentialMoves(v,whoseTurn);			
			//$FALL-THROUGH$
		case PLACE_POTENTIAL_STATE:
			addPotentialSlideMoves(v,whoseTurn);
			break;
		case PLACE_TAMSK_FIRST_STATE:
		case PLACE_TAMSK_LAST_STATE:
			addTamskMoves(v,whoseTurn);
			break;
   		}
   		if(v.size()==0) 
   		{
   			p1("no moves "+board_state+" "+moveNumber);
   			G.Error("No moves");
   		}
	   	return(v);
   }
   
	 GipfPlay robot = null;
	 public void initRobotValues(GipfPlay r)
	 {
		 robot = r;
	 }
	 public boolean p1(boolean condition,String msg,Object... args)
	 {	if(!condition)
	 	{
		 p1(G.concat(msg,args));
		 G.Error(msg,args);
	 	}
	 	return condition;
	 }
	 public boolean p1(String msg)
		{
			if(G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/matrx/matrxgames/robot/";
				robot.saveCurrentVariation(dir+msg+".sgf");
				return(true);
			}
			return(false);
		}
}
