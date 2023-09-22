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
import lib.Plog;
import lib.SocketProxy;

// this version if used if USE_SERVER_SOCKETS is true.  This ultimately uses
// java's native socket library, but the control paths and data structures
// mirror those used in the codename1 branch.
public class ClientSocket implements SocketProxy
{
	@SuppressWarnings("unused")
	private int port;
	@SuppressWarnings("unused")
	private String server = null;
	private NativeServerSocket sockImpl = null;
	String socketErrorMessage = null;		// error from the connection process
	ServerConnection conn = null;
	
	// do the connection and wait for the callback
	public ClientSocket(String s,int n) throws IOException
	{
		server = s;
		port = n;
		sockImpl = (NativeServerSocket)G.MakeNative(NativeServerSocket.class);
		if(sockImpl!=null && sockImpl.isSupported())
		{
			int connection = sockImpl.connect(s,n);
			socketErrorMessage = sockImpl.getIOExceptionMessage(connection);
			if(socketErrorMessage!=null)
			{	Plog.log.addLog("socket ioexception ",socketErrorMessage);
				throw new IOException(socketErrorMessage);
			}
			if(connection>=0)
			{
			NativeInputStream ins = new NativeInputStream(sockImpl,connection);
			NativeOutputStream outs = new NativeOutputStream(sockImpl,connection);
			conn = new ServerConnection(ins,outs);
			}
		}
	}

	public InetAddress getLocalAddress() {
		return conn!=null ? conn.getLocalAddress() : null;
	}

	public InetAddress getInetAddress() {
		return conn!=null ? conn.getInetAddress() : null;
	}

	public boolean isConnected() {
		return conn!=null ? conn.isConnected() : false;
	}

	public InputStream getInputStream() throws IOException {
		return conn!=null ? conn.getInputStream() : null;
	}

	public OutputStream getOutputStream() throws IOException {
		return conn!=null ? conn.getOutputStream() : null;
	}

	public void close() throws IOException {
		if(conn!=null) { conn.close(); }		
	}


}
