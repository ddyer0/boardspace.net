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

import java.awt.Component;
import java.awt.event.MouseEvent;

public class PinchEvent extends MouseEvent
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1;
	public double amount;
	public double getAmount() { return(amount); }
	public double twist;
	public double getTwist() { return(twist); }
	
	public PinchEvent(Component o,int xx,int yy,int button)
	{
		super(o, 0, G.Date(), 3,   xx, yy, button, false);
	}
	
	public PinchEvent(Component o,double am,int xx,int yy,double tw) 
	{	
		super(o, 0, G.Date(), 3,   xx, yy, 1, false);
		//(Component source, int id, long when, int modifiers,
        //        int x, int y, int clickCount, boolean popupTrigger)
		amount = am;
		twist = tw;
	}
}