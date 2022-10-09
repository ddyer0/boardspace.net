package lib;

import com.codename1.ui.geom.Rectangle;

public interface SizeProvider 
{
	public int getWidth();
	public int getHeight();
	public int getX();
	public int getY();
	public Rectangle getRotatedBounds();
}
