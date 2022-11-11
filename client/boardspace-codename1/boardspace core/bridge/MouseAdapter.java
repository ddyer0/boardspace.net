package bridge;

/**
 * this class adapts Codename1 events to AwtEvents.   Events intended for codename1 windows
 * are translated into Awt events and dispatched to the intended recepients.  This allows 
 * windows to operate in ignorance of the detailed differences between codename1 events and
 * awt events.
 * 
 */
import java.util.Stack;
import java.util.Vector;

import lib.AwtComponent;
import lib.G;
import lib.Log;
import lib.PinchEvent;

import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Button;
import com.codename1.ui.events.ActionEvent.Type;
import static com.codename1.util.MathUtil.atan2;


@SuppressWarnings("rawtypes")
public class MouseAdapter 
	implements com.codename1.ui.events.ActionListener,com.codename1.ui.events.SelectionListener
{	public static int instanceNumber = 0;
	public int id = instanceNumber++;
	public boolean twoFinger = false;		// currently in two fingered gesture
	public boolean recordHistory = false;	// debugging only, record low level mouse gestures and print them in batches
	public boolean drag_is_pinch = false;	// debugging only, true to turn all drags into pinches, for testing in the simulator
	enum bType { Up, Down, Pinch, Multi};
	public bType mouseState = bType.Up;
	
	// codename1 calls actionPerformed with identical events for all three types.
	class PressedListener implements com.codename1.ui.events.ActionListener
		{ public void actionPerformed(com.codename1.ui.events.ActionEvent e) 
			{ 
			  startWheeling(e);
			}
		}
	class ReleasedListener implements com.codename1.ui.events.ActionListener
		{ public void actionPerformed(com.codename1.ui.events.ActionEvent e) 
			{ 
			  if(isWheeling(e,true))
			  {	
				wheelAction(e); 
			  }
			  else { releasedAction(e); } 
			}
		}
	class DragListener implements com.codename1.ui.events.ActionListener
	{ public void actionPerformed(com.codename1.ui.events.ActionEvent e) 
		{ if(!isWheeling(e,false)) { dragAction(e); } 
		}
	}
	
	class SFocusListener implements com.codename1.ui.events.FocusListener
	{ 	public void focusGained(Component cmp) {
			focusAction(cmp,true);
		}
		public void focusLost(Component cmp) {
			focusAction(cmp,false);
		}
	}
	
	Component cn1Component;		// the system window that receives Codename1 events
	AwtComponent awtComponent;		// the boardspace window that receives AWT events
	
	private PressedListener pressedListener = null;
	PressedListener getPressedListener() 
	{ if(pressedListener==null) 
		{ pressedListener=new PressedListener(); 
		  systemPressedListeners.push(pressedListener); 
		}
	  return(pressedListener); 
	}
	private ReleasedListener releasedListener = null;
	ReleasedListener getReleasedListener() 
	{ if(releasedListener==null) 
		{ releasedListener=new ReleasedListener();
		  systemReleasedListeners.push(releasedListener);
		}
	  return(releasedListener); 
	}
	private DragListener dragListener = null;
	DragListener getDragListener() 
	{ if(dragListener==null) 
		{ dragListener=new DragListener();
		  systemDragListeners.push(dragListener);
		}
	  return(dragListener); 
	}
	
	
	private SFocusListener focusListener = null;
	SFocusListener getFocusListener()
	{ if(focusListener==null) 
		{ focusListener=new SFocusListener();
		  systemFocusListeners.push(focusListener);
		}
	  return(focusListener); 	
	}
	
	public MouseAdapter(Component c) 
	{ cn1Component = c; 
	  awtComponent = (AwtComponent)c;
	}
	public void setAwtComponent(AwtComponent w) { awtComponent = w; }
	public MouseAdapter(Component cn1,AwtComponent awt) { awtComponent = awt; cn1Component = cn1; }
	
	Vector<ItemListener>itemListeners = null;
	Vector<ListSelectionListener>listSelectionListeners = null;
	Vector<MouseListener>mouseListeners = null;
	Vector<MouseWheelListener>mouseWheelListeners = null;
	Vector<MouseMotionListener>mouseMotionListeners = null;
	Vector<ActionListener>actionListeners = null;
	Vector<FocusListener>focusListeners = null;
	
	public int lastMouseX = 0;
	public int lastMouseY = 0;
	public bType lastMouseButton = null;
	public long lastMouseTime = 0;
	private long wheelStartTime = 0;
	private int wheelStartX = 0;
	private int wheelStartY = 0;
	
	// DETECT_WHEEL is an ad-hoc means to detect scroll wheel usage where they
	// masquerade as fast up/down sequences, which is the way Codename1 presents
	// them.   When a "down" event occurs, nothing happens immediately.  This might
	// have the undesirable effect of making it impossible to have an active-on-down
	// action.  When subsequent "drag" or "up" events arrive, and the current coordinates
	// and elapsed time are not compatible with a fast scroll even, the delayed "down"
	// event is issued and the detection is terminated.  Scroll wheel events are
	// always very fast, with the "up" following the "down" by a few millisecons.
	// the X coordinate doesn't change, and the Y coordinate changes by a lot.
	//
	private boolean USE_WHEEL = !G.isSimulator();	// so it behaves like real android
	private boolean DETECT_WHEEL = !USE_WHEEL || G.isRealWindroid();

	private Stack<PressedListener> systemPressedListeners = new Stack<PressedListener>();
	private Stack<ReleasedListener> systemReleasedListeners = new Stack<ReleasedListener>();
	private Stack<DragListener> systemDragListeners = new Stack<DragListener>();
	private Stack<SFocusListener>systemFocusListeners = new Stack<SFocusListener>();
	
	private void startWheeling(com.codename1.ui.events.ActionEvent e)
	{	
		if(DETECT_WHEEL)
		{
		boolean isForSure = USE_WHEEL && Display.getInstance().isScrollWheeling();
		if(isForSure) { DETECT_WHEEL = false; }
		else {
		wheelStartTime = G.Date();
		wheelStartX = e.getX();
		wheelStartY = e.getY();
		//Plog.log.addLog("\nStart ",wheelStartTime," ",wheelStartX," ",wheelStartY);
		}}
		if(!DETECT_WHEEL) { pressedAction(e); }
	}
	
	public boolean isWheeling(com.codename1.ui.events.ActionEvent e,boolean release)
	{	boolean isForSure = USE_WHEEL && Display.getInstance().isScrollWheeling();
		if(isForSure) { DETECT_WHEEL=false; return true; }
		if(DETECT_WHEEL && wheelStartTime>0 )
		{
		//Plog.log.addLog("is ",release);
	    long now = G.Date();
		boolean more = (wheelStartTime>0)
						&& (e.getX()==wheelStartX)
						//&& (drag || ( wheelDragEvents>0))
						&& (!release || (Math.abs(e.getY()-wheelStartY)>10))
						&& ((now-wheelStartTime)<200);
		if(!more)
			{	// we owe a press action
				//Plog.log.addLog("S t=",now-wheelStartTime," ",e.getX()-wheelStartX,",",e.getY()-wheelStartY);
				wheelStartTime = 0;
				pressedAction(e);
			}
		return more;
		}
		return false;
	}
	
	// unlink this from the various windows and listeners to help the GC
	public void removeListeners()
	{
		while(!systemPressedListeners.isEmpty())
			{ cn1Component.removePointerPressedListener(systemPressedListeners.pop()); 
			}
	
		while(!systemReleasedListeners.isEmpty())
		{ cn1Component.removePointerReleasedListener(systemReleasedListeners.pop()); 
		}
	
		while(!systemDragListeners.isEmpty())
		{ cn1Component.removePointerDraggedListener(systemDragListeners.pop()); 
		}
		
		while(!systemFocusListeners.isEmpty())
		{
			cn1Component.removeFocusListener((com.codename1.ui.events.FocusListener) systemFocusListeners.pop());
		}
		
		removeSystemActionListeners();
		
		itemListeners.clear();
		mouseListeners.clear();
		mouseMotionListeners.clear();
		actionListeners.clear();

		awtComponent = null;
		cn1Component = null;
	}
	
	public void addSystemListeners()
	{
		if(cn1Component!=null) 
		{ cn1Component.addPointerPressedListener(getPressedListener());
		  cn1Component.addPointerReleasedListener(getReleasedListener());
		  cn1Component.addPointerDraggedListener(getDragListener()); 
		  cn1Component.addFocusListener(getFocusListener());
		  // no specific support for mouse wheel listeners, rolled into pressed and released
		}
	}
	
	public void addActionListener(ActionListener m)
	{
		if(actionListeners==null) { actionListeners = new Vector<ActionListener>(); }
		if(!actionListeners.contains(m)) { actionListeners.addElement(m); }
		addSystemActionListener();
	}
	private void removeSystemActionListeners()
	{
		// a grab bag of other classes also allow actionlistners
		// including TextArea and 
		if(cn1Component instanceof com.codename1.ui.TextField)
			{ com.codename1.ui.TextField l = (com.codename1.ui.TextField)cn1Component;
			  l.setDoneListener(null);
			}
		if(cn1Component instanceof com.codename1.ui.List)
		{
			com.codename1.ui.List<?> l = (com.codename1.ui.List<?>)cn1Component;
			l.removeSelectionListener(this);
		}
		if(cn1Component instanceof ActionProvider)
		{	// this adds the actionlistener for the native codename1 window 
			ActionProvider b = (ActionProvider)cn1Component;
			b.removeActionListener(this);
		}else
		{
		if(cn1Component!=null) 
			{ cn1Component.removePointerReleasedListener(this); 
			}
		}
	
	}
	
	
	private void addSystemFocusListener()
	{
		cn1Component.addFocusListener(getFocusListener());
	}
	
	private void addSystemActionListener()
	{
		// a grab bag of other classes also allow actionlistners
		// including TextArea and 
		if(cn1Component instanceof com.codename1.ui.TextField)
			{ com.codename1.ui.TextField l = (com.codename1.ui.TextField)cn1Component;
			  l.setDoneListener(this);
			}
		if(cn1Component instanceof com.codename1.ui.List)
		{
			com.codename1.ui.List<?> l = (com.codename1.ui.List<?>)cn1Component;
			l.addSelectionListener(this);
		}
		if(cn1Component instanceof ActionProvider)
		{	// this adds the actionlistener for the native codename1 window 
			ActionProvider b = (ActionProvider)cn1Component;
			b.addActionListener(this);
		}else
		{
		if(cn1Component!=null) 
			{ cn1Component.addPointerReleasedListener(this); }
		}
	}
	public void addItemListener(ItemListener m) 
	{
		if(itemListeners == null) { itemListeners = new Vector<ItemListener>(); }
		if(!itemListeners.contains(m)) 
			{ itemListeners.addElement(m); 
			  addSystemActionListener(); 
			}
	}
	public void addListSelectionListener(ListSelectionListener m) 
	{
		if(listSelectionListeners == null) { listSelectionListeners = new Vector<ListSelectionListener>(); }
		if(!listSelectionListeners.contains(m)) 
			{ listSelectionListeners.addElement(m); 
			  addSystemActionListener(); 
			}
	}
	public void addMouseListener(MouseListener m)
	{	
		if(mouseListeners==null) { mouseListeners = new Vector<MouseListener>(); }
		if(!mouseListeners.contains(m)) { mouseListeners.addElement(m); }
		addSystemListeners();
	}
	
	public void removeMouseListener(MouseListener m)
	{
		if(mouseListeners!=null) { mouseListeners.remove(m); }
	}
	
	public void addMouseWheelListener(MouseWheelListener m)
	{	
		if(mouseWheelListeners==null) { mouseWheelListeners = new Vector<MouseWheelListener>(); }
		if(!mouseWheelListeners.contains(m)) { mouseWheelListeners.addElement(m); }
		addSystemListeners();
	}
	public void removeMouseWheelListener(MouseWheelListener m)
	{
		if(mouseWheelListeners!=null) { mouseWheelListeners.remove(m); }
	}
	
	public void removeMouseMotionListener(MouseMotionListener m)
	{
		if(mouseMotionListeners!=null) { mouseMotionListeners.remove(m); }
	}
	public void handleMouseEvent(com.codename1.ui.events.ActionEvent e,bType down)
	{	
		int thisX = cn1Component.getAbsoluteX();
		int thisY = cn1Component.getAbsoluteY();
		int x = e.getX()-thisX;
		int y = e.getY()-thisY;
		handleMouseEvent(x,y,down);
	}
	public void handleMouseEvent(int x,int y,bType down)
	{
		//Plog.log.addLog("mouseevent ",x," ",y," ",down);
		if(mouseListeners!=null)
		{
		for(int idx = 0;idx<mouseListeners.size();idx++)
			{	MouseListener listener = mouseListeners.elementAt(idx);
				MouseEvent ev = new MouseEvent(awtComponent,x,y,down==bType.Up ? 0 : 1);
				switch(down)
				{
				case Down: listener.mousePressed(ev);  break;
				case Up: listener.mouseReleased(ev); break;
				default: break;
				}
			}}
		lastMouseX = x;
		lastMouseY = y;
		lastMouseButton = down;
		if(recordHistory)
		{	lastMouseTime = G.Date();
			Log.restartLog();
			Log.appendNewLog("M");
			Log.appendLog("#");
			Log.appendLog(id);
			Log.appendLog(" ");
			Log.appendLog(down.toString());
			Log.appendLog(" ");
			Log.appendLog(x);
			Log.appendLog(" ");
			Log.appendLog(y);
			Log.appendLog("r\n");
			if(down==bType.Up) 
			{ Log.finishLog();
			}
		}
	}

	public void addMouseMotionListener(MouseMotionListener m)
	{
		if(mouseMotionListeners==null) { mouseMotionListeners = new Vector<MouseMotionListener>(); }
		if(!mouseMotionListeners.contains(m)) { mouseMotionListeners.addElement(m); }
		addSystemListeners();
	}
	public void handleMouseMotionEvent(com.codename1.ui.events.ActionEvent e,bType down)
	{	handleMouseMotionEvent(e.getX(),e.getY(),down,0,0);
	}
	public void handleMouseMotionEvent(int rawX,int rawY,bType down,double amount,double angle)
	{	
		int thisX = cn1Component.getAbsoluteX();
		int thisY= cn1Component.getAbsoluteY();
		int x = rawX-thisX;
		int y = rawY-thisY;
		//Plog.log.addLog("Move ",rawX," ",rawY," ",down);
		if(recordHistory)
		{	Log.restartLog();
			Log.appendNewLog("D");
			Log.appendLog("#");
			Log.appendLog(id);
			Log.appendLog(" ");
			Log.appendLog(down.toString());
			Log.appendLog(" ");
			Log.appendLog(x);
			Log.appendLog(" ");
			Log.appendLog(y);
			Log.appendLog(" ");
			if(down==bType.Pinch) 
				{ Log.appendLog(amount);
				  Log.appendLog(' ');
				  Log.appendLog(angle);
				  Log.appendLog(' ');
				}
			Log.appendLog(G.Date()-lastMouseTime);
			Log.appendLog("r\n");
		}
		//down = bType.Pinch;
		//amount = 1.0;
		if(mouseMotionListeners!=null)
		{
		for(int idx = 0;idx<mouseMotionListeners.size();idx++)
			{
				MouseMotionListener listener = mouseMotionListeners.elementAt(idx);
				switch(down)
				{
				case Multi:
					break;
				case Down:
					{
						MouseEvent ev = new MouseEvent(awtComponent,x,y,1);
						if((x!=lastMouseX) || (y!=lastMouseY))
						{ listener.mouseDragged(ev);
						}
					}
					break;
				case Up:
					{
					MouseEvent ev = new MouseEvent(awtComponent,x,y,1);
					listener.mouseMoved(ev);
					}
					break;
				case Pinch:
					{
					//Log.addLog("Pinchevent "+x+","+y+" "+amount+" raw "+rawX+","+rawY);
					PinchEvent ev = new PinchEvent(awtComponent,amount,x,y,angle);
					listener.mousePinched(ev);
					}
					break;
				}
			}}
		lastMouseX = x;
		lastMouseY = y;

	}
	public String getCommand(Type eventType)
	{
		if(cn1Component!=null) 
		{	if(cn1Component instanceof TextField)
			{	// for "done" events, return the action string
				// otherwise return "".  For example, this is the case for PointerReleased events
				String str = (eventType==Type.Done) ? ((TextField)cn1Component).getCommand() : "";
				return(str);
			}
			if(cn1Component instanceof Button)
			{
				Button com = (Button)cn1Component;
				Command comm = com.getCommand();
				if(comm!=null) { return(comm.getCommandName()); }
			}
		}
		return(null);
	}
	public void handleActionEvent(com.codename1.ui.events.ActionEvent e)
	{	
		//System.out.println("actionevent "+e.getEventType().toString());
		handleActionEvent(e.getX(),e.getY(),e.getEventType());
	}
	public void handleWheelEvent(int x,int y,int direction)
	{
		if(mouseWheelListeners!=null)
		{
			for(int idx = 0;idx<mouseWheelListeners.size();idx++)
			{
				bridge.MouseWheelListener listener = mouseWheelListeners.elementAt(idx); 
				//Plog.log.addLog("dispatch action to ",listener);
				MouseWheelEvent e = new MouseWheelEvent(awtComponent,x,y,0,direction);
				listener.mouseWheelMoved(e);
			}		
		}
	}
	public void handleActionEvent(int x,int y,Type type)
	{
		if(actionListeners!=null)
		{
			for(int idx = 0;idx<actionListeners.size();idx++)
			{
				bridge.ActionListener listener = actionListeners.elementAt(idx); 
				//G.print("dispatch action to "+listener);
				ActionEvent ev = new ActionEvent(awtComponent,getCommand(type),x,y);
				listener.actionPerformed(ev);
			}
		}
	}
	
	public void handleItemEvent(com.codename1.ui.events.ActionEvent e)
	{	
		if(itemListeners!=null)
		{
		int thisX = cn1Component.getAbsoluteX();
		int thisY = cn1Component.getAbsoluteY();
		int x = e==null ? 0 : e.getX()-thisX;
		int y = e==null ? 0 : e.getY()-thisY;
		for(int idx = 0;idx<itemListeners.size();idx++)
			{
				ItemListener listener = itemListeners.elementAt(idx);
				ItemEvent ev = new ItemEvent(awtComponent,x,y);
				listener.itemStateChanged(ev);
			}
		}
		if(listSelectionListeners!=null)
		{
		int thisX = cn1Component.getAbsoluteX();
		int thisY = cn1Component.getAbsoluteY();
		int x = e==null ? 0 : e.getX()-thisX;
		int y = e==null ? 0 : e.getY()-thisY;
		for(int idx = 0;idx<listSelectionListeners.size();idx++)
			{
				ListSelectionListener listener = listSelectionListeners.elementAt(idx);
				ListSelectionEvent ev = new ListSelectionEvent(awtComponent,x,y);
				listener.valueChanged(ev);
			}
		}
	}
	public void pressedAction(com.codename1.ui.events.ActionEvent evt)
	{	if(!twoFinger)
		{
		handleMouseEvent(evt,bType.Down);
		}
	}
	public void releasedAction(com.codename1.ui.events.ActionEvent evt)
	{	twoFinger = false;
		handleMouseEvent(evt,bType.Up);
	}
	public void dragAction(com.codename1.ui.events.ActionEvent evt)
	{	handleMouseMotionEvent(evt,bType.Down);
	}
	public void wheelAction(com.codename1.ui.events.ActionEvent evt)
	{	
		int endY = evt.getY();
		//Plog.log.addLog("Wheel ",wheelStartX," ",wheelStartY," ",wheelStartY-endY);
		handleWheelEvent(wheelStartX,wheelStartY,wheelStartY-endY);
	}
	// this is the codename1 actionperformed
	public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
		//G.print("Mouse adapter event "+evt);
		handleActionEvent(evt);
		handleItemEvent(evt);
		if(!evt.isLongEvent()) { handleMouseEvent(evt,bType.Up);}		
	}

	public void selectionChanged(int oldSelected, int newSelected) {
		if(oldSelected!=newSelected) { handleItemEvent(null); } 
	}
	
	// test data to do a little pinch/zoom in the simulator
	static int[][][]dragTestData = 
		{ {{400,410},{400,440}},
		  {{410,420},{310,360}},
		  {{510,520},{220,270}},
		  //{{130,130},{300,340}},
		  //{{140,140},{300,340}},
		  //{{150,150},{300,340}}

		};

	/**
	 * handling of 2 finger mouse pinch
	 */
	private double startingPinchDistance = 0;
	private double startingPinchAngle = 0;
	private double lastPinchScale = 0;
	private int lastPinchX1;
	private int lastPinchY1;
	private int lastPinchX0;
	private int lastPinchY0;
    public boolean pointerDragged(int[]x,int[]y)
    {	
    	if((mouseState==bType.Down) && drag_is_pinch && (x.length==1))
    	{	// this sets the initial point as a pair of points in a diagonal
    		int nx[]=new int[2];
    		int ny[]=new int[2];
    		nx[0]=x[0];
    		ny[0]=y[0];
    		nx[1]=x[0]-10;
    		ny[1]=y[0]-10;
    		x=nx;
    		y=ny;
    	}
    	
    	if(x.length==1 && mouseState!=bType.Pinch) { return true; }
    	
    	if(recordHistory)
    	{
    	Log.restartLog();
    	Log.appendNewLog("MultiDrag");
		Log.appendLog("#");
		Log.appendLog(id);
		Log.appendLog(" ");
    	Log.appendLog(x.length);
    	Log.appendLog(' ');
    	Log.appendLog(mouseState.toString());
    	Log.appendLog('\n');
    	}
        double currentAngle = atan2(lastPinchX0-lastPinchX1,lastPinchY0-lastPinchY1);
        switch(x.length)
    	{
    	default: //G.print("Fingers "+x.length);
    		mouseState = bType.Multi;
    		break;
    	case 2:
     		{
    		twoFinger = true;
    		lastPinchX0 = x[0];
            lastPinchY0 = y[0];
            lastPinchX1 = x[1];
            lastPinchY1 = y[1];
            double currentDis = G.distance(lastPinchX0,lastPinchY0,lastPinchX1,lastPinchY1);
            int cx = (lastPinchX0+lastPinchX1)/2;
            int cy = (lastPinchY0+lastPinchY1)/2;
            //Log.addLog("Raw "+lastPinchX0+","+lastPinchY0+" "+lastPinchX1+","+lastPinchY1+" ="+cx+","+cy);
            // prevent division by 0
            if (mouseState!=bType.Pinch) 
            	{
                startingPinchDistance = currentDis;
                startingPinchAngle = currentAngle;
                lastPinchScale = 1.0;
                mouseState = bType.Pinch;
            	}
            {
            double scale = lastPinchScale = currentDis / startingPinchDistance;
            handleMouseMotionEvent(cx,cy,bType.Pinch,scale,currentAngle-startingPinchAngle);
            }
    		}
    		break;
       	case 1:
       		// don't relay the single finger if we're continuing from a double finger 
       		if(mouseState==bType.Pinch)
       		{	// started to pinch and lifted one finger, continue pinch/drag mode
       			int nx0 = x[0];
       			int ny0 = y[0];
       			if(drag_is_pinch)
       			{ // hack for debugging without touch screens; continue the pinch
       			  // using 1 finger around the current center
       			int cx = (lastPinchX1+lastPinchX0)/2;
       			int cy = (lastPinchY1+lastPinchY0)/2;
       			int dx = nx0-cx;
       			int dy = ny0-cy;
       			lastPinchX1 = cx+dx;
       			lastPinchX0 = cx-dx;
       			lastPinchY1 = cy+dy;
       			lastPinchY0 = cy-dy;
          				double currentDis = G.distance(lastPinchX0,lastPinchY0,lastPinchX1,lastPinchY1);
           				lastPinchScale = currentDis / startingPinchDistance;
           				handleMouseMotionEvent(cx,cy,bType.Pinch,lastPinchScale,currentAngle-startingPinchAngle);
       			}
       			else
       			{
       			// started to pinch and lifted one finger, continue pinch/drag mode
       			int d0 = G.distanceSQ(lastPinchX0,lastPinchY0,nx0,ny0);
       			int d1 = G.distanceSQ(lastPinchX1,lastPinchY1,nx0,ny0);
       			// distance to the nearer finger
       			int dx = (d0<d1) ? nx0-lastPinchX0 : nx0-lastPinchX1 ;
       			int dy = (d0<d1) ? ny0-lastPinchY0 : ny0-lastPinchY1;
       			lastPinchX0 += dx;	// continue moving the other finger as a ghost
       			lastPinchY0 += dy;	// at the same distance
       			lastPinchX1 += dx;
       			lastPinchY1 += dy;
       			int cx = (lastPinchX0+lastPinchX1)/2;
       			int cy = (lastPinchY0+lastPinchY1)/2;
       			handleMouseMotionEvent(cx,cy,bType.Pinch,lastPinchScale,currentAngle-startingPinchAngle);
       			}
       		}
    		break;
    	}
        return false;
    }
    public boolean pointerDragged(int x,int y)
    {	
    	
    	if(!twoFinger) 
    	{
    	if(recordHistory)
			{
    		Log.restartLog();
    		Log.appendNewLog("Drag");
			Log.appendLog("#");
			Log.appendLog(id);
			Log.appendLog(" ");
    		Log.appendLog(x);
    		Log.appendLog(',');
    		Log.appendLog(y);
    		Log.appendLog('\n');
			}
	    	 mouseState = bType.Down; 
	    }
    	return(true);
    }
    
    // only when the first finger goes down
    public boolean pointerPressed(int x,int y)
    { 	
    	if(!twoFinger)
		{
    	if(recordHistory)
			{
    		Log.restartLog();
    		Log.appendNewLog("Down");
			Log.appendLog("#");
			Log.appendLog(id);
			Log.appendLog(" ");

    		Log.appendLog(x);
    		Log.appendLog(',');
    		Log.appendLog(y);
    		Log.appendLog('\n');
			}
    	switch(mouseState)
    	{
    	case Up: mouseState = bType.Down;
    	case Down:
    		return(true);
    	case Multi:
    	case Pinch:
    	}}
   		return(false);
   	 
    }

    
    // we get this only when the last finger goes up
    public boolean pointerReleased(int x,int y) 
    	{ 
    	
    	if(recordHistory)
    	{
    	Log.restartLog();
    	Log.appendNewLog("Up");
		Log.appendLog("#");
		Log.appendLog(id);
		Log.appendLog(" ");
    	Log.appendLog(x);
    	Log.appendLog(',');
    	Log.appendLog(y);
    	Log.appendLog('\n');
    	Log.finishLog();
    	}
    	mouseState = bType.Up;
    	twoFinger = false;
    	return(true);
    	}
    
    
	public void addFocusListener(FocusListener m)
	{
		if(focusListeners==null) { focusListeners = new Vector<FocusListener>(); }
		if(!focusListeners.contains(m)) { focusListeners.addElement(m); }
		addSystemFocusListener();
	}
	   
	public void removeFocusListener(FocusListener m)
	{
		if(focusListeners!=null)
		{ if(focusListeners.contains(m)) { focusListeners.remove(m); }
		}
	}
	public void focusAction(Component comp,boolean gained)
	{	if(focusListeners!=null)
		{
		for(FocusListener f : focusListeners)
			{
			if(gained) { f.focusGained(new FocusEvent(comp,0,0)); }
			else { f.focusLost(new FocusEvent(comp,0,0)); }
			}
		}
	}
	
}
