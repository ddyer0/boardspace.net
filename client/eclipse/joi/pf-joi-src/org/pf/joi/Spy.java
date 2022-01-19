// ===========================================================================
// CONTENT  : CLASS Spy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 09/04/2004
// HISTORY  :
//  17/11/1999  duma  CREATED
//  26/06/2000  duma  added   -> renderRegistry
//	23/02/2003	duma	changed	-> Use org.pf.plugin.ClassAssociations now and load from 'joi.renderer'
//	09/04/2004	duma	changed	-> Added objectAsComponent(), getValueComponent(), isEditable()
//
// Copyright (c) 1999-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;

import org.pf.reflect.ClassInfo;
import org.pf.reflect.Dynamic;

/**
 * This is the abstract superclass of all wrapper classes, that are holding
 * inspected objects.
 * It provides the API an inspector can use to display internal information
 * of the inspected object.
 *
 * @author Manfred Duchrow
 * @version 1.3
 */
public abstract class Spy
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
   * The filename that will be looked up in each classpath element to load
   * renderer classes automatically in the registry of JOI.  <br>
   * <b>"META-INF/joi.renderer"</b>
   */
	public static final String RENDERER_MAPPING_FILENAME = "META-INF/joi.renderer" ;
	public static final String RENDERER_MAPPING_FILENAME_CLASSLOADER = "/META-INF/joi-cl.renderer" ;
	public static final String RENDERER_MAPPING_FILENAME_ALL = "/META-INF/joi-all.renderer" ;

	// -------------------------------------------------------------------------
	
  protected static final String PRIMITIVE_INT = "int";
	protected static final String PRIMITIVE_LONG = "long";
	protected static final String PRIMITIVE_BOOLEAN = "boolean" ;
	protected static final String PRIMITIVE_DOUBLE = "double" ;
	protected static final String PRIMITIVE_FLOAT = "float" ;
	protected static final String PRIMITIVE_SHORT = "short" ;
	protected static final String PRIMITIVE_BYTE = "byte" ;
	protected static final String PRIMITIVE_CHAR = "char" ;
	
  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static ClassAssociations<ObjectRenderer> rendererRegistry = null ;
  protected static ClassAssociations<ObjectRenderer> getRendererRegistry() { return rendererRegistry ; }
  protected static void setRendererRegistry( ClassAssociations<ObjectRenderer> newValue ) { rendererRegistry = newValue ; }
  
  // =========================================================================
  // PRIVATE CLASS METHODS
  // =========================================================================
  private static void initializeRendererRegistry()
  {  	
    setRendererRegistry( new ClassAssociations( ObjectRenderer.class ) ) ;
    CommonFunctions.loadPluginDefinitions(getRendererRegistry(), RENDERER_MAPPING_FILENAME, 
    		RENDERER_MAPPING_FILENAME_CLASSLOADER, RENDERER_MAPPING_FILENAME_ALL);
  } // initializeRendererRegistry() 

	// -------------------------------------------------------------------------

  private static ClassAssociations rendererRegistry()
  {
	  if ( getRendererRegistry() == null )
	  {
	    initializeRendererRegistry() ;
	  }
  	return getRendererRegistry() ;
  } // rendererRegistry() 

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public Spy()
  {
  	super() ;
  } // Spy() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Returns the name of the inspected object
   */
	abstract public String getName() ;

	// --------------------------------------------------------------------------

	/**
	 * Returns the type of the holded object.
	 */
	abstract public Class getType() ;

	// -------------------------------------------------------------------------

	/**
	 * Returns the value of the holded object.
	 */
	abstract public Object getValue()
		throws Exception ;

	// -------------------------------------------------------------------------

	/**
	 * Returns the declaration modifiers of the holded object.
	 *
	 * @see java.lang.reflect.Modifier
	 */
	abstract public int getModifiers() ;

	// -------------------------------------------------------------------------

	/**
	 * Returns the string representation of the holded object's value.
	 */
	public String getValueString()
		throws Exception
	{
		Object value		        = null ;

		value = this.getValue() ;
		return this.objectAsString( value ) ;
	} // getValueString() 

	// --------------------------------------------------------------------------

	/**
	 * Returns the visual component representation of the holded object's value
	 * or null if such a visual representation is not available.
	 */
	public Component getValueComponent()
		throws Exception
	{
		Object value ;

		value = this.getValue() ;
		return this.objectAsComponent( value ) ;
	} // getValueComponent() 

	// --------------------------------------------------------------------------

	/**
	 * Returns the string representation of the holded object's type (class).
	 */
	public String getTypeString()
	{
		return this.getTypeStringOf( this.getType() ) ;
	} // getTypeString() 

	// --------------------------------------------------------------------------

	/**
	 * Returns the string representation of the holded object value's type (class).
	 */
	public String getValueTypeString()
		throws Exception
	{
		Object value 			= null ;
		String typeString	= null ;

		value = this.getValue() ;
		if ( value == null )
		{
			typeString = "---" ;
		}
		else
		{
			if ( this.getType().isPrimitive() )
			{
				typeString = this.getType().getName() ;
			}
			else
			{
				typeString = this.getTypeStringOf( value.getClass() ) ;
			}
		}
		return typeString ;
	} // getValueTypeString() 

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the value of the underlying object is null
	 */
	public boolean valueIsNull()
    throws Exception
	{
		return ( this.getValue() == null ) ;
  } // valueIsNull() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if this spy is an element spy.
	 * Subclasses may override this to return true ;
	 */
	public boolean isElementSpy() 
	{
		return false ;
	} // isElementSpy()

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the spyed object is a primitive type (e.g. int, boolean)
	 */
	public boolean isPrimitive() 
	{
		return this.getType().isPrimitive() ;
	} // isPrimitive()

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the value of this spy can be modified.
	 * This method always returns false. Subclasses my override this method to
	 * return true if appropriate.
	 */
	public boolean isEditable() 
	{
		return false ;
	} // isEditable() 

	// -------------------------------------------------------------------------	
	
	/**
	 * Sets the given object as the new value of the spy's inspected object
	 * Here this method does nothing. Subclasses may override it to implement
	 * the actual modification.
	 * This method should only be overriden if isEditable() is also overriden
	 * to return true!
	 */
	public void setValue( Object newValue ) 
		throws Exception
	{
		// Nothing to do here in the abstract class
	} // setValue() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the type of the inspected object is int.
	 */
	public boolean is_int() 
	{
		return PRIMITIVE_INT.equals( this.getType().getName() ) ;
	} // is_int() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the type of the inspected object is long.
	 */
	public boolean is_long() 
	{
		return PRIMITIVE_LONG.equals( this.getType().getName() ) ;
	} // is_long() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the type of the inspected object is short.
	 */
	public boolean is_short() 
	{
		return PRIMITIVE_SHORT.equals( this.getType().getName() ) ;
	} // is_short() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the type of the inspected object is byte.
	 */
	public boolean is_byte() 
	{
		return PRIMITIVE_BYTE.equals( this.getType().getName() ) ;
	} // is_byte() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the type of the inspected object is char.
	 */
	public boolean is_char() 
	{
		return PRIMITIVE_CHAR.equals( this.getType().getName() ) ;
	} // is_char() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the type of the inspected object is boolean.
	 */
	public boolean is_boolean() 
	{
		return PRIMITIVE_BOOLEAN.equals( this.getType().getName() ) ;
	} // is_boolean() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the type of the inspected object is double.
	 */
	public boolean is_double() 
	{
		return PRIMITIVE_DOUBLE.equals( this.getType().getName() ) ;
	} // is_double() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the type of the inspected object is float.
	 */
	public boolean is_float() 
	{
		return PRIMITIVE_FLOAT.equals( this.getType().getName() ) ;
	} // is_float() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected String getTypeStringOf( Class type )
	{
		String typeString 	= null ;

		if ( type == null )
		{
			typeString = "---" ;
		}
		else
		{
			if ( type.isArray() )
			{
				type = type.getComponentType() ;
				typeString = type.getName() + "[]" ;
			}
			else
			{
				typeString = type.getName() ;
			}
		}
		return typeString ;
	} // getTypeStringOf() 

	// --------------------------------------------------------------------------

  protected ObjectRenderer rendererFor( Object object )
  {
  	ClassInfo classInfo ;
  	
  	classInfo = rendererRegistry().findForClassOfObject( object.getClass(), object ) ;
  	if ( classInfo == null )
		{
			return null ;
		}
		return (ObjectRenderer)classInfo.createInstance() ;
  } // rendererFor() 

	// --------------------------------------------------------------------------

	/**
	 * Returns the string representation of the given object.
	 */
	protected String objectAsString( Object object )
	{
		String result		        = null ;
		ObjectRenderer renderer = null ;

		if ( object == null )
		{
			result = "null" ;
		}
		else
		{
			if ( object instanceof Inspectable )
			{
				result = ((Inspectable)object).inspectString() ;
			}
			else
		  {
				renderer = this.rendererFor( object ) ;
				if ( renderer != null )
				{
					result = renderer.inspectString( object ) ; 
				}
				else
				{
					result = this.invokeInspectString( object ) ;
					if ( result == null )
					{
		  			result = object.toString() ;
					}
				}
	  	}
		}
		return result ;
	} // objectAsString() 
	
  // --------------------------------------------------------------------------

	/**
	 * Returns the visual component representation of the given object.
	 * If no such representation is available it returns null.
	 */
	protected Component objectAsComponent( Object object )
	{
		Component result = null ;
		ObjectRenderer renderer ;
		ObjectRenderer2 renderer2 ;

		if ( object == null )
		  return null ;

		renderer = this.rendererFor( object ) ;
		if ( renderer instanceof ObjectRenderer2 )
		{
			renderer2 = (ObjectRenderer2)renderer ;
			result = renderer2.inspectComponent( object ) ; 
		}
		else
		{
			// TODO implement invokeInspectComponent()
			/*
			result = this.invokeInspectComponent( object ) ;
			*/
		}
		return result ;
	} // objectAsComponent() 
	
  // --------------------------------------------------------------------------

	protected String invokeInspectString( Object object )
	{
		Object result = null ;
		
		try
		{
			result = Dynamic.perform( object, "inspectString" ) ; 
			if ( ! ( result instanceof String ) )
			{
				result = null ;
			}
		}
		catch (Throwable e)
		{
		}
		return (String)result ;
	} // invokeInspectString() 

	// -------------------------------------------------------------------------

} // class Spy 
