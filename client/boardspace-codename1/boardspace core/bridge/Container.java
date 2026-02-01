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

import lib.G;
import lib.Http;

import java.util.Vector;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.Insets;
import com.codename1.ui.layouts.Layout;

public class Container extends com.codename1.ui.Container
{
	public Insets getInsets() { return(new Insets(0,0,0,0)); }
	
	public Container(Layout x) { super(x);  }
	public Container() { super();  }
	public void validate() {}
	public Rectangle getFrameBounds() { return getBounds(); }
	public void setFrameBounds(int l, int t, int w, int h) 
	{	setX(l);
		setY(t);
		setWidth(w);
		setHeight(h);
	}
	public void repaint(int tm) { repaint(); }
	public Object getMediaComponent() { return this; }
	public void setBackground(Color c) { getStyle().setBgColor(c.getRGB()); }
	public void setForeground(Color c) { getStyle().setFgColor(c.getRGB());  }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForegrund() { return(new Color(getStyle().getFgColor())); }
	public void setSize(int size_x2, int size_y2) 
	{ 	setWidth(size_x2);
		setHeight(size_y2);
	}
	Vector<WindowListener> listeners = null;
	public void addWindowListener(WindowListener myrunner) {
		if(listeners==null) { listeners = new Vector<WindowListener>(); }
		if(!listeners.contains(myrunner)) { listeners.addElement(myrunner); }
	}
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
			if(c instanceof Container) { ((Container)c).closingChildren(); }	
		}
	}
	public void windowActivated()
	{
		if(listeners!=null)
		{	
			for(WindowListener l : listeners)
			{
				l.windowActivated(new WindowEvent(this));
			}
		}
	}

	public void paint(Graphics g)
	{	try {
		if(MasterForm.canRepaintLocally(this))
		{
		if(isOpaque())
			{ 
			  g.setColor(getStyle().getBgColor());
			  g.fillRect(getX(),getY(),getWidth(),getHeight());
			}
		super.paint(g);

		}}
		catch (ThreadDeath err) { throw err;}
		catch (Throwable err)
		{
			Http.postError(this,"error in EDT paint",err);
		}
		}
		
	public void addSelf(com.codename1.ui.Component c)
	{
		G.runInEdt(new Runnable() { public void run() { add(c); }});
	}
	public void addC(com.codename1.ui.Component c)
	{ 
		G.runInEdt(new Runnable() { public void run() { add(c); }});
	}
	public void addC(String where,com.codename1.ui.Component c)
	{
		G.runInEdt(new Runnable() { public void run () { add(where,c); }});
	}
	public void suprem(com.codename1.ui.Component c) 
	{ 	try { c.setVisible(false); super.removeComponent(c); revalidate(); }
		catch (Throwable err) 
		{  // this can generate off errors from layout
			G.print(this,"removing componet "+c,err); 
		}
	}
	public void removeComponent(com.codename1.ui.Component c) 
	{	final com.codename1.ui.Component cc = c;
		G.runInEdt(new Runnable() { public void run() { suprem(cc); }});
	}
	public void addC(int index,com.codename1.ui.Component c)
	{
		G.runInEdt(new Runnable() { public void run() { add(index,c); }}); 
	}
	public void paintBackgrounds(Graphics g)
	{
		//System.out.println("backgrounds "+this);
	}

	public void remove(com.codename1.ui.Component c)
	{	removeComponent(c);
	}
	
	public Component getComponent(int i) { return((Component)getComponentAt(i)); }
	
	public void addc(ProxyWindow c) { add(c.getComponent()); }
	
	 public void paintComponentBackground(Graphics g)
	 {	//System.out.println("container paintcomponentbackground");
	 }
	 
}
