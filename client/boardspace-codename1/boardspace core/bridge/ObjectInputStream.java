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
