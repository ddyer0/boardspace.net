package lib;

import bridge.Color;
import bridge.FontMetrics;
import com.codename1.ui.Font;


public interface AwtComponent extends SizeProvider
{
	public Font getFont();
	public Color getBackground();
	public Color getForeground();
	public FontMetrics getFontMetrics(Font f);
}
