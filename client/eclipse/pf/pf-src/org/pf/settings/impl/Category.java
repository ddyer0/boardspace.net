// =======================================================================
// Title   : CLASS Category
// Author  : Manfred Duchrow
// Version : 1.3 - 08/01/2009
// History :
// 	01/10/1999	duma  created
//	24/05/2002	duma	added		->	default category
//	11/01/2003	duma	added		->	changeCaseSensitive()
//	08/01/2009	mdu		added		->	toString()
//
// Copyright (c) 1999-2009, by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings.impl;

// ========================================================================
// IMPORT
// ========================================================================
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Helper class inside settings to represent a category or section.
 */
class Category extends SettingsElement
{
	// ========================================================================
	// CONSTANTS
	// ========================================================================
	private static final String DEFAULT_CATEGORY_NAME	= GenericSettingsImpl.DEFAULT_CATEGORY_NAME ;

	// ========================================================================
	// INSTANCE VARIABLES
	// ========================================================================
  private List keysAndValues = new Vector() ;
  private List getKeysAndValues() { return keysAndValues ; }
  //private void setKeysAndValues( List aValue ) { keysAndValues = aValue ; }

	// ========================================================================
	// CONSTRUCTORS
	// ========================================================================
	Category( String aName, boolean checkCase )
	{
		super( aName == null ? DEFAULT_CATEGORY_NAME : aName, checkCase ) ;
	} //  Category()

	// ========================================================================
	// PUBLIC INSTANCE METHODS
	// ========================================================================
	public String toString() 
	{
		return "Category(" + this.getName() + ")" ;
	} // inspectString()
	
	// -------------------------------------------------------------------------
	
	// ========================================================================
	// PROTECTED INSTANCE METHODS
	// ========================================================================
	protected boolean matchesName( String aName )
	{
		if ( ( aName == null ) && ( this.isDefaultCategory() ) )
			return true ;
		else
			return super.matchesName( aName ) ;
	} // matchesName()
		
	// ------------------------------------------------------------------------	

	// ========================================================================
	// PACKAGE INSTANCE METHODS
	// ========================================================================
	void changeCaseSensitive( boolean isCaseSensitive ) 
	{
		Iterator keyValues ;
		KeyValuePair keyValue ;
		
		this.setCaseSensitive( isCaseSensitive ) ;
		keyValues = this.getKeysAndValues().iterator(); 
		while ( keyValues.hasNext() )
		{
			keyValue = (KeyValuePair)keyValues.next();
			keyValue.changeCaseSensitive( isCaseSensitive ) ;
		}		
	} // changeCaseSensitive()

	// -------------------------------------------------------------------------
	
	boolean isDefaultCategory() 
	{
		return this.getName().equals( DEFAULT_CATEGORY_NAME ) ;
	} // isDefaultCategory() 
		
	// ------------------------------------------------------------------------
	
	String getValueOf( String keyName ) 
	{
		String resultValue				= null ;
		KeyValuePair setting	 	= null ;
		
		setting = this.getSettingNamed( keyName ) ;
		if ( setting != null )
		{
			resultValue = setting.getValue() ;
		}
		return resultValue ;
	} // getValueOf()
		
	// ------------------------------------------------------------------------
	
	void setValueOf( String keyName, String value )
	{
		KeyValuePair setting = null ;
		
		setting = this.getSettingNamed( keyName ) ;
		if ( setting == null )
		{
			setting = this.createNewSetting( keyName ) ;
			this.getKeysAndValues().add( setting ) ;
		}
		setting.setValue( value ) ;
	} // setValueOf()

	// ------------------------------------------------------------------------

	String[] getKeyNames()
	{
		Iterator list 						= null ;
		KeyValuePair setting		= null ;
		String[] keyNames					= null ;
		int counter								= 0 ;
		
		keyNames = new String[this.getKeysAndValues().size()] ;
		list = this.getKeysAndValues().iterator() ;
		while ( list.hasNext() )
		{
			setting = (KeyValuePair)list.next() ;
			keyNames[counter] = setting.getName() ;
			counter++ ;
		} 
		return keyNames ;		
	} // getKeyNames()
	
	// ------------------------------------------------------------------------

	void removeKey( String keyName )
	{
		KeyValuePair setting = null ;
		
		setting = this.getSettingNamed( keyName ) ;		
		if ( setting != null )
			this.getKeysAndValues().remove( setting ) ;
	} // removeKey()

	// ------------------------------------------------------------------------
	
	KeyValuePair getSettingNamed( String keyName )
	{
		Iterator list 					= null ;
		KeyValuePair setting	= null ;
		
		list = this.getKeysAndValues().iterator() ;
		while ( list.hasNext() )
		{
			setting = (KeyValuePair)list.next() ;
			if ( setting.matchesName( keyName ) )
				return setting ;
		} 
		return null ;
	} // getSettingNamed()		
		
	// ------------------------------------------------------------------------

	KeyValuePair createNewSetting( String aName )
	{
		return new KeyValuePair( aName, this.getCaseSensitive() ) ;
	} // createNewSetting()
	
	// ------------------------------------------------------------------------

} // class Category