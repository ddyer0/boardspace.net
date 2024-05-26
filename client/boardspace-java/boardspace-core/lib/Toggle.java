/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package lib;

import java.awt.Color;
import java.awt.Rectangle;

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
	public Color foregroundColor = Color.black;
	public Color backgroundColor = Color.white;
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
   		int w = G.Width(this);
   		int h = G.Height(this);
   		if(w>h*3)
   		{	
   		int left = G.Left(this);
   		int top = G.Top(this);
   		boolean hit = ic.drawChip(gc,canvas,new Rectangle(left,top,h,h),hp,id,tip);
   		GC.Text(gc,false,left+h,top,w-h,h,foregroundColor,backgroundColor,name);
   		return hit;
   		}
   		else
   		{
   		return ic.drawChip(gc,canvas,this,hp,id,tip);
   		}
   		
   	}
}
