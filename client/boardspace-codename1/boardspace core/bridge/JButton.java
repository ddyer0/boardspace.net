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

import com.codename1.ui.Command;
import com.codename1.ui.Font;
import com.codename1.ui.geom.Dimension;

import bridge.SystemImage.ScaleType;
import lib.FontManager;
import lib.Image;

public class JButton extends Button 
{	Command command = null;
	public JButton(String label) 
	{ super(label); 
	}
	public JButton(String label,int fontsize)
	{
		this(label);
		setFont(FontManager.getFont(getFont(),fontsize));
	}
	public JButton(Image label)
		{ 
		super(label); 
		}
	
	static Image prepareIconImage(Image image,double setsize)
	{	// scale icon images to the desired size, which is intended to be
		// compatible with text images in the same line
		if(setsize>0)
		{
		int w = image.getWidth();
		int h = image.getHeight();
		Font def = SystemFont.getGlobalDefaultFont();
		double sz = SystemFont.getFontSize(def)*setsize;
		int neww = (int)(sz*w/h);
		int newh = (int)sz;
		return image.getScaledInstance(neww,newh,ScaleType.SCALE_SMOOTH);
		}
		return image;
	}
	
	@SuppressWarnings("deprecation")
	public JButton(String com,Image image,double setsize) 
	{ 	super(prepareIconImage(image,setsize)); 
		command = new Command(com); 
	}
	
	public Command getCommand() 
	{	if(command!=null) { return(command); } 
		return super.getCommand(); 
	}
	public String toString() 
	{ 	Command com = getCommand();
	    String lbl = (com==null) ? getLabel() : ""+com; 
	    return("<button "+lbl+" "+isVisible()+">"); 
	}
	public void setHeight(int h)
	{
		super.setHeight(h);
		//G.print("set h ",this," ",h);
	}
	public void setVisible(boolean v)
	{
		boolean change = v!=isVisible();
		super.setVisible(v);
		if(change) 
			{ repaint(); 
			}
	}
	public Dimension getPreferredSize()
	{	Image im = theImage;
		String label = getLabel();
		Font f = getFont();
		Dimension dim = null;
		if(im!=null)
		{	// images prefer to be the size of the font.  This is the magic spot
			// where the toolbar gets its height
			FontMetrics fm = FontManager.getFontMetrics(f);
			int sz = fm.getHeight()*3/2;
			int w = im.getWidth();
			int h = im.getHeight();
			dim = new Dimension((int)(sz*w/h),sz);
		}
		else if(label!=null)
		{	// use the actual font metrics to specify the size
			FontMetrics fm = FontManager.getFontMetrics(f);
			int w = fm.stringWidth(label);
			int h = fm.getHeight()*3/2;
			dim = new Dimension(w+h,h);
		}
		else
		{
		 dim = super.getPreferredSize();
		}
		//G.print("\nbutton pref ",this," ",dim," ",getUIID()," ",getStyle().getVerticalMargins());
		//G.print("Pref ",this,dim);
		return dim;
	}
	/*
	public Dimension calcPreferredSize()
	{
		Dimension s = super.calcPreferredSize();
		return s;
	}
	public void setHeight(int h)
	{	super.setHeight(h);
	}
	*/
}
