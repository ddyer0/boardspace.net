package lib;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Rectangle;

import bridge.Config;
import online.common.exCanvas;
import static online.common.OnlineConstants.clickSound;



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
		imagesLoaded = true;
		StockArt.load_masked_images(showOn, IMAGEPATH,artwork);
		}
	}
	class CalculatorButtonStack extends OStack<CalculatorButton>
	{
		public CalculatorButton[] newComponentArray(int sz) { return(new CalculatorButton[sz]);		}
	}
	CalculatorButton[] produceLayout(boolean up)
	{	CalculatorButtonStack buttons = new CalculatorButtonStack();
		double rowStart[] = {0.08,0.08,0.09,0.10,0.11};
		double row1Y = includeDisplay ?0.33 :  0.18 ;
		double buttonw = 0.062;
		double buttonh = buttonw*(includeDisplay ? 2.0: 2.5);
		double buttonSpace = 0.00;
		double rowSpace = 0.00;
		
	    String keytopsDown[][] = 
	    	{{"`","1","2","3","4","5","6","7","8","9","0","-","+","del"},
	    	 {"Tab","q","w","e","r","t","y","u","i","o","p","[","]","\\"},
	    	 {"Caps","a","s","d","f","g","h","j","k","l",";","'","Enter"},
	    	 {"Shift","z","x","c","v","b","n","m",",",".","/","\u2190","\u2192"  },
	    	 {"Ctrl","Bar","\u25bd"  }};
	    String keytopsUp[][] = 
	    	{{"~","!","@","#","$","%","^","&","*","(",")","_","+","del"},
	    	 {"Tab","Q","W","E","R","T","Y","U","I","O","P","{","}","|"},
	    	 {"Caps","A","S","D","F","G","H","J","K","L",":","\"","Enter"},
	    	 {"Shift","Z","X","C","V","B","N","M","<",">","?","\u2190","\u2192" },
	    	 {"Ctrl","Bar","\u25bd"}};
	    String keytops[][] = up ? keytopsUp : keytopsDown;
	    for(int row=0;row<keytops.length;row++)
	    {	double rowx = rowStart[row];
	    	String rowChars[] = keytops[row];
	    	double rowy = row1Y + row*(buttonh+rowSpace);
	    	for(int col=0;col<rowChars.length;col++)
	    	{
	    		String top = rowChars[col];
	    		CalculatorButton.id id = CalculatorButton.id.find(top);
	    		if(top.length()==1)
	    		{	if(id==CalculatorButton.id.Nleft) { rowx+=0.04; rowy+= 0.03; }
	    			if(id==CalculatorButton.id.NcloseKeyboard) { rowx += 0.13; rowy+= 0.05; }
	    			buttons.push(new CalculatorButton(id,Keytop,rowx,rowy,buttonw));
	    			rowx += buttonw+buttonSpace;
	    		}
	    		else if("Bar".equals(top))
	    		{	// special logic for the space bar
	    			double sp = buttonw*8.85;
	    			buttons.push(new CalculatorButton(id,KeytopLeft,KeytopCenter,KeytopRight,
	    					rowx,rowy,sp,buttonh));
	    			rowx += sp+0.03;
	    		}
	    		else {
	    			rowx += buttonw/3;
	    			buttons.push(new CalculatorButton(id,KeytopW,rowx,rowy,buttonw));
	    			rowx += buttonw+buttonw/4+buttonSpace;
	    		}
	    	}
	    }
	    return(buttons.toArray());
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
	Rectangle crect = null;
	boolean upperCase = false;
	CalculatorButton buttonsUp[]=null;
	CalculatorButton buttonsDown[] = null;
	TextContainer display = null;
	TextContainer targetDisplay = null;		// the originally specified display
	boolean includeDisplay = false;
	public boolean cancel = false;
	public boolean done = false;
	public boolean shift = false;
	public boolean control = false;
	public boolean shiftLock = false;
	public boolean closed = false;
	
	public static String formatDisplay(String n)
	{	int len = n.length();
		if(len<=3) { return(n); }
		else return(formatDisplay(n.substring(0, len-3))
						+","
						+ n.substring(len-3));
	}
	public void resizeAndReposition()
	{	TextContainer dis = targetDisplay;
		includeDisplay = false;
		Rectangle parentBounds = showOn.getBounds();
		FontMetrics fm = G.getFontMetrics(showOn.largeBoldFont());
		int lineH = fm.getHeight();
		int newW = G.Width(parentBounds);
		int newH = Math.min(20*lineH, Math.min(G.Height(parentBounds),Math.max(lineH*12,newW/2)));
		int newY;
		int newX = G.Left(parentBounds);
		newW = Math.min((int)(newH*2.5), newW);
		if(dis!=null)
		{
			newH = (int)(newW*0.4);
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
			else { crect = new Rectangle(newX,newY,newW,newH); }
		}		
		if(dis==null)
		{ includeDisplay = true;
		  dis = new TextContainer(CalculatorButton.id.Display);
		  dis.singleLine = true;
		  dis.setEditable(showOn,true);
		  newH = (int)(newW*0.5);
		  newY = G.centerY(parentBounds)-newH/2;
		  if(targetDisplay!=null) 
		  	{ String msg = targetDisplay.getText();
		  	  dis.setText(msg);
		  	  dis.setCaratPosition(msg.length());
		  	}
	  	  dis.setFocus(true);
		  crect = new Rectangle(newX,newY,newW,newH);
		}			

		display = dis;

	}
	// constructor
	public Keyboard(exCanvas see,TextContainer dis)
	{	showOn = see;
		buttonsUp = produceLayout(true);
		buttonsDown = produceLayout(false);
		targetDisplay = dis;
		resizeAndReposition();
	}

	public void StartDragging(HitPoint hp)
	{	
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
		if(includeDisplay) 
			{ display.doMouseMove(ex,ey,upcode); 
			}
		if(upcode==MouseState.LAST_IS_DOWN) { sawDown = true; }
		}
	}
	public void setClosed()
	{ 	if(targetDisplay!=null) { targetDisplay.setText(display.getText()); } 
		closed = true; 
	}
	
	public boolean StopDragging(HitPoint hp)
	{
		if(sawDown && (hp.hitCode instanceof CalculatorButton.id))
		{	//display.doMouseUp();
			sawDown = false;
			if(showOn!=null && showOn.doSound()) { SoundManager.playASoundClip(clickSound,100); }
			CalculatorButton.id bcode = (CalculatorButton.id)hp.hitCode;
			display.setFocus(true);
			if((bcode.ival>=0) && (bcode.ival<=9)) 
				{ display.append((char)(bcode.ival+'0')); 
				}
			else if((bcode.ival>=' ') && (bcode.ival<0xff)) 
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
				  control = false;
				  shift = false;
				}}
			else {
				switch(bcode)
				{
				case NcloseKeyboard:
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
				case Nshift:
					shift = !shift;
					break;
				case Ncontrol:
					control = !control;
					break;
				case Ncaps:
					shiftLock = !shiftLock;
					shift = false;
					break;
				case Back: 
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
				default: G.print("hit unknown button %s",bcode);
				
				}
			}
			return(true);
		}
		return(false);
	}

	public void draw(Graphics gc,HitPoint highlight)
	{	loadImages(showOn.loader);
	
		if(G.pointInRect(highlight, crect)) 
			{ // this makes sure that everything under the keyboard is inactive.
			highlight.neutralize();
			}

		int left = G.Left(crect);
		int top = G.Top(crect);
		int w = G.Width(crect);
		int h = G.Height(crect);
		
		buttonsUp = produceLayout(true);
		buttonsDown = produceLayout(false);
		int lmargin = (int)(w*0.026);
		int tmargin = (int)(w*0.017);
		int bwidth = (int)(w*0.048);
		int bheight = (int)(w*0.037);
		Rectangle cr = GC.setClip(gc,left+lmargin,top+tmargin,w-bwidth,h-bheight);
		GC.fillRect(gc,Color.lightGray,left+lmargin, top+tmargin,w-bwidth,h-bheight);
		Keyboard.getImage(showOn.loader).drawImage(gc,left,top,w,h);
	  	GC.setClip(gc,cr);
    	GC.setFont(gc, showOn.largeBoldFont());
    	for(CalculatorButton button : (shiftLock != shift) ? buttonsUp : buttonsDown)
    	{	
    		switch(button.value)
    		{
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
    		button.draw(gc,showOn,highlight,crect);
    	}
    	if(includeDisplay)
    	{
    	FontMetrics fm = G.getFontMetrics(display.font);
    	int fontH = fm.getHeight();
    	display.setBounds(left+(int)(0.1*w),
    					  top+(int)(0.1*h),
    					  (int)(0.8*w),
    					  Math.min((int)(0.15*h),Math.max(fontH*2, (int)(0.1*h))));
    	display.setVisible(true);
    	display.redrawBoard(gc,highlight);
    	}
	}
}