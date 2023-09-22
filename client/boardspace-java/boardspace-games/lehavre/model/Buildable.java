/* copyright notice */package lehavre.model;

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