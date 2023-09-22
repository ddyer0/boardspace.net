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

import lib.AwtComponent;
import lib.GC;
import lib.Graphics;
import lib.StockArt;

import com.codename1.ui.Font;

public class JCheckBoxMenuItem extends JMenuItem implements Icon
{
	boolean isSelected = false;
	public JCheckBoxMenuItem(String text) {
		super(text);
	}	
	public JCheckBoxMenuItem(String text,boolean value)
	{	super(text);
		isSelected = value;
	}
	public String toString() { return(text);}
	public boolean isSelected() { return(isSelected); }
	public boolean getState() { return(isSelected); }
	public void setSelected(boolean initial) { isSelected = initial; }
	public void setState(boolean initial) { isSelected = initial; }
	
	public void actionPerformed(ActionEvent v)
	{
		isSelected = !isSelected;
		super.actionPerformed(v);
	}

	public void paintIcon(AwtComponent c, Graphics g, int x, int y) {
		int h = getIconHeight();
		int w = getIconWidth();
		String txt = getText();
		StockArt im = isSelected() ? StockArt.FilledCheckbox : StockArt.EmptyCheckbox;
		GC.fillRect(g,c.getBackground(),x,y,w,h);
		double sc[] = im.getScale();
		int sh = (int)(sc[2]*h);
		int xoff = (int)((sc[0]-0.5)*sh);
		int yoff = (int)((sc[1]-0.5)*sh);
		im.getImage().drawImage(g,xoff,yoff,sh,sh);
		GC.setColor(g,getForeground());
		Font f = c.getFont();
		FontMetrics fm = FontMetrics.getFontMetrics(f);
		GC.setFont(g,f);
		GC.Text(g,txt,x+h,y+fm.getAscent());
		
	}
	public int getIconWidth() {
		String txt = getText();
		FontMetrics fm = FontMetrics.getFontMetrics(getFont());
		return(fm.getHeight()*4+fm.stringWidth(txt));
	}
	public int getIconHeight() {
		FontMetrics fm = FontMetrics.getFontMetrics(getFont());
		return(fm.getHeight());
	}

}
