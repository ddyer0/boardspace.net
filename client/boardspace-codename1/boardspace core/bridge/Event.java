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

public class Event {
	Object source = null;
	int x;
	int y;
	public Event() { };
	public Event(Object ob,int ax,int ay) 
		{ source = ob; x=ax; y=ay; 
		};
	public int getX() { return(x); }
	public int getY() { return(y); }
	public Object getSource() { return(source); }
	public String paramString() { return(""+this); }

}
