package lib;
/**
 * StackIterator is an interface to make it convenient to use a singleton
 * instance of an object and a stack of the same more or less interchangeably.
 * OStack<T> implements this interface.
 * <p>
 * The intended use of this interface is where zero or one of something is normally
 * to be collected, but sometimes more are possible.
 * 
 * @author Ddyer
 *
 * @param <T>
 */
public interface StackIterator<T> {
	public int size();					// number of items in the stack, singletons will return 1
	public T elementAt(int n);			// fetch the n'th element of the stack, singletons return themselves
	/* add a new element to the stack, returning the stack. There is no duplicate check */
	public StackIterator<T> push(T item);
	/* remove an item from the stack, returning the stack.  It's not an error if the item wasn't in the stack 
	 * the order of the remaining items in the stack is unchanged.  If the actual count goes from 2 to 1 
	 * the single remaining value may be returned instead of the original stack.  If the count goes from 1 to 0,
	 * null will be returned */
	public StackIterator<T> remove(T item);	
	/* remove the n'th item from the stack, returning the stack.  It's not an error if n is out of range 
	 * the order of the remaining items in the stack is unchanged.  If the actual count goes from 2 to 1 
	 * the single remaining value may be returned instead of the original stack.  If the count goes from 1 to 0,
	 * null will be returned */
	public StackIterator<T> remove(int n);
	/* insert an item into the stack, returning the stack.  If the count goes from 1 to more than 1, returned
	 * stack won't be the same as the original. */
	public StackIterator<T> insertElementAt(T item,int at);
}
