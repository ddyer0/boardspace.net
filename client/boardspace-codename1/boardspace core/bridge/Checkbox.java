package bridge;

import com.codename1.ui.Font;

import lib.AwtComponent;
import lib.G;

public class Checkbox extends com.codename1.ui.CheckBox implements ActionProvider,AwtComponent
{	private final MouseAdapter mouse = new MouseAdapter(this);
	public void addItemListener(ItemListener m) {mouse.addItemListener(m); }
	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(this))
		{ 
		  super.repaint();
		} 
	}
	public Font getFont() { return(G.getFont(getStyle())); }
	public Checkbox(String string, boolean b) { super(string); setSelected(b); }
	public Checkbox(boolean b) { super();  setSelected(b); }
	public void setBackground(Color color) { getStyle().setBgColor(color.getRGB()); }
	public void setForeground(Color color) { getStyle().setFgColor(color.getRGB()); }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	public void setState(boolean b) { setSelected(b); }
	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}
	
}
