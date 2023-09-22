/* copyright notice */package lehavre.util;

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