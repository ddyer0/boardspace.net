// ===========================================================================
// CONTENT  : CLASS MultiValueCategory
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

/**
 * A category that can hold settings with multiple values.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
class MultiValueCategory extends GenericCategory
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
	protected MultiValueCategory( String aName, boolean checkCase )
	{
		super( aName, checkCase ) ;
	} // MultiValueCategory()
   
	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected String[] getValuesOf( String keyName ) 
	{
		String[] resultValues		= null ;
		NamedValues setting	 		= null ;
		
		setting = this.findSettingNamed( keyName ) ;
		if ( setting != null )
		{
			resultValues = setting.getValues() ;
		}
		return resultValues ;
	} // getValuesOf()
   		
	// ------------------------------------------------------------------------
	
	protected void setValuesOf( String keyName, String[] values )
	{
		NamedValues setting = null ;
		
		setting = this.getNamedValues( keyName ) ;
		setting.setValues( values ) ;
	} // setValuesOf()
  
	// ------------------------------------------------------------------------

	protected void addValue( String keyName, String value )
	{
		NamedValues setting = null ;
		
		setting = this.getNamedValues( keyName ) ;
		setting.addValue( value ) ;
	} // addValue()
  
	// ------------------------------------------------------------------------
	
	protected void addValues( String keyName, String[] values )
	{
		NamedValues setting = null ;
		
		setting = this.getNamedValues( keyName ) ;
		setting.addValues( values ) ;
	} // addValues()
  
	// ------------------------------------------------------------------------
	
	protected void removeValueFrom( String keyName, String value )
	{
		NamedValues setting = null ;
		
		setting = this.findSettingNamed( keyName ) ;
		if ( setting != null )
			setting.removeValue( value ) ;
	} // removeValueFrom()
  
	// ------------------------------------------------------------------------
		
	protected NamedValues getNamedValues( String keyName )
	{
		GenericNamedObject setting = null ;
		
		setting = this.findSettingNamed( keyName ) ;
		if ( setting == null )
		{
			setting = this.createAndAddNewSetting( keyName ) ;
		}		
		return (NamedValues)setting ;
	} // getNamedValues()
  
	// -------------------------------------------------------------------------
	
	protected NamedValues findSettingNamed( String keyName )
	{
		GenericNamedObject setting	= null ;

		setting = this.getSettingNamed( keyName ) ;		
		if ( setting instanceof NamedValues )
			return (NamedValues)setting ;
		else 
			return null ;
	} // findSettingNamed()
   		
	// ------------------------------------------------------------------------

	protected GenericNamedObject createNewSetting( String aName )
	{
		return new NamedValues( aName, this.getCaseSensitive() ) ;
	} // createNewSetting()
   	
	// ------------------------------------------------------------------------

} // class MultiValueCategory
