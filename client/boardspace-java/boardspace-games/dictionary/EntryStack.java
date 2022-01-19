package dictionary;

import java.util.Enumeration;

import lib.OStack;

/**
 * a stack of dictionary entries
 * 
 * @author Ddyer
 *
 */
public class EntryStack extends OStack<Entry>
{
	public Entry[] newComponentArray(int sz) {
		return(new Entry[sz]);
	}
	// absorb the contents of a dictionary segment
	public void union(DictionaryHash with)
	{
		if(with!=null)
		{
			for(Enumeration<Entry> k = with.elements(); k.hasMoreElements();) 
			{
			Entry name = k.nextElement();
			push(name);
			}
		}
	}
}