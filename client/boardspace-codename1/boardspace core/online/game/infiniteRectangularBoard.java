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
import online.game.cell.Geometry;
/**
 * the generic part of all rectangular boards with diagonal connections
 * @see squareBoard
 * @see trackBoard
 * @author ddyer
 *
 */
public abstract class infiniteRectangularBoard<CELLTYPE extends cell<CELLTYPE>> extends infiniteSquareBoard<CELLTYPE> 
{

    // factory method for the board cells
    private static int oct_dxs[] = {-1,-1,-1, 0, 1, 1, 1, 0};
    private static int oct_dys[] = {-1, 0, 1, 1, 1, 0,-1,-1};
    public static  String DIRECTION_NAMES[] =
		{ "SW", "W", "NW", "N", "NE", "E", "SE", "S"};
    
    public int[] dxs() { return oct_dxs; }
    public int[] dys() { return oct_dys; }
    public Geometry geometry() { return Geometry.Oct; }

    /** create a square board with specified columns and rows.
     *  the newly crreated cells are linked to their neighbors
     *  and into a list through cell.next
     */
	public void  initBoard(int ncol,int nrow)
	{	
		super.initBoard(ncol,nrow);

	}
	/**
	 * true of direction number is diagonal, on a rectangular geometry geometry board
	 * @param dir
	 * @return true if dir represents a diagonal direction
	 */
    public boolean isDiagonalDirection(int dir) { return((dir&1)==0); } 

	// these are the standard directions for square boards,
	// we check at init time that they actually came out this way
    // note that these are protected to prevent them accidentally being
    // used by other classes when the board they should be using has a different geometry.
	/** move visual down and to the left to the next cell */
    public static final int CELL_DOWN_LEFT = 0;	
	/** move visually left from a cell to the next cell */
    public static final int CELL_LEFT = 1;
	/** move visually up and to the left to the next cell */
    public static final int CELL_UP_LEFT = 2;
	/** move visually up to the next cell */
    public static final int CELL_UP = 3;
	/** move visually up and to the right to the next cell */
    public static final int CELL_UP_RIGHT = 4;
	/** move visually right to the next cell */
    public static final int CELL_RIGHT = 5;
	/** move visually down and to the right to the next cell */
    public static final int CELL_DOWN_RIGHT = 6;
	/** move visually down from this cell to the next cell */
    public static final int CELL_DOWN = 7;
	/** move a direction visually a quarter turn clockwise */
    public static final int CELL_QUARTER_TURN = 2;
	/** move a direction visually a half turn */
    public static final int CELL_HALF_TURN = 4;
	/** the number of directions in a cell */
    public static final int CELL_FULL_TURN = 8;
	
	public static final int CELL_DOWN() { return(CELL_DOWN); }
	public static final int CELL_RIGHT() { return(CELL_RIGHT); }
	public static final int CELL_LEFT() { return(CELL_LEFT); }
	public static final int CELL_UP() { return(CELL_UP); }
	public static final int CELL_DOWN_LEFT() { return(CELL_DOWN_LEFT); }
	public static final int CELL_UP_LEFT() { return(CELL_UP_LEFT); }
	public static final int CELL_DOWN_RIGHT() { return(CELL_DOWN_RIGHT); }
	public static final int CELL_UP_RIGHT() { return(CELL_UP_RIGHT); }
	public static final int CELL_FULL_TURN() { return(CELL_FULL_TURN); }
	public static final int CELL_QUARTER_TURN() { return(CELL_QUARTER_TURN); }
	public static final int CELL_HALF_TURN() { return(CELL_HALF_TURN); }

}
