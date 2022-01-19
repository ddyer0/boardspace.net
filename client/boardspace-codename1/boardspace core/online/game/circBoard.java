package online.game;

/**
 * Base class for boards with radial symmetry.  Rows are mapped around, Columns as radii.
 * Other than the spatial transform, the board representation is just a square board.
 * 
 * @author ddyer
 *
 */
public abstract class circBoard<CELLTYPE extends cell<CELLTYPE>> extends rectBoard<CELLTYPE> 
{	/**
the inner radius of the board
*/
	double inner_radius = 2.0;
	/**
	 * the outer radius of the board.
	 */
	double outer_radius = 3.0;
	/**
	 * set the radii of the board
	 * @param fr the inner radius
	 * @param to the outer radius
	 */
	public void SetDisplayRadius(double fr,double to)
	{	inner_radius = fr;
		outer_radius = to;
	}
	/**
	 *  convert col,row to y with no rotation or perspective
	 *  columns are interpolated from inner to outer radius, rows
	 *  are interpolated around the board clockwise.
	 * @param colchar the column
	 * @param thisrow the row
	 * @param xoff
	 * @param yoff
	 * @return a double
	 */
    public double cellToY00(char colchar, int thisrow,double xoff,double yoff)
    {	double rad = -2*Math.PI*((double)thisrow/nrows+xoff/displayParameters.CELLSIZE);
    	double sin = Math.sin(rad);
    	double d = displayParameters.YCELLSIZE*((yoff/displayParameters.YCELLSIZE)+((outer_radius-inner_radius)*(colchar-'A'))/ncols+inner_radius);
    	return(d*sin);
    }
	/**
	 *  convert col,row to y with no rotation or perspective
	 *  columns are interpolated from inner to outer radius, rows
	 *  are interpolated around the board clockwise.
	 * @param colchar the column
	 * @param thisrow the row
	 * @return a double
	 */
    public double cellToY00(char colchar, int thisrow)
    {	return(cellToY00(colchar,thisrow,0,0));
    }
	/**
	 *  convert col,row to y with no rotation or perspective
	 *  columns are interpolated from inner to outer radius, rows
	 *  are interpolated around the board clockwise.
	 * @param colchar the column
	 * @param thisrow the row
	 * @param xoff
	 * @param yoff
	 * @return a double
	 */
    public double cellToX00(char colchar, int thisrow,double xoff,double yoff)
    {	double rad = 2*Math.PI*((double)thisrow/nrows+xoff/displayParameters.CELLSIZE);
		double cos = Math.cos(rad);
		double d = displayParameters.YCELLSIZE*((yoff/displayParameters.YCELLSIZE)+((outer_radius-inner_radius)*(colchar-'A'))/ncols+inner_radius);
		return(d*cos);
    }
	/**
	 *  convert col,row to y with no rotation or perspective
	 *  columns are interpolated from inner to outer radius, rows
	 *  are interpolated around the board clockwise.
	 * @param colchar the column
	 * @param thisrow the row
	 * @return an integer
	 */
    public double cellToX00(char colchar, int thisrow)
    {
    	return(cellToX00(colchar,thisrow,0.0,0.0));	
    }
    /**
     * rotate an x,y by the rotation of the board
     * @param x00
     * @param y00
     * @return a double
     */
    public double rotateX(double x00, double y00)
    {	return displayParameters.rotateX(x00,y00);
    }


    /**
     * rotate an x,y by the rotation of the board
     * @param x00
     * @param y00
     * @return a double
     */
   public double rotateY(double x00, double y00)
    {	return displayParameters.rotateY(x00,y00);
    }
    
	/**
	 *  convert col,row to x with rotation
	 *  columns are interpolated from inner to outer radius, rows
	 *  are interpolated around the board clockwise.
	 * @param colchar the column
	 * @param row the row
	 * @param offx
	 * @param offy
	 * @return a double
	 */
   public double cellToX0(char colchar, int row, double offx, double offy)
    {
        double col0 = cellToX00(colchar, row,  offx,offy);
        if (displayParameters.rotation != 0)
        {
            return (rotateX(col0, cellToY00(colchar, row, offx, offy)));
        }

        return (col0);
        
    }
	/**
	 *  convert col,row to y with rotation 
	 *  columns are interpolated from inner to outer radius, rows
	 *  are interpolated around the board clockwise.
	 * @param colchar the column
	 * @param thisrow the row
	 * @param offx
	 * @param offy
	 * @return a double
	 */
    public double cellToY0(char colchar, int thisrow, double offx, double offy)
    {   double y0 = cellToY00(colchar, thisrow, offx,offy);
        if (displayParameters.rotation != 0)
        {
            return (rotateY(cellToX00(colchar, thisrow, offx,offy), y0));
        }
       return (y0); 
    }  
   
}
