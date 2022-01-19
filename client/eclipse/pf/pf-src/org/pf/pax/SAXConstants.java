// ===========================================================================
// CONTENT  : INTERFACE SAXConstants
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 30/07/2004
// HISTORY  :
//  30/07/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Just provides some constants to be with by SAX parsers.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface SAXConstants
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 * The name of the XMLReader feature to switch validating on or off
	 */
	public static final String FEATURE_VALIDATING	= "http://xml.org/sax/features/validation" ;

	/**
	 * This constant contains the SAX feature name URL for namespace support.
	 */
	public static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";

	/**
	 * This constant contains the SAX feature name URL for namespace prefixes.
	 */
	public static final String FEATURE_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
  
} // interface SAXConstants