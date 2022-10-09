package lib;

import java.awt.Rectangle;

public interface SizeProvider
{
	public int getWidth();
	public int getHeight();
	public int getX();
	public int getY();
	public Rectangle getRotatedBounds();
}
