// ===========================================================================
// CONTENT  : CLASS MultiValueSettingsImpl
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 02/07/2003
// HISTORY  :
//  02/07/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.impl ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.settings.MultiValueSettings;

/**
 * Implementation of the MultiValueSettings interface.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class MultiValueSettingsImpl extends GenericSettingsImpl implements MultiValueSettings
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MultiValueSettingsImpl()
  {
    super() ;
  } // MultiValueSettingsImpl()
      
	// -------------------------------------------------------------------------
	
	/**
	 * Initialize the new instance with a name.
	 * @param aName The name of the whole settings object
	 */
	public MultiValueSettingsImpl(String aName)
	{
		super(aName);
	} // MultiValueSettingsImpl()
     
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the values of keyName in the specified category.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of a key inside the category
	 * @return The associated values or null if either the category or the key could not be found
	 */
	public String[] getValuesOf(String categoryName, String keyName)
	{
		String[] resultValues		= null ;
		String value ;

		resultValues = this.basicGetValuesOf( categoryName, keyName ) ;
		if ( ( resultValues == null ) && ( this.hasDefaults() ) )
		{
			if ( this.hasMultiValueDefaults() )
				return this.getMultiValueDefaults().getValuesOf( categoryName, keyName ) ;
				
			value = this.getDefaults().getValueOf( categoryName, keyName ) ;
			if ( value == null )
				return null ;
			else
				return new String[] { value } ;
		}
		return resultValues ;
	} // getValuesOf()
    
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the values that are registered under the given keyName.
	 * 
	 * @param keyName The name of a key inside the default category
	 * @return The associated values or null if the key could not be found
	 */
	public String[] getValuesOf(String keyName)
	{
		return this.getValuesOf( null, keyName ) ;
	} // getValuesOf()
    
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the values of given key in the specified category.
	 * If the category does not yet exist, it will be created.
	 * If the key does not yet exist, it will be created otherwise the old
	 * values will be replaced.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of the key the value should be assigned to.
	 * @param values The values to be assigned to the key
	 */
	public void setValuesOf(String categoryName, String keyName, String[] values)
	{
		MultiValueCategory category ;
		
		category = this.getMultiValueCategoryNamed( categoryName ) ;
		category.setValuesOf( keyName, values ) ;
	} // setValuesOf()
    
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the values of given key in the default category.
	 * If the default category does not yet exist, it will be created.
	 * If the key does not yet exist, it will be created otherwise the old
	 * values will be replaced.
	 * 
	 * @param keyName The name of the key the values should be assigned to.
	 * @param values The values to be assigned to the key
	 */
	public void setValuesOf(String keyName, String[] values)
	{
		this.setValuesOf( null, keyName, values ) ;
	} // setValuesOf()
    	
	// -------------------------------------------------------------------------
	
	/**
	 * Adds the value to the given key in the specified category.
	 * If the category does not yet exist, it will be created.
	 * If the key does not yet exist, it will be created otherwise the 
	 * given value will be added to the old values.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of the key the value should be added to.
	 * @param value The value to be added to the key's current values
	 */
	public void addValue( String categoryName, String keyName, String value ) 
	{
		MultiValueCategory category ;
		
		category = this.getMultiValueCategoryNamed( categoryName ) ;
		category.addValue( keyName, value ) ;
	} // addValue()
 
	// ------------------------------------------------------------------------
	
	/**
	 * Adds the value to the given key in the default category.
	 * If the default category does not yet exist, it will be created.
	 * If the key does not yet exist, it will be created otherwise the
	 * given value will be added to the old values. <br>
	 * This method does the same as setValueOf().
	 * 
	 * @param keyName The name of the key the value should be added  to.
	 * @param value The value to be added to the key's current values
	 * @see org.pf.settings.Settings#setValueOf(String,String)
	 */
	public void addValue( String keyName, String value ) 
	{
		this.addValue( null, keyName, value ) ;
	} // addValue()
 
	// ------------------------------------------------------------------------

	/**
	 * Adds the values to the given key in the specified category.
	 * If the category does not yet exist, it will be created.
	 * If the key does not yet exist, it will be created otherwise the 
	 * new values will be added to the old values.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of the key the value should be added to.
	 * @param values The values to be added to the key's current values
	 */
	public void addValues( String categoryName, String keyName, String[] values ) 
	{
		MultiValueCategory category ;
	
		category = this.getMultiValueCategoryNamed( categoryName ) ;
		category.addValues( keyName, values ) ;
	} // addValues()
  
	// ------------------------------------------------------------------------
	
	/**
	 * Adds the values to the given key in the default category.
	 * If the default category does not yet exist, it will be created.
	 * If the key does not yet exist, it will be created otherwise the
	 * new values will be added to the old values.
	 * 
	 * @param keyName The name of the key the values should be added  to.
	 * @param values The values to be added to the key's current values
	 */
	public void addValues( String keyName, String[] values ) 
	{
		this.addValues( null, keyName, values ) ;
	} // addValues()
  
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
	public void removeValue(String categoryName, String keyName, String value)
	{
		MultiValueCategory category ;
	
		category = this.getMultiValueCategoryNamed( categoryName ) ;
		category.removeValueFrom( keyName, value ) ;
	} // removeValue()
    
	// -------------------------------------------------------------------------
	
	/**
	 * Removes the given value from the setting with the specified key 
	 * which is looked up in the default category.
	 * 
	 * @param keyName The name of the key from which to remove the value
	 * @param value The value to remove
	 */
	public void removeValue(String keyName, String value)
	{
		this.removeValue( null, keyName, value ) ;
	} // removeValue()
    
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the values of keyName in the specified category.
	 * Works only on this settings objects and NOT on the default settings !!!
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of a key inside the category
	 * @return The associated values or null if either the category or the key could not be found
	 */
	protected String[] basicGetValuesOf( String categoryName, String keyName ) 
	{
		String[] resultValues	= null ;
		MultiValueCategory category 	= null ;
		
		category = this.findCategoryNamed( categoryName ) ;
		if ( category != null )
		{
			resultValues = category.getValuesOf( keyName ) ;
		}
		return resultValues ;
	} // basicGetValuesOf()
    
	// ------------------------------------------------------------------------

	protected MultiValueCategory getMultiValueCategoryNamed( String categoryName )
	{
		GenericCategory category = null ;
		
		category = this.getCategoryNamed( categoryName ) ;
		if ( category == null )
		{
			category = this.createAndAddCategory(categoryName);
		}
		return (MultiValueCategory)category ;
	} // getMultiValueCategoryNamed()

	// -------------------------------------------------------------------------
	
	protected MultiValueCategory findCategoryNamed( String categoryName )
	{
		GenericCategory category ;
		
		category = this.getCategoryNamed( categoryName ) ;
		if ( category instanceof MultiValueCategory )
			return (MultiValueCategory)category ;
		else
			return null ;
	} // findCategoryNamed()
    
	// -------------------------------------------------------------------------
	
	protected MultiValueSettings getMultiValueDefaults()
	{
		return (MultiValueSettings)this.getDefaults() ;
	} // getMultiValueDefaults()
    
	// -------------------------------------------------------------------------
	
	protected boolean hasMultiValueDefaults()
	{
		return this.getDefaults() instanceof MultiValueSettings ;
	} // hasMultiValueDefaults()
    
	// -------------------------------------------------------------------------
	
	protected GenericCategory createNewCategory( String categoryName ) 
	{
		return new MultiValueCategory( categoryName, this.getCaseSensitive() ) ;
	} // createNewCategory()
      
	// ------------------------------------------------------------------------

} // class MultiValueSettingsImpl
