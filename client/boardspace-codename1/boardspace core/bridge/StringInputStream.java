package bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class StringInputStream extends InputStream 
{
	Reader rr = null;
	public StringInputStream(String s)
	{
		rr = new StringReader(s);
	}
	public int read() throws IOException {
		return(rr.read());
	}
}
