package santorini;

import lib.DrawableImageStack;
import lib.G;
import lib.ImageLoader;
import lib.Random;
import online.game.*;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class SantoriniChip extends chip<SantoriniChip> implements SantoriniConstants
{	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	private SantoriniChip npChip = null;
	int index = 0;
	Type type=null;
	SantorId id;
	enum Type { Man,God,Tile,Dome};
	
	private SantoriniChip(String name,SantorId idx,Type typ,double[]sc)
	{
		index = allChips.size();
		type=typ;
		id = idx;
		scale=sc;
		file = name;
		randomv = r.nextLong();
		allChips.push(this);
	}
	public int chipNumber() { return(index); }

    static final int FIRST_CHIP_INDEX = 0;
    static final SantorId godId = null;
    
    public SantoriniChip getAltChip(int set)
    {	if(set==2)
    	{	if(npChip!=null) { return(npChip); }
    	}
    	else 
    	{if((this==MainTile)&&(set>0)) { return(AltTile); }
    	}
    	return(this);
    }
    public int playerIndex()
    {
    	if(isMan()) { return((this==Cube_A) || (this==Cube_B) ? 0 : 1); }
    	return(-1);
    }
	public static SantoriniChip getChip(int color)
	{	return((SantoriniChip)allChips.elementAt(FIRST_CHIP_INDEX+color));
	}

  /* pre load images and create the canonical pieces
   * 
   */
   static public SantoriniChip MainTile = new SantoriniChip("square-b",null,Type.Tile,new double[]{0.538,0.456,1.5});
   static public SantoriniChip AltTile =  new SantoriniChip("square-a",null,Type.Tile,new double[]{0.5,0.5,1.5});;
   static public SantoriniChip Dome =  new SantoriniChip("dome",null,Type.Dome,new double[]{0.585,0.515,1.0});;
   static public SantoriniChip Cube_A =  new SantoriniChip("cube-a",null,Type.Man,new double[]{0.586,0.50,1.0});;
   static public SantoriniChip Cube_B =  new SantoriniChip("cube-b",null,Type.Man,new double[]{0.553,0.559,1.0});;
   static public SantoriniChip Cylinder_A =  new SantoriniChip("cylinder-a",null,Type.Man,new double[]{0.468,0.50,1.05});;
   static public SantoriniChip Cylinder_B =  new SantoriniChip("cylinder-b",null,Type.Man,new double[]{0.53,0.476,1.175});;

   static public SantoriniChip Tile_NP = new SantoriniChip("square-np",null,Type.Tile,new double[]{0.556,0.502,1.5});
   static public SantoriniChip Dome_NP =  new SantoriniChip("dome-np",null,Type.Dome,new double[]{0.725,0.377,1.2});
   static public SantoriniChip Cube_NP =  new SantoriniChip("cube-np",null,Type.Man,new double[]{0.636,0.408,1.379});
   static public SantoriniChip Cylinder_NP =  new SantoriniChip("cylinder-np",null,Type.Man,new double[]{0.613,0.466,1.652});;
   static public SantoriniChip LeftView = new SantoriniChip("leftview-nomask",null,null,new double[] {0.5,0.5,1});
   static public SantoriniChip RightView = new SantoriniChip("rightview-nomask",null,null,new double[] {0.5,0.5,1});
  
   static final double godScale[] = {0.5,0.5,1};
   static public SantoriniChip Apollo = new SantoriniChip("Apollo",SantorId.Apollo,Type.God,godScale);
   static public SantoriniChip Artemis = new SantoriniChip("Artemis",SantorId.Artemis,Type.God,godScale);
   static public SantoriniChip Athena = new SantoriniChip("Athena",SantorId.Athena,Type.God,godScale);
   static public SantoriniChip Atlas = new SantoriniChip("Atlas",SantorId.Atlas,Type.God,godScale);
   //static public SantoriniChip Achilles = new SantoriniChip("Achilles",SantorId.Achilles,Type.God,godScale);
   //static public SantoriniChip Demeter = new SantoriniChip("Demeter",SantorId.Demeter,Type.God,godScale);
   static public SantoriniChip Hephaestus = new SantoriniChip("Hephaestos",SantorId.Hephaestus,Type.God,godScale);
   //static public SantoriniChip Hermes = new SantoriniChip("Hermes",SantorId.Hermes,Type.God,godScale);
   //static public SantoriniChip Minotaur = new SantoriniChip("Minotaur",SantorId.Minotaur,Type.God,godScale);
   static public SantoriniChip Prometheus = new SantoriniChip("Prometheus",SantorId.Prometheus,Type.God,godScale);
   static public SantoriniChip Aphrodite = new SantoriniChip("Aphrodite",SantorId.Aphrodite,Type.God,godScale);
   static public SantoriniChip Ares = new SantoriniChip("Ares",SantorId.Ares,Type.God,godScale);
   static public SantoriniChip Dionysus = new SantoriniChip("Dionysus",SantorId.Dionysus,Type.God,godScale);

   static public SantoriniChip Cleo = new SantoriniChip("Cleo",SantorId.Cleo,Type.God,godScale);
   static public SantoriniChip Hades = new SantoriniChip("Hades",SantorId.Hades,Type.God,godScale);
   static public SantoriniChip Pan = new SantoriniChip("Pan",SantorId.Pan,Type.God,godScale);
   static public SantoriniChip Hercules = new SantoriniChip("Hercules",SantorId.Hercules,Type.God,godScale);
   //static public SantoriniChip Terpsichore = new SantoriniChip("Terpsichore",SantorId.Terpsichore,Type.God,godScale);

   public boolean isMan() { return(type==Type.Man); }
   public boolean isGod() { return(type==Type.God); }
   public boolean isTile() { return(type==Type.Tile); }
   public boolean isDome() { return(type==Type.Dome); }
   
   // heroes
   //static public SantoriniChip Medea = new SantoriniChip("Medea",SantorId.Medea,Type.God,godScale);

   //static public SantoriniChip Hecate = new SantoriniChip("Hecate",SantorId.Hecate,Type.God,godScale);
   //static public SantoriniChip Theseus = new SantoriniChip("Theseus",SantorId.Theseus,Type.God,godScale);
   static public SantoriniChip Gods[] = 
	   {
	    	// gods
	    	Aphrodite,	// start and end next to man	    	
	    	Apollo,		// modern: swap with moving man, but only on level+1
	    	Ares,		// classic: same as modern minataur  modern: remove before
	    	Artemis,	// modern: 1 additional move.  We allow moving up both times.
	    	Athena,		// prevent others from moving up

	    	Atlas,		// place domes at any level
	    	Cleo,		// classic: no follow
	    	Dionysus,	// move opponents man after building a dome
	    	Hades,		// same, can't move down
	    	Hercules,	// classic: build twice on different spaces

	    	Hephaestus,	// modern: build a second
	    	Pan,		// win if jump down 2.
	    	Prometheus,	// new only

	    	
//	    	Demeter,	// classic: 2x men  modern: 1 extra build
//	    	Achilles,	// hero classic: hero kill man  modern: build before and after
//	    	Hermes,		// classic: move twice  modern: move both on same level
//	    	Minotaur,	// only modern
//	    	//Terpsichore,	// classic: move both build 1 modern: move both build both
	    	
	    	//Medea,		// remove block once
	    	//classic Achilles,
	    	//Hecate, //too weird
	    	//Theseus,  
	    	// Posidon
   	};
   
   public static int findGodIndex(String n)
   {	
	   	for(int idx = Gods.length-1; idx>=0; idx--)
	   	{	SantoriniChip ch = Gods[idx];
	   		if(ch.id.shortName.equalsIgnoreCase(n)) { return(idx); }
	   	}
	   throw G.Error("No god named "+n);
   }
   public static int findGodIndex(SantoriniChip n)
   {	
	   	for(int idx = Gods.length-1; idx>=0; idx--)
	   	{	SantoriniChip ch = Gods[idx];
	   		if(ch==n) { return(idx); }
	   	}
	   throw G.Error("No god named "+n);
   }
   public static SantoriniChip findGod(int idx)
   {
	   return Gods[idx];
   }
   public static SantoriniChip findGod(SantorId id)
   {
	   if(id==SantorId.Godless) { return(null); }
	   for(SantoriniChip ch : Gods) { if(ch.id==id) { return(ch); }}
	   throw G.Error("Not a known god %s",id);
   }
   public static String findGodName(int idx)
   {	return(findGod(idx).id.shortName);
   }
   
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		forcan.load_images(ImageDir,Gods,forcan.load_image(ImageDir,"gods-mask"));
		imagesLoaded = forcan.load_masked_images(ImageDir,allChips);
		MainTile.npChip = AltTile.npChip = Tile_NP;
		Dome.npChip = Dome_NP;
		Cylinder_A.npChip = Cylinder_B.npChip = Cylinder_NP;
		Cube_A.npChip = Cube_B.npChip = Cube_NP; 
		}
	}
}
