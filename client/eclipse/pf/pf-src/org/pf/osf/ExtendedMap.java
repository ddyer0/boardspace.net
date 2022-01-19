// ===========================================================================
// CONTENT  : CLASS ExtendedMap
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 22/05/2004
// HISTORY  :
//  22/05/2004  mdu  CREATED
//
// Copyright (c) 2004, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.osf ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Map;

import org.pf.reflect.AttributeReadWriteAccess;
import org.pf.util.MapWrapper;

/**
 * Provides map wrapper that additionally implements the interface
 * AttributeReadWriteAccess to enable OSF generic search functionality on
 * the inner (wrapped) map.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class ExtendedMap extends MapWrapper<String, Object> implements AttributeReadWriteAccess
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ExtendedMap()
  {
    super() ;
  } // ExtendedMap() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an inner map.
   * 
   * @param map The map this map wrapper is based on
   */
	public ExtendedMap(Map<String, Object> map)
	{
		super(map);
	} // ExtendedMap() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * @see org.pf.reflect.AttributeReadWriteAccess#setAttributeValue(java.lang.String, java.lang.Object)
	 */
	public void setAttributeValue(String name, Object value) throws NoSuchFieldException
	{
		this.put(name, value);
	} // setAttributeValue()

	// -------------------------------------------------------------------------

	/**
	 * @see org.pf.reflect.AttributeReadAccess#getAttributeNames()
	 */
	public String[] getAttributeNames()
	{
		String[] names;

		names = new String[this.size()];
		this.keySet().toArray(names);
		return names;
	} // getAttributeNames()

	// -------------------------------------------------------------------------

	/**
	 * @see org.pf.reflect.AttributeReadAccess#getAttributeValue(java.lang.String)
	 */
	public Object getAttributeValue(String name) throws NoSuchFieldException
	{
		return this.get(name);
	} // getAttributeValue()

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================

} // class ExtendedMap 
