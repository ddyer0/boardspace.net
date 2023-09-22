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
package lib;

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
/**
 * testing framework for java UDP
 * 
 * playtable continously broadcast messages like this on udp port 52033
 * 
 * 23:53:25  In 192.168.68.110:{"ip":"192.168.68.110","port":62108,"senderName":"192.168.68.110","productName":"Catan","handheldIdentifier":"party.blok.catan"}
 * 23:53:30  In 192.168.68.110:{"ip":"192.168.68.110","port":56881,"senderName":"192.168.68.110","productName":"Spades","handheldIdentifier":"com.blokparty.spades"}
 * 
 * @author Ddyer with a lot a help for Dr. Google.
 *
 */
public class UDP
{	static int CodenamesBeacon = 5353;
	static int PlaytableBeacon = 52033;
	static int PlaytableBeacon_Catan = 47777;	// local game on 57575, internet 35.241.26.53:443(ssl)
	static int server = 5002;
    static int broadcastPort = server;
    static String broadcastIPStr = "255.255.255.255";
    public Vector<String>messagesOut = new Vector<String>();
    public Vector<String>messagesIn = new Vector<String>();
    String client = "C "+new Random().nextInt(100);
    public Vector<String>myIPAddresses = new Vector<String>();
    public Vector<String>myBroadcastAddresses = new Vector<String>();
    
    boolean exit = false;
    
    public void broadcastReceiver()
    {
    try {
	    InetAddress broadcastIPAddress = InetAddress.getByName(broadcastIPStr);    
	    DatagramSocket socket = new DatagramSocket(broadcastPort, InetAddress.getByName("0.0.0.0"));
	    socket.setBroadcast(true);
	
	    while(!exit)
		    {
	        DatagramPacket packet = new DatagramPacket(new byte[1000], 1000,broadcastIPAddress, broadcastPort);
	    	socket.receive(packet);
	    	InetAddress ip = packet.getAddress();
	    	//String host = ip.getHostAddress();
	    	//if(!myIPAddresses.contains(host))	// filter out messages that come from us
	    	{
	    	String msg = new String(packet.getData(), packet.getOffset(), 	packet.getLength());
	    	StringBuilder msg2 = new StringBuilder();
	    	for(int i=0;i<msg.length();i++)
	    		{
	    		int b = msg.charAt(i);
	    		if(b<' ' || b>=128) { }
	    		else { msg2.append((char)b); }
	    	}
		    System.out.println(G.briefTimeString(G.Date())+" In "+ip.getHostAddress()+":" + msg2);
		    }}
	     socket.close(); 
    }
    catch (IOException e)
    	{
    	e.printStackTrace();
    	}
    }
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
    	if(myBroadcastAddresses.size()==0) { myBroadcastAddresses.addElement(broadcastIPStr); }
		} catch (SocketException e) {
		e.printStackTrace();
	}

    }
	public void sendMessage(String msg,String broadcastIPStr,int broadcastPort) throws IOException
	{	
		InetAddress broadcastIPAddress = InetAddress.getByName(broadcastIPStr);   
    	DatagramSocket socket = new DatagramSocket(broadcastPort, InetAddress.getByName("0.0.0.0"));
	    socket.setBroadcast(true);
	    byte[] bytes = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(bytes,bytes.length, broadcastIPAddress , broadcastPort);
		socket.send(packet);
		socket.close();
	}
    private void broadcastOverAllInterfaces(DatagramSocket udpSocket ,String m) throws IOException
    {
	  byte[] msg = m.getBytes();

	  for(int i = 0;i<myBroadcastAddresses.size(); i++) 
	  {	String addr = myBroadcastAddresses.elementAt(i);
	  	InetAddress net = InetAddress.getByName(addr);
	  	DatagramPacket sendPacket = new DatagramPacket(msg,msg.length, net , broadcastPort);
	    G.print("To "+addr+":"+m);
	    udpSocket.send(sendPacket);
	  }
    }
    
    public void broadcastSender()
    {
		try {
		DatagramSocket udpSocket = new DatagramSocket();
    	udpSocket.setBroadcast(true);
    	while(!exit)
    	{	boolean sent = false;
    		if(messagesOut.size()>0)
    			{ String m = messagesOut.remove(0);
    			  broadcastOverAllInterfaces(udpSocket,m);
    			  sent = true;
    			}
    		if(!sent) { G.doDelay(100); }
    	}
    	udpSocket.close();
		}
    	catch (IOException e) {
			e.printStackTrace();
		}
    }
    public void start()
    {	getAllInterfaces();
    	Runnable sender = new Runnable() { public void run() { broadcastSender(); }};
    	Runnable receiver = new Runnable() { public void run() { broadcastReceiver(); }};
    	new Thread(sender).start();
    	new Thread(receiver).start();
    }   
    public void processMessages()
    {	while(messagesIn.size()>0)
    	{
    	String msg = messagesIn.remove(0);
    	G.print("Broadcast in: "+msg);
    	}
    }
    public static void test()
    {UDP ssdp = new UDP();
	ssdp.start();
	Random r = new Random();
	@SuppressWarnings("unused")
	int loop=0;
		while(true)
		{	ssdp.processMessages();
			loop++;
			//ssdp.messagesOut.addElement("localhost:"+"msg "+loop);
			G.doDelay(r.nextInt(500));
			
		}
	}
    public static void main(String args[])   {   	test();    }
    
}

