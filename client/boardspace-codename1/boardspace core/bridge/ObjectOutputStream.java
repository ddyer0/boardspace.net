package bridge;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.codename1.io.Util;

public class ObjectOutputStream extends OutputStream
{	OutputStream outstream;
	DataOutputStream dataStream;
	public ObjectOutputStream(OutputStream s)
	{
		outstream = s;
		dataStream = new DataOutputStream(s);
	}

	public void writeObject(Object ob) throws IOException
	{	Util.writeObject(ob, dataStream);
	}
	public void write(int b) throws IOException {
		dataStream.write(b);
	}
	
}
