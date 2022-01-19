package lib;

/* below here should be the same for codename1 and standard java */
import online.common.exCanvas;
/**
 * a Drawable object can be anything that implements drawChip, used for the purpose of displaying
 * an animation element or icon. The primary use of this interface is for {@link lib.DrawableImage} and it's subclasses;
 * {@link online.game.chip} {@link online.game.cell} and {@link online.game.stackCell}.
 * other uses include animation sprites, and {@link lib.MultiGlyph}, a container for several Drawable objects drawn together.
 * 
 * @author ddyer
 *
 */
public interface Drawable {
	/**
	 * implements the drawing method, which is used to draw this object.
	 *   
	 * @param gc	the graphics object for drawing
	 * @param c		the canvas being drawn on
	 * @param size	the width, in pixels, to draw the object as
	 * @param posx	the x position of the center of the object
	 * @param posy	the y position of the center of the object
	 * @param msg	text to superimpose after drawing the object.
	 */
	public void drawChip(Graphics gc,exCanvas c,int size, int posx,int posy,String msg);
	/**
	 * rotate x,y around the current center px, py and remember it.  This is used
	 * to set current_center_x and current_center_y for animations.
	 * @param displayRotation
	 * @param x
	 * @param y
	 * @param px
	 * @param py
	 */
	public void rotateCurrentCenter(double displayRotation,int x,int y,int px,int py);
	/**
	 * the rotation to use during active animations, which normally is arranged
	 * to default to the orientation of the destination
	 * @return
	 */
	public double activeAnimationRotation();
	/**
	 * This is a specialization used in animations; when a piece is being animated between two locations,
	 * it is actually already sitting at the destination.  This is used by the code displaying the destination
	 * to make the destination disappear until the animation is finished.
	 * @return the height of the stack for the purpose of reducing the height of the destination target.
	 */
	public int animationHeight();
	public String getName();

}
