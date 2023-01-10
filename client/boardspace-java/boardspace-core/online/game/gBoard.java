package online.game;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import online.game.cell.Geometry;
import online.game.commonCanvas.Itype;

import static java.lang.Character.*;
import java.util.Enumeration;

import lib.*;

/* this is the base class for board geometry and coordinate systems for board where
 * the cells are in any of the common forms of grids
 * hexBoard  rectBoard and circBoard are built on top an game specific boards
 * on top of them.  The recommended access method to boards are these
 * 
 * 	public cell allCells();
 *	public void SetDisplayRectangle(Rectangle r);
 *	public int cellToX(char col,int row);
 *	public int cellToY(char col,int row);
 *	public Point decodeCellPosition(int x,int y,double cs);
 *	public Point encodeCellPosition(int x,int y,double cs);
 *	public void DrawGrid(Graphics g,Rectangle r,boolean use_grid,
 *	         Color backgroundColor, Color pointColor, Color lineColor, Color gridColor)
 *	public int findDirection(char col1,int row1,char col2,int row2);
 *	public void SetDisplayParameters(double xscale,double yscale,double xper,double yper,
 *			double rot,double xoff,double yoff);
 *  public cell closestCell(int x,int y);
 *  
 * to access the cells in some raster order
 *  public char firstColumn();
 *	public char lastColumn();
 *  public int firstRowInColumn(char col);
 *	public int lastRowInColumn(char col);
 */
class Bbox
{
	int left=0;
	int top=0;
	int right=-1;
	int bottom=-1;
	void addPoint(int x,int y)
	{
		if (left>right)
        {   left = x;
            top = y;
            right = left;
            bottom = top;
        }
        else
        {   if (x < left) {  left = x;  }
            if (x > right)  { right = x;  }
            if (y < top) {  top = y; }
            if (y > bottom) { bottom = y; }
        }
	}
}
public abstract class gBoard<CELLTYPE extends cell<CELLTYPE>> extends RBoard<CELLTYPE> implements Opcodes
{	
	
	/**
	 * this Enumeration implements iteration over all the cells in a board
	 * with the guarantee that the enumeration order will be left-right-top-bottom
	 * or right-left-top-bottom as seen on the screen.  This allows considerations
	 * of shadows and occlusion to be considered independent of the current rotation
	 * of the board.  It also hides some complex iteration logic.
	 * @author Ddyer
	 *
	 */
    public class BoardIterator implements Enumeration<CELLTYPE> 
    {	gBoard<CELLTYPE> b;
    	int firstCol;
    	int thisCol;
     	int stepCol;
    	int lastCol;
    	boolean hasMore = false;
    	int thisRow;
    	int lastRow;
    	int stepRow;
    	int seq = 0;
    	int colNum;
    	boolean swapXY = false;
    	boolean swapOn = false;
    	Itype type;
    	public String toString() { return("<boardIterator "+type+">"); }
    	/*
    	 * create an iterator for the board
    	 */
    	BoardIterator(gBoard<CELLTYPE> board,Itype typ)
    	{	type = typ;
    		b = board;
    		swapXY = b.displayParameters.swapXY;
    		switch(type)
    		{
    		case LRBT:
    		case LRTB:
    		case RLTB:
    			// hexagonal boards are represented as fixed number of columns and a variable number
    			// of rows per column, so it's only convenient to iterate down the column
    			G.Assert(board.geometry!=Geometry.Hex,"for Hexagonal boards, use TBRL or TBLR, not %s",type);
    			G.Assert((board.geometry!=Geometry.Hex) || !swapXY, "swapXY not supported for hexagonal boards");
    		default: break;
    		}
    		
    		switch(type)
    		{
    		case BTLR:
    		case LRBT:
     		case LRTB:	
    		case TBLR:
    			// left to right
    			firstCol = thisCol = b.leftColNum();
    			lastCol = b.rightColNum();
    			stepCol = b.stepColNum();
    			break;
    		case BTRL:
    		case RLTB:
    		case TBRL:
    			// right to left
      			stepCol = -b.stepColNum();
    			swapOn = swapXY;
    			firstCol = thisCol = b.rightColNum();
    			lastCol =  b.leftColNum();
      			break;
    		}
    		switch(type)
    		{
       		case BTRL:
       		case BTLR:
    		case LRBT:
    			// bottom to top
   	   			lastRow = b.topRowInColumn((char)('A'+thisCol));
        		thisRow = b.bottomRowInColumn((char)('A'+thisCol));
        		stepRow = -b.stepRow();
    			break;
      		case LRTB:
    		case TBLR:
    		case RLTB:
    		case TBRL:
    			// top to bottom
    			stepRow = b.stepRow();
      			thisRow = b.topRowInColumn((char)('A'+thisCol));
    			lastRow = b.bottomRowInColumn((char)('A'+thisCol));
     		}
   			if(swapOn)
			{
				stepCol = -stepCol;
				stepRow = -stepRow;
				int t = firstCol;
				thisCol = firstCol = lastCol;
				lastCol = t;
				t = thisRow;
				thisRow = lastRow;
				lastRow = t;				
			}

    		hasMore = true;
    	}
    	
    	public CELLTYPE nextElement()
    	{	
     		CELLTYPE c = b.getCell((char)('A'+thisCol),thisRow);
    		seq++;
    		switch(type)
    		{
    		case BTRL:
    		case BTLR:  			
    		case TBLR:
    		case TBRL:
	    		// normal step for raster scans
	    		if(thisRow == lastRow) 
	    			{
	    				if(thisCol==lastCol) { hasMore = false; }
	    				else {
	    				thisCol += stepCol;

	    				switch(type)
	    				{
	    				case TBLR:
	    				case TBRL:
	    					thisRow  = b.topRowInColumn((char)('A'+thisCol));
	    					lastRow = b.bottomRowInColumn((char)('A'+thisCol));
	    					break;
	    				case BTRL:
	    				case BTLR:
	    					lastRow  = b.topRowInColumn((char)('A'+thisCol));
	    					thisRow = b.bottomRowInColumn((char)('A'+thisCol));
	    					break;
						default:	// can't happen
							break;
	    				}}
	    			}
	    		else { thisRow += stepRow;
	    			}
    			break;
    		case LRBT:
    		case LRTB:
    		case RLTB:
	    		if(swapXY)
	    		{	// swapXY is true for rotation 1 and 3.  We want to iterate
	    			// with rows as the fast axis instead of columns
	    			if(thisRow==lastRow)
	    			{
	    				if(thisCol==lastCol) { hasMore = false; }
	    				else {
	    				thisCol += stepCol;
	    				thisRow = b.topRowInColumn((char)('A'+thisCol));
	    				lastRow = b.bottomRowInColumn((char)('A'+thisCol));
	    				if(swapOn)
	    				{	// rotation 3 is backwards on both axes
	    					int t = thisRow;
	    					thisRow = lastRow;
	    					lastRow = t;
	    				}
	    				}
	    			}
	    			else { thisRow += stepRow; }
	    		}
	    		else
	    		{
	    		// normal step for raster scans
	    		if(thisCol == lastCol) 
	    			{
	    				if(thisRow==lastRow) { hasMore = false; }
	    				else {
	    				thisRow += stepRow;
	    				thisCol = firstCol;
	    				}
	    			}
	    		else { thisCol += stepCol;
	    			}
	    		}
	    		break;
    		}
    		return(c);
    	}
        
    	public boolean hasMoreElements() {
    		
    		return hasMore;
    	}

    }
    /**
     * get an Enumeration for the board's cells.
     * @param e
     * @return an Enumeration of the board's cell type.
     */
    public Enumeration<CELLTYPE> getIterator(Itype e)
    {
    	return(new BoardIterator(this,e));
    }

	public Geometry geometry = null;		// the expected geometry for the board
    /**
     * if true, display top to bottom instead of bottom to top on the Y axis.  This is intended
     * for games that want to offer a "reverse" view.  This is only fully tested for simple
     * square geometry games.
     * <p> in addition to changing the piece position, it also changes the grid numbering
     * and the mouse position encoding so spectators still see you pointing at the correct point.
     */
	
	/* set reverse_y if the color map is not an identity.  This is typically
	 for 2 player games where the players have swap colors.  This characteristic
	 is sticky, once set, it persists for replayed games.
	*/
	public void autoReverseY() 
	{  displayParameters.reverse_y = (getColorMap()[0]!=0); 
	   displayParameters.autoReverseY = true;
	   displayParameters.autoReverseYNormal=false;}
	/* same as autoReverseY except the reversal is when the color map is normal */
	public void autoReverseYNormal() 
	{  displayParameters.reverse_y = (getColorMap()[0]==0); 
	   displayParameters.autoReverseYNormal = true; 
	   displayParameters.autoReverseY=false;}

	public void setColorMap(int[]map)
	{
		super.setColorMap(map);
		if(displayParameters.autoReverseY) { autoReverseY(); }
		if(displayParameters.autoReverseYNormal) { autoReverseYNormal(); }
	}
    /**
     * return the increment needed to step from topRow() toward bottomRow()
     * the recommended iterator for rows is<br>
     * <code>for(int row=topRow(),last=bottomRow(),stepRow=stepRow();
     * 			 row-stepRow!=last;
     * 			 row += stepRow) {}
     * </code>
     * @return + - 1
     */
	private int stepRow() { return(displayParameters.reverse_y ? 1 : -1); }
    /**
     * return the increment needed to step from leftCol() toward rightCol()
     * the recommended iterator for rows is<br>
     * <code>int stepCol = stepCol();
     * <br>for(char col=leftCol(),lastCol=rightCol();
     * 			 (col-stepCol)!=lastCol;
     * 			 col += stepCol) {}
     * </code>
     * @return an int
     */
    private int stepColNum() { return((displayParameters.reverse_y==displayParameters.reverse_x)?1:-1);}
    /**
     * return the column number that is being displayed visually at the left of the window
     * considering the reverse_x and reverse_y switch.   Shadows are normally cast toward the left,
     * so drawing should proceed from left to right.  However, some board perspectives may work
     * better right to left.
     * @return an integer
     */
    private int leftColNum() { return((displayParameters.reverse_y==displayParameters.reverse_x)?0:ncols-1);}
    /**
     * same as leftColNum but for drawing the grid
     */
    private int gridLeftColNum() { return(leftColNum()); }


    /**
     * return the column number that is being displayed visually at the right of the window
     * considering the reverse_x and reverse_y switch.   Shadows are normally cast toward the left,
     * so drawing should proceed from left to right.  However, some board perspectives may work
     * better right to left.
     * @return an integer
     */
    private int rightColNum() { return((displayParameters.reverse_y==displayParameters.reverse_x)?ncols-1:0); }
    /**
     * same as rightColNum but for drawing the grid
     * @return an integer
     */
    private int gridRightColNum() { return(rightColNum()); }
    
   
    /** if isTorus is true, the edges of the board will be connected to create
     * an edge less board.   I'm not sure if this makes sense for non-square
     * boards.   At present (7/2006) this is actually implemented for hex
     * boards for use by Hive.
     */
    public boolean isTorus=false;

	
    /**
     * the number of rows needed to display the board
     */
	public int nrows; 				//rows needed to display the board
     /**
      * the number of columns needed to display the board
      */
    public int ncols; 					// columns needed to display the board
    public int firstRowInCol[]=null;	// for support of non-square boards (ie domination, punct)
    public int nInCol[]=null;			// for support of non-square boards (ie domination, punct)
    
    /**
    * the actual board is represented by a 2d array of cells, which
    * are addressed using column, row. Oddly shaped boards such as
    * for yinsh, and odd coordinate systems such as for punct are
    * handled by the translation routines. 
    */
 	private cell<CELLTYPE>[][]board=null;		// the actual board
 	public cell<CELLTYPE>[][]getBoardArray() { return(board); }

    /**
     * find the distance from "from" to "to", given that they are known to be in a line.
     * if not, all bets are off!
     * @param from
     * @param to
     * @return the number of steps between the points
     */
    public int findDistance(CELLTYPE from, CELLTYPE to)
    {	if(from.onBoard && to.onBoard)
    	{
    	int dir = findDirection(from.col,from.row,to.col,to.row);
    	int dis = 0;
    	CELLTYPE current = from;
    	while(current!=to)
    	{
    		current = current.exitTo(dir);
    		G.Assert(current!=null && current!=from, "from %s and to %s are not on a line",from,current);
    		dis++;
    	}
    	return(dis);
    	}
    	// pick/drop to or from the reserve
    	return(1);
    }
     /** 
     * translate col,row for a cell into x index into the board array.  This has nothing to do with pixels on a screen.
     * @param col
     * @param row
     * @return an integer
     */
    protected abstract int BCtoXindex(char col,int row);
    /**
     * translate col,row for a cell to y index into the board array.  This has nothing
     * to do with pixels on a screen.
     * @param col
     * @param row
     * @return an integer
     */
    protected abstract int BCtoYindex(char col,int row);
    // these methods inverse map from array coordinates to column,row
    // normally this method is only used during board construction
    /**
     * inverse translate x,y indexes into the board array into col.  This is used during board setup as part
     * of the consistency checking on the the board/cell coordinate system.  This has nothing to do with pixels on a screen.
     * @param x
     * @param y
     * @return an integer
     */
    protected abstract int YindexToBC(int x,int y);
    /**
     * inverse translate x,y indexes into a row.  This is used during board setup as part
     * of the consistency checking on the board/cell coordinate system.  This has nothing to do with pixels on a screen.
     * @param x
     * @param y
     * @return a char
     */
    protected abstract char XindexToBC(int x,int y);
    /**
     * return true if col,row corresponds to a valid board cell
     * @param col
     * @param row
     * @return a boolean
     */
    public abstract boolean validBoardPos(char col,int row);
    /**
     * find the direction from x,y to x1,y1 in the current board geometry.
     * @param fc
     * @param fr0
     * @param tc
     * @param tr0
     * @return an integer corresponding to a direction code
     */
    public abstract int findDirection(char fc,int fr0,char tc,int tr0);
    /**
     * return the visually top row in a column, considering the current board
     * and the "reverse y" feature.  This is intended to be used in drawBoardElements when a board
     * should be drawn in top down (or bottom up) visual order to make overlapping
     * pieces look right.
     * @param c
     * @return an integer
     */
    private int topRowInColumn(char c) { return(displayParameters.reverse_y?firstRowInColumn(c):lastRowInColumn(c)); }
    /**
     * return the visually bottom row in a column, considering the current board
     * and the "reverse y" feature. This is intended to be used in drawBoardElemennt when a board
     * should be drawn in top down (or bottom up) visual order to make overlapping
     * pieces look right.
     * @param c
     * @return an integer
     */
    private int bottomRowInColumn(char c) { return(displayParameters.reverse_y?lastRowInColumn(c):firstRowInColumn(c)); }

    /**
     * convert col,row to x pixel coordinate with no rotation or perspective
     * @param col
     * @param row
     * @return a double
     */
    public abstract double cellToX00(char col,int row);
    /**
     * convert col,row to y pixel coordinate with no rotation or perspective
     * @param col
     * @param row
     * @return a double
     */
    public abstract double cellToY00(char col,int row);

    /**
     * factory method for the board cells.  This is used to create all the cells
     * in the low level board.
     * @param col columb id - usually 'A'-xx
     * @param row column row = usually 1-nn
     * @return a new instance of the appropriate cell class
     */
    public abstract CELLTYPE newcell(char col,int row);

 	@SuppressWarnings("unchecked")
	public void createBoard(int xdim,int ydim)
    {  
    	board = new cell[xdim][ydim];
    	allCells=null;
    	cellArray = null;
    }
    
    // this is used only during board construction, when we populate
    // the board array with cells, or subtypes of cells.
    void SetBoardCell(char col,int row,CELLTYPE con)
    {	int y = BCtoYindex(col,row);
		int x = BCtoXindex(col,row);
		char nc = XindexToBC(x,y);
		int nr = YindexToBC(x,y);
		// a little consistency check
		//System.out.println("C "+nc + col);
    	G.Assert(((nc==col) && (nr==row)),"%s,%s encodes correctly",col,row);
		board[x][y]=con;
    }

	@SuppressWarnings("unchecked")
	private final CELLTYPE GetBoardCell(char col,int row)
    {	if(validBoardPos(col,row))
    	{
    	int y = BCtoYindex(col,row);
    	int x = BCtoXindex(col,row);
    	return((CELLTYPE)board[x][y]);
    	}
    	return(null); 
    }
    /**
     * get the cell associated with col, row.  This is the "public" version that may be 
     * overridden and augmented by the subclasses.
     * @param col
     * @param row
     * @return the board cell corresponding to the col,row
     */
    public CELLTYPE getCell(char col,int row)
    {
    	return(GetBoardCell(col,row));
    }



    /**
     * unlink the cell, then remove it from the board array too.
     * @param c
     */
    public void removeCell(CELLTYPE c)
    {
    	unlinkCell(c);
    	SetBoardCell(c.col,c.row,null);
    }

    
    /** 
     * verify a copy of this board.  This method checks the
     * cells on the board using {@link cell#sameCell sameCell}.  This method
     * is typically wrapped by each subclass to check other 
     * state in the board. 
     * @param from_b
     */
    public void sameboard(gBoard<CELLTYPE> from_b)
    {	super.sameboard(from_b);
    	cell<CELLTYPE> otherB[][]=from_b.board;
    	int ysize = otherB.length;
    	int xsize = otherB[0].length;
    	G.Assert((ysize==board.length) && (xsize==board[0].length),"Board Dimimensions mismatch");
    	
    }
     

    /** copy from a given board.  This method is typically 
     * wrapped by each subclass.
     * 
     * @param from_b
     */
    public void copyFrom(gBoard<CELLTYPE> from_b)
    {	// copy the contents and dynamic state from the other board
        super.copyFrom(from_b);
	
        isTorus = from_b.isTorus;
        nrows = from_b.nrows;
        ncols = from_b.ncols;
        firstRowInCol = from_b.firstRowInCol;
        nInCol = from_b.nInCol;
        geometry = from_b.geometry;

      }
	

   /**
    * find the closest cell and encode the x,y as a position
    * relative to the cell grid.  Normally the board has it's own
    * mouse zone, so player views with boards in different positions 
    * will still give a fair approximation to the mouse position on the board.
    * @return a point
    * @see #decodeCellPosition
    */
  public Point encodeCellPosition(int x,int y,double cellsize)
  {	CELLTYPE mincell = closestCell(x,y);
   	if(mincell!=null)
   	{	int cellx = cellToX(mincell.col,mincell.row);
   		int celly = cellToY(mincell.col,mincell.row);
   		int reverse =  displayParameters.reverse_y ? -1 : 1;
   		//System.out.println("Close to "+mincell);
   		int xoff = 1000+(int)(10*(reverse*(x-cellx)/cellsize));
   		int yoff = 1000+(int)(10*(reverse*(y-celly)/cellsize));
   		// encode the subcell position in tenths of a cell, chinese remainder
   		// the cell number with the subcell position.  This isn't quite correct
   		// because it should also consider differences in rotation, but we don't
   		// use vastly different rotations, so it's pretty much ok
   		//
   		//mincell = getCell('A',4);
   		//G.print("encode "+mincell.col+mincell.row);
		return(new Point(10000*(mincell.col-'A')+xoff,10000*mincell.row+yoff));
   	}
   	return(null);
  }
  /** decode a cell position (encoded by {@link #encodeCellPosition})
   * to an x,y inside the board rectangle
   */
 public Point decodeCellPosition(int x,int y,double cellsize)
 {	Point val = null;
 	char col = (char)('A'+x/10000);
 	int row = y/10000;
 	if(validBoardPos(col,row))
 	{	int cx = cellToX(col,row);
 		int cy = cellToY(col,row);
 		int subx = x%10000-1000;
 		int suby = y%10000-1000;
 		//G.print("decode "+col+row);
 		int reverse =  displayParameters.reverse_y ? -1 : 1;
 		val = new Point(cx+(int)(reverse*cellsize/10*subx),cy+(int)(reverse*cellsize/10*suby));
 	}
 	return(val);
 }  
/**
 * dxs and dys together define the relative offset to adjacent cells in each
 * direction.  The number of offsets must agree with the geometry of cells on
 * the board - ie; for Hexagonal geometry there must be 6 offsets.
 * This is an internal function used a board setup time.
 * @param dxs an array of x offsets
 * @param dys an array of y offsets
 */
 protected void setCrossLinks(int dxs[],int dys[])
 {	
	G.Assert(dxs!=null && dys!=null,"dxs and dys must be supplied");
 	G.Assert(dxs.length==allCells.geometry.n,"the dxs.length %s must agree with the cell geometry %s",dxs.length,allCells.geometry);
	 int mod = dxs.length;
     for(CELLTYPE cc = allCells; cc!=null; cc=cc.next)
     {	int xc = BCtoXindex(cc.col,cc.row);
 		int yc = BCtoYindex(cc.col,cc.row);
 		// do this in this convoluted way so we take into account
 		// the various ways that hexagonal grids can be numbered.  They all
 		// use the same underlying 2x1 array architecture.  Square
 		// grids are easier.
 		for(int dir = 0;dir<mod; dir++)
 		  { int xn = xc + dxs[dir];
 		    int yn = yc + dys[dir];
 		    // note that XtoBC and YtoBC are the point where a torus wraparound
 		    // is handled.  Call GetBoardCell rather than getCell so we go directly
 		    // to the array without any further interpretation.
 		   CELLTYPE nc2 = GetBoardCell(XindexToBC(xn,yn),YindexToBC(xn,yn));
  		    cc.addLink(dir,nc2);
 		  }
  	 }
   for(CELLTYPE c=allCells; c!=null; c=c.next) { c.countLinks(); }
     // verify that all the crosslinks are correct - that if a links to b, b links to a
     // in the opposite direction as defined by exitToward.
 	for(CELLTYPE c = allCells; c!=null; c=c.next)
 	{	
 		for(int dir=0;dir<mod;dir++)
 		{	CELLTYPE c1 = c.exitTo(dir);
 			if(isTorus) { G.Assert(!c1.isEdgeCell(),"not an edge cell"); }
 			if(c1!=null) 
 			{ CELLTYPE c2 = c1.exitTo(dir+mod/2);
 			  int dirto = findDirection(c.col,c.row,c1.col,c1.row);
 			  int dirfrom = findDirection(c1.col,c1.row,c.col,c.row);
 			  // if this fails, then the "firstInCol" numbers are inconsistent
 			  G.Assert(c2==c,"reverse map, from %s direction %s to %s and back %s",c,dirto,c1,c2);
			  G.Assert(isTorus || ((dirto==dir)&&(dirfrom==(dir+mod/2)%mod)),"direction mapping");
 			}
 		}
 	}

 }
 

 
 /**
 // this sets the basic parameters of the board, to determine the location of the cell grid points
  * within the rectangle.  For simple boards, this is just scale and offset.  With all the parameters,
  * it can be used to match the grid points to an arbitrary piece of artwork.  Unfortunately these
  * parameters are not independently adjustable, so to match artwork some experimentation is required
  * to zero in on the desired grid.
  * 
  * @param scale	overall scale (normal 1.0)
  * @param yscale	y scale relative to overall scale (normal 1.0)
  * @param xoff		x offset in units of cells
  * @param yoff		y offset in units of cells
  * @param rot		rotation in degrees clockwise
  * @param xperspec	x forshortening as a function of y (0 is no effect)
  * @param yperspec y forshortening as a function of y (0 is no effect)
  * @param skew		x multiply as a percentage of y
  */
 public void SetDisplayParameters(double scale, double yscale, double xoff,double yoff,double rot,
         double xperspec, double yperspec,double skew)
 {	displayParameters.SetDisplayParameters(scale, yscale, xoff, yoff, rot, xperspec, yperspec, skew);
 }

 /**
 // this sets the basic parameters of the board, to determine the location of the cell grid points
  * within the rectangle.  
  * 
  * @param scale	overall scale (normal 1.0)
  * @param yscale	y scale relative to overall scale (normal 1.0)
  * @param xoff		x offset in units of cells
  * @param yoff		y offset in units of cells
  * @param rot		rotation in degrees clockwise
  */
 public void SetDisplayParameters(double scale, double yscale, double xoff,double yoff,double rot)
 {
	 SetDisplayParameters(scale,yscale,xoff,yoff,rot,0,0,0);
 }
 
 /**
  * The third method of setting display parameters requires mapping 4 points, and
  * using strict linear interpolation to find the position of the rest of the points.
  * This works well matching grid points to artwork, but only if you have a really
  * orthographic projection.
  * @param LL
  * @param LR
  * @param UL
  * @param UR
  */
 public void SetDisplayParameters(double LL[],double LR[],double UL[],double UR[])
 {	displayParameters.SetDisplayParameters(LL, LR, UL, UR); 
 }
 protected double yCellRatio() { return(1.0); }
 protected double yGridRatio() { return(2.0); }
 
 /**
  * adjust the internal scaling parameters so the board displays
  * inside the specified rectangle.  This works by running a trial
  * transformation of all the points on the board, then scaling the 
  * result to completely fill the rectangle if possible.
  * @param r
  */
 public void SetDisplayRectangle(Rectangle r)
 {	super.SetDisplayRectangle(r);
 	double ycell = yCellRatio();
 	double ygrid = yGridRatio();
 	displayParameters.setDisplayRectangle0(r,ycell,ygrid,ncols);
 	
	 //
	 // do a quick traverse to determine the bounding box
	 // this has an extra row in the calculation, which is 
	 // not technically correct but the existing boards grew up
	 // with it, so so have to keep it.  The extra row is where
	 // the coordinates get drawn.
	 //
	 Bbox br = new Bbox();
	 boolean isHex = allCells.nAdjacentCells()==6;
	 boolean invx = displayParameters.reverse_x;
	 boolean invy = displayParameters.reverse_y;
	 double GRIDSIZE = displayParameters.GRIDSIZE;
	 double YCELLSIZE = displayParameters.YCELLSIZE;
	 
	 //
	 // used normal coordinates, so the box is always calculated the same
	 displayParameters.reverse_x = displayParameters.reverse_y = false;
	 for(CELLTYPE c = allCells; c!=null; c=c.next)
	 {	char col = c.col;
	 	// this is what makes calculating the box tricky for hexagonal grids with reverse coordinates
	 	int thisrow = (isHex&&(c.row==1)) ? 0 : c.row;
	
	     br.addPoint(cellToX(col, thisrow, GRIDSIZE, 0),  cellToY(col, thisrow, GRIDSIZE, 0));
	     br.addPoint(cellToX(col, thisrow, -GRIDSIZE, 0), cellToY(col, thisrow, -GRIDSIZE, 0));
	     br.addPoint(cellToX(col, thisrow, 0, YCELLSIZE), cellToY(col, thisrow, 0, YCELLSIZE));
	     br.addPoint(cellToX(col, thisrow, 0, -YCELLSIZE),cellToY(col, thisrow, 0, -YCELLSIZE));
	 }
	 displayParameters.reverse_x = invx;
	 displayParameters.reverse_y = invy;
	 
	 displayParameters.setDisplayRectangle1(r,br,ycell,ygrid);

 }

 

/**
 * rotate convert col,row to x,y, rotate, and add xoff yoff
 * @param colchar
 * @param row
 * @param offx
 * @param offy
 * @return a double
 */
 private double cellToX0(char colchar, int row, double offx, double offy)
 {	
     double col0 = cellToX00(colchar, row) + offx;
     return displayParameters.rotateX(col0, cellToY00(colchar, row) + offy);
 }

 /**
  * rotate convert col,row to x,y, rotate, and add xoff yoff
  * @param colchar
  * @param thisrow
  * @param offx
  * @param offy
  * @return a double
  */
 private double cellToY0(char colchar, int thisrow, double offx, double offy)
 {
     double y0 = cellToY00(colchar, thisrow) + offy;
     return displayParameters.rotateY(cellToX00(colchar, thisrow) + offx, y0);
 }   
 

 //
 // complete transform col,row to x
 //
 private int cellToX(char colchar0, int rownum0, double offx, double offy)
 {	if(displayParameters.INTERPOLATE)
	 {
	 char colchar = displayParameters.swapXY ? (char)(rownum0+'A'-1) : colchar0;
	 int rownum = displayParameters.swapXY ? colchar0-'A'+1 : rownum0;
	 int xpos = (displayParameters.reverse_x!=displayParameters.reverse_y) ? ncols-(colchar-'A')-1: (colchar-'A');
	 int ypos = displayParameters.reverse_y ? nrows-rownum : rownum-1;
	 double xfrac = (double)xpos/ncols;
	 double yfrac = (double)ypos/nrows;
	 double lxmid = G.interpolateD(xfrac, displayParameters.LLCOORD[0],displayParameters.LRCOORD[0]);
	 double uxmid = G.interpolateD(xfrac,displayParameters.ULCOORD[0],displayParameters.URCOORD[0]);
	 //double lymid = G.interpolateD(xfrac,LLCOORD[1],LRCOORD[1]);
	 //double uymid = G.interpolateD(xfrac,ULCOORD[1], URCOORD[1]);
	 double xp = G.interpolateD(yfrac,lxmid,uxmid);
	 //double yp = G.interpolateD(yfrac,lymid,uymid);
	 return((int)(xp*displayParameters.WIDTH+offx));
	 }
	 else
	 {
     double xpos0 = displayParameters.XOFFSET + cellToX0(colchar0, rownum0, offx, offy);

     if (displayParameters.XPERSPECTIVE > 0.0)
     {
         double ypos0 = cellToY0(colchar0, rownum0, offx, offy);

         return ((int) displayParameters.XtoXP(xpos0, ypos0));
     }

     return ((int) xpos0);
	 }
 }

/**
 * convert col,row to x,y
 * @param colchar
 * @param row
 * @return an integer
 */
 public int cellToX(char colchar, int row)
 {
     return (cellToX(colchar, row, 0, 0));
 }
 /**
  * convert a cell to its x
  * @param c
  * @return an integer
  */
 public int cellToX(CELLTYPE c)
 {	
	 return cellToX(c.col,c.row);
 }

 //
 // complete transform col,row to y
 //
 private int cellToY(char colchar0, int thisrow0, double offx, double offy)
 {	
	 if(displayParameters.INTERPOLATE)
	 {	char colchar = displayParameters.swapXY ? (char)('A'+thisrow0-1) : colchar0;
	 	int thisrow = displayParameters.swapXY ? colchar0-'A'+1 : thisrow0;
		int xpos = displayParameters.reverse_x!=displayParameters.reverse_y ? ncols-(colchar-'A')-1: (colchar-'A');
		int ypos = displayParameters.reverse_y ? nrows-thisrow : thisrow-1;
		double xfrac = (double)xpos/ncols;
		double yfrac = (double)ypos/nrows;
		//double lxmid = G.interpolateD(xfrac, LLCOORD[0],LRCOORD[0]);
		//double uxmid = G.interpolateD(xfrac,ULCOORD[0],URCOORD[0]);
		double lymid = G.interpolateD(xfrac,displayParameters.LLCOORD[1],displayParameters.LRCOORD[1]);
		double uymid = G.interpolateD(xfrac,displayParameters.ULCOORD[1], displayParameters.URCOORD[1]);
		//double xp = G.interpolateD(yfrac,lxmid,uxmid);
		double yp = G.interpolateD(yfrac,lymid,uymid);
		return((int)(yp*displayParameters.HEIGHT+offy));
	 }
	 else
	 {
	     double ypos0 = displayParameters.YOFFSET + cellToY0(colchar0, thisrow0, offx, offy);
	
	     if (displayParameters.YPERSPECTIVE > 0)
	     {
	         return ((int) (displayParameters.YSCALE*displayParameters.YtoYP(ypos0)));
	     }
	
	     return ((int) (ypos0*displayParameters.YSCALE));
	 }
 }
 

/** convert col,row to x,y
 * 
 * @param colchar
 * @param thisrow
 * @return an integer
 */
 public int cellToY(char colchar, int thisrow)
 {	
     return (cellToY(colchar, thisrow, 0, 0));
 }
 /**
  * convert a cell to it's y
  * @param c
  * @return an integer
  */
 public int cellToY(CELLTYPE c)
 {	return cellToY(c.col,c.row);
 }

 /**
  * this is an overridable method of the board, used to draw the grid
  * coordinates.  This allows arbitrary adjustments to fit the artwork.
  */
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {  if(txt!=null && !"".equals(txt))
	 { if(isDigit(txt.charAt(0)))
		{ xpos +=cellsize/4; }
	 GC.Text(gc, true, xpos, ypos, -1, 0,clt, null, txt);
	 }
 }
	/**
	 * 
	 * @param ch
	 * @return true if this char is a letter
	 */
	private boolean isLetter(char ch)
	{
		return(Character.isLowerCase(ch) || Character.isUpperCase(ch));
	}
	
	// draw at the left or right.  This has not been tested for the combination
	// of a hexagonal board and right numbers
	private void drawVerticalGrid(Graphics gc,String left_style,boolean isLeft,
			Color gridColor,int div,
			int colNum,int lastcolNum,int stepCol)
	{
        boolean INTERPOLATE = displayParameters.INTERPOLATE;
        double CELLSIZE = displayParameters.CELLSIZE;
        double GRIDSIZE = displayParameters.GRIDSIZE;
        boolean isHex = allCells.nAdjacentCells()==6;
        int height = G.Height(boardRect);
        char cc = left_style.charAt(0);
        boolean use_cols = isLetter(cc);
        char dc = left_style.charAt(left_style.length() - 1);
        boolean use_digs = isDigit(dc);
        boolean invert_digs = (cc=='2');
        boolean invert_cols = (dc=='B');
        int drawn = 0;
        for(int col = colNum;
        	col!=lastcolNum; 
        	col+=stepCol)
        {
      	 char thiscol = (char) ('A' + (displayParameters.swapXY?lastcolNum:col));
      	 for (int row = gridFirstRowInColumn(thiscol),
      			 	rowstep = 1,
      			 	lim = gridLastRowInColumn(thiscol)+rowstep;
      	 		row != lim;
      	 		row += rowstep)
      	 		
      	 {	int mask = 1<<row;
      	 	if((drawn&mask)==0)
      	 	{drawn |= mask;
            int ypos = (G.Top(boardRect) + height) -
                (INTERPOLATE
               	? cellToY((char)(thiscol+(displayParameters.reverse_y?1:-1)), row,0,0)
               	: cellToY(thiscol, row,-CELLSIZE / div,isHex?GRIDSIZE:0));
            int xpos = G.Left(boardRect) +
                (INTERPOLATE 
                	? cellToX((char)(thiscol + (displayParameters.reverse_y ? 1 : -1)),row,0,0)
                	: cellToX(thiscol, row,displayParameters.swapXY?0:-CELLSIZE / div,displayParameters.swapXY?CELLSIZE/div:0));
            int digit = (invert_digs ? (firstRowInCol.length - row + 1 ) : (row));
            char column = invert_cols ? (char)('A'+ncols-(thiscol-'A'+1)) : thiscol;
            //G.print("left "+thiscol+" "+thisrow);
            DrawGridCoord(gc, gridColor, xpos,ypos, (int)CELLSIZE,
                (use_cols ? ("" + column) : "") +
                (use_digs ? ("" + digit) : ""));
        }}
        }

	}
 /**
  * draw the grid according to the specified grid style.  This may
  * include drawing cells, lines, dots or other bits of board representation
  * as well as the usual row and column numbers.
  * @param gc
  * @param boardRect
  * @param use_grid
  * @param backgroundColor
  * @param pointColor
  * @param lineColor
  * @param gridColor
  */
 public void DrawGrid(Graphics gc, Rectangle boardRect, boolean use_grid,
         Color backgroundColor, Color pointColor, Color lineColor, Color gridColor)
     {
         // draw the grid and board.  Because of the curious way the lines connecting
         // the grid points are drawn, the chips and rings have to be drawn in a 
         // second pass.
         int POINTRADIUS = (int) Math.max(displayParameters.CELLSIZE / 20, 1);
         int height = G.Height(boardRect);
         boolean cell = (drawing_style==DrawingStyle.STYLE_CELL) || (drawing_style==DrawingStyle.STYLE_NOTHING);
         int div = cell ? 1 : 2;
         String left_style = Grid_Style[GRID_LEFT];
         String right_style = Grid_Style.length==4 ? Grid_Style[3] : null;
         
         boolean rev = displayParameters.reverse_y != displayParameters.reverse_x;
         int leftColNum = gridLeftColNum();
         int stepCol =stepColNum();
         int rightColNum = gridRightColNum();
         int lastcolNum=rightColNum+stepCol;
         boolean INTERPOLATE = displayParameters.INTERPOLATE;
         double GRIDSIZE = displayParameters.GRIDSIZE;
         double rotation = displayParameters.rotation;
         double CELLSIZE = displayParameters.CELLSIZE;
         // paint left numbers
         if (use_grid)
         {	if(left_style!=null) { drawVerticalGrid(gc,left_style,true,gridColor,div, leftColNum, lastcolNum, stepCol); }
            if(right_style!=null) { drawVerticalGrid(gc,right_style,false,gridColor,div, lastcolNum+1, lastcolNum+2, stepCol); }
         }
         // paint top and bottom column numbers
         for (int col = leftColNum; col!=lastcolNum; col+=stepCol)
         {
             char thiscol = (char) ('A' + col);
             int firstrow = gridFirstRowInColumn(thiscol); 
             
             int targeted_top_row  = gridLastRowInColumn(thiscol);
             int targeted_bottom_row = firstrow - 1;

             for (int thisrow = targeted_bottom_row;
                     thisrow <= targeted_top_row; thisrow++)
             { //where we draw the grid
               int thisrow_top =  thisrow+1+firstRowInCol[col] ;
               char thiscol_top = (char)(thiscol+1);
               if (thisrow == targeted_bottom_row)
                 {	 String style = Grid_Style[rev?GRID_TOP:GRID_BOTTOM] ;
                     if (use_grid && (style != null))
                     {
                         double off = (displayParameters.reverse_y ? -GRIDSIZE/2 : GRIDSIZE/2);
                         char cc = style.charAt(0);
                         boolean use_cols = isLetter(cc);
                         char dc  = style.charAt(style.length() -1);
                         boolean use_digs = isDigit(dc);
                         boolean invert_digs = dc=='2';
                         boolean invert_cols = cc=='B';
                         int arow = targeted_bottom_row;
                         char acol = thiscol;
                         int gxpos = G.Left(boardRect) +
                             (INTERPOLATE
                            		? cellToX(acol, arow, 0, 0)
                            		: cellToX(acol, arow, 0, off));
                         int gypos = (G.Top(boardRect) + height) -
                         	(INTERPOLATE
                         			? cellToY(acol, arow, 0, 0)
                         			: cellToY(acol, arow, 0, off));
                         // bottom is drawn at the coordinate location of of x,0 plus 1/2 cell
                         int digit = (invert_digs ? (firstRowInCol.length - firstrow + 1 ) : (firstrow));
                         char column = invert_cols ? (char)('A'+ncols-(thiscol-'A'+1)) : thiscol;

                         DrawGridCoord(gc, gridColor, gxpos, gypos, (int)CELLSIZE,
                       		 (use_cols ? ("" + column) : "") +
                       		 (use_digs ? ("" + digit) : ""));
                     }
                 }
                 else if(thisrow <= targeted_top_row)
                 {	CELLTYPE c = getCell(thiscol,thisrow);
                      {
                     switch(drawing_style)
                     {
                     case STYLE_CELL:
                     {
                         double dist = CELLSIZE / Math.sqrt(2);
                         int px = 0;
                         int py = 0;
                         int nsides0 = c.nAdjacentCells();
                         int nsides = (nsides0==8) ? 4 : nsides0;
                         int stepsize = 360 / nsides;
                         int initial_angle = stepsize/2;
                         GC.setColor(gc,lineColor);

                         for (int angle = 0; angle <= 360; angle += stepsize)
                         {
                             double dangle = (((initial_angle + angle) - rotation) * 2 * Math.PI) / 360; // radians
                             double sina = Math.sin(dangle);
                             double cosa = Math.cos(dangle);
                             int yp = (G.Top(boardRect) + height) -
                                 (INTERPOLATE 
                                		? cellToY(thiscol_top,thisrow_top,0,0)
                                		: cellToY(thiscol_top, thisrow_top, dist * sina,
                                				dist * cosa));
                             int xp = G.Left(boardRect) +
                             	(INTERPOLATE
                             			? cellToX(thiscol_top,thisrow_top,0,0)
                             			: cellToX(thiscol_top, thisrow_top, dist * sina,
                                     dist * cosa));

                             if (angle > 30)
                             {
                                 GC.drawLine(gc,px, py, xp, yp);
                             }

                             px = xp;
                             py = yp;
                         }
                     	}
                     break;
                     case STYLE_NOTHING: break;
                     
                     case STYLE_LINES:
                     case STYLE_NO_EDGE_LINES:
                     {
                         boolean edge = (drawing_style==DrawingStyle.STYLE_NO_EDGE_LINES) && c.isEdgeCell();
                         // draw a small dot to mark the center position
                         int ypos = (G.Top(boardRect) + height) - cellToY(thiscol, thisrow);
                         int xpos = G.Left(boardRect) + cellToX(thiscol, thisrow);
 
                         if(c.linkCount()>0) 
                         	{GC.cacheAACircle(gc, xpos, ypos, POINTRADIUS, pointColor,  backgroundColor, true);
                         	}
                         GC.setColor(gc,lineColor);

                         // draw lines to the adjacent positions
                         for (int dir = 0; dir < c.nAdjacentCells(); dir++)
                         { // this convolution is to find the 6 adjacent board positions
                           // and draw a line to each of them if it's a valid board position
                        	 CELLTYPE nc = c.exitTo(dir);
                             if (nc!=null)
                             {
                                 int nypos0 = cellToY(nc.col,nc.row);
                                 int nypos = (G.Top(boardRect) + height) - nypos0;
                                 int nxpos = G.Left(boardRect) + cellToX(nc.col, nc.row);

                                 if (edge && nc.isEdgeCell())
                                 {
                                 }
                                 else
                                 {
                                     gc.drawLine(xpos, ypos, nxpos, nypos);
                                 }
                             }
                         }
                     }
                     break;
					default:
						break;
                     }
                     }
                 String style = Grid_Style[rev?GRID_BOTTOM:GRID_TOP];
                 if (use_grid && (style != null) &&
                         (thisrow == targeted_top_row))
                 {	char cc = style.charAt(0);
                 	boolean use_cols = isLetter(cc);
                 	char dc = style.charAt(style.length() - 1);
                    boolean use_digs = isDigit(dc);
                    boolean invert_digs = dc=='2';
                    boolean invert_cols = cc=='B';
                    double off = displayParameters.reverse_y?GRIDSIZE/2:-GRIDSIZE/2;
                    char acol = thiscol;
                    int arow = thisrow_top-firstRowInCol[col];
                    int gxpos = G.Left(boardRect) +
                     	(INTERPOLATE 
                     			?cellToX(acol, arow, 0, 0)
                     			:cellToX(acol, arow, 0, off)
                         );
                     int gypos = (G.Top(boardRect) + height) -
                     	(INTERPOLATE 
                     		? cellToY(acol, arow, 0, 0)
                     		: cellToY(acol, arow, 0, off));
                     //G.print("Bottom "+thiscol+" " +thisrow2);
                     // top grid is drawn at the location of the last cell in the row, plus 1/2 cell
                     int digit = (invert_digs ? (firstRowInCol.length - thisrow + 1 ) : thisrow);
                     char column = invert_cols ? (char)('A'+ncols-(thiscol-'A'+1)) : thiscol;

                     DrawGridCoord(gc, gridColor,gxpos, gypos ,(int)CELLSIZE,
                        (use_cols ? ("" + column) : "") +
                       (use_digs ? ("" + digit) : ""));
                 }
                 }
             }
         }
     }

    
	// display and initialization utilities

	/** return the approximate cell size.
	 * 
	 */
	public int cellSize() { return((int)displayParameters.CELLSIZE); }
	/** return the first row in this column.  Note that if the board supports
	 * a reverse-y option, it may be more useful to call {@link #topRowInColumn }
	 * 
	 * @param col
	 * @return and integer
	 */
	public abstract int firstRowInColumn(char col);
	/**
	 * return the last row in this column.  Note that if the board supports 
	 * a reverse-y option, it may be more useful to call {@link #bottomRowInColumn }
	 * @param col
	 * @return an integer
	 */
	public abstract int lastRowInColumn(char col);

	/**
	 * same as firstRowInColumn, but for drawing the grid. This is used by the Gounki board to
	 * extend the board by 2 rows.
	 * @param col
	 * @return the row number of the first row in this column
	 */
	public int gridFirstRowInColumn(char col)
	{
		return(firstRowInColumn(col));
	}
	/**
	 * same as lastRowInColumn but for drawing the grid.  This is used by the Gounki board to
	 * extend the board by 2 rows.
	 * @param col
	 * @return the row number of the last row in this column
	 */
	public int gridLastRowInColumn(char col)
	{
		return(lastRowInColumn(col));
	}


}
