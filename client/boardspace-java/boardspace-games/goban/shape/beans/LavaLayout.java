package goban.shape.beans;
// --------------------------------------------------------------------------
//
// NAME                    : LavaLayout.java
// VERSION                 : 
// COMPILED USING          : SUN JDK 1.1.2 / KAWA IDE 2.5
// AUTHOR                  : Dan Page (dsp@cs.nott.ac.uk)
// LAST REVISION DATE      : 
// NOTES                   : 
//
// --------------------------------------------------------------------------

// --------------------------------------------------------------------------
// Import all the super duper java class files.
// --------------------------------------------------------------------------
import java.awt.*;
import java.util.*;

import lib.G;
 
// --------------------------------------------------------------------------
// Define the main class.
// --------------------------------------------------------------------------
public class LavaLayout implements LayoutManager 
{

	// --------------------------------
	// Define private member variables.
	// --------------------------------
	private Dimension gridSize   = null;
	private Dimension gapSize    = null;
	private Hashtable<Component,LavaLayoutConstraints > components = null;
	private int PREFERRED        = 0;
	private int MINIMUM          = 1;
	private int MAXIMUM          = 2;

  // -----------------------------------------
  // Define a constructor to set up the class.
  // -----------------------------------------
	public LavaLayout( int dx, int dy ) { this( dx, dy, 0, 0 ); }

  // -----------------------------------------
  // Define a constructor to set up the class.
  // -----------------------------------------
	public LavaLayout( int dx, int dy, int xGap, int yGap ) 
	{
  
  	// Create member variables.
  	gridSize   = new Dimension( dx, dy );
  	gapSize    = new Dimension( xGap, yGap );
  	components = new Hashtable<Component, LavaLayoutConstraints>();
  	
	}

	// ------------------------------------------------------------------
	// Define a method to add a layout component to the layout manager.
	// We need not worry about this due to the addition of constraints in
	// a different method.
	// ------------------------------------------------------------------
	public void addLayoutComponent( String name, Component component ) 
	{
	
	}
	
	// --------------------------------------------------------------
	// Define a method to remove a component from the layout manager.
	// --------------------------------------------------------------
	public void removeLayoutComponent( Component component ) 
	{
  	
  	// Remove the component from the components hashtable.
  	components.remove( component );
  	
	}
	
	// ----------------------------------------------------------------
	// Define a method to set the constraints of a component.  This is
	// the same as in the GridBag layout and provides extra information
	// about the component layout to the layout manager.
	// ----------------------------------------------------------------
	public void setConstraints( Component component, LavaLayoutConstraints constraints ) 
	{
	
		// Add the component/constraints pair to the components hastable.
    try { components.put( component, (LavaLayoutConstraints)constraints.clone() ); }
    catch( CloneNotSupportedException e ) { }
  
	}

	// -----------------------------------------------------------------------------
	// Define a method to retrieve a constraints object for the specified component.
	// -----------------------------------------------------------------------------
	public LavaLayoutConstraints getConstraints( Component component ) 
	{
  
  	// Try and get the contraints using the component as a key.
  	LavaLayoutConstraints constraints = ( LavaLayoutConstraints )( components.get( component ) );
  	
  	// Check if we got the constraints or not.
  	if( constraints != null ) 
  	{
  	
  		// Try and clone the constraints so we can return them to the caller.
  		try { return ( LavaLayoutConstraints )( constraints.clone() ); }
  		catch( CloneNotSupportedException e ) { return null; }
  		
  	}
  	else { return null; }
  	
	}

	// ------------------------------------------------------
	// Define a method to get the minimum size of the layout.
	// ------------------------------------------------------
	public Dimension minimumLayoutSize( Container parent ) { return calcLayoutSize( parent, MINIMUM ); }

	// --------------------------------------------------------
	// Define a method to get the preferred size of the layout.
	// --------------------------------------------------------
	public Dimension preferredLayoutSize( Container parent ) { return calcLayoutSize( parent, PREFERRED ); }

	// --------------------------------------------------------
	// Define a method to implement an algorithm to generically 
	// calculate layout sizes based on type.
	// --------------------------------------------------------
	private Dimension calcLayoutSize( Container parent, int sizeType ) 
	{
	
		// Define local variables.
  	Component component                	= null;
  	Dimension componentSize            	= null;
   	LavaLayoutConstraints componentCons 	= null;
  	Dimension preferredSize            	= null;
  	double maximumX                    	= 0;
  	double maximumY                    	= 0;
  	double tempX                       	= 0;
  	double tempY                       	= 0;
   	int counter                        	= 0;
		//System.out.println("Calc " + parent);
  	// Loop through all the components and generate a maximum x and y size for container.
  	for ( counter = 0; counter < parent.getComponentCount(); counter++ ) 
  	{

    	// Get the current component and its and constraint object.
    	component     = parent.getComponent( counter );
    	componentCons = ( LavaLayoutConstraints )( components.get( component ) );
    
    	// Get the size of the component.
     	if     ( sizeType == PREFERRED ) { componentSize = component.getPreferredSize(); }
    	else if( sizeType == MINIMUM )   
    		{ componentSize = component.getMinimumSize(); 
     		}
    	else if( sizeType == MAXIMUM )   { componentSize = component.getMaximumSize(); }
    	else                             { componentSize = null; }
    
    	// Check if the constraints are null.
    	if( componentCons != null && componentSize != null ) 
    	{
    	
    		// Calculate temp x and y values.
    		tempX = ( double )( G.Width(componentSize) ) / ( double )( componentCons.gridWidth );
    		tempY = ( double )( G.Height(componentSize) ) / ( double )( componentCons.gridHeight );
    	
    		// Check if we need to update current maximum x and y values.
    		if ( tempX > maximumX ) { maximumX = tempX;  }
    		if ( tempY > maximumY ) { maximumY = tempY;  }
    	
    	}
  	
  	}
  	// Generate the final preferred size.
  	int pw = parent.getInsets().left + parent.getInsets().right + ( int )( G.Width(gridSize) * maximumX ) + ( ( G.Width(gridSize) + 1 ) * G.Width(gapSize) );
  	int ph = parent.getInsets().top + parent.getInsets().bottom + ( int )( G.Height(gridSize) * maximumY ) + ( ( G.Height(gridSize) + 1 ) * G.Height(gapSize) );
  	preferredSize        = new Dimension( pw,ph );
  	// System.out.println("Panel "+this+ " size " + preferredSize + " dom x " + dominant_x + " dom y " + dominant_y);
  	// Return the new dimension to caller.
  	return preferredSize;
		       
	}

	// ---------------------------------------------------------------------------
	// Define the all mighty layout method that lays out all the components using
	// their constraints as layout information.
	// ---------------------------------------------------------------------------
	public void layoutContainer( Container parent ) 
	{
	
		// Define local varaibles.
  	Component component                	= null;
  	LavaLayoutConstraints componentCons 	= null;
  	Dimension componentSize            	= null;
  	Point componentLocation            	= null;
   	Dimension gridDimension            	= null;
  	int counter                        	= 0;
  		
  	// Calculate the dimension of each element of the grid.
  	int dwidth  = ( int )( ( double )( parent.getSize().width - parent.getInsets().left - parent.getInsets().right )
  														 / ( double )( gridSize.width ) );
  	int dheight = ( int )( ( double )( parent.getSize().height - parent.getInsets().top - parent.getInsets().bottom )
  														 / ( double )( gridSize.height ) );
  	gridDimension        = new Dimension( dwidth, dheight );

  	//System.out.println("Layout " + parent);
  	// Loop through all the components and lay them all out.
  	for( counter = 0; counter < parent.getComponentCount(); counter++ ) 
  	{
    
    	// Get the current component and its and constraint object.
    	component     = parent.getComponent( counter );
    	componentCons = ( LavaLayoutConstraints )( components.get( component ) );
    	
    	// Check the constraints object is not null.
    	if( componentCons != null )
    	{
    	
    		// Calculate component size.
    		componentSize        = new Dimension( 0, 0 );
    		componentSize.width  = ( int )( gridDimension.width * componentCons.gridWidth - 2 * gapSize.width );
    		componentSize.height = ( int )( gridDimension.height * componentCons.gridHeight - 2 * gapSize.height );
    		
    		// Calculate component location.
    		componentLocation   = new Point( 0, 0 );
    		componentLocation.x = ( int )( gridDimension.width * componentCons.gridX + gapSize.width + parent.getInsets().left );
    		componentLocation.y = ( int )( gridDimension.height * componentCons.gridY + gapSize.height + parent.getInsets().top );
    	
    		// Set the component location and size.
     		component.setSize( componentSize );
    		component.setLocation( componentLocation );
	    	{ Dimension dim = component.getMinimumSize(); 
    	  int difw = dim.width-componentSize.width;
    	  int difh = dim.height-componentSize.height;
    	  if(difw>0) {System.out.println(component + " wants to be " + difw + " pixels wider "+dim.width);}
    	  if(difh>0) {System.out.println(component + " wants to be " + difh + " pixels taller "+dim.height);}
    	  }    	  
     
	    } 
	    else { G.print("No constraints for " + component); }
	    
  	}
  //System.out.println("Finished " + parent);

  
	}

	public void setConstraints(Checkbox changeLibrary,
			LavaLayoutConstraints makeLavaCons) {
		
	}

}