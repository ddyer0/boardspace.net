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
import lehavre.main.*;

/**
 *
 *	The <code>GameWindow</code> class is the super-class of all windows
 *	in the game. It provides useful fields and methods to help with the
 *	appearance and game flow.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/17
 */
public abstract class GameWindow
extends ControlledWindow
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>GameWindow</code> instance.
	 *	@param control the control object
	 *	@param type the prefix for the GUI helper
	 */
	public GameWindow(LeHavre control, String type) {
		super(control, type);
	}

	/**
	 *	Displays the instructions page.
	 *	@param name the name of the page
	 *	@param title the window title
	 */
	protected void showInstructions(String name, String title) {
		JFrame win = new InstructionsWindow(get(name));
		win.setTitle(title);
		win.setVisible(true);
	}

	/**
	 *	Displays a warning before closing the window.
	 *	The user can still abort the action.
	 */
	public void quit() {
		if(confirm("Quit")) control.quit();
	}
}