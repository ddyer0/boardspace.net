package medina;
import lib.Image;
import lib.ImageLoader;
import lib.G;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class MedinaChip extends chip<MedinaChip>
{	
	
	public MedinaChip getAltChip(int chipset)
	{	if((chipset==1) && isBoardComponent())
		{	return(CANONICAL_PIECE[chipIndex+alt_chipset_index]);
		}
		return(this);
	}
	
	private int chipIndex;
	public int chipNumber() { return(chipIndex); }

    static final int N_STANDARD_CHIPS = 2;
    static final int FIRST_CHIP_INDEX = 0;

	private MedinaChip(String na,int pla,Image im,long rv,double scl[])
	{	
		file = na;
		chipIndex=pla;
		image = im;
		randomv = rv;
		scale = scl;
	}
	public String toString()
	{	return("<"+ file+" #"+chipIndex+">");
	}
	
		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static public MedinaChip CANONICAL_PIECE[] = null;	// created by preload_images
    static double SCALES[][] =
    {	{0.641,0.592,1.54}		// gray palace
    	,{0.728,0.690,1.43}		// black palace
   		,{0.760,0.635,1.54}		// brown palace
   		,{0.760,0.646,1.51}		// orange palace
   		,{0.567,0.666,1.150}	// blue dome
    	,{0.567,0.666,1.187}	// green dome
    	,{0.567,0.666,1.187}	// red dome
    	,{0.567,0.666,1.187}	// yellow dome
       	,{0.55,0.672,1.26}		// stable
       	,{0.652,0.701,1.321}// tower
       	,{0.666,0.548,1.358}// wall-h
       	
       	,{0.613,0.521,1.329}// wall-v
       	,{0.755,0.677,1.5}		// meeple-h
       	,{0.632,0.561,1.03}		// well
       	,{0.73,0.621,1.03}		// tea
       	,{0.73,0.621,1.03}		// tea back
       	,{0.73,0.621,1.03}		// waste
        	
       	,{0.5,0.5,1.0}		// tower card
      	,{0.5,0.5,1.0}		// tower card
      	,{0.5,0.5,1.0}		// tower card
      	,{0.5,0.5,1.0}		// tower card
      	,{0.5,0.5,1.0}		// palace card
      	,{0.5,0.5,1.0}		// palace card
      	,{0.5,0.5,1.0}		// palace card
      	,{0.5,0.5,1.0}		// palace card
      	
      	,{0.73,0.592,1.33}		// gray palace
    	,{0.73,0.690,1.33}		// black palace
   		,{0.730,0.635,1.33}		// brown palace
   		,{0.730,0.646,1.33}		// orange palace
   		,{0.636,0.42,1.102}		// blue dome
    	,{0.636,0.42,1.102}		// green dome
    	,{0.636,0.42,1.102}		// red dome
    	,{0.636,0.42,1.102}		// yellow dome
       	,{0.659,0.579,1.2}		// stable
       	,{0.692,0.701,1.221}	// tower
       	,{0.666,0.69,1.09}		// wall-h
       	,{0.73,0.621,1.03}		// wall-v
       	,{0.755,0.677,1.3}		// meeple-h
       	,{0.584,0.534,1.3}		// well-top
     	

    };
    public static final int palace_index = 0;
    public static final int dome_index = 4;
    public static final int stable_index = 8;
    public static final int tower_index = 9;
    public static final int wall_index = 10;
    public static final int meeple_index = 12;
    public static final int well_index = 13;
    public static final int tea_index = 14;
    public static final int tea_back_index = 15;
    public static final int waste_index = 16;
    
    public static final int tower_card_index = 17;
    public static final int palace_card_index = 21;
    public static final int alt_chipset_index = 25;
    public static MedinaChip PURPLE_PALACE = null;			// the dome with the tea tiles
    public static MedinaChip getDome(int n) { return(CANONICAL_PIECE[dome_index+n]); }
    public boolean isDome() { return((chipIndex>=dome_index) && (chipIndex<(dome_index+4))); }

    public static MedinaChip getPalace(int n) { return(CANONICAL_PIECE[palace_index+n]); }
    
    public enum PalaceColor { Grey,Black,Brown,Orange }
    public boolean isPalace() { return((chipIndex>=palace_index)&&(chipIndex<palace_index+4)); }
    public PalaceColor palaceColor() { G.Assert(isPalace(),"isn't a palace"); return(PalaceColor.values()[chipIndex-palace_index]); }
    
    enum DomeColor { Blue,Green,Red,Yellow};
    public DomeColor domeColor() { G.Assert(isDome(), "isn't a dome"); return(DomeColor.values()[chipIndex-dome_index]); }
    public boolean isPalaceComponent() 	// palace or stable
    	{	return((chipIndex==stable_index)
    				|| ((chipIndex>=palace_index)&&(chipIndex<palace_index+4)));
    	}
    public static MedinaChip STABLE = null;
    public boolean isStable() { return(chipIndex==stable_index); }
    public static MedinaChip TOWER = null;
    public boolean isTower() { return(chipIndex==tower_index); }
    public static MedinaChip H_WALL = null;
    public static MedinaChip V_WALL = null;
    public boolean isWall() { return((chipIndex>=wall_index)&&(chipIndex<(wall_index+2))); }
    public static MedinaChip MEEPLE = null;
    public boolean isMeeple() { return(chipIndex==meeple_index);}
    public static MedinaChip WELL = null;
    public boolean isWell() { return(chipIndex==well_index); }
    public static MedinaChip TEA = null;
    public boolean isTea() { return(chipIndex==tea_index); }
    public static MedinaChip TEA_BACK = null;
    public boolean isTeaBack() { return(chipIndex==tea_back_index); }
    public static MedinaChip WASTE = null;
    
    public static MedinaChip getPalaceCard(int i) { return(getChip(palace_card_index+i)); }
    public static MedinaChip getTowerCard(int i) { return(getChip(tower_card_index+i)); }
    public boolean isBoardComponent() 
    	{ return((chipIndex>=palace_index)&&(chipIndex<=meeple_index));
    	}
 	public static MedinaChip getChip(int color)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}
 	
  /* pre load images and create the canonical pieces
   * 
   */
    static final String[] ImageNames = 
       {
	   // palaces are in order of their value as palace claim tiles
	   "gray-palace","black-palace","brown-palace","orange-palace"
	   ,"blue-dome","green-dome","red-dome","yellow-dome"
	   ,"stable","tower"
	   ,"wall-h","wall-v"
	   ,"meeple-h"
	   ,"well"
	   ,"tea","tea-back","waste"
	   ,"tower-1","tower-2","tower-3","tower-4"
	   // palaces claim tiles are in order 1 point to 4 points
	   ,"palace-gray","palace-black","palace-brown","palace-orange"
	   // alternate tiles
	   ,"gray-palace-top","black-palace-top","brown-palace-top","orange-palace-top"
	   ,"blue-dome-top","green-dome-top","red-dome-top","yellow-dome-top"
	   ,"stable-top","tower-top"
	   ,"wall-h-top","wall-v-top"
	   ,"meeple-h"
	   ,"well-top"
	   };
   
   public static final String[] PrettyNames = 
   {
   // palaces are in order of their value as palace claim tiles
   "Grey Palace",
   "Black Palace",
   "Brown Palace",
   "Orange Palace",
   "Blue Dome",
   "Green Dome",
   "Red Dome",
   "Yellow Dome",
   "Stable",
   "Tower",
   "Wall",
   "Wall",
   "Meeple",
   "Well",
   "Tea","Tea-back","Waste",
   "Tower 1 Bonus (1 point)",
   "Tower 2 Bonus (2 points)",
   "Tower 3 Bonus (3 points)",
   "Tower 4 Bonus (4 points)",
   // palaces claim tiles are in order 1 point to 4 points
   "Grey Palace Bonus (1 point)",
   "Black Palace Bonus (2 points)",
   "Brown Palace Bonus (3 points)",
   "Orange Palace Bonus (4 points)",
   // alternate tiles
   "Grey Palace",
   "Black Palace",
   "Brown Palace",
   "Orange Palace",
   "Blue Dome",
   "Green Dome",
   "Red Dome",
   "Yellow Dome",
   "Stable",
   "Tower",
   "Wall",
   "Wall",
   "Meeple",
   "Well"
   };

   static final int nChips = ImageNames.length;
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        MedinaChip CC[] = new MedinaChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new MedinaChip(PrettyNames[i],i-FIRST_CHIP_INDEX,IM[i],rv.nextLong(),SCALES[i]); 
        	}
        CANONICAL_PIECE = CC;
        STABLE = CC[stable_index];
        TOWER = CC[tower_index];
        H_WALL = CC[wall_index];
        V_WALL = CC[wall_index+1];
        MEEPLE = CC[meeple_index];
        WELL = CC[well_index];
        TEA = CC[tea_index];
        TEA_BACK = CC[tea_back_index];
        WASTE = CC[waste_index];
        PURPLE_PALACE = getPalace(1);
        check_digests(CC);
		}
	}


}
