package dictionary;

import java.util.Enumeration;
import java.util.Hashtable;

import lib.G;

@SuppressWarnings("serial")
/**
 * a hash table from strings to dictionary entries
 * @author Ddyer
 *
 */
public class DictionaryHash extends Hashtable<String,Entry>
{	public int size  = 0;
	public DictionaryHash(int x) { size = x; }
	public void removeFakes()
	{	
		int fakes=0;
		int nonfakes=0;
		for(Enumeration<String>k = keys(); k.hasMoreElements(); )
		{	String name = k.nextElement();
			Entry item = get(name);
			if(item.order<=0)
			{
				remove(name);
				if((name.length()<=3)||(name.length()>7)) 
					{ //G.print("Fake word: "+name);
					  fakes++;
					}
				else { //G.print("fake short word: " +name); 
					}
			}
			else { nonfakes++; }
		}
		G.print("fakes "+fakes+" nonfakes "+nonfakes);
	}	

}