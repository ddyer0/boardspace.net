// =======================================================================
// Title   : Interface SettingsImpl
// Author  : Manfred Duchrow
// Version : 1.1 - 18/12/2002
// History :
// 	16/06/2002 	duma  created
//	18/12/2002	duma	added		->	getName(), setName(), getSettingsNameOf()
//																getDefaults, setDefaults(), 
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings;

// ========================================================================
// IMPORT
// ========================================================================

/**
 * This interface provides read/only access to settings.
 * The structure is, that there is a category with any number of
 * key/value pairs inside.
 * A key is unique in one category but may occur several times in
 * different categories.
 * <br>
 * If a category of null is passed to any method, the default category
 * will be taken instead.
 * <b>
 * Apart from the interface each implementing class must provide a
 * public default constructor so that instance creation with newInstance()
 * is possible !</b>
 * <p>
 * Additionaly the settings object can be named to distinguish between several
 * settings objects. The name also might be used to keep the source (e.g. 
 * file name or URL) of the settings.
 * <br>
 * Each settings object can have another settings objects with default settings.
 * Whenever a setting can't be found in the main settings, it will be looked
 * up in the default settings. Of course this can be cascaded unlimited.
 * 
 * @author Manfred Duchrow
 * @version 1.1
 */
public interface ReadOnlySettings
{

	// ========================================================================
	// PUBLIC INSTANCE METHODS
	// ========================================================================
	
	/**
	 * Returns the name of the settings object as a whole. 
	 * This might be a simple name or a resource locator or a filename.
	 * However, naming a setting is optional and therefore this method can also
	 * return null.
	 */
  public String getName() ;
  
  // -----------------------------------------------------------------------
  
	/**
	 * Sets the name of the settings object as a whole. 
	 * This might be a simple name or a resource locator or a filename.
	 */
  public void setName( String aName ) ;
  
  // -------------------------------------------------------------------------
  
	/**
	 * Returns the value of keyName in the specified category.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of a key inside the category
	 * @return The associated value or null if either the category or the key could not be found
	 */
	public String getValueOf( String categoryName, String keyName )  ;

	// ------------------------------------------------------------------------
	
	/**
	 * Returns the value of keyName in the default category.
	 * 
	 * @param keyName The name of a key inside the default category
	 * @return The associated value or null if the key could not be found
	 */
	public String getValueOf( String keyName ) ;

	// ------------------------------------------------------------------------
	
	/**
	 * Returns an array of all currently known categories.
	 * An empty string is the name for the default category!
	 */
	public String[] getCategoryNames() ;

	// ------------------------------------------------------------------------
	
	/**
	 * Returns all currently known key names in the category with the
	 * specified name.
	 * If the category name is null or an empty string, the default
	 * category's keys will be returned.
	 * 
	 * @param categoryName The name of the category the keys are wanted from
	 */
	public String[] getKeyNamesOf( String categoryName ) ;
	
	// ------------------------------------------------------------------------
	
	/**
	 * Returns all currently known key names in the default category.
	 */
	public String[] getKeyNamesOfDefaultCategory() ;
	
	// ------------------------------------------------------------------------
	
	/**
	 * Gets the defaults that are looked up, if a setting can't be found
	 * in the main settings object.
	 * 
	 * @return A settings object with default values or null
	 */
	public ReadOnlySettings getDefaults() ;

  // -------------------------------------------------------------------------
	
	/**
	 * Sets defaults that must be looked up, if a setting can't be found
	 * in the main settings object.
	 * 
	 * @param defaults A settings object with default values or null
	 */
	public void setDefaults( ReadOnlySettings defaults ) ;

  // -------------------------------------------------------------------------

	/**
	 * Returns the name of the name of the settings where the specified
	 * category and key are found.
	 * That is a lookup in this settings object and then, if not found, 
	 * in its defaults and so on.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of a key inside the category
	 * @return The name of the settings object or null 
	 */
	public String getSettingsNameOf( String categoryName, String keyName ) ;

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the name of the name of the settings where the specified
	 * key is found in the default category.
	 * That is a lookup in this settings object and then, if not found, 
	 * in its defaults and so on.
	 * 
	 * @param keyName The name of a key inside the default category
	 * @return The name of the settings object or null 
	 */
	public String getSettingsNameOf( String keyName ) ;

	// -------------------------------------------------------------------------
	
} // interface ReadOnlySettings