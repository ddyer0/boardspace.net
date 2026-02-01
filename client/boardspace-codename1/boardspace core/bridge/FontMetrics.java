/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package bridge;

import java.util.Hashtable;

import com.codename1.ui.Font;

import lib.FontManager;
import lib.G;
import lib.Graphics;
import com.codename1.ui.geom.Rectangle2D;

//There are these fonts now: https://www.codenameone.com/blog/good-looking-by-default-native-fonts-simulator-detection-more.html
class LruCache
{	int size=0;
	static int hits = 0;
	static int probes = 0;
	String strs[];
	int values[];
	public String toString() { return "<wc "+hits*100/probes+">"; }
	static int NOVALUE = - 0x023536215;
	LruCache(int sz)
	{	size = sz;
		strs = new String[sz];
		values = new int[sz];
	}
	
	synchronized int getValue(String key)
	{	probes++;
		for(int i=0;i<size; i++) 
			{ if(key==strs[i]) 
				{ hits++; 
				  int v = values[i];
				  while(i>0)
				  {
					  strs[i] = strs[i-i];
					  values[i] = values[i-1];
					  i--;
				  }
				return v; }
				}
		return NOVALUE;
	}

	synchronized void storeValue(String key,int val)
	{	for(int idx=size-2; idx>=0;idx--)
		{	strs[idx+1] = strs[idx];
			values[idx+1] = values[idx];
		}
		values[0] = val;
		strs[0] = key;
	}}

public class FontMetrics {
	
	
	Font myFont;
	static Hashtable <Font,FontMetrics>fmCache  = new Hashtable<Font,FontMetrics>();
	LruCache widthCache = new LruCache(3);
	public FontMetrics(Font f) 
		{ myFont = f;
		  // temporary until issue 4399 is fixed
		  if(!G.isIOS()) { fmCache.put(f,this); } 
		}
	public static FontMetrics getFontMetrics(Font g) 
	{	
		FontMetrics m = fmCache.get(g);
		if(m==null) 
			{	//G.print("add font "+g+" "+fmCache.size());
				m = new FontMetrics(g);
			}
		return m;
	}
	public int getMaxDescent() { return myFont.getDescent(); }
	
	public static FontMetrics getFontMetrics(Component c) 
		{ return( getFontMetrics(FontManager.getFont(c.getStyle()))); 
		}
	public static FontMetrics getFontMetrics(SystemGraphics g) 
		{ return(getFontMetrics(g.getFont())); 
		}
	
	public static FontMetrics getFontMetrics(Graphics g) 
	{ 	return(getFontMetrics(g.getFont())); 
	}
	
	public Rectangle2D getStringBounds(String str, Graphics context)
	{	 
		return(new Rectangle2D(0,0,stringWidth(str),getHeight()));
	}
	
	public Rectangle2D getStringBounds(String str, int from,int to,Graphics context)
	{
		return(new Rectangle2D(stringWidth(str.substring(0,from)),
				0,
				stringWidth(str.substring(from,to)),
				getHeight()));
	}

	public Font getFont() { return(myFont); }
	public int getSize() { return(SystemFont.getFontSize(myFont)); }
	
	public int stringWidth(String str) 
	{ 	int w = widthCache.getValue(str);
		// widthcache per font is only intended to optimize when
		// the same string is queried multiple times while being prepped for display
		if(w!=LruCache.NOVALUE) { return w; }
		w = myFont.stringWidth(str);
		widthCache.storeValue(str,w);
		return(w); 
	}
	public int getHeight() 
	{	int sys = getSize();
		int h = SystemFont.getHeight(myFont); 
		return Math.max(sys,h);
	}
	public int getDescent() { return(myFont.getDescent()); }
	public int getAscent() { return(myFont.getAscent()); }
	public int getMaxAscent() 
		{ // the standard spec is a little fuzzy, it says
		  // "ascent" is the height for most alphanumeric characters,
		  // but some characters may be taller.  So we hope getAscent is 
		  // good enough
			return(myFont.getAscent());	
		}
}
