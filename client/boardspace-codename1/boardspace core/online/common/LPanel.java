package online.common;

import bridge.*;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Insets;

import lib.G;
import lib.LFrameProtocol;
import lib.NullLayout;
import lib.XFrame;


public class LPanel extends FullscreenPanel implements WindowListener,LobbyConstants
{
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
    public XFrame theFrame=null;
    public commonPanel theLobby;
    
    public Dimension getPreferredSize()
    {	int w = 9999;
    	int h = 9999;
    	if(theFrame!=null) { w = getParent().getWidth(); h=getParent().getHeight(); }
    	return(new Dimension(w,h));
    }
 
    /* constructor */
    public LPanel(String inStr,XFrame frame, commonPanel inLobby)
    {
        super();	
        //setOpaque(false);
        new BoxLayout(this,BoxLayout.Y_AXIS);
        theFrame = frame;
        theLobby = inLobby;
        setLayout(new NullLayout(this));
        //setResizable(true);
     	// this is a workaround to force all JPopupMenu to be "heavyweight"
     	// which somehow avoids the menu clipping problem.
     	JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        if(frame!=null)
        {	
        	frame.setContentPane(this);//frame.add(this);
        	frame.addWindowListener(this);
        	frame.setTitle(inStr);	// defer this until after the menus are added
        }
        
        add(theLobby);
    }
    
    /**
     * create a free standing frame containing panel "a"
     * 
     * @param name
     * @param a
     * @return
     */
    public static LFrameProtocol newLFrame(String name,commonPanel a)
    {
    	XFrame fr = new XFrame();
    	new LPanel(name, fr, a);
    	return(fr);
    }
    
    public void setVisible(boolean v)
    {
    	super.setVisible(v);
    	if(theFrame!=null) { theFrame.setVisible(true); }
    }


    public void setSize(int w,int h)
    {
    	if(theFrame!=null) { theFrame.setSize(w,h); }
    }


    Dimension oldsize = new Dimension(0,0);
    public void setLocalBounds(int l,int t,int w,int h)
    {	
        if ((theLobby != null) 
        		&& w>0
        		&& h>0
        		&& ((w!=G.Width(oldsize))||(h!=G.Height(oldsize))))
        {
        Insets in = getInsets();
        int barheight = 0;//myMenuBar.getHeight(); we use frame menu bars which have zero height for us
        oldsize = new Dimension(w,h);
        	theLobby.setBounds( in.left, barheight+in.top,
        			w-in.left-in.right, h-in.top-in.bottom-barheight);
        }
    }
    public void windowClosing(WindowEvent e)
    {
       if(theLobby!=null) { theLobby.requestShutdown(); }
    }
}