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
package medina;

import lib.*;
import medina.MedinaChip.DomeColor;
import medina.MedinaChip.PalaceColor;

class ClusterStack extends OStack<Cluster>
{
	public Cluster[] newComponentArray(int n) { return(new Cluster[n]); }
}

public class Cluster {
	
	// constructor
	Cluster (MedinaCell initial_cell,MedinaChip initial_chip)
	{	seed = initial_cell;
		type = initial_chip;
		isMeepleCluster = type.isMeeple();
		isPalaceCluster = type.isPalace();
		complete = true;
	}
	void addExpansionCell(MedinaCell c)
	{
		if(expansionCells == null) 
			{ expansionCells = new CellStack(); }
		expansionCells.pushNew(c);
		complete = false;
	}
	void addWallCluster(MedinaCell c)
	{
		G.Assert(c.cluster!=null,"cluster must be set");
		if(touchedWalls==null) { touchedWalls = new ClusterStack(); }
		touchedWalls.pushNew(c.cluster); 
	}
	
	void addMeepleCell(MedinaCell c)
	{	if(meeples==null) { meeples = new CellStack(); }
		if(meeples.pushNew(c)) { meeple_size++; }
	}
	
	boolean isExpansionCell(MedinaCell c)
	{	if(expansionCells!=null) { return(expansionCells.contains(c)); }
		return(false);
	}
	public Cluster next;
	public MedinaCell tower;	// associated tower for a wall cluster
	public MedinaCell seed;	// cell that seened the cluster
	MedinaChip type;	// piece type that founded the cluster
	PalaceColor palaceColor() { return(type.palaceColor()); }
	public MedinaChip clusterOwner;		// dome that owns the cluster
	public MedinaCell wallClaimer;	// the move that last claimed this wall cluster
	public DomeColor owner() { return(clusterOwner.domeColor()); }
	
	int size = 0;		// number of elements in the cluster
	int stable_size = 0;	// number of stables in this cluster
	int palace_size = 0;	// number of palaces in this cluster
	int meeple_size=0;	// number of adjacent meeples
	int moveExpanded = 0;
	public int wall_size = 0;	// number of adjacent walls
	public int edge_size = 0;	// empty edge adjacent
	
	
	CellStack expansionCells=null;
	CellStack meeples=null;
	ClusterStack touchedWalls = null;
	
	public boolean complete;		// true if claimed or not expandable
	public boolean claimed;		// true if claimed (by owner)
	public boolean isMeepleCluster;	// true if type is a meeple
	public boolean isPalaceCluster;	// true if type is a palace
	
	public String toString()
	{	int exs = expansionCells==null ? 0 : expansionCells.size();
		return("<cluster "+seed+type+" sz="+size+" exp="+exs);
	}

}
