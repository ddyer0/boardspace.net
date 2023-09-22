/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package bridge;


import java.util.EventListener;

/**
 * The listener interface for receiving "interesting" mouse events
 * (press, release, click, enter, and exit) on a component.
 * (To track mouse moves and mouse drags, use the
 * <code>MouseMotionListener</code>.)
 * <P>
 * The class that is interested in processing a mouse event
 * either implements this interface (and all the methods it
 * contains) or extends the abstract <code>MouseAdapter</code> class
 * (overriding only the methods of interest).
 * <P>
 * The listener object created from that class is then registered with a
 * component using the component's <code>addMouseListener</code>
 * method. A mouse event is generated when the mouse is pressed, released
 * clicked (pressed and released). A mouse event is also generated when
 * the mouse cursor enters or leaves a component. When a mouse event
 * occurs, the relevant method in the listener object is invoked, and
 * the <code>MouseEvent</code> is passed to it.
 *
 * @author Carl Quinn
 *
 * @see MouseAdapter
 * @see MouseEvent
 * @see <a href="http://docs.oracle.com/javase/tutorial/uiswing/events/mouselistener.html">Tutorial: Writing a Mouse Listener</a>
 *
 * @since 1.1
 */
public interface MouseListener extends EventListener {

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    public void mouseClicked(MouseEvent e);

    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent e);

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e);

    /**
     * Invoked when the mouse enters a component.
     */
    public void mouseEntered(MouseEvent e);

    /**
     * Invoked when the mouse exits a component.
     */
    public void mouseExited(MouseEvent e);
 
}
