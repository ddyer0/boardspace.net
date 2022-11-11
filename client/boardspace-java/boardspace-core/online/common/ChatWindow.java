package online.common;

import lib.Graphics;

import java.awt.event.MouseWheelEvent;

import lib.CanvasProtocol;
import lib.ChatInterface;
import lib.ExtendedHashtable;
import lib.HitPoint;
import lib.LFrameProtocol;
/*
 * this is used to create a standalone chat window for chat rooms
 */
@SuppressWarnings("serial")
public class ChatWindow extends exCanvas  
	implements CanvasProtocol
{	
	ExtendedHashtable info;
	public ChatWindow(LFrameProtocol frame,ExtendedHashtable sharedInfo,ChatInterface chat)
	{	
		init(sharedInfo,frame);
		theChat = chat;
		theChat.setCanvas(this);
		theChat.setVisible(true);
		addMouseListener(this);
		addMouseMotionListener(this);
		setVisible(true);
	}


	public void ViewerRun(int w)
	{	super.ViewerRun(w);
		if(theChat.doRepeat()) 
			{ repaint(20); 
			}
	}


	public void setLocalBounds(int l, int t, int w, int h) {
		theChat.setBounds(l,t,w,h);
	}


	public void Pinch(int x, int y, double amount, double twist) {
		
	}
	public boolean touchZoomEnabled() { return(true); }
	public boolean runTheChat() { return(true); }
		
	public void StartDragging(HitPoint hp) {
		theChat.StartDragging(hp);	
	}

	public void StopDragging(HitPoint hp) 
	{
		theChat.StopDragging(hp);
	}

	public void drawCanvas(Graphics offGC, boolean complete, HitPoint pt) {
		// only do the actual drawing here. if redrawBoard is called twice
		// there can be mysterious failures because registerChipHit has 
		// already triggered.
		if(offGC!=null)
			{ theChat.redrawBoard(offGC,pt); 
			  drawKeyboard(offGC,pt);
			}
	}


	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
	   	if(mouseTrackingAvailable(pt)) { magnifier.DrawTileSprite(gc,pt); }
 	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{	
		if((e.getModifiersEx()==0) && theChat.doMouseWheel(e.getX(),e.getY(),e.getWheelRotation()))
		{
			repaint(10,"mouse wheel");
		}
	}
}
