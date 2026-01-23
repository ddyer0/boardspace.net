package bridge;

import java.util.Hashtable;

import com.codename1.ui.Font;

import common.CommonConfig.Default;
import lib.G;

public class SystemFont
{
	public static int defaultFontSize = Default.getInt(Default.fontsize);
	public static final Style MenuTextStyle = Style.Plain;

	public enum Style
	   {   Plain(Font.STYLE_PLAIN),
		   Italic(Font.STYLE_ITALIC),
		   Bold(Font.STYLE_BOLD);
		   int s;
		   Style(int style) { s=style;}
	   }

	static int fontcount = 0;
	static Hashtable<Integer,Font> derivedFont = new Hashtable<Integer,Font>();
	static Hashtable<Integer,Font> uidFont = new Hashtable<Integer,Font>();
	static Hashtable<Font,Integer> fontUid = new Hashtable<Font,Integer>();
	public static Hashtable<Font,String> fontOrigin = new Hashtable<Font,String>();
	public static Hashtable<Font,Integer> fontSize = new Hashtable<Font,Integer>();
	public SystemFont() {};
	/**
	 * get the font from a style object, and try to assure that
	 * the result has a known pixel size.
	 * @param style
	 * @return
	 */
	public static Font getFont(com.codename1.ui.plaf.Style style)
	{	Font f = style.getFont();
		double sz = GetPixelSize(f);
		if(sz<=0)
		{	boolean isttf = f.isTTFNativeFont();
			if(isttf && sz==-1)
				{ int oldh = f.getHeight();
				  // this papers over a bug where a font with size 0 is stuck in the cache
				  Font f1 = deriveFont(f,oldh,f.getStyle());
				  if(f1==f) { f1=deriveFont(f,oldh+1,f.getStyle()); }
				  f = f1;
				}
			else {
			String bad = "Unregistered font "+f
				+ " ttf="+isttf
				+ " h=" + f.getHeight()
				+ " s=" + f.getSize()
				+ " px=" + f.getPixelSize()
				;
			G.print(bad);
			f = getGlobalDefaultFont();
			style.setFont(f);
			}
		}
		return(f);
	}
	public static int getFontSize(Font f)
	{	double fs = GetPixelSize(f);
		if(fs>0) { return((int)fs); }
		
		int sz = fontSize.containsKey(f) ? fontSize.get(f) : -1;
		if(!G.Advise(sz>=0,"Unregistered font %s %s %s",f,f.isTTFNativeFont(),fs))
			{ sz = 1; }
		return(sz);
	}
	static Font defaultFont = null;
	public static void setGlobalDefaultFont(Font f)
	{	defaultFont = f;
	}
	public static Font getGlobalDefaultFont()
	{
		if(defaultFont==null) 
		{ defaultFont = getFont("fixed", Style.Plain, defaultFontSize());
		}
		return(defaultFont);
	}
	public static int defaultFontSize() { return (int)(14*G.getDisplayScale()); }
	public static  Font getFont(Font f,Style style,int size)
	{	if(!G.Advise(size>0,"not a zero size font")) { size = 1; }
		Font fd = deriveFont(f,size<=0?getFontSize(f):size,style.s);
		if(GetPixelSize(fd)==size) { return(fd); }
		fontSize.put(fd,size);
		return(fd);
	}
	public static Font getFont(Font f,int size)
	{	if(!G.Advise(size>0,"not a zero size font")) { size = 1; }
		Font fd = deriveFont(f,size,f.getStyle());
		if(GetPixelSize(fd)==size) { return(fd); }
		fontSize.put(fd,size);
		return(fd);
	}
	// a lot of trouble is caused because codename1 doesn't reliably cache derived fonts
	public static Font deriveFont(Font f, int size, int style)
	{	int code = derivedFontCode(f,size,style);
		Font derived = derivedFont.get(code);
		if(derived==null)
		{
		derived = f.derive(size,style);
		derivedFont.put(code,derived);
		}	
		return derived;
	}
	static int getUid(Font f)
	{	synchronized (fontUid)
		{
		Integer id = fontUid.get(f);
		if(id==null)
		{
			id = fontcount++;
			fontUid.put(f,id);
			uidFont.put(id,f);
		}
		return id;
		}
	}
	static int derivedFontCode(Font f, int size, int style)
	{
		return getUid(f)+style*0x10000+size*0x100000;
	}
	public static Font getFont(String family,Style style,int size)
	{	
		if(!G.Advise(size>0,"not a zero size font")) { size = 1; }
		Font f = Font.createSystemFont(fontFaceCode(family),style.s,size);
		if(GetPixelSize(f)==size) 
			{ return(f); 
			}
		return(getFont(f,size));	// convert to a truetype font
		//return(new FontManager(0, style ,size));
	}
	static int fontFaceCode(String spec)
	   {
		   if ("monospaced".equalsIgnoreCase(spec)) { return Font.FACE_MONOSPACE; }
		   if ("serif".equalsIgnoreCase(spec)) { return Font.FACE_PROPORTIONAL; }
		   return Font.FACE_SYSTEM;
	   }
	
	public static com.codename1.ui.Font menuFont()
	{
		return getFont(getGlobalDefaultFont(),
				lib.FontManager.MenuTextStyle,
				lib.FontManager.standardizeFontSize(G.MenuTextSize*G.getDisplayScale()));
	}

	static public FontMetrics getFontMetrics(ProxyWindow c) 
	   {
		   return(getFontMetrics(getFont(c.getStyle())));
	   }

	static public FontMetrics getFontMetrics(bridge.Component c) 
	   {
		   return(getFontMetrics(getFont(c.getStyle())));
	   }

	static public FontMetrics getFontMetrics(Font f)
	   {
		   return(f==null ? null : FontMetrics.getFontMetrics(f));
	   }


	public static void setGlobalDefaultFont()
	{
		setGlobalDefaultFont(getGlobalDefaultFont());
	}

	public static double adjustWindowFontSize(int w,int h)
	{	// on IOS platforms, everything starts scaled to full screen
		return(1.0);
	}
	private static double GetPixelSize(Font f)
	{	
			return(f!=null && f.isTTFNativeFont() ? getTTFsize(f) : -1);
	}
	private static double getTTFsize(Font f)
	{	double siz = f.getPixelSize();
		if(siz<=0)
		{
		// try hard to identify the true size of the font.  This is necessitated
		// by codename1 returning the initial font object whose pixel size is
		// actually unknown.
		int originalHeight = f.getHeight();
		int requestedHeight = originalHeight;
		int style = f.getStyle();
		  // this papers over a bug where a font with size 0 is stuck in the cache
		Font f1 = deriveFont(f,requestedHeight,f.getStyle());
		if(f1==f) { requestedHeight++; f1=deriveFont(f,requestedHeight,style); }
		while(f1.getHeight()>originalHeight) 
			{ requestedHeight--; 
			  f1 = deriveFont(f,requestedHeight, style);
			}
		while(f1.getHeight()<originalHeight)
			{ requestedHeight--;
			  f1 = deriveFont(f,requestedHeight,style);
			}
		siz = f1.getPixelSize();
		fontSize.put(f,requestedHeight);
		fontOrigin.put(f,"getTTFsize");
		}
		return(siz);
	}
}
