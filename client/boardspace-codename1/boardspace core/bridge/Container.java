package bridge;

import lib.G;
import lib.Http;

import com.codename1.ui.Graphics;
import com.codename1.ui.layouts.Insets;
import com.codename1.ui.layouts.Layout;

public class Container extends Component
{
	public Insets getInsets() { return(new Insets(0,0,0,0)); }
	
	public Container(Layout x) { super(x); }
	public Container() { super(); }
	public void validate() {}
	public void layoutContainer()
	{
		try {
			super.layoutContainer();
		}
    	catch (ThreadDeath err) { throw err;}
		catch (Throwable err)
		{
			Http.postError(this,"error in layoutContainer",err);
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
		
	public void supadd(com.codename1.ui.Component c) 
	{	try { super.add(c);  }
		catch (Throwable err) { Http.postError(this,"adding component "+c,err); }
	}
	public com.codename1.ui.Container add(com.codename1.ui.Component c)
	{  	final com.codename1.ui.Component cc = c;
		G.runInEdt(new Runnable() { public void run() { supadd(cc); }});
		setShouldCalcPreferredSize(true);
	    return(this);
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
	public void addComponent(int index,com.codename1.ui.Component c)
	{
		final com.codename1.ui.Component cc = c;
		G.runInEdt(new Runnable() { public void run() { supadd(index,cc); }}); 
	}
	public void paintBackgrounds(Graphics g)
	{
		//System.out.println("backgrounds "+this);
	}
	public void supadd(int index,com.codename1.ui.Component c )
	{
		super.addComponent(index,c);
	}
	public void remove(com.codename1.ui.Component c)
	{	removeComponent(c);
	}
	
	public Component getComponent(int i) { return((Component)getComponentAt(i)); }
	
	public void add(ProxyWindow c) { add(c.getComponent()); }

	 public void paintComponentBackground(Graphics g)
	 {	//System.out.println("container paintcomponentbackground");
	 }
	 
}