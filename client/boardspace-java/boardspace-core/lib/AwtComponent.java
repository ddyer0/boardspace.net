package lib;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;


public interface AwtComponent
{
	public Font getFont();
	public Color getBackground();
	public Color getForeground();
	public FontMetrics getFontMetrics(Font f);
}
