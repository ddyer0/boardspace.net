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

import java.util.StringTokenizer;

import lib.Base64;
import lib.ExtendedHashtable;
import lib.G;
import lib.LFrameProtocol;
import lib.Plog;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.StringStack;
import lib.commonPanel;
import online.common.OnlineConstants;
import online.game.commonCanvas;
import online.game.commonPlayer;

// TODO: the remote client protocol doesn't handle player time

public class RpcRemoteClient implements RpcInterface,SimpleObserver
{	boolean active = false;
	private commonCanvas canvas ;
	private commonPanel panel = null;
	private LFrameProtocol frame = null;
	private ExtendedHashtable info;
	private int forPlayer = -1;
	StringStack messages = new StringStack();
	Plog log = new Plog(100);
	
	public void shutDown()
	{	
		setRpcIsActive(false);
		if(canvas!=null) 
			{ canvas.shutDown(); 
			  panel.remove(canvas.getComponent());
			}
	}
	public RpcRemoteClient(String cls,ExtendedHashtable i,commonPanel p,LFrameProtocol fr,int player) 
	{
		info = i;
		panel = p;
		frame = fr;
		forPlayer = player;
		G.setRemoteViewer(true);
    	commonPlayer my = new commonPlayer(0); 
    	my.primary = true; //mark it as "us"
    	info.put(OnlineConstants.MYPLAYER,my);
    	
		canvas = (online.game.commonCanvas)G.MakeInstance(cls);		
		canvas.init(info,frame);
		panel.setCanvas(canvas);
		canvas.setSize(panel.getWidth(),panel.getHeight());
		canvas.setVisible(true);		
		canvas.addObserver(this);
		canvas.setRemoteViewer(forPlayer);
		active = true;
	}

	public boolean rpcIsActive()
	{ 	if(active && canvas.stopped) 
		{ // this notices when the main window has been closed
		setRpcIsActive(false); 
		}

		return(active); 
	}
	public void setRpcIsActive(boolean v) 
	{ 	active = v; 
		if(!v) 
			{ canvas.removeObserver(this);
			  panel.requestShutdown();
			  panel.remove(canvas.getComponent());
			  panel.repaint();
			}
	}

    //
    // interface for being a rpc client
    //
	public boolean needsRecapture() {
		return (messages.size()>0);
	}

	public ServiceType serviceType() {
		return(ServiceType.RemoteScreen);
	}

	public String captureState() {
		StringStack m = messages;
		if(m.size()>0)
		{	StringBuilder out = new StringBuilder();
			messages = new StringStack();
			for(int i=0,lim = m.size(); i<lim; i++)
			{
				out.append(Keyword.Update.name());
				out.append(' ');
				out.append(Base64.encodeSimple(m.elementAt(i)));
				out.append(' ');
			}
			out.append(Keyword.Digest.name());
			out.append(" ");
			out.append(canvas.Digest());
			return(out.toString());
		}
		return("");
	}

	public String captureInitialization() {
		return("");
	}

	public void execute(String msg) {
		StringTokenizer tok = new StringTokenizer(msg);
		Keyword command = Keyword.valueOf(tok.nextToken());
		switch(command)
		{
		case Complete:
			canvas.saveInitialization(msg);
			break;
		case Update:
			while(tok.hasMoreTokens())
			{
				String b64 = tok.nextToken();
				String com = Base64.decodeString(b64);
				log.addLog(com);
				canvas.deferMessage(com,-1);
			}
			break;
		case None:	// noop
			break;
		default:
			log.addLog("Not expecting command "+ command);
			G.Error("Not expecting command %s\n%s", command,log.finishLog());
		}
	}
	public void preInit(ExtendedHashtable info)
	{	
	}
	public String getName() {
		return ("Rpc viewer for "+canvas);
	}
	public void setName(String n) { };

	public void update(SimpleObservable o, Object eventType, Object arg) {
		messages.push((String)arg);
		observers.setChanged();
	}

	public void updateProgress() {
		canvas.updateProgress();
	}

	SimpleObservable observers = new SimpleObservable(this);

	public void removeObserver(SimpleObserver b) {
		observers.removeObserver(b);	
	}

	public void addObserver(SimpleObserver o) {
		observers.addObserver(o);
	}

}
