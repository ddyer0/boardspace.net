// ===========================================================================
// CONTENT  : INTERFACE MultiValueSettings
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 02/07/2003
// HISTORY  :
//  02/07/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This interface defines the methods that are necessary to get and and set
 * settings which might have multiple values.
 * <p>
 * The inherited methods must work as follows:
 * <ul>
 * 	<li><b>getValueOf()</b><br>
 * 		Returns the first value of the setting or null, if the setting can't 
 * 		be found
 * 	</li>
 * 	<li><b>setValueOf()</b><br>
 * 		Adds the given value to the values of the specified setting
 * 	</li>
 * </ul>
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface MultiValueSettings extends Settings
{
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the values that are registered under the given keyName.
	 * 
	 * @param keyName The name of a key inside the default category
	 * @return The associated values or null if the key could not be found
	 */
	public String[] getValuesOf( String keyName ) ;

	// -------------------------------------------------------------------------

	/**
	 * Returns the values of keyName in the specified category.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of a key inside the category
	 * @return The associated values or null if either the category or the key could not be found
	 */
	public String[] getValuesOf( String categoryName, String keyName )  ;

	// ------------------------------------------------------------------------
	
	/**
	 * Removes the given value from the setting with the specified key 
	 * in the specified category.
	 * If the categoryName is null or an empty string the key will be 
	 * looked up in the default category.
	 * 
	 * @param categoryName The name of the category the key resides in
	 * @param keyName The name of the key from which to remove the value
	 * @param value The value to remove
	 */
	public void removeValue( String categoryName, String keyName, String value ) ;
	
	// ------------------------------------------------------------------------
	
	/**
	 * Removes the given value from the setting with the specified key 
	 * which is looked up in the default category.
	 * 
	 * @param keyName The name of the key from which to remove the value
	 * @param value The value to remove
	 */
	public void removeValue( String keyName, String value ) ;
	
	// ------------------------------------------------------------------------
		
} // interface MultiValueSettings
