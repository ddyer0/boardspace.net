package bridge;

import com.codename1.ui.Graphics;

import lib.NullLayout;
import lib.NullLayoutProtocol;

// 
// this window is the "real" window, paired with a "fake" ProxyWindow which is not connected
// directly to the window system.
//
public class ComponentProxy extends Component implements FullScreen,NullLayoutProtocol
{	ProxyWindow client;
	
	public ComponentProxy(ProxyWindow c) 
	{ 	setLayout(new NullLayout(this));
	    client = c;
	    //for some reason this is a really bad idea, all the codename1 buttons stop working.
	    // setFocusable(true);
	    mouse.setAwtComponent(c); 
	}
	public void doNullLayout(Container p)
	{
		client.doNullLayout(p);
	}
	public boolean pinch(double f,int x,int y) 
	{ 
	  return client.pinch(f,x,y); 
	}

	public void actualPaint(Graphics g) 
	{ super.paint(g);  // callback to continue the original paint
	}
	//
	// This enables subtle drag behavior, inconjunction with the global 
	// Display.getInstance().setDragStartPercentage(1)
	//
	public int getDragRegionStatus(int x, int y) { return(Component.DRAG_REGION_LIKELY_DRAG_XY); }

	public void paint(Graphics g)
	{	
		boolean rotated = MasterForm.rotateNativeCanvas(this,g);
		client.paint(lib.Graphics.create(g));
		if(rotated) { MasterForm.unrotateNativeCanvas(this, g); }
	}

	public void addFocusListener(FocusListener who) {
		mouse.addFocusListener(who);
		
	}
	public void removeFocusListener(FocusListener who) {
		mouse.removeFocusListener(who);	
	}

}
