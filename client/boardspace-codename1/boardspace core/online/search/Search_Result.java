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
package online.search;

import lib.NamedObject;

/**
 * this is a class of singletons used to describe the internal state of a search.
 * @author ddyer
 *
 */
public class Search_Result extends NamedObject
{
    public static final Search_Result Active = new Search_Result("Active");
    public static final Search_Result Done = new Search_Result("Done");
    public static final Search_Result Level_Done = new Search_Result(
            "Level Done");

    Search_Result(String nam)
    {
        super(nam);
    }
}
