package bridge;

public class ActionEvent extends Event {
	public ActionEvent(Object t,String c,int x,int y) { super(t,x,y); command=c; }
	public ActionEvent(Object t,int eventType,String c) { super(t,0,0); command=c; }
	String command = null;
	public Object getCommand() { return(command); }
	public String getActionCommand() { return(command); }
	public static int ACTION_PERFORMED = 1001;
}
