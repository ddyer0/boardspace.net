package bridge;

import lib.AwtComponent;
import lib.G;

import com.codename1.ui.Font;
import com.codename1.ui.TextField;

public class JPasswordField extends TextField implements AwtComponent
{	MouseAdapter mouse = new MouseAdapter(this);
	public void addActionListener(ActionListener who) { mouse.addActionListener(who); }
	public Font getFont() { return(G.getFont(getStyle())); }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(this))
		{ 
		  super.repaint();
		} 
	}
	public JPasswordField(int i) 
		{ super(i);
	      setHint("Password");
	      setConstraint(TextField.PASSWORD);
		}

	public void setActionCommand(String oK) 
	{
	}


	public String getPassword() {
		return(getText());
	}
	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}

}
