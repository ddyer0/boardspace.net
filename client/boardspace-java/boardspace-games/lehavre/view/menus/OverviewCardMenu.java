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