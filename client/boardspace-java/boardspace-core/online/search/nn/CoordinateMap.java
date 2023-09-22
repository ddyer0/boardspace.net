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

/** a CoordinateMap is used by filter layers to map indexes of neurons to those of 
 * adjacent neurons.
 * @author Ddyer
 *
 */
public interface CoordinateMap {
	/** get the index in the layer associated with column,row */
	public int getIndex(char col,int row);
	/** get the number of neighbors of each neuron */
	public int getNNeighbors();
	/** get the index of the i'th neighbor of col,row */
	public int getNeighborIndex(char col,int row,int i);
	/** get the index of the i'th neighbor of the neuron at index */
	public int getNeighborIndex(int index,int i);
	public int getNCols();
	public int getNRows();
	public int getMaxIndex();
	public char getColForIndex(int d);
	public int getRowForIndex(int d);
}
