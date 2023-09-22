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
import java.io.OutputStream;

// this output stream is strictly associated with NativeServerSocket
public class NativeOutputStream extends OutputStream
{
	NativeServerSocket impl;
	int outputHandle;
	int socketHandle;
	public NativeOutputStream(NativeServerSocket sock,int hand)
	{
		impl = sock;
		socketHandle = hand;
		outputHandle = sock.getOutputHandle(hand);
	}
	public String toString() { return("<NativeOutputStream #"+outputHandle+">"); }
	private void throwException(int handle) throws IOException
	{
		if(handle<0)
			{String m = impl.getIOExceptionMessage(handle);
			 if(m!=null) { throw new IOException(m+" "+this); }}
	}

	public void close() throws IOException
	{
		throwException(impl.closeOutput(outputHandle));
		throwException(impl.closeSocket(socketHandle));
	}
	public void write(int b) throws IOException 
	{
		throwException(impl.write(outputHandle,b));
	}
	public void write(byte[]array) throws IOException
	{	write(array,0,array.length);
	}
	
	public void write(byte[]array,int off,int len) throws IOException
	{
		throwException(impl.writeArray(outputHandle,array,off,len));
	}
    public void flush() throws java.io.IOException
    {
    	throwException(impl.flush(outputHandle));
    }
}
