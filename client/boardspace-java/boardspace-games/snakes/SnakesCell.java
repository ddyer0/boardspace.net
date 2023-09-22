/* copyright notice */package snakes;
import lib.Random;

import lib.*;
import online.game.chipCell;


class CellStack extends OStack<SnakesCell>
{
	public SnakesCell[] newComponentArray(int n) { return(new SnakesCell[n]); }
}

public class SnakesCell extends chipCell<SnakesCell,SnakesChip> implements SnakesConstants
{ 	Coverage cover = new Coverage();
	TileType requiredRole = null;
	int rotation = 0;
	boolean onTarget = false;
	String declined = null;			// temporary storage for debugging special rules
	public SnakesChip[] newComponentArray(int n) { return(new SnakesChip[n]); }
	// constructor
	public SnakesCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = SnakeId.BoardLocation;
	}
	public void reInit()
	{	super.reInit();
		cover.reInit();
		rotation = 0;
		
	}
	public SnakesCell(Random r,SnakeId loc) { super(r,loc); }
	public SnakeId rackLocation() { return((SnakeId)rackLocation); }
	
	// this is the final test for a legal-looking snake tile placement, where the exits from
	// the tile either run into undeveloped space or match up properly.  It doesn't detect
	// mutant snakes with more or less than one head and one tail.  Exits outside the target
	// area or off the board altogether make the placement invalid.
	public boolean testConnections(Coverage conn,	// conn is the proposed cover
									int rotation)	// rotation is the proposed rotation
	{	CellType contype = conn.type;
		// some of the complicated-looking bits here are to simulate the effect of rotating "conn"
		// which changes the exit directions, and likewise the complementary tile's approach direction.
		if(contype==CellType.blank) { return(true); }
		SnakesCell xc0 = exitTo((SnakesBoard.CELL_LEFT()+rotation));
		if((conn.exits[0]&& ((xc0==null) || !xc0.onTarget))		// we care what's adjacent, and it's off target
				||  ((xc0!=null)								// there is something over there
						&& (xc0.cover.type!=CellType.blank) 	// and it's not a blank
						// the approach direction is 2+ the exit direction (mod 4 of course)
						&& (conn.exits[0]!=xc0.cover.exits[(SnakesBoard.CELL_LEFT()+rotation+2)])))	// and it links correctly
			{ return(false); }
		SnakesCell xc1 = exitTo((SnakesBoard.CELL_UP()+rotation));
		if( (conn.exits[1] && ((xc1==null) || !xc1.onTarget)) 
				|| ((xc1!=null) 
						&& ( xc1.cover.type!=CellType.blank) 
						&& (conn.exits[1]!=xc1.cover.exits[(SnakesBoard.CELL_UP()+rotation+SnakesBoard.CELL_HALF_TURN())]))) 
			{ return(false); }
		SnakesCell xc2 = exitTo((SnakesBoard.CELL_RIGHT()+rotation));
		if( (conn.exits[2] && ((xc2==null) || !xc2.onTarget)) 
				|| ((xc2!=null) 
						&& ( xc2.cover.type!=CellType.blank) 
						&& (conn.exits[2]!=xc2.cover.exits[(SnakesBoard.CELL_RIGHT()+rotation+SnakesBoard.CELL_HALF_TURN())]))) 
			{ return(false); }
		SnakesCell xc3 = exitTo((SnakesBoard.CELL_DOWN()+rotation));
		if( (conn.exits[3] && ((xc3==null) || !xc3.onTarget)) 
				|| ((xc3!=null) 
						&& (xc3.cover.type!=CellType.blank) 
						&& (conn.exits[3]!=xc3.cover.exits[(SnakesBoard.CELL_DOWN()+rotation+SnakesBoard.CELL_HALF_TURN())]))) 
			{ return(false); }
		return(true);
	}
	
	// true it it's legal to place a chip, considering the edges of the board
	// and the "blank" cells in each chip.  All the links must be legal or unresolved.
	// but mutant snakes are not considered.
    boolean canPlaceChipLegally(SnakesChip ch,int rotation,boolean targeted,boolean applySpecialRules)
    {	if(onBoard)
    	{
    	Coverage cover0 = ch.cover[0];
    	if(cover0.type!=CellType.blank)
    	{  
    	   if(cover.type!=CellType.blank 
    			|| (targeted&&!onTarget) 
    			|| (targeted && !testConnections(cover0,rotation))
    			|| ((requiredRole!=null) && (TileType.getPlacedRole(cover0.role,rotation)!=requiredRole))
    			) 
    		{  return(false); }}
    	
    	SnakesCell up = exitTo((SnakesBoard.CELL_UP()+rotation));
    	
    	Coverage cover1 = ch.cover[1];
       	if(cover1.type!=CellType.blank) 
     	{ 
     	  if((up==null)
     			||(up.cover.type!=CellType.blank) 
     			|| (targeted&&!up.onTarget)
     			|| (targeted&& !up.testConnections(cover1,rotation))
     			|| ((up.requiredRole!=null) && (TileType.getPlacedRole(cover1.role,rotation)!=up.requiredRole))
     	  		) 
     		{ return(false); }
    	}
    	if(up!=null)
    	{
    	Coverage cover2 = ch.cover[2];
    	if(cover2.type!=CellType.blank)
    	{
		SnakesCell upright = up.exitTo((SnakesBoard.CELL_RIGHT()+rotation));
		if((upright==null) 
				|| (upright.cover.type!=CellType.blank) 
				|| (targeted&&!upright.onTarget) 
				|| (targeted && !upright.testConnections(cover2,rotation))
				|| ((upright.requiredRole!=null) && (TileType.getPlacedRole(cover2.role,rotation)!=upright.requiredRole))
				) 
			{ return(false); }
    	}}
    	
    	Coverage cover3 = ch.cover[3];
    	if(cover3.type!=CellType.blank) 
    	{
		SnakesCell right = exitTo((SnakesBoard.CELL_RIGHT()+rotation));
		if((right==null) 
				|| (right.cover.type!=CellType.blank) 
				|| (targeted&&!right.onTarget)
				|| (targeted&&!right.testConnections(cover3,rotation))
				|| ((right.requiredRole!=null) && (TileType.getPlacedRole(cover3.role,rotation)!=right.requiredRole))
				)
			{ return(false); }
		}
    	}
    
       	return(applySpecialRules ? ch.applySpecialRules(this,rotation) : targeted? ch.applyGivensRules(this,rotation) : true);
    }
    public TileType getTileRole() { return(cover.role); }
    //
    // step from this cell to the next cell linked, but ignore the direction we came from.
    //
    public SnakesCell nextStep(SnakesCell ignore)
    {	boolean exits[] = cover.exits;
    	for(int i=0;i<4;i++) 
    	{	if(exits[i])
    		{	SnakesCell n = exitTo(i);
    			if(n!=ignore) { return(n); }
    		}
    	}
    	return(null);
    }
    public SnakesCell otherEnd(SnakesCell prevStep)
    {	if(this.topChip()!=null)
    	{
		SnakesCell curstep = this;
		do 
    		{
    		SnakesCell p = prevStep;
    		prevStep = curstep;
    		curstep = curstep.nextStep(p);
    		}
			while((curstep!=null)
				&& (curstep!=this)
				&& (curstep.cover.type==CellType.body));
		return(curstep);
    	}
    	return(null);
    }
    //
    // check the other end of a chain of links that starts here.
    // the other end must not be the same (ie; two heads or two tails)
    // and must not be a circle back to here (circular snake).
    // special case for "body" links, we have to find both ends
    // and make sure they're not the same type.
    //
    public boolean otherEndOk()
    {	switch(cover.type)
    	{
    	default: throw G.Error("Not expecting cover type "+cover.type);
    	case blank: 
    		return(true);
    	case body:
    	case head:
    	case tail:
    		SnakesCell prevStep = null;
    		SnakesCell curstep = this;
    		do 
	    		{
	    		SnakesCell p = prevStep;
	    		prevStep = curstep;
	    		curstep = curstep.nextStep(p);
	    		}
    		while((curstep!=null)
    				&& (curstep!=this)
    				&& (curstep.cover.type==CellType.body));
    		
    		if(curstep==null) { return(true); }
    		if(curstep.cover.type == cover.type) { return(false); }
    		if(cover.type==CellType.body)
    		{	// if this is a body cell, we found one end which is a head of a tail.
    			// we now need to find the other end to make sure it's not the same.
    			return(curstep.otherEndOk());
    		}
    		return(true);
    	}
    }
    
    //
    // mutant snakes have 2 heads or 2 tails or are circular.   It's too complicated
    // to figure this out without actually placing the tile, so this is called after
    // placement to see if we just made an illegal shape.
    //
    public boolean isMutant()
    {
		if(onBoard)
		{	if(!otherEndOk()) 
			{ return(true); 
			}

			SnakesCell up = exitTo((SnakesBoard.CELL_UP()+rotation));
			if(up!=null) 
			{ 	if(!up.otherEndOk()) { return(true); }
				SnakesCell upright =  up.exitTo((SnakesBoard.CELL_RIGHT()+rotation));
				if(upright!=null)  { if(!upright.otherEndOk()) { return(true); }; }
			}
		
			SnakesCell right = exitTo((SnakesBoard.CELL_RIGHT()+rotation));
			if(right!=null) { if(!right.otherEndOk()) { return(true); } }
		}
		return(false);
    }
    
    public void addChip(SnakesChip c)
    {	super.addChip(c);
    }

	// add a chip which covers a 2x2 super square, with rotation.
	// the rotated chip must still fit on the board, pivoting around this chip.
	public void addChip(SnakesChip c,int rr)
	{	addChip(c);
		
		if(onBoard)
		{	// the cell we drop a chip on is always the zero position in the coverage map.
			// position 1 is nominally up, but rotated right - down - left by the rotation state
			canPlaceChipLegally(c,rr,true,false);
			// the coverage of a particular square rotates too.
			rotation = rr;
			cover.addChip(c.cover[0],rotation);
			SnakesCell up = exitTo((SnakesBoard.CELL_UP()+rotation));
			if(up!=null)
			{
			Coverage cover = c.cover[1];
			if(cover.type!=CellType.blank)
				{up.addChip(c);
				up.rotation = rotation;
				up.cover.addChip(cover,rotation);
				}
			SnakesCell upright = up.exitTo((SnakesBoard.CELL_RIGHT()+rotation));
			if(upright!=null)
				{
				Coverage upcover = c.cover[2];
				if(upcover.type!=CellType.blank)
				{
				upright.cover.addChip(upcover,rotation);
				upright.addChip(c);
				upright.rotation=rotation;
				}}
			}
			
			SnakesCell right = exitTo((SnakesBoard.CELL_RIGHT()+rotation));
			if(right!=null)
			{
			Coverage rightcover = c.cover[3];
			if(rightcover.type!=CellType.blank)
				{
				right.cover.addChip(rightcover,rotation);
				right.addChip(c);
				right.rotation = rotation;
				}
			}
		}
	}
	public SnakesChip removeTop()
	{	SnakesChip ch = super.removeTop();
		if(onBoard)
		{	// clear the coverage map
			int rot = rotation;
			if(ch.cover[0].type!=CellType.blank) { reInit(); }
			
			SnakesCell up = exitTo((SnakesBoard.CELL_UP()+rot));
			if(up!=null) 
			{ 	SnakesCell upright =  up.exitTo((SnakesBoard.CELL_RIGHT()+rot));
				if((upright!=null) && (ch.cover[2].type!=CellType.blank)) { upright.reInit(); }
				if(ch.cover[1].type!=CellType.blank) { up.reInit(); } 
			}
		
			SnakesCell right = exitTo((SnakesBoard.CELL_RIGHT()+rot));
			if((right!=null) && (ch.cover[3].type!=CellType.blank) ) { right.reInit(); }
		}
		return(ch);
	}
	
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r)
		{ return(super.Digest(r)+randomv*(cover.Digest(r)+rotation));
		}
	static public long Digest(Random r,OStack<SnakesCell>st)
	{	long v=0;
		for(int lim=st.size()-1; lim>=0; lim--) { v ^= st.elementAt(lim).Digest(r); }
		return(v);	
	}
	public boolean sameContents(SnakesCell other)
	{	return(super.sameContents(other)
			&& (onTarget == other.onTarget)
			&& (rotation == other.rotation)
			&& cover.sameContents(other.cover));
	}
	public void copyFrom(SnakesCell other)
	{	super.copyFrom(other);
		requiredRole = other.requiredRole;
		rotation = other.rotation;
		declined = other.declined;
		onTarget = other.onTarget;
		cover.copyFrom(other.cover);
	}
	static public boolean sameCell(SnakesCell c,SnakesCell d)
	{	return((c==null)?(c==d):c.sameCell(d));
	}
}
