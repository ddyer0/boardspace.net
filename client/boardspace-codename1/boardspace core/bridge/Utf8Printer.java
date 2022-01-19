package bridge;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;

//
// this is a print stream that encodes its output using UTF-8
// and falls back to system default if that fails (which should
// never happen)
//
public class Utf8Printer extends PrintStream {
	public Utf8Printer(OutputStream ss,String encod) throws UnsupportedEncodingException
	{	
			super(ss);
	}
	public Utf8Printer(OutputStream ss) { super(ss); }
	
	//
	// use this static instead of "new" so you don't have to 
	// catch UnsupportedEncodingException
	//
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

	public void print(String s)
	{
        try {
        	byte bs[] = s.getBytes("UTF-8");
        	write(bs,0,bs.length);
		} catch (UnsupportedEncodingException e) {
			super.print(s);
		}
    }
	public void println(String s)
	{
        print(s);print('\n');
    }
	public void print(char s)
	{	print(""+s);
	}
	public void println(char s)
	{
		println(""+s);
	}
   
}
