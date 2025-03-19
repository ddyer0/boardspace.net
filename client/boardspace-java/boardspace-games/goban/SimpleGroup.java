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
package goban;

import goban.shape.shape.ElementProvider;
import goban.shape.shape.Globals;
import goban.shape.shape.LocationProvider;
import goban.shape.shape.ResultProtocol;
import goban.shape.shape.ShapeLibrary;
import goban.shape.shape.ShapeProtocol;
import goban.shape.shape.SingleResult;
import lib.CompareTo;
import lib.G;
import lib.OStack;

class Connection
{
GoCell from;
SimpleGroup to;
Connection(GoCell fr,SimpleGroup tt)
{
	from=fr;
	to=tt;
}
public String toString() { return("<conn "+from+to+">"); }
public static boolean pushNew(ConnectionStack connections,GoCell from,SimpleGroup to)
	{
	for(int lim=connections.size()-1; lim>=0; lim--)
	{
		Connection c = connections.elementAt(lim);
		if((c.to==to)&&(c.from==from)) { return(false); }
	}
	connections.push(new Connection(from,to));
	return(true);
	}
}


class ConnectionStack extends OStack<Connection>
{
	public Connection[] newComponentArray(int n) { return(new Connection[n]); }
}
class GroupStack extends OStack<SimpleGroup>
{
	public SimpleGroup[] newComponentArray(int n) { return(new SimpleGroup[n]); }
}

public class SimpleGroup extends OStack<GoCell> implements ElementProvider,GoConstants,	Globals,
	CompareTo<SimpleGroup>
{	public GoCell[] newComponentArray(int n) { return(new GoCell[n]); }
	private SimpleGroup theLiberties = null;
	private GoBoard board;
	private Kind kind = Kind.Empty;
	private String reason = null;
	public SimpleGroup chippedFrom = null;	// if this is a group chiped from other groups
	public Kind chippedFromKind=null;
	private SimpleGroup theBorder = null;
	public int killedLiberties = 0;
	private GroupStack adjacentGroups = null;
	private GroupStack adjacentEmptyGroups = null;
	private ConnectionStack connections = null;
	private GroupStack embeddedGroups = null;
	private GroupStack embeddedEmptyGroups = null;

	// when filling endgame atari, each group is only filled once
	// this can distinguish known dead ends when connecting groups
	public GoCell atariFill = null;		
	// constructor
	public SimpleGroup(GoBoard b,GoChip top)
	{	super();
		board = b;
		if(top==null) { kind = Kind.Empty; }
		else if(top==GoChip.black) { kind=Kind.Black; }
		else if(top==GoChip.white) { kind=Kind.White; }		else { G.Error("Not expecting top %s",top); }
		reason = "Default Construction";
	}
	// constructor
	public SimpleGroup(GoBoard b,Kind k,String r) 
	{ 
	  super(); 
	  board = b;
	  kind = k; 
	  reason=r; }
	public String toString() { return("<group "+size()+" "+top()+" "+kind+" "+reason+">"); }

	//
	// some simple boolean operations on groups
	//
	public void union(SimpleGroup other)
	{
		for(int lim=other.size()-1; lim>=0; lim--)
		{
			pushNew(other.elementAt(lim));
		}
	}
	public int compareTo(SimpleGroup other)
	{
		return(Integer.signum(size()-other.size()));
	}
	public SimpleGroup copy()
	{
		SimpleGroup c = new SimpleGroup(board,kind,"copy of "+reason);
		c.union(this);
		return(c);
	}
	public void difference(SimpleGroup other)
	{
		for(int lim=other.size()-1; lim>=0; lim--)
		{
			remove(other.elementAt(lim),false);
		}
	}
	

	public void setChipped(SimpleGroup from)
	{	chippedFrom = from;
		chippedFromKind = from.getKind();
	}
	
	public int getXMax()
	{	int max = 0;
		for(int lim=size()-1; lim>=0; lim--)
		{
			GoCell c = elementAt(lim);
			max = Math.max(max,c.getX());
		}
		return(max);
	}
	public int getYMax()
	{	int max = 0;
		for(int lim=size()-1; lim>=0; lim--)
		{
			GoCell c = elementAt(lim);
			max = Math.max(max,c.getY());
		}
		return(max);
	}
	public int getXMin()
	{	int max = 999;
		for(int lim=size()-1; lim>=0; lim--)
		{
			GoCell c = elementAt(lim);
			max = Math.min(max,c.getX());
		}
		return(max);
	}
	public int getYMin()
	{	int max = 999;
		for(int lim=size()-1; lim>=0; lim--)
		{
			GoCell c = elementAt(lim);
			max = Math.min(max,c.getY());
		}
		return(max);
	}
	public X_Position getXPosition()
	{	if(getXMin()==1) { return(X_Position.Left); }
		if(getXMax()==board.boardColumns) { return(X_Position.Right); }
		return(X_Position.Center);
	}
	public Y_Position getYPosition()
	{
		if(getYMin()==1) { return(Y_Position.Top); }
		if(getYMax()==board.boardRows) 
			{ return(Y_Position.Bottom); }
		return(Y_Position.Center);
	}
	// border is all adjacent cells, empty or not
	public SimpleGroup getBorder()
	{	if(theBorder==null)
		{	SimpleGroup bg = new SimpleGroup(board,Kind.Liberty,"Border");
			for(int lim=size()-1; lim>=0; lim--)
			{
				GoCell c = elementAt(lim);
				for(int direction=c.geometry.n-1; direction>=0; direction--)
				{
					GoCell next = c.exitTo(direction);
					if(next!=null)
					{
						if(containsPoint(next)==null) { bg.pushNew(next); }
					}
				}
			}
			theBorder = bg;
		}
		return(theBorder);
	}

	// connections to adjacent groups.  For black/white this will be other black/white groups
	public ConnectionStack getConnections()
	{	if(connections==null)
		{	if(chippedFrom!=null) { return(chippedFrom.getConnections()); }
			connections = new ConnectionStack();
			SimpleGroup libs = getLiberties();
			for(int lim=libs.size()-1; lim>=0; lim--)
			{
				GoCell lib = libs.elementAt(lim);
				for(int direction=lib.geometry.n-1; direction>=0; direction--)
				{
					GoCell next = lib.exitTo(direction);
					if((next!=null) && (next.topChip()==kind.chip))
					{	SimpleGroup gr = next.getGroup();
						if(gr!=this) 
						{	Connection.pushNew(connections,lib,gr);
							

						}
						
					}
				}
			}
		}
		return(connections);
	}
	// true if "c" connects us to "other"
	public boolean isConnection(GoCell c,SimpleGroup other)
	{
		ConnectionStack connections = getConnections();
		for(int lim=connections.size()-1; lim>=0; lim--)
		{
			Connection conn = connections.elementAt(lim);
			if((conn.from==c)&&(conn.to==other)) { return(true); }
		}
		return(false);
	}
	// get the group of "c" connects is to any friendly chip
	SimpleGroup isConnection(GoCell c)
	{
		for(int direction=c.geometry.n-1; direction>=0;direction--)
		{	GoCell next = c.exitTo(direction);
			if(next!=null)
			{	SimpleGroup gr = next.getGroup();
				if(this==gr) {  }
				else if(gr.kind.chip==kind.chip) 
					{ return(gr); }
			}
		}
		return(null);
	}
	
	// adjacent groups will be the opposite color.
	// for mixed groups, it will be the third color
	public GroupStack adjacentEmptyGroups()
	{	
		if(adjacentEmptyGroups==null)
		{
		GroupStack empties = (chippedFrom!=null) 
										? chippedFrom.adjacentGroups()
										: new GroupStack();
		SimpleGroup border = getBorder();
		for(int lim=border.size()-1; lim>=0; lim--)
		{	GoCell adj = border.elementAt(lim);
			SimpleGroup gr = adj.getGroup();
			if(gr!=this)
			{
			Kind k = gr.getKind();
			// note we don't use k.isNowEmpty() because we want this to reflect
			// the state of the actual board.
			switch(k)
			{
			case White:
			case SafeWhite:
			case DeadWhite:
			case SekiBlack:
			case SekiWhite:
			case Black:
			case SafeBlack:
			case DeadBlack:	
			case RemovedBlack:
			case RemovedWhite:
				break;

			case Dame:
			case OutsideDame:
			case BlackDame:
			case WhiteDame:
			case BlackTerritory:
			case WhiteTerritory:
			case FalseEye:
			case FillWhite:
			case FillBlack:
			case ReservedForWhite:
			case ReservedForBlack:

			case WhiteSnapbackTerritory:
			case BlackSnapbackTerritory:
				empties.pushNew(gr);
				break;
			case BlackAndEmpty:
			case WhiteAndEmpty:
			case Liberty:
			case Empty:
			default: G.Error("not expecting %s",k);
			}
			}
		}
		adjacentEmptyGroups = empties;
		}
		return(adjacentEmptyGroups);
	}
	
	// adjacent groups will be the opposite color.
	// for mixed groups, it will be the third color
	public GroupStack adjacentGroups()
	{	
		if(adjacentGroups==null)
		{
		GroupStack enemies = (chippedFrom!=null) 
										? chippedFrom.adjacentGroups()
										: new GroupStack();
		SimpleGroup border = getBorder();
		for(int lim=border.size()-1; lim>=0; lim--)
		{	GoCell adj = border.elementAt(lim);
			SimpleGroup gr = adj.getGroup();
			if(gr!=this)
			{
			Kind k = gr.getKind();
			switch(k)
			{
			case White:
			case SafeWhite:
			case DeadWhite:
			case SekiBlack:
			case SekiWhite:
			case Black:
			case SafeBlack:
			case DeadBlack:	
			case RemovedBlack:
			case RemovedWhite:
				enemies.pushNew(gr); 
				break;

			case Dame:
			case OutsideDame:
			case BlackDame:
			case WhiteDame:
			case BlackTerritory:
			case WhiteTerritory:
			case FalseEye:
			case FillWhite:
			case FillBlack:
			case ReservedForWhite:
			case ReservedForBlack:

			case WhiteSnapbackTerritory:
			case BlackSnapbackTerritory:
				break;
			case BlackAndEmpty:
			case WhiteAndEmpty:
			case Liberty:
			case Empty:
			default: G.Error("not expecting %s",k);
			}
			}
		}
		adjacentGroups = enemies;
		}
		return(adjacentGroups);
	}
	public SimpleGroup getLiberties() 
		{	
			if(theLiberties==null)
			{	if(chippedFrom!=null) { return(chippedFrom.getLiberties()); }
				G.Assert(kind.chip!=null,"is a colored group");
				theLiberties = new SimpleGroup(board,Kind.Liberty,"liberties of "+this);
				for(int lim=size()-1; lim>=0; lim--)
				{
					GoCell member = elementAt(lim);
					for(int direction=member.geometry.n-1; direction>=0; direction--)
					{
						GoCell lib=member.exitTo(direction);
						if((lib!=null) && (lib.topChip()==null))
						{
							theLiberties.pushNew(lib);
						}
					}
				}
			}
			return(theLiberties);
		}
	public int sweepCounter = -1;
	public int getSweepCounter() { return(sweepCounter); }
	public void setSweepCounter(int to) 
		{ sweepCounter = to; 
		}
	
	public Kind getKind() { return(kind); }
	public void setKind(Kind k,String r) 
		{ 
		  kind=k;
		  reason=r;
		}
	public LocationProvider containsPoint(int x, int y) {
		for(int lim=size()-1; lim>=0; lim--)
		{
			LocationProvider p = elementAt(lim);
			if(p.equals(((x>'@')?(x-'@'):x),y)) { return(p); }
		}
		return null;
	}
	public boolean chipIsIncluded(GoChip ch) { return(kind.chipIsIncluded(ch)); }
	public boolean chipIsScanned(GoChip ch) { return(kind.chipIsScanned(ch)); }
	
	public LocationProvider containsPoint(LocationProvider p) {
		return(containsPoint(p.getX(),p.getY()));
	}
	public void addElement(GoCell c) { theLiberties=null; super.addElement(c);  }
	public GoCell remove(GoCell c,boolean sw) { theLiberties=null; return(super.remove(c,sw)); }
	
	public boolean isLiberty(GoCell c)
	{	return(getLiberties().containsPoint(c)!=null);
	}
	public boolean isBorder(GoCell c)
	{
		return(getBorder().containsPoint(c)!=null);
	}
	public boolean isSafeLiberty(GoCell c)
	{
		if(isLiberty(c))
		{
			Kind k = c.getKind();
			return(k.isSafeLiberty(kind.chip));
		}
		return(false);
	}
	public int countSharedLiberties(SimpleGroup other)
	{	int libs = 0;
		SimpleGroup myLibs = getLiberties();
		SimpleGroup otherLibs = other.getLiberties();
		for(int lim=myLibs.size()-1; lim>=0; lim--)
		{	GoCell member = myLibs.elementAt(lim);
			if(otherLibs.containsPoint(member)!=null) { libs++; } 
		}
		return(libs);
	}
	public int countSharedBorder(SimpleGroup other)
	{	SimpleGroup border = getBorder();
		int n=0;
		for(int lim=border.size()-1; lim>=0; lim--)
		{
			if(other.containsPoint(border.elementAt(lim))!=null) { n++; }
		}
		return(n);
	}
	// count safe liberties in this group, no connections
	// are followed.
	private int countLocalSafeLiberties()
	{
		SimpleGroup libs = getLiberties();
		Kind tkind = kind.getTerritoryKind();
		int n=0;
		for(int lim=libs.size()-1; lim>=0; lim--)
		{
			GoCell lib = libs.elementAt(lim);
			Kind k = lib.getKind();
			switch(k)
			{
			case WhiteTerritory:
			case BlackTerritory:
				if(k==tkind) { n++; }
				break;
			case FalseEye:
			default: break;
			}
		}
		return(n);
	}
	public void addKilledLiberties(SimpleGroup gr)
	{
		killedLiberties += countSharedBorder(gr);
	}

	private int countExtraLiberties(int sweep,GoCell from,GoCell cutter)
	{	Kind k = from.getKind();
		switch(k)
		{
		case ReservedForBlack:
		case ReservedForWhite:
		case FillBlack:
		case FillWhite:
			if(k.fillChip==kind.chip)
				{
				int libs = 0;
				for(int direction=from.geometry.n; direction>=0; direction--)
				{
					GoCell next = from.exitTo(direction);
					if(next!=null)
					{
						if(next.getKind().isNowEmpty() && (next.getSweepCounter()!=sweep)) { next.setSweepCounter(sweep); libs++; }
					}
				}
				return(libs);
				}
				return(0);
			default: return(0);
		}
	}
	private int countSafeLiberties(int sweep,GoCell cutter,ConnectCode connectDame)
	{	if(getSweepCounter()!=sweep)
		{	setSweepCounter(sweep);
			int libs = killedLiberties;
			libs += countAdjacentSafeLiberties(sweep,cutter,connectDame);
			if(libs>2) { return(libs); }
			ConnectionStack connections = getConnections();
			// two passes, first using already filled spaces, second using newly filled spaces.
			for(int lim=connections.size()-1; lim>=0; lim--)
			{
				Connection conn = connections.elementAt(lim);
				GoCell from = conn.from;
				Kind fromKind = from.getKind();
				SimpleGroup to = conn.to;
				if((from!=cutter)
						&& (to.getSweepCounter()!=sweep)
						&& fromKind.isNowConnected(kind.chip))
				{	to.setSweepCounter(sweep);					
					int plus = countExtraLiberties(sweep,from,cutter) + to.countAdjacentSafeLiberties(sweep,cutter,connectDame);
					if(plus>0) { libs += plus;	}
				}
			}
			// second pass using newly filled spaces, less preferred
			// because we might be filling usable liberties
			for(int lim=connections.size()-1; lim>=0; lim--)
			{
				Connection conn = connections.elementAt(lim);
				GoCell from = conn.from;
				Kind fromKind = from.getKind();
				SimpleGroup to = conn.to;
				if((from!=cutter)
						&& (to.getSweepCounter()!=sweep)
						&& fromKind.isNowEmpty())
				{	to.setSweepCounter(sweep);
					
					int plus = countExtraLiberties(sweep,from,cutter) + to.countAdjacentSafeLiberties(sweep,cutter,connectDame);
					if(plus>0) {
						libs += plus;
						if(fromKind.isSafeLiberty(kind.chip)
								&& (from.sweepTag!=sweep))
							{ libs--; 	// we filled the liberty once, no matter how many groups got connected
							  from.sweepTag = sweep; 
							}
					}
				}
			}
			return(libs);
		}
		return(0);
	}
	
	//
	// count the libs that would be new, but do not mark them
	//
	public int countNewSafeLiberties(int sweep,GoCell cutter)
	{	int libs = 0;
		SimpleGroup liberties = getLiberties();
		GoChip chip = kind.chip;
		for(int lim=liberties.size()-1; lim>=0; lim--)
		{	GoCell elem = liberties.elementAt(lim);
			if((elem!=cutter) && (elem.getSweepCounter()!=sweep))
			{	Kind k = elem.getKind();
				if(k.isSafeLiberty(chip)) { libs++; }
			}
		}
		return(libs);
	}
	
	// count the safe liberties of this group and those connected by FillWhite or FillBlack
	private int countAdjacentSafeLiberties(int sweep,GoCell cutter,GoCell elem,ConnectCode connectDame)
	{	int libs = 0;
		Kind k = elem.getKind();
		GoChip chip = kind.chip;
		if(k.fillChip==chip)
		{	
			// we need to scan explicitly because placing the fill stone
			// can add new liberties
			for(int direction=elem.geometry.n-1; direction>=0; direction--)
			{
			GoCell next = elem.exitTo(direction);
			if(next!=null)
				{
				SimpleGroup nextGroup = next.getGroup();
				if(nextGroup!=this)
				{
				Kind nextKind = nextGroup.getKind();
				if(nextKind.isSafeLiberty(chip)) 
					{ if(next.getSweepCounter()!=sweep) 
							{ libs++; next.setSweepCounter(sweep); }
					}
				switch(nextKind)
				{
				case White:
				case Black:
				case SafeWhite:
				case SafeBlack:
				case DeadWhite:
				case DeadBlack:
					// count the liberties of groups connected by fill
					if((nextKind.chip==chip) && (nextGroup.getSweepCounter()!=sweep))
					{	nextGroup.setSweepCounter(sweep);
						libs += nextGroup.countAdjacentSafeLiberties(sweep,cutter,connectDame);
					}
					break;
				default:
				}}
				}
			}
		}
		return(libs);
	}
	// count safe liberties, including those connected by FillBlack or FillWhite, 
	// and optionally including those connected by outside connections
	public int countAdjacentSafeLiberties(int sweep,GoCell cutter,ConnectCode connectDame)
	{	int libs = killedLiberties;

		SimpleGroup liberties = getLiberties();
		GoChip chip = kind.chip;
		for(int lim=liberties.size()-1; lim>=0; lim--)
		{	GoCell elem = liberties.elementAt(lim);
			if((elem!=cutter) && (elem.getSweepCounter()!=sweep))
			{	Kind k = elem.getKind();
				elem.setSweepCounter(sweep);
				if(k.isSafeLiberty(chip))
					{ libs++; }
				else switch(k)
				{
				default: break;
				case FillWhite:
				case FillBlack:
				case ReservedForWhite:
				case ReservedForBlack:
					libs += countAdjacentSafeLiberties(sweep,cutter,elem,connectDame);
					break;
				}
			}
		}
		//
		// the policy on outside connections, "always connected" or "never connected"
		// makes a difference in marginal cases.  Maybe we'll use this to prioritize
		// connections in some future revision.
		//
		{
		ConnectionStack connections = getConnections();
		int nCandidates = 0;
		int candidateSafe = 0;
		SimpleGroup connected = null;
		SimpleGroup candidate = null;
		for(int lim=connections.size()-1; lim>=0; lim--)
		{
			Connection conn = connections.elementAt(lim);
			GoCell from = conn.from;
			SimpleGroup to = conn.to;
			Kind fromKind = from.getKind();
			switch(fromKind)
			{
			case OutsideDame:
				// consider outside connections to have been filled too.
				if((from!=cutter) && (to.getSweepCounter()!=sweep))
				{	
					if((connectDame==ConnectCode.All) || ((to.top().sweepTag==sweep) && connectDame!=ConnectCode.None))
					{
					to.setSweepCounter(sweep);
					connected=to;
					libs += to.countAdjacentSafeLiberties(sweep,cutter,connectDame);
					}
					else if(connectDame!=ConnectCode.None)
					{
					nCandidates++;
					int newsaf = to.countNewSafeLiberties(sweep,cutter);
					if( ((candidate==null) || (newsaf<candidateSafe))) 
						{ candidate=to; 
						  
						  candidateSafe = newsaf; 
						}
					}
					to.top().sweepTag = sweep;
				}
				break;
			default: break;
			}}
		if((nCandidates>1) && (candidate!=connected))
			{
			// count the least promising connection if there are multiples
			libs += candidate.countAdjacentSafeLiberties(sweep,cutter,connectDame );
			}
		}
		return(libs);
	}

	// this doesn't work well enough to be in the program.
	// the two problems are generally that the order you assign
	// the miai matters a lot; you have to identify the important
	// pairs and assign them first.  The second problem is that
	// the explicit assignment creates hard disconnections and opens
	// new connections in complicated ways.
	public void assignMiai(int threshold)
	{
		ConnectionStack connections = getConnections();
		for(int lim=connections.size()-1; lim>=0; lim--)
		{	
			Connection conn = connections.elementAt(lim);
			GoCell from = conn.from;
			SimpleGroup to = conn.to;
			Kind fromKind = from.getKind();
			if(fromKind==Kind.OutsideDame)
			{
				for(int lim2=lim-1; lim2>=0; lim2--)
				{
					Connection conn2 = connections.elementAt(lim2);
					GoCell from2 = conn2.from;
					Kind from2Kind = from2.getKind();
					if((conn2.to==to) 
							&& (from2!=from) 
							&& (from2Kind==Kind.OutsideDame))
					{	if(threshold>=5)
						{ from.miaiCounter++;
						  from2.miaiCounter++;
						}
						else if((from.miaiCounter+from2.miaiCounter)>=threshold)
						{
						board.chipFromGroup(from,kind.chip==GoChip.black ? Kind.FillBlack : Kind.FillWhite,"miai");
						board.chipFromGroup(from2,kind.chip!=GoChip.black ? Kind.FillBlack : Kind.FillWhite,"miai");
						}
						break;
					}
				}
			}
		}
		
		int candidateSafe=0;
		Connection candidate=null;
		int altCandidateSafe=0;
		Connection altCandidate=null;
		
		for(int lim=connections.size()-1; lim>=0; lim--)
		{
			Connection conn = connections.elementAt(lim);
			GoCell from = conn.from;
			Kind fromKind = from.getKind();
			if(fromKind==Kind.OutsideDame)
			{	int sa = conn.to.countAdjacentSafeLiberties(board.incrementSweepCounter(),null,ConnectCode.None);
				if((candidate==null) || (candidateSafe>sa))
				{
					altCandidate = candidate;
					altCandidateSafe = candidateSafe;
					candidate = conn;
					candidateSafe=sa;
				}
				else if((altCandidate==null) || (altCandidateSafe>sa))
				{	altCandidate = conn;
					altCandidateSafe = sa;
				}
			}
			}
		if(altCandidate!=null)
		{	GoCell from = candidate.from;
			GoCell altFrom =altCandidate.from;
			if(threshold>=5)
			{
			from.miaiCounter++;
			altFrom.miaiCounter++;
			}
		else if ((from.miaiCounter+altFrom.miaiCounter)>=threshold)
			{
			board.chipFromGroup(from,kind.chip!=GoChip.black ? Kind.FillBlack : Kind.FillWhite,"more desirable connection");
			board.chipFromGroup(altFrom,kind.chip==GoChip.black ? Kind.FillBlack : Kind.FillWhite,"less desirable connection");
			}
		}
	}
	// specialized version of safety for endgame atari classification.  Not for general use
	private boolean hasOneSafeLiberty(GoCell cut,GoCell connect)
	{	//if(knownLiberties>=0) { return(knownLiberties>=2); }
		SimpleGroup myLibs = getLiberties();
		GoCell lib=null;
		for(int lim=myLibs.size()-1; lim>=0; lim--)
		{	GoCell adj = myLibs.elementAt(lim);
			if((adj!=cut)
				&& (adj!=lib)) 
				{ 
				Kind adjKind = adj.getKind();
				switch(adjKind)
				{	
				case BlackSnapbackTerritory:
				case BlackTerritory:
				case WhiteSnapbackTerritory:
				case WhiteTerritory:
					if((adj!=connect) && (kind.chip==adjKind.fillChip))
						{ return(true); }
					break;
				case OutsideDame:
				case BlackDame:
				case WhiteDame:
				case ReservedForWhite:
				case ReservedForBlack:
				case FillWhite:
				case FillBlack:
				case Dame:
				case FalseEye:
					break;
				default: 
					G.Error("Not expected %s",adjKind);

				}}
			}
		return(false);
	}


	private boolean hasTwoRealLiberties()
	{	SimpleGroup libs = getLiberties();
		return(libs.size()>=2);
	}
	
	public int countLiberties()
	{	SimpleGroup libs = getLiberties();
		return(libs.size()+killedLiberties);
	}
	public int countAvailableLiberties()
	{
		return(countAvailableLiberties(board.incrementSweepCounter(),true));
	}
	public int countAvailableLiberties(int sweep,boolean followConnect)
	{
		SimpleGroup libs = getLiberties();
		int count = 0;
		for(int lim=libs.size()-1; lim>=0; lim--)
		{
			GoCell lib = libs.elementAt(lim);
			if(lib.sweepTag!=sweep)
			{	Kind libKind = lib.getKind();
				lib.sweepTag = sweep;
				if(libKind.isNowEmpty()) { count++; }
				else if(followConnect)
				{
					switch(libKind)
					{
					default: break;
					case FillBlack:
					case FillWhite:
						if(kind.chip==libKind.fillChip)
							{ 	for(int direction=lib.geometry.n-1; direction>=0; direction--)
								{
								GoCell next = lib.exitTo(direction);
								if(next!=null)
								{
								if(next.topChip()==kind.chip)
								{
								count += next.getGroup().countAvailableLiberties(sweep,followConnect);
								}}}
							}
						}
				}
			}
		}
		return(count);
	}
	public GoCell singleCommonBorder(SimpleGroup other)
	{	GoCell some = null;
		for(int lim=size()-1; lim>=0; lim--)
		{
			GoCell elem = elementAt(lim);
			if(other.isBorder(elem))
			{
				if(some!=null) { return(null); }
				some = elem;
			}
		}
		return(some);
	}
	public GoCell singleUnfilledLiberty(GoCell atari,SimpleGroup common)
	{	GoCell single = atari;
		if(killedLiberties>0) { return(null); }
		SimpleGroup libs = getLiberties();
		for(int lim=libs.size()-1; lim>=0; lim--)
		{
			GoCell elem = libs.elementAt(lim);
			Kind k = elem.getKind();
			switch(k)
			{
			case FillWhite:
			case FillBlack:
				if(k.fillChip==kind.chip) { return(null); }
				//$FALL-THROUGH$
			case Black:
			case White:
			case SafeWhite:
			case SekiBlack:
			case SekiWhite:

			case SafeBlack:
			case DeadBlack:
			case DeadWhite:
				break;
			case Dame:
			case RemovedBlack:
			case RemovedWhite:
			case OutsideDame:
			case BlackDame:
			case WhiteDame:
			case WhiteTerritory:
			case BlackTerritory:
			case FalseEye:
			case BlackSnapbackTerritory:
			case WhiteSnapbackTerritory:
			case ReservedForWhite:
			case ReservedForBlack:
				if((elem==single)||(single==null)) 
					{ 
						if((common==null) || !common.isLiberty(elem)) { single=elem; }
					} 
					else 
					{ return(null); }
				break;
			case BlackAndEmpty:
			case Liberty:
			case Empty:
			case WhiteAndEmpty:
			default:
				G.Error("not expecting %s",kind);
			}
		}

		return(single);
	}
	
	public boolean killIfIsDead(GoCell atari,Kind k)
	{	Kind kind = getKind();
		boolean newKill = false;
		switch(kind)
			{
			default: throw G.Error("not expecting %s",kind);
			case SafeWhite:
			case SafeBlack:
			case SekiBlack:
			case SekiWhite:

			case RemovedWhite:
			case RemovedBlack:
				break;
			case DeadWhite:
			case DeadBlack:
			case White:
			case Black:
			{
			GoCell lib = singleUnfilledLiberty(atari,null);
			if((lib!=null)&&!canKill2())
			{
			setKind(k,"captured by endgame fill");	
			GroupStack adjacent = adjacentGroups();
			for(int lim=adjacent.size()-1; lim>=0; lim--)
				{
				SimpleGroup adj = adjacent.elementAt(lim);
				switch(k)
				{ case RemovedBlack:
				  case RemovedWhite: 
					  	adj.addKilledLiberties(this);
					  	newKill = true;
						break;
				default: break;
				  
				}
				}
				}
				}
			}
		return(newKill);
	}

	
	// places where filling a dame result in self atari can't be filled directly
	// and so get filled by the other color.
	public void markSelfAtari()
	{	
		if(size()==1)
		{	GoCell c = top();
			switch(kind)
			{
			case OutsideDame:
			case WhiteDame:
			case BlackDame:
			case Dame:
			case FalseEye:			

				{
				int blackLibertiesAvailable = 0;
				int whiteLibertiesAvailable = 0;
				int blackGroups = 0;
				int whiteGroups = 0;
				boolean blackHasOne = false;
				boolean whiteHasOne = false;
				GoCell unsharedBlackLib = null;
				boolean multipleBlackLib = false;
				GoCell unsharedWhiteLib = null;
				boolean multipleWhiteLib = false;
				SimpleGroup whiteGroup = null;
				SimpleGroup blackGroup = null;
				for(int direction=c.geometry.n-1; direction>=0; direction--)
					{
					GoCell next = c.exitTo(direction); 
					if(next!=null)
						{
						SimpleGroup gr = next.getGroup();
						GoChip color = next.topChip();
						if(color!=null)
						{
						int count = gr.countLiberties();
						if(color==GoChip.black) 
							{ if(blackGroup!=gr)
								{ blackLibertiesAvailable += count;
								  blackHasOne |= (count==1);
								  multipleBlackLib |= (count>=3);
								  if(blackGroup==null) { blackGroup=gr; blackGroups++; }
								  else { if(gr!=blackGroup) { blackGroups++; }
									if(count<=2) 
								  	{ GoCell unshared = blackGroup.unSharedLiberty(gr); 
								  	  if(unsharedBlackLib==null) { unsharedBlackLib = unshared; }
								  	  else { multipleBlackLib |= (unshared!=unsharedBlackLib); }
								  	}
									
								  }
								}
							}
						else { if(whiteGroup!=gr) 
							{ whiteLibertiesAvailable += count;
							  whiteHasOne |= (count==1);
							  multipleWhiteLib |= (count>=3);
							  if(whiteGroup==null) { whiteGroup=gr; whiteGroups++; }
							  else { if(whiteGroup!=gr) { whiteGroups++; }
							    if(count<=2) 
							  		{ GoCell unshared = whiteGroup.unSharedLiberty(gr); 
							  		if(unsharedWhiteLib==null) { unsharedWhiteLib = unshared; }
							  		else { multipleWhiteLib |= (unshared!=unsharedWhiteLib); }
							  		}
							  }
						}
						}
					}}}
					if((blackLibertiesAvailable<=2) 
							&& (whiteLibertiesAvailable>2) 
							&& (blackGroup!=null)
							)
						{ if(blackGroup.canConnectOrKill())
							{
							if(blackLibertiesAvailable==1)
								{
								if(!blackGroup.canKill2())
								{
								blackGroup.setKind(Kind.DeadBlack,"black snap back");
								setKind(Kind.WhiteSnapbackTerritory,"black snap back");
								}}
							else 
							{ // killing here results in overkill - the groups
							  // we kill could have escaped if given a chance
							  //c.killIfIsCapture(GoChip.white);
								// 1986-WOMENS-HONINBO-GAME-2 had a problem where these fills accidentally
								// killed something, which caused downstream confusion
								// whiteHasOne avoids this without other side effects
							  if(!blackHasOne && (whiteGroups>=2)  && (multipleWhiteLib || (whiteGroups==2))  )
							  	{ chippedFromKind = getKind();
								  setKind(Kind.ReservedForWhite,"black self atari"); 
							  	}
							}
							}
						}
					else if((whiteLibertiesAvailable<=2) 
							&& (blackLibertiesAvailable>2) 
							&& (whiteGroup!=null))
						{ if(whiteGroup.canConnectOrKill())
							{
							if(whiteLibertiesAvailable==1)
							{
							if(!whiteGroup.canKill2())
							{
							whiteGroup.setKind(Kind.DeadWhite,"white snap back");
							setKind(Kind.BlackSnapbackTerritory,"white snap back");
							}}
							else 
							{	// killing here results in overkill - the groups
								// we kill could have escaped if given a chance
								//c.killIfIsCapture(GoChip.black);
								// 1986-WOMENS-HONINBO-GAME-2 had a problem where these fills accidentally
								// killed something, which caused downstream confusion
								// whiteHasOne avoids this without other side effects
								if(!whiteHasOne && (blackGroups>=2) && (multipleBlackLib || (blackGroups==2)))
									{
									chippedFromKind = getKind();
									setKind(Kind.ReservedForBlack,"white self atari"); 
									}
							}
							}
						}
					}
				break;
			case BlackTerritory:
			case WhiteTerritory:
			case BlackSnapbackTerritory:
			case WhiteSnapbackTerritory:
				break;
			default: G.Error("not expecting %s",kind);
			}
		}
		
	}
	//
	// can connect to a friendly group, or can capture an enemy group immediately
	private boolean canConnectOrKill() 
	{	
		ConnectionStack cc = getConnections();
		if(cc.size()>0) { return(true); }
		
		GroupStack enemies = adjacentGroups();
		for(int lim = enemies.size()-1; lim>=0; lim--)
		{	SimpleGroup gr = enemies.elementAt(lim);
			Kind k = gr.getKind();
			switch(k)
			{
			case Black:
			case White:
			case DeadWhite:
			case DeadBlack:
				if(gr.countAvailableLiberties()<=1) 
					{ return(true); }	// captures out
				break;
			case RemovedBlack:
			case RemovedWhite:
			case SafeWhite:
			case SafeBlack:
			case SekiBlack:
			case SekiWhite:
				break;
			default: G.Error("not expecting %s",k);
			}
			
		}

		return(false);
	}
	//
	// atari 2 enemy groups.  This is used where an atari group
	// has two adjacent groups in atari.  As an endgame situation
	// this means we don't need to capture either one.  This fixes
	// the lower left corner in GO-ddyer-bobc-2018-07-31-0032.sgf
	//
	boolean canKill2() 
	{	int n = 0;
		GroupStack enemies = adjacentGroups();
		for(int lim = enemies.size()-1; lim>=0; lim--)
		{	SimpleGroup gr = enemies.elementAt(lim);
			Kind k = gr.getKind();
			switch(k)
			{
			case Black:
			case White:
			case DeadWhite:
			case DeadBlack:
				if(gr.countAvailableLiberties()<=1) 
					{ n++;
					  if(n>=2) { return(true); }
					}	// captures out
				break;
			case RemovedBlack:
			case RemovedWhite:
			case SafeWhite:
			case SafeBlack:
			case SekiBlack:
			case SekiWhite:
				break;
			default: G.Error("not expecting %s",k);
			}
			
		}

		return(false);
	}
	// true if this group is a boundary for the specified kind.
	private boolean isBoundaryTerritory(Kind forKind)
	{
		switch(kind)
  		{
  		default: throw G.Error("not expecting %s",kind);
  		case SekiWhite:
  		case SafeWhite:
  		case White: if(forKind==Kind.BlackTerritory) { return(true); }
  			break;
  		case SekiBlack:
		case SafeBlack:
  		case Black: if(forKind==Kind.WhiteTerritory) { return(true); }
  			break;
 		case Dame:
  		case OutsideDame:
  		case WhiteDame:
  		case BlackDame:
  		case FillWhite:
  		case FillBlack:
		case ReservedForWhite:
		case ReservedForBlack:
  		case BlackTerritory:
  		case WhiteTerritory:
  		case FalseEye:
  		case DeadBlack:
  		case DeadWhite:
		case RemovedBlack:
		case RemovedWhite:
		case WhiteAndEmpty:
		case BlackAndEmpty:
  			break;
  		}
		return(false);
	}
	

	//
	// recursive mark and sweep, return true if we encounter "kind". bounded by
	// the complementary color for kind.
	// kind should be WhiteTerrory (bounded by Black stones) or BlackTerritory (bounded by White stones)
	// 
	// this is used to identify dame, which are spaces where both black and white territory
	// can be reached.
	//
	public SimpleGroup canReachTerritory(int sweep,Kind forKind,SimpleGroup notInside)
	{
		if(getSweepCounter()!=sweep)
		{	
	  		setSweepCounter(sweep);
	  		if(isBoundaryTerritory(forKind)) { return(null); }
	  		switch(forKind)
	  		{
	  		case BlackTerritory:	if((kind==Kind.SafeBlack)||(kind==Kind.SekiBlack)) 
	  			{ return(this); }
	  			break;
	  		case WhiteTerritory: 	if((kind==Kind.SafeWhite)||(kind==Kind.SekiWhite)) 
	  				{ return(this); }
	  			break;
  			default: break;
	  		}

	  		// not a termination, keep scanning.  If we're not an empty group, 
	  		// scan only for direct connections to other groups.
	  		if(kind.chip!=null)
	  		{
	  		SimpleGroup libs = getLiberties();
	  		for(int lim=libs.size()-1; lim>=0; lim--)
	  		{
	  			GoCell lib = libs.elementAt(lim);
	  			SimpleGroup k = lib.getGroup();
	  			Kind kd = k.getKind();
	  			if((k.getSweepCounter()!=sweep) 
	  					&& ((kd==forKind) || k.hIsTerritory(forKind))
	  					&& ((notInside==null) || (notInside.containsPoint(k.top())==null))
	  					)
	  				{ return(k); 
	  				}
	  		}
	  		
	  		ConnectionStack conn = getConnections();
	  		for(int lim=conn.size()-1; lim>=0; lim--)
	  		{	Connection conto = conn.elementAt(lim);
	  			SimpleGroup cto = conto.to;
	  			SimpleGroup found = cto.canReachTerritory(sweep,forKind,notInside);
	  			if(found!=null) 
	  				{ return(found); }
	  		}}
	  		
	  		GroupStack enemies = adjacentGroups();
	  		for(int lim=enemies.size()-1; lim>=0; lim--)
	  		{
	  			SimpleGroup enemy = enemies.elementAt(lim);
	  			SimpleGroup found = enemy.canReachTerritory(sweep,forKind,notInside);
	  			if(found!=null)
	  				{ return(found); }
	  		}
		}
	  	return(null);
	}	

	public SimpleGroup hasInsecureBoundary()
	{
		switch(kind)
		{
		case BlackAndEmpty:
		case WhiteAndEmpty:
			{
			GroupStack adj = adjacentGroups();
			for(int lim=adj.size()-1; lim>=0; lim--)
			{
				SimpleGroup gr = adj.elementAt(lim);
				if(!gr.hasTwoRealLiberties() && !gr.isInside(this)) { return(gr); }
			}
			}
			break;
		default: G.Error("not expecting %s",kind);
		}
		return(null);
	}
	public boolean isInside(SimpleGroup other)
	{	int common = this.countSharedBorder(other);
		return(common==getBorder().size());
	}
	public boolean allDead()
	{
		GroupStack adj = getEmbeddedColoredGroups();
		boolean allRemoved = true;
		for(int lim=adj.size()-1; lim>=0; lim--)
		{
			SimpleGroup gr = adj.elementAt(lim);
			switch(gr.kind) {
			case RemovedBlack:
			case RemovedWhite:
			case DeadBlack:
			case DeadWhite:
				break;
			default:	
				allRemoved = false;
			}
		}
		return allRemoved;
		
	}

	// check for endgame positions where a simple cut can kill,
	// presumably because outside liberties are all filled.
	// TODO: two common cases where this doesn't find the cut are a
	// simple 2-move cut and capture where the victim makes 1 liberty
	// but can't make a second, and a snap-back that has evolved from
	// other end game fills.
	public GoCell endgameCut()
	{	
		// if anything is killed and removed, we have liberties
		if(allDeadAttackers()) { return(null); }
			
		SimpleGroup libs = getLiberties();
		if(libs.size()>1)
		{
		for(int lim=libs.size()-1; lim>=0; lim--)
		{
			GoCell lib = libs.elementAt(lim);
			Kind libKind = lib.getKind();
			if( libKind.isSafeLiberty(kind.chip))
			{	boolean tc = testCutAndKill(lib);
				if(tc)	{ return(lib);	}
			}
		}}
		return(null);
	}

	// 
	// this group is black or white.  map over our liberties
	// and count all the Dame as filled. If there is only one
	// liberty left, return it.  This is used to detect the
	// simple case of stones that will have to connect in endgame
	//
	// TODO: this doesn't handle the common case where a space making atari can't be
	// filled because the stones making the atari would be atari
	//
	public GoCell endgameAtari()
	{	int safe = countAdjacentSafeLiberties(board.incrementSweepCounter(),null,ConnectCode.Double);
		if(safe>=2) return(null); 
 		GoCell atariCell = null;
		// dame blackdame and whitedame are nominally "inside" and should be filled only
		// when necessary to avoid atari, and when they make connections.  This outsideConnection
		// keeps track of connecting outside, and if two possibilities exist avoids filling
		GoCell outsideConnection = null;
		int outsideFills = 0;

		
		SimpleGroup libs = getLiberties();
		for(int lim=libs.size()-1; lim>=0; lim--)
		{	GoCell adj = libs.elementAt(lim);
			Kind aKind = adj.getKind();
					switch(aKind)
					{
					case FalseEye:
						break;
					case WhiteTerritory: 
					case BlackTerritory:
						
						if(atariCell!=null) { return(null); }
						else { atariCell = adj;
							 }	
						break;
					case SafeWhite:	// added in classify split
					case WhiteDame:
	
						if(kind==Kind.White)
							{ SimpleGroup conto = isConnection(adj);
							  if(conto!=null) 
								{ 
								  if(atariCell!=null) { return(null); }
									else { atariCell = adj;
										//outsideConnection = null;
									}
								} 
							}
						break;
					case SafeBlack:	// added in classify split
					case BlackDame:
						if(kind==Kind.Black) 
							{SimpleGroup conto = isConnection(adj);
							 if(conto!=null) 
								{
								 
								if(atariCell!=null) { return(null); }
									else { atariCell = adj; 
										//outsideConnection = null;
										}
								}
							
							}
						break;
					case FillBlack:
					case FillWhite:
					case ReservedForWhite:
					case ReservedForBlack:
						
						{
						int escape = countAdjacentSafeLiberties(board.incrementSweepCounter(),null,adj,ConnectCode.Double);
						if(escape>=2)
						{
							return(null);
						}
						}
						
					 
						break;
						//if(countSafeLiberties(board.incrementSweepCounter(),null,true)>=2) { return(null); }
						//if(immediateSafeLiberties(this,null,null)>=2) { return(null) ; }
						//if(canGetSafeLiberty(adj,atariCell)) 
						//	{ return(null); }
						//break;
					case Dame:
						// ordinary dame are "inside" cells that normally don't need to be filled
						//return(null);
						{
						SimpleGroup conto = isConnection(adj);
						if( conto!=null)
							{
							
							if(atariCell!=null) 
								{ return(null); }
							atariCell = adj;
							}
							else { outsideConnection = null;
								atariCell = adj;
								 }
						}
						break;
					case OutsideDame:
						{
						outsideFills++;
						if(canConnectAndBeSafe(adj))
							{	if(outsideConnection!=null) 
									{ return(null); }
								//atariCell = adj;
								outsideConnection = adj;
								//dameConnection = null;
							}
						}
						break;
					case BlackSnapbackTerritory:
					case WhiteSnapbackTerritory:
						return(null); 
					case Empty:
					default: G.Error("Not expecting kind %s for %s",aKind,adj);
				
			}
		}
		if((atariCell!=null) && atariCell.isEye()) { atariCell=null; }	// never fill an eye

		if(atariCell==null)
		{ GroupStack enemies = adjacentGroups();
		  for(int lim=enemies.size()-1; lim>=0; lim--)
		  {
			  SimpleGroup enemy = enemies.elementAt(lim);
			  Kind k = enemy.getKind();
			  switch(k)
			  {
			  case DeadWhite:
			  case DeadBlack:
			  case White:
			  case Black:
				  { 
					  GoCell unfilled = enemy.singleUnfilledLiberty(null,null);
					   //if(unfilled==null) { unfilled = enemy.singleUnfilledLiberty(null,this); }
					  if(unfilled!=null)
					  {
						  atariCell = unfilled;
					  }
				  }
				  
				break;
			  case SafeWhite:
			  case SafeBlack:
			  case FillBlack:
			  case FillWhite:
			  case SekiBlack:
			  case SekiWhite:
			  case WhiteTerritory: // rare, if we capture then fill
			  case BlackTerritory:
				  break;
			  case RemovedWhite:
			  case RemovedBlack:
				  atariCell = enemy.singleCommonBorder(this);
				  break;
			  default: G.Error("not expecting %s",k);
			  }
			  
		}
		}
		if(atariCell!=null)
		{	GoCell cap = canCapture(null);
		 	// try to substitute a capture for a fill
			// messing with the details of this heuristic has not proved effective 
			// sometimes capturing is a better move, sometimes not.  
			if(cap!=null )
			{
			 if(cap.isMultipleCapture(kind.chip)
					 || !cap.captureWillBeAtari(kind.chip)					// capture is not a ko-like capture
					 )
				{ return(cap); }
			  // avoid single stone captures that can be recaptured.
			  return(null);
			}
			if(!escapesAtari(atariCell,null)) 
				{	
					//when squeezed we can't extend to escape atari, but we can capture something
					if(outsideFills<=0)	
						{ 
						// this prevents filling which do not escape atari when
						// no preliminary fills have to happen first.  On the other
						// hand, when there are several preliminary fills, allow
						// "pointless" fills to happen because there would have been
						// plenty of time.
						if(atariCell.isFalseEye()) 
							{ atariCell.setKind(Kind.FalseEye,"endgame atari");
							  //G.print("E "+atariCell);
							}
						  return(null);
						}
					else // outsideFills>0
					{	GoCell atari = canAtariOutside();
						if(atari!=null) { atariCell = atari; }
					}
				}
			if((countAvailableLiberties()==1) && allDeadAttackers()) 
				{ // if we have more than 1 liberty, and are being pressed only by dead
				  // stones, we don't need to run away.  This is intended to avoid filling
				  // due to incidental ataris inside territory, as in TNGN04_4, while permitting
				  // captures instead of connecting in normal endgame squeeze, as in 036_165
				atariCell=null; 
				}
			}
		return(atariCell);
	}
	private GoCell canAtariOutside()
	{	GroupStack enemies = adjacentGroups();
		for(int lim=enemies.size()-1; lim>=0; lim--)
		{
			SimpleGroup enemy = enemies.elementAt(lim);
			if((enemy.size()==1) && (enemy.countAvailableLiberties()==2))
			{	SimpleGroup libs = enemy.getLiberties();
				for(int elim=libs.size()-1; elim>=0;elim--)
				{	GoCell lib = libs.elementAt(elim);
					if(!isLiberty(lib)
							&& (lib.countAvailableLiberties(kind)>=2)
							)
						{ return(lib); 
						}
				}
			}
		}
		return(null);
	}
	private boolean allDeadAttackers()
	{
		GroupStack adj = adjacentGroups();
		for(int lim=adj.size()-1; lim>=0; lim--)
		{	SimpleGroup gr=adj.elementAt(lim);
			Kind k = gr.getKind();
			switch(k)
			{
			case DeadWhite:
			case DeadBlack: 
					break;
			case RemovedWhite:
			case RemovedBlack:
					return(true); 	// one actually removed is as good as all dead
			case White:
			case Black:
			case SafeWhite:
			case SafeBlack:
			case FillBlack:
			case FillWhite:
			case ReservedForWhite:
			case ReservedForBlack:
			case SekiBlack:
			case SekiWhite:

				return(false);
			default: G.Error("not expecting %s",k);
			}
		}
		return(true);
	}

	private boolean testCutAndKill(GoCell cutter)
	{	int availableToCutter = cutter.countAvailableLiberties(kind.getOppositeKind());
		if(availableToCutter<2) { return(false); }
		int connectedLibs = countSafeLiberties(board.incrementSweepCounter(),cutter,ConnectCode.Double);
		if(connectedLibs>=2) { return(false); }	// more than two liberties
		
		int sweep = board.incrementSweepCounter();
		//
		// cutter is a hypothetical cutting stone that is not on the board.
		// count the immediate liberties it will have to see if it can be simply captured.
		//
		int libCount = 0;
		cutter.setSweepCounter(sweep);
		SimpleGroup libs = getLiberties();
		for(int lim=libs.size()-1; lim>=0; lim--)
		{
			GoCell lib = libs.elementAt(lim);
			if(lib!=cutter)
			{	lib.setSweepCounter(sweep);
				SimpleGroup libGroup = lib.getGroup();
				Kind libKind = libGroup.getKind();
				if(libKind.isSafeLiberty(kind.chip))
				{	
					// see if extending the other safe results in more liberties
					for(int direction=lib.geometry.n-1; direction>=0; direction--)
					{
						GoCell next = lib.exitTo(direction);
						if(next!=null)
						{	if(next==cutter)
							{
							if(availableToCutter==1) { libCount++; }	// recapture the cutter
							}
							else {
							SimpleGroup nextGroup = next.getGroup();
							Kind nextKind = nextGroup.getKind();
							switch(nextKind)
								{
								default:
									if(nextKind.isSafeLiberty(kind.chip)) { libCount++; }
									break;
								case FillWhite:
								case FillBlack:
									if(kind.chip==nextKind.fillChip)
									{	libCount += nextGroup.killedLiberties;	// add the liberties the fill got by killing something
									}
									break;
								case White:
								case Black:
								case SafeWhite:
								case SafeBlack:
								case DeadWhite:
								case DeadBlack:
									if(nextGroup.getSweepCounter()!=sweep)
									{
									nextGroup.setSweepCounter(sweep);
									libCount += nextGroup.countAdjacentSafeLiberties(sweep,cutter,ConnectCode.All);
									}
									break;
								}
							}
						}
					}
				}
			}
		}
		if(libCount>2) { return(false); }
		
		ConnectionStack connections = getConnections();
		for(int lim=connections.size()-1; lim>=0; lim--)
		{
			Connection conn = connections.elementAt(lim);
			GoCell from = conn.from;
			Kind fromKind = from.getKind();
			if((from!=cutter) && fromKind.isNowEmptyOrConnected(kind.chip))
			{
				libCount += conn.to.countSafeLiberties(sweep,cutter,ConnectCode.All);
			}
		}
		if(libCount>2) { return(false); }
		GoCell cap = canCapture(cutter);
		if(cap!=null) 
			{ return(false); 
			}
		if(connectedLibs==2) {return(true); }
		if(libCount>=2) { return(false); }
		return(true);
		//return(canCapture(cutter,null)==null);
		//if(connectedLibs==2) { return(true); }
		
	}

	
	//
	// find the "best" capture that's immediately possible.
	// ie; avoid single stone captures 
	//
	private GoCell canCapture(GoCell cutter)
	{	GoCell possible = null;
		SimpleGroup possibleGroup = null;
		GroupStack enemies = adjacentGroups();
		boolean possibleSecondChoice = false;
		for(int lim=enemies.size()-1; lim>=0; lim--)
		{	SimpleGroup enemy = enemies.elementAt(lim);
			GoCell lib = enemy.singleUnfilledLiberty(null,null);
			
			if((lib!=null) && ((cutter==null) || !enemy.isLiberty(cutter))) 
				{ 
				  boolean secondChoice = false;
				  Kind k = lib.getKind();
				  switch(k)
				  {
				  case WhiteTerritory:
				  case BlackTerritory:
					  secondChoice = true;
					//$FALL-THROUGH$
				case Dame:
				  case WhiteDame:
				  case BlackDame:
				  case OutsideDame:
				  case FalseEye:
				  case ReservedForWhite:
				  case ReservedForBlack:
					  int esize = enemy.size();
					  if((possibleGroup==null) || (esize>possibleGroup.size()) || (!secondChoice && possibleSecondChoice))
					  {
					  // prefer to capture the larger group.  Other possible criteria
					  // are to capture the group with more shared liberties, or where 
					  // the capturing stone has more liberties.  See honinbo-8-2 where
					  // a white group has to connect by capturing, but can also capture
					  // meaningless groups.					  
					  possibleGroup = enemy;
					  possible = lib;
					  possibleSecondChoice = secondChoice;
					  }
					  break;
				  case FillWhite:
				  case FillBlack:
				  case WhiteSnapbackTerritory:
				  case BlackSnapbackTerritory:

					  break;
					  default: G.Error("not expecting %s",k);
				  }
				}
		}
		return(possible);
	}
	private boolean escapesAtari(GoCell connector,GoCell cutter)
	{	int libs = 0;
 		int sweep =	board.incrementSweepCounter(); 	
		for(int direction=connector.geometry.n-1; direction>=0; direction--)
		{
			GoCell next = connector.exitTo(direction);
			if((next!=null)&& (next!=cutter))
			{	
				SimpleGroup g = next.getGroup();
				if(g!=this)
				{
				Kind adjKind = g.getKind();
				switch(adjKind)
				{
				default: throw G.Error("Not expecting kind %s",g.getKind());
				case Dame:
				case WhiteTerritory:
				case BlackTerritory:
				case FalseEye:
				case WhiteSnapbackTerritory:
				case BlackSnapbackTerritory:
				case RemovedBlack:
				case RemovedWhite:
				case BlackDame:
				case WhiteDame: libs++;
					if(libs>=2) 
						{ return(true); }
					break;
				case OutsideDame:
				case FillWhite:
				case ReservedForWhite:
				case ReservedForBlack:
				case FillBlack:
					break;
				case White:
				case Black:

				case DeadWhite:
				case DeadBlack:
					if(adjKind.chip==kind.chip) 
						{ // note we must use the same sweep counter for the whole function, so 
						  // common safe liberties of multiple groups will only be counted once.
						  libs += countSafeLiberties(sweep,cutter,ConnectCode.Double);
						  if(libs>1) { return(true); } 
						}
						else if((g.size()>1) && (g.countAvailableLiberties()<=1)) 
							{ return(true); }	// captures something
					break;
				case SafeWhite: 
				case SafeBlack: 
				case SekiBlack:
				case SekiWhite:
					if(adjKind.chip==kind.chip) { return(true);}
				}
			}}
		}
		return(false);
	}
	// is c one of our liberties
	boolean isAdjacent(GoCell c)
	{	return(getBorder().contains(c));
	}
	
	int countAsLiberties(SimpleGroup other)
	{	int common=0;
		for(int lim=other.size()-1; lim>=0; lim--)
		{	GoCell c = other.elementAt(lim);
			if(isAdjacent(c)) { common++; }
		}
		return(common);
	}

	//
	// this only considers direct connections to get to safe liberties
	// it should consider recursive connections
	//
	private boolean canConnectAndBeSafe(GoCell c)
	{
		for(int direction=c.geometry.n-1; direction>=0;direction--)
		{	GoCell next = c.exitTo(direction);
			if(next!=null)
			{	SimpleGroup gr = next.getGroup();
				if(this==gr) {  }
				else if(gr.kind.chip==kind.chip) { if(gr.hasOneSafeLiberty(c,null)) { return(true); }}
			}
		}
		return(false);

	}

	public GroupStack getEmbeddedColoredGroups()
	{
		if(embeddedGroups==null)
		{
			embeddedGroups = new GroupStack();
			switch(kind)
			{
			default: break;
			case WhiteAndEmpty:
			case BlackAndEmpty:
				for(int lim=size()-1; lim>=0;lim--)
				{	GoCell mem = elementAt(lim);
					if(mem.topChip()!=null) { embeddedGroups.pushNew(mem.getGroup()); }
					
				}
			break;
			}
		}
		return(embeddedGroups);
	}
	
	public GroupStack getEmbeddedEmptyGroups()
	{
		if(embeddedEmptyGroups==null)
		{
			embeddedEmptyGroups = new GroupStack();
			switch(kind)
			{
			default:
				break;
			case BlackAndEmpty:
			case WhiteAndEmpty:
			for(int lim=size()-1; lim>=0;lim--)
			{	GoCell mem = elementAt(lim);
				if(mem.topChip()==null) { embeddedEmptyGroups.pushNew(mem.getGroup()); }
				
			}
			break;
			}
		}
		return(embeddedEmptyGroups);
	}
	
	// 
	// remove unnecessary fill moves from areas that are being
	// declared territory.  The dead don't need to get away, and
	// optional fill moves don't need to be made.
	//
	public void unFillDead()
	{	GroupStack adjacentEmpties = adjacentEmptyGroups();
		for(int lim=adjacentEmpties.size()-1; lim>=0; lim--)
		{
			SimpleGroup empty = adjacentEmpties.elementAt(lim);
			Kind k = empty.getKind();
			switch(k)
			{
			case WhiteTerritory:
			case BlackTerritory:
			case BlackSnapbackTerritory:
			case WhiteSnapbackTerritory:
				if(k.fillChip==kind.chip)
				{
					empty.setKind(k.getOppositeKind(),"safe adj to dead");
				}
				break;
			case FillWhite:
			case FillBlack:
				if(k.fillChip!=kind.chip) { break; }
				//$FALL-THROUGH$
			case ReservedForWhite:
			case ReservedForBlack:
			case FalseEye:
				{
				 empty.setKind(kind.getTerritoryKind().getOppositeKind(),"unnecessary fill becomes territory");
				
				}
				break;
			default: break;
			}
		}
		GroupStack enemies = adjacentGroups();
		for(int lim=enemies.size()-1; lim>=0; lim--)
		{
			SimpleGroup enemy = enemies.elementAt(lim);
			Kind ekind = enemy.getKind();
			switch(ekind)
			{
			default: break;
			case RemovedWhite:
			case RemovedBlack:
				if(ekind.chip!=kind.chip)
				{	enemy.setKind(ekind.getSafeKind(),"undead");
				}
			}
		}
	}

	public boolean killEmbeddedGroups(Kind color)
	{
		return(getTerritory(color).killEmbeddedGroups());
	}
	public boolean killEmbeddedGroups()
	{	GroupStack colored = getEmbeddedColoredGroups();
		int safeSize = 0;
		boolean progress = false;
		for(int lim=colored.size()-1; lim>=0; lim--)
		{
			SimpleGroup co = colored.elementAt(lim);
			Kind kind = co.getKind();
			switch(kind)
			{
			default: break;
			case SekiBlack:
			case SekiWhite:
			case SafeWhite:
			case SafeBlack:
				safeSize += co.size();
				break;
			case White:
			case Black:
				break;
			}
		}
		if(safeSize < size()*0.5)
		{
		for(int lim=colored.size()-1; lim>=0; lim--)
		{
			SimpleGroup co = colored.elementAt(lim);
			Kind kind = co.getKind();
			switch(kind)
			{
			default: throw G.Error("not expecting %s",kind);
			case Black:
			case White:
				{
				if((co.size()<7) || co.classifySafety()!=kind.getSafeKind())
				{
				co.setKind(kind.getDeadKind(),"endgame no safety");
				co.unFillDead();
				progress=true;
				}}
				break;
			case DeadWhite:
			case DeadBlack:
			case RemovedWhite:
			case RemovedBlack:
			case SekiBlack:
			case SekiWhite:
			case SafeWhite:
			case SafeBlack:
			case WhiteTerritory:
			case BlackTerritory:
			case FillWhite:	// when capture then fill
			case FillBlack:
				break;
			}
		}
		
		GroupStack empty = getEmbeddedEmptyGroups();
		Kind tkind = (kind==Kind.WhiteAndEmpty) 
					? Kind.BlackTerritory 
					: Kind.WhiteTerritory;
		
		for(int lim=empty.size()-1; lim>=0; lim--)
		{
			SimpleGroup em = empty.elementAt(lim);
			Kind kind = em.getKind();
			switch(kind)
			{
			default: throw G.Error("not expecting %s",kind);
			case Dame:
			case WhiteDame:
			case OutsideDame:
			case BlackDame:
			case FalseEye:
				em.setKind(tkind,"safe after killing introns");
				progress=true;
				break;
			case FillBlack:
			case FillWhite:
			case ReservedForWhite:
			case ReservedForBlack:
				break;
			case WhiteTerritory:
			case BlackTerritory:
			case WhiteSnapbackTerritory:
			case BlackSnapbackTerritory:
				
				break;
			}
		}}
		return(progress);
	}
	// count the number of members in this group that are not
	// adjacent to color specified.  This is used with mixed
	// groups to count non-common liberties with the surrounding color
	//
	private int nonAdjacentMembers(GoChip color)
	{	int nonmembers = 0;
		for(int lim=size()-1; lim>=0; lim--)
		{
		GoCell member = elementAt(lim);
		GoChip top = member.topChip();
		if(top==null)
		{	boolean someadj=false;
			for(int direction=member.geometry.n-1; direction>=0 && !someadj; direction--)
			{	GoCell adj = member.exitTo(direction);
				if((adj!=null) && (adj.topChip()==color)) { someadj=true; }
			}
			if(!someadj) { nonmembers++; }
		}
		}
		return(nonmembers);
	}
	
	private GroupStack territories = null;
	public GroupStack addTerritory(SimpleGroup added)
	{	if(territories==null)
		{
			territories = new GroupStack();
		}
		if(added!=null) { territories.pushNew(added); }
		return(territories);
	}
	public GroupStack getTerritories() 
	{ return(addTerritory(null));
	}
	
	public void addTerritories()
	{	GroupStack adjacent = adjacentGroups();
		for(int lim=adjacent.size()-1; lim>=0; lim--)
		{
			SimpleGroup adj = adjacent.elementAt(lim);
			adj.addTerritory(this);
		}
	}
	public void removeComplexTerritories()
	{	GroupStack territories = getTerritories();
		for(int lim=territories.size()-1; lim>=0; lim--)
		{
			SimpleGroup territory = territories.elementAt(lim);
			GroupStack embedded = territory.getEmbeddedColoredGroups();
			boolean removed = false;
			for(int elim=embedded.size()-1; elim>=0 && !removed; elim--)
			{	SimpleGroup emb = embedded.elementAt(elim);
				if(emb.size()>8)
				{
					Kind fate = emb.classifySafety();
					switch(fate)
					{
					default: G.Error("not expecting %s",fate);
						//$FALL-THROUGH$
					case Empty: break;
					case SafeWhite:
					case SafeBlack:
					case SekiWhite:
					case SekiBlack:
						// safe group inside another territory
						territories.remove(lim,false);
						removed = true;
						break;
					}
				}
			}
		}
	}
	public SimpleGroup canReachSafeGroup(int sweep)
	{
		if(getSweepCounter()!=sweep)
		{	setSweepCounter(sweep);
			switch(kind)
			{default: throw G.Error("not expecting %s",kind);
			case RemovedBlack:
			case RemovedWhite:
				break;
			 case Black:
			 case White:
			 case DeadBlack:
			 case DeadWhite:
				{
					ConnectionStack connections = getConnections();
					for(int lim=connections.size()-1; lim>=0; lim--)
					{	Connection conn = connections.elementAt(lim);
						Kind k = conn.from.getKind();
						if(k.isNowEmptyOrConnected(kind.chip))
						{
							SimpleGroup sa = conn.to.canReachSafeGroup(sweep);
							if(sa!=null) { return(sa); }							
						}
					}
				}
				break;
			case SafeWhite:
			case SafeBlack:
			case SekiBlack:
			case SekiWhite:
					// we are a safe group
					return(this);
			}
		}
		return(null);
	}

	public void reClassifyAsSafe(Kind k)
	{	switch(k)
		{
		default: G.Error("not expecting %s",k);
			break;
		case Empty:	// we use this for not safe
			break;
		case SekiBlack:
		case SekiWhite:
			setKind(k,"");
			reclassifyAsSeki(getTerritories());
			break;
		case SafeWhite:
		case SafeBlack:
			setKind(k,"safe");
			reclassifyAsTerritory(adjacentGroups());
			reclassifyAsTerritory(adjacentEmptyGroups());
			break;
		
		}
		
	}
	
	// get the territory associated with this group
	// "this" group is a black or white group
	// "emp" is a presumably-adjacent empty group that is in the territory
	public SimpleGroup getTerritory(SimpleGroup emp)
	{	return(emp.getTerritory(kind));
	}
	// 
	// get the territory for kind, associated with this group
	// 
	public SimpleGroup getTerritory(Kind k)
	{
		GoCell top = top();
		switch(k)
		{
		case SafeWhite:
		case White:	return(top.getWhiteTerritory());
		
		case SafeBlack:
		case Black: return(top.getBlackTerritory());
		default: throw G.Error("not expecting %s",kind);
		}
	}
	//
	// cautiously reclassify a potential territory that is adjacent to a safe group
	// as a real territory.  The caution applies to everything that's actually outside
	// rather than inside
	//
	public void reclassifyAsTerritory(GroupStack empty)
	{
		for(int lim=empty.size()-1; lim>=0; lim--)
		{
			SimpleGroup ter = getTerritory(empty.elementAt(lim));
			
			if(ter.size()<10) 
				{ 	boolean hasUnsafe = false;
					GroupStack adjacent = ter.adjacentGroups();
					for(int alim=adjacent.size()-1; !hasUnsafe && alim>=0; alim--)
					{
						SimpleGroup agroup = adjacent.elementAt(alim);
						Kind akind = agroup.getKind();
						switch(akind)
						{
						case SafeWhite:
						case SafeBlack: break;
						default: hasUnsafe = true;
						}
					}
					// have to be very careful about classifying 
					// adjacent empty spaces as territories
					if(!hasUnsafe)
						{ ter.killEmbeddedGroups(); 
						} 
				}
		}
	}
private void reclassifyAsSeki(GroupStack territories)
{
	for(int lim=territories.size()-1; lim>=0; lim--)
	{
		SimpleGroup territory = territories.elementAt(lim);
		if(territory.size()<12)
		{
			{GroupStack embedded = territory.getEmbeddedColoredGroups();
			for(int elim=embedded.size()-1; elim>=0; elim--)
			{
				SimpleGroup enemy = embedded.elementAt(elim);
				Kind ekind = enemy.getKind();
				enemy.setKind(ekind.getSekiKind(),"seki in territory");
			}}
			{GroupStack embedded = territory.getEmbeddedEmptyGroups();
			for(int elim=embedded.size()-1; elim>=0; elim--)
			{
				SimpleGroup enemy = embedded.elementAt(elim);
				enemy.setKind(Kind.Dame,"dame in seki");
			}}
		}
	}
}

private int countUnconnectedEyes()
{	if(size()<10)
	{SimpleGroup connected = (size()>1)? getInsideConnectedBorder(getOutsideConnectedBorder()) : null;
	if(connected!=null)
	{
		SimpleGroup smaller = copy();
		smaller.difference(connected);
		if(smaller.isTwoEyed(null))
		{
			return(2); 
		}
		int count = smaller.countConnectedEyes();
		sekiSeen |= smaller.sekiSeen;
		return(count);
	}}
	return(hCountEyes());
}
//
// count eyes for a shape that is connected or is connectable outside
//
private int countConnectedEyes()
	{	int eyes = 0;
		int tsize = size();
		switch(tsize)
		{
		case 0:	break;
		case 1:
		case 2: 
			eyes++;
			break;
		
		case 3:	
		case 4:
		case 5:
		case 6: 
		case 7: {
			eyes++;
			Fate fate = useShapeLibrary();
			switch(fate)
			  {
			  case Seki:	
				  sekiSeen=true;	// ugly, set as a flag for use later.
				  break;
			  case Alive:
			  case Alive_In_Ko:
			  case Alive_With_Eye:	
				  eyes++;
				  break;
			  default:
				  break;
			  }
			}
		break;
		default: 
			{ eyes += hCountEyes();	
			}
		}
		return(eyes);
	}
	private int hCountEyes()
	{	int eyes = 0;
		int embeddedEyes = 0;
		int sz=0;
		int tsize = size();
		int etsize = 0;	// size of embedded territories
		int sweep = board.incrementSweepCounter();
		boolean bridged = false;
		GroupStack embeddedGroups = getEmbeddedColoredGroups();
		for(int elim=embeddedGroups.size()-1; elim>=0; elim--) 
			{ SimpleGroup egroup = embeddedGroups.elementAt(elim);
			   
			  Kind ekind = egroup.getKind();
			  switch(ekind)
			  {
			  default: throw G.Error("not expecting %s",ekind);
			  case FillWhite:
			  case FillBlack:	// when capture then fill
				  break;
			  case RemovedWhite:
			  case RemovedBlack:
				  break;
			  case White:
			  case Black:
			  case DeadWhite:
			  case DeadBlack:
			  case SafeWhite:
			  case SekiBlack:
			  case SekiWhite:
			  case SafeBlack:
				  if(egroup.countAvailableLiberties()<2)
				  {
					  if(egroup.size()>1)
					  	{ embeddedEyes++; if(egroup.size()>4) { embeddedEyes++; } }
					  else 
					  {	GoCell top = egroup.top();
					    if(!top.isFalseEyeCapture()) 
					    	{ embeddedEyes++; }
					  }
				  }
				  sz += egroup.size();
				  GroupStack embeddedTerritories = egroup.getTerritories();
				  for(int etlim=embeddedTerritories.size()-1; etlim>=0; etlim--)
				  {
					  SimpleGroup embedded = embeddedTerritories.elementAt(etlim);
					  if(embedded.getSweepCounter()!=sweep)
					  {
						if(embedded.isInside(this))
									  
							  {
						  		etsize += embedded.size();
							  }
						else if(embedded.allDead())
						{	// this is a change inspired by GO-ddyer-bobc-2024-12-10-0111
							// where the black group at the top right was awarded undeserved
							// eyes from the adjacent corner.   The key change is the black
							// stone at m15 which changes the connectivity of the white-and-empty
							// group that's adjacent to black.
							bridged = true;
						}
					  }
				  }
				  break;
			  }
			  
			}
		switch(tsize)
		{
		case 0: break;
		case 1:
			//if(!top().isFalseEyeCapture()) 
			{
			eyes++;
			}
			break;
		default:
			// these percentages award eyes to territories based solely
			// on the number of intron stones.  Clearly not admissable,
			// but usually works well.  Tweaking these numbers is hazardous
			if(sz+1==tsize) { eyes++; if(sz>4) {eyes++; }}
			else if(bridged) {}
			else if( etsize<tsize*0.4)
			{
			eyes += embeddedEyes;
			if(sz<tsize*0.75) { eyes++; }
			if(sz<tsize*0.4) { eyes++; }
			}
			break;
		}
		return(eyes);
	}

	private SimpleGroup getBorderAsGroup()
	{	Kind borderKind = kind.getBorderKind();
		SimpleGroup border = new SimpleGroup(board,borderKind.chip);
		GroupStack borderGroups = adjacentGroups();
		for(int lim=borderGroups.size()-1; lim>=0; lim--)
		{
			border.union(borderGroups.elementAt(lim));
		}
		return(border);
	}
	
	// 
	// scan from "c" and count the number of members reached, avoiding members of notIn
	//
	private int	getConnectedSize(GoCell c,SimpleGroup notIn,int tag)
	{	int n=0;
		if((c!=null)			// on the board
			&&(c.sweepTag!=tag)	// not already reached
			
			)
		{
			c.sweepTag = tag;
			if((containsPoint(c)!=null)	// is inside
					&& ((notIn==null)||(notIn.containsPoint(c)==null)))	// and not in the exclusion)
			{n++;
			for(int direction=c.geometry.n-1; direction>=0; direction--)
			{
				n+= getConnectedSize(c.exitTo(direction),notIn,tag);

			}}
		}
		return(n);
	}


	// 
	// if all members are reachable.  This is used to test if filling
	// the outside border connections is enough to connect the group
	//
	private boolean isSimplyConnected()
	{	int tag = board.incrementSweepCounter();
		int sz = getConnectedSize(top(),null,tag);
		return(sz==size());
	}
	//
	// get the border plus connections that are not inside
	// 
	private SimpleGroup getOutsideConnectedBorder()
	{		
		SimpleGroup border = getBorderAsGroup();
		GroupStack adjacent = adjacentGroups();	
		// fill all the outside connections
		for(int lim=adjacent.size()-1; lim>=0; lim--)
		{
			SimpleGroup adj = adjacent.elementAt(lim);
			ConnectionStack adjConn = adj.getConnections();
			for(int nConn = adjConn.size()-1; nConn>=0; nConn--)
			{
				Connection conn = adjConn.elementAt(nConn);
				if(containsPoint(conn.from)==null) { border.pushNew(conn.from); }
			}
		}
		return(border);
	}
	private SimpleGroup insideBorder = null;
	//
	// get the inside connected border, starting from the outside 
	// connected border.  Inside connected borders are considered
	// perfect if they are two eyed, otherwise better if they have
	// fewer stones
	//
	private SimpleGroup getInsideConnectedBorder(SimpleGroup border)
	{	if(insideBorder==null)
		{
		GroupStack adjacent = adjacentGroups();	
		insideBorder = getInsideConnectedBorder(border,adjacent,999);
		}
		return(insideBorder);
	}
	
	// 
	// true if this is a two eyed group when removing the "border" group
	// this works by scanning and testing of all elements are reached.
	//
	private boolean isTwoEyed(SimpleGroup border)
	{	int tag = board.incrementSweepCounter();
		int nonBorderSize = 0;
		GoCell startPoint = null;
		
		// count the members that are not in the border group,
		// and also find a seed member that's not in the border group
		for(int lim = size()-1; lim>=0; lim--)
		{
			GoCell seed = elementAt(lim);
			if((border==null) || (border.containsPoint(seed)==null))
			{	nonBorderSize++;
				startPoint = seed;
			}
		}
		// see if the seed expands in to the whole group
		return(getConnectedSize(startPoint,border,tag)<nonBorderSize);
	}

	private SimpleGroup getInsideConnectedBorder(SimpleGroup border,GroupStack adjacent,int bestSize)
	{	SimpleGroup best = null;
		// fill all the outside connections
		for(int lim=adjacent.size()-1; lim>=0; lim--)
			{
				SimpleGroup adj = adjacent.elementAt(lim);
				ConnectionStack adjConn = adj.getConnections();
				for(int nConn = adjConn.size()-1; nConn>=0; nConn--)
				{
					Connection conn = adjConn.elementAt(nConn);
					if(containsPoint(conn.from)!=null) 
						{ if(border.pushNew(conn.from))
							{
							boolean isConn = border.isSimplyConnected();

							if(isConn) 
								{ SimpleGroup bc = border.copy();
								  border.remove(conn.from,false);
								  return(bc);
								}
							// consider looking deeper
							if(border.size()+1<bestSize)
								{	// try another stone
								SimpleGroup candidate = getInsideConnectedBorder(border,adjacent,bestSize+1);
								border.remove(conn.from,false); 
								if(candidate!=null)
								{
									if(isTwoEyed(candidate)) 
										{ return(candidate);
										}
						
									if(candidate.size()<bestSize)
									{
									bestSize = candidate.size();
									best = candidate;
								}}}
								else
								{ border.remove(conn.from,false); 
								}
							}
						}
				}
			}
			return(best);
	}
	
	// true if everything is connected
	private boolean isConnectedShape()
	{	if(adjacentGroups().size()<=1) { return(true); }
		SimpleGroup border = getOutsideConnectedBorder();
	
		// if now is simply connected, the original group is connected 
		if(border.isSimplyConnected()) { return(true); }
		return(false);
	}
	//
	// count the eyes available to a colored group
	// including those available by connection
	//
	//
	// count the eyes available to a colored group
	// including those available by connection
	//
	public int countEyes(int sweep)
	{	if(sweepCounter==sweep) { return(0); }
		setSweepCounter(sweep);
		GroupStack territories = getTerritories();
		int eyes = 0;
		for(int lim=territories.size()-1; lim>=0;lim--)
		{
		SimpleGroup ter = territories.elementAt(lim);
		Kind tkind = ter.getKind();
		if(ter.getSweepCounter()!=sweep)	// not counted this one
		{
			ter.setSweepCounter(sweep);
			switch(tkind)
			{
			case Dame:
			case BlackTerritory:
			case WhiteTerritory:
			case BlackDame:
			case WhiteDame:
			case BlackSnapbackTerritory:
			case WhiteSnapbackTerritory:
			default: throw G.Error("not expecting %s",tkind);
			
			case FalseEye:
			case FillBlack:
			case FillWhite:	// got filled
			case ReservedForWhite:
			case ReservedForBlack:
			case SekiWhite:
			case SekiBlack:
				break;

			case RemovedWhite:
			case RemovedBlack:
				// avoid counting captures as eyes if they are false.
				eyes += ter.countUnconnectedEyes();
				break;
			case WhiteAndEmpty:
			case BlackAndEmpty:
				// if the shape is connected, then score the eyes directly
				if(ter.isConnectedShape())
					{
					eyes += ter.countConnectedEyes();
					sekiSeen |= ter.sekiSeen;
					if(eyes>=2) 
					{ if(ter.size()<=7) { ter.killEmbeddedGroups(); } 
					}}
				else 
					{ // score the eyes after connecting
					  eyes+=ter.countUnconnectedEyes(); 
					  sekiSeen |= ter.sekiSeen;
					}

				break;
			}
		}}
		
		if(eyes<2)
			{
			ConnectionStack connections = getConnections();
			for(int lim=connections.size()-1; lim>=0; lim--)
			{
				Connection conn = connections.elementAt(lim);
				eyes += conn.to.countEyes(sweep);
			}
			}
		if(eyes<2)
		{
			GroupStack adjacent = adjacentGroups();
			for(int lim=adjacent.size()-1; lim>=0; lim--)
			{
				SimpleGroup adj = adjacent.elementAt(lim);
				Kind akind = adj.getKind();
				switch(akind)
				{
				default: break;
				case RemovedWhite:
				case RemovedBlack:	// just capture, we get at least a liberty, but it's counted elsewhere
				case FillWhite:
				case FillBlack:	// capture then connect
					GroupStack connections = adj.adjacentGroups();
					for(int nconn = connections.size()-1; nconn>=0; nconn--)
					{	
						eyes += connections.elementAt(nconn).countEyes(sweep);
					}
					
					break;
				}
			}
			if(eyes<2)
			{
				if(countLocalSafeLiberties()>2) { eyes++; }
			}
		}
		return(eyes);
	}
	  public boolean sekiSeen = false;
	  public Kind classifySafety()
	  {
		int sweep = board.incrementSweepCounter();
	    sekiSeen = false;
	  	int eyes = countEyes(sweep);
	  	boolean seki = sekiSeen;
	  	if(seki && eyes<2)
	  		{ return(kind.getSekiKind()); 
	  		}
	  	if(eyes<2)	// negative eye counts indicate a seki
	  	{
	  	// for a group to be dead, it must also not be adjacent to other groups that
	  	// seem to be dead.  In that case it's a seki
	  	GroupStack adjacent = adjacentGroups();
	  	for(int lim=adjacent.size()-1; lim>=0; lim--)
	  	{
		  	int reSweep = board.incrementSweepCounter();
	  		SimpleGroup adj = adjacent.elementAt(lim);
	  		// this test excludes some seki declared with an embedded eye
	  		// PRO BEST TEN P-107
	  		if(!adj.getKind().isDead())
	  		{
	  		sekiSeen = false;
	  		int adjEyes = adj.countEyes(reSweep);
	  		boolean adjSeki = sekiSeen;
	  		if((adjSeki || (adjEyes==eyes))
	  				//&& (adj.getLiberties().nonAdjacentMembers(kind.chip)<2)
	  				//&& (adj.size()>=3)
	  				)
	  			{ 
	  			// this is were case dead-9x9 marks the groups as seki
	  			return(kind.getSekiKind()); 
	  			}
	  		}}
	  	}
	  	return(eyes>=2?kind.getSafeKind():Kind.Empty);
	  }
	  //
	  // look up a shape in the shape library, and return its fate if known
	  //
	  // using the actual count of outside liberties would be more accurate. 
	  // Also, for some borderline shapes, additional fill moves would be needed to stay alive.
	  //
	  public Fate useShapeLibrary()
	  {
			if(size()<=7)
			{
			// 45TH-HONINBO-LEAGUE-GAME-5 is a good test case
			ShapeProtocol shape = ShapeLibrary.find(this);
			if(shape!=null)
				{ X_Position xpos = getXPosition();
				  Y_Position ypos = getYPosition();
				  int xmin = getXMin();
				  int ymin = getYMin();
				  int introns = 0;
				  for(int lim=size()-1; lim>=0; lim--)
				  {	GoCell member = elementAt(lim);
				  	if(member.topChip()!=null)
				  	{
				  		introns |= shape.getIntronBit(member.getX()-xmin,member.getY()-ymin);
				  	}   				  
				  }
				  //ResultProtocol fate2 = shape.Fate_Of_Shape(Move_Order.Second,xpos,ypos,introns);
				  ResultProtocol fate1 = shape.Fate_Of_Shape(Move_Order.First,xpos,ypos,introns);
				  //SingleResult f2 = fate2.Fate_for_N_Liberties(2);
				  SingleResult f1 = fate1.Fate_for_N_Liberties(2);
				  //Fate fate2a = f2.getFate();
				  Fate fate1a = f1.getFate();
				  //G.print("Shape ="+fate+" for "+this+" \n"+fate1+"\n"+fate2);
				  return(fate1a);
				  
				}
			else { G.Error("Shape not found for %s",this); }
			}
			return(Fate.Unknown);
	  }
	  
	//
	// classify an empty group as BlackTerritory, WhiteTerritory or Dame
	// skipping stones declared dead.  This is the first step in classifying
	// the territory on the board.
	//
	public Kind classifyEmptyGroup()
	  {	boolean blackContacts = false;
	    boolean whiteContacts = false;
	    for(int lim = size()-1; lim>=0; lim--)
	    {
	    	GoCell member = elementAt(lim);
	    	for(int direction = member.geometry.n-1; direction>=0; direction--)
			  {	GoCell next = member.exitTo(direction);
			  	if(next!=null)
			  	{
			  		GoChip top = next.topChip();
			  		if(top==null) {}
			  		else if (top==GoChip.black) 
			  			{ if(whiteContacts) { return(Kind.Dame);}
			  			  blackContacts=true; 
			  			}
			  		else if (top==GoChip.white)
			  			{ if(blackContacts) { return(Kind.Dame); }
			  			  whiteContacts=true;
			  			}
			  		else { G.Error("not expecting %s",top); }
			  	}
			  }
	    }
	    if(blackContacts) { return(Kind.BlackTerritory); }	// has only black contacts
	    else if(whiteContacts) { return(Kind.WhiteTerritory); }	// has only white contacts
	    else 
	    	return(Kind.Dame);
	  }
	private GoCell unSharedLiberty(SimpleGroup other)
	{
		SimpleGroup olibs = other.getLiberties();
		for(int lim=olibs.size()-1; lim>=0; lim--)
		{	GoCell un = olibs.elementAt(lim);
			if(!isLiberty(un)) { return(un); }
		}
		return(null);
	}
	//
	// heuristically mark some potential territory groups as real territory
	//
	public boolean hCanMakeTerritory(SimpleGroup emptyGroup)
	{  	
	  	SimpleGroup insec = hasInsecureBoundary();
	    if(insec==null)
	    {	
	    	GoChip color = kind.getIntronColor();
	    	int embeddedSize = 0;
	    	int embeddedAtariSize = 0;
	    	int embeddedAtariGroups = 0;
	    	GroupStack embeddedGroups = getEmbeddedColoredGroups();
	    	for(int lim=embeddedGroups.size()-1; lim>=0; lim--)
	    	{
	    		SimpleGroup gr = embeddedGroups.elementAt(lim);
	    		// was gr.countAvailableLiberties, but changed because in game
	    		// GO-bobc-ddyer-2016-10-11-0011 a seki with a throw-in was
	    		// classified incorrectly.  Using countAvailableLiberties counted
	    		// the white group as atari, which it is not - the mandatory fill 
	    		// would capture and gain back the liberty.  Tested against
	    		// allgames.sgf, this didn't break anything.
	    		int libs = gr.countLiberties();
	    		int sz = gr.size();
	    		if(libs==1) { embeddedAtariSize += sz; embeddedAtariGroups++; }

	    		embeddedSize += sz;
	    	}
	     	
	    	if(	((size()>=8)		// only for big groups
	    			&& ((embeddedSize-embeddedAtariSize)<=6)) 
	    		|| ((embeddedAtariSize > 6)
	    			|| (embeddedAtariGroups > 1)
	    			|| ((embeddedSize-embeddedAtariSize)*3<size())))	// 3 is an ad-hoc parameter, but sensitive so don't change lightly
	    	{ 	// if there are no unshared liberties, this is a potential seki
	     		int nonLibs = nonAdjacentMembers(color);
	    		if(nonLibs>=1) { 		return(true); } 
	    		//
	    		// this is pretty subtle - if the empty group that's part of the mixed
	    		// group can't reach territory, it's a potential seki 
	    	  	if(emptyGroup.canReachTerritory(board.incrementSweepCounter(),kind.getTerritoryKind(),this)!=null) 
	    	  		{ return(true); }
	    		return(false);
	    	}
	    }
	    return(false);
	  }
	//
	// this is used to classify large-ish empty spaces as probably territory for
	// a particular player.
	//
	private boolean hIsTerritory(Kind forKind)
	{	
		if((size()>7) && (kind==Kind.Dame))
		{	int borderColorSize = 0;
			int borderTotalSize = 0;
			GoChip borderColor = forKind.fillChip;
			SimpleGroup border = getBorder();
			int sz = border.size();
			borderTotalSize += sz;
			for(int lim=sz-1; lim>=0; lim--)
			{
				GoChip top = border.elementAt(lim).topChip();
				if(top==borderColor) { borderColorSize++; }
			}
			return((borderTotalSize*0.75)<borderColorSize);
		}
		return(false);
	}
}
