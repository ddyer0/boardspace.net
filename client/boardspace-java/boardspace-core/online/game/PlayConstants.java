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
package online.game;

import lib.CellId;
import lib.HitPoint;
import lib.UniversalConstants;

/**
 * a collections of constants for use by game viewers.  Usually the games "constants" class
 * will implements one of the two dependent classes, 
 * @see online.game.Play2Constants
 * @see online.game.Play6Constants
 * @author ddyer
 *
 */
public interface PlayConstants extends UniversalConstants {
    static final String KEYWORD_START_STORY = "story";		// new "story" format
    static final String KEYWORD_TRACKMOUSE = "trackMouse";
    static final String KEYWORD_SCROLL = "scroll";
    static final String KEYWORD_END_HISTORY = ".end.";
	static final String IdNotFoundError = "Id %s not found";	// not a translated string
	
 

	/**
	 * HitNoWhere is used as the code for "nothing hit yet".  Other values are used by various 
	 * stock widgets and buttons, such as the VCR
	 * controls and the "done" button.  Use the interface {@link CellId} to refer to any id
	 * @see HitPoint
	 */
	
	
	static final int GET_CURRENT_POSITION = -2;




}
