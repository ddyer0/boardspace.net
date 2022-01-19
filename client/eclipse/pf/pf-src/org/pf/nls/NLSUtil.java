// ===========================================================================
// CONTENT  : CLASS NLSUtil
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 24/03/2006
// HISTORY  :
//  24/03/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.nls ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Locale;

import org.pf.file.Classpath;
import org.pf.file.FileUtil;
import org.pf.text.StringUtil;

/**
 * Provides some utility and convenience methods related NLS.
 * The system property "org.pf.nls.path" can be used to specify the path 
 * for NLS text file lookup. Its value must be set like a classpath.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class NLSUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String SYS_PROP_NLS_PATH = "org.pf.nls.path" ;
	
  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static NLSUtil soleInstance = new NLSUtil() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  /**
   * Returns the only instance this class supports (design pattern "Singleton")
   */
  public static NLSUtil current()
  {
    return soleInstance ;
  } // current() 

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  private NLSUtil()
  {
    super() ;
  } // NLSUtil() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Creates a locale from the given name.
   * That is the name is parsed and the different elements separated by '_'
   * are used to instantiate a locale.
   */
  public Locale createLocale( String localeName ) 
	{
  	String[] parts ;
  	
  	if ( this.str().isNullOrBlank( localeName ) )
		{
			return null ;
		}
  	parts = this.str().parts( localeName, "_" ) ;
  	switch ( parts.length )
		{
			case 1 :
				return new Locale( parts[0].toLowerCase() ) ;
			case 2 :
				return new Locale( parts[0].toLowerCase(), parts[1].toUpperCase() ) ;
			case 3 :
				return new Locale( parts[0].toLowerCase(), parts[1].toUpperCase(), parts[2] ) ;
		}
  	return null ;
	} // createLocale()
	
	// -------------------------------------------------------------------------
  
	/**
	 * Returns a classpath that is used to lookup NLS text files.
	 */
	public Classpath getNLSLookupPath() 
	{
		return this.fileUtil().getLookupPath( SYS_PROP_NLS_PATH ) ;
	} // getPluginClasspath()

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected StringUtil str()
	{
		return StringUtil.current();
	} // str()

	// -------------------------------------------------------------------------
  
  protected FileUtil fileUtil() 
	{
		return FileUtil.current() ;
	} // fileUtil()
	
	// -------------------------------------------------------------------------
  
} // class NLSUtil 
