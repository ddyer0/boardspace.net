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
import lib.IStack;
import lib.IntObjHashtable;

/* support for hex boards which start as a rhombus of some size, then can be
 * dynamically enlarged one cell at a time.  This is intended to be used for
 * boardless hex games such as Palago and Hive 
 *
 * as a side effect of dynamically enlarging the board, the "col" field of cells
 * can wander out of the normal A-Z range, which would wreak havoc in game records,
 * so the *printed* representation of col will be 3A 2A 1A A B ... Z 1Z 2Z 3Z
 * this requires some minor adjustment in the xxMovespec class for each game.
 * also, because it's encoded as a char, the range is not really infinite, but
 * 0-255 (or maybe more) will have to do.
 * 
 * another side effect of adding cells after the fact is that the printing order 
 * based on the raster is pretty much toast.  Instead, we make a sorted version
 * of the cell array, sorted appropriately for the Itype.xxx iterator request.
 * This is cached, so care has to be taken to uncache it when the cell list 
 * changes, or the perspective/screen reverse causes the presentation to change.
 * 
 * The torus option is still available, and can be used for legacy games made with
 * torroid boards.  Flipping back and forth from torus to expandable is possible,
 * but some care has to be taken.
 * 
 * Adding new cells is not completely automatic, GetCell will create new cells
 * automatically, and you can call createAdjacentCells to expand the margins.
 * 
 * new cells "on the board" should not use randomv, but depend on the native cell
 * methods to give each cell an intrinsic random seed.
 * 
 * @author ddyer
 *
 */
public abstract class infiniteBoard<CELLTYPE extends cell<CELLTYPE>> extends fBoard<CELLTYPE>
{
    /* the x,y offset to adjacent cell in any of the 6 directions of a hexagonal board. 
     * these are mostly superseded by using the exitToward method of the cells 
     */
       
    /** the keys for cells added in addition to the initial size */
	public int ncols = 19;
	public int nrows = 19;
	
	public boolean createNewCells = true;

	public boolean includeInBoundingBox(CELLTYPE c)
	{
		return (!(c.topChip()==null) 
				|| (c.col>='A' && c.row>=1 && c.col<='A'+ncols && c.row<=nrows));
	}
	
    /**
     * create the adjacent cells to "c" and link them
     * 
     * @param c
     */
    public void createExitCells(CELLTYPE c)
    {	if(!isTorus)
    	{
    	for(int direction = 0; direction<c.geometry.n; direction++)
    	{
    		CELLTYPE d = c.exitTo(direction);
    		if(d==null)
    		{
    			char nc = getExitCol(c,direction);
    			int nr = getExitRow(c,direction);
    			CELLTYPE d2 = createInitialCell(nc,nr);
    			//G.print("add adjacent "+d2);
    			linkAdjacentCells(d2);
    			addedCells.push(keyfor(nc,nr));
    		}
    	}}
    }

	public int initialSize() { return Math.max(ncols,nrows); }
	
	/**
	 * this remembers all the cells in the board, including any added to the initial set
	 */
    IStack addedCells = new IStack();
	IntObjHashtable<CELLTYPE> board = new IntObjHashtable<CELLTYPE>();
	
	private int keyfor(char col,int row)
	{
		return (col+1000)*10000+row+1000;
	}
	private char colforkey(int k)
	{
		return (char)((k/10000)-1000);
	}
	private int rowforkey(int k)
	{
		return (k%10000)-1000;
	}
	public void SetBoardCell(char col,int row,CELLTYPE cc)
	{
		int key = keyfor(col,row);
		board.put(key,cc);
	}
	public CELLTYPE GetBoardCell(char col,int row)
	{
		int key = keyfor(col,row);
		return board.get(key);
	}

	public boolean validBoardPos(char col,int row)
	{
		return (GetBoardCell(col,row)!=null);
	}

	/**
	 * fetch a cell by coordinates, and create any cell that doesn't exist
	 */
	public CELLTYPE getCell(char col,int row)
	{
		CELLTYPE c = super.getCell(col,row);
		if(c==null && createNewCells && !isTorus)
		{
			c = createInitialCell(col,row);
			//G.print("add "+c);
			linkAdjacentCells(c);			
			addedCells.push(keyfor(col,row));
		}
		return c;
	}
    public final char torusWrapCol(char col) 
    { if(isTorus)
    	{if(col<'A') { col+=ncols; }
    		else if (col>=('A'+ncols)) { col -= ncols; }
    	}
      return(col);
    }
    public final int torusWrapRow(int row)
    {	if(isTorus)
    	{
    	if(row<=0) {  row += nrows; }
    	else if(row>nrows) { row -= nrows; }
    	}
    	return(row);
    }
    private int boardCellCount()
    {	int n = 0;
    	CELLTYPE c = allCells;
    	while (c!=null) { n++; c=c.next; }
    	return n;
    }
    private void removeAddedCell()
    {
    	int key = addedCells.pop();
    	G.Assert(	(colforkey(key)==allCells.col)
    				&& (rowforkey(key)==allCells.row),"must match cells");
    	board.remove(key);
    	unlinkCell(allCells);	// this removes the cell from allcells and all reverse links
    }
	public void copyFrom(infiniteBoard<CELLTYPE> other)
	 {	//
		// deal with copies that have been used which can create extra cells,
		// and masters that have been used so the copies have to catch up
		// and masters that have transitioned from legacy torus and back
		//
		setIsTorus(other.isTorus);
		// first discard the extra cells in this copy
	 	IStack added = other.addedCells;
	 	CELLTYPE oac = other.allCells;
	 	if(oac==null || !(allCells.sameCellLocation(oac) && (added.size()==addedCells.size())))
	 	{	int otherc = other.boardCellCount();
	 		int ourc = boardCellCount();
	 		while (ourc>otherc)
	 		{	removeAddedCell();
	 			ourc--;
	 		}
	 		if(otherc==ourc && allCells!=null)
	 		{
	 		while(!allCells.sameCellLocation(oac))
	 		{	removeAddedCell();
	 			ourc--;
	 			oac = oac.next;	 			
	 		}}
	 	}
	 	
		// next create the extra cells added to the board so far
	 	for(int i=addedCells.size(),lim=added.size(); i<lim; i++)
	 	{
	 		int key = added.elementAt(i);
	 		char col = colforkey(key);
	 		int row = rowforkey(key);
	 		addedCells.push(key);
	 		CELLTYPE c = createInitialCell(col,row);
	 		linkAdjacentCells(c);
	 	}
	 	G.Assert((allCells==null && other.allCells==null) 
	 				|| allCells.sameCellLocation(other.allCells),"cells match");
		super.copyFrom(other);
	 	ncols = other.ncols;
	 	nrows = other.nrows;
	 	createNewCells = other.createNewCells;
	 }
	public void sameboard(infiniteBoard<CELLTYPE> other)
	{	// sameboard checks the cells using the allCells chain
		super.sameboard(other);
		
	}
 
	/**
	 * create an initial grid of rows and columns
	 * cols start at 'A' and rows start at 1
	 * 
	 * @param nc number of columns
	 * @param nr number of rows
	 */
    public void initBoard(int nc,int nr)
    {	
       	ncols = nc;
       	nrows = nr;
       	geometry = geometry();
        if (Grid_Style == null)
        {
            Grid_Style = new String[3];
            Grid_Style[GRID_TOP] = "A1"; // default to letters and numbers across the top and bottom 
            Grid_Style[GRID_BOTTOM] = "A1";
        }
        board.clear();
        addedCells.clear();
        for (int col = 0; col < ncols; col++)
        {
            char thiscol = (char) ('A' + col);
            for(int row = 1; row<=nrows; row++)
            {
            	createInitialCell(thiscol,row);
            }            	
        }
        
        G.Assert(geometry==allCells.geometry,"board geometry should match cell geometry");

        setDirectCrossLinks();
  
    }
        
    private CELLTYPE createInitialCell(char thiscol,int row)
    {
    	CELLTYPE newc = newcell(thiscol,row);
    	addCell(newc);
    	SetBoardCell(thiscol,row,newc);
    	return newc;
    }

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
    public boolean reInitBoard(int ncols,int nrows)
    {	
    	if(allCells!=null)
    	{
    		for(CELLTYPE c = allCells; c!=null; c=c.next) { c.reInit(); } 
    		return(false);
    	}
    	else
    	{	
    		initBoard(ncols,nrows);
    		addedCells.clear();
     		return(true);
    	}
    }
    public void setIsTorus(boolean v)
    {
    	if(v!=isTorus)
    	{	isTorus = v;
    		allCells = null;
    		reInitBoard(ncols,nrows);
    	}
    }

    /**
     * convert col,row into a row number, considering the reverse x and reverse y flags.
     * @param col
     * @param arow
     * @return an integer
     */
    final int geo_rownum(char col,int arow)
    {	if(displayParameters.reverse_y)
    	{
    	return nrows-arow;
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
    final int geo_colnum(char colchar)
    {	
    	int col = colchar - 'A';
    	if(displayParameters.reverse_x!=displayParameters.reverse_y) 
    		{ return ncols-col;
    		}
    	return(col);
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
    /** convert cell col,row to Y coordinate
     * 
     */
    public double cellToY00(char colchar, int arow)
    {	
        int thisrow = geo_rownum(colchar,arow);
        double y0 = (thisrow * displayParameters.CELLSIZE);
        return (y0);
    }

}
