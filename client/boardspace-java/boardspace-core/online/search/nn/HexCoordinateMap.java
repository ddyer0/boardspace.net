package online.search.nn;

import lib.G;

/* coordinate map for a rhombic hex board */
public class HexCoordinateMap implements CoordinateMap 
{	int span;
	int neighborMap[][];
	public int getNRows() { return(span); }
	public int getNCols() { return(span); }
	
	public HexCoordinateMap(int s)  
	{ 	span = s; 
		buildNeighborMap(); 
	}
	public char getColForIndex(int ind)
	{	char ch = (char)('A'+ind/span);
		return(ch);
	}
	public int getRowForIndex(int ind)
	{
		return(1+ind%span);
	}
	public int getIndex(char col, int row) {
		if((col<'A')
			|| (col>='A'+span)
			|| (row<1) 
			|| (row>span)) { return(-1); }
		int c = (col-'A')*span+row-1;
		return(c);
	}

	private int getSetSize() {
		return (span*span);
	}

	public int getNNeighbors() {
		return 6;
	}

	public int getNeighborIndex(char col, int row, int neighbor) 
	{
		switch(neighbor)
		{
		default: throw G.Error("Neighbor %s not defined",neighbor);
		case 0:	
			return(getIndex(col,row+1)); 
		case 1: 
			return(getIndex((char)(col+1),row+1)); 
		case 2:
			return(getIndex((char)(col+1),row));
		case 3:
			return(getIndex(col,row-1));
		case 4:
			return(getIndex((char)(col-1),row-1));

		case 5:
			return(getIndex((char)(col-1),row));

		}
	}

	private void buildNeighborMap()
	{	int nnab = getNNeighbors();
		neighborMap = new int[getSetSize()][nnab];
		for(int coln=0;coln<span;coln++)
		{	char col = (char)('A'+coln);
			for(int row=1;row<=span;row++)
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
