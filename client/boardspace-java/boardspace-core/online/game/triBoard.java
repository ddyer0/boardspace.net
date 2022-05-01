package online.game;
import lib.G;
import online.game.cell.Geometry;
/** support for generic triangular grid boards.  This geometry
 * is used by Spangles. 
 *
 * @author ddyer
 *
 */

public abstract class triBoard<CELLTYPE extends cell<CELLTYPE>> extends gBoard<CELLTYPE>
{
    /*  
     * the board is oriented such that all triangles have a horizontal base, with
     * alternating triangles pointing "up" or "down".  "Down" means from tip to
     * base, and "down" is direction #2.  Direction 0 is visually left for both
     * up pointing and down pointing cells. Direciton 1 is right for both up and
     * down cells.  Therefore, the reverse direction if a->b is 2<>2 0<>1 1<>0
     */
    private static final int reverseDirection[] = {1,0,2};
    private static final int dxs[] = { -1, 1, 0};
    private static final int dys[] = { 0,0,1 };
    public static final int CELL_FULL_TURN  = 3;
    public double yCellRatio() { return(Math.sqrt(3.0)); }
    public double yGridRatio() { return(Math.sqrt(3.0)); }

    public int cellToX(CELLTYPE c)
    {	
   	 int xp = cellToX(c.col,c.row);
   	 if(isTorus)
   	 { 	if(xp+displayParameters.xspandist<0) { xp = cellToX((char)(c.col+ncols),c.row); }
   	 		else if(xp-displayParameters.xspandist>displayParameters.WIDTH) { xp = cellToX((char)(c.col-ncols),c.row); }
   	 }
   	 return xp;
    }
    public int cellToY(CELLTYPE c)
    {	int yp = cellToY(c.col,c.row);
    	if(isTorus)
    	{
    		if(yp+displayParameters.yspandist<0) 
    			{ yp = cellToY(c.col,c.row+nrows); 
    			}
    		else if(yp-displayParameters.yspandist>displayParameters.HEIGHT)
    		{
    			yp = cellToY(c.col,c.row-nrows);
    		}
    	}
    	return yp;
    }

    /**
     *  convert col,row to y with no rotation or perspective
     */
    public double cellToY00(char colchar, int thisrow)
    {	int odd = (colchar&1);
    	return(thisrow * displayParameters.YCELLSIZE+odd*displayParameters.YCELLSIZE/4);
    }

    /**
     *  col,row to x with no rotation or perspective
     */
    public double cellToX00(char colchar, int row)
    {
        int col = colchar - 'A';
        int odd = (row&1);
        return (col * displayParameters.CELLSIZE+odd*displayParameters.CELLSIZE);
    }

	//  find the direction from fc,fr to tc,tr (given that they are on a line)
	//  these directions are appropriate for cell.exitToward.
	public int findDirection(char fc,int fr0,char tc,int tr0)
	  {	if(fr0==tr0) { return((fc<tc) ? 1 : 0); }
	  	return(2);
	  }
   
    /** there are two common styles of user-level coordinates for
     *  hex boards.  Columns are pretty universally named A,B,C etc,
     *  but the rows can be either "1 Origin" where the cell above A is 1,
     *  or "Diagonal Origin" where each diagonal row is consistantly
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

    

	// display and initialization utilities
	public int firstRowInColumn(char col) { return(1+firstRowInCol[col-'A']); }
	public int lastRowInColumn(char col) { return(firstRowInCol[col-'A']+nInCol[col-'A']); }
//
//  set up the crosslinks for use by the cell's exitToward method
//  and verify the result.  This also illustrates the "old way" 
//  of navigating to adjacent cells, by going through the dx/dy table
//
// note for torroidal boards, the dimensions must be even
 private void setCrossLinks()
 {	
    for(CELLTYPE cc = allCells; cc!=null; cc=cc.next)
     {	int xc = BCtoXindex(cc.col,cc.row);
 		int yc = BCtoYindex(cc.col,cc.row);
 		// do this in this convoluted way so we take into account
 		// the various ways that hex grids can be numbered.  They all
 		// use the same underlying 2x1 array architecture.  Square
 		// grids are easier.
 		for(int dir = 0;dir<cc.geometry.n; dir++)
 		  { int xn = xc + ((dys[dir]==0) ? dxs[dir] : (((cc.row&1)!=0)?1:-1));
 		    int yn = yc + (dys[dir]*(((cc.col&1)!=0)?1:-1));
 		    // note that XtoBC and YtoBC are the point where a torus wraparound
 		    // is handled.
 		    char xbc = XindexToBC(xn,yn);
 		    int ybc = YindexToBC(xn,yn);
 		    CELLTYPE nc2 = getCell(xbc,ybc);
 		    if(isTorus && (nc2==null)) 
 		    {	 getCell(XindexToBC(xn,yn),YindexToBC(xn,yn));
 		    	throw G.Error("Missing cell %s %s",xn,yn);
 		    }
  		    cc.addLink(dir,nc2);
 		  }
  	 }
   for(CELLTYPE c=allCells; c!=null; c=c.next) { c.countLinks(); }
     // verify that all the crosslinks are correct - that if a links to b, b links to a
     // in the opposite direction as defined by exitToward.
 	for(CELLTYPE c = allCells; c!=null; c=c.next)
 	{	
 		for(int dir=0;dir<c.geometry.n;dir++)
 		{	CELLTYPE c1 = c.exitTo(dir);
 			if(isTorus) { G.Assert(!c1.isEdgeCell(),"not an edge cell"); }
 			if(c1!=null) 
 			{ CELLTYPE c2 = c1.exitTo(reverseDirection[dir]);
 			  int dirto = findDirection(c.col,c.row,c1.col,c1.row);
 			  int dirfrom = findDirection(c1.col,c1.row,c.col,c.row);
			  G.Assert(c2==c,"reverse map");
			  G.Assert(isTorus || ((dirto==dir)&&(dirfrom==reverseDirection[dir])),"direction mapping");
 			}
 		}
 	}
 }
 /**
  * create a board with 
  * @param fc
  * @param nc
  * @param fcol
  */
    public void initBoard(int[] fc, int[] nc, int[] fcol)
    {	geometry = Geometry.Triangle;
        ncols = fc.length;
        nrows = 0;
        G.Assert(fc.length==nc.length,"rows and cols match");
        for (int i = 0; i < ncols; i++)
        {
            nrows = Math.max(nrows, nc[i]);
        }

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
        int boardDimX = ncols;
        int boardDimY = (nrows * 2);
        nInCol = nc;
        
        int []nInRow = new int[nrows];
        char []firstColInRow = new char[nrows];
        
        createBoard(boardDimX,boardDimY);
        
        for (int col = 0; col < ncols; col++)
        {
            char thiscol = (char) ('A' + col);
            int lastincol = nInCol[col];

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
            	
            }}
        	G.Assert(geometry==allCells.geometry,"board geometry should match cell geometry");
        	setCrossLinks();
  
    }

    /* convert col,row to a x */
    public final int BCtoXindex(char col, int row)
    {	return(col-'A');
    }
    /* convert x,y to a column.  */
    public final char XindexToBC(int x, int y)
    {	if(isTorus)
    		{ if(x>=ncols) { x-= ncols; }
    		  if(x<0) { x+= ncols; }
    		}
    	return((char)('A'+x));
    }
     /* convert col,row to a y */
    public final int BCtoYindex(char col, int row)
    {	return(row-1);
    }

    /* convert x,y to a row.  */
    public final int YindexToBC(int x, int y)
    {	if(isTorus)
			{
    		if(y>=nInCol[0]) { y-= nInCol[0]; }
	  		if(y<0) { y+= nInCol[0]; }
			}
    	return(y+1);
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

}
