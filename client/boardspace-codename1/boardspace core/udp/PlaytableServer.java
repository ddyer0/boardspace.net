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
