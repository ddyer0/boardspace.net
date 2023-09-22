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
package bridge;

import java.awt.event.MouseEvent;

import lib.G;

public class XJMenu extends JMenu {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public XJMenu(String title,boolean noAutoPop) { super(title); inhibitpopup = noAutoPop; } 
	public boolean inhibitpopup = false;
	protected void processMouseEvent(MouseEvent e) 
	{
		try {
			int id = e.getID();
			switch(id)
            {
            case MouseEvent.MOUSE_ENTERED:	
            	// inhibit mouse entered to avoid auto-selection moving
            	// between items on the jmenubar
            	if(inhibitpopup) { break; }
            default: super.processMouseEvent(e);
            }
        }
		catch (ArrayIndexOutOfBoundsException err) 
		{ G.print("error in java menu "+err);
		}
	}
	
}
