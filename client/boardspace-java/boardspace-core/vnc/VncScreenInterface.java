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

import lib.Image;
import java.awt.Rectangle;

/**
 * interface for a window that will be available to be mirrored by vnc
 * 
 * @author ddyer
 *
 */
public interface VncScreenInterface 
{
	public boolean needsRecapture();					// true if the screen is known to have changed
	public Image captureScreen();						// get the backing bitmap from the screen, NOT a stable copy
	public void captureScreen(Image im,int timeout);	// capture a copy of the screen next time it changes
	public Rectangle getScreenBound();					// get the dimensions of the screen
	public void setTransmitter(VNCTransmitter vncTransmitter);
	public VNCTransmitter getTransmitter();
	public void stopService(String reason);
	public boolean isActive();
}