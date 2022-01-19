// =======================================================================
// Title   : CLASS SettingsElement
// Author  : Manfred Duchrow
// Version : 1.1 - 11/01/2003
// History :
// 	01/10/1999 	duma  created
//	11/01/2003	duma	added		-> changeCaseSensitive()
//
// Copyright (c) 1999-2003, by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings.impl;

// ========================================================================
// IMPORT
// ========================================================================

abstract class SettingsElement
{
	// ========================================================================
	// INSTANCE VARIABLES
	// ========================================================================
  private boolean caseSensitive = true ;
  boolean getCaseSensitive() { return caseSensitive ; }
  void setCaseSensitive( boolean aValue ) { caseSensitive = aValue ; }
  
  private String name = null ;
  String getName() { return name ; }
  void setName( String aValue ) { name = aValue ; }

	// ========================================================================
	// CONSTRUCTORS
	// ========================================================================
	protected SettingsElement( String aName )
	{
		super() ;
		this.setName( aName ) ;
	} // SettingsElement()
 
	// -------------------------------------------------------------------------

	protected SettingsElement( String aName, boolean checkCase )
	{
		this( aName ) ;
		this.setCaseSensitive( checkCase ) ;
	} // SettingsElement()
 
	// -------------------------------------------------------------------------

	// ========================================================================
	// ABSTRACT INSTANCE METHODS
	// ========================================================================
	abstract void changeCaseSensitive( boolean isCaseSensitive ) ;

	// -------------------------------------------------------------------------
	
	// ========================================================================
	// PROTECTED INSTANCE METHODS
	// ========================================================================
	protected boolean matchesName( String aName )
	{
		if ( this.getCaseSensitive() )
			return this.getName().equals( aName ) ;
		else
			return this.getName().equalsIgnoreCase( aName ) ;
	} // matchesName()
 		
	// ------------------------------------------------------------------------	
	
} // class SettingsElement