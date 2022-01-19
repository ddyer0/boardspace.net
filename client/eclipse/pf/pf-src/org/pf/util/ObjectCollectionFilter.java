// ===========================================================================
// CONTENT  : CLASS ObjectCollectionFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 15/01/2012
// HISTORY  :
//  27/05/2005  mdu  CREATED
//	15/01/2012	mdu		changed	-->	to generic type
//
// Copyright (c) 2005-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * This filter returns tru if a given object is contained in the
 * underlying collection of objects.
 * It is possible to specify whether to use equality or identity to determine
 * if an object is in the collection. The default is equality check.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class ObjectCollectionFilter<T> implements ObjectFilter<T>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final boolean DEFAULT_USE_IDENTITY = false ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Collection<T> objectCollection = null ;
  protected Collection<T> getObjectCollection() { return objectCollection ; }
  protected void setObjectCollection( Collection<T> newValue ) { objectCollection = newValue ; }
  
  private boolean useIdentity = DEFAULT_USE_IDENTITY ;
  protected boolean getUseIdentity() { return useIdentity ; }
  protected void setUseIdentity( boolean newValue ) { useIdentity = newValue ; }
  
  private boolean negated = false ;
  protected boolean getNegated() { return negated ; }
  protected void setNegated( boolean newValue ) { negated = newValue ; }
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with some objects.
   * The default comparison method is equality.
   * 
   * @param objects The object to keep in the filter
   */
	public ObjectCollectionFilter(Collection<T> objects)
	{
		super();
		if (objects == null)
		{
			this.setObjectCollection(this.newCollection(0));
		}
		else
		{
			this.setObjectCollection(this.newCollection(objects.size()));
			this.getObjectCollection().addAll(objects);
		}
	} // ObjectCollectionFilter() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with some objects.
	 * 
	 * @param objects The object to keep in the filter
	 * @param identity If true an identity comparison is done rather than equality check
	 */
	public ObjectCollectionFilter(Collection<T> objects, boolean identity)
	{
		this(objects);
		this.setUseIdentity(identity);
	} // ObjectCollectionFilter() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with some objects.
	 * The default comparison method is equality.
	 * 
	 * @param objects The object to keep in the filter
	 */
	public ObjectCollectionFilter(T[] objects)
	{
		super();
		if (objects == null)
		{
			this.setObjectCollection(this.newCollection(0));
		}
		else
		{
			this.setObjectCollection(CollectionUtil.current().toList(objects));
		}
	} // ObjectCollectionFilter() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with some objects.
	 * 
	 * @param objects The object to keep in the filter
	 * @param identity If true an identity comparison is done rather than equality check
	 */
	public ObjectCollectionFilter(T[] objects, boolean identity)
	{
		this(objects);
		this.setUseIdentity(identity);
	} // ObjectCollectionFilter() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns true if the given object is in the collection
	 */
	public boolean matches(T object)
	{
		Iterator<T> iter;
		T element;
		boolean matched = false;

		if (this.getObjectCollection() != null)
		{
			iter = this.getObjectCollection().iterator();
			while (iter.hasNext())
			{
				element = iter.next();
				if (this.getUseIdentity())
				{
					if (element == object)
					{
						matched = true;
					}
				}
				else
				{
					if (element == null)
					{
						if (object == null)
						{
							matched = true;
						}
					}
					else
					{
						if (element.equals(object))
						{
							matched = true;
						}
					}
				}
			}
		}
		return this.getNegated() ? !matched : matched;
	} // matches()

	// -------------------------------------------------------------------------

	/**
	 * This method reverses the logic of the filter.
	 * That is, for all objects matches() returned true before, now matches()
	 * returns false and vice versa.
	 */
	public void negate()
	{
		this.setNegated(!this.getNegated());
	} // negate()

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected Collection<T> newCollection(int initialCapacity)
	{
		return new ArrayList<T>(initialCapacity);
	} // newCollection()

	// -------------------------------------------------------------------------

} // class ObjectCollectionFilter 
