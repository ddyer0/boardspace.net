package lib;

/**
 * this interface defines the visitor interface between a canvas and it's #deferredEventManager
 * @author ddyer
 *
 */
public interface DeferredEventHandler extends SimpleObserver 
{	/**
	handle an event from a deferredEventManager.
	@return true if the event was handled
	*/
	public boolean handleDeferredEvent(Object e, String command);
	public void wake();
}
