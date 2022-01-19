// =======================================================================
// Title   : CLASS SingleValueCategory
// Author  : Manfred Duchrow
// Version : 2.0 - 02/07/2003
// History :
// 	01/10/1999	duma  created
//	24/05/2002	duma	added		-> default category
//	11/01/2003	duma	added		-> changeCaseSensitive()
//	02/07/2003	duma	changed	-> renamed from Category and changed superclass to GenericCategory
//
// Copyright (c) 1999-2003, by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings.impl;

// ========================================================================
// IMPORT
// ========================================================================

/**
 * Helper class inside settings to represent a category or section.
 * @version 2.0
 */
class SingleValueCategory extends GenericCategory
{
	// ========================================================================
	// CONSTANTS
	// ========================================================================

	// ========================================================================
	// INSTANCE VARIABLES
	// ========================================================================

	// ========================================================================
	// CONSTRUCTORS
	// ========================================================================
	protected SingleValueCategory( String aName, boolean checkCase )
	{
		super( aName, checkCase ) ;
	} //  SingleValueCategory()

	// -------------------------------------------------------------------------
	
	// ========================================================================
	// PROTECTED INSTANCE METHODS
	// ========================================================================
	protected GenericNamedObject createNewSetting( String aName )
	{
		return new KeyValuePair( aName, this.getCaseSensitive() ) ;
	} // createNewSetting()
	
	// ------------------------------------------------------------------------

} // class SingleValueCategory