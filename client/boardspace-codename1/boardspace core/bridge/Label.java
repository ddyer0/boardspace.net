package bridge;

import lib.AwtComponent;
import lib.G;
import com.codename1.ui.Font;
import lib.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;

public class Label extends com.codename1.ui.Label implements AwtComponent
{	
	public Icon icon;		// java style icon
	public Label(String string) 
	{	super(string);
	 	setText(string);
	}

	public Label(Icon ic)
	{   super();
		icon = ic;
	}
	public Label(Image ic)
	{
		super(ic.getImage());
	}
	public Font getFont() { return(G.getFont(getStyle())); }
	public void setLabelFor(JPasswordField passwordField) {
		
	}

	public void setLabelFor(Choice<String> langField) {
		
	}
	public void setText(String n)
	{
		if(n==null) { n = ""; }
		if(!n.equals(getText())) { super.setText(n);  }
	}
	
	public void repaint() 
	{ if(MasterForm.canRepaintLocally(this))
		{
		  super.repaint();
		}
	}

	@SuppressWarnings("deprecation")
	public Label(String string, int center) 
	{ super(string);
	  setAlignment(center); 
	}
	
	public void setBackground(Color c) { getStyle().setBgColor(c.getRGB()); }
	public void setForeground(Color c) { getStyle().setFgColor(c.getRGB()); }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	public void setBounds(int l, int t, int w, int h) 
	{	setWidth(w); 
		setHeight(h);
		setX(l);
		setY(t); 
	}

	public void setSize(int w, int h) 
	{	setWidth(w); setHeight(h); 
	}
	public void setLocation(Point s) {
		setX(s.getX()); 
		setY(s.getY()); 
	}
	public Dimension getPreferredSize()
	{
		if(icon!=null)
		{
			return new Dimension(icon.getIconWidth(),icon.getIconHeight());
		}
		else
		{
			return super.getPreferredSize();
		}
	}
	public void paint(com.codename1.ui.Graphics g)
	{
		if(icon!=null)
		{
			icon.paintIcon(this,lib.Graphics.create(g),getWidth(),getHeight());
		}
		else
		{
		super.paint(g);
		}
	    //g.setColor(0xd0ff);
		//g.drawRect(0,0,getWidth()-1,getHeight()-1);
		//g.drawLine(getWidth(),0,0,getHeight());
	}

	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}
	public Rectangle getRotatedBounds() { return getBounds(); }
}
