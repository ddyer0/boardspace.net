// ===========================================================================
// CONTENT  : INTERFACE IDisposable
// AUTHOR   : M.Duchrow
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
 * This interface specifies just a dispose() method an object must understand 
 * to dispose its internal resources.<br>
 * This is more or less the same as IReleasable.
 * Developers may prefer dispose() over release() or just
 * want to extend a class that already implements the more commonly used 
 * dispose().
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IDisposable
{ 
	/**
	 * Get rid of all internal references to other objects.
	 * Usuallay after calling this method the receiver cannot be used anymore. 
	 */
	public void dispose() ;
	
} // interface IDisposable