
package goban.shape.shape;



/**
 * shape protocol hides the actual implementation of a shape from the library
 * @author Ddyer
 *
 */
public interface ShapeProtocol extends Globals
{ 	/**
	some shapes have well-known names, ie; "bent four"
 */
	public String getName();
	/**
	 * get the points in this shape
	 * @return
	 */
	public LocationProvider[] getPoints();
	/**
	 * get the bounding box width for this shape
	 * @return
	 */
	public int Width();
	/**
	 * get the bounding box height for this shape 
	 */
	public int Height();
	/**
	 * get the hashcode for this shape (for comparison to other shapes)
	 * @return
	 */
	public int hashCode();
	/**
	 * true of this shape contains a point x,y
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean ContainsPoint(int x,int y);
	
	public int getIntronBit(int x,int y);
	public boolean equal(ElementProvider v,int x,int y);
	public boolean equal(LocationProvider v[],int x,int y);
	public boolean equal(ElementProvider v2);
	public ResultProtocol Fate_Of_Shape(Move_Order move_spec,X_Position x_pos,Y_Position y_pos,int introns);
	public String Ascii_Picture();
	public SingleResult Exact_Fate_Of_Shape( Move_Order move_spec, X_Position x_pos,Y_Position y_pos,int introns,int libs);
	public LocationProvider Position_To_Play(SingleResult r);
	public LocationProvider Position_To_Play(SingleResult r,SingleResult other);
}
