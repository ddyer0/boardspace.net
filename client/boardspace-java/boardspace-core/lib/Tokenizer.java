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
package lib;

import java.util.Enumeration;

/**
 * this is a replacement for StringTokenizer which is more
 * customizable, and starts with a few customizations.
 * 
 * If double quote is included in the delimiters string (it is by default) 
 * then substrings within double quotes are included as a single token. 
 * 
 * Characters in the singletons string are always returned as single character tokens.
 * 
 * @author ddyer
 *
 */
public class Tokenizer implements Enumeration<String>
{	private String basis = "";
	private String next = null;
	private int maxIndex = 0;
	private int index = 0;
	private int restIndex = 0;
	boolean alphaNumeric = false;
	boolean keepDelimiters = false;
	public static String StandardDelimiters = " \n\t\r\"";
	public static String StandardSingletons = "()[]{}";
	public String delimiters = StandardDelimiters;
	public String singletons = StandardSingletons;
	StringBuilder builder = new StringBuilder();
	
	public void reload(String str)
	{
		basis = str;
		maxIndex = str.length();
		index = 0;
		restIndex = 0;
		next = null;
	}
	public Tokenizer(String str,String del)
	{
		basis = str;
		delimiters = del;
		if(str!=null)
		{
			maxIndex = str.length();
		}
	}
	public Tokenizer(String str,String del,boolean keep)
	{
		this(str,del);
		keepDelimiters = keep;
	}
	public Tokenizer(String str)
	{	this(str,StandardDelimiters);
	}
	/**
	 * a strict alphanumeric token
	 * 
	 * @param str
	 * @param alpha
	 */
	public Tokenizer(String str,boolean alpha)
	{
		this(str);
		alphaNumeric = alpha;
	}
	public boolean hasMoreElements() {
		if(next==null) { next=parseNextElement(); }
		return next!=null;
	}
	/** compatibility with StringTokenizer()
	 * 
	 * @return
	 */
	public boolean hasMoreTokens()
	{
		return hasMoreElements();
	}
	public String nextElement()
	{	String n = (next==null) ? parseNextElement() : next;
		next = null;
		restIndex = index;
		return n;
	}
	/** compatibility with StringTokenizer()
	 * 
	 * @return
	 */
	public String nextToken()
	{
		return nextElement();
	}
	public long longToken() { return G.LongToken(nextToken());	}
	public int intToken() { return G.IntToken(nextElement()); }
	public char charToken() { return nextElement().charAt(0); }
	public boolean boolToken() { return Boolean.parseBoolean(nextElement());	}
	public double doubleToken() { return G.DoubleToken(nextElement()); }
	/** returns the remainder of the string, skipping leading delimiters */
	public String getRest() { skipDelimiters(); return(basis.substring(restIndex)); }

	private String parseNextAlpha()
	{	builder.setLength(0);
		boolean charsin = false;
		while(index<maxIndex)
		{
			char ch = basis.charAt(index++);
			if(G.isLetterOrDigit(ch)) { charsin=true; builder.append(ch);}
			else if(charsin) { break; }
		}
		return charsin ? builder.toString() : null;
	}
	private void skipDelimiters()
	{	if(!keepDelimiters)
		{
		char ch = (char)0;
		while((index<maxIndex)
				&& (delimiters.indexOf((ch=basis.charAt(index)))>=0)
				&& (ch!='"'))	// double quote is special because it is paired
		{	
			index++;
			restIndex = index;	
		}}
	}
	
	private String parseNextElement() {
		if(alphaNumeric) { return parseNextAlpha(); }
		restIndex = index;
		boolean inquote = false;
		boolean literal = false;
		builder.setLength(0);
		boolean charsin = false;
		while(index<maxIndex)
		{
			char ch = basis.charAt(index++);
			
			if(literal) { builder.append(ch); literal = false; }
			else if(inquote) 
				{ if(ch=='\"') 
					{ inquote = false;
					  return builder.toString();
					}
				else if(ch=='\\') { literal = true; }
				else { builder.append(ch); }
			}
			else if(singletons.indexOf(ch)>=0) 
			{
				if(charsin) { index--; }
				else { builder.append(ch); charsin = true; }
				break;
			}
			else if(delimiters.indexOf(ch)>=0) 
				{ 	if(charsin) { index--; break; }
					else if(keepDelimiters)
					{
						builder.append(ch); 
						charsin=true;
						break;
					}
					if(ch=='\"') { inquote = true; }
					
	
				}
			else { builder.append(ch); charsin=true; }
		}
		
		return ((charsin)?builder.toString() : null); 
	}
    /**
     * Calculates the number of times that this tokenizer's
     * {@code nextToken} method can be called before it generates an
     * exception. The current position is not advanced.
     *
     * @return  the number of tokens remaining in the string using the current
     *          delimiter set.
     * @see     java.util.StringTokenizer#nextToken()
     */
    public int countTokens() {
    	
    	String nx = next;
    	int max = maxIndex;
    	int ind= index;
    	int rest = restIndex;

    	int n=0;
    	while(hasMoreTokens()) { nextToken(); n++; }
    	
    	maxIndex = max;
    	index = ind;
    	restIndex = rest;
    	next = nx;
    	
    	return n;
    }
	/*
	public static void main(String... args)
	{
		Tokenizer tok = new Tokenizer("this is a test \"o\\\"f the\" stuff");
		while(tok.hasMoreElements())
		{
			G.print("S '"+tok.nextElement()+"' ..."+tok.getRest());
		}
	}
	 */
    /** parseCol is needed for infinite boards such as che and hive
     * 
     * @return
     */
	public char parseCol() {
		return parseCol(nextToken());
	}
	
	public static char parseCol(String n)
	{	if(n==null) { return(char)0;}
		int len = n.length();
		if(len==1) { return n.charAt(0); }
		int off = G.IntToken(n.substring(0,len-1));
		char end = n.charAt(len-1);
		if(end=='A') { return (char)(end-off); }
		if(end=='Z') { return (char)(end+off); }
		throw G.Error("parse error for %s",n);
	}
	
}
