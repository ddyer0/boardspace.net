package lib;


import bridge.SystemFont;
import common.CommonConfig.Default;

public class Font extends SystemFont {

	public static String defaultFontFamily = "Arial Unicode MS";

	public static String defaultFontFamily() { 	return defaultFontFamily; }

	public static void setDefaultFontFamily(String n) { defaultFontFamily = n; }

	public static int defaultFontSize = Default.getInt(Default.fontsize);

	public static void setDefaultFontSize(int n)
	{	defaultFontSize = Math.max(6, n);
		Default.setInt(Default.fontsize,defaultFontSize);
	}

}
