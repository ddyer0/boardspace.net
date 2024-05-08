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

import com.codename1.ui.Graphics;
import com.codename1.ui.layouts.Insets;
import com.codename1.ui.layouts.Layout;

public class Container extends Component
{
	public Insets getInsets() { return(new Insets(0,0,0,0)); }
	
	public Container(Layout x) { super(x);  }
	public Container() { super();  }
	public void validate() {}

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
		
	public void supadd(com.codename1.ui.Component c) 
	{	try { super.add(c); 
			setShouldCalcPreferredSize(true);
		}
		catch (Throwable err) { Http.postError(this,"adding component "+c,err); }
	}
	
	public void addC(com.codename1.ui.Component c)
	{ 
		G.runInEdt(new Runnable() { public void run() { supadd(c); }});
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
		G.runInEdt(new Runnable() { public void run() { supadd(index,c); }}); 
	}
	public void paintBackgrounds(Graphics g)
	{
		//System.out.println("backgrounds "+this);
	}
	public void supadd(int index,com.codename1.ui.Component c )
	{
		super.add(index,c);
	}
	public void remove(com.codename1.ui.Component c)
	{	removeComponent(c);
	}
	
	public Component getComponent(int i) { return((Component)getComponentAt(i)); }
	
	public int getComponentIndex(JMenu m)
	{
		return m.getComponentIndex(m);
	}
	public int getComponentIndex(Component m)
	{
		return m.getComponentIndex(m.getComponent());
	}

	public void add(ProxyWindow c) { addC(c.getComponent()); }
	public com.codename1.ui.Container add(com.codename1.ui.Component c)
	{	G.Assert(!G.debug() || G.isEdt(),"should be edt");
		supadd(c);
		return this;
	}
	 public void paintComponentBackground(Graphics g)
	 {	//System.out.println("container paintcomponentbackground");
	 }
	 
}
