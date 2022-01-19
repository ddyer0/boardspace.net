// ===========================================================================
// CONTENT  : CLASS ResourceBundleTextProvider
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 25/03/2006
// HISTORY  :
//  25/03/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.nls ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This is a simple wrapper around a resource bundle to give it the same 
 * interface ITextProvider as available for a TextContainer.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class ResourceBundleTextProvider implements IExtendedTextProvider
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

	// =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private ResourceBundle resourceBundle = null ;
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ResourceBundleTextProvider( ResourceBundle bundle )
  {
    super() ;
    this.setResourceBundle( bundle ) ;
  } // ResourceBundleTextProvider() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the text associated with the given key or null if the key cannot
   * be found.
   * <br>
   * If the specified key is null then null will be returned.
   * 
   * @param key The identifier for the text
   */
  public String getText( String key ) 
	{
  	if ( key == null )
		{
			return null ;
		}
		return this.getResourceBundle().getString( key ) ;
	} // getText() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if this text provider holds no text and also its default 
   * text provider contains nothing.
   */
  public boolean containsNothing() 
  {
  	return ! this.getResourceBundle().getKeys().hasMoreElements() ;
  } // containsNothing()
  
  // -------------------------------------------------------------------------

  /**
   * Returns the locales for which this text provider has text
   */
  public Locale[] getLocales() 
  {
  	return new Locale[] { this.getLocale() } ;
  } // getLocales()

  // -------------------------------------------------------------------------

  /**
   * Returns the locale the underlying resource bundle was created
   */
  public Locale getLocale() 
	{
		return this.getResourceBundle().getLocale() ;
	} // getLocale()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the wrapped resource bundle
   */
  public ResourceBundle getResourceBundle()
	{
		return resourceBundle;
	} // getResourceBundle() 

  // -------------------------------------------------------------------------
  
  /**
   * Set the wrapped resource bundle
   */
  public void setResourceBundle( ResourceBundle newValue )
	{
		resourceBundle = newValue;
	} // setResourceBundle() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class ResourceBundleTextProvider 
