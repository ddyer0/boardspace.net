package lib;

import com.codename1.ui.geom.Rectangle;

import online.common.exCanvas;
/**
 * a image-based button that has standard button behavior and appearance.
 * This class extends Rectangle, so it can be positioned by the standard layout methods
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
public class Toggle extends Rectangle
{	String name = "";
	private DrawableImage<?> onIcon;
	private DrawableImage<?> offIcon;
	boolean isOn = false;
	boolean justTurnedOff = false;
	boolean mouseOverNow = false;
	boolean temporarilyOff = false;
	public boolean activateOnMouse = false;
	String onToolTip = null;
	String offToolTip = null;
	CellId onId;
	CellId offId;
	exCanvas canvas;
	/**
	 * create a toggle represented by an icon that can be mouse active
	 * @param can
	 * @param name
	 * @param ic
	 * @param idx
	 * @param active
	 */
	public Toggle(exCanvas can,String na,DrawableImage<?> ic,CellId idx,boolean active,String tip)
	{	canvas = can;
		name = na;
		offIcon = onIcon = ic;
		onId = offId = idx;
		activateOnMouse = active;
		onToolTip = offToolTip = tip;
	}
	/**
	 * create a toggle with "off" and "on" states
	 * @param can
	 * @param name
	 * @param ic
	 * @param offIc
	 * @param idx
	 */
	public Toggle(exCanvas can,String na,
					DrawableImage<?> ic,CellId idx,String tip,
					DrawableImage<?> offIc,CellId idx2,String tip2)
	{	canvas = can;
		name = na;
		onIcon = ic;
		onId = idx;
		onToolTip = tip;
		
		offIcon = offIc;
		offId = idx2;
		offToolTip = tip2;
	}
	public boolean isOnNow() { return(!temporarilyOff && (isOn||mouseOverNow)); }
	public void setTemporarilyOff(boolean v) { temporarilyOff = v;}
   	public void draw(Graphics gc,HitPoint hp)
   	{	// the behavior of the hints box is choreographed so mouse-over
   		// turns on the hints while the mouse is present, click toggles
   		// it on and off
   		if(G.Height(this)>0)
   		{
   		DrawableImage<?> ic = isOn ? onIcon : offIcon;
   		String tip = isOn ? onToolTip : offToolTip;
   		CellId id = isOn ? onId : offId;
   		if(ic.drawChip(gc,canvas,this,hp,id,tip))
   		{
   			if(!isOn && !justTurnedOff && activateOnMouse)
   			{ mouseOverNow = true; 
   			}
   		}
   		else { justTurnedOff=false; mouseOverNow = false; }
   		}
   	}
   	public void setValue(boolean v) { isOn = v; }
   	public void toggle()
   	{
    	isOn = !isOn;
    	if(!isOn) { justTurnedOff = true; mouseOverNow = false; }
   	}
}
