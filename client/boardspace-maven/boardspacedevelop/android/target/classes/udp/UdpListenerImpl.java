package udp;

import com.codename1.impl.android.AndroidNativeUtil;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Vector;
import android.net.wifi.WifiManager;
import android.content.Context ;
import android.net.DhcpInfo ;
import java.util.Random;
/**
 * android implementation of the UDP transmit/receive interface.  This has
 * just one important parameter, the port to broadcast on.
 * 
 * This filters out our own messages by prepending a sender id to each
 * message and recognising it.
 * 
 * @author Ddyer
 *
 */
public class UdpListenerImpl 
{	
	boolean exitRequest=false;
    String senderId = "S"+(new Random().nextLong()&0x7fffffffffffffffL)+":";
	Vector<String>messages = new Vector<String>();
	boolean filter=false;
	/**
	 * get a message or wait a maximum amount of time for one to arrive.
	 * as usual, wait 0 means forever. wait -1 means wait never.
	 * otherwise wait time in milliseconds
	 * 
	 * messages from this interface will begin with s....: or error:
	 * but remember that it's a public broadcast, so any kind of crap 
	 * might occur.
	 * @param wait
	 * @return
	 */
	public String getMessage(int wait) 
	{ synchronized(this)
		{ if(!exitRequest 
				&& (wait>=0)
				&& (messages.size()==0))
			{ try { wait(wait); } catch (InterruptedException e) {} }
		}
	synchronized (messages)
	{
	  if(messages.size()>0) 
	  { 
	    return(messages.remove(0));
	  }}
	  return(null);
	}
	
	private InetAddress getBroadcastAddress() throws IOException 
	{
    	Context c = AndroidNativeUtil.getContext();
		WifiManager wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcp = wifi.getDhcpInfo();
		// handle null somehow
		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads);
	}
	
	public boolean sendMessage(String msg,int broadcastPort)
	{	boolean ok = false;
	    try {
	
		 DatagramSocket socket = new DatagramSocket();
		 if(socket!=null)
		 {
		 socket.setBroadcast(true);	 
		 InetAddress broadcastIPAddress = getBroadcastAddress();
		 if(filter) { msg = senderId+msg; }
 	     byte[] bytes = msg.getBytes();
		 DatagramPacket packet = new DatagramPacket(bytes,bytes.length, broadcastIPAddress , broadcastPort);
		 socket.send(packet);
		 socket.close();
		 ok = true;
		 }
		 else { synchronized(messages) { messages.add("error: socket creation failed for "+msg); }}
		}
		catch (IOException e) 
			{	if(!exitRequest) { synchronized (messages) { messages.add("error: udp on send "+e); }}
			}
	    return(ok);
	}
	
	/**
	 * receive broadcast messages on a specfied port, put them
	 * in a queue to be retrieved by some other process.
	 * 
	 * @param broadcastPort
	 */
	public void runBroadcastReceiver(int broadcastPort,boolean fil)
    {
    try {
    	filter = fil;
    	exitRequest = false;
    	DatagramSocket socket = new DatagramSocket(broadcastPort, InetAddress.getByName("0.0.0.0"));
	    if(socket!=null)
	    {
	    socket.setBroadcast(true);
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
	    		synchronized(messages) { messages.add(host+":" + msg); }
	    		synchronized(this) { notifyAll(); }
	    	}
		    }
	     socket.close(); 
	    }
    }
    catch (IOException e)
    	{
    	if(!exitRequest) { synchronized(messages) { messages.add("error: on receive "+e); }}
    	}
    }
	public void stop()
	{	exitRequest = true;
	}
    public boolean isSupported() {
        return true;
    }
}


