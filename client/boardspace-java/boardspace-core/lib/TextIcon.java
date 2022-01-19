package lib;

import bridge.Icon;
import bridge.IconBase;

import java.awt.Component;
import java.awt.FontMetrics;

/**
 * implement the Icon interface to be used in a menu.
 * 
 * @author Ddyer
 *
 */
public class TextIcon extends IconBase implements Icon 
{	
	Text originalText;
	int width;
	int height;
	public TextIcon(Component c,Text o) 
		{ originalText=o;
		  FontMetrics fm = G.getFontMetrics(c);
		  width = originalText.width(fm);
		  height = originalText.height(G.getFontMetrics(c));
		}
	public TextIcon(Text o,int w,int h) 
	{ originalText=o;
	  width = w;
	  height = h;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		int w = getIconWidth();
		int h = getIconHeight();
		originalText.draw(g, true, x,y,w,h,null, null);
	}

	public int getIconWidth() {
		return(width);
	}

	public int getIconHeight() {
		return(height);
	}
}
