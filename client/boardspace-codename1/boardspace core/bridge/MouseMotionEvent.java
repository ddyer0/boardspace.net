package bridge;

public class MouseMotionEvent extends Event 
{
	int x;
	int y;
	int button = 0;
	public MouseMotionEvent(Object s,int xp,int yp,int b) { super(s,xp,yp); button=b;}
	public int getButton() { return(button); }

}
