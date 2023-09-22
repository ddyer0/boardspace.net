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
