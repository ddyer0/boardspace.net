package lib;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import bridge.Utf8Printer;

public class TextPrintStream extends Utf8Printer implements ShellProtocol
{	private AppendInterface area;
	public TextPrintStream(ByteArrayOutputStream out,AppendInterface a)
		{	super(out);	
			area = a;
			output = out;
	    }
	
	public TextPrintStream(ByteArrayOutputStream out,AppendInterface a,String enc) throws UnsupportedEncodingException 
	{	super(out,enc);	
		area = a;
		output = out;
    }
	
	static public TextPrintStream getPrinter(ByteArrayOutputStream out,AppendInterface a)
		{	try {
			 	return(new TextPrintStream(out,a,"UTF-8"));
			}
			catch (UnsupportedEncodingException e)
			{	return(new TextPrintStream(out,a));
			}
		}
		private ByteArrayOutputStream output = null;
		private void flushOutput()
		{
			flush();
			String msg = output.toString();
			output.reset();
			area.append(msg);
		}
		public void print(String s)
		{
			super.print(s);
			if((s!=null) && s.endsWith("\n"))
			{
				flushOutput();
			}
		}
		public void println(String s)
		{	super.println(s);
			flushOutput();
		}

		public void startShell(Object... args) {		}

		public void print(Object... msg) {
			if(msg!=null)
			{for(int i=0;i<msg.length;i++) 
			{ Object str = msg[i];
			  if(str==null) 
				{ str = "null"; 
				}
			super.print(str.toString()); 
			}}
		}
		public void println(Object... msg) {
			print(msg);
			println("");
		}
}