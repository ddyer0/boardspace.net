// ===========================================================================
// CONTENT  : ABSTRACT CLASS GenericNamedObject
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
 *´Generic implementation for named objects
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
abstract class GenericNamedObject extends SettingsElement
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
	protected GenericNamedObject( String aName, boolean checkCase )
	{
		super( aName, checkCase ) ;
	} //  GenericNamedObject()

	// -------------------------------------------------------------------------

	protected GenericNamedObject( String aName, String aValue, boolean checkCase )
	{
		this( aName, checkCase ) ;
		this.setValue( aValue ) ;
	} //  GenericNamedObject()

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // ABSTRACT INSTANCE METHODS
  // =========================================================================
	abstract protected String getValue() ;
	abstract protected void setValue( String aValue ) ;

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void changeCaseSensitive( boolean isCaseSensitive ) 
	{
		this.setCaseSensitive( isCaseSensitive ) ;
	} // changeCaseSensitive()

	// -------------------------------------------------------------------------
		
	// Rendering for JOI 
	protected String inspectString()
	{
		return "Name=\"" + this.getName() + "\"\nValue=\"" + this.getValue() + "\"" ; 
	} // inspectString()

	// -------------------------------------------------------------------------
		
} // class GenericNamedObject
