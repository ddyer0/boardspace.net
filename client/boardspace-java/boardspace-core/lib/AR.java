package lib;

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
	    * @param c1 the destination array
	    * @param c2 the source array
	    * @return c1 or the new array
	    */
	   static public int[] copy(int c1[],int c2[])
	   {	if(c2!=null)
		    {int len = c2.length;
		     if(c1==null) { c1 = new int[len]; }
		     else { G.Assert(len==c1.length,"same length"); }
	   	     for(int i=0;i<len;i++) { c1[i]=c2[i]; }
		    }
	   		return(c1);
	   }

	/**
	    * copy the contents of one integer array into another, if the destination
	    * is null, create a copy of the source.
	    * @param c1 the destination array
	    * @param c2 the source array
	    * @return c1 or the new array
	    */
	   static public long[] copy(long c1[],long c2[])
	   {	if(c2!=null)
		    {int len = c2.length;
		    if(c1==null) { c1 = new long[len]; }
		    else { G.Assert(len==c1.length,"same length");}
	   	    for(int i=0;i<len;i++) { c1[i]=c2[i]; }
		    }
	   	return(c1);
	   }

	/**
	    * copy the contents of one double array into another, if the 
	    * destination is null, create a copy of the source.
	    * @param c1 the destination array
	    * @param c2 the source array
	    * @return c1 or the new array
	    */
	   static public double[] copy(double c1[],double c2[])
	   {	if(c2!=null)
		    {int len = c2.length;
		     if(c1==null) { c1 = new double[len]; }
		     else { G.Assert(len==c1.length,"same length"); }
	   	     for(int i=0;i<len;i++) { c1[i]=c2[i]; }
		    }
	   		return(c1);
	   }

	/**
	    * copy the contents of one integer array into another
	    * @param c1 the destination array
	    * @param c2 the source array
	    */
	   static public char[] copy(char c1[],char c2[])
	   {	if(c2!=null)
		   	{ int len = c2.length;
		   	if(c1==null) { c1 = new char[len]; }
		   	else { G.Assert(len==c1.length,"same length"); }
	   	    for(int i=0;i<len;i++) { c1[i]=c2[i]; }
		   	}
	   		return(c1);
	   }

	/**
	    * copy the contents of one array of objects to another
	    * @param c1 the destination array
	    * @param c2 the source array
	    */
	   static public void copy(Object c1[],Object c2[])
	   {	int len = c1.length;
	   		G.Assert(len==c2.length,"same length");
	   	    for(int i=0;i<len;i++) 
	   	    { Object s = c2[i];
	   	      // limits to java type system.  Object[][] gets here too, and end up copying
	   	      // the structure.  Trying to cast Object[][] to Object[] fails because arrays
	   	      // are not the type of their contents.  The best we can do is scream.
	   	      G.Assert(s==null || !s.getClass().isArray(),"can't be an array[][]");
	   	      c1[i]=c2[i]; 
	   	    }
	   }

	/**
	    * copy an array of arrays of integers
	    * @param c1
	    * @param c2
	    */
	   static public void copy(int [][]c1,int [][]c2)
	   {
		   int len = c1.length;
		   G.Assert(len==c2.length,"same length");
		   for(int i=0;i<len;i++) { copy(c1[i],c2[i]); }
	   }

	/**
	    * create a copy of an integer array
	    * @param c1
	    * @return a new array
	    */
	   static public int[]copy(int c1[])
	   {	int val[] = null;
		    if(c1!=null)
		    {
		    	val = new int[c1.length];
		    	copy(val,c1);
		    }
		    return(val);
	   }

	/**
	    * create a copy of an double array
	    * @param c1
	    * @return a new array
	    */
	   static public double []copy(double c1[])
	   {	double val[] = null;
		    if(c1!=null)
		    {
		    	val = new double[c1.length];
		    	copy(val,c1);
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

}
