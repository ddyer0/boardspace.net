package octiles;


import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.G;
import lib.Random;
import online.common.exCanvas;
import online.game.chip;

/*
 * generic "playing piece" class, provides the background octagon chip, runners,
 * and lines as overlays to the octagon.
 */
public class OctilesChip extends chip<OctilesChip>
{	static final int RUNNER_INDEX = 100;
	static final int BLUE_RUNNER = 100;
	static final int RED_RUNNER = 101;
	static final int YELLOW_RUNNER = 102;
	static final int GREEN_RUNNER = 103;
	static final int BLUE_RUNNER_INDEX = 0;
	static final int RED_RUNNER_INDEX = 1;
	static final int YELLOW_RUNNER_INDEX = 2;
	static final int GREEN_RUNNER_INDEX = 3;
	
	private int tileIndex;
	public int colorIndex = -1;
	public boolean isTile = false;
	public boolean isRunner = false; 
	public String contentsString() { return(" "+file); }
	private int lines[]=null;		// definition of the lines for this chip
	private int drawOrder[]=null;
	private OctilesChip altChip = null;
	public OctilesChip disk = null;
	private boolean isFlat = false;
	public OctilesChip getAltChip(int chipSet) { return(((chipSet==0)||(altChip==null)) ? this : altChip); }
	
	private static final int tileMap[][] =
	{		// index is the side clockwise starting at West, value is the connection side
			// each entry should be a permutation of 0-8
			{4,5,6,7,0,1,2,3},	// tile 1, 4 straight connections
			{4,5,7,6,0,1,3,2},	// tile 2, 2 straights and 2 60 degree swishes
			{5,4,7,6,1,0,3,2},	// tile 3, 4 60 degree swishes
			{5,3,6,1,7,0,2,4},	// tile 4, 1 straight, 2 60, 1 90
			{4,3,6,1,0,7,2,5},	// tile 5, 2 straights at 90 degre angles, 2 90 degree
			{3,6,4,0,2,7,1,5},	// tile 6, 2 60's and 2 90's 
			{1,0,5,6,7,2,3,4},	// tile 7, 1 30 and 3 60's
			{1,0,6,7,5,4,2,3},	// tile 8, 2 30s and 2 straights
			{1,0,6,5,7,3,2,4},	// tile 9, 30 60 90 straight
			{7,4,6,5,1,3,2,0},	// tile 10, mirror of tile 9
			{6,3,4,1,2,7,0,5},	// tile 11, 4 90s
			{1,0,7,5,6,3,4,2},	// tile 12, 30 60 2 90s
			{1,0,4,6,2,7,3,5},	// tile 13 30 90 90 60
			{1,0,7,6,5,4,3,2},	// tile 14 2 30s 2 60s
			{4,2,1,5,0,3,7,6},	// tile 15 straight 2 30s 1 90
			{1,0,5,4,3,2,7,6},	// tile 16 3 30s 1 60
			{1,0,4,5,2,3,7,6},	// tile 17 2 60s 2 90s
			{1,0,3,2,5,4,7,6}	// tile 18 4 30s
			
	};	// 18 tiles
	
	private static final int drawMap[][] =
	{		// the values are the draw order for strokes.  0 first.  Drawing
			// in this order makes the pattern of overpass/underpass for tiles
			// almost completely agree with the real tiles.
			{0,3,2,1,4,5,6,7},	// tile 1, 4 straight connections
			{2,3,0,1,4,5,6,7},	// tile 2, 2 straights and 2 60 degree swishes
			{0,2,1,3,4,5,6,7},	// tile 3, 4 60 degree swishes
			{0,4,2,1,3,5,6,7},	// tile 4, 1 straight, 2 60, 1 90
			{0,2,1,3,4,5,6,7},	// tile 5, 2 straights at 90 degre angles, 2 90 degree
			{0,1,2,3,4,5,6,7},	// tile 6, 2 60's and 2 90's 
			{0,1,4,2,3,5,6,7},	// tile 7, 1 30 and 3 60's
			{0,1,2,3,4,5,6,7},	// tile 8, 2 30s and 2 straights
			{0,1,2,4,3,5,6,7},	// tile 9, 30 60 90 straight
			{0,2,1,3,4,5,6,7},	// tile 10, mirror of tile 9
			{5,1,2,3,4,0,6,7},	// tile 11, 4 90s
			{0,1,2,4,3,5,6,7},	// tile 12, 30 60 2 90s
			{0,1,3,2,4,5,6,7},	// tile 13 30 90 90 60
			{0,1,2,3,4,5,6,7},	// tile 14 2 30s 2 60s
			{0,1,2,3,4,5,6,7},	// tile 15 straight 2 30s 1 90
			{0,1,2,3,4,5,6,7},	// tile 16 3 30s 1 60
			{0,1,3,2,4,5,6,7},	// tile 17 2 60s 2 90s
			{0,1,2,3,4,5,6,7}	// tile 18 4 30s
			
	};	// 18 tiles
	
	public static final int NTILES = tileMap.length;
	public static final int NRUNNERS = 4;
	
	public int chipNumber() { return((tileIndex<RUNNER_INDEX)?tileIndex+1:tileIndex); }

   static final int FIRST_TILE_INDEX = 0;

	private OctilesChip(String na,boolean flat,int pla,Image im,long rv,double scl[],int[]ll,int[]dd)
	{	
		file = na;
		isFlat = flat;
		tileIndex=pla;
		if(pla>=RUNNER_INDEX) { colorIndex = pla-RUNNER_INDEX; }
		isTile = (pla>=FIRST_TILE_INDEX) && (pla<(FIRST_TILE_INDEX+NTILES));
		isRunner = (pla>=RUNNER_INDEX)&&(pla<(RUNNER_INDEX+NRUNNERS));
		image = im;
		randomv = rv;
		scale = scl;
		lines = ll;
		drawOrder = dd;
	}
	
		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
	static int h_index = 0;
	static int v_index = 1;
	static int h_flat_index = 2;
	static int v_flat_index = 3;
	
    static OctilesChip CANONICAL_PIECE[] = null;	// created by preload_images
    static OctilesChip CANONICAL_RUNNER[] = null;	// preloading images of the runners
    static Image[][] lines30 = null;
    static Image[][] lines90 = null;
    static Image[][] lines120 = null;
    static Image[][] lines180 = null;


    // used for perspective chips
    static private double SCALES[][] =
    {	{0.541,0.55,1.95},		// light square
    	{0.516,0.616,1.0},		// blue runner
    	{0.541,0.608,1.0},		// red runner
    	{0.473,0.641,1.0},		// yellow runner
    	{0.508,0.608,1.0},		// green runner
    };
    // used for non-perspective chips
    static private double ALTSCALES[][] =
        {	{0.53,0.47,1.85},		// light square
        	{0.5,0.5,0.8},		// blue runner
        	{0.5,0.5,0.8},		// red runner
        	{0.5,0.5,0.8},		// yellow runner
        	{0.5,0.45,0.8}		// green runner
    };
    public static OctilesChip getRunner(int i) { return(CANONICAL_RUNNER[i]); }
	public static OctilesChip getTile(int color)
	{	return(CANONICAL_PIECE[FIRST_TILE_INDEX+color]);
	}
	
	public static OctilesChip getChip(int i)
	{	if(i>=RUNNER_INDEX) { return(getRunner(i-RUNNER_INDEX)); }
		return(getTile(i-1));
	}
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,int cx,int cy,String label)
	{	drawChip(gc,canvas,SQUARESIZE,cx,cy,label,0,0);
	}
	/**
	 *  determine the exit edge when placed with rotation and entering from some direction
	 *  the +1 is because the table assumes direction 0 is west, when actually it is southwest
	 * @param rotation
	 * @param enter
	 * @return direction of exit
	 */
	public int exitDirection(int rotation,int enter)
	{	G.Assert(lines!=null,"has lines");
		return((lines[(enter-rotation+8-1)%8]+8+rotation+1)%8);
	}
	/**
	 * draw the background chip using the inherited drawChip, then draw
	 * the lines as an overlay, considering the rotation and requests for
	 * highlighting
	 * @param gc
	 * @param canvas
	 * @param SQUARESIZE
	 * @param cx
	 * @param cy
	 * @param label
	 * @param rotation 0-7 steps rotation clockwise
	 * @param markedStrokes bitmask of 1<<stroke position to be highlighted
	 */
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,int cx,int cy,String label,int rotation,int markedStrokes)
	{	drawChip(gc,canvas,SQUARESIZE,1.0,cx,cy,label,rotation,markedStrokes);
	}
	/**
	 * draw the background chip using the inherited drawChip, then draw
	 * the lines as an overlay, considering the rotation and requests for
	 * highlighting
	 * @param gc
	 * @param canvas
	 * @param SQUARESIZE
	 * @param xscale
	 * @param cx
	 * @param cy
	 * @param label
	 * @param rotation 0-7 steps rotation clockwise
	 * @param markedStrokes bitmask of 1<<stroke position to be highlighted
	 */
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label,int rotation,int markedStrokes)
	{	
		super.drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,label);
		if(lines!=null && (gc!=null))
		{
		int mask = 0;
		OctilesChip alt = getAltChip(canvas.getAltChipset());
		boolean flat = alt.isFlat;
		double[] scale = flat ? alt.scale : this.scale;
		for(int ind= 0; ind<lines.length;ind++)
		{	int idx = drawOrder[ind];
			int exit = lines[idx];
			int entry = idx;
			int mod = 8;
			Image row[] = null;
			Image row_h[] = null;
			int newmask = (1<<entry) | (1<<exit);
			boolean marked = ( (1<<idx) & markedStrokes)!=0;
			if((mask&newmask)==0)
			{
			mask |= newmask;
			switch(Math.abs(idx-exit))
			{
			default: throw G.Error("Unexpected line length");
			case 7: entry = exit; 
				//$FALL-THROUGH$
			case 1:		
					row = lines30[flat?v_flat_index:v_index];
					row_h = lines30[flat?h_flat_index:h_index];
					break;
			case 6: entry = exit;
				//$FALL-THROUGH$
			case 2: 	
					row = lines90[flat?v_flat_index:v_index];
					row_h = lines90[flat?h_flat_index:h_index];
					break;
			case 5: entry = exit;
					//$FALL-THROUGH$
			case 3: 
					row = lines120[flat?v_flat_index:v_index];
					row_h = lines120[flat?h_flat_index:h_index];
					break;
			case 4: case 0: 
					row = lines180[flat?v_flat_index:v_index];
					row_h = lines180[flat?h_flat_index:h_index];
					entry = idx;
					mod=4; break;
			}
			canvas.drawImage(gc, row[(entry+rotation+800)%mod], scale,cx, cy, SQUARESIZE, 1.0,0.0,null,true);
			if(marked)
				{
				// draw the highlight line
				canvas.drawImage(gc, row_h[(entry+rotation+800)%mod], scale,cx, cy, SQUARESIZE, 1.0,0.0,null,true);
				}

			if(mask==0xff) { break;}
			}
			
		}
		if(mask!=0xff) { throw G.Error("missing lines"); }
		}
	}
   static final String[] ImageNames = 
       { "chip",
	     "blue-runner",
	     "red-runner",
	     "yellow-runner",
	     "green-runner",
	     "blank1-flat",
	     "blue-runner-flat",
	     "red-runner-flat",
	     "yellow-runner-flat",
	     "green-runner-flat",
	     "blue-disk",
	     "red-disk",
	     "yellow-disk",
	     "green-disk",
       };
 
   static final String[] line30names = 
   	{ "line-30-0","line-30-1","line-30-2","line-30-3","line-30-4",
	   "line-30-5","line-30-6","line-30-7"};
   static final String[] line90names = 
  	{ "line-90-0","line-90-1","line-90-2","line-90-3","line-90-4",
	   "line-90-5","line-90-6","line-90-7"};   
   static final String[] line120names = 
 	{ "line-120-0","line-120-1","line-120-2","line-120-3","line-120-4",
	   "line-120-5","line-120-6","line-120-7"};   
  
   static final String[] line180names = 
	   	{ "line-180-0","line-180-1","line-180-2","line-180-3"
		   }; 
   
    private static Image[][] compositeImages(ImageLoader forcan,String ImageDir,String[]maskNames)
    {
		Image masks[] = forcan.load_images(ImageDir, maskNames, "-mask");	
		Image hlines[] = Image.CompositeMasks(masks,8,0xe00000);
		Image vlines[] =  forcan.load_images(ImageDir, maskNames,masks);
		String flatNames[] = new String[maskNames.length];
		
		for(int i=0;i<flatNames.length;i++) { flatNames[i]=maskNames[i]+"-flat"; }
		Image flatmasks[] = forcan.load_images(ImageDir, flatNames, "-mask");
		
		Image flathlines[] =  Image.CompositeMasks(flatmasks,8,0xe00000);
		Image flatvlines[] = forcan.load_images(ImageDir, flatNames,flatmasks);
		return(new Image[][] {hlines,vlines,flathlines,flatvlines});
    }
   
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        OctilesChip CC[] = new OctilesChip[NTILES];
        Random rv = new Random(340644);		// an arbitrary number, just change it
        for(int i=0;i<NTILES;i++) 
        	{
        	OctilesChip chip = new OctilesChip("Tile "+(i+1),false,i,IM[0],rv.nextLong(),SCALES[0],tileMap[i],drawMap[i]);
        	OctilesChip altChip = new OctilesChip("Tile "+(i+1),true,i,IM[5],rv.nextLong(),ALTSCALES[0],tileMap[i],drawMap[i]);
        	chip.altChip = altChip;
        	CC[i]=chip;
        	
        	}
        CANONICAL_RUNNER = new OctilesChip[NRUNNERS];
        double dummyScale[] = {0.4,0.54,2.1};
        for(int i=0;i<NRUNNERS;i++)
        {	OctilesChip runner = new OctilesChip(ImageNames[i+1],false,RUNNER_INDEX+i,IM[i+1],
        			rv.nextLong(),SCALES[i+1],null,null);
        	OctilesChip altRunner = new OctilesChip(ImageNames[i+6],true,RUNNER_INDEX+i,IM[i+6],
        			rv.nextLong(),ALTSCALES[i+1],null,null);
        	OctilesChip disk = new OctilesChip(ImageNames[i+10],true,RUNNER_INDEX+i,IM[i+10],
        			0,dummyScale,null,null);
        	runner.disk = disk;
        	altRunner.disk = disk;
        	runner.altChip = altRunner;
        	CANONICAL_RUNNER[i] = runner; 
        }
		// line overlays for the blank tile
		lines30 = compositeImages(forcan,ImageDir,line30names);				
		lines90 = compositeImages(forcan,ImageDir,line90names);		
		lines120 = compositeImages(forcan,ImageDir,line120names);
		lines180 = compositeImages(forcan,ImageDir,line180names);
		CANONICAL_PIECE = CC;
        check_digests(CC);
	       
		}


	}

}