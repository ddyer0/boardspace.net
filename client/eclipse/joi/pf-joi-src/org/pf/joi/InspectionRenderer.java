// ===========================================================================
// CONTENT  : CLASS InspectionRenderer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 09/04/2004
// HISTORY  :
//  25/01/2002  duma  CREATED
//	09/04/2004	duma	changed	-->	Use ImageProvider now
//
// Copyright (c) 2002-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;
import java.lang.reflect.Modifier;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * A tree cell renderer that produces the icons and labels for an inspected
 * object's elements.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class InspectionRenderer extends DefaultTreeCellRenderer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public InspectionRenderer()
  {
		super() ;
    initialize() ;
  } // InspectionRenderer() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  public Component getTreeCellRendererComponent(JTree tree, Object value,
																							  boolean sel,
																							  boolean expanded,
																							  boolean leaf, int row,
																							  boolean has_Focus) 
	{
		Spy element									= null ;
		ElementSpyTreeNode treeNode	= null ;
		int mod										= 0 ;
		
		if ( leaf ) 
		{
			if ( value instanceof ElementSpyTreeNode ) 
			{
				treeNode = (ElementSpyTreeNode)value ;
				element = treeNode.getModel() ;
				mod = element.getModifiers() ;
				if ( Modifier.isPrivate(mod) )
					this.setLeafIcon( this.getPrivateIcon() ) ;
				else if ( Modifier.isProtected(mod) )
					this.setLeafIcon( this.getProtectedIcon() ) ;
				else if ( Modifier.isPublic(mod) )
					this.setLeafIcon( this.getPublicIcon() ) ;
				else 
					this.setLeafIcon( this.getPackageIcon() ) ;
			}
			else
			{
				this.setLeafIcon( this.getRootIcon() ) ;
			}
		}
		
		return super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row, has_Focus ) ;
				
	} // getTreeCellRendererComponent() 

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	protected void initialize()
	{
		this.setOpenIcon( this.getRootIcon() ) ;		
		this.setLeafIcon( this.getPrivateIcon() ) ;		
	} // initialize() 

  // -------------------------------------------------------------------------

  public ImageIcon getRootIcon()
	{
		return this.imageProvider().getRootIcon() ;
	} // getRootIcon()

  // -------------------------------------------------------------------------
  
	public ImageIcon getPackageIcon()
	{
		return this.imageProvider().getPackageIcon() ;
	} // getPackageIcon()
	
	// -------------------------------------------------------------------------
	
	public ImageIcon getPublicIcon()
	{
		return this.imageProvider().getPublicIcon() ;
	} // getPublicIcon()
	
	// -------------------------------------------------------------------------
	
	public ImageIcon getProtectedIcon()
	{
		return this.imageProvider().getProtectedIcon() ;
	} // getProtectedIcon()
	
	// -------------------------------------------------------------------------
	
	public ImageIcon getPrivateIcon()
	{
		return this.imageProvider().getPrivateIcon() ;
	} // getPrivateIcon()
	
	// -------------------------------------------------------------------------
	
	protected ImageProvider imageProvider() 
	{
		return ImageProvider.instance() ;
	} // imageProvider() 

	// -------------------------------------------------------------------------
	
} // class InspectionRenderer 
