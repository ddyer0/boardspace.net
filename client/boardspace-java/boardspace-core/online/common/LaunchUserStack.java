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
package online.common;

import lib.OStack;

public class LaunchUserStack extends OStack<LaunchUser>
{	public LaunchUser[] newComponentArray(int sz) { return(new LaunchUser[sz]);	}
	public LaunchUser addUser(User u,int seat,int order)
	{
		LaunchUser lu = new LaunchUser();
		lu.user = u==null ? new User("Player "+(seat+1)) : u;
		lu.seat = seat;
		lu.order = order;
		push(lu);
		return lu;
	}
}