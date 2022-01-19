// ===========================================================================
// CONTENT  : CLASS ObjectOrMapAccessWrapper
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 13/11/2009
// HISTORY  :
//  13/11/2009  mdu  CREATED
//
// Copyright (c) 2009, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.reflect ;

import java.util.Map;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * A wrapper that is capable to read and write object fields via getter and
 * setter methods or directly. The access is possible for all visibilities.
 * However, if the underlying object is an implementor of the interface
 * java.util.Map then getter and setter method access is simulated to access
 * entries in the Map and attribute access by name is also done by accessing
 * Map entries.
 * <br>
 * The only constraint is, that the keys in the Map are all of type String.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ObjectOrMapAccessWrapper extends ObjectAccessWrapper
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private boolean isMap ;
  public boolean isMap() { return isMap ; }
  protected void setIsMap( boolean newValue ) { isMap = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with an object to wrap.
   * @param object The object to be accessed by this wrapper (must not be null)
   */
  public ObjectOrMapAccessWrapper( Object object )
  {
    super(object) ;
  } // ObjectOrMapAccessWrapper() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the names of all attributes that can be accessed by the
	 * method getAttributeValue().
	 * <br>
	 * If the underlying object is a Map then the names of all current entries 
	 * are returned.
	 */
  public String[] getAttributeNames()
  {
  	if ( this.isMap() )
		{
			return ReflectUtil.current().toStringArray( this.getMap().keySet(), "this" );
		}
  	return super.getAttributeNames();
  } // getAttributeNames()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the return value of the getter method of the given field name
   * or null in any case of reflection problem.
   * <br>
   * If the underlying object is a Map it returns the value of the entry
   * with the fieldName as key. If no such entry exists, null will be returned.
   */
  public Object get( final String fieldName ) 
	{
  	if ( this.isMap() )
		{
			return this.getMap().get( fieldName ) ;
		}
  	return super.get( fieldName );
	} // get() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Invokes the setter method of the given field name and passes the
   * specified value as parameter to it. 
   * <br>
   * If the underlying object is a Map it sets the value of the entry
   * with the fieldName as key. If no such entry exists, it will be created.
   */
  public void set( final String fieldName, final Object value ) 
	{
  	if ( this.isMap() )
		{
			this.getMap().put( fieldName, value );
		}
  	super.set( fieldName, value );
	} // set() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the current value of the attribute (field) with the given name.
	 *
	 * @param name The attribute's name ( case sensitive )
	 * @throws NoSuchFieldException If there is no attribute with the given name
	 */
  public Object getAttributeValue( String name ) throws NoSuchFieldException
  {
  	if ( this.isMap() )
		{
			if ( !this.getMap().containsKey( name ) )
			{
				throw new NoSuchFieldException( name ) ;
			}
			return this.get( name ) ;
		}
  	return super.getAttributeValue( name ) ;
  } // getAttributeValue() 
  
  // -------------------------------------------------------------------------
  
	/**
	 * Sets the current value of the attribute (field) with the given name.    <br>
	 *
	 * @param name The attribute's name ( case sensitive )
	 * @param value The value to be put into the attribute's 'slot'
	 * @throws NoSuchFieldException If there is no attribute with the given name
	 */
  public void setAttributeValue( String name, Object value ) throws NoSuchFieldException
  {
  	if ( this.isMap() )
		{
			if ( !this.getMap().containsKey( name ) )
			{
				throw new NoSuchFieldException( name ) ;
			}
			this.set( name, value );
			return;
		}
  	super.setAttributeValue( name, value ) ;
  } // setAttributeValue() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the value of the field with the given field name or
   * null in any case of reflection error.
   * <br>
   * If the underlying object is a Map then this method returns the value
   * of the map entry corresponding to the given name key.
   * If the map does not contain the given name then null will be returned.
   */
  public Object getValueOfField( final String name ) 
	{
  	if ( this.isMap() )
		{
			if ( this.getMap().containsKey( name ) )
			{
				return this.get( name ) ;
			}
			return null;
		}
		return super.getValueOfField( name );
	} // getValueOfField() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Modifies the field with the given name directly to the specified value
   * without calling the setter. 
   * <br>
   * If the underlying object is a Map then this method sets the value
   * of the map entry regardless whether or not the given name key already exists.
   */
  public void setValueOfField( final String name, final Object value ) 
  {
  	if ( this.isMap() )
		{
			this.set( name, value );
		}
  	super.setValueOfField( name, value );
  } // setValueOfField() 
  
  // -------------------------------------------------------------------------

  /**
   * Set the underlying object.
   * @param object The object to be accessed by this wrapper (must not be null)
   */
  public void setObject( Object object )
  {
  	super.setObject( object );
		this.setIsMap( object instanceof Map ) ;
  } // setObject() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected Map getMap() 
	{
		return (Map) this.getObject();
	} // getMap() 
	
	// -------------------------------------------------------------------------
  
} // class ObjectOrMapAccessWrapper 
