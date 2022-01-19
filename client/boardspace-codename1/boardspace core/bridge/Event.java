package bridge;

public class Event {
	Object source = null;
	int x;
	int y;
	public Event() { };
	public Event(Object ob,int ax,int ay) 
		{ source = ob; x=ax; y=ay; 
		};
	public int getX() { return(x); }
	public int getY() { return(y); }
	public Object getSource() { return(source); }
	public String paramString() { return(""+this); }

}
