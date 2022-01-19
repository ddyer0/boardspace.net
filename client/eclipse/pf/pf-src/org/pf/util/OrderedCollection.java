// ===========================================================================
// CONTENT  : CLASS OrderedCollection
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 15/01/2012
// HISTORY  :
//  08/12/1999  duma  CREATED
//	21/01/2000	duma	MOVED		->	from package 'com.mdcs.util' to 'org.pf.util'
//  02/02/2000  duma  added   ->  at(), atPut()
//	15/01/2012	mdu		changed	->  to generic type
//
// Copyright (c) 1999-2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.*;

/**
 * A collection that supports similar methods to the list interface but it 
 * starts with index <b>1</b> and ends with element index <b>size()</b> !<br>
 * <strong>IT IS NOT ZERO BASED !</strong>
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class OrderedCollection<E> implements Collection<E>
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  private static final int BASE_DIFF = 1 ;
  	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private List<E> basicCollection = new ArrayList<E>() ;
	protected List<E> getBasicCollection() { return basicCollection ; }
	protected void setBasicCollection( List<E> newValue ) { basicCollection = newValue ; }
	  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
	public OrderedCollection()
	{
	} // OrderedCollection()  

	// --------------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a given initial capacity.
	 */
	public OrderedCollection(int initialCapacity)
	{
		this(new ArrayList<E>(initialCapacity));
	} // OrderedCollection()  

	// --------------------------------------------------------------------------------

	/**
	 * Initialize the new instance with default values.
	 */
	public OrderedCollection(List<E> list)
	{
		this.setBasicCollection(list);
	} // OrderedCollection()  

	// =========================================================================
	// PUBLIC INSTANCE METHODS MATCHING java.util.List interface
	// =========================================================================

	/**
	 * Returns the number of elements in this list. 
	 * If this list contains more than Integer.MAX_VALUE elements, 
	 * returns Integer.MAX_VALUE.
	 * 
	 * @return the number of elements in this list.
	 */
	public int size()
	{
		return this.getBasicCollection().size();
	} // size()

	// --------------------------------------------------------------------------------

	/**
	 * Returns true if this list contains no elements.
	 *
	 * @return true if this list contains no elements.
	 */
	public boolean isEmpty()
	{
		return this.getBasicCollection().isEmpty();
	} // isEmpty()

	// --------------------------------------------------------------------------------

	/**
	 * Returns true if this list contains the specified element. 
	 * More formally, returns true if and only if this list contains at least 
	 * one element e such that (o==null ? e==null : o.equals(e)).
	 *
	 * @param o - element whose presence in this list is to be tested.
	 */
	public boolean contains(Object o)
	{
		return this.getBasicCollection().contains(o);
	} // contains()

	// --------------------------------------------------------------------------------

	/**
	 * Returns an iterator over the elements in this list in proper sequence.
	 *
	 * @return an iterator over the elements in this list in proper sequence.
	 */
	public Iterator<E> iterator()
	{
		return this.getBasicCollection().iterator();
	} // iterator()

	// --------------------------------------------------------------------------------

	/**
	 * Returns an array containing all of the elements in this list in proper sequence. 
	 * Obeys the general contract of the Collection.toArray method.
	 */
	public Object[] toArray()
	{
		return this.getBasicCollection().toArray();
	} // toArray()

	// --------------------------------------------------------------------------------

	/**
	 * Returns an array containing all of the elements in this list in proper sequence; 
	 * the runtime type of the returned array is that of the specified array. 
	 * Obeys the general contract of the Collection.toArray(Object[]) method.
	 *
	 * @param a - the array into which the elements of this list are to be stored, 
	 * 						if it is big enough; otherwise, a new array of the same runtime type 
	 *						is allocated for this purpose.
	 * @throws ArrayStoreException - if the runtime type of the specified array is not 
	 * 					a supertype of the runtime type of every element in this list.
	 */
	public <T> T[] toArray(T[] a) throws ArrayStoreException
	{
		return this.getBasicCollection().toArray(a);
	} // toArray()

	// --------------------------------------------------------------------------------

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param o - element to be appended to this list.
	 * @return true (as per the general contract of the Collection.add method).
	 */
	public boolean add(E o)
	{
		return this.getBasicCollection().add(o);
	} // add()

	// --------------------------------------------------------------------------------

	/**
	 * Removes the first occurrence in this list of the specified element 
	 * (optional operation). If this list does not contain the element, 
	 * it is unchanged. More formally, removes the element with the lowest index i 
	 * such that (o==null ? get(i)==null : o.equals(get(i))) (if such an element exists).
	 *
	 * @param o - element to be removed from this list, if present.
	 * @return true if this list contained the specified element.
	 */
	public boolean remove(Object o)
	{
		return this.getBasicCollection().remove(o);
	} // remove()

	// --------------------------------------------------------------------------------

	/**
	 * Returns true if this list contains all of the elements of the specified collection.
	 *
	 * @param c - collection to be checked for containment in this list.
	 * @return true if this list contains all of the elements of the specified collection.
	 */
	public boolean containsAll(Collection<?> c)
	{
		return this.getBasicCollection().containsAll(c);
	} // containsAll()

	// --------------------------------------------------------------------------------

	/**
	 * Appends all of the elements in the specified collection to the end of this list, 
	 * in the order that they are returned by the specified collection's iterator 
	 * (optional operation). The behavior of this operation is unspecified if the 
	 * specified collection is modified while the operation is in progress. 
	 * (Note that this will occur if the specified collection is this list, and it's 
	 * nonempty.)
	 *
	 * @param c - collection whose elements are to be added to this list.
	 * @return true if this list changed as a result of the call.
	 */
	public boolean addAll(Collection<? extends E> c)
	{
		return this.getBasicCollection().addAll(c);
	} // addAll()

	// --------------------------------------------------------------------------------

	/**
	 * Inserts all of the elements in the specified collection into this list at the 
	 * specified position (optional operation). Shifts the element currently at that 
	 * position (if any) and any subsequent elements to the right (increases their indices). The new elements will appear in this list in the order that they are returned by the specified collection's iterator. The behavior of this operation is unspecified if the specified collection is modified while the operation is in progress. (Note that this will occur if the specified collection is this list, and it's nonempty.)
	 *
	 * @param index - index at which to insert first element from the specified collection.
	 * 								( 1 <= index <= size() )
	 * @param c - elements to be inserted into this list.
	 * @throws IndexOutOfBoundsException - if the index is out of range 
	 																			(index < 1 || index > size()).
	 */
	public boolean addAll(int index, Collection<? extends E> c) throws IndexOutOfBoundsException
	{
		return this.getBasicCollection().addAll(index - BASE_DIFF, c);
	} // addAll()

	// --------------------------------------------------------------------------------

	/**
	 * Removes from this list all the elements that are contained in the specified 
	 * collection.
	 * 
	 * @param c - collection that defines which elements will be removed from this list.
	 * @return true if this list changed as a result of the call.
	 */
	public boolean removeAll(Collection<?> c)
	{
		return this.getBasicCollection().removeAll(c);
	} // removeAll()

	// --------------------------------------------------------------------------------

	/**
	 * Retains only the elements in this list that are contained in the specified 
	 * collection. In other words, removes from this list all the elements that are 
	 * not contained in the specified collection.
	 *
	 * @param  c - collection that defines which elements this set will retain.
	 * @return true if this list changed as a result of the call.
	 */
	public boolean retainAll(Collection<?> c)
	{
		return this.getBasicCollection().retainAll(c);
	} // retainAll()

	// --------------------------------------------------------------------------------

	/**
	 * Removes all of the elements from this list. This list will be empty after this 
	 * call returns.
	 */
	public void clear()
	{
		this.getBasicCollection().clear();
	} // clear()

	// --------------------------------------------------------------------------------

	/**
	 * Compares the specified object with this list for equality. 
	 * Returns true if and only if the specified object is also a list, both lists 
	 * have the same size, and all corresponding pairs of elements in the two lists 
	 * are equal. (Two elements e1 and e2 are equal if (e1==null ? e2==null : e1.equals(e2)).) 
	 * In other words, two lists are defined to be equal if they contain the same 
	 * elements in the same order. This definition ensures that the equals method 
	 * works properly across different implementations of the List interface.
	 *
	 * @param o - the object to be compared for equality with this list. 
	 * @return true if the specified object is equal to this list.
	 */
	public boolean equals(Object o)
	{
		return this.getBasicCollection().equals(o);
	} // equals()

	// --------------------------------------------------------------------------------

	/**
	 * Returns the hash code value for this list. The hash code of a list is defined to be 
	 * the result of the following calculation: 
	 * hashCode = 1;<br>
	 * Iterator i = list.iterator();     <br>
	 * while (i.hasNext())      <br>
	 * {        <br>
	 *   Object obj = i.next();   <br>
	 *   hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());  <br>
	 * }
	 *
	 * This ensures that list1.equals(list2) implies that list1.hashCode()==list2.hashCode() 
	 * for any two lists, list1 and list2, as required by the general contract of 
	 * Object.hashCode.
	 *
	 * @return the hash code value for this list.
	 */
	public int hashCode()
	{
		return this.getBasicCollection().hashCode();
	} // hashCode()

	// --------------------------------------------------------------------------------

	/**
	 * Returns the element at the specified position in this list.
	 *
	 * @param index - index of element to return. ( 1 <= index <= size() )
	 * @return the element at the specified position in this list.
	 * @throws IndexOutOfBoundsException - if the index is out of range 
	 * 																		(index < 1 || index > size()).
	 */
	public E get(int index) throws IndexOutOfBoundsException
	{
		return this.getBasicCollection().get(index - BASE_DIFF);
	} // get()

	// --------------------------------------------------------------------------------

	/**
	 * Replaces the element at the specified position in this list with 
	 * the specified element.
	 *
	 * @param index - index of element to replace. ( 1 <= index <= size() )
	 * @param element - element to be stored at the specified position.
	 * @return the element previously at the specified position.
	 * @throws IndexOutOfBoundsException - if the index is out of range 
	 * 																		(index < 1 || index > size()).
	 */
	public E set(int index, E element) throws IndexOutOfBoundsException
	{
		return this.getBasicCollection().set(index - BASE_DIFF, element);
	} // set()

	// --------------------------------------------------------------------------------

	/**
	 * Inserts the specified element at the specified position in this list.
	 * Shifts the element currently at that position (if any) and any subsequent 
	 * elements to the right (adds one to their indices).
	 *
	 * @param index - index at which the specified element is to be inserted.
	 * @param element - element to be inserted.
	 * @throws IndexOutOfBoundsException - if the index is out of range 
	 * 																		(index < 1 || index > size()).
	 */
	public void add(int index, E element) throws IndexOutOfBoundsException
	{
		this.getBasicCollection().add(index - BASE_DIFF, element);
	} // add()

	// --------------------------------------------------------------------------------

	/**
	 * Removes the element at the specified position in this list. 
	 * Shifts any subsequent elements to the left (subtracts one from their indices). 
	 * Returns the element that was removed from the list.
	 *
	 * @param index - the index of the element to removed.
	 * @throws IndexOutOfBoundsException - if the index is out of range 
	 * 																		(index < 1 || index > size()).
	 */
	public E remove(int index)
	{
		return this.getBasicCollection().remove(index - BASE_DIFF);
	} // remove() 

	// --------------------------------------------------------------------------------

	/**
	 * Returns the index in this list of the first occurrence of the specified element, 
	 * or 0 if this list does not contain this element. More formally, returns the lowest 
	 * index i such that (o==null ? get(i)==null : o.equals(get(i))), or 0 if there is 
	 * no such index.
	 *
	 * @param o - element to search for.
	 * @return the index in this list of the first occurrence of the specified element, 
	 * 					or 0 if this list does not contain this element.
	 */
	public int indexOf(E o)
	{
		return (this.getBasicCollection().indexOf(o) + BASE_DIFF);
	} // indexOf()

	// --------------------------------------------------------------------------------

	/**
	 * Returns the index in this list of the last occurrence of the specified element, 
	 * or 0 if this list does not contain this element. 
	 * More formally, returns the highest index i such that 
	 * (o==null ? get(i)==null : o.equals(get(i))), or 0 if there is no such index.
	 *
	 * @param o - element to search for.
	 * @return the index in this list of the last occurrence of the specified element, 
	 * 					or 0 if this list does not contain this element.
	 */
	public int lastIndexOf(E o)
	{
		return (this.getBasicCollection().lastIndexOf(o) + BASE_DIFF);
	} // lastIndexOf()

	// --------------------------------------------------------------------------------

	/**
	 * Returns a list iterator of the elements in this list (in proper sequence).
	 */
	public ListIterator<E> listIterator()
	{
		return this.getBasicCollection().listIterator();
	} // listIterator()

	// --------------------------------------------------------------------------------

	/**
	 * Returns a list iterator of the elements in this list (in proper sequence), 
	 * starting at the specified position in this list. The specified index indicates 
	 * the first element that would be returned by an initial call to the nextElement 
	 * method. An initial call to the previousElement method would return the element 
	 * with the specified index minus one.
	 *
	 * @param index - index of first element to be returned from the list iterator 
	 									(by a call to the next method).
	 * @return a list iterator of the elements in this list (in proper sequence), 
	 *         starting at the specified position in this list.
	 * @throws IndexOutOfBoundsException - if the index is out of range 
	 * 																		(index < 1 || index > size()).
	 */
	public ListIterator<E> listIterator(int index) throws IndexOutOfBoundsException
	{
		return this.getBasicCollection().listIterator(index - BASE_DIFF);
	} // listIterator()

	// --------------------------------------------------------------------------------

	/**
	 * Returns a view of the portion of this list between the specified 
	 * fromIndex, <b>inclusive</b>, and toIndex, <b>inclusive</b>. 
	 * (If fromIndex and toIndex are equal, the returned list has <u>one</u> element.) 
	 * The returned list is a copy of this list, so changes in the returned list are 
	 * not reflected in this list, and vice-versa.
	 * This method eliminates the need for explicit range operations 
	 *
	
	 * @param fromIndex - low endpoint (inclusive) of the subList.
	 * @param toIndex - high endpoint (inclusive) of the subList.
	 * @return a view of the specified range within this list.
	 * @throws IndexOutOfBoundsException - for an illegal endpoint index value 
	                        (fromIndex < 1 || toIndex > size() || fromIndex > toIndex).
	 */
	public List<E> subList(int fromIndex, int toIndex) throws IndexOutOfBoundsException
	{
		List<E> sub = null;
		int index = BASE_DIFF;

		sub = new ArrayList<E>();

		if ((fromIndex < BASE_DIFF) || ((toIndex - BASE_DIFF) >= this.size()))
		{
			throw (new IndexOutOfBoundsException());
		}
		else
		{
			index = fromIndex;
			while (index <= toIndex)
			{
				sub.add(this.get(index));
			}
		}
		return sub;
	} // subList()

	// =========================================================================
	// PUBLIC INSTANCE METHODS / ADDITIONAL
	// =========================================================================

	/**
	 * Returns the element at the specified position in this list.
	 *
	 * @param index - index of element to return. ( 1 <= index <= size() )
	 * @return the element at the specified position in this list.
	 * @throws IndexOutOfBoundsException - if the index is out of range
	 * 																		(index < 1 || index > size()).
	 */
	public E at(int index) throws IndexOutOfBoundsException
	{
		return this.get(index);
	} // at()

	// --------------------------------------------------------------------------------

	/**
	 * Replaces the element at the specified position in this list with 
	 * the specified element.
	 *
	 * @param index - index of element to replace. ( 1 <= index <= size() )
	 * @param element - element to be stored at the specified position.
	 * @return the element previously at the specified position.
	 * @throws IndexOutOfBoundsException - if the index is out of range 
	 * 																		(index < 1 || index > size()).
	 */
	public E atPut(int index, E element) throws IndexOutOfBoundsException
	{
		return this.set(index, element);
	} // atPut()

	// --------------------------------------------------------------------------------

	/**
	 * Returns a copy of this collection in a Vector
	 *
	 * @see java.util.Vector
	 */
	public Vector<E> toVector()
	{
		return (new Vector<E>(this.getBasicCollection()));
	} // toVector()

	// -------------------------------------------------------------------------

	/**
	 * Loop over all the receiver's elements and call the given blocks eval() - method
	 * for each.
	 *
	 * @param aBlock The block to evaluate for each element.
	 * @return The result of the last block evaluation.
	 */
	public Object do_(Block1 aBlock)
	{
		Iterator iterator = null;
		Object result = null;

		iterator = this.iterator();
		while (iterator.hasNext())
		{
			result = aBlock.eval(iterator.next());
		}
		return result;
	} // do_()

	// -------------------------------------------------------------------------

	/**
	 * Loop over all the receiver's elements and call the given blocks eval() - method
	 * for each. Returns that element that first evaluates the block to Boolean.TRUE.
	 *
	 * @param aBlock The block to evaluate for each element.
	 * @return The result of the last block evaluation.
	 */
	public Object detect_(Block1 aBlock)
	{
		Iterator iterator = null;
		Boolean result = Boolean.FALSE;
		Object each = null;

		iterator = this.iterator();
		while ((!result.booleanValue()) && iterator.hasNext())
		{
			each = iterator.next();
			result = (Boolean)aBlock.eval(each);
		}
		if (result.booleanValue())
		{
			return each;
		}
		else
		{
			return null;
		}
	} // detect_()

	// -------------------------------------------------------------------------

	/**
	 * Returns the first element of the receiver.   <br>
	 * If the collection is empty the result is null.
	 */
	public E first()
	{
		return this.get(BASE_DIFF);
	} // first()

	// -------------------------------------------------------------------------

	/**
	 * Returns the last element of the receiver.   <br>
	 * If the collection is empty the result is null.
	 */
	public E last()
	{
		return this.get(this.size() - 1 + BASE_DIFF);
	} // last()

	// -------------------------------------------------------------------------
  
} // class OrderedCollection