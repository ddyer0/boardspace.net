/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package bridge;

import java.util.EventListener;
import java.util.Vector;

import lib.AwtComponent;
import lib.G;
import lib.SizeProvider;

import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

// our component is a codename1 container, so it can have popup menus as children
public class Component extends Container implements EventListener,AwtComponent,SizeProvider
{	public com.codename1.ui.Graphics getGraphics() { throw G.Error("Not implented, not implementedable"); }
	public Component getMediaComponent() { return this; }
	// mouse adapter converts CN1 events to 
	public MouseAdapter mouse = new MouseAdapter(this);
	public void addMouseMotionListener(MouseMotionListener who) { mouse.addMouseMotionListener(who); }
	public void addMouseListener(MouseListener who) { mouse.addMouseListener(who); }
	public void addMouseWheelListener(MouseWheelListener who) { mouse.addMouseWheelListener(who); }
	 
	public void removeMouseListener(MouseListener who) { mouse.removeMouseListener(who); }
	public void removeMouseMotionListener(MouseMotionListener who) { mouse.removeMouseMotionListener(who); }
	public void removeMouseWheelListener(MouseWheelListener who) { mouse.removeMouseWheelListener(who); }

	public void pointerReleased(int x,int y)
    {	if(mouse.pointerReleased(x,y)) {   	super.pointerReleased(x,y); } 
    }
    public void pointerDragged(int[] x, int[] y) 
    { 	if(mouse.pointerDragged(x,y))
    		{ super.pointerDragged(x,y);
    		}
    }
    public void pointerDragged(int x,int y)
    {	if(mouse.pointerDragged(x,y)) {   	super.pointerDragged(x,y); } 
    }
        
    public void pointerPressed(int x,int y)
    {
    	if(mouse.pointerPressed(x,y)) {   	super.pointerPressed(x,y); }
    }
    
	
	public void pointerHover(int x[],int y[])
	{	// as of 12/2022, this is called on the simulator but not on real devices
		//G.print("Component Hover ",x[0]," ",y[0]);
		if(mouse.pointerHover(x,y)) { super.pointerHover(x,y); }
	}
	
	Vector<WindowListener> listeners = null;
	public Dimension getMinimumSize() { return(new Dimension(100,100)); }
	

	public void dispose() 
	{ //G.print("Dispose "+this);
		closingChildren();	// make sure all children know they're going away
	}
	public void closingChildren()
	{	if(listeners!=null)
		{
		for(WindowListener l : listeners)
		  {
		  l.windowClosing(new WindowEvent(this));
		  }}
		for(int nc = getComponentCount()-1; nc>=0; nc--)
		{
			com.codename1.ui.Component c = getComponentAt(nc);
			if(c instanceof Component) { ((Component)c).closingChildren(); }
			
		}
	}
	public void addWindowListener(WindowListener myrunner) {
		if(listeners==null) { listeners = new Vector<WindowListener>(); }
		if(!listeners.contains(myrunner)) { listeners.addElement(myrunner); }
	}

	public Font getFont() 
		{ Style s = getStyle();
		  return G.getFont(s);
		}
	public Component getComponent() { return(this); }
	
	public Component(Layout x) { super(x); }
	public Component() 
	{ super(); 
	  //** warning 10/15/2021 turning this on had severe effects on the behavior of codename1 buttons
	  //** used in the tab bar on Android only.  The effect was that many taps or double taps
	  //** were needed to activate the button normally.   This was part of an experiment to 
	  //** support use of bluetooth keyboards, which was ababdoned for other reasons.
	  //setFocusable(true);
	}
	public lib.Image createImage(int w,int h) 
	{ return(lib.Image.createImage(w,h));
	}

	public void setBackground(Color c) { getStyle().setBgColor(c.getRGB()); }
	public void setForeground(Color c) { getStyle().setFgColor(c.getRGB());  }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }
	public void setFont(Font f) { getStyle().setFont(f); }
	public Dimension getSize() { return(getBounds().getSize()); }
	public Rectangle getBounds() { return super.getBounds(); }
	public void setSize(int size_x2, int size_y2) 
	{ 	setWidth(size_x2);
		setHeight(size_y2);
	}
	public void setLocation(Point componentLocation) 
	{ 	setX(componentLocation.getX());
		setY(componentLocation.getY());
	}
	public void setLocation(int x,int y)
	{	setX(x);
		setY(y);
	}
	public void setWidth(int w)
	{
		if(w!=getWidth()) { super.setWidth(w); setShouldLayout(true); }
	}
	public void setHeight(int h)
	{
		if(h!=getHeight()) { super.setHeight(h); setShouldLayout(true); }
	}
	public void setBounds(int l, int t, int w, int h) 
	{	setX(l);
		setY(t);
		setWidth(w);
		setHeight(h);
	}
	public void setBounds(Rectangle r)
	{
		setBounds(G.Left(r),G.Top(r),G.Width(r),G.Height(r));
	}
	public Rectangle actualGetBounds() { return(super.getBounds()); }
	
	public String isValid() { return("valid=?"); }
	public void validate()
	{ 	setVisible(false);
		setVisible(true);
		layoutContainer();
	}
	public void update() { 	}
	
	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(this))
		{ 
		  super.repaint();
		} 
	}
	public void repaint(int tm) { repaint(); }
	
	public boolean canRotate = false;
	public void paint(Graphics g)
	{	//useProxy=true;
		if(getHeight()<=0) { G.p1("too small "+getHeight()+this);}
		if(canRotate)
		{
		lib.Graphics g1 = lib.Graphics.create(g);
			boolean rotated = MasterForm.rotateCanvas(this, g1 );
		super.paint(g);
		if(rotated) { MasterForm.unrotateCanvas(this,g1); }
		}
		else
		{
			super.paint(g);
		}
		
	    //g.setColor(0xff);
		//g.drawRect(0,0,getWidth()-1,getHeight()-1);
		//g.drawLine(getWidth(),0,0,getHeight());
	}
	public void actualPaint(lib.Graphics g)
	{	
	}
	public void actualRepaint()
	{
		super.repaint();
	}
	public void remove(PopupMenu tmenu) 
	{ 	// this just needs to capture and ignore the add event.
		
	}
	public Container add(PopupMenu tmenu) 
	{ 	// this just needs to capture and ignore the add event.
		return(this);
	}
		
	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}

    public void addKeyListener(KeyListener who)
    {
    	MasterForm.getMasterForm().addKeyListener(who); 
    }
    public void removeKeyListener(KeyListener who)
    {
    	MasterForm.getMasterForm().removeKeyListener(who); 
    }
    public void paintBackgrounds(Graphics g)
    {	//System.out.println("Component background "+this);
    }
    public void paintComponentBackground(Graphics g)
    { //G.print("Component paintComponentBackground" );
    }
    @SuppressWarnings("deprecation")
	public static String getHierarchy(Container c)
    {
     	StringBuilder b = new StringBuilder();
    	while(c!=null)
    	{	b.append(c.getClass().getName()+" : ");
    		b.append(c.getX()+","+c.getY()+" "+c.getWidth()+"x"+c.getHeight()+"\n");
    		c = c.getParent();
    	}
    	return b.toString();
    }
}
