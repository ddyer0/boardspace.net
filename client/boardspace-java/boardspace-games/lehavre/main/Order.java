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
package lehavre.main;

import java.io.*;
import java.util.*;

/**
 *
 *	The <code>Order</code> class is a serializable class that is used
 *	in messages sent to the peers. It holds the order ID and a list of
 *	argument objects.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/16
 */
public final class Order
implements Serializable
{
	static final long serialVersionUID =1L;
	/** The order ID. */
	private int id;

	/** The list of argument objects. */
	private List<? extends Serializable> args;

	/**
	 *	Creates a new <code>Order</code> object with
	 *	the given order ID and argument objects.
	 *	@param id the order id
	 *	@param args the list of argument objects
	 */
	public Order(int id, List<? extends Serializable> args) {
		this.id = id;
		this.args = args;
	}

	/**
	 *	Returns the order id.
	 *	@return the order id
	 */
	public int getOrderId() {
		return id;
	}

	/**
	 *	Returns the list of argument objects.
	 *	@return the list of argument objects
	 */
	public List<? extends Serializable> getArguments() {
		return args;
	}

	/**
	 *	Returns the string representation for the object.
	 *	@return the string representation
	 */
	public String toString() {
		return String.format("%s %d (%s)", getClass().getSimpleName(), id, args);
	}
}