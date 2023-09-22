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
import online.game.cell.Geometry;
/** support for generic triangular grid boards.  This geometry
 * is used by Spangles. 
 *
 * @author ddyer
 *
 */

public abstract class triBoard<CELLTYPE extends cell<CELLTYPE>> extends infiniteBoard<CELLTYPE>
{
    /*  
     * the board is oriented such that all triangles have a horizontal base, with
     * alternating triangles pointing "up" or "down".  "Down" means from tip to
     * base, and "down" is direction #2.  Direction 0 is visually left for both
     * up pointing and down pointing cells. Direciton 1 is right for both up and
     * down cells.  Therefore, the reverse direction if a->b is 2<>2 0<>1 1<>0
     */
	private final int tri_dxs[] = { -1, 1, 0};
    private final int tri_dys[] = { 0,0,1 };
    
    public int[] dxs() { throw G.Error("not used"); }
    public int[] dys() {  throw G.Error("not used"); }
    

    // get the col for exit in dir.  This incantation is special for the spangles board
    // so the flat side points to the adjacent flat side above or below
	public char getExitCol(CELLTYPE cc,int dir)
 	{
 		char xn = (char)(cc.col + ((tri_dys[dir]==0) ? tri_dxs[dir] : (((cc.row&1)!=0)?1:-1)));
 		return xn;
 	}
	// get the row  for exit in dir.  This incantation is special for the spangles board
    // so the flat side points to the adjacent flat side above or below
	public int getExitRow(CELLTYPE cc,int dir)
 	{
 		int yn = cc.row + (tri_dys[dir]*(((cc.col&1)!=0)?1:-1));
 		return yn;
 	}
    
    public Geometry geometry() { return Geometry.Triangle; }
    
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
	  {	throw G.Error("not to be used");
	  }

 
    /** there are two common styles of user-level coordinates for
     *  hexagonal boards.  Columns are pretty universally named A,B,C etc,
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
    /**
     * our links are not directly invertible, but there is always a reverse link
     */
    public boolean invertable() { return false; }
}
