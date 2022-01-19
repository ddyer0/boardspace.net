// ===========================================================================
// CONTENT  : CLASS SwingUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.4 - 03/06/2006
// HISTORY  :
//  31/05/2002  duma  CREATED
//	08/06/2002	duma	added		->	setSystemLookAndFeel()
//	06/06/2004	mdu		added		->	findComponent(..)
//	19/06/2004	mdu		added		->	centerWindow()
//	03/07/2004	mdu		changed	->  Support menus in findComponent(..)
//	03/06/2006	mdu		changed	->	constructor to protected
//
// Copyright (c) 2002-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.swing ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JRootPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Provides support and convenience and helper methods for Swing based UI.
 *
 * @author Manfred Duchrow
 * @version 1.4
 */
public class SwingUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
	private static SwingUtil soleInstance = new SwingUtil() ;

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
	/**
	 * Returns the single instance of this class.
	 */
	public static SwingUtil current()
	{
		return soleInstance ;
	} // current() 

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected SwingUtil()
  {
    super() ;
  } // SwingUtil() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Change the given window's upper left corner coordinates, so that it will be
	 * shown in the center of the screen according to the specified width
	 * and height.
	 * 
	 * @param window The window that must be centered
	 * @param width The width of the window
	 * @param height The height of the window
	 */
	public void centerWindow( Window window, int width, int height )
	{
		Dimension screenSize = null;
		int xpos = 1;
		int ypos = 1;

		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		xpos = (screenSize.width - width) / 2;
		ypos = (screenSize.height - height) / 2;

		window.setBounds(xpos, ypos, width, height);
	} // centerWindow() 
	
  // -------------------------------------------------------------------------

	/**
	 * Change the given window's upper left corner coordinates, so that it will be
	 * shown in the center of the screen.
	 * 
	 * @param window The window that must be centered
	 */
	public void centerWindow( Window window )
	{
		this.centerWindow( window, window.getWidth(), window.getHeight() ) ;
	} // centerWindow() 
	
  // -------------------------------------------------------------------------

	/**
	 * Change the given frame's upper left corner coordinates, so that it will be
	 * shown in the center of the screen according to the specified width
	 * and height.
	 * 
	 * @param frame The frame that must be centered
	 * @param width The width of the frame
	 * @param height The height of the frame
	 */
	public void centerFrame( JFrame frame, int width, int height )
	{
		this.centerWindow( frame, width, height ) ;
	} // centerFrame() 
	
  // -------------------------------------------------------------------------

	/**
	 * Change the given frames upper left corner coordinates, so that it will be
	 * shown in the center of the screen.
	 * 
	 * @param frame The frame that must be centered
	 */
	public void centerFrame( JFrame frame )
	{
		this.centerWindow( frame ) ;
	} // centerFrame() 
	
  // -------------------------------------------------------------------------

	/**
	 * Activates the look & feel for the current operating system.
	 */
	public void setSystemLookAndFeel()
	{
		try
		{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() ) ;
		}
		catch (ClassNotFoundException e)
		{
		}
		catch (InstantiationException e)
		{
		}
		catch (IllegalAccessException e)
		{
		}
		catch (UnsupportedLookAndFeelException e)
		{
		}
	} // setSystemLookAndFeel() 

  // -------------------------------------------------------------------------

	/**
	 * Searches for the component with the specified name in the given 
	 * container and all sub containers.
	 * Returns null if the component cannot be found, otherwise the found 
	 * component.
	 * 
	 * @param container The container in which to search
	 * @param componentName The name of the component 
	 */
	public Component findComponent( Container container, String componentName ) 
  {
		return this.findComponent( container, null, componentName ) ;
  } // findComponent() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Searches for the component with the specified type and name in the given 
	 * container and all sub containers.
	 * Returns null if the component cannot be found, otherwise the found 
	 * component.
	 * 
	 * @param container The container in which to search
	 * @param componentType The type of the component (may be null)
	 * @param componentName The name of the component (may be null)
	 */
	public Component findComponent( Container container, Class componentType, String componentName ) 
  {
		Component component = null ;
		Component[] components ;
		
	  components = this.componentsOf( container ) ;
	  if ( components != null )
		{
			for (int i = 0; i < components.length; i++ )
			{
				if ( componentType != null )
				{
					if ( componentType.isInstance( components[i] ) )
					{
						if ( componentName != null )
						{
							if ( componentName.equals( components[i].getName() ) )
							{
								component = components[i] ;
							}
						}
						else
						{
							component = components[i] ;
						}
					}					
				}
				else
				{
					if ( componentName != null )
					{
						if ( componentName.equals( components[i].getName() ) )
						{
							component = components[i] ;
						}
					}
				}
				if ( ( component == null ) && ( components[i] instanceof Container ) )
				{
					component = this.findComponent( (Container)components[i], componentType, componentName ) ;
				}
				if ( component != null )
				{
					return component ;
				}
			}
		}
	  if ( container instanceof JRootPane )
		{
	  	JRootPane pane = (JRootPane)container ;
			return this.findComponent( pane.getJMenuBar(), componentType, componentName ) ;
		}
	  return null ;
  } // findComponent() 

  // -------------------------------------------------------------------------
	
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected Component[] componentsOf( Container container ) 
	{
	  if ( container instanceof JMenu )
		{
	  	JMenu menu = (JMenu)container ;
			return menu.getMenuComponents() ;
		}
		return container.getComponents() ;
	} // componentsOf()

	// -------------------------------------------------------------------------
	
} // class SwingUtil 
