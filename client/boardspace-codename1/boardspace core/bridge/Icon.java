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

import lib.Graphics;



public interface Icon
{

    /**
     * Draw the icon at the specified location.  Icon implementations
     * may use the Component argument to get properties useful for
     * painting, e.g. the foreground or background color.
     */
    void paintIcon(Component c, Graphics g, int x, int y);

    /**
     * Returns the icon's width.
     *
     * @return an int specifying the fixed width of the icon.
     */
    int getIconWidth();

    /**
     * Returns the icon's height.
     *
     * @return an int specifying the fixed height of the icon.
     */
    int getIconHeight();
 
}