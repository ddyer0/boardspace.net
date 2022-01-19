// ===========================================================================
// CONTENT  : CLASS ArrayElementSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 10/04/2004
// HISTORY  :
//  24/11/1999  duma  CREATED
//	11/01/2000	duma	changed	->	Moved getModifiers() and matchFilter() to ElementSpy
//	11/01/2000	duma	changed	->	Superclass from ElementSpy to CollectionElementSpy
//	10/04/2004	duma	changed	->	constructor, added index 
//
// Copyright (c) 1999-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Instances of this class are holding information about the elements
 * of an inspected object.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class ArrayElementSpy extends CollectionElementSpy
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================      
  private Class type = null ;
  public Class getType() { return type ; }  
  protected void setType( Class aValue ) { type = aValue ; }  

  private int index = 0 ;
  protected int getIndex() { return index ; }
  protected void setIndex( int newValue ) { index = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ArrayElementSpy( AbstractObjectSpy container, int pos, 
  												Object object, Class type )
  {
  	super( container, Integer.toString(pos), object ) ; 
  	this.setType( type ) ;
  	this.setIndex( pos ) ;
  } // ArrayElementSpy() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the type of the contained element.
	 */
	protected Class getCurrentType()
	{
		return this.getType() ;
	} // getCurrentType() 

	// --------------------------------------------------------------------------

	protected void modifyValue( Object newValue )
		throws Exception
	{
		super.modifyValue( newValue ) ;
		this.setObject( newValue ) ;
	} // modifyValue() 
	
	// --------------------------------------------------------------------------
		
} // class ArrayElementSpy 
