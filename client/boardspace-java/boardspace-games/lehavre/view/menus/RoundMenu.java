/* copyright notice */package lehavre.view.menus;

import java.awt.event.*;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.util.*;

/**
 *
 *	The <code>RoundMenu</code> class is the context menu of a round card.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/26
 */
public final class RoundMenu
extends JPopupMenu
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>RoundMenu</code> instance for the given round card.
	 *	@param control the control object
	 *	@param round the round card
	 */
	public RoundMenu(final LeHavre control, final Round round) {
		JMenuItem item = new JMenuItem(control.getDictionary().get("menuInfo"));
		item.addActionListener(
			new ActionListener() {
				private Dictionary dict = control.getDictionary();
				public void actionPerformed(ActionEvent e) {
					control.showInformation(Util.getToolTipText(dict, round), dict.get("popupRound"));
				}
			}
		);
		add(item);
	}
}