// ===========================================================================
// CONTENT  : CLASS Bool
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 01/02/2000
// HISTORY  :
//  01/02/2000  duma  CREATED
//	20/10/2002	duma	added		->	toBoolean(boolean)
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Utility class for missing convenience with Boolean and boolean.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class Bool
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	protected final static Bool TRUE = new Bool(true);
	protected final static Bool FALSE = new Bool(false);

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private boolean boolValue = false;
	protected boolean getBoolValue()
	{
		return boolValue;
	}
	protected void setBoolValue(boolean newValue)
	{
		boolValue = newValue;
	}

	// =========================================================================
	// CLASS METHODS
	// =========================================================================
	/**
	 * Returns the Bool instance that represents the <b>true</b> value.
	 */
	public static Bool getTrue()
	{
		return TRUE;
	} // getTrue()  

	// -------------------------------------------------------------------------

	/**
	 * Returns the Bool instance that represents the <b>true</b> value.
	 */
	public static Bool getFalse()
	{
		return FALSE;
	} // getFalse()  

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if the given object is a sort of boolean "true" representation
	 * otherwise false.  <br>
	 * Objects recognized as true are:  <br>
	 * Boolean.TRUE, "true", "on", "yes", "1"
	 */
	public static boolean isTrue(Object anObject)
	{
		if (anObject instanceof String)
			return get((String) anObject).isTrue();

		if (anObject instanceof Boolean)
			return get((Boolean) anObject).isTrue();

		return false;
	} // isTrue()  

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if the given object is a sort of boolean "false" representation
	 * otherwise false.  <br>
	 * Objects recognized as true see {@link #isTrue()}. All others are
	 * treated as a false representation.
	 */
	public static boolean isFalse(Object anObject)
	{
		return (!isTrue(anObject));
	} // isFalse()  

	// -------------------------------------------------------------------------

	/**
	 * Returns the Bool instance corresponding to the given <i>boolean</i> value.
	 */
	public static Bool get(boolean bool)
	{
		return (bool ? getTrue() : getFalse());
	} // get()  

	// -------------------------------------------------------------------------

	/**
	 * Returns the Bool instance corresponding to the given <i>Boolean</i> value.
	 */
	public static Bool get(Boolean bool)
	{
		return get(bool.booleanValue());
	} // get()  

	// -------------------------------------------------------------------------

	/**
	 * Returns the Bool instance corresponding to the given <i>String</i> value.
	 * <br>
	 * The values "true", "on", "yes" and "1" return the <b>true</b> instance.
	 * All other values return the <b>false</b> instance.
	 */
	public static Bool get(String boolStr)
	{
		boolean bool = false;
		if (boolStr != null)
		{
			if ((boolStr.equalsIgnoreCase("true"))
				|| (boolStr.equalsIgnoreCase("on"))
				|| (boolStr.equalsIgnoreCase("yes"))
				|| (boolStr.equals("1")))
			{
				bool = true;
			}
		}
		return get(bool);
	} // get()  

	// -------------------------------------------------------------------------

	/**
	 * Returns the Boolean object Boolean.FALSE or Boolean.TRUE that 
	 * corresponds to the given boolean flag.
	 */
	public static Boolean toBoolean( boolean flag )
	{
		return ( flag ? Boolean.TRUE : Boolean.FALSE);
	} // toBoolean()  

	// -------------------------------------------------------------------------

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	private Bool(boolean bool)
	{
		this.setBoolValue(bool);
	} // Bool()  

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns true if the receiver is a representation of <b>true</b>.
	 */
	public boolean isTrue()
	{
		return this.getBoolValue();
	} // isTrue()  

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the receiver is a representation of <b>false</b>.
	 */
	public boolean isFalse()
	{
		return (!this.isTrue());
	} // isFalse()  

	// -------------------------------------------------------------------------

	/**
	 * Returns the opposite value of the receiver.
	 */
	public Bool not()
	{
		return (this.isTrue() ? FALSE : TRUE);
	} // not()  

	// -------------------------------------------------------------------------

	/**
	 * Returns the Boolean value of the receiver.
	 */
	public Boolean asBoolean()
	{
		return (this.isTrue() ? Boolean.TRUE : Boolean.FALSE);
	} // asBoolean()  

	// -------------------------------------------------------------------------

	/**
	 * Returns the boolean value of the receiver
	 */
	public boolean booleanValue()
	{
		return this.getBoolValue();
	} // booleanValue()  

	// -------------------------------------------------------------------------

	/**
	 * Returns whether the given boolean value is equal to the receiver.
	 */
	public boolean equals(boolean bool)
	{
		return (this.getBoolValue() == bool);
	} // equals()  

	// -------------------------------------------------------------------------

	/**
	 * Returns whether the given Boolean object is equal to the receiver.
	 */
	public boolean equals(Boolean bool)
	{
		return this.equals(bool.booleanValue());
	} // equals()  

	// -------------------------------------------------------------------------

	/**
	 * Returns whether the given Bool object is equal to the receiver.
	 */
	public boolean equals(Bool bool)
	{
		return (this == bool);
	} // equals()  

	// -------------------------------------------------------------------------

	/**
	 * Returns whether the given object is equal to the receiver.
	 * This one returns always <b>false</b>, because there are separate methods
	 * for the only three types that can be equal ( boolean, Boolean, Bool ).
	 */
	public boolean equals(Object obj)
	{
		return false;
	} // equals()  

	// -------------------------------------------------------------------------

	/**
	 * Returns a hash code of the Bool object.   <br>
	 * The values are the same as in Boolean
	 * @return The integer 1231 if this object represents true;
	 *          returns the integer 1237 if this object represents false.
	 * @see Boolean#hashCode()
	 */
	public int hashCode()
	{
		return (this.isTrue() ? 1231 : 1237);
	} // hashCode()  

	// -------------------------------------------------------------------------

	/**
	 * Returns the string representation of the receiver,
	 * which is either "true" or "false".
	 */
	public String toString()
	{
		return (this.isTrue() ? "true" : "false");
	} // toString()  

	// -------------------------------------------------------------------------

} // class Bool