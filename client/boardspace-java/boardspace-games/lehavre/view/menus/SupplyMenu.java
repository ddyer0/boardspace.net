/* copyright notice */package lehavre.view.menus;

import java.awt.event.*;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.util.*;

/**
 *
 *	The <code>SupplyMenu</code> class is the context menu of a supply chit label.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class SupplyMenu
extends JPopupMenu
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>SupplyMenu</code> instance for the given supply chit.
	 *	@param control the control object
	 *	@param supply the supply chit
	 */
	public SupplyMenu(final LeHavre control, final Supply supply) {
		JMenuItem item = new JMenuItem(control.getDictionary().get("menuInfo"));
		item.addActionListener(
			new ActionListener() {
				private final String text, title;
				{
					Dictionary dict = control.getDictionary();
					text = Util.getToolTipText(dict, supply);
					title = dict.get("popupSupply");
				}
				public void actionPerformed(ActionEvent e) {
					control.showInformation(text, title);
				}
			}
		);
		add(item);
	}
}