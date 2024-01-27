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
import lib.ImageConsumer;
import lib.NullLayout;
import lib.NullLayoutProtocol;
import lib.PinchEvent;

import com.codename1.ui.Component;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.Layout;

public class Frame extends Window 
		implements NullLayoutProtocol,MouseMotionListener,MouseListener,ImageConsumer
{	boolean resizable = false;
	public void setResizable(boolean n) { resizable = n; }
	Container glassPane = new FullscreenPanel();
	public String getTitle() { return(getName()); }

	public void setJMenuBar(JMenuBar m){}
	
	// tabname appears in the master frame, as the selectable name of the frame.
	public String tabName = null;
	public String tabName() { return(tabName); }
	public void setTabName(String g) { tabName = g; }
	public void init()
	{	super.setLayout((Layout)new NullLayout(this));
		setOpaque(true);
		glassPane.setSize(getWidth(),getHeight());
		super.addC(glassPane);
		MasterForm.getMasterPanel().addC(this);
	}
	public Frame() 
	{ 	super();
		init();
	}
	public Frame(String n) 
	{ 	super();
		tabName=n;
		init();
	}
	public void doNullLayout()
	{	int w = getWidth();
		int h = getHeight();
		setLocalBounds(0,0,w,h);
	}
	public void setLocalBounds(int x,int currentY,int w,int h)
	{	// note that unusually, currentY may not be zero
		if(glassPane!=null)
		{
		glassPane.setSize(new Dimension(w,h-currentY));
		glassPane.setX(0);
		glassPane.setY(currentY);
		}
		//else { G.Error("glasspane is null "+this+" "+w+"x"+h); }
	}

	public void addC(com.codename1.ui.Component c) 
	{  glassPane.addC(c); 
	}
	public void remove(com.codename1.ui.Component c)
	{	c.setVisible(false);
		glassPane.removeComponent(c);
	}
	public void setContentPane(Container newContentPane) 
	{	super.removeComponent(glassPane);
		super.addC(newContentPane);
		glassPane = newContentPane;	
	}
	public Container getContentPane()
	{
		return(glassPane);
	}
	public void dispose() 
	{	super.dispose();
		MasterPanel master = MasterForm.getMasterPanel();
		master.remove(this);
	}
	public void remove()
	{	MasterPanel master = MasterForm.getMasterPanel();
		master.remove(this);
	}
	public void setSize(int w,int h)
	{
		super.setSize(w, h);
		if(glassPane!=null) { glassPane.setBounds(0,0,w,h); }
	}

	public void setBounds(int x,int y,int w,int h) 
	{ 
		setSize(w,h);
		setX(x);
		setY(y);
	}
	
	public void showInEdt()
	{	try {
		super.setVisible(true);
		}
		catch (ThreadDeath err) { throw err;}
		catch (Throwable err)
		{
			 Http.postError(this,"show "+this,err);	
		}
	}
	public void setVisible(boolean vis)
	{	
		if(vis)
			{
			MasterForm.getMasterForm().show(); 
			if(!isVisible())
			{	G.runInEdt(
					new Runnable () 
						{	public void run() { showInEdt(); } });
				
			}
			MasterForm.getMasterPanel().repaint();}
	}
	
	public void show()
	{	setVisible(true);
	}
	
	public void pack() { }

	
	
	int dragStartX = 0;
	int dragStartY = 0;
	boolean dragging = false;
	public void mouseDragged(MouseEvent e) 
	{
		int x = e.getX();
		int y = e.getY();
		if(!dragging) 
			{ dragging = true;
			  dragStartX = x; 
			  dragStartY = y; 
			}
		else { int dx = x-dragStartX; 
			   int dy = y-dragStartY;
			   int posX = getX();
			   int posY = getY();
		
			   if(dx!=0 || dy!=0)
				   {setX(posX+dx);
				    setY(posY+dy);
				    MasterForm.getMasterPanel().repaint();
				   }
			}
	}
	
	public void setLocationRelativeTo(Object object) {
		Rectangle targetBounds = MasterForm.getMasterPanel().getBounds();
		if(object instanceof Component) { targetBounds = ((Component)object).getBounds(new Rectangle()); }
		int cx = targetBounds.getX()+targetBounds.getWidth()/2;
		int cy = targetBounds.getY()+targetBounds.getHeight()/2;
		int newx = cx - getWidth()/2;
		int newy = cy - getHeight()/2;
		setX(Math.max(0,newx));
		setY(Math.max(0,newy));
	
	}

	public void mouseMoved(MouseEvent e) {
		
	}
	public void mouseClicked(MouseEvent e) {
		
	}
	public void mousePressed(MouseEvent e) {
		dragging = false;
	}
	public void mouseReleased(MouseEvent e) {
		dragging = false;
	}
	public void mouseEntered(MouseEvent e) {
		
	}
	public void mouseExited(MouseEvent e) {
		
	}
	public void mousePinched(PinchEvent e) {
		
	}
	public void setLayout(LayoutManager x) {
		glassPane.setLayout((Layout)x); 
	}

	public void addC(String where, Component p) {
		glassPane.add(where,p);
	}
	public com.codename1.ui.Container add(String where,Component c)
	{	G.Error("not expected");
		return super.add(where,c);
	}
	public com.codename1.ui.Container add(Component c)
	{
		return glassPane.add(c);
	}

	public void setLowMemory(String string) {
	}

}
