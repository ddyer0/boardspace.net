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
package lehavre.view.labels;

import lehavre.main.NetworkInterface;
import lehavre.model.*;
import lehavre.view.*;
import lehavre.view.menus.*;

/**
 *
 *	The <code>GoodLabel</code> class is a specialized label
 *	to display goods chits in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/26
 */
public final class GoodLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>GoodLabel</code> instance for the given goods chit.
	 *	@param language the language version
	 *	@param menu the goods chit menu
	 *	@param good the goods chit
	 */
	public GoodLabel(NetworkInterface network,String language, GoodMenu menu, Good good) {
		super(network,language, String.format(MainWindow.GOODS_PATH, good));
		setComponentPopupMenu(menu);
	}
}