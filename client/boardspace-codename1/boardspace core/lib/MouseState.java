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
 * mouse state encodes the state of the mouse up, down, dragging, pinching and so on
 * @author Ddyer
 *
 */
public enum MouseState {
/** last mouse action was enter */
LAST_IS_ENTER,
/** last mouse action was mouse exit */
LAST_IS_EXIT,
/** last mouse action was mouse down */
LAST_IS_DOWN, 
/** last mouse action was mouse drag */

LAST_IS_DRAG, 
/** last mouse action was mouse up */

LAST_IS_UP, 
/** last mouse action was mouse move */

LAST_IS_MOVE,

/** last mouse action is a pinch gesture */
LAST_IS_PINCH,
/** mouse wheel action */
LAST_IS_WHEEL,

/** last is idle, nothing happening! */
LAST_IS_IDLE
};
