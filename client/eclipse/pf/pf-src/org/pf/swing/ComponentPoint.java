// ===========================================================================
// CONTENT  : CLASS ComponentPoint
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 26/01/2008
// HISTORY  :
//  26/01/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.swing ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Represents a point relative to a component.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class ComponentPoint extends Point
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Component component ;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a component and relative position x=0, y=0.
   * 
   * @param component The component to which the point is relative
   */
  public ComponentPoint( Component component )
  {
    this( component, 0, 0) ;
  } // ComponentPoint()

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a component and a point.
   * 
   * @param component The component to which the point is relative
   * @param p The point within the component 
   */
  public ComponentPoint( Component component, Point p )
  {
  	super(p) ;
  	this.component = component ;
  } // ComponentPoint()
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a component and relative coordinates.
   * 
   * @param component The component to which the point is relative
   * @param x The point's x coordinate
   * @param y The point's y coordinate
   */
  public ComponentPoint( Component component, int x, int y )
  {
  	super( x, y ) ;
  	this.component = component ;
  } // ComponentPoint()
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with values to derive from the given
   * mouse event.
   * 
   * @param mouseEvent The event that contains the component and coordinates of the mouse click
   */
  public ComponentPoint( MouseEvent mouseEvent )
  {
  	this( mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY() ) ;
  } // ComponentPoint()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the component the point is relative to.
   */
  public Component getComponent() 
	{
		return component ;
	} // getComponent()
	
	// -------------------------------------------------------------------------

  /**
   * Returns the point coordinates without the component. 
   */
  public Point getPoint() 
	{
		return new Point(this) ;
	} // getPoint()
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class ComponentPoint
