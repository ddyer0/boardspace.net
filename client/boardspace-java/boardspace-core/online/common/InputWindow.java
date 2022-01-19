package online.common;

import lib.Graphics;

import lib.CellId;
import lib.HitPoint;
import lib.Keyboard;
import lib.SimpleObservable;
import lib.TextContainer;
import lib.TextContainer.Op;

@SuppressWarnings("serial")
public class InputWindow  extends exCanvas  
{	int flipInterval = 500; 
	boolean useKeyboard = true; // G.isCodenameOne();
	Keyboard keyboard = null;
	enum InputId implements CellId
	{	InputField;
	@Override
	public String shortName() {
		return(name());
	}};
	
	TextContainer input = new TextContainer(InputId.InputField);
	TextContainer label = null;

	
	public InputWindow(String l)
	{
		if(label!=null) { label = new TextContainer(l); }
		input.setEditable(this,true);
		input.addObserver(this);
	}
	
	public void setLocalBounds(int l, int t, int w, int h) {
		if(label!=null) 
		{
			label.setBounds(l,t,w,h/2);
			input.setBounds(l,t+h/2,w,h/2);
		}
		else
		{
			input.setBounds(l,t,w,h);
		}
	}

	public void Pinch(int x, int y, double amount, double twist) {
		
	}

	public void StartDragging(HitPoint hp) {
	}

	public void StopDragging(HitPoint hp) {
		CellId hit = hp.hitCode;
		if(hit instanceof InputId)
		{
		switch((InputId)hit)
		{
		case InputField:
			if(useKeyboard)
			{
	    		if(useKeyboard) {
	    			keyboard = new Keyboard(this,input);
	    		}
	    		else 
	    		{	requestFocus(); 
	    			repaint(flipInterval);
	    		}}
			}
		}
	}

	public void drawCanvas(Graphics offGC, boolean complete, HitPoint pt) {
		input.redrawBoard(offGC,pt);
	}

	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
		
	}
	public void sendInput()
	{
		keyboard = null;
	}
	public void update(SimpleObservable o, Object eventType, Object arg) {
		if(input!=null)
		{
			if(arg ==Op.Send)
			{
			//G.addLog("send");
			sendInput();
			
			}
		repaint(); 
		}
	}
	
}
