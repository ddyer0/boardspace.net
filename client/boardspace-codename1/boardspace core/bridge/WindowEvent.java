package bridge;

public class WindowEvent extends Event {
	public WindowEvent(Component component) { source = component; }
	public Window getWindow() { return((Window)getSource()); }
}
