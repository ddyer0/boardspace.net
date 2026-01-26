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

import com.codename1.ui.Font;
import lib.FontManager;
import lib.G;

public class JTextField extends JTextComponent 
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
	
	public JTextField(int i) {
		super(i);
		setDoneListener(mouse);
	}

	public JTextField(String string) {
		super(string);
		setDoneListener(mouse);
	}

	public JTextField(String string, int i) 
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
	public Font getFont() { return(FontManager.getFont(getStyle())); }
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

	//
	// jan 2026, switch to this instead of deferring the settext to paint()
	// shai advises settext in paint is a bad idea because it triggers reconfiguration
	//
	private void setTextEdt(String g)
	{
		super.setText(g);
	}
	public void setText(String s)
	{
		G.runInEdt(new Runnable() { public void run(){ setTextEdt(s);  }});	
	}
	public FontMetrics getFontMetrics(Font f) {
		return FontManager.getFontMetrics(f);
	}
}
