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

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This modifies "Tokenizer" to input directly from a stream, using readline as a buffer
 * 
 * @author ddyer
 *
 */
public class StreamTokenizer extends Tokenizer
{	
	BufferedReader stream;
	
	public StreamTokenizer(BufferedReader in,String del)
	{	super(null,del);
		stream = in;
	}
	public StreamTokenizer(BufferedReader in)
	{	super(null);
		stream = in;
	}
	private String readline()
	{
		try {
			return stream.readLine();
		}
		catch (IOException e)
		{
			return null;
		}
	}
	public boolean hasMoreElements()
	{	while(!super.hasMoreElements())
		{
		String line = readline();
		if(line!=null) { reload(line); }
		else { return false; }
		}
		return true;
	}
	public String nextElement()
	{	String s = super.nextElement();
		if(s==null && hasMoreElements()) { s = super.nextElement(); }
		return s;
	}

}
