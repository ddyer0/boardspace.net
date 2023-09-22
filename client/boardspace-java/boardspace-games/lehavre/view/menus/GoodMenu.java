/* copyright notice */package lehavre.view.menus;

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