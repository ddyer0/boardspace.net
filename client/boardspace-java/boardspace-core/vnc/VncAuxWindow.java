package vnc;

import java.awt.Component;

import online.common.exCanvas;

public class VncAuxWindow extends OffscreenWindow
{
	exCanvas parent = null;
	public Component getMediaComponent() { return(parent.getMediaComponent()); }
	public VncServiceProvider newInstance() 
	{ return(this);	
	}

	public VncAuxWindow(exCanvas parentWindow,String name,int x,int y,int w,int h)
	{	
		super(name,x,y,w,h);
		parent = parentWindow;
	}

	
	public void redraw() {
		
	}


}
