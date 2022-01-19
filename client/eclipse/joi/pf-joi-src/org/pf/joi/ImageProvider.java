// ===========================================================================
// CONTENT  : CLASS ImageProvider
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 09/04/2004
// HISTORY  :
//  09/04/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.net.URL;

import javax.swing.ImageIcon;

/**
 * This singleton is responsible to provide all icons and image JOI needs.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ImageProvider
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final String PACKAGE_NAME					= 
		InspectionRenderer.class.getPackage().getName().replace( '.', '/' ) ; 

	private static final String ICONS_DIR						= "icons/" ;
	private static final String LOGO_ICON_FILE			= "joi.gif" ;
	private static final String ROOT_ICON_FILE			= "root_co.gif" ;
	private static final String PACKAGE_ICON_FILE		= "default_co.gif" ;
	private static final String PUBLIC_ICON_FILE		= "public_co.gif" ;
	private static final String PROTECTED_ICON_FILE	= "protected_co.gif" ;
	private static final String PRIVATE_ICON_FILE		= "private_co.gif" ;

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static ImageProvider soleInstance = new ImageProvider() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private ImageIcon logoIcon = null ;
  public ImageIcon getLogoIcon() { return logoIcon ; }
  protected void setLogoIcon( ImageIcon newValue ) { logoIcon = newValue ; }  
  
  private ImageIcon rootIcon = null ;
  public ImageIcon getRootIcon() { return rootIcon ; }
  protected void setRootIcon( ImageIcon newValue ) { rootIcon = newValue ; }
    
  private ImageIcon packageIcon = null ;
  public ImageIcon getPackageIcon() { return packageIcon ; }
  protected void setPackageIcon( ImageIcon newValue ) { packageIcon = newValue ; }  

  private ImageIcon publicIcon = null ;
  public ImageIcon getPublicIcon() { return publicIcon ; }
  protected void setPublicIcon( ImageIcon newValue ) { publicIcon = newValue ; }
  
  private ImageIcon protectedIcon = null ;
  public ImageIcon getProtectedIcon() { return protectedIcon ; }
  protected void setProtectedIcon( ImageIcon newValue ) { protectedIcon = newValue ; }

  private ImageIcon privateIcon = null ;
  public ImageIcon getPrivateIcon() { return privateIcon ; }
  protected void setPrivateIcon( ImageIcon newValue ) { privateIcon = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  /**
   * Returns the only instance this class supports (design pattern "Singleton")
   */
  public static ImageProvider instance()
  {
    return soleInstance ;
  } // instance()

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  private ImageProvider()
  {
    super() ;
    this.initialize() ;
  } // ImageProvider()

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void initialize()
	{
		this.setLogoIcon( this.createIcon( LOGO_ICON_FILE ) ) ;
		this.setRootIcon( this.createIcon( ROOT_ICON_FILE ) ) ;
		this.setPackageIcon( this.createIcon( PACKAGE_ICON_FILE ) ) ;
		this.setPublicIcon( this.createIcon( PUBLIC_ICON_FILE ) ) ;
		this.setProtectedIcon( this.createIcon( PROTECTED_ICON_FILE ) ) ;
		this.setPrivateIcon( this.createIcon( PRIVATE_ICON_FILE ) ) ;
	} // initialize()

  // -------------------------------------------------------------------------

	protected ImageIcon createIcon( String filename )
	{
		URL url ;
		String filePath ;

		filePath = PACKAGE_NAME + "/" + ICONS_DIR + filename ;
		
		url = this.findFileOnClasspath( filePath ) ;
		if ( url == null )
		{
			System.err.println( "file '" + filePath + "' not found" ) ;
			return null ;
		}
		else
			return new ImageIcon( url ) ;		
	} // createIcon()

  // -------------------------------------------------------------------------

  /**
   * Tries to find the file with the given Name on the classpath.
   * If the file was found and really exists, then it will be returned.
   * In all other cases null will be returned.
   * <h3>Copied from com.pf.file.FileFinder, because JOI should be
   * independent of other proprietary packages</h3>
   */
  protected URL findFileOnClasspath( String filePath ) 
  {
    ClassLoader cl 							= null ;
    URL url											= null ;

    try
  	{
		  cl = this.getClass().getClassLoader() ;
		  if ( cl == null )
		  {
		    // System.out.println( "No classloader found !\n<P>" ) ;
		    return null ;
		  }
		  url = cl.getResource( filePath ) ;
		  if ( url == null )
		  {
		    System.out.println( "ERROR: File '" + filePath + "' not found in CLASSPATH !!!" ) ;
		  }
    }
    catch ( Exception ex )
    {
      ex.printStackTrace() ;
    }
    return url ;
  } // findFileOnClasspath()

  // -------------------------------------------------------------------------

} // class ImageProvider 
