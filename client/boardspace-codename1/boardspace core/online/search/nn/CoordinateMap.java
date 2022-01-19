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
