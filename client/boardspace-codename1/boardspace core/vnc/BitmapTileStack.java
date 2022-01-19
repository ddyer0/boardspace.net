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
