package bridge;

public class ItemEvent  extends Event {
	public ItemEvent(Object ev,int x,int y) { super(ev,x,y); }
	//public ItemSelectable getItemSelectable() 
	//{ return((ItemSelectable)source); 
	//}
	public Object getItem() { return(source);	}

}
