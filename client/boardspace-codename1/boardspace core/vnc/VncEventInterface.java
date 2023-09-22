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
package vnc;

public interface VncEventInterface
{
	public void keyStroke(int keycode);					// send a key down to the screen
	public void keyRelease(int keycode);				// send a key up to the screen
	public void mouseMove(int x, int y);				// send the mouse position to the screen
	public void mouseDrag(int x, int y,int buttons);				// send the mouse position to the screen
	public void mousePress(int x,int y,int buttons);				// send mouse buttons down
	public void mouseRelease(int x,int y,int buttons);				// send mouse buttons up
	public void mouseStroke(int x,int y,int buttons) ;				// send mouse click
	public void setTransmitter(VNCTransmitter vncTransmitter);
	public void stopService(String reason);
	public void notifyActive();
	public void notifyFinished();
}