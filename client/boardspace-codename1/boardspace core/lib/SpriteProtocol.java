package lib;

import online.common.exCanvas;
/**
 * The sprite protocol is a rudimentary mechanism for animating sprites across
 * the board, with minimal interaction or effort to the game.  A sample implementation
 * is available as {@link SimpleSprite}.  The general expectation is that sprites have a
 * starting and ending time, and will draw themselves on request any time between the
 * start and finish.
 * <p>use {@link online.common.exCanvas#addSprite addSprite} to add a sprite to the active list
 * @author ddyer
 *
 */
public interface SpriteProtocol 
{	
	/**
	 * draw is called last during drawing of the game redraw, to draw this 
	 * sprite with the specified gc.
	 * 
	 * @param gc the current gc for drawing
	 * @param c the canvas being drawn on
	 * @param now the current time, in milliseconds
	 */
	public void draw(Graphics gc,exCanvas c,long now);
	/**
	 * return true if this sprite has expired and should be deleted.
	 * @param now the current time in milliseconds
	 * @return true if the sprite has finished
	 */
	public boolean isExpired(long now);
	/**
	 * return true if this sprite has started moving
	 * @param now
	 * @return true if started
	 */
	public boolean isStarted(long now);
	/**
	 * request this sprite to inactivate itself and return any resources.
	 */
	public void cancel();
	/**
	 * return true if this animation is supposed to overlap cell contents.  This is 
	 * used to inhibit the normal behavior of turning off display of the destination
	 * cell until the animation finishes
	 * @return true if the destination should remain visible rather than being invisible until the animation finishes
 */
	public boolean isOverlapped();
	/** Used when chaining
	 * several steps to the endpoint, where the endpoint is already placed.
	 * @return true if this animation is always active.  
	 */
	public boolean isAlwaysActive();
	/**
	 * return the height of this animation element.  This is used to reduce the
	 * height of the destination stack when animating a move.  In most cases this
	 * will be "1", but when moving a stack as a single element, it's the height
	 * of the stack.
	 */
	public int animationHeight();

}
