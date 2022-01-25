package lib;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import bridge.Color;
import bridge.FontMetrics;
import bridge.KeyEvent;
import bridge.KeyListener;
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
 * @author Ddyer
 *
 */
public class TextContainer implements AppendInterface,KeyListener
{	static private int MIN_SELECTION_DISTANCE = 10;
	static private TextContainer focusInstance = null;
	public boolean hasFocus() { return (focusInstance == this); }
	StringBuilder data=new StringBuilder();
	private int lastLineHeight = 10;		// this is used to scale the amound of movement needed to start scrolling
	int caratPosition = 0;				// position in data of the blinking cursor
	
	private boolean mouseActive = false;		// true while the mouse is down and potentially used for selecting or scrolling
	private boolean mouseSelectingStart = false;
	private boolean mouseSelectingExpand = false;
	private int mouseExpandPos1 = -1;
	private int mouseExpandPos2 = -1;
	int MARGIN = 4;
	private boolean mouseSelecting = false;		// true if the mouse is being used to select text, initiated by a horizontal movement
	private boolean caratSelecting = false;		// one time flag to set the carat to the last mouse position	
	private int mouseSelectingX = -1;	// x tracking the mouse while selecting text
	private int mouseSelectingY = -1;	// y tracking the mouse while selecting text
	int selectionStart = -1;			// start of selection 
	int selectionEnd = -1;				// end of selection
	private String EXTENSIONMARKER = "";
    private long lastMouseActiveTime = 0;
    private Set<String>extensionLines = new HashSet<String>();
    
	private boolean autoScroll = true;				// scroll to end to keep the end carat visible
	private int selectingX = -1;			// internal start position when mouse dragging
	private int selectingY = -1;			// internal start position when mouse dragging
    private int maxScroll = 0;
	private boolean editable=false;
    private ScrollArea scrollBar = new ScrollArea();
	
	private boolean focusToggle = false;
	long flipTime = 0;
	public int flipInterval = 750;
	
	int endmargin = 15;
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
		selectionStart = -1;
		selectionEnd = -1;
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
		selectionStart = -1;
		selectionEnd = -1;
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
		if(editable && selectionStart>=0) 
		{ data.delete(selectionStart, selectionEnd);
		  mlCache = null;
		  setCaratPosition(selectionStart);
			  selectionStart = -1;
		  selectionEnd = -1;
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
		setCaratPosition(caratPosition+=c.length());
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
		caratPosition = Math.min(data.length(),Math.max(0, i));
		autoScroll = true;
		setChanged(Op.Repaint);
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
	public boolean drawAsSingleLine(Graphics g,HitPoint hp,Rectangle r,String line)
	{
		boolean isIn = G.pointInRect(hp,x,y,width,height);
		FontMetrics fm = G.getFontMetrics(font);
		int lineh = lastLineHeight = fm.getHeight();
		int lineX = x+MARGIN;
		int lineY = centerSingleLineY? y+lineh/2+height/2-lineh/8 : y+lineh+MARGIN;
		int lineStart = data.length()-line.length();
		int select = Math.max(0, selectionStart-lineStart);
		int selectEnd = Math.max(0, selectionEnd-lineStart);
		String fullLine = line;

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
			if(maxx>width-endmargin && line.length()>0)
			{	// shorten the line at the left
				int EXTSIZE = EXTENSIONMARKER.length();
				line = EXTENSIONMARKER+line.substring(1);
				if(select>=0) { select += EXTSIZE-1; }
				select += EXTSIZE-1;
				selectEnd += EXTSIZE-1;
				from = GC.getStringBounds(g,fm, line);
				maxx = (int)from.getWidth();
				while(maxx>width-endmargin && (line.length()>EXTSIZE+1))
				{   
					line = EXTENSIONMARKER+line.substring(EXTSIZE+1);
					if(select>0) { select--; }
					if(selectEnd>0) { selectEnd--; }
					from = GC.getStringBounds(g,fm, line);
					maxx = (int)from.getWidth();
				}
			}
		
		if(select>=0 && selectEnd>select)
		{	
		Rectangle sbounds = GC.getStringBounds(g,fm,line,select,selectEnd);
		Rectangle leftBounds = GC.getStringBounds(g,fm,line,0,select);
		int xx = (int)leftBounds.getWidth();
		GC.fillRect(g, Color.lightGray, lineX+xx, lineY-lineh/2,(int)sbounds.getWidth(),lineh/2);
		GC.setColor(g,foregroundColor);
		if(mouseSelecting)
			{
			GC.drawLine(g,lineX+xx,lineY-lineh,lineX+xx,lineY);
			}
		}
			// draw the line of text
			GC.Text(g,line,lineX, lineY);
			
		if(focusToggle||mouseSelecting)
			{
			drawFocusLine(g,fm,lineX,lineY,line,data.length()-(mouseSelecting ? selectionEnd : caratPosition));
			}
		}
			
			if(mouseSelecting)
		{   int mousePos1=-1;
			int mousePos2=-1;
				if(G.pointInRect(mouseSelectingX,mouseSelectingY,x,lineY-lineh,width,lineh)) 
				{ mousePos1 =  findPositionInLine(fm,g,line,fullLine,mouseSelectingX-lineX);	
					}
			if(G.pointInRect(selectingX,selectingY,x,lineY-lineh,width,lineh)) 
				{ mousePos2 =  findPositionInLine(fm,g,line,fullLine,selectingX-lineX); 
					}
			if(mousePos1>=0 && mousePos2>=0 && mousePos1>mousePos2) 
				{ int x = mousePos1;
				  mousePos1=mousePos2; 
				  mousePos2 = x; 
				}
			if(mouseSelectingStart 
					&& (mousePos1>=0)
					&& (mousePos2>=mousePos1)
					&& (selectionStart>=0)
					&& ((mousePos1>=selectionStart && mousePos1<=selectionEnd)
							|| (mousePos2>=selectionStart && mousePos2<=selectionEnd)))
			{	mouseSelectingExpand = true;
				mouseExpandPos2 = mousePos2;
				mouseExpandPos1 = mousePos1;
			}	
			else if(mouseSelectingExpand && (mousePos1>=0) && (mousePos2>=mousePos1))
			{	
				if((mousePos1<mouseExpandPos1))
				{	
					selectionStart = Math.max(0, mousePos1-1);
				}
				if((mousePos2>mouseExpandPos2))
				{	
					setCaratPosition(selectionEnd = mousePos2);
				}
				selectionEnd = Math.max(selectionStart, selectionEnd);
			}
			else if((mousePos1>=0) && (mousePos2>=mousePos1))
			{
				selectionStart = Math.max(0, mousePos1-1);
			selectionEnd = Math.max(selectionStart, selectionEnd);
			setCaratPosition(selectionEnd = mousePos2);
			}
			mouseSelectingStart = false;
			
		}
		if(caratSelecting)
		{	caratSelecting = false;
			if(G.pointInRect(selectingX,selectingY,x,lineY-lineh,width,lineh))
			{ int start = lineStart+findPositionInLine(fm,g,line,fullLine,selectingX-lineX);
			  setCaratPosition ( Math.min(line.length()+lineStart,start)); 
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
				  // indicate a continuation line
		  if(!"".equals(curline)) { curline = EXTENSIONMARKER+curline; }
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
		int mousePos2 = -1;
		int charCount = 0;
		doFocus();
		int xpos = x+MARGIN;
		int lastY = y;
		int caratY = -1;
		for(int linen = 0,ypos = MARGIN;linen<nLines;linen++,ypos+=lineh)
		{	int realY = y+ypos-scrollY+lineh;
			String line = lines.elementAt(linen);
			int linelen = line.length();
			boolean isExtensionLine = extensionLines.contains(line);
			if(isExtensionLine) { charCount--; }	// previous line added one more
			int trueLinelen = isExtensionLine ? linelen - EXTENSIONMARKER.length() : linelen;
			
			if((ypos+lineh>=scrollY) && ypos<=scrollY+availableH)
			{
			boolean containsAny = charCount>=selectionStart && charCount<=selectionEnd;
			boolean containsStart = selectionStart>=0 && selectionStart>=charCount && selectionStart<=charCount+linelen;
			boolean containsEnd = selectionEnd>=0 && selectionEnd>=charCount && selectionEnd<=charCount+linelen;
			boolean containsCarat = (caratPosition>=charCount) && (caratPosition<=charCount+trueLinelen);
			if(containsStart || containsEnd || containsAny)
			{
				int line0 = containsStart ? selectionStart-charCount : 0;
				int line1 = containsEnd ? selectionEnd-charCount : linelen;
				Rectangle line0P = GC.getStringBounds(g,fm,line,0,line0);
				Rectangle line1P = GC.getStringBounds(g,fm,line,0,line1);
				GC.setColor(g,Color.lightGray);
				int left = (int)(xpos+line0P.getWidth());
				int right = (int)(xpos+line1P.getWidth());
				GC.fillRect(g, left, realY-lineh/2,right-left,lineh/2);
				if(mouseSelecting)
				{
				if(containsStart)
				{
				GC.drawLine(g, left, realY-lineh,left,realY);
				}
				if(containsEnd)
				{
				GC.drawLine(g, right, realY-lineh,right,realY);
				}}
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
				drawFocusLine(g,fm,xpos,realY,line,linelen-(caratPosition-charCount)-EXTENSIONMARKER.length());
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
					}
				if(G.pointInRect(selectingX,selectingY,x,realY-lineh,width,lineh)) 
					{ mousePos2 = charCount +  findPositionInLine(fm,g,line,line,selectingX-xpos); 
					}

				
			}
			if(caratSelecting)
			{	
 				if(G.pointInRect(selectingX,selectingY,x,realY-lineh,width,lineh))
				{ caratSelecting = false;
				  int start = findPositionInLine(fm,g,line,line,selectingX-xpos);
				  if(isExtensionLine)
				  {
					  start = Math.max(0, start-EXTENSIONMARKER.length());
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
		if(mouseSelecting && (mousePos1>=0) && (mousePos2>=0))
		{
			if(mousePos1>mousePos2) { int x = mousePos1; mousePos1=mousePos2; mousePos2 = x; }
			
			if(mouseSelectingStart 
					&& (selectionStart>=0)
					&& ((mousePos1>=selectionStart && mousePos1<=selectionEnd)
							|| (mousePos2>=selectionStart && mousePos2<=selectionEnd)))
			{	mouseSelectingExpand = true;
				mouseExpandPos2 = mousePos2;
				mouseExpandPos1 = mousePos1;
				mouseSelectingStart = false;
			}				
			else if (mouseSelectingExpand && (mousePos1>=0) && (mousePos2>=mousePos1))
			{
				if((mousePos1>=0) && (mousePos1<mouseExpandPos1))
				{	
					selectionStart = Math.max(0, mousePos1-1);
				}
				if((mousePos2>=0) && (mousePos2>mouseExpandPos2))
				{	
					selectionEnd = mousePos2;
				}
			}
			else if((mousePos1>=0)&&(mousePos2>mousePos1))
			{
			selectionStart = Math.max(0, mousePos1-1);
			selectionEnd = mousePos2;
			}
			selectionEnd = Math.max(selectionStart, selectionEnd);
			
		}
		GC.frameRect(g,Color.black,x,y,width,height);
		if(isIn) { hp.hitCode = defaultId; }
		caratSelecting = false;
		GC.setClip(g,oldclip);
		return(isIn);
	}
	public int findPositionInLine(FontMetrics fm,Graphics g,String line,String fullLine,int x)
	{	int linelen = line.length();
		int fulllen = fullLine.length();
		for(int i=0;i<linelen;i++)
	{
			Rectangle rect = GC.getStringBounds(g,fm, line,0,i);
			if(rect.getWidth()>x)
		{
					return(i+(fulllen-linelen)); 
				}
		}
		return(fulllen);
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
			hit = drawAsSingleLine(g,hp,r,lines[lines.length-1]);
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
		if(selectionStart>=0)
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
		selectionStart = 0;
		selectionEnd = data.length();
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
	{	if(selectionStart>=0)
		{
		int end = Math.min(selectionEnd, data.length());
		if(end>selectionStart)
		{
		if(data.charAt(end-1)=='\n') { end--; } 
		if((end>selectionStart))
		{
			String str = G.substring(data,selectionStart,end);
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
		selectionStart = -1;
		selectionEnd = -1;
		setCaratPosition(data.length());
	}
	private void setScrollY(int to)
	{
		scrollY = Math.min(maxScroll, Math.max(0,to));
		if(scrollable()) { scrollBar.setScrollPosition(to); } 
	}
	
	public boolean doMouseDrag(int ex,int ey)
	{	if(!G.pointInRect(ex, ey,x,y,width,height)) { return(false); }
		long now = G.Date();
		if(!mouseActive)
			{ mouseActive = true;
			  selectingX = ex; 
			  selectingY=ey;
			  mouseSelectingX = ex;
			  mouseSelectingY = ey;
			  autoScroll = false;
			  mouseSelecting = false;
			  mouseSelectingStart = false;
			}
		if(mouseSelecting)
    	{
			mouseSelectingX = ex;
         	mouseSelectingY = ey;
    	}
		else if(Math.abs(ex-selectingX)>MIN_SELECTION_DISTANCE)
		{
			mouseSelectingStart = mouseSelecting = true;
		}
		lastMouseActiveTime = now;
		return(mouseActive);
	}
	
	public boolean containsPoint(HitPoint p) { return(G.pointInRect(p, x,y,width,height)); }
	
	public boolean doMouseUp(int ex,int ey)
	{	
		if(G.pointInRect(ex, ey,x,y,width,height))
		{
		if(mouseSelecting) 
			{ doCopy();  			  
			}
		else if(!mouseSelecting) {
		// if no selection or scrolling happened, just reposition 
		// the blinking carat 
			selectionStart = -1;
			selectionEnd = -1;
			caratSelecting = true;
			setFocus(true);
		}

		mouseActive = false;
		mouseSelecting = mouseSelectingExpand = mouseSelectingStart = false;
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
		case LAST_IS_MOVE:
		case LAST_IS_IDLE:
			break;
		}}
	}
	// this tests if the mouse is idle enough that we just want
	// to select the current widget.
	public boolean idle()
	{	// 1.5 seconds since the drag stopped
		return(G.Date()-lastMouseActiveTime>1500);
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
