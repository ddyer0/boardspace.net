package online.game;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Enumeration;

import lib.CompareTo;
import lib.G;
import lib.Sort;
import online.game.cell.Geometry;
import online.game.commonCanvas.Itype;

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
/**
 * the BoardIterator takes the cell array and sorts cells according
 * to the x,y coordinates and the desired presentation order.  The 
 * most common choice is top-bottom, so tall things in later rows will
 * occlude previously painted cells.  Left-right or right-left is trickier
 * but shadows are typically to the left, so painting right to left is good.
 * -- makes the most common sort order TBRL
 * @author ddyer
 *
 */
class cellSorter implements CompareTo<cellSorter>
{	public int x;
	public int y;
	Object c;
	Itype type;
	cellSorter(int xx,int yy,Object cc,Itype tt)
	{
		x = xx;
		y = yy;
		c = cc;
		type = tt;
	}
	public int compareTo(cellSorter o) {
		switch(type)
		{
		case ANY: return 0;
		case LRTB:	// left-right fast, top-bottom slow
			return (x>o.x) 
					? 1 
					: x < o.x 
						? -1
						: G.signum(o.y-y);

		case RLTB:	// right-left fast, top-bottom slow
			return (x<o.x) 
					? 1 
					: x > o.x 
						? -1
						: G.signum(o.y-y);

		case TBRL:	// top-bottom fast, right-left slow (normal for hexagonal grids)
			return (y<o.y) 
				? 1 
				: y > o.y 
					? -1
					: G.signum(o.x-x);
				
		case TBLR:	// top-bottom fast, left-right slow
			return (y<o.y) 
					? 1 
					: y > o.y 
						? -1
						: -G.signum(o.x-x);
					
		case LRBT:	// left-right fast, bottom-top slow
			return (x>o.x) 
					? 1 
					: x < o.x 
						? -1
						: -G.signum(o.y-y);
		case RLBT:
			return (x<o.x) 
					? 1 
					: x > o.x 
						? -1
						: -G.signum(o.y-y);
	
		case BTLR:	// bottom-top fast, left-right slow
			return (y>o.y) 
					? 1 
					: y > o.y 
						? -1
						: -G.signum(o.x-x);
					
		case BTRL:	// bottom-top fast, right-left slow
			return (y>o.y) 
					? 1 
					: y > o.y 
						? -1
						: G.signum(o.x-x);
		
		}
		return 0;
	}
	public int altCompareTo(cellSorter o) {
		return -compareTo(o);
	}

}

/**
 * class fBoard is agnostic about how the cells are organized.
 * most traditional boards use gBoard which has an explicit array
 * but Palago (and other) boardless games can use this and any other
 * means of keeping track of the cells.
 * @author ddyer
 *
 * @param <CELLTYPE>
 */

public abstract class fBoard <CELLTYPE extends cell<CELLTYPE>> extends RBoard<CELLTYPE> implements Opcodes

{
	

public class BoardIterator implements Enumeration<CELLTYPE> 
{	
	
	
	CELLTYPE currentCell[] = null;
	CELLTYPE current = null;
	int index = 0;
	boolean reverse = displayParameters.reverse_y;
	Itype type;
	boolean invalid = false;		// set to true to force this to be discarded
	int seq = 0;
	public void reset() 
	{ 	if(type==Itype.ANY) { current = allCells; }
		else 
		{ seq = 0; 
		}
	}
	public String toString() { return("<boardIterator "+type+">"); }

	@SuppressWarnings("unchecked")
	/*
	 * create an iterator for the board
	 */
	BoardIterator(Itype typ)
	{	type = typ;
		if(typ==Itype.ANY)
		{
			current = allCells;
		
		}
		else
		{
		CELLTYPE[] cells = (CELLTYPE[])getCellArray();
		int len = cells.length;
		forgetCellArray();
		cellSorter[] sort = new cellSorter[len];
		
		for(int i=0;i<len;i++)
		{	CELLTYPE c = cells[i];
			int xp = cellToX(c.col,c.row);
			int yp= cellToY(c.col,c.row);
			sort[i] = new cellSorter(xp,yp,c,type);
		}
		Sort.sort(sort,false);
		for(int i=0;i<len;i++)
		{
			cells[i] = (CELLTYPE)sort[i].c;
		}
		currentCell = cells;
		}
	}
	
	public CELLTYPE nextElement()
	{	if(type==Itype.ANY) 
		{CELLTYPE c = current;
		 current = c.next;
		 seq++;
		 return c;
		}
		else
		{
			CELLTYPE c = currentCell[seq];
			seq++;
			return c;
		}
	}
	public boolean hasMoreElements() {
		if(type==Itype.ANY ) { return current!=null; }
		else
		{
		return seq<currentCell.length;
		}
	}

	}

 	public Geometry geometry = null;		// the expected geometry for the board
 	public abstract Geometry geometry();
 	
	public boolean isTorus = false;

	public void copyFrom(fBoard<CELLTYPE> other)
	 {	super.copyFrom(other);
	 	geometry = other.geometry;
	 	isTorus = other.isTorus;
	 }
	
    /** 
     * verify a copy of this board.  This method checks the
     * cells on the board using {@link cell#sameCell sameCell}.  This method
     * is typically wrapped by each subclass to check other 
     * state in the board. 
     * @param from_b
     */
    public void sameboard(fBoard<CELLTYPE> from_b)
    {	super.sameboard(from_b);
    	G.Assert(geometry==from_b.geometry,"geometry mismatch");
    	G.Assert(isTorus==from_b.isTorus,"isTorus mismatch");
    }
 
    
    /**
     * factory method for the board cells.  This is used to create all the cells
     * in the low level board.
     * @param col columb id - usually 'A'-xx
     * @param row column row = usually 1-nn
     * @return a new instance of the appropriate cell class
     */
    public abstract CELLTYPE newcell(char col,int row);
    public abstract void SetBoardCell(char col,int row,CELLTYPE v);
    public abstract CELLTYPE GetBoardCell(char col,int row);
      
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

    //
    // complete transform col,row to x
    //
    protected int cellToX(char colchar0, int rownum0, double offx, double offy)
    {	if(displayParameters.INTERPOLATE)
   	 {
   	 	throw G.Error("interpolate not supported");
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
 // complete transform col,row to y
 //
 protected int cellToY(char colchar0, int thisrow0, double offx, double offy)
 {	
	 if(displayParameters.INTERPOLATE)
	 {	throw G.Error("interpolate not supported");
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

 
 BoardIterator boardIterator = null;
 
 public void forgetCellArray()
	{
		super.forgetCellArray();
		boardIterator = null;
	}
	
 /**
  * get an Enumeration for the board's cells.  This is a new algorithm
  * that sorts based on the actual board coordinates rather than the 
  * raster order associated with the underlying board array
  * @param e
  * @return an Enumeration of the board's cell type.
  */
 public Enumeration<CELLTYPE> getIterator(Itype e)
 {	 if(displayParameters.CELLSIZE<=1)
 		{ 
	 	// interaction with direct drawing, the iterator can be requested when
	 	// the cell size isn't established, resulting in a random sort.  This
	 	// tends to manifest as the shadows ping-ponging in a stange way.
	 	// switching to ANY makes the iterator random for sure, but will be
	 	// replaced by a proper one when the screen geometry is established
	 	e = Itype.ANY; 
 		}
	 if(boardIterator==null 
 			|| (boardIterator.type!=e)
 			|| boardIterator.invalid
 			|| (boardIterator.reverse!=displayParameters.reverse_y))
 		{
	 	boardIterator = new BoardIterator(e);
 		}
	 else
	 {
		 boardIterator.reset();
	 }
 	return(boardIterator);
 }
 public void foregetIterator()
 {
	 boardIterator = null;
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
 protected double yCellRatio() { return(1.0); }
 protected double yGridRatio() { return(2.0); }
 public abstract int initialSize();
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
 	displayParameters.setDisplayRectangle0(r,ycell,ygrid,initialSize());
 	
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


	/** return the approximate cell size.
	 * 
	 */
	public int cellSize() { return((int)displayParameters.CELLSIZE); }
	

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
	
	 public abstract int findDirection(char col,int row,char toCol,int toRow);
	 /**
	  * verify that all links are reversable, and the direction map is consistent
	  */
	 public void checkInverseCrossLinks()
	 {
	 for(CELLTYPE c=allCells; c!=null; c=c.next) { c.countLinks(); }
	     // verify that all the crosslinks are correct - that if a links to b, b links to a
	     // in the opposite direction as defined by exitToward.
	 	for(CELLTYPE c = allCells; c!=null; c=c.next)
	 	{	int mod = geometry.n;
	 		for(int dir=0;dir<mod;dir++)
	 		{	CELLTYPE c1 = c.exitTo(dir);
	 			if(isTorus) { G.Assert(!c1.isEdgeCell(),"not an edge cell"); }
	 			if(c1!=null) 
	 			{ CELLTYPE c2 = c1.exitTo(dir+mod/2);
	 			  int dirto = findDirection(c.col,c.row,c1.col,c1.row);
	 			  int dirfrom = findDirection(c1.col,c1.row,c.col,c.row);
	 			  // if this fails, then the "firstInCol" numbers are inconsistent
	 			  G.Assert(c2==c,"reverse map, from %s direction %s to %s and back %s",c,dirto,c1,c2);
	 			  // torus wrap cells are on the opposite side expected
				  G.Assert( isTorus || ((dirto==dir)&&(dirfrom==(dir+mod/2)%mod)),"direction mapping");
	 			}
	 		}
 	}}
	 
	 /**
	  * this version of checkcrosslinks is simpler, and doesn't check directions
	  * which are not meaningful for a tri-based board
	  */
	 public void checkReversableCrossLinks()
	    {
	       	// verify that all the crosslinks are correct - that if a links to b, b links to a
	        // in the opposite direction as defined by exitToward.
	    	for(CELLTYPE c = allCells; c!=null; c=c.next)
	    	{	
	    		for(int dir=0;dir<c.geometry.n;dir++)
	    		{	CELLTYPE c1 = c.exitTo(dir);
	    			if(isTorus) { G.Assert(!c1.isEdgeCell(),"not an edge cell"); }
	    			if(c1!=null) 
	    			{ boolean found = false;
	    			  for(int rev = 0;!found && rev<geometry.n;rev++)
	    				{
	    				CELLTYPE c2 = c1.exitTo(rev);
	    				if(c2==c) { found = true; }
	    				}
	    			  G.Assert(found,"reverse map");
	    			}
	    		}
	    	}
	    }

	 
	 
	 private void addReverseLink(CELLTYPE cc,CELLTYPE nc2)
	 {	int mod = cc.geometry.n;
		// locate the reverse link slot and add a reverse link
	 	for(int rev = 0;rev<mod; rev++)
	    	{
	    		char rcn = torusWrapCol(getExitCol(nc2,rev));
			  	int rrn  = torusWrapRow(getExitRow(nc2,rev)); 	
			  	if(rcn==cc.col && rrn==cc.row)
			  		{
			  		if(nc2.exitTo(rev)==null) { nc2.addLink(rev,cc); }
			  		return;
			  		}
	    	}
	 	G.Error("reverse link not found");
	 }
	 /**
	  * @param col
	  * @return  a new column if the board is a torus and this column wraps
	  */
	 public char torusWrapCol(char col) { return col; }
	 /**
	  * 
	  * @param row
	  * @return a new row if the board is a torus and this row wraps
	  */
	 public int torusWrapRow(int row) { return row; }
	 /**
	  * @return an array dx directions from a cell.  The exact meaning is dependent on the representation of the board
	  */
	 public abstract int[] dxs();
	 /**
	  * 
	  * @return an array of dy directions from a cell.  The exact meaning is dependent on the representation of the board
	  */
	 public abstract int[] dys();
	 /**
	  * the default implementation of this uses dys() and dxs()
	  * @param cc
	  * @param dir
	  * @return the new column for cell exit direction 
	  */
	 public char getExitCol(CELLTYPE cc,int dir)
	 	{	char xn = (char)(cc.col+dxs()[dir]);
	 		return xn;
	 	}
	 /**
	  * the default implementation of this uses dxs() and dys()
	  * @param cc
	  * @param dir
	  * @return the new row for cell exit direction
	  */
	 public int getExitRow(CELLTYPE cc,int dir)
	 	{
	 		return cc.row+dys()[dir];
	 	}
	 /**
	  * link this cell to all the adjacent cells that currently exist, with reverse links from the linked
	  * cells.  this is used to turn a raw board into a linked network of cells.
	  * 
	  * @param cc
	  
	  */
	 public void linkAdjacentCells(CELLTYPE cc)
	 {	int mod = cc.geometry.n;
	 	for(int dir = 0;dir<mod; dir++)
		  { 
	 		if(cc.exitTo(dir)==null)
	 		{
	 		char cn = torusWrapCol(getExitCol(cc,dir));
		  	int rn  = torusWrapRow(getExitRow(cc,dir)); 		  	
		    CELLTYPE nc2 = GetBoardCell(cn,rn);
		    if(nc2!=null)
		    	{
		    	cc.addLink(dir,nc2);
		    	addReverseLink(cc,nc2);
		    	
		    	}
		    }
		  }
	 }
	 /**
	  * 
	  * @return if true, links should be invertable by geometry.n/2
	  */
	 public boolean invertable() { return true; }
	 /**
	  * 
	  * @return if true, links should be reversible by finding the reverse link somewhere
	  */
	 public boolean reversible() { return true; }
	 
	 public void setDirectCrossLinks()
	    {	
		 for(CELLTYPE cc = allCells; cc!=null; cc=cc.next)
		 {	linkAdjacentCells(cc);
		 }
		 for(CELLTYPE cc = allCells; cc!=null; cc=cc.next) { cc.countLinks(); }
		 checkCrossLinks();
	    }
	 public void checkCrossLinks()
	 {
		 // this is technically superfluous, but is a huge help when debugging new board types
		 if(invertable()) {  checkInverseCrossLinks(); }
		 else if(reversible()) { checkReversableCrossLinks(); }
	 }

}
