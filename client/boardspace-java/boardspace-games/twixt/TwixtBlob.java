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
package twixt;

import lib.*;

class BlobStack extends OStack<TwixtBlob>
{
	public TwixtBlob[] newComponentArray(int n) { return(new TwixtBlob[n]); }
}
class TwixtConnection
{
	TwixtBlob to;
	TwixtCell through;
	TwixtConnection next;
	public TwixtConnection(TwixtBlob connto,TwixtCell conthru,TwixtConnection nex)
	{	to=connto;
		through=conthru;
		next=nex;
	}
}
class CombinedBlobStack extends OStack<CombinedTwixtBlob> implements TwixtConstants
{
	public CombinedTwixtBlob[] newComponentArray(int n) { return(new CombinedTwixtBlob[n]); }
	public void addLinks(TwixtBoard b) { for(int lim=size()-1; lim>=0; lim--) { elementAt(lim).addLinks(b); }}
	public void removeLinks() { for(int lim=size()-1; lim>=0; lim--) { elementAt(lim).removeLinks(); }}
	public PieceColor color() { if(size()>0) { return(elementAt(0).color()); } return(null); }
}

// common elements for simple blobs and combined blobs
abstract class BlobCore implements TwixtConstants
{
	PieceColor color;
	PieceColor color() { return(color); }
	int ncols;
	int ncomponents = 0;
	char leftCol = 0;
	char rightCol = 0;
	int topRow = 0;
	int bottomRow = 0;
	int hasLeftEdge = 0;
	int hasRightEdge = 0;
	boolean hasLeftEdge()
	{	return(hasLeftEdge>=2);
	}
	boolean hasRightEdge()
	{	return(hasRightEdge>=2);
	}

	@SuppressWarnings("deprecation")
	public String toString()
    {
      return("<"+getClass().getName()+":"+ span()+ color+" "+leftCol+"-"+rightCol+","+topRow+"-"+bottomRow+">");
    }
    //
    // this is a basic metric - the span of the blob in the direction
    // the player cares about.
    //
    public double span()
    {  // playerChar records what color the player is playing
    	switch(color)
    	{case Black: 
    		double left = leftCol;
    		if(hasLeftEdge()) { left=Math.min(left,'B'+0.5); }
    		double right = rightCol;
    		if(hasRightEdge()) { right =Math.max(right, (char)('A'+ncols-2.5)); }
    		return(right-left); 
    	case Red: 
    		double top = topRow;
    		if(hasLeftEdge()) { top = Math.min(top,1.5); }
    		double bottom = bottomRow;
    		if(hasRightEdge()) { bottom = Math.max(ncols-1.5,bottom); }
    		return(bottom-top); 
    	default: G.Error("not expected");
    	}
    	return(0);
    }

	public boolean potentiallyConnects(BlobCore other)
	{
		if(rightCol+4 < other.leftCol) { return(false); }
		if(topRow-4 > other.bottomRow) { return(false); }
		if(leftCol-4 > other.rightCol) { return(false); }
		if(bottomRow+4 < other.topRow) { return(false); }
		return(true);
	}
	void combineWith(BlobCore blob)
	{	G.Assert(color==blob.color,"blob color mismatch"); 
		if(ncomponents==0)
		{ leftCol = blob.leftCol;
		  topRow = blob.topRow;
		  rightCol = blob.rightCol;
		  bottomRow = blob.bottomRow;
		  hasLeftEdge = blob.hasLeftEdge;
		  hasRightEdge = blob.hasRightEdge;
		}
	else {
		leftCol = (char)Math.min(leftCol, blob.leftCol);
		topRow = Math.min(topRow, blob.topRow);
		rightCol = (char)Math.max(rightCol,blob.rightCol);
		bottomRow = Math.max(bottomRow, blob.bottomRow);
		hasLeftEdge += blob.hasLeftEdge;
		hasRightEdge += blob.hasRightEdge;
		}
		ncomponents++;
	}
}
class CombinedTwixtBlob extends BlobCore
{	BlobStack blobs = new BlobStack();
	CombinedTwixtBlob(PieceColor co,int nc) { color = co; ncols=nc; }

	void addBlob(TwixtBlob blob)
	{	
		
		combineWith(blob);
		blobs.push(blob);		
	}
	// remove all links from actual cells
	void removeLinks()
	{
		for(int lim = blobs.size()-1; lim>=0; lim--) { blobs.elementAt(lim).removeLinks(); }
	}
	// addLinks to double connections of cells
	void addLinks(TwixtBoard b)
	{
		for(int lim = blobs.size()-1; lim>=0; lim--) { blobs.elementAt(lim).addLinks(b); }	
	}

}

public class TwixtBlob extends BlobCore
{
	boolean combined=false;
	int expandedToEdge = 0;
	CombinedTwixtBlob parent=null;
	CellStack cells = new CellStack();
	TwixtBlob connectFrom = null;
	TwixtConnection connections;	// connections to other blobs (ie shared liberties)
	TwixtBlob mergedWith=null;		// linked list of other blobs we're merged with
	// constructor
	public TwixtBlob(PieceColor col,int nc) { color = col; ncols=nc; }
	
	// remove all links from actual cells
	void removeLinks()
	{
		for(int lim = cells.size()-1; lim>=0; lim--) 
		{ TwixtCell c = cells.elementAt(lim);
		  while(c.height()>1) { c.removeTop(); }
		}
	}
	// add all possible links from from.  We are part of a blob that 
	// has been expanded, so only the natural link directions need to
	// be considered and illegal crossing links are not a factor to 
	// consider.
	void addLinks(TwixtCell from)
	{
		for(TwixtChip bridge : color.getBridges())
		{	if(!from.containsChip(bridge))
			{
			TwixtCell next = from.bridgeTo(bridge);
			if(next!=null && !next.isEmpty()) { from.addChip(bridge); }
			}
		}
	}
	// addLinks to double connections of cells
	void addLinks(TwixtBoard b)
	{	for(TwixtConnection conn = connections; conn!=null; conn=conn.next)
		{
			TwixtCell thru = conn.through;
			if(thru.isEmpty()) 
				{ b.addChip(thru,color.getPeg()); 
				  
				}
		}
		for(TwixtConnection conn = connections; conn!=null; conn=conn.next)
		{
		TwixtCell thru = conn.through;
		addLinks(thru); 
		}
		for(int lim = cells.size()-1; lim>=0; lim--) { addLinks(cells.elementAt(lim)); }	
	}
	

	public int size() { return(cells.size());}
	//pretty print
	public String toString()
    {
      return("<blob:"+ span()+" "+ ((cells==null)?"m":"") + color+" "+leftCol+"-"+rightCol+","+topRow+"-"+bottomRow+">");
    }

	 // add a connection, watch out for duplicate points.  We especially
	 // care when the number of connections is two or more.
	public void addConnection(TwixtBlob to,TwixtCell cell)
	  { 
		for(TwixtConnection conn=connections;  conn!=null;  conn=conn.next)
			{	if((conn.to==to) && (conn.through==cell)) return;
			}
			connections = new TwixtConnection(to,cell,connections);
	  }
	

    // count the number of cells that connect two blobs
    int numConnTo(TwixtBlob to)
    {
      TwixtConnection conn = connections;
      int val = 0;
      while(conn!=null)
      { if(to==conn.to) { val++; }
        conn = conn.next;
      }
      return(val);
    }
    
    // add a cell to this blob and maintain the books
    void addCell(TwixtCell cell)
    {
    	// maintain the bounding box
    	if(cells.size()==0)
    	{ leftCol =rightCol = cell.col;
    	  topRow = bottomRow = cell.row;
    	}
    	else
    	{
    	leftCol = (char)Math.min(leftCol,cell.col);
    	rightCol = (char)Math.max(rightCol,cell.col);
    	topRow = Math.min(topRow,cell.row);
    	bottomRow = Math.max(bottomRow,cell.row);
    	}
    	// if adjacent to the edge, can't be blocked
    	switch(color)
    	{
    	default: throw G.Error("Not expected");
    	case Red:
    		if(cell.row<=2) { hasLeftEdge += 100; }
    		if(cell.row+1>=ncols) { hasRightEdge +=100; }
    		break;
    	case Black:
    		if(cell.col<='B') { hasLeftEdge += 100; }
    		if((cell.col-'A'+2)>=ncols) { hasRightEdge += 100; }
    		break;
    	}
    	cells.push(cell);
    	cell.blob = this;
    	   	
    }

    // absorb a brother blob
    void absorb(TwixtBlob b)
    {	CellStack cells = b.cells;
    	while(cells.size()>0)
    	{	addCell(cells.pop());
    	}
    	hasLeftEdge += b.hasLeftEdge;
    	hasRightEdge += b.hasRightEdge;
    	expandedToEdge += b.expandedToEdge;
    }
    
 }
