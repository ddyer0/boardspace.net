// ===========================================================================
// CONTENT  : CLASS TextContainerList
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.pf.util.CollectionUtil;
import org.pf.util.NamedValueList ;

/**
 * Can hold many TextContainer objects which can be accessed via a locale
 * name or a Locale object.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class TextContainerList extends NamedValueList<TextContainer> implements IExtendedTextProvider
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Locale[] lookupOrder = null ;
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public TextContainerList()
  {
    super() ;
  } // TextContainerList() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the text container for the given locale name or null if none
   * can be found.
   */
  public TextContainer getTextContainer( String localeName ) 
	{
		return (TextContainer)this.valueAt( localeName ) ;
	} // getTextContainer() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the text container for the given locale or null if none
   * can be found.
   */
  public TextContainer getTextContainer( Locale locale ) 
  {
  	return this.getTextContainer( locale.toString() ) ;
  } // getTextContainer() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Add the text container to this list. If it already exists then replace it.
   */
  @Override
  public void add( String name, TextContainer container )
  {
  	if ( ( name == null ) || ( container == null ) )
		{
			return ;
		}
  	this.remove( name ) ;
  	super.add( name, container );
  } // add() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the locales for which this list has an associated text container
   */
  public Locale[] getLocales() 
	{
		Locale[] locales ;
		TextContainer container ;
		
		locales = new Locale[this.size()] ;
		for (int i = 0; i < this.size(); i++ )
		{
			container = this.textContainerAt(i) ;
			locales[i] = container.getLocale() ;
		}
		return locales ;
	} // getLocales() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the text associated with the given key or null if the key cannot
   * be found.
   * <br>
   * If the specified key is null then null will be returned.
   * <p>
   * Iterates over all containers according to the lookup order
   * or if absent just sequential. Returns the first value found.
   * 
   * @see #setLookupOrder(Locale[])
   * @param key The identifier for the text
   */
  public String getText( String key ) 
  {
  	ITextProvider[] providers ;
  	String text ;
  	
  	providers = this.getOrderedTextProviders() ;
  	for (int i = 0; i < providers.length; i++ )
		{
			text = providers[i].getText( key ) ;
			if ( text != null )
			{
				return text ;
			}
		}
  	return null ;
  } // getText() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true if this text provider holds no text and also its default 
   * text provider contains nothing.
   */
  public boolean containsNothing() 
  {
  	IExtendedTextProvider provider ;
  	
  	for (int i = 0; i < this.size(); i++ )
		{
  		provider = this.textContainerAt(i) ;
  		if ( ! provider.containsNothing() )
  		{
  			return false ;
  		}
		}
  	return true ;
  } // containsNothing()
  
  // -------------------------------------------------------------------------

  /**
   * Returns the current lookup order that will be used within getText(String).
   * 
   * @see #getText(String)
   */
  public Locale[] getLookupOrder()
	{
		return lookupOrder;
	} // getLookupOrder() 

  // -------------------------------------------------------------------------
  
  /**
   * Set the lookup order that will be used within getText(String).
   * 
   * @see #getText(String)
   */
  public void setLookupOrder( Locale[] newValue )
	{
		lookupOrder = newValue;
	} // setLookupOrder() 

  // -------------------------------------------------------------------------
  
  /**
   * Returns the text container at the specified index of null
   */
  public TextContainer textContainerAt( int index ) 
	{
		return (TextContainer)this.valueAt(index) ;
	} // textContainerAt() 
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected ITextProvider[] getOrderedTextProviders() 
	{
  	Locale[] locales ;
  	List providers ;
  	ITextProvider provider ;
  	
  	providers = new ArrayList() ;
  	locales = this.getLookupOrder() ;
		if ( locales == null )
		{
			for (int i = 0; i < this.size(); i++ )
			{
				providers.add( this.textContainerAt(i) ) ;				
			}
		}
		else
		{
			for (int i = 0; i < locales.length; i++ )
			{
				provider = this.getTextContainer( locales[i] ) ;
				if ( provider != null )
				{
					providers.add( provider ) ;
				}
			}
		}
		return (ITextProvider[])CollectionUtil.current().toArray( providers ) ;
	} // getOrderedTextProviders() 
	
	// -------------------------------------------------------------------------
  
} // class TextContainerList 
