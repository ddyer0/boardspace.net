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

import lehavre.main.*;

/**
 *
 *	The <code>ControlledWindow</code> class is a slightly enhanced plain window
 *	that is able to communicate with the controller object.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/19
 */
public abstract class ControlledWindow
extends PlainWindow
{
	static final long serialVersionUID =1L;
	/** The control object. */
	protected final LeHavre control;

	/**
	 *	Creates a new <code>ControlledWindow</code> instance.
	 *	@param control the control object
	 *	@param type the prefix for the GUI helper
	 */
	public ControlledWindow(LeHavre control, String type) {
		super(type);
		this.control = control;
	}

	/**
	 *	Displays a confirmation dialog with the given message
	 *	and returns true if the player clicks OK.
	 *	@param message the warning message
	 */
	protected boolean confirm(String message) {
		return control.confirm(message);
	}

	/**
	 *	Calls the get method of the dictionary and retrieves the value
	 *	of the entry with the given key.
	 *	@param key the key
	 */
	protected String get(String key) {
		return control.getDictionary().get(key);
		
	}

	/**
	 *	Calls the write method of the logger and logs the given message.
	 *	@param message the message
	 */
	protected void log(String message) {
		control.log(message);
	}

	/**
	 *	Calls the write method of the logger and logs the given message.
	 *	@param message the message
	 *	@param format provide true to format the message
	 */
	protected void log(String message, boolean format) {
		control.log(message, format);
	}
}