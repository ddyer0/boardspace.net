package lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MarkableInputStream extends InputStream
{
	InputStream in = null;
	boolean nativeSupport = false;
	public MarkableInputStream(InputStream from)
	{ 	in = from;
		nativeSupport = false;//from.markSupported();
	}
	public boolean markSupported() { return(true); }
	ByteArrayOutputStream markOut = null;
	ByteArrayInputStream markIn = null;
	int markLen = 0;
	public void mark(int n)
	{ 	if(nativeSupport)
		{
		in.mark(n);
		}
	else
		{
		markLen = n;
		markOut = new ByteArrayOutputStream(); 
		markIn = null;
		}
	}
	public void reset() throws IOException
	{	if(nativeSupport)
		{
		in.reset();
		}
		else if(markOut==null) { throw new IOException("Mark not valid"); }
		else {
		markIn = new ByteArrayInputStream(markOut.toByteArray());
		}
	}
	public int read() throws IOException 
	{	if(nativeSupport) 
		{
			return(in.read());
		}
		else
		{
		int n = -1;
		if(markIn!=null) 
			{ n = markIn.read();
			  if(n<0) { markIn = null; } 
			}
		if(n<0) 
		{ n = in.read(); 
		  if(n>=0)
		  {
		  if(markLen-- <= 0) { markOut = null; }	// mark no longer valid
		  if(markOut!=null) 
		  { markOut.write(n);	// record the byte for possible replay
		  }}
		}
		return(n);
		}
	}
	public void close() throws IOException
	{	in.close();
	}
}
