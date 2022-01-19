package lehavre.view.labels;

import lehavre.main.NetworkInterface;
import lehavre.view.*;
import lehavre.view.menus.*;

/**
 *
 *	The <code>LoanLabel</code> class is a specialized label
 *	to display loan cards in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/7
 */
public final class LoanLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/** The path to the loan card image file. */
	private static final String LOAN_PATH = "cards/loan";

	/** The context menu. */
	private LoanMenu menu;

	/**
	 *	Creates a new <code>LoanLabel</code> instance.
	 *	@param language the language version
	 *	@param menu the loan menu
	 */
	public LoanLabel(NetworkInterface net,String language, LoanMenu menu) {
		super(net,language, LOAN_PATH, false);
		this.menu = menu;
		setComponentPopupMenu(menu);
	}

	/**
	 *	Enables or disables the label.
	 *	@param enabled provide true to enable the label
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		menu.setPaybackEnabled(enabled);
	}
}