package goban.shape.shape;

/**
 * ElementProvider is an interface to provide a customizable replacement to 
 * allow your actual data structures to be used to specify a set of points,
 * instead of only Vector as was the case in the original shape library. 
 * @author Ddyer
 *
 */
public interface ElementProvider {
	/**
	 * retrieve a specified element of the set
	 * @param n
	 * @return
	 */
	public LocationProvider elementAt(int n);
	/**
	 * return true if the set contains an element at location x,y
	 * @param x
	 * @param y
	 * @return a LocationProvider
	 */
	public LocationProvider containsPoint(int x,int y);
	/**
	 * return true of the set contains location p
	 * @param p
	 * @return a LocationProvider
	 */
	public LocationProvider containsPoint(LocationProvider p);
	/**
	 * return the size of the set.
	 * @return
	 */
	public int size(); 
}
