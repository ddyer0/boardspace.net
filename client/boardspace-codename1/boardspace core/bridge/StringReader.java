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

import java.io.IOException;
import java.io.Reader;

public class StringReader extends Reader  {
	private String string;
	private int index = 0;
	public StringReader(String str) { string = str; index = 0; }
	@Override
	public void close() throws IOException {}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int lim = string.length();
		int count = 0;
		int dest = off;
		while(len-- > 0 && index<lim) { cbuf[dest++]=string.charAt(index++); count++; }
		return(count==0?-1:count);
	}

}
