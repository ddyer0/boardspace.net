package goban.shape.shape;

import com.codename1.io.Externalizable;
import com.codename1.io.Util;

import bridge.Utf8OutputStream;
import bridge.Utf8Printer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import lib.AR;
import lib.G;

/** hold the result data for one shape in one position.  If the shape has N stones,
the size of the results array will be 2^N.  Since most positions for most shapes
have the same result, we store a mask indicating which positions on the board
these results apply to.
*/
public class ResultCode implements Externalizable
{	/**
	 * 
	 */
	public ResultCode() {};		// for Externalizable
	static final long serialVersionUID = 1L;
	int positions;			//a bit set of position codes. 
	ResultProtocol results[];
	
	static int log2(int b)
	{ int i;
		for(i = 0; ((1<<(i+1))-1)<b; i++) ;
		return(i);
	}
	static String posnames[]=
	{ "lt", "ct", "rt",		//left top, center top, right top
		"lc", "cc", "rc",		//left center, center, right center
			"lb", "cb", "rb"};	//left bottom, center bottom,right bottom
    
	public static String decode_position_bit(int b)
	{
		int bitn = log2(b);
		String first = ((bitn&1)!=0)?"F":"S";	//odd positions are "Move First" even are "Move Second"
		return(first+posnames[(bitn>>1)]);
	}
	
	@SuppressWarnings("deprecation")
	public String toString()
	{   Utf8OutputStream b = new Utf8OutputStream();
    	PrintStream codes = Utf8Printer.getPrinter(b);

		int code = positions;
		while(code!=0)
		{ int bit = code&(-code);
			code=code-bit;
			codes.print(decode_position_bit(bit));
		}
		codes.flush();
		codes.close();
		return("#<" + getClass().getName() + " " + b.toString() + " >");
	}	  
	/* constructor */
	public ResultCode(int pos,ResultProtocol pr[])
	{ positions = pos;
		results = pr;
	}	

	
	/** compare two ResultCodes and summarize the differences */
	int CompareWith(ResultCode other)
	{ int difs=0;
		G.print("Compare " + this + " " + other); 
		for(int i=0;i<results.length;i++)
		{  
			if(results[i]!=other.results[i]) /* hashed, so EQ test is appropriate */
			{  G.print(i + " " + results[i] + " " + other.results[i]);
				difs++;
			}
		}
		return(difs);
	}
	/** remove the results corresponding to a bit (that is, exacltly half of them).
	This is a repair step in the process of converting old dropped shapes into the form
	in which we now use them.
	*/
	public void RemoveBit(int bitvalues)
	{//	System.out.println("remove from " + bitvalues);
		while(bitvalues!=0)
		{int len = results.length;
			int logbit = log2(bitvalues)+1;
			int bitval = (1<<(logbit-1));
			bitvalues = bitvalues-bitval;
			//	System.out.println("Remove " + bitval + " " + logbit + " rem " + bitvalues);
			ResultProtocol newres[] = new ResultProtocol[len/2];
			for(int i=0,j=0;i<len;i++) 
			{ if ((i&bitval)==0) 
				{ ResultProtocol rr = results[i]; 
					/* the ordinals are also stored with the parent shape's structure in mind */
					newres[j++] = rr.LowerOrdinals(logbit);
					//System.out.println(i + " " + results[i] + "  -> " + (j-1) + " " + newres[j-1]);
				}
			}
			results=newres;
		}}
	/** create a reordered result list, based on the same set of points appearing in a different
	order.  This requires two changes; first the results themselves must be changed so their
	"ordinal" numbers refer to the new ordinals, then the order in which they appear in the
	"intron" array have to be permuted to reflect the new point list.
	*/
	ResultCode ReOrder(LocationProvider old_points[],LocationProvider new_points[])
	{	int len = old_points.length;
		int explen = 1<<len;
		int point_reorder[] = new int[len];
		ResultProtocol result[]=new ResultProtocol[explen];
		LocationProvider OldMin = zhash.vectormin(old_points);
		LocationProvider NewMin = zhash.vectormin(new_points);
		int dx = NewMin.getX()-OldMin.getX();
		int dy = NewMin.getY()-OldMin.getY();
		//System.out.println("Reorder " + this);
		//for(int i=0;i<old_points.length; i++)	{System.out.println(old_points[i] + " " + new_points[i]);}
		
		//for(int i=0;i<results.length;i++) { System.out.println(i + " " + results[i]);}
		
		for(int i=0;i<len;i++) 
		{ LocationProvider testpt = old_points[i];
	  	  int old = zhash.IndexOf(new_points,testpt.getX()+dx,testpt.getY()+dy);
	  	  G.Assert(old>=0,"point not found");
	  	  point_reorder[i]=old; 
		}
		for(int i=0;i<explen;i++) 
		{	int new_index=0;
			for(int j=0;j<len;j++) 
			{ int ppos = point_reorder[j];	//bit j becomes bit ppos
				if(((1<<j)&i)!=0) 
				{new_index|=(1<<ppos); 
				}
			}
			result[new_index]=results[i].ReOrder(point_reorder);
		} 		
		for(int i=0;i<explen;i++) {G.Assert(result[i]!=null,"Missed copying a result");}
		//System.out.println("To " );
		//for(int i=0;i<result.length;i++) { System.out.println(i + " " + result[i]);}
		
		return(new ResultCode(positions,result));
	}
	//public static void main(String arg[])
	//{for(int i=0;i<18;i++) 
	//	{System.out.println(i + " " + decode_position_bit(1<<i));	}
	//}

	public int getVersion() {
		return((int)serialVersionUID);
	}

	public void externalize(DataOutputStream out) throws IOException {
		
		out.writeInt(positions);			//a bit set of position codes. 
		Util.writeObject(results,out);
	}

	public void internalize(int version, DataInputStream in) throws IOException {
		positions = in.readInt();
		Object v[] = (Object[])Util.readObject(in);
		if(v!=null) 
		{ 	results = new ResultProtocol[v.length];
			AR.copy(results,v);
		}
		else { results = null; }
	}

	public String getObjectId() {
		return("ResultCode");
	}
}
