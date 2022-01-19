package bridge;

import lib.G;
import com.codename1.ui.Font;
import lib.Graphics;
import com.codename1.ui.geom.Rectangle2D;

//There are these fonts now: https://www.codenameone.com/blog/good-looking-by-default-native-fonts-simulator-detection-more.html

public class FontMetrics {

	Font myFont;
	public FontMetrics(Font f) { myFont = f; }
	public FontMetrics(Graphics g) { myFont =g.getFont(); }
	public static FontMetrics getFontMetrics(Component c) { return(new FontMetrics(G.getFont(c.getStyle()))); }
	public static FontMetrics getFontMetrics(SystemGraphics g) { return(new FontMetrics((Graphics)g)); }
	public static FontMetrics getFontMetrics(Graphics g) { return(new FontMetrics(g)); }
	public static FontMetrics getFontMetrics(Font g) { return(new FontMetrics(g)); }
	public Rectangle2D getStringBounds(String str, Graphics context)
	{
		return(new Rectangle2D(0,0,stringWidth(str),getHeight()));
	}
	public Rectangle2D getStringBounds(String str, int from,int to,Graphics context)
	{
		return(new Rectangle2D(stringWidth(str.substring(0,from)),
				0,
				stringWidth(str.substring(from,to)),
				getHeight()));
	}

	public Font getFont() { return(myFont); }
	public int getSize() { return(G.getFontSize(myFont)); }
	public int stringWidth(String str) { return(myFont.stringWidth(str)); }
	public int getHeight() { return(myFont.getHeight()); }
	public int getDescent() { return(myFont.getDescent()); }
	public int getAscent() { return(myFont.getAscent()); }
	public static Font deriveFont(Font f,int sz) { return(G.getFont(f,sz)); }
	public int getMaxAscent() 
		{ // the standard spec is a little fuzzy, it says
		  // "ascent" is the height for most alphanumeric characters,
		  // but some characters may be taller.  So we hope getAscent is 
		  // good enough
			return(myFont.getAscent());	
		}
}
