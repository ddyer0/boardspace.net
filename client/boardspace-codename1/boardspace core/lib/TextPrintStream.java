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