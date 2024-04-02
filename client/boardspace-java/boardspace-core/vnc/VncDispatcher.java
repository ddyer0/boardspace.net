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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;

import lib.Graphics;
import lib.CellId;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.SimpleObservable;
import lib.SimpleObserver;

enum DispatchId implements CellId {  selection ;
};
/**
 * this is a generic dispatch window that allows a client to select one of the
 * available services, and switch to it.  It's invisible!  There's an offscreen
 * bitmap which is normally viewed via a vnc connection, and events from the remote
 * viewer are fed in.
 * 
 * One instance is created and registered as a VncServiceProvider, but it 
 * will never have a bitmap or actually be used, except to call newInstance()
 * to create a clone.  This clone (or clones) will actually be used as the
 * source/sink for vnc acivity.
 * 
 * This variant of a vnc service provider will have its own run loop, unlike
 * the subwindows used for private information in games, which will be run
 * directly by the game loop.
 * 
 * @author Ddyer
 *
 */
public class VncDispatcher extends OffscreenWindow implements Runnable,VncServiceProvider,SimpleObserver,VNCConstants
{	private boolean exitRequest = false;
	String exitReason = null;
	static Component dummyComponent = new Container();
	public Component getMediaComponent()
	{ 
	  return(dummyComponent); 
	}
	public VncDispatcher(int w,int h)
	{
		super("Main Dispatcher",0,0,w,h);
		
	}
	
	// create a new instance and start it.
	public VncServiceProvider newInstance()
	{	VncDispatcher n = new VncDispatcher(width,height);
		new Thread(n,"VNC dispatcher").start();
		return(n);
	}
	public void wake() { G.wake(this);}
	
	public void redraw()
	{	VNCService.addObserver(this);
		Graphics gc = getGraphics();
		redrawBoard(gc,null);
	}
	public void redrawBoard(Graphics gc,HitPoint hp)
	{	Font dfont = G.getGlobalDefaultFont();
		InternationalStrings s = G.getTranslations();
		GC.setColor(gc, Color.white);
		GC.fillRect(gc, 0,0,width,height);
		GC.setColor(gc, Color.gray);
		GC.fillRect(gc, 10,10,width-20,height-20);
		int lineh = G.getFontSize(dfont)*3;
		GC.setFont(gc,G.getFont(dfont,lineh));
		int nServices = VNCService.getNServices();
		int ypos = 20+lineh * 2;
		if(nServices>0)
		{
			for(int servn = 0;servn<nServices;servn++)
			{	VncServiceProvider provider = VNCService.getNthService(servn);
				if(provider!=null)
				{
				String msg = provider.getName();
				boolean attached = provider.isActive();
				if(attached) { msg += s.get(ActiveConnection);}
				GC.Text(gc, false, 20,ypos,width-40,lineh,attached?Color.lightGray : Color.black,null,msg);
				if(!attached && G.pointInRect(hp,20,ypos,width-40,lineh))
				{
					hp.hitCode = DispatchId.selection;
					hp.hitObject = provider;
				}
				ypos+= lineh;
				}
			}
		}
		else
		{
			GC.Text(gc, true,0,0,width,height, Color.blue,null,s.get(NothingPlaying));			
		}
		GC.Text(gc, true, 0,0,width-100,100, Color.black, null,
				G.timeString(G.Date()-(G.getLocalTimeOffset()*60*1000)));
		setRepainted();
		repaint(10000,"vnc redraw");
	}
	
	// this is received when the VNCService service list changes
	public void update(SimpleObservable o, Object eventType, Object arg) {
		if(eventType==Operation.ServiceChange)
			{	repaint(0,"vnc update");
			}
		else if(eventType==Operation.Shutdown)
		{
			stopService("service shutdown");
		}
	}
	public void StopDragging(HitPoint pt) {
		CellId hit = pt.hitCode;
		if(hit instanceof DispatchId)
		{	DispatchId hitCode = (DispatchId)hit;
			switch(hitCode)
			{
			case selection:
				transmitter.setProvider((VncServiceProvider)(pt.hitObject));
				transmitter.sendUpdateRequired();
				stopDispatcher("made exit selection "+pt.hitObject);
				break;
			}
		}
	}
	private void stopDispatcher(String reason)
	{	exitRequest = true;
		exitReason = reason;
		G.wake(this);
	}
	public void stopService(String reason)
	{	boolean ex = exitRequest;
		if(!ex)
			{
			stopDispatcher(reason);
			super.stopService(reason);
			
			}
	}
	public void run() {
		exitRequest = false;
		while(!exitRequest)
		{
			mouse.performMouse();
			synchronized(this) { if(!exitRequest) { G.waitAWhile(this,0); }}
		}
	}
	public void notifyActive() { repaint(0,"vnc dispatch");  G.wake(this); }
}
