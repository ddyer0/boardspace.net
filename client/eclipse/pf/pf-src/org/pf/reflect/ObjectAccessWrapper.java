// ===========================================================================
// CONTENT  : CLASS ObjectAccessWrapper
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 19/12/2008
// HISTORY  :
//  19/12/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.reflect ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.List;

/**
 * A wrapper that is capable to read and write object fields via getter and
 * setter methods or directly. The access is possible for all visibilities.   
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ObjectAccessWrapper implements AttributeReadWriteAccess
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================	
	protected static final String[] EMPTY_STRING_ARRAY = new String[0] ;
	protected static final ReflectUtil RU = ReflectUtil.current() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Object object ;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with an object.
   * @param object The object to be accessed by this wrapper (must not be null)
   */
  public ObjectAccessWrapper( Object object )
  {
    super() ;
    this.setObject( object ) ;
  } // ObjectAccessWrapper() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the return value of the getter method of the given field name
   * or null in any case of reflection problem.
   */
  public Object get( final String fieldName ) 
	{
		String getterName ;
		
		getterName = this.makeGetterName( fieldName ) ;
		try
		{
			return Dynamic.perform( this.getObject(), getterName ) ;
		}
		catch ( Exception e )
		{
			this.handleException( e );
			return null ;
		}
	} // get() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Invokes the setter method of the given field name and passes the
   * specified value as parameter to it. 
   */
  public void set( final String fieldName, final Object value ) 
	{
  	String setterName ;
  	
  	setterName = this.makeSetterName( fieldName ) ;
  	try
		{
			Dynamic.perform( this.getObject(), setterName, value ) ;
		}
		catch ( Exception e )
		{
			this.handleException(e) ;
		}
	} // set() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns the value of the field with the given field name or
   * null in any case of reflection error.
   */
  public Object getValueOfField( final String fieldName ) 
	{
  	try
		{
  		return this.getAttributeValue( fieldName ) ;
		}
		catch ( NoSuchFieldException e )
		{
			this.handleException( e ) ;
		}
		return null ;
	} // getValueOfField() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Modifies the field with the given name directly to the specified value
   * without calling the setter. 
   */
  public void setValueOfField( final String fieldName, final Object value ) 
  {
  	try
		{
  		this.setAttributeValue( fieldName, value ) ;
		}
		catch ( NoSuchFieldException e )
		{
			this.handleException( e ) ;
		}
  } // setValueOfField() 
  
  // -------------------------------------------------------------------------

	/**
	 * Returns the current value of the attribute (field) with the given name.
	 *
	 * @param name The attribute's name ( case sensitive )
	 * @throws NoSuchFieldException If there is no attribute with the given name
	 */
  public Object getAttributeValue( String name ) throws NoSuchFieldException
  {
  	return RU.getValueOf( this.getObject(), name ) ;
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
  	RU.setValueOf( this.getObject(), name, value ) ;
  } // setAttributeValue() 
  
  // -------------------------------------------------------------------------
  
	/**
	 * Returns the names of all attributes that can be accessed by the
	 * method getAttributeValue().
	 */
  public String[] getAttributeNames()
  {
  	List fields ;
  	
  	if ( this.getObject() == null )
		{
  		return EMPTY_STRING_ARRAY ;
		}
		fields = RU.getFieldsOf( this.getObject() );
		if ( fields == null )
		{
			return EMPTY_STRING_ARRAY ;
		}
		return ReflectUtil.current().toStringArray( fields, "getName" ) ; 
  } // getAttributeNames() 
  
  // -------------------------------------------------------------------------
  
  public Object getObject()
  {
  	return object;
  } // getObject() 

  // -------------------------------------------------------------------------
  
  /**
   * Set the underlying object.
   * @param object The object to be accessed by this wrapper (must not be null)
   */
  public void setObject( Object object )
  {
  	this.object = object;
  } // setObject() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected String makeGetterName( String fieldName ) 
	{
  	return this.makeAccessMethodName( "get", fieldName ) ;
	} // makeGetterName() 
	
	// -------------------------------------------------------------------------

  protected String makeSetterName( String fieldName ) 
  {
  	return this.makeAccessMethodName( "set", fieldName ) ;
  } // makeSetterName() 
  
  // -------------------------------------------------------------------------
  
  protected String makeAccessMethodName( final String prefix, final String fieldName ) 
	{
		StringBuffer methodName ;
		
		methodName = new StringBuffer( fieldName.length() + prefix.length() ) ;
		methodName.append( prefix ) ;
		methodName.append( fieldName.substring( 0, 1 ).toUpperCase() ) ;
		methodName.append( fieldName.substring( 1 ) ) ;
		return methodName.toString() ;
	} // makeAccessMethodName() 

	// -------------------------------------------------------------------------

  /**
   * Handles all exceptions that might occur due to reflection access.
   * Subclasses may override this method to do some logging.
   */
	protected void handleException( Exception e )
	{
		// Nothing to do here
	} // handleException() 

	// -------------------------------------------------------------------------
	
} // class ObjectAccessWrapper 
