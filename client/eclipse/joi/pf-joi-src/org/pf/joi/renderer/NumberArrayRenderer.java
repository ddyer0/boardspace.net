// ===========================================================================
// CONTENT  : CLASS NumberArrayRenderer
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 01/09/2007
// HISTORY  :
//  01/09/2007  mdu  CREATED
//
// Copyright (c) 2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.renderer ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Array;

import org.pf.joi.ObjectRenderer;

/**
 * Generic renderer for arrays of number objects.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class NumberArrayRenderer implements ObjectRenderer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public NumberArrayRenderer()
  {
    super() ;
  } // NumberArrayRenderer()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public String inspectString( Object obj )
	{
		int len ;
		StringBuffer buffer ;
		
		if ( ( obj != null ) && ( obj.getClass().isArray() ) )
		{
			len = Array.getLength( obj ) ;
			buffer = new StringBuffer( 5 * len ) ;
			buffer.append( "[" ) ;
			for (int i = 0; i < len; i++)
			{
				if ( i > 0 )
				{
					buffer.append( ',' ) ;
				}
				buffer.append( Array.get( obj, i ) ) ;			
			}
			buffer.append( "]" ) ;
			return buffer.toString() ;
		}
		return "" + obj ;
	} // inspectString()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class NumberArrayRenderer
