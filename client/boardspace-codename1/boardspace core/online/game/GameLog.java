package online.game;


import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;

import bridge.*;

import lib.G;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.MouseState;
import lib.ScrollArea;
import lib.Text;
import lib.TextChunk;
/**
 * This class embodies the standard logic for printing
 * a game record within a game window.  The two forms
 * are "n column" with one vertical column per player,
 * and "1 column" with player name on the left and the
 * player's current move to the right.  Most of the
 * multiplayer games use this format.
 * @author ddyer
 *
 */
public class GameLog implements Opcodes
{
	commonCanvas canvas;	// the associated canvas for the game
	ScrollArea scrollbar = new ScrollArea();	// the scrollbar for the display
	
	// these are set by positionTheChat so the color scheme is uniform by default
	Color backgroundColor = Color.lightGray;
	Color foregroundColor = Color.darkGray;
	
	public GameLog(commonCanvas can)
	{
		canvas = can;
		scrollbar.alwaysVisible = false;
	}
	
    private boolean emitLine(Graphics gc,HitPoint highlight,int barWidth,Rectangle r,int ypos,int x,
    		int maxLineH,int mid,Color textColor,Text columns[],Color colors[],String moven)
    {
		int yp = G.Top(r) + ypos;
		int left = G.Left(r);
		
		boolean inside = false;
		
		GC.Text(gc, false, left + 2, yp, x - left - 2,maxLineH, textColor, null, moven);
		for(int col = 0,lastCol=columns.length-1; col<=lastCol; col++)
		{
    	int xp =  x + 2 + (col*mid);
     	Text pt = columns[col];
    	if(pt!=null)
    		{ int activeWidth = mid-((lastCol==col)?barWidth:0);
    		  boolean hit = G.pointInRect(highlight, xp,yp,activeWidth,maxLineH);
    		  Color color = colors[col];
    		  if(hit)
    			  { inside = true;
    			    highlight.spriteRect = new Rectangle(xp,yp,activeWidth,maxLineH);
    			    highlight.spriteColor= Color.red;
    			    highlight.hit_index = col;
    			    color = Color.lightGray;
    			  }
    		  pt.draw(gc, false, xp,  yp, mid - 4, maxLineH, textColor, color);
     		  columns[col] = null;
    		}
		}
		return(inside);
    }
     /**
     * combine two text lines. By default this consists of just
     * catenating the lines with a comma between.  The default
     * method notices if there is alread punctuation of some kind
     * at the join, and skips adding the comma.
     * @param p1
     * @param sms
     * @return the combined string
     */
    private Text combineLines(Text p1,Text sms)
    {
    	if(p1.equals("")) { return(sms); }
    	if(sms.equals("")) { return(p1); }
    	int len = p1.length();
        if ((len > 0)
        		&& (p1.charAt(len-1)>='0')
        		&& (sms.charAt(0) >= 'A')
        		&& (p1.charAt(len-1)!=':'))
        { //insert a comma between move parts unless there
          //is already some punctuation or spacing
            p1.append(",");
        }
        p1.append(sms);
        return(p1);
    }
    // combine two string arrays
    private String[] combineEvents(String first[],String second[])
    {
    	if(first==null) { return(second); }
    	if(second==null) { return(first); }
    	int lfirst = first.length;
    	int lsecond = second.length;
    	String res[] = new String[lfirst+lsecond];
    	for(int i=0;i<lfirst;i++) { res[i]=first[i]; }
    	for(int j=0;j<lsecond; j++) { res[lfirst+j] = second[j]; }
    	return(res);
    }
    // general strategy for mouse sensitivity.  Maintain the "HitPoint" object to indicate
    // what item is current under the mouse and clickable.  Most of the repainting operations
    // note when the paint over the hitpoint and display a highlighted version.  The
    // same functions, with a gc of null, don't paint anything but perform the logical 
    // operations so the hit point is noted.
    int gameLogScrollY = 0;
    private int startingIdx = -1;
    private int startingYpos = -1;
    private int startingMaxLineheight = -1;
    private int lastHistorySize = -1;
    private int lastHistoryStep = -1;
    private int totalHeight = -1;
    
    /**
     * draw the standard game log.  This method uses {@link #censoredMoveText}, which uses
     * {@link commonMove#shortMoveText}
     * @param gc
     * @param highlight
     * @param r
     * @param textColor
     * @param highlightColor
     * @param playerFont the font to use to draw player names
     * @param lineFont the font to use for log lines
     */
    public void redrawGameLog(Graphics gc,HitPoint highlight, Rectangle r, 
    		Color textColor, Color highlightColor,
    		Font playerFont,Font lineFont)
    {	
      	boolean scrolled = scrollbar.mouseIsActive();
      	HitPoint mainHighlight = scrolled&!scrollbar.activelyScrolling() ? highlight : null;
        if (((gc != null)||G.pointInRect(mainHighlight,r)) && canvas.rectangleIsVisible(r))
        {	//G.Assert(Thread.currentThread()==runThread,"running in the wrong thread");

        	redrawGameLog_internal(gc,highlight,r,textColor,highlightColor,playerFont,lineFont);
        }
    }
    private void redrawGameLog_internal(Graphics gc,HitPoint highlight, Rectangle r, 
    		Color textColor, Color highlightColor,
    		Font playerFont,Font lineFont)
    {	
    	BoardProtocol b = canvas.getBoard();
        int numPlayers = (b==null) ? 0 : b.nPlayers();
        if(numPlayers>0)
        {
      	boolean barvisible = scrollbar.scrollBarVisible();
      	boolean baractive = scrollbar.mouseIsActive();
      	boolean scrolled = baractive;
      	int barWidth = barvisible ? scrollbar.getScrollbarWidth() : 0;
      	FontMetrics myFM = G.getFontMetrics(lineFont);
            int rowHeight = myFM.getAscent() + myFM.getDescent(); // not height, too much for some fonts
         
 
        	CommonMoveStack history = canvas.History;
            int sz = history.size();
            int historyStep = history.viewStep;
            if(sz!=lastHistorySize || historyStep!=lastHistoryStep) 
            	{ scrollbar.setHasNewScrollPosition(false);
            	  lastHistoryStep = historyStep;
            	  lastHistorySize = sz;
            	}
            int scrollY = scrolled 
            				? scrollbar.getScrollPosition() 
            				: gameLogScrollY;
        HitPoint mainHighlight = scrollbar.thumbScrolling() ? null : highlight;
            int highlightYPos = -1;
            int maxLineH = 0;
            // basic layout, 1 cell for the row number, then two columns for the moves
            GC.setColor(gc,textColor);
            GC.setFont(gc,playerFont);

            int rownumWidth = myFM.stringWidth("XXX");
            int x = G.Left(r) + rownumWidth;
            int y = G.Top(r);
            int mid = (G.Width(r) - rownumWidth) / numPlayers;
            
            for(int i=0;i<numPlayers;i++)
            {
            	String name = canvas.prettyName(i);
            	GC.Text(gc, false, x+i*mid, y, mid, rowHeight, textColor, null, name);
            }

            GC.setFont(gc,lineFont);

            String moven = null;
            Text p1 = TextChunk.create("");
            int column = 0;
            int boxPos = rowHeight;
            int ypos = 0;
            int lastSeenY = 0;
            int boxH = G.Height(r);
            boolean linebreak = false;
            boolean needLineNumber = false;
            Text columns[] = new Text[numPlayers];
            Color bgColors[] = new Color[numPlayers];
            int indexes[] = new int[numPlayers];
            String rowMoveNumber = "";
            int maxEverLineHeight = rowHeight;
            int p1Index = 0;
            for (int first = 0, idx = 0; (idx <= sz) ; idx++)
            {
                commonMove sp = (idx == sz) ? null
                                            : (commonMove) history.elementAt(idx);
                Text sms = (sp==null) ? TextChunk.create("") : canvas.censoredMoveText(sp,idx); 
                String smsString = sms.getString();
                
                // this is a hack to make the log look just right.  Ignore the first element if it is "start"
                if((idx==0)&&"start".equalsIgnoreCase(smsString))
                	{ sms = TextChunk.create(""); smsString=""; 
                	  column = sp.player;
                	  p1Index = idx;
                	}
                String newnum = (sp == null) ? "" : ("".equals(smsString) ? moven : sp.getSliderNumString());
                int newplayer = (sp==null)? (column+1) : sp.player;
                if (moven == null)
                {
                    moven = newnum;
                }
                else if (linebreak || !moven.equals(newnum))
                { // changing moves

                    Color bgcolor = null;
                    if ((first <= historyStep) && (historyStep < idx))
                    {
                        bgcolor = highlightColor;
                        highlightYPos = ypos;
                    }
                    int lineh = p1.lineHeight(myFM);
                    maxLineH = Math.max(lineh,maxLineH);
                    if (ypos >= scrollY)
                    {	// collect all the columns so we can use a common baseline for the whole row
                        columns[column] = p1;
                        bgColors[column] = bgcolor;
                        indexes[column] = p1Index;
                        //G.print("Col "+columns[column]+" "+idx);
                        if(!needLineNumber) 
                        	{ rowMoveNumber = moven;
                        	  needLineNumber = true; 
                        	}
                    }
                    if ( (newplayer<(column+1)) || ((column+1)>=numPlayers))
                    {   // emit a complete line
                    	maxEverLineHeight = Math.max(maxLineH,maxEverLineHeight);
                    	if(needLineNumber && ((boxPos+maxLineH)<boxH))
                    	{	
                    		if(emitLine(gc,mainHighlight,barWidth,r,boxPos,x,maxLineH,mid,textColor,columns,bgColors,rowMoveNumber))
                    		{	int ind = highlight.hit_index;
                     			highlight.hitObject = history.elementAt(indexes[ind]);
                    			highlight.hitCode = GameId.HitGameRecord;
                     		}
                    		lastSeenY = ypos+maxLineH;
                    		boxPos+= maxLineH;
                    	}
                    	ypos += maxLineH;
                    	needLineNumber = false;
                    	maxLineH = 0;
                        column = newplayer;
                        p1Index = idx;
                    }
                    else
                    {
                        column=newplayer;
                        p1Index = idx;
                        if (moven.length() == 1)
                        {
                            moven = " " + moven;
                        }
                     }
                    
                    linebreak=false;
                    moven = newnum;
                    p1 = TextChunk.create("");
                    first = idx;
                }

                if (sp != null)
                {
               	   //p1Index = idx; 
                   //if(sms.length() > 0) 
                   // 	{p1 = combineLines(p1,sms);
                   // 	}
                   // if(p1.length()>0) { linebreak |= sp.getLineBreak();	}
                    
                    // this is an attempt to avoid constructing absurdly long lines
                    // when there are a lot of actions in a turn.  Case in point
                    // is word games, where you can shuffle tiles a lot
                    Text savedLine = p1==null ? null : p1.clone();
                    Text newline = combineLines(p1,sms);
                    if(savedLine!=null 
                    		&& (savedLine!=newline)
                    		&& (newline!=sms)
                    		&& (newline.width(myFM)>mid))
                    {
                    	linebreak = true;
                    	p1 = savedLine;
                    	idx--;
                    	}
                    else 
                    { 
                    p1 = newline; 
                    if(p1.length()>0) { linebreak |= sp.getLineBreak();	}
                }

                }
            }
            maxEverLineHeight = Math.max(maxLineH,maxEverLineHeight);
            // take care of the trailing line if we didn't fill the last line
            if(needLineNumber && ((boxPos+maxLineH)<boxH))
        	{	
        		if(emitLine(gc,mainHighlight,barWidth,r,boxPos,x,maxLineH,mid,textColor,columns,bgColors,rowMoveNumber))
        		{	
        			int ind = highlight.hit_index;
         			highlight.hitObject = history.elementAt(indexes[ind]);
         			highlight.hitCode = GameId.HitGameRecord;
        		}
        		boxPos += maxLineH;
        		lastSeenY = ypos+maxLineH;

        	}
            ypos += maxLineH;
            totalHeight = ypos;
            maxLineH = 0;
            // finally, frame the whole thing
            GC.frameRect(gc, Color.blue, G.Left(r),G.Top(r), G.Width(r), Math.min(ypos-scrollY+rowHeight,boxH-1));
            boolean recalc = false;
            if(!scrolled)
            {
            	recalc = autoScroll(historyStep,highlightYPos,lastSeenY,ypos,boxH-1,rowHeight);
            }        

        GC.setFont(gc,canvas.standardPlainFont());
        
        int big = boxH/2;
        // the scroll bar, if visible, overlays the right part of the log area
        // the vertical parameters of the scroll bar are in pixel units
        drawScrollbar(gc,r,scrollY,rowHeight,big,totalHeight+rowHeight*2-boxH);
        if(recalc && (gc!=null))
        {	// if we made a big jump and are recaclulating, draw again with no graphics to 
        	// do the recalculation in advance of the next real repaint.  This avoids a double
        	// refresh.
        	redrawGameLog_internal(null,highlight,r,textColor,highlightColor,playerFont,lineFont);
        }
        }
    }
    /**
     * draw a game log in 2 column format, where the left column is the player/move number
     * and the right column is the move activity.  This is more suitable for multiplayer games
     * or games where the turn order is variable.  This method uses several helper functions
     * that can be defined in the viewer class to customize the behavior see {@link #censoredMoveText}
     * {@link #colorize} {@link online.common.commonMove#gameEvents } {@link #combineLines }
     * 
     * @param gc
     * @param highlight
     * @param r
     * @param textColor	color for the main text
     * @param highlightColor color for highlighted text (at the current move point)
     * @param bold bold font
     * @param normal non-bold font
     */
       public void redrawGameLog2(Graphics gc, HitPoint highlight, Rectangle r,
           Color textColor,Color highlightColor,Font bold,Font normal)
       {	//
       	// note on the consolidation of redrawGameLogs.  There were originally
       	// 4 slightly different versions.  This is based on the "Tammany hall" version,
       	// which was the newest.  
       	//
       	//G.startLog("start log "+gameLogScroll);
    	boolean scrolled = scrollbar.mouseIsActive();   
    	boolean active = scrollbar.activelyScrolling();
        HitPoint mainHighlight = scrolled && !active ? highlight :null;
    	    	
    	if (((gc != null)||G.pointInRect(mainHighlight, r)) && canvas.rectangleIsVisible(r))
           {
    		 redrawGameLog2_internal(gc,mainHighlight,r,textColor,highlightColor,bold,normal);
            }
       }
       private void redrawGameLog2_internal(Graphics gc,HitPoint highlight,Rectangle r,
    		   	Color textColor,Color highlightColor,Font bold,Font normal)
       {
    	boolean barvisible = scrollbar.scrollBarVisible();
    	int barWidth = barvisible ? scrollbar.getScrollbarWidth() : 0;
    	boolean scrolled = scrollbar.mouseIsActive();   
	    	
       
	   FontMetrics myFM = G.getFontMetrics(normal);
           int rowHeight = myFM.getAscent() + myFM.getDescent(); // not height, too much for some fonts
           CommonMoveStack history = canvas.History;
           int sz = history.size();
           int historyStep = history.viewStep;
           if(sz!=lastHistorySize || historyStep!=lastHistoryStep) 
               	{ lastHistoryStep = historyStep;
               	  lastHistorySize = sz;
               	}
           	// scrolled means the scroll bar was changed from inside
       		// in which case we use the scrolled value
           
           int scrollY = scrolled ? scrollbar.getScrollPosition() : gameLogScrollY;
           int highlightYPos = -1;
           int hr = G.Height(r);
                
           // basic layout, 1 cell for the row number, then two columns for the moves
           GC.setColor(gc,textColor);
           GC.setFont(gc,bold);

           int width = G.Width(r);
           int rownumWidth = Math.min(myFM.stringWidth("XXXXXXXXXXXX"),width/2);
           int x = G.Left(r) + rownumWidth;
           int y = G.Top(r);
           int mid = (width - rownumWidth);

           GC.setFont(gc,normal);

           String moven = null;
           String prevMoven = null;
           int player = -100;
           Text currentLine = TextChunk.create("");
           int currentIndex = 0;
           String gameEvents[] = null;
           boolean linebreak = false;
           int idx = 0;
           int ypos = 0;
           int lastSeenY = ypos;
           int maxLineHeight = rowHeight;
           if(sz>0 && (history.elementAt(0).op == MOVE_START)) { idx++;  }
           boolean recalculating = startingIdx<0; 
       HitPoint mainHighlight = scrollbar.thumbScrolling() ? null : highlight;
           if(!recalculating && !scrolled)
           		{	
        	    // saving these numbers allows the log to be 
               	// restarted at the current position, so you don't
               	// have to troll thought all the parts that precede the piece
               	// that is actually displayed.
               	// it turns out this is a noticeable problem for long games
               	// such as viticulture
               	idx = startingIdx;
               	lastSeenY = ypos = startingYpos;
               	maxLineHeight = startingMaxLineheight;
               }

               int smsidx = idx;
               int smsypos = ypos;
               int smsheight = maxLineHeight;
           	   boolean first = !scrolled;
           	   boolean earlyExit = false;
           	   //G.print("scr "+scrollY+ " "+idx);
               while (idx < sz && !earlyExit)
               {	

               	commonMove sp = history.elementAt(idx);
               	Text sms = canvas.censoredMoveText(sp,idx);
               
               	String newnum = sp.getSliderNumString();
            	int nextIdx = idx;
                   // look for reasons to break rather than add this line to the current display line
               	if((moven!=null) 
               			&& (newnum!=null) 
               			&& !"".equals(newnum) 
               			&& !newnum.equals(moven))
                   	{	// changing move number
                   	linebreak = true;
                   	}
                if((player!=-100) && (player!=sp.player)) 
               		{ linebreak = true; 
               		}
 
                if(!linebreak) 
                { 	// process this line
                	if(!"".equals(newnum)) { moven = newnum; }
                    gameEvents = combineEvents(gameEvents,sp.gameEvents());
                // this is an attempt to avoid constructing absurdly long lines
                // when there are a lot of actions in a turn.  Case in point
                // is word games, where you can shuffle tiles a lot
                Text savedLine = currentLine==null ? null : currentLine.clone();
                Text newline = combineLines(currentLine,sms);
                if(savedLine!=null 
                		&& (savedLine!=newline)
                		&& (newline!=sms)
                		&& (newline.width(myFM)>mid))
                {
                	linebreak = true;
                	currentLine = savedLine;
                }
                else 
                { currentLine = newline; 
                    player = sp.player;
                    linebreak = sp.getLineBreak();
                    if(linebreak) { idx++; }	// skip ahead too
                }
            }
                if(!linebreak) 
                { 
                	idx++; 
                }			// advance if we are not outputting
                if(idx>=sz) { linebreak = true; }	// make sure we do the last line

                if(idx!=nextIdx && nextIdx==historyStep)
                	{
                    	
                	highlightYPos = ypos;
                	}
 
             	if(linebreak)
              	{	
                 // output related to the accumulated line
            	int owed = 0;
                int breakYpos = ypos;
            	if(idx>=sz || currentLine.length()>0 || (gameEvents!=null))
              	{	// output the line we have built
              		int h = currentLine.lineHeight(myFM);
                   maxLineHeight = Math.max(h,maxLineHeight);
                   if(ypos>=scrollY)
                   {
                   if(ypos+h<scrollY+hr)
                   {
                	if(first)
                   {	// remember where we started
                   	startingIdx = smsidx;
                   	startingYpos = smsypos;
                       	startingMaxLineheight = smsheight;
                       	first = false;
                   }
              		Color bgColor = breakYpos==highlightYPos ? highlightColor : null;
   					String mn = moven +" "+canvas.prettyName(player);
                       if(!mn.equals(prevMoven))
                          	{int l = G.Left(r);
                          	 GC.Text(gc, false, l + 2, y + (ypos-scrollY), x-l ,	h, textColor, bgColor, mn);
                          	}
                    prevMoven = mn;
                    int yco = y + (ypos-scrollY);
                    int boxw = mid - barWidth;
                    boolean inside = G.pointInRect(mainHighlight, x,yco,boxw,h);
          			if(inside)
      				{ 
      				mainHighlight.spriteRect = new Rectangle(x,yco,boxw,h);
          				  mainHighlight.spriteColor = Color.red;
          				  mainHighlight.hit_index = currentIndex;
          				  mainHighlight.hitObject = history.elementAt(currentIndex);
          				  mainHighlight.hitCode = GameId.HitGameRecord;
          				}
                   	currentLine.draw(gc, false , x + 2, yco, mid - 4, h, textColor, bgColor );
                   	lastSeenY = ypos+h;
                   }
                   else if(historyStep>=0 && !recalculating) 
      				{ earlyExit=true; }	// shut off the rendering
                   }
                   
                   if(currentLine.length()>0) 
                   	{ ypos += h; 
                   	}
                   	else 
                   	{ owed = h; 
                   	}
                   
               	}
               if(gameEvents!=null)
                 {	
                 	for(int gameEventsIndex = 0;gameEventsIndex<gameEvents.length;gameEventsIndex++)
                 	{  
                		String msg = gameEvents[gameEventsIndex];
                		Text chunk = canvas.colorize(msg);
                		int h = currentLine.lineHeight(myFM);
                  		if(ypos>=scrollY)
                		{
                  		maxLineHeight = Math.max(h,maxLineHeight);
                	if(first)
                	{	// remember where we started
                       	startingIdx = smsidx;
                       	startingYpos = smsypos;
                       	startingMaxLineheight = smsheight;
                       	first = false;
                    }

                  		if ((ypos + h) <= (scrollY+hr))
		               		{
                  			int yco = y + (ypos-scrollY);
                  			int boxw = mid-barWidth;
                  			boolean inside = G.pointInRect(mainHighlight, x,yco,boxw,h);
                  			if(inside && !mainHighlight.dragging)
              				{ 
              				mainHighlight.spriteRect = new Rectangle(x,yco,boxw,h);
                  				  mainHighlight.spriteColor = Color.red;
                   				  mainHighlight.hitCode = GameId.HitGameRecord;
                   				  mainHighlight.hitObject = history.elementAt(currentIndex);
                  				}
                      		Color bgColor = breakYpos==highlightYPos ? highlightColor : null;
		               		chunk.draw(gc, false , x + 2, yco, mid - 4, h, Color.darkGray, bgColor);
		               		lastSeenY = ypos+h;
		               		}
	               			else if(historyStep>=0 && !recalculating) 
	               				{ earlyExit = true; }	// shut off the rendering
                  		}
                  		
                   	  ypos += h;
                  	  owed = 0; 
                 	}
                }
            
            ypos += owed;
            owed = 0; 

            if(idx>=sz) { totalHeight = ypos; }	// we saw the end
            
       		linebreak = false;
       		gameEvents = null;
       		player = -100;
       		// starting a new line
       		currentLine = TextChunk.create("");
       		currentIndex = idx;
       		smsidx = idx;
       		smsypos = ypos;
       		smsheight = maxLineHeight;
       		moven = null;
            } // end of linebreak

           } // end of while

           GC.frameRect(gc, Color.blue, G.Left(r), G.Top(r), G.Width(r), Math.min(y+lastSeenY,hr));
       boolean recalc = false;
           if(!scrolled)
             {
    	   recalc = autoScroll(historyStep,highlightYPos,lastSeenY,totalHeight,hr,rowHeight);

             }
           GC.setFont(gc,canvas.standardPlainFont());
           // the scroll bar, if visible, overlays the right part of the log area
           // the vertical parameters of the scroll bar are in pixel units
           int big = hr/2;
           drawScrollbar(gc,r,scrollY,rowHeight,big,totalHeight-hr+rowHeight);

       if(recalc && (gc!=null))
       	{
    	// draw again with no graphics output, so the next real refresh will be correct
    	// and not require another repaint.  This prevents a double flash of the log when
    	// there is a major relocation
    	redrawGameLog2_internal(null,  highlight,r,textColor,highlightColor,bold,normal); 
           }
       }
       
       /**
        * adjust the scroll position based on what is displayed.  The two main considerations
        * are the rollback position, acquired from the game history, and the end of the record
        * Nominally, we aim to center the rollback position, and extend the game record all the
        * way to the bottom of the view rectangle.
        * 
        * @param highlightYPos
        * @param lastSeenY
        * @param ypos
        * @param hr
        * @param rowHeight
        */
       private boolean autoScroll(int historyStep,int highlightYPos,int lastSeenY,int ypos, int hr, int rowHeight)
       {
    	   int nextscrollY = gameLogScrollY;
    	   boolean recalc = false;
           if(highlightYPos>=0)
           { // scrolled back, and we saw the position
               boolean seenend = lastSeenY >= ypos;
               int position = highlightYPos-nextscrollY;
               if(seenend)
               {
            	nextscrollY = Math.max(0, Math.min(highlightYPos-hr/2, ypos-hr+rowHeight));
               }
               else if((position<hr/4) || (position+rowHeight>=3*hr/4))
               {
        	   nextscrollY = Math.max(0,highlightYPos-hr/2);
               }
           }
           else if(historyStep>=0)
           {  // find it for sure
        	   nextscrollY = 0;
        	   gameLogScrollY = 1;
        	   recalc = true;
           }
           else
           {
           // we're not scrolled, we need to see the end
           nextscrollY = Math.max(0, ypos-hr+rowHeight);
           }

           if (gameLogScrollY != nextscrollY)
           	{	
        	 if(nextscrollY<gameLogScrollY)
        		   {
        	   startingIdx = -1;
        		   recalc = true;
        		   }
  		   	if(recalc) { canvas.repaint(20); }
        	gameLogScrollY = nextscrollY;

            }
           return(recalc);
       }
       /**
        * configure the scrollbar and draw it.
        * 
        * @param gc
        * @param r
        * @param scrollPos
        * @param bigJump
        * @param scrollMax
        */
       public void drawScrollbar(Graphics gc,Rectangle r,int scrollPos,int smallJump,int bigJump,int scrollMax)
       {
           int scrollw = (int)(ScrollArea.DEFAULT_SCROLL_BAR_WIDTH*G.getDisplayScale());
           scrollbar.InitScrollDimensions(G.Right(r)-scrollw, r , scrollw,scrollMax, smallJump, bigJump);
           //
           // the scroll bar position is choreographed between the changes caused by internal scroll
           // actions, and changes to the game log caused by actual changes in the game history.
           // internal changes freeze the scroll position until an external change occurs
           //
           scrollbar.setScrollPosition(scrollPos); 
           scrollbar.backgroundColor = backgroundColor;
           scrollbar.foregroundColor = foregroundColor;
           scrollbar.drawScrollBar(gc);
       }
	 /**
	  * hook from all sorts of mouse activity except the scroll wheel
	  * @param ex
	  * @param ey
	  * @param upcode
	  * @return
	  */
       public boolean doMouseMotion(int ex,int ey,MouseState upcode)
       {
    	   return scrollbar.doMouseMotion(ex, ey, upcode);
       }
       /**
        * hook from mouse scroll wheel events
        * @param ex
        * @param ey
        * @param amount
        * @return
        */
       public boolean doMouseWheel(int ex,int ey,int amount)
       {	return scrollbar.doMouseWheel(ex,ey,amount);
       }
       
       /**
        * this is the hook to repeat scrolling triggered by holding the mouse down.
        * it should be called from an even loop
        * @return
        */
       public boolean doRepeat()
       {	return scrollbar.doRepeat();
       }
}