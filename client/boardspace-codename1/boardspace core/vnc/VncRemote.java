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
