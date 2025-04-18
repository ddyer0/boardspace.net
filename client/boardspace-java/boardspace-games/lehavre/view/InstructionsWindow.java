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

import javax.swing.*;

/**
 *
 *	The <code>InstructionsWindow</code> class is used to display
 *	the instructions how to use the game.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/17
 */
public final class InstructionsWindow
extends PlainWindow
{
	static final long serialVersionUID =1L;
	/** The format of the displayed text. */
	private static final String TEXT_FORMAT;

	static {
		StringBuilder sb = new StringBuilder("<html><style type=\"text/css\">\n");
		sb.append("body {font-family:sans-serif; padding:5px 20px 20px 20px;}\n");
		sb.append("h1, h2, h3 {margin-top:15px; margin-bottom:0px;}\n");
		sb.append("</style>%s</html>");
		TEXT_FORMAT = sb.toString();
	}

	/**
	 *	Creates a new <code>InstructionsWindow</code> instance
	 *	and displays the given instructions text.
	 *	@param text the contents
	 */
	public InstructionsWindow(String text) {
		super("instr");
		JEditorPane area = new JEditorPane();
		area.setEditable(false);
		area.setContentType("text/html");
		area.setText(String.format(TEXT_FORMAT, text));
		area.setBackground(gui.getColor("Window"));
		area.setOpaque(true);
		area.setCaretPosition(0);
		JScrollPane scroll = new JScrollPane(area);
		scroll.getVerticalScrollBar().setUnitIncrement(gui.getInt("ScrollUnit"));
		scroll.setPreferredSize(gui.getSize("Text"));
		getContentPane().add(scroll);
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 *	Closes the window.
	 */
	public void quit() {
		setVisible(false);
		dispose();
	}
}