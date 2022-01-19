package bridge;

import java.awt.image.ImageObserver;

import lib.Image;

public abstract class ImageUpdateProxy implements ImageObserver 
{
    public boolean imageUpdate(Image img, int infoflags, int x, int y,
            int width, int height)
        {
            boolean val = (infoflags & ImageObserver.ALLBITS) == 0;
            return (val);
        }
	
	public boolean imageUpdate(java.awt.Image img, int infoflags, int x, int y,
	        int width, int height)
	{
		return imageUpdate(Image.createImage(img,"temp for imageupdateproxy"),infoflags,x,y,width,height);
	}
}
