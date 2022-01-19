// ===========================================================================
// CONTENT  : CLASS TakeAllLayoutManager
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 13/03/2004
// HISTORY  :
//  13/03/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.swing;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * This layout manager expects only one component in the parent container.
 * Its simple task is to resize the inner component to the container's full
 * available size.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class TakeAllLayoutManager implements LayoutManager
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private int topMargin 		= 0 ;
	private int bottomMargin 	= 0 ;
	private int leftMargin 		= 0 ;
	private int rightMargin 	= 0 ;
	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public TakeAllLayoutManager()
  {
    this(0) ;
  } // TakeAllLayoutManager() 
  
  // -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a margin for all sides.
	 */
	public TakeAllLayoutManager( int margin )
	{
		this( margin, margin, margin, margin ) ;
	} // TakeAllLayoutManager() 
  
	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with the specified margin values.
	 */
	public TakeAllLayoutManager( int left, int right, int top, int bottom )
	{
		super() ;
		this.setLeftMargin( left ) ;
		this.setRightMargin( right ) ;
		this.setTopMargin( top ) ;
		this.setBottomMargin( bottom ) ;
	} // TakeAllLayoutManager() 
  
	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/* (non-Javadoc)
	 * @see java.awt.LayoutManager#layoutContainer(java.awt.Container)
	 */
	public void layoutContainer(Container parent)
	{
		Insets insets ;
		Component comp ;
		int maxWidth ;
		int maxHeight ;
		int nComps ;
	
		insets = parent.getInsets();
		insets.left = insets.left + this.getLeftMargin() ;
		insets.top = insets.top + this.getTopMargin() ;
		insets.right = insets.right + this.getRightMargin() ;
		insets.bottom = insets.bottom + this.getBottomMargin() ;
		
		maxWidth = parent.getSize().width
									 - (insets.left + insets.right);
		maxHeight = parent.getSize().height
										- (insets.top + insets.bottom);
		nComps = parent.getComponentCount();
		for (int i = 0 ; i < nComps ; i++) 
		{
			comp = parent.getComponent(i);
			comp.setBounds( insets.left, insets.top, maxWidth, maxHeight ) ;
		}						
	} // layoutContainer() 

	// -------------------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see java.awt.LayoutManager#addLayoutComponent(java.lang.String, java.awt.Component)
	 */
	public void addLayoutComponent(String name, Component comp)
	{
		// ignore
	} // addLayoutComponent() 

	// -------------------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see java.awt.LayoutManager#minimumLayoutSize(java.awt.Container)
	 */
	public Dimension minimumLayoutSize(Container parent)
	{
		// ignore
		return null ;
	} // minimumLayoutSize() 

	// -------------------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see java.awt.LayoutManager#preferredLayoutSize(java.awt.Container)
	 */
	public Dimension preferredLayoutSize(Container parent)
	{
		// ignore
		return null;
	} // preferredLayoutSize() 

	// -------------------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see java.awt.LayoutManager#removeLayoutComponent(java.awt.Component)
	 */
	public void removeLayoutComponent(Component comp)
	{
		// ignore
	} // removeLayoutComponent() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the current top margin 
	 */
	public int getTopMargin() 
	{ 
		return topMargin ; 
	} // getTopMargin() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the top margin
	 * @param newValue The margin in pixel (must be >=0)
	 */
	public void setTopMargin( int newValue ) 
	{ 
		if ( newValue >= 0 )
			topMargin = newValue ; 
	} // setTopMargin() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the current bottom margin 
	 */
	public int getBottomMargin() 
	{ 
		return bottomMargin ; 
	} // getBottomMargin() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the bottom margin
	 * @param newValue The margin in pixel (must be >=0)
	 */
	public void setBottomMargin( int newValue ) 
	{ 
		if ( newValue >= 0 )
			bottomMargin = newValue ; 
	} // setBottomMargin() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the current left margin 
	 */
	public int getLeftMargin() 
	{ 
		return leftMargin ; 
	} // getLeftMargin() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the left margin
	 * @param newValue The margin in pixel (must be >=0)
	 */
	public void setLeftMargin( int newValue ) 
	{ 
		if ( newValue >= 0 )
			leftMargin = newValue ; 
	} // setLeftMargin() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the current right margin 
	 */
	public int getRightMargin() 
	{ 
		return rightMargin ; 
	} // getRightMargin() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the right margin
	 * @param newValue The margin in pixel (must be >=0)
	 */
	public void setRightMargin( int newValue ) 
	{ 
		if ( newValue >= 0 )
			rightMargin = newValue ; 
	} // setRightMargin() 
	
	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class TakeAllLayoutManager 
