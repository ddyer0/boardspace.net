// ===========================================================================
// CONTENT  : CLASS NLSIniFileDetector
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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.pf.file.Classpath;
import org.pf.file.ClasspathElement;
import org.pf.file.FileFinder;
import org.pf.file.FileLocator;
import org.pf.settings.Settings;
import org.pf.settings.rw.IniReaderWriter;
import org.pf.text.StringUtil;
import org.pf.util.CollectionUtil;

/**
 * This class is responsible to lookup the classpath for a specified 
 * base name to which it depends the file extension ".ini".
 * All found files are read to retrieve the included metadata which 
 * defines which texts for which locale can be found in which file.
 * Eventually it provides a FileLocator per locale which allows to load
 * the associated texts with a NLSIniFileLoader.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class NLSIniFileDetector
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final boolean DEBUG = "true".equals(System.getProperty("org.pf.nls.NLSIniFileDetector.debug","false")) ; 
	
	public static final String DEFAULT_FILE_EXTENSTION = ".ini" ;
	
	protected static final String FILE_MAPPING_SECTION = "nls-files" ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map fileMappings = null ;
  protected Map getFileMappings() { return fileMappings ; }
  protected void setFileMappings( Map newValue ) { fileMappings = newValue ; }
  
  private Classpath lookupPath = null ;
  protected Classpath getLookupPath() { return lookupPath ; }
  protected void setLookupPath( Classpath newValue ) { lookupPath = newValue ; }
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public NLSIniFileDetector()
  {
    super() ;
    this.reset() ;
  } // NLSIniFileDetector() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Detects all meta information files for the given base name on the
   * current NLS lookup path.
   * <p>
   * This path usually is the classpath. However, it can explicitly set
   * with -Dorg.pf.nls.path=<i>any classpath</i>.
   * <p>
   * After calling this method the getFilesForLocales() can be used to get
   * the names of the file that contain the NLS text data.
   * 
   * @param baseName The filename without file extension (".ini" will be appended)
   * @return true if at least one language-to-file mapping was found.
   */
  public boolean detect( String baseName )
	{
		String filename ;
		
		this.reset() ;
		filename = baseName + DEFAULT_FILE_EXTENSTION ;
		this.readFileMappingsFor( filename, this.getLookupPath() ) ;
		return !this.getFileMappings().isEmpty() ;
	} // detect() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns a file locators for wich a mapping to the given locales have been
   * found.
   * <p>
   * This method should be called only after detect() has been called at least 
   * once before.
   * 
   * @param locales The locales for which to return the corresponding file locators
   */
  public FileLocator[] getFilesForLocales( Locale[] locales ) 
	{
		Collection filenames ;
		String filename ;
		URL url ;
		Collection fileLocators ;
		FileLocator locator ;
		Iterator iter ;
		
		filenames = new HashSet() ;
		if ( ! this.coll().isNullOrEmpty( locales ) )
		{
			for (int i = 0; i < locales.length; i++ )
			{
				filename = (String)this.getFileMappings().get( locales[i].toString() ) ;
				if ( ! this.str().isNullOrEmpty( filename ) )
				{
					filenames.add( filename ) ;
				}
			}
		}
		fileLocators = new ArrayList( filenames.size() ) ;
		iter = filenames.iterator() ;
		while ( iter.hasNext() )
		{
			filename = (String)iter.next();
			url = FileFinder.locateFileOnPath( filename, this.getLookupPath() ) ;
			if ( url != null )
			{
				locator = FileLocator.create( url ) ;
				fileLocators.add( locator ) ;
			}
		}
		return (FileLocator[])this.coll().toArray( fileLocators ) ;
	} // getFilesForLocales() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the file locator for the given locale or null if not found.
   */
  public FileLocator getFileForLocales( Locale locale ) 
	{
		FileLocator[] locators ;
		
		locators = this.getFilesForLocales( new Locale[] { locale } ) ;
		if ( this.coll().isNullOrEmpty( locators ) )
		{
			return null ;
		}
		return locators[0] ;
	} // getFileForLocales() 
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected void reset() 
	{
		this.setFileMappings( new HashMap() ) ;
		this.setLookupPath( this.nls().getNLSLookupPath() ) ;
	} // reset() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Read all classes specified in the given file, if it exists in the specified
   * classpath element. Add those classes to the registry if they can be
   * instantiated and are of the correct type.
   *  
   * @param filename The name of the properties file that contains the class definitions
   * @param classpath The classpath the specified filename to be looked up in
   */
	protected void readFileMappingsFor( String filename, Classpath classpath )
	{
		ClasspathElement[] elements ;
	
		if ( DEBUG )
		{
			this.debug( "Lookup file <" + filename + "> in <" + classpath + ">" ) ;
		}
		elements = classpath.elementsContaining( filename ) ;
		for (int i = 0; i < elements.length; i++)
		{
			if ( DEBUG )
			{
				this.debug( "Found file <" + filename + "> in <" + elements[i].getName() + ">" ) ;
			}
			this.readFileMapping( elements[i], filename ) ;			
		}
	} // readFileMappingsFor() 

	// -------------------------------------------------------------------------

	protected void readFileMapping( ClasspathElement element, String filename )
	{
		IniReaderWriter iniReader ;
		Settings settings ;
				
		iniReader = new IniReaderWriter( filename ) ;
		try
		{
			settings = iniReader.loadSettingsFrom( element ) ;
			if ( settings == null )
			{
				return ;
			}
		}
		catch ( IOException e )
		{
			if ( DEBUG )
			{
				this.debug( "Failed to load file <" + filename + "> from <" + element.getName() + ">" ) ;
			}
			return ;
		}
		this.addFileMappings( settings ) ;
	} // readFileMapping() 

	// -------------------------------------------------------------------------

	protected void addFileMappings( Settings settings ) 
	{
		String[] keys ;
		String filename ;
		
		keys = settings.getKeyNamesOf( FILE_MAPPING_SECTION ) ;
		for (int i = 0; i < keys.length; i++ )
		{
			filename = settings.getValueOf( FILE_MAPPING_SECTION, keys[i] ) ;
			this.getFileMappings().put( keys[i], filename ) ;
		}
	} // addFileMappings() 
	
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
	
	private void debug( String text ) 
	{
		System.out.println( text ) ;
	} // debug() 
	
	// -------------------------------------------------------------------------
	
} // class NLSIniFileDetector 
