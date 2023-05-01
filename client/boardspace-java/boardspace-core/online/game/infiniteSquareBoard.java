package online.game;

import lib.G;

/**
 * common support for square geometry boards with connections in 2 4 or 8 directions.  
 * @see squareBoard
 * @see rectBoard
 * @see trackBoard 
 * @author ddyer
 *
 */
public abstract class infiniteSquareBoard<CELLTYPE extends cell<CELLTYPE>> extends infiniteBoard<CELLTYPE>
{	
	private static int sq_dxs[] = { -1, 0, 1, 0};
	private static int sq_dys[] = {  0, 1, 0, -1};
	public int[] dxs() { return sq_dxs; }
	public int[] dys() { return sq_dys; }
	
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
    /**
     * find the direction from cell from to cell to, which are in a line
     * @param from
     * @param to
     * @return an integer
     */
    public int findDirection(CELLTYPE from,CELLTYPE to)
    {
    	return(findDirection(from.col,from.row,to.col,to.row));
    }
}
