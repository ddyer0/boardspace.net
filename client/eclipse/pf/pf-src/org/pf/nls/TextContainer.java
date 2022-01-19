// ===========================================================================
// CONTENT  : CLASS TextContainer
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.pf.text.StringUtil;

/**
 * This is is a simple container for text strings associated with an identifying
 * key. It also allows to set a default TextContainer which will be checked 
 * for a key if this container cannot find it.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class TextContainer implements IExtendedTextProvider
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private IExtendedTextProvider defaultContainer = null ;
  private Locale locale = null ;
  private Map textMap = null ;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public TextContainer( Locale aLocale )
  {
    super() ;
    this.setLocale( aLocale ) ;
    this.setTextMap( this.createNewTextMap( this.getInitialCapacity() ) ) ;
  } // TextContainer() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the default container that will be used if a key cannot be found
   * in this container.
   * 
   * @return A text container of null
   */
  public IExtendedTextProvider getDefaultProvider()
	{
		return defaultContainer;
	} // getDefaultProvider() 

  // -------------------------------------------------------------------------
  
  /**
   * Sets the default container.
   * 
   * @param textContainer A new default container or null to remove any existing
   */
  public void setDefaultProvider( IExtendedTextProvider textContainer )
	{
		defaultContainer = textContainer;
	} // setDefaultProvider() 
  
  // -------------------------------------------------------------------------

  /**
   * Returns the locale for which this container holds the text strings
   */
  public Locale getLocale()
	{
		return locale;
	} // getLocale() 
  
  // -------------------------------------------------------------------------
  
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
		String text ;
		
		if ( key == null )
		{
			return null ;
		}
		
		text = this.getString( key ) ;
		if ( ( text == null ) && ( this.hasDefaultContainer() ) )
		{
			text = this.getDefaultProvider().getText( key ) ;
		}
		return text ;
	} // getText() 
	
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
   * Returns an array with all keys of this container.
   * This does not include keys that might be additionally available through
   * the default container.
   */
  public String[] getKeys() 
	{
		return this.str().asStrings( this.getTextMap().keySet() ) ;
	} // getKeys() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if this container or its default container can provide a
   * text for the specified key.
   * 
   * @see #hasKey(String)
   * @param key The key to lookup
   */
  public boolean containsKey( String key ) 
	{
		return this.getText( key ) != null ;
	} // containsKey() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if this container can provide a text for the specified key.
   * This method returns false even if the key is available in the default
   * container.
   * 
   * @see #containsKey(String)
   * @param key The key to lookup
   */
  public boolean hasKey( String key ) 
  {
  	return this.getText( key ) != null ;
  } // hasKey() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true if this container holds no text.
   * However, it might still have a default container which has text. In such
   * case this method still returns true. To check all (including default containers)
   * use method containsNothing().
   * 
   * @see #containsNothing()
   */
  public boolean isEmpty() 
	{
		return this.getTextMap().isEmpty() ;
	} // isEmpty() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if this container holds no text and also its default container
   * contains nothing.
   * To only check this container disregarding any default container
   * use method isEmpty().
   * 
   * @see #isEmpty()
   */
  public boolean containsNothing() 
  {
  	if ( this.isEmpty() )
		{
			if ( this.hasDefaultContainer() )
			{
				return this.getDefaultProvider().containsNothing() ;
			}
			return true ;
		}
		return false ;
  } // containsNothing() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the number of entries in this container.
   * This does not include any entries in a potentially existing 
   * default container. 
   */
  public int size() 
	{
		return this.getTextMap().size() ;
	} // size() 
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the string or null from the internal map for the given key.
   */
  protected String getString( String key ) 
	{
  	return (String)this.getTextMap().get( key ) ;
	} // getString() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Add the given text under the specified key.
   * If either key or text is null nothing happens.
   * If the key already exists the new text will replace the old one.
   */
  protected void addText( String key, String text ) 
	{
		if ( ( key != null ) && ( text != null ) )
		{
			this.getTextMap().put( key, text ) ;
		}
	} // addText() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns whether or not this container has a default container
   */
  protected boolean hasDefaultContainer() 
	{
		return this.getDefaultProvider() != null ;
	} // hasDefaultContainer() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the initial capacity for the entries in this container.
   * Subclasses may override this method in order to return another value.
   * Here it returns 50.
   */
  protected int getInitialCapacity() 
	{
		return 50 ;
	} // getInitialCapacity() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Creates a new instance of a map implementation.
   * Subclasses may override this method to use different implementation than
   * java.util.HashMap which is used here.
   */
  protected Map createNewTextMap( int initialCapacity ) 
	{
		return new HashMap( initialCapacity ) ;
	} // createNewTextMap() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Set the locale for which this container holds the text strings
   */
  protected void setLocale( Locale newLocale )
	{
		locale = newLocale;
	} // setLocale() 

  // -------------------------------------------------------------------------
  
  protected Map getTextMap()
	{
		return textMap;
	} // getTextMap() 
  
  // -------------------------------------------------------------------------

  protected void setTextMap( Map newValue )
	{
		textMap = newValue;
	} // setTextMap() 
	
	// -------------------------------------------------------------------------
	
  protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	//-------------------------------------------------------------------------
  
} // class TextContainer 
