package lib;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * this is a simple widget is an extension of Rectangle that presents a changeable horizontal bar.
 * This us typically used for progress bars and size controls.
 * @author ddyer
 *
 */
public class Slider extends Rectangle
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
/** the label string */
	public String label="slider";
	/** the current value */
	public double value=0.5;
	/** the minimum value */
	public double min=0.0;
	/** the maximum value */
	public double max=1.0;
	/** the hit code if this item is under the mouse */
	public CellId hitcode;
	/** the color for text */
	public Color textColor = Color.black;
	/** the color for the bar */
	public Color barColor = Color.red;
	/** the color for highlighting when under the mouse */
	public Color highlightColor = Color.green;
	/** the tooltip for this slider */
	public String helpText = null;
	public void setHelpText(String s) { helpText = s; }
	/** the default constructor */
	public Slider(String lab,CellId code)
	{	label = lab;
		hitcode = code;
	}
	public Slider(String lab,CellId code,double minval,double maxval,double curval)
	{
		label = lab;
		hitcode = code;
		min = minval;
		max = maxval;
		value = curval;
	}
	/** set the current value */
	public double setValue(double v)
	{	value = Math.max(min,Math.min(max, v));
		return(value);
	}
	/** set the current value proportional to the hit location */
	public double setValue(HitPoint pt)
	{	boolean hit = G.pointInRect(pt,this);
		if(hit)
		{	value = setValue(min + ((double)(pt.getX()-G.Left(this))/G.Width(this))*(max-min));
		}
		return(value);
	}
	public void draw(Graphics gc,HitPoint pt)
	{
		draw(gc,pt,null);
	}
	/** draw the slider, and record the hit if we're under the mouse */
    public void draw(Graphics gc,HitPoint pt,InternationalStrings s)
    { boolean hit = G.pointInRect(pt,this);
      int width = G.Width(this);
      int height = G.Height(this);
      int x = G.Left(this);
      int y = G.Top(this);
	  if(hit) 
	        {	pt.hitCode = hitcode;
	        	if(pt.down) 
	        	{ pt.dragging=true; 
	        	  setValue(pt);
	        	  }
	        	pt.setHelpText(helpText);
	        }
      if(gc!=null)
    	{String l = label;
    	 if(s!=null) { l = s.get(l); }
    	 if(l.charAt(l.length()-1)=='=')  
    	 	{ l = l+G.format(" %4d",value);
    	 	}
    	GC.Text(gc,true,x,y,width,height/2,
    		textColor,null,l);
    	GC.setColor(gc,barColor);
    	int limit = (int)((value-min)*width/(max-min));
    	int yoff = (2*height)/3;
    	int ysize = height/3;
    	gc.fillRect(x,y+yoff,limit,ysize);
    	if(hit)
    	{
       	int barlimit = G.Left(pt) - x;
       	GC.setColor(gc,highlightColor);
       	GC.fillRect(gc,x,y+yoff+1,barlimit,ysize-3);
    	}
    	GC.frameRect(gc,Color.black,this);
    	}
    }
    /** adjust the min and max values to center the current value */
    public void centerValue()
    {
    	double range = (max-min)/2;
    	min = value-range;
    	max = value+range;
    }
}