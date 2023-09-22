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

import bridge.Icon;
import bridge.IconBase;

import java.awt.Component;
import java.awt.FontMetrics;

/**
 * implement the Icon interface to be used in a menu.
 * 
 * @author Ddyer
 *
 */
public class TextIcon extends IconBase implements Icon 
{	
	Text originalText;
	int width;
	int height;
	public TextIcon(Component c,Text o) 
		{ originalText=o;
		  FontMetrics fm = G.getFontMetrics(c);
		  width = originalText.width(fm);
		  height = originalText.height(G.getFontMetrics(c));
		}
	public TextIcon(Text o,int w,int h) 
	{ originalText=o;
	  width = w;
	  height = h;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		int w = getIconWidth();
		int h = getIconHeight();
		originalText.draw(g, true, x,y,w,h,null, null);
	}

	public int getIconWidth() {
		return(width);
	}

	public int getIconHeight() {
		return(height);
	}
}
