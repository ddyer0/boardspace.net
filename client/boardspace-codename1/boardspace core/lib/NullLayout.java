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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

import bridge.LayoutManager;

public class NullLayout extends Layout implements LayoutManager
{	
	NullLayoutProtocol expectedParent;
	public NullLayout(NullLayoutProtocol parent) 
	{ expectedParent = parent; 
	}
    /* Required by LayoutManager. */
    public void addLayoutComponent(String name, Component comp)    {  }

    /* Required by LayoutManager. */
    public void removeLayoutComponent(Component comp)    {  }

    /* Required by LayoutManager. */
    public Dimension preferredLayoutSize(Container parent) {
    Dimension dim = new Dimension(parent.getWidth(), parent.getHeight());
    return dim;
    }

/* Required by LayoutManager. */
public Dimension minimumLayoutSize(Container parent) {
    Dimension dim = new Dimension(1, 1);
    return dim;
}

    public void layoutContainer( Container parent) 
    {	
     	expectedParent.doNullLayout();
    }
	public Dimension getPreferredSize(Container parent) { return(new Dimension(parent.getWidth(),parent.getHeight())); }


}