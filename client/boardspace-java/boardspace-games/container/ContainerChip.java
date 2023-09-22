/* copyright notice */package container;

import lib.Image;
import lib.ImageLoader;
import lib.G;
import lib.Random;
import online.game.chip;
import online.game.ColorNames;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class ContainerChip extends chip<ContainerChip> implements ContainerConstants,ColorNames
{	
	
	private int chipIndex;
	private String colorName;
	private ContainerChip altChip = null;
	public int chipNumber() { return(chipIndex); }
	public ContainerChip getAltChip(int set) { return ( ((set==0)||(altChip==null)) ? this : altChip); }
	
	private ContainerChip(String na,int pla,Image im,long rv,double scl[],String co,ContainerChip alt)
	{	
		file = na;
		altChip = alt;
		chipIndex=pla;
		image = im;
		colorName = co;
		randomv = rv;
		scale = scl;
	}
	public String toString()
	{	return("<"+ file+" #"+chipIndex+">");
	}
	public String getColor() { return(colorName); }
		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static public ContainerChip CANONICAL_PIECE[] = null;	// created by preload_images
    static double SCALES[][] =
    {	{0.5,0.5,1.0}		// yellow ship
      	,{0.5,0.5,0.95}		// aqua ship
      	,{0.5,0.5,0.99}		// black ship
      	,{0.5,0.5,0.98}		// maroon ship
      	,{0.5,0.5,1.0}		// azure ship
      	,{0.5,0.5,0.98}		// yellow ship - facing right
      	,{0.5,0.5,0.97}		// aqua ship
      	,{0.5,0.5,0.96}		// black ship
      	,{0.5,0.5,0.98}		// maroon ship
      	,{0.5,0.5,0.94}		// azure ship
      	
     	,{0.5,0.5,1.0}		// black cube
      	,{0.5,0.5,0.95}		// white cube
      	,{0.5,0.5,0.95}		// tan cube
      	,{0.5,0.5,1.0}		// brown cube
      	,{0.5,0.5,1.05}		// orange cube
      	,{0.5,0.5,1.0}		// gold cube
      	
      	,{0.5,0.5,1.4}		// black machine
      	,{0.5,0.5,1.25}		// white machine
      	,{0.5,0.5,1.4}		// tan machine
      	,{0.5,0.5,1.4}		// brown machine
      	,{0.5,0.5,1.4}		// orange machine
      	
      	,{0.5,0.5,1.1}		// warehouse
      	
      	,{0.5,0.5,1.0}		// loan
      	,{0.5,0.6,1.0}		// goal 0
      	,{0.5,0.6,1.0}		// goal 1
      	,{0.5,0.6,1.0}		// goal 2
      	,{0.5,0.6,1.0}		// goal 3
      	,{0.5,0.6,1.0}		// goal 4
     	,{0.5,0.6,1.0}		// blank goal
   	
    };
    
    static double FLATSCALES[][] =
        {	{0.5,0.5,1.0}		// yellow ship
          	,{0.527,0.5,1.02}		// aqua ship
          	,{0.538,0.5,0.955}		// black ship
          	,{0.533,0.5,0.98}		// maroon ship
          	,{0.5,0.566,1.016}		// azure ship
          	
          	,{0.5,0.5,0.98}		// yellow ship - facing right
          	,{0.5,0.5,1.03}		// aqua ship
          	,{0.5,0.5,0.96}		// black ship
          	,{0.5,0.5,0.98}		// maroon ship
          	,{0.5,0.5,0.94}		// azure ship
          	
         	,{0.679,0.55,1.265}		// black cube
          	,{0.720,0.474,1.234}		// white cube
          	,{0.738,0.55,1.228}		// tan cube
          	,{0.730,0.5,1.250}		// brown cube
          	,{0.736,0.49,1.209}		// orange cube
          	,{0.758,0.513,1.204}		// gold cube
          	
          	,{0.5,0.5,1.4}		// black machine
          	,{0.5,0.5,1.25}		// white machine
          	,{0.43,0.5,1.29}	// tan machine
          	,{0.5,0.452,1.4}		// brown machine
          	,{0.484,0.540,1.33}		// orange machine
          	
          	,{0.486,0.485,1.1}		// warehouse
        };
    public static String ContainerColors[] = { BlackColor,WhiteColor,TanColor,BrownColor,OrangeColor,GoldColor};
    public static String getContainerColor(int i)
    {	if((i>=0)&&(i<ContainerColors.length)) { return(ContainerColors[i]); }
    	return("Unknown#"+i);
    }	

 	public static ContainerChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}

  /* pre load images and create the canonical pieces
   * 
   */
 
   static final int SHIP_INDEX = 0;
   static final int RIGHT_FACE_OFFSET = 5;
   static final int CONTAINER_INDEX = SHIP_INDEX+10;
   static final int MACHINE_INDEX = CONTAINER_INDEX+6;
   static final int WAREHOUSE_INDEX = MACHINE_INDEX+5;
   static final int LOAN_INDEX = WAREHOUSE_INDEX+1;
   static final int GOAL_INDEX = LOAN_INDEX+1;
   static final int BLANK_GOAL_INDEX = GOAL_INDEX+5;
   static final String[] ImageNames = 
       {
	   // ships facing left
    	"yellow","aqua","black","maroon","seafoam",
	   // ships facing right
    	"yellow-r","aqua-r","black-r","maroon-r","seafoam-r",
	   // container cubes
	   "c-black","c-white","c-tan","c-brown","c-orange","c-gold",
	   // machines
	   "m-black","m-white","m-tan","m-brown","m-orange",
	   "warehouse","loan",
	   "goal-0","goal-1","goal-2","goal-3","goal-4",
	   "blank-card"
	   
	   };
   static final String[] ImageFlatNames = 
       {
	   // ships facing left
    	"yellow-flat","aqua-flat","black-flat","maroon-flat","seafoam-flat",
	   // ships facing right
    	"yellow-r-flat","aqua-r-flat","black-r-flat","maroon-r-flat","seafoam-r-flat",
	   // container cubes
	   "c-black-flat","c-white-flat","c-tan-flat","c-brown-flat","c-orange-flat","c-gold-flat",
	   // machines
	   "m-black-flat","m-white-flat","m-tan-flat","m-brown-flat","m-orange-flat",
	   "warehouse-flat"};
    static final String[] ColorNames = 
    {
	   // ships facing left
 	YellowColor,AquaColor,BlackColor,MaroonColor,AzureColor,		// ships left
 	YellowColor,AquaColor,BlackColor,MaroonColor,AzureColor,		// ships right
 	BlackColor,WhiteColor,TanColor,BrownColor,OrangeColor,GoldColor,	// containers
 	BlackColor,WhiteColor,TanColor,BrownColor,OrangeColor,			// machines
 	"","",		// loan cards
	"","","","","",""  // goal cards
	   };
   static final int nChips = ImageNames.length;
   public static ContainerChip BLANK_CARD;

   public boolean isShip()
   {	return((chipIndex>=SHIP_INDEX)&&(chipIndex<(SHIP_INDEX+10)));
   }

   static public ContainerChip getShip(int pl) 
   	{ G.Assert(pl>=0 && pl<MAX_PLAYERS, "is a ship index");
   	  return(CANONICAL_PIECE[pl+SHIP_INDEX]);
   	}
   public ContainerChip getAltShip() 
   	{ G.Assert(isShip(),"is a ship"); 
   	  return(CANONICAL_PIECE[chipIndex+RIGHT_FACE_OFFSET]);
   	}
	
   public boolean isContainer()
   {	return((chipIndex>=CONTAINER_INDEX)&&(chipIndex<=CONTAINER_INDEX+CONTAINER_COLORS));
   }
   public int getContainerIndex() 
   	{   G.Assert(isContainer(),"is a container");
   		return(chipIndex-CONTAINER_INDEX); 
   }
   public boolean isGoldContainer() { return(chipIndex==(CONTAINER_INDEX+MACHINE_COLORS)); }
   static public ContainerChip GOLD_CONTAINER = null;
   static public ContainerChip getContainer(int color)
 	{	if(color>=CONTAINER_OFFSET) { color -= CONTAINER_OFFSET; }	// allow container codes
 		G.Assert(color>=0 && color<CONTAINER_COLORS, "is a container index");
 		return(CANONICAL_PIECE[color+CONTAINER_INDEX]);
 	}
   
   public boolean isMachine()
   {	return((chipIndex>=MACHINE_INDEX)&&(chipIndex<MACHINE_INDEX+5));
   }
   public int getMachineIndex() 
   	{ G.Assert(isMachine(),"is a ship");
   	  return(chipIndex-MACHINE_INDEX);
   	}
   
   // get machine for a certain color.  Returns null for gold/luxury which is not
   // produced by a machine.
   static public ContainerChip getMachine(int color)
	{	if(color>=0 && color<MACHINE_COLORS) {return(CANONICAL_PIECE[color+MACHINE_INDEX]);}
		G.Assert(color==5,"Is a machine index");
		return(null);
	}
   // get the associated machine for a container chip
   public ContainerChip getMachine()
   {	G.Assert(isContainer(),"is a container");
   		return(getMachine(getContainerIndex()));
   }
   public boolean isCard()
   {	return((chipIndex>=GOAL_INDEX) && (chipIndex<(GOAL_INDEX+MAX_PLAYERS))); 
   }
   public int getCardIndex() 
   { 	G.Assert(isCard(),"is a goal card");
   		return(chipIndex-GOAL_INDEX); 
   	}
   public static ContainerChip getCard(int color)
 	{  G.Assert((color>=0) && (color<MAX_PLAYERS), "is a goal card index");
	   return(CANONICAL_PIECE[color+GOAL_INDEX]);
 	}
   
   public boolean isLoan() { return(chipIndex==LOAN_INDEX); }
   public static ContainerChip getLoan() { return(CANONICAL_PIECE[LOAN_INDEX]); }
   public static ContainerChip getCashbox() { return(CANONICAL_PIECE[LOAN_INDEX]); }
   public boolean isWarehouse() { return(chipIndex==WAREHOUSE_INDEX); }
   static public ContainerChip getWarehouse()
	{
		return(CANONICAL_PIECE[WAREHOUSE_INDEX]);
	}

	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String dir)
	{	if(BLANK_CARD==null)
		{
		int nColors = ImageNames.length;
        Image IMF[]=forcan.load_masked_images(dir,ImageFlatNames);
        Image IM[]=forcan.load_masked_images(dir,ImageNames);
        ContainerChip CC[] = new ContainerChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	ContainerChip alt = (i<ImageFlatNames.length)  
        				? new ContainerChip(ImageFlatNames[i],i,IMF[i],0,FLATSCALES[i],ColorNames[i],null)
        				: null;
        	CC[i]=new ContainerChip(ImageNames[i],i,IM[i],rv.nextLong(),SCALES[i],ColorNames[i],alt); 
        	}
        CANONICAL_PIECE = CC;
        GOLD_CONTAINER = getChip(CONTAINER_INDEX+MACHINE_COLORS);
        BLANK_CARD = getChip(BLANK_GOAL_INDEX);
        check_digests(CC);
		}
	}


}
