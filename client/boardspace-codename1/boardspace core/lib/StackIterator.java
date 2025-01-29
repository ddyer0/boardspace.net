/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package lib;
/**
 * StackIterator is an interface to make it convenient to use a singleton
 * instance of an object and a stack of the same more or less interchangeably.
 * OStack<T> implements this interface.
 * <p>
 * The intended use of this interface is where zero or one of something is normally
 * to be collected, but sometimes more are possible.
 * 
 * This interface is inplemented by OStack, which can be used to implement the
 * "become a stack" option.
 * 
 * You can build a stack using code like 
 * ... stack = stack==null ? newitem : stack.push(newitem) ...
 * 
 * and process the stack using code like
 * while (stack!=null) { Object item = stack.top(); stack=stack.discardTop(); }
 * 
 * or 
 * 
 * if(stack!=null)
 * {	for(int i=0;i<stack.size();i++) { Object item = stack.elementAt(i); }
 * }
 * @author Ddyer
 *
 * @param <T>
 */
public interface StackIterator<T> {
	/* add a new element to the stack, returning the stack. There is no duplicate check */
	public StackIterator<T> push(T item);
	/* insert an item into the stack, returning the stack.  If the count goes from 1 to more than 1, returned
	 * stack won't be the same as the original. */
	public StackIterator<T> insertElementAt(T item,int at);

	/**
	 * return the number of elements in the iterator
	 * @return
	 */
	default public int size() { return 1; }		// number of items in the stack, singletons will return 1

	
	/* remove an item from the stack, returning the stack.  It's not an error if the item wasn't in the stack 
	 * the order of the remaining items in the stack is unchanged.  If the actual count goes from 2 to 1 
	 * the single remaining value may be returned instead of the original stack.  If the count goes from 1 to 0,
	 * null will be returned */
	default public StackIterator<T> remove(T item) { return item==this ? null : this; }
	
	/* remove the n'th item from the stack, returning the stack.  It's not an error if n is out of range 
	 * the order of the remaining items in the stack is unchanged.  If the actual count goes from 2 to 1 
	 * the single remaining value may be returned instead of the original stack.  If the count goes from 1 to 0,
	 * null will be returned */
	default public StackIterator<T> remove(int n) { return n==0 ? null : this; }

	@SuppressWarnings("unchecked")
	default public T elementAt(int n) { return (T)this; };	// fetch the n'th element of the stack, singletons return themselves
	/*
	 * return the top element of the stack
	 */
	@SuppressWarnings("unchecked")
	default public T top() { return (T)this; }
	/*
	 * remove the top element of the stack, return the new stack
	 */
	default public StackIterator<T>discardTop() { return null; }
}
