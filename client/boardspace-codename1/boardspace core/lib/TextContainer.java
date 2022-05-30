package lib;


import com.codename1.ui.Font;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;

import bridge.*;
import online.common.exCanvas;
import static online.common.OnlineConstants.clickSound;


import java.util.HashSet;
import java.util.Set;

/**
 * This is a window-free replacement for TextArea, coded to be used with
 * other boardspace UI items.
 * 
 * Mouse activity: 
 *   If the item is editable and the mouse is recently idle, select it and bring up the keyboard.
 *   If you press and release while focused, move the cursor
 *   If you press and move slowly, designate the start of a selection area. 
 *   You can drift the selected position while viewing the magnified touch point.
 *   Move briskly left or right from the selection point to expand the selection.
 *   Click in the middle of a current selection to modify it to left or right.
 *   
 *   Vertical scrolling and flinging is handled by the scroll bar
 *    
 *   These text windows display all text, extra-long lines are marked by a blot in the margins.
 *   Single line displays can have blots in both left and right margins
 *   
 * @author Ddyer
 *
 */
public class TextContainer implements AppendInterface,KeyListener
{	
	static private TextContainer focusInstance = null;
	public boolean hasFocus() { return (focusInstance == this); }
	StringBuilder data=new StringBuilder();
	private int lastLineHeight = 10;		// this is used to scale the amound of movement needed to start scrolling
	
	// on single line displays, where the line is longer than the available space, we maintain
	// a scrollable line with caratPosition as the point that is kept visible.  Normally this will
	// be the end of the line, but it can move.
	private int caratPosition = 0;					// position in data of the blinking cursor
	// on single line displays, visibleCaratPosition is moved to follow the mouse selecting text.
	// during the active selection, caratPosition is NOT changed, because doing so shifts the visible
	// segment of the line out from under the mouse being dragged.
	private int visibleCaratPosition = 0;
	private boolean mouseActive = false;		// true while the mouse is down and potentially used for selecting or scrolling
	private boolean mouseSelectingExpand = false;
	private int mouseExpandPos1 = -1;
	private int MARGIN = 4;
	private boolean mouseSelecting = false;		// true if the mouse is being used to select text, initiated by a horizontal movement
	private boolean caratSelecting = false;		// one time flag to set the carat to the last mouse position	
	private int mouseSelectingX = -1;	// x tracking the mouse while selecting text
	private int mouseSelectingY = -1;	// y tracking the mouse while selecting text
	// on multiline displays, if there are overflow lines (too wide for the display), the 
	// visible and actual line positions begin to diverge.  We maintain both in parallel, 
	// so the visible marker and the text copy/paste operations agree
	private int visibleSelectionStart = -1;			// start of selection 
	private int visibleSelectionEnd = -1;			// end of selection
	private int actualSelectionStart = -1;
	private int actualSelectionEnd = -1;
    private Set<String>extensionLines = new HashSet<String>();
     
	private boolean autoScroll = true;		// scroll to end to keep the end carat visible
	private int selectingX = -1;			// internal start position when mouse dragging
	private int selectingY = -1;			// internal start position when mouse dragging
    private int maxScroll = 0;
	private boolean editable=false;
    private ScrollArea scrollBar = new ScrollArea();

	private boolean focusToggle = false;
	long flipTime = 0;
	public int flipInterval = 750;
	
	private int endmargin = 15;
	public void setFocus(boolean v,int interval) 
	{
	  flipInterval = interval;
	  setFocus(v);
	}
	public void setFocus(boolean v)
	{	
		if(editable && (v!=hasFocus()))
			{if(canvas!=null) 
				{ canvas.repaint(); 
				  canvas.requestFocus(this);
				}
			 if(v) { focusInstance = this; }
			 else if(focusInstance==this) { focusInstance=null;}
			 focusToggle = v;
			}
		if(hasFocus() && clearBeforeAppend) { clear(); }	
	}
	public boolean clearBeforeAppend = false;
	public enum Op  { Send,Repaint; }
	
	
	/**
	 * if true, render this text area as a button, presumably with a small amount of text
	 * this is used to make buttons for "send" and "to player" in chat windows.
	 */
	public boolean renderAsButton = false;
	/**
	 * if true, render as a single line of text.  The area can still contain multiple lines, but only the
	 * last line is editable.  This is used to make the input line in chat windows, which can also accommodate
	 * text pastes of unlimited size.
	 */
	public boolean singleLine = false;
	public boolean centerSingleLineY = true;
	/**
	 * if true, render as a multi-line text area
	 */
	public boolean multiLine = false;
	/**
	 * if true, make the multiline area scrollable.  Default is true.
	 */
	public boolean scrollable = true;
	public void setScrollable(boolean v) { scrollable = v; }
	
	public boolean scrollable()
	{
		return(scrollable && !renderAsButton && !singleLine);
	}
	exCanvas canvas = null;
	Color backgroundColor = Color.white;
	Color foregroundColor = Color.black;
	Color buttonColor = Color.gray;
	Font font = G.getGlobalDefaultFont();
	int x;
	int y;
	int width;
	int height;
	int scrollX=0;
	int scrollY=0;
	private boolean isVisible = false;
	public boolean isVisible() { return(isVisible); }
	public void setVisible(boolean v) { isVisible=v; }
	
	public Dimension getPreferredSize()
	{
		FontMetrics fm = G.getFontMetrics(font);
		Rectangle sz = GC.getStringBounds(null,fm,data.toString());
		return(new Dimension(G.Width(sz),G.Height(sz)));
	}
	private SimpleObservable observer = new SimpleObservable();		// who to tell
	public void addObserver(SimpleObserver o) {	
		observer.addObserver(o);
	}
	public void setChanged(Op op)
	{	
		observer.setChanged(op);
	}
	
	CellId defaultId = null;
	public String toString() { return("<TextContainer "+defaultId+">"); }
	
	public TextContainer(CellId id) {	defaultId = id; }
	// constructor
	public TextContainer(String t)
	{
		data = (t==null) ? new StringBuilder("") : new StringBuilder(t);
	}
	public String getText() { return(data.toString()); }
	public void setText(String t) 
	{	data = new StringBuilder(t==null ? "" : t);
		mlCache = null;
		visibleSelectionStart = -1;
		visibleSelectionEnd = -1;
		clearBeforeAppend = false; 
		setScrollY(0);
		setCaratPosition(0);
		if(canvas!=null) { canvas.repaint(0,"new text"); }

	}
	public boolean editable() 
	{ return(editable); 
	}
	
	public Keyboard makeKeyboardIfNeeded(exCanvas c,Keyboard key)
	{	if((key==null) && hasFocus( )&& editable) { return new Keyboard(c,this);}
		return(key);
	}
	public void setEditable(exCanvas can,boolean e) 
	{ boolean wasEditable = editable;
	  editable = e;
	  if(can!=null)
	  {
	  canvas = can;
	  can.requestFocus(this);
	  if(editable && !wasEditable) 
	  	{ 
	  	  can.addKeyListener(this);
	  	}
	  if(!editable && wasEditable) 
	  	{ 
		  can.removeKeyListener(this); 
		}
	  }
	}
	public void setBackground(Color background) {
		backgroundColor = background;
	}
	public void setButtonColor(Color button)
	{
		buttonColor = button;
	}
	public void setForeground(Color foreground) {
		foregroundColor = foreground;
		setChanged(Op.Repaint);
	}
	public void setFont(Font basicFont) {
		font = basicFont;
		setChanged(Op.Repaint);
	}

	public synchronized void clear()
	{
		data.setLength(0);
		mlCache = null;
		visibleSelectionStart = -1;
		visibleSelectionEnd = -1;
		actualSelectionStart = -1;
		actualSelectionEnd = -1;
		setScrollY(0);
		clearBeforeAppend = false;
		autoScroll = true;
		setCaratPosition(0);
	}
	public void append(char c)
	{
		if(clearBeforeAppend) 	{ clear(); }
		data.append(c);
		mlCache = null;
		setCaratPosition(caratPosition+1);
	}
	public synchronized void doDeleteSelection()
	{
		if(editable && visibleSelectionStart>=0) 
		{ data.delete(visibleSelectionStart, visibleSelectionEnd);
		  mlCache = null;
		  setCaratPosition(visibleSelectionStart);
		  visibleSelectionStart = -1;
		  visibleSelectionEnd = -1;
		  actualSelectionStart = -1;
		  actualSelectionEnd = -1;
		}

	}
	public void insert(char c)
	{	if(clearBeforeAppend) { clear(); }
		doDeleteSelection();
		if(caratPosition<data.length()) { data.insert(caratPosition,c); }
		else { data.append(c); }
		mlCache = null;
		setCaratPosition(caratPosition+1);
	}
	public void insert(String c)
	{	if(clearBeforeAppend) { clear(); }
		doDeleteSelection();
		if(caratPosition<data.length()) { data.insert(caratPosition,c); }
		else { data.append(c); }
		mlCache = null;
		setCaratPosition(caratPosition+c.length());
	}
	public void append(String newstr) {
		if(clearBeforeAppend) { clear(); }
		doDeleteSelection();
		data.append(newstr); 
		mlCache = null;
		setCaratPosition(caratPosition+newstr.length());
	}
	public void finishLine()
	{	int len = data.length();
		if((len>0) && (data.charAt(len-1)!='\n')) { data.append('\n'); }
	}
	public int messageSize() { return(data.length()); }
	
	public void setCaratPosition(int i) {
		int cp = caratPosition;
		visibleCaratPosition = caratPosition = Math.min(data.length(),Math.max(0, i));
		autoScroll = true;
		if(cp!=caratPosition) { setChanged(Op.Repaint); }
	}
	public Point getLocation()
	{
		return(new Point(x,y));
	}
	public Rectangle getBounds() { return(new Rectangle(x,y,width,height)); }
	
	public void setBounds(Rectangle r)
	{
		setBounds(G.Left(r),G.Top(r),G.Width(r),G.Height(r));
	}
	public void setBounds(int left, int top, int inWidth, int inHeight) {
		x = left;
		y = top;
		width = inWidth;
		height = inHeight;
		mlCache = null;
    	int barWidth = (int)(ScrollArea.DEFAULT_SCROLL_BAR_WIDTH*G.getDisplayScale());
    	// the negative width keeps the scroll bar from doing any scroll actions in the main text area
    	// which handles it by itself
    	Rectangle messageRect = new Rectangle(left, top, inWidth-barWidth, inHeight);
		
    	scrollBar.InitScrollDimensions(
	  			 left+inWidth-barWidth, 
	  			 messageRect,
	  			 barWidth,			// scroll bar width
	  			 inHeight*10,
	  			 lastLineHeight,			// small jump size
	             inHeight/3);			// big jump size
    	scrollBar.alwaysVisible = false;
    	scrollBar.backgroundColor = backgroundColor;
    	scrollBar.foregroundColor = buttonColor;
		setChanged(Op.Repaint);
	}
	public int getWidth() {
		return(width);
	}
	public int getHeight() {
		return(height);
	}
	public int getX() {
		return(x);
	}
	public int getY() {
		return(y);
	}
	private int findCaratY(StringStack lines,int fontH)
	{	
		int charpos = 0;
		int linepos = 0;
		for(int i=0,lim=lines.size();i<lim;i++)
		{
			int len = lines.elementAt(i).length()+1;
			if(charpos<=caratPosition && charpos+len>caratPosition) { return(linepos);}	
			charpos += len;
			linepos += fontH;
		}
		return(linepos);
	}
	
	private void autoScroll(int fontH,StringStack lines)
	{	autoScroll = false;
		int pos = caratPosition;
		boolean eol = data.length()>0 && (data.charAt(data.length()-1)=='\n');
		int nLines = lines.size()+(eol?1:0);
		int availableHeight = height-MARGIN*2;
		int textSize = fontH*nLines;
		if(textSize<availableHeight) { setScrollY(0); }
		else if(pos>=0)
		{	int caratPos = MARGIN+findCaratY(lines,fontH);
			if((caratPos>=scrollY)&&(caratPos+fontH<scrollY+availableHeight)) {}
			else {
				if(caratPos<scrollY) { setScrollY(caratPos); }
				else { int mv = Math.max(0,caratPos-availableHeight+fontH);
					   setScrollY(mv); 
					 }
		}}
	}
	public boolean drawAsButton(Graphics g,HitPoint hp,Rectangle r,String line)
	{	FontMetrics fm = G.getFontMetrics(font);
		lastLineHeight = fm.getHeight();
		if(GC.handleSquareButton(g, r, hp,line,backgroundColor,backgroundColor))
		{	
			hp.hitCode = defaultId;
			hp.spriteRect = r;
			hp.spriteColor = Color.red;
			return(true);
		}
		return(false);
	}
	private void doFocus()
	{
		if(hasFocus())
		{
		long now = G.Date();
		if(now>flipTime)
			{ focusToggle = !focusToggle; 
			  flipTime = now+flipInterval; 
			}
		//G.print("rep "+(int)(flipTime-now));
		canvas.repaint((int)(flipTime-now+10));
		}

	}
	//
	// line might be a single line of a multiple line paste
	// lineStart is the start of the line within the full line
	//
	public boolean drawAsSingleLine(Graphics g,HitPoint hp,Rectangle r,String originalLine,int lineStart)
	{	String line = originalLine;
		if(!mouseSelecting) { setCaratPosition(visibleCaratPosition); }
		int lineLen = line.length();
		boolean isIn = G.pointInRect(hp,x,y,width,height);
		FontMetrics fm = G.getFontMetrics(font);
		int carat = Math.min(lineLen,caratPosition-lineStart);				// carat position within the display line
		int visCarat = Math.min(lineLen,visibleCaratPosition-lineStart);
		int lineh = lastLineHeight = fm.getHeight();
		int lineX = x+MARGIN;
		int lineY = centerSingleLineY? y+lineh/2+height/2-lineh/8 : y+lineh+MARGIN;
		int select = Math.max(0, visibleSelectionStart-lineStart);
		int selectEnd = Math.max(0, visibleSelectionEnd-lineStart);	// visible part of selection
		int charsDeletedAtLeft = 0;
		int charsDeletedAtRight = 0;
		if(select<0) { select = 0; }
		doFocus();
		GC.frameRect(g,Color.black,x,y,width,height);		
		if(clearBeforeAppend)
			{
			GC.Text(g,true,lineX, lineY-lineh,width-MARGIN*2,height-MARGIN*2,foregroundColor,backgroundColor,line);
			}
			else
		{
			Rectangle from = GC.getStringBounds(g,fm,line);
			int maxx = (int)from.getWidth();
			int availableWidth = width-endmargin;
			if(maxx>availableWidth && lineLen>0)	// does not all fit
			{	// if the carat is not at the end, shorten at the left until the carat is visible
				int pos = carat;
				String leftpart = line.substring(0,pos);
				Rectangle left = GC.getStringBounds(g,fm,leftpart);
				int caratX = (int)left.getWidth();
				while(caratX>=availableWidth/2 && charsDeletedAtLeft<pos)
				{	charsDeletedAtLeft++;
					leftpart = line.substring(charsDeletedAtLeft,pos);
					left = GC.getStringBounds(g,fm,leftpart);
					caratX = (int)left.getWidth();
				}
				
				if(caratX<availableWidth && pos<lineLen)
				{
					do {
					pos++;
					leftpart = line.substring(charsDeletedAtLeft,pos);
					left =  GC.getStringBounds(g,fm,leftpart);
					caratX= (int)left.getWidth();
					} 
					while((caratX<availableWidth) && (pos<lineLen));
				}
				
				while((caratX<availableWidth) && (charsDeletedAtLeft>0))
				{
					charsDeletedAtLeft--;
					leftpart = line.substring(charsDeletedAtLeft,pos);
					left =  GC.getStringBounds(g,fm,leftpart);
					caratX= (int)left.getWidth();
				}
				
				charsDeletedAtRight = lineLen-pos;
				line = line.substring(charsDeletedAtLeft,pos);
				lineLen = line.length();
				from = GC.getStringBounds(g,fm,line);
				maxx = (int)from.getWidth();

				
				if(charsDeletedAtLeft>0)
					{ 
					select = Math.max(0,select-charsDeletedAtLeft);
					selectEnd = Math.max(0,selectEnd-charsDeletedAtLeft);
					}
							
			}
			selectEnd = Math.min(selectEnd,lineLen);
		
		// draw a gray background around the selection
		if(select>=0 && selectEnd>select)
		{	
		Rectangle sbounds = GC.getStringBounds(g,fm,line,select,selectEnd);
		Rectangle leftBounds = GC.getStringBounds(g,fm,line,0,select);
		int xx = (int)leftBounds.getWidth();
		GC.fillRect(g, Color.lightGray, lineX+xx, lineY-lineh/2,(int)sbounds.getWidth(),lineh/2);
		GC.setColor(g,foregroundColor);
		}
		
		// draw the line of text
		GC.Text(g,line,lineX, lineY);
		if((charsDeletedAtLeft>0)||(lineStart>0))
			{ GC.fillRect(g, Color.black,G.Left(r),lineY-lineh,MARGIN,lineh);
			}
		if((charsDeletedAtRight>0)||(lineStart+lineLen+charsDeletedAtLeft<data.length()))
			{ GC.fillRect(g, Color.black,G.Right(r)-MARGIN,lineY-lineh,MARGIN,lineh);
			}	
		
		if(focusToggle||mouseSelecting)
			{
			drawFocusLine(g,fm,lineX,lineY,line,lineLen-(visCarat-charsDeletedAtLeft));
				//	(shortenAtLeft ? data.length():line.length())
				//	-(mouseSelecting ? visibleSelectionEnd : carat));
			}
		}
		/**
		 * 
		 * Using the mouse to expand selection interacts badly with auto scrolling
		 * the current solution is to not scroll during mouse drag
		 * 
		GC.frameRect(g,Color.blue,100,100,500,40);
		GC.Text(g,false,100,100,500,40,Color.black,Color.white,
				"sel "+visibleSelectionStart+"-"+visibleSelectionEnd
				+" carat "+caratPosition
				+" exp "+mouseSelectingExpand+" "+mouseExpandPos1
				+" mouse "+(mouseExpandPos1+charsDeletedAtLeft)
				+" left "+charsDeletedAtLeft);
		 */
		if(mouseSelecting)
		{   
			if(G.pointInRect(mouseSelectingX,mouseSelectingY,x,lineY-lineh,width,lineh)) 
			{int mousePos1=	lineStart+charsDeletedAtLeft+findPositionInLine(fm,g,line,mouseSelectingX-lineX);	
				  //print("pos1 ",mousePos1," from ",charsDeletedAtLeft);
			if(mousePos1>=0)
			{	
				if(!mouseSelectingExpand)
				{ mouseExpandPos1 = mousePos1; 
				  mouseSelectingExpand = true; 
				}
				if(visibleSelectionStart<0)
				{ actualSelectionStart = visibleSelectionStart  = mousePos1;
				  actualSelectionEnd = visibleSelectionEnd = mousePos1+1;
				  visibleCaratPosition = mousePos1;
				}
				else
				if((mousePos1<mouseExpandPos1))
				{		
					actualSelectionStart = visibleSelectionStart = Math.max(0, mousePos1);
					//G.print("start = ",mousePos1);
					visibleCaratPosition = mousePos1;
				}
				else if((mousePos1>mouseExpandPos1))
				{	actualSelectionEnd = visibleSelectionEnd = mousePos1;
					//G.print("end = ",mousePos1);
					visibleCaratPosition = mousePos1;
				}
				visibleSelectionEnd = Math.max(visibleSelectionStart, visibleSelectionEnd);
			}}
			
		}
		if(caratSelecting)
		{	caratSelecting = false;
			if(G.pointInRect(selectingX,selectingY,x,lineY-lineh,width,lineh))
			{ int start = lineStart+charsDeletedAtLeft+findPositionInLine(fm,g,line,selectingX-lineX);
			  setCaratPosition ( Math.min(originalLine.length(),start)); 
			}
		}
		if(isIn) { hp.hitCode = defaultId;}
		return(isIn);
	}
	private void drawFocusLine(Graphics g,FontMetrics fm,int lineX,int lineY,String line,int caratInLine)
	{
		{ // draw a blinking bar for the character position
		  int caratBeforeLine = line.length()-caratInLine;
		  if(caratBeforeLine>=0)
			  {	// if the carat position is in the last line
				  Rectangle beforeCarat = GC.getStringBounds(g,fm, line,0,caratBeforeLine);
				  int pos = lineX+(int)beforeCarat.getWidth();
			  GC.drawLine(g, pos ,lineY-fm.getHeight(), pos ,lineY); 
			}
		}
	}
	private StringStack splitLongLine(StringStack newLines,FontMetrics fm,String line,int linew)
	{
		// find a split space nearest the boundary, if it exists
		int nnew = 0;
		String curline = line;
		while(!"".equals(curline))
		  {
		  String sub = null;
		  int thisw = 0;
		  int splitw = 0;
		  do 
		  { splitw = curline.indexOf(' ',splitw+1);
		  	String nextSplit = null;
		  	if(splitw<0) 
		  		{ 
		  		  nextSplit = curline;
		  		}
		  		else
		  		{
		  		  nextSplit = curline.substring(0,splitw);
		  		}
		  	thisw = fm.stringWidth(nextSplit);
		  	
		  	if(thisw<=linew)
		  	{	sub = nextSplit;
		  	}
		  	else if((sub==null)||("".equals(sub.trim())))
		  	{	// none but leading spaces found, and still doesn't fit
		  		if(nextSplit.length()>1)
		  		{
		  		do { nextSplit = nextSplit.substring(0,nextSplit.length()-1); } 
		  		while(fm.stringWidth(nextSplit)>=linew && nextSplit.length()>1);
		  		}
		  		sub = nextSplit;
		  		splitw = nextSplit.length();
		  	}
		  	
		  } while(thisw<linew && splitw>0);

		  newLines.push(sub);
		  if(nnew>0) { extensionLines.add(sub); }
		  nnew++;
		  int sublen = sub.length();
		  int curlen = curline.length();
		  // trim leading spaces from the
		  while(curlen>sublen && (curline.charAt(sublen)==' '))
		  	{
			  sublen++;
		  	}
		  curline = curline.substring(sublen);
		  sub = null;
		}
		return(newLines);
	}
	
	StringStack mlCache = null;
	// this is a test of the line splitter this is a test of the line splitter this is a test of the line splitter 
	public synchronized StringStack resplit(StringBuilder lines,FontMetrics fm,int linew)
	{	if(mlCache!=null) { return(mlCache); }
		extensionLines.clear();
		StringStack newLines = new StringStack();
		int lineIndex = 0;
		int limit = lines.length();
		while(lineIndex<limit)
		{	int nextIndex = TextContainer.indexOf(lines, '\n',lineIndex);
			if(nextIndex<0) 
				{ nextIndex = limit; }
			String curline = G.substring(lines,lineIndex,nextIndex);
			lineIndex = nextIndex+1;

			if(fm.stringWidth(curline)>=linew)
				{ splitLongLine(newLines,fm,curline,linew);
				}
			else  { newLines.push(curline); }
		}
		mlCache = newLines;
		return(newLines);
	}
	public boolean drawAsMultipleLines(Graphics g,HitPoint hp,Rectangle r,StringBuilder data)
	{	
		boolean isIn = G.pointInRect(hp,x,y,width,height);
		FontMetrics fm = G.getFontMetrics(font);
		int lineh = lastLineHeight = fm.getHeight();
		int linew = G.Width(r)-MARGIN*2;
		// do not segment lines by length
		StringStack lines = resplit(data,fm,linew);
		int nLines = lines.size();
		maxScroll = Math.max(0, lineh*nLines-height/2+lineh);
		scrollBar.setScrollHeight(maxScroll);
		if(autoScroll && !mouseActive) { autoScroll(lineh,lines); }
		GC.fillRect(g,backgroundColor,x,y,width,height);
		GC.frameRect(g, isIn? Color.blue : Color.black, x,y,width,height);
		Rectangle oldclip = GC.combinedClip(g,x+1,y+MARGIN,x+width-MARGIN-1,y+height-MARGIN);
		
		int availableH = height-lineh-MARGIN*2;
		int mousePos1 = -1;
		int actualMousePos1 = -1;
		int charCount = 0;
		int nExtensionLines = 0;
		doFocus();
		int xpos = x+MARGIN;
		int lastY = y;
		int caratY = -1;
		for(int linen = 0,ypos = MARGIN;linen<nLines;linen++,ypos+=lineh)
		{	int realY = y+ypos-scrollY+lineh;
			String line = lines.elementAt(linen);
			int linelen = line.length();
			boolean isExtensionLine = extensionLines.contains(line);
			if(isExtensionLine) { charCount--; nExtensionLines++; }	// previous line added one more
			int trueLinelen = linelen;
			
			if((ypos+lineh>=scrollY) && ypos<=scrollY+availableH)
			{
			boolean containsAny = charCount>=visibleSelectionStart && charCount<=visibleSelectionEnd;
			boolean containsStart = visibleSelectionStart>=0 && visibleSelectionStart>=charCount && visibleSelectionStart<=charCount+linelen;
			boolean containsEnd = visibleSelectionEnd>=0 && visibleSelectionEnd>=charCount && visibleSelectionEnd<=charCount+linelen;
			boolean containsCarat = (caratPosition>=charCount) && (caratPosition<=charCount+trueLinelen);
			if(containsStart || containsEnd || containsAny)
			{
				int line0 = containsStart ? visibleSelectionStart-charCount : 0;
				int line1 = containsEnd ? visibleSelectionEnd-charCount : linelen;
				Rectangle line0P = GC.getStringBounds(g,fm,line,0,line0);
				Rectangle line1P = GC.getStringBounds(g,fm,line,0,line1);
				GC.setColor(g,Color.lightGray);
				int left = (int)(xpos+line0P.getWidth());
				int right = (int)(xpos+line1P.getWidth());
				GC.fillRect(g, left, realY-lineh/2,right-left,lineh/2);
			}
			GC.setColor(g,foregroundColor); 
			GC.Text(g,line,xpos, realY);
			if(isExtensionLine)
			{
				GC.fillRect(g, Color.black,x+1,realY-lineh,MARGIN-1,lineh);
			}
			lastY = realY;
			if(containsCarat && (focusToggle||mouseSelecting))
			{
			if(isExtensionLine) 
			{
				drawFocusLine(g,fm,xpos,realY,line,linelen-(caratPosition-charCount));
			}
			else
			{
			drawFocusLine(g,fm,xpos,realY,line,linelen-(caratPosition-charCount));
			}
			caratY = realY;
			}

			if(mouseSelecting)
			{
				if(G.pointInRect(mouseSelectingX,mouseSelectingY,x,realY-lineh,width,lineh)) 
					{ mousePos1 = charCount +  findPositionInLine(fm,g,line,line,mouseSelectingX-xpos); 
					  actualMousePos1 = mousePos1 + nExtensionLines;
					}
				
			}
			if(caratSelecting)
			{	
 				if(G.pointInRect(selectingX,selectingY,x,realY-lineh,width,lineh))
				{ caratSelecting = false;
				  int start = findPositionInLine(fm,g,line,line,selectingX-xpos);
				  if(isExtensionLine)
				  {
					  start = Math.max(0, start);
				  }
				  
				  {
				  setCaratPosition(charCount+Math.min(line.length(),start));
				  }
				}
			}
			}
			charCount += isExtensionLine ? trueLinelen+1 : linelen+1;
		}
		if(caratY<0 && focusToggle)
		{	
			drawFocusLine(g,fm,xpos,lastY+lineh,"",0);
		}
		if(mousePos1>=0) 
		{
			if(!mouseSelectingExpand)
			{ mouseExpandPos1 = mousePos1; 
			  mouseSelectingExpand = true; 
			}
			if(visibleSelectionStart<0)
			{
				visibleSelectionStart = mousePos1;
				visibleSelectionEnd = mousePos1+1;
				actualSelectionStart = actualMousePos1;
				actualSelectionEnd = actualMousePos1+1;
				setCaratPosition(mousePos1);
			}
			else if(mousePos1<mouseExpandPos1)
			{	visibleSelectionStart = Math.max(0,mousePos1);
				actualSelectionStart = Math.max(0,actualMousePos1);
				setCaratPosition(mousePos1);
			}				
			else if (mousePos1>mouseExpandPos1)
			{
				visibleSelectionEnd = mousePos1;
				actualSelectionEnd = actualMousePos1;
				setCaratPosition(mousePos1);
			}
			
			visibleSelectionEnd = Math.max(visibleSelectionStart, visibleSelectionEnd);
			actualSelectionEnd = Math.max(actualSelectionStart,actualSelectionEnd);
		
		}
		GC.frameRect(g,Color.black,x,y,width,height);
		if(isIn) { hp.hitCode = defaultId; }
		caratSelecting = false;
		GC.setClip(g,oldclip);
		return(isIn);
	}
	private int findPositionInLine(FontMetrics fm,Graphics g,String line,String fullLine,int x)
	{	int linelen = line.length();
		int fulllen = fullLine.length();
		for(int i=1;i<linelen;i++)
		{
			Rectangle rect = GC.getStringBounds(g,fm, line,0,i);
			int thisW = (int)rect.getWidth();
			if(thisW>x)
				{	
					return(i+(fulllen-linelen)-1); 
				}
		}
		return(fulllen);
	}
	private int findPositionInLine(FontMetrics fm,Graphics g,String line,int x)
	{	int linelen = line.length();
		int prevW = 0;
		for(int i=0;i<linelen;i++)
		{
			Rectangle rect = GC.getStringBounds(g,fm, line,0,i);
			int thisW = (int)rect.getWidth();
			if(thisW>=x)
				{ 	if((thisW-x)*2>(thisW-prevW)) { i--;}
					return(i); 
				}
			prevW = thisW;
		}
		return(linelen);
	}

	public boolean redrawBoard(Graphics g,HitPoint hp) 
	{	Rectangle r = new Rectangle(x, y, width, height);
		boolean hit = false;	
		if(isVisible && width>0)
		{
		GC.setFont(g,font);
		if(renderAsButton)
		{	String dataString = data.toString();
			String lines[] = G.split(dataString,'\n');
			hit = drawAsButton(g,hp,r,lines[0]);
		}
		else if(singleLine)
		{	String dataString = data.toString();
			String lines[] = G.split(dataString,'\n');
			int cp = 0;
			int idx = 0;
			String line = lines[idx];
			while((caratPosition<cp+line.length()) && idx+1<lines.length) 
				{ cp += line.length()+1; 
				  idx++; 
				  line = lines[idx]; 
				}
			hit = drawAsSingleLine(g,hp,r,line,cp);
		}
		else
		{ 	hit = drawAsMultipleLines(g,hp,r,data);
			if(scrollable) { scrollBar.drawScrollBar(g); }
		}}
		//G.addLog("painted");
		//G.finishLog();
		return(hit);
	}

	public void keyTyped(KeyEvent e) {
		if(editable && hasFocus())
		{	int mod = e.getModifiersEx();
			char code = e.getKeyChar();
			if((mod & KeyEvent.CTRL_DOWN_MASK)!=0)
			{
				code = (char)(0x40|code);
				switch(code)
				{
				case 'A':	
						selectAll();
						setChanged(Op.Repaint);
						break;
				case 'B':
						doBack();
						break;
				case 'C':
					doCopy();
					break;
				case 'D':
						doDel(true);
						break;
				case 'E':
						doToEnd();
						break;
				case 'F':
						doForward();
						break;
				case 'H':
						doDel(false);
						break;
				case 'I':
						insert('\t');
						break;
				case 'M':
						doSend();
						break;
				case 'V':
						doPaste();
						break;
				case 'X':
						doDel(false);
						break;
				default: break;
				}
			}
			else {
				switch(code)
				{
				case '\n':
					doSend();
					break;
				case '\u007f':	// del
				case '\b':	// backspace
					doDel(false);
					break;
				case '\u2190':
					doBack();
					break;
				case 39:
				case '\u2192':
					doForward();
					break;
				default:
					insert(code);
					break;
				}
			}
		}
		//G.print("Key typed "+e);
	}
	public boolean doSend()
	{	if(multiLine)
			{
			insert('\n');
			return(false);
			}
			else 
			{ setChanged(Op.Send);
			  return(true);
			}
	}
	
	public synchronized void doDel(boolean forward)
	{
		if(data.length()>0)
		{
		if(visibleSelectionStart>=0)
		{
			doDeleteSelection();
		}
		else
		{	if(forward) { if(caratPosition>=data.length()) { return; }}
			else setCaratPosition(caratPosition-1);
			data.deleteCharAt(caratPosition);
			mlCache = null;
		}
		}
	}
	public void doBack()
	{	setCaratPosition(caratPosition-1);
	}
	public void doForward()
	{
		setCaratPosition(caratPosition+1);
	}
	public void doUp()
	{
		if(multiLine)
		{	int pos = Math.max(0, caratPosition);
			int linepos = linePosition(data,pos);
			int prevlinepos = Math.max(0, prevIndexOf(data,'\n',caratPosition-linepos-1)+1); 
			int len = Math.min(linepos,caratPosition-linepos-prevlinepos-1);
			setCaratPosition(prevlinepos + len);
		}
		else {
			setCaratPosition(0);
		}
	}
	private int prevIndexOf(StringBuilder b,char ch,int start)
	{	start--;
		while((start>=0) && (b.charAt(start)!=ch)) { start--; }
		return(start);
	}
	private int linePosition(StringBuilder b,int start)
	{	int ind = prevIndexOf(b,'\n',start)+1;
		return(start-ind);
	}
	public void doDown()
	{
		if(multiLine)
		{	int pos = Math.max(0, caratPosition);
			int linepos = linePosition(data,pos);
			int nextlinepos = TextContainer.indexOf(data, '\n',pos)+1;
			int thirdlinepos = TextContainer.indexOf(data, '\n',nextlinepos);
			if(nextlinepos>0) 
			{ 	int end = thirdlinepos>=0 ? thirdlinepos : data.length();
				setCaratPosition(Math.min(end,nextlinepos+linepos)); 
			} 
			else { setCaratPosition(data.length());}
		}
		else
		{
			setCaratPosition(data.length()-1);
		}
	}
	public void selectAll()
	{
		visibleSelectionStart = 0;
		visibleSelectionEnd = data.length();
		G.writeTextToClipboard(data.toString());
	}
	public void keyPressed(KeyEvent e) {
		int code = e.getExtendedKeyCode();
		//G.print("Pressed "+code);
		switch(code)
		{
		case 222:
			// this is fuckin mysterious - single quotes are not otherwise found.
			if(e.getKeyChar()=='\'') { append('\''); } 
			break;
		case 35:
			doToEnd();
			break;
		case 36:		// home key
			doToBeginning();
		case 37:
			doBack();
			break;
		case 39:
			doForward();
			break;
		case 38:
			doUp();
			break;
		case 40:
			doDown();
			break;
		default: break;
		}
	}
	public void keyReleased(KeyEvent e) {
		//G.print("Key released "+e);
	}
	public void doCopy()
	{	if(actualSelectionStart>=0)
		{
		int end = Math.min(actualSelectionEnd, data.length());
		if(end>actualSelectionStart)
		{
		if(data.charAt(end-1)=='\n') { end--; } 
		if((end>actualSelectionStart))
		{	
			String str = G.substring(data,actualSelectionStart,end);
			G.writeTextToClipboard(str);
			if(canvas!=null && canvas.doSound())
			  {
				  SoundManager.playASoundClip(clickSound,100);
			  }
		}}}
	}
	public void doPaste()
	{
		String str = G.readTextFromClipboard();
		if(str!=null)
		{
			insert(str);
		}
	}
	public void doToEnd()
	{
		visibleSelectionStart = -1;
		visibleSelectionEnd = -1;
		setCaratPosition(data.length());
	}
	public void doToBeginning()
	{
		visibleSelectionStart = -1;
		visibleSelectionEnd = -1;
		setCaratPosition(0);
	}
	private void setScrollY(int to)
	{	
		scrollY = Math.min(maxScroll, Math.max(0,to));
		if(scrollable()) { scrollBar.setScrollPosition(to); } 
	}

	public boolean doMouseDrag(int ex,int ey)
	{	if(!G.pointInRect(ex, ey,x,y,width,height)) { return(false); }
		if(!mouseActive)
			{ mouseActive = true;
			  selectingX = ex; 
			  selectingY=ey;
			  mouseSelectingX = ex;
			  mouseSelectingY = ey;
			  autoScroll = false;
			  mouseSelecting = false;			
			}
		mouseSelectingX = ex;
		mouseSelectingY = ey;
		mouseSelecting = true;  		
    	return(mouseActive);
	}
	
	public boolean containsPoint(HitPoint p) { return(G.pointInRect(p, x,y,width,height)); }
	
	public boolean doMouseUp(int ex,int ey)
	{	
		if(mouseSelecting) 
		{ doCopy();
		}
		
		if(G.pointInRect(ex, ey,x,y,width,height))
		{
			if(!mouseSelecting)
			{
			// if no selection or scrolling happened, just reposition 
			// the blinking carat 
			visibleSelectionStart = -1;
			visibleSelectionEnd = -1;
			caratSelecting = true;
			setFocus(true);
			}

		mouseActive = false;
		mouseSelecting = mouseSelectingExpand = false;
		}
		return(false);
	}
	public void doMouseMove(int ex, int ey,MouseState upcode)
	{	
		// don't involve the scrollbar if we have started a selection gesture
		if(!mouseSelecting && scrollable() && scrollBar.doMouseMotion(ex, ey, upcode))
		{	
			setScrollY(scrollBar.getScrollPosition());
		}
		else {
		switch(upcode)
		{
		case LAST_IS_DOWN:
		case LAST_IS_DRAG:
			if(clearBeforeAppend) { clear(); }
			doMouseDrag(ex,ey); 
			break;
		case LAST_IS_UP:
			doMouseUp(ex,ey);
			default: 
			break;
		case LAST_IS_EXIT:
			if(mouseSelecting)
			{
				doCopy();
			}
		case LAST_IS_MOVE:
		case LAST_IS_IDLE:
			mouseSelectingExpand = mouseSelecting = false;
			mouseExpandPos1 = -1;
			break;
		}}
	}
  
    // this is the repeat scroll while flinging hasn't wound down to 0 yet.
    public boolean doRepeat()
	    {	
	    	if(scrollable() && scrollBar.doRepeat())
	    	{
	    		setScrollY(scrollBar.getScrollPosition());
	    		return(true);
	    	}    	
	    	
	    	return(false);
	    }
	public boolean activelyScrolling() {
		return(scrollable() && scrollBar.activelyScrolling());
	}
	public boolean doMouseWheel(int up) {
		scrollBar.smallJump(up<0);
		setScrollY(scrollBar.getScrollPosition());
		return(true);
	} 
	public boolean doMouseWheel(int xx,int yy,int amount)
	{	if(G.pointInRect(xx,yy,x,y,width,height))
		{
		return doMouseWheel(amount);
		}
		return(false);
	}
	public static int indexOf(StringBuilder b,char ch,int first)
	{
		int len = b.length();
		int ind = first;
		while(ind<len) { if(b.charAt(ind)==ch) { return(ind); }; ind++; }
		return(-1);
	}
}
