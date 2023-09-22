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
import java.io.OutputStream;

import com.codename1.io.FileSystemStorage;

public class FileOutputStream extends OutputStream {
	OutputStream stream;
	public FileOutputStream(File file) throws IOException
	{	String p =file.getPath();
		//G.print("open output file "+p);
		stream = FileSystemStorage.getInstance().openOutputStream(p);
	}

	public FileOutputStream(String zipname) throws IOException
	{ 	String p = zipname;
		//G.print("open output string "+p);
		stream = FileSystemStorage.getInstance().openOutputStream(p);
	}

	public void write(int b) throws IOException {
		stream.write(b);
	}
	// subtle point - if close is not supplied, close on the real stream
	// won't be called either.
	public void close() throws IOException { stream.close(); }
	// for efficiency, call write methods of the real stream
	// rather than allowing defaults to call our simple method
	public void write(byte[]b) throws IOException { stream.write(b); }
	public void write(byte[] b, int off, int len) throws IOException { stream.write(b,off,len); }

}
