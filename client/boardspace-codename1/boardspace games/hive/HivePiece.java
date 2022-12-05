package hive;

import com.codename1.ui.geom.Rectangle;

import lib.Graphics;
import lib.Image;
import lib.ImageLoader;

import java.util.Hashtable;

import hive.HiveConstants.HiveId;
import hive.HiveConstants.PieceType;
import lib.G;
import lib.OStack;
import lib.Random;
import online.common.exCanvas;
import online.game.chip;

class ChipStack extends OStack<HivePiece>
{
	public HivePiece[] newComponentArray(int n) { return(new HivePiece[n]); }
}
public class HivePiece extends chip<HivePiece> 
{	
	// this corresponds to the standard artwork loaded for both colors and both tile sets
	static public PieceType StartingPieceTypes[]= {PieceType.QUEEN,
    	PieceType.ANT,PieceType.ANT,PieceType.ANT,
    	PieceType.GRASSHOPPER,PieceType.GRASSHOPPER,PieceType.GRASSHOPPER,
    	PieceType.BEETLE,PieceType.BEETLE,
    	PieceType.SPIDER,PieceType.SPIDER,
    	PieceType.MOSQUITO,PieceType.LADYBUG,PieceType.PILLBUG,PieceType.BLANK
    	};
	// this corresponds to the standard artwork loaded for both colors and both tile sets
	static public PieceType SetupPieceTypes[]=
    	{PieceType.ANT, 	PieceType.GRASSHOPPER,
    	PieceType.BEETLE,
    	PieceType.SPIDER,
    	PieceType.MOSQUITO,PieceType.LADYBUG,PieceType.PILLBUG,PieceType.BLANK
    	};
	static public int StartingPieceSeq[]={-1,1,2,3,	// ants
			1,2,3,	// grasshoppers
		1,2,	// beetles
		1,2,	// spiders
		-1,-1,-1,	// mosquito, ladybug,pillbug
		-1		// blank
	};
	static final int NUMPIECETYPES=PieceType.values().length;		// 8 including pillbug   
	
	public HiveId color=null;							// owning player
	public boolean potentiallyUnique = false;		// true if the starting set has just one of these
	public PieceType type;							// the underlying type for this piece
	private int seq=0;								// sequenceNumber for this bug - ie  Ant 0 1 or 2...
	private String bugShortName = null;				// the bug name without player
	private String bugExactName = null;				// the bug name including player
	private HivePiece altChip = null; 				// alternate chip in the carbon chipset, used only for display
	private int mySet = 0;							// 0 for the classic tiles, 1 for the carbon tiles
	private Image topImage = null;					// separate image for the top of the piece
	private double[] topImageScale = {0.0,0.0,1.0};
	public boolean isQueen() { return(PieceType.QUEEN==type); }
	public String standardBugName() 
	{ if(bugShortName==null) 
		{	bugShortName = type.shortName + ((seq<=0)?"":seq); 
		}
		return(bugShortName);
	}
	public String exactBugName() 
		{ if(bugExactName==null) 
			{	bugExactName = ((color==HiveId.White_Bug_Pool)?"w":"b") + standardBugName();
			}
		  return(bugExactName);
		}

	public void drawChipImage(Graphics gc,exCanvas canvas,int cx,int cy,double scale[],int squaresize,double xscale,double yscale,String label,boolean center)
	{	Rectangle rr = canvas.drawImage(gc, getImage(canvas.loader), scale, cx,cy,squaresize,xscale,yscale,topImage==null?label:null,center);
		//G.frameRect(gc,Color.red,rr);
		if((topImage!=null)&&(rr!=null))
		{	canvas.adjustScales2(topImageScale,((mySet==0)?this:altChip));
			canvas.drawImage(gc, topImage, topImageScale,
			G.centerX(rr),
			G.centerY(rr),
			G.Width(rr),
			1.0,yscale,label,center);
		}
	}
	public int pieceDigestClass() { return((((type.ordinal()+1)*6)+seq)*2
			+((color==HiveId.White_Bug_Pool)?0:1)); }

	public HivePiece(int set,PieceType ptype,HiveId pl,int seqn,double []scal,Image im)
	  {	
	  	scale = scal;
	  	image = im;
	  	mySet = set;
	  	type = ptype;
	  	color = pl;
	  	seq = seqn;
	  	Random r = new Random(pieceDigestClass());
	  	randomv = r.nextLong()+r.nextLong();
	  }
	  
	  // get the tile from the other set that matches this one
	  public HivePiece getAltChip(int chipset)
		{	return((chipset==0)?this:altChip);
		}

	  public String toString() 
	  { return("<" + exactBugName()+">");
	  }
	  public String contentsString() 
		{ return(exactBugName()); 
		}
	  public boolean isPillbug()
	  {
		  return((type==PieceType.PILLBUG)||(type==PieceType.ORIGINAL_PILLBUG));
	  }
	  public static boolean isPillbug(HivePiece p)
	    {	return((p==null) 
	    		? false 
	    		: p.isPillbug());
	    }
	    static final double SCALES[][] = 
	    { { 0.5130,0.46086,0.45217},	// white queen
	       	{ 0.5,0.47692,0.4235},	// white ant1
	       	{ 0.5,0.5,0.40},	// white grasshopper1
	    	{ 0.5,0.5,0.40},	// white beetle1
	    	{0.50,0.48, 0.43},	// white spider1
	    	{0.5,0.5,0.428},	// white mosquito
	    	{ 0.481,0.5,0.521739},	// white ladybug
	       	{0.5,0.5,0.4608},	// white pillbug
	       	{0.5,0.5,0.4608},	// white blank
	    	
	    	{0.532,0.503, 0.437037},	// black queen
	       	{ 0.53,0.47,0.41739},	// black ant1
	    	{ 0.5,0.5,0.40869},	// black grasshopper1
	       	{ 0.5,0.5,0.4},	// black beetle1
	       	{0.5,0.5,0.4},	// black spider1
	      	{ 0.5,0.5,0.4235294},	// black mosquito
	      	{  0.513043,0.52173, 0.50434},	// black ladybug
	       	{ 0.5,0.5,0.47},	// black pillbug
	       	{ 0.5,0.5,0.47},	// black blank

	    };
	    
	    static final double CARBON_SCALES[][] = 
	    { { 0.496,0.52,0.44},	// white queen
	       	{ 0.5,0.5,0.408},	// white ant1
	       	{ 0.5,0.5,0.424},	// white grasshopper1
	    	{ 0.5,0.5,0.426086},	// white beetle1
	    	{0.528,0.5,0.44},	// white spider1
	    	{0.56, 0.496,0.432},	// white mosquito
	    	{ 0.5,0.5,0.44},	// white ladybug
	       	{0.504,0.5, 0.448},	// white pillbug
	       	{0.504,0.5, 0.448},	// white blank

	    	
	    	{0.45925,0.55555,0.451851},	// black queen
	       	{ 0.5,0.5,0.41739},	// black ant1
	    	{ 0.5,0.5,0.40869},	// black grasshopper1
	       	{ 0.528, 0.50769,0.42307},	// black beetle1
	       	{0.474074074,0.5,0.448},	// black spider1
	      	{ 0.5,0.5,0.434782},	// black mosquito
	      	{  0.568,0.52173,  0.45},	// black ladybug
	       	{ 0.5,0.5,0.452173},	// black pillbug
	       	{ 0.5,0.5,0.452173},	// black blank

	    };
 
	    static final String[] ImageFileNames = 
	        {
	        "white-queen",
	        "white-ant-base",
	        "white-hopper-base",
	        "white-beetle-base",
	        "white-spider-base",
	        "white-mosquito-base",
	        "white-ladybug-base",
	        "white-pillbug-base",
	        "white-blank-base",
	        "black-queen",
	        "black-ant-base",
	        "black-hopper-base",
	        "black-beetle-base",
	        "black-spider-base",
	        "black-mosquito-base",
	        "black-ladybug-base",
	        "black-pillbug-base",
	        "black-blank-base"
	        };
	    
	    static final String[] TopImageFileNames =
	    		{ "white-ant-top",
	    		"white-hopper-top",
	    		"white-beetle-top",
	    		"white-spider-top",
	    		"white-mosquito-top",
	    		"white-ladybug-top",
	    		"white-pillbug-top",
	    		"blank-top",
	    		"black-ant-top",
	    		"black-hopper-top",
	    		"black-beetle-top",
	    		"black-spider-top",
	    		"black-mosquito-top",
	    		"black-ladybug-top",
	    		"black-pillbug-top",
	    		"blank-top"
	    		};
	    static final double[][]TopImageScales = {
	    	{0.451,0.682,0.560},	// white ant top
	    	{0.451,0.682,0.585},	// white hopper top
	    	{0.451,0.682,0.585},	// white beetle top
	    	{0.401,0.638,0.632},	// white spider
	    	{0.453,0.622,0.568},	// white mosquito
	    	{0.506,0.609,0.481},	// white ladybug
	    	{0.469,0.640,0.518},	// white pillbug
	    	{0.506,0.677,0.481},	// white blank
	    	{0.401,0.677,0.546},	// black ant
	    	{0.467,0.695,0.572},	// black grasshopper
	    	{0.503,0.627,0.620},	// black beetle
	    	{0.508,0.637,0.593},	// black spider
	    	{0.459,0.683,0.530},	// black mosquito
	    	{0.497,0.590,0.424},	// black ladybug
	    	{0.443,0.651,0.521},	// black pillbug
	    	{0.443,0.710,0.421}		// black blank
	    	};
	    
	    static final String[] CarbonImageFileNames = 
	    {
	    "cwhite-queen",
	    "cwhite-ant-base",
	    "cwhite-hopper-base",
	    "cwhite-beetle-base",
	    "cwhite-spider-base",
	    "cwhite-mosquito-base",
	    "cwhite-ladybug-base",
	    "cwhite-pillbug-base",
	    "cwhite-blank-base",
	    "cblack-queen",
	    "cblack-ant-base",
	    "cblack-hopper-base",
	    "cblack-beetle-base",
	    "cblack-spider-base",
	    "cblack-mosquito-base",
	    "cblack-ladybug-base",
	    "cblack-pillbug-base",
	    "cblack-blank-base"
	    };  
	    static final String[] TopCarbonFileNames = 
	    		{"cwhite-ant-top",
	    		"cwhite-hopper-top",
	    		"cwhite-beetle-top",
	    		"cwhite-spider-top",
	    		"cwhite-mosquito-top",
	    		"cwhite-ladybug-top",
	    		"cwhite-pillbug-top",
	    		"cblank-top",
	    		"cblack-ant-top",
	    		"cblack-hopper-top",
	    		"cblack-beetle-top",
	    		"cblack-spider-top",
	    		"cblack-mosquito-top",
	    		"cblack-ladybug-top",
	    		"cblack-pillbug-top",
	    		"cblank-top"

	    	};
	    static final double[][]TopCarbonScales = 
	    	{{0.496,0.677,0.517},	// carbon white ant
	    	{0.496,0.677,0.517},	// carbon white grasshopper
	    	{0.513,0.671,0.605},	// carbon white beetle
	    	{0.432,0.619,0.526},	// carbon white spider
	    	{0.433,0.693,0.550},	// carbon white mosquito
	    	{0.415,0.639,0.530},	// carbon white ladybug
	    	{0.404,0.715,0.524},	// carbon white pillbug
	    	{0.424,0.734,0.481},	// carbon white blank
	    	{0.524,0.677,0.451},	// carbon black ant
	    	{0.463,0.640,0.546},	// carbon black grasshopper
	    	{0.427,0.627,0.546},	// carbon black beetle
	    	{0.491,0.644,0.546},	// carbon black spider
	    	{0.426,0.710,0.491},	// carbon black mosquito
	    	{0.426,0.645,0.491},	// carbon black ladybug
	    	{0.450,0.690,0.491},	// carbon black pillbug
	    	{0.404,0.715,0.524},	// carbon black blank
	    	};
	    
	static private HivePiece makepiece(ChipStack stack,HiveId pl,int set,PieceType typ,int seq,double scal[],Image im)
	{	// create a piece and place it in the rack.  Create cells for the rack.
		HivePiece p =new HivePiece(set,typ,pl,seq,scal,im);
		stack.push(p);
		return(p);
	}
	
	private static Hashtable<String,HivePiece>hiveBugs=new Hashtable<String,HivePiece>();
	
	// for use by the parser when the bug is identified by player
	static public HivePiece getBug(String name)
	{	HivePiece p = hiveBugs.get(name.toLowerCase());
		if(p==null) {throw G.Error("No bug named %s",name); }
		return(p);
	}
	
	// for use by the parser when the bug may or may not be identified by the player
	static public HivePiece getBug(String source,String name)
	{	HivePiece p = hiveBugs.get(name.toLowerCase());
		if(p==null) { p=getBug(source+name); }
		return(p);
	}

	static public HivePiece getCanonicalChip(HiveId pl,PieceType pieceType)
	{	return(getCanonicalChip(hivePieces[(pl==HiveId.White_Bug_Pool)?0:1],pieceType));
	}
	static public HivePiece getCanonicalChip(ChipStack pieces,PieceType pieceType)
	{	
		for(int i=0,lim=pieces.size(); i<lim; i++)
		{	HivePiece p = pieces.elementAt(i);
			if(p.type==pieceType) { return(p); }
		}

		return(null);
	}
	static private HivePiece makeNewChip(ChipStack stack,PieceType pt,int seq)
	{	HivePiece p = getCanonicalChip(stack,pt);	// get a sample
		if(p==null) { return(null); }
		HivePiece newp = makepiece(stack,p.color,p.mySet,pt,seq,p.scale,p.image);
		if(p.topImage!=null)
		{	double ang = -((Math.PI*2)/6)*(seq-1);	//2pi/6 is 1/6 rotation
			newp.topImage = p.topImage.rotate(ang,0x0);
			newp.topImageScale = p.topImageScale;
		}
		return(newp);
	}
	static private HivePiece getNewChip(HiveId pl,PieceType pt,int seq)
	{	int index = (pl==HiveId.White_Bug_Pool)?0:1;
		HivePiece p0 = makeNewChip(hivePieces[index],pt,seq);
		
		if(p0!=null)
			{ HivePiece p1 = makeNewChip(carbonPieces[index],pt,seq);
			  hiveBugs.put(p0.exactBugName().toLowerCase(),p0);
			  p0.altChip = p1;
			  p1.altChip = p0;
			}		
		return(p0);
	}

	static private ChipStack hivePieces[] = null;
	static private ChipStack carbonPieces[] = null;

	// find the piece owned by player with typ and sequence
	static HivePiece findPiece(HiveId player,PieceType typ,int idx,boolean create)
	{	OStack<HivePiece>stack = hivePieces[player==HiveId.White_Bug_Pool?0:1];
		if((idx==-1) && (typ!=PieceType.QUEEN)) { idx = 1; }
		for(int i=0,lim=stack.size(); i<lim;i++)
		{	HivePiece p = stack.elementAt(i);
			if((p.type == typ) && (p.seq == idx)) { return(p); }
		}
		if(create) 
			{ return(getNewChip(player,typ,idx)); 
			}
		return(null);
	}

	public static ChipStack[] loadImageSet(String ImageDir,ImageLoader forcan,int set,String []names,double [][]scales,String topNames[],double[][]topscales)
	{	
		ChipStack pieces[] = new ChipStack[2];

		Image images[] = forcan.load_masked_images(ImageDir, names); // load the main images
		Image topImages[] = forcan.load_masked_images(ImageDir, topNames);	// and the top images
		int topImageIndex = 0;
		int baseImageIndex= 0;
		PieceType prev_type = StartingPieceTypes[0];
        // make the canonical pieces
        boolean isBase = false;
        for(int pl=0; pl<=1;pl++)
        { ChipStack whitePieces = new ChipStack();
          pieces[pl] = whitePieces;
        	for(int aidx=0;aidx<StartingPieceTypes.length;aidx++)
        	{int seq = StartingPieceSeq[aidx];
        	 PieceType type = StartingPieceTypes[aidx];
        	 int e_seq = ((seq<0) && (type!=PieceType.QUEEN)) ? 1 : seq; 
        	 if(type!=prev_type)
        	 {	baseImageIndex++;
        	 	prev_type = type;
        	 	if(isBase) { topImageIndex++; }
        	 }
        	 // seq -1 flags a piece which is unique and gets a shorter name, but except for the queen
        	 // we actually use 1 as the sequence number for most purposes.
        	 HivePiece p = makepiece(whitePieces,(pl==0)?HiveId.White_Bug_Pool : HiveId.Black_Bug_Pool,set,type,e_seq,scales[baseImageIndex],images[baseImageIndex]);
        	 if(names[baseImageIndex].endsWith("-base"))
        	 {	p.topImageScale = topscales[topImageIndex];
        	 	p.topImage = topImages[topImageIndex].rotate(-Math.PI*2/6*(e_seq-1),0x0); 
        	 	isBase = true;
        	 }
        	 else { isBase = false; }
        	 p.potentiallyUnique = e_seq!=seq;	// flag pieces which may or may not exist in multiples
        	 if(set==0) 
        	 	{ String bugName = p.exactBugName().toLowerCase();
        	 	  hiveBugs.put(bugName,p);						// save the name for lookup by the move parser
        	 	  if(seq==-1)
        	 	  	{ // for pieces other than the queen which have only one instance in the standard set,
        	 		  // we want to refer to them as either "wM" or "wM1", so add this duplicate name
        	 		  hiveBugs.put(bugName.substring(0,2),p); 
        	 	  	}
        	 	}
        	}
        }
        return(pieces);
	}
	
	// match up the altChip for carbon and standard pieces from the starting sets
	static private void matchPairs(OStack<HivePiece>a,OStack<HivePiece>b)
	{	for(int i=a.size()-1; i>=0; i--)
		{	HivePiece p1 = a.elementAt(i);
			HivePiece p2 = b.elementAt(i);
			p1.altChip = p2;
			p2.altChip = p1;
		}
	}

	static Image gameIcon = null;
	static HivePiece WhiteQueen = null;
	static HivePiece BlackQueen = null;
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(gameIcon==null	)
		{
        hivePieces = loadImageSet(Dir,forcan,0,ImageFileNames,SCALES,TopImageFileNames,TopImageScales);
        carbonPieces = loadImageSet(Dir,forcan,1,CarbonImageFileNames,CARBON_SCALES,TopCarbonFileNames,TopCarbonScales);
        WhiteQueen = getCanonicalChip(HiveId.White_Bug_Pool,PieceType.QUEEN);
        BlackQueen = getCanonicalChip(HiveId.Black_Bug_Pool,PieceType.QUEEN);
        for(int i=0;i<=1;i++)
        	{ matchPairs(hivePieces[i],carbonPieces[i]);
        	}
		Image im[] = forcan.load_masked_images(Dir,new String[] {"hive-icon"});
		gameIcon = im[0];
        }

	}   
}
