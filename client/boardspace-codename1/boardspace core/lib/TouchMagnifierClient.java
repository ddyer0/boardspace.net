package lib;

import bridge.Component;

public interface TouchMagnifierClient
{
	public int getWidth();

	public int getHeight();

	public MouseManager getMouse();

	public void drawClientCanvas(Graphics g2, boolean b, HitPoint hp);

	public Image getOffScreenImage();

	public int getSX();

	public int getSY();

	public Component getComponent();

	public void repaintForMouse(int fromNow, String string);
}
