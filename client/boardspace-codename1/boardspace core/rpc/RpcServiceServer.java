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
