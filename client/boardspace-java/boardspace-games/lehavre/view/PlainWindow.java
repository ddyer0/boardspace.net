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
package lehavre.view;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.metal.*;

import lib.FontManager;

/**
 *
 *	The <code>PlainWindow</code> class is an abstract class to be
 *	extended by other window classes of the game. It provides a
 *	GUI helper object to read the GUI settings.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/17
 */
public abstract class PlainWindow
extends JFrame
{
	static final long serialVersionUID =1L;

	/** The special Le Havre fonts. */
	public static final Font boldFont, cardFont;

	/** Some specific bold fonts. */
	public static final Font FONT14, FONT15, FONT16, FONT20, SFONT18, SFONT24;

	static {
		/* Change the look and feel. */
		try {
			UIManager.setLookAndFeel(new MetalLookAndFeel());
		} catch(Exception e) {
			e.printStackTrace();
		}

		/* Load the 1st special font. */
		Font font = GUIHelper.getBoldFont();

		boldFont = (font != null ? font : FontManager.getFont("SansSerif", FontManager.Style.Plain, 1));
		GUIHelper gui = new GUIHelper("font");
		FONT14 = boldFont.deriveFont((float)gui.getInt("SmallButtons"));
		FONT15 = boldFont.deriveFont((float)gui.getInt("GoodLabels"));
		FONT16 = boldFont.deriveFont((float)gui.getInt("LargeButtons"));
		FONT20 = boldFont.deriveFont((float)gui.getInt("SymbolLabels"));

		/* Load the 2nd special font. */
		font = GUIHelper.getPlainFont();
		cardFont = (font != null ? font : FontManager.getFont("SansSerif", FontManager.Style.Plain, 1));
		SFONT18 = cardFont.deriveFont((float)gui.getInt("SettingLabels"));
		SFONT24 = cardFont.deriveFont((float)gui.getInt("LobbyLabels"));

	}

	/** The GUI helper object. */
	protected final GUIHelper gui;

	/**
	 *	Creates a new <code>PlainWindow</code> instance.
	 *	@param type the prefix for the GUI helper
	 */
	public PlainWindow(String type) {
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					quit();
				}
			}
		);
		gui = (type != null ? new GUIHelper(type) : null);
	}

	/** Implement what to do on closing. */
	public abstract void quit();
}