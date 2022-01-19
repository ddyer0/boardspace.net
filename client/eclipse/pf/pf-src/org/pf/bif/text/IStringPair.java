// ===========================================================================
// CONTENT  : INTERFACE IStringPair
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 24/03/2008
// HISTORY  :
//  24/03/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Specifies a pair of String objects. Such pairs quite common and deserve
 * to be modeled in a separate interface. 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IStringPair
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 * An immutable empty array of this type.
	 */
	public static final IStringPair[] EMPTY_ARRAY = new IStringPair[0] ;
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the first string of this pair.
	 */
  public String getString1() ;
  
  // -------------------------------------------------------------------------
	
  /**
   * Returns the second string of this pair.
   */
  public String getString2() ;
  
  // -------------------------------------------------------------------------

  /**
   * Returns both strings of this pair in a String array.
   */
  public String[] asArray() ; 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the two strings as one string separated by the given separator.
   * If the provided separator is null, no separator should be put between
   * the strings.
   * 
   * @param separator A separator to be placed between the two strings.
   */
  public String asString( String separator ) ;
  
  // -------------------------------------------------------------------------
  
} // interface IStringPair