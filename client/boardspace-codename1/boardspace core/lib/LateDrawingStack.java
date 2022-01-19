package lib;

/**
 * a stack of late drawing objects
 * @author Ddyer
 *
 */
public class LateDrawingStack extends OStack<LateDrawing>
{
	public LateDrawing[] newComponentArray(int sz) {
		return(new LateDrawing[sz]);
	}
	/**
	 * draw and remove the elements
	 */
	public void draw(Graphics g)
	{
		for(int i=0,lim=size();i<lim;i++) { elementAt(i).draw(g); }
		clear();
	}
}