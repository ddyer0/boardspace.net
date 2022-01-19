// =======================================================================
// Title   : Interface Settings
// Author  : Manfred Duchrow
// Version : 1.2 - 15/06/2002
// History :
// 	01/10/1999 	duma  created
//	26/05/2002	duma	changed	->	From implementation to interface
//	15/06/2002	duma	changed	->	Moved all read methods to new interface ReadOnlySettings
//
// Copyright (c) 1999-2002, by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings;

// ========================================================================
// IMPORT
// ========================================================================

/**
 * This interface provides read and write access to settings.
 * The structure is, that there is a category with any number of
 * key/value pairs inside.
 * A key is unique in one category but may occur several times in
 * different categories.
 * <br>
 * If a category of null is passed to any method, the default category
 * will be taken instead.
 * A SettingsWriter must treat the default category as not existent and write
 * all its key/value pairs directly without any category. The default 
 * category can be recognized by its empty name ("").
 * <p>
 * <b>
 * Apart from the interface each implementing class must provide a
 * public default constructor so that instance creation with newInstance()
 * is possible !</b>
 * 
 * @author Manfred Duchrow
 * @version 1.2
 */
public interface Settings extends ReadOnlySettings
{

	// ========================================================================
	// PUBLIC INSTANCE METHODS
	// ========================================================================
	
	/**
	 * Sets the value of given key in the specified category.
	 * If the category does not yet exist, it will be created.
	 * If the key does not yet exist, it will be created otherwise the old
	 * value will be replaced.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of the key the value should be assigned to.
	 * @param value The value to be assigned to the key
	 */
	public void setValueOf( String categoryName, String keyName, String value ) ;

	// ------------------------------------------------------------------------
	
	/**
	 * Sets the value of given key in the default category.
	 * If the default category does not yet exist, it will be created.
	 * If the key does not yet exist, it will be created otherwise the old
	 * value will be replaced.
	 * 
	 * @param keyName The name of the key the value should be assigned to.
	 * @param value The value to be assigned to the key
	 */
	public void setValueOf( String keyName, String value ) ;

	// ------------------------------------------------------------------------
	
	/**
	 * Removes the key and its associated value from the specified category.
	 * If the categoryName is null or an empty string the key/value pair
	 * will be removed from the default category.
	 * 
	 * @param categoryName The name of the category the key should be removed from
	 * @param keyName The name of the key to remove
	 */
	public void removeKey( String categoryName, String keyName ) ;
	
	// ------------------------------------------------------------------------
	
	/**
	 * Removes the key and its associated value from the default category.
	 * 
	 * @param keyName The name of the key to remove
	 */
	public void removeKey( String keyName ) ;
	
	// ------------------------------------------------------------------------
	
	/**
	 * Removes the category with the specified name and all its key/value pairs.
	 * If the categoryName is null or an empty string the default category
	 * will be removed.
	 * 
	 * @param categoryName The name of the category to removed
	 */
	public void removeCategory( String categoryName ) ;
	
	// ------------------------------------------------------------------------
		
	/**
	 * Removes the default category and all its key/value pairs.
	 */
	public void removeDefaultCategory() ;
	
	// ------------------------------------------------------------------------
		
	
} // interface Settings