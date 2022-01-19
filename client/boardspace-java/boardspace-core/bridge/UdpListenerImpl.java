package bridge;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import lib.G;
import lib.Http;
import lib.Plog;
import udp.UdpListener;

public class UdpListenerImpl implements UdpListener ,NativeInterface 
{
	   
	Vector<String>messages = new Vector<String>();
	boolean exitRequest=false;
    String senderId = "S"+(new Random().nextLong()&0x7fffffffffffffffL)+":";
    boolean filter = false;
	public String getMessage(int wait) 
	{ synchronized(this)
		{ if((wait>=0) && (messages.size()==0)) { G.waitAWhile(this, wait); }
		}
	  if(messages.size()>0) { return(messages.remove(0)); }
	  return(null);
	}
	public void wake() { G.wake(this); }
    public Vector<String>myIPAddresses = new Vector<String>();
    public Vector<String>myBroadcastAddresses = new Vector<String>();

    private void getAllInterfaces() 
    {
    	myBroadcastAddresses.clear();
    	myIPAddresses.clear();
 
    	Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) 
    	{
    		NetworkInterface networkInterface = interfaces.nextElement();

    		if (networkInterface.isLoopback() || !networkInterface.isUp()) {
    			continue; // Don't want to broadcast to the loopback interface
    		}

    		for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
    			InetAddress broadcast = interfaceAddress.getBroadcast();
    			InetAddress myAddress = interfaceAddress.getAddress();
    			if(broadcast!=null) { myBroadcastAddresses.addElement(broadcast.getHostAddress()); }
    			if(myAddress!=null) { myIPAddresses.addElement(myAddress.getHostAddress()); }
    		}}
		} catch (SocketException e) {
		e.printStackTrace();
	}

    }
	
    DatagramSocket mySocket = null;
	@SuppressWarnings("unused")
	public void runBroadcastReceiver( int broadcastPort,boolean fil) 
	{	Plog.log.addLog("Udp listener start");
	    try {
	    	DatagramSocket socket = mySocket = new DatagramSocket(broadcastPort);
	    	if(socket!=null)
	    	{
		    socket.setBroadcast(true);
		    filter = fil;
		    while(!exitRequest)
			    {
		    	DatagramPacket packet = new DatagramPacket(new byte[1000], 1000);
		    	socket.receive(packet);
		    	InetAddress ip = packet.getAddress();
		    	String host = ip.getHostAddress();
		    	String msg = new String(packet.getData(), packet.getOffset(), 	packet.getLength());
		    	
		    	if(filter && msg.charAt(0)=='S')
	    		{	if(!msg.startsWith(senderId))
	    			{
	    			int dx = msg.indexOf(':');
	    			if(dx>0) { msg = msg.substring(dx+1); }
	    			}
	    			else { msg = null; }
	    		}
		    	if(msg!=null)
		    	{
		    		messages.add(host+":" + msg);
		    		G.wake(this);	// wake anyone who is waiting on us
		    	}
			    }
		    socket.close(); 
	    	}
	    	else 
	    	{ messages.add("error: socket creation failed for "+broadcastPort); 
	    	}
	    	
	    	}
	    	catch (IOException e)
	    	{
	        	messages.add("error: on receive: "+e);
	
	    	}
	    	Plog.log.addLog("Udp listener exit");
	    }
    private void broadcastOverAllInterfaces(DatagramSocket udpSocket ,String m,int broadcastPort)
    		throws IOException
    {
	  byte[] msg = m.getBytes();

	  for(int i = 0;i<myBroadcastAddresses.size(); i++) 
	  {	String addr = myBroadcastAddresses.elementAt(i);
	  	InetAddress net = InetAddress.getByName(addr);
	  	DatagramPacket sendPacket = new DatagramPacket(msg,msg.length, net , broadcastPort);
	    //G.print("To "+addr+":"+m);
	    udpSocket.send(sendPacket);
	  }
    }
    
	public boolean sendMessage(String m,int broadcastPort) 
	{	DatagramSocket udpSocket = null;
		boolean ok = false;
		try {
    	getAllInterfaces();
		udpSocket = new DatagramSocket();
    	udpSocket.setBroadcast(true);
    	String msg = filter?(senderId+m):m;

    	broadcastOverAllInterfaces(udpSocket,msg,broadcastPort);
    	ok = true;
   		}
		catch (IOException e)
		{
			if(G.debug()) { Http.postError(this, "sending to udp",e); }
		}
		finally 
		{
			if(udpSocket!=null)
			{
		  	    udpSocket.close();
			}
		}
		return(ok);
	}
	public void closeSocket()
	{
		DatagramSocket m = mySocket;
		if(m!=null) { mySocket = null; m.close(); }
	}
	public void stop()
	{	
		exitRequest = true;
		closeSocket();
	}
	public boolean isSupported() { return(true);	}
	
}
