// ===========================================================================
// CONTENT  : INTERFCAE IReleasable
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 24/02/2006
// HISTORY  :
//  24/02/2006  mdu  CREATED
//
// Copyright (c) 2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.lifecycle ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This interface specifies just a release() method an object must understand 
 * to release its internal resources.<br>
 * This is more or less the same as IDisposable.
 * However, developers may prefer release() over dispose() or deliberately
 * want to use a different mechanism than the more commonly used dispose().
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface IReleasable
{
  /**
   * Release any resources that should not be referred to anymore in order
   * to let the garbage collector take care of them.
	 * Usuallay after calling this method the receiver cannot be used anymore. 
   */
	public void release() ;
	
} // interface IReleasable
