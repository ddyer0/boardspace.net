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
package lehavre.view.menus;

import java.awt.event.*;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.util.*;

/**
 *
 *	The <code>GoodMenu</code> class is the context menu of a goods chit label.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class GoodMenu
extends JPopupMenu
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>GoodMenu</code> instance for the given goods chit.
	 *	@param control the control object
	 *	@param good the goods chit
	 */
	public GoodMenu(final LeHavre control, final Good good) {
		JMenuItem item = new JMenuItem(control.getDictionary().get("menuInfo"));
		item.addActionListener(
			new ActionListener() {
				private final String text, title;
				{
					Dictionary dict = control.getDictionary();
					text = Util.getToolTipText(dict, good);
					title = dict.get("popupGood");
				}
				public void actionPerformed(ActionEvent e) {
					control.showInformation(text, title);
				}
			}
		);
		add(item);
	}
}