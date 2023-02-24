package lib;

import java.awt.Component;

public interface TouchMagnifierClient extends SizeProvider
{
	public int getRotatedWidth();

	public int getRotatedHeight();

	public MouseManager getMouse();

	public void drawClientCanvas(Graphics g2, boolean b, HitPoint hp);

	public Image getOffScreenImage();

	public int getSX();

	public int getSY();

	public Component getComponent();

	public void repaintForMouse(int fromNow, String string);
}
