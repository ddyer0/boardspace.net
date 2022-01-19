// ===========================================================================
// CONTENT  : CLASS OrderedSet
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 22/02/2006
// HISTORY  :
//  12/03/2004  mdu  CREATED
//	19/02/2006	mdu		bugfix	->	removeAll()
//	22/02/2006	mdu		added		-> 	implements List
//
// Copyright (c) 2004-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * Implements a Set that keeps the order of its elements according to when
 * they were added. Additionally it implements the List interface.
 * So instances of this class can either be used as Set or as List.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class OrderedSet implements Set, List
{
	public Spliterator spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT);
    }
	// =========================================================================
	// CONSTANTS
	// =========================================================================

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private List elements = null ;
	protected List getElements() { return elements ; }
	protected void setElements( List newValue ) { elements = newValue ; }	

	// =========================================================================
	// CLASS METHODS
	// =========================================================================

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public OrderedSet()
	{
		this(20) ;
	} // OrderedSet() 
  
	// -------------------------------------------------------------------------
  
	/**
	 * Initialize the new set with an initial capacity.
	 */
	public OrderedSet( int initialCapacity )
	{
		super() ;
		this.setElements( this.createInnerList(initialCapacity) ) ;
	} // OrderedSet() 
  
	// -------------------------------------------------------------------------  

	/**
	 * Initialize the new set with an inner list.
	 */
	protected OrderedSet( List innerList )
	{
		super() ;
		this.setElements( innerList ) ;
	} // OrderedSet() 
	
	// -------------------------------------------------------------------------  
	
	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	public boolean add(Object obj)
	{
		if ( ! this.getElements().contains( obj ) )
		{
			return this.getElements().add( obj );
		}
		else
		{
			return false ;
		}
	} // add() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection coll)
	{
		boolean changed = false ;
		Iterator iter ;
		
		iter = coll.iterator() ;
		while ( iter.hasNext() )
		{
			if ( this.add( iter.next() ) )
			{
				changed = true ;
			}			
		}
		return changed ;
	} // addAll() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#clear()
	 */
	public void clear()
	{
		this.getElements().clear() ;
	} // clear() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	public boolean contains(Object obj)
	{
		return this.getElements().contains( obj ) ;
	} // contains() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection coll)
	{
		return this.getElements().containsAll( coll ) ;
	} // containsAll() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty()
	{
		return this.getElements().isEmpty() ;
	} // isEmpty() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#iterator()
	 */
	public Iterator iterator()
	{
		return this.getElements().iterator() ;
	} // iterator() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	public boolean remove(Object obj)
	{
		return this.getElements().remove( obj ) ;
	} // remove() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection coll)
	{
		return this.getElements().removeAll( coll ) ;
	} // removeAll() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection coll)
	{
		return this.getElements().retainAll( coll );
	} // retainAll() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#size()
	 */
	public int size()
	{
		return this.getElements().size() ;
	} // size() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#toArray()
	 */
	public Object[] toArray()
	{
		return this.getElements().toArray() ;
	} // toArray() 

	// -------------------------------------------------------------------------

	/**
	 * @see java.util.Collection#toArray(java.lang.Object[])
	 */
	public Object[] toArray(Object[] array)
	{
		return this.getElements().toArray( array );
	} // toArray() 

	// -------------------------------------------------------------------------
	
	/**
	 * Inserts the specified element at the specified position in this list  
	 */
	public void add( int index, Object element )
	{
		if ( ! this.contains( element ) )
		{
			this.getElements().add( index, element ) ;
		}
	} // add() 
	
	// -------------------------------------------------------------------------
	
	/**
	 *  Inserts all of the elements in the specified collection into this list 
	 *  at the specified position if they are not yet in this list.
	 *  
	 *  @return true if this list changed as a result of the call.
	 */
	public boolean addAll( int index, Collection c )
	{
		Iterator iter ;
		boolean changed = false ;
		int insertIndex = index ;
		Object object ;
		
		iter = c.iterator() ;
		while ( iter.hasNext() )
		{
			object = iter.next() ;
			if ( ! this.contains( object ) )
			{
				this.add( insertIndex, object ) ;
				insertIndex++ ;
				changed = true ;
			}
		}
		return changed;
	} // addAll() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the element at the specified position in this list.
	 */
	public Object get( int index )
	{
		return this.getElements().get(index);
	} // get() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the index in this list of the first occurrence of the specified 
	 * element, or -1 if this list does not contain this element.
	 */
	public int indexOf( Object o )
	{
		return this.getElements().indexOf(o);
	} // indexOf() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the index in this list of the last occurrence of the specified 
	 * element, or -1 if this list does not contain this element.
	 */
	public int lastIndexOf( Object o )
	{
		return this.getElements().lastIndexOf(o);
	} // lastIndexOf() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns a list iterator of the elements in this list (in proper sequence).
	 */
	public ListIterator listIterator()
	{
		return this.getElements().listIterator() ;
	} // listIterator() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a list iterator of the elements in this list (in proper sequence), 
	 * starting at the specified position in this list.
	 */
	public ListIterator listIterator( int index )
	{
		return this.getElements().listIterator( index ) ;
	} // listIterator() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the object at the specified index
	 */
	public Object remove( int index )
	{
		return this.getElements().remove( index );
	} // remove() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the given element at the specified index if the element is not
	 * yet already in the collection!
	 */
	public Object set( int index, Object element )
	{
		if ( ! this.contains( element ) )
		{
			return this.getElements().set( index, element );
		}
		return this.get( index ) ;
	} // set() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a sub list containing the elements in the range of the defined
	 * indices. The result is also an OrderedSet.
	 * Contrary to the definition of this method in the List interface here
	 * the result list is NOT backed by this list.
	 * Tah means, any changes to the result list do not have any impact on
	 * this list!
	 */
	public List subList( int fromIndex, int toIndex )
	{
		List result ;
		
		result = this.createInnerList( toIndex - fromIndex ) ;
		for (int i = fromIndex; i < toIndex; i++ )
		{
			result.add( this.get(i) ) ;
		}
		return new OrderedSet( result );
	} // subList() 

	// -------------------------------------------------------------------------
	
	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected List createInnerList( int initialCapacity ) 
	{
		return new ArrayList(initialCapacity) ;
	} // createInnerList() 
	
	// -------------------------------------------------------------------------
	
} // class OrderedSet 
