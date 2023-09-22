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
	public void Wheel(int x, int y, int button,double amount) {
		
	}

	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
		
	}

	public void drawCanvas(Graphics offGC, boolean complete, HitPoint pt) {
		
	}
 
	public void setCanvas(commonCanvas v) {
		parentCanvas = v;
		
	}


}
