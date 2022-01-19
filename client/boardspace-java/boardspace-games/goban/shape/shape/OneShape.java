package goban.shape.shape;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
import bridge.Util;
import lib.AR;
import lib.CompareTo;
import lib.G;

/** hold the result data for one shape in all positions */
public class OneShape extends SimpleShape implements Globals,ShapeProtocol,Serializable,CompareTo<OneShape>
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public String picture;							//an ascii picture of the shape
	public String fate_comment=null;				//some manually entered comment
	public String key_points=null;					//"key points" description, not yet used by this incarnation
	public ResultCode results[];					//the detailed results for the shape
	/** minimal constructor */
	public OneShape() {};
	/** more complete constructor */
	public OneShape(String name,LocationProvider p[],ResultCode res[])
	{ this.name=name;
		this.points=p;
		this.results=res;
	}
	void readObject(ObjectInputStream s)  throws IOException ,ClassNotFoundException 
	{
		s.defaultReadObject();
	}

	/** get the result code (for all introns) of a shape for a move position */
	public ResultCode Fate_Of_Shape_Results
		( Move_Order move_spec, /* first or second */
		X_Position x_pos, 		 /* left center or right */
		Y_Position y_pos)		/* top center or bottom */
	{	int idx = zhash.Encode_Move_Geometry(move_spec,x_pos,y_pos);
		int nres = results.length;
		//System.out.println("Seeking result for " + ResultCode.decode_position_bit(idx));
		for(int i=0;i<nres;i++)
		{ ResultCode r = results[i];
			//System.out.println("result " + r + " " + r.positions);
			if((idx&r.positions)!=0) { return(r); }
		}
		return(null);
	}
	
	/** get the result for a single intron pattern for a particular shape */
	public ResultProtocol Fate_Of_Shape
		(Move_Order move_spec,	/* first or second */
		X_Position x_pos,		  /* left center or right */
		Y_Position y_pos,		  /* top center or bottom */
		int introns)					  /* bit mask of internal positions occupied by stones */
	{
		ResultCode r = Fate_Of_Shape_Results(move_spec,x_pos,y_pos);
		return(r.results[introns]);
	}
	
	/** get the exact result for a shape for a move position */
	public SingleResult Exact_Fate_Of_Shape
		(Move_Order move_spec,	/* first or second */
		X_Position x_pos,		  /* left center or right */
		Y_Position y_pos,		  /* top center or bottom */
		int introns,					  /* bit mask of internal positions occupied by stones */
		int libs)								/* the liberty count */
	{
		ResultProtocol	r = Fate_Of_Shape(move_spec,x_pos,y_pos,introns);
		return(r.Fate_for_N_Liberties(libs));
	}
	
	/* this is used internally when generating the isomers of a shape, to detect
	duplicates */
	boolean MatchesSomeIsomer(Vector<LocationProvider[]> v,LocationProvider p[])
	{	
		LocationProvider testmin =	zhash.vectormin(p);
		for(Enumeration<LocationProvider[]> e = v.elements(); e.hasMoreElements(); )
		{ LocationProvider compareto[] = e.nextElement();
		  LocationProvider compmin = zhash.vectormin(compareto);	
			int dx = compmin.getX()-testmin.getX();
			int dy = compmin.getY()-testmin.getY();
			if(zhash.equalvector(p,compareto,dx,dy)) { return(true); }
		}
		return(false);
	}	
	/** encode y-flip + 4 rotations as an integer */
	public int Encode_Isomer(int flip,int rotate) 
	{
		return(flip*4+rotate);
	}
	/** extend a single shape to a list of ShapeNormalizers, one for each distinct isomer
	of the parent shape.  Each normalizer contains an array of points which correspond to
	the local picture of the shape, but importantly, the list is in the same order as the
	original, so the ordinal "play here" values contained in SingleResult objects we eventually 
	find will refer to the correct points.
	*/
	public ShapeNormalizer[] ExtendToIsomers()
	{	LocationProvider pts[]=points;
		Vector<LocationProvider[]> isomers=new Vector<LocationProvider[]>();
		Vector<Integer> isomer_codes = new Vector<Integer>();
		for(int j=0;j<2;j++)
		{
			for(int i=0; i<4; i++)
			{
				if(!MatchesSomeIsomer(isomers,pts))
				{
					isomers.addElement(pts);
					isomer_codes.addElement(Encode_Isomer(j,i));
				}
				pts = zhash.Rotate_Points(pts);
			}
			pts=zhash.Flip_Points(pts);
		}
		{ int siz = isomers.size();
			ShapeNormalizer norm[] = new ShapeNormalizer[siz];
			for(int i=0; i<siz; i++)
			{ norm[i] = new ShapeNormalizer(this,
				(LocationProvider[])(isomers.elementAt(i)),
					((Integer)(isomer_codes.elementAt(i))).intValue());
			}
			return(norm);
		}
	}
	/* find the isomer of "other" which is similat to this shape */
	ShapeNormalizer FindSimilarIsomer(OneShape other)
	{
		ShapeNormalizer norm[]= other.ExtendToIsomers();
		for(int i=0;i<norm.length;i++)
		{ ShapeNormalizer n = norm[i];
			if(zhash.similarvector(points,n.points))
			{ return(n);
			}
		}
		return(null);
	}	

	public OneShape Combine_With(OneShape othershape)
	{ ShapeNormalizer similar = FindSimilarIsomer(othershape);
		if(similar!=null)
		{  OneShape copy = similar.NormalizedCopy();
			ResultCode other_results[]=copy.results;
			Vector<ResultCode> intermediate_results = new Vector<ResultCode>();
			int oldlen = results.length;
			
			for(int i=0;i<other_results.length;i++)
			{//reorder the results for the new point list
				ResultCode thisres = other_results[i].ReOrder(copy.points,points);
				int thisppos = thisres.positions;
				
				for(int j=0;j<oldlen;j++)
				{
					if((results[j].positions & thisppos)!=0)
					{/* results in the "similar" group cover the same positions are
						in our group. Optionally complain if they don't exactly match */
						if(CheckDuplicates && (results[j].CompareWith(thisres)!=0))
						{
							G.print("position conflict for merged shapes "+ this + " and " + similar);
							G.print("Details:");
							results[j].CompareWith(thisres);
						}      			
					} else
					{
						intermediate_results.addElement(thisres);	
					}
				}
			}
			/* not intermediate_results is a fully transformed and pruned copy
			of other_results, ready to combine with our results */      
			{  
				
				int newlen = oldlen+intermediate_results.size();
				ResultCode newres[] = new ResultCode[newlen];
				for(int i=0;i<oldlen;i++) 
				{ newres[i]=results[i];
					//console.println("old " + newres[i]);
				}
				
				for(int i=oldlen,j=0;i<newlen;i++,j++) 
				{ newres[i]= (ResultCode)intermediate_results.elementAt(j);
				}
				OneShape sh = new OneShape(this.name + " " + copy.name,points,newres);
				return(sh);
			}}
		return(null);
	}
	@Override
	public int compareTo(OneShape o) {
		long a = (long)hashCode();
		long b = (long)o.hashCode();
		return(a>b ? 1 : a==b ? 0 : -1);
	}
	
	@Override
	public int altCompareTo(OneShape o) {
		long a = (long)hashCode();
		long b = (long)o.hashCode();
		return(a>b ? 1 : a==b ? 0 : -1);
		}
	
	public int getVersion() {
		return((int)serialVersionUID);
	}
		  
    public void externalize(DataOutputStream out) throws IOException 
    {
		    Util.writeUTF(picture, out);
		    Util.writeUTF(fate_comment, out);
		    Util.writeUTF(key_points, out);
		    Util.writeObject(results, out);
		    Util.writeObject(points,out);
	}
    public void internalize(int version, DataInputStream in) throws IOException {
		   picture = Util.readUTF(in);
		   fate_comment = Util.readUTF(in);
		   key_points = Util.readUTF(in);
		   {
		   Object[]v = (Object[])Util.readObject(in);
		   if(v!=null)
		   {	results = new ResultCode[v.length];
		   		AR.copy(results,v);
		   }
		   else { results=null; }
		   }
		   {
		   Object[]v = (Object[])Util.readObject(in);
		  
		   if(v!=null)
		   {	points = new LocationProvider[v.length];
		   		AR.copy(points,v);
		   }
		   else { points=null; }
		   }
		  }
    public String getObjectId() {
		return("OneShape");
		}	
}
