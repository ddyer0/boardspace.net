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

import com.codename1.ui.Font;
import bridge.FocusEvent;
import bridge.FocusListener;
import bridge.WindowEvent;
import bridge.JTextComponent;
import lib.ChatWidget.ChatId;
import lib.TextContainer.Op;
/**
 * A window that displays text.  This is used as a console window
 * by textdisplayframe
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
public class TextInputWindow extends exCanvas implements CanvasProtocol, Runnable, FocusListener
{	int flipInterval = 500;
	JTextComponent target = null;
	TextContainer area = new TextContainer("");
	Keyboard keyboard = null;
	boolean useKeyboard = G.defaultUseKeyboard();
	
    public void closeKeyboard()
    {
    	Keyboard kb = keyboard;
    	if(kb!=null) { kb.setClosed(); }
    }
    
	public Keyboard makeKeyboardIfNeeded(exCanvas c,Keyboard key)
	{	if((key==null) && hasFocus( )) { return new Keyboard(this,area);}
		return(key);
	}
    private void loseFocus()
    {
    	if(useKeyboard)
    	{
    		area.setFocus(false);
    	}
    }
    public Keyboard getKeyboard() 
    { Keyboard k = keyboard;
      if(k!=null && k.closed) 
      	{ k = keyboard = null; 
      	  loseFocus();
      	}
      return(k); 
    }
    public void createKeyboard()
    {	if(useKeyboard)
    	{
    	keyboard = new Keyboard(this,area);
     	}
    }
 

	public TextInputWindow(ExtendedHashtable shared,LFrameProtocol f,JTextComponent tar)
	{	init(shared,f);
		target = tar;
		area.setEditable(this,true);
    	area.addObserver(this);
		area.setVisible(true);
		if(target!=null) { area.setText(target.getText()); }
		setFocusable(true);
		addFocusListener(this);
		new Thread(this).start();
	}
	
	public void setFrameBounds(int l,int t,int w,int h)
	{	super.setFrameBounds(l, t, w, h);
		area.setBounds(l, t, w, h);
	}
	public void setFont(Font f)
	{	super.setFont(f);;
		area.setFont(f);
	}
	
	public void selectAll() { area.selectAll(); }

	public void setText(String s) {
		area.setText(s);
	}
	
	/* for window listener */
	public void windowClosing(WindowEvent e) {
		painter.shutdown();
	}
	
	public void startProcess() {
		// this is a dummy, we have no process.  TextMouseWindow has a process
	}

	public void run() {
		while(true)
		{	
			ViewerRun(100);
		}
	}
	public void setLocalBounds(int l, int t, int w, int h) {
		area.setBounds(l,t,w,h);
	}
	public void StartDragging(HitPoint hp) {
		
	}

	void changeFocus(boolean has,ChatId id)
	{	
		useKeyboard = G.defaultUseKeyboard();
		if(id!=null && has)
		{
		area.setFocus((id==ChatId.InputField),flipInterval);
		}
	}
	public void StopDragging(HitPoint hp)
	{	
		CellId hc = hp.hitCode;
		if(hc instanceof ChatId)
		{

	    	ChatId id = (ChatId)hc;
	    	switch(id)
	    	{
	    	case InputField: 
	    		break;
	    	default: break;
	    	}	
   
		}
	}	
    public HitPoint MouseMotion(int ex, int ey,MouseState upcode)
    {	HitPoint p =  new HitPoint(ex, ey,upcode);

    	if(keyboard!=null && keyboard.containsPoint(p))
			{ keyboard.doMouseMove(ex,ey,upcode);
			  
			}
    	redrawBoard(null,p);
		return setHighlightPoint(p);
    }
	public void MouseDown(HitPoint hp)
	{
		if(keyboard!=null) { keyboard.MouseDown(hp); }
	}
	public void drawCanvas(Graphics offGC, boolean complete, HitPoint pt) 
	{
		//area.redrawBoard(offGC,pt);
		if(keyboard!=null) { keyboard.draw(offGC,pt); }
	}

	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
		
	}
	public void focusGained(FocusEvent e) {
		area.setFocus(true);
		createKeyboard();
		
	}

	public void focusLost(FocusEvent e) {
		area.setFocus(false);
		closeKeyboard();
	}
	public void update(SimpleObservable o, Object eventType, Object arg) 
	{
		if(arg ==Op.Send)
		{
		//G.addLog("send");
		if(target!=null) { target.setText(area.getText()); }
		keyboard = null;
		shutDown();
		myFrame.dispose();
		}
	}

}
