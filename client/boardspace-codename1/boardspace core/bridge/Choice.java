package bridge;

import lib.AwtComponent;
import lib.G;

import com.codename1.ui.ComboBox;
import com.codename1.ui.Font;

public class Choice<TYPE> extends ComboBox<TYPE> implements AwtComponent , ActionProvider
{
	MouseAdapter mouse = new MouseAdapter(this);
	public void addItemListener(ItemListener m) {mouse.addItemListener(m); }
	public void addActionListener(ActionListener m) { mouse.addActionListener(m); }
	public Font getFont() { return(G.getFont(getStyle())); }	
	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(this))
		{ 
		  super.repaint();
		} 
	}
	public void select(TYPE string) {
		int nitems = size();
		for(int i=0;i<nitems;i++)
		{
			if(string.equals(getModel().getItemAt(i))) { setSelectedIndex(i); break; }
		}
		
	}
	public void add(TYPE string) {
		addItem(string);		
	}

	public void setForeground(Color c) { getStyle().setFgColor(c.getRGB()); }
	public void setBackground(Color c) { getStyle().setBgColor(c.getRGB()); }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	public void select(int index) { setSelectedIndex(index);	}
	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}

}
