package lehavre.view.labels;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

import lehavre.main.NetworkInterface;
import lehavre.model.*;
import lehavre.view.*;
import lehavre.view.menus.*;

/**
*
 *	The <code>ShipLabel</code> class is a specialized label
 *	to display ships in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/26
 */
public final class ShipLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/** The path to the ships image directory. */
	private static final String SHIPS_PATH = "cards/ships/%s";

	/** The represented ship. */
	private Ship ship;

	/** The context menu. */
	private ShipMenu menu;

	/** The basic image. */
	private Image basicImage;

	/** The mouse listener. */
	private MouseListener buildListener = null;

	/**
	 *	Creates a new <code>ShipLabel</code> instance for the given ship.
	 *	@param language the language version
	 *	@param menu the ship menu
	 *	@param ship the ship
	 */
	public ShipLabel(NetworkInterface net,String language, ShipMenu menu, Ship ship) {
		super(net,language, String.format(SHIPS_PATH, ship));
		this.ship = ship;
		this.menu = menu;
		basicImage = ((ImageIcon)getIcon()).getImage();
		buildListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() > 1) ShipLabel.this.menu.getBuildOption().doClick();
			}
		};
		setComponentPopupMenu(menu);
	}

	/**
	 *	Enables or disables the building option.
	 *	@param enabled provide true to enable the building option
	 */
	public void setBuildingEnabled(NetworkInterface network,boolean enabled) {
		boolean wasEnabled = menu.isBuildingEnabled();
		if(enabled && !wasEnabled) {
			mark(network);
			addMouseListener(buildListener);
		} else if(!enabled && wasEnabled) {
			setIcon(new ImageIcon(basicImage));
			removeMouseListener(buildListener);
		}
		menu.setBuildingEnabled(enabled);
	}

	/**
	 *	Enables or disables the purchase option.
	 *	@param enabled provide true to enable the purchase option
	 */
	public void setPurchaseEnabled(boolean enabled) {
		if(!ship.isBuyable()) return;
		menu.setPurchaseEnabled(enabled);
	}

	/**
	 *	Enables or disables the sale option.
	 *	@param enabled provide true to enable the sale option
	 */
	public void setSaleEnabled(boolean enabled) {
		menu.setSaleEnabled(enabled);
	}

	/**
	 *	Draws a building symbol onto the building.
	 */
	private void mark(NetworkInterface network) {
		int width = getWidth(), height = getHeight();
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.drawImage(basicImage, 0, 0, this);
		ImageIcon icon = (ImageIcon)new ImageLabel(network,null, String.format(MainWindow.SYMBOL_PATH, "build"), false).getIcon();
		g.drawImage(icon.getImage(), (width - icon.getIconWidth()) / 2, (height - icon.getIconHeight()) / 2, this);
		setIcon(new ImageIcon(img));
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				invalidate();
				repaint();
			}
		});
	}
}