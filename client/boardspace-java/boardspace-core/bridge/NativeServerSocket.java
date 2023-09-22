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

/**
 * this implements server socket "accept", used for the simulator and android
 * 
 * the key to making this work is that it also implements the I/O for the
 * underlying sockets.
 * 
 * All the methods return integers, which if < -1 are error codes that should be
 * re-thrown as IOException errors.
 * 
 * @author Ddyer
 *
 */
public interface NativeServerSocket extends NativeInterface 
{	// bind the socket
	public int bindSocket(int port);
	// unbind the socket
	public int unBind();
	// listen for a connection on the socket, waiting forever
	public int connect(String host,int port);
	public int listen();
	// get handles for the input and output streams associated
	// with the new connection.  These will be used to construct
	// NativeInputStream and NativeOutputStream
	public int getInputHandle(int handle);
	public int getOutputHandle(int handle);
	// read data stream
	public int read(int handle);
	public int readArray(int handle, byte[] array, int offset, int len);
	public int closeInput(int handle);
	public int closeSocket(int handle);
	// write data stream
	public int write(int handle, int b);
	public int writeArray(int handle,byte[]array,int offset,int len);
	public int closeOutput(int handle);
	public int flush(int handle);
	
	// check for errors
	public String getIOExceptionMessage(int handle);
}
