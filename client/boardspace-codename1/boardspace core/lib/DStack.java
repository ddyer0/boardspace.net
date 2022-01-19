package lib;

import java.util.Arrays;

/**
 * this is a light weight alternate for Vector class, it is <b>not thread safe</b>, and 
 * unlike vector, it has no compatibility problems with java 1.1.
 * 
 * @author ddyer
 *
 */
public class DStack {
	
	double data[] = new double[5];
	int index = 0;
	public String toString() 
	{
		return("<DStack "+index+" ["+((index>0)?data[index-1]:"")+"]>");
	}
	/**
	 * produce an array of integers containing the elements of the stack
	 * @return an array of double
	 */
	public double[] toArray()
	{	int sz = size();
		double newdata[] = new double[sz];
		for(int i=0;i<sz;i++)
		{
			newdata[i] = data[i];
		}
		return(newdata);
	}
	/**
	 * 
	 * @return the number of elements in the stack
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
	public double pop() { index--; double val = data[index]; data[index]=0; return(val); }
	/** return the top integer from the stack.  An error will result if the stack is empty.
	 * 
	 * @return the top integer on the stack
	 */
	public double top() { return(data[index-1]); }
	/** return the n'th element of the stack (zero origin)
	 * 
	 * @param i
	 * @return the integer at the index
	 */
	public double elementAt(int i) { return(data[i]); }
	/** set the value of an element on the stack
	 * 
	 * @param i		integer index
	 * @param v		integer value to set
	 */
	public void setElementAt(int i,double v)
	{	data[i]=v;
	}
	public void increaseSize(int n)
	{	double newdata[] = new double[n];
		for(int i=0;i<data.length;i++) { newdata[i]=data[i]; }
		data = newdata;
	}
	public void push(double da) 
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
	public boolean contains(double o)
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
	public int indexOf(double o)
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
	public boolean removeValue(double o,boolean shuffle)
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
	public boolean pushNew(double o)
	{	if(contains(o)) { return(false); }
		push(o);
		return(true);
	}
	/** copy the contents from another DStack
	 * 
	 * @param other
	 */
	public void copyFrom(DStack other)
	{	clear();
		for(int i=0,lim=other.size(); i<lim; i++)
		{	push(other.elementAt(i));
		}
	}
	/**
	 * compare two DStacks 
	 * @param other
	 * @return true if this stack is identical to the other
	 */
	public boolean sameContents(DStack other)
	{	int sz = size();
		if(sz==other.size())
		{	for(int i=0;i<sz;i++) { if(elementAt(i)!=other.elementAt(i)) { return(false); }}
			return(true);
		}
		return(false);
	}

	/** sort the contents
	 * 
	 */
	public void sort()
	{	if(index>=0) { Arrays.sort(data,0,index); }
	}

	/**
	 * remove the i'th element of the stack.  This is done by 
	 * moving the top element, so the order of items in the stack
	 * will change if items are removed from the middle. 
	 * @param i
	 * @param shuffle if true, preserve the order of the other elements
	 */
	public double removeAtIndex(int i,boolean shuffle)
	{	if((i<index) && (i>=0)) 
		{	
		if(i<index) 
			{ double v = data[i];
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
	public void insert(double value,int indx)
	{	double newEl = value;
		int at = indx;
		while(at<index)
		{
		double oldEl = data[at];
		data[at] = newEl;
		newEl = oldEl;
		at++;
		}
		push(newEl);
	}
}
