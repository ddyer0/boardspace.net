package lib;

import online.common.exCanvas;
/**
 * a image-based button that has standard button behavior and appearance.
 * This class extends Rectangle, so it can be positioned by the standard layout methods
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
public class Toggle extends ToggleButton
{
	private DrawableImage<?> onIcon;
	private DrawableImage<?> offIcon;
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
		onToolTip = offToolTip = TextChunk.create(tip);
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
		onToolTip = TextChunk.create(tip);
		
		offIcon = offIc;
		offId = idx2;
		offToolTip = TextChunk.create(tip2);
	}

   	public boolean actualDraw(Graphics gc,HitPoint hp)
   	{	// the behavior of the hints box is choreographed so mouse-over
   		// turns on the hints while the mouse is present, click toggles
   		// it on and off
   		DrawableImage<?> ic = isOn ? onIcon : offIcon;
   		Text tip = isOn ? onToolTip : offToolTip;
   		CellId id = isOn ? onId : offId;
   		return ic.drawChip(gc,canvas,this,hp,id,tip);
   		
   	}
}
