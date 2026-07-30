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
package dictionary;

import lib.ByteOutputStream;
import lib.G;

public interface Entry extends ByteKey
{
	public void setDefinitionData(byte[]d,int idx,int off);
	public byte[] getDefinitionData();
	public int getDefinitionIndex();
	public int getDefinitionLength();
	public int getOrder();
	public void setOrder(int loaded);
	public long letterMask();
	public void setLetterMask(long v);
	
	public default int setDefinition(String words,ByteOutputStream f,ByteOutputStream d)
	{
		if(words==null) { setDefinitionData(null,0,0); return(0); }
		else
		{
			byte []dd = Smaz.compress(words,f,d);
			setDefinitionData(dd,0,dd.length);
			return dd.length;
		}
	}
	public int setCompressedDefinition(ByteOutputStream def, ByteOutputStream verb, ByteOutputStream data);
	
	public default String getDefinition()
	{	byte dd[] = getDefinitionData();
		if(dd==null) { return(null); }
		return(Smaz.decompress(dd,getDefinitionIndex(),getDefinitionLength()));
	}
	public default int sortOrder()
	{	int os = getOrder();
		return((os<=0)?Integer.MAX_VALUE : os);
	}
	public default int compareTo(Entry o) {
		return(G.signum(sortOrder() - o.sortOrder()));
	}
	
}
 	
