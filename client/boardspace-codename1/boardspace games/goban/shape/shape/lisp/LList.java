package goban.shape.shape.lisp;

import lib.G;

/**
 * implements a simple list (as in "lisp") and some useful methods on it 
 * 
 * @author Dave Dyer <ddyer@netcom.com>
 * @version 1.01, March 1997
 * 
 */

class LList 
{
	/**
	 * 
	 */
	private Object contents;
	private LList next;


	/**
	 * for the Enumeration protocol 
	 */

	public LList nextElement () 
	{
		return(this.next);
	}


	/**
	 * get the contents of this element of the list 
	 */

	public Object Contents () 
	{
		return(this.contents);
	}


	/**
	 * get the next element of the list 
	 */

	public LList Next () 
	{
		return(this.next);
	}


	/**
	 * set the contents of the list to an arbitrary object 
	 */

	public void Set_Contents (Object to) 
	{
		this.contents = to;
	}


	/**
	 * set the next element of the list (to some other list) 
	 */

	public void Set_Next (LList next) 
	{
		this.next = next;
	}

	// constructors 
	public LList () 
	{
		
	}

	public LList (Object contents, LList next) 
	{
		this.contents = contents;
		this.next  = next;
	}


	/**
	 * delete the list containing "item" from the list.  Returns a new head
	 * for the list, so correct usage is  theList = theList.Delete_Item(object);
	 * 
	 */

	public LList Delete_Item (Object item) 
	{
		 LList prev=null;
		  LList cur = this;
		  while(cur!=null) {
		    if(cur.contents == item) { 
		      if(prev!=null) 
			{ prev.next = cur.next;
			  cur.next=null;
		          return(this);
		        }
		      else
		       {prev = cur.next;
		        cur.next = null;
		        return(prev);
		       }
		     }
		    prev = cur;
		    cur=cur.next;
		  }
		  return(this);
	}


	/**
	 * return the length of the list, or 0 if it is null 
	 */

	public static int LList_Length (LList l) 
	{
			int len=0;
		  while(l!=null) { len++; l=l.next; }
		  return(len);
	}

	public String getString()
	{	String rest = ((next==null) ? "" : " " + next.getString()) ;
		if(contents==null) { return("null "+ rest); }
		if(contents instanceof String) { return((String)contents + rest); }
		if(contents instanceof LList) 
			{ LList c = (LList)contents;
			  return("(" + c.getString()+")")+ rest;
			}
		throw G.Error("Contents type not handled");
	}

	// sort_short_list
}

/* class List   */