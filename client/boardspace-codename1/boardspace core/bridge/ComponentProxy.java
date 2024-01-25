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

import com.codename1.ui.Graphics;

import lib.NullLayout;
import lib.NullLayoutProtocol;

// 
// this window is the "real" window, paired with a "fake" ProxyWindow which is not connected
// directly to the window system.
//
public class ComponentProxy extends Component implements NullLayoutProtocol
{	ProxyWindow client;
	
	public ComponentProxy(ProxyWindow c) 
	{ 	setLayout(new NullLayout(this));
	    client = c;
	    //for some reason this is a really bad idea, all the codename1 buttons stop working.
	    // setFocusable(true);
	    mouse.setAwtComponent(c); 
	}
	public void doNullLayout()
	{
		client.doNullLayout();
	}
	public boolean pinch(double f,int x,int y) 
	{ 
	  return client.pinch(f,x,y); 
	}

	public void actualPaint(Graphics g) 
	{ super.paint(g);  // callback to continue the original paint
	}
	//
	// This enables subtle drag behavior, inconjunction with the global 
	// Display.getInstance().setDragStartPercentage(1)
	//
	public int getDragRegionStatus(int x, int y) { return(Component.DRAG_REGION_LIKELY_DRAG_XY); }

	public void paint(Graphics g)
	{	
		boolean rotated = MasterForm.rotateNativeCanvas(this,g);
		client.paint(lib.Graphics.create(g));
		if(rotated) { MasterForm.unrotateNativeCanvas(this, g); }
	}

	public void addFocusListener(FocusListener who) {
		mouse.addFocusListener(who);
		
	}
	public void removeFocusListener(FocusListener who) {
		mouse.removeFocusListener(who);	
	}

}
