package bridge;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import lib.FontManager;
import lib.G;

public class SystemFont implements Config
{	public static int defaultFontSize = Default.getInt(Default.fontsize);
public static final FontManager.Style MenuTextStyle = FontManager.Style.Plain;

	public static Font defaultFont = null;
	public static int PLAIN = Font.PLAIN;
	public static int ITALIC = Font.ITALIC;
	public static int BOLD = Font.BOLD;

	public static int defaultFontSize() 
	{ 
		return Math.max(minFontHeight,Math.min(maxFontHeight,FontManager.standardizeFontSize(defaultFontSize)));
	}

	public static Font getGlobalDefaultFont()
	{
		if(defaultFont==null)
		{
			setGlobalDefaultFont();
		}
		return(defaultFont);
	}

	public SystemFont() {}
	
	public static String[] getFontFamilies()
	{	
	    return(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
	}

	public static Font menuFont()
	{
		return getFont(FontManager.getGlobalDefaultFont(),
				FontManager.MenuTextStyle,
				FontManager.standardizeFontSize(G.MenuTextSize*G.getDisplayScale()));
	}
	public static  Font getFont(String family,FontManager.Style style,int size)
	{	if(!G.Advise(size>0,"not zero size font %s %s",family,style)) { size = 1; }
		return(new Font(family, style.s ,size));
	}
	public static Font getFont(Font f,int size)
	{	if(!G.Advise(size>0,"not zero size font %s",f)) { size = 1; }
		return(f.deriveFont(f.getStyle(),size));
	}
	public static  Font getFont(Font f,FontManager.Style style,int size)
	{	if(!G.Advise(size>0,"not zero size font %s",f)) { size = 1; }
		return(f.deriveFont(style.s,size<=0?f.getSize():size));
	}

	@SuppressWarnings("deprecation")
	static public FontMetrics getFontMetrics(Font f)
	   {	
		   return(f==null ? null : Toolkit.getDefaultToolkit().getFontMetrics(f));
	   }

	static public FontMetrics getFontMetrics(Component c)
	   {
		   return(getFontMetrics(c.getFont()));
	   }
	public static int getFontSize(Font f) { return(f.getSize()); }

	/**
	 * set font as the global default font
	 * @param font
	 */
	public static void setGlobalDefaultFont(Font font)
	{	defaultFont = font;
		FontUIResource f = new javax.swing.plaf.FontUIResource(font);
	    java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
	    while (keys.hasMoreElements())
	    {
	        Object key = keys.nextElement();
	        Object value = UIManager.get(key);
	        if (value instanceof javax.swing.plaf.FontUIResource)
	        {
	            UIManager.put(key, f);
	        }
	    }
	}

	/**
	 * set a global default font scaled to the size of the and resolution of the screen
	 */
	public static void setGlobalDefaultFont()
	{	int fontHeight = defaultFontSize();
		Font f = getFont(FontManager.defaultFontFamily()/*"Arial Unicode MS"/*"sansserif"*/, FontManager.Style.Plain, fontHeight);
		setGlobalDefaultFont (f);
	}


	public static double adjustWindowFontSize(int w,int h)
	{	// this allows the fonts to grow when the windows get larger
		double wfac = w/(double)standardWindowWidth;
		double hfac = h/(double)standardWindowHeight;
		double adj = Math.sqrt(Math.min(wfac,hfac));
		return(adj);
	}

}
