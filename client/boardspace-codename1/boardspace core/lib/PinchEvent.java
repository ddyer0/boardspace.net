package lib;

import bridge.MouseEvent;

public class PinchEvent extends MouseEvent
{
	public double amount;
	public double getAmount() { return(amount); }
	public double twist;
	public double getTwist() { return(twist); }
	
	public PinchEvent(Object o,int xx,int yy,int button)
	{
		super(o,xx,yy,button);
	}
	public PinchEvent(Object o,double am,int xx,int yy,double tw) 
	{	super(o,xx,yy,3);
		amount = am;
		twist = tw;
	}
}