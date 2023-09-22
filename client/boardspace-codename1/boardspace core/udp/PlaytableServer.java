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
package udp;

import bridge.Config;
import lib.G;
import lib.StringStack;


/**
 * info we have about a server that announced on the UDP hello service
 * 
 * @author Ddyer
 *
 */
public class PlaytableServer {
	public String hostIP;
	public int hostPort;
	public String hostName;
	public String status;
	public long lastContactTime = 0;
	StringStack services = new StringStack();
	PlaytableServer(String ip,int p,String s,String name) 
	{ 	hostIP = ip; 
		hostName = name;
		hostPort = p;
		status = s;
		lastContactTime = G.Date();
	}
	public String toString() 
	{
		return("<"+prettyName()+">");
	}
	public String prettyName()
	{
		String host = getHostName();
		if(host==null) { host=""; }
		if(host.length()<=1) { host += "@"+getHostIp()+":"+getPort(); }
		return(host);
	}
	public String getHostIp() { return(hostIP); }
	public String getHostName() { return(hostName); }
	public int getPort() { return(hostPort); }
	public boolean isRpc() { return(hostPort==Config.RpcPort); }
}
