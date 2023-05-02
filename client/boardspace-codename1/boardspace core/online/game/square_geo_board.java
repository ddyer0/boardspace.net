package online.game;

import lib.G;
import lib.Random;

/**
 * common support for square geometry boards with connections in 2 4 or 8 directions.  
 * @see squareBoard
 * @see rectBoard
 * @see trackBoard 
 * @author ddyer
 *
 */
public abstract class square_geo_board<CELLTYPE extends cell<CELLTYPE>> extends gBoard<CELLTYPE>
{	
	
	public void setRotation(int rotation)
	{
		switch(rotation%4)
    	{
    	default:
    	case 0: 	
    		displayParameters.reverse_y = false;
    		displayParameters.reverse_x = false;
    		displayParameters.swapXY = false;
		break;
    	case 2:		
    		displayParameters.reverse_y = true;
    		displayParameters.reverse_x = false;
    		displayParameters.swapXY = false;
		break;
    	case 3:	
    		displayParameters.reverse_y = false;
    		displayParameters.reverse_x = true;
    		displayParameters.swapXY = true;
		break;
    	case 1:	
    		displayParameters.reverse_y = true;
    		displayParameters.reverse_x = true;
    		displayParameters.swapXY = true;
		break;
   	}
	}

    /**
     * find the direction from fc,fr to tc,tr (given that they are on a line)
     * these directions are appropriate for cell.exitTo.
     */
    public int findDirection(char fc,int fr0,char tc,int tr0)
    {	int dx = G.signum(tc-fc);
    	int dy = G.signum(tr0-fr0);
    	int dxs[] = dxs();
    	int dys[] = dys();
    	for(int i=0;i<dxs.length;i++) { if((dxs[i]==dx)&&(dys[i]==dy)) { return(i); }}
    	throw G.Error("No direction found");
    }

	public int firstRowInColumn(char col) { return(1); } ;
	public int lastRowInColumn(char col) { return(nrows); };
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
     * @return true if this is the first time - ie; is init rather than re-init
     */
	public boolean reInitBoard(int ncol,int nrow)
	{
		if((nrows==nrow) && (ncols==ncol) && (allCells!=null))
		{
			for(CELLTYPE c = allCells ; c!=null; c=c.next) { c.reInit(); } 
			return false;
		}
		else 
		{ initBoard(ncol,nrow);
		  Random r = new Random(0xf267*nrows*ncols);
		  allCells.setDigestChain(r);
		  return true;
		}
	}
	
    public void initBoard(int ncol,int nrow)
    {	nrows = nrow;
    	ncols = ncol;
    	geometry = geometry();
    	createBoard(ncols,nrows);
    	// for future support of non-rectangular boards (ie for Domination)
    	firstRowInCol = new int[ncol];
    	nInCol = new int[ncol];
    	for(int i=0;i<ncol;i++) { nInCol[i]=nrow; }
    	
    	if(Grid_Style==null)
    	{
        Grid_Style = new String[3];
        Grid_Style[GRID_LEFT] = "1"; // default to letters across the bottom, numbers along the side
        Grid_Style[GRID_BOTTOM] = "A";
    	}
    	for(int co=0;co<ncols;co++)
    	{ char col = (char)('A'+co);
    	  for(int ro=0;ro<nrows;ro++)
	    	{	int row = 1+ro;
	    		CELLTYPE nc = newcell(col,row);
	    		SetBoardCell(col,row,nc);
	    		nc.next = allCells;
	    		allCells = nc;
	    	}
    	}
    	G.Assert(geometry==allCells.geometry,"Board geometry is %s, but should match cell geometry %s",geometry,allCells.geometry);
    	setCrossLinks();
 
    }
     /* convert col,row to a y */
    public final int BCtoYindex(char col, int row)
    {	return(row-1);
    }

    /* convert x,y to a row.  */
    public final int YindexToBC(int x, int y)
    {	if(isTorus)
			{
    		if(y>=nrows) { y-= nrows; }
	  		if(y<0) { y+= nrows; }
			}
    	return(y+1);
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
    
    public final boolean validBoardPos(char col0, int row0)
    {	return((col0>='A')
    			&&(col0<(char)('A'+ncols))
    			&&(row0>=1)
    			&&(row0<=nrows));
    			
    }

    // convert col,row to y with no rotation or perspective
    public double cellToY00(char colchar, int thisrow0)
    {	int thisrow = displayParameters.swapXY ? colchar-'A'+1 : thisrow0;
    	int row = displayParameters.reverse_y ? nrows-thisrow+1 : thisrow;
    	return(row * displayParameters.YCELLSIZE);
    }

    // col,row to x with no rotation or perspective
    public double cellToX00(char colchar0, int row)
    {	char colchar = displayParameters.swapXY ? (char)(row+'A'-1) : colchar0;
        int col = colchar - 'A';
        if(displayParameters.reverse_x!=displayParameters.reverse_y) { col = ncols-(col+1); }
        return (col * displayParameters.CELLSIZE);
    }
}
