package lib;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
/**
 * Flipreader is an input stream reader that is able to flip
 * between text and binary.  The main interface is like StringTokenizer,
 * but you can switch to binary readBytes.  The convention for flipping
 * is to call read() until a | is read, which marks the boundary of binary
 * data.
 * 
 * @author Ddyer
 *
 */
public class FlipReader
{	InputStream rawStream;
	BufferedInputStream from;
	int nextChar = -1;
	FlipReader(InputStream stream)
	{	rawStream = stream;
		from = new BufferedInputStream(stream);
	}
	
	int singleRead()
	{	
		try {
			return(from.read());
			} catch (IOException e) {
				try {
					rawStream.close();
				} catch (IOException e1) {
			}
		} 
		return(-1);
	}
	public int read() 
		{ int ch = nextChar;
		  if(ch<=' ')
			{ ch=singleRead();
			}
		  nextChar = -1;
		  return(ch);
		}
	
	public boolean hasMoreTokens() 
	{ 	if(nextChar<=' ')
		{
		while( ((nextChar = singleRead())<=' ') && (nextChar>0))  {}
		}
		return(nextChar>' ');
	}
	
	public int readBytes(byte[]data,int offset,int limit) 
	{ nextChar = -1;
	  int total = 0;
	  int remain = limit;
	  try {
		while(remain>0)
		{
		int count = from.read(data,offset+total,remain);
		if(count<0) { break; }
		total+= count;
		remain -= count;
		}
		return(total);
	  } catch (IOException e) {
		e.printStackTrace();
		try {
			rawStream.close();
		} catch (IOException e1) {
		}
	  } 
	  return(-1);
	}
	public String nextToken() {
		  try {
		if(hasMoreTokens())
		{
		 StringBuilder b = new StringBuilder();
		 if(nextChar>=' ') { b.append((char)nextChar); nextChar = -1; }
		 int ch;
		 while( (ch=from.read())>' ') { b.append((char)ch); }
		 return(b.toString());
		}}
		  catch (IOException e) {
				e.printStackTrace();
				try {
					rawStream.close();
				} catch (IOException e1) {
				}
		  }
		return(null);
}
}
