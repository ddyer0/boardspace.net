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
package universe;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import com.codename1.ui.geom.Rectangle;

import online.game.*;
import online.game.cell.Geometry;
import universe.UniverseChip.ChipColor;
import lib.*;



/**
 * This board is designed to handle general polyomino pieces, but the current implementation
 * fully implemented games are for 2 and 4 player pentominoes (Pan-Kai and Universe, Several Blokus variations, Phlip (aka Domain))
 * and various polyomino puzzles.
 * 
 * For these games, there is only one chip with each shape, the rotations and reflections are treated
 * as separate chips, but there is only one of each group in play at any time.
 * 
 * some of the games use the full set of polyominoes up to size 5, others use only the pentominoes,
 * others use a selection of dominos and trominos
 * 
 * @author ddyer
 *
 */

class UniverseBoard extends squareBoard<UniverseCell> implements BoardProtocol,UniverseConstants
{	static int REVISION = 101;					// before revisions, 100
										// revision 101 tweaks the endgame logic to include a "permanant pass" state
	public int getMaxRevisionLevel() { return(REVISION); }
	
	private UniverseState unresign;
	private UniverseState board_state;
	public UniverseState getState() {return(board_state); }
	public void setState(UniverseState st) 
	{ 	unresign = (st==UniverseState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	FlipStyle flippable = FlipStyle.allowed;		// controls if the robot allows flipping the tile over
	private boolean reassignSudoku=false;			// true if assigning new sudoku values should flush all the current ones

	// this chip rack, indexed by the owner and chip number. In most cases, owner and color are the same thing, 
	// but for phlip pieces, the flipped side is the same owner but a different color.   The cells in the rack
	// are always indexed by owner, which never changes.
	public boolean allDone[] = null;
	public UniverseCell rack[][] = null;			// the current chips available to the players
	public UniverseCell chipLocation[][] = null;	// the current location of each of those chips.
	public UniverseCell sudokuBoxes[] = null;		// root cells sudoku boxes. 
	public UniverseCell[] givenRack = null;			// rack of the givens
	public UniverseCell takenRack[] = null;			// rack of the excluded
	public CellStack givens = new CellStack();		// stack of cells with givens
	public int rackOffset = 0;						// offset of the first chip in the rack
	public int chipOffset = 0;						// offset to the global UniverseChip
    public int boardColumns = 0;					// size of the board.  Universe has an irregular shape.
    public int boardRows = 0;						// size of the board.  Universe has an irregular shape.
	public variation rules;							// the current variation
	public boolean robotBoard = false;
    public CellStack animationStack = new CellStack();

    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public UniverseChip pickedObject = null;
    public UniverseChip originalPickedObject = null;
    public UniverseCell pickedCell = null;
    public UniverseCell lastMove = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    static private final int MAX_REGIONS = 50;		// good question, what's the maximum number of regions that can be formed?
    private int universeRegionSize[] = new int[MAX_REGIONS];// size of empty regions by number
    private int topsForPlayer[] = null;
    public ChipColor playerColor[] = null;
    private CellStack diagonalPointsForPlayer[] = null;
    private boolean diagonalPointsForPlayerValid[] = null;
    private CellStack occupiedCells[]=null;
    private double estScores[] = null;
    private int consecutivePasses = 0;
    private boolean estScoresValid = false;
    private int regionIndex = 0;					// index into regionsize
    private int numberOfSize[] = new int[6];		// count of chips with size remaining, for polysolver and nudoku
    private int fullBoardSize = 0;					// size of the initial board
    private int emptySpaces = 0;
    private int sweep_counter = 0;
    private int sweep_size = 0;
    private boolean sweep_adjacent = false;		// used in polysolver to mark that the adjacency requirement is met 
    private int preferredOrientation = 0;
    public void SetDrawState() { throw G.Error("Not expected"); }
    public CellStack flippedCells = new CellStack();
    public IStack undoStack = new IStack();
    
    public boolean cachedMovesValid = false;
    
    // validate for a particular player
    private void validateDiagonalsCacheForPlayer(int playerIndex)
    {	if(!diagonalPointsForPlayerValid[playerIndex])
    	{	setDiagonalSweep3(diagonalPointsForPlayer[playerIndex],playerIndex);
    		diagonalPointsForPlayerValid[playerIndex] = true;
    	}
     }
    private void invalidateDiagonalPointsCache()
    {
    	AR.setValue(diagonalPointsForPlayerValid, false);
    }
    public void validateCachedMoves()
    {
    	cachedMovesValid=true;
     	for(int pl = rack.length-1; pl>=0; pl--)
    	{	
    		for(UniverseCell loc : rack[pl])
    		{
    			switch(rules)
    			{
    			case Diagonal_Blocks:
    			case Diagonal_Blocks_Duo:
         		case Diagonal_Blocks_Classic:
         		case Blokus:
         		case Blokus_Classic:
    			case Blokus_Duo:
    				validateDiagonalsCacheForPlayer(pl);
    				loc.canBePlayed = getLegalDiagonalBlocksMoves(null,loc,-1,pl,false);
    				break;
    			case Phlip:
    				loc.canBePlayed = getLegalPhlipMoves(null,loc,-1,pl);
    				break;
    			case Universe:
    			case Pan_Kai:
    				loc.canBePlayed = getLegalUniverseMoves(null,loc,-1,pl);
    				break;
    			default: loc.canBePlayed = true;
    				break;
    			}
    		}
    	}
    }
    
    public boolean canBePlayed(UniverseCell c)
    {	if(!cachedMovesValid) { validateCachedMoves(); }
    	return(c.canBePlayed);
    }
    // factory method for board cells
	public UniverseCell newcell(char c,int r)
	{	return(new UniverseCell(c,r));
	}
	public boolean emptyBoard()
	{
		return(emptySpaces==fullBoardSize);
	}
	public UniverseBoard(String init,long key,int players,int []map,int rev) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = UNIVERSEGRIDSTYLE; //coordinates left and bottom
       	setColorMap(map, players);
       	doInit(init,key,players,rev); // do the initialization 
    }
    
	private UniverseCell edgeLocations[] = null;	// edges or diagonal from edges
    public int edgesForPlayer(int pl)
    {	if(edgeLocations==null)
    	{
	    	CellStack edges = new CellStack();
	    	for(UniverseCell c = allCells; c!=null; c=c.next)
	    	{
	    	if(c.isEdgeCell()) { edges.push(c); }
	    	}
	    	edgeLocations = edges.toArray();
    	}
    	int n=0;
    	for(int lim = edgeLocations.length-1; lim>=0; lim--)
    	{
    		UniverseCell c = edgeLocations[lim];
    		if(c.chip.color==playerColor[pl]) { n++; }
    	}
    	return(n);
    }
    
    // estimated score for static evaluator
    public int scoreForPlayer(int pl)
    {	// be a little defensive for displays when the board is being reinitialized for a new game
    	if(pl>=nPlayers()) { return(0); }
    	switch(rules)
    	{
    	default:
    		throw G.Error("Not expected");
    		
    	case Nudoku_6x6:
    	case Nudoku_8x8:
    	case Nudoku_9x9:
    	case Nudoku_12:
    	case Nudoku_11:
    	case Nudoku_10:
    	case Nudoku_9:
    	case Nudoku_8:
    	case Nudoku_7:
    	case Nudoku_6:
    	case Nudoku_5:
    	case Nudoku_4:
    	case Nudoku_3:
    	case Nudoku_2:
    	case Nudoku_1:
    	case Sevens_7:
    	case Nudoku_1_Box:
    	case Nudoku_2_Box:
    	case Nudoku_3_Box:
    	case Nudoku_4_Box:
    	case Nudoku_5_Box:
    	case Nudoku_6_Box:
    		return(0);
    	case Phlip:
    	case Diagonal_Blocks_Duo:
    	case Blokus_Duo:
    	case Diagonal_Blocks:
 		case Diagonal_Blocks_Classic:
 		case Blokus:
 		case Blokus_Classic:
    		return(topsForPlayer[pl]);
    	case Universe:
    	case Pan_Kai:
    		int pieces = 0;
    		for(UniverseCell c : chipLocation[pl]) { if(c.onBoard) { pieces++; }}
    		// win[pl] flags that the player sill had legal moves, so would have scored at least 1+
    		if(win[pl]) {pieces++;}
    		return(pieces);
    	}
    	
    }
    public boolean currentPlayerIsBehind()
    {	G.Assert(players_in_game==2,"not a 2 player game");
    	return(scoreForPlayer(whoseTurn)<scoreForPlayer(getNextPlayer(whoseTurn)));
    }

    //
    // the number of pieces left to play
    //
    private int piecesLeft(int pl)
    {	int n=0;
    	for(UniverseCell c : rack[pl]) { if(c.topChip()!=null) { n++; }}
    	return(n);
    }
    private double estScoreFor(int pl)
    {	double val = topsForPlayer[pl];
    	// count the available diagonals as a proxy for the number of moves
		validateDiagonalsCacheForPlayer(pl);
    	double diagonals = diagonalPointsForPlayer[pl].size()*0.2/(piecesLeft(pl)+1.0);
    	return((val+diagonals)/90.0);
    }
    public double estScoreForPlayer(int pl)
    {	// call calculateEstScores first
    	G.Assert(estScoresValid,"est scores not set up");
    	return(estScores[pl]);
    }
    public void calculateEstScores()
    {
    	for(int i=0;i<players_in_game;i++)
    	{
    		estScores[i] = estScoreFor(i);
    	}
    	estScoresValid = true;
    }

  
    // this sets up the links for the boxes in sudoku board the hard way, but specifiying
    // the sets of cells that go into each box individually.
    public void initSudokuAny(String []boxes)
    {	sudokuBoxes = new UniverseCell[boxes.length];
    	int idx = 0;
    	for(String str : boxes)
   		{	UniverseCell first = null;
   			UniverseCell prev = null;
   			StringTokenizer tok = new StringTokenizer(str);
   			int background = G.IntToken(tok);
   			while(tok.hasMoreTokens())
   			{
   				char col = G.CharToken(tok);
   				int row = G.IntToken(tok);
   				UniverseCell c = getCell(col,row);
    			c.cellImageIndex = background;
    			if(first==null) { first = c; }
    			c.nextInBox = prev;
    			prev = c;
   			}
    	first.nextInBox = prev;
    	sudokuBoxes[idx++] = first;
     	}
    }
    
    // this sets up the links for the boxes in sudoku board, by listing an
    // arbitrary set of rectangles.
    public void initSudokuRects(Rectangle[]rects,int backgrounds[])
    {	sudokuBoxes = new UniverseCell[rects.length];
    	int idx = 0;
    	for(Rectangle r : rects)
   		{	UniverseCell first = null;
   			int rownum = G.Top(r);
   			int colnum = G.Left(r);
   			int ncols = G.Width(r);
   			int nrows = G.Height(r);
    		UniverseCell prev = null;
    		for(int cc = 0; cc<ncols; cc++)
    			{
    				for(int rr=0;rr<nrows;rr++)
    				{
    					UniverseCell c = getCell((char)('@'+colnum+cc),rownum+rr);
    					c.cellImageIndex = backgrounds[idx];
    					if(first==null) { first = c; }
    					c.nextInBox = prev;
    					prev = c;
    				}
    			}
    			first.nextInBox = prev;
    			sudokuBoxes[idx++] = first;
     		}
    }
    // this sets up the links for the boxes in sudoku board in a regular grid, usually
    // either the 9 3x3 boxes or the 6 2x3 boxes
    public void initSudokuBoxes(int ncols,int nrows)
    {	sudokuBoxes = new UniverseCell[(boardColumns*boardRows)/(ncols*nrows)];
    	int idx = 0;
    	for(int col=0,colnum = 0; colnum<boardColumns; col++,colnum+=ncols)
    	{
    		for(int row=0,rownum=1; rownum<=boardRows; row++,rownum+=nrows)
    		{	UniverseCell first = null;
    			UniverseCell prev = null;
    			for(int cc = 0; cc<ncols; cc++)
    			{
    				for(int rr=0;rr<nrows;rr++)
    				{
    					UniverseCell c = getCell((char)('A'+colnum+cc),rownum+rr);
    					c.cellImageIndex = (((row+col)&1)==0) ? GRAY_INDEX : DARKGRAY_INDEX;
    					if(first==null) { first = c; }
    					c.nextInBox = prev;
    					prev = c;
    				}
    			}
    			first.nextInBox = prev;
    			sudokuBoxes[idx++] = first;
     		}
    	}
    }
    // init to "no boxes" for non-nudoku puzzles.  This includes the 7's puzzle.
    public void initSudoKuBoxes()
    {  	sudokuBoxes = null;
    	for(UniverseCell c = allCells; c!=null; c=c.next) 
    		{ c.nextInBox = null;
    		  c.sudokuValue = 0; 
    		}
    }
    // from some seed (normally A1) traverse the rows checking the columns, or
    // traverse the columns checking the rows.
    public boolean validSudokuHome(UniverseCell colSeed,int major_direction,int minor_direction)
  	{
       	while(colSeed!=null)
    	{
    		UniverseCell rowSeed = colSeed;
       		// check each row
        	int rowvalues = 0; 		
    		while(rowSeed !=null)
    		{
    			int v = rowSeed.sudokuValue;
    			if(v!=0)
    			{	int mask = (1<<v);
    				if((rowvalues & mask)!=0)
    					{ return(false); }
    				rowvalues |= mask;
    			}
    			rowSeed = rowSeed.exitTo(minor_direction);
    			}
    		
    		colSeed = colSeed.exitTo(major_direction);
    		}
       		return(true);
       	}
    public boolean isValidSeven(UniverseCell c)
    {	c.sweep_counter = sweep_counter;
		if(c.nextInBox!=null)
		{	// null means an unassigned cell
			// non null must be a ring that adds up to 7
			UniverseCell start = c;
			UniverseCell current = c;
			int sum = 0;
			do { 
				sum += current.sudokuValue;
				current.sweep_counter = sweep_counter;
				current = current.nextInBox;
				
			} while((current!=null) && (current!=start));
			if(sum!=7) 
				{ return(false); }
		}
		return(true);
    }
    public boolean allSevensLinked()
    {	for(UniverseCell c =allCells; c!=null; c=c.next) 
    		{ if(c.nextInBox==null) 
    			{ return(false); }}
    	return(true);
    }
    public boolean isValidSevens()
    {	sweep_counter++;
    	switch(rules)
    	{
    	case Sevens_7: 
    		{
    		for(UniverseCell c = allCells; c!=null; c=c.next)
    			{
    			if(c.sweep_counter!=sweep_counter)
    				{
    				if(!isValidSeven(c)) { return(false); }
    				}
    			}
    		}
    	break;
    	default:
    		throw G.Error("Not expecting rules %s",rules);
    	}
    	return(true);
    }
    public boolean hasValidGivens()
    {
    	for(int idx = 0,limit = givens.size(); idx<limit; idx++)
    	{
    		UniverseCell c = givens.elementAt(idx);
    		if(c.topChip()!=null)
    		{
    		UniverseChip given = c.getGiven();
    		int val = given.givensIndex();
    		if(val!=c.sudokuValue) 
    			{ return(false); }
    		}
    	}
    	return(true);
    }
    //
    // true if the current board values are a valid sudoku.  Values of zero are ignored,
    // so any partially filled board is valid if none of the sudoku rules are currently
    // violated.  This is just a checking that the nonzero numbers are unique in row, column and box groups.
    public boolean isValidSudoku()
    {	if(!hasValidGivens()) { return(false); }
    	// check the boxes
    	if(sudokuBoxes!=null)
    	{
    	for(int box = 0,lim = sudokuBoxes.length; box<lim; box++)
    	{
    		UniverseCell boxseed = sudokuBoxes[box];
    		UniverseCell thisbox = boxseed;
        	int boxvalues = 0;
    		do 
    		{	int v = thisbox.sudokuValue;
    			int mask = (1<<v);
    			if(v!=0)
    			{
    				if((boxvalues&mask)!=0) 
    					{ return(false); }		// duplicate value
    				boxvalues |= mask;			// remember we saw this value
    			}
    			thisbox = thisbox.nextInBox;
    		} while(thisbox!=boxseed);
    	}
    	
      	UniverseCell seed = getCell('A',1);
    	// check the rows
      	if(!validSudokuHome(seed,CELL_RIGHT,CELL_UP)) 
      		{ return(false); }
    	// check the columns
      	if(!validSudokuHome(seed,CELL_UP,CELL_RIGHT)) 
      		{ return(false); }
    	}
    	return(true);
    }
    
    int sudokuSteps = 0;
    // generate a valid sudoku by simple recursion and backtracking.  Sudokusteps counts the number of steps
    // involved, but doesn't have any real signidicance
    public boolean generateSudoku(UniverseCell c,Random r)
    {	sudokuSteps++;
    	if(c!=null)
     	{
    	int attempts[] = randomIntArray(r,boardColumns);
       	int modulus = attempts.length;
    	for(int i=0;i<modulus; i++)
    	{
        c.sudokuValue = attempts[i];
        if(isValidSudoku()) 
        	{ boolean ok = generateSudoku(c.next,r);
        	  if(ok) { return(ok); }
        	}
    	}
    	c.sudokuValue = 0;
    	return(false); 	// failed
    	}
    	return(true);
    }
    private int[] randomIntArray(Random r,int size)
    {  	int values[] = new int[size];
    	for(int i=0;i<size; i++) { values[i]=i+1; }
    	r.shuffle(values);
    	return(values);
    }
    
    // assign random sudoku values to all the chips that haven't been used yet.
    private void assignResidualValues(Random r)
    {	int idx =  0;
    	int values[] = null;
    	for(UniverseCell c : rack[0])
    	{
    	UniverseChip top = c.topChip();
    	if(top!=null)
    	{
    		int pl = top.patternSize();
    		int sudovalues[] = new int[pl];
    		while(pl-->0)
    		{	
    			if(values==null || idx>=values.length) 
    				{ values = randomIntArray(r,boardColumns); idx = 0; 
    				}
    			sudovalues[pl] = values[idx++];
    		}
    		top.assignSudokuValues(sudovalues);
    	}
    	}
    }
    // associate the sudoku values with the pieces on the board
    private void assignValues()
    {	Hashtable<UniverseChip,int[]>values = new Hashtable<UniverseChip,int[]>();
    	for(UniverseCell c = allCells; c!=null; c=c.next)
    	{	UniverseChip top = c.topChip();
    		if(top!=null)
    		{
    		int vv[] = values.get(top);
    		if(vv==null) 
    			{ vv = new int[top.patternSize()]; 
    			  values.put(top,vv);
    			}
    		vv[c.patternStep] = c.sudokuValue;
    		}
     	}
    	// once all the values have been accumulated and fully popylated, assign
    	// the new values which will also recalculate rotations and flippage.
    	for(Enumeration<UniverseChip> keyv = values.keys(); keyv.hasMoreElements();)
    			{	UniverseChip key = keyv.nextElement();
    				key.assignSudokuValues(values.get(key));
    				//G.print(""+key+" "+key.uniqueVariations.length);
    			}
    }
    //
    // generate a random solved sudoku grid
    //
    public void generateSudoku(long seed)
    {	sudokuSteps = 0;
    	for(UniverseCell c = allCells; c!=null; c=c.next) { c.sudokuValue = 0; }
    	Random r = new Random(seed);
    	while(!generateSudoku(allCells,r)) 
    		{
    		// backtracking at the top level ought to be rare, and certainly finite
    		};
    	G.print("Sudoku in "+sudokuSteps+" steps");
    }
    
    // generate a single 7, stepping in a random direction from "current".  If we succeed link
    // the values in a ring.
    public boolean generateSevensFrom(Random r,UniverseCell start, UniverseCell current, int sum)
    {	//int nextVal = r.nextInt(Math.min(6,7-sum))+1;	// the min clause removes "7" cells.
    	int nextVal = Math.min(r.nextInt(6)+1,7-sum);	// favor shorter chains
    	current.sweep_counter = sweep_counter;
    	current.sudokuValue = nextVal;
    	if(sum+nextVal == 7) 
    		{ current.nextInBox = start; 
    		  return(true); 
    		} 	// success!
    	
    	int directions[] = randomIntArray(r,CELL_FULL_TURN);
    	// step in all possible directions in a random order
    	for(int direction = 0; direction<directions.length; direction++)
    	{
    		UniverseCell next = current.exitTo(directions[direction]);
    		if((next!=null) && (next.nextInBox==null) && (next.sweep_counter!=sweep_counter))
    		{
    			boolean step = generateSevensFrom(r,start,next,sum+nextVal);
    			if(step)
    				{ current.nextInBox = next; 
    				  return(true); 
    				}
    			// failed in this direction
    		}
    	}
    	return(false);	// failed completely
    }
    // generate a sevens puzzle starting at C with R
    public boolean generateSevens(CellStack seeds,Random r)
    {	sudokuSteps++;
    	if(seeds.size()==0) { return(true); } // got to the end
     	
    	UniverseCell top = seeds.pop();
    	boolean val = generateSevens(seeds,top,r);
    	seeds.push(top);
    	return(val);
    }
    public boolean generateSevens(CellStack seeds,UniverseCell top,Random r)
    {
    	if(top.nextInBox!=null) {	 return(generateSevens(seeds,r)); } // try another

    	boolean ok = false;
    	for(int i=0;i<boardColumns && !ok; i++)		// no partular logic to boardColumns, just an attempt count
    		{
    		sweep_counter++;
    		ok = generateSevensFrom(r,top,top,0);
    		}
    	if(!ok) { 		return(false);		}	// failed at this level
    	
    	for(int i=0;i<2;i++)	// small retries works better. 
    	{
    	if(generateSevens(seeds,r)) 
    			{ 
    			return(true); 
    			}
    	}
    	// failed at some recursive level, must clean up and backtrack
    	UniverseCell end = top;
    	do { UniverseCell cur = end;
    		 end = end.nextInBox;
    		 cur.nextInBox = null;
    		 cur.sudokuValue = 0;
    		} while(end!=top);
      	return(false); 	// failed
    }
    
    //
    // generate a random solved sudoku grid
    //
    public void generateSevens(long seed)
    {	sudokuSteps = 0;
    	CellStack starts = new CellStack();
    	Random r = new Random(seed);
    	initSudoKuBoxes();	//clear the boxes
    	for(UniverseCell c = allCells; c!=null; c=c.next) { starts.push(c); }
    	// randomize the order of potential "start" cells, to eliminate the
    	// obvioys weakness if G7 is always a start cell and subsequent start cells
    	// are G6 G5 etc.
    	starts.shuffle(r);
    	while(!generateSevens(starts,r)) 
    		{
    		// backtracking at the top level ought to be rare, and certainly finite
    		starts.shuffle(r);
    		};
    	G.print("Sevens in "+sudokuSteps+" steps");
    }
    
    // pick a completely random move.  This is called before the sudoku are assigned,
    // so only the polyomino fit matters.
    public commonMove randomMove(Random r)
    {
    	CommonMoveStack  all = new CommonMoveStack();
    	switch(rules)
    	{
        case Sevens_7:
        	getLegalSevensMoves(all,whoseTurn);
        	break;
    	case Nudoku_12:
		case Nudoku_11:
		case Nudoku_10:
		case Nudoku_9:
		case Nudoku_8:
		case Nudoku_7:
		case Nudoku_6:
		case Nudoku_5:
		case Nudoku_4:
		case Nudoku_3:
		case Nudoku_2:
		case Nudoku_1_Box:
		case Nudoku_2_Box:
   		case Nudoku_3_Box:
		case Nudoku_4_Box:	// 3 x 2 in 2x2
   		case Nudoku_5_Box:
		case Nudoku_6_Box:
		case Nudoku_1:
    		getLegalSnakessolverMoves(all,-1,whoseTurn);
    		break;
    	case Nudoku_6x6:
    	case Nudoku_9x9:
    		getLegalPolysolverMoves(all,-1,whoseTurn);
    		break;
    	default: throw G.Error("not handled");
    	}
    	if(all.size()>0)
    	{
    		return(all.elementAt(Random.nextInt(r,all.size())));
    	}
    	return(null);
    }
    int puzzleSteps = 0;
    //
    // generate a random polyomino puzzle by simple trial and  error.
    // 
    public boolean generateSudokuPuzzle(Random r,CommonMoveStack solution)
    {	commonMove m = null;
    	int attempts = 0;
    	int limit = emptyBoard() ? 100 : 5;
    	puzzleSteps++;
    	while(attempts++ < limit && ((m = randomMove(r))!=null))
    		{ RobotExecute((UniverseMovespec)m);
    		  solution.push(m);
    		  boolean complete = generateSudokuPuzzle(r,solution);
    		  if(!complete)
    			  {
    			  // backgrack if we can't find a solition in a few tries.
    			  solution.pop();
    			  UnExecute((UniverseMovespec)m);
    			  }
    		}
    	return(board_state==UniverseState.GAMEOVER_STATE);
    }
   
    // check that the assigned sudoku values result in unique tiles.  If not, we have
    // no better plan than to try a different set of values.
    public boolean checkDuplicates()
    {
    	Hashtable <String,UniverseChip>uids = new Hashtable<String,UniverseChip>();
    	boolean dups = false;
    	for(UniverseCell c : rack[0])
    	{
    		UniverseChip top = UniverseChip.getChip(chipOffset+c.row);
    		if(top!=null)
    		{	for(UniverseChip ch : top.getVariations())
    			{
    			String uid = ch.getPatternUid();
    			UniverseChip prev = uids.get(uid);
    			if(prev!=null)
    				{ //G.print("Uid "+ch+" matches "+prev); dups = true; 
    				}
    			else { uids.put(uid,ch); }
    			}
    		}
    		
    	}
    	if(dups) { G.print(""); }
    	return(dups);
    }
    //
    // generate a complete nudoku puzzle by first choosing a random
    // polyomino solution, then a random sudoku solution, and marrying them.
    // we retry until this results in a unique set of tiles, which helps
    // make the resulting puzzle have only one solution, but doesn't guarantee it.
    //
    public CommonMoveStack  generateNudokuPuzzle()
    {	Random r = new Random();
    	long k1 = r.nextLong();		// key for generating the poly solution
    	long k2 = r.nextLong();		// key for generating sudoku
    	long k3 = r.nextLong();		// key for assigning residual values
    	CommonMoveStack solution = new CommonMoveStack();
    	Random rpoly = new Random(k1);
    	UniverseChip.clearSudokuValues();
       	puzzleSteps = 0;
       	generateSudokuPuzzle(rpoly,solution);

    	do {
    		// generate s sudoku
       		generateSudoku(k2++);
       		UniverseChip.clearSudokuValues();
       		assignValues();
       		assignResidualValues(new Random(k3));
    	} while(checkDuplicates());		// loop until the puzzle pieces are are all unique
   		
    	solution.shuffle(r);;
    	G.print("Puzzle in "+puzzleSteps+" steps");
    	// marry the pieces and forget the solution
   		for(UniverseCell c : rack[0])
   		{
   			UniverseChip ch = c.topChip();
   			if(ch==null) { ch = UniverseChip.getChip(playerColor[c.col-'A'],c.row+chipOffset); }
   			if((ch!=null) && (ch.sudokuValues!=null))
   			{
   				solution.push(new UniverseMovespec(MOVE_ASSIGN,c.col,c.row,ch.sudokuValues));
   			}
   		}
   		return(solution);
    }
    public CommonMoveStack  generateSevensPuzzle()
    {	CommonMoveStack solution = new CommonMoveStack();
    	Random r = new Random();
    	long k1 = r.nextLong();		// key for generating the poly solution
    	UniverseChip.clearSudokuValues();
    	
       	generateSevens(k1);


       	// save the initial solution
       	sweep_counter++;
       	for(UniverseCell c = allCells; c!=null; c=c.next)
       	{
       		if(c.sweep_counter!=sweep_counter)
       		{
       			UniverseCell start = c;
       			do {
       				UniverseCell from = c;
       				from.sweep_counter = sweep_counter;
       				c = c.nextInBox;
       				solution.push(new UniverseMovespec(MOVE_LINK,from.col,from.row,c.col,c.row,0));
       			} while(c!=start);
       		}
       	}
       	int svals[] = saveSudokuValues();
       	solution.push(new UniverseMovespec(MOVE_ASSIGN,allCells.col,allCells.row,svals));
  	
    	return(solution);
    	
    }
    public CommonMoveStack  generatePuzzle()
    {
    	switch(rules)
    	{
        case Nudoku_9x9:
        case Nudoku_6x6:
        case Nudoku_12:
		case Nudoku_11:
		case Nudoku_10:
		case Nudoku_9:
		case Nudoku_8:
		case Nudoku_7:
		case Nudoku_6:
		case Nudoku_5:
		case Nudoku_4:
		case Nudoku_3:
		case Nudoku_2:
		case Nudoku_1_Box:
		case Nudoku_2_Box:
   		case Nudoku_3_Box:
		case Nudoku_4_Box:	// 3 x 2 in 2x2
   		case Nudoku_5_Box:
		case Nudoku_6_Box:
		case Nudoku_1:
			return(generateNudokuPuzzle());
    	case Sevens_7:
    		return(generateSevensPuzzle());
    		
    	default: throw G.Error("Not expecing rule %s", rules);
    	}
    	
    }
	public void sameboard(BoardProtocol f) { sameboard((UniverseBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(UniverseBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell
        G.Assert(sameContents(rack,from_b.rack),"rack mismatch");
        G.Assert(sameCells(chipLocation,from_b.chipLocation),"chip location mismatch");
        G.Assert(pickedCell.sameCell(from_b.pickedCell),"pickedCell mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(sameCells(flippedCells,from_b.flippedCells),"flippedCells mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");

        G.Assert(AR.sameArrayContents(topsForPlayer,from_b.topsForPlayer),"tops count mismatch");
        G.Assert(AR.sameArrayContents(numberOfSize,from_b.numberOfSize),"numberOfSize mismatch");
        G.Assert(AR.sameArrayContents(universeRegionSize,from_b.universeRegionSize),"region size mismatch");
        G.Assert(emptySpaces==from_b.emptySpaces,"emptyspaces mismatch");
        G.Assert(regionIndex == from_b.regionIndex,"regionIndex mismatch");
        G.Assert(originalPickedObject==from_b.originalPickedObject,"originalPickedObject doesn't match");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

   public long digestSevens()
   {   long val = 0;
	   sweep_counter++;
	   for(UniverseCell c = allCells; c!=null; c=c.next)
	   {
		   if(c.sweep_counter!=sweep_counter)
		   {
			   long v = 0;
			   UniverseCell top = c;
			   do { 
				   top.sweep_counter = sweep_counter;
				   if(top.nextInBox!=null) { v ^= top.randomv*2; }
				   top = top.nextInBox;
			   	   } while((top!=null) && (top!=c));
			   val += v;
		   }
	   }
	   return(val);
   }
   public long Digest()
    {
       long v = 0;

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        v += UniverseChip.DigestAll();
 		for(UniverseCell c = allCells; c!=null; c=c.next)
		{	v ^= c.Digest(r);
		}

 		switch(rules)
 		{
 		default: break;
 		case Sevens_7:
 			v ^= digestSevens();
 		}

		v ^= Digest(r,chipLocation);
		v ^= chip.Digest(r,pickedObject);

		v ^= Digest(r,flippedCells);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
		v ^= Digest(r,topsForPlayer);


		v ^= Digest(r,pickedCell);
	    // initialize the region map for universe and pan-kai
	    v ^= Digest(r,numberOfSize);
	    v ^= Digest(r,universeRegionSize);
	    v ^= regionIndex*r.nextLong();

        return (v);
    }
   public UniverseBoard cloneBoard() 
	{ UniverseBoard copy = new UniverseBoard(gametype,randomKey,players_in_game,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}

   public void copyFrom(BoardProtocol b) { copyFrom((UniverseBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(UniverseBoard from_b)
    {	
    	super.copyFrom(from_b);
        pickedObject = from_b.pickedObject;	
        originalPickedObject = from_b.originalPickedObject;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(givens,from_b.givens);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(flippedCells,from_b.flippedCells);
        copyFrom(undoStack,from_b.undoStack);
        pickedCell.copyFrom(from_b.pickedCell);
        getCell(occupiedCells,from_b.occupiedCells);
        getCell(chipLocation,from_b.chipLocation);
        AR.copy(allDone,from_b.allDone);
        AR.copy(playerColor,from_b.playerColor);
        copyFrom(rack,from_b.rack);

    	randomPiecePtable = AR.intArray(rack[0].length); 				// allocate a permutation table
    	board_state = from_b.board_state;
    	unresign = from_b.unresign;
    	copyFrom(takenRack,from_b.takenRack);

		for(UniverseCell dest=allCells,src=from_b.allCells;
			dest!=null;
			dest=dest.next,src=src.next)
		{ 
		  dest.nextInBox = getCell(src.nextInBox);
		}
		
	    // initialize the region map for universe and pan-kai
        fullBoardSize = from_b.fullBoardSize;
        emptySpaces = from_b.emptySpaces;
        AR.copy(topsForPlayer,from_b.topsForPlayer);
        AR.copy(universeRegionSize,from_b.universeRegionSize);
        AR.copy(numberOfSize,from_b.numberOfSize);
	    regionIndex = from_b.regionIndex;
	    sweep_counter = from_b.sweep_counter;
        sameboard(from_b);
    }
    public int[] saveSudokuValues()
    {
    	IStack v = new IStack();
    	for(UniverseCell c = allCells; c!=null; c=c.next)
    	{
    		v.push(c.sudokuValue);
    	}
    	return(v.toArray());
    }
    public void restoreSudokuValues(int v[])
    {	if(v!=null)
    	{
    	UniverseCell c = allCells;
    	for(int i=0,lim=v.length; i<lim;i++)
    	{
    	c.sudokuValue = v[i];
    	c = c.next;
    	}
    	}
    }
    public void doInit()
    {	doInit(gametype,randomKey,players_in_game,revision);
    }
    public void doInit(String game,long key)
    {
    	doInit(game,key,players_in_game,revision);
    }
    public void setRules(variation rul)
    {
    	rules = rul;
    	gametype = rules.name;
    }
    public void removeFromDiag(UniverseCell c)
    {
    	for(int i=0;i<players_in_game; i++) 
    		{ int diag_mask = 0x100<<i;
    		  if((c.diagonalResult&diag_mask)!=0)
    		  {	  c.diagonalResult &= ~diag_mask;
    			  diagonalPointsForPlayer[i].remove(c,false); 
    		  }
    		}
    }
    public void addDiagonalPoints(UniverseCell c,UniverseChip ch,int playerIndex)
    {
			CellStack points = diagonalPointsForPlayer[playerIndex];
   			addDiagonalSweep4(points,c,playerIndex);
 
   			UniverseCell d = c;
   			for( OminoStep step : ch.pattern)
   			{	UniverseCell d1 = d;
			boolean retry = false;
			switch(step.dx)
    		{	case -1:	d = d.exitTo(CELL_LEFT);
    						break;
    			case 1:		d = d.exitTo(CELL_RIGHT);
    						break;
    			default:
    		}
			if(d==null)
			{ 
			// if stepping once is off the grid, try the other way
			d = d1;
			retry = true;
			}
 			
	    	if(d!=null)
    		{
    		switch(step.dy)
    		{	case -1:	d = d.exitTo(CELL_UP);
    						break;
    			case 1:		d = d.exitTo(CELL_DOWN);
    						break;
			default:
				break;
    		}}
    		if(retry && (d!=null))
    		{	
    			switch(step.dx)
        		{	case -1:	d = d.exitTo(CELL_LEFT);
        						break;
        			case 1:		d = d.exitTo(CELL_RIGHT);
        						break;
        			default:
        		}	
    		}
    		if(d!=null) { addDiagonalSweep4(points,d,playerIndex); }
   			}
     }
    public boolean isPrimaryLocation(UniverseCell c)
    {
    	UniverseChip ch = c.topChip();
    	if(ch!=null)
    		{ 	int player = getPlayerIndexForOwner(ch);
         		int pat = ch.getPatternIndex();
        		return(chipLocation[player][pat-rackOffset] == c);
    		}
    	return(false);
    	
    }
    public int getPlayerIndexForOwner(UniverseChip ch) 
    { for(int i=0;i<playerColor.length;i++) 
    	{ if(ch.nativeColor==playerColor[i]) 
    		{ return(i); }
    	}
      throw G.Error("No player owns color "+ch);
    }
    public int getPlayerIndexForColor(UniverseChip ch) 
    { for(int i=0;i<playerColor.length;i++) 
    	{ if(ch.color==playerColor[i]) 
    		{ return(i); }
    	}
      throw G.Error("No player owns color "+ch);
    }
    
    public void addChip(UniverseCell c,UniverseChip ch)
    {	int player = getPlayerIndexForOwner(ch);
    	int pat = ch.getPatternIndex();
		if(player>=0) 
			{ //G.print("loc "+player+" "+(pat-rackOffset)+"="+c);
			  chipLocation[player][pat-rackOffset] = c; 
			}
    	if(c.onBoard) 
   		{ 	CellStack occupied = occupiedCells[player];
   			int allDiag_mask = 0xff00;
   			int sz = ch.patternSize();
   			topsForPlayer[getPlayerIndexForColor(ch)] += sz;
   			emptySpaces -= sz;
   			occupied.push(c); 
    		c.addChip(ch,0);
    		if((c.diagonalResult&allDiag_mask)!=0) { removeFromDiag(c);  }
        	G.Assert(c.geometry==Geometry.Square,"is a square cell");
        	UniverseCell d = c;
        	int idx = 1;
        	for( OminoStep step : ch.pattern)
        	{	UniverseCell d1 = d;
    			boolean retry = false;
    			switch(step.dx)
        		{	case -1:	d = d.exitTo(CELL_LEFT);
        						break;
        			case 1:		d = d.exitTo(CELL_RIGHT);
        						break;
        			default:
        		}
    			if(d==null)
    			{ 
    			// if stepping once is off the grid, try the other way
    			d = d1;
    			retry = true;
    			}
     			
    	    	if(d!=null)
        		{
        		switch(step.dy)
        		{	case -1:	d = d.exitTo(CELL_UP);
        						break;
        			case 1:		d = d.exitTo(CELL_DOWN);
        						break;
				default:
					break;
        		}}
        		if(retry && (d!=null))
        		{	
        			switch(step.dx)
            		{	case -1:	d = d.exitTo(CELL_LEFT);
            						break;
            			case 1:		d = d.exitTo(CELL_RIGHT);
            						break;
            			default:
            		}	
        		}
        		if(d==null) { throw G.Error("Stepped off the board"); }
        		d.sweep_counter = c.sweep_counter;
        		d.addChip(ch,idx++);
        		occupied.push(d);
        		if((d.diagonalResult&allDiag_mask)!=0) { removeFromDiag(d);  }
         	}
  			addDiagonalPoints(c,ch,player);
   		}
    	else { c.addChip(ch); }
    }
    public UniverseChip removeChip(UniverseCell c)
    {  	UniverseChip ch = c.chip;
    	int player = getPlayerIndexForOwner(ch);
    	int pat = ch.getPatternIndex();
    	if(player>=0) 
    		{ //G.print("loc "+player+" "+(pat-rackOffset)+"="+null);
    		  chipLocation[player][pat-rackOffset] = null; 
    		}
    	c.chip=null;
    	if(c.onBoard)
    		{ 
    		//
    		// also remove the rest of the chips in the pattern
    		//
    		CellStack occupied = occupiedCells[player];
    		int sz = ch.patternSize();
       		topsForPlayer[getPlayerIndexForColor(ch)] -= sz;
       		emptySpaces += sz;
       		occupied.remove(c,false); 
    		UniverseCell d = c;
    		for( OminoStep step : ch.pattern)
    		{	UniverseCell d1 = d;
    			boolean retry = false;
    			switch(step.dx)
    			{	case -1:	d = d.exitTo(CELL_LEFT);
    							break;
    				case 1:		d = d.exitTo(CELL_RIGHT);
    							break;
    				default:
    			}
    			if(d==null)
    			{ 
    			// if stepping once is off the grid, try the other way
    			d = d1;
    			retry = true;
    			}
     			if(d!=null)
    			{
    			switch(step.dy)
    			{	case -1:	d = d.exitTo(CELL_UP);
    							break;
    				case 1:		d = d.exitTo(CELL_DOWN);
    							break;
				default:
					break;
    			}
    			if(retry && (d!=null))
    			{	
    				switch(step.dx)
    	    		{	case -1:	d = d.exitTo(CELL_LEFT);
    	    						break;
    	    			case 1:		d = d.exitTo(CELL_RIGHT);
    	    						break;
    	    			default:
    	    		}	
    			}
    			G.Assert(d!=null,"Stepped off the board");
    			d.sweep_counter = c.sweep_counter;
    			d.removeTop();
    			occupied.remove(d,false);
    			}
    			}
 
    		}
    	return(ch);
    }
    /* initialize a board back to initial empty undoInfo */
  	public void doInit(String gtype,long key,int np,int rev)
    {	
    	randomKey = key;	// not used, but for reference in this demo game
    	adjustRevision(rev);
    	variation oldrules = rules;
       	rules = variation.find(gtype);
       	G.Assert(rules!=null,WrongInitError,gtype);
       	
       	gametype = rules.name;
       	int svals[] = (oldrules==rules) ? saveSudokuValues() : null;
       	//Random or = new Random(randomKey);
       	preferredOrientation = 0;
     	int racksize =  rules.tileSet.length;
       	rackOffset = OminoStep.FULLSET.length-racksize;
       	chipOffset = rackOffset;
     	reassignSudoku = true;
       	flippable = FlipStyle.allowed;
        UniverseChip.tweenStyle = TweenStyle.smudge;
        initSudoKuBoxes();		// default to no boxes
       	switch(rules)
       	{
       	case Universe:
           	UniverseChip.clearSudokuValues();
           	flippable = FlipStyle.allowed;
            UniverseChip.tweenStyle = TweenStyle.smudge;
      		switch(np)
       		{
      		case 1:
      		case 2: np = 4;	// change default 2 to 4
				//$FALL-THROUGH$
			case 4: initBoard(universe_n_in_col,universe_first_in_col); //this sets up the board and cross links
       			boardRows = boardColumns = universe_n_in_col.length;
       			break;
       		case 3:
       			initBoard(universe_3n_in_col,universe_3first_in_col); //this sets up the board and cross links
       			boardRows = boardColumns = universe_3n_in_col.length;
       			break;
       		default: throw G.Error("Not expecting %d players",np);
       		}
       		for(UniverseCell c = allCells; c!=null; c=c.next)
       		{
       			if(! (((c.col>='D') && (c.col<('D'+10))) 
       					&& ((c.row>=4) && (c.row<14)))) { c.cellImageIndex = DARKGRAY_INDEX; }
       		}
       		break;
       	case Pan_Kai:
           	UniverseChip.clearSudokuValues();
            UniverseChip.tweenStyle = TweenStyle.smudge;
           	flippable = FlipStyle.allowed;
            UniverseChip.tweenStyle = TweenStyle.smudge;
       		initBoard(10,10);
       		np = 2;
       		boardRows = boardColumns = 10;
       		break;
       	case Phlip:
           	UniverseChip.clearSudokuValues();
            UniverseChip.tweenStyle = TweenStyle.smudge;
           	flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.smudge;
       		initBoard(9,9);
       		chipOffset = UniverseChip.DOMAIN_PIECE_OFFSET;
       		rackOffset = 0;
       		np = 2;
       		boardRows = boardColumns = 9;
       		break;
		case PolySolver_8x8:
			initBoard(8,8);
			np = 2;
			boardRows = boardColumns = 8;
			flippable = FlipStyle.allowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
	       	UniverseChip.clearSudokuValues();
	       	break;
		case PolySolver_6x6:
	       	UniverseChip.clearSudokuValues();
			//$FALL-THROUGH$
		case Nudoku_8x8:
			initBoard(8,8);
			flippable = FlipStyle.allowed;
			UniverseChip.tweenStyle = TweenStyle.barbell;
       		np = 1;
       		boardRows = boardColumns = 8;
       		initSudokuBoxes(4,2);
			break;
		case Nudoku_6x6:
       		initBoard(6,6);
			flippable = FlipStyle.allowed;
			UniverseChip.tweenStyle = TweenStyle.barbell;
       		np = 1;
       		boardRows = boardColumns = 6;
       		initSudokuBoxes(3,2);
       		break;
      	case PolySolver_9x9:
      		flippable = FlipStyle.allowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
           	UniverseChip.clearSudokuValues();
            UniverseChip.tweenStyle = TweenStyle.barbell;
            UniverseChip.tweenStyle = TweenStyle.barbell;
       		initBoard(9,9);
       		np = 1;
       		boardRows = boardColumns = 9;
       		initSudokuBoxes(3,3);
       		break;

		case Nudoku_9x9:
			flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
            UniverseChip.tweenStyle = TweenStyle.barbell;
       		initBoard(9,9);
       		np = 1;
       		boardRows = boardColumns = 9;
       		initSudokuBoxes(3,3);
       		break;
		case Nudoku_6_Box:
		case Nudoku_12:
      		initBoard(6,6);
      		flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
       		np = 1;
       		boardRows = boardColumns = 6;
       		initSudokuBoxes(3,2);
       		break;
		case Nudoku_11:
			{
			String sBoxes[] = { "0 A 1 A 2 A 3 B 1 B 2 B 3",  "1 C 1 C 2 C 3 D 1 D 2 E 1", "0 D 3 E 2 E 3 F 1 F 2 F 3",
								"3 A 4 A 5 A 6 B 4 B 5 C 4",  "2 B 6 C 5 C 6 D 4 D 5 D 6", "3 E 4 E 5 E 6 F 4 F 5 F 6"};
     		initBoard(6,6);
     		flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
          	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
       		np = 1;
       		boardRows = boardColumns = 6;
       		initSudokuAny(sBoxes);
			}
       		break;
		case Nudoku_5_Box:
       	case Nudoku_10:
			{
			Rectangle Ten[] = { new Rectangle(1,1,2,3), new Rectangle(3,1,2,3),new Rectangle(5,1,2,3),
								   new Rectangle(1,4,3,2), new Rectangle(4,4,3,2)};
			int Tenbox[] = { 0, 2, 0, 1, 3}; 
			flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
			initBoard(6,5);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
       		np = 1;
       		boardRows = 5;
       		boardColumns = 6;
       		initSudokuRects(Ten,Tenbox);
       		}
			break;
		case Nudoku_9:
			{
			Rectangle Nine[] = { new Rectangle(1,1,2,2), new Rectangle(3,1,3,2),
								  new Rectangle(1,3,3,2), new Rectangle(4,3,2,2),
								  new Rectangle(1,5,2,2), new Rectangle(3,5,3,2)};
			int nineBoxes[] = { 0, 1, 2 ,3, 0 ,1 };
			flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
			initBoard(5,6);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
       		np = 1;
       		boardRows = 6;
       		boardColumns = 5;
       		initSudokuRects(Nine,nineBoxes);
       		}
			break;
		case Nudoku_8:
			{
			Rectangle Eight[] = { new Rectangle(1,1,2,3), new Rectangle(3,1,1,3), new Rectangle(4,1,2,3),
								  new Rectangle(1,4,2,2), new Rectangle(3,4,1,2), new Rectangle(4,4,2,2)};
			int eightBoxes[] = { 0, 1, 0, 1,0, 1};
			flippable = FlipStyle.notallowed;
			initBoard(5,5);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
            UniverseChip.tweenStyle = TweenStyle.barbell;
       		np = 1;
       		boardRows = 5;
       		boardColumns = 5;
       		initSudokuRects(Eight,eightBoxes);
       		}
			break;
		case Nudoku_7:
			{
			Rectangle Seven[] = { new Rectangle(1,1,3,2), new Rectangle(4,1,2,2),
									new Rectangle(1,3,2,2), new Rectangle(3,3,3,2)};
			int sevenBoxes[] = { 0,2,2,1};
			initBoard(5,4);
			flippable = FlipStyle.notallowed;
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
            UniverseChip.tweenStyle = TweenStyle.barbell;
      		np = 1;
       		boardRows = 4;
       		boardColumns = 5;
       		initSudokuRects(Seven,sevenBoxes);
       		}
			break;
		case Nudoku_6:
      		initBoard(4,4);
           	rackOffset = 0;
           	flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
       		np = 1;
       		boardRows = boardColumns = 4;
       		initSudokuBoxes(2,2);
       		break;
       		
		case Nudoku_4_Box:	// 3 x 2 in 2x2
			initBoard(6,4);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
           	flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
      		np = 1;
       		boardRows = 4;
       		boardColumns = 6;
       		initSudokuBoxes(3,2);
       		break;
   		case Nudoku_3_Box:	// 3 x 2 boxes 3 high
    		initBoard(3,6);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
           	flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
      		np = 1;
       		boardRows = 6;
       		boardColumns = 3;
       		initSudokuBoxes(3,2);
       		break;

		case Nudoku_2_Box:
		case Nudoku_5:
     		initBoard(3,4);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
           	flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
      		np = 1;
       		boardRows = 4;
       		boardColumns = 3;
       		initSudokuBoxes(3,2);
       		break;
		case Nudoku_4:
			{
			Rectangle Four[] = { new Rectangle(1,1,3,1), new Rectangle(1,2,3,2)};
			int fourBoxes[] = { 0,1};
      		initBoard(3,3);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
           	flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
       		np = 1;
       		boardRows = boardColumns = 3;
       		initSudokuRects(Four,fourBoxes);
			}
			break;
		case Nudoku_3:
     		initBoard(4,2);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
           	flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
      		np = 1;
       		boardRows = 2;
       		boardColumns = 4;
       		initSudokuBoxes(2,2);
       		break;
		case Nudoku_1_Box:
		case Nudoku_2:
     		initBoard(3,2);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
           	flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
       		np = 1;
       		boardRows = 2;
       		boardColumns = 3;
       		initSudokuBoxes(3,2);
			break;
		case Sevens_7:
			{
     		initBoard(7,7);
           	rackOffset = 0;
           	chipOffset = 0;
           	flippable = FlipStyle.allowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
       		np = 1;
       		boardRows = boardColumns = 7;
       		restoreSudokuValues(svals);
       		}
 			break;
		case Nudoku_1:
     		initBoard(2,2);
           	rackOffset = 0;
           	chipOffset = UniverseChip.SNAKES_PIECE_OFFSET;
           	flippable = FlipStyle.notallowed;
            UniverseChip.tweenStyle = TweenStyle.barbell;
       		np = 1;
       		boardRows = boardColumns = 2;
       		initSudokuBoxes(2,2);
			break;
			
  		case Blokus:
 		case Blokus_Classic:
      		initBoard(20,20);
      		getCell('A',1).makeStart(DARKGRAY_INDEX);
      		getCell('A',20).makeStart(DARKGRAY_INDEX);
      		getCell('T',1).makeStart(DARKGRAY_INDEX);
     		getCell('T',20).makeStart(DARKGRAY_INDEX);
     		boardRows = boardColumns = 20;
     		break;
     		
		case Diagonal_Blocks_Classic:
       	case Diagonal_Blocks:
           	UniverseChip.clearSudokuValues();
           	flippable = FlipStyle.allowed;
            UniverseChip.tweenStyle = TweenStyle.smudge;
       		switch(np)
       		{
       		case 3:
       			initBoard(diagonal_blocks_3_n_in_col,diagonal_blocks_3_first_in_col); //this sets up the board and cross links
       			boardRows = boardColumns = diagonal_blocks_3_n_in_col.length;
       			getCell('H',13).makeStart(DARKGRAY_INDEX);
       			getCell('F',6).makeStart(DARKGRAY_INDEX);
       			getCell('M',8).makeStart(DARKGRAY_INDEX);
       			boardRows = boardColumns = 17;
       			break;
       		case 2:
       		case 1:
       		case 4:
                initBoard(diagonal_blocks_4_n_in_col,diagonal_blocks_4_first_in_col); //this sets up the board and cross links
       			boardRows = boardColumns = diagonal_blocks_4_n_in_col.length;
       			getCell('F',15).makeStart(DARKGRAY_INDEX);
       			getCell('F',6).makeStart(DARKGRAY_INDEX);
       			getCell('O',6).makeStart(DARKGRAY_INDEX);
       			getCell('O',15).makeStart(DARKGRAY_INDEX);
       			boardRows = boardColumns = 20;
       			break;
			default:
				break;

       		}
       		
       		break;
       	case Blokus_Duo:
           	UniverseChip.clearSudokuValues();
           	flippable = FlipStyle.allowed;
            UniverseChip.tweenStyle = TweenStyle.smudge;
      		np = 2;
            initBoard(14,14); //this sets up the board and cross links
   			boardRows = boardColumns = 14;
   			getCell('E',10).makeStart(DARKGRAY_INDEX);
   			getCell('J',5).makeStart(DARKGRAY_INDEX);
       		break;

       	case Diagonal_Blocks_Duo:
           	UniverseChip.clearSudokuValues();
           	flippable = FlipStyle.allowed;
            UniverseChip.tweenStyle = TweenStyle.smudge;
      		np = 2;
            initBoard(diagonal_blocks_duo_n_in_col,diagonal_blocks_duo_first_in_col); //this sets up the board and cross links
   			boardRows = boardColumns = diagonal_blocks_duo_n_in_col.length;
   			getCell('E',5).makeStart(DARKGRAY_INDEX);
   			getCell('J',10).makeStart(DARKGRAY_INDEX);
       		break;
       	default:
       		throw G.Error("Variation %s not handled",rules);
       	}
        players_in_game = np;
       	int map[]=getColorMap();
       	playerColor = new ChipColor[np];
       	for(int i=0;i<np;i++) { playerColor[i]=ChipColor.values()[map[i]];}
    	Random r = new Random(67246765);
        occupiedCells = new CellStack[np];
        diagonalPointsForPlayer = new CellStack[np];
        diagonalPointsForPlayerValid = new boolean[np];
	    topsForPlayer = new int[np];
	    estScores = new double[np];
    	rack = new UniverseCell[np][racksize];
    	chipLocation = new UniverseCell[np][racksize];
    	allDone = new boolean[np];
    	win = new boolean[np];

        for(int i=0;i<np;i++)
        { occupiedCells[i] = new CellStack();
          diagonalPointsForPlayer[i] = new CellStack();
        }
        cachedMovesValid = false;
        invalidateDiagonalPointsCache();
	
	    estScoresValid = false;
     	givenRack = new UniverseCell[10];
    	for(int i=0;i<givenRack.length;i++)
    	{
    		givenRack[i]=new UniverseCell(r,UniverseId.GivensRack,GIVENS_COLUMN,i);
    		givenRack[i].setGiven(UniverseChip.getGiven(i));
    	}
    	
       	takenRack = new UniverseCell[racksize];
       	for(int chip = 0; chip<racksize; chip++)
			{	UniverseCell c = takenRack[chip] = new UniverseCell(r,UniverseId.TakensRack,TAKENS_COLUMN,chip);
				c.onBoard = false;
			}
    	
    	cachedMovesValid = false;
    	lastMove = null;
	    AR.setValue(numberOfSize,0);
	    givens.clear();
    	pickedCell = new UniverseCell(r,UniverseId.PickedCell);
    	for(int pl=0;pl<np; pl++)
     		{	for(int chip = 0; chip<racksize; chip++)
     			{	UniverseCell c = rack[pl][chip] = new UniverseCell(r,UniverseId.ChipRack,(char)('A'+pl),chip);
     				c.onBoard = false;
     				int offset = chip+chipOffset;
     				UniverseChip ch = UniverseChip.getChip(playerColor[pl],offset);
     				addChip(c,ch);
     				numberOfSize[ch.patternSize()]++;
     				chipLocation[pl][chip] = c;
     			}
     		}
  
	    setState(UniverseState.PUZZLE_STATE);
	    
	    // initialize the region map for universe and pan-kai and polysolver
	    
	    emptySpaces = fullBoardSize = createUniverseRegions();
	    sweep_counter = 0;
	    consecutivePasses = 0;
	    whoseTurn = 0;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		flippedCells.clear();
		undoStack.clear();
		pickedObject = null;
		originalPickedObject = null;
        allCells.setDigestChain(r);
        moveNumber = 1;
    }

    private int getNextPlayer(int pl)
    {
    	return((pl+1)%players_in_game);
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
        case PASS_STATE:
        case CONFIRM_STATE:
        case CONFIRM_SWAP_STATE:
        case RESIGN_STATE:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(getNextPlayer(whoseTurn));
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
        {case RESIGN_STATE:
         case CONFIRM_SWAP_STATE:
         case PASS_STATE:
         case CONFIRM_STATE:
             return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent)
    {	setState(UniverseState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==UniverseState.GAMEOVER_STATE) { return(win[player]); }
    	return(false);
    }
    // estimate the value of the board position.
    public double ScoreForPolySolverPlayer(int player,boolean print)
    {  	G.Assert(players_in_game==1, "not intended for multiplayer games");
    	return(-emptySpaces);
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public UniverseCell acceptPlacement()
    {	lastMove = droppedDestStack.top();
        pickedObject = null;
        originalPickedObject = null;
        droppedDestStack.clear();
        pickedSourceStack.clear();
        cachedMovesValid = false;
		estScoresValid = false;
		return(lastMove);
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	UniverseCell dr = droppedDestStack.pop();
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
	   		case GivensRack:
	   			G.print("undrop");
	   			break;
			case BoardLocation: 
				// adjust the regions before removing the chip
			{
      			invalidateDiagonalPointsCache();
				if(dr.onBoard) 
					{ 
					switch(rules)
					{
	    			case Nudoku_6x6:
	    			case Nudoku_9x9:
	    			case Nudoku_12:
	        		case Nudoku_11:
	        		case Nudoku_10:
	        		case Nudoku_9:
	        		case Nudoku_8:
	        		case Nudoku_7:
	        		case Nudoku_6:
	        		case Nudoku_5:
	        		case Nudoku_4:
	        		case Nudoku_3:
	        		case Nudoku_2:
	        		case Nudoku_1_Box:
	        		case Nudoku_2_Box:
	           		case Nudoku_3_Box:
	        		case Nudoku_4_Box:	// 3 x 2 in 2x2
	           		case Nudoku_5_Box:
	        		case Nudoku_6_Box:
	        		case Nudoku_1:
	    			case PolySolver_9x9:
	    			case PolySolver_6x6:
	    			case Phlip:
	    			case Sevens_7:
	    			case Diagonal_Blocks:
	    			case Diagonal_Blocks_Duo:
	         		case Diagonal_Blocks_Classic:
	         		case Blokus:
	         		case Blokus_Classic:
	    			case Blokus_Duo:
	    					{
    						pickedObject = removeChip(dr); 
	    					int size = pickedObject.patternSize();
	    					numberOfSize[size]++;
	    					}
	    					break;
	    			case Universe:
					case Pan_Kai:
						increaseUniverseRegionSize(dr);
						pickedObject = removeChip(dr); 
						break;
					default: throw G.Error("not expecting %s",rules);
					}
				}
				
				addChip(pickedCell,pickedObject);
				
				break;
			}
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base undoInfo for the move
    //
    private void unPickObject()
    {	UniverseChip po = pickedObject;
    	if(po!=null)
    	{
    		UniverseCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case GivensRack:
    			break;
     		case ChipRack:
     			if(board_state==UniverseState.PUZZLE_STATE) { originalPickedObject = po; } 
				//$FALL-THROUGH$
			case BoardLocation: 
       			{
       			if(originalPickedObject!=null 
       					&& (revision>=101 || (rules==variation.Diagonal_Blocks_Duo))) 
       				{ po = originalPickedObject; 
       				}
       			invalidateDiagonalPointsCache();
       			removeChip(pickedCell);
       			if(po.isGiven()) { ps.setGiven(po); }
       			else { addChip(ps,po); }; 
     			switch(rules)
    			{
    			case Universe:
    			case Pan_Kai:
    				if(ps.onBoard) 
    					{ 
    					reduceUniverseRegionSize(ps); 
    					}
    				break;
    			case Nudoku_6x6:
    			case Nudoku_9x9:
    			case Nudoku_8x8:
    			case Nudoku_12:
        		case Nudoku_11:
        		case Nudoku_10:
        		case Nudoku_9:
        		case Nudoku_8:
        		case Nudoku_7:
        		case Nudoku_6:
        		case Nudoku_5:
        		case Nudoku_4:
        		case Nudoku_3:
        		case Nudoku_2:
        		case Nudoku_1:
        		case Nudoku_1_Box:
        		case Nudoku_2_Box:
           		case Nudoku_3_Box:
        		case Nudoku_4_Box:	// 3 x 2 in 2x2
           		case Nudoku_5_Box:
        		case Nudoku_6_Box:
    			case PolySolver_9x9:
    			case PolySolver_6x6:
    			case Phlip:
    			case Sevens_7:
    			case Diagonal_Blocks:
    			case Diagonal_Blocks_Duo:
         		case Diagonal_Blocks_Classic:
         		case Blokus:
         		case Blokus_Classic:
    			case Blokus_Duo:
    				if(ps.onBoard)
    				{
    				  int size = po.patternSize();
  					  numberOfSize[size]--;
    				}
    				break;
    			default: throw G.Error("Not handled");
    			}}

    			break;
    		}
    		originalPickedObject = pickedObject = null;
     	}
     }
    // extend to maximum size from c
    private void sweepRegionExtend(UniverseCell c)
    {	c.sweep_counter = sweep_counter;
    	sweep_size++;
    	for(int direction=0;direction<CELL_FULL_TURN; direction++)
    	{	UniverseCell next = c.exitTo(direction);
    		if(next!=null)
    		{	if(next.sweep_counter==sweep_counter) { }
    			else if(next.topChip()==null) 
    				{ sweepRegionExtend(next); }
    		}
    	}
    }
    // find exactly one region adjavent to C and return it's size,
    // starting from some empty cell adjacent to c or some cell covered by the same piece
    private int sweepRegionMark(UniverseCell c,int sweep)
    {	c.sweep_counter = sweep;
    	for(int direction=0;direction<CELL_FULL_TURN; direction++)
    	{	UniverseCell next = c.exitTo(direction);
    		if(next!=null)
    		{
    		if(next.sweep_counter == sweep) {}
    		else
    		{
    		UniverseChip top = next.topChip();
    		if(top == null) { sweepRegionExtend(next); return(sweep_size); }
    		if(top==c.topChip()) 
    			{ int v = sweepRegionMark(next,sweep);
    			  if(v>0) { return(v); }	// found something
    			}
    		}}
    	}
    	return(sweep_size);
    }
    //
    // set all empty cells from C to the current region number
    private void sweepRegionCreate(UniverseCell c)
    {	universeRegionSize[c.universeRegionNumber]--;
    	c.universeRegionNumber = regionIndex;
    	universeRegionSize[regionIndex]++;
    	c.sweep_counter = sweep_counter;
    	for(int direction=0;direction<CELL_FULL_TURN; direction++)
    	{	UniverseCell next = c.exitTo(direction);
    		if(next!=null)
    		{	if(next.sweep_counter==sweep_counter) { }	// already seen
    			else if(next.topChip()==null)
    				{ sweepRegionCreate(next); }
    		}
    	}
    }
    
    //
    // set all empty cells from C to the current region number
    // no bookkeeping on the old region number
    //
    private void sweepRegionNew(UniverseCell c)
    {	
    	c.universeRegionNumber = regionIndex;
    	universeRegionSize[regionIndex]++;
    	c.sweep_counter = sweep_counter;
    	for(int direction=0;direction<CELL_FULL_TURN; direction++)
    	{	UniverseCell next = c.exitTo(direction);
    		if(next!=null)
    		{	if(next.sweep_counter==sweep_counter) { }	// already seen
    			else if(next.topChip()==null)
    				{ sweepRegionNew(next); }
    		}
    	}
    }
    // make new regions one region adjacent to C and return it's size
    private void sweepRegionNew(UniverseCell c,int ignoreSweep,int newsweep,int baseRegion)
    { 	
    	c.sweep_counter = newsweep;
    	for(int direction=0;direction<CELL_FULL_TURN; direction++)
    	{	UniverseCell next = c.exitTo(direction);
    		if(next!=null)
    		{
    		UniverseChip top = next.topChip();
    		if(next.sweep_counter == newsweep) {}
    		else if(top==c.topChip()) { sweepRegionNew(next,ignoreSweep,newsweep,baseRegion); }	// continue the seed group
    		else if(next.sweep_counter == ignoreSweep) {}				// ignore one region
    		else if(next.universeRegionNumber>baseRegion) {}
    		else if(top==null) 
    			{ 	regionIndex++;
    				sweep_counter++;
    				universeRegionSize[regionIndex]=0;
    				sweepRegionCreate(next); 

    				if(board_state!=UniverseState.PUZZLE_STATE) 
    					{G.Assert(universeRegionSize[regionIndex]>=5, "region too small, size %d",universeRegionSize[regionIndex]);
    					}
    			}
     		}
    	}
    }
    //
    // called when placing a piece.  Initially the whole board is one region
    // the cells covered by the piece keep their region number.
    // when placement causes the board to split, one part keeps the same region number
    // and new regions are created for the rest.
    //
    private void reduceUniverseRegionSize(UniverseCell c)
    {	int region = c.universeRegionNumber;
    	universeRegionSize[region] -= 5;		// all are size 5
    	if(board_state!=UniverseState.PUZZLE_STATE)
    	{
    	if(universeRegionSize[region]>5)
    	{	// check for legal split
    		int ignoreSweep = ++sweep_counter;
    		sweep_size = 0;
    		int sz = sweepRegionMark(c,ignoreSweep);	// count the new size of some adjacent region
    		
    		if(sz!=universeRegionSize[region])					// if the region changed size, we find the remnants and create new regions
    		{	// split, make new regions
    			int rc = regionIndex;
    			sweep_counter++;
    			sweepRegionNew(c,ignoreSweep,sweep_counter,regionIndex);
    			G.Assert(regionIndex!=rc,"didn't make a new region");
    		}
    	}}
    }
    
    // absorb all of the region containing c into "intoRegion"
    private void absorbUniverseRegion(UniverseCell c,int intoRegion)
    {	int oldRegion = c.universeRegionNumber;
    	universeRegionSize[intoRegion]++;
    	universeRegionSize[oldRegion]--;
    	c.universeRegionNumber = intoRegion;
    	for(int direction = 0;direction<CELL_FULL_TURN;direction++)
    	{	UniverseCell next = c.exitTo(direction);
    		if(next!=null)
    		{	if(next.universeRegionNumber==oldRegion) { absorbUniverseRegion(next,intoRegion); }
    		}
    	}
    }
    
    // create the region map from scratch
    private int createUniverseRegions()
    {
    	int fullSize = 0;
	    for(UniverseCell c = allCells; c!=null; c=c.next)
	    	{	fullSize++;
	    		c.universeRegionNumber = 0;
	    		c.sweep_counter = 0;
	    	}
    	AR.setValue(universeRegionSize,0);
    	AR.setValue(topsForPlayer,0);
    	regionIndex = -1;
    	sweep_counter++;
    	for(UniverseCell c = allCells; c!=null; c=c.next)
    	{	UniverseChip top = c.topChip();
       		if(top!=null) { topsForPlayer[getPlayerIndexForColor(top)]++; }
    		else if(c.sweep_counter!=sweep_counter)
    		{	
    			regionIndex++;
    			sweepRegionNew(c);
    		}
    	}
	    return(fullSize);
    }
    // absorb all the regions adjacent to c 
    private void multipleUniverseRegionsAbsorb(UniverseCell c,int newsweep)
    {	UniverseChip top = c.topChip();
    	G.Assert(top!=null,"chip already removed");
    	c.sweep_counter = newsweep;
    	for(int direction=0;direction<CELL_FULL_TURN; direction++)
    	{	
    		UniverseCell next = c.exitTo(direction);
	    	if(next!=null)
	    	{
	    		UniverseChip nextop = next.topChip();
	    		if(next.sweep_counter == newsweep) {}
	    		else if(top==nextop) { multipleUniverseRegionsAbsorb(next,newsweep); }
	    		else if((nextop==null) && (next.universeRegionNumber!=c.universeRegionNumber)) 
	    			{ sweep_size = Math.min(next.universeRegionNumber,sweep_size);
	    			  absorbUniverseRegion(next,c.universeRegionNumber);
	    			}
	    	}
	    }
    }
    // when lifting a piece from the board in normal reverse order, the region
    // number "under" the piece is the surviving region.  The adjacent regions
    // are absorbed into it.
    private void increaseUniverseRegionSize(UniverseCell c)
    {	int region = c.universeRegionNumber;
    	universeRegionSize[region] += 5;		// all are size 5
    	if(board_state!=UniverseState.PUZZLE_STATE)
    	{
    	if(universeRegionSize[region]>5)
    	{	// check for legal split
    		sweep_size = MAX_REGIONS;
    		sweep_counter++;
    		multipleUniverseRegionsAbsorb(c,sweep_counter);	// count the new size of some adjacent region
    		if(sweep_size!=MAX_REGIONS)
    			{ regionIndex = sweep_size-1;
    			}
    	}}
    }
    // 
    // drop the floating object.
    //
    void dropObject(UniverseCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case GivensRack:
			pickedObject = null;
			droppedDestStack.push(c);
			break;
		case ChipRack:
		case TakensRack:
		case BoardLocation: 
			UniverseChip po = pickedObject;
			removeChip(pickedCell);
			pickedObject = null;
			if(po.isGiven()) 
				{ c.setGiven(po); 
				  givens.pushNew(c);
				}
			else 
				{
				addChip(c,po);
			
			switch(rules)
			{
			case PolySolver_6x6:
			case PolySolver_9x9:
			case Nudoku_6x6:
			case Nudoku_9x9:
			case Nudoku_8x8:
			case Nudoku_12:
    		case Nudoku_11:
    		case Nudoku_10:
    		case Nudoku_9:
    		case Nudoku_8:
    		case Nudoku_7:
    		case Nudoku_6:
    		case Nudoku_5:
    		case Nudoku_4:
    		case Nudoku_3:
    		case Nudoku_2:
    		case Nudoku_1_Box:
    		case Nudoku_2_Box:
    		case Nudoku_3_Box:
    		case Nudoku_4_Box:	// 3 x 2 in 2x2
       		case Nudoku_5_Box:
    		case Nudoku_6_Box:
    		case Nudoku_1:
    		case Phlip:
    		case Sevens_7:
    		case Diagonal_Blocks_Duo:
    		case Blokus_Duo:
    		case Diagonal_Blocks:
     		case Diagonal_Blocks_Classic:
     		case Blokus:
     		case Blokus_Classic:
     			if(c.onBoard) 
					{ int size = po.patternSize();
					  numberOfSize[size]--;
					}
				break;
			case Universe:
			case Pan_Kai:
				if(c.onBoard) 
					{ reduceUniverseRegionSize(c); 
					}
				break;
			default: throw G.Error("Not handled");
			}
			break;
		}}
       	droppedDestStack.push(c);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(UniverseCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	UniverseChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.isomerIndex());	// all colors and rotations get a unique id
    		}
        return (NothingMoving);
    }
    
    public UniverseCell getCell(UniverseId source,char col,int row)
    {	
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case PickedCell:
        	return(pickedCell);
        case GivensRack:
        	return(givenRack[row]);
        case TakensRack:
        	return(takenRack[row]);
        case ChipRack:
        	if(col==GIVENS_COLUMN) { return(givenRack[row]); }
        	if(col==TAKENS_COLUMN) { return(takenRack[row]); }
        	return(rack[col-'A'][row]);
        case BoardLocation:
        	return(getCell(col,row));
        }
    }
    public UniverseCell getCell(UniverseCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board chipLocation really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickGiven(UniverseCell c)
    {
    	UniverseChip ch = c.getGiven();
    	G.Assert(ch!=null, "no given found");
    	pickedObject = ch;
    	c.setGiven(null);
    	givens.remove(c,false);
    	pickedSourceStack.push(c);
    	addChip(pickedCell,ch);
    }
    void pickObject(UniverseCell c,int rotation)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case GivensRack:
			pickedObject = c.getGiven();
			addChip(pickedCell,pickedObject);
			break;
			
		case BoardLocation: 
  			invalidateDiagonalPointsCache();
			//$FALL-THROUGH$
		case ChipRack:
		case TakensRack:
			// merge the regions before the chip is removed
			if(c.onBoard) 
				{ 
				switch(rules)
				{
				case PolySolver_6x6:
				case PolySolver_9x9:
				case Nudoku_8x8:
				case Nudoku_6x6:
				case Nudoku_9x9:
				case Nudoku_12:
	    		case Nudoku_11:
	    		case Nudoku_10:
	    		case Nudoku_9:
	    		case Nudoku_8:
	    		case Nudoku_7:
	    		case Nudoku_6:
	    		case Nudoku_5:
	    		case Nudoku_4:
	    		case Nudoku_3:
	    		case Nudoku_2:
	    		case Nudoku_1:
				case Phlip:
	    		case Nudoku_1_Box:
	    		case Nudoku_2_Box:
	       		case Nudoku_3_Box:
	    		case Nudoku_4_Box:	// 3 x 2 in 2x2
	       		case Nudoku_5_Box:
	    		case Nudoku_6_Box:
	    		case Sevens_7:
                case Diagonal_Blocks:
                case Diagonal_Blocks_Duo:
                case Blokus_Duo:
         		case Diagonal_Blocks_Classic:
         		case Blokus:
         		case Blokus_Classic:
         		{
					pickedObject = removeChip(c);
					if(c.onBoard)
						{int size =  pickedObject.patternSize();
						numberOfSize[size]++;
						}
					}
					break;
				case Universe:
				case Pan_Kai:
					{
					increaseUniverseRegionSize(c); 
					pickedObject = removeChip(c);
					}
					break;
				default: throw G.Error("Not handled");
				}}
				else 
				{
				pickedObject = removeChip(c);	
				}
			if(board_state==UniverseState.PUZZLE_STATE) 
				{ originalPickedObject = pickedObject; 
				}
			if(rotation>=0) {pickedObject = pickedObject.getVariation(rotation%4,rotation>=4); }
			addChip(pickedCell,pickedObject);
			if(c.rackLocation==UniverseId.ChipRack) 
				{ originalPickedObject = pickedObject; 
				}

			break;
    	
    	}
    	pickedSourceStack.push(c);
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in undoInfo %s", board_state);
        case CONFIRM_STATE:
         	setNextStateAfterDone(); 
        	break;
        case PLAY_STATE:
        case PLAY_OR_SWAP_STATE:
			setState(UniverseState.CONFIRM_STATE);
			break;

        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(UniverseCell c)
    {	return(getSource()==c);
    }
    public UniverseCell getSource()
    {
    	return((pickedSourceStack.size()>0) ? pickedSourceStack.top() : null);
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
         	setState(UniverseState.PLAY_STATE);
        	break;
        case PLAY_STATE:
        case PLAY_OR_SWAP_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting undoInfo %s",board_state);
    	case GAMEOVER_STATE: 
    		break;

    	case CONFIRM_STATE:
    	case PASS_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		setState(hasLegalUniverseMoves(whoseTurn) ? UniverseState.PLAY_STATE :  UniverseState.PASS_STATE);
    		break;
    	}
    }
    
    // 
    // flip over a chip located at c
    //
    int steps=0;
    private UniverseCell flipAdjacentChip(UniverseCell c,UniverseChip top)
    {	int player = getPlayerIndexForOwner(top);
    	int pattern = top.getPatternIndex()-rackOffset;
    	boolean flip = top.flipped;
    	steps++;
    	UniverseCell loc = chipLocation[player][pattern];	
    	UniverseChip alt = top.getFlippedPattern(!flip);				// flipped and rotated 2 is the one that looks the same
       	Rectangle topBox = top.boundingBox(1,0,0);					// the relative position of the actual chip on the board
    	Rectangle altBox = alt.boundingBox(1,0,0);					// the position of the new chip
    	UniverseCell newCell = getCell((char)(loc.col+G.Left(topBox)-G.Left(altBox)),loc.row+G.Top(altBox)-G.Top(topBox));
       	removeChip(loc);
    	addChip(newCell,alt);
    	return(newCell);
    }
   
    private void flipAdjacent(UniverseCell c,UniverseChip ch,int sweep)
    {
    	c.sweep_counter = sweep;
    	for(int dir = 0; dir<CELL_FULL_TURN; dir++)
    	{
    		UniverseCell next = c.exitTo(dir);
    		if(next!=null)
    		{
    		UniverseChip top = next.topChip();
    		if(next.sweep_counter==sweep) {}
    		else if(top==ch) { flipAdjacent(next,ch,sweep); }
    		else if(top!=null && (top.getColor()!=ch.getColor())) 
    		{ flippedCells.push(flipAdjacentChip(next,top)); }
    		}
    	}
    }
    private void flipAdjacent(UniverseCell c)
    {	sweep_counter++;
    	flipAdjacent(c,c.topChip(),sweep_counter);
    }
    private boolean checkForDiagonalBlocksEndgame(int knownzero)
    {
    	boolean any = false;
		for(int who = 0; !any && (who<players_in_game); who++)
		{	// caches will be valid for whoseTurn
			int test = (who+whoseTurn)%players_in_game;
			any |= (test==knownzero) ? false : hasLegalDiagonalBlocksMoves(test);
		}
		return(!any);
    }
 
    private void doGameOver()
    {
			int max = 0;
			int wins=0;
			for(int i=0,lim=topsForPlayer.length; i<lim; i++) { max = Math.max(max,topsForPlayer[i]); }
			for(int i=0; i<players_in_game; i++) 
			{	if(topsForPlayer[i]==max)
				{wins++;
				 win[i] = true;
				}
			}
			if(wins>1) { for(int i=0; i<players_in_game; i++) { win[i]=false; }}
			setGameOver(true);
    }
    private boolean isSwapState()
    {
    	return((players_in_game==2)
    			&& ((rules==variation.Diagonal_Blocks_Duo)||(rules==variation.Blokus_Duo))
    			&& (topsForPlayer[whoseTurn]==0)
    			&& (topsForPlayer[getNextPlayer(whoseTurn)]>0)
    			&& (playerColor[0]==ChipColor.values()[getColorMap()[0]])// not already swapped
    			 );
    }
    private boolean allAllDone()
    {	for(boolean b : allDone) { if(!b) { return(false); }}
    	return(true);
    }
    
    private void doDone()
    {
    	UniverseCell dest = acceptPlacement();

    	if((board_state==UniverseState.PASS_STATE) &&  (revision>=101) && rules.passIsPermanent())
        {
        	allDone[whoseTurn]=true;
        }
    	
        if (board_state==UniverseState.RESIGN_STATE)
        {	
        	if(players_in_game==2) 
        	{
        		win[whoseTurn] = false;
        		win[getNextPlayer(whoseTurn)] = true;
        	}
        	setGameOver(true);
        }
        else
        {	
        	switch(rules)
        	{
       		case Sevens_7:
       			if(isValidSevens())
       				{ setState(UniverseState.PLAY_STATE);
       				}
       			break;
    		case Nudoku_9x9:
    		case Nudoku_8x8:
    		case Nudoku_6x6:
    		case Nudoku_12:
    		case Nudoku_11:
    		case Nudoku_10:
    		case Nudoku_9:
    		case Nudoku_8:
    		case Nudoku_7:
    		case Nudoku_6:
    		case Nudoku_5:
    		case Nudoku_4:
    		case Nudoku_3:
    		case Nudoku_2:
    		case Nudoku_1:
    		case Nudoku_1_Box:
    		case Nudoku_2_Box:
       		case Nudoku_3_Box:
    		case Nudoku_4_Box:
    		case Nudoku_5_Box:
    		case Nudoku_6_Box:
  			if(!isValidSudoku())
    				{ setState(UniverseState.PLAY_STATE);  
    				break;
    				}
				//$FALL-THROUGH$
			case PolySolver_6x6:
    		case PolySolver_9x9:
	            {
	            if(emptySpaces==0)	{ win[whoseTurn]=true; setGameOver(true); }
	            else { setState(UniverseState.PLAY_STATE); }
	            }
	        		break;
     		case Diagonal_Blocks_Classic:
     		case Blokus:
     		case Blokus_Classic:
        	case Diagonal_Blocks:
        	case Diagonal_Blocks_Duo:
        	case Blokus_Duo:
        		{
        		setNextPlayer();
        		if((rules!=variation.Blokus_Duo) && isSwapState())
        		{
        			setState(UniverseState.PLAY_OR_SWAP_STATE);
        		}
        		else
        		{
        		boolean more = hasLegalDiagonalBlocksMoves(whoseTurn);

        		if(more) 
        			{ setState(UniverseState.PLAY_STATE); 
        			}
        			else if (checkForDiagonalBlocksEndgame(-1))
        			{
        				doGameOver();
        			}
        			else if(allDone[whoseTurn])
        			{	while(allDone[whoseTurn]) { setNextPlayer(); }
        				setNextStateAfterDone();
        			}
        			else { setState(UniverseState.PASS_STATE); 
        			}
        		}}
        		break;
        	default:
        		throw G.Error("Not handled");
        	case Phlip:
        		{	if(dest!=null) { flipAdjacent(dest); }
        			boolean more = hasLegalPhlipMoves(whoseTurn);
        			setNextPlayer();
        			boolean some = hasLegalPhlipMoves(whoseTurn);
        			if(some) { setState(UniverseState.PLAY_STATE); }
        			else if(!allAllDone() && allDone[whoseTurn])
        			{
        				while(allDone[whoseTurn]) { setNextPlayer(); }
        				setNextStateAfterDone();
        			}
        			else if(more)
        				{ setState(UniverseState.PASS_STATE); 
        				}
        			else 
        			{	
        				win[0] = topsForPlayer[0]>topsForPlayer[1];
        				win[1] = topsForPlayer[1]>topsForPlayer[0];
        				setGameOver(true);
        			}
        		}
        		break;
        	case Universe:
        	case Pan_Kai:
	        	{	boolean some=false;
	        		boolean ido = false;
	        		for(int i=0;i<players_in_game && !some;i++)
	        	
	        	{	if(i==whoseTurn) { ido = hasLegalUniverseMoves(i); }
	        		else some |= hasLegalUniverseMoves(i);
	        	}
	        	if(!some) { setGameOver(true); if(ido) {win[whoseTurn]=true;}}
	        	else {setNextPlayer(); setNextStateAfterDone(); }}
        	}
        }
    }
    
    private void doRobotDone()
    {
    	UniverseCell dest = acceptPlacement();

        if (board_state==UniverseState.RESIGN_STATE)
        {	
        	if(players_in_game==2) 
        	{
        		win[whoseTurn] = false;
        		win[getNextPlayer(whoseTurn)] = true;
        	}
        	setGameOver(true);
        }
        else
        {	
        	switch(rules)
        	{
       		case Sevens_7:
       			if(isValidSevens())
       				{ setState(UniverseState.PLAY_STATE);
       				}
       			break;
    		case Nudoku_9x9:
    		case Nudoku_6x6:
    		case Nudoku_12:
    		case Nudoku_11:
    		case Nudoku_10:
    		case Nudoku_9:
    		case Nudoku_8:
    		case Nudoku_7:
    		case Nudoku_6:
    		case Nudoku_5:
    		case Nudoku_4:
    		case Nudoku_3:
    		case Nudoku_2:
    		case Nudoku_1:
    		case Nudoku_1_Box:
    		case Nudoku_2_Box:
       		case Nudoku_3_Box:
    		case Nudoku_4_Box:
    		case Nudoku_5_Box:
    		case Nudoku_6_Box:
  			if(!isValidSudoku())
    				{ setState(UniverseState.PLAY_STATE);  
    				break;
    				}
				//$FALL-THROUGH$
			case PolySolver_6x6:
    		case PolySolver_9x9:
	            {
	            if(emptySpaces==0)	{ win[whoseTurn]=true; setGameOver(true); }
	            else { setState(UniverseState.PLAY_STATE); }
	            }
	        		break;
        	case Diagonal_Blocks:
        	case Diagonal_Blocks_Duo:
        	case Blokus_Duo:
     		case Diagonal_Blocks_Classic:
     		case Blokus:
     		case Blokus_Classic:     		
        		{
        		setNextPlayer();
        		if(isSwapState())
        		{
        			setState(UniverseState.PLAY_OR_SWAP_STATE);
        		}
        		else if(consecutivePasses>=2)
        		{
        			setGameOver(true);
        		}
        		else
        		{
        			setState(UniverseState.PLAY_STATE); 	// defer endgame test until the move generator fails
        		}
        		}
        		break;
        	default:
        		throw G.Error("Not handled");
         	case Phlip:
        		{	if(dest!=null) { flipAdjacent(dest); }
        			boolean more = hasLegalPhlipMoves(whoseTurn);
        			setNextPlayer();
        			boolean some = hasLegalPhlipMoves(whoseTurn);
        			if(some) { setState(UniverseState.PLAY_STATE); }
        			else if(more)
        				{ setState(UniverseState.PASS_STATE); 
        				}
        			else 
        			{	
        				win[0] = topsForPlayer[0]>topsForPlayer[1];
        				win[1] = topsForPlayer[1]>topsForPlayer[0];
        				setGameOver(true);
        			}
        		}
        		break;
        	case Universe:
        	case Pan_Kai:
	        	{	boolean some=false;
	        		for(int i=0;i<players_in_game && !some;i++)
	        	
	        	{	if(i==whoseTurn) {}
	        		else some |= hasLegalUniverseMoves(i);
	        	}
	        	if(!some) { setGameOver(true); win[whoseTurn]=true;}
	        	else {setNextPlayer(); setNextStateAfterDone(); }}
        	}
        }
    }

    private void doSwap()
    {	ChipColor pp = playerColor[0];
    	playerColor[0]=playerColor[1];
    	playerColor[1]=pp;
    	// swap the contents but not the cells themselves
    	UniverseCell r0[] = rack[0];
    	UniverseCell r1[] = rack[1];
    	UniverseCell l0[] = chipLocation[0];
    	UniverseCell l1[] = chipLocation[1];
    	for(int lim=r0.length-1; lim>=0; lim--)
    	{
    		UniverseCell c0 = r0[lim];
    		UniverseCell c1 = r1[lim];
    		UniverseCell loc0 = l0[lim];
    		UniverseCell loc1 = l1[lim];
    		UniverseChip t0 = c0.isEmpty() ? null : c0.removeTop();
    		UniverseChip t1 = c1.isEmpty() ? null : c1.removeTop();
    		if(t1!=null) { c0.addChip(t1); }
    		if(t0!=null) { c1.addChip(t0); }
    		l0[lim] = loc1;
    		l1[lim] = loc0;
    	
    	}
    	CellStack cc = occupiedCells[0];
    	occupiedCells[0] = occupiedCells[1];
    	occupiedCells[1] = cc;
    	
    	int tp = topsForPlayer[0];
    	topsForPlayer[0]=topsForPlayer[1];
    	topsForPlayer[1]=tp;
    	
    	invalidateDiagonalPointsCache();
    	cachedMovesValid = false;
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	UniverseMovespec m = (UniverseMovespec)mm;
        //G.print("E Tops "+regionIndex+" "+m);
        //G.print("E "+m);
        switch (m.op)
        {
        case MOVE_ALLDONE:
        	allDone[whoseTurn] = true;
        	doDone();
        	break;
        case MOVE_DONE:
         	doDone();

            break;
        case MOVE_GAMEOVER:
        	doGameOver();
        	break;
        case MOVE_SWAP:
        	doSwap();
        	consecutivePasses = 0;
        	setState((board_state==UniverseState.CONFIRM_SWAP_STATE) ? UniverseState.PLAY_OR_SWAP_STATE : UniverseState.CONFIRM_SWAP_STATE);
        	break;
        case MOVE_RACK_BOARD:
        	consecutivePasses = 0;
           	switch(board_state)
        	{	default: throw G.Error("Not expecting state %s",board_state);
        		case PUZZLE_STATE:
        		case PLAY_OR_SWAP_STATE:
        		case PLAY_STATE:
        			{G.Assert(pickedObject==null,"something is moving");
        			UniverseCell src = revision<101
        					? getCell(UniverseId.ChipRack, (char)('A'+m.player), m.from_row)
        					: getCell(UniverseId.ChipRack, m.from_col, m.from_row);
        			undoInfo = src.topChip().isomerIndex();		// save for robot undo
                    pickObject(src,m.rotation);
                    m.chip = pickedObject;
                    UniverseCell dest = getCell(UniverseId.BoardLocation,m.to_col,m.to_row);
                    dropObject(dest); 
                    setNextStateAfterDrop();
                    if(replay.animate)
                    {
        			animationStack.push(src);
        			animationStack.push(dest);
                    }
                     switch(rules)
                    {
        			case Nudoku_6x6:
        			case Nudoku_9x9:
        			case Nudoku_12:
            		case Nudoku_11:
            		case Nudoku_10:
            		case Nudoku_9:
            		case Nudoku_8:
            		case Nudoku_7:
            		case Nudoku_6:
            		case Nudoku_5:
            		case Nudoku_4:
            		case Nudoku_3:
            		case Nudoku_1_Box:
            		case Nudoku_2_Box:
               		case Nudoku_3_Box:
            		case Nudoku_4_Box:	// 3 x 2 in 2x2
               		case Nudoku_5_Box:
            		case Nudoku_6_Box:
            		case Nudoku_2:
            		case Nudoku_1:
                    case PolySolver_9x9:
                    case PolySolver_6x6:
                    case Sevens_7:
                    	doDone();
                    	break;
                    case Phlip:
                    case Universe:
                    case Pan_Kai:
                    case Diagonal_Blocks:
                    case Diagonal_Blocks_Duo:
                    case Blokus_Duo:
             		case Diagonal_Blocks_Classic:
             		case Blokus:
             		case Blokus_Classic:
             			break;
                    default: throw G.Error("Not handled");
              
        			}
        		}
                    break;
        	}
        	break;
        case MOVE_DROPB:
			{
			UniverseCell c = getCell(UniverseId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
        	consecutivePasses = 0;
            if(isSource(c) && (pickedObject==originalPickedObject)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
          		  	m.chip = pickedObject;
            		dropObject(c);
            		setNextStateAfterDrop();
            		if(replay==replayMode.Single)
            			{
            			animationStack.push(getSource());
            			animationStack.push(c);
            			}
            		}
			}
            break;
        case MOVE_PICKGIVEN:
        	consecutivePasses = 0;
        	{UniverseCell c = getCell(UniverseId.BoardLocation,m.from_col, m.from_row);
        	 pickGiven(c); 
        	 m.chip = pickedObject;
        	}
        	break;
        case MOVE_PICKB:
        	consecutivePasses = 0;
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	G.Assert(pickedObject==null,"something already moving");
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(UniverseState.PLAY_STATE);
        		}
        	else 
        		{ UniverseCell c = getCell(UniverseId.BoardLocation,m.from_col, m.from_row);
        		  pickObject(c,-1);
        		  m.chip = pickedObject;
         		}
 
            break;
            
        case MOVE_ROTATE_CW:
        	consecutivePasses = 0;
        	{	UniverseCell c = getCell(UniverseId.PickedCell,m.from_col,m.from_row);
         		UniverseChip ch = removeChip(c);
        		UniverseChip ch2 = ch.nextRotation();
        		pickedObject = ch2;
        		m.chip = pickedObject;
        		addChip(c,ch2);
        	}
        	break;
            
        case MOVE_ROTATE_CCW:
        	consecutivePasses = 0;
        	{	UniverseCell c = getCell(UniverseId.PickedCell,m.from_col,m.from_row);
         		UniverseChip ch = removeChip(c);
        		UniverseChip ch2 = ch.prevRotation();
        		pickedObject = ch2;
        		m.chip = pickedObject;

        		addChip(c,ch2);
        	}
        	break;
        case MOVE_FLIP:
        	consecutivePasses = 0;
        	{	UniverseCell c = getCell(UniverseId.PickedCell,m.from_col,m.from_row);
         		UniverseChip ch = removeChip(c);
        		UniverseChip ch2 = ch.flip();
        		pickedObject = ch2;
        		m.chip = pickedObject;
        		addChip(c,ch2);
        	}
        	break;
        case MOVE_DROP: // drop on chip pool;
        	consecutivePasses = 0;
        	{
        	UniverseCell c = revision<101 
        						? getCell(UniverseId.ChipRack,(char)('A'+m.player), m.to_row)
        						: getCell(UniverseId.ChipRack, m.to_col, m.to_row);
            if(isSource(c)) { unPickObject(); }
            else 
            	{ 
            	m.chip = pickedObject;
            	dropObject(c); 
            	setNextStateAfterDrop();
            	if(replay==replayMode.Single)
            	{
    			animationStack.push(getSource());
    			animationStack.push(c);
            	}
            	}
        	}
            break;
        case MOVE_PASS:
        	consecutivePasses++;
        	setState(UniverseState.CONFIRM_STATE);
        	break;
        case MOVE_PICK:
        	consecutivePasses = 0;
        	{
        	UniverseCell c = revision<101 
        			? getCell(UniverseId.ChipRack, (char)('A'+m.player), m.from_row)
        			: getCell(UniverseId.ChipRack, m.from_col, m.from_row);
            pickObject(c,-1);
  		 	m.chip = pickedObject;
           setNextStateAfterPick();
        	}
            break;

        case MOVE_UNPICK:
        	consecutivePasses = 0;
        	{
        		if(pickedObject!=null) 
        			{ 
        			  unPickObject();
        			}
        		break;
        	}
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            createUniverseRegions();
            
            // standardize the gameover undoInfo.  Particularly importing if the
            // sequence in a game is resign/start
            setState(UniverseState.PUZZLE_STATE);
            doDone();

            break;

        case MOVE_RESIGN:
        	unPickObject();
            setState(unresign==null?UniverseState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
    		createUniverseRegions();
            // standardize "gameover" is not true
            setState(UniverseState.PUZZLE_STATE);
 
            break;
        case MOVE_LINK:
        	{
        	UniverseCell from = getCell(UniverseId.BoardLocation,m.from_col,m.from_row);
        	UniverseCell to = getCell(UniverseId.BoardLocation,m.to_col,m.to_row);
        	from.nextInBox = to;
        	if(isValidSevens() && allSevensLinked()) 
        		{ win[whoseTurn] = true; 
        			setGameOver(true); ; 
        		}
        	}
        break;
        case MOVE_ASSIGN:
        	switch(rules)
        	{
        	default: throw G.Error("Not expecting rules %s",rules);
        	case Sevens_7:
        		{
        		restoreSudokuValues(m.assignedValues);
        		}
        		break;
     		case Nudoku_9x9:
    		case Nudoku_6x6:
    		case Nudoku_12:
    		case Nudoku_11:
    		case Nudoku_10:
    		case Nudoku_9:
    		case Nudoku_8:
    		case Nudoku_7:
    		case Nudoku_6:
    		case Nudoku_5:
    		case Nudoku_4:
    		case Nudoku_3:
    		case Nudoku_2:
    		case Nudoku_1:
     		case PolySolver_6x6:
    		case PolySolver_9x9:
	        	{
	        		UniverseCell src = getCell(UniverseId.ChipRack,m.from_col,m.from_row);
	        		UniverseChip top = UniverseChip.getChip(chipOffset+src.row);
	        		if(reassignSudoku) { UniverseChip.clearSudokuValues(); reassignSudoku = false; }
	        		if(top!=null) 
	        			{ top.assignSudokuValues(m.assignedValues);
	        			}
	        	}}
        	break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(UniverseState.GAMEOVER_STATE);
			break;
         default:
        	 cantExecute(m);
        }

        //System.out.println("E "+m+" after "+ chipLocation[0][0]);
        //G.print("X Tops "+regionIndex+" "+m);

        return (true);
    }
    public boolean LegalToFlip()
    {
    	switch(flippable)
    	{
    	case notallowed: return(board_state==UniverseState.PUZZLE_STATE);
    	case allowed: return(true);
    	default: throw G.Error("Not handled");
    	}
    }
    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case CONFIRM_STATE:	
         case PASS_STATE:
         case RESIGN_STATE:
         case CONFIRM_SWAP_STATE:
        	 return(false);
         case PLAY_STATE: 
         case PLAY_OR_SWAP_STATE:
        	return((pickedObject==null)
        			?(player==whoseTurn)
        			:((droppedDestStack.size()==0) 
        					&& (pickedSourceStack.top().onBoard==false)
        					&&(playerColor[player]==pickedObject.color)));


		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)
        				?true
        				:(playerColor[player]==pickedObject.color));
        }
    }
  

    // c is a seed cell for the current trial placement.  
    // "seed" is the sweep value that marks seed cells to be placed
    // "used seed" is the swee value that will mark seed cells we've evaluated.
    //
    // the task is to check adjacent empty areas and make sure they are big enough, or
    // are non-split reductions of bigger areas.
    //
    private boolean sweep_universe_start(UniverseCell c,int seed,int used_seed)
    {	c.sweep_counter = used_seed;		// prevent revisiting
    	for(int direction = 0;direction<CELL_FULL_TURN; direction++)
    	{	UniverseCell next = c.exitTo(direction);
    		if(next!=null)
    		{
    	   	if(next.sweep_counter >= used_seed) {} 	// already processed
    		else if(next.topChip()!=null) {}				// not empty, not interesting
    		else if(seed==next.sweep_counter) 
    			{	if(!sweep_universe_start(next,seed,used_seed)) 
    					{ return(false); };	}		// another cell in the pattern
    		else 
    		{	// a potentially interesting cell.  get the size of the area
   				sweep_size = 0;
   				sweep_counter++;
    			sweep_extend(next,seed,used_seed);
    			if(sweep_size<5)
    				{	// potentially a too-small area
    					int original_region_size = universeRegionSize[c.universeRegionNumber];		// original size of the area we placed into
    					if((original_region_size-5-sweep_size) > 0)
    					{	// if original size, minus 5 for the placed pentomino, minus the small region = 0, we just
    						// reduced the size of a small area and that's ok.  If there's anything else left over, we
    						// just split into a small area which is forbidden.
    						return(false);
    					}
    				}
    			}
    		}
    	}
    	return(true);
    }
    // extend an area until it's at least 5, which would be a legal
    // area for pan-kai/universe
    private void sweep_extend(UniverseCell c,int seed,int used_seed)
    {	
    	c.sweep_counter = sweep_counter;
    	sweep_size++;
    	if(sweep_size >= 5) { return; }	// big enough
    	for(int direction = 0;direction<CELL_FULL_TURN; direction++)
    	{	UniverseCell next = c.exitTo(direction);
    		if(next!=null)
    		{	if(next.sweep_counter==seed) {  }	// another seed cell, uninteresting
    			else if(next.sweep_counter==sweep_counter) {}	// already seen
    			else if (next.sweep_counter==used_seed) { }
    			else if(next.topChip()!=null) { }		// an occupied cell, uninteresting
    			else { sweep_extend(next,seed,used_seed); }		// extend
    		}
    	}
    }

    private boolean legalUniversePlacement(UniverseCell c)
    {	int rSize = universeRegionSize[c.universeRegionNumber];
    	if((rSize==5) || (rSize>=10))	// regions between 5 and 10 are effectively dead zones
    	{
    	sweep_counter++;		// this value is used to make seed cells that have been processed.
    	return(sweep_universe_start(c,c.sweep_counter,sweep_counter));
    	}
    	else { return(false); }
    }
    
    private void addDiagonalSweep4(CellStack points,UniverseCell c,int playerIndex)
    {	int adj_mask = 1<<playerIndex;
    	int diag_mask = 0x100<<playerIndex;
		for(int i=0;i<4;i++) 
			{ UniverseCell d = c.exitTo(i);
			  if(d!=null) 
			  {
			   UniverseChip chip = d.chip;
			   if((d.diagonalResult&diag_mask)!=0) { points.remove(d,false); }
			   d.diagonalResult |= adj_mask; 
			   
			  if((chip==null) || (chip.color!=playerColor[playerIndex]))
			  {
				  for(int dir = 1;dir<4; dir+=2)
				  {
					  UniverseCell e = d.exitTo((dir+i)&3);
					  if((e!=null) && (e.chip==null) && ((e.diagonalResult&(diag_mask|adj_mask))==0))
					  	{ e.diagonalResult |= diag_mask; 
					  	  points.pushNew(e); 
					  	}
					  }
				  }
			  }
			}
    }
    // mark the cells which are adjacent to a cell of a player as adjacent.
    private void setDiagonalSweep3(CellStack points, int playerIndex)
    {	points.clear();
    	int adj_mask = 1<<playerIndex;
    	int diag_mask = 0x100<<playerIndex;
    	int combined_mask = adj_mask | diag_mask;
    	for(UniverseCell c = allCells; c!=null; c=c.next) { c.diagonalResult &= ~combined_mask; }
    	
    	if(topsForPlayer[playerIndex]==0)
    	{
    	UniverseCell first = null;
    	// allow the starting positions
    	for(UniverseCell c = allCells; c!=null; c=c.next) 
    		{ if(c.startCell && c.chip==null) 
    			{ if(first==null) { first=c; } else { points.push(c); }
    			  c.diagonalResult |= diag_mask;
    			}
     		}
    		if(first!=null) { points.push(first); }
    	}
    	else
    	{
    	
    	CellStack occupied = occupiedCells[playerIndex];
    	for(int sz = occupied.size(),chipn=0; chipn<sz; chipn++)
    	{	UniverseCell c = occupied.elementAt(chipn);
    		addDiagonalSweep4(points,c,playerIndex);
    	}}
    }


    //
    // must be adjacent to existing blob or the edge at the start
    private boolean sweep_polysolver_start(UniverseCell c,UniverseChip ch,int groupSeed,int used_seed)
    {	c.sweep_counter = used_seed;		// prevent revisiting
    	
    	for(int direction = 0;direction<CELL_FULL_TURN; direction++)
    	{	UniverseCell next = c.exitTo(direction);
    		if(next==null) 
    			{ if(emptyBoard())	// if it's the first move 
    				{ sweep_adjacent = true; }	// first move must be adjacent to the edge
    			}
    		else
    		{
    	   	if(next.sweep_counter >= used_seed) {} 	// already processed
    		else if(next.topChip()!=null) 
    			{ sweep_adjacent = true; 		// adjacent to existing places
    			}
    	   	else if(groupSeed==next.sweep_counter) 
    			{	// another cell in the patter we're placing
    	   			if(!sweep_polysolver_start(next,ch,groupSeed,used_seed)) { return(false); }
    			}
    		}
    	}
    	return(true);
    }
    
    private final boolean canMake2()	// can make 2 out of 1's
    { return(numberOfSize[1]>=2); 
    }
    private final boolean canMake3()	// can make 3 out of 2's and 1's 
    { return((numberOfSize[1]>=1) 
    			&& ((numberOfSize[2]*2+numberOfSize[1])>=3)); 
    }
    private final boolean canMake4()	// can make 4 out of 3 2 and 1's
    {  	return( ((numberOfSize[3]>=1) && (numberOfSize[1]>=1))
				|| (numberOfSize[2]>=2)
				|| ((numberOfSize[2]*2 + numberOfSize[1])>=4));
    }
    private final boolean canMake5()	// can make 5 out of smaller pieces
    {	return( ((numberOfSize[4]>=1) && (numberOfSize[1]>=1))
    			|| ((numberOfSize[3]>=1) && ((numberOfSize[2]>=1) || canMake2()))
    			|| ((numberOfSize[2]>=2) && (numberOfSize[1]>=1))
    			|| ((numberOfSize[2]==1) && (numberOfSize[1]>=3))
    			|| (numberOfSize[1]>=5));
    }
    // 
    // this is a weak filter on solvability for the PolySolver, based on
    // the consideration if the sizes of the pieces remaining can sum
    // to the number of squares remaining.
    //
    private boolean isSolvableAfterPlacing(UniverseChip ch)
    {	boolean isOk=false;
    	int patsize = ch.patternSize() ;
    	
    	numberOfSize[patsize]--;
    	int region = emptySpaces;
    	int remainingSize = region-patsize;
    	int ns5 = numberOfSize[5];
    	
    	if(remainingSize>=10)
    	{
    		 while((remainingSize>=10) && (ns5>0))
    		{
    	     remainingSize -=5;
    	     ns5--;
     		}
    	}
    	switch(remainingSize)
    	{
    	case 9: isOk = ((ns5>=1) && ((numberOfSize[4]>=1) || canMake4()))
    				|| ((numberOfSize[4]>=2) && (numberOfSize[1]>=1))
    				|| ((numberOfSize[4]>=1) && (numberOfSize[3]>=1) && ((numberOfSize[2]>=1) || canMake2()))
    				|| (numberOfSize[3]>=3)
    				|| ((numberOfSize[3]>=2) && canMake3())
    				|| ((numberOfSize[3]>=1) && (((numberOfSize[2]*2+numberOfSize[1]))>=6))
    				|| ((numberOfSize[1]>=1) && (((numberOfSize[2]*2+numberOfSize[1]))>=9))
    				;
    				break;
    	case 8: isOk = ((ns5>=1) && ((numberOfSize[3]>=1) || canMake3()))
    					|| (numberOfSize[4]>=2)
    					|| (numberOfSize[4]==1) && (canMake4())
    					|| ((numberOfSize[3]>=2) && (numberOfSize[2]>=1) || canMake2())
    					|| ((numberOfSize[3]>=1) 
    							&& ((numberOfSize[1]>=1) && ((numberOfSize[2]*2+numberOfSize[1])>=5)))
    					|| (((numberOfSize[2]*2+numberOfSize[1]))>=8)
    					;
    				break;
    	case 7:	isOk = ((ns5>=1) && ((numberOfSize[2]>=1) || canMake2()))	
    					|| ((numberOfSize[4]>=1) && ((numberOfSize[3]>=1) || canMake3()))
    					|| ((numberOfSize[3]>=2) && (numberOfSize[1]>=1))
    					|| ((numberOfSize[3]>=1) && (((numberOfSize[2]*2)+(numberOfSize[1]))>=4))
    					|| (numberOfSize[1]>=1) && (((numberOfSize[2]*2)+(numberOfSize[1]))>=7)
    					;
    				break;
    	case 6:	isOk = ((ns5>=1) && (numberOfSize[1]>=1))
    					|| ((numberOfSize[4]>=1) && ((numberOfSize[2]>=1) || canMake2()))
    					|| (numberOfSize[3]>=2)
    					|| ((numberOfSize[3]==1) && canMake3())
    					|| (((numberOfSize[2]*2)+(numberOfSize[1]))>=6);
     				break;
    	case 5: isOk = (ns5>=1) || canMake5();
    			break;
    	case 4: isOk = (numberOfSize[4]>=1) || canMake4();
    			break;
    	case 3: isOk = (numberOfSize[3]>=1) || canMake3();
    			break;
    	case 2: isOk = (numberOfSize[2]>=1) || canMake2();
    			break;
    	case 1:	isOk = (numberOfSize[1]>=1);
    			break;
    	case 0:	isOk = true;
    			break;
    	default: isOk = true;		// call the others solvable by default
    	}
    	
    	numberOfSize[patsize]++;
    	return(isOk);
    }
    private boolean legalPolysolverPlacement(UniverseCell c,UniverseChip ch)
    {	sweep_counter++;		// this value is used to make seed cells that have been processed.
    	sweep_adjacent = false;
    	boolean can = sweep_polysolver_start(c,ch,c.sweep_counter,sweep_counter);
    	return(can && sweep_adjacent);
    }
    
    // can physically add a chip, and also follow robot conventions
    // about the characteristics of next moves.  This especially includes
    // the "adjacent" rules which are not imposed on human players.
    private boolean canAddChip(UniverseCell c,UniverseChip ch)
    {	sweep_counter++;
    	c.sweep_counter = sweep_counter;

     	if(ch!=null)
    	{	// it fits, but is it legal
    		switch(rules)
    		{
    		case Nudoku_9x9:
    		case Nudoku_6x6:
    		case Nudoku_12:
    		case Nudoku_11:
    		case Nudoku_10:
    		case Nudoku_9:
    		case Nudoku_8:
    		case Nudoku_7:
    		case Nudoku_6:
    		case Nudoku_5:
    		case Nudoku_4:
    		case Nudoku_3:
    		case Nudoku_2:
    		case Nudoku_1_Box:
    		case Nudoku_2_Box:
    		case Nudoku_3_Box:
    		case Nudoku_4_Box:
    		case Nudoku_5_Box:
    		case Nudoku_6_Box:
    		case Nudoku_8x8:
    		case Nudoku_1:
     		case PolySolver_6x6:
    		case PolySolver_9x9:
    		case Sevens_7:
    		case Phlip:
    			return(ch.canAddChip(c));
     		case Diagonal_Blocks:
     		case Diagonal_Blocks_Classic:
     		case Blokus:
     		case Blokus_Classic:
    		case Diagonal_Blocks_Duo:
    		case Blokus_Duo:
    			// colorindex is a swap of 0 1 
    	    	validateDiagonalsCacheForPlayer(getPlayerIndexForOwner(ch));
    			return(canAddDiagonalChip(c,ch,null));
    			// has to touch diagonally, except for the first move of course
    			// break;
    		default:	throw G.Error("Not expecting %s",rules);
    		case Universe:
    		case Pan_Kai:
    			// must not form a new region size < 5.  As a side effect, canAddChip copies the sweep counter to the new cells.
    			return(ch.canAddChip(c) && legalUniversePlacement(c));
    		}
    	}
     	return(false);
    }
    
    private boolean canAddPolySolverChip(UniverseCell c,UniverseChip ch)
    {	return(canAddChip(c,ch) && legalPolysolverPlacement(c,ch));    
    }
    private void addSampleMove(UniverseCell c,UniverseChip ch,Mspec m)
    {	ch.markSampleChip(c,m);
    }
    public boolean LegalToHitBoard(UniverseCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
 		case PLAY_OR_SWAP_STATE:
			return((pickedObject==null)
					? isDest(cell) 
					: canAddChip(cell,pickedObject));		// enforce the game rules

		case GAMEOVER_STATE:
		case CONFIRM_SWAP_STATE:
		case PASS_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	return(pickedObject==null
        				? (cell.topChip()!=null)
        				: canAddChip(cell,pickedObject));
        }
    }
  public boolean canDropOn(UniverseCell cell)
  {		UniverseCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
  StateStack robotState = new StateStack();
 
  private int undoInfo;
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(UniverseMovespec m)
    {	int flipsize = flippedCells.size();
    	robotState.push(board_state);
    	undoInfo = 0;
    	//G.print("R "+m);
        // to undo undoInfo transitions is to simple put the original undoInfo back.
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
        	if (DoneState())
            {
                doRobotDone();
            }
        	else if ((m.op == MOVE_DONE)||(m.op==MOVE_PASS) || (m.op==MOVE_RACK_BOARD) || (m.op==MOVE_LINK) || (m.op==MOVE_GAMEOVER))
            {
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
        undoStack.push((undoInfo*100)*100+(flippedCells.size()-flipsize));
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(UniverseMovespec m)
    {
    	int undo = undoStack.pop();
        int flip = undo%100;
        undo = undo/100;
         //G.print("U "+m);
        consecutivePasses=0;
        while(flip-->0)
        {	UniverseCell unflip = flippedCells.pop();
        	flipAdjacentChip(unflip,unflip.topChip());
        }
        
        switch (m.op)
        {
  	    case MOVE_SWAP:
  	    	doSwap();
  	    	break;
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_PASS:
        case MOVE_GAMEOVER:
   	    case MOVE_DONE:
        	invalidateDiagonalPointsCache();
            break;
        case MOVE_LINK:
        	{
        		UniverseCell from = getCell(UniverseId.BoardLocation,m.from_col,m.from_row);
        		from.nextInBox = null;
        	}
        	break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PASS_STATE:
        		case PLAY_OR_SWAP_STATE:
        		case PLAY_STATE:
        			G.Assert(pickedObject==null,"something is moving");
        			pickObject(getCell(UniverseId.BoardLocation,m.to_col,m.to_row),-1);
        			originalPickedObject = pickedObject = UniverseChip.getIsomer(undo/100);
        			dropObject(getCell(UniverseId.ChipRack,m.from_col, m.from_row));
       			    acceptPlacement();
                    break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
    
    //
    // legal moves for universe and pan-kai must fit on the board, and must leave no small regions
    // cut off from the main empty region. 
    //
    private boolean getLegalUniverseMoves(CommonMoveStack  all,UniverseCell loc,int orientation,int who)
    {	UniverseChip baseChip = loc.topChip();
    	if(baseChip!=null)
    	{
    	for(UniverseCell c = allCells; c!=null; c=c.next)
   		{
   		if(c.topChip()==null) 
   		{	
   		int rsize = universeRegionSize[c.universeRegionNumber];
   		if((rsize==5) || (rsize>=10))
   		{


		UniverseChip variations[] = baseChip.getVariations();
		int nvar = variations.length;
		for(int varnum = 0,lim = (orientation>=0)?1 : nvar;
			varnum<lim;
			varnum++)
			{	// if a preferred orientation is given, use only that one
			int varidx = orientation>=0 ? (varnum+orientation)%nvar : varnum;
			UniverseChip ch = variations[varidx];

					if( ((flippable==FlipStyle.allowed)|| !ch.flipped) && canAddChip(c,ch))
					{	if(all==null) { return(true); }
						all.push(new UniverseMovespec(MOVE_RACK_BOARD,loc.col,loc.row,ch.rotated,ch.flipped,c.col,c.row,who));
					}
			}
   		}
    	
   		}}}
   	 return(false);
    }
    
    private boolean getLegalUniverseMoves(CommonMoveStack  all,int orientation,int who)
    {	for(UniverseCell loc : rack[who])
    	{	boolean done = getLegalUniverseMoves(all,loc,orientation,who);
    		if(done) { return(true); }
    	}
    	return(false);
    }
    //
    // this finds the chip highlight for the full footprint of the chip, 
    // and as a side effect copies the sweep counter from the seed to the
    // rest of the pattern.  This version is for Diagonal-Blocks games
    // so it also chechks that there are no cells marked "adjacent" 
    // and at least one cell marked "diagonal".  The diagonalResult field
    // has to be maintained separately
    // 

    public boolean canAddDiagonalChip(UniverseCell c,UniverseChip ch,UniverseCell home_diagonal)
    {	if(c.chip!=null) { return(false); }
    	int playerIndex = getPlayerIndexForOwner(ch);
    	int diag_mask = 0x100<<playerIndex;
    	int adj_mask = 1<<playerIndex;
    	boolean hasDiagonal = (c.diagonalResult&diag_mask)!=0;
    	UniverseCell d = c;
    	if(((d.diagonalResult&adj_mask)!=0) || (d.chip!=null)) 
    		{ 
    		  return(false);
    		}
    	for( OminoStep step : ch.pattern)
    	{	UniverseCell d1 = d;
    		boolean retry = false;
    		switch(step.dx)
    		{	case -1:	d = d.exitTo(CELL_LEFT);
    						break;
    			case 1:		d = d.exitTo(CELL_RIGHT);
    						break;
    			default:
    		}
    		if((d==null)||(d.chip!=null))
    			{ 
    			// if stepping once is off the grid, try the other way
    			d = d1;
    			retry = true;
    			}
    		switch(step.dy)
    		{	case -1:	d = d.exitTo(CELL_UP);
    						break;
    			case 1:		d = d.exitTo(CELL_DOWN);
    						break;
			default:
				break;
    		}
    		if(retry && (d!=null))
    		{
    			switch(step.dx)
        		{	case -1:	d = d.exitTo(CELL_LEFT);
        						break;
        			case 1:		d = d.exitTo(CELL_RIGHT);
        						break;
        			default:
        		}	
    		}
    		if((d==null)||(d.chip!=null)) 
    			{ 
    			  return(false); 
    			}
    		int diag = d.diagonalResult;
    		if((diag&adj_mask)!=0)
    			{ 
    			 
    			return(false); 
    			};
    		boolean isDiagonal = ((diag&diag_mask)!=0);
    		if(isDiagonal)
    		{
    		if(home_diagonal!=null)
    		{
    		if(d==home_diagonal)
    			{	// yes, it touches the desired place
    				hasDiagonal = true;
    			}
    			else
    			{
    				// more than one diagonal, accept only if this is the canonical position
    				if( (d.row+d.col*100) < (home_diagonal.row+home_diagonal.col*100))
    						{
    							// if the home diagonal is not this diagonal and lowest
    							// in an arbitraty canonical order, reject this move.  This
    							// intends to reject duplicate moves that touch multiple diagonals
    							return(false);
    						}
    			}
    		}
    		else
    		{
    		hasDiagonal |= isDiagonal;
    		}}
    		// copy the sweep to the new cell
    		d.sweep_counter = c.sweep_counter;
     	}

    	return(hasDiagonal);
    	
    }
    //
    // new version that tries only known diagonals
    // this version generates a few duplicate moves when one poly covers multiple diagonal points.
    //
    private boolean getLegalDiagonalBlocksMovesDia(CommonMoveStack  all,UniverseCell chipHome,UniverseChip baseChip,int orientation,int who,boolean onePlace)
    {	
    	boolean some = false;
    	CellStack points = diagonalPointsForPlayer[who];
    	for(int i=0,last = onePlace ? 1 : points.size(); i<last; i++)
   		{
    	UniverseCell c = points.elementAt(i);
    	//G.Assert(c.diagonalResult==diagonalSweepResult.diagonal,"not a diagonal point");
 		UniverseChip variations[] = baseChip.getVariations();
		int nvar = variations.length;
		for(int varnum = 0,lim = (orientation>=0)?1 : nvar;
			varnum<lim;
			varnum++)
		{	// if a preferred orientation is given, use only that one
				int varidx = orientation>=0 ? (varnum+orientation)%nvar : varnum;
				UniverseChip ch = variations[varidx];
				UniverseCell startingPoint = c;
				if((flippable==FlipStyle.allowed)|| !ch.flipped)
				{
				if(canAddDiagonalChip(startingPoint,ch,c))
					{	if(all==null) { return(true); }
						all.push(new UniverseMovespec(MOVE_RACK_BOARD,chipHome.col,chipHome.row,ch.rotated,ch.flipped,c.col,c.row,who));
						some = true;
					}
				char startingCol = c.col;
				int startingRow = c.row;
				for(int offset = 0,lastOffset = ch.patternSize()-1; offset<lastOffset; offset++)
				{	char col = (char)(startingCol - ch.patStepX[offset]);
					int row = startingRow - ch.patStepY[offset];
					UniverseCell testCell = getCell(col,row);
					if(testCell!=null)
					{
						if(canAddDiagonalChip(testCell,ch,startingPoint))
						{	if(all==null) { return(true); }
							all.push(new UniverseMovespec(MOVE_RACK_BOARD,chipHome.col,chipHome.row,ch.rotated,ch.flipped,col,row,who));
							some = true;
						}
					
					}
				}
				}
   			}
   		}
    	return(some);
    }
    public boolean validMove(UniverseMovespec m)
    {
    	switch(m.op)
    	{
    	case MOVE_RACK_BOARD:
    		UniverseCell from = getCell(UniverseId.ChipRack,m.from_col,m.from_row);
    		UniverseChip ch = from.chip;
    		if(ch!=null)
    		{
			UniverseCell to = getCell(UniverseId.BoardLocation,m.to_col,m.to_row);
			if(to.chip==null)
			{
				return(canAddDiagonalChip(to,ch.getVariation(m.rotation%4,m.rotation>=4),null));
			}}
    		return(false);
    	case MOVE_PASS:
    	default: return(false);
    	}
    }
    //
    // new version that tries only known diagonals
    // this version generates a few duplicate moves when one poly covers multiple diagonal points.
    //
    private commonMove getRandomDiagonalBlockMove(Random rand,UniverseCell chipHome,UniverseChip baseChip,int who)
    {	CellStack points = diagonalPointsForPlayer[who];
    	int index = points.size();				
    	// note that diagonalPoints is supposed to be used in a random order,
    	// so it doesn't matter that we actually randomize it in the process of using it here.
    	while(index>0)
   		{
    	int off = Random.nextSmallInt(rand,index);					// pick a random element
    	UniverseCell c = points.elementAt(off);			// get it (and below, swap it to the top)
    	points.setElementAt(points.elementAt(--index),off);	// reload the slot with the last element
    	points.setElementAt(c,index);				// reload the last element with the current element
    	
 		UniverseChip variations[] = baseChip.getVariations();
		int nvar = variations.length;
		int lastOffset = baseChip.patternSize()-1;			// all variations will have the same size
		int randomchipoffset=Random.nextSmallInt(rand,lastOffset+1);	// we'll start all variations at the same random offset
		for(int varnum = 0,lim = nvar,randvar=Random.nextSmallInt(rand,lim);
				varnum<lim;
				varnum++)
			{	// if a preferred orientation is given, use only that one
				int varidx = varnum+randvar;
				UniverseChip ch = variations[varidx>=lim?varidx-lim:varidx];
				if((flippable==FlipStyle.allowed)|| !ch.flipped)
				{
				char startingCol = c.col;
				int startingRow = c.row;
				for(int offset0 = 0;
					offset0<=lastOffset;
					offset0++)
				{	int offset = offset0 + randomchipoffset;
					if(offset>lastOffset) { offset -= (lastOffset+1); }
					char col = (char)(startingCol - ((offset==lastOffset)?0:ch.patStepX[offset]));
					int row = startingRow - ((offset==lastOffset)?0:ch.patStepY[offset]);
					UniverseCell testCell = getCell(col,row);
					if(testCell!=null)
					{
						if(canAddDiagonalChip(testCell,ch,c))
						{	return(new UniverseMovespec(MOVE_RACK_BOARD,chipHome.col,chipHome.row,ch.rotated,ch.flipped,col,row,who));
						}
					
					}
				}
				}
 			}
   		}
    	return(null);
    }
    // traditional version that just tries all possible cells
    @SuppressWarnings("unused")
	private boolean getLegalDiagonalBlocksMovesFor(CommonMoveStack  all,UniverseCell chipHome,UniverseChip baseChip,int orientation,int who)
    {	
    	boolean some = false;
    	    	for(UniverseCell c = allCells; c!=null; c=c.next)
   		{
    	int diag_mask = 0x100<<who;
   		if(c.chip==null && (c.diagonalResult&diag_mask)!=0) 
   		{	
 			UniverseChip variations[] = baseChip.getVariations();
			int nvar = variations.length;
			for(int varnum = 0,lim = (orientation>=0)?1 : nvar;
				varnum<lim;
				varnum++)
			{	// if a preferred orientation is given, use only that one
				int varidx = orientation>=0 ? (varnum+orientation)%nvar : varnum;
				UniverseChip ch = variations[varidx];

				if( ((flippable==FlipStyle.allowed)|| !ch.flipped) && canAddDiagonalChip(c,ch,null))
					{	if(all==null) { return(true); }
						all.push(new UniverseMovespec(MOVE_RACK_BOARD,chipHome.col,chipHome.row,ch.rotated,ch.flipped,c.col,c.row,who));
						some = true;
					}
 			}
   			}
   		}
    	return(some);
    }
    private boolean getLegalDiagonalBlocksMoves(CommonMoveStack  all,UniverseCell chipHome,int orientation,int who,boolean robot)
    {	// must not call this without calling setDiagonalSweep1 and setDiagonalSweep2 first
    	UniverseChip baseChip = chipHome.chip;
    	boolean onePlace = false;
		if(robot
			&& (baseChip!=null)
    		&& (topsForPlayer[who]<15))	
   			{	if(baseChip.patternSize()<4)
    				{	// force the first few moves to be 4's 5's
    				baseChip = null;
    				}
    				else
    				{
    				if(topsForPlayer[who]==0) { onePlace = true; }	
    				}
    		}
    	if(baseChip!=null)
    	{	//int size0 = (all==null) ? 0 : all.size();
    		boolean val = false;
    		val = getLegalDiagonalBlocksMovesDia(all,chipHome,baseChip,orientation,who,onePlace);
    		return(val);
    		
    	}

   	 return(false);
    }  

    private boolean getLegalDiagonalBlocksMoves(CommonMoveStack  all,int orientation,int who,boolean robot)
    {   validateDiagonalsCacheForPlayer(who);
    	// order from smallest to largest
    	UniverseCell rr[] = rack[who];
		for(int i=0,lim=rr.length; i<lim; i++)
    	{	UniverseCell loc = rr[i];
   			boolean done = getLegalDiagonalBlocksMoves(all,loc,orientation,who,robot);
   			if((all==null) && done) { return(true); }
    	}
		if(board_state==UniverseState.PLAY_OR_SWAP_STATE)
			{ all.push(new UniverseMovespec(MOVE_SWAP,who)); }
		return(all==null?false:all.size()>0);
    }
    private int randomPiecePtable[] = null;
    private commonMove getRandomDiagonalBlocksMove(Random rand,int who,boolean weightBySize)
    {   validateDiagonalsCacheForPlayer(who);
    	UniverseCell rr[] = rack[who];
    	//
    	// tough lesson here.  the original version of this used a single random index, and went
    	// through the array of chips serially modulo that index.  Playing against pentobi and
    	// studying the results (why we were still losing even with many more playouts) eventually
    	// showed that there was a strong bias in favor of the small pieces, once all the big pieces
    	// had been played.  This makes sense, since once the bigger pieces are favored and that tends
    	// to clear out the last half of the piece array, which rolls over to the "one" piece.
    	// 
    	// reversing the scan order from large to small didn't help.  So this version, which takes
    	// the pieces in a completely random order, is the next attempt.  It's the first one that wins.
    	//
    	int index = rr.length;
    	while(index>0)
    	{	int off = Random.nextSmallInt(rand,index);			// pick a random index
    		int chipIndex = randomPiecePtable[off];
    		UniverseCell chiphome = rr[chipIndex];	// get the cell corresponding to this index
    		UniverseChip basechip = chiphome.chip;
    	
    		if(basechip==null)
    				{ // just remove this one
    				randomPiecePtable[off] = randomPiecePtable[--index];				// re-occupy this slot and reduce the index
    				randomPiecePtable[index] = chipIndex;
    				}
    		else
    		{
    		int sz = basechip.patternSize();
    		if(weightBySize && sz<5 && (Random.nextSmallInt(rand,5)>=sz)) {/* G.print("s "); */	}	// reject this temporarily
	    		else
	    		{
	   			randomPiecePtable[off] = randomPiecePtable[--index];	// accept and remove from further consideration	
   				randomPiecePtable[index] = chipIndex;

	      		commonMove val = getRandomDiagonalBlockMove(rand,chiphome,basechip,who);
	    			if(val!=null) { return(val); }
	   			}}
	    	}
   		if(consecutivePasses==(players_in_game-1))
    		{
    			return(new UniverseMovespec(MOVE_GAMEOVER,whoseTurn));
    		}
   			if (topsForPlayer[whoseTurn]<topsForPlayer[getNextPlayer(whoseTurn)])
   			{
   				return(new UniverseMovespec(MOVE_RESIGN,whoseTurn));
   			}
    		else 
    		{ return(new UniverseMovespec(MOVE_PASS,whoseTurn)); 
    		}
    }
    private boolean hasLegalDiagonalBlocksMoves(int who)
    {	return(getLegalDiagonalBlocksMoves(null,-1,who,false));
    }
    public CommonMoveStack getDiagonalBlocksMoves(int who)
    {	CommonMoveStack stack = new CommonMoveStack();
    	getLegalDiagonalBlocksMoves(stack,-1,who,false);
    	return(stack);
    }
    
    // this implements a heuristic for the polysolver and nudoku solvers - that since
    // everything will eventually be covered, we only need to consider one place for
    // any particular move, and that the best place is the one with the most restructed
    // set of choices.  This narrows the tree as rapidly as possible.
    //
    // note that there once was a "no holes" policy too, but that interacted badly with
    // this.  Picking the most restrictive cell in some positions is sure to create 
    // a hole.  Since this is the more powerful method by far, we kept it and just
    // let the holes be filled rapidly.
    //
    private void limitToDifficultCell(MspecStack pass1,CommonMoveStack fall,int who)
    {
       	
      	 // if there are any cells for which no move covers, then the puzzle is unsolvable.
      	 // if there are any cells for which exactly one fits, then it is the only one to try
      	UniverseCell mostDifficultCell = getCell('F',2);
      	int minMoves = 999999999;
      	boolean done = false;
      	boolean empty = emptyBoard();
      	for(UniverseCell c = allCells; !done && c!=null; c=c.next)
      		{
      		if(c.topChip()==null)
      			{if (c.nMoves==0)
      				{
      				for(int dir = 0;dir<4; dir++)
      				{	UniverseCell d = c.exitTo(dir);
      					if((d!=null) && (d.topChip()!=null))
      					{	pass1.clear();		// make the puzzle unsolvable
      						done = true;
      					}
      				}}
      			else { 
      				if(c.nMoves<minMoves)
      				{	if(!empty ? c.nonEmptyAdjacent() : c.isEdgeCell())
      					{
      					minMoves = c.nMoves;
      					mostDifficultCell = c;
      					}
      				}
      			}
      			}
      		}
      	
      	// finally, construct he moves that affect the most difficult cell
      	while(pass1.size()>0)
      	{
      		Mspec mov = pass1.pop();
      		int count = mostDifficultCell.nMoves;
      		UniverseCell c = mov.c;
      		UniverseChip ch = mov.ch;
      		addSampleMove(c,ch,mov);
      		if(mostDifficultCell.nMoves>count)
      		{	UniverseMovespec m = new UniverseMovespec(MOVE_RACK_BOARD,(char)('A'+who),ch.getPatternIndex(),ch.rotated,ch.flipped,c.col,c.row,who);
      			fall.push(m);
      		}
      	
      	}
    }
    private boolean addPossibleMoves(MspecStack pass1,UniverseCell c,UniverseChip baseChip,int orientation)
    {
		// this size of chip is plausible
		UniverseChip variations[] = baseChip.getVariations();
		int nvar = variations.length;
		for(int varnum = 0,lim = (orientation>=0)?1 : nvar;
			varnum<lim;
			varnum++)
		{	// if a preferred orientation is given, use only that one
			int varidx = orientation>=0 ? (varnum+orientation)%nvar : varnum;
			UniverseChip ch = variations[varidx];
			boolean can = ((flippable==FlipStyle.allowed) || !ch.flipped) && canAddPolySolverChip(c,ch);
			if(can)
				{	if(pass1==null) { return(true); }
					Mspec m = new Mspec(c,ch);
					pass1.push(m);
					addSampleMove(c,ch,m);
					}
			}
		return(false);
    }

    /*
	The "snakes" nudoku puzzles use 8 dominos and 7 trominos, so there is no useful
	termination based on the set of shapes that is left over.  On the other hand we
	only need to try each shape once, and there are only 2 of them.
     */
    private boolean getLegalSnakessolverMoves(CommonMoveStack  fall,int orientation,int who)
    {	
    	MspecStack pass1 = (fall!=null) ? new MspecStack() : null;
    	
    	for(UniverseCell c = allCells; c!=null; c=c.next) { c.sampleMove = null; c.nMoves = 0; c.sweep_counter = 0; }
  
    	for(UniverseCell c = allCells; c!=null; c=c.next)
   		{
   		if(c.topChip()==null) 
   		{	
   			int alreadyDone = 0;
   	    	
   	    for(UniverseCell loc : rack[who])
    	{	UniverseChip baseChip = loc.topChip();
    		if(baseChip!=null)
    		{
    			int size = baseChip.patternSize();
    			int mask = (1<<size);
    			if((alreadyDone & mask)==0)
    			{
   				 if(baseChip.sudokuValues==null) 
   				 	{ alreadyDone |= (1<<size);}	// exclude further trials of this size

   				 if(addPossibleMoves(pass1,c,baseChip,orientation)) { return(true); }
 
    			}
    		}
    	}
   		}}
   	
   	limitToDifficultCell(pass1,fall,who);

   	return(false);
    }

    private boolean addPossiblePhlipMoves(CommonMoveStack  all,UniverseCell c,UniverseChip baseChip,int who,int orientation)
    {
		// this size of chip is plausible
		UniverseChip variations[] = baseChip.getVariations();
		int nvar = variations.length;
		for(int varnum = 0,lim = (orientation>=0)?1 : nvar;
			varnum<lim;
			varnum++)
		{	// if a preferred orientation is given, use only that one
			int varidx = orientation>=0 ? (varnum+orientation)%nvar : varnum;
			UniverseChip ch = variations[varidx];
			boolean can = ((flippable==FlipStyle.allowed) || !ch.flipped) && canAddChip(c,ch);
			if(can)
				{	if(all==null) { return(true); }
					int color = getPlayerIndexForOwner(baseChip);
					UniverseMovespec m = new UniverseMovespec(MOVE_RACK_BOARD,(char)('A'+color),
									ch.getPatternIndex(),ch.rotated,ch.flipped,c.col,c.row,who);
					all.push(m);
					}
			}
		return(false);
    }
    private boolean getLegalPhlipMoves(CommonMoveStack  fall,UniverseCell loc,int orientation,int who)
    {	UniverseChip baseChip = loc.topChip();
    	if(baseChip!=null)
    	{
		for(UniverseCell c = allCells; c!=null; c=c.next)
		{
		if(c.topChip()==null) 
			{	
			int alreadyDone = 0;
			int pat = baseChip.getPatternIndex();
			int mask = (1<<pat);
			G.Assert(mask!=0,"no mask");
			if((alreadyDone & mask)==0)
			{
				 if(baseChip.sudokuValues==null) 
				 	{ alreadyDone |= mask;}	// exclude further trials of this size

				 if(addPossiblePhlipMoves(fall,c,baseChip,who,orientation)) { return(true); }
			}
			}
		}
		}
	return(false);
    }
    
    private boolean getLegalPhlipMoves(CommonMoveStack  fall,int orientation,int who)
    {	for(UniverseCell loc : rack[who])
    	{
    	boolean done = getLegalPhlipMoves(fall,loc,orientation,who);
    	if(done) { return(true); }
    	}
    	return(false);
    }
    
 //
 // the polysolver tries to shortcut termination when the remaining set of pieces
 // can't fill the board.  For example, if there were 9 spaces left but only pentaminos 
 // remain to be played.
 //
 private boolean getLegalPolysolverMoves(CommonMoveStack  fall,UniverseCell loc,int orientation,int who)
 {	int excluded = 0;
 	int included = 0;	// make -1 to turn off the exclusion logic
 	UniverseChip baseChip = loc.topChip();
 	if(baseChip==null) { return(false); }
 	MspecStack pass1 = (fall!=null) ? new MspecStack() : null;
	for(UniverseCell c = allCells; c!=null; c=c.next)
		{
		if(c.topChip()==null) 
		{	
	 			int size = baseChip.patternSize();
	 			int mask = (1<<size);
	 			if((excluded & mask)==0)
	 			{
	 			boolean canPlace = (included & mask)!=0;
	 			canPlace |= isSolvableAfterPlacing(baseChip);
	 			
	 			if(!canPlace) 
	 			{	// this size of chip is not plausible
	 				excluded |= (1<<size);
	 			}
	 			else
	 			{
	 			included |= mask;
	 			if(addPossibleMoves(pass1,c,baseChip,orientation)) { return(true); }
	 			}
			}
	 	}
 	}
	limitToDifficultCell(pass1,fall,who);
	return(false);
 }
 
 private boolean getLegalPolysolverMoves(CommonMoveStack  fall,int orientation,int who)
 { 
	for(UniverseCell c = allCells; c!=null; c=c.next) { c.sampleMove = null; c.nMoves = 0; }
	for(UniverseCell loc : rack[who])
 	{	boolean done = getLegalPolysolverMoves(fall,loc,orientation,who);
 		if(done) { return(true); }
 	}
	return(false);
 }
 private boolean hasLegalUniverseMoves(int who)
 {	return(getLegalUniverseMoves(null,-1,who));
 }
 private boolean hasLegalPhlipMoves(int who)
 {	return(getLegalPhlipMoves(null,-1,who));
 }

 private boolean sevensLinksHere(UniverseCell to)
 {
	 for(UniverseCell c = allCells; c!=null; c=c.next) { if(c.nextInBox==to) { return(true); }}
	 return(false);
 }
 private boolean sevensBoxIncomplete(UniverseCell start)
 {	UniverseCell cur = start;
 	do { 
 		cur = cur.nextInBox;
 	} while((cur!=null) && (cur!=start));
	return(cur==null);
 }
 public int numberSevensAvailable(UniverseCell c)
 {	int n = 0;
	 for(int dir=0;dir<CELL_FULL_TURN; dir++)
	 {
		 UniverseCell d = c.exitTo(dir);
		 if((d!=null) && (d.nextInBox==null)) { n++; }
	 }
	 return(n);
 }
 private UniverseCell findIncompleteSevensHead()
 {	UniverseCell any = null;
	 for(UniverseCell c = allCells; c!=null; c=c.next)
	 {
		 if((c.nextInBox!=null) && sevensBoxIncomplete(c) && !sevensLinksHere(c)) { return(c); }
		 if(c.nextInBox==null) { any = c; }
	 }
	 if(any!=null)
	 {
		 // find the most restricted starting cell
		 int anycount = numberSevensAvailable(any);
		 for(UniverseCell c = allCells; c!=null; c=c.next)
		 {	if(c.nextInBox==null)
		 	{
			 int newcount = numberSevensAvailable(c);
			 if(newcount<anycount) { anycount = newcount; any = c; }
		 	}
		 }
	 }
	 return(any);
 }
 private int sevensBoxTotal(UniverseCell from)
 {
	 int sum = 0;
	 UniverseCell cur = from;
	 do 
	 { sum += cur.sudokuValue;
	   cur = cur.nextInBox;
	 } while((cur!=null) && (cur!=from));
	 return(sum);
 }
 private UniverseCell sevensBoxEnd(UniverseCell from)
 {
	 while(from.nextInBox!=null) { from = from.nextInBox; }
	 return(from);
 }
 private void getLegalSevensMoves(CommonMoveStack all,int who)
 {
	 UniverseCell head = findIncompleteSevensHead();
	 if(head!=null)
	 {	int sum = sevensBoxTotal(head);
	 	UniverseCell tail = sevensBoxEnd(head);
		if(sum==7)
			{ all.push(new UniverseMovespec(MOVE_LINK,tail.col,tail.row,head.col,head.row,who)); 
			}
		else
		{	
			for(int dir = 0; dir<CELL_FULL_TURN; dir++)
			{
				UniverseCell next = tail.exitTo(dir);
				if((next!=null) && (next.nextInBox==null) && (next.sudokuValue+sum<=7))
				{	all.push(new UniverseMovespec(MOVE_LINK,tail.col,tail.row,next.col,next.row,who));
				}
			}
			if(head!=tail)
			{
			// also add possible links to the head, so we grow in both directions.
			for(int dir = 0; dir<CELL_FULL_TURN; dir++)
			{
				UniverseCell next = head.exitTo(dir);
				if((next!=null) && (next.nextInBox==null) && (next.sudokuValue+sum<=7))
				{	all.push(new UniverseMovespec(MOVE_LINK,next.col,next.row,head.col,head.row,who));
				}
			}}
		}
	 }
 }
 public commonMove Get_Random_Diagonal_Move(Random rand,boolean weightBySize)
 {	commonMove val = getRandomDiagonalBlocksMove(rand,whoseTurn,weightBySize);
 	if(val==null)
 		{
 		CommonMoveStack  all = new CommonMoveStack();
 		getLegalDiagonalBlocksMoves(all,-1,whoseTurn,false);
 		G.Assert(all.size()==0, "random move missed moves");
 		val = new UniverseMovespec(MOVE_PASS,whoseTurn);
  		}
 	 return(val);
 }
 public CommonMoveStack  GetListOfMoves()
 {	return(getListOfMoves(whoseTurn));
 }
 public CommonMoveStack getListOfMoves(int who)
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(rules)
 	{
 	default: throw G.Error("Not implemented");
 	case Diagonal_Blocks:
 	case Diagonal_Blocks_Duo:
 	case Blokus_Duo:
	case Diagonal_Blocks_Classic:
 	case Blokus:
 	case Blokus_Classic:
 		{
	 		boolean firstTurn = emptyBoard();
	 		invalidateDiagonalPointsCache();
	 		getLegalDiagonalBlocksMoves(all,firstTurn?preferredOrientation:-1,who,true);
	 		if(all.size()==0)
	 		{	// try harder.  This covers the rare case that the robot has no moves for 5's near the
	 			// beginning of the game.
	 			getLegalDiagonalBlocksMoves(all,firstTurn?preferredOrientation:-1,who,false);
	 		}
	 		if(all.size()==0)
	 			{ all.push(new UniverseMovespec(MOVE_PASS,who)); 
	 			}
	 		}
 		
 		break;
    case Sevens_7:
    		getLegalSevensMoves(all,who);
    		if(all.size()==0)
    		{	//boolean isValid = isValidSevens();
    			//boolean filled = allSevensLinked();
    			//G.print("valid "+isValid+"  link "+filled);
    			all.push(new UniverseMovespec(MOVE_RESIGN,who));
    		}
    		break;
	case Nudoku_12:		// nudoku puzzles use the snakes tile set and various patterns of boxes.
	case Nudoku_11:
	case Nudoku_10:
	case Nudoku_9:
	case Nudoku_8:
	case Nudoku_7:
	case Nudoku_6:
	case Nudoku_5:
	case Nudoku_4:
	case Nudoku_3:
	case Nudoku_2:
	case Nudoku_1_Box:
	case Nudoku_2_Box:
	case Nudoku_3_Box:
	case Nudoku_4_Box:	// 3 x 2 in 2x2
	case Nudoku_5_Box:
	case Nudoku_6_Box:
	case Nudoku_1:
 		if(!isValidSudoku())
 		{	// the sudoku validity check is here, not in the move generator, so we work by 
 			// actually making invalid moves and then resigning, which causes backtracking.
 			all.push(new UniverseMovespec(MOVE_RESIGN,who));
 			break;
 		}
 		else
 		{
 		getLegalSnakessolverMoves(all,-1,who);
 		}
 		break;
	case Nudoku_6x6:
	case Nudoku_9x9:

 		if(!isValidSudoku())
 		{
 			all.push(new UniverseMovespec(MOVE_RESIGN,who));
 			break;
 		}
 		else
 		{
 		getLegalPolysolverMoves(all,-1,who);
 		}
 		break;
	case PolySolver_6x6:
 	case PolySolver_9x9:
 		{
 		boolean firstTurn = emptyBoard();
 		getLegalPolysolverMoves(all,firstTurn?preferredOrientation:-1,who);
		if(all.size()==0)
			{ all.push(new UniverseMovespec(MOVE_RESIGN,who)); }
 		}
		break;
 	case Phlip:
 		{
 		boolean firstTurn = emptyBoard();
 		getLegalPhlipMoves(all,firstTurn?preferredOrientation:-1,who);
 		if(all.size()==0)
 			{
 			all.push(new UniverseMovespec(MOVE_PASS,who)); 
 			}
 		}
 		break;
 	case Universe:
 	case Pan_Kai:
 		{
 		boolean firstTurn = emptyBoard();
 		getLegalUniverseMoves(all,firstTurn?preferredOrientation:-1,who);
 		if(all.size()==0)
 			{ all.push(new UniverseMovespec(MOVE_PASS,who)); }
 		}
 		break;
 	}
  	return(all);
 }
 	public CommonMoveStack moveStack = new CommonMoveStack();
 	public static CommonMoveStack bestMoves = new CommonMoveStack(); 
 	public int bestScore = 48;
 	public UniverseBoard mainBoard = null;
 	public void findWorstGame(String rem)
 	{	// find the lowest scoring game from this starting position.  This will take
 		// forever unless your're already close to a final position, perhaps 3 moves
 		// away.
 		findWorstGame(0,0);
 		int b = bestScore;
 		if(bestScore<b)
 		{
 		G.print("Removed "+rem);
 		for(int i=0;i<bestMoves.size();i++)
 		{ G.print(""+bestMoves.elementAt(i)); 
 		}
 		G.print("New best "+bestScore);
 		}
 	}
 	public void findWorstGame(int depth,int pass)
 		{
 		int total = scoreForPlayer(0)+scoreForPlayer(1);
 		if(total>=bestScore) { return; }
 		if(GameOver()) 
 			{	//bestScore = total;
 				bestMoves.copyFrom(moveStack);
 				UniverseBoard save = (UniverseBoard)mainBoard.cloneBoard();
 				mainBoard.copyFrom(this);
 				for(int i=0;i<bestMoves.size();i++) { G.print(""+bestMoves.elementAt(i)); }
				G.print("New best "+bestScore);
				// put a breakpoint here to be able to see the result.  This is 
				// important because there are false solutions which are not legal
				// game positions.
				mainBoard.copyFrom(save);
				return;
 			}
 		CommonMoveStack moves = (((pass&(1<<whoseTurn))==0) ? getListOfMoves(whoseTurn) : null);
 		if(moves==null) { moves = new CommonMoveStack(); moves.push(new UniverseMovespec(MOVE_PASS,whoseTurn)); }
 		while(moves.size()>0)
 		{	UniverseMovespec next = (UniverseMovespec)moves.pop();
 			moveStack.push(next);
 			if(next.op==MOVE_PASS) { pass |= 1<<whoseTurn; }
 			RobotExecute(next);
 			findWorstGame(depth+1,pass);
 			
 			moveStack.pop();
 			UnExecute(next);
 		
 		}
 		}
}
