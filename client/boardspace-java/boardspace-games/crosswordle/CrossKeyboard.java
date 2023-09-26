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
package crosswordle;

import java.awt.Rectangle;

import lib.CalculatorButton;
import lib.Graphics;
import lib.HitPoint;
import lib.Keyboard;
import lib.KeyboardLayout;
import lib.TextContainer;

public class CrossKeyboard extends Keyboard {
	private CrosswordleViewer viewer = null;
	public CrossKeyboard(CrosswordleViewer see, TextContainer dis, Rectangle r, KeyboardLayout lay) 
	{	super(see, dis, r, lay);
		viewer = see;
	}
	public void drawButton(Graphics gc,CalculatorButton button,HitPoint highlight,Rectangle cr)
	{	button.textColor = viewer.keyboardTextColor(button);
		super.drawButton(gc,button,highlight,cr);
	}
	
}
