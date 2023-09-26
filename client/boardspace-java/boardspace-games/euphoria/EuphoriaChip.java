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
package euphoria;

import lib.Image;
import lib.ImageLoader;
import euphoria.EuphoriaConstants.Colors;
import lib.G;
import lib.HitPoint;
import lib.IntObjHashtable;
import lib.OStack;
import lib.Random;
import online.game.chip;


class ChipStack extends OStack<EuphoriaChip>
{
	public EuphoriaChip[] newComponentArray(int n) { return(new EuphoriaChip[n]); }
}
/**
 * this is a specialization of {@link chip} to represent the stones used by euphoria
 * 
 * this class contains all the miscellaneous objects.
 * this is further specialized by WorkerChip RecruitChip etc.
 * @author ddyer
 *
 */

public class EuphoriaChip extends chip<EuphoriaChip> 
{
	private int index = 0;			// index of this chip
	public Colors color = null;		// player/color associated
	public String name="";
	static private IntObjHashtable<EuphoriaChip>allChipHash = new IntObjHashtable<EuphoriaChip>();
	static private IntObjHashtable<EuphoriaChip>allChipDigest = new IntObjHashtable<EuphoriaChip>();
	static private ChipStack allChips=new ChipStack();
	static private Random euphoriaChipRandom = new Random(6939626);
	static private ChipStack allChipSingles = new ChipStack();
	static private boolean imagesLoaded = false;
    static double cloudScale[] = {0.5,0.5,1.5};
    static double chipScale[] = {0.5,0.5,1.3};
    static double waterScale[] = {0.5,0.5,1.1};
    static double batteryScale[] = {0.5,0.5,0.7};
    static double minerScale[] = {0.5,0.5,1.5};
    static double stoneScale[] = {0.8,0.5,1.5};
    private boolean isResource = false;
    private boolean isCommodity = false;
    private boolean isAuthority = false;
    public boolean isWorker() { return(false); }
    public boolean isResource() { return(isResource); }
    public boolean isCommodity() { return(isCommodity); }
    public boolean isArtifact() { return(false); }
    public boolean isAuthorityMarker() { return(isAuthority); }
    public boolean isRecruit() { return(false); }
    public boolean isMarket() { return(false); }
    public EuphoriaChip getSpriteProxy() { return(this); }
    public int knowledge() { throw G.Error("shouldn't ask");}
    private EuphoriaChip subtype;
    public EuphoriaChip subtype() { return(subtype); }
    public boolean acceptsContent(EuphoriaChip ch) { return(this==ch.subtype()); }
    // constructor for subclasses to use
	public EuphoriaChip(int i,String na,double[]sc,long rve)
	{	index = i;
		scale=sc;
		image=null;
		name = file = na;
		randomv = rve;
		subtype = this;
		EuphoriaChip old = allChipHash.get(i);
		long dig = Digest();
		EuphoriaChip oldDigest = allChipDigest.get(dig);
		if(old!=null) { throw G.Error("Duplicate chip number - first was %s this is %s",old,this); }
		if(oldDigest!=null) { throw G.Error("Duplicate chip digest - first was %s this is %s",old,this); }
		allChipHash.put(i, this);
		allChipDigest.put(dig, this);
		allChips.push(this);
	}
	static public EuphoriaChip find(String n)
	{	
		for(int lim=allChips.size()-1; lim>=0; lim--)
		{
		EuphoriaChip ch = allChips.elementAt(lim);
		if(n.equalsIgnoreCase(ch.name)) { return(ch); }
		}
		return(null);
	}
	// local constructor
	private EuphoriaChip(int i,String na,double[]sc,boolean res,boolean com)
	{	this(i,na,sc,euphoriaChipRandom.nextLong());
		isResource = res;
		isCommodity = com;
		allChipSingles.push(this);
	}
	// local constructor
	private EuphoriaChip(int i,String na,double[]sc)
	{	this(i,na,sc,false,false);
	}

	
	static public EuphoriaChip getChip(int i) { return(allChipHash.get(i)); }
	
	public int chipNumber() { return(index); }
	

    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //

	// call from the viewer's preloadImages
    static public EuphoriaChip Food = new EuphoriaChip(1,"apple",chipScale,false,true);
    static public EuphoriaChip Water = new EuphoriaChip(2,"Water",waterScale,false,true);
    static public EuphoriaChip Energy = new EuphoriaChip(3,"battery",batteryScale,false,true);
    static public EuphoriaChip Bliss = new EuphoriaChip(4,"cloud",cloudScale,false,true);
    static public EuphoriaChip Gold = new EuphoriaChip(5,"Gold",chipScale,true,false);
    static public EuphoriaChip Clay = new EuphoriaChip(6,"Brick",chipScale,true,false);
    static public EuphoriaChip Stone = new EuphoriaChip(7,"Stone",stoneScale,true,false);
    static public EuphoriaChip Miner = new EuphoriaChip(8,"Miner",minerScale);
    static public EuphoriaChip AllegianceMarker = new EuphoriaChip(9,"Allegiance",chipScale);
    static public EuphoriaChip AuthorityBlocker = new EuphoriaChip(10,"AuthorityBlocker",chipScale);
    static public EuphoriaChip CloudBackground = new EuphoriaChip(11,"cloud-background",chipScale);
    
    static public EuphoriaChip Hubcap = new EuphoriaChip(12,"hubcap",chipScale);
    static public EuphoriaChip Subterran_level1 = new EuphoriaChip(13,"subterran-level1",chipScale);
    static public EuphoriaChip Subterran_level2 = new EuphoriaChip(14,"subterran-level2",chipScale);
    static public EuphoriaChip Euphorian_level1 = new EuphoriaChip(15,"euphorian-level1",chipScale);
    static public EuphoriaChip Euphorian_level2 = new EuphoriaChip(16,"euphorian-level2",chipScale);
    static public EuphoriaChip Wastelander_level1 = new EuphoriaChip(17,"wastelander-level1",chipScale);
    static public EuphoriaChip Wastelander_level2 = new EuphoriaChip(18,"wastelander-level2",chipScale);
    static public EuphoriaChip Icarite_level1 = new EuphoriaChip(19,"icarite-level1",chipScale);
    static public EuphoriaChip Icarite_level2 = new EuphoriaChip(20,"icarite-level2",chipScale);
    static public EuphoriaChip Magnifier = new EuphoriaChip(21,"magnifier",chipScale);
    static public EuphoriaChip UnMagnifier = new EuphoriaChip(22,"unmagnifier",chipScale);
     
    static public EuphoriaChip Euphorian = new EuphoriaChip(23,"euphorian",chipScale);
    static public EuphoriaChip Subterran = new EuphoriaChip(24,"subterran",chipScale);
    static public EuphoriaChip Wastelander = new EuphoriaChip(25,"wastelander",chipScale);
    static public EuphoriaChip Icarite = new EuphoriaChip(26,"icarite",chipScale); 
    static public EuphoriaChip Commodity = new EuphoriaChip(27,"commodity",chipScale);
    static public EuphoriaChip Resource = new EuphoriaChip(28,"resource",chipScale);
    static public EuphoriaChip Nocard = new EuphoriaChip(29,"nocard",chipScale);
    static public EuphoriaChip Trash = new EuphoriaChip(70,"waste",new double[]{0.5,0.4,0.8});

    static public EuphoriaChip CardMarket = new EuphoriaChip(0,"cardmarket",null);
    
    static public EuphoriaChip allegianceMedallions[] = {
       	Euphorian,
       	Subterran,
       	Wastelander,
       	Icarite,
    };
    
    //
    // this fattens up the target for "drawChip" which is only used for the magnifier
    // at the moment.
    //
    public boolean pointInsideCell(HitPoint p,int x,int y,int sqx,int sqy)
    {
    	return(super.pointInsideCell(p,x,y,sqx*2,sqy*2));
    }
    static private EuphoriaChip[] colorArray(int baseidx,String basename)
    {	Colors co[] = Colors.values();
    	EuphoriaChip val[] = new EuphoriaChip[co.length];
    	for(Colors c : co)
    	{	int idx = c.ordinal();
    		val[idx] = new EuphoriaChip(baseidx+idx,basename+"-"+c,chipScale,euphoriaChipRandom.nextLong());
     	}
    	return(val);
    }
    static EuphoriaChip Level1Markers[] = { Euphorian_level1, Subterran_level1, Wastelander_level1, Icarite_level1};
    static EuphoriaChip Level2Markers[] = { Euphorian_level2, Subterran_level2, Wastelander_level2, Icarite_level2};
    
    static EuphoriaChip MoraleMarkers[] = colorArray(30,"Morale");
    static EuphoriaChip KnowledgeMarkers[] = colorArray(40,"Knowlege");
    static EuphoriaChip AuthorityMarkers[] = colorArray(50,"Authority");
    static EuphoriaChip ArtifactMarket[] = colorArray(60,"Artifact");
      
    static public EuphoriaChip getKnowledge(Colors c) { return(KnowledgeMarkers[c.ordinal()]); }
    static public EuphoriaChip getMorale(Colors c) { return(MoraleMarkers[c.ordinal()]); }
    static public EuphoriaChip getAuthority(Colors c) { return(AuthorityMarkers[c.ordinal()]); }
	

	private static void preloadMaskGroup(ImageLoader forcan,String Dir,EuphoriaChip chips[],String mask)
	{	String names[] = new String[chips.length];
		for(int i=0;i<names.length;i++) { names[i]=chips[i].file.toLowerCase(); }
		Image IM[] = forcan.load_images(Dir, names,forcan.load_image(Dir,mask));
		for(int i=0;i<chips.length;i++) { chips[i].image = IM[i]; }
	}

  /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images (all two of them) into
     * a static array of EuphoriaChip which are used by all instances of the
     * game.
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{
		EuphoriaChip CC[] = allChipSingles.toArray();
		String names[] = new String[CC.length];
		for(int i=0;i<names.length;i++) { names[i] = CC[i].file.toLowerCase(); }
		// load the main images, their masks, and composite the mains with the masks
		// to make transparent images that are actually used.
        Image IM[]=forcan.load_masked_images(Dir,names);
		for(int i=0;i<CC.length;i++) { CC[i].image = IM[i]; }
		
		preloadMaskGroup(forcan,Dir,MoraleMarkers,"morale-mask");
		{int i=0;
		for(EuphoriaChip m : MoraleMarkers) 
			{ m.subtype = MoraleMarkers[0];
			  m.color = Colors.find(i);
			  i++;
			}}
		
		preloadMaskGroup(forcan,Dir,AuthorityMarkers,"authority-mask");
		for(int i=0;i<AuthorityMarkers.length;i++) 
			{ AuthorityMarkers[i].isAuthority = true; 
			  AuthorityMarkers[i].subtype = AuthorityMarkers[0];
			  AuthorityMarkers[i].color = Colors.find(i);
			}
		AuthorityBlocker.subtype = AuthorityMarkers[0];
		
		preloadMaskGroup(forcan,Dir,KnowledgeMarkers,"knowlege-mask");
		{int i=0;
		 for(EuphoriaChip k : KnowledgeMarkers) 
		 	{ k.subtype = KnowledgeMarkers[0]; 
		 	  k.color = Colors.find(i);
		 	  i++;
		 	}		 
		}
		imagesLoaded = true;
 		}
	}   
	static long Digest(Random r,EuphoriaChip ch)
	{	return(ch==null ? r.nextLong() : ch.Digest(r));
	}
	
}
