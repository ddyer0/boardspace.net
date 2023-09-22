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
package lib;
/**
 * this class is the generic interface for an object you can print to.
 * launch a console window and to print things into it.
 * 
 * @author ddyer
 *
 */
public interface ShellProtocol 
{	/** start a shell in a separate window, with a list of name value pairs as
		known symbols in the console.
 	*/
	public void startShell(Object... args);
	/**
	 * print objects into the console window
	 * @param o
	 */
	public void print(Object... o);
	/**
	 * print objects into the console window
	 * @param o
	 */
	public void println(Object... o);
}
