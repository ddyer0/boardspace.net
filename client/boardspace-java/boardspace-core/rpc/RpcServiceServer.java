package rpc;

import lib.SimpleObservable;
import lib.SimpleObserver;

/**
 * keep the list of available games and roles that are a vailable,
 * and provide the dispatch information as a rpc service
 * 
 * @author ddyer
 *
 */
public class RpcServiceServer implements RpcInterface
{	
	public RpcInterface launchedService = null;
	int captureState = -1;
	boolean active = true;
	public ServiceType serviceType() { return ServiceType.Dispatch; }
	RpcInterfaceStack services = RpcService.services;
	
	public boolean needsRecapture() {
		return(captureState!=services.getState());
	}

	public String getName() {
		
		return "Dispatcher";
	}
	public void setName(String c) { }

	public boolean rpcIsActive() {
		if(services.dead) { active = false; } 
		return(active);
	}
	public void setRpcIsActive(boolean v)
	{
		active = v;
	}
	public void updateProgress() {}

	public String captureInitialization() {
		return("");
	}

	SimpleObservable observers = new SimpleObservable();
	
	public void removeObserver(SimpleObserver b) {		
		observers.removeObserver(b);
	}

	public void addObserver(SimpleObserver o) {
		observers.addObserver(o);
	}

	public String captureState() {
		captureState = services.getState();
		return services.captureState();
	}
	// callback from the service stack when it launches a service
	public void launch(RpcInterface v)
	{
		launchedService = v;
	}
	public void execute(String msg) {
		services.execute(msg,this);
		observers.setChanged();
	}
	public void shutDown()
	{
		setRpcIsActive(false);
	}
}