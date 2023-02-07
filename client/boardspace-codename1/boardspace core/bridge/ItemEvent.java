package bridge;

public class ItemEvent  extends Event {
	public ItemEvent(Object ev,int x,int y) { super(ev,x,y); }
	//public ItemSelectable getItemSelectable() 
	//{ return((ItemSelectable)source); 
	//}
	public static int DESELECTED = 1;
	public static int SELECTED = 2;
	public int getStateChange() { return SELECTED; }
	public Object getItem() { return(source);	}

}
