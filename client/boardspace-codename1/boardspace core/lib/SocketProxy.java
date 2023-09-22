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

import bridge.InetAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/*
 * this encapsulates the things out netconns need from sockets.
 * curse codename1 for making server sockets and client sockets
 * not share an ancestor
 */
public interface SocketProxy {
	public InetAddress getLocalAddress();
	public InetAddress getInetAddress();
	public boolean isConnected();
	public InputStream getInputStream() throws IOException;
	public OutputStream getOutputStream() throws IOException;
	public void close() throws IOException;
}
