package bridge;

public class MouseEvent extends Event 
{
	int button = 0;
	public MouseEvent(Object s,int xp,int yp,int b) { super(s,xp,yp); button=b;}
	public int getButton() { return(button); }
	public int getModifiersEx() { return(button); }
}