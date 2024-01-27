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
package lib;
/**
 * standard java layouts are pretty much a disaster IMO, so consequently 
 * boardspace doesn't use them for much of anything.  Instead, windows
 * typically implement NullLayoutProtocol and use new NullLayout(this)
 * as their layout manager.
 * 
 * Boardspace also typically uses a single "canvas" window for each major frame,
 * and the canvas lays itself up using a setLocalBounds.
 * Frames that contain a single window typically make that subwindow full sized.
 * Frames that contain multiple real windows not typically used. 
 */
public interface NullLayoutProtocol {
	public void doNullLayout();
}
