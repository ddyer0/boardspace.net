// ===========================================================================
// CONTENT  : INTERFACE IJSONConvertible
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 21/01/2012
// HISTORY  :
//  21/01/2012  mdu  CREATED
//
// Copyright (c) 2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.bif.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Implementers of this interface are capable of converting themselves into
 * a valid JSON string representation. 
 * <p/>
 * For details about JSON see
 * {@link  <a href="http://www.json.org/" target="blank">JSON Web Site</a>}
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface IJSONConvertible
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final String JSON_OBJECT_START = "{";
	public static final String JSON_OBJECT_END = "}";
	public static final String JSON_ARRAY_START = "[";
	public static final String JSON_ARRAY_END = "]";
	public static final String JSON_ELEMENT_SEPARATOR = ",";
	public static final String JSON_PAIR_SEPARATOR = ":";
	public static final String JSON_STRING_DELIMITER = "\"";
	public static final String JSON_LITERAL_NULL = "null";
	public static final String JSON_LITERAL_TRUE = "true";
	public static final String JSON_LITERAL_FALSE = "false";
	public static final String JSON_STRING_ESCAPE = "\\";
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * The receiver of this method must append its internal state as JSON string
	 * representation to the given buffer.
	 * 
	 * @param buffer The buffer to which to append the JSON string (must not be null).
	 */
	public void appendAsJSONString(StringBuffer buffer);
	
	/**
	 * The receiver of this method returns its internal state as JSON string
	 * representation.
	 */
	public String toJSON();
  
} // interface IJSONConvertible