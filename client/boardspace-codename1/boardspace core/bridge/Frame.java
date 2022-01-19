package bridge;

import lib.G;
import lib.Http;
import lib.NullLayout;
import lib.NullLayoutProtocol;
import lib.PinchEvent;
import lib.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;

public class Frame extends Window implements NullLayoutProtocol,
	MouseMotionListener,MouseListener
{	boolean resizable = false;
	public void setResizable(boolean n) { resizable = n; }
	Container glassPane = new FullscreenPanel();

	public void setJMenuBar(JMenuBar m){}
	
	Image iconImage = null;
	public void setIconAsImage(Image im) 
	{ iconImage = im;
	  MasterForm.getMasterPanel().setTabName(this,getTitle(),iconImage);
	}
	
	public String getTitle() { return(getName()); }
	public com.codename1.ui.Image getIconImage() 
	{ return(iconImage!=null ? iconImage.getImage() : null); 
	}
	public Image getIconAsImage() 
	{ 

	  return(iconImage); 
	}
	// tabname appears in the master frame, as the selectable name of the frame.
	public String tabName = null;
	public String tabName() { return(tabName); }
	public void setTabName(String g) { tabName = g; }
	public void init()
	{
		MasterForm.getMasterPanel().add(this);
		setOpaque(true);
		glassPane.setLayout(new NullLayout((NullLayoutProtocol)glassPane));
		setLayout(new NullLayout(this));
		glassPane.setSize(getWidth(),getHeight());
		super.add(glassPane);
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
	public void doNullLayout(Container parent)
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
	public com.codename1.ui.Container add(com.codename1.ui.Component c) 
	{ return glassPane.add(c); 
	}
	public void remove(com.codename1.ui.Component c)
	{	c.setVisible(false);
		glassPane.removeComponent(c);
	}
	public void setContentPane(Container newContentPane) 
	{	super.removeComponent(glassPane);
		super.add(newContentPane);
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
					new Runnable () {	public void run() { showInEdt(); } });
				
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
		if(object instanceof Component) { targetBounds = ((Component)object).getBounds(); }
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
	
}
