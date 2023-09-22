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

import lib.G;
import lib.SocketProxy;
/**
 * this is the callback class for ServerSockets.  It has to be a completely independent
 * class so the simple newInstance() interface will work. 
 * @author Ddyer
 *
 */
public class ServerConnection implements SocketProxy
{	public ServerConnection () {  }
	private InputStream inputStream;
	private OutputStream outputStream;
	public InputStream getInputStream() { return(inputStream); }
	public OutputStream getOutputStream() { return(outputStream); }
	public void close() throws IOException
	{ InputStream i = inputStream;
	  OutputStream o = outputStream;
	  if(i!=null) { i.close(); inputStream=null; }
	  if(o!=null) { o.close(); outputStream = null; }
	}
	public ServerConnection(InputStream is,OutputStream os)
	{
		inputStream = is;
		outputStream = os;
	}
	public void connectionEstablished(ServerSocket sock,InputStream is, OutputStream os) 
	{	G.print("established "+is+" "+os);
		inputStream = is;
		outputStream = os;
		sock.addConnection(this);
	}
	public void connectionError(int errorCode, String message) {
		G.print("Socket connection error: "+errorCode+" "+message);
	}
	public boolean isConnected()
	{
		return((inputStream!=null) && (outputStream!=null));
	}
	public InetAddress getLocalAddress() {
		return(new InetAddress());
	}
	public InetAddress getInetAddress() {
		return(new InetAddress());
	}
}
