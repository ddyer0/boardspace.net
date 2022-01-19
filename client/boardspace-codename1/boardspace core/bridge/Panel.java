package bridge;

import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

import lib.G;

public class Panel extends Container
{
	public Panel(Layout flowLayout) { super(flowLayout);	}
	public Panel() { super(); }
	public Dimension getMinimumSize() { return(new Dimension(10,10)); }
	public void setBoundsEdt(int l,int t,int w,int h)
	{
		super.setBounds(l,t,w,h);
	}
	public void setBounds(int l,int t,int w,int h)
	{
		G.runInEdt(new Runnable(){ public void run() { setBoundsEdt(l,t,w,h); }});
	}
}
