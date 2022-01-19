package rpc;

import java.util.StringTokenizer;

import bridge.Config;
import lib.Base64;
import lib.G;
import lib.OStack;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.StackIterator;
import rpc.RpcInterface.Keyword;
import udp.UDPService;

/**
 * listen for rpc connections from side screens
 * 
 * @author ddyer
 *
 */

class RpcInterfaceStack extends OStack<RpcInterface> implements SimpleObserver
{	int state = 0;
	public int getState() { return(state); }
	public boolean dead = false;
	public RpcInterface[] newComponentArray(int sz) {
		return  new RpcInterface[sz];
	}
	public RpcInterface find(String service)
	{
		for(int i=0;i<size();i++)
		{
			RpcInterface some = elementAt(i);
			if(service.equals(some.getName()))
			{
				return(some);
			}
		}
		return(null);
	}
	public String captureState() {
		StringBuilder b = new StringBuilder();
		for(int i=0;i<size();i++)
		{	RpcInterface item = elementAt(i);
			b.append(Base64.encode(item.getName()));
			b.append(" ");
			b.append(item.rpcIsActive());
			b.append(" ");
		}
		return(b.toString());
	}
	public void execute(String msg,RpcServiceServer caller) {
		StringTokenizer tok = new StringTokenizer(msg);
		String op = tok.nextToken();
		Keyword opc = Keyword.valueOf(op);
		switch(opc)
		{	case Select:
				String selected = Base64.decodeString(tok.nextToken());
				RpcInterface service = find(selected);
				if(service!=null) 
					{ synchronized (service)
						{if(!service.rpcIsActive())
						{
						service.setRpcIsActive(true);
						caller.launch(service);
						state++;
						}}
					}
				break;
			default: 
				throw G.Error("Not implemented");			
		}
	}
	public RpcInterface remove(RpcInterface f,boolean shuf)
	{
		RpcInterface vf = super.remove(f, shuf);
		state++;
		return vf;
	}
	public StackIterator<RpcInterface> push(RpcInterface v)
	{
		StackIterator<RpcInterface> vc = super.push(v);
		state++;
		return(vc);
	}

	public void update(SimpleObservable o, Object eventType, Object arg) {
		// we should get here when a service name changes
		state++;
	}
}

public class RpcService implements Config
{
	static RpcInterfaceStack services = new RpcInterfaceStack();
	
	static SimpleObservable observers = new SimpleObservable();
	public static void addObserver(SimpleObserver o) { observers.addObserver(o); }
	public static void removeObserver(SimpleObserver o) { observers.removeObserver(o); }
	
	public static void registerService(RpcInterface id)
	{
		services.push(id);
		id.addObserver(services);
		observers.setChanged(services);
	}
	public static void unregisterService(RpcInterface id)
	{	if(id!=null)
		{id.setRpcIsActive(false);
		 id.removeObserver(services);
		services.remove(id,false);
		observers.setChanged(services);
		}
	}
	public static void stopRpcServer()
	{	
		if(RpcService.rpcServer!=null)
		{
		UDPService.setRole(false);
		RpcService.rpcServer.stop(); 
		RpcService.rpcServer=null;
		}
	}
	public static RpcListener rpcServer = null;
	public static void runRpcServer(boolean start)
	{	if(REMOTERPC | G.debug())
		{
		G.putGlobal(G.PLAYTABLE,start);
		if(start)
		{
		stopRpcServer();
		G.print("starting Rpc server");
		rpcServer = new RpcListener(RpcPort);
		rpcServer.start();
		}	
		else if(rpcServer!=null) 
		{
			stopRpcServer();
		}}
	}
	public static boolean isRpcServer()
	{
		return(rpcServer!=null);
	}
}
