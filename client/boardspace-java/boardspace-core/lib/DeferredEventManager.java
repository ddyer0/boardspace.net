package lib;

import java.awt.event.*;
import java.util.Vector;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * one of the rules for a well behaved application is that the actions resulting
 * from buttons and menus should be handled by the main process thread rather than
 * the AWT thread.  The best way for this to be adhered to is if the canvases don't
 * receive the events in the first place.   This class implements a listener which
 * maintains a queue of events for the listeners to consume.
 * <p>
 * this class is appropriate to be made an ActionLIstener or ItemListener, it will
 * automatically accept events on behalf of the canvas, and then develiver them to
 * the game event loop on request.
 * 
 * @author ddyer
 *
 */
public class DeferredEventManager implements ActionListener,ItemListener,ListSelectionListener
{
	private Vector<Object> deferredEvents = new Vector<Object>();
	DeferredEventHandler handler = null;
	
	public DeferredEventManager(DeferredEventHandler d)
	{
		handler = d;
	}
	/**
	 * queue an actionevent (normally from an actionPerformed or itemStateChanged method)
	 * for later handling.
	 * @param e
	 */
	public void deferActionEvent(Object e)
	{
		deferredEvents.addElement(e);
		wake();
	}
	private void wake()
	{
		G.wake(handler);
	}
	/**
	 * handle an actionPerformed method by saving it for later.
	 */
    public void actionPerformed(ActionEvent e)
    {   deferActionEvent(e); 
    }
    /**
     * handle an itemStateChanged event by saving it for later.
     */
    public void itemStateChanged(ItemEvent e)
    {  	deferActionEvent(e);
    }

    /**
     * call the deferredEventManager method of the cp object.
     * This will call G.Error if the event is not handled.
     * @param cp some thing that implements #deferredEventHandler
     */
	public void handleDeferredEvent(DeferredEventHandler cp)
	{	handler = cp;
		if(!deferredEvents.isEmpty())
		{ boolean val = false;
		  Object e = deferredEvents.elementAt(0);
		  deferredEvents.removeElementAt(0);
		  if(e instanceof PinchEvent)
		  {
			  val = cp.handleDeferredEvent(e, null);
		  }
		  else if(e instanceof ActionEvent)
		  	{  ActionEvent ee = (ActionEvent)e;
		  	   val = cp.handleDeferredEvent(ee.getSource(), ee.getActionCommand());
		  	}
		  else if(e instanceof ListSelectionEvent)
		  {
			  ListSelectionEvent ee = (ListSelectionEvent)e;
			  val = cp.handleDeferredEvent(ee.getSource(), null);
		  }
		  else if(e instanceof ItemEvent)
		  {	ItemEvent ee = (ItemEvent)e;
		  	val = cp.handleDeferredEvent(ee.getSource(), null);
		  }
		  else if(e instanceof MouseWheelEvent)
		  {
			  val = cp.handleDeferredEvent(e,"mousewheel");
		  }
		  else if(G.debug() && !val)
		  {
			 G.print("Unexpected event deferred: "+e); 
		  }	
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		deferActionEvent(e);	
	}
	
}
