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
import lib.Image;

public class JButton extends Button 
{	Command command = null;
	public JButton(String label) { super(label); }

	public JButton(Image label) { super(label); }
	
	public JButton(String com,Image image) { super(image); command = new Command(com); }
	
	public Command getCommand() 
	{	if(command!=null) { return(command); } 
		return super.getCommand(); 
	}
	public String toString() { return("<button "+getCommand()+" "+isVisible()+">"); }
	public void setVisible(boolean v)
	{
		boolean change = v!=isVisible();
		super.setVisible(v);
		if(change) 
			{ repaint(); }
	}
}
