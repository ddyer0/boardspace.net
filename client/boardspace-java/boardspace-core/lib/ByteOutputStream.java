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

public class ByteOutputStream 
{	private int size=0;
	private int lim;
	private byte data[];
	
	public ByteOutputStream()
	{
		this(32);
	}
	public ByteOutputStream(int siz)
	{
		lim = siz;
		data = new byte[siz];
	}
	private void expand()
	{	if(size>=lim)
		{	expand(lim*2+1);
		}
	}
	private void expand(int newsiz)
	{	if(lim<newsiz)
		{
		byte newdata[] = new byte[newsiz];
		for(int i= 0; i< lim; i++)  { newdata[i] = data[i]; }
		data = newdata;
		lim = newsiz;
		}
	}
	public byte[] toByteArray() {
		if (size>0)
		{
			byte newdata[] = new byte[size];
			for(int i= 0; i< size; i++)  { newdata[i] = data[i]; }
			return newdata;
		}
	return null;
	}

	public void write(int b)  {
		expand();
		data[size++] = (byte)b;
	}
	public void write(byte []b)
	{	write(b,0,b.length);
	}
	public void write(byte []b,int off0,int n)
	{	int newlen = size+n;
		int off = off0;
		expand(newlen);
		for(int i=0;i<n;i++) { data[size++] = b[off++]; }
		
	}
	public int size() {
		return(size);
	}
	public void reset() {
		size = 0;
	}
	
}