package lib;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;


public interface AwtComponent
{
	public Font getFont();
	public Color getBackground();
	public Color getForeground();
	public int getX();
	public int getY();
	public FontMetrics getFontMetrics(Font f);
}
