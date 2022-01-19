package bridge;

public class MouseWheelEvent  extends MouseEvent {

	public MouseWheelEvent(Object s, int xp, int yp, int b) {
		super(s, xp, yp, b);
	}

	public int getWheelRotation() { return(0); }

}
