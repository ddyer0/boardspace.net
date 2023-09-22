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
/**
 * this interface is used with the implementation of quiksort. It's nonstandard
 * in that it allows two different sort predicates to be defined, and also that
 * it's intended to be used with homogeneous arrays of the same object.
 * @author ddyer
 *
 */
public interface CompareTo<T> {
	/**
	 * the default comparison predicate
	 * @param o
	 * @return 1 0 -1 depending on the relationship
	 */
	public int compareTo(T o);
	/**
	 * the alternate comparison predicate
	 * @param o
	 * @return 1 0 -1 depending on the relationship
	 */
	public int altCompareTo(T o);
	
}
