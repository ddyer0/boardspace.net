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

public class Sort {

	// note that sort and partition can be eliminated when java 1.1 is no longer supported
	@SuppressWarnings("unchecked")
	static <T> int partition(CompareTo<T> []array, int start, int end,boolean alt)
	   {
	          int left = start-1, right = end;
	          CompareTo<T> partitionElement =array[end];
	           // Arbitrary partition start...there are better ways...
	           for (;;)
	           {
	               while ((alt?partitionElement.altCompareTo((T)(array[++left]))
	                         :partitionElement.compareTo((T)array[++left])) > 0)
	               {
	                   if (left == end) break;
	               }
	               while ((alt?partitionElement.altCompareTo((T)array[--right])
	            		   :partitionElement.compareTo((T)array[--right])) < 0)
	               {
	                   if (right == start) break;
	               }
	               if (left >= right) break;
	               { CompareTo<T> a1 = array[left];
	                 array[left]=array[right];
	                 array[right]=a1;;
	               }
	           }
	           { CompareTo<T> a1 = array[left];
	             array[left]=array[end];
	             array[end]=a1;;
	           }
	          return left;
	       }

	/**
	 * quiksort part of an array.  If alt is true, sort using the alternate sort
	 * predicate instead of the primary.
	 * @param array an array of a type which implemented {@link CompareTo}
	 * @param start
	 * @param end
	 * @param alt
	 */
	   static public <T> void sort(CompareTo<T>[]array, int start, int end,boolean alt)
	   {  if (end > start)
	         {
	             int p = partition(array, start, end, alt);
	             sort(array, start, p-1,alt);
	             sort(array, p+1, end,alt);
	          }
	   }

	/**
	    * quiksort an array
	    * @param array an array of a type which implemented {@link CompareTo}
	    * @param start
	    * @param end
	    */
	   static public <T> void sort(CompareTo<T>[]array, int start, int end)
	   {	sort(array,start,end,false);
	   }

	/**
	    * quiksort an array
	    * @param array an array of a type which implemented {@link CompareTo}
	    */
	   static public <T> void sort(CompareTo<T> []array)
	   {	synchronized (array) { sort(array,0,array.length-1,false); }
	   }

	/**
	    * quiksort an array
	    * @param array an array of a type which implemented {@link CompareTo}
	    */
	   static public <T> void sort(CompareTo<T> []array,boolean alt)
	   {	synchronized(array) { sort(array,0,array.length-1,alt); }
	   }

}
