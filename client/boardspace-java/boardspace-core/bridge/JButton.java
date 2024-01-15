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

import lib.Image;

@SuppressWarnings("serial")
public class JButton extends javax.swing.JButton 
{	String commandString = null;
	Image image = null;
	public JButton(String label) { super(label); commandString=label; }

	public JButton(Image label) { super(label); image = label;  }
	
	public JButton(String com,Image image)
		{ super(null,image);
		  commandString = com; 
		  setActionCommand(com);
		}

	public String getCommandString() 
	{	if(commandString!=null) { return(commandString); } 
		return "unknown"; 
	}
	public String toString() { return("<button "+commandString+" "+isVisible()+">"); }
	public void setVisible(boolean v)
	{
		boolean change = v!=isVisible();
		super.setVisible(v);
		if(change) 
			{ repaint(); }
	}

}

