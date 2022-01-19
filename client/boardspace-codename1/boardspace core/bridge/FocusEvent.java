package bridge;

public class FocusEvent extends Event
{
	Component c;
	public FocusEvent(Object ev, int x, int y) 
	{	
		if(ev instanceof Component) { c = (Component)ev; }
	}
	public Component getComponent() { return(c); }
}
