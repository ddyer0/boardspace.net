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

import vnc.VNCConstants.Operation;

/**
 * this is a generic "spectator" window that can be attached to any regular window
 * It's not intended for use in production, but as a debugging vehicle
 * 
 * @author Ddyer
 *
 */
public class VncRemote implements VncServiceProvider
{	private VncScreenInterface bitmapProvider;
	private VncEventInterface eventProvider;
	private String serviceName;
	private VncRemote activeInstance = null;
	boolean stopped = false;
	public boolean isActive()
	{ return( activeInstance!=null 		
				? activeInstance.isActive()
				: !stopped && (bitmapProvider!=null) && bitmapProvider.isActive()); 
	}
	public VncRemote(String name,VncScreenInterface bits,VncEventInterface events)
	{	serviceName = name;
		bitmapProvider = bits;
		eventProvider = events;
	}
	public VncScreenInterface provideScreen() { return(bitmapProvider); }
	
	public VncEventInterface provideEventHandler() { return(eventProvider); }

	public VncServiceProvider newInstance() {
		return (activeInstance = new VncRemote(serviceName,bitmapProvider,eventProvider));
	}
	public String getName() {
		return(serviceName);
	}
	public void setName(String n) 
	{ String oldName = serviceName;
	  serviceName = n; 
	  if(!n.equals(oldName)) { VNCService.notifyObservers(Operation.ServiceChange); }
	}
	String stopReason = null;
	public void stopService(String reason) 
	{ stopped = true;
	  stopReason = reason;
	  bitmapProvider.stopService(reason);
	  eventProvider.stopService(reason); 
	}

}
