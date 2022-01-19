package bridge;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;

//
// this is a print stream that encodes its output using UTF8.
// it falls back to system default if utf-8 isn't supported,
// which should never happen.
//
public class Utf8Printer extends PrintStream 
{
	public Utf8Printer(OutputStream ss,String encod) throws UnsupportedEncodingException
	{	
			super(ss,false,encod);
	}
	public Utf8Printer(OutputStream ss) { super(ss); }
	
	// use this static instead of "new" so you don't have
	// to catch UnsupportedEncodingException
	public static Utf8Printer getPrinter(OutputStream ss)
	{
		try {
			return new Utf8Printer(ss,"UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			return(new Utf8Printer(ss));
		}
	}
}
