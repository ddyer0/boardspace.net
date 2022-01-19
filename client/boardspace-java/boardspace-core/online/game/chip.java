package online.game;
import java.util.*;

import lib.Digestable;
import lib.DrawableImage;
import lib.DrawableImageStack;
import lib.G;
import lib.Random;

/**
 * this is the base class for "chips" which are objects typically
 * placed on boards.  It also can be used for random other graphics
 * associated with a game, including the board itself.  
 * Chips inherit from {@link DrawableImage} which supplies the graphic functionality.
 * <p>
 * chips are typically immutable objects that can be shared among boards without copying.
 * and they are also uniquely identified and digestable.  Chips come in 
 * sets (of all the types of objects used in a game) and each chip type
 * should be unique.  Chips are typically loaded once and stored in
 * a static variable, so all chips in use will be the same object in
 * all games, and in all boards (robots use a separate board that's a
 * copy of the actual game)
 * <p>
 * each game typically subclasses chip with it's particular artwork and
 * characteristics.  Bits of artwork that are not game pieces can be loaded
 * as chips too so they can be handled uniformly.
 * @author ddyer
 *
 */
public abstract class chip<T extends chip<T>> extends DrawableImage<T> implements Digestable
{	
	public T alternates[];
	/**
	 * this is the opportunity to substitute a decorative different chip on the way to being drawn,
	 * based on the cell where it resides.  This is (for example) used to present one of several
	 * alternate chip images for go stones.
	 * @param cell
	 * @return
	 */
 	public T getAltDisplayChip(cell<?>c)
    {
 		return(getAltDisplayChip((int)c.randomv));
    }
	/**
	 * this is the opportunity to substitute a decorative different chip on the way to being drawn,
	 * based on the cell where it resides.  This is (for example) used to present one of several
	 * alternate chip images for go stones.
	 * @param rv
	 * @return
	 */
    @SuppressWarnings("unchecked")
	public T getAltDisplayChip(int rv)
    {
    	if(alternates!=null) { return(alternates[Math.abs(rv%alternates.length)]); }
    	return((T)this);
    }

	/**
	 * return true if this is the same chip. as other
	 * @param c
	 * @return true if the other chip is the same as this one
	 */
	public boolean sameChip(chip<?> c) { return(c==this); }
	/**
	 * this random number is normally assigned when the chip
	 * is created, and is used to identify the chip identity
	 * in digests.  Note that these "random" numbers are a 
	 * not really random, but immutable and consistant values
	 * associated with each chip, forever.
	 */
	public long randomv=0;
	/**
	 * return the digest identity of this chip
	 */
	public long Digest() 
		{ G.Advise(randomv!=0,"randomv is zero");
		  return(randomv); 
		}
	/** return the identity of this chip in context R
	 * use diffent random R so the same chip, encountered
	 * in different places, won't generate the same digest.
	 */
	public long Digest(Random r) { return(r.nextLong()*Digest()); }
	public static long Digest(chip<?> c) { return((c==null)?0:c.Digest()); }
	public static long Digest(Random r,chip<?> c) { return(r.nextLong()*Digest(c)); }
	
	// default which may be overridden
	@SuppressWarnings("deprecation")
	public String toString() { return("<"+getClass().getName()+" "+contentsString()+">"); }
	/**
	 * this verifies that the digests of all the canonical chips are distinct.
	 * @param objs
	 */
	static public void check_digests(Digestable[] objs)
	{	Hashtable<String,Digestable> h = new Hashtable<String,Digestable>();
		for(int i=0;i<objs.length;i++) 
		{ Digestable thisc = objs[i];
		  String digs = ""+thisc.Digest();
		  Digestable oldc = h.get(digs);
		  G.Assert(oldc==null,"%s has the same digest as %s",oldc,thisc);
		  h.put(digs,thisc);
	}}
	/**
	 * this verifies that the digests of all the canonical chips are distinct.
	 * @param objs
	 */
	static public void check_digests(DrawableImageStack stack)
	{	Digestable v[] = new Digestable[stack.size()];
		for(int lim=stack.size()-1; lim>=0; lim--)
			{ v[lim]=(Digestable)stack.elementAt(lim);
			}
		check_digests(v);
	}

	
}