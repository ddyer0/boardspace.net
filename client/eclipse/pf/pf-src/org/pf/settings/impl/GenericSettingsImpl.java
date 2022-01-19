// ===========================================================================
// CONTENT  : ABSTRACT CLASS GenericSettingsImpl
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 24/03/2006
// HISTORY  :
//  02/07/2003  mdu  CREATED
//
// Copyright (c) 2003-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.impl ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pf.settings.CaseSensitivity;
import org.pf.settings.ReadOnlySettings;
import org.pf.settings.Settings;
import org.pf.util.CollectionUtil;

/**
 * Generic implementation the Settings interface.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
abstract public class GenericSettingsImpl implements Settings, CaseSensitivity
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 * The name of the default category (i.e."") which is also representing 
	 * null as value for category.
	 */
	public static final String DEFAULT_CATEGORY_NAME	= "" ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private String name = null ;
	
	private List categories = new ArrayList() ;
	protected List getCategories() { return categories ; }
	protected void setCategories( List aValue ) { categories = aValue ; }

	private ReadOnlySettings defaultSettings = null ;
	protected ReadOnlySettings defaultSettings() { return defaultSettings ; }
	public void defaultSettings( ReadOnlySettings newValue ) { defaultSettings = newValue ; }
  
	private boolean caseSensitive = true ;
	protected boolean caseSensitive() { return caseSensitive ; }
	protected void caseSensitive( boolean newValue ) { caseSensitive = newValue ; }  

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public GenericSettingsImpl()
  {
    super() ;
  } // GenericSettingsImpl()

	// -------------------------------------------------------------------------

	/**
	 * Create a new instance with the specified name.
	 */
	public GenericSettingsImpl( String aName )
	{
		this() ;
		this.setName( aName ) ;
	} // GenericSettingsImpl()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the value of keyName in the specified category.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of a key inside the category
	 * @return The associated value or null if either the category or the key could not be found
	 */
	public String getValueOf( String categoryName, String keyName ) 
	{
		String resultValue				= null ;

		resultValue = this.basicGetValueOf( categoryName, keyName ) ;
		if ( ( resultValue == null ) && ( this.hasDefaults() ) )
			return this.getDefaults().getValueOf( categoryName, keyName ) ;
		
		return resultValue ;
	} // getValueOf()

	// ------------------------------------------------------------------------
	
	/**
	 * Returns the value of keyName in the default category.
	 * 
	 * @param keyName The name of a key inside the default category
	 * @return The associated value or null if the key could not be found
	 */
	public String getValueOf( String keyName ) 
	{
		return getValueOf( null, keyName ) ;
	} // getValueOf()

	// ------------------------------------------------------------------------
	
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
	public void setValueOf( String categoryName, String keyName, String value )
	{
		GenericCategory category = null ;
		
		category = this.getCategoryNamed( categoryName ) ;
		if ( category == null )
		{
			category = this.createAndAddCategory(categoryName);
		}
		category.setValueOf( keyName, value ) ;
	} // setValueOf()

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
	public void setValueOf( String keyName, String value )
	{
		this.setValueOf( null, keyName, value ) ;
	} // setValueOf()

	// ------------------------------------------------------------------------
	
	/**
	 * Returns an array of all currently known categories.
	 * An empty string is the name for the default category!
	 */
	public String[] getCategoryNames()
	{
		List categoryNames ;
		
		categoryNames = this.getCategoryNameList() ;

		return (String[]) categoryNames.toArray(new String[categoryNames.size()]) ;		
	} // getCategoryNames()
	
	// ------------------------------------------------------------------------
	
	/**
	 * Returns all currently known key names in the category with the
	 * specified name.
	 * If the category name is null or an empty string, the default
	 * category's keys will be returned.
	 */
	public String[] getKeyNamesOf( String categoryName )
	{
		List keyNames ;
		 
		keyNames = this.basicGetKeyNamesOf( categoryName ) ;
		if ( this.hasDefaults() )
			this.util().addAll( keyNames, this.getDefaults().getKeyNamesOf(categoryName) ) ; 
		 
		return (String[])keyNames.toArray(new String[keyNames.size()]) ;
	} // getKeyNamesOf() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Returns all currently known key names in the default category.
	 */
	public String[] getKeyNamesOfDefaultCategory()
	{
		return this.getKeyNamesOf( null ) ;
	} // getKeyNamesOfDefaultCategory()
	
	// ------------------------------------------------------------------------
	
	/**
	 * Removes the key and its associated value from the specified category.
	 * If the categoryName is null or an empty string the key/value pair
	 * will be removed from the default category.
	 * <br>
	 * This method will never remove anything from the default settings!
	 * 
	 * @param categoryName The name of the category the key should be removed from
	 * @param keyName The name of the key to remove
	 */
	public void removeKey( String categoryName, String keyName )
	{
		GenericCategory category = null ;
		 
		category = this.getCategoryNamed( categoryName ) ;
		if ( category != null )
		{
			category.removeKey( keyName ) ;
		}
	} // removeKey()
	
	// ------------------------------------------------------------------------
	
	/**
	 * Removes the key and its associated value from the default category.
	 * <br>
	 * This method will never remove anything from the default settings!
	 * 
	 * @param keyName The name of the key to remove
	 */
	public void removeKey( String keyName )
	{
		this.removeKey( null, keyName ) ;
	} // removeKey()
	
	// ------------------------------------------------------------------------
	
	/**
	 * Removes the category with the specified name and all its key/value pairs.
	 * If the categoryName is null or an empty string the default category
	 * will be removed.
	 * <br>
	 * This method will never remove anything from the default settings!
	 * 
	 * @param categoryName The name of the category to removed
	 */
	public void removeCategory( String categoryName )
	{
		GenericCategory category = null ;
		
		category = this.getCategoryNamed( categoryName ) ;
		if ( category != null )
		{
			this.getCategories().remove( category ) ;
		}
	} // removeCategory()
	
	// ------------------------------------------------------------------------
		
	/**
	 * Removes the default category and all its key/value pairs.
	 * <br>
	 * This method will never remove anything from the default settings!
	 */
	public void removeDefaultCategory()
	{
		this.removeCategory( null ) ;
	} // removeDefaultCategory()
	
	// ------------------------------------------------------------------------
		
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
	public String getSettingsNameOf( String categoryName, String keyName ) 
	{
		if ( this.basicGetValueOf( categoryName, keyName ) != null )
			return this.getName() ;
			
		if ( this.hasDefaults() )	
			return this.getDefaults().getSettingsNameOf( categoryName, keyName ) ;
			
		return null ;
	} // getSettingsNameOf() 

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
	public String getSettingsNameOf( String keyName ) 
	{
		return this.getSettingsNameOf( null, keyName ) ;
	} // getSettingsNameOf() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the name of the settings object as a whole. 
	 * This might be a simple name or a resource locator or a filename.
	 * However, naming a setting is obtional and therefore this method can also
	 * return null.
	 */
	public String getName() 
	{
		return name ;
	} // getName()
  
	// -----------------------------------------------------------------------
  
	/**
	 * Sets the name of the settings object as a whole. 
	 * This might be a simple name or a resource locator or a filename.
	 */
	public void setName( String aName ) 
	{
		name = aName ;
	} // setName()
  
	// -------------------------------------------------------------------------
  	
	/**
	 * Returns true, if the search for category and key names is case sensitive.
	 */
	public boolean getCaseSensitive() 
	{ 
		return caseSensitive ; 
	} // getCaseSensitive()
  
	// -------------------------------------------------------------------------
  
	/**
	 * Sets whether or not the internal search for category names and
	 * key names must be case sensitive.
	 * <br>
	 * If there are default settings set in this thettings their case-sensitivity
	 * will be changed accordingly.
	 * 
	 * @param isCaseSensitive The new value that determins if names are compared case sensitive or not
	 */
	public void setCaseSensitive( boolean isCaseSensitive ) 
	{ 
		GenericCategory category ;
		Iterator iterator ;
		CaseSensitivity defaults ;

		this.caseSensitive( isCaseSensitive ) ;
		iterator = this.getCategories().iterator() ;  	
		while( iterator.hasNext() )
		{
			category = (GenericCategory)iterator.next();
			category.changeCaseSensitive( isCaseSensitive ) ;
		}
		
		if ( this.hasDefaults() && ( this.getDefaults() instanceof CaseSensitivity ) )
		{
			defaults = (CaseSensitivity)this.getDefaults() ;
			defaults.setCaseSensitive( isCaseSensitive ) ;
		}
	} // setCaseSensitive()
	
	// -------------------------------------------------------------------------

	/**
	 * Gets the defaults that are looked up, if a setting can't be found
	 * in the main settings object.
	 * 
	 * @return A settings object with default values or null
	 */
	public ReadOnlySettings getDefaults() 
	{
		return this.defaultSettings() ;
	} // getDefaults()

	// -------------------------------------------------------------------------
	
	/**
	 * Sets defaults that must be looked up, if a setting can't be found
	 * in the main settings object.
	 * 
	 * @param defaults A settings object with default values or null
	 */
	public void setDefaults( ReadOnlySettings defaults ) 
	{
		this.defaultSettings( defaults ) ;
	} // setDefaults() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// ABSTRACT INSTANCE METHODS
	// =========================================================================
	abstract protected GenericCategory createNewCategory( String categoryName ) ;

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns a list of all currently known categories.
	 * An empty string is the name for the default category!
	 */
	protected List getCategoryNameList()
	{
		List categoryNames ;
		
		categoryNames = this.basicGetCategoryNames() ;
		if ( this.hasDefaults() )
		{
			util().addAllNew( categoryNames, this.getDefaults().getCategoryNames() ) ;
		}

		return categoryNames ;
	} // getCategoryNameList()
	
	// ------------------------------------------------------------------------
	
	/**
	 * Returns all currently known key names in the category with the
	 * specified name.
	 * If the category name is null or an empty string, the default
	 * category's keys will be returned.
	 */
	protected List getKeyNameListOf( String categoryName )
	{
		List keyNames ;
		 
		keyNames = this.basicGetKeyNamesOf( categoryName ) ;
		if ( this.hasDefaults() )
			util().addAllNew( keyNames, this.getDefaults().getKeyNamesOf(categoryName) ) ; 
		 
		return keyNames ;
	} // getKeyNameListOf() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Returns the value of keyName in the specified category.
	 * Works only on this settings objects and NOT on the default settings !!!
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of a key inside the category
	 * @return The associated value or null if either the category or the key could not be found
	 */
	protected String basicGetValueOf( String categoryName, String keyName ) 
	{
		String resultValue	= null ;
		GenericCategory category 	= null ;
		
		category = this.getCategoryNamed( categoryName ) ;
		if ( category != null )
		{
			resultValue = category.getValueOf( keyName ) ;
		}
		return resultValue ;
	} // basicGetValueOf()

	// ------------------------------------------------------------------------
	
	/**
	 * Returns an array of all currently known categories.
	 * An empty string is the name for the default category!
	 * Works only on this settings objects and NOT on the default settings !!!
	 */
	protected List basicGetCategoryNames()
	{
		Iterator list 			= null ;
		GenericCategory category		= null ;
		List categoryNames	= null ;
		
		categoryNames = new ArrayList(this.getCategories().size()) ;
		list = this.getCategories().iterator() ;
		while ( list.hasNext() )
		{
			category = (GenericCategory)list.next() ;
			categoryNames.add( category.getName() ) ;
		} 
		return categoryNames ;		
	} // basicGetCategoryNames()
	
	// ------------------------------------------------------------------------
	
	/**
	 * Returns all currently known key names in the category with the
	 * specified name.
	 * If the category name is null or an empty string, the default
	 * category's keys will be returned.
	 * Works only on this settings objects and NOT on the default settings !!!
	 */
	protected List basicGetKeyNamesOf( String categoryName )
	{
		GenericCategory category = null ;
		List keyNames			= new ArrayList() ;
		 
		category = this.getCategoryNamed( categoryName ) ;
		if ( category != null )
		{
			this.util().addAll( keyNames, category.getKeyNames() ) ;
		}
		return keyNames ;
	} // basicGetKeyNamesOf() 
	
	// ------------------------------------------------------------------------
	
	protected boolean hasDefaults()
	{
		return this.defaultSettings() != null ;
	} // hasDefaults()

	// -------------------------------------------------------------------------

	/**
	 * Returns the category with the specified categoryName.
	 * Works only on this settings objects and NOT on the default settings !!!
	 */
	protected GenericCategory getCategoryNamed( String categoryName )
	{
		Iterator list 						= null ;
		GenericCategory category					= null ;
		
		list = this.getCategories().iterator() ;
		while ( list.hasNext() )
		{
			category = (GenericCategory)list.next() ;
			if ( category.matchesName( categoryName ) )
				return category ;
		} 
		
		return null ;
	} // getCategoryNamed()		
		
	// ------------------------------------------------------------------------

	protected GenericCategory createAndAddCategory( String categoryName ) 
	{
		GenericCategory category;
		
		category = this.createNewCategory( categoryName ) ;
		this.getCategories().add( category ) ;
		
		return category;
	} // createAndAddCategory()

	// ------------------------------------------------------------------------
	
	protected CollectionUtil util()
	{
		return CollectionUtil.current() ;
	} // util()

	// -------------------------------------------------------------------------
	
	// Rendering for JOI 
	protected String inspectString()
	{
		return "Settings(\"" + this.getName() + "\")" ; 
	} // inspectString()

	// -------------------------------------------------------------------------
			
} // class GenericSettingsImpl
