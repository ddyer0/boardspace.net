package udp;

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
import lib.Http;

public class UdpListenerImpl implements udp.UdpListener{
	Vector<String>messages = new Vector<String>();
	boolean exit=false;
    String senderId = "S"+(new Random().nextLong()&0x7fffffffffL)+":";
    boolean filter = false;
	public String getMessage(int wait) 
	{ synchronized(this)
		{ if((wait>=0) && (messages.size()==0)) { try { wait(wait); } catch (InterruptedException e) {} }
		}
	  if(messages.size()>0) { return(messages.remove(0)); }
	  return(null);
	}

	DatagramSocket socket = null;
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
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

    }
	
	public void runBroadcastReceiver( int broadcastPort,boolean fil) 
	{
	    try {
		    socket = new DatagramSocket(broadcastPort);
		    socket.setBroadcast(true);
		    filter = fil;
		    while(!exit)
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
		    		synchronized(this) { notifyAll(); }
		    	}

			    }
		    if(socket!=null)
			    {
			     socket.close(); 
			     socket = null;
			    }
	    	}
	    	catch (IOException e)
	    	{
	        	messages.add("error: on receive: "+e);
	
	    	}
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
	{	boolean ok = false;
		try {
    	getAllInterfaces();
		DatagramSocket udpSocket = new DatagramSocket();
    	udpSocket.setBroadcast(true);
    	String msg = filter ? (senderId+m) : m;

    	broadcastOverAllInterfaces(udpSocket,msg,broadcastPort);
    	
   	    udpSocket.close();
   	    ok = true;
  		}
		catch (IOException e)
		{
			Http.postError(this,"error: sending to udp",e);
		}
		return(ok);
	}
	
	public void stop()
	{	exit = true;
		DatagramSocket s = socket;
		if(s!=null) 
		{ socket = null; 
		  s.close();
		}
	}
	public boolean isSupported() { return(true); }
}

