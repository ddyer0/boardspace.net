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

import java.util.Arrays;


/**
 * this is a light weight replacement for Vector.  It is <b>not thread safe</b>, and it has no compatibility
 * problems with java 1.1.  Some of it's behavior is not strictly stack-like or identical to Vector.
 * @author ddyer
 *
 */
public abstract class OStack<T> implements StackIterator<T>
{		
		private T data[]=null;
		//@SuppressWarnings("unchecked")
		//private T[]newComponentArray(int sz)
		//{	
		//	return( (T[])Array.newInstance(instanceClass, sz));
		//}
		public abstract T[]newComponentArray(int sz);
		
		/**
		 * return an array with the same components as the stack
		 * @return the components as an array
		 */
		public T[] toArray()
		{	int sz = size();
			
			
			T[] newdata = newComponentArray(sz);
			for(int i=0;i<sz;i++)
			{
				newdata[i] = data[i];
			}
			return(newdata);
		}
		public T[] toReverseArray()
		{	int sz = size();
			
			
			T[] newdata = newComponentArray(sz);
			for(int i=sz-1;i>=0;i--)
			{
				newdata[i] = data[i];
			}
			return(newdata);
		}
		/**
		 * this allows you to set the actual array used by a stack
		 * @param arr
		 */
		public void useArray(T[]arr)
		{
			data = arr;
			index = arr.length-1;
		}

		/**
		 * push all elements of the array onto the stack
		 * @param ar
		 */
		public void pushArray(T[]ar)
		{	int newlen = ar.length;
			increaseSize(newlen+index);
			for(int i=0;i<newlen;i++) { push(ar[i]); }
		}
		/** constructor, includes the class for the instance */
		//public OStack (Class<T> in) { instanceClass = in; }
		/** constructor, setting initial size */
		//public OStack (Class<T> in,int siz) { instanceClass = in; increaseSize(siz); }
		public OStack() {};
		public OStack(int sz) { increaseSize(sz); }
		private int index = 0;
		@SuppressWarnings("deprecation")
		public String toString() 
		{
			return("<" + getClass().getSimpleName()+" "+index+" ["+((index>0)?data[index-1]:"")+"]>");
		}
		/**
		 *
		 * @return  return the number of elements on the stack
		 */
		public int size() { return(index); }
		
		/**
		 * set the size of the stack, discarding elements off the top if necessary. 
		 * @param n
		 */
		public void setSize(int n)
		{	if(n>index) { increaseSize(n); }
			else 
				{ while(index>n)
					{	data[--index] = null;	// clear memory to help the gc
					}
				}
		}
		
		/** clear the stack.  Actually clears them so the gc won't be encumbered with old items.
		 * 
		 */
		public void clear() 
		{ while(index>0) { data[--index] = null; } 
		}
		/** remove and return the top element of the stack.  An error will occur if the stack is already empty.
		 * 
		 * @return the top <T> from the stack
		 */
		public T pop() { T val = data[index-1]; data[--index]=null; return(val); }
		/** return the top element of the stack, or null if the stack is empty
		 * 
		 * @return the <T> on top if the stack
		 */
		public T top() { return(index>0?data[index-1]:null); }
		/** return the n'th element of the stack, zero origin.
		 * 
		 * @param i
		 * @return the element at index i
		 */
		public T elementAt(int i) { return(data[i]); }
		/**
		 * remove the i'th element of the stack.  This is done by 
		 * moving the top element, so the order of items in the stack
		 * will change if items are removed from the middle. 
		 * if i is out of range, return null
		 * @param i
		 * @param shuffle if true, preserve the order of the other elements
		 * @return the <T> removed
		 */
		public T remove(int i,boolean shuffle)
		{	if((i<index) && (i>=0)) 
			{	
			if(i+1<index) 
				{ 
				  T old = data[i];
				  if(shuffle)
				  {
					 int lim = index-1;
					 while(i<lim)
					 {	data[i] = data[i+1];
						i++;	
					}
					data[lim] = null;
					index = lim;
				  }
				  else 
				  {
				  data[i]= pop();
				  }
				  return(old);
				}	// swap top to the new empty
				else { return(pop()); }
			}
			return(null);
		}
		/** remove one occurrence of the <T> from the stack.  
		 * This is done by moving the top element, so the order of 
		 * elements in the stack will change if elements are removed from
		 * the middle. if o doesn't exist, return null
		 * @param o
		 * @param shuffle  if true, preserve the order of the other elements.
		 * @return the <T> removed, or null if nothing found
		 */
		public T remove(T o,boolean shuffle)
		{	if(shuffle)
			{
			// remove and shuffle the contents to preserve the order of the other elements
			for(int i=index-1; i>=0; i--)	// backwards so recently pushed items are fastest 
			{	if(eq(data[i],o))
					{	while(i+1 < index) { data[i] = data[i+1]; i++; }
						pop();
					  return(o);
					}
			}
			}
			else
			{
			// remove without preserving the order of the other elements
				for(int i=index-1; i>=0; i--)	// backwards so recently pushed items are fastest 
			
			{	if(eq(data[i],o))
					{ T temp = pop();
					  if(i<index) { data[i]=temp; }
					  return(o);
					}
			}}
			return(null);
		}
		/**
		 * true if the to items are equal for the purpose of this stack
		 * @param o
		 * @param x
		 * @return true if the same
		 */
		public boolean eq(T o,T x) { return(o==x); }
		/**
		 * return true if the stack contains the <T>
		 * @param o
		 * @return true if the stack contains the <T>
		 */
		public boolean contains(T o)
		{	for(int i=index-1; i>=0; i--) // backwards so recently pushed items are fastest 
				{ if(eq(data[i],o))
					{return(true); }}
			return(false);
		}
		/**
		 * find the index of the object, or -1
		 * @param o
		 * @return an int
		 */
		public int indexOf(T o)
		{	
			for(int i=index-1; i>=0; i--) // backwards so recently pushed items are fastest 
			{ if(eq(data[i],o))
				{return(i); }}
			return(-1);
		}
		/**
		 * replace or add a new item
		 */
		public void replace(T old,T replacement)
		{	for(int i=index-1; i>=0; i--) // backwards so recently pushed items are fastest 
			{ if(eq(data[i],old))
				{data[i] = replacement;
				 return;
				}
			}
			addElement(replacement);
		}
		/**
		 * push a <T> onto the stack if it is not already there.
		 * @param o
		 * @return true if the a new <T> was added
		 */
		// return true if the <T> is new
		public boolean pushNew(T o)
		{	if(contains(o)) { return(false); }
			push(o);
			return(true);
		}

		private void increaseSize(int len)
		{	T[] newdata = newComponentArray(len);
			int lim = (len<index)?len:index;
			for(int i=0;i<lim;i++) { newdata[i]=data[i]; }
			index = lim;
			data = newdata;
		}
		/** push a <T> onto the stack with no duplicate check.
		 * 
		 * @param da
		 */
		public StackIterator<T> push(T da) 
			{ int len = (data==null)?0:data.length;
			  if(index>=len)
				{ increaseSize((len+1)*2+1);
				}
			  //
			  // is this x=index; index=index+1; data[x] = d;
			  // or is this data[index] = d; index=index+1;
			  //
			  data[index] = da;
			  index++;			// do this as a second operation so readers will never see an empty slot
			  return(this);
			}
		public StackIterator<T>remove(T da)
		{
			remove(da,true);
			return(this);
		}
		public StackIterator<T>remove(int da)
		{
			remove(da,true);
			return(this);
		}

		/**
		 * for compatibility, same as "push"
		 */
		public void addElement(T da) { push(da); }
		/**
		 * set the nth element
		 */
		public void setElementAt(T v,int n)
		{
			data[n]=v;
		}

		/**
		 * insert a new element at index i.  The elements above the index
		 * are shuffled, so they remain the the same relative order
		 * @param v the element to be inserted
		 * @param n the index to insert it at
		 */
		public StackIterator<T> insertElementAt(T v,int n)
		{	T newEl = v;
			int at = n;
			while(at<index)
			{
			T oldEl = data[at];
			data[at] = newEl;
			newEl = oldEl;
			at++;
			}
			addElement(newEl);
			return(this);
		}
		/**
		 * copy the contents from another OStack
		 * @param other
		 */
		public void copyFrom(OStack<T> other)
		{	clear();
			for(int i=0,lim=other.size(); i<lim; i++)
			{	push(other.elementAt(i));
			}
		}
		public void union(OStack<T> other)
		{
			for(int lim = other.size()-1; lim>=0; lim--) { push(other.elementAt(lim)); }
		}
		public void unionNew(OStack<T> other)
		{
			for(int lim = other.size()-1; lim>=0; lim--) { pushNew(other.elementAt(lim)); }
		}

		/**
		 * compare two OStacks.  Comparison of the elements is with EQ
		 * @param other
		 * @return true of the stacks are identical
		 */
		public boolean sameContents(OStack<?> other)
		{	int sz = size();
			if(sz==other.size())
			{	Object[] d = data;
				Object[] od = other.data;
				for(int i=0;i<sz;i++) { if(d[i]!=od[i]) 
					{ return(false); }}
				return(true);
			}
			return(false);
		}
		/**
		 * this sorts using java.lang.comparable
		 */
		public void sort()
		{	if(index>0) { Arrays.sort(data,0,index); }
		}
		/**
		 * this sorts using {@link lib.CompareTo}, not {@link java.lang.Comparable}
		 * @param alt
		 */
		@SuppressWarnings("unchecked")
		public void sort(boolean alt)
		{	if(index>0) { Sort.sort((CompareTo[])data,0,index-1,alt); }
		}
		
		/**
		 * shuffle, as in shuffle a deck of cards. 
		 * @param r the Random which determined the random sequence
		 */
		public void shuffle(Random r)
		{	r.shuffle(data,index);
		}
}

