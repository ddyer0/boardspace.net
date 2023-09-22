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
import java.net.InetAddress;
import java.net.Socket;

import lib.ServerSocketProxy;
import lib.SocketProxy;

class SocketP implements SocketProxy
{
	Socket real;
	
	SocketP(Socket s) { real = s; }
	public InetAddress getInetAddress()
	{
		return real.getInetAddress();
	}
	public InetAddress getLocalAddress() {
		return real.getLocalAddress();
	}
	public boolean isConnected() {
		return(real.isConnected());
	}

	public InputStream getInputStream() throws IOException {
		return(real.getInputStream());
	}
	
	public OutputStream getOutputStream() throws IOException {
		return real.getOutputStream();
	}
	public void close() throws IOException {
		real.close();
	}
}
/**
 * a thin wrapper for the standard ServerSocket, provides the additional
 * "closeQuietly" method, and provides a SocketProxy instead of a raw socket.
 * this version is used if USE_NATIVE_SOCKETS is false
 * @author Ddyer
 *
 */
public class JavaServerSocket extends java.net.ServerSocket implements ServerSocketProxy
{
	public void closeQuietly() { try { close(); } catch (IOException e) {}}
	
	public JavaServerSocket(int port) throws IOException 
	{
		super(port);
	}
	public SocketProxy acceptProxy() throws IOException
	{
		Socket s = accept();
		return(new SocketP(s));
		
	}
	public static boolean isServerSocketSupported() { return(true);	}

}

