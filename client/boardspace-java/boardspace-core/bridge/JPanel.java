package bridge;

import java.awt.Component;
import java.awt.Container;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import lib.ImageConsumer;
import lib.NullLayout;
import lib.NullLayoutProtocol;

@SuppressWarnings({ "serial", "serial" })
public class JPanel extends Panel implements NullLayoutProtocol,ImageConsumer
{
	String title = "";
	private Container contentPane = null;
	public String getTitle() { return title; }
	public JPanel () { super(); init(); }
	public Rectangle getFrameBounds() { return getBounds(); }
	public JPanel(String p)
	{	super();
		title = p;
		init();
	}
	public void setOpaque(boolean v) {}
	public void setFrameBounds(int x,int y,int w,int h)
	{
		super.setBounds(x,y,w,h);
	}
	public void init()
	{	setLayout(new NullLayout(this));
	}
	public void addC(java.awt.Component p) {
		if(contentPane==null) { add(p); } else  { contentPane.add(p); }
	}

	public void addC(String where, Component p) {
		if(contentPane==null) { add(where,p); } else { contentPane.add(where,p); }
	}

	public Container getContentPane() {
		return contentPane;
	}

	public void setContentPane(Container p) {
		if(contentPane!=null) { remove(contentPane); }
		contentPane = p;
		add(p);
	}
	ArrayList<WindowListener>listeners = new ArrayList<WindowListener>();
	
	public void addWindowListener(WindowListener who) {
			listeners.add(who);
	}
	private void setClosing()
	{
		for(int i=0;i<listeners.size(); i++)
		{	WindowListener w = listeners.get(i);
			// this isn't really kosher, but acceptable to xframe
			w.windowClosing(null);
			
		}
	}
	public void setVisible(boolean vis)
	{	
		super.setVisible(vis);
		
		if(vis)
			{
			MasterForm mf =MasterForm.getMasterForm();
			if(!mf.isVisible()) 
				{ 
				mf.setVisible(true); 
				}
			// defer adding this panel to the master until it's supposed to be seen
			// doing this in the constructor caused mysterious "blank" windows that
			// could be fixed by window-level operations such as resizing or minimizing
			MasterPanel mp = MasterForm.getMasterPanel();
			if(mp.getComponentZOrder(this)<0)
				{ mp.addC(this);
				}
			}
		MasterForm.getMasterPanel().adjustTabStyles();
	}
	
	public void doNullLayout()
	{	int w = getWidth();
		int h = getHeight();
		setLocalBounds(0,0,w,h);
	}
	public void setLocalBounds(int x,int currentY,int w,int h)
	{	// note that unusually, currentY may not be zero	
		if(contentPane!=null) { contentPane.setBounds(0,currentY,w,h); }
		else {
			for(int i=0;i<getComponentCount();i++)
			{
				getComponent(i).setBounds(0,currentY,w,h);
			}
		}
	}
	
	public void dispose() 
	{	setClosing();
		Container p = getParent();
		if(p!=null) { p.remove(this); }
		
		
	}
	public void revalidate()
	{
		super.revalidate();
		doLayout();
		if(contentPane!=null) { contentPane.doLayout(); }
	}
	public void setLowMemory(String string) {
	}
	public Component getMediaComponent() {
		return this;
	}

}
