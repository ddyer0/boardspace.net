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

//this input stream is strictly associated with NativeServerSocket
public class NativeInputStream extends InputStream
{	private NativeServerSocket impl;
	private int streamHandle;
	private int socketHandle;
	public NativeInputStream(NativeServerSocket sock,int sockHand)
	{
		impl = sock;
		socketHandle = sockHand;
		streamHandle = sock.getInputHandle(sockHand);
	}
	public String toString() { return("<NativeInputStream #"+streamHandle+">"); }

	private void throwException(int handle) throws IOException
	{
		if(handle<0)
			{String m = impl.getIOExceptionMessage(handle);
		     if(m!=null) { throw new IOException(m+" "+this); }
			}
	}
	
	public void close() throws IOException
	{
		throwException(impl.closeInput(streamHandle));
		throwException(impl.closeSocket(socketHandle));
		
	}
	public int read() throws IOException 
	{
		int v = impl.read(streamHandle);
		throwException(v);
		return(v);
	}
	public int read(byte[]array) throws IOException
	{
		return(read(array,0,array.length));
	}
	
	public int read(byte[]array,int offset,int len) throws IOException
	{
		int v = impl.readArray(streamHandle,array,offset,len);
		throwException(v);
		return(v);
	}


}
