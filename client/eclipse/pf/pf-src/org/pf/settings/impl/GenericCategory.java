// ===========================================================================
// CONTENT  : ABSTRACT CLASS GenericCategory
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 08/01/2009
// HISTORY  :
//  02/07/2003  mdu  CREATED
//	08/01/2009	mdu		added		-> toString()
//
// Copyright (c) 2003-2009, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.impl ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implements the generic behaviour of categories
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
abstract class GenericCategory extends SettingsElement
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String DEFAULT_CATEGORY_NAME	= GenericSettingsImpl.DEFAULT_CATEGORY_NAME ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private List keysAndValues = new ArrayList() ;
	protected List getKeysAndValues() { return keysAndValues ; }
	protected void setKeysAndValues( List aValue ) { keysAndValues = aValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
	protected GenericCategory( String aName, boolean checkCase )
	{
		super( ( ( aName == null ) ? DEFAULT_CATEGORY_NAME : aName ), checkCase ) ;
	} // GenericCategory() 
 
	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	public String toString()
	{
		return this.getClass().getName() + "(\"" + this.getName() + "\")" ; 
	} // toString() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // ABSTRACT INSTANCE METHODS
  // =========================================================================
  /**
   * Create a new settings object with the given name
   */
	abstract protected GenericNamedObject createNewSetting( String aName ) ;

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected void changeCaseSensitive( boolean isCaseSensitive ) 
	{
		Iterator keyValues ;
		GenericNamedObject keyValue ;
		
		this.setCaseSensitive( isCaseSensitive ) ;
		keyValues = this.getKeysAndValues().iterator(); 
		while ( keyValues.hasNext() )
		{
			keyValue = (GenericNamedObject)keyValues.next();
			keyValue.changeCaseSensitive( isCaseSensitive ) ;
		}		
	} // changeCaseSensitive() 
 
	// -------------------------------------------------------------------------
	
	protected boolean isDefaultCategory() 
	{
		return this.getName().equals( DEFAULT_CATEGORY_NAME ) ;
	} // isDefaultCategory() 
 		
	// ------------------------------------------------------------------------
	
	protected String getValueOf( String keyName ) 
	{
		String resultValue				= null ;
		GenericNamedObject setting	 	= null ;
		
		setting = this.getSettingNamed( keyName ) ;
		if ( setting != null )
		{
			resultValue = setting.getValue() ;
		}
		return resultValue ;
	} // getValueOf() 
 		
	// ------------------------------------------------------------------------
	
	protected void setValueOf( String keyName, String value )
	{
		GenericNamedObject setting = null ;
		
		setting = this.getSettingNamed( keyName ) ;
		if ( setting == null )
		{
			setting = this.createAndAddNewSetting( keyName ) ;
		}
		setting.setValue( value ) ;
	} // setValueOf() 
 
	// ------------------------------------------------------------------------

	protected String[] getKeyNames()
	{
		Iterator list 						= null ;
		GenericNamedObject setting		= null ;
		String[] keyNames					= null ;
		int counter								= 0 ;
		
		keyNames = new String[this.getKeysAndValues().size()] ;
		list = this.getKeysAndValues().iterator() ;
		while ( list.hasNext() )
		{
			setting = (GenericNamedObject)list.next() ;
			keyNames[counter] = setting.getName() ;
			counter++ ;
		} 
		return keyNames ;		
	} // getKeyNames() 
 	
	// ------------------------------------------------------------------------

	protected void removeKey( String keyName )
	{
		GenericNamedObject setting = null ;
		
		setting = this.getSettingNamed( keyName ) ;		
		if ( setting != null )
			this.getKeysAndValues().remove( setting ) ;
	} // removeKey() 
 
	// ------------------------------------------------------------------------
	
	protected boolean matchesName( String aName )
	{
		if ( ( aName == null ) && ( this.isDefaultCategory() ) )
			return true ;
		else
			return super.matchesName( aName ) ;
	} // matchesName() 
 		
	// ------------------------------------------------------------------------	

	protected GenericNamedObject getSettingNamed( String keyName )
	{
		Iterator list 					= null ;
		GenericNamedObject setting	= null ;
		
		list = this.getKeysAndValues().iterator() ;
		while ( list.hasNext() )
		{
			setting = (GenericNamedObject)list.next() ;
			if ( setting.matchesName( keyName ) )
				return setting ;
		} 
		return null ;
	} // getSettingNamed() 
 		
	// ------------------------------------------------------------------------

	protected GenericNamedObject createAndAddNewSetting( String keyName )
	{
		GenericNamedObject setting ;
		
		setting = this.createNewSetting( keyName ) ;
		this.getKeysAndValues().add( setting ) ;
		return setting ;
	} // createAndAddNewSetting() 
 
	// ------------------------------------------------------------------------

} // class GenericCategory 
