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
package online.game;
import lib.G;
import lib.Random;
import online.game.cell.Geometry;
/** support for generic hexagonal boards in various convex shapes, including
 * coordinate system transforms and drawing functions.
 *
 * @author ddyer
 *
 */
public abstract class hexBoard<CELLTYPE extends cell<CELLTYPE>> extends gBoard<CELLTYPE>
{
    /* the x,y offset to adjacent cell in any of the 6 directions of a hexagonal board. 
     * these are mostly superseded by using the exitToward method of the cells 
     */
    private static final int[] hex_dys = { -1, +1, +2, +1, -1, -2 }; 	//x offsets for adjacent space
    private static final int[] hex_dxs = { -1, -1, 0, 1, 1, 0 };		//y offsets for adjacent space
    public int[] dxs() { return hex_dxs; }
    public int[] dys() { return hex_dys; }
    public Geometry geometry() { return Geometry.Hex; }
    
    public double yCellRatio() { return(1.165); }
    public double yGridRatio() { return(Math.sqrt(3.0)); }
    /** for hexagonal geometry, add this to rotate to the opposite direction */
    public static final int CELL_HALF_TURN = 3;
    /** for hexagonal geometry, this is the modulus of the clock (ie; 6 steps) */
    public static final int CELL_FULL_TURN = 6;
    /** this is the direction for {@link cell#exitTo} to go visually down */
    public static final int CELL_DOWN = 0;
    /** this is the direction for {@link cell#exitTo} to go visually up */
    public static final int CELL_UP = 3;
    /** this is the direction for {@link cell#exitTo} to go visually down and left */
    public static final int CELL_DOWN_LEFT = 1;
    /** this is the direction for {@link cell#exitTo} to go visually up and left */
    public static final int CELL_UP_LEFT = 2;
    /** this is the direction for {@link cell#exitTo} to go visually up and right */
    public static final int CELL_UP_RIGHT = 4;
    /** this is the direction for {@link cell#exitTo} to go visually down and right */
    public static final int CELL_DOWN_RIGHT = 5;
	public static final int CELL_FULL_TURN() { return(CELL_FULL_TURN); }
	public static final int CELL_HALF_TURN() { return(CELL_HALF_TURN); }

    /**
     * find the direction from fc,fr to tc,tr (given that they are on a line)
	 * these directions are appropriate for {@link cell#exitTo}.
	 */
	public int findDirection(char fc,int fr0,char tc,int tr0)
	  {	
	  	if(fc==tc) { return((fr0<tr0)? CELL_UP_LEFT : CELL_DOWN_RIGHT); }
	  	{	int fr = BCtoYindex(fc,fr0);
	  		int tr = BCtoYindex(tc,tr0);
	  		if(tr<fr) { return((tc<fc) ? CELL_DOWN : CELL_UP_RIGHT); }
	  		return((tc<fc) ? CELL_DOWN_LEFT : CELL_UP);
	  	}
	  }
   
    /** there are two common styles of user-level coordinates for
     *  hexagonal boards.  Columns are pretty universally named A,B,C etc,
     *  but the rows can be either "1 Origin" where the cell above A is 1,
     *  or "Diagonal Origin" where each diagonal row is consistently
     *  numbered.   "1 Original" is the defult.  Set diagonal_grid=true
     *  to use the alternate scheme.
     *
     *  Note that this scheme affects how Grid coordinates are translated
     *  into low level board coordinates.   This translation is not intuitive
     *  and tends to be very confusing.  This preferred style is that ALL
     *  communication outside this class be in terms of grid coordinates
     *  such as C3 rather than board coordinates.
     */
    public boolean diagonal_grid = false;

    private int[] firstRowOffset = null; // number of the first cell in a column, zero origin.  This
    			// determines the real shape of the board.
    private int[] nInRow = null;		// number of hexes in individual rows (derived)
    private char[] firstColInRow = null; 	// the column of the first row with this number (derived)
    private int minZ = 999;
    
	// display and initialization utilities
    public char firstColInRow(int row) { return(firstColInRow[row-1]); }
	public int firstRowInColumn(char col) { return(1+firstRowInCol[col-'A']); }
	public int lastRowInColumn(char col) { return(firstRowInCol[col-'A']+nInCol[col-'A']); }
	
	// this is maintained by initboard for the benefit of the tricolor algorithm
	private boolean rombus = false;

	/**
			return the index to 3-color a hexagonal grid.  This is partially ad-hoc
			so it may not work for all cases.  It is believe to work for "hex" style
			rombus boards and for perfect hex-hex boards			 
			* @param col
			* @param row
			* @return 0-2
			*/
	public int triColor(char col,int row)
	{	
		int col0 = col-'A'+1;
		if(rombus || col0*2<=ncols) 
			{ col=(char)(ncols-col0+'A');  } 
		return ((col- row )%3);	
	}

    /**
     * initialize a hexagonal board.  This is somewhat confusing and mysterious, so a few examples
     * are the best guide.
     * <li>a 5 on a side hexagonal shaped board
     * <pre>
     *  initBoard(
     *  	new int[]{ 4, 3, 2, 1, 0, 1, 2, 3, 4 },
     *  	new int[] {5, 6, 7, 8, 9, 8, 7, 6, 5 },
     *  	null); 
     * </pre>
     * <li>a 11-on a side rhombus shaped board
     * <pre>
     * initBoard(
     * 	new int[] { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 },
     *  new int[] { 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11 },
     *  null);
     * </pre>
     * <li>a "punct style" board with missing corners
     * <pre> 
     * initBoard(
     * 	new int[]{   9,  6,  5,  4,  3,  2,  1,  0,  1,  0, 1, 2, 3, 4, 5, 6, 9  },
     *  new int[]{   7, 10, 11, 12, 13, 14, 15, 16, 15, 16,15,14,13,12,11,10, 7 },
     *  new int[]{   1,  0,  0,  0,  0,  0,  0,  0,  1,  1, 2, 3, 4, 5, 6, 7, 9 },
     * )
     * </pre>
     * @param fc an array designating the actual row number of the row numbered 1 in each column.
     * @param nc an array designating the number of cells in each column
     * @param fcol (optional) an array designating row number of the first actual row in each column
     */
    public void initBoard(int[] fc, int[] nc, int[] fcol)
    {	geometry = Geometry.Hex;
        ncols = fc.length;
        nrows = 0;
        for (int i = 0; i < ncols; i++)
        {
            nrows = Math.max(nrows, fc[i] + nc[i]+1);
        }
        rombus = true;
        firstRowInCol = fcol;
        diagonal_grid = (firstRowInCol != null);

        if (!diagonal_grid)
        {
            firstRowInCol = new int[ncols]; // an array of zeros
        }

        if (Grid_Style == null)
        {
            Grid_Style = new String[3];

            if (diagonal_grid)
            {
                Grid_Style[GRID_LEFT] = "1"; // default to letters across the bottom, numbers along the side
                Grid_Style[GRID_BOTTOM] = "A";
            }
            else
            {
                Grid_Style[GRID_TOP] = "A1"; // default to letters and numbers across the top and bottom 
                Grid_Style[GRID_BOTTOM] = "A1";
            }
        }

        //boardDimX = (ncols * 2) + 1;
        firstRowOffset = fc;
        nInCol = nc;
        nInRow = new int[nrows];
        firstColInRow = new char[nrows];
        
        createBoard(ncols,(nrows * 2));

        
        int maxZ = 0;
        for (int col = 0; col < ncols; col++)
        {
            char thiscol = (char) ('A' + col);
            int lastincol = nInCol[col];
            rombus &= (lastincol==ncols);

            for (int thisrow0 = 1, thisrow = 1+firstRowInCol[col];
                    thisrow0 <= lastincol; thisrow0++, thisrow++)
            { //where we draw the grid
            	CELLTYPE newc = newcell(thiscol,thisrow);
            	newc.next = allCells;
            	allCells = newc;
            	SetBoardCell(thiscol,thisrow,newc);
            	
            	if(nInRow[thisrow-1]==0) 
            		{// this works because we encounter the rows left to right
            		firstColInRow[thisrow-1] = thiscol; 
            		}
            	nInRow[thisrow-1]++;
            	
            	minZ = Math.min(minZ,thiscol-thisrow);
            	maxZ = Math.max(maxZ,thiscol-thisrow);
            	
            }}
        
    		G.Assert(geometry==allCells.geometry,"board geometry should match cell geometry");

        	setCrossLinks();
  
    }
    private int prevfc[] = null;
    private int prevnc[] = null;
    private int prevfcol[] = null;
    /**
     * create or re-initialize a board.  This depends on the reInit() method
     * to reinitialize the cells, and is appropriate where the random values
     * associated with the cells are not predictable, and where the board
     * construction is complete with initBoard.   Do not use this method 
     * when the rough method is followed by additional construction steps.
     * 
     * @param fc
     * @param nc
     * @param fcol
     */
    public boolean reInitBoard(int[] fc, int[] nc, int[] fcol)
    {
    	if(allCells!=null && fc==prevfc && nc==prevnc && fcol==prevfcol)
    	{
    		for(CELLTYPE c = allCells; c!=null; c=c.next) { c.reInit(); } 
    		return(false);
    	}
    	else
    	{	prevfc = fc;
    		prevnc = nc;
    		prevfcol = fcol;
    		initBoard(fc,nc,fcol);
    		Random r = new Random(0x524673*nrows*ncols);
    		allCells.setDigestChain(r);
    		return(true);
    	}
    }

    /**
     * convert board coordinates to y index of board array.
     * This clever representation allows adjacency to be computed easily.
     */
    public final int BCtoYindex(char col, int row)
    {
        int col0 = col - 'A'; // convert A=0 B=1
        int y = ((col0 >= 0) && (col0 < ncols))
            ? ((2 * ((row-1) - +firstRowInCol[col0])) + firstRowOffset(col0)) : 0; // y index in the board array
        return (y); // y index in the board array
    }

    /* convert x,y to a row.
    */
    public final int YindexToBC(int x, int y)
    {	if(isTorus)
    	{
    	// this is complicated and partially ad-hoc.  If constructing a torroidal
    	// board, we start at a real cell and project and x,y to get to the next
    	// cell, which puts us off the board.  We wrap columns easily, but the rows
    	// are modified by the grid offsets that shape the board.  So if we're wrapping
    	// we have to compensate for these offsets.  This logic works for simple hex
    	// boards, but probably would not work for a board with a nontrivial shape.
    	// Fortunately, this is used only when setting up the object linkages among
    	// the cells, and is checked by the crosslink routine.
        int col0 = x;
        int y0 = y;
        // this logic only tries to work if we step only one cell
        if(col0==-1) {  col0 += ncols; y0 -= (firstRowOffset(0)-firstRowOffset(ncols-1)+1); }
        else if(col0==ncols) { col0 -= ncols; y0 += (firstRowOffset(0)-firstRowOffset(ncols-1)+1); }
        G.Assert(((col0 >= 0) && (col0 < ncols)),"%s not in board",col0);
        int row = (((y0 - firstRowOffset(col0)) / 2)+1 + firstRowInCol[col0]);
        return(torusWrapRow((char)('A'+col0),row)); 
    	}
       
    int col0 = x;//(x - 1) / 2;
    int row = ((col0 >= 0) && (col0 < ncols))
           ? (((y - firstRowOffset(col0)) / 2)+1 + firstRowInCol[col0]) : 0;
    return (row);
    }
    
    public final char torusWrapCol(char col) 
    { if(col<'A') { col+=ncols; }
      else if (col>=('A'+ncols)) { col -= ncols; }
      return(col);
    }
    public final int torusWrapRow(char col0,int row0)
    {	int col = col0-'A';
    	int row = row0-firstRowInCol[col];
    	if(row<=0) { row0+=nInCol[col]; }
    	else if(row>nInCol[col]) {row0-=nInCol[col]; }
    	return(row0);
    }
    /*
        Public interfaces which use Column,Row addresses
    */
    public final boolean validBoardPos(char col0, int row0)
    {
        int col = col0 - 'A';

        if ((col >= 0) && (col < +ncols))
        {
            int row = row0 - firstRowInCol[col];

            return ((row > 0) && (row <= nInCol[col]));
        }

        return (false);
    }

    /* convert board coordinates to x index of board array
       This clever representation allows adjacency to be computed easily.
     */
    public final int BCtoXindex(char col, int rowx)
    {
        int col0 = col - 'A'; // convert A=0 B=1
        //int x = (col0 * 2) + 1; // x index in the board array
        //return (x); // x index in the board array
        return(col0);
    }

    /* convert x,y to a column.
     */
    public final char XindexToBC(int x, int yx)
    {
        int col0 = x;//(x - 1) / 2;
        char col = (char) ('A' + col0);
        if(isTorus) { col=torusWrapCol(col); }
        return (col);
    }

    /**
     * convert col,row into a row number, considering the reverse x and reverse y flags.
     * @param col
     * @param arow
     * @return an integer
     */
    public final int geo_rownum(char col,int arow)
    {	if(displayParameters.reverse_y)
    	{
    	int fcol = Math.max(Math.min(ncols-1,col-'A'),0);	// can be out of range when asking about border rows for the grid
    	int lcol = geo_colnum(col);
    	return nInCol[fcol]-(arow-1-firstRowInCol[fcol])+firstRowInCol[lcol];
    	}
	    else
	    {
	    	return arow;
	    }

    }

    /**
     * convert col into a column number, considering the reverse x and reverse y flags.
     * @param colchar
     * @return an integer
     */
    public final int geo_colnum(char colchar)
    {	
    	int col = colchar - 'A';
    	if(displayParameters.reverse_x!=displayParameters.reverse_y) 
    		{ col = (ncols-col)-1; 
    		}
    	return(col);
    }
    public int firstRowOffset(int col)
    {
    	return isTorus ? ncols-col : firstRowOffset[col];
    }
 
    /** convert cell col,row to Y coordinate
     * 
     */
    public double cellToY00(char colchar, int arow)
    {
        int col = geo_colnum(colchar);
        int thisrow = geo_rownum(colchar,arow);
        int coffset = firstRowOffset(col);
        double y0 = ((thisrow - firstRowInCol[(col+ncols)%ncols] ) * displayParameters.YCELLSIZE) 
        		+ (coffset * displayParameters.GRIDSIZE)
        		;

        return (y0);
    }

    /** convert cell col,row to X coordinate
     * 
     */
     public double cellToX00(char colchar, int row)
    {	
        int col = geo_colnum(colchar);
        double col2 = ((col+1) * displayParameters.CELLSIZE);
        return col2;

    }
    /**
    // 
    //  This shows how to iterate over the board by rows, in all three directions.
    //  this is the hard way.  The easy way is to use the exitToward method
    void mapcells(char fcol,int frow,int player,int inc)
    {	int fcolnum=fcol-'A';
    	// step for constant column
     	for(int row=1+firstRowInCol[fcolnum],lastcol=row+nInCol[fcolnum]; row<lastcol;row++)
    	{
     	}
    	// step for constant row
       	for(int colnum=0,lastcolnum=nInRow[frow-1]; colnum<lastcolnum;colnum++)
    	{
     	}
       	{
    	// step for constant row-col
       	int zidx = fcol-frow-minZ;
       	char col = firstZ[zidx];
       	int row = frow-(fcol-col);
       	for(int znum = 0,lastZ=nZ[zidx]; znum<lastZ; znum++,col++,row++)
    	{	
    	}
       	}
    }
    */
}
