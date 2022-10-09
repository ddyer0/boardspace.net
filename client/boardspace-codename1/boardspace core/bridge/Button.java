package bridge;

import lib.AwtComponent;
import lib.G;

import com.codename1.ui.Command;
import com.codename1.ui.Font;
import lib.Image;

import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;

public class Button extends com.codename1.ui.Button implements ActionProvider,AwtComponent
{	private final MouseAdapter mouse = new MouseAdapter(this);
	public void addActionListener(ActionListener m) { mouse.addActionListener(m); }
	public Button(Image image) { super(image.getImage()); }
	public Button(String label)
	{ super(label);
	}
	public void setBackground(Color color) { getStyle().setBgColor(color.getRGB()); }
	public void setForeground(Color color) { getStyle().setFgColor(color.getRGB()); }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	static {
		// this fixes the mysterious button ALL CAPS behavior that appeared 
		// on android on 9/2017
		setCapsTextDefault(false);
	}
	public Font getFont() { return(G.getFont(getStyle())); };
	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(this))
		{ 
		  super.repaint();
		} 
	}
	public Point getLocation() { return(new Point(getX(),getY())); }
	public void setFont(Font f) { getStyle().setFont(f); }
	public String getLabel() { return(getText()); }
	public void setLabel(String t) { setText(t);  }
	
	public void setActionCommand(String oK) {
		setCommand(new Command(oK));
	}
	public String getActionCommand() 
		{ Command com = getCommand();
		  return((com==null) ? null : com.toString());
		}
	public void paint(com.codename1.ui.Graphics g)
	{
		super.paint(g);
	    //g.setColor(0xd000ff);
		//g.drawRect(0,0,getWidth()-1,getHeight()-1);
		//g.drawLine(getWidth(),0,0,getHeight());
	}
	public void setBounds(int l,int t,int w,int h) 
	{ setX(l);
	  setY(t);
	  setWidth(w);
	  setHeight(h);
	}

	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}
	public void pointerPressed(int x,int y)
	{	if(contains(x,y)) { x = getX()+1; y=getY()+1; }
		super.pointerPressed(x,y);
	}
	public void pointerReleased(int x,int y)
	{	if(contains(x,y)) { x = getX()+1; y=getY()+1; }
		super.pointerReleased(x,y);
	}
	public Rectangle getRotatedBounds() { return getBounds(); }
}	
