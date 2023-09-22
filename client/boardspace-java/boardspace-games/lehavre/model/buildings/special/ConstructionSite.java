/* copyright notice */package lehavre.model.buildings.special;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.Util;
import lehavre.view.*;
import lehavre.view.labels.*;

/**
 *
 *	The <code>ConstructionSite</code> class represents the Construction Site (037).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class ConstructionSite
extends Building
{
	static final long serialVersionUID =1L;
	/** The amount of hand cards per player. */
	public static final int HANDCARDS = 3;

	/** The instance. */
	private static ConstructionSite instance = null;

	/** Creates a new <code>ConstructionSite</code> instance. */
	private ConstructionSite() {
		super(Buildings.$037);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static ConstructionSite getInstance() {
		if(instance == null) instance = new ConstructionSite();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		showHandCards(control, true);
		return false;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return (control.getGameState().getActivePlayer().getHandCards() != null);
	}

	/**
	 *	Creates the dialog to view the hand cards.
	 *	If set, asks the player to choose one.
	 *	@param control the control object
	 *	@param choose provide true to choose one
	 */
	@SuppressWarnings("serial")
	public void showHandCards(final LeHavre control, final boolean choose) {
		Player player = control.getCurrentPlayer();
		ArrayList<Buildings> cards = player.getHandCards();
		final Dictionary dict = control.getDictionary();
		final JDialog dialog = GoodsDialog.createDialog(control, Util.getTitle(dict, "cards", player));
		dialog.setModal(choose);
		GUIHelper gui = new GUIHelper("dialog");
		JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER, gui.getInt("EntryHGap"), gui.getInt("EntryVGap")));
		inner.setOpaque(false);
		BuildingLabel label;
		String language = dict.getLanguage();
		for(int i = 0; i < cards.size(); i++) {
			final Building building = Building.create(cards.get(i));
			label = new BuildingLabel(control.network,language, null, building);
			label.setComponentPopupMenu(
				new JPopupMenu() {{
					JMenuItem item = new JMenuItem(dict.get("menuInfo"));
					item.addActionListener(
						new ActionListener() {
							private final String text, title;
							{
								text = Util.getToolTipText(dict, building.getProto());
								title = dict.get("popupBuilding");
							}
							public void actionPerformed(ActionEvent e) {
								control.showInformation(text, title);
							}
						}
					);
					add(item);
				}}
			);
			if(choose) label.addMouseListener(
				new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						if(e.getClickCount() > 1) {
							control.choose(building);
							dialog.setVisible(false);
							dialog.dispose();
						}
					}
				}
			);
			inner.add(label);
		}
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(choose) control.showError("MustChoose");
				else {
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		};
		String descr = String.format(dict.get("cardsDescr"), dict.get(choose ? "cardsChoose" : "cardsInfo"));
		Dimension size = gui.getSize("Descr");
		GoodsDialog.showDialog(dialog, DialogWindow.createContents(dict, descr, size, inner, action));
	}
}