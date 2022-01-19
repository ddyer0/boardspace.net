// ===========================================================================
// CONTENT  : INTERFACE IExtendedTextProvider
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 25/03/2006
// HISTORY  :
//  25/03/2006  mdu  CREATED
//
// Copyright (c) 2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.nls ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Locale;

/**
 * Defines additional methods for retrieving keys and checking contants. 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IExtendedTextProvider extends ITextProvider
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true if this text provider holds no text and also its default 
   * text provider contains nothing.
   */
  public boolean containsNothing() ;
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the locales for which this text provider has text
   */
  public Locale[] getLocales() ;

  // -------------------------------------------------------------------------
  
} // interface IExtendedTextProvider