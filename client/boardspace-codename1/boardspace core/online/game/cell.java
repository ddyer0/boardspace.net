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

import com.codename1.ui.geom.Rectangle;
import bridge.Color;
import com.codename1.ui.Font;

import lib.*;

/** this is the common cell which can be used by all boards.
 * Actual boards are normally use a subclass of this one.
 * The different numbering schemes for rows and columns
 * do not affect the representation at this level.
 * <p>
 * cells logically correspond to squares of the board.  They have row
 * and column coordinates.  The keep pointers the neighboring cells, so
 * it is possible to navigate without respect to the coordinate system. They
 * also keep track of the board's contents.  Disembodied cells (not part of
 * the board) are commonly used to store other object of interest so the code
 * can be written to always manipulate cells.
 * <p>
 * Specialized subclasses of cell exist for common variations in geometry,
 * and the default board construction process automatically initializes a 
 * grid of neighbors with geometry appropriate to the board.
 * 
 * @see chipCell
 * @see stackCell
 * @author ddyer
 *
 */
class ScreenData
{
    /** the current rotation as recorded by drawChip or drawStack.  This is recorded live but may be adjusted
     * after the fact by rotateCurrentCenter, so the residual value is relative to standard screen rotation
     * of 0 */
    public double current_rotation = 0;
    /** the current center x as recorded by drawChip or drawStack.  This is recorded live but may be adjusted
     * after the fact by rotateCurrentCenter, so the residual value is the real screen coordinate rather than
     * the possibly scaled and rotated value used during drawing.  */
    public int current_center_x = 0;
    /** the current  center y as recorded by drawChip or drawStack. This is recorded live but may be adjusted
     * after the fact by rotateCurrentCenter, so the residual value is the real screen coordinate rather than
     * the possibly scaled and rotated value used during drawing. */
    public int current_center_y = 0;
    /** the current size, as recorded by drawChip or drawStak */
    public int lastSize = -1;
    /** for stack cells, the last x scale for spacing between chips */
	public double lastXScale = 0.0;
	/** for stack cells, the last y scale for spacing between chips */
	public double lastYScale = 0.0;

	public SpriteStack animations = null;	// if not null, could be an animation where we're the destination
	public void addActiveAnimation(SpriteProtocol sprite)
	{
		if(animations==null) { animations = new SpriteStack(); }
		animations.push(sprite);
	}
	public int activeAnimationHeight()
	{	if(animations!=null)
	  	{	int n = 0;
	  		int skipped = 0;
	  		long now = G.Date();
	  		for(int lim = animations.size()-1; lim>=0; lim--)
	  		{
	  		SpriteProtocol an = animations.elementAt(lim);
	  		if(an.isExpired(now)) { animations.remove(lim,false); }
	  			else if(!an.isAlwaysActive() && (an.isOverlapped() || !an.isStarted(now))) { skipped++; }
	  			else { n+=an.animationHeight(); }
	  		}
	  		if((n+skipped)==0) { animations=null; }
	  		return(n);
	  	}
	  return(0);
	}
	public void copyFrom(ScreenData o)
	{
		current_center_x = o.current_center_x;
		current_center_y = o.current_center_y;
		lastXScale = o.lastXScale;
		lastYScale = o.lastYScale;
		current_rotation = o.current_rotation;
		lastSize = o.lastSize;
	}
}
public abstract class cell<FINALTYPE 
	extends cell<FINALTYPE>> 
	implements UniversalConstants,Drawable,Digestable
{	/** Geometry is the number of neighbors a cell has.  Normally the main board
	has a grid of cells with uniform, or almost uniform, geometry */
	public String getName() { return(toString()); }
	public int getWidth() { return lastSize(); }
	public int getHeight() { return lastSize(); }
	public static long classHash = 0;
	@SuppressWarnings("deprecation")
	public long getClassHash()
	{
		if(classHash==0)
		{
			classHash = getClass().getName().hashCode()*0x12235652;
		}
		return classHash;
	}
	public enum Geometry 
	{	/** isolated cell, no neighbors */
		Standalone(0),
		/** each cell links to 2 others */
		Line(2),	
		/** triangle, each cell has 3 neighbors */
		Triangle(3),	
		/** square, 4 orthoginals only, no diagonals */
		Square(4),		
		/** pentagonal, each cell has 5 neighbors */
		Pentagon(5),
		/** hex, each cell has 6 neighbors */
		Hex(6),			
		/** each cell has 7 neighbors.  I don't think this is a practical geometry */
		Septagon(7),
		/** square plus diagonals, the most common 8 connection geometry */
		Oct(8),	
		/** no fixed array, add and remove links as needed */
		Network(-1);
		public int n=0;	
		Geometry(int n) { this.n=n; }
	}
	private static double sensitiveSizeMultiplier = 1.6;
	/**
	 * multiplier to the size of a chip, used to determine if it is hit or not.  This
	 * allows the sensitive area to be different from the visible area which is 
	 * confounded by the original artwork.  The standard value is 1.6 which yields
	 * an area approximately equal to the visible area.  This is used by 
	 * @see #pointInsideCell
	 * @return the multiplier
	 */
	public double sensitiveSizeMultiplier() { return(sensitiveSizeMultiplier); }
	public int linkCount() { return(0); }
	public abstract chip<?> topChip();
	/** true if this cell is empty */
	public boolean isEmpty() { return topChip()==null; }
	
	/**
	 * this is used only by animations to get the chip at depth
	 * from the top.  It should never fail if the depth is nonsense.
	 * @param depth the depth from the top of the stack
	 * @return a chip
	 */
	public Drawable animationChip(int depth) { return(topChip()); }
	/**
	 * used to adjust the size for a particular destination animation
	 * @param size
	 * @return adjusted size
	 */
	public int animationSize(int size) { return(size); }
	public boolean forceSerialAnimation() { return(false); }
	
	public ScreenData getScreenData() 
	{
		if(screenData==null) 
			{ setScreenData(new ScreenData());
			}
		return(screenData);
	}
	public double lastXScale() { return(getScreenData().lastXScale); }
	public double lastYScale() { return(getScreenData().lastYScale); }
	public void setLastScale(double x,double y) {
		ScreenData da = getScreenData();
		da.lastXScale = x;
		da.lastYScale = y;
	}
	/**
	 * for stacks, this will correspond to the top of the stack
	 * @return
	 */
	public int centerY() { return getScreenData().current_center_y; }
	/**
	 * for stacks, this will correspond to the top of the stack
	 * @return
	 */
	public int centerX() { return getScreenData().current_center_x; }
	public double currentRotation() { return getScreenData().current_rotation; }
	public int lastSize() { return(getScreenData().lastSize);}
	public boolean hasScreenData() { return(screenData!=null); }
	public static cell<?>setScreenDataTarget = null;
	public void setScreenData(ScreenData d) 
	{ 	if(this==setScreenDataTarget) 
			{ G.print(G.getStackTrace()); }
		screenData = d; 
	}
	public void setLastSize(int d) { getScreenData().lastSize = d; }
	public void setCurrentRotation(double d) { getScreenData().current_rotation = d; }
	public void setCurrentCenterX(int c) { getScreenData().current_center_x = c; }
	public void setCurrentCenterY(int c) { getScreenData().current_center_y = c; }
	public void setCurrentCenter(int x,int y)
	{
		ScreenData a = getScreenData();
		a.current_center_x = x;
		a.current_center_y = y;
	}
	public double distanceTo(cell<?>from)
	{
		return G.distance(from.centerX(), from.centerY(),
				centerX(),  centerY());
	}
	public void assertCenterSet()
	{
		G.Assert(hasScreenData(),"From Cell %s has no screen data",this);
	}
	/**
	 * copy the current location (used for animations) from another cell.  Unusually
	 * for "copy" semantics, it will also copy data from this cell to the "from" cell
	 * if this cell has data and the "from" cell does not.  In any case, this will
	 * share the screendata with the from cell.  The intent of this oddness is that
	 * the center x,y and so on that are developed in the UI will be passed back
	 * to the real board, so that animations using the real board will have the
	 * correct coordinates.
	 * 
	 * @param from
	 */
	public void copyCurrentCenter(FINALTYPE from)
	{	if(from==null) { setScreenData(null);}
	    else if(!from.hasScreenData() && screenData!=null) { from.setScreenData(screenData); }
		else { setScreenData(from.getScreenData());}

	}
	/**
	 * duplicate the screen data of the "from" cell, but do not share structure.  This
	 * ought to be used when you create a proxy cell for animations.
	 * @param from
	 */
	public void duplicateCurrentCenter(FINALTYPE from)
	{	
		if(from==null || !from.hasScreenData()) {  return; }
		ScreenData sd = getScreenData();
		ScreenData fd = from.getScreenData();
		if(sd==fd) { setScreenData(null); sd = getScreenData(); }
		sd.copyFrom(fd);
	}
	/**
	 * starting Y position for animations of chips for this stack.  This number is
	 * always in "real window" coordinates, compensated for any rotation during drawing.
	 * @param size the chip display size
	 * @param depth the depth from the top of the stack
	 * @return a screen coordinate
	 */
	public int animationChipYPosition(int depth) { return(centerY()); }

	/**
	 * starting X position for animations of chips for this stack. This number is
	 * always in "real window" coordinates, compensated for any rotation during drawing.
	 * @param size the chip display size
	 * @param depth the depth from the top of the stack
	 * @return a screen coordinate
	 */
	public int animationChipXPosition(int depth) { return(centerX()); }
	/**
	 * the rotation to use for drawing animations with this cell as the destination.
	 * Normally, this will be the rotation that is in effect when drawn in non-animation
	 * too.
	 */
	public double activeAnimationRotation() { return(currentRotation()); }
	/**
	 * the geometry of this cell, which is also the number of neighbors
	 * the cell has.  Note that some board have edges, and therefore the
	 * neighbors are null, while others are fully connected into or torus
	 * so the neighbors are never null.  It's also possible to manipulate
	 * the default connectivity to create other special forms, such as the
	 * "hole" in the center of the tzaar board, or the oddly interconnected
	 * grid of octagons and squares in Octiles.
	 */
	public Geometry geometry = Geometry.Standalone;			// geometry corresponds to one of the above.
	/**
	 * an integer code for the "location" of this cell. This is typically
	 * used by mouse tracking to indicate what sort of object was hit.
	 */
	public CellId rackLocation=DefaultId.HitNoWhere;		// this is used to identify cells role in the game
	/**
	 * true if this cell is "on the board"
	 */
	public boolean onBoard=true;			// special mark for cells on the board
	/** the number of other real cells linked to from this cell.  If link
	 * count is less than geometry, the the cell is an edge of some sort.
	 */
    public int linkCount= 0;				// the number of other cells this cell is connected to
    /**
     * all the cells on the board are linked through the "next" field, so
     * it's possible to traverse all the cells on the board (in no particular
     * order) by starting at the board "allCells" and following the "next" 
     * links.
     */
	public FINALTYPE next = null;	// all cells on the whole board as a linked list.  This provides
		// the simplest and fastest way to traverse all the cells on a board.  This list has
		// to be built manually as the board is being built

	/**
	 * the adjacent cells.  Standard board creation will link these up after all the
	 * initial cells are created.  These adjacent cells normally form a circle,
	 * but some directions lead off the board and are empty.
	 * this is the convenient way to transit from one cell to and adjacent one
	 * <p>
	 * these adjacent cells are normally accessed by the exitToward method.
	 */
    private cell<FINALTYPE> adjacent[] = null;
    /**
     * get the number of cells adjacent to this one. Note that some of those
     * cells may be null at the edge of the board or in case of removed cells.
     * @return a boolean
     */
    public final int nAdjacentCells() 
    { // is is final to prevent accidentally overriding.
    	return(adjacent==null?0:adjacent.length);
    }

/**
 * the cell column. 
 */
	public char col;			// column for this cell, in the board's coordinate system
	/** 
	 * the cell row.
	 */
	public int row;				// row for this cell, in the board's coordinate system

	/** 
	 * copy the contents of this cell from "from". This method
	 * is typically wrapped by subclasses encapsulate the copy process.
	 * <p>
	 * the copy process should be aware of shared structure in the
	 * cell contents, but shouldn't copy or alter the cell's geometry
	 * 
	 * @param from
	 */
	public void copyAllFrom(FINALTYPE from)
	{	col = from.col;
		row = from.row;
		rackLocation = from.rackLocation;
		onBoard = from.onBoard;
		randomv = from.randomv;
	}
	// expect this to be encapsulated by specialized cell types
	public void copyFrom(FINALTYPE from)
	{	copyCurrentCenter(from);
	}
	/**
	 * the height of the stack of objects on this cell.  
	 */
	public int height() { return(0); }
	/** the default for all cell types and subtypes is the height */
	public int animationHeight() { return(height()); }
	
	/**
	 * a random number which serves as the "identity" of this cell for Digest()
	 */
	public long randomv = 0;
	/**
	 * set the identity of each cell in the "next" chain
	 */
	public void setDigestChain(Random r)
	{
       	for(cell<?>c=this; c !=null; c=c.next) { c.randomv = r.nextLong();	}
	}
	/**
	 *  Digest the cell's identity
	 *  @return an integer
	 */
	public long hiddenDigest() 
	{ if(randomv==0) 
		{ 
		return(getClassHash()+rackLocation.name().hashCode()*1000L+col*200+row+1); 
		}
		return(randomv); 
	}
	/**
	 * Digest the contents of this cell and the cell's identity.  The
	 * cell's identity is normally the next random integer from Random r
	 * 
	 * @param role the current random sequence
	 * @return an integer
	 */
	public long Digest(Random role) { return(hiddenDigest()^role.nextLong()); }

	/**
	 * static method to Digest a cell with may be null,  a role.
	 * @param r the current random sequence
	 * @param c the current cell, or null
	 * @return a long
	 */
	public static long Digest(Random r,cell<?> c) 
		{ return((c==null)
				?((r==null)?0:r.nextLong())
				: c.Digest(r)); }
	

	/**
	 * static method to Digest an array of cells which include nulls.
	 * @param r the current random sequence
	 * @param arr an array of cells
	 * @return a long
	 */
	public static long Digest(Random r,cell<?>[] arr) 
		{ long v = 0;
		  for(cell<?> c : arr) { v ^= Digest(r,c); }
		  return(v);
		}
	/**
	 *  constructor for singleton cells
	 */
	public cell()
	{	geometry = Geometry.Standalone;
		col = '@';
		row = -1;
		onBoard = false;
	}
	/**
	 *  constructor for singleton cells with identity
	 */
	public cell(Random r)
	{	this();
		randomv = r.nextLong();
	}
	/**
	 *  constructor for isolated cells with an identity
	 *  for th emouse.
	 *  @param loc - a location code
	 *  */ 
	public cell(CellId loc)
	{	this();
		rackLocation = loc;
	}
	/**
	 *  constructor for isolated cells with an identity
	 *  for the mouse.
	 *  @param loc - a location code
	 *  */ 
	public cell(Random r,CellId loc)
	{	this();
		rackLocation = loc;
		randomv = r.nextLong();
	}
	
	@SuppressWarnings("unchecked")
	public cell<FINALTYPE>[] newSelfArray(int n)
	{	return((cell<FINALTYPE>[])new cell[n]);
		//java.lang.reflect.Array.newInstance(getClass(),Math.max(0,geometry.n));
	}

	/**
	 * constructor for just a cell with geometry
	 * @param geo
	 */
	public cell(Geometry geo)
	{	this();
		geometry = geo;
		adjacent= (geo==Geometry.Standalone)
			?null
			: newSelfArray(Math.max(0,geometry.n));
	}
	/**
	 * constructor for just a cell with geometry and identity
	 * @param geo
	 */
	public cell(Random r,Geometry geo)
	{	this(geo);
		randomv = r.nextLong();
	}
	/**
	 * constructor for just a cell with geometry
	 * @param geo
	 */
	public cell(Geometry geo,CellId rack)
	{	this(geo);
		rackLocation = rack;
	}
	/**
	 * constructor for just a cell with geometry and identity
	 * @param geo
	 */
	public cell(Random r,Geometry geo,CellId rack)
	{	this(r,geo);
		rackLocation = rack;
	}
	/**
	 *  constructor for cells on the board
	 * @param geo the cell's geometry
	 * @param co column coordinate
	 * @param ro row coordinate
	 */
	public cell(Geometry geo,char co,int ro)
	{	this(geo);
		col = co;
		row = ro;
		onBoard = true;
	}
	/**
	 * constructor for cells on the board with a particular rack location and geometry
	 * @param geo
	 * @param rack
	 * @param co
	 * @param ro
	 */
	public cell(Geometry geo,CellId rack,char co,int ro)
	{	this(geo);
		rackLocation = rack;
		col = co;
		row = ro;
		onBoard = true;
	}
	/**
	 *  constructor for cells on the board
	 * @param geo the cell's geometry
	 * @param co column coordinate
	 * @param ro row coordinate
	 */
	public cell(Random r,Geometry geo,char co,int ro)
	{	this(geo,co,ro);
		randomv = (r==null)?0:r.nextLong();
	}
	/**
	 *  constructor for cells on the board
	 * @param geo the cell's geometry
	 * @param loc the cell's rack location
	 * @param co column coordinate
	 * @param ro row coordinate
	 */
	public cell(Random r,Geometry geo,CellId loc,char co,int ro)
	{	this(geo,co,ro);
		rackLocation = loc;
		randomv = r.nextLong();
	}
	/**
	 * clear the contents of the cell.  This method is normally wrapped
	 * by subclasses to encapsulate the initialization process.
	 */
	public void reInit() { }
	
	public static void reInit(cell<?> c[])
	{
		for(cell<?> d : c) { if(d!=null) { d.reInit(); }}
	}
	public static void reInit(cell<?> c[][])
	{	for(cell<?> d[] : c)
		{	cell.reInit(d);
		}
	}
	
/**
 * This is 
 * used to compare the cells of different boards.
 * @param other
 * @return true if this cell has the same location as the other cell.  
 */
	public final boolean sameCellLocation(FINALTYPE other)
	{	return((other!=null)
				&& (other.rackLocation==rackLocation)
				&& (other.row==row)
				&& (other.col==col));
	}
	
	/**
	 * return true is this cell has the same contents as other.
	 * 
	 * @param other
	 * @return true if this cell contains the same pieces as other
	 */
	abstract public boolean sameContents(FINALTYPE other);
	

	/**
	 * return true if this cell has the same location and contents as the other
	 * <p>
	 * this method is typically wrapped by subclasses to encapsulate 
	 * comparing the contents of cells.
	 * 
	 * @param other
	 * @return true of this cell is congruent with other
	 */
	public boolean sameCell(FINALTYPE other)
	{	return(sameCellLocation(other) && sameContents(other));
	}

	/**
	 * this method is typically overridded by the actual class to give
	 * a short summary of the cell contents.
	 * @return a short string representing the contents
	 */
	public String contentsString() { return(""); }
	/**
	 * a default printer which describes location and contents.
	 */
	@SuppressWarnings("deprecation")
	public String toString() { return("<"+getClass().getName()+" "+G.printCol(col)+row+ "=" + contentsString()+ ">"); }

    /**
    * this is the convenient way to traverse a row, column, or diagonal
    * we expect the directions to be 0-n, but for convenient we accept 
    * anything modulo the geometry of the cell
     * 
     * @param dir The directions are numbered 0 through n-1 (n=2,3,4,6, or 8 depending on the grid.)
     * the directions will form a clockwise circle, and direction x+n/2 is opposite direction n.
     * Usually, it's not relevant how these directions map into the board's coordinate system.
     * if it does matter, use the board's static constants CELL_LEFT CELL_UP CELL_RIGHT CELL_DOWN.
     * square boards with diagonals also define CELL_UP_LEFT CELL_UP_RIGHT_DOWN_LEFT and CELL_DOWN_RIGHT
     * CELL_QUARTER_TURN adds 90 degrees clockwise.  CELL_HALF_TURN adds 180 degrees.
     * @return the cell found in the desired direction, or null if it doesn't exist.
     */
    public FINALTYPE exitTo(int dir)
    {	if(geometry==Geometry.Standalone) { throw G.Error("%s has no exits",this); }
    	int len = adjacent.length;
    	int md = dir%len;				//modulo 0-n
    	@SuppressWarnings("unchecked")
		FINALTYPE ap = (FINALTYPE)adjacent[md<0 ? md+len : md];
     	return(ap);
    }
    
    /** re-count the links */
    public void countLinks()
    {	linkCount=0;
    	for(int i=0,max=adjacent.length;i<max;i++) { if(adjacent[i]!=null) { linkCount++; }}
    }
    
    /** 
     *  Note that this reflects reality immediately after the board is created, before
     *  and cells or other links are explicitly removed.
     * @return true if this cell has at least one null link.
     */
    public boolean isEdgeCell()
    {	return(linkCount!=geometry.n);
    }
    /**
     *  true if we're linked to c
     *  
     */
    public boolean isAdjacentTo(FINALTYPE c)
    {	for(int i=0;i<geometry.n;i++) { if(adjacent[i]==c) { return(true); }}
    	return(false);
    }
    /** 
     * find the direction to an adjacent cell, return -1 if not adjacent.
     * @param to
     * @return
     */
    public int findDirectionToAdjacent(FINALTYPE to)
	{	int len = adjacent.length;
		for(int i=0;i<len;i++) { if(adjacent[i]==to) { return(i); }}
		return(-1);
	}
    /** this is used to remove a particular bidirectional link between two cells,
     * as is needed for Fanorona.
     * @param c
     */
    public void unCrossLinkTo(Object c)
    {
    	for(int i=0;i<adjacent.length;i++)
    	{	if(adjacent[i]==c) { adjacent[i]=null; linkCount--; }
    	}
    }
    
    /**
     * this is used to remove a cell from the network of links in the board, as is necessary for the
     * center cell in tzaar
     */
    @SuppressWarnings("unchecked")
	public void unCrossLink()
    {
		for(int i=0;i<adjacent.length;i++)
		{	FINALTYPE rev = (FINALTYPE)adjacent[i];
			if(rev!=null) 
				{ adjacent[i]=null;
				  rev.unCrossLinkTo(this);
				  linkCount--;
				}
		}	
    }
    /**
     * add a 1 way link to another cell.  direction must be currently vacant
     * @param dir
     * @param to
     */
    public void addLink(int dir,FINALTYPE to)
    {	G.Assert(adjacent[dir]==null,"link is already set");
    	adjacent[dir]=to;
    }
    /** 
     * unconditional add link, not an error if the link already exists
     * @param dir
     * @param to
     */
    public void addLinkU(int dir,FINALTYPE to)
    {	adjacent[dir]=to;
    }
    
    /**
     * true if "to" is in the link list
     * @param to
     * @return a boolean
     */
    public boolean hasLink(FINALTYPE to)
    {
    	if(adjacent!=null) { for(Object l : adjacent) { if(l==to) { return(true); }} }
    	return(false);
    }
    /** add a unidirectional link between this cell and to 
     * @param to
     * */
    public void addLink(FINALTYPE to)
    {
    	G.Assert(geometry==Geometry.Network,"must be a network type");
    	if(!hasLink(to))
    	{	int plus = linkCount+1;
			cell<FINALTYPE> newlinks[] = newSelfArray(plus);
    		for(int i=0;i<linkCount;i++) { newlinks[i]=adjacent[i]; } 
    		newlinks[linkCount++] = to;
    		adjacent = newlinks;
    	}
    }
    /**
     * add a bidirectional link between cells "from" and "to".  Geometry must be Geometry.Network
     * @param from
     * @param to
     */
    public void addLink(FINALTYPE from,FINALTYPE to)
    {
    	from.addLink(to);
    	to.addLink(from);
    }
    /**
     * considering only points near r, accumulate points into pt,
     * finding the closest approach.
  * @param pt a point, presumably where the mouse is pointing
  * @param r a rectangle nominally corresponding to the board.
  * @return true if this is the closest point so far
     */
       public boolean closestPointToCell(HitPoint pt,Rectangle r)
       {	int w = G.Width(r);
       	int cx = G.Left(r)+w/2;
       	int h = G.Height(r);
       	int cy = G.Top(r)+h/2;
       	return(closestPointToCell(pt,Math.max(w,h),cx,cy));
       }
    /**
     * considering only points with a square of squareSize around x,y,
     * accumulate points into pt, eventually leaving hitObject as the closest cell 
     * this is intended to locate the closest point in a dense grid when the 
     * point of interest is somewhere near the grid.
     * @param pt
     * @param x
     * @param y
     * @return true if this is the closest point so far
     */
    public boolean closestPointToCell(HitPoint pt,int squareSize,int x,int y)
    {
    	if(G.pointInsideSquare(pt,x,y,squareSize/2)
    		&& pt.closestPoint(x,y,rackLocation)) 
    		{ 
    		pt.hitObject = this; 
    		pt.hit_x = x;
    		pt.hit_y = y;
    		pt.hit_width = squareSize;
    		pt.hit_height = squareSize;
    		return(true); 
    		}
    	return(false);
    }
    /**
     * this method is made visible so you can tweak the radius that constitutes a hit.
     * Note that the target is a smaller square, not a circle.  The overall area of the
     * "inside" is scaled by {@link #sensitiveSizeMultiplier()}
     * @param pt the mouse location
     * @param x  x position of the chip
     * @param y  y position of the chip
     * @param SQW square cell width of the chip
     * @param SQH is the cell height of the chip
     * @return true of point is inside the square centered at x,y
     * @see HitPoint#drawChipHighlight drawChipHighlight
     * @see chipCell#drawChip drawChip
     * @see stackCell#drawStack drawStack
     */
    public boolean pointInsideCell(HitPoint pt,int x,int y,int SQW,int SQH)
    {	double mp = sensitiveSizeMultiplier();
    	if(G.pointNearCenter(pt, x, y, 
    			(int)(mp*SQW/3),
    			(int)(mp*SQH/3))
    			&& pt.closestPoint(x,y,rackLocation))
    	{	pt.hitObject = this;
    		pt.hit_x = x;
    		pt.hit_y = y;
    		pt.hit_width = SQW;
    		pt.hit_height = SQH;
    		return(true);
    	}
    	return(false);
    }   
   /**
    * register a hit on this cell.  
    * @param highlight
    * @param e_x
    * @param e_y
    * @return true if this is the hit object (even if not new)
    */
   public boolean registerChipHit(HitPoint highlight,int e_x,int e_y,int e_w,int e_h)
   {
     	  highlight.hitObject = this; 
     	  highlight.hit_x = e_x;
     	  highlight.hit_y = e_y;
     	  highlight.hit_width = e_w;
     	  highlight.hit_height = e_h;
     	  highlight.hitCode = rackLocation;
     	  G.Assert(rackLocation!=DefaultId.HitNoWhere,"rackLocation must not be HitNoWhere");
     	  return(true);
   }
 /**
  * do highlight detection for {@link #drawChip} and {@link stackCell#drawStack drawStack}  This uses {@link #pointInsideCell} to 
  * determine if the point is inside, and calls {@link #registerChipHit} to mark the hit in the highlight object.
  * 
  * @param highlight
  * @param squareWidth
  * @param e_x
  * @param e_y
   * @return true if this is the first hit
  */
    public boolean findChipHighlight(HitPoint highlight,int squareWidth,int squareHeight,int e_x,int e_y)
 	{
        if(pointInsideCell(highlight, e_x, e_y, squareWidth,squareHeight))
        {	// this is carefully balanced so we do not re-evaluate the selecion if it is
      	// already established.  We return TRUE if this is the selection that was established,
      	// either this time or previously
        	return(registerChipHit(highlight,e_x,e_y,squareWidth,squareHeight));
        }
        return(false);

    }
    private ScreenData screenData=null;
    
	/**
	 * rotate the recorded center_x,center_y with a given rotation around a pivot
	 * this is used to position animations correctly when cells were displayed with
	 * a nonzero rotation
	 * @param displayRotation
	 * @param px
	 * @param py
	 */
	public void rotateCurrentCenter(double displayRotation,int x,int y,int px,int py)
	{	if(displayRotation!=0)
		{	
  		setCurrentCenter( G.rotateX(x, y, displayRotation, px, py),
  							G.rotateY(x, y, displayRotation, px, py));
		}
		else { 
			setCurrentCenter(x, y);
		}
		setCurrentRotation(displayRotation);
	}

	/**
	 * add an animation to this cell.
	 * @param sprite something that follows SpriteProtocol
	 */
	public void addActiveAnimation(SpriteProtocol sprite)
	{	getScreenData().addActiveAnimation(sprite);
	}
	/**
	 * the determines the number of animations which will end at this cell
	 * are still pending.  Normally, these animations correspond to chips
	 * that are already in place on this cell, but shouldn't be drawn yet
	 * until the animation is complete.
	 * @return an integer
	 */
	public int activeAnimationHeight()
	{	return getScreenData().activeAnimationHeight();
	}
	/**
	 * the top level of a stack, where drawStack stops.  This can be altered to draw
	 * partial stacks without actually removing the top chips.  It's used by sprite
	 * animation to make the newly placed chip disappear while an animation shows
	 * the chip on it's way to the destination.
	 * @return the nominal top of the stack, normally height()
	 */
	public int stackTopLevel() 
	{ return(height()-activeAnimationHeight());
	}
    /**
     * draw a particular glyph for this cell.  
     * This is used by {@link stackCell#drawStack drawStack} to draw each item in the stack.
     *  
     *
     * @param gc
     * @param drawOn
     * @param piece
     * @param SQUARESIZE
     * @param e_x
     * @param e_y
     * @param thislabel draw this text using {@link lib.exCanvas#labelFont labelFont} and {@link lib.exCanvas#labelColor labelColor}
     */
    public void drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,int SQUARESIZE,int e_x,int e_y,String thislabel)
    {	drawChip(gc,drawOn,piece,SQUARESIZE,1.0,e_x,e_y,thislabel);

    }
    /**
     * 
     * @param gc
     * @param drawOn
     * @param piece
     * @param SQUARESIZE
     * @param xscale
     * @param e_x
     * @param e_y
     * @param thislabel
     */
    public void drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,int SQUARESIZE,double xscale,int e_x,int e_y,String thislabel)
    {
      if(e_x>=HIDDEN_WINDOW_RIGHT && e_y>=0)		// avoid capturing the coordinates of hidden windows
      	{ 
    	  rotateCurrentCenter(gc,e_x,e_y);
      	  setLastSize(SQUARESIZE);
      	}
      if((gc!=null)&&(piece!=null)&&(SQUARESIZE>0))
  	  {
      if(thislabel==null) { piece.drawChip(gc,drawOn,SQUARESIZE,xscale,e_x,e_y,thislabel); }
      else {
	      Font f = GC.getFont(gc);
	      Color c = GC.getColor(gc);
   	  GC.setColor(gc,drawOn.labelColor);
  	  GC.setFont(gc,drawOn.labelFont);
 	  piece.drawChip(gc,drawOn,SQUARESIZE,xscale,e_x,e_y,thislabel);
	  	  GC.setFont(gc,f);
	  	  GC.setColor(gc, c);
   	  }}
    }
    /** draw the chip contained in this cell, and note if the mouse is inside.  The actual
     * drawing is done by the chip's drawChip method.  This is a good method to override if
     * you want to substitute fancy display-only bits on the board.
     * 
     * @param gc the graphic context to draw on (can be null)
     * @param drawOn the canvas to draw on (corresponding to gc)
     * @param piece  the chip to draw
     * @param highlight contains the mouse position, receives info about the hit if any
     * @param SQUARESIZE scale te picture to this size
     * @param e_x x center of the square
     * @param e_y y center of the square
     * @param thislabel after drawing, superimpose this text
     * @return true if the mouse hit this object.
     */
	public boolean drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,HitPoint highlight,int SQUARESIZE,int e_x,int e_y,String thislabel)
	{ 
      return(drawChip(gc,piece==null?null:piece.getAltDisplayChip(this),drawOn,highlight,SQUARESIZE,1.0,e_x,e_y,thislabel));
  	}
	
	/**
	 * test the highlight point for a hit on this piece, as adjusted by the image aspect
	 * ratio, the scale, and offsets for this chip
	 * @param highlight
	 * @param piece
	 * @param squareWidth
	 * @param squareHeight
	 * @param x
	 * @param y
	 * @return true if this chip is hit
	 */
	public boolean findChipHighlight(HitPoint highlight,chip<?> piece,int squareWidth,int squareHeight,int x,int y)
	{	
		// this used to use the scale[x] and scale[y] to adjust the centering
		// of the pieces, but now does not.  The scale adjustments should be
		// used to move the pieces so the artwork visually centers, so the
		// logical center of the piece is still zero.   If you need the art
		// to actually move the location of a piece, override this method
		//int dx=0;
		//int dy=0;
		//if(piece!=null)
		//{double scale[] = piece.scale;
		// dx = (int)((scale[0]-0.5)*squareWidth);
		// dy = (int)((scale[1]-0.5)*squareHeight);
		//}
		//return(findChipHighlight(highlight,squareWidth,squareHeight,x-dx,y-dy));
	
		// the purpose of "piece" in this method, which is stripped off here,
		// is to allow pentomino games to calculate hits based on the full
		// extend of the piece, rather than a single square.
		return(findChipHighlight(highlight,squareWidth,squareHeight,x,y));
	}
	/**
	 * draw a chip and test for mouse sensitivity
	 * @param gc			// the graphics to draw with
	 * @param piece			// the chip to draw
	 * @param drawOn		// the canvas to do the drawing
	 * @param highlight		// the mouse point, or null.  Receives hit information
	 * @param squareWidth	// the overall scale of the object
	 * @param scale			// the percentage scale of the x axis (used to stretch or squish)
	 * @param e_x			// the center of the object
	 * @param e_y			// the center y of the object
	 * @param thislabel		// label to print on top
	 * @return true if this chip is hit
	 */
	public boolean drawChip(Graphics gc,chip<?> piece,exCanvas drawOn,HitPoint highlight,int squareWidth,double scale,int e_x,int e_y,String thislabel)
	{ int last = gc!=null ? gc.last_count : -1;
      drawChip(gc,drawOn,piece,squareWidth,scale,e_x,e_y,thislabel);
      double aspect = (piece==null)?1.0:piece.getAspectRatio(drawOn);
      int squareHeight = Math.max(1,(int)(squareWidth/aspect));
      // note we need to pass "piece" here so that pieces with unusual 
      // drawing methods can keep track of what is being drawn.  Specificially
      // all the pentomino games use single cell "pieces" that actually
      // extend across many pieces.
      //
      // note that the gc based method of locating the highlight probably isn't correct, it interacts
      // with the peculiar characteristics of the artwork, in particular "trench" which overrides this
      // method.  The crux of the issue is if the artwork isn't perfectly centered and is being recentered
      // using the scale x,y,size
      //
      boolean val = (last>=0 && gc.last_count==last+1 ) 
    		  			? findChipHighlight(highlight,piece,gc.last_w,gc.last_h,gc.last_x+gc.last_w/2,gc.last_y+gc.last_h/2) 
    		  			: findChipHighlight(highlight,piece,squareWidth,squareHeight,e_x,e_y);
      return(val);
 	}
	
	/**
	 * draw a chip and test for mouse sensitivity
	 * @param gc			// the graphics to draw with
	 * @param drawOn		// the canvas to do the drawing
	 * @param piece			// the chip to draw
	 * @param highlight		// the mouse point, or null.  Receives hit information
	 * @param squareWidth	// the overall scale of the object
	 * @param squareHeight	// the overall height of the sensitive area
	 * @param e_x			// the center of the object
	 * @param e_y			// the center y of the object
	 * @param thislabel		// label to print on top
	 * @return true if this chip is hit
	 */
	public boolean drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,HitPoint highlight,int squareWidth,int squareHeight,int e_x,int e_y,String thislabel)
	{ 
      drawChip(gc,drawOn,piece,squareWidth,1.0,e_x,e_y,thislabel);
      boolean val = findChipHighlight(highlight,null,squareWidth,squareHeight,e_x,e_y);
      return(val);
 	}
	// method for "DrawableSprite" interface
	public void drawChip(Graphics gc,exCanvas c,int size, int posx,int posy,String msg)
	{
		throw G.Error("method required to use the DrawableSprite interface");
	}
	/**
	 * set the current_center_x and current_center_y (used for animations) rotated 
	 * appropriately for the current canvas rotation.  This should be used for 
	 * locations drawn in the context of main game board which are not captured
	 * by drawChip or drawStack
	 *  
	 * @param gc
	 * @param cx
	 * @param cy
	 */
	public void rotateCurrentCenter(Graphics gc,int cx,int cy)
	{	if(gc!=null) { gc.rotateCurrentCenter(this,cx,cy); }
		}
 }