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

import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

import lib.G;

public class Panel extends Container
{
	public Panel(Layout flowLayout) { super(flowLayout);	}
	public Panel() { super(); }
	public Dimension getMinimumSize() { return(new Dimension(10,10)); }
	public void setBoundsEdt(int l,int t,int w,int h)
	{
		super.setBounds(l,t,w,h);
	}
	public void setBounds(int l,int t,int w,int h)
	{
		G.runInEdt(new Runnable(){ public void run() { setBoundsEdt(l,t,w,h); }});
	}
}
