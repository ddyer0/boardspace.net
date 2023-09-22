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

/**
 * An abstract adapter class for receiving window events.
 * The methods in this class are empty. This class exists as
 * convenience for creating listener objects.
 * <P>
 * Extend this class to create a <code>WindowEvent</code> listener
 * and override the methods for the events of interest. (If you implement the
 * <code>WindowListener</code> interface, you have to define all of
 * the methods in it. This abstract class defines null methods for them
 * all, so you can only have to define methods for events you care about.)
 * <P>
 * Create a listener object using the extended class and then register it with
 * a Window using the window's <code>addWindowListener</code>
 * method. When the window's status changes by virtue of being opened,
 * closed, activated or deactivated, iconified or deiconified,
 * the relevant method in the listener
 * object is invoked, and the <code>WindowEvent</code> is passed to it.
 *
 * @see WindowEvent
 * @see WindowListener
 * @see <a href="http://docs.oracle.com/javase/tutorial/uiswing/events/windowlistener.html">Tutorial: Writing a Window Listener</a>
 *
 * @author Carl Quinn
 * @author Amy Fowler
 * @author David Mendenhall
 * @since 1.1
 */
public abstract class WindowAdapter
    implements WindowListener, WindowStateListener, WindowFocusListener
{
    /**
     * Invoked when a window has been opened.
     */
    public void windowOpened(WindowEvent e) {}

    /**
     * Invoked when a window is in the process of being closed.
     * The close operation can be overridden at this point.
     */
    public void windowClosing(WindowEvent e) {}

    /**
     * Invoked when a window has been closed.
     */
    public void windowClosed(WindowEvent e) {}

    /**
     * Invoked when a window is iconified.
     */
    public void windowIconified(WindowEvent e) {}

    /**
     * Invoked when a window is de-iconified.
     */
    public void windowDeiconified(WindowEvent e) {}

    /**
     * Invoked when a window is activated.
     */
    public void windowActivated(WindowEvent e) {}

    /**
     * Invoked when a window is de-activated.
     */
    public void windowDeactivated(WindowEvent e) {}

    /**
     * Invoked when a window state is changed.
     * @since 1.4
     */
    public void windowStateChanged(WindowEvent e) {}

    /**
     * Invoked when the Window is set to be the focused Window, which means
     * that the Window, or one of its subcomponents, will receive keyboard
     * events.
     *
     * @since 1.4
     */
    public void windowGainedFocus(WindowEvent e) {}

    /**
     * Invoked when the Window is no longer the focused Window, which means
     * that keyboard events will no longer be delivered to the Window or any of
     * its subcomponents.
     *
     * @since 1.4
     */
    public void windowLostFocus(WindowEvent e) {}
}