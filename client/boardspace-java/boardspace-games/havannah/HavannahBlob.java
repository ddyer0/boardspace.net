/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package havannah;

import lib.*;

class BlobStack extends OStack<HavannahBlob>
{
	public HavannahBlob[] newComponentArray(int n) { return(new HavannahBlob[n]); }
}

/** a service class for the havannah board/robot to keep track of blobs of like-colored
 * markers.  This was derived from the hex blob implentation, but needs different things
 * to detect wins.
 * 
 * @author ddyer
 *
 */
public class HavannahBlob
{ HavannahChip color;	// W or B chip
  HavannahCell cells;	// linked list of cells in this blob
  int size = 0;
  int cornerMask = 0;
  int sideMask = 0;
  boolean ring = false;
  boolean isRing() { return ring;}
  int sideMask() { return sideMask; }
  int cornerMask() { return cornerMask; }
  HavannahConnection connections;	// connections to other blobs (ie shared liberties)
  HavannahBlob mergedWith=null;		// linked list of other blobs we're merged with
  
  // constructor
  public HavannahBlob(HavannahChip col) { color = col; }
 
 //pretty print
 public String toString()
    {
      return("<blob: "+size
    		  + " ring " + ring
    		  +" corner " + G.bitCount(cornerMask())
    		  + " side "+G.bitCount(sideMask()) 
      	+ color+">");
    }
 
 public void checkForRing(int sweep,boolean revised)
 {	ring = false;
 	if(size<6) { return ;}	// smallest possible ring is 6 cells
 	for(HavannahCell c = cells; c!=null && !ring; c = c.nextInBlob)
 		{	for(int dir = 0; dir<c.geometry.n;dir++)
 		{	HavannahCell adj = c.exitTo(dir);
 			// revision 101, the cell doesn't have to be a different color
 			if(adj!=null && (revised || (adj.topChip()!=color)) && !hasEdgeContactFrom(adj,sweep,1)) 
 					{ ring = true;
 					 return;
 					}
 		}}
 }
 
// from is a cell to act are the center of a ring
private boolean hasEdgeContactFrom(HavannahCell from,int sweep,int depth)
{	
	for(int dir = 0;dir<from.geometry.n ;dir++)
		{	HavannahCell adj = from.exitTo(dir);
			if(hasEdgeContact(adj,sweep,depth+1)) 
				{ // if we're in contact in any direction, spread the news
				  // to the previous directions.  This handles the case where
				  // where we follow a blind branch in one direction then find
				  // the edge in a different direction.
				  while(dir>0) 
				  	{ dir--; 
				  	  spreadEdgeContact(from.exitTo(dir),sweep);
				  	}
				  return true;
				}
		}
	return false;
}

private void spreadEdgeContact(HavannahCell adj,int sweep)
{
	if(adj!=null && adj.topChip()!=color && !adj.edgeContact && adj.sweep_counter==sweep)
	  	{ 
		adj.edgeContact = true; 
		for(int dir = 0;dir<adj.geometry.n ;dir++)
		{
	  	spreadEdgeContact(adj.exitTo(dir),sweep);
	  	}
	}
}

private boolean hasEdgeContact(HavannahCell c,int sweep,int depth)
{
	if(c==null)	{ return true;	}
	HavannahChip top = c.topChip();
	if(top==color) { return false; }
	if(c.sweep_counter==sweep) { return c.edgeContact; }	// previously seen
	c.sweep_counter = sweep;
	c.edgeContact = false;
	c.depth = depth;
	boolean v = (c.edgeContact |= hasEdgeContactFrom(c,sweep,depth+1));
	return(v);
}

  public boolean isWin()
  {	
	  if(isRing()) { return true; }
	  if(G.bitCount(sideMask())>=3) { return true; }
	  if(G.bitCount(cornerMask())>=3) { return true; }
	  return false;
  }
 // add a connection, watch out for duplicate points.  We espectially
 // care when the number of connections is two or more.
  public void addConnection(HavannahBlob to,HavannahCell cell)
  { 
	for(HavannahConnection conn=connections;  conn!=null;  conn=conn.next)
	{	if((conn.to==to) && (conn.through==cell)) return;
	}
	connections = new HavannahConnection(to,cell,connections);
  }


    // count the number of cells that connect two blobs
    int numConnTo(HavannahBlob to)
    {
      HavannahConnection conn = connections;
      int val = 0;
      while(conn!=null)
      { if(to==conn.to) { val++; }
        conn = conn.next;
      }
      return(val);
    }
    
    // add a cell to this blob and maintain the books
    void addCell(HavannahCell cell)
    {
    	// maintain the bounding box
    	cornerMask |= cell.cornerMask();
    	sideMask |= cell.sideMask();   	
       	cell.nextInBlob=cells;		// build the linked list of cells in this blob
    	size ++;
        cells = cell;
   	
    }
    // create a merged blob with a merged bounding box
    void mergeBlob(HavannahBlob mergeto)
    { 
      mergeto.mergedWith=this;
      // initialize the bounding box if this is a virgin blob which doesn't contain anything yet
      // punt of the "to" blob has already been merged
      for(HavannahConnection conn = connections; conn!=null; conn=conn.next)
      { if(conn.to == mergeto) return;
      }
      
      cornerMask |= mergeto.cornerMask;
      sideMask |= mergeto.sideMask;
      size += mergeto.size;
      ring |= mergeto.ring;
      
     // add the new blob to thte connections list
      connections = new HavannahConnection(mergeto,null,connections);    
      // recursively merge other blobs
      for(HavannahConnection conn=mergeto.connections; conn!=null; conn=conn.next)
      {	if((conn.to.mergedWith==null) && (mergeto.numConnTo(conn.to)>=2))
      	{ mergeBlob(conn.to);
      	}
      }
    }
    
    // given a vector of simple blobs, create a vector ofmerged blobs
    // consisting of those with two or more links to other blobs.  This is an 
    // approximation to the blobs will surely be able to connection, but it's
    // not quite right in reality.  Sometimes a conneciton can be overloaded
    static BlobStack mergeBlobs(OStack<HavannahBlob> blobs)
    {	BlobStack newblobs = new BlobStack();
    	for(int i=0; i<blobs.size(); i++)
    	{	HavannahBlob cb = blobs.elementAt(i);
    		if(cb.mergedWith==null)
    		{
    		HavannahBlob newb = new HavannahBlob(cb.color);
    		newblobs.push(newb);
    		newb.mergeBlob(cb);
    		}
     	}
    	return(newblobs);
    }
 }
