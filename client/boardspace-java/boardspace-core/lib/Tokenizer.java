package lib;

import java.util.Enumeration;

/**
 * this is a replacement for StringTokenizer.  If double quote is included
 * in the delimiters list (it is by default) then substrings within double
 * quotes are included as a single token.
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
	String delimiters = " \n\t\r\"";
	StringBuilder builder = new StringBuilder();
	
	public Tokenizer(String str)
	{
		basis = str;
		if(str!=null)
		{
			maxIndex = str.length();
			next = nextElement();
		}
	}
	public boolean hasMoreElements() {
		return next!=null;
	}
	public String nextToken()
	{	String n = next;
		next = nextElement();
		return(n);
	}
	public int intToken() { return G.IntToken(nextToken()); }
	
	public String getRest() { return(basis.substring(restIndex)); }

	public String nextElement() {
		restIndex = index;
		boolean inquote = false;
		boolean literal = false;
		builder.setLength(0);
		int charsin = 0;
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
			else if(delimiters.indexOf(ch)>=0) 
				{ 	if(charsin>0) { index--; break; }
					if(ch=='\"') { inquote = true; }
					
					
				}
			else { builder.append(ch); charsin++; }
		}
		
		return ((charsin>0)?builder.toString() : null); 
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
