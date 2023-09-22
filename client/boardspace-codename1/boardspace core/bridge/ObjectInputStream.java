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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.codename1.io.Util;

import lib.G;

public class ObjectInputStream 
{	InputStream instream;
	DataInputStream dataStream;
	public ObjectInputStream(InputStream s)
	{
		instream = s;
		dataStream = new DataInputStream(s);
	}
	public Object readObject() throws IOException,ClassNotFoundException
	{	return Util.readObject(dataStream);
	}
	public void close() throws IOException { instream.close(); }
	public void defaultReadObject() { G.Error("ObjectInputStream defaultreadobject Not implemented");	}
}
