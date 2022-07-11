package goban;

import goban.GoConstants.ConnectCode;
import goban.GoConstants.GoId;
import goban.GoConstants.Kind;
import goban.shape.shape.LocationProvider;
import lib.Random;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.stackCell;


class CellStack extends OStack<GoCell>
{
	public GoCell[] newComponentArray(int n) { return(new GoCell[n]); }
}

public class GoCell extends stackCell<GoCell,GoChip> implements LocationProvider
{	
	private int sweepCounter = 0;
	private SimpleGroup theGroup = null;
	private SimpleGroup whiteTerritory = null;
	private SimpleGroup blackTerritory = null;
	public int sweepTag = 0;
	public boolean safeOverride = false;
	private GoBoard board = null;
	private int groupChangeClock = 0;
	private int btChangeClock = 0;
	private int wtChangeClock = 0;
	public int miaiCounter = 0;
	public int getSweepCounter() { return(sweepCounter); }
	public void setSweepCounter(int to)
	{	sweepCounter = to;
	}

	public boolean isAdjacent(GoCell c)
	{
		for(int direction=geometry.n-1; direction>=0; direction--)
		{
			if(exitTo(direction)==c) { return(true); }
		}
		return(false);
	}
	//
	// return true if there are two or more adjacent groups
	// of a type.  This is used to decide if a sole liberty
	// should be filled in endgame.
	//
	public boolean twoOrMoreAdjacentGroups(Kind k)
	{	SimpleGroup g = null; 
		for(int dir=geometry.n; dir>0; dir--)
		{
			GoCell ex = exitTo(dir);
			if(ex!=null) 
			{ 
			  if(ex.getKind()==k)
				  {	SimpleGroup adj = ex.getGroup();
				  	if(g==null) { g = adj; }
				    if(g!=adj) { return(true); }
				  }
			}
		}
		return(false);
	}	

  // 
  // sweep from this cell, continuing over empty cells or cells with "top" color.
  // add elements to group
  //
  public void floodKind(SimpleGroup group)
  {	  if(group.containsPoint(this)==null)
	  {	 
		  if(group.chipIsIncluded(topChip()))
				  { setGroup(group);  
				    group.addElement(this);
				  }
		  for(int direction=0;direction<geometry.n;direction++)
		  {
			  GoCell next = exitTo(direction);
			  if(next!=null)
			  {
			  if(group.chipIsScanned(next.topChip())) { next.floodKind(group); }
			  }
		  }
	  }
  }
	public int countAdjacentSafeLiberties(Kind k)
	{	int libs = 0;
		int sweep = board.incrementSweepCounter();
		sweepTag=sweep;
		for(int direction=geometry.n-1; direction>=0; direction--)
		{
			GoCell next = exitTo(direction);
			if(next!=null)
			{
				SimpleGroup nextGroup = next.getGroup();
				Kind nextKind = nextGroup.getKind();
				if(nextKind.isNowEmpty() && nextKind.isSafeLiberty(k.chip) && (next.sweepTag!=sweep)) 
					{ libs++; 
					  next.sweepTag=sweep; 
					}
				else if(nextKind.fillChip==k.chip)
				{ switch(nextKind)
					{
					default: break;
					case White:
					case Black:
					case SafeWhite:
					case SafeBlack:
					case DeadWhite:
					case DeadBlack: 
						libs += nextGroup.countAdjacentSafeLiberties(sweep,this,ConnectCode.None);
					}
				}
			}
		}
		return(libs);
	}
	public int countAvailableLiberties(Kind k)
	{	int libs = 0;
		int sweep = board.incrementSweepCounter();
		sweepTag=sweep;
		for(int direction=geometry.n-1; direction>=0; direction--)
		{
			GoCell next = exitTo(direction);
			if(next!=null)
			{
				SimpleGroup nextGroup = next.getGroup();
				Kind nextKind = nextGroup.getKind();
				if(nextKind.isNowEmpty() && (next.sweepTag!=sweep)) 
					{ libs++; 
					  next.sweepTag=sweep; 
					}
				else if(nextKind.fillChip==k.chip)
				{ switch(nextKind)
					{
					default: break;
					case White:
					case Black:
					case SafeWhite:
					case SafeBlack:
					case DeadWhite:
					case DeadBlack: 
						libs += nextGroup.countAvailableLiberties(sweep,true);
					}
				}
			}
		}
		return(libs);
	}
  public SimpleGroup getWhiteTerritory()
  {	if(wtChangeClock != board.changeClock) { whiteTerritory=null; }
	if(whiteTerritory==null)
	{	SimpleGroup gr = new SimpleGroup(board,Kind.BlackAndEmpty,"initial white territory");
		floodKind(gr);
		whiteTerritory = gr;
	}
	return(whiteTerritory);   
  }
  public SimpleGroup getBlackTerritory()
  {	if(btChangeClock != board.changeClock) { blackTerritory=null; }
	if(blackTerritory==null)
	{	SimpleGroup gr = new SimpleGroup(board,Kind.WhiteAndEmpty,"initial black territory");
		floodKind(gr);
		blackTerritory = gr;
	}
	return(blackTerritory);   
  }
  
	public SimpleGroup getGroup() 
		{ 	if(groupChangeClock != board.changeClock) { theGroup=null; }
			if(theGroup==null)
			{	SimpleGroup gr = new SimpleGroup(board,topChip());
				floodKind(gr);
				theGroup = gr;
			}
			return(theGroup); 
		}
	public Kind getKind() { return(getGroup().getKind()); }
	public void setGroup(SimpleGroup p)
		{ Kind k = p.getKind();
		  switch(k)
		  {
		  case Black:
		  case White:
		  case Dame:
		  case Empty:
		  case BlackTerritory:
		  case OutsideDame:
		  case WhiteTerritory:
		  case FalseEye:
		  case FillBlack:
		  case FillWhite:
		  case ReservedForWhite:
		  case ReservedForBlack:
		  case SafeWhite:
		  case SafeBlack:
		  case DeadBlack:
		  case DeadWhite:
		  case RemovedBlack:
		  case RemovedWhite:
		  case BlackSnapbackTerritory:
		  case WhiteSnapbackTerritory:
		  case WhiteDame:
		  case BlackDame:
		  case SekiWhite:
		  case SekiBlack:
			  theGroup=p;
			  groupChangeClock = board.changeClock;
			  break;
		  case BlackAndEmpty: 
			  whiteTerritory = p;
			  wtChangeClock = board.changeClock;
			  break;
		  case WhiteAndEmpty:
			  blackTerritory = p;
			  btChangeClock = board.changeClock;
			  break;
		  default: G.Error("not expecting %s",k);
		  }
		}
	public void setKind(Kind k,String r) { theGroup.setKind(k,r); }
	public String displayString = null; 
	public GoChip annotation = null;
	public int annotationStep = 0;
	
	public boolean equals(LocationProvider o) { return((getX()==o.getX()) && (getY()==o.getY())); }
	public boolean equals(int x,int y) { return((getX()==x) && (getY()==y)); }
	public int getX() { return(col-'@'); }
	public int getY() { return(row); }

	public GoChip[] newComponentArray(int n) { return(new GoChip[n]); }
	// constructor
	public GoCell(char c,int r,GoBoard b) 
	{	super(Geometry.Square,c,r);
		rackLocation = GoId.BoardLocation;
		board = b;
	}
	/** upcast the cell id to our local type */
	public GoId rackLocation() { return((GoId)rackLocation); }
	
	public GoCell(Random r) { super(r); }
	public GoCell(Random r,CellId id) { super(r,id); }
	
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) 
	{ return(super.Digest(r)
			^(annotation==null?0:annotation.Digest(r))
			^(safeOverride ? (col*124|row)*r.nextLong() : 0)); }
	
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(GoCell ot)
	{	//GoCell other = (GoCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		annotation=ot.annotation;
		annotationStep = ot.annotationStep;
		theGroup = null;
		safeOverride = ot.safeOverride;
		whiteTerritory = null;
		blackTerritory = null;
	}
	public SimpleGroup getLiberties()
	{
		return(getGroup().getLiberties());
	}
	public int groupSize() { return(getGroup().size()); }
	public boolean hasLiberties()
	{
		return(getLiberties().size()>0);
	}
	public int countLiberties() { return(getLiberties().size()); }
	
	
	// kill something adjacent, but only if its not recapturable.
	public boolean killIfMultipleCapture(GoChip color,Kind kkind)
	{  if(isMultipleCapture(color))
		  {killIfIsCapture(color,kkind);
		   return(true);
		  }
		return(false);
	}
	//
	// multiple captures means more than one group, more than one stone,
	// or a single stone that's not recpaturable.
	//
	public boolean isMultipleCapture(GoChip color)
	{
		int capsize = 0;
		for(int direction=geometry.n-1; direction>=0; direction--)
		  {	GoCell next = exitTo(direction);
		  	if(next!=null)
		  	{	GoChip top = next.topChip();
		  		if(top!=null && top!=color)
		  		{	SimpleGroup gr =next.getGroup();
		  			if(gr.countLiberties()==1) { capsize += gr.size(); }
		  		}
		  	}
		  }
		  return((capsize>1) /*|| !captureWillBeAtari(color)*/);
	}
	 //
	 // kill the groups adjacent to atari if it will capture them
	 // the atari space hasn't been marked as fill yet, so if it 
	 // is the killer, it will still show as a liberty
	 //
	public int killIfIsCapture(GoChip color,Kind kkind)
	  {	  int kills = 0;
		  for(int direction=geometry.n-1; direction>=0; direction--)
		  {	GoCell next = exitTo(direction);
		  	if(next!=null)
		  	{	GoChip top = next.topChip();
		  		if(top!=null && top!=color)
		  		{	SimpleGroup gr =next.getGroup();
		  			if(gr.killIfIsDead(this,kkind)) 
		  				{ kills++;
		  					// when we're killing something by adding to a group, rather than as an independent
		  					// kill stone, manipulate the killed liberty counts, even though this and the original
		  					// group will not be merged.
		  				  GroupStack enemies = gr.adjacentGroups();
		  				  for(int lim=enemies.size()-1; lim>=0; lim--)
		  				  {
		  					  SimpleGroup enemy = enemies.elementAt(lim);
		  					  if(enemy.isLiberty(this))
		  					  {
		  						  kills += enemy.killedLiberties;
		  						  enemy.killedLiberties++;
		  					  }
		  				  }
		  				}
		  		}
		  	}
		  }
		  return(kills);
	  }
	public boolean isSelfAtari(GoChip color)
	{
		for(int direction=geometry.n-1; direction>=0; direction--)
		{	GoCell next = exitTo(direction);
			if(next!=null)
			{	if((next.topChip()==color) && (next.getGroup().countAvailableLiberties()<=2))
				{
					return(true);
				}
			}
		}
		return(false);
	}
	// an eye is an empty cell with the same group on all sides
	public boolean isEye()
	{	SimpleGroup someGroup = null;
		if(topChip()!=null) { return(false); }
		for(int direction=geometry.n-1; direction>=0; direction--)
		{
			GoCell next = exitTo(direction);
			if(next!=null)
			{
				SimpleGroup gr = next.getGroup();
				if((someGroup!=null)&&(someGroup!=gr)) { return(false); }
				someGroup = gr;
			}
		}
		return(true);
	}

	//
	// true if this is a false eye that was generated
	// by endgame captures.  There has to be a fill 
	// adjacent, and the fill can't connect.
	//
	public boolean isFalseEyeCapture()
	{
		GoCell fill = null;
		boolean falseEye = false;
		int nGroups = 0;
		SimpleGroup someGroup = null;
		SimpleGroup otherGroup = null;
		for(int direction=geometry.n-1; direction>=0; direction--)
		{
			GoCell next = exitTo(direction);
			if(next!=null)
			{
				SimpleGroup gr = next.getGroup();
				Kind k = gr.getKind();
				switch(k)
				{
				default: throw G.Error("not expecting %s",k);
				case FalseEye:
					falseEye=true;
					break;
				case RemovedWhite:
				case RemovedBlack:
				case WhiteSnapbackTerritory:
				case BlackSnapbackTerritory:
					break;
				case FillWhite:
				case ReservedForWhite:
				case ReservedForBlack:
				case FillBlack: fill = next;
					//$FALL-THROUGH$
				case WhiteTerritory:
				case BlackTerritory:
					break;
				case OutsideDame:
				case WhiteDame:
				case BlackDame:
				case Dame:		// these recently added, not sure (10/25/2015)
					break;
				case White:
				case Black:
				case DeadBlack:
				case DeadWhite:
				case SafeWhite:
				case SafeBlack:
				case SekiWhite:
				case SekiBlack:
					if(someGroup==null) { nGroups++; someGroup=gr; }
					else if(someGroup!=gr ) { nGroups++; otherGroup=gr; }
				}
			}
		}
		boolean newFalse  = fill!=null && (nGroups>1) && !someGroup.isConnection(fill,otherGroup);
		return(newFalse || falseEye);
	}

	public boolean isFalseEye()
	{	int nGroups = 0;
		int nColors = 0;
		GoChip someColor = null; 
		SimpleGroup someGroup = null;
		if(topChip()!=null) { return(false); }
		for(int direction=geometry.n-1; direction>=0; direction--)
		{
			GoCell next = exitTo(direction);
			if(next!=null)
			{	GoChip top = next.topChip();
				SimpleGroup gr = next.getGroup();
				if(someGroup==null) { someColor = top; someGroup = gr; nGroups++; nColors++; }
				else { if(someColor!=top) { nColors++; } if(gr!=someGroup) { nGroups++; }}
			}}
		return((nGroups>1)&&(nColors==1));
	}
	//
	// this detects the case where a capture is a ko-like capture that can be 
	// recaptured immediately 
	// 
	boolean captureWillBeAtari(GoChip myColor)
	{	// needs to consider multiple captures which make it not atari
		for(int direction=geometry.n-1; direction>=0; direction--)
		{
			GoCell next = exitTo(direction);
			if(next!=null)
			{
				GoChip top = next.topChip();
				if((top==null) || (top==myColor)) { return(false); }
			}
		}
		return(true);
	}
	// if this cell is a place where an enemy pitch would be self atari
	public Kind isGuardedCell(GoCell filled)
	{
		if(topChip()==null)
		{	int empty = 0;
			int white = 0;
			int black = 0;
			for(int direction = geometry.n-1; direction>=0; direction--)
			{
				GoCell next = exitTo(direction);
				if((next!=null) && (next!=filled))
				{
					GoChip top = next.topChip();
					if(top==null) { empty++; }
					else if(next.getGroup().countLiberties()>=2)
					{
					// if won't be atari
					if (top==GoChip.white) { white++; }
					else { black++; }
					}
					}
			}
			if(empty==1)
			{
				if((black>0)&&(white==0)) { return(Kind.ReservedForBlack); }
				else if ((white>0)&&(black==0)) { return(Kind.ReservedForWhite); }
			
			}
		}
		return(Kind.Empty);
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		annotation = null;
		annotationStep = 0;
		groupChangeClock = 0;
		btChangeClock = 0;
		wtChangeClock = 0;
		safeOverride = false;
		theGroup = null;
	}
           
}
