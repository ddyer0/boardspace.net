package rpc;

import lib.SimpleObserver;

/** this is the basic interface for a rpc service, the current examples
 *  of a service are 
 *  (1) the basic dispatch to list available games and roles
 *  (2) spectator/remote player for a game
 *  (3) side screen for a game
 *  
 * @author ddyer
 *
 */
public interface RpcInterface extends RpcConstants 
{
	public boolean needsRecapture();		// true if the state is known to have changed
	public ServiceType serviceType();
	public String captureState();			// get the String which encapsulates the shared state
	public String captureInitialization();	// initialization information for a new service
	public void execute(String msg);		// do something
	public String getName();
	public void setName(String f);
	public void updateProgress();			// network connection is live

	// start/stop
	public boolean rpcIsActive();
	public void setRpcIsActive(boolean v);
	public void shutDown();
	// same as SimpleTarget
	public void removeObserver(SimpleObserver b);
	public void addObserver(SimpleObserver o);	 
	
	enum Keyword { Complete,Update,Digest,Select, None };
}

