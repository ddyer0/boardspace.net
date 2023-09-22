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

import lib.Image;
import lib.OStack;

public class BitmapTileStack extends OStack<BitmapTile>
{
	public BitmapTile[] newComponentArray(int sz) {
		return new BitmapTile[sz];
	}

	public void setRGB(Image im)
	{	
		for(int i=0,lim=size(); i<lim; i++)
		{
			BitmapTile b = elementAt(i);
			im.setRGB(b.to_x, b.to_y,b.width,b.height, b.data,0,b.width);
		}
	}
	
}
