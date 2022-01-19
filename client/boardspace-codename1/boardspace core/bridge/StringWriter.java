package bridge;

import java.io.IOException;
import java.io.Writer;

public class StringWriter extends Writer
{
	StringBuilder out;
	
	public StringWriter() { out = new StringBuilder(); }

	public void close() throws IOException { }

	@Override
	public void flush() throws IOException {  }

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		out.append(cbuf,off,len);	
	}
	public String toString() { return(out.toString()); }
	
}
