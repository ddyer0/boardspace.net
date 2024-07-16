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
package lib;

import bridge.*;

import com.codename1.ui.Component;

public class XFrame implements WindowListener,SizeProvider,LFrameProtocol
{
	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 01L;
    private JCheckBoxMenuItem soundCheckBox = null;
    private JMenu options = null;
    private JMenu actions = null;
	private TopFrameProtocol myFrame = null;
    private InternationalStrings s = null;
	String name="Unnamed";
	/** constructor */
	public XFrame(String string) 
	{ 	myFrame = G.useTabInterface() ? new TabFrame(string) : new JFrame(string); //new TabFrame(string);
		initStuff(string);
	}
	/** constructor */
	public XFrame()
	{	myFrame = G.useTabInterface() ? new TabFrame() : new JFrame();//new TabFrame();
		initStuff("");


	}
	
	static final String SoundMessage = "Sound";
	static final String OptionsMessage = "Options";
	static final String ActionsMessage = "Actions";

	public static String XFrameMessages[] = {
	            OptionsMessage,
	            ActionsMessage,
	           SoundMessage,

	    };

	private void initStuff(String n)
	{ name = n;
	  s = G.getTranslations();
	  //setResizable(true);
   	  // this is a workaround to force all JPopupMenu to be "heavyweight"
   	  // which somehow avoids the menu clipping problem.
	  JPopupMenu.setDefaultLightWeightPopupEnabled(false);
      options = new XJMenu(s.get(OptionsMessage),true);
      actions = new XJMenu(s.get(ActionsMessage),true);
      soundCheckBox = new JCheckBoxMenuItem(s.get(SoundMessage),Config.Default.getBoolean(Config.Default.sound));
	  addWindowListener(this);
	  myFrame.setOpaque(false);
	}
	
	public void setVisible(boolean v)
	{	
		if(v && (getWidth()<100 || getHeight()<100))
		{
		 double scale = G.getDisplayScale();
		 int w = (int)(scale*450);
		 int h = (int)(scale*430);
		 myFrame.setFrameBounds( 0, 0, w, h); 
		}
		myFrame.setVisible(v);
	}

	public void setTitle(String n) 
	{ name = n;
	  myFrame.setTitle(n);
	}
	public String getTitle()
	{
		return(name);
	}

	
	public void windowOpened(WindowEvent e) {
		
	}
	public boolean killed = false;
	public boolean killed() 
	{
		return(killed);
	}

	private boolean disposed = false;
	public void dispose()
	{	killed=true;
		if(!disposed)
		{	disposed = true;	
			myFrame.dispose();
		}
	}

	public void windowClosing(WindowEvent e) {
		killFrame();
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

	// for lframeprotocol
	public void show(MenuInterface menu, int x, int y) throws AccessControlException {
		showNative(menu, x, y);		
	}
	// for lframeprotocol
	public void showNative(MenuInterface menu, int x, int y) throws AccessControlException {
		G.show((Component)myFrame, menu, x, y);		
	}
	int lastKnownWidth = -1;
	int lastKnownHeight = -1;
	public void screenResized()
	{	if(G.isCheerpj())
		{int w = lastKnownWidth;
		int h = lastKnownHeight;
		lastKnownWidth = G.getScreenWidth();
		lastKnownHeight = G.getScreenHeight();
		if(lastKnownWidth!=w || lastKnownHeight!=h)
		  {
		   setInitialBounds(getX(),getY(),getWidth(),getHeight());
		  }
		}
	}
	public void setInitialBounds(int inx,int iny,int inw,int inh)
	{	myFrame.setInitialBounds(inx,iny,inw,inh);
	}

	public boolean doSound() {
	    return (soundCheckBox.isSelected());
	}
	public void setDoSound(boolean enable) {
		soundCheckBox.setSelected(enable); 
	}

 
	public JCheckBoxMenuItem addOption(String text, boolean initial, JMenu m,DeferredEventManager e)
    {	
		if(options.getItemCount()==0) 
		{	options.add(soundCheckBox);		// always first
		}
		JCheckBoxMenuItem b = new JCheckBoxMenuItem(text);
        b.setState(initial);
        m.add(b);
        if(e!=null) { b.addItemListener(e); }
        addToMenuBar(options); 
        return (b);
    }

    public JCheckBoxMenuItem addOption(String text, boolean initial,DeferredEventManager e)
    {
        return (addOption(text, initial, options,e));
    }
    public JMenu addChoiceMenu(String text, DeferredEventManager e)
    {	if(options.getItemCount()==0) 
    	{	options.add(soundCheckBox);		// always first
    	}
    	JMenu item = new XJMenu(text,false);
    	options.add(item);
        if(e!=null) { item.addActionListener(e); }
        addToMenuBar(options);
        return(item);
    }

    public void addAction(JMenu m,JMenuItem mi,DeferredEventManager e)
    {	
    	m.add(mi);
    	mi.addActionListener(e);
    }
    public JMenuItem addAction(JMenu m,String mname,DeferredEventManager e)
    {	JMenuItem mi = new JMenuItem(mname);
    	addAction(m,mi,e);
    	return(mi);
    }
  

    public JMenuItem addAction(JMenuItem b,DeferredEventManager e)
    {	actions.add(b);
    	addActionInternal(b,e);
    	return(b);
    }
    public JMenu addAction(JMenu b,DeferredEventManager e)
    {	actions.add(b);
    	addActionInternal(b,e);
    	return(b);
    }
    private void addActionInternal(NativeMenuItemInterface b,DeferredEventManager e)
    {
    	try{
    	if(e!=null) { b.addActionListener(e); }
    		addToMenuBar(actions); 
    	}
    	catch (Throwable err)
        {
            G.print("AddAction got an error: " + err);
        }
    }

    public  JMenuItem addAction(String text,DeferredEventManager e)
    {
    	JMenuItem b = new JMenuItem(text);
        addAction(b,e);
        return(b);
    }

    public void removeAction(JMenu m)
    {
        if (m != null)
        {
        	try
            { //error trap for ms explorer bug
            	actions.remove(m); 
 
                if (actions.getItemCount() == 0)
                {
                	myFrame.removeFromMenuBar(actions);
                }
            }
           	catch (Throwable err)
            {
                G.print("RemoveAction got an error: " + err);
            }

        }
    }

    public void removeAction(JMenuItem m)
    {
        if (m != null)
        {
        	try
            { //error trap for ms explorer bug
            	actions.remove(m); 
 
                if (actions.getItemCount() == 0)
                {
                	myFrame.removeFromMenuBar(actions);
                }
            }
           	catch (Throwable err)
            {
                G.print("RemoveAction got an error: " + err);
            }

        }
    }
		  

    // the please kill/don't kill logic is so that during critical
    // phases (ie; scoring a game) the frame will resist closing
    // and that under whatever circumstances, it only closes once.
	private boolean pleaseKill = false;
	private boolean dontKill = false;

	public void killFrame()
    { 
        pleaseKill = true;			// remember some tried to do it
        if(!dontKill) { killed = true; setVisible(false);}
        	
        }

    public void setDontKill(boolean v)
    {
        dontKill = v;
        if (pleaseKill && (dontKill == false))
        {
            killFrame();
        }
    }

	public MenuParentInterface getMenuParent() {
		return this;
	}

	public void addWindowListener(WindowListener who) {
		myFrame.addWindowListener(who);
		
	}

	public void setIconAsImage(Image icon) {
		myFrame.changeImageIcon(icon);
	}
	public int getWidth() {
		return myFrame.getWidth();
	}
	public int getHeight() {
		return myFrame.getHeight();
	}
	public int getX() {
		return myFrame.getX();
	}
	public int getY() {
		return myFrame.getY();
	}
	public void addC(Component p)
	{	myFrame.addC(p);
	}
	public void addC(String where,Component p)
	{
		myFrame.addC(where,p);
	}
	public void repaint() { myFrame.repaint(); }
	public Container getContentPane() { return myFrame.getContentPane(); }
	public void setBounds(int x,int y,int w,int h) { myFrame.setFrameBounds(x,y,w,h); }
	public void setOpaque(boolean v) { myFrame.setOpaque(v); }
	public void setLayout(LayoutManager x) { myFrame.setLayout(x); }
	public void setContentPane(Container p) { myFrame.setContentPane(p); }
	public TopFrameProtocol getFrame() { return myFrame; }
	public void addToMenuBar(JMenu m) {
		myFrame.addToMenuBar(m,null);
	}
	public void addToMenuBar(JMenu m, DeferredEventManager l) {
		myFrame.addToMenuBar(m,l);
	}
	public void setCanSavePanZoom(DeferredEventManager v) {
		myFrame.setCanSavePanZoom(v);
	}
	public void setHasSavePanZoom(boolean v) {
		myFrame.setHasSavePanZoom(v);
	}
	public CanvasRotater getCanvasRotater() {
		return myFrame.getCanvasRotater();
	}
	public void setCloseable(boolean b) {
		myFrame.setCloseable(b);
	}
	public void packAndCenter() 
	{	// finish, position and center the frame.
		myFrame.packAndCenter();
		
	}
	public void moveToFront() {
		myFrame.moveToFront();
	}
	public void setEnableRotater(boolean b) {
		myFrame.setEnableRotater(b);
	}
}