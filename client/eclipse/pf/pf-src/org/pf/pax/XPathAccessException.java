// ===========================================================================
// CONTENT  : CLASS XPathAccessException
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 14/01/2012
// HISTORY  :
//  14/01/2012  mdu  CREATED
//
// Copyright (c) 2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.pax ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.RuntimeException ;

/**
 * A runtime exception that encapsulate all exceptions that might occur
 * in accessing XML data via XPath queries.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class XPathAccessException extends RuntimeException
{
  /**
   * Initialize the new instance with default values.
   */
  public XPathAccessException()
  {
    super() ;
  } // XPathAccessException()

	public XPathAccessException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public XPathAccessException(String message)
	{
		super(message);
	}

	public XPathAccessException(Throwable cause)
	{
		super(cause);
	}
    
} // class XPathAccessException
