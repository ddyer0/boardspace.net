// =======================================================================
// Title   : Class SettingsImpl
// Author  : Manfred Duchrow
// Version : 2.0 - 02/07/2003
// History :
// 	01/10/1999 	duma  created
//	26/05/2002	duma	moved		->	From org.pf.settings.Settings
//	18/12/2002	duma	added		->	name, defaultSettings and corresp. methods
//	11/01/2003	duma	added		->	Propagates "caseSensitive" to categories in setCaseSensitive()
//	02/07/2003	duma	changed	->	Moved most methods to new superclass GenericSettingsImpl
//
// Copyright (c) 1999-2003, by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings.impl;

// ========================================================================
// IMPORT
// ========================================================================

/**
 * An instance of this class holds a indefinite number of settings.
 * The structure is, that there is a category with any number of
 * key/value pairs inside.
 * A key is unique in one category but may occur several times in
 * different categories.
 * <br>
 * If a category of null is passed to any method, the default category
 * will be taken instead.
 * A SettingsWriter must treat the default category as not existent and write
 * all its key/value pairs directly without any category. The default 
 * category can be recognized by its empty name ("").
 * 
 * @author Manfred Duchrow
 * @version 2.0
 */
public class SettingsImpl extends GenericSettingsImpl
{
	// ========================================================================
	// INSTANCE VARIABLES
	// ========================================================================

	// ========================================================================
	// CONSTRUCTORS
	// ========================================================================

	/**
	 * Create a new instance.
	 */
	public SettingsImpl()
	{
		super() ;
	} // SettingsImpl()

	// -------------------------------------------------------------------------

	/**
	 * Create a new instance with the specified name.
	 */
	public SettingsImpl( String aName )
	{
		super( aName ) ;
	} // SettingsImpl()

	// -------------------------------------------------------------------------

	// ========================================================================
	// PUBLIC INSTANCE METHODS
	// ========================================================================

	// ========================================================================
	// PROTECTED INSTANCE METHODS
	// ========================================================================
	protected GenericCategory createNewCategory( String categoryName ) 
	{
		return new SingleValueCategory( categoryName, this.getCaseSensitive() ) ;
	} // createNewCategory()

	// ------------------------------------------------------------------------

} // class SettingsImpl