package dictionary;

import lib.CompareTo;
import lib.G;


/**
 * dictionary entry
 * 
 * @author Ddyer
 *
 */
public class Entry implements CompareTo<Entry>
{
	public String word;			// the word (duh)
	public int order;				// order of this word in a word frequency list 
	public long letterMask;		// mask indicating which letters are used in the word
	private byte definitionData[] = null;
	public int setCompressedDefinition(byte s[]) { definitionData = Smaz.compress(s); return(definitionData.length); }
	

	public int setDefinition(String words)
	{
		if(words==null) { definitionData=null; return(0); }
		else
		{
			definitionData = Smaz.compress(words);
			/*
			String redef = getDefinition();
			G.Assert(words.equals(redef), "decode mismatch");
			
			byte sm[] = Smaz.compress(words.getBytes());
			String redef2 = Smaz.decompress(sm);
			G.Assert(words.equals(redef2), "decode mismatch");
			 */
			return(definitionData.length);
		}
	}
	public String getDefinition()
	{
		if(definitionData==null) { return(null); }
		else
		{ 
			return(Smaz.decompress(definitionData));
		}
	}
	
	public String toString() { return("< "+word+" "+order+">"); }
	// constructor
	public Entry(String n) { word = n; order = -1; letterMask = letterSet(word); }
	
	/**
	 *  this generates a letter mask in the canonical way.  Any other algorithms
	 * @param w  a word
	 * @return
	 */
	public static long letterSet(String w)
	{
		long s = 0;
		for(int i=w.length()-1; i>=0;i--)
		{	char ch = w.charAt(i);
			s = Dictionary.letterMask(s,ch);
		}
		return(s);
	}
	private int sortOrder()
	{
		return((order<=0)?Integer.MAX_VALUE : order);
	}
	public int compareTo(Entry o) {
		return(G.signum(sortOrder() - o.sortOrder()));
	}
	public int altCompareTo(Entry o) {
		return(-compareTo(o));
	}
}