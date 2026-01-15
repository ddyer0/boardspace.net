package lib;


import bridge.SystemFont;

public class FontManager extends SystemFont {

	public static String defaultFontFamily = "Arial Unicode MS";

	public static String defaultFontFamily() { 	return defaultFontFamily; }

	public static void setDefaultFontFamily(String n) { defaultFontFamily = n; }

	public static void setDefaultFontSize(int n)
	{	defaultFontSize = Math.max(6, n);
		Default.setInt(Default.fontsize,defaultFontSize);
	}

	/**
	 * standardize font sizes based on the screen dots per inch.  Literal
	 * sizes in the code are are for a 96 dpi screen, where 12 point is a good
	 * default.  On retina screens that has to be increased a lot.
	 * @param sz
	 * @return the adjusted font size
	 */
	public static int standardizeFontSize(double sz)
	{	
		return((int)(G.getDisplayScale()*sz));
	}


}
