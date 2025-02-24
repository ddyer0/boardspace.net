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

import lib.G;
import lib.OStack;

/**
 * this class provides a list of the servers that have been seen.
 * normally, we expect there will be only one, but for testing
 * and in unusual circumstances there may be more.
 * 
 * This list of servers is presented as part of the login screen, so
 * the choices are login, play offline, or connect to a local playtable.
 * 
 * @author Ddyer
 *
 */
public class PlaytableStack extends OStack<PlaytableServer>
{	public int state = 0;
	PlaytableServer selectedServer = null;
	static PlaytableStack playtableServers = new PlaytableStack();
	public static PlaytableStack getInstance() { return(playtableServers); }
	public int getState() { return(state); }
	public void setChanged() { state++; }
	
	public PlaytableServer[] newComponentArray(int sz) {
		return(new PlaytableServer[sz]);
	}
	
	private PlaytableServer find(String n,int p)
	{	
		for(int i=size()-1; i>=0; i--) 
			{ PlaytableServer e = elementAt(i);
			  if(n.equals(e.hostIP) && (p==e.hostPort)) { return(e); }}
		return(null);
	}
	public static PlaytableServer getSelectedServer()
	{
		if(playtableServers.size()==1) { return(playtableServers.elementAt(0)); }
		else if(playtableServers.contains(playtableServers.selectedServer)) { return(playtableServers.selectedServer); }
		return(null);
	}
	public static void setSelectedServer(PlaytableServer s) { playtableServers.selectedServer = s; }
	public static void setSelectedServer(int n) 
	{
		playtableServers.selectedServer = playtableServers.elementAt(n);
		playtableServers.setChanged();
	}
	public static void reInit() { playtableServers.clear(); }
	public static int getNServers() { return(playtableServers.size()); }
	public static PlaytableServer getNthServer(int n) 
	{ if((n>=0) && (n<playtableServers.size())) { return(playtableServers.elementAt(n)); }
		else { return(null);}
	}
	public static void removePlaytableServer(String n,int p)
	{
		PlaytableServer old = playtableServers.find(n,p);
		if(old!=null) 
			{ playtableServers.remove(old,false); 
			  playtableServers.setChanged();
			}
	}
	public static void addPlaytableServer(String n,int p,String status,String host)
	{	
		PlaytableServer old = playtableServers.find(n,p);
		if(old!=null)
		{
			// higher numbered protocols are preferred. 5004 is vnc, 5005 is rpc
			old.hostPort = p;
			old.lastContactTime = G.Date();
			old.status = status;
			old.hostName = host;
			playtableServers.setChanged();
			G.print("Found playtable server "+host+"@"+n+":"+p," ",status); 
		}
		else 
		{
		playtableServers.push(new PlaytableServer(n,p,status,host));
		playtableServers.setChanged();
		G.print("New playtable server "+host+"@"+n+":"+p+" "+status); 
		}
	}
}