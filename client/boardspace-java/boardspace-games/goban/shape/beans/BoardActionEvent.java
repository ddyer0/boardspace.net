
package goban.shape.beans;
import java.util.*;

import goban.shape.shape.LocationProvider;

/** this event is fired when the user clicks on a square of the board.
 The event.object will be the board, and the event.Mouse_At will be the
 board coordinates of the click */
public class BoardActionEvent extends EventObject
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public LocationProvider Mouse_At;

	BoardActionEvent(Object source,int code,String s,LocationProvider p,int x,int y)
	{super(source);
	 Mouse_At=p;
}}