package lehavre.view.labels;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import java.awt.Image;
import lehavre.main.NetworkInterface;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.view.*;
import lehavre.view.menus.*;
import lib.SimpleObservable;
import lib.SimpleObserver;

/**
 *
 *	The <code>BuildingLabel</code> class is a specialized label
 *	to display buildings in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class BuildingLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/** The path to the buildings image directory. */
	private static final String BUILDINGS_PATH = "cards/buildings/%s";

	/** The already used worker images. */
	private static final Hashtable<PlayerColor, Image> images = new Hashtable<PlayerColor, Image>();

	/** The location and offset of the worker piece. */
	private static final Point WORKER = new GUIHelper("main").getOffset("Worker");
	private static final int WORKER_OFFSET = new GUIHelper("main").getInt("WorkerShift");

	/** The various images. */
	private Image basicImage;
	private Image basicImageCopy;
	private Image fullImageCopy;

	/** The context menu. */
	private BuildingMenu menu;

	/** The mouse listeners. */
	private final MouseListener entryListener;
	private final MouseListener buildListener;

	/** The language version. */
	private String language;
	NetworkInterface network=null;
	/**
	 *	Creates a new <code>BuildingLabel</code> instance for the given building.
	 *	@param language the language version
	 *	@param menu the building menu
	 *	@param building the building
	 */
	public BuildingLabel(NetworkInterface net,String language, BuildingMenu menu, Building building) {
		super(net,language, String.format(BUILDINGS_PATH, building), menu != null);
		this.language = language;
		this.menu = menu;
		this.network = net;
		if(menu != null) {
			basicImage = ((ImageIcon)getIcon()).getImage();
			basicImageCopy = basicImage;
			fullImageCopy = fullImage;
			building.addObserver(
				new SimpleObserver() {
					public void update(SimpleObservable obs, Object eventType, Object obj) {
						if(obj != null && obj instanceof Integer) {
							switch((Integer)obj) {
								case Building.OCCUPIED: occupy(network); break;
								case Building.MODERNISED: updateView(network); break;
							default:
								break;
							}
						}
					}
				}
			);
			setComponentPopupMenu(menu);
			entryListener = new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if(e.getClickCount() > 1) {
						BuildingLabel.this.menu.getEntryOption().doClick();
					}
				}
			};
			buildListener = new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if(e.getClickCount() > 1) BuildingLabel.this.menu.getBuildOption().doClick();
				}
			};
		} else {
			entryListener = null;
			buildListener = null;
		}
	}

	/**
	 *	Returns the building.
	 *	@return the building
	 */
	public Building getBuilding() {
		return (menu != null ? menu.getBuilding() : null);
	}

	/**
	 *	Enables or disables the entry option.
	 *	@param enabled provide true to enable the entry option
	 */
	public void setEntryEnabled(boolean enabled) {
		if(menu != null) {
			menu.setEntryEnabled(enabled);
			removeMouseListener(entryListener);
			if(enabled) addMouseListener(entryListener);
		}
	}

	/**
	 *	Enables or disables the ban option.
	 *	@param enabled provide true to enable the ban option
	 */
	public void setBanEnabled(boolean enabled) {
		if(menu != null) menu.setBanEnabled(enabled);
	}

	/**
	 *	Enables or disables the building option.
	 *	@param enabled provide true to enable the building option
	 */
	public void setBuildingEnabled(NetworkInterface net,boolean enabled) {
		if(menu != null) {
			if(!getBuilding().isBuildable()) return;
			boolean wasEnabled = menu.isBuildingEnabled();
			if(enabled) {
				if(!wasEnabled) {
					mark(net);
					addMouseListener(buildListener);
				}
			} else {
				removeMouseListener(buildListener);
				if(wasEnabled) setIcon(new ImageIcon(basicImage));
			}
			menu.setBuildingEnabled(enabled);
		}
	}

	/**
	 *	Enables or disables the purchase option.
	 *	@param enabled provide true to enable the purchase option
	 */
	public void setPurchaseEnabled(boolean enabled) {
		if(menu != null) {
			if(!getBuilding().isBuyable()) return;
			menu.setPurchaseEnabled(enabled);
		}
	}

	/**
	 *	Enables or disables the sale option.
	 *	@param enabled provide true to enable the sale option
	 */
	public void setSaleEnabled(boolean enabled) {
		if(menu != null) {
			if(!getBuilding().isSellable()) return;
			menu.setSaleEnabled(enabled);
		}
	}

	/**
	 *	Draws a building symbol onto the building.
	 */
	private void mark(NetworkInterface net) {
		int width = getWidth(), height = getHeight();
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.drawImage(basicImage, 0, 0, this);
		ImageIcon icon = (ImageIcon)new ImageLabel(net,null, String.format(MainWindow.SYMBOL_PATH, "build"), false).getIcon();
		g.drawImage(icon.getImage(), (width - icon.getIconWidth()) / 2, (height - icon.getIconHeight()) / 2, this);
		setIcon(new ImageIcon(img));
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				invalidate();
				repaint();
			}
		});
	}

	/**
	 *	Draws a player piece onto the building.
	 */
	private void occupy(NetworkInterface net) {
		ArrayList<Integer> workers = getBuilding().getWorkers();
		int workerCount = workers.size();
		if(workerCount > 0) {
			// draw background
			BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.drawImage(basicImage, 0, 0, this);
			// get worker discs
			GameState game = menu.getControl().getGameState();
			Player player;
			PlayerColor color;
			Image workerImage;
			ArrayList<Image> workerImages = new ArrayList<Image>();
			for(int i = 0; i < workerCount; i++) {
				player = game.getPlayer(workers.get(i));
				color = player.getColor();
				workerImage = null;
				if(images.containsKey(color)) {
					workerImage = images.get(color);
				} else {
					workerImage = ((ImageIcon)new WorkerLabel(net,player).getIcon()).getImage();
					images.put(color, workerImage);
				}
				workerImages.add(workerImage);
			}
			// draw worker discs
			switch(workerCount) {
				case 1:
					g.drawImage(workerImages.remove(0), WORKER.x, WORKER.y, this);
					break;
				case 2:
					g.drawImage(workerImages.remove(0), WORKER.x - WORKER_OFFSET, WORKER.y, this);
					g.drawImage(workerImages.remove(0), WORKER.x + WORKER_OFFSET, WORKER.y, this);
					break;
				case 3:
					g.drawImage(workerImages.remove(0), WORKER.x - WORKER_OFFSET, WORKER.y + WORKER_OFFSET, this);
					g.drawImage(workerImages.remove(0), WORKER.x + WORKER_OFFSET, WORKER.y + WORKER_OFFSET, this);
					g.drawImage(workerImages.remove(0), WORKER.x, WORKER.y - WORKER_OFFSET, this);
					break;
				case 4:
					g.drawImage(workerImages.remove(0), WORKER.x - WORKER_OFFSET, WORKER.y + WORKER_OFFSET, this);
					g.drawImage(workerImages.remove(0), WORKER.x + WORKER_OFFSET, WORKER.y + WORKER_OFFSET, this);
					g.drawImage(workerImages.remove(0), WORKER.x + WORKER_OFFSET, WORKER.y - WORKER_OFFSET, this);
					g.drawImage(workerImages.remove(0), WORKER.x - WORKER_OFFSET, WORKER.y - WORKER_OFFSET, this);
					break;
				case 5:
					g.drawImage(workerImages.remove(0), WORKER.x - WORKER_OFFSET, WORKER.y + WORKER_OFFSET, this);
					g.drawImage(workerImages.remove(0), WORKER.x + WORKER_OFFSET, WORKER.y + WORKER_OFFSET, this);
					g.drawImage(workerImages.remove(0), WORKER.x + WORKER_OFFSET, WORKER.y - WORKER_OFFSET, this);
					g.drawImage(workerImages.remove(0), WORKER.x - WORKER_OFFSET, WORKER.y - WORKER_OFFSET, this);
					g.drawImage(workerImages.remove(0), WORKER.x, WORKER.y, this);
					break;
				default:
					throw new IllegalArgumentException("Illegal worker count for " + getBuilding() + ": " + workerCount);
			}
			// set new image
			setIcon(new ImageIcon(img));
		} else {
			setIcon(new ImageIcon(basicImage));
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				invalidate();
				repaint();
			}
		});
	}

	/**
	 *	Draws a piece of brick onto the building if needed.
	 *	Then updates the entire view.
	 */
	public void updateView(NetworkInterface network) {
		if(getBuilding().isModernised()) {
			GUIHelper gui = new GUIHelper("brick");
			BufferedImage img = new BufferedImage(fullWidth, fullHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.drawImage(fullImage, 0, 0, this);
			Point p = gui.getOffset("Large");
			String path = String.format(MainWindow.GOODS_PATH, Good.Brick);
			ImageIcon icon = (ImageIcon)new ImageLabel(network,language, path, false).getIcon();
			g.drawImage(icon.getImage(), p.x, p.y, this);
			fullImage = img;
			img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
			g = img.createGraphics();
			g.drawImage(basicImage, 0, 0, this);
			p = gui.getOffset("Small");
			icon = (ImageIcon)new ImageLabel(network,language, path, true).getIcon();
			g.drawImage(icon.getImage(), p.x, p.y, this);
			basicImage = img;
		} else {
			basicImage = basicImageCopy;
			fullImage = fullImageCopy;
		}
		occupy(network);
	}
}