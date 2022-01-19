// ===========================================================================
// CONTENT  : CLASS Block1
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 21/01/2000
// HISTORY  :
//  21/01/2000  duma  CREATED
//
// Copyright (c) 2000, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// CONTENT  : CLASS Block1
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 21/01/2000
// HISTORY  :
//  21/01/2000  duma  CREATED
//
// Copyright (c) 2000, by MDCS. All rights reserved.
// ===========================================================================
// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This is ablock closure for one argument blocks.    <br>
 * The best way to use it, is to define the methods <i>value</i>
 * and <i>result()</i> when creating a new instance.<br>
 * Example:<br>
 *
 * <code>
 * 
 * Block1 block = new Block1()       <br>
 *   {
 *      Vector vector = new Vector() ;
 *      public void value( Object arg )
 *      {
 *         vector.add( ((String)arg).trim() )
 *      } ;
 *      public Object result() { return vector ; } ;
 *   } ;
 * </code>
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
abstract public class Block1
{ 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // INSTANCE METHODS
  // =========================================================================
	abstract protected void value( Object arg ) ;
	
	// -------------------------------------------------------------------------

	public Object eval( Object arg )
	{
		this.value( arg ) ;
		return result() ;
	} // eval()
		
	// -------------------------------------------------------------------------

	public Object result() 
	{ 
		return null ; 
	} // result() 

	// -------------------------------------------------------------------------
  
} // class Block1