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
package online.search.nn;

import lib.G;
/** coordinate map for a rectangular board */
public class SquareCoordinateMap implements CoordinateMap
{	int nrows;
	int ncols;
	int neighborMap[][] = null;
	public int getNCols() { return(ncols); }
	public int getNRows() { return(nrows); }
	public SquareCoordinateMap(int cols,int rows)
	{
		ncols = cols;
		nrows = rows;
		buildNeighborMap();
	}
	public int getIndex(char col, int row) {
		if((col<'A')
				|| (col>='A'+ncols)
				|| (row<1) 
				|| (row>nrows)) { return(-1); }
		return((col-'A')*nrows+row-1);
	}
	public char getColForIndex(int ind)
	{
		return((char)('A'+ind/nrows));
	}
	public int getRowForIndex(int ind)
	{
		return(1+ind%nrows);
	}

	private int getSetSize() {
		return(ncols*nrows);
	}

	public int getNNeighbors() {
		return 8;
	}

	public int getNeighborIndex(char col, int row, int neighbor) 
	{
		switch(neighbor)
		{
		default: throw G.Error("Neighbor %s not defined",neighbor);
		case 0:
			return(getIndex((char)(col-1),row)); 
		case 1:
			return(getIndex((char)(col-1),row+1));
		case 2:
			return(getIndex(col,row+1));	
		case 3:
			return(getIndex((char)(col+1),row+1));
		case 4:
			return(getIndex((char)(col+1),row));
		case 5:
			return(getIndex((char)(col+1),row-1));
		case 6:
			return(getIndex(col,row-1));
		case 7: 
			return(getIndex((char)(col-1),row-1));
		}
		}
	private void buildNeighborMap()
	{	int nnab = getNNeighbors();
		neighborMap = new int[getSetSize()][nnab];
		for(int coln=0;coln<ncols;coln++)
		{	char col = (char)('A'+coln);
			for(int row=1;row<=nrows;row++)
			{
				int idx = getIndex(col,row);
				int nb[] = neighborMap[idx];
				for(int i=0;i<nnab;i++)
				{
					nb[i]=getNeighborIndex(col,row,i);
				}
				
			}
		}
	}
	public int getNeighborIndex(int index,int nab)
	{	return(neighborMap[index][nab]);
	}
	public int getMaxIndex() { return(getNCols()*getNRows()); }
}
