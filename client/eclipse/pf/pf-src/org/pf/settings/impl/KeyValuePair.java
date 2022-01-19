// =======================================================================
// Title   : CLASS KeyValuePair
// Author  : Manfred Duchrow
// Version : 2.1 - 02/07/2003
// History :
// 	01/10/1999 	duma  created
//	11/01/2003	duma	added		-> changeCaseSensitive()
//	02/07/2003	duma	changed	-> Moved nearly all methods to new superclass GenericNamedObject 
//	08/01/2009	mdu		added		-> toString()
//
// Copyright (c) 1999-2009 by Manfred Duchrow
// =======================================================================
package org.pf.settings.impl;

// ========================================================================
// IMPORT
// ========================================================================

/**
 * Helper class inside settings to represent a key/value pair.
 * 
 * @version 2.1
 */
class KeyValuePair extends GenericNamedObject
{
	// ========================================================================
	// INSTANCE VARIABLES
	// ========================================================================
  private String value = null ;
  protected String getValue() { return value ; }
  protected void setValue( String aValue ) { value = aValue ; }

	// ========================================================================
	// CONSTRUCTORS
	// ========================================================================
	KeyValuePair( String aName, boolean checkCase )
	{
		super( aName, checkCase ) ;
	} // KeyValuePair() 

	// ------------------------------------------------------------------------

	KeyValuePair( String aName, String aValue, boolean checkCase )
	{
		super( aName, aValue, checkCase ) ;
	} // KeyValuePair() 
	
	// ========================================================================
	// PUBLIC INSTANCE METHODS
	// ========================================================================
	public String toString() 
	{
		return "KeyValuePair(" + this.getName() + "->" + this.getValue() + ")" ;
	} // toString() 
	
	// -------------------------------------------------------------------------
	
} // class KeyValuePair 
