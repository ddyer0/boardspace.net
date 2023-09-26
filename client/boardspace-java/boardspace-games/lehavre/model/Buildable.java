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
package lehavre.model;

import lehavre.util.GoodsList;

/**
 *
 *	The <code>Buildable</code> interface has to be implemented by
 *	any game classes that represend buildable objects in-game.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/12
 */
public interface Buildable
extends java.io.Serializable
{
	/**
	 *	Returns the building costs.
	 *	@return the building costs
	 */
	public GoodsList getCosts();
}