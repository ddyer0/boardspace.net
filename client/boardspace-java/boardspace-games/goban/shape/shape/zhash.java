
package goban.shape.shape;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import bridge.Utf8OutputStream;
import bridge.Utf8Printer;
import lib.G;
import lib.Random;
public class zhash implements Globals
{
	
	private static final int white_seed = 05040323;
	private static final int black_seed = 79684858;
	private static final int zhash_white[] = new int[max_board_size*max_board_size];
	private static final int zhash_black[] = new int[max_board_size*max_board_size];
	static {
		Random rw=new Random(white_seed);
		Random rb=new Random(black_seed);
		for(int i=0;i<zhash_white.length;i++)
		{zhash_white[i]=rw.nextInt();
			zhash_black[i]=rb.nextInt();
		}
	}
	private static final int pointvalue(LocationProvider p,int xmin,int ymin)
	{ return((p.getX()-xmin)+(p.getY()-ymin)*max_board_size);
	}
	static final int hash_white(ElementProvider v,int xmin,int ymin)
	{	int n = v.size();
		int val=0;
		for(int i=0; i<n; i++) { val ^= zhash_white[pointvalue(v.elementAt(i),xmin,ymin)]; }
		return(val);
	}
	static final int hash_black(ElementProvider v,int xmin,int ymin)
	{	int n = v.size();
		int val=0;
		for(int i=0; i<n; i++) 
			{ int pv = pointvalue(v.elementAt(i),xmin,ymin);
			  val ^= zhash_black[pv];
			}
		return(val);
	}
	static final int zhash_black(ElementProvider v)
	{ return(hash_black(v,0,0));
	}
	static final int zhash_white(ElementProvider v)
	{ return(hash_white(v,0,0));
	}
	static final int hash_black(LocationProvider p[],int xmin,int ymin)
	{	int n = p.length;
		int val=0;
		for(int i=0; i<n; i++) 
			{ int pv = pointvalue(p[i],xmin,ymin);
			  val ^= zhash_black[pv]; }
		return(val);
	}
	static final int hash_white(LocationProvider p[],int xmin,int ymin)
	{	int n = p.length;
		int val=0;
		for(int i=0; i<n; i++) { val ^= zhash_white[pointvalue(p[i],xmin,ymin)]; }
		return(val);
	}
	static final int hash_black(LocationProvider v[])
	{ return(hash_black(v,0,0));
	}
	static final int hash_white(LocationProvider v[])
	{ return(hash_white(v,0,0));
	}
	
	/** convert a string picture of a shape into an array of points */
	public static LocationProvider[] Make_Point_Array(String s)
	{ int npoints=0;
		int minx = 0;
		int maxx = 0;
		int miny = 0;
		int len = s.length();
		
		{int idx = 0;
			int x = 0;
			int y = 0;
			while(idx<len)
			{ char ch = s.charAt(idx);
				idx++;
				switch(ch)
				{
				case ' ': x++; break;
				case '\r':
				case '\n': if(x!=0) { y++; x=0; } break;
				case '.': 
				case 'x':
				case 'X': 
					if(npoints==0) { minx=x;maxx=x; miny=y; }
					else { minx = Math.min(x,minx); 
     	  			 maxx = Math.max(x,maxx);     
					}
					npoints++;
					x++;
					break;
				default: G.Assert(false,"Unexpected char " + ch + " in " + s);
				}
			}}
		
		/* phase 2, build the actual point list */
		{int idx=0;
			int i=0;
			int x=0;
			int y=0;
			LocationProvider r[]=new SimpleLocation[npoints];
			while(idx<len)
			{ char ch = s.charAt(idx);
				idx++;
				switch(ch)
				{
				case ' ': x++; break;
				case '\r':
				case '\n': if(x!=0) { y++; x=0; } break;
				case '.': 
				case 'x':
				case 'X': r[i]=new SimpleLocation((x-minx)/2,y-miny); 
					i++;
					x++;
					break;
				default: ;
				}
			}
			return(r);
		}
	}
	/* find the minimum x,y coordinate in a list of points.  We
	use this to normalize the bounding rectangle of the shape
	to include 0,0
	*/
	public static final LocationProvider vectormin(ElementProvider v)
	{ int n=v.size();
	  LocationProvider res = null;
  	  if(n>0)
  	  {LocationProvider p = v.elementAt(0);
			int minx = p.getX();
			int miny = p.getY();
			
			for(int i=1; i<n; i++)  	
			{ p = v.elementAt(i);
				minx = Math.min(minx,p.getX());
				miny = Math.min(miny,p.getY());
			}
			res=new SimpleLocation(minx,miny);
		}
		return(res);
	}

	public static final int IndexOf(LocationProvider v[],LocationProvider p)
	{
		for(int i=0;i<v.length;i++) { if (v[i].equals(p))	return(i); }
		return(-1);
	}
	
	public static final int IndexOf(LocationProvider v[],int x,int y)
	{
		for(int i=0;i<v.length;i++) { if (v[i].equals(x,y))	return(i); }
		return(-1);
	}
	
	public static final LocationProvider vectormin(LocationProvider pts[])
	{ int n=pts.length;
	  LocationProvider res = null;
  	  if(n>0)
  	  {LocationProvider p = pts[0];
			int minx = p.getX();
			int miny = p.getY();
			
			for(int i=1; i<n; i++)  	
			{ p = pts[i];
				minx = Math.min(minx,p.getX());
				miny = Math.min(miny,p.getY());
			}
			res=new SimpleLocation(minx,miny);
		}
		return(res);		 			
	} 
	public static final int vectorheight(LocationProvider points[])
	{
		int len = points.length;
		int miny = points[0].getY();
		int maxy = miny;
		for(int i=1; i<len;i++) 
    	{ int y = points[i].getY();
			miny=Math.min(y,miny);  
			maxy=Math.max(y,maxy);  
    	}	
		return(maxy-miny+1);
	}		
	public static final int vectorwidth(LocationProvider points[])
	{
		int len = points.length;
		int minx = points[0].getX();
		int maxx = minx;
		for(int i=1; i<len;i++) 
    	{ int x = points[i].getX();
			minx=Math.min(x,minx);  
			maxx=Math.max(x,maxx);  
    	}	
		return(maxx-minx+1);
		
	}
	public static final int vectorheight(ElementProvider points)
	{
		int miny = 1;
		int maxy = 0;
		for(int lim=points.size()-1; lim>=0; lim--)
    	{LocationProvider p = points.elementAt(lim);
			int y = p.getY();
			if(miny>maxy) { miny = maxy = y; }
			else {  miny=Math.min(y,miny);  
				maxy=Math.max(y,maxy);  
			}	
		}
		return(maxy-miny+1);
	}		
	public static final int vectorwidth(ElementProvider points)
	{
		int minx = 1;
		int maxx = 0;
		for(int lim=points.size()-1; lim>=0; lim--)
    	{LocationProvider p = points.elementAt(lim);
			int x = p.getX();
			if(minx>maxx) { minx = maxx = x; }
			else {  minx=Math.min(x,minx);  
				maxx=Math.max(x,maxx);  
			}	
		}
		return(maxx-minx+1);
		
	}
	
	static public final String Ascii_Picture(ElementProvider points)
	{ LocationProvider min = zhash.vectormin(points);
		ByteArrayOutputStream ob = new Utf8OutputStream();
		PrintStream s = Utf8Printer.getPrinter(ob);
		int w = zhash.vectorwidth(points);
		int h = zhash.vectorheight(points);
		for(int y=0;y<h;y++) 
		{ for(int x=0;x<w;x++)
			{ String str = (points.containsPoint(min.getX()+x,min.getY()+y)!=null)
				? " X" : " .";
				s.print(str);
			} s.print('\n');
		}
		s.close();
		return(ob.toString());
	}
	/** return a new list of points, flipped on the y axis.  The resulting
	list preserves the ordinal position of the flipped points, so that
	to[i] and from[i] correspond to the same point, modulo the coordinate 
	transformation.  This is important for the mapping from SingleResult
	codes back to the board position it is supposed to refer to.
	*/
	static final LocationProvider[] Flip_Points(LocationProvider pts[])
	{
		int h = vectorheight(pts)-1;
		int len = pts.length;
		LocationProvider res[] = new LocationProvider[len];
		
		for(int i=0; i<len; i++) 
		{ LocationProvider from=pts[i];
			res[i]=new SimpleLocation(from.getX(),h-from.getY());
		}
		return(res);
	}
	/** return a new list of points, rotated 90 degrees clockwise. The resulting
	list preserves the ordinal position of the flipped points, so that
	to[i] and from[i] correspond to the same point, modulo the coordinate 
	transformation.  This is important for the mapping from SingleResult
	codes back to the board position it is supposed to refer to.*/
	static final LocationProvider[] Rotate_Points(LocationProvider pts[])
	{	
		int h = vectorheight(pts)-1;
		int len = pts.length;
		LocationProvider res[] = new LocationProvider[len];
		
		for(int i=0; i<len; i++) 
		{ LocationProvider from=pts[i];
			res[i]=new SimpleLocation(h-from.getY(),from.getX());
		}
		return(res);
	}
	
	/** return true if the vector of points contains a point x,y */	
	public static final boolean hasmember(ElementProvider v,int x,int y)
	{ int len = v.size();
   	 for(int i=0;i<len;i++) 
   	 {LocationProvider p = v.elementAt(i);
			if((p.getX()==x)&&(p.getY()==y)) { return(true); }
   	 }
   	 return(false);
	}
	/** return true if the array of points contains a point x,y */
	public static final boolean hasmember(LocationProvider v[],int x,int y)
	{ int len = v.length;
   	 for(int i=0;i<len;i++) 
   	 {LocationProvider p = v[i];
			if((p.getX()==x)&&(p.getY()==y)) { return(true); }
   	 }
   	 return(false);
	}
	/** return true if the two point vectors contain the same sets of points,
	with the second offset by dx, dy
	*/
	static public boolean equalvector(LocationProvider p[],LocationProvider compareto[],int dx,int dy)
	{	int testlen = p.length;
		if(testlen!=compareto.length) { return(false); }
		for(int i=0;i<testlen; i++)
		{LocationProvider testpoint = p[i];
			if(!zhash.hasmember(compareto,dx+testpoint.getX(),dy+testpoint.getY())) 
			{return(false);}	
		}
		return(true);
	}
	/** return true if the two vectors contain the same set of points, offset
	by some constant x,y offset */
	static public boolean similarvector(LocationProvider v1[],LocationProvider v2[])
	{	LocationProvider v1min = zhash.vectormin(v1);
		LocationProvider v2min = zhash.vectormin(v2);
		return(equalvector(v1,v2,v1min.getX()-v2min.getX(),v1min.getY()-v2min.getY()));
	}
	/** encode x position of shape on board, y position of shape on board, who moves first
	as a bit in an integer.  9 positions two moves, so we need 18 bits. The particular coding
	is arbitrary, but what was traditionally used by the Go program.
	*/
	static int Encode_Move_Geometry(Move_Order move_spec, X_Position x_pos,  Y_Position y_pos)
	{ int v = move_spec.codeValue + x_pos.codeValue + y_pos.codeValue;
	  return(1<<v);
	}

	
}
