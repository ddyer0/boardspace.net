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
package vnc;

import lib.G;

public class ByteTile implements TileInterface
{	byte[] data;	
	int width;
	int height;
	int myx;  // to help debugging
	int myy;  // to help debugging
	public ByteTile(int x,int y,int w,int h) { myx=x; myy=y; width=w; height=h; data=new byte[width*height*3]; }
	
	/** a more efficient variant of update, to eliminate the extra data
	 * copy required to linearize the update data
	 */
	public boolean update(int[] newdata,int x,int y,int w,int h,int span)
	{	
		boolean dirty = false;
		int rowIdx = x+y*span;
		int didx = 0;
		for(int i=0;i<h;i++)
		{	int srcIdx = rowIdx;
			for(int j=0;j<w;j++)
			{
				int pixel = newdata[srcIdx++];
				
				// note that this dance of int 0xff and byte is delicate
				// bytes are signed so extracting a byte from a byte array
				// potentially results in a negative number.
				int r = (pixel&0xff);
				int g = ((pixel>>8)&0xff);
				int b = ((pixel>>16)&0xff);
				
				int or = data[didx]&0xff;
				if(or!=r) 
					{ data[didx] = (byte)r; 
					  dirty=true; 
					}
				didx++;
				
				int og = data[didx]&0xff;
				if(og!=g)
					{ data[didx]=(byte)g; 
					  dirty=true; 
					}
				didx++;
				
				int ob = data[didx]&0xff;
				if(ob!=b) 
					{ data[didx]=(byte)b;
					  dirty=true;
					}
				didx++;
			}
			rowIdx+=span;
		}
		return(dirty);
	}
		
	/** update the tile from a linearized array of new data */
	public boolean update(int[] newdata)
	{	int limit = width*height;
		boolean dirty = false;
		G.Assert(newdata.length>=limit, "wrong data length");
		for(int idx=0,didx=0;  idx<limit; idx++)
		{	int pixel = newdata[idx];
			
			// note that this dance of int 0xff and byte is delicate
			// bytes are signed so extracting a byte from a byte array
			// potentially results in a negative number.
			int r = (pixel&0xff);
			int g = ((pixel>>8)&0xff);
			int b = ((pixel>>16)&0xff);
			
			int or = data[didx]&0xff;
			if(or!=r) 
				{ data[didx] = (byte)r; 
				  dirty=true; 
				}
			didx++;
			
			int og = data[didx]&0xff;
			if(og!=g)
				{ data[didx]=(byte)g; 
				  dirty=true; 
				}
			didx++;
			
			int ob = data[didx]&0xff;
			if(ob!=b) 
				{ data[didx]=(byte)b;
				  dirty=true;
				}
			didx++;
		}
		return(dirty);
	}
	/**
	 * direct update with raw rgb data
	 * @param newdata
	 * @return true if the data is known to have changed
	 */
	public boolean update(byte[]newdata)
	{	int limit = width*height*3;
		boolean dirty = false;
		G.Assert(newdata.length>=limit,"incorrect data length");
		for(int idx=0; idx<limit; idx++)
		{	byte r = newdata[idx];			
			byte or = data[idx];
			if(or!=r) { data[idx] = r; dirty=true; }
		}
		return dirty;
	}

	public byte[] getData() {
		return(data);
	}

	public int getHeight() {
		return(height);
	}

	public int getWidth() {
		return(width);
	}

}
