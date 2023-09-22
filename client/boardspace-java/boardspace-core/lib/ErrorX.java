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

import java.io.PrintStream;

/**
 * this is the standard "Error" subclass to use.  It allows you to add additional
 * information to the error object as part of handling the error.  Used to append
 * context information before reporting the problem.
 * 
 * @author ddyer
 *
 */
public class ErrorX extends Error {
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	String extraInfo="";
	public void addExtraInfo(String more) { extraInfo += more + "\n"; }
	public ErrorX(String m) { super(m); }
	public ErrorX(Throwable m) { super(m.toString()); }
	
	public void printStackTrace()
	{
		printStackTrace(System.err);
	}

	public void printStackTrace(PrintStream s)
	{	super.printStackTrace(s);
		if(!"".equals(extraInfo)) { s.println(extraInfo); }
	}
}
