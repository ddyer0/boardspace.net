package bridge;

import java.util.EventListener;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

import lib.AwtComponent;
import lib.G;
import lib.MenuInterface;
import lib.NullLayoutProtocol;
import lib.SizeProvider;

// our component is a codename1 container, so it can have popup menus as children
public abstract class ProxyWindow implements SizeProvider,EventListener,AwtComponent,NullLayoutProtocol
{	public Graphics getGraphics() { throw G.Error("Not implented, not implementedable"); }
	protected ComponentProxy theComponent = new ComponentProxy(this);
	public ComponentProxy getComponent() { return(theComponent); }
	public Component getMediaComponent() { return theComponent; }
	public Font getFont() { return(theComponent.getFont()); }
	public void addMouseMotionListener(MouseMotionListener who) 
		{ theComponent.addMouseMotionListener(who); }
	public void addMouseListener(MouseListener who) 
		{ theComponent.addMouseListener(who); }
	public void addMouseWheelListener(MouseWheelListener who)
		{
		  theComponent.addMouseWheelListener(who);
		}
	public void removeMouseListener(MouseListener who)
	{
			theComponent.removeMouseListener(who);
	}
	public void addKeyListener(KeyListener who)
	{
		theComponent.addKeyListener(who);
	}
	public void removeKeyListener(KeyListener who)
	{
		theComponent.removeKeyListener(who);
	}
	public void addFocusListener(FocusListener who) 
	{ 	theComponent.addFocusListener(who); }
	public void removeFocusListener(FocusListener who)
	{
		theComponent.removeFocusListener(who);
	}


	public void removeMouseMotionListener(MouseMotionListener who)
	{
			theComponent.removeMouseMotionListener(who);
	}
	public void pointerHover(int[] x,int[] y) 
		{ theComponent.pointerHover(x,y); 
		}
	public boolean pinch(double f,int x,int y) {  return(false);};
	public FontMetrics getFontMetrics(Font f) { return(G.getFontMetrics(f)); }

	// constructor
	public ProxyWindow() {  }
	
	// methods to be overridden:
	public void paint(lib.Graphics g) 
	{ 
	  theComponent.actualPaint(g.getGraphics());
	}

	public void actualPaint(lib.Graphics g)
	{
		theComponent.actualPaint(g.getGraphics());
	}
	public void actualRepaint(int n)
	{
		theComponent.repaint(n);
	}
	public void actualRepaint()
	{
		theComponent.repaint();
	}
	// trampoline methods
	public Container add(Component c) { theComponent.add(c); return(null); }
	public void setVisible(boolean to) { theComponent.setVisible(to); }
	public void setLayout(Layout l) { theComponent.setLayout(l); }
	public void setEnabled(boolean b) {  theComponent.setEnabled(b); } 
	
	public Style getStyle() { return theComponent.getStyle(); }
	public void setSize(int size_x2, int size_y2) 
	{ 	theComponent.setWidth(size_x2);
		theComponent.setHeight(size_y2); 
	}


	public void setLocation(Point componentLocation) 
	{ 	theComponent.setX(componentLocation.getX());
		theComponent.setY(componentLocation.getY());
	}
	public void setLocation(int x,int y)
	{	theComponent.setX(x);
		theComponent.setY(y);
	}
	public void invalidate() { theComponent.invalidate(); }
	
	/*
	 * scroll and zoom are implemented locally, unknown to and independent
	 * of the codename1 window system.
	 */
	public int getX() { return(theComponent.getX()); }
	public int getY() { return(theComponent.getY()); }
	public int getWidth() { return(theComponent.getWidth()); }
	public int getHeight() { return(theComponent.getHeight()); }
	public Rectangle getBounds() { return new Rectangle(getX(),getY(),getWidth(),getHeight()); }
	public void setBounds(int l, int t, int w, int h) 
	{	theComponent.setX(l);
		theComponent.setY(t);
		theComponent.setWidth(w);
		theComponent.setHeight(h);
	}
	
	
	public lib.Image createImage(int w,int h) { return(lib.Image.createImage(w,h)); }
	public void setBackground(Color c) { getStyle().setBgColor(c.getRGB()); }
	public void setForeground(Color c) { getStyle().setFgColor(c.getRGB());  }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }
	
	public void setFont(Font f) { getStyle().setFont(f); }
	public Dimension getSize() { return(getBounds().getSize()); }
	public void setBounds(Rectangle r)
	{
		setBounds(G.Left(r),G.Top(r),G.Width(r),G.Height(r));
	}
	
	public String isValid() { return("valid=?"); }
	public void validate() { }
	public void update() { 	}
	
	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(theComponent))
		{ 
		  theComponent.repaint();
		} 
	}
	public void repaint(int tm) 
	{ theComponent.repaint(tm); 
	}
	
	public void remove(PopupMenu tmenu) 
	{ 	// this just needs to capture and ignore the remove event.	
	}
	public ProxyWindow add(PopupMenu tmenu) 
	{ 	// this just needs to capture and ignore the add event.
		return(this);
	}
	

	public void requestFocus(KeyListener l)
	{	theComponent.requestFocus();
		//MasterForm.getMasterForm().setFocused(l);
	}
	
	public void show(MenuInterface menu, int x, int y) throws AccessControlException {
		G.show(getComponent(),menu,x,y);
	}
	public String getName() { return theComponent.getName(); }
	public void setName(String n) { theComponent.setName(n); }
	public void remove(Component c) { theComponent.removeComponent(c); }
	public void removeThis() 
	{ 	com.codename1.ui.Container par = theComponent.getParent();
		if(par!=null) { par.removeComponent(getComponent()); }
	}
}