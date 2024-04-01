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
 * a text-based button that has standard button behavior and appearance.
 * This class extends Rectangle, so it can be positioned by the standard layout methods
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
public class TextButton extends ToggleButton 
{	double rotation=0;
	Color highlightColor = Color.gray;
	public Color backgroundColor = Color.white;
	Color inactiveColor = Color.white;
	public Color textColor = Color.black;
	public Color frameColor = Color.black;
	public boolean square;
	Text onText = TextChunk.create("button");
	Text offText = TextChunk.create("off");
	public void setRotation(double r) { rotation=r; }

	/* constructor */
	public TextButton(String label,CellId code,String help,Color highlight,Color background,Color inactive)
	{	this(label,code,help,label,code,help,highlight,background,inactive);
	}

	/* constructor */
	public TextButton(String onLabel,CellId onCode,String onHelp,String offLabel,CellId offCode,String offHelp,
			Color highlight,Color background,Color inactive)
	{	onText = TextChunk.create(onLabel);
		offText = TextChunk.create(offLabel);
		if(onHelp!=null) { onToolTip = TextChunk.create(onHelp); }
		if(offHelp!=null) { offToolTip = TextChunk.create(offHelp); }
		
		onId = onCode;
		offId = offCode;
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
		Text msg = isOn ? onText : offText;
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
			highlight.hitCode = isOn ? onId : offId;
			Text tip = isOn ? onToolTip : offToolTip;
			if(tip!=null) { highlight.setHelpText(tip); }
			return(true);
		}
		return(false);
	}

	public boolean actualDraw(Graphics gc, HitPoint hp) {
		return show(gc,this,0,hp);
	}

}
