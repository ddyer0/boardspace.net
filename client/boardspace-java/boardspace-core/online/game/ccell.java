package online.game;
/**
 * some legacy or simple games use just a "char" to indicate the 
 * contents of the cell.  Most modern games use a "chip" subclass.
 * <p>
 * It's recommended to subclass {@link chipCell} or {@link stackCell} rather
 * than this class.
 * @author ddyer
 * 
 *
 */

public abstract class ccell<FINALTYPE extends ccell<FINALTYPE>> extends cell<FINALTYPE>
{	
	/**
	 * the contents of the cell
	 */
	public char contents;		// for simple games, this is all that's needed
	/**
	 * create a cell on the board with indicated geometry
	 * @param geo
	 * @param mcol
	 * @param mrow
	 */
	public ccell(Geometry geo,char mcol,int mrow) 
		{ super(geo,mcol,mrow); 
		}
	/**
	 * copy the contents of this cell from the other.  This method is normally
	 * wrapped by each subclass to encapsulate the copy process.
	 */
	public void copyAllFrom(FINALTYPE other)
	{	super.copyAllFrom(other);
		copyFrom(other);
	}
	public void copyFrom(FINALTYPE other)
	{	super.copyFrom(other);
		contents = other.contents;		
	}
	public boolean sameContents(FINALTYPE other)
	{ return(contents==other.contents); 
	}
	
	/**
	 * describe the contents of the cell
	 * @return a short string representing the contents of the cell
	 */
	public String contentsString() { return(""+contents); }
	/**
	 * retrieve the contents of the cell
	 * @return the actual contents char of the cell
	 */
    public char getContents() { return(contents); }
    /** 
     * set the contents of the cell
     * @param newc
     * @return the original contents
     */
    public char setContents(char newc) { char old = contents; contents=newc; return(old); }
   
}
