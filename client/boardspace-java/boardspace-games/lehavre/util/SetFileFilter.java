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
package lehavre.util;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 *	The <code>SerFileFilter</code> class is used by a <code>JFileChooser</code> object
 *	to filter for serialized game settings objects (extension ".set").
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/3/8
 */
public final class SetFileFilter
extends FileFilter
{
	/** The accepted file extension. */
	public static final String ACCEPTED_EXTENSION = ".set";

	/**
	 *	Returns true if the given file is accepted.
	 *	@param file the file
	 *	@return true if the given file is accepted
	 */
	public boolean accept(File file) {
		if(file.isDirectory()) return true;
		return file.getName().toLowerCase().endsWith(ACCEPTED_EXTENSION);
	}

	/**
	 *	Returns the description of this filter.
	 *	@return the description
	 */
	public String getDescription() {
		return ACCEPTED_EXTENSION;
	}
}