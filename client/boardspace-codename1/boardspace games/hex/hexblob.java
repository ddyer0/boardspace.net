package hex;

import lib.*;

class BlobStack extends OStack<hexblob>
{
	public hexblob[] newComponentArray(int n) { return(new hexblob[n]); }
}

/** a service class for the hex board/robot to keep track of blobs of like-colored
 * markers.  The same data structure is used both to describe "simple blobs" of stones
 * which are directly connected; and "merged blobs" where simple blobs are combined
 * if they will be able to connect.
 * 
 * This is the basic data structure to detect wins, and also for the 
 * rudimentary robot to evaluate positions.  
 * @author ddyer
 *
 */
public class hexblob
{ hexChip color;	// W or B chip
  hexCell cells;	// linked list of cells in this blob
  char leftcol;		// bounding box in board coordinates
  int toprow;
  char rightcol;
  int bottomrow;
  hexConnection connections;	// connections to other blobs (ie shared liberties)
  hexblob mergedWith=null;		// linked list of other blobs we're merged with
  
  // constructor
  public hexblob(hexChip col) { color = col; }
 
 //pretty print
 public String toString()
    {
      return("<blob:"+ span()+" "+ ((cells==null)?"m":"") + color+" "+leftcol+"-"+rightcol+","+toprow+"-"+bottomrow+">");
    }

 // add a connection, watch out for duplicate points.  We espectially
 // care when the number of connections is two or more.
  public void addConnection(hexblob to,hexCell cell)
  { 
	for(hexConnection conn=connections;  conn!=null;  conn=conn.next)
	{	if((conn.to==to) && (conn.through==cell)) return;
	}
	connections = new hexConnection(to,cell,connections);
  }

    //
    // this is a basic metric - the span of the blob in the direction
    // the player cares about.
    //
    public int span()
    {  // playerChar records what color the player is playing
    	if(color==hexChip.White) { return(rightcol-leftcol+1); }
    	if(color==hexChip.Black) { return(bottomrow-toprow+1); }
    	throw G.Error("Not expecting this");
    }

    // count the number of cells that connect two blobs
    int numConnTo(hexblob to)
    {
      hexConnection conn = connections;
      int val = 0;
      while(conn!=null)
      { if(to==conn.to) { val++; }
        conn = conn.next;
      }
      return(val);
    }
    
    // add a cell to this blob and maintain the books
    void addCell(hexCell cell)
    {
    	// maintain the bounding box
    	if(cells==null)
    	{ leftcol =rightcol = cell.col;
    	  toprow = bottomrow = cell.row;
    	}
    	else
    	{
    	leftcol = (char)Math.min(leftcol,cell.col);
    	rightcol = (char)Math.max(rightcol,cell.col);
    	toprow = Math.min(toprow,cell.row);
    	bottomrow = Math.max(bottomrow,cell.row);
    	}
    	
       	cell.nextInBlob=cells;		// build the linked list of cells in this blob
        cells = cell;
   	
    }
    // create a merged blob with a merged bounding box
    void mergeBlob(hexblob mergeto)
    { 
      mergeto.mergedWith=this;
      // initialize the bounding box if this is a virgin blob which doesn't contain anything yet
      if(connections==null) 	
      	{ leftcol = mergeto.leftcol;
      	  rightcol = mergeto.rightcol;
      	  toprow = mergeto.toprow;
      	  bottomrow = mergeto.bottomrow;
      	}
      else
      {
      // punt of the "to" blob has already been merged
      for(hexConnection conn = connections; conn!=null; conn=conn.next)
      { if(conn.to == mergeto) return;
      }
      // combine the bounding boxes
      leftcol = (char)Math.min(leftcol,mergeto.leftcol);
      rightcol = (char)Math.max(rightcol,mergeto.rightcol);
      toprow = Math.min(toprow,mergeto.toprow);
      bottomrow = Math.max(bottomrow,mergeto.bottomrow);
      }
      // add the new blob to thte connections list
      connections = new hexConnection(mergeto,null,connections);    
      // recursively merge other blobs
      for(hexConnection conn=mergeto.connections; conn!=null; conn=conn.next)
      {	if((conn.to.mergedWith==null) && (mergeto.numConnTo(conn.to)>=2))
      	{ mergeBlob(conn.to);
      	}
      }
}
    
    // given a vector of simple blobs, create a vector ofmerged blobs
    // consisting of those with two or more links to other blobs.  This is an 
    // approximation to the blobs will surely be able to connection, but it's
    // not quite right in reality.  Sometimes a conneciton can be overloaded
    static BlobStack mergeBlobs(OStack<hexblob> blobs)
    {	BlobStack newblobs = new BlobStack();
    	for(int i=0; i<blobs.size(); i++)
    	{	hexblob cb = blobs.elementAt(i);
    		if(cb.mergedWith==null)
    		{
    		hexblob newb = new hexblob(cb.color);
    		newblobs.push(newb);
    		newb.mergeBlob(cb);
    		}
     	}
    	return(newblobs);
    }
 }
