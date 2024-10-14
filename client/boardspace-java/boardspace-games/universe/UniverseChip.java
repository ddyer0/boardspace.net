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
package universe;

import java.awt.Rectangle;

import java.util.Hashtable;

import lib.*;
import online.game.chip;
import online.game.cell.Geometry;
class ChipStack extends OStack<UniverseChip>
{
	public UniverseChip[] newComponentArray(int n) { return(new UniverseChip[n]); }
}
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.
 * 
 * The canonical polyomino piece of each shape has a set of all rotations and reflections precalculated,
 * and a set of all unique rotations and reflections precalculated.
 * 
 * This chip set doubles slightly, in that each chip can be assigned "sudoku" values for each tile. Once
 * assigned, the values are immutable.
 */
public class UniverseChip extends chip<UniverseChip> implements UniverseConstants
{	
	private enum sided { normal, 	// flip side is different if geometry dictates.
		twosided, 	// piece is a different color on the opposite side
		onesided};	// purce can't be flipped over

	public static TweenStyle tweenStyle = TweenStyle.barbell;		// this affects only rendering
	public static final int SNAKES_PIECE_OFFSET = 100;				// the "snakes" tile set if 100-xx
	public static final int DOMAIN_PIECE_OFFSET = 200;				// the "domain" set of tiles
	public static final int GIVENS_PIECE_OFFSET = 300;				// the "givens" single chips
	public static int assignedSudokuValues = 0;						// count of chips with sudoku values
	private int chipIndex;								// the shape number, including the offset to the chipset. 
	private sided sides = sided.normal;						// true if this chip is always different when flipped over
	OminoStep[] pattern;								// the pattern that defines this exact shape, including rotations and reflections
	int [] patStepX;
	int [] patStepY;
	int sudokuValues[];									// sudoku values associated with the pattern
	private OminoStep extraStep;						// the same as pattern except for bulk five
	UniverseChip uniqueVariations[] = null;				// the same uniqueVariations array is shared by all instances of the shape.
	private UniverseChip allVariations[] = null;		// the same allVariations array is sharef by all instances of the shape.
	private String uid;									// a unique number that identifies this shape
	private String patternuid;
	ChipColor color;						// the color enum for this chip
	ChipColor nativeColor;
	private int patternIndex=-1;			// the pattern (shape) number for this chip
	public boolean flipped = false;			// true if this is a mirror image of the basic pattern
	public int rotated = 0;					// 0-3 for rotations of the basic pattern.
	
	public boolean isGiven()
	{
		return(chipIndex>=GIVENS_PIECE_OFFSET)&&((chipIndex-GIVENS_PIECE_OFFSET)<GIVENS_PIECES.length);
	}
	public int givensIndex()
	{
		G.Assert(isGiven(),"not a given");
		return(chipIndex-GIVENS_PIECE_OFFSET);
	}
	
	//phlip only, assign new colors to the flip side.  Owner doesn't change.
	private void assignColors(UniverseChip from)
	{
		color = from.color;
		image = from.image;
		h_border_image = from.h_border_image;
		v_border_image = from.v_border_image;
		h_barbell_image = from.h_barbell_image;
		v_barbell_image = from.v_barbell_image;
	}
	
	public ChipColor getColor() { return(color); }
	
	// only for phlip pieces
	private static void assignColors(UniverseChip chset[])
	{	for(UniverseChip chp : chset)
		{	for(UniverseChip ch : chp.getVariations())
			{
			if(ch.flipped)
				{
				ChipColor altColor = ChipColor.values()[ch.color.ordinal()^1];
				UniverseChip altChip = getChip(altColor,ch.getPatternIndex(),false,ch.rotated);
				ch.assignColors(altChip);
				}
			}
		}
	}
	//
	// images used for tweening multiple-square pieces
	private StockArt h_border_image;
	private StockArt v_border_image;
	private StockArt h_barbell_image;
	private StockArt v_barbell_image;
	
	private static UniverseChip[][] chipSets = null;
	
	// all chips, used to find duplicates under rotation and flip
	static private Hashtable<String,UniverseChip>allChips = new Hashtable<String,UniverseChip>();
	
	public String getUid() { return(uid); }					// the really unique id, different for the same shape in different chipsets
	public String getPatternUid() { return(patternuid); }	// descriptor of the pattern without the chip number
	private UniverseChip intern()
	{	UniverseChip ch = allChips.get(uid);
		if(ch==null) { ch = this; allChips.put(uid,this); }
		return(ch);
	}
	public int patternSize() { return(pattern.length+1); }

	//
	// assign the sudoku values for this shape, and rebuild the flip/rotate variations using the new numbers.
	//
	public synchronized void assignSudokuValues(int values[])
	{	if((uniqueVariations!=null) && (this!=uniqueVariations[0]))
			{
			assignedSudokuValues++;
			uniqueVariations[0].assignSudokuValues(values);
			}
			else if(values!=sudokuValues)
			{	assignedSudokuValues=0;
				sudokuValues = values;
				allVariations = null;
				uniqueVariations = null;
				assignUid();
				buildVariations();		// rebuild the variations with the new sudoku values
			}
	}
	private void assignUid() 
	{	patternuid = OminoStep.calculateUidString(pattern,sudokuValues);
		uid = patternuid+G.format("#%s%s",chipIndex,(sided.twosided==sides&&flipped)?"f":"");
	}
	static public long DigestAll()
	{	long v = 0;
		for(UniverseChip row[] : chipSets)
		{
		for(UniverseChip ch : row) { v ^= ch.Digest(); }
		}
		return(v);
	}

	// clear the sudoku values.  This is necesary when we're geneating a new puzzle
	// and don't want the newly generated pentomino puzzle to be restricted by the 
	// current sudoku values
	static public void clearSudokuValues()
	{	allChips.clear();
		for(UniverseChip row[] : chipSets)
		{
		for(UniverseChip ch : row)
		{
			ch.assignSudokuValues(null);
		}}
		assignedSudokuValues = 0;
	}

	// constructor for givens
	private UniverseChip(int v,OminoStep[]pat,Image im,double[]sc,long rv)
	{
		sudokuValues = new int[1];
		chipIndex = GIVENS_PIECE_OFFSET+v;
		sudokuValues[0] = v;
		pattern = pat;
		patStepX = new int[pat.length];
		patStepY = new int[pat.length];
		buildPatStep();
		scale = sc;
		image = im;
		randomv = rv;
		assignUid();
		
	}
	// constructor used by preloadimages and buildvariations
	private UniverseChip(sided two,boolean flip,ChipColor co,OminoStep[] pat,Image im,
			StockArt h_border,StockArt v_border,
			StockArt h_barbell,StockArt v_barbell,
			long rv,double[]sca,int patind,int chip,int su[])
	{	sides = two;
		nativeColor = color = co;
		pattern = pat;
		patStepX = new int[pat.length];
		patStepY = new int[pat.length];
		buildPatStep();
		extraStep = null;
		randomv = rv;
		image = im;
		flipped = flip;
		h_border_image = h_border;
		v_border_image = v_border;
		h_barbell_image = h_barbell;
		v_barbell_image = v_barbell;
		scale = sca;
		patternIndex = patind;
		chipIndex = chip;
		sudokuValues = su;
		// 
		// this is to make the appearance of bulky five correct when drawn
		if((pattern==OminoStep.FIVE_BULK)||(pattern==OminoStep.FOUR_SQUARE)) 
			{ extraStep = OminoStep.FIVE_BULK_EXTRA; 
			}
		assignUid();
	}
	public Rectangle boundingBox(int squaresize,int x,int y)  
	{ return(OminoStep.boundingBox(pattern,squaresize,x,y)); 
	} 
	public Rectangle boundingSquare(int squaresize,int x,int y)  
	{ return(OminoStep.boundingSquare(pattern,squaresize,x,y)); 
	} 
	// build a single variation of this chip
	private UniverseChip buildVariation(int rot, boolean fl)
	{	OminoStep pat[] = OminoStep.permutedPattern(pattern,rot,fl);
		OminoStep extra = OminoStep.permutedStep(extraStep,rot,fl);
		UniverseChip ch = new UniverseChip(sides,fl,color,pat,image,
				h_border_image,v_border_image,
				h_barbell_image,v_barbell_image,
				randomv*(rot+1+(fl?12:0)),scale,
				patternIndex,chipIndex,sudokuValues);
		ch.allVariations = allVariations;
		ch.extraStep = extra;
		ch.rotated = rot;
		return(ch);
	}
	
	// build all the variations of this chip, check them for duplicates given
	// arbitrary flips and rotations.  This depends on teh UID fields of the 
	// chips being accurate.   Also, previously interned values for a different
	// set of sudoku values must not exist.
	private void buildVariations()
	{	if(allVariations==null)
		{
		ChipStack uni = new ChipStack();
		allVariations = new UniverseChip[8];	// 4 rotations 2 flips
		allVariations[0] = this;
		uni.push(this);
		this.intern();
		for(int i=1;i<8;i++)
			{
			UniverseChip ch = buildVariation(i%4,(i>=4));		// build a rotated and reflected chip
			UniverseChip in = ch.intern();						// canonicalize
			// if this new variation is a duplicate, it will be discarded.  We wasted our time and space, but just this once.
			allVariations[i] = in;								// save in the variations array
			uni.pushNew(in); // it was new		// if it's new save in the uniquevariations stack
			}
		uniqueVariations = uni.toArray();						// now that we know how many, save the unique variations array
		for(UniverseChip ch : uniqueVariations) { ch.uniqueVariations = uniqueVariations; }	// distribute to all of us
		}
	}

	
	//
	// public identifiers for this chip
	//
	public int getPatternIndex()			// the chip number - single block is #0 domino is #1 and so on 
	{ return(patternIndex); }

	static private int flipoff = 10000;
	static private int rotoff = 1000;
	public int isomerIndex() 
		{ return(chipIndex+rotated*rotoff+(flipped?flipoff:0)); 
		}
	static public UniverseChip getIsomer(int n) 
		{ UniverseChip ch = getChip(n%rotoff);
		  return(ch.getVariation(((n%flipoff)/rotoff),n>=flipoff)); }

	// 
	// note that for shapes which have nondistinct rotations and flips, the 
	// items you get will have rotation/reflection of the first copy of the 
	// shape, not necessarily the one you asked for.
	//
	public UniverseChip getVariation(int rot,boolean flip)
	{	buildVariations();
		return(allVariations[(rot%4)+(flip?4:0)]);
	}
	
	public UniverseChip getFlippedPattern(boolean flip)
	{
		for(UniverseChip ch : allVariations) 
		{
			if(ch.flipped==flip)
			{
				if(ch.patternuid.equals(patternuid)) { return(ch); }
			}
		}
		return(getVariation(rotated,flip));
	}
	public int chipNumber() { return(chipIndex); }
	
	public String toString()
	{	String sudostring = "";
		if(sudokuValues!=null) 
		{
			sudostring = "(";
			for(int v : sudokuValues) { sudostring+=" "+v; }
			sudostring += " )";
		}
		return("<#"+chipIndex+" "+color+" r"+rotated+(flipped?"F":"")+sudostring+uid +">" );
	}
	public String contentsString() 
	{ return(OminoStep.toString(pattern)); 
	}

	enum ChipColor 
    {	Yellow, Red, Green, Blue,
    }
     
	public void drawChipTween(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,boolean vertical,int cx,int cy,String label)
	{	switch(tweenStyle)
		{
	case none: break;
	case smudge:
		if (!vertical) { h_border_image.drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,label); }
		else { v_border_image.drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,label); }
		break;
	case barbell:
		if (vertical) { h_barbell_image.drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,label); }
		else { v_barbell_image.drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,label); }
		break;
	default:
		break;
		}
	}
    public void drawChip(Graphics gc,
            exCanvas canvas,
            int SQUARESIZE,
            double xscale,
            int cx,
            int cy,
            java.lang.String label)
    {	OminoStep.drawChip(pattern,sudokuValues,extraStep,this,gc,canvas,
    				SQUARESIZE,
    				xscale,cx,cy,label);
    }
    //
    // build a map of the distance in each point of the pattern from origin
    // 
    public void buildPatStep()
    {	int dx = 0;
    	int dy = 0;
     	int idx = 0;
    	for( OminoStep step : pattern)
    	{	dx += step.dx;
    		dy += step.dy;
    		patStepX[idx] = dx;
    		patStepY[idx] = -dy;
    		idx++;
    	}
    }

    
    //
    // this finds the chip highlight for the full footprint of the chip, 
    // and as a side effect copies the sweep counter from the seed to the
    // rest of the pattern.
    //
    public boolean canAddChip(UniverseCell c)
    {	
    	if(c.topChip()!=null) { return(false); }
     	UniverseCell d = c;
    	if(d.topChip()!=null) { return(false); }
    	for( OminoStep step : pattern)
    	{	UniverseCell d1 = d;
    		boolean retry = false;
    		switch(step.dx)
    		{	case -1:	d = d.exitTo(UniverseBoard.CELL_LEFT());
    						break;
    			case 1:		d = d.exitTo(UniverseBoard.CELL_RIGHT());
    						break;
    			default:
    		}
    		if((d==null)||(d.topChip()!=null))
    			{ 
    			// if stepping once is off the grid, try the other way
    			d = d1;
    			retry = true;
    			}
    		switch(step.dy)
    		{	case -1:	d = d.exitTo(UniverseBoard.CELL_UP());
    						break;
    			case 1:		d = d.exitTo(UniverseBoard.CELL_DOWN());
    						break;
			default:
				break;
    		}
    		if(retry && (d!=null))
    		{
    			switch(step.dx)
        		{	case -1:	d = d.exitTo(UniverseBoard.CELL_LEFT());
        						break;
        			case 1:		d = d.exitTo(UniverseBoard.CELL_RIGHT());
        						break;
        			default:
        		}	
    		}
    		if((d==null)||(d.topChip()!=null)) { return(false); }
    		// copy the sweep to the new cell
    		d.sweep_counter = c.sweep_counter;
     	}

    	return(true);
    	}
    

    //
    // this finds the chip highlight for the full footprint of the chip, 
    // and as a side effect copies the sweep counter from the seed to the
    // rest of the pattern.
    // 
    public boolean markSampleChip(UniverseCell c,Mspec m)
    {	if(c.topChip()!=null) { return(false); }
		G.Assert(c.geometry==Geometry.Square,"is a square cell");
		UniverseCell d = c;
		d.sampleMove = m;
		d.nMoves++;
		if(d.topChip()!=null) { return(false); }
		for( OminoStep step : pattern)
		{	UniverseCell d1 = d;
			boolean retry = false;
			switch(step.dx)
			{	case -1:	d = d.exitTo(UniverseBoard.CELL_LEFT());
							break;
				case 1:		d = d.exitTo(UniverseBoard.CELL_RIGHT());
							break;
				default:
			}
			if((d==null)||(d.topChip()!=null))
				{ 
				// if stepping once is off the grid, try the other way
				d = d1;
				retry = true;
				}
			switch(step.dy)
			{	case -1:	d = d.exitTo(UniverseBoard.CELL_UP());
							break;
				case 1:		d = d.exitTo(UniverseBoard.CELL_DOWN());
							break;
			default:
				break;
			}
			if(retry && (d!=null))
			{
				switch(step.dx)
	    		{	case -1:	d = d.exitTo(UniverseBoard.CELL_LEFT());
	    						break;
	    			case 1:		d = d.exitTo(UniverseBoard.CELL_RIGHT());
	    						break;
	    			default:
	    		}	
			}
			if((d==null)||(d.topChip()!=null)) { return(false); }
		   	d.sampleMove = m;
	    	d.nMoves++;
	 	}

	return(true);
    }
    //
    // this finds the chip highlight for the full footprint of the chip
    // 
    public boolean findChipHighlight(HitPoint highlight,
            int squareWidth,
            int squareHeight,
            int cx,
            int cy)
    {	return(OminoStep.findChipHighlight(pattern,highlight,squareWidth,squareHeight,cx,cy));
    }
    
    static private UniverseChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private UniverseChip SNAKES_PIECE[] = null;
    static private UniverseChip PHLIP_PIECES[] = null;
    static private UniverseChip GIVENS_PIECES[] = null;
    
    public UniverseChip[] getVariations() { return(uniqueVariations); }
    public int nVariations() { return(uniqueVariations.length); }
    public UniverseChip nextRotation()
    {	return(getVariation(rotated+1,flipped));
    }
    public UniverseChip prevRotation()
    {	return(getVariation(rotated+3,flipped));
    }
    public UniverseChip flip()
    {  	return(getVariation(rotated,!flipped));
    }
    public static UniverseChip getGiven(int n)
    {
    	return(getChip(n+GIVENS_PIECE_OFFSET));
    }
	public static UniverseChip getChip(int index)
	{	if(index>=GIVENS_PIECE_OFFSET)
		{ return(GIVENS_PIECES[index-GIVENS_PIECE_OFFSET]);
		}
		else if(index>=DOMAIN_PIECE_OFFSET)
		{
		return(PHLIP_PIECES[index-DOMAIN_PIECE_OFFSET]);
		} 
		else if(index>=SNAKES_PIECE_OFFSET)
		{
		return(SNAKES_PIECE[index - SNAKES_PIECE_OFFSET]);
		}
		else 
		{ return(CANONICAL_PIECE[index]); }
	} 
	
	public static UniverseChip getChip(ChipColor pl,int idx)
	{	if(idx>=GIVENS_PIECE_OFFSET)
		{
			return(GIVENS_PIECES[idx-GIVENS_PIECE_OFFSET]);
		}
		else if(idx>=DOMAIN_PIECE_OFFSET)
		{
		return(PHLIP_PIECES[pl.ordinal()*OminoStep.PHLIPSET.length + idx - DOMAIN_PIECE_OFFSET]);
		} 
		else if(idx>=SNAKES_PIECE_OFFSET)
		{
		return(SNAKES_PIECE[pl.ordinal()*OminoStep.SNAKES.length + idx - SNAKES_PIECE_OFFSET]);
		}
		else
		{
		return(CANONICAL_PIECE[pl.ordinal()*OminoStep.FULLSET.length + idx]);
		}
	}
	public static UniverseChip getChip(ChipColor co,int idx, boolean flip, int rot)
	{	UniverseChip ch = getChip(co,idx);
		return(ch.getVariation(rot,flip));
	}
    static private double SCALES[][] =
    {	{0.479,0.480,1.398},		// yellow
    	{0.479,0.480,1.398},		// red
    	{0.479,0.480,1.398},		// green
    	{0.479,0.480,1.398}		// blue
   };
    private static double[] GIVENSSCALE = { 0.5,0.5,1.0}; 
    
  /* pre load images and create the canonical pieces
   * 
   */
   static final UniverseChip[] createGroup(sided two,OminoStep pa[][],Image IM[],
		   StockArt BO_H[],StockArt BO_V[],StockArt BB_H[], StockArt BB_V[],
		   int piece_offset)
   {   ChipColor cc[] = ChipColor.values();
       Random rv = new Random(526352+piece_offset);		// an arbitrary number, just change it
       UniverseChip result[] = new UniverseChip[cc.length*pa.length];
       for(ChipColor c1 : ChipColor.values())
       {
    	int color = c1.ordinal();
       	int idx = 0;
       	for(OminoStep p1[] : pa)
   		{
     	  UniverseChip ch = new UniverseChip(two,false,c1,p1,IM[color],
     			  BO_H[color],BO_V[color],
     			  BB_H[color],BB_V[color],
     			  rv.nextLong(),SCALES[color],
     			  idx,color*pa.length+idx+piece_offset,
     			  null);
     	  idx++;
     	 result[ch.chipIndex-piece_offset] = ch;
		    ch.getVariation(0,false);	// trigger calculating the rotations and reflections
   		}}
       check_digests(result);
       return(result);
   }
   
   static final String[] ImageNames = 
       {"yellow","red","green","blue"};
   static final String[] BorderNames = 
   		{"yellow-border","red-border","green-border","blue-border"};
   private static String[] GivensImageNames = {"light-chip"};
   private static boolean imagesLoaded = false;
   
	// call from the viewer's preloadImages
	public synchronized static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        Image IG[]=forcan.load_masked_images(ImageDir,GivensImageNames);
        Image h_border = forcan.load_image(ImageDir,"h-border-mask");
        Image v_border = forcan.load_image(ImageDir,"v-border-mask");
        Image h_barbell = forcan.load_image(ImageDir,"h-barbell-mask");
        Image v_barbell = forcan.load_image(ImageDir,"v-barbell-mask");

        Image h_borders[] = forcan.load_images(ImageDir,BorderNames,h_border);
        Image v_borders[] = forcan.load_images(ImageDir,BorderNames,v_border);
        
        Image h_barbells[] = forcan.load_images(ImageDir,BorderNames,h_barbell);
        Image v_barbells[] = forcan.load_images(ImageDir,BorderNames,v_barbell);

        StockArt BO_H[]=StockArt.preLoadArt(BorderNames,h_borders,SCALES);
        StockArt BO_V[]=StockArt.preLoadArt(BorderNames,v_borders,SCALES);
        
        StockArt BB_H[]=StockArt.preLoadArt(BorderNames,h_barbells,SCALES);
        StockArt BB_V[]=StockArt.preLoadArt(BorderNames,v_barbells,SCALES);
        
        CANONICAL_PIECE = createGroup(sided.normal,OminoStep.FULLSET,IM,BO_H,BO_V,BB_H,BB_V,0);
        Image.registerImages(CANONICAL_PIECE);
        G.Assert(SNAKES_PIECE_OFFSET>CANONICAL_PIECE.length,"overlap");
        SNAKES_PIECE = createGroup(sided.normal,OminoStep.SNAKES,IM,BO_H,BO_V,BB_H,BB_V,SNAKES_PIECE_OFFSET);
        Image.registerImages(SNAKES_PIECE);
        G.Assert(DOMAIN_PIECE_OFFSET>SNAKES_PIECE.length+SNAKES_PIECE_OFFSET,"overlap");
        PHLIP_PIECES = createGroup(sided.twosided,OminoStep.PHLIPSET,IM,BO_H,BO_V,BB_H,BB_V,DOMAIN_PIECE_OFFSET);
        Image.registerImages(PHLIP_PIECES);
        Random r = new Random(12387535);
        GIVENS_PIECES =  new UniverseChip[OminoStep.GIVENS.length];
        Image.registerImages(GIVENS_PIECES);
        for(int i=0;i<GIVENS_PIECES.length;i++)
        {	UniverseChip ch = GIVENS_PIECES[i] = new UniverseChip(i,OminoStep.GIVENS[i],IG[0],GIVENSSCALE,r.nextLong());
        	ch.getVariation(0,false);
  		}
        
        chipSets = new UniverseChip[3][];
        chipSets[0] = CANONICAL_PIECE;
        chipSets[1] = SNAKES_PIECE;
        chipSets[2] = PHLIP_PIECES;	
        // GIVENS_PIECES is not included in chipsets because they keep their sudoku values
 
        assignColors(PHLIP_PIECES);		// change the color of the flipped pieces
        
        imagesLoaded = true;
		}
	}


}
