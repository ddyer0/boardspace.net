package bridge;

public class MouseWheelEvent  extends MouseEvent {

	public MouseWheelEvent(Object s, int xp, int yp, int b,int a) {
		super(s, xp, yp, b);
		amount = a;
	}
	int amount = 0;
	public int getWheelRotation() { return(amount); }

}
