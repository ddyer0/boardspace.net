package bridge;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;

import lib.G;

public class SystemFont 
{
	public enum Style
	   {   Plain(Font.PLAIN),
		   Italic(Font.ITALIC),
		   Bold(Font.BOLD);
		   int s;
		   Style(int style) { s=style;}
	   }
	Font font;
	public SystemFont() {}
	SystemFont(Font f)
	{
		font = f;
	}
	
	public static String[] getFontFamilies()
	{	
	    return(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
	}

	public static Font menuFont()
	{
		return SystemFont.getFont(G.getGlobalDefaultFont(),
				G.MenuTextStyle,
				G.standardizeFontSize(G.MenuTextSize*G.getDisplayScale()));
	}
	public static  Font getFont(String family,Style style,int size)
	{	if(!G.Advise(size>0,"not zero size font %s %s",family,style)) { size = 1; }
		return(new Font(family, style.s ,size));
	}
	public static Font getFont(Font f,int size)
	{	if(!G.Advise(size>0,"not zero size font %s",f)) { size = 1; }
		return(f.deriveFont(f.getStyle(),size));
	}
	public static  Font getFont(Font f,Style style,int size)
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


}
