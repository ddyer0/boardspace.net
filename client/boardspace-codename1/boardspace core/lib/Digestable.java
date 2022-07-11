package lib;

/**
 * items which are Digested to summarize the state of the game.
 * 
 * @author Ddyer
 *
 */
public interface Digestable {
	/** the digest in some other context, defined by r 
	 * @param r a random variable
	 */
	public long Digest(Random r);	// digest in context 
}
