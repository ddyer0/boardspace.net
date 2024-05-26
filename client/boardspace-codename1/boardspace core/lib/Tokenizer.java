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
{	String basis = "";
	String next = null;
	int maxIndex = 0;
	int index = 0;
	int restIndex = 0;
	boolean alphaNumeric = false;
	public static String StandardDelimiters = " \n\t\r\"";
	public static String StandardSingletons = "()[]{}";
	String delimiters = StandardDelimiters;
	String singletons = StandardSingletons;
	StringBuilder builder = new StringBuilder();
	
	public Tokenizer(String str,String del)
	{
		basis = str;
		delimiters = del;
		if(str!=null)
		{
			maxIndex = str.length();
		}
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

	public String nextElement()
	{	String n = (next==null) ? parseNextElement() : next;
		next = null;
		return n;
	}
	
	public int intToken() { return G.IntToken(nextElement()); }
	public char charToken() { return nextElement().charAt(0); }
	public boolean boolToken() { return Boolean.parseBoolean(nextElement());	}
	public double doubleToken() { return G.DoubleToken(nextElement()); }
	public String getRest() { return(basis.substring(restIndex)); }

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
					if(ch=='\"') { inquote = true; }
					
					
				}
			else { builder.append(ch); charsin=true; }
		}
		
		return ((charsin)?builder.toString() : null); 
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

}
