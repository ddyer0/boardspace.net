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
 *	The <code>OverviewCardMenu</code> class is the context menu of
 *	the overview card label.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/26
 */
public final class OverviewCardMenu
extends JPopupMenu
{
	static final long serialVersionUID =1L;
	/** The dynamic menu item. */
	//private JMenuItem payback;

	/**
	 *	Creates a new <code>OverviewCardMenu</code> instance.
	 *	@param control the control object
	 *	@param card the overview card object
	 */
	public OverviewCardMenu(final LeHavre control, final OverviewCard card) {
		JMenuItem item = new JMenuItem(control.getDictionary().get("menuInfo"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Dictionary dict = control.getDictionary();
				control.showInformation(Util.getToolTipText(dict, card), dict.get("popupOverview"));
			}
		});
        add(item);
	}
}