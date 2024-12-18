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
package vnc;


import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.JCheckBoxMenuItem;
import lib.*;
import udp.PlaytableServer;
import udp.PlaytableStack;

/**
 * This is a third "main window" type, after lobby and game canvas.  It supports
 * viewing the bitmap from a VNC connection and related activities.
 * 
 * @author Ddyer
 *
 */
@SuppressWarnings("serial")
public class AuxViewer extends exCanvas
{
	VNCReceiver receiver = new VNCReceiver(this);
	JCheckBoxMenuItem verbose = null;
	boolean needMenu = false;
	PopupManager serverMenu = new PopupManager();
	boolean showConnectionStatus = false;		// overlay for debugging
	String stateSummary = null;
	PlaytableServer server = PlaytableStack.getSelectedServer();
	public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	super.init(info, frame);
        sharedInfo = info;
        myFrame = frame;
        server = PlaytableStack.getSelectedServer();
        verbose = myFrame.addOption("verbose", false, deferredEvents);
        if(!startServer(server))
        {	needMenu = true;
        }	
    }
	public boolean handleDeferredEvent(Object target, String command)
	{	if(verbose==target) 
			{ boolean val = verbose.getState();
			  if(receiver!=null) { receiver.verbose = val ? 10 : 0; }
			return(true);
			}
		if(serverMenu.selectMenuTarget(target))
			{ server = (PlaytableServer)serverMenu.rawValue;
			  startServer(server);
			  return(true);
			}
		return(super.handleDeferredEvent(target, command));
	}
	public boolean startServer(PlaytableServer currentServer)
	{
        if(currentServer!=null) 
    	{    
    	   receiver.start(currentServer.getHostIp(), currentServer.getPort());
    	   return(true);
    	}
        return(false);
	}
	public void runStep()
	{	NetPacketConn net = receiver.netConn;
		if(showConnectionStatus) { stateSummary = net.stateSummary(); }
		repaint(1000);
	}
	
	 public void shutDown()
	 {
		 super.shutDown();
		 LFrameProtocol f = myFrame;
		 if(f!=null) { myFrame = null; f.killFrame(); }
		 receiver.shutDown();
	 }
	 public void ViewerRun(int waitTime)
	 {	super.ViewerRun(waitTime);
	 	doMenu();
	 	runStep();
	 }
	public void setLocalBounds(int l, int t, int w, int h) {
		
	}
	public void Pinch(int x, int y, double amount,double angle) {
		
	}
	public void StartDragging(HitPoint hp) {
		//G.print("Aux Viewer Start drag "+hp);
	}
	public void StopDragging(HitPoint hp) {
		//G.print("Aux Viewer Stop drag "+hp);
		
	}
	public void drawCanvas(Graphics offGC, boolean complete, HitPoint pt) 
	{	GC.setColor(offGC, Color.gray);
		int width = getWidth();
		int height =getHeight();
		int size = (int)(15*G.getDisplayScale());
		GC.fillRect(offGC,0,0,width,height);
		receiver.drawCanvas(offGC, complete,pt);
		GC.draw_anim(offGC, new Rectangle(width-size,height-size,size,size),size, lastInputTime, progress);
		if(showConnectionStatus && stateSummary!=null)
		{	GC.setFont(offGC,largeBoldFont());
			GC.Text(offGC, false, 10, 10, getWidth(), 40,Color.black,Color.white,stateSummary);
		}
	}
	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
		
	}
	// stop the vnc activity, in this case stop everything
	String stopReason = null;
	public void stopService(String reason) {
		shutDown();
	}
	public void doMenu()
	{	
		if(needMenu)
		{	needMenu = false;
	        int nservers = PlaytableStack.getNServers();
	        serverMenu.newPopupMenu(this, deferredEvents);
	        for(int i=0;i<nservers;i++)
	        {	PlaytableServer ser = PlaytableStack.getNthServer(i);
	        	serverMenu.addMenuItem(ser.toString(),ser);
	        }
	        serverMenu.show(getWidth()/2, getHeight()/2);

		}
	}


}
