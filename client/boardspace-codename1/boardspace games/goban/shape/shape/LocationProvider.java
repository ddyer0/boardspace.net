package goban.shape.shape;
/**
 * LocationProvider is an interface to allow your own data structures to
 * be used to specify elements of the set, rather than just Point as was
 * the case in the original version of the shape library. 
 * @author Ddyer
 *
 */
public interface LocationProvider {
	/**
	 * get the x coordinate of the location
	 * @return an integer
	 */
	public int getX();
	/**
	 * get the y coordinate of the location
	 * @return an integer
	 */
	public int getY();
	/**
	 * return true of e specifies the same location
	 * @param e
	 * @return a boolean
	 */
	public boolean equals(LocationProvider e);
	/**
	 * return true of this location is x,y
	 * @param x
	 * @param y
	 * @return a boolean
	 */
	public boolean equals(int x,int y);
}
