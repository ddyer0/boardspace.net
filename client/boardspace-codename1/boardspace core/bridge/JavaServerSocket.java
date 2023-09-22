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
package bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import com.codename1.io.SocketConnection;
import com.codename1.io.Socket.StopListening;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;

import lib.G;
import lib.Http;
import lib.ServerSocketProxy;
import lib.SocketProxy;

public class JavaServerSocket extends SocketConnection implements ServerSocketProxy
{
	int port;
	StopListening stop = null;
	
	// this isn't nice - the static variable is global and not associated
	// with the particular request
	public static Vector<ServerConnection> connections = new Vector<ServerConnection>();
	public static Object semaphore = new Object();

	public static void addConnection(ServerConnection c)
	{
		connections.addElement(c);
		synchronized(semaphore) { semaphore.notify(); }
	}
	public JavaServerSocket()
	{
	}
	public JavaServerSocket(int portToListen) throws IOException
	{
		port = portToListen;
	}
	public synchronized void close() throws IOException { if(stop!=null) { stop.stop(); stop=null; }}
	
	public void listen()
	{	
		if(stop==null) { stop = listen(port,JavaServerSocket.class); }
	}
	/**
	 * Listen to incoming connections on port
	 * @param port the device port
	 * @param scClass class of callback for when the connection is established or fails, this class
	 * will be instantiated for every incoming connection and must have a public no argument constructor.
	 * @return StopListening instance that allows the the caller to stop listening on a server socket
	 */
	public static StopListening listen( final int port,  final Class<? extends SocketConnection>scClass) 
	{
	    class Listener implements StopListening, Runnable {
	        private boolean stopped;
	        public void run() {
	            try {
	            	NativeServerSocket sockImpl = (NativeServerSocket)NativeLookup.create(NativeServerSocket.class);
	            	if(sockImpl==null) { G.Error("NativeServerSocket class not found"); }
	            	else {
	            		boolean supported = sockImpl.isSupported();
	            	if(supported)
	            	{
	                final int bound = sockImpl.bindSocket(port);
                    final SocketConnection sc = (SocketConnection)scClass.newInstance();
	                if(bound<0) { sc.connectionError(bound,sockImpl.getIOExceptionMessage(bound)); }
	                else
	                {while(bound>=0 && !stopped) {
	                	final int connection = sockImpl.listen();
	                    if(connection>=0) {
	                        //sc.setConnected(true);
	                        Display.getInstance().createThread(new Runnable() {
	                            public void run() {
	                                sc.connectionEstablished(
	                                		new NativeInputStream(sockImpl,connection),
	                                		new NativeOutputStream(sockImpl,connection));
	                            }
	                        }, "Connection " + port).start();
	                    } else {
	                        sc.connectionError(connection, sockImpl.getIOExceptionMessage(connection));
	                    }
	                }
	                sockImpl.unBind();
	                }
	            }}
	            }
	            catch(Throwable err) {
	               Http.postError(this, "server listen", err);
	            }
	        }

	        public void stop() {
	            stopped = true;
	        }
	        
	    }
	    Listener l = new Listener();
	    Display.getInstance().createThread(l, "Listening on " + port).start();
	    return l;
	}	
	public ServerConnection accept() throws IOException 
	{	listen();
		while(stop!=null)
			{
			try {
				if(connections.size()>0) { return(connections.remove(0)); }
				synchronized(semaphore) { semaphore.wait(); }
			} catch (InterruptedException e) {	}
			}
		return(null);
	}
	public SocketProxy acceptProxy() throws IOException
	{ return(accept()); 
	}

	public void closeQuietly() {
		try { close(); } catch (IOException e) { }
	}
	@SuppressWarnings("deprecation")
	public static boolean isServerSocketSupported()
	{	return(com.codename1.io.Socket.isServerSocketSupported());
	}
	public void connectionError(int errorCode, String message) 
	{
		G.print("Socket connection error: "+errorCode+" "+message);
	}
	public void connectionEstablished(InputStream is, OutputStream os) {
		G.print("established "+is+" "+os);
		addConnection(new ServerConnection(is,os));		
	}
}
