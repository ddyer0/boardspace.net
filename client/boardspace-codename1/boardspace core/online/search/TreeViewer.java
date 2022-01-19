package online.search;

import lib.Graphics;
import bridge.JMenu;
import bridge.JMenuItem;

import lib.ExtendedHashtable;
import lib.G;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.ShellProtocol;
import online.common.exCanvas;
import online.game.commonCanvas;

public class TreeViewer extends exCanvas implements TreeViewerProtocol
{	
	commonCanvas parentCanvas = null;
	   
    JMenu pauseMenu = null;
    JMenu resumeMenu = null;
    JMenu exitMenu = null;

	
	public void setTree(TreeProviderProtocol v) {};
	public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	super.init(info, frame);
        sharedInfo = info;
        s = G.getTranslations();
        myFrame = frame;
     	startShell = myFrame.addAction("start shell",deferredEvents);
     	startInspector = myFrame.addAction("start inspector",deferredEvents);

       	pauseMenu = new JMenu("Pause");
        myFrame.addToMenuBar(pauseMenu,deferredEvents);
        resumeMenu = new JMenu("Resume");
        myFrame.addToMenuBar(resumeMenu,deferredEvents);
        exitMenu = new JMenu("Exit Bots");
        myFrame.addToMenuBar(exitMenu,deferredEvents);

    }
	public boolean handleDeferredEvent(Object target,String command)
	{
		if (target == startShell) {  startShell(); return(true); }
		if (target == startInspector) {  startInspector(); return(true); }
		if(target==pauseMenu)
			{
			parentCanvas.pauseRobots();
			return(true);
			}
    	if(target==resumeMenu) 
    		{
    		parentCanvas.resumeRobots();
    		return(true);
    		}
    	if(target==exitMenu)
    		{ 
    		parentCanvas.stopRobots();
    		return(true);
    		}
    	return(super.handleDeferredEvent(target, command));
	}
	//
    // this console, based on "beanshell" 
    //
	private JMenuItem startShell = null;
	private JMenuItem startInspector = null;
	
	public void startInspector()
	{
		
	}
    public void startShell()
       {	
       	ShellProtocol shell = (ShellProtocol)G.MakeInstance("lib.ShellBridge");
       	shell.startShell("viewer",this,"out",System.out);
       	G.setPrinter(shell);
       }	
	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	public void setLocalBounds(int l, int t, int w, int h) {
		repaint();
	}

	public void StartDragging(HitPoint hp) {
		
	}

	public void StopDragging(HitPoint hp) {
		
	}
	public void Pinch(int x, int y, double amount,double angle) {
		
	}
	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
		
	}

	public void drawCanvas(Graphics offGC, boolean complete, HitPoint pt) {
		
	}
 
	public void setCanvas(commonCanvas v) {
		parentCanvas = v;
		
	}


}
