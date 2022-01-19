package lib;

/* internal storage for a mouse event */
public class CanvasMouseEvent
{	public CanvasMouseEvent next = null;	// next event after this one
	public int x;							// the x
	public int y;							// the y
	public MouseState event;				// event type
	public int button;						// buttons that were down
	public long date;						// timestamp
	public double amount;					// amount of pinch
	public double twist;					// angle of twist
	public boolean first;
		// constructor
	public CanvasMouseEvent(MouseState e,int ax,int ay,int ab,long d,double am,double tw,MouseState prev)
		{
			event = e;
			x = ax;
			y = ay;
			button = ab;
			date = d;
			amount = am;
			twist = tw;
			first = e!=prev;
		}
		// merge this event with a subsequent event of the same type
	public void merge(int ax,int ay,long d,double am,double tw)
		{
			x = ax;
			y = ay;
			date = d;
			amount = am;
			twist = tw;
		}
	}
