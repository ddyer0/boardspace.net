package goban.shape.beans;
// --------------------------------------------------------------------------
//
// NAME                    : LavaLayoutConstraints.java
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

 
// --------------------------------------------------------------------------
// Define the main class.
// --------------------------------------------------------------------------
public class LavaLayoutConstraints 
{

	// -------------------------------
	// Define public member variables.
	// -------------------------------
	public int gridX      = 0;
	public int gridY      = 0;
	public int gridWidth  = 0;
	public int gridHeight = 0;
	
  // -----------------------------------------
  // Define a constructor to set up the class.
  // -----------------------------------------
	public LavaLayoutConstraints() { this( 0, 0, 0, 0 ); }

  // -----------------------------------------
  // Define a constructor to set up the class.
  // -----------------------------------------
	public LavaLayoutConstraints( int x, int y, int dx, int dy ) 
	{
  
  	// Create member varaibles.
  	gridX      = x;
  	gridY      = y;
  	gridWidth  = dx;
  	gridHeight = dy;
	
	}

	// -------------------------------------------------
	// Define a method to clone this set of constraints.
	// -------------------------------------------------
	public Object clone() throws CloneNotSupportedException 
	{
	
		// Create a new set of constraints with same members and return it.
		return new LavaLayoutConstraints( gridX, gridY, gridWidth, gridHeight );
 
	}

}