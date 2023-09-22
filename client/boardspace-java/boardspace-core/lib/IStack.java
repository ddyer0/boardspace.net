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
 * this is a light weight alternate for Vector class, it is <b>not threadsafe</b>, and 
 * unlike vector, it has no compatibility problems with java 1.1.
 * 
 * @author ddyer
 *
 */
public class IStack implements Digestable
{
	
	int data[] = new int[5];
	int index = 0;
	public String toString() 
	{
		return("<IStack "+index+" ["+((index>0)?data[index-1]:"")+"]>");
	}
	/**
	 * produce an array of integers containing the elements of the stack
	 * @return an array of int
	 */
	public int[] toArray()
	{	int sz = size();
		int newdata[] = new int[sz];
		for(int i=0;i<sz;i++)
		{
			newdata[i] = data[i];
		}
		return(newdata);
	}
	/**
	 * 
	 * @return the numnber of elements in the stack
	 */
	public int size() { return(index); }
	/**
	 * set the size of the stack, reducing the number of elements
	 * @param n
	 */
	public void setSize(int n)
	{	if(n>index) { increaseSize(n); }
		index = n;
	}
	/**
	 * clear the stack
	 */
	public void clear() { index=0; }
	/**
	 * remove and return the top integer on the stack.  An error will result if the stack is empty.
	 * @return the integer from the top of the stack
	 */
	public int pop() { index--; int val = data[index]; data[index]=0; return(val); }
	/** return the top integer from the stack.  An error will result if the stack is empty.
	 * 
	 * @return the top integer on the stack
	 */
	public int top() { return(data[index-1]); }
	/**
	 * return the top value, or if the stack is empty return the z value
	 * @param z
	 * @return
	 */
	public int topz(int z) { return(index>0?data[index-1]:z); }

	/** return the n'th element of the stack (zero origin)
	 * 
	 * @param i
	 * @return the integer at the index
	 */
	public int elementAt(int i) { return(data[i]); }
	/** set the value of an element on the stack
	 * 
	 * @param i		integer index
	 * @param v		integer value to set
	 */
	public void setElementAt(int i,int v)
	{	data[i]=v;
	}
	public void increaseSize(int n)
	{	int newdata[] = new int[n];
		for(int i=0;i<data.length;i++) { newdata[i]=data[i]; }
		data = newdata;
	}
	public void push(int da) 
		{ if(index>=data.length)
			{ increaseSize((data.length*3)/2);
			}
		  data[index] = da;
		  index++;	// increment later, so readers never see an unfilled slot
		}
	/**
	 * return true if the stack contains the integer
	 * @param o
	 * @return true if the stack contains the integer
	 */
	public boolean contains(int o)
	{	for(int i=index-1; i>=0; i--) // backwards so recently pushed items are fastest 
			{ if(data[i]==o)
				{return(true); }}
		return(false);
	}
	/**
	 * return the index of item o
	 * @param o
	 * @return the index if the stack contains the integer, -1 otherwise
	 */
	public int indexOf(int o)
	{	for(int i=index-1; i>=0; i--) // backwards so recently pushed items are fastest 
			{ if(data[i]==o)
				{return(i); }}
		return(-1);
	}
	/**
	 * remove one instance of the value from the stack.  If shuffle is true, shuffle the
	 * contents to preserve the order of the remaining elements 
	 * @param o the value to be removed
	 * @param shuffle if true, preserve order
	 * @return true if something was removed
	 */
	public boolean removeValue(int o,boolean shuffle)
	{	int ind = indexOf(o);
		if(ind>=0) { removeAtIndex(ind,shuffle); return(true); }
		return(false);
	}
	/**
	 * push a <T> onto the stack if it is not already there.
	 * @param o
	 * @return true if the a new <T> was added
	 */
	// return true if the <T> is new
	public boolean pushNew(int o)
	{	if(contains(o)) { return(false); }
		push(o);
		return(true);
	}
	/** copy the contents from another IStack
	 * 
	 * @param other
	 */
	public void copyFrom(IStack other)
	{	clear();
		for(int i=0,lim=other.size(); i<lim; i++)
		{	push(other.elementAt(i));
		}
	}
	/**
	 * compare two IStacks 
	 * @param other
	 * @return true if this stack is identical to the other
	 */
	public boolean sameContents(IStack other)
	{	int sz = size();
		if(sz==other.size())
		{	for(int i=0;i<sz;i++) { if(elementAt(i)!=other.elementAt(i)) { return(false); }}
			return(true);
		}
		return(false);
	}
	public long Digest(Random r)
	{	int v=0;
		for(int i=0,lim=size(); i<lim; i++) { v ^= elementAt(i)*r.nextLong(); }
		return(v);
	}
	/** sort the contents
	 * 
	 */
	public void sort()
	{	if(index>=0) { Arrays.sort(data,0,index); }
	}
	/**
	 * randomize the contents
	 * @param r
	 */
	public void shuffle(Random r)
	{	r.shuffle(data,index);
	}
	/**
	 * remove the i'th element of the stack.  This is done by 
	 * moving the top element, so the order of items in the stack
	 * will change if items are removed from the middle. 
	 * @param i
	 * @param shuffle if true, preserve the order of the other elements
	 */
	public int removeAtIndex(int i,boolean shuffle)
	{	if((i<index) && (i>=0)) 
		{	
		if(i+1<index) 
			{ int v = data[i];
			  if(shuffle)
			  {
				 int lim = index-1;
				 while(i<lim)
				 {	data[i] = data[i+1];
					i++;	
				}
				data[lim] = -1;
				index = lim;
			  }
			  else 
			  {
			  data[i]= pop();
			  }
			  return(v);
			}	// swap top to the new empty
			else { return(pop()); }
		}
		G.Error("Invalid index");
		return(-1);
	}
	

	/**
	 * insert a new element at index i.  The elements above the index
	 * are shuffled, so they remain the the same relative order
	 * @param value the element to be inserted
	 * @param indx the index to insert it at
	 */
	public void insert(int value,int indx)
	{	int newEl = value;
		int at = indx;
		while(at<index)
		{
		int oldEl = data[at];
		data[at] = newEl;
		newEl = oldEl;
		at++;
		}
		push(newEl);
	}
}
