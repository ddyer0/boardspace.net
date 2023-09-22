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
package vnc;

import bridge.Config;
import lib.G;
import lib.OStack;
import lib.SimpleObservable;
import lib.SimpleObserver;
import udp.UDPService;
import vnc.VNCConstants.Operation;


class VNCServiceStack extends OStack<VncServiceProvider>
{
	public VncServiceProvider[] newComponentArray(int sz) { return(new VncServiceProvider[sz]); }
}


public class VNCService implements Config
{
	static VNCServiceStack services = new VNCServiceStack();
	static SimpleObservable observers = new SimpleObservable();
	public static void notifyObservers(Operation op)
	{	observers.setChanged(op,services);
	}
	public static void addObserver(SimpleObserver o) { observers.addObserver(o); }
	public static void removeObserver(SimpleObserver o) { observers.removeObserver(o); }
	
	public static void registerService(VncServiceProvider id)
	{
		services.push(id);
		notifyObservers(Operation.ServiceChange);
	}
	public static void unregisterService(VncServiceProvider id)
	{
		services.remove(id,false);
		notifyObservers(Operation.ServiceChange);
	}
	public static int getNServices() { return(services.size()); };
	public static VncServiceProvider getNthService(int n) { return(services.elementAt(n)); }
	public static void stopVNCServer()
	{	
		if(VNCService.vncServer!=null)
		{
		UDPService.setRole(false);
		VNCService.vncServer.stop(); 
		VNCService.vncServer=null;
		notifyObservers(Operation.Shutdown);
		}
	}
	public static VncListener vncServer = null;
	public static void runVNCServer(boolean start)
	{	G.putGlobal(G.PLAYTABLE,start);
		if(REMOTEVNC)
		{
		if(start)
		{
		stopVNCServer();
		G.print("starting vnc server");
		vncServer = new VncListener(BitmapSharingPort, new VncDispatcher(600,400));
		vncServer.start();
		}
		else if(vncServer!=null) 
		{
			stopVNCServer();
		}}
	}
	public static boolean isVNCServer()
	{
		return(vncServer!=null);
	}
	

}
