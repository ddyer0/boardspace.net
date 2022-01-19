// ===========================================================================
// CONTENT  : CLASS DefaultFilenameFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 12/01/2014
// HISTORY  :
//  21/01/2000  duma  CREATED
//  12/01/2014  mdu   added   --> ALL, NONE
//
// Copyright (c) 2000-2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.FilenameFilter;

import org.pf.text.StringPattern;

/**
 * This filter implements the standard pattern matching on UNIX and Windows
 * platforms. It supports the wildcards '*' and '?'.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class DefaultFilenameFilter implements FilenameFilter
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  /**
   * A filename filter that matches all names.
   */
  public static final FilenameFilter ALL = new DefaultFilenameFilter("*", true);

  /**
   * A filename filter that never matches any name.
   */
  public static final FilenameFilter NONE = new DefaultFilenameFilter(null);

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private StringPattern pattern = null ; // Default is NONE
  protected StringPattern getPattern() { return pattern ; }  
  protected void setPattern( StringPattern newValue ) { pattern = newValue ; }  
	
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with the given filename pattern.
   * Case sensitivity is ON by default.
   *
   * @param pattern The pattern for filenames that match. Can include wildcards '*' and '?'
   */
  public DefaultFilenameFilter(String pattern)
  {
    this(pattern, false);
  } // DefaultFilenameFilter()  

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the given filename pattern.
   *
   * @param pattern The pattern for filenames that match. Can include wildcards '*' and '?'.
   *   If the pattern is null, no name will match.
   * @param ignoreCase false, if match should be done case sensitive, otherwise true.
   */
  public DefaultFilenameFilter(String pattern, boolean ignoreCase)
  {
    super();
    if (pattern != null)
    {      
      this.setPattern(new StringPattern(pattern, ignoreCase));
    }
  } // DefaultFilenameFilter()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Tests if a given file name matches the filter.
   *
   * @param dir the directory in which the file was found.
   * @param name the name of the file.
   * @return true if the name matches the underlying pattern, false otherwise.
   */
  public boolean accept(File dir, String name)
  {
    if (this.getPattern() == null)
    {
      return false;
    }
    return (this.getPattern().matches(name));
  } // accept()

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class DefaultFilenameFilter