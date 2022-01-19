// ===========================================================================
// CONTENT  : CLASS NLSIniFileLoader
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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.pf.file.FileLocator;
import org.pf.settings.Settings;
import org.pf.settings.rw.IniReaderWriter;
import org.pf.text.StringFilter;
import org.pf.text.StringUtil;
import org.pf.util.CollectionUtil;

/**
 * This loader is capable of reading text values from an NLS-ini file to
 * one or more TextContainer objects.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class NLSIniFileLoader
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

	// =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public NLSIniFileLoader()
  {
    super() ;
  } // NLSIniFileLoader() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns a text container with all texts for the specified locale from
   * the given file.
   */
  public TextContainer loadText( File file, String localeName )
	{
  	return this.loadText( file, this.nls().createLocale( localeName ) ) ;
	} // loadText() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns a text container with all texts for the specified locale from
   * the given file.
   */
  public TextContainer loadText( File file, Locale locale )
  {
  	return this.loadText( FileLocator.create( file ), locale ) ;
  } // loadText() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a list of text containers with all texts for the specified locales 
   * from the given file.
   */
  public TextContainerList loadText( File file, Locale[] locales )
  {
  	return this.loadText( FileLocator.create( file ), locales ) ;
  } // loadText() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a list of text containers with all texts for the specified locales 
   * from the given file.
   * Returns null if the given file is null or the localeNames are null or empty.
   */
  public TextContainerList loadText( File file, String[] localeNames )
  {
  	return this.loadText( FileLocator.create( file ), localeNames ) ;
  } // loadText() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a text container with all texts for the specified locale from
   * the given file.
   */
  public TextContainer loadText( FileLocator fileLocator, String localeName )
  {
  	return this.loadText( fileLocator, this.nls().createLocale( localeName ) ) ;
  } // loadText() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a text container with all texts for the specified locale from
   * the given file.
   */
  public TextContainer loadText( FileLocator fileLocator, Locale locale )
  {
  	TextContainerList containerList ;
  	
  	containerList = this.loadTextContainers( fileLocator, new Locale[] { locale } ) ;
  	return containerList.getTextContainer( locale ) ;
  } // loadText() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a list of text containers with all texts for the specified locales 
   * from the given file.
   */
  public TextContainerList loadText( FileLocator fileLocator, Locale[] locales )
  {
  	return this.loadTextContainers( fileLocator, locales ) ;
  } // loadText() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a list of text containers with all texts for the specified locales 
   * from the given file.
   * Returns null if the given file is null or the localeNames are null or empty.
   */
  public TextContainerList loadText( FileLocator fileLocator, String[] localeNames )
  {
  	Locale[] locales ;
  	
  	if ( ( fileLocator == null ) || ( this.str().isNullOrEmpty( localeNames ) ) )
  	{
  		return null ;
  	}
  	locales = this.stringsToLocales( localeNames ) ;
  	if ( this.coll().isNullOrEmpty( locales) )
  	{
  		return null ;
  	}
  	return this.loadTextContainers( fileLocator, locales ) ;
  } // loadText() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the default encoding that will be used to read text from a file
   * if no explicit encoding was provided.
   * 
   * @return "UTF-8"
   */
  public String getDefaultEncoding() 
	{
		return "UTF-8" ;
	} // getDefaultEncoding() 
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected TextContainerList loadTextContainers( FileLocator locator, Locale[] locales ) 
  {
  	Settings settings ;
  	IniReaderWriter reader ;
  	String[] keyNames ;
  	TextContainerList textContainers ;
  	String[] localeNames ;
  	String[] metadataKeys ;
  	String[] textKeys ;
  	StringFilter metadataKeyFilter ;
  	
  	reader = new IniReaderWriter( locator, this.getCharacterEncoding() ) ;
  	settings = reader.loadSettings() ;
  	if ( settings == null )
		{
			return null ;
		}
  	textContainers = this.createTextContainerList( locales ) ;
  	localeNames = this.localesToStrings( locales ) ;
  	keyNames = settings.getCategoryNames() ;
  	metadataKeyFilter = new StringFilter() 
  	{
  		final String prefix = getMetadataIndicator();
  		public boolean matches( String aString )
  		{
  			return aString.startsWith( prefix );
  		}
  	};
  	metadataKeys = this.str().copy( keyNames, metadataKeyFilter ) ;
  	textKeys = this.str().copyWithout( keyNames, metadataKeyFilter ) ;
  	this.handleMetadataSections( settings, metadataKeys ) ;
  	this.handleTextKeys( textContainers, settings, textKeys, localeNames ) ;
  	return textContainers ;
  } // loadTextContainers() 
  
  // -------------------------------------------------------------------------

  protected void handleTextKeys( TextContainerList textContainers, Settings settings, String[] textKeys, String[] localeNames )
	{
  	TextContainer container ;
  	String textId ;
  	String text ;
  	
  	for (int i = 0; i < localeNames.length; i++ )
		{
  		container = textContainers.getTextContainer( localeNames[i] ) ;
  		for (int j = 0; j < textKeys.length; j++ )
			{
  			textId = textKeys[j] ;
  			text = settings.getValueOf( textId, localeNames[i] ) ;
  			if ( text != null )
  			{
  				container.addText( textId, text ) ;
  			}
			}
		}		
	} // handleTextKeys()

  // -------------------------------------------------------------------------
  
	protected String[] localesToStrings( Locale[] locales ) 
	{
		String[] strings ;
		
		strings = new String[locales.length] ;
		for (int i = 0; i < strings.length; i++ )
		{
			strings[i] = locales[i].toString() ;
		}
		return strings ;
	} // localesToStrings() 
	
	// -------------------------------------------------------------------------
  
	protected Locale[] stringsToLocales( String[] localeNames ) 
	{
  	List locales ;
  	Locale locale ;
  	
  	locales = new ArrayList(localeNames.length) ;
  	for (int i = 0; i < localeNames.length; i++ )
		{
			locale = this.nls().createLocale( localeNames[i] ) ;
			if ( locale != null )
			{
				locales.add( locale ) ;
			}
		}
  	return (Locale[])this.coll().toArray( locales ) ; 
	} // stringsToLocales()
	
	// -------------------------------------------------------------------------
	
  protected TextContainerList createTextContainerList( Locale[] locales ) 
	{
  	TextContainerList containerList ;
  	
  	containerList = new TextContainerList() ;
  	for (int i = 0; i < locales.length; i++ )
		{
			containerList.add( locales[i].toString(), new TextContainer( locales[i] ) ) ;
		}
  	return containerList ;
	} // createTextContainerList() 
	
	// -------------------------------------------------------------------------
  
  protected void handleMetadataSections( Settings settings, String[] sectionNames ) 
	{
		for (int i = 0; i < sectionNames.length; i++ )
		{
			this.handleMetadataSection( settings, sectionNames[i] ) ;
		}
	} // handleMetadataSections() 
	
	// -------------------------------------------------------------------------
  
  protected void handleMetadataSection( Settings settings, String sectionName ) 
  {
  	// Nothing todo yet
  } // handleMetadataSection() 
  
  // -------------------------------------------------------------------------
  
  protected String getCharacterEncoding() 
	{
		if ( this.getEncoding() == null )
		{
			return this.getDefaultEncoding() ;
		}
		return this.getEncoding() ;
	} // getCharacterEncoding() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the encoding that should be used.
   * Subclasses must override this method in order to support other encoding
   * than the default encoding (i.e. UTF-8).
   * 
   * @see #getDefaultEncoding()
   */
  protected String getEncoding() 
	{
		return null ; // means use default encoding
	} // getEncoding() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Sections that start with the prefix returned by this method are treated
   * as metedata sections. That is, they do not contain localized text for any
   * language.
   */
  protected String getMetadataIndicator() 
	{
		return "@" ;
	} // getMetadataIndicator() 
	
	// -------------------------------------------------------------------------
  
  protected CollectionUtil coll() 
	{
		return CollectionUtil.current() ;
	} // coll()
	
	// -------------------------------------------------------------------------
  
  protected NLSUtil nls() 
	{
		return NLSUtil.current() ;
	} // nls()
	
	// -------------------------------------------------------------------------
  
  protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	//-------------------------------------------------------------------------
  
} // class NLSIniFileLoader 

