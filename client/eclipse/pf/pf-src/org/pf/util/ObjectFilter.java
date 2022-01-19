// ===========================================================================
// CONTENT  : INTERFACE ObjectFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 15/01/2012
// HISTORY  :
//  27/05/2005  mdu  CREATED
//	24/06/2006	mdu		changed -> to extend IObjectFilter
//	15/01/2012	mdu		changed	-->	to generic type
//
// Copyright (c) 2005-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.bif.filter.IObjectFilter;

/**
 * A filter for any kind of object.
 * It is recommended to use IObjectFilter rather than this interface.
 * This interface probably will be set to deprecated in the future.
 *
 * @author Manfred Duchrow
 * @version 1.2
 * @see org.pf.bif.filter.IObjectFilter
 */
public interface ObjectFilter<T> extends IObjectFilter<T> 
{ 
  
} // interface ObjectFilter