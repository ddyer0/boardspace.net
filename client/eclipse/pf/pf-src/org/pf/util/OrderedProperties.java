// ===========================================================================
// CONTENT  : CLASS OrderedProperties
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/09/2004
// HISTORY  :
//  17/09/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

/**
 * Represents a properties collection with empty lines and comments.
 * The order will be preserved.
 * <b>DO NOT USE THE NORMAL METHODS INHERITED FROM java.util.Hashtable
 * except those that are explicitly overridden in this class!</b>
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class OrderedProperties extends Properties
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final int INITIAL_CAPACITY		= 50 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private List elements = null ;
  protected List getElements() { return elements ; }
  protected void setElements( List newValue ) { elements = newValue ; }
  
  private Map propertyIndex = null ;
  protected Map getPropertyIndex() { return propertyIndex ; }
  protected void setPropertyIndex( Map newValue ) { propertyIndex = newValue ; } 
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public OrderedProperties()
  {
    this( INITIAL_CAPACITY ) ;
  } // OrderedProperties() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with an initial capacity.
   */
  public OrderedProperties( int initialCapacity )
  {
    super() ;
    this.setElements( new ArrayList( initialCapacity ) ) ;
    this.setPropertyIndex( new HashMap( initialCapacity ) ) ;
  } // OrderedProperties() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns an array containing the properties names in the order they 
	 * have been added or read from a stream.
	 */
	public String[] getPropertyNames()
	{
		List names ;
		Iterator iter ;
		NamedText property ;
		
		names = new ArrayList(this.size()) ;
		iter = this.getElements().iterator() ;
		while( iter.hasNext() )
		{
			property = (NamedText)iter.next();
			if ( this.isValidProperty( property ) )
			{
				names.add( property.name() ) ;
			}
		}
		return (String[])names.toArray( new String[names.size()] ) ; 
	} // getPropertyNames() 
	
	// -------------------------------------------------------------------------
	
  /**
   * Returns how many elements are contained.
   */
  public int size() 
	{
		return this.getElements().size() ;
	} // size() 

	// -------------------------------------------------------------------------

  /**
   * Returns the value of the property with the given name or null if it is
   * not found.
   * 
   * @param name The name of the property to look for
   */
  public String getProperty( String name ) 
	{
		NamedText property ;
		
		property = this.findProperty( name ) ;
		if ( this.isNullOrDeleted( property ) )
		{
			return null ;
		}
		else
		{
			return property.text() ;
		}
	} // getProperty() 

	// -------------------------------------------------------------------------
  
  /**
   * Modifies the value or adds the property specified by the given name.
   * Returns true the previous value if the property was found and modified 
   * or null if the property was added.
   * 
   * @param name The name of the property to set
   * @param value The new value to be set
   */
  public Object setProperty( String name, String value ) 
	{
		NamedText property ;
		String oldValue ;
		
		property = this.findProperty( name ) ;
		if ( property == null )
		{
			this.appendProperty( name, value ) ;
			return null ;
		}
		oldValue = property.text() ;
		property.text( value ) ;
		return oldValue ;
	} // setProperty() 

	// -------------------------------------------------------------------------
  
  /**
   * Deletes the property with the specified name.
   * Returns true if the property was deleted, otherwise false.
   * 
   * @param name The name of the property to delete
   */
  public boolean deleteProperty( String name ) 
	{
		return this.remove( name ) != null ;
	} // deleteProperty() 

	// -------------------------------------------------------------------------

  /**
   * Removes the key (and its corresponding value) from this hashtable. 
   * This method does nothing if the key is not found.
   * 
   * @return the value to which the key had been mapped, or null if the key did not have a mapping.
   */
  public Object remove( Object key ) 
	{
  	NamedText property ;
  	
  	if ( ! ( key instanceof String ) )
  		return null ;
  	
  	property = this.findProperty( (String)key ) ;
  	if ( property == null )
		{
			return null ;
		}
  	this.getPropertyIndex().remove( property.name() ) ;
  	this.getElements().remove( property ) ;
  	return property.text() ;
	} // remove() 

	// -------------------------------------------------------------------------
  
  /**
   * Searches for the property with the specified key in this property list. 
   * If the key is not found in this property list the method returns the 
   * default value argument.
   */
	public String getProperty( String key, String defaultValue )
	{
  	NamedText property ;
  	  	
  	property = this.findProperty( key ) ;
  	if ( property == null )
		{
			return defaultValue ;
		}
  	return property.text() ;
	} // getProperty() 

	// -------------------------------------------------------------------------
	
	/**
	 * NOT SUPPORTED METHOD!
	 * @throws UnsupportedOperationException
	 */
	public void list( PrintStream out )
	{
		throw new UnsupportedOperationException( this.getClass().getName() + ".list()" ) ;
	} // list() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * NOT SUPPORTED METHOD!
	 * @throws UnsupportedOperationException
	 */
	public void list( PrintWriter out )
	{
		throw new UnsupportedOperationException( this.getClass().getName() + ".list()" ) ;
	} // list() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * NOT SUPPORTED METHOD!
	 * @throws UnsupportedOperationException
	 */
	public synchronized void load( InputStream inStream ) throws IOException
	{
		throw new UnsupportedOperationException( this.getClass().getName() + ".load()" ) ;
	} // load() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns an enumeration of the properties names in the order they have been
	 * added or read from a stream.
	 * <p>
	 * <b>Be aware that the returned enumeration is NOT backed by this 
	 * OrderedProperties object!</b>  
	 */
	public Enumeration propertyNames()
	{
		Vector names ;
		String[] nameArray ;
		
		names = new Vector( this.size() ) ;
		nameArray = this.getPropertyNames() ;
		for( int i=0; i < nameArray.length; i++ )
		{
			names.add( nameArray[i] ) ;
		}
		return names.elements() ; 
	} // propertyNames() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * NOT SUPPORTED METHOD!
	 * @throws UnsupportedOperationException
	 */
	public synchronized void save( OutputStream out, String header )
	{
		throw new UnsupportedOperationException( this.getClass().getName() + ".save()" ) ;
	} // save() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * NOT SUPPORTED METHOD!
	 * @throws UnsupportedOperationException
	 */
	public synchronized void store( OutputStream out, String header )
		throws IOException
	{
		throw new UnsupportedOperationException( this.getClass().getName() + ".store()" ) ;
	} // store() 
	
	// -------------------------------------------------------------------------

	/**
	 * Removes all properties 
	 */
	public synchronized void clear()
	{
		this.getElements().clear();
		this.getPropertyIndex().clear() ;
	} // clear() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns all keys of this properties list in the order they have been added.
	 */
	public synchronized Enumeration keys()
	{
		return this.propertyNames() ;
	} // keys() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns all keys of this properties list in the order they have been added.
	 */
	public Set keySet()
	{
		Set keys ;
		String[] nameArray ;
		
		keys = new OrderedSet( this.size() ) ; 
		nameArray = this.getPropertyNames() ;
		for( int i=0; i < nameArray.length; i++ )
		{
			keys.add( nameArray[i] ) ;
		}
		return keys ;
	} // keySet() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns all entries in the order they have been added
	 */
	public Set entrySet()
	{
		OrderedMap entries ;
		Iterator iter ;
		NamedText element ;
		
		entries = new OrderedMap() ; 
		iter = this.getElements().iterator() ;
		while ( iter.hasNext() )
		{
			element = (NamedText)iter.next() ;
			if ( this.isValidProperty( element ) )
			{
				entries.put( element.name(), element.text() ) ;
			}
		}
		return entries.entrySet() ;
	} // entrySet()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Appends the property specified by the given name with the given value..
   * Returns true if the property was successfully added at the end, 
   * otherwise false.
   * 
   * @param name The name of the property to append
   * @param value The new value to be append
   */
  protected boolean appendProperty( String name, String value ) 
	{
		NamedText property ;
		
		property = this.newProperty( name, value ) ;
		this.addElement( property ) ;
		return true ;
	} // appendProperty() 

	// -------------------------------------------------------------------------
  
  /**
   * Create a new property object with the given name and value. Subclasses
   * may override this to implement their own property class.
   */
  protected NamedText newProperty( String name, String value )
	{
		return new NamedText( name, value );
	} // newProperty() 
  
  // -------------------------------------------------------------------------
  
	protected void addElement( NamedText element ) 
	{
  	Integer index ;
  	
  	index = Integer.valueOf( this.size() ) ;
		this.getElements().add( element ) ;
		this.registerProperty( element, index );
	} // addElement() 

	// -------------------------------------------------------------------------
  
 	protected void registerProperty( NamedText property, Integer index )
	{
		this.getPropertyIndex().put( property.name(), index ) ;
	} // registerProperty() 
 	
 	// -------------------------------------------------------------------------
 	
  protected NamedText findProperty( String key ) 
	{
		Integer index ;
		
		index = this.indexOfProperty( key ) ;
		if ( index == null )
		{
			return null ;
		}
		return this.propertyAt( index ) ;
	} // findProperty() 

	// -------------------------------------------------------------------------
  
  protected Integer indexOfProperty( String key ) 
	{
		return (Integer)this.getPropertyIndex().get( key ) ;
	} // indexOfProperty() 

	// -------------------------------------------------------------------------
  
  protected NamedText propertyAt( Integer index ) 
	{
		return this.propertyAt( index.intValue() ) ;
	} // propertyAt() 

	// ------------------------------------------------------------------------- 
  
  protected NamedText propertyAt( int index ) 
	{
		return (NamedText)this.getElements().get(index) ;
	} // propertyAt() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the given property is null or marked as deleted.
   * Subclasses may override this method to enhance behavior.
   * 
	 * @param property The property to check
	 * @return true if the given property is null or marked as deleted
	 */
	protected boolean isNullOrDeleted( NamedText property )
	{
		return ( property == null ) ;
	} // isNullOrDeleted() 
	
	// -------------------------------------------------------------------------
	
	protected boolean isValidProperty( NamedText property ) 
	{
		return true ;
	} // isValidProperty() 

	// -------------------------------------------------------------------------
	
} // class OrderedProperties 
