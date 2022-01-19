package carnac;

import lib.Image;
import lib.ImageLoader;
import lib.G;
import lib.OStack;
import lib.Random;
import online.game.chip;

class ChipStack extends OStack<CarnacChip>
{
	public CarnacChip[] newComponentArray(int n) { return(new CarnacChip[n]); }
}
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class CarnacChip extends chip<CarnacChip> 
{	
	enum FaceColor { Red("R"), White("B");
		String shortName;
		FaceColor(String shortm)
		{
		shortName = shortm;
		}
		public FaceColor oppositeColor()
			{ return((this==Red)?White:Red); 
			};
		}
	enum FaceOrientation { Up, Horizontal, Vertical };
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static ChipStack allChips = new ChipStack();
	private static boolean imagesLoaded = false;
    static private CarnacChip CANONICAL_PIECE[] = null;	// created by preload_images

	private int menhirIndex;
	private String name = "";
	FaceColor topColor;
	FaceColor frontColor;
	FaceColor sideColor;
	private FaceOrientation orientation;
	CarnacChip alt = null;
	public double[] altScale()
	{
		double altScale[] = new double[3];
		for(int i=0,j=scale.length-3; i<3;i++,j++)
			{
			altScale[i] = scale[j];
			}
		return(altScale);
	}
	public CarnacChip getAltChip(int cset)
	{
		if((alt!=null) && (cset==1)) { return(alt); }
		return(this);
	}
	private CarnacChip(String na)
	{	menhirIndex = -1;
		file = name = na;
		allChips.push(this);
	}
	private CarnacChip(String na,Image im,double[]scl,CarnacChip c)
	{
		menhirIndex = -1;
		file = name = na;
		image = im;
		alt = c;
		scale = scl;
	}
	private CarnacChip(String na,int pla,double scl[],
			FaceColor front,FaceColor top,FaceColor side,
			FaceOrientation or)
	{	file = name = na;
		menhirIndex=pla;
		randomv = r.nextLong();
		scale = scl;
		orientation = or;
		topColor = top;
		frontColor = front;
		sideColor = side;
		allChips.push(this);
	}

static CarnacChip chips[] = {
		new CarnacChip("c-up-white-white-red",0,
				new double[] {0.692,0.686,1.66, 0.709,0.273,1.660},
		FaceColor.White,FaceColor.White,FaceColor.Red,FaceOrientation.Up),	
										
		new CarnacChip("c-up-white-red-red",1,
				new double[]{0.708,0.666,1.66, 0.712,0.248,1.660},
		FaceColor.White, FaceColor.Red, FaceColor.Red,FaceOrientation.Up),	

		new CarnacChip("c-up-red-white-white",2,
				new double[]{0.705,0.679,1.686, 0.699,0.333,1.660},
		FaceColor.Red, FaceColor.White, FaceColor.White,FaceOrientation.Up),									
										
		new CarnacChip("c-up-red-red-white",3,
				new double[]{0.691,0.685,1.66, 0.658,0.316,1.660},
		FaceColor.Red, FaceColor.Red, FaceColor.White,FaceOrientation.Up),	
		


		new CarnacChip("c-h-red-white-red",4,
				new double[]{0.461,0.567,1.88, 0.437,0.307,1.879},
		FaceColor.Red, FaceColor.White, FaceColor.Red,FaceOrientation.Horizontal),	

		new CarnacChip("c-h-red-white-white",5,
				new double[]{0.503,0.529,1.803, 0.512,0.256,1.879},
		FaceColor.Red,FaceColor.White,FaceColor.White,FaceOrientation.Horizontal),							
										
		new CarnacChip("c-h-white-red-white",6,
				new double[]{0.477,0.529,1.88, 0.521,0.290,1.948},
		FaceColor.White, FaceColor.Red, FaceColor.White,FaceOrientation.Horizontal),	
										
		new CarnacChip("c-h-white-red-red",7,
				new double[]{0.496,0.470,1.88, 0.521,0.290,1.948},
		FaceColor.White, FaceColor.Red, FaceColor.Red,FaceOrientation.Horizontal),	

		
		new CarnacChip("c-vertical-white-white-red",8,
				new double[]{0.620,0.718,1.529,  0.589,0.521,1.743},
		FaceColor.White,FaceColor.White,FaceColor.Red,FaceOrientation.Vertical),	
										
		new CarnacChip("c-vertical-red-white-red",9,
				new double[]{0.645,0.763,1.588,  0.589,0.521,1.743},
		FaceColor.Red, FaceColor.White, FaceColor.Red,FaceOrientation.Vertical),	
										
		new CarnacChip("c-vertical-white-red-white",10,
				new double[]{0.627,0.729,1.588,	0.632,0.504,1.703},
		FaceColor.White, FaceColor.Red, FaceColor.White,FaceOrientation.Vertical),	
										
		new CarnacChip("c-vertical-red-red-white",11,
				new double []{0.635,0.728,1.703,	0.632,0.504,1.703},
		FaceColor.Red, FaceColor.Red, FaceColor.White,FaceOrientation.Vertical),	
										
	};


	
	CarnacChip tipped[] = new CarnacChip[4];
	public int chipNumber() { return(menhirIndex); }
	public String shortName() { return((topColor!=null) ? topColor.shortName+frontColor.shortName+sideColor.shortName : ""); }
	public FaceColor getTopColor() { return(topColor); }
	public FaceColor getFrontColor() { return(frontColor); }
	public FaceColor getSideColor() { return(sideColor); }
	public FaceOrientation getFaceOrientation() { return(orientation); }
	

	public String toString()
	{	return("<"+ name+" #"+menhirIndex+orientation+shortName()+">");
	}
	public String contentsString() 
	{ return(name); 
	}
	static public CarnacChip findChip(FaceOrientation or,FaceColor top,FaceColor side,FaceColor front)
	{	for(CarnacChip ch : CANONICAL_PIECE)
		{	if((ch.orientation==or)
				&& (ch.topColor==top)
				&& (ch.sideColor==side)
				&& (ch.frontColor==front)) { return(ch); }
		}
		throw G.Error("No chip found");
	}
	
	public static CarnacChip board =  new CarnacChip("board");
	public static CarnacChip board_medium =  new CarnacChip("board-medium");
	
	public static CarnacChip backgroundTile =  new CarnacChip("background-tile-nomask");
	public static CarnacChip backgroundReviewTile =  new CarnacChip("background-review-tile-nomask");
	public static CarnacChip redSymbol =  new CarnacChip("red-symbol-nomask");
	public static CarnacChip blackSymbol =  new CarnacChip("black-symbol-nomask");
			

	public static CarnacChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}


	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	
		if(!imagesLoaded)
		{
		forcan.load_masked_images(ImageDir,allChips);
		CANONICAL_PIECE = new CarnacChip[12];
		String imNa[] = new String[12];
		String na[] = new String[12];
		for(int i=0;i<allChips.size();i++) 
			{ CarnacChip c = allChips.elementAt(i); 
			  int index = c.menhirIndex;
			if(index>=0) 
				{ CANONICAL_PIECE[index] = c;
				  imNa[index] = c.file;
				  na[index] = c.file + "-alt-mask";
				}
			}
		// create an alternate chipset with different masks.  Chipset 1 
		// consists of just the tops of all the chips
		Image masks[] = forcan.load_images(ImageDir,na);
		Image im[] = forcan.load_images(ImageDir,imNa,masks);
		for(int i=0;i<12;i++)
		{	CarnacChip cp = CANONICAL_PIECE[i];
			cp.alt = new CarnacChip(imNa[i],im[i],cp.altScale(),cp);
		}
		}
		imagesLoaded = true;
	}


}
