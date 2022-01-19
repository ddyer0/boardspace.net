package lib;

import java.awt.Component;
import java.awt.event.MouseEvent;

public class PinchEvent extends MouseEvent
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1;
	public double amount;
	public double getAmount() { return(amount); }
	public double twist;
	public double getTwist() { return(twist); }
	
	public PinchEvent(Component o,int xx,int yy,int button)
	{
		super(o, 0, G.Date(), 3,   xx, yy, button, false);
	}
	
	public PinchEvent(Component o,double am,int xx,int yy,double tw) 
	{	
		super(o, 0, G.Date(), 3,   xx, yy, 1, false);
		//(Component source, int id, long when, int modifiers,
        //        int x, int y, int clickCount, boolean popupTrigger)
		amount = am;
		twist = tw;
	}
}