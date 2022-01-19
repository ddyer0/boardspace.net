
package goban.shape.shape.gui;
import java.util.*;

import goban.shape.beans.*;
import goban.shape.shape.ElementProvider;
import goban.shape.shape.Globals;
import goban.shape.shape.LocationProvider;
import goban.shape.shape.SimpleLocation;

public class Simple_Group implements Globals, ElementProvider
{	public Vector<LocationProvider> members=null;		//a vector of the current members of the group
	public GridBoard board=null;	//the board which this group refers too
	int colors;				//a set of colors for this group
	
	
	/* constructor */
	public Simple_Group(int c,GridBoard b,LocationProvider seed) 
	{	colors=c; 
		board=b; 
		members = new Vector<LocationProvider>();
		Find_Members(seed); }
	
	public Simple_Group(int c,GridBoard b)
	{
		colors = c;
		board = b;
		members = new Vector<LocationProvider>();
	}
	public void addElement(LocationProvider e) { members.addElement(e); }
	public void Union(Simple_Group other)
	{ for(Enumeration<LocationProvider> e=other.members.elements(); e.hasMoreElements();)
		{	LocationProvider p = e.nextElement();
			if(containsPoint(p)==null) { members.addElement(p); }
		}	 
	}
	public void Remove()
	{
		for(Enumeration<LocationProvider> e=members.elements(); e.hasMoreElements(); )
		{ LocationProvider p = e.nextElement();
			board.RemoveWhiteStone(p);
			board.RemoveBlackStone(p);
		}	
	}
	public void Restore()
	{
		for(Enumeration<LocationProvider> e=members.elements(); e.hasMoreElements(); )
		{ LocationProvider p = e.nextElement();
			if((colors&WhiteColor)!=0) { board.RemoveBlackStone(p); board.AddWhiteStone(p); }
			else { board.RemoveWhiteStone(p); board.AddBlackStone(p); }
		}	
	}
	private boolean isMemberColor(LocationProvider p)
	{ return(isMemberColor(p.getX(),p.getY()));
	}
	private boolean isMemberColor(int x,int y)
	{	if(((WhiteColor&colors)!=0) && board.ContainsWhiteStone(x,y))
		{return(true);
		}
		if(((BlackColor&colors)!=0) && board.ContainsBlackStone(x,y))
		{return(true);
		}
		if(((EmptyColor&colors)!=0) && board.isEmpty(x,y)) 
		{return(true);
		}
		return(false);
	}

	public int Find_Members(int x,int y)
	{
		if(containsPoint(x,y)!=null) {return(members.size()); }	//already in
		if(isMemberColor(x,y)) 
		{ members.addElement(new SimpleLocation(x,y));
		  return(FindMembersFrom(x,y));
		}
		return(members.size());
	}
	public int FindMembersFrom(int x,int y)
	{
  	  Find_Members(x+1,y);
  	  Find_Members(x-1,y);
  	  Find_Members(x,y+1);
  	  Find_Members(x,y-1);
  	  return(members.size());
	}
	public int Find_Members(LocationProvider seed)
	{ if(containsPoint(seed)!=null) {return(members.size()); }	//already in
	  if(isMemberColor(seed)) 
	  { members.addElement(seed);
	    return(FindMembersFrom(seed.getX(),seed.getY()));
	  }
	  return(members.size());
	}
	
	public Simple_Group Find_Liberties()
	{ 	Simple_Group v = new Simple_Group(EmptyColor,board);
		for(Enumeration<LocationProvider> e=members.elements(); e.hasMoreElements(); )
		{ LocationProvider p = (LocationProvider)e.nextElement();
		  int x = p.getX();
		  int y = p.getY();
		  if((v.containsPoint(x+1,y)==null) && board.isEmpty(x+1,y)) 
		  	{ v.addElement(new SimpleLocation(x+1,y)); 
		  	}
		  if((v.containsPoint(x-1,y)==null) && board.isEmpty(x-1,y))
		  	{ v.addElement(new SimpleLocation(x-1,y)); 
			}
		  if((v.containsPoint(x,y+1)==null)	&& board.isEmpty(x,y+1))
		  	{ v.addElement(new SimpleLocation(x,y+1)); 
			}
		  if((v.containsPoint(x,y-1)==null)&& board.isEmpty(x,y-1))
		  	{ v.addElement(new SimpleLocation(x,y-1)); 
			}
		}
	    return(v);
	}
	
	public int N_Liberties()
	{
		return(Find_Liberties().size());	
	}	
	public String toString()
	{ return("#<" + getClass().getName() + " " + size() + "M " + N_Liberties() + "L >");
	}	
	private void AddGroup(Vector<Simple_Group> v,LocationProvider p,int colormask)
	{
		for(Enumeration<Simple_Group> e = v.elements(); e.hasMoreElements();)
		{ Simple_Group g=e.nextElement();
			if(g.containsPoint(p)!=null) 
			{ //System.out.println(g + " contains " + p);
				return; }
		}
		{Simple_Group g = new Simple_Group(colormask,board,p);
			//System.out.println("new group at " + p);
			v.addElement(g);
		}	
	}
	public boolean isColor(LocationProvider p,int colormask)
	{	if(((colormask&WhiteColor)!=0) && board.ContainsWhiteStone(p)) {return(true);}
		if(((colormask&BlackColor)!=0) && board.ContainsBlackStone(p)) {return(true);}
		if(((colormask&EmptyColor)!=0) && board.isEmpty(p)) {return(true); }
		if(((colormask&BorderColor)!=0)	&& !board.isInside(p))
		{return(true);
		}
		return(false);
	}
	
	public Vector<Simple_Group> Find_Adjacent_Groups(int colormask)
	{	Vector<Simple_Group> v = new Vector<Simple_Group>();
		
		for(Enumeration<LocationProvider> e=members.elements(); e.hasMoreElements(); )
		{ LocationProvider p = e.nextElement();
		  int x = p.getX();
		  int y = p.getY();
			{LocationProvider lib = new SimpleLocation(x+1,y);
				if(isColor(lib,colormask)) { AddGroup(v,lib,colormask); }
			}
			{LocationProvider lib = new SimpleLocation(x-1,y);
				if(isColor(lib,colormask)) { AddGroup(v,lib,colormask);}
			}
			{LocationProvider lib = new SimpleLocation(x,y+1);
				if(isColor(lib,colormask)) { AddGroup(v,lib,colormask); }
			}
			{LocationProvider lib = new SimpleLocation(x,y-1);
				if(isColor(lib,colormask)) { AddGroup(v,lib,colormask); }
			}
			
		}
		return(v);
		
	}

	public int N_Adjacent_Groups(int colormask)
	{	return(Find_Adjacent_Groups(colormask).size());
	}
	public LocationProvider elementAt(int n) {
		return members.elementAt(n);
	}
	public int size() {
		return(members.size());
	}

	public LocationProvider containsPoint(int x, int y) {
		for(int lim=members.size()-1; lim>=0; lim--)
			{ LocationProvider p = members.elementAt(lim);
			  if(p.equals(x,y)) {return(p); }
			}
		return(null);
	}

	public LocationProvider containsPoint(LocationProvider p) {
		return(containsPoint(p.getX(),p.getY()));
	}
	
}
