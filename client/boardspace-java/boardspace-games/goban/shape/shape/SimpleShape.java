package goban.shape.shape;
import lib.G;
import lib.NamedObject;

import java.io.Serializable;

public class SimpleShape extends NamedObject implements Globals,Serializable,ElementProvider
{
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;

	public LocationProvider points[]=null;						//the normalized list of points in the shape
		
	public LocationProvider[] getPoints()
	{return(points); 
	}
	
	public int Width()
	{	return(zhash.vectorwidth(points));
	}
	
	public int Height()
	{ return(zhash.vectorheight(points));
	}
	
	public int hashCode()
	{	return(zhash.hash_black(points));
	}
	public String Ascii_Picture()
	{ return(zhash.Ascii_Picture(this));	
	}
	
	public boolean ContainsPoint(int x,int y)
	{ return(zhash.hasmember(points,x,y));
	}
	
	public int getIntronBit(int x,int y)
	{
		for(int i=0,bit=1,sz=size(); i<sz; i++,bit=bit<<1 )
		{
			if(points[i].equals(x,y)) { return(bit); }
		}
		throw G.Error("%d,%d is not contained",x,y);
	}
	
	/* compare a hash match to be sure its a real match.  In 
	"production" mode this somewhat lengthly check should be
	able to be eliminated
	*/
	public boolean equal(ElementProvider v2,int xmin,int ymin)
	{
		int l1 = points.length;
		int l2 = v2.size();
		if(l1==l2)
    	{ for(int i=0; i<l1; i++)
    		{ LocationProvider p1 = points[i];
				if(!zhash.hasmember(v2,p1.getX()+xmin,p1.getY()+ymin)) 
				{System.out.println("points differ");
					return(false); /* some point doesn't match */
				}
    		}
    		return(true); /* all points match */
    	}		
    	System.out.println("lengths differ");
		return(false); /* different lengths */
	}
	/* compare a hash match to be sure its a real match.  In 
	"production" mode this somewhat lengthly check should be
	able to be eliminated
	*/
	public boolean equal(LocationProvider v2[],int xmin,int ymin)
	{
		int l1 = points.length;
		int l2 = v2.length;
		if(l1==l2)
    	{ for(int i=0; i<l1; i++)
    		{ LocationProvider p1 = points[i];
				if(!zhash.hasmember(v2,p1.getX()+xmin,p1.getY()+ymin)) 
				{System.out.println("points differ");
					return(false); /* some point doesn't match */
				}
    		}
    		return(true); /* all points match */
    	}		
    	System.out.println("lengths differ");
		return(false); /* different lengths */
	}
	
	public boolean equal(ElementProvider v2)
	{
		
		LocationProvider p = zhash.vectormin(v2);
		return(equal(v2,p.getX(),p.getY()));
	}
	/** position to play */
	public final LocationProvider Position_To_Play(SingleResult r)
	{
		int ord = r.Ordinal_Place_to_Play();
		if((ord>0) && (ord<=points.length))
		{ return(points[ord-1]);
		}
		else 
		{return(null); 
		}	
	}
	/** do some meta analysis based on the combined results for moving first and
	moving second.  If the results are the same, and the result is either strictly
	alive or strictly dead, then there is no need to move, even as a ko threat */
	public final LocationProvider Position_To_Play(SingleResult r,SingleResult other)
	{
		if(other!=null)
		{ Fate r_fate = r.getFate();
  		  Fate other_fate = other.getFate();
  		  if((r_fate==other_fate) 
  		  	&& (//(r_fate==Fate.Alive)	|| (r_fate==Fate.Alive_With_Eye)||
					(r_fate==Fate.Dead) || (r_fate==Fate.Dead_With_Eye)))
			{ return(null); /* no move is necessary */
			}
		}
		return(Position_To_Play(r));
	}

	public LocationProvider elementAt(int n) {
		return(points[n]);
	}

	public int size() {
		return(points==null ? 0 : points.length);
	}
	
	public final LocationProvider containsPoint(int x,int y)
	{	for(int lim=points.length-1; lim>=0; lim--)
		{ LocationProvider el = points[lim];
			if(el.equals(x,y)) return(el);
		}
		return(null);
	}

	public LocationProvider containsPoint(LocationProvider p) {
		return(containsPoint(p.getX(),p.getY()));
	}
	
}
