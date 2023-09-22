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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
//
// this is a byte stream where the bytes are expected to be utf-8 encoded,
// and the final toString will produce a proper unicode string from them.
//
public class Utf8OutputStream extends ByteArrayOutputStream 
{
	public String toString() 
	{	byte ba[] = toByteArray();
		String ss=null;
		try {
			ss = new String(ba, 0, ba.length, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			ss = super.toString();
		}
		return(ss);
	} 
	public String toString(boolean utf8)
	{
		return(utf8 ? toString() : super.toString());
	}
}
