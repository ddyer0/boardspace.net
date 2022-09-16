package lib;

import bridge.*;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;

public class XFrame extends JFrame implements WindowListener
{
	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 01L;
	private boolean useMenuBar = !G.isCodename1();		// if true, use the local menu bar
	private boolean closeable = true;
	public void setCloseable(boolean v) { closeable = v; }
	public boolean getCloseable() { return(closeable); }
	public JMenuBar jMenuBar = null;
	JPopupMenu popupMenuBar = null;
	public DeferredEventManager canSavePanZoom = null;
	public boolean hasSavePanZoom = false;
	public void setCanSavePanZoom(DeferredEventManager m) 
	{ 	canSavePanZoom = m;
		MasterForm.getMasterPanel().adjustTabStyles(); 
	}
	public void setHasSavePanZoom(boolean v) 
	{ hasSavePanZoom = v; 
	  MasterForm.getMasterPanel().adjustTabStyles(); 
	}
	Label title;
	String name="Unnamed";
	/** constructor */
	public XFrame(String string) 
	{ super(string);
	  name=string;
	  addWindowListener(this);
	  setOpaque(false);
	  initTitleBar();
	}
	public void setVisible(boolean v)
	{	
		if(v && (getWidth()<100 || getHeight()<100))
		{
		 double scale = G.getDisplayScale();
		 int w = (int)(scale*450);
		 int h = (int)(scale*430);
		 setBounds( 0, 0, w, h); 
		}
		super.setVisible(v);
	}
	/** constructor */
	public XFrame() 
	{ super(); 
	  name = "";
	  addWindowListener(this);
	  initTitleBar();
	}
	public void setTitle(String n) 
	{ name = n;
	  if(title==null) { title = new Label(name); }
	  if(n!=null && G.isSimulator()) { title.setText(n+" "+getWidth()+"x"+getHeight()); }
	  MasterForm.getMasterPanel().setTabName(this,name,getIconAsImage());
	}
	public String getTitle()
	{
		return(name);
	}


	public void initTitleBar()
	{
		title = new Label(name);
	
	}
	public boolean hasCommand(String cmd)
	{	if(("rotate".equals(cmd) || "twist".equals(cmd))) { return(!G.isIOS()); }
		if("close".equals(cmd)) { return(true); }
		if("actionmenu".equals(cmd)) 
			{ return(popupMenuBar!=null); 
			}
		if("savepanzoom".equals(cmd)) { return(canSavePanZoom!=null); }
		if("restorepanzoom".equals(cmd)) { return(hasSavePanZoom); }
		return(false);
	}
	public void buttonMenuBar(ActionEvent evt,int x,int y)
	{	String cmd = evt.getActionCommand().toString();
		if("twist".equals(cmd))
		{
			MasterForm.setGlobalRotation(MasterForm.getGlobalRotation()+1);
			repaint();			
		}
		else if("rotate".equals(cmd))
		{
			MasterForm.setGlobalRotation(MasterForm.getGlobalRotation()+2);
			repaint();
		}
		else if("actionmenu".equals(cmd))
			{if(popupMenuBar!=null)
			{ 	
			try 
			{ 
				popupMenuBar.show(this,
						MasterForm.translateX(this, x),
						MasterForm.translateY(this, y)
						);
			} catch (AccessControlException e) {}
			}}
		else if("close".equals(cmd))
		{
			if(getCloseable()) 
				{ dispose(); 
				}
		}
		else if("savepanzoom".equals(cmd) || "restorepanzoom".equals(cmd))
		{	if(canSavePanZoom!=null)
			{	canSavePanZoom.deferActionEvent(evt);
			}
		}
		else {
			Http.postError(this,"unexpected action event: "+cmd,null);
		}
	}

	public void setJMenuBar(JMenuBar m) { jMenuBar = m; super.setJMenuBar(m); }
	public void addToMenuBar(JMenu m)
	{	
		addToMenuBar(m,null);
	}
	public void addToMenuBar(JMenu m,DeferredEventManager l)
	{	
		if(useMenuBar)
		{	if(jMenuBar==null) {  setJMenuBar(new JMenuBar()); }
			m.setVisible(true);
			jMenuBar.add(m);
		}
		else {
			if(popupMenuBar==null) { popupMenuBar=new JPopupMenu();}
			popupMenuBar.add(m);
		}
		if(l!=null) { m.addItemListener(l); }
	}

	
	public void removeFromMenuBar(JMenu m)
	{	
		if(useMenuBar)
		{
			if(jMenuBar!=null) { jMenuBar.remove(m); }
		}
		else {
			if(popupMenuBar!=null) { popupMenuBar.remove(m); }
		}
	}
	
	private Rectangle oldBounds = null;
	private int minimumWidth=300;
	private int minimumHeight=300;
	public void expand(int minw,int minh)
	{	//minimumWidth = minw;		// this was an exeriment to set minimums greater than available size
		//minimumHeight = minh;		// which created scrollable frames
		expand();
	}
	public Dimension getMinimumSize()
	{
		return(new Dimension(minimumWidth,minimumHeight));
	}

	public void expand()
	{
		if(oldBounds!=null)
		{ setBounds(G.Left(oldBounds),G.Top(oldBounds),G.Width(oldBounds),G.Height(oldBounds));
		  oldBounds = null; 
		}
		else 
		{ Container parent = getParent();
		// parent can legitimately be null if the window is closing
		if(parent!=null)
			{
			int w = parent.getWidth();
			int h = parent.getHeight();
			oldBounds = new Rectangle(getX(),getY(),getWidth(),getHeight());
			title.setText(name+" "+getWidth()+"x"+getHeight());
			setBounds(0,0,Math.max(minimumWidth,w),Math.max(minimumHeight,h));
			if(minimumWidth>w || minimumHeight>h)
			{
				G.print("Oversize "+name+" "+minimumWidth+"x"+minimumHeight+" "+w+"x"+h);
			}
			}
		}
	}
	

	public void windowOpened(WindowEvent e) {
		
	}
	public boolean killed = false;
	public boolean killed() 
	{
		return(killed);
	}

	public void dispose()
	{	killed=true;
		super.dispose();		
	}

	public void windowClosing(WindowEvent e) {
		killed = true;
	}

	public void windowClosed(WindowEvent e) {
		killed=true;
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {		
	}

	public void windowActivated(WindowEvent e) {		
	}

	public void windowDeactivated(WindowEvent e) {		
	}


}