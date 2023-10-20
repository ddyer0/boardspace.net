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

	public void MouseDown(HitPoint hp)
	{
		theChat.MouseDown(hp);
	}
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

	public void Wheel(int x, int y, int button,double amount) {

		if((button==0) && theChat.doMouseWheel(x,y,amount))
		{
			repaint(10,"mouse wheel");
		}
	}
}
