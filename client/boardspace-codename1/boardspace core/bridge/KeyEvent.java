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
package bridge;

public class KeyEvent extends Event
{	public static int CTRL_DOWN_MASK = 1024;
	private int modifiers =0;
	private char theChar = 0;
	int theCode = 0;
	public KeyEvent(int code)
	{
		theCode = code;
		theChar = (char)code;
		if(code<0x20)
		{
			modifiers = CTRL_DOWN_MASK;
		}
	}
	public int getKeyCode()
	{
		return(theCode);
	}
	public int getModifiersEx()
	{
		return(modifiers);
	}
	public char getKeyChar()
	{
		return(theChar);
	}
	public int getExtendedKeyCode() {
		return(getKeyCode());
	}
}
