package lib;


import bridge.Color;
import bridge.Config;
import bridge.FontMetrics;
import online.common.exCanvas;
import static online.common.OnlineConstants.clickSound;

import com.codename1.ui.geom.Rectangle;


/** pop up keyboard */
public class Keyboard implements Config
{
	
	static double whiteScale[]={0.5,0.5,1};
	static double doubleScale[]={0.5,0.5,1.7};
	static public StockArt KeytopW = StockArt.Make("keytop-wide",doubleScale);
	static public StockArt Keytop = StockArt.Make("keytop",whiteScale);
	static public StockArt KeytopLeft = StockArt.Make("keytop-left",whiteScale);
	static public StockArt KeytopRight = StockArt.Make("keytop-right",whiteScale);
	static public StockArt KeytopCenter = StockArt.Make("keytop-center",whiteScale);
	static public StockArt Keyboard = StockArt.Make("keyboard",whiteScale);
	static StockArt[]artwork = {KeytopW,Keytop,Keyboard,KeytopLeft,KeytopRight,KeytopCenter };
	static boolean imagesLoaded = false;
	private int context = -1;
	
	public void setContext(int n) { context = n; }
	public int getContext() { return(context); }
	static void loadImages(ImageLoader showOn) 
	{	if(!imagesLoaded)
		{
		StockArt.load_masked_images(showOn, IMAGEPATH,artwork);
		imagesLoaded = true;
		}
	}
	class CalculatorButtonStack extends OStack<CalculatorButton>
	{
		public CalculatorButton[] newComponentArray(int sz) { return(new CalculatorButton[sz]);		}
	}
	public void setKeyboardLayout(KeyboardLayout l)
	{	if(l!=activeLayout)
			{
			activeLayout = l;
			buttonList = null;
			}
	}
	public void selectLayout()
	{	if(fixedLayout && activeLayout!=null) { return; }	
		KeyboardLayout active = activeLayout;
		
		KeyboardLayout newlayout = 
				narrow ? symbol ? KeyboardLayout.Narrow_Symbol 
						: shift ? KeyboardLayout.Narrow_Upper : KeyboardLayout.Narrow_Lower
						: shift ? KeyboardLayout.Normal_Upper : KeyboardLayout.Normal_Lower;
		if(newlayout!=active)
		{
			setKeyboardLayout(newlayout);
		}
	}
	public KeyboardLayout activeLayout()
	{
		if(activeLayout==null)
		{	selectLayout();
		}
		return activeLayout;
	}
	public void redoLayout()
	{	KeyboardLayout layout = activeLayout();
		String keytops[][] = layout.getKeyMap();
		double buttonw = layout.getButtonW();
		double buttonh = layout.getButtonH();
		CalculatorButtonStack buttons = new CalculatorButtonStack();
		
		double rowSpace = 0.00;

		double row1Y = buttonh ;
		double row1X = buttonw*5/4;
		
	    for(int row=0;row<keytops.length;row++)
	    {	double rowx =row1X;
	    	String rowChars[] = keytops[row];
	    	double rowy = row1Y + row*(buttonh+rowSpace);
	    	for(int col=0;col<rowChars.length;col++)
	    	{
	    		String top = rowChars[col];
	    		CalculatorButton.id id = CalculatorButton.id.find(top);
	    		G.Assert(id!=null,"not found: %s",top);
	    		double dx = id.dx*buttonw;
	    		double dy = id.dy*buttonw;
	    		//double dh = id.dh;
	    		double dw = id.dw;
	    		rowx += dx;
	    		rowy += dy;
	    		if(id.ival==' ') {
	    			buttons.push(new CalculatorButton(id,KeytopLeft,KeytopCenter,KeytopRight,
	    					rowx,rowy,buttonw*dw,buttonh));
	    		}
	    		else if(dw<1.5)
	    		{
	    			buttons.push(new CalculatorButton(id,Keytop,rowx,rowy,buttonw));
	    		}
	    		else 
	    		{ buttons.push(new CalculatorButton(id,KeytopW,rowx+(dw-1)*buttonw/2,rowy,buttonw)); 
	    		}
	    		rowx += buttonw*dw + (dy==0 ? buttonw*dx : 0);
	    	}
	    }
	    buttonList = buttons.toArray();
	}
	public CalculatorButton[] buttonList() 
	{
		if(buttonList==null) { redoLayout(); }
		return buttonList;
	}
	
	public boolean containsPoint(HitPoint p)
	{
		return(G.pointInRect(p, crect));
	}
	public boolean containsPoint(int x,int y)
	{
		return(G.pointInRect(x,y, crect));
	}
	
	exCanvas showOn=null;
	Rectangle crect = new Rectangle();
	public void setBounds(Rectangle r)
	{
		crect = r;
		buttonList = null;
	}
	boolean fixedLayout =  false;
	boolean fixedBounds = false;
	private KeyboardLayout activeLayout = null;
	private CalculatorButton buttonList[] = null;
	
	TextContainer display = null;
	TextContainer targetDisplay = null;		// the originally specified display
	boolean includeDisplay = false;
	public boolean cancel = false;
	public boolean done = false;
	public boolean shift = false;
	public boolean control = false;
	public boolean shiftLock = false;
	public boolean symbol = false;
	public boolean closed = false;
	public boolean narrow = false;
	public static String formatDisplay(String n)
	{	int len = n.length();
		if(len<=3) { return(n); }
		else return(formatDisplay(n.substring(0, len-3))
						+","
						+ n.substring(len-3));
	}
	public void resizeAndReposition()
	{	
		if(fixedBounds) { return; }
		
		TextContainer dis = targetDisplay;
		includeDisplay = false;
		Rectangle parentBounds = showOn.getRotatedBounds();
		FontMetrics fm = G.getFontMetrics(showOn.largeBoldFont());
		int lineH = fm.getHeight();
		int feature = G.minimumFeatureSize();
		int newW = G.Width(parentBounds);
		int newH = Math.min(20*lineH, Math.min(G.Height(parentBounds),Math.max(lineH*15,newW/2)));
		narrow = newW<feature*20;
		selectLayout();
		int newY;
		int newX = G.Left(parentBounds);
		newW = Math.min((int)(newH*2.5), newW);
		if(dis!=null)
		{
			newH = (int)(newW*(narrow?0.55:0.45));
			Rectangle dr = dis.getBounds();
			newY = G.Bottom(dr);
			if(newY>G.centerY(parentBounds) && newY+newH>G.Bottom(parentBounds))
			{
				newY = Math.max(G.Top(parentBounds),G.Top(dr)-newH);
			}
			newY = Math.min(newY, G.Bottom(parentBounds)-newH);
			newX = Math.max(G.Left(parentBounds),Math.min(G.centerX(dr)-newW/2,G.Right(parentBounds)-newW));
			Rectangle pos = new Rectangle(newX,newY,newW,newH);
			dis.setEditable(showOn,true);
			if(pos.intersects(dr))
			{	// if we end up overlapping the display, incorporate it
				dis = null;
			}	
			setBounds(new Rectangle(newX,newY,newW,newH)); 
			
		}		
		if(dis==null)
		{ 
		  dis = new TextContainer(CalculatorButton.id.Display);
		  dis.singleLine = true;
		  dis.setEditable(showOn,true);
		  newH = (int)(newW*(narrow?0.55:0.5));
		  newY = G.centerY(parentBounds)-newH/2;
		  if(targetDisplay!=null) 
		  	{ String msg = targetDisplay.getText();
		  	  dis.setText(msg);
		  	  dis.setCaratPosition(msg.length());
		  	}
	  	  dis.setFocus(true);
		  setBounds(new Rectangle(newX,newY,newW,newH));
		  display = dis;		// make sure display is set before includeDisplay
		  includeDisplay = true;
		}			
		display = dis;
		

	}
	// constructor
	public Keyboard(exCanvas see,TextContainer dis)
	{
		this(see,dis,null,null);
	}
	public Keyboard(exCanvas see,TextContainer dis,Rectangle r,KeyboardLayout lay)
	{	showOn = see;
		targetDisplay = display = dis;
		if(lay!=null) { fixedLayout = true; activeLayout = lay; }
		if(r!=null) { fixedBounds = true; setBounds(r); }
		else {	resizeAndReposition(); }
	}
	public void MouseDown(HitPoint hp)
	{
		if(!hp.dragging)
		{
			handleKey(hp);	
		}
	}
	public void StartDragging(HitPoint hp)
	{	// when we embed the display line, pass mouse drag events to it.
		if(includeDisplay)
			{  boolean drag = display.doMouseDrag(G.Left(hp),G.Top(hp));
			   hp.dragging = drag;
			}

	}
	// sawDown is a semi-kludge to ignore the apparent keystroke that occurs
	// when the keyboard has just appeared, and the mouse is somewhere inside
	// the area of the new keyboard.  It will see a mouse up for which there
	// was no mouse down, and should not count that as a keystroke.
	private boolean sawDown = false;
	public void doMouseMove(int ex, int ey,MouseState upcode)
	{	if(containsPoint(ex,ey))
		{
		if(upcode==MouseState.LAST_IS_DOWN) 
			{ sawDown = true; }
		if(includeDisplay) 
			{ display.doMouseMove(ex,ey,upcode); 
			}
		}
	}
	public void setClosed()
	{ 	if(targetDisplay!=null) { targetDisplay.setText(display.getText()); } 
		closed = true; 
	}
	
	public boolean handleKey(HitPoint hp)
	{
		if(sawDown && (hp.hitCode instanceof CalculatorButton.id))
		{	//display.doMouseUp();
			//Plog.log.addLog("Key ",hp);
			CalculatorButton.id bcode = (CalculatorButton.id)hp.hitCode;
			sawDown = false;
			if(showOn!=null && showOn.doSound()) { SoundManager.playASoundClip(clickSound,100); }
			display.setFocus(true);
			if((bcode.ival>=' ') && (bcode.ival<0xff)) 
				{
				char ch = (char)(bcode.ival);
				if(control)
				{
				 control = false;
				 switch(ch)
				 {
				 case 'I': case 'i':
					 display.insert('\t');
					 break;
				 case 'M': case 'm': case 'N': case 'n':
					 closed |= display.doSend();
					 break;
				 case 'A': case 'a':
					 display.selectAll();
					 break;
				 case 'C': case 'c':
					 display.doCopy();
					 break;
				 case 'V': case 'v':
					 display.doPaste();
					 break;
				 case 'E': case 'e':
					 display.doToEnd();
					 break;
					 
				 }
				}
				else
				{
				  display.insert(ch); 
				  if(control||shift)
				  {
					  control = false;
					  shift = false;
					  selectLayout();
				  }
				}}
			else {
				switch(bcode)
				{
				case Ntab: 
					display.insert('\t');
					break;
				case CloseKeyboard:
				case NarrowCloseKeyboard:
					setClosed();
					break;
				case Nenter:
					if((targetDisplay!=null)&&(targetDisplay!=display)) 
						{ targetDisplay.setText(display.getText()); 
						  closed |= targetDisplay.doSend();
						  display.clear();
						}	
						else 
						{ closed |= display.doSend();
						}
					break;
				case NAlpha:
					symbol = false;
					shift = false;
					selectLayout();
					break;
				case NSymbol:
					symbol = !symbol;
					selectLayout();
					break;
				case Nshift:
					shift = !shift;
					selectLayout();
					break;
				case Ncontrol:
					control = !control;
					selectLayout();
					break;
				case Ncaps:
					shiftLock = !shiftLock;
					shift = shiftLock;
					selectLayout();
					break;
				case Ndel:
					display.doDel(false);
					break;
				case Clear: 
					display.setText("");
					break;
				case Ok:
					done = true;
					break;
				case Cancel:
					cancel = true;
					done = true;
					break;
				case Nleft:
					display.doBack();
					break;
				case Nright:
					display.doForward();
					break;
				default: G.print("hit unknown button ",bcode);
				
				}
			}
			return(true);
		}
		return false;
	}
	public boolean StopDragging(HitPoint hp)
	{	// keyboard activates on key down, we just need to ignore them
		return (hp.hitCode instanceof CalculatorButton.id);
	}
	
	public void drawButton(Graphics gc,CalculatorButton button,HitPoint highlight,Rectangle cr)
	{
		switch(button.value)
		{
		case NSymbol:
			button.textColor = symbol ? Color.blue : Color.black;
			break;
		case Ncontrol:
			button.textColor = control ? Color.blue : Color.black;
			break;
		case Nshift:
			button.textColor = shift ? Color.blue : Color.black;
			break;
		case Ncaps:
			button.textColor = shiftLock ? Color.blue : Color.black;
			break;
		default: break;
		}
		
		if(button.value.ival>0) 
			{ button.draw(gc,showOn,highlight,cr); 
			}
	}
	
	public void draw(Graphics gc,HitPoint highlight)
	{	loadImages(showOn.loader);
	
		if(G.pointInRect(highlight, crect)) 
			{ // this makes sure that everything under the keyboard is inactive.
			highlight.neutralize();
			}
		int w = G.Width(crect);
		int h = G.Height(crect);
		int left = G.Left(crect);
		int top = G.Top(crect);
		Rectangle drect = crect;
		if(includeDisplay)
		{				
	    	FontMetrics fm = G.getFontMetrics(display.font);
	    	int fontH = fm.getHeight();
	    	int dtop = top+(int)(0.08*h);
			int dhgt = fontH*2;
					
	    	display.setBounds(left+(int)(0.1*w),
	    					  dtop,
	    					  (int)(0.8*w),
	    					  dhgt);
			
			drect = new Rectangle(left,dtop+dhgt-(int)(0.04*h),w,h-dhgt);
		}
		int lmargin = (int)(w*0.026);
		int tmargin = (int)(w*0.017);
		int bwidth = (int)(w*0.048);
		int bheight = (int)(w*0.037);
		Rectangle cr = GC.setClip(gc,left+lmargin,top+tmargin,w-bwidth,h-bheight);
		GC.fillRect(gc,Color.lightGray,left+lmargin, top+tmargin,w-bwidth,h-bheight);
		Image im = Keyboard.getImage();
		if(im!=null) { im.drawImage(gc,left,top,w,h); }
	  	GC.setClip(gc,cr);
    	GC.setFont(gc, showOn.largeBoldFont());
    	for(CalculatorButton button : buttonList())
    	{	
    		drawButton(gc,button,highlight,drect);  
    	}
    	if(includeDisplay)
    	{
	    	display.setVisible(true);
	    	display.redrawBoard(gc,highlight);
    	}

	}
}
