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
 * static array manipulation methods
 * 
 * @author ddyer
 *
 */
public class AR {

	/**
	    * @param c1
	    * @param c2
	    * @return true if two integer arrays contain the same integers
	    */
	   static public boolean sameArrayContents(int c1[],int c2[])
		{	int len = c1.length;
			if(len==c2.length)
			{	for(int i=0;i<len;i++) { if(c1[i]!=c2[i]) { return(false); }}
				return(true);
			}
			return(false);
		}

	/**
	    * @param c1
	   * @param c2
	   * @return true if two integer arrays contain the same integers
	   */
	  static public boolean sameArrayContents(long c1[],long c2[])
		{	int len = c1.length;
			if(len==c2.length)
			{	for(int i=0;i<len;i++) { if(c1[i]!=c2[i]) { return(false); }}
				return(true);
			}
			return(false);
		}

	/**
	    * @param c1
	   * @param c2
	   * @return true if two byte arrays contain the same integers
	   */
	  static public boolean sameArrayContents(byte c1[],byte c2[])
		{	int len = c1.length;
			if(len==c2.length)
			{	for(int i=0;i<len;i++) { if(c1[i]!=c2[i]) { return(false); }}
				return(true);
			}
			return(false);
		}

	/**
	    * @param c1
	    * @param c2
	    * @return true if two boolean arrays contain the same sequence of booleans
	    */
	   static public boolean sameArrayContents(boolean c1[],boolean c2[])
		{	int len = c1.length;
			if(len==c2.length)
			{	for(int i=0;i<len;i++) { if(c1[i]!=c2[i]) { return(false); }}
				return(true);
			}
			return(false);
		}

	static public boolean sameArrayContents(char c1[],char c2[])
	{	int len = c1.length;
		if(len==c2.length)
		{	for(int i=0;i<len;i++) { if(c1[i]!=c2[i]) { return(false); }}
			return(true);
		}
		return(false);
	}

	static public boolean sameArrayContents(double c1[],double c2[])
	{	int len = c1.length;
		if(len==c2.length)
		{	for(int i=0;i<len;i++) 
				{ if(c1[i]!=c2[i])
					{ return(false); 
					}
				}
			return(true);
		}
		return(false);
	}

	/**
	    * @param c1
	    * @param c2
	    * @return true if two boolean arrays contain the same sequence of Objects
	    */
	   static public boolean sameArrayContents(Object c1[],Object c2[])
		{	int len = c1.length;
			if(len==c2.length)
			{	for(int i=0;i<len;i++) { if(c1[i]!=c2[i]) { return(false); }}
				return(true);
			}
			return(false);
		}

	/**
	    * copy the contents of one integer array into another.  If the
	    * destination is null, create a copy of the source.
	    * @param to the destination array
	    * @param from the source array
	    * @return to or the new array
	    */
	   static public int[] copy(int to[],int from[])
	   {	if(from!=null)
		    {int len = from.length;
		     if(to==null) { to = new int[len]; }
		     else { G.Assert(len==to.length,"same length"); }
	   	     for(int i=0;i<len;i++) { to[i]=from[i]; }
	   }
	   		return(to);
	   }

	/**
	    * copy the contents of one integer array into another, if the destination
	    * is null, create a copy of the source.
	    * @param to the destination array
	    * @param from the source array
	    * @return to or the new array
	    */
	   static public long[] copy(long to[],long from[])
	   {	if(from!=null)
		    {int len = from.length;
		    if(to==null) { to = new long[len]; }
		    else { G.Assert(len==to.length,"same length");}
	   	    for(int i=0;i<len;i++) { to[i]=from[i]; }
	   }
	   	return(to);
	   }

	/**
	    * copy the contents of one double array into another, if the 
	    * destination is null, create a copy of the source.
	    * @param to the destination array
	    * @param from the source array
	    * @return to or the new array
	    */
	   static public double[] copy(double to[],double from[])
	   {	if(from!=null)
		    {int len = from.length;
		     if(to==null) { to = new double[len]; }
		     else { G.Assert(len==to.length,"same length"); }
	   	     for(int i=0;i<len;i++) { to[i]=from[i]; }
	   }
	   		return(to);
	   }

	/**
	    * copy the contents of one integer array into another
	    * @param to the destination array
	    * @param from the source array
	    * @return to or the new array
	    */
	   static public char[] copy(char to[],char from[])
	   {	if(from!=null)
		   	{ int len = from.length;
		   	if(to==null) { to = new char[len]; }
		   	else { G.Assert(len==to.length,"same length"); }
	   	    for(int i=0;i<len;i++) { to[i]=from[i]; }
	   }
	   		return(to);
	   }

	/**
	    * copy the contents of one array of objects to another
	    * @param to the destination array
	    * @param from the source array
	    */
	   static public void copy(Object to[],Object from[])
	   {	int len = to.length;
	   		G.Assert(len==from.length,"same length");
	   	    for(int i=0;i<len;i++) 
	   	    { Object s = from[i];
	   	      // limits to java type system.  Object[][] gets here too, and end up copying
	   	      // the structure.  Trying to cast Object[][] to Object[] fails because arrays
	   	      // are not the type of their contents.  The best we can do is scream.
	   	      G.Assert(s==null || !s.getClass().isArray(),"can't be an array[][]");
	   	      to[i]=from[i]; 
	   	    }
	   }

	/**
	    * copy an array of arrays of integers
	    * @param to
	    * @param from
	    */
	   static public void copy(int [][]to,int [][]from)
	   {
		   int len = to.length;
		   G.Assert(len==from.length,"same length");
		   for(int i=0;i<len;i++) { copy(to[i],from[i]); }
	   }

	/**
	    * create a copy of an integer array
	    * @param from
	    * @return a new array
	    */
	   static public int[]copy(int from[])
	   {	int val[] = null;
		    if(from!=null)
		    {
		    	val = new int[from.length];
		    	copy(val,from);
		    }
		    return(val);
	   }

	/**
	    * create a copy of an double array
	    * @param from
	    * @return a new array
	    */
	   static public double []copy(double from[])
	   {	double val[] = null;
		    if(from!=null)
		    {
		    	val = new double[from.length];
		    	copy(val,from);
		    }
		    return(val);
	   }

	/**
	    * set each cell of a boolean array to a fixed value
	   */
	   static public void setValue(boolean c1[],boolean v)
	   {
		   for(int lim=c1.length-1; lim>=0; lim--) { c1[lim]=v; }
		}

	   /**
	    * set each cell of an array to a fixed value
	    */
	   static public void setValue(int c1[],int v)
	   {
		   for(int lim=c1.length-1; lim>=0; lim--) { c1[lim]=v; }
		}

	/**
	    * set each cell of an array to a fixed value
	   */
	   static public void setValue(Object c1[],Object v)
	   {
		   for(int lim=c1.length-1; lim>=0; lim--) { c1[lim]=v; }	   
		}

	static public void setValue(Object c1[][],Object v)
	   {
		   for(Object cv[] : c1) { setValue(cv,v); }	   
		}

	/**
	    * set each cell of an array to a fixed value
	    * @param speed
	    * @param d
	    */
	   public static void setValue(double[] speed, double d) {
			for(int lim=speed.length-1; lim>=0; lim--) { speed[lim] = d; }
	   }

	/**
	    * copy the contents of one boolean array into another
	    * @param c1 the destination array
	    * @param c2 the source array
	    */
	   static public void copy(boolean c1[],boolean c2[])
	   {	int len = c1.length;
	   		G.Assert(len==c2.length,"same length");
	   	    for(int i=0;i<len;i++) { c1[i]=c2[i]; }
	   }

	/**
	    * utility to allocate an array length n containing integers 0-n-1
	    * @param n
	    * @return an array of integers
	    */
	   static public int[]intArray(int n)
	   {
		   int ar[] = new int[n];
		   for(int i=0;i<n;i++) { ar[i]=i; }
		   return(ar);
	   }
/**
 * return the index of content "i" in array a, or -1 if its not there
 * 
 * @param a
 * @param i
 * @return
 */
	   static public int indexOf(Object []a,Object i)
	   {
		   for(int lim = a.length-1; lim>=0; lim--) { if (a[lim]==i) return lim; }
		   return(-1);
	   }
}
