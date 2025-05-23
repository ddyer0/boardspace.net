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

import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.geom.Dimension;

import lib.G;
/**
 * note here on Tablayout, which is used for the activities and actions bars at the top
 * of boardspace screen.  Getting the dimensions of the icons right and consistent is
 * a nightmare.  The final, for now, disposition is to based the height on the default
 * font size, and the width proportionally scaled to the height, plus a fudge factor.
 * 
 * The containing panel takes its height from the size of this
 * 
 * @author ddyer
 *
 */
public class TabLayout extends com.codename1.ui.layouts.Layout
{	private int spacing = (int)(4*G.getDisplayScale());
	public void layoutContainer(Container parent) {
        int w = parent.getWidth();
        int h = parent.getHeight()-spacing*2;
        int nc = parent.getComponentCount();
		int sum = getFullWidth(parent);
		int squeeze = nc==0 ? 0 : (sum>w) ? (sum-w+nc)/nc : 0;
		int deficit = 0;
		for(int i=0,xpos=spacing;i<nc;i++) 
		{ com.codename1.ui.Component p = parent.getComponentAt(i);
		  p.setX(xpos);
		  p.setY(spacing);
		  p.setHeight(h);
		  int ww2 = prefw(parent,i);
		  int desired = ww2-squeeze+deficit;
		  int actual = Math.max(20, desired);
		  p.setWidth(actual);
		  deficit = desired-actual;
		  xpos += actual+spacing;
		}
	}
	
	int getFullWidth(Container parent)
	{	int nc = parent.getComponentCount();
		int h = parent.getHeight()-spacing*2;
		int sum = h/2;
		for(int i=0;i<nc;i++) 
		{ 
		  sum += prefw(parent,i)+spacing; 
		}
		return(sum);
	}
	// this is an ad-hoc calculation to find the preferred width for an icon
	// assuming it will be scaled to the height of the parent.  The h/6 factor
	// accounts for wider horizontal margins than vertical, not sure where they
	// come from.
	int prefw(Container parent, int i)
	{	int h = parent.getHeight()-spacing*2;
		Dimension dim = parent.getComponentAt(i).getPreferredSize();
		int inc = (int)(h*((double)dim.getWidth()/dim.getHeight())+h/6);
		return inc;
	}
	int getFullHeight(Container parent)
	{	int nc = parent.getComponentCount();
		// use font height as the basic scale metric
		Font f = SystemFont.getGlobalDefaultFont();
		int fs = SystemFont.getFontSize(f);
		int max = (int)(fs*2.2);
		//G.print("Font "+f+" sz ",fs," h ",max);
		for(int i=0;i<nc;i++) { max = Math.max(parent.getComponentAt(i).getPreferredSize().getHeight(),max); }
		return(max);
	}
	public Dimension getPreferredSize(Container parent) {
		
		Dimension dim =new Dimension(getFullWidth(parent),getFullHeight(parent));
		return(dim);
	}

}
