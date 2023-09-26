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
package lehavre.model.goods;

/**
 *	The <code>Good</code> class is a super class for dummy goods objects.
 *	Their only purpose is to have representatives of the goods on class
 *	level. In future versions of the game this class may be enahnced and,
 *	at some point, replace the original <code>model.Good</code> enum.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/28
 */
public abstract class Good
{
	/**
	 * Returns true if the given object is equal to this one.
	 * @return true if the given object is equal to this one
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj != null && getClass().equals(obj.getClass()));
	}

	/**
	 * Computes a hash code for this object.
	 * @return the computed hash code
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Returns the string representation of this resource.
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}