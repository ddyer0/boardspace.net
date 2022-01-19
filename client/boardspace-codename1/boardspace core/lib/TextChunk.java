package lib;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.FontMetrics;
import bridge.Icon;

/**
 * class TextChunk is used to prepare strings for drawing colorized colors, with size
 * reduced to fit space available.   
 * 
 * @author ddyer
 *
 */
public class TextChunk implements Text
{
	
	TextChunk() {}	// default constructor
	
	public Icon getIcon(){ return(null); }
	
	/**
	 * construct a text chunk, with a particular color, segmented into lines if split is true
	 * @param str the text to use 
	 * @param c a color or null
	 * @param split true to split into lines
	 */
	TextChunk(String str,Color c, boolean split) 
		{ 
		  replacementData = data = str==null ? "" : str; 
		  dataColor = c;  
		  if(split) { split('\n'); }
		} 

	// legal configurations of next, down, and isLine
	// 
	// isLine = true, down = null					this is a single chunk line
	// isLine = true, next is the next line
	// isLine = false, next is the next chunk on the current line
	// down!=null this is a line composed of multiple chunks
	//
	private String data="";			// this is the string corresponding to the whole tree below this point
	String replacementData = "";	// data used when copying the object
	private Color dataColor = null;	// this is the color for the whole tree below this point
	
	private Text next = null;		// next chunk or next line
	private boolean isLine=false;	// true if this is a line header
	Text down = null;				// if a chain contains several chunks of different color, down leads to the chain
	
	public Text clone()
	{
		TextChunk copy = new TextChunk();
		copy.copyFrom(this);
		return(copy);
	}
	public void copyFrom(TextChunk from)
	{
		data = from.data;
		replacementData = from.replacementData;
		dataColor = from.dataColor;
		next = from.next==null ? null : from.next.clone();
		isLine = from.isLine;
		down = from.down==null ? null : from.down.clone();
	}
	
	public Text firstLine() { return(isLine?next:this); }
	public Text firstChunk() { return((down==null) ? this : down); }
	
	public Text nextLine() { return(isLine?next:null); }
	public Text nextChunk() { return(isLine?null:next); }
	
	public void setNext(Text x) { next = x; }
	public int nLines()
		{ int n=1;
		  Text ll = firstLine();
		  Text nl = ll;
		  {while ((nl = nl.nextLine())!=null) { ll = nl; n++; }
		  }
		  return(n); 
		}
	
	public Text lastLine() 
		{ Text ll = firstLine();
		  Text nl = ll;
		  {while ((nl = nl.nextLine())!=null) { ll = nl; }
		  }
		  return(ll);
		}
	public Text lastChunk() 
		{	Text fc = firstChunk();
			Text nc = fc;
			while((nc=nc.nextChunk())!=null) { fc = nc; }
			return(fc);
		}

	private void split(char at)
	{	if(!isLine)
		{
		String lines[] = G.split(data,at);
		if(lines.length>1)
			{
			isLine = true;
			G.Assert(next==null,"Should be no next yet");
			for(int lim=lines.length-1; lim>=0; lim--) 
				{ TextChunk newchunk = new TextChunk(lines[lim],dataColor,false);
				  newchunk.next = next;
				  newchunk.isLine = true;
				  next = newchunk;
				}
			}
		}		
	}
	
	public Text beJoined(Text... chunks)
	{
		if(isLine) 
			{ TextChunk n = new TextChunk();
			  n.beJoined(chunks);
			  down = n;
			}
		else { 
			String message = "";
			for(int i=0,lim=chunks.length-1;i<=lim;i++) 
			{ Text chunk = chunks[i];
			  message += chunk.getString(); 
			  chunk.lastChunk().setNext( (i<lim) ? chunks[i+1].firstChunk() : null);
			}
			data = message;
			down = chunks[0].firstChunk();
			}
		return(this);
	}
	public static Text join(Text... chunks)
	{	
		return(new TextChunk().beJoined(chunks));
	}

public int width(FontMetrics myFM)
    {	int val = 0;
    	for(Text vv = firstLine(); vv!=null; vv=vv.nextLine())
    		{ val = Math.max(vv.lineWidth(myFM),val); 
    		}
    	return(val);
    }
public int lineWidth(FontMetrics myFM)
	{
	int w = 0;
	for(Text chunk = firstChunk(); chunk!=null; chunk=chunk.nextChunk())
		{
		w += chunk.chunkWidth(myFM);
		}
	return(w);
	}

public int height(FontMetrics myFM) 
{	int h = 0;
	for(Text vv = firstLine(); vv!=null; vv=vv.nextLine())
		{
		 h += vv.lineHeight(myFM);
		}
	return(h);
}

public int chunkWidth(FontMetrics myFM) {
	return(myFM.stringWidth(data));
}

/** this method is overridden by  TextGlyph */
public int singleChunkHeight(FontMetrics myFM) { return(myFM.getHeight()); }

public int chunkHeight(FontMetrics myFM) 
{
	if(down!=null)
	{
		return(down.chunkHeight(myFM));
	}
	int h=singleChunkHeight(myFM);
	if(next!=null) { h = Math.max(h,next.chunkHeight(myFM)); }
	return(h);
}
public int lineHeight(FontMetrics myFM)
{	int val = 0;
	for(Text chunk = firstChunk(); chunk!=null; chunk=chunk.nextChunk())
	{	val = Math.max(chunk.chunkHeight(myFM),val);	
	}
	return(val);
}

public void colorize(InternationalStrings s,Text... coloredChunks)
    {	
    	if(down!=null)
    		{ down.colorize(s,coloredChunks);  
    		  if(!isLine) { return; }
     		}
    	if(next!=null)
    		{
    		  next.colorize(s,coloredChunks);
    		}
	    for(Text chunk : coloredChunks)
		{
	    String str0 = chunk.getString();
		String str = (s==null) ? str0 : s.get(str0);
		int strlen = str.length();
		int ind = data.indexOf(str);
		if((ind>=0)				// match
				&& ((ind==0) || !G.isLetterOrDigit(data.charAt(ind-1)))	// at the beginning or preceded by a space 
				&& (((ind+strlen)==data.length()) || !G.isLetterOrDigit(data.charAt(ind+strlen))) // at the end or followed by a space
				)
			{
			Text left = new TextChunk(data.substring(0,ind),dataColor,false);
			Text middle = chunk.cloneSimple();
			Text right = new TextChunk(data.substring(ind+str.length()),dataColor,false);
			left.colorize(s,coloredChunks);
			right.colorize(s,coloredChunks);
			beJoined(left,middle,right);
			}
		}
	    if(next!=null) { next.colorize(s,coloredChunks); }
    }
    /**
     * create a colorized multi-line text string from text.  coloredChunks is an array of simple colored textchunks.  
     * s is the current language translations, if needed to translate the colored chunks
     * into the current language.
     * 
     * @param line
     * @param s
     * @param coloredChunks
     * @return a colorized text chunk
     */
    public static Text colorize(String line,InternationalStrings s,Text... coloredChunks)
    {	Text ch = new TextChunk(line,null,true);
    	ch.colorize(s,coloredChunks);
    	return(ch);
    }
    /**
     * create a single line text chunk from text
     * @param text
     * @return a Text
     */
    public static Text create(String text) { return(new TextChunk(text,null,false)); }
 
    /**
     * create a single line text chunk from text
     * @param text
     * @param color
     * @return a Text
     */

    public static Text create(String text,Color c) { return(new TextChunk(text,c,false)); }
    /**
     * create a multi-line text chunk from text
     * @param text
     * @return a Text
     */
    public static Text split(String text) { return(new TextChunk(text,null,true)); }
    /**
     * create a multi-line text chunk from text
     * @param text
     * @param color
     * @return a Text
     */
    public static Text split(String text,Color c) { return(new TextChunk(text,c,true)); }
    
    public Color getColor() { return(dataColor); }
	public String getString() { return(data); }
	public int length() { return(data.length()); }
	public boolean equals (String str) { return(data.equals(str)); }
	public char charAt(int idx) { return(data.charAt(idx)); }
	@SuppressWarnings("deprecation")
	public String toString() { return("<"+getClass().getSimpleName()+" "+((dataColor==null)?"":dataColor)+data+">"); }
	public void append(String str) 
		{	append(new TextChunk(str,dataColor,false));
		}
	public TextChunk cloneSimple() { return(new TextChunk(replacementData,dataColor,false)); }
	public void append(Text str)
	{	if(isLine && (down!=null)) { lastLine().append(str); }
		else 
		{ 
		if(down==null)
		{	next=null;
			beJoined(this.cloneSimple(),str);	// join 2 chunks, make the current node the hub
		}
		else
			{	
				down.lastChunk().setNext(str);
				data += str.getString();
			}
		}
	}

	// tweaked 2/5/2020 using euphoria decorations as a model
	public Font selectFontSize(Graphics inG,int inWidth,int inHeight)
    {	if(inG!=null)
    	{
        Font f0 = GC.getFont(inG);
        Font f = f0;
        
    	if(inWidth>=0)	// if width is negative, use the original font size
    	{
    	FontMetrics myFM = GC.getFontMetrics(inG);
        int neww = width(myFM);
        int nlines = nLines();
        int siz = G.getFontSize(f0);
        int linesize = ((myFM.getAscent() + myFM.getDescent())*9)/10;
        if((inHeight>8)&&nlines>1)
        	{ siz = Math.max(8,Math.min(linesize,inHeight/nlines)); 		// limit the font size vertically
        	}
        int xoff = -1;
        while ((xoff < 0) && (siz > 6))
        	{	// find a smaller font, within reason
        	f = G.getFont(f0,siz--);
            myFM = G.getFontMetrics(f);
            neww = width(myFM);
            xoff = (inWidth - neww);
        	}
    	}
    	GC.setFont(inG,f);
    	
    	return(f0);
    	}
    	return(null);
    }
    public int drawTextChunk(Graphics inG,FontMetrics myFM,Color baseColor,int drawX,int drawY)
    {
    	if(down!=null) { return(down.drawTextLine(inG,myFM,baseColor,drawX,drawY)); }
    	else { 
    		GC.Text(inG,data,drawX,drawY);
			return(drawX + chunkWidth(myFM));
    	}
    }
    
    // draw the entire text tree described by this chunk and all it's parts.
    public int drawTextLine(Graphics inG,FontMetrics myFM,Color baseColor,int drawX,int drawY)
    {	Text message = firstChunk();
    	while(message!=null)
    		{
    		Color color = message.getColor();
    		if(color==null) { color = baseColor; }
    		GC.setColor(inG,color);
    		drawX = message.drawTextChunk(inG,myFM,baseColor,drawX,drawY);
    		message = message.nextChunk();
    		}
    	return(drawX);
    }

    // tweaked 2/5/2020 using euphoria decorations as a model
    public int draw(Graphics inG, double rotation, boolean center, Rectangle r, Color fgColour, Color bgColor)
    {
    	return draw(inG,rotation,center,r,fgColour,bgColor,true);
    }
    // tweaked 2/5/2020 using euphoria decorations as a model
    public int draw(Graphics inG, double rotation, boolean center, Rectangle r, Color fgColour, Color bgColor,boolean fit)
    {	int l = G.Left(r);
    	int t = G.Top(r);
    	int w = G.Width(r);
    	int h = G.Height(r);
    	if(rotation!=0) {  GC.setRotation(inG, rotation, l+w/2, t+h/2); }
    	int v = draw(inG,center,false,l,t,w,h,fgColour,bgColor,fit);
    	if(rotation!=0) { GC.setRotation(inG, -rotation, l+w/2, t+h/2); }
    	return(v);
    }
    public int draw(Graphics inG, boolean center, int inX,
        int inY, int inWidth, int inHeight, Color fgColour, Color bgColor)
    {
    	return(draw(inG,center,false,inX,inY,inWidth,inHeight,fgColour,bgColor,true));
    }
    public int drawRight(Graphics inG, int inX, int inY,
            int inWidth, int inHeight, Color fgColour, Color bgColor)
        {
        	return(draw(inG,false,true,inX,inY,inWidth,inHeight,fgColour,bgColor,true));
        }
    public int drawRight(Graphics inG, Rectangle r, Color fgColour, Color bgColor)
        {
        	return(draw(inG,false,true,G.Left(r),G.Top(r),G.Width(r),G.Height(r),fgColour,bgColor,true));
        }
  

    public int draw(Graphics inG, boolean center, int inX, int inY, int inWidth, int inHeight, Color fgColour,
			Color bgColor, boolean fit) 
    {
		return draw(inG,center,false,inX,inY,inWidth,inHeight,fgColour,bgColor,true);
	}
    public int draw(Graphics inG, boolean center, boolean right,int inX, int inY,
        int inWidth, int inHeight, Color fgColour, Color bgColor,boolean fit)
    {  	int neww = 0;
    	if(inG!=null)
    	{
    	Color gColor = GC.getColor(inG);
    	Color baseColor = (fgColour==null) ? ((dataColor==null)?gColor:dataColor) : fgColour;
   	
        if (bgColor != null)
        {
            GC.fillRect(inG,bgColor,inX, inY, inWidth, inHeight);
        }

        if (data != null)
        {  	Font f = fit ?  selectFontSize(inG,inWidth,inHeight) : null;
        	FontMetrics myFM = GC.getFontMetrics(inG);
        	Text line = firstLine();
        	int nlines = nLines();
        	int lh = 9*(myFM.getAscent() + myFM.getDescent())/10;	// 90% of the standard size, squeeze the lines a little
        	int lcenter = ((inHeight + lh) / 2);
         	neww = width(myFM);
        	int drawY = inY +
           			((nlines > 1) 
            				? Math.max(0,lcenter-(nlines-1)*lh/2)
            				: lcenter-lh/8);
 
            while(line!=null)
            	{
            	int width = line.chunkWidth(myFM);
            	int drawX = center ? inX+(inWidth-width)/2 : right ? inX+inWidth-width : inX; 
            	drawX = line.drawTextLine(inG,myFM,baseColor,drawX,drawY);
            	drawY += lh;
            	line = line.nextLine();
            	}

            GC.setColor(inG,gColor);
            if(fit) { GC.setFont(inG, f); }
         }
    	}
        return (neww);
    }

	

}