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
package loa;

import lib.NamedObject;


final public class Cache_State extends NamedObject
{
    static final Cache_State Invalid = new Cache_State("Invalid");
    static final Cache_State Valid = new Cache_State("Valid");
    static final Cache_State First_Group_Valid = new Cache_State("First Valid");
    static final Cache_State[] all_cache_states = 
        {
            Invalid, Valid, First_Group_Valid
        };

    private Cache_State(String nam)
    {
        super(nam);
    }
}
