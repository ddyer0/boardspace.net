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

import java.awt.Rectangle;
/**
 * a image-based button that has standard button behavior and appearance.
 * This class extends Rectangle, so it can be positioned by the standard layout methods
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
abstract public class ToggleButton extends Rectangle
{	String name = "";
	boolean isOn = false;
	boolean justTurnedOff = false;
	boolean justTurnedOn = false;
	boolean mouseOverNow = false;
	boolean temporarilyOff = false;
	boolean temporarilyOn = false;
	
	public boolean activateOnMouse = false;
	public boolean deactivateOnMouse = false;
	
	Text onToolTip = null;
	Text offToolTip = null;
	CellId onId;
	CellId offId;
	exCanvas canvas;

	public abstract boolean actualDraw(Graphics gc,HitPoint hp);
	
	public boolean isOnNow() 
		{ return(!temporarilyOff 
				&& !temporarilyOn 
				&& (mouseOverNow ? !isOn : isOn)); }
	
	public void setTemporarilyOff(boolean v) { temporarilyOff = v;}
   	public boolean draw(Graphics gc,HitPoint hp)
   	{	// the behavior of the hints box is choreographed so mouse-over
   		// turns on the hints while the mouse is present, click toggles
   		// it on and off
   		if(G.Height(this)>0)
   		{
   		if(actualDraw(gc,hp))
   		{
   			if(!isOn && !justTurnedOff && activateOnMouse)
   			{ mouseOverNow = true; 
   			}
   			if(isOn && !justTurnedOn && deactivateOnMouse)
   			{
   				mouseOverNow = true; 
   			}
   			return true;
   		}
   		else { justTurnedOn = justTurnedOff=false; mouseOverNow = false; }
   		}
   		return false;
   	}
   	public boolean isOn() { return isOn; }
   	public void setValue(boolean v) { isOn = v; }
   	public void toggle()
   	{
    	isOn = !isOn;
    	if(!isOn) { justTurnedOff = true; mouseOverNow = false; }
    	else { justTurnedOn = true; mouseOverNow = false; }
   	}
}
