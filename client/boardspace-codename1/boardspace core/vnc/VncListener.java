/*
	This implements a TCP based bitmap sharing scheme.  
	I owe a big thanks to sourceforge project "ajaxvnc"
	There's not much left of it here, but it provided the
	template.  
 */

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
package vnc;

import java.io.IOException;

import bridge.Config;
import bridge.JavaServerSocket;
import bridge.ServerSocket;
import bridge.ThreadDeath;
import lib.*;


/**
 *	listen for connections on a socket, spawn a VNCTransmitter for each connection 
 */
public class VncListener implements Runnable,Config
{
    private int port;
    private boolean exitRequest = false;
    ServerSocketProxy listening;
    int sessions=0;
    VncServiceProvider provider;
 
    /**
     * listen for connections on the specified port, invoke
     * the service provider for each new connection
     * 
     * @param inport
     * @param p
     */
	public VncListener(int inport,VncServiceProvider p) {
		port = inport;
		provider = p;
	}
	
 	public static ServerSocketProxy makeServerSocket(int port) throws IOException
 	{
 		return(USE_NATIVE_SOCKETS && !G.isIOS()
 				? new ServerSocket(port)
 				: new JavaServerSocket(port));
 	}
	public static boolean isServerSocketSupported() {
		return( (USE_NATIVE_SOCKETS && !G.isIOS())
				 ? ServerSocket.isServerSocketSupported()
				 : JavaServerSocket.isServerSocketSupported());
	}
	
	public static boolean isSupported() { return isServerSocketSupported(); }
	public void start()
	{	if(isSupported())
		{
		new Thread(this,"VNC Listener on "+port).start();
		}
	else { G.print("Server sockets are not supported here");
		}
	}
	public void stop()
	{	ServerSocketProxy l = listening;
		listening = null;
		exitRequest = true;
		if(l!=null) 
		{ l.closeQuietly();
		}
	}

	public void run()
	{	try {	
		int tries = 0;
		while(!exitRequest && (tries++<5))
		{
		if(listening==null) 
		{ G.print("Starting server on port "+port);
		  listening = makeServerSocket(port); 
		}
		SocketProxy p;
		try {
			p = listening.acceptProxy();
			if(p!=null) 
			{	sessions++;
				tries=0;
				VncServiceProvider pro = provider.newInstance();
				new VNCTransmitter(p,port,pro,"Sess-"+sessions).start();
			}
		} catch (IOException e) 
			{
			if(!exitRequest)
				{
				 e.printStackTrace();
				 G.print("Server socket closed, reopening");
				 if(listening!=null) { listening.closeQuietly(); }
				 listening = null;	// bind a new socket
				}
			
			}
		}
		}
		catch (ThreadDeath e) { stop(); G.print("VncListener killed"); }
		catch (Throwable err)
		{	
			stop();
			Http.postError(this, "in VncListener",err);
		}
	}	
}
