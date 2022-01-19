package bridge;
import java.awt.Component;
import java.awt.Graphics;

public abstract class IconBase implements Icon
{	
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		paintIcon(c,lib.Graphics.create(g),x,y);
	}
}
	
