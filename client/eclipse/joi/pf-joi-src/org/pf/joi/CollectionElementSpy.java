// ===========================================================================
// CONTENT  : CLASS CollectionElementSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 24/04/2004
// HISTORY  :
//	11/01/2000	duma	CREATED
//	27/01/2002	duma	added		->	modifiers
//	24/04/2004	duma	added		->	getElementType()
//
// Copyright (c) 2000-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Modifier;

/**
 * Superclass for spys that hold information about collection elements.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class CollectionElementSpy extends ElementSpy
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
 	private static final Class OBJECT_TYPE = new Object().getClass() ;
 
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Object object = null ;
	public Object getObject() { return object ; }
	protected void setObject( Object newValue ) { object = newValue ; }
	  
  private String name = null ;
  public String getName() { return name ; }  
  protected void setName( String aValue ) { name = aValue ; }  

  private int modifiers = Modifier.PRIVATE ;
  public int getModifiers() { return modifiers ; }
  public void setModifiers( int newValue ) { modifiers = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public CollectionElementSpy( AbstractObjectSpy container, String name, 
  															Object object )
  {
  	super( container ) ; 
  	this.setName( name ) ;
  	this.setObject( object ) ;
  } // CollectionElementSpy()  

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with default values.
   */
  public CollectionElementSpy( AbstractObjectSpy container, Object object )
  {
  	super( container ) ; 
  	this.setObject( object ) ;
  } // CollectionElementSpy()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the object that is wrapped by this array element holder.
	 */
	public Object getValue()
		throws Exception
	{
		return this.getObject() ;
	} // getValue()

  // -------------------------------------------------------------------------

	/**
	 * Returns the type of the entry values, which by default <i>Object</i>.
	 * Subclasses may override this.
	 */
	public Class getType()
	{
		return (OBJECT_TYPE);
	} // getType()

	// --------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the type of the contained element.
	 * Subclasses may override this.
	 */
	protected Class getCurrentType()
	{
		try
		{
			if ( this.getValue() == null )
				return (OBJECT_TYPE);

			return this.getValue().getClass();
		}
		catch ( Exception e )
		{
			return (OBJECT_TYPE);
		}
	} // getCurrentType()

	// --------------------------------------------------------------------------
  
} // class CollectionElementSpy 
