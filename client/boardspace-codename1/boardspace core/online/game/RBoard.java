package online.game;

import com.codename1.ui.geom.Rectangle;

import lib.Random;
import lib.G;
import lib.HitPoint;
import lib.IStack;
import lib.OStack;
/**
 * a board where the cells have random locations, not in a regular grid.  Cells
 * can be accessed through the allCells chain, or getCellArray()
 * 
 * @author ddyer
 *
 * @param <CELLTYPE>
 */


public abstract class RBoard<CELLTYPE extends cell<CELLTYPE> >  extends BaseBoard {
    /** values for drawing_style determine how the board is drawn around the
     * grid points.  Most modern boards use STYLE_NOTHING which draws nothing.
     * <li>STYLE LINES is a  go or yinsh style board, made of lines that connect cell centers
     * <li>STYLE_NO_EDGE_LINES is a gipf style board, make of lines that connect cell centers except for the edges.
	 * <li>STYLE_CELL is a hexagon or checker style board where the cells are outlined
	 * <li>STYLE_NOTHING is a graphics-defined board where no lines are drawn.
     */
    public enum DrawingStyle
    {	STYLE_LINES,			// yinsh style
    	STYLE_NO_EDGE_LINES,	// gipf style
    	STYLE_CELL,				// hexagon style
    	STYLE_NOTHING			// no line drawing, use graphics
    };
    
    public DrawingStyle drawing_style = DrawingStyle.STYLE_LINES;	// default
    // { "A1","1",null } left top and bottom edges.
    /**
     * null, or an array of 3 or 4  strings, which determine the default number/letter
     * style for grids. <p>
     *  Grid_Style[0] for left, Grid_Style[1] for top Grid_Style[2] for bottom, Grid_Style[3] for right
     *  <p>
     *  The strings should be "A" for letters,
     *  <br>
     *   "1" for numbers "2" for inverse numbers
     *  <br>
     *  or "A1" for both letters and numbers 
     *  <br>
     *  or null
     *  
     * if null, a default of {"1",null,"A"} will be used for diagonal_grid
     * or a default of {null,"A1","A1"} for normal grid will be supplied.
     */

    public String[] Grid_Style = null; // null or an array of 3 string specifiers
    static final int GRID_LEFT = 0;	// index for the left element of grid_style
    static final int GRID_TOP = 1;	// index for the top element of grid_style
    static final int GRID_BOTTOM = 2;// index for the bottom element of grid_style
    static final int GRID_RIGHT = 3;	// index for the right element of the grid style

    public CELLTYPE allCells;			// linked list of the whole board's cells
    protected cell<CELLTYPE> cellArray[];			// a plain array of all cells.
    protected Rectangle boardRect=new Rectangle(0,0,100,100);
    //
    // the board drawing section.  These support methods implement the transformation
    // from row,col to display x,y
    //
    
    public DisplayParameters displayParameters = new DisplayParameters();
    
    public void initBoard()
    {
    	allCells = null;
    	cellArray = null;
    	boardRect = null;
    }
    /**
     * find the closest cell on the board.  For many cases, this is preferable
     * to checking each cell for closeness to the mouse, because there are no
     * gaps or overlaps in the result.
     * @param highlight
     * @param brect
     * @return a cell
     */
    public CELLTYPE closestCell(HitPoint highlight,Rectangle brect)
    {	if(highlight!=null)
    	{ 
  	  CELLTYPE cc = closestCell(G.Left(highlight),G.Top(highlight),brect);
  	  if(cc!=null)
  	  {	// redo the distance calculation so the use of closestCell by drawStack
  		// will be correct for cells not in the main board
  		int cx = cellToX(cc);
   		int cy = cellToY(cc);
  		highlight.closestPoint(cx,cy,highlight.hitCode);
  	  }
      return(cc);
    	}
    	return(null);
    }
    public void addCell(CELLTYPE c)
    {
    	c.next = allCells;
    	allCells = c;
    	cellArray = null;
    }
    
    public long Digest(Random r)
    {	long v=0;
 		// note we can't change this without invalidating all the existing digests.
 		for(CELLTYPE c=allCells; c!=null; c=c.next)
 		{	
            v ^= c.Digest(r);
 		}
 		return(v);
    }

	public cell<CELLTYPE>[] getCellArray()
    {
    	if((cellArray==null) && (allCells!=null))
    	{
    	int nc = 0;
    	for(CELLTYPE c = allCells; c!=null; c=c.next) {nc++; }
    	cellArray = allCells.newSelfArray(nc);
    	nc = 0;
    	for(CELLTYPE c = allCells; c!=null; c=c.next) { cellArray[nc++] = c; };  
    	}
    	return(cellArray);
    }
	
	/**
	 * this implements valid for 1d boards.  Mostly this is overridden by 2d methods in gBoard
	 * @param col
	 * @param row
	 * @return true if col,row corresponds to a position on the board
	 */
    public boolean validBoardPos(char col,int row) 
    {
    	return((row>=0) && (row<getCellArray().length));
    }

	/**
	 * this implements valid for 1d boards.  Mostly this is overridden by 2d methods in gBoard
	 * @param col
	 * @param row
	 * @return the cell
	 */
	@SuppressWarnings("unchecked")
	public CELLTYPE getCell(char col,int row)
    {	
    	if(validBoardPos(col,row))
    	{	
		cell<CELLTYPE> ca[] = getCellArray();
    		return((CELLTYPE)ca[row]);
    	}
    	return(null); 
    }
    
    /**
     * get the cell from the current board which shares the same role as c, which is 
     * most likely from a different board of the same game.  This is used in copying boards.
     * @param c
     * @return the local cell with the same role as C
     */
    public CELLTYPE getCell(CELLTYPE c)
    {	if(c==null) { return(null); }
		if(c.onBoard) { return(getCell(c.col,c.row)); }
    	throw G.Error("getCell Not implemented");
    }
    /**
     * get make stack "to" a copy of stack "from" using cells from the current board 
     * @param to
     * @param from
     * @return null 
     */
    public CELLTYPE getCell(OStack<CELLTYPE>to,OStack<CELLTYPE> from)
    {	to.clear();
    	for(int i=0,lim=from.size(); i<lim; i++) { to.push(getCell(from.elementAt(i))); }
    	return(null);
    }
    
    /**
     * get make stack "to" a copy of stack "from" using cells from the current board 
     * @param to
     * @param from
     * @return null 
     */
    public CELLTYPE getCell(OStack<CELLTYPE>to[],OStack<CELLTYPE> from[])
    {	for(int i=0,lim=from.length; i<lim; i++) { getCell(to[i],from[i]); }
    	return(null);
    }
    /**
     * make array "to" a copy of array "from" using cells from the current board
     * @param to
     * @param from
     * @return null
     */
    public CELLTYPE getCell(CELLTYPE to[],CELLTYPE from[])
    {	
    	for(int i=0,lim=from.length; i<lim; i++) { to[i] = getCell(from[i]); }
    	return(null);
    }
    /**
     * make array "to" a copy of array "from" using cells from the current board
     * @param to
     * @param from
     * @return null
     */
    public CELLTYPE getCell(CELLTYPE to[][],CELLTYPE from[][])
    {	
    	for(int i=0,lim=from.length; i<lim; i++) 
    		{ getCell(to[i],from[i]);
    		}
    	return(null);
    }
    /**
    unlink this cell bidirectionally, effectively removing it from the board.
    this is used to create holes in the board, like the center square in tzaar
    This also removes C from "allCells", but not from the cell array
    * @param C
    */
   public void unlinkCell(CELLTYPE C)
   { 	for(CELLTYPE tc = allCells,prev = null;
   		tc!=null;
   		prev=tc,tc = tc.next)
   	{	// locate in the allcells list
   		if(tc==C) 
   		{
   		if(prev==null) { allCells = tc.next; } else { prev.next = tc.next; }
   		tc.unCrossLink();
   		cellArray = null;
   		C.onBoard = false;
   		return;
  		}
   	}
   throw G.Error("cell to unlink not found");
   }

   /** 
    * verify a copy of this board.  This method checks the
    * cells on the board using {@link cell#sameCell sameCell}.  This method
    * is typically wrapped by each subclass to check other 
    * state in the board. 
    * @param from_b
    */
   public void sameboard(RBoard<CELLTYPE> from_b)
   {	
	super.sameboard(from_b);
	checkSameCells(from_b);
   }
   
   public void checkSameCells(RBoard<CELLTYPE> from_b)
   {
	if(allCells!=null)
	{
 	cell<CELLTYPE> myB[] = getCellArray();
 	cell<CELLTYPE> otherB[]=from_b.getCellArray();
 	
   	int ysize = otherB.length;
   	// don't check the actual contents, some games re-sort the array!
   	G.Assert((ysize==myB.length),"Board Number of cells mismatch");
   	for(CELLTYPE c = allCells,d=from_b.allCells; c!=null; c=c.next,d=d.next) 
   		{ G.Assert(c.sameCell(d),"Cells %s and %s mismatch",c,d);
   		}
 	}
   }
   /**
    *  this is a visitor method so the actual board can clone the cell it's own way
    * @param c
    * @param from
    */
   public void copyFrom(CELLTYPE c,CELLTYPE from)
   {	if(c!=null) { c.copyFrom(from); }
   }  


   /** copy from a given board.  This method is typically 
    * wrapped by each subclass.
    * 
    * @param from_b
    */
   public void copyFrom(RBoard<CELLTYPE> from_b)
   {	// copy the contents and dynamic state from the other board
    super.copyFrom(from_b);
    // displayparameters are a shared instance among all copies and clones
    // of a board.  The initially created instance in clones of the main
    // board is discarded.
    displayParameters = from_b.displayParameters;
   	CELLTYPE myc = allCells;
   	CELLTYPE hisc = from_b.allCells;
   	cellArray = null;
       
   	while((myc!=null)&&(hisc!=null)) 
   	{ //note that this may be subtly different from
   	  // myc.copyFrom(hisc)
   	  copyFrom(myc,hisc);
   	  myc = myc.next;
   	  hisc = hisc.next;
   	}
   	G.Assert((myc==null) && (hisc==null),"not the same cells in boards");
    }
   
   /**
    * find the closest cell on the board.  For many cases, this is preferable
    * to checking each cell for closeness to the mouse, because there are no
    * gape or overlaps in the result.
    * @param x the x location
    * @param y the Y location
    * @param brect
    * @return a cell
    */
   public CELLTYPE closestCell(int x,int y,Rectangle brect)
   {	
     return(closestCell(x-G.Left(brect),G.Height(brect)-(y-G.Top(brect))));
   }
   /** get the x coordinate associated with the cell 
    * @param c cell on the board
    * */
   public abstract int cellToX(CELLTYPE c);
   /** get the y coordinate associated with the cell 
    * @param c cell on the board
    * */
   public abstract int cellToY(CELLTYPE c);
  
   /**
   * find the closest cell to a given x,y.  This is used
   * to determine what the mouse is pointing at without
   * leaving any gaps between cells.  If the mouse is 
   * not "near" any cell, return null.  This isn't quite
   * enough for torroidal boards that wrap around.
    * @param x
    * @param y
    * @return a cell
    */
    public CELLTYPE closestCell(int x,int y)
   { int mindis = -1;
   	CELLTYPE mincell=null;
    	int cellspan=-1;
   	//G.Assert(!isTorus,"can't be a torus, wrap doesn't work");
   	// find the closest cell
   	for(CELLTYPE c=allCells; c!=null; c=c.next)
   	{	int cx = cellToX(c);
   		int cy = cellToY(c);
   		int dis = G.distanceSQ(x,y,cx,cy);
   		if((mindis<0) || (dis<mindis)) 
   		{ mincell = c; 
   		  mindis = dis;
  		}
   		if(c!=allCells)
   		{
   		// keep track of the minimum distances between cells too.  Set the span
   		// to the maximum of distances to adjacent cells.
   		int celldis = -1;
   		for(int dir = c.geometry.n-1; dir>=0; dir--)
   			{	CELLTYPE adj = c.exitTo(dir);
   				if(adj!=null)
   					{ int ax = cellToX(adj);
   					  int ay = cellToY(adj);
   					  int disq = G.distanceSQ(cx,cy,ax,ay);
   					  celldis = Math.max(celldis,disq);
   					}
   			}
   		if((cellspan<0)||(celldis>cellspan)) 
   			{ cellspan = celldis; 
   			}
   		}
   	}
   	return((mindis<=cellspan*2) ? mincell : null);
   }
    
    /**
     * compare two OSTACK of the component cell type, and verify that they
     * are the same.
     * @param local
     * @param remote
     * @return true if local and remote contain the same cells according to {@link cell#sameCell}
     */
    public  boolean sameCells(OStack<CELLTYPE>local,OStack<CELLTYPE>remote)
     {   int sz = local.size();
     	if(remote.size()!=sz) { return(false); }
     	for(int i=0;i<sz;i++) 
     		{ CELLTYPE c = local.elementAt(i);
     		  CELLTYPE d = remote.elementAt(i);
     		  if(!((c==null)?(d==null):c.sameCell(d)))
     			{ return(false); 
     			}; 
     		}
     	return(true);
     
     }
    public boolean sameContents(CELLTYPE[][]local,CELLTYPE[][]remote)
    {
    	if(local.length!=remote.length) { return(false); }
    	for(int i=0;i<local.length;i++)
    	{
    		if(!sameContents(local[i],remote[i])) { return(false); }
    	}
    	return(true);
    }
    public boolean sameContents(CELLTYPE[]local,CELLTYPE[]remote)
    {
 	   if(local.length!=remote.length) { return(false); }
    	for(int lim = local.length-1; lim>=0; lim--) 
    	{	CELLTYPE c = local[lim];
    		CELLTYPE d = remote[lim];
    		if(!((c==null)?(d==null):c.sameContents(d))) 
    			{ return(false); }}
    	return(true);
    }
    /**
     * compare two arrays of OSTACK of the component cell type, and verify that they
     * are the same.
     * @param local
     * @param remote
     * @return true if local and remote contain the same cells according to {@link cell#sameCell}
     */
    public  boolean sameCells(OStack<CELLTYPE>[]local,OStack<CELLTYPE>[]remote)
     {   int sz = local.length;
     	if(remote.length!=sz) { return(false); }
     	for(int i=0;i<sz;i++) 
     		{ if(!sameCells(local[i],remote[i])) { return(false); }
      		}
     	return(true);
     }
    
    public boolean sameCells(CELLTYPE a,CELLTYPE b)
    {
 	   return(a==null ? b==null : a.sameCell(b));
    }
    public long Digest(Random r,CELLTYPE c) { return(c==null)?0:c.Digest(r); }

   /**
    * copy an array of cells using copyFrom for each cell
    * @param to
    * @param from
    */
   public void copyFrom(CELLTYPE to[],CELLTYPE from[])
   {
	  for(int i=0;i<to.length; i++) { copyFrom(to[i],from[i]);}
   }
   /**
    * copy a 2d array of cells using copyFrom for each cell
    * @param to
    * @param from
    */
   public void copyFrom(CELLTYPE to[][],CELLTYPE from[][])
   {
	  for(int i=0;i<to.length; i++) { copyFrom(to[i],from[i]);}
   } 

   
   /**
    * copy an IStack
    * @param to
    * @param from
    */
   public void copyFrom(IStack to,IStack from) 
   {
	to.clear();
   	for(int i=0,lim=from.size(); i<lim; i++)
   	{
   		to.push(from.elementAt(i));
   	}
   }

   /**
    * Digest an OStack of cells
    * @param r
    * @param st
    * @return a long representing the alternate state of a a stack of cells
    */
   public long Digest(Random r,OStack<CELLTYPE>st)
	{	long v=0;
		for(int lim=st.size()-1; lim>=0; lim--) 
			{ v ^= Digest(r,st.elementAt(lim)); 
			}
		return(v);	
	}
   
   
   /**
    * Digest an array of OStack of cells
    * @param r
    * @param st
    * @return a long representing the alternate state of an array of stacks of cells
    */
   public long Digest(Random r,OStack<CELLTYPE>[]st)
   {
	   long v=0;
	   for(int i=0;i<st.length;i++) { v ^= Digest(r,st[i]); }
	   return(v);
   }
   
   /**
    * compare to arrays of the component cell type, and verify that they are the same
    * 
    * @param local
    * @param remote
    * @return true if local and remote contain the same cells according to {@link cell#sameCell}
    */
   public boolean sameCells(CELLTYPE[]local,CELLTYPE[] remote)
    {	if(local.length!=remote.length) { return(false); }
    	for(int lim = local.length-1; lim>=0; lim--) 
    	{	CELLTYPE c = local[lim];
    		CELLTYPE d = remote[lim];
    		if(!((c==null)?(d==null):c.sameCell(d))) 
    			{ return(false); }}
    	return(true);
    }

   /**
    * compare two arrays of OStack for eq contents
    * @param local
    * @param remote
    * @return true if the contents of a the arrays or stacks of cells are the same
    */
   public boolean sameContents(OStack<?>[]local,OStack<?>[]remote)
   {	if(local.length!=remote.length) { return(false); }
   		for(int i=0;i<local.length;i++) 
   			{ if(!local[i].sameContents(remote[i])) { return(false); } 
   			}
   		return(true);
    }
   /**
    * reinit all the cells of an array (which may contain nulls)
    * @param c
    */
   public void reInit(CELLTYPE[] c)
   {	for(CELLTYPE d : c) { if(d!=null) { d.reInit(); }}
   }
   
   public void reInit(OStack<?>c[]) { for(OStack<?>a : c) { a.clear(); }}
   /**
    * reinit all the cells in an array of arrays, which may contain nulls.
    * @param cells
    */
   public void reInit(CELLTYPE[][] cells)
   {	for(CELLTYPE c[] : cells) { reInit(c); }
   }

   
   /**
   * compare to arrays of the component cell type, and verify that they are the same
   * 
   * @param local
   * @param remote
   * @return true if local and remote contain the same cells according to {@link cell#sameCell}
   */
  public boolean sameCells(CELLTYPE[][]local,CELLTYPE[][] remote)
   {	if(local.length!=remote.length) { return(false); }
   	for(int lim = local.length-1; lim>=0; lim--) 
   	{	if(!sameCells(local[lim],remote[lim])) 
   			{ return(false); }
   	}
   	return(true);
   }
  /**
   * compare to ostack for eq contents
   * @param local
   * @param remote
   * @return true of the stacks of cells have the same contents
   */
  public boolean sameContents(OStack<?>local,OStack<?>remote)
  {	return(local.sameContents(remote));
  }
  /**
   * adjust the internal scaling parameters so the board displays
   * inside the specified rectangle.  This works by running a trial
   * transformation of all the points on the board, then scaling the 
   * result to completely fill the rectangle if possible.
   * @param r
   */
  public void SetDisplayRectangle(Rectangle r)
  { boardRect = G.clone(r);
  }
  
  public boolean reverseXneqReverseY()
  {
  	return (displayParameters.reverse_x!=displayParameters.reverse_y);
  }
  public boolean reverseY() { return displayParameters.reverse_y; }
  public boolean reverseX() { return displayParameters.reverse_x; }
  public void setReverseY(boolean v) {  displayParameters.reverse_y = v; }
  public void setReverseX(boolean v) {  displayParameters.reverse_x = v; }
  
}
