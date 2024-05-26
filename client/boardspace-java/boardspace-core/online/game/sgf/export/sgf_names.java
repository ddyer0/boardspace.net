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
package online.game.sgf.export;

public interface sgf_names
{
    // public property names, common to all SGF.  In a more modern cast these would be
	// made into an enum, but changing them is not worth the effort because these known
	// names are embedded in a lot of places.
	
    public static final String fileformat_property = "FF"; //format of the file
    public static final String sgf_file_format = "4"; //we comply with format 4
    public static final String game_property = "GM"; //integer indicating the game type
    public static final String gamename_property = "GN"; //the name of this particular game
    public static final String gamespeed_property = "GS";	// gamespeed
    public static final String gametitle_property = "GC"; //a comment describing the game as a whole
    public static final String size_property = "SZ"; //the size of the board
    public static final String tomove_property = "PL"; //the player to play next
    public static final String addblack_property = "AB"; //"Add black"
    public static final String addwhite_property = "AW"; //"Add white"
    public static final String addempty_property = "AE"; //"Add empty"
    public static final String date_property = "DT"; //"the date	public static final String black_property = "B";				//a black move
    public static final String white_property = "W"; //a white move
    public static final String comment_property = "C"; //a comment
    public static final String nodename_property = "N"; //node name
    public static final String black_property = "B"; //black move
    public static final String setup_property = "SU"; //setup for the game
    public static final String version_property = "VV"; 	// internal sgf file version
    public static final String result_property = "RE";	// game result
    public static final String colormap_property = "CM";	// game colormap
    public static final String time_property = "TM";	// player elapsed time
    public static final String timecontrol_property = "TC";	// time control parameters
    public static final String annotation_property = "AN";	// annotations
    /** where to add a new node
     * <li>asOnly</li>
     * <li>atEnd</li>
     * <li>atBeginning</li> 
     */
    enum Where {
    	/**
    	 * make this the only node, disard current successors 
    	 */
    	asOnly,	
    	/** append to the list of succeessors */
    	atEnd,	
    	/** insert at the beginning of the list of successors */
    	atBeginning	
    }
}
