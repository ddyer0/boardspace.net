package punct;
import lib.G;

// accumulate information about a connected blob of cells, as viewed from
// the top.  This includes facts like the number of pieces involved and 
// the real span of the blob, but also bits that are used heuristically
// in the static evaluator.
public class punctBlob implements PunctConstants
{	
	int nPieces;			// how many pieces in this blob
	int bloBit;				// bitmask bit assigned to this blob
	PunctColor color;		// the color we're a blob for
	PunctPiece pieces[]= new PunctPiece[NUMPIECES];	// the pieces in this blob
	// we call the three axes of symmetry "x" "y" and "z" even though they're
	// not really.  The win by connection condition is that any of these three
	// spans = 16 (ie; 17 cells)
	char xmin;	// X corresponds to the column 
	char xmax;	//
	int ymin;	// Y corresponds to the row
	int ymax;	//
	int zmin;	// Z corresponds to column-row
	int zmax;	// 
	
	// bounding box of liberties x2
	char e_xmin;	// X corresponds to the column 
	char e_xmax;	//
	int e_ymin;	// Y corresponds to the row
	int e_ymax;	//
	int e_zmin;	// Z corresponds to column-row
	int e_zmax;	// 
	
	int thickness;		// adjacent to self in sweep
	int contacts;		// contacts with the other player
	int empties;		// contacts with empty space
	int emptyCenterAdjacent;	// contacts with empty spaces in the center
	int plineCount;				// count of alignments of this punct with other pieces
	int pincers;	// contacts matched on the other side
	int halfPincers;// contacts matched by edge or center

	crossLink crossLinks=null;	// other blobs we can reach in 2 liberties
	
	// constructor from a seed piece/point
	punctBlob(PunctPiece p,int bit,char col,int row)
	{	bloBit = bit;
		e_xmax = e_xmin = xmin = xmax = col;
		e_ymax = e_ymin = ymin = ymax = row;
		e_zmax = e_zmin = zmin = zmax = col-row;
		pieces[nPieces++] = p;
		color = p.color;
	}
	
	void showLinks()
	{	crossLink cl = crossLinks;
		while(cl!=null) 
		{ System.out.println(this + " "+cl.connections+" "+cl.to);
		  cl=cl.next;
		}
	}
	
	// add a new crosslink or strengthen an existing link because
	// we found a new common empty cell.  conn_points are somewhat
	// arbitrary values, but closer connections are greater values.
	// if connection is through a center hex, it's worth less
	void addCrossLink(punctBlob to,int conn_points,boolean center)
	{	if(to!=this)
		{crossLink cl = crossLinks;
		 //System.out.println("Link "+this+" "+to+" @ "+distance);
		 while((cl!=null) && (cl.to!=to)) { cl=cl.next; }
		 if(cl==null) 
		 	{ cl = new crossLink(); 
		 	  cl.next=crossLinks;
		 	  crossLinks=cl; 
		 	  cl.to=to;
		 	}
		 // it's harder to connect through a center point because we can't drop there
		 cl.connections += conn_points * (center ? 1 : 2);
		}
	}
	
	// accumulate the bounding box associated with a liberty or secondary liberty
	void addLib(char col,int row)
	{
		e_xmin = (char)Math.min(col,e_xmin);
		e_xmax = (char)Math.max(col,e_xmax);
		e_ymin = Math.min(row,e_ymin);
		e_ymax = Math.max(row,e_ymax);
		int newz = col-row;
		e_zmin = Math.min(e_zmin,newz);
		e_zmax = Math.max(e_zmax,newz);
	
	}
	// add a piece to the blob, maintain the bounding boxes
	void addPiece(punctCell c,PunctPiece p,char col,int row)
	{	G.Assert(p.color==color,"color matches");
		{
		addLib(col,row);
		xmin = (char)Math.min(col,xmin);
		xmax = (char)Math.max(col,xmax);
		ymin = Math.min(row,ymin);
		ymax = Math.max(row,ymax);
		int newz = col-row;
		zmin = Math.min(zmin,newz);
		zmax = Math.max(zmax,newz);
		for(int i=0;i<nPieces;i++) { if(pieces[i]==p) { return; }}
		pieces[nPieces++] = p;
		int ord = p.color.ordinal();
		plineCount += c.plines[ord];
		plineCount -= c.plines[ord^1];
		}
	}
	
	// return the "hard" span of this blob
	public int maxSpan()
	{return(Math.max(Math.max(xmax-xmin,ymax-ymin),zmax-zmin));
	}
	
	// return the "sofy" span of this blob, including connections
	// and extensions into empty cells.  This is the central
	// heuristic in the static evaluator
	//
	public double extendedSpan(int pc)
	{	crossLink cl = crossLinks;
		double xtra = 0;
		int realSpan = Math.max(Math.max(e_xmax-e_xmin,e_ymax-e_ymin),e_zmax-e_zmin);
		int cmax = realSpan;
		int xxmax = e_xmax;
		int xxmin = e_xmin;
		int xymin = e_ymin;
		int xymax = e_ymax;
		int xzmin = e_zmin;
		int xzmax = e_zmax;
		while(cl!=null)
		{	punctBlob to = cl.to;
			xxmin = Math.min(xxmin,to.e_xmin);
			xxmax = Math.max(xxmax,to.e_xmax);
			xymin = Math.min(xymin,to.e_ymin);
			xymax = Math.max(xymax,to.e_ymax);
			xzmin = Math.min(xzmin,to.e_zmin);
			xzmax = Math.max(xzmax,to.e_zmax);	// extend the bounding box
			int xspan = Math.max(Math.max(xxmax-xxmin,xymax-xymin),xzmax-xzmin);
			int extra = (xspan-cmax);
			if(extra<=cmax)
			{
			double prob = Math.min(1.0,((double)cl.connections/pc));	// more than 30 connection points is a lock
			if(extra==cmax) { prob=prob/2; }	// count both, but at half value
			xtra += prob * extra;
			cmax = xspan;
			}
			cl = cl.next;
		}
		return(realSpan+xtra);
	}
	// true if this blob is a win
	public boolean winForColor(PunctColor c)
	{	return((color==c)
			&& ( ((xmax-xmin)==WINNING_SPAN)
					|| ((ymax-ymin)==WINNING_SPAN)
					|| ((zmax-zmin)==WINNING_SPAN)));
	}
	public String toString()
	{	return("<blob "+color+" "+nPieces+" span="+maxSpan()+" xspan="+extendedSpan(40)+" "+xmin+"-"+xmax+" "+ymin+"-"+ymax+" "+zmin+"-"+zmax+">");
	}
}
