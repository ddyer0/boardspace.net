// ===========================================================================
// CONTENT  : CLASS MapFacade
// AUTHOR   : M.Duchrow
// VERSION  : 1.1 - 08/07/2004
// HISTORY  :
//  21/05/2004  mdu  CREATED
//	08/08/2004	mdu		bugfix	-->	Clear attrNames when new wrappedObject is set
//
// Copyright (c) 2004, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.osf ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.pf.reflect.AttributeReadAccess;
import org.pf.reflect.AttributeReadWriteAccess;
import org.pf.util.NamedValue;
import org.pf.util.OrderedSet;

/**
 * This is a wrapper class around AttributeReadAccess objects. It provides 
 * the Map interface to allow generic access to the attributes of such an 
 * object.
 *
 * @author M.Duchrow
 * @version 1.1
 */
public class MapFacade implements Map
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private AttributeReadAccess wrappedObject = null ;
  protected AttributeReadAccess getWrappedObject() { return wrappedObject ; }
  protected void setWrappedObject( AttributeReadAccess newValue ) { wrappedObject = newValue ; }
  
  private String[] attrNames = null ;
  protected String[] getAttrNames() { return attrNames ; }
  protected void setAttrNames( String[] newValue ) { attrNames = newValue ; }  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with an object to apply the Map interface on.
   * 
   * @throws IllegalArgumentException If the given object is null
   */
  public MapFacade( AttributeReadAccess object )
  {
    super() ;
    this.setObject( object ) ;
  } // MapFacade() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Replaces the underlying object. The given object must not be null.
   * 
   * @param object The object to be wrapped by this MapFacade
   * @throws IllegalArgumentException If the given object is null
   */
  public void setObject( AttributeReadAccess object ) 
	{
    if ( object == null )
		{
			throw new IllegalArgumentException( "Object must not be null!" ) ;
		}
    this.setWrappedObject( object ) ;
    this.setAttrNames( null ) ; 
	} // setObject()

	// -------------------------------------------------------------------------
  
	/**
	 * <b>NOT SUPPORTED</b>
	 * @see java.util.Map#clear()
	 */
	public void clear()
	{
		throw new UnsupportedOperationException() ;
	} // clear() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey( Object key )
	{
		String[] names ;
		
		if ( key != null )
		{
			names = this.getAttributeNames() ;
			for (int i = 0; i < names.length; i++ )
			{
				if ( names[i].equals( key ) )
					return true ;
			}
		}
		return false;
	} // containsKey() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue( Object value )
	{
		Collection coll ;
		
		coll = this.values() ;
		return coll.contains( value ) ;
	} // containsValue() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet()
	{
		NamedValue entry ;
		Set entrySet ;
		String[] names ;
		
		names = this.getAttributeNames() ;
		entrySet = this.newSet( names.length ) ;
		for (int i = 0; i < names.length; i++ )
		{
			entry = new NamedValue( names[i], this.get( names[i] ) ) ;
			entrySet.add( entry ) ;
		}
		return entrySet ;
	} // entrySet() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get( Object key )
	{
		if ( key instanceof String )
		{
			try
			{
				return this.getWrappedObject().getAttributeValue( (String)key ) ;
			}
			catch ( NoSuchFieldException e )
			{
				// Ignore - returning null is sufficient here
			}
		}
		return null;
	} // get() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty()
	{
		return this.getAttributeNames().length < 1 ;
	} // isEmpty() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#keySet()
	 */
	public Set keySet()
	{
		Set keys ;
		String[] names ;
		
		names = this.getAttributeNames() ;
		keys = this.newSet( names.length ) ;
		for (int i = 0; i < names.length; i++ )
		{
			keys.add( names[i] ) ;
		}
		return keys ;
	} // keySet() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put( Object key, Object value )
	{
		Object oldValue ;
		
		if ( ( key instanceof String ) && ( value != null ) )
		{
			if ( this.allowsWriteAccess() )
			{
				try
				{
					oldValue = this.get( key ) ;
					this.getWriteObject().setAttributeValue( (String)key, value ) ;
					return oldValue ;
				}
				catch ( NoSuchFieldException e )
				{
					// Ignore
				}
			}
		}
		return null;
	} // put() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll( Map map )
	{
		Iterator iter ;
		Map.Entry entry ;
		
		if ( map != null )
		{
			iter = this.entrySet().iterator() ;
			while ( iter.hasNext() )
			{
				entry = (Map.Entry)iter.next();
				this.put( entry.getKey(), entry.getValue() ) ;
			}
		}
	} // putAll() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * <b>NOT SUPPORTED</b>
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove( Object key )
	{
		throw new UnsupportedOperationException() ;
	} // remove() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#size()
	 */
	public int size()
	{
		return this.getAttributeNames().length ;
	} // size() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.util.Map#values()
	 */
	public Collection values()
	{
		Collection coll ;
		String[] names ;
		
		names = this.getAttributeNames() ;
		coll = this.newCollection( names.length ) ;
		for (int i = 0; i < names.length; i++ )
		{
			coll.add( this.get( names[i] ) ) ;
		}
		return coll ;
	} // values() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected String[] getAttributeNames() 
	{
		String[] names ;
		
		if ( this.getAttrNames() == null )
		{
			names = this.getWrappedObject().getAttributeNames() ;
			if ( this.cacheAttributeNames() )
			{
				this.setAttrNames( names ) ;
			}
			else
			{
				return names ;
			}
		}
		return this.getAttrNames() ;
	} // getAttributeNames() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the wrapped object also provides methods for write
	 * access to its attributes.
	 */
	protected boolean allowsWriteAccess() 
	{
		return this.getWrappedObject() instanceof AttributeReadWriteAccess ;
	} // allowsWriteAccess() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the wrapped object a an AttributeReadWriteAccess
	 */
	protected AttributeReadWriteAccess getWriteObject() 
	{
		return (AttributeReadWriteAccess)this.getWrappedObject() ;
	} // getWriteObject()

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the attribute names of the underlying object should be
	 * cached.
	 */
	protected boolean cacheAttributeNames() 
	{
		return true ;
	} // cacheAttributeNames() 

	// -------------------------------------------------------------------------
	
	protected Collection newCollection( int initialCapacity ) 
	{
		return new ArrayList( initialCapacity ) ;
	} // newCollection() 

	// -------------------------------------------------------------------------
	
	protected Set newSet( int initialCapacity ) 
	{
		return new OrderedSet( initialCapacity ) ;
	} // newSet() 

	// -------------------------------------------------------------------------

} // class MapFacade 
