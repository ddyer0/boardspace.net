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

public class SimpleUser implements CompareTo<SimpleUser>
{
	int channel; String name;
	public SimpleUser(int id,String n) { channel=id; name=n; }
	public int hashCode() { return(channel); }
	public boolean equals(Object x)
		{ if(x instanceof SimpleUser)
			{ SimpleUser xs=(SimpleUser)x;
			  return(xs.channel==channel);
			}
			return(false);
		}
	public String toString()  { return("("+name+")@"+channel); }
	public int compareTo(SimpleUser o) 
		{
		  return(name.compareToIgnoreCase(o.name));
		}
	public String name() { return(name); }
	public void setName(String n) { name = n; }
	public int channel() { return(channel); }
}
