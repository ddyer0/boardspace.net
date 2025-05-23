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

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;

public class TextField extends com.codename1.ui.TextField implements AwtComponent
{
	MouseAdapter mouse = new MouseAdapter(this);
	public void addMouseListener(MouseListener m) 
		{ mouse.addMouseListener(m); }
	public void addMouseMotionListener(MouseMotionListener m) 
		{ mouse.addMouseMotionListener(m); }
	public void addActionListener(ActionListener m) 
		{ mouse.addActionListener(m); }

	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }
	
	public TextField(int i) {
		super(i);
		setDoneListener(mouse);
	}

	public TextField(String string) {
		super(string);
		setDoneListener(mouse);
	}

	public TextField() { 
		super(); 
		setDoneListener(mouse);
	}
	public TextField(String string, int i) 
	{ super(string,i); 
	  setDoneListener(mouse);
	}
	public void keyPressed(int keycode)
	{	
		if(keycode==-90) // where does this number come from?
		{ 
		  fireDoneEvent(); 
		} 
	}

	public void setBackground(Color color) { getStyle().setBgColor(color.getRGB()); }
	public void setForeground(Color color) { getStyle().setFgColor(color.getRGB()); }
	public Font getFont() { return(SystemFont.getFont(getStyle())); }
	public void setFont(Font f) { getStyle().setFont(f); getSelectedStyle().setFont(f); }

	private String actionCommand = "Done";
	public void setActionCommand(String oK) {
		//G.print("setaction "+this+" ("+oK+")");
		actionCommand = oK;
	}
	public String getCommand() { return(actionCommand); }

	public void setBounds(int i, int top, int j, int textHeight) {
		setX(i); 
		setY(top);
		setWidth(j);
		setHeight(textHeight);
	}
	public void requestFocusInWindow() {
	}

	public void repaint() 
	{ 	if(MasterForm.isInFront(this))
		{
		  super.repaint();
		} 
	}
	private String pendingText = null;
	public String getText() 
	{ if(pendingText!=null) 
		{ return(pendingText); } 
		else { return(super.getText()); }
	}
	public void setText(String s)
	{
		pendingText = (s==null)?"":s;
		repaint();
	}
	public void paint(Graphics g)
	{	{ String p = pendingText; if(p!=null) { pendingText=null; super.setText(p); }} 
		if(MasterForm.isInFront(this))
		{	g.setFont(getFont());
			super.paint(g);
		}
	}
	public FontMetrics getFontMetrics(Font f) {
		return lib.Font.getFontMetrics(f);
	}
}
