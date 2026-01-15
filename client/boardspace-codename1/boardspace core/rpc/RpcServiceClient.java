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
package rpc;


import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.SystemFont;

import lib.Base64;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.OStack;
import lib.SimpleObservable;
import lib.StringStack;
import lib.Tokenizer;
import lib.commonPanel;
import lib.exCanvas;
/**
 * this class presents the choice of games to join to side screens
 * @author ddyer
 *
 */
class RpcChoice
{
	String name;
	boolean active;
	public RpcChoice(String n,boolean b) { name = n; active = b; }

}
class RpcChoiceStack extends OStack<RpcChoice>
{
	public RpcChoice[] newComponentArray(int sz) { return new RpcChoice[sz]; }	
}

@SuppressWarnings("serial")
public class RpcServiceClient  extends exCanvas implements RpcInterface
{
	private RpcChoiceStack choices = new RpcChoiceStack();
	private StringStack responses = new StringStack();
	private boolean active = false;
	commonPanel panel;
	LFrameProtocol frame;
	
	enum DispatchId implements CellId { Select; ;
	}
	
	public RpcServiceClient(ExtendedHashtable info, commonPanel p, LFrameProtocol f) {
		panel = p;
		frame = f;
		init(info,frame);
		setSize(panel.getWidth(),panel.getHeight());
		panel.setCanvas(this);
		setRpcIsActive(true);
	}

	public void setLocalBounds(int l, int t, int w, int h) {
		
	}

	public void StartDragging(HitPoint hp) {
		
	}

	public void StopDragging(HitPoint hp) {
		CellId id = hp.hitCode;
		if(id instanceof DispatchId)
		{
			DispatchId did = (DispatchId)id;
			switch(did)
			{
			default: G.print("Not expecting ",did);
			case Select:
				{
					RpcChoice choice = (RpcChoice)hp.hitObject;
					choice.active = true;
					responses.push(Keyword.Select.name()+" "+Base64.encode(choice.name));
					setChanged();
					//observers.setChanged(true);
				}
			}
		}
		else
		{
			G.print("Not expecting %s",id);
		}
		
	}

	public void redrawBoard(Graphics gc,HitPoint hp)
	{	Font dfont = lib.FontManager.getGlobalDefaultFont();
		int width = getWidth();
		int height =getHeight();
		InternationalStrings s = G.getTranslations();
		GC.setColor(gc, Color.white);
		GC.fillRect(gc, 0,0,width,height);
		GC.setColor(gc, Color.gray);
		GC.fillRect(gc, 10,10,width-20,height-20);
		int lineh = lib.FontManager.getFontSize(dfont)*3;
		GC.setFont(gc,SystemFont.getFont(dfont,lineh));
		int nServices = choices.size();
		int ypos = 20+lineh * 2;
		if(nServices>0)
		{
			for(int servn = 0;servn<nServices;servn++)
			{	
				RpcChoice msg = choices.elementAt(servn);
				if(msg!=null)
				{
				boolean attached = msg.active;
				Rectangle r = new Rectangle(20,ypos,width-40,lineh);
				GC.Text(gc, false, r, attached?Color.lightGray : Color.black,null,msg.name);
				if(!attached && G.pointInRect(hp,r))
				{
					hp.hitCode = DispatchId.Select;
					hp.hitObject = msg;
					hp.spriteRect = r;
					hp.spriteColor = Color.red;
					GC.frameRect(gc, Color.red, r);
				}
				ypos+= lineh;
				}
			}
			}
		else if(active)
		{
			GC.Text(gc, true,0,0,width,height, Color.blue,null,s.get(NothingPlaying));			
		}
		else {
			
			GC.Text(gc, true,0,0,width,height, Color.blue,null,s.get(Shutdown));
		}
		GC.Text(gc, true, 0,0,width-100,100, Color.black, null,
				G.timeString(G.Date()-(G.getLocalTimeOffset()*60*1000)));
		int size = width/40;
		GC.draw_anim(gc, new Rectangle(width-size,height-size,size,size),size, lastInputTime, progress);
	}
	
	public void drawCanvas(Graphics offGC, boolean complete, HitPoint pt) 
	{	
		redrawBoard(offGC,pt);
	}
	
	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
		
	}

	
	
	// support for RpcInterface

	public boolean needsRecapture() {
		return(responses.size()>0);
	}

	public ServiceType serviceType() {
		return(ServiceType.Dispatch);
	}
	
	public String captureInitialization() { return ""; }

	// return the result of a click
	public String captureState() {
		StringStack res = responses;
		if(res.size()>0)
		{	StringBuilder b = new StringBuilder();
			responses = new StringStack();		// start a new responses list
			for(int i=0;i<res.size();i++)
			{
				String msg = res.elementAt(i);
				b.append(msg);
				b.append(' ');
			}
			return(b.toString());
		}
		return(null);
	}
	// load the list of possibilities
	public void execute(String msg) {
		Tokenizer tok = new Tokenizer(msg);
		choices.clear();
		while(tok.hasMoreTokens())
		{
			String name = Base64.decodeString(tok.nextToken());
			boolean act = tok.boolToken();	
			choices.push(new RpcChoice(name,act));
		}
		repaint();
	}

	public boolean rpcIsActive() {
		return active;
	}
	public void update(SimpleObservable from,Object eventType, Object msg)
	{
		painter.wakeMe();
	}
	public void shutDown()
	{
		setRpcIsActive(false);
		super.shutDown();
		panel.remove(getComponent());
		wake();
	}
	public void ViewerRun(int sleep)
	{	repaint(1000);
		super.ViewerRun(sleep);
	}
	public void setRpcIsActive(boolean v) {
		active = v;
		if(v)
		{
			addObserver(this);
		}
		if(!v)
		{
		removeObserver(this);
		}
	}

	
}
