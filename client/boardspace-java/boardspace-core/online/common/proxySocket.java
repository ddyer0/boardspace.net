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

import java.net.*;

import lib.ConnectionManager;



public class proxySocket extends Socket 
{	ConnectionManager myNetConn=null;
	proxySocket(ConnectionManager cm)
	{	myNetConn = cm;
		status = status_wait;
		//cm.proxyWait(this);
	}

	public static final int status_wait = 0;
	public static final int status_ok = 1;
	public static final int status_failed = 2;
	public int channel = 0;
	public int status = status_wait;
}
