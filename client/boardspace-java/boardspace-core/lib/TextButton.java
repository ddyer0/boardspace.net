package lib;

import java.awt.Color;
import java.awt.Rectangle;
/**
 * a text-based button that has standard button behavior and appearance.
 * This class extends Rectangle, so it can be positioned by the standard layout methods
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
public class TextButton extends Rectangle
{	double rotation=0;
	Color highlightColor = Color.gray;
	Color backgroundColor = Color.white;
	Color inactiveColor = Color.white;
	Color textColor = Color.black;
	Color frameColor = Color.black;
	CellId hitcode;
	boolean square;
	Text msg = TextChunk.create("button");
	Text helpText=null;
	public void setRotation(double r) { rotation=r; }

	/* constructor */
	public TextButton(String label,CellId code,String help,Color highlight,Color background)
	{
		this(label,code,help,highlight,background,background);
	}
	public TextButton(String label,CellId code,String help,Color highlight,Color background,Color inactive)
	{	msg = TextChunk.create(label);
		if(help!=null) { helpText = TextChunk.create(help); }
		hitcode = code;
		highlightColor = highlight;
		backgroundColor = background;
		inactiveColor = inactive;
	}
	/**
	 * show this button
	 * 
	 * @param gc			the current graphics
	 * @param highlight		the current mouse position
	 * @return				true if the mouse is in the button
	 */
	public boolean show(Graphics gc,HitPoint highlight)
	{	return(show(gc,rotation,highlight));
	}
	/**
	 * show this button, rotated
	 * 
	 * @param gc			the current graphics
	 * @param rot			the desired rotation (radians)
	 * @param highlight		the current mouse position
	 * @return				true if the mouse is in the button
	 */
	public boolean show(Graphics gc,double rot,HitPoint highlight)
	{	return show(gc,this,rot,highlight);
	}
	
	// 
	/**
	 * show this button with a different rectangle, but otherwise the same as just show.
	 * 
	 * @param gc	the current graphics
	 * @param r		the rectangle
	 * @param rot	rotation (radians)
	 * @param highlight	the current mouse position
	 * @return	true if the mouse is in this rectangle
	 */
	public boolean show(Graphics gc,Rectangle r,double rot,HitPoint highlight)
	{
		boolean hit = false;
		if(square)
		{
			hit = GC.handleSquareButton(gc,rot, r,highlight,msg, textColor, 
						frameColor ,highlightColor,
						highlight==null ? inactiveColor : backgroundColor);
		}
		else
		{
			hit = GC.handleRoundButton(gc,rot, r, highlight,
						msg, textColor, 
						frameColor, highlightColor, 
						highlight==null ? inactiveColor : backgroundColor);
		}
		if(hit)
		{
			highlight.hitCode = hitcode;
			if(helpText!=null) { highlight.setHelpText(helpText); }
			return(true);
		}
		return(false);
	}

}
