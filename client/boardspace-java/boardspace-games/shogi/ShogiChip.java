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
package shogi;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class ShogiChip extends chip<ShogiChip>
{	
	static enum PieceType
	{	
		Promoted_General("Promoted General",null,0),	// doesn't exist
		Promoted_Bishop("Flying Horse",null,15),
		Promoted_Gold("Promoted Gold",null,0),	// doesn't exist
		Promoted_Silver("Promoted Silver",null,9),
		Promoted_Knight("Promoted Knight",null,10),
		Promoted_Lance("Promoted Lance",null,10),
		Promoted_Rook("Dragon",null,17),
		Promoted_Pawn("Promoted Pawn",null,12),
		General("General",null,0),
		Bishop("Bishop",Promoted_Bishop,15),
		Gold("Gold",null,9),
		Silver("Silver",Promoted_Silver,8),
		Knight("Knight",Promoted_Knight,6),
		Lance("Lance",Promoted_Lance,5),
		Rook("Rook",Promoted_Rook,15),
		Pawn("Pawn",Promoted_Pawn,1);
		PieceType promoted = null; 
		PieceType demoted = null;
		String prettyName = null;
		int standardValue = 0;
		PieceType(String pretty,PieceType prom,int va)
		{	prettyName = pretty;
			promoted = prom;
			standardValue = va;
			demoted = this;
		}
		static {
			for(PieceType p : values())
			{	PieceType prom = p.promoted;
				if(prom!=null) { prom.demoted = p; }
			}
		}
		static PieceType find(int v)
		{	for(PieceType val : values()) { if(val.ordinal()==v) { return(val); }}
			return(null);
		}
		static PieceType find(String str)
		{	
			for(PieceType val : values())
			{	if(str.equalsIgnoreCase(val.toString())) { return(val); }	
			}
			return(null);
		}
	};
	
	int playerIndex;
	PieceType pieceType;
	boolean promoted=false;
	private String name = "";
	private int pieceIndex;
	public int forwardOneRow = 0;
	public int chipNumber() { return(pieceIndex); }
	public String contentsString() { return(name); }
	public ShogiChip alt_image = null;
	private boolean isAlt=false;
	
    static final int NPIECETYPES = PieceType.values().length;
    
    // chipset & 2 encodes the player direction
    // chipset & 1 encodes the western vs kanji characters
	public ShogiChip getAltChip(int chipset)
	{	ShogiChip ch = ((pieceType!=null)&&((chipset&2)!=0)) ? getChip(playerIndex^1,pieceType) : this;
		// if we are the other chipset, make sure the player swapped version we get is also otherchipset
		if(ch.isAlt!=isAlt) { ch=ch.alt_image; }	
		return((((chipset&1)!=0)&&(ch.alt_image!=null)) ?ch.alt_image : ch);
	}
	
    // the order here corresponds to the placement order in the initial setup
    static final String e_ImageNames[] = {
      	"down-large-blank","down-bishop-p","down-medium-blank","down-silver-p","down-knight-p","down-lance-p","down-rook-p","down-pawn-p",  
    	"down-general","down-bishop","down-gold","down-silver","down-knight","down-lance","down-rook","down-pawn",

     	"up-large-blank","up-bishop-p","up-medium-blank","up-silver-p","up-knight-p","up-lance-p","up-rook-p","up-pawn-p",
      	"up-general","up-bishop","up-gold","up-silver","up-knight","up-lance","up-rook","up-pawn",
     	
    	};
    static final String w_ImageNames[] = {
      	"down-large-blank","down-bishop-wp","down-medium-blank","down-silver-wp","down-knight-wp","down-lance-wp","down-rook-wp","down-pawn-wp",  
     	"down-general-w","down-bishop-w","down-gold-w","down-silver-w","down-knight-w","down-lance-w","down-rook-w","down-pawn-w",
      	
     	"up-large-blank","up-bishop-wp","up-medium-blank","up-silver-wp","up-knight-wp","up-lance-wp","up-rook-wp","up-pawn-wp",
   	    "up-general-w","up-bishop-w","up-gold-w","up-silver-w","up-knight-w","up-lance-w","up-rook-w","up-pawn-w",
     	    	};
  
	private ShogiChip(String na,int pla,Image im,long rv,double scl[])
	{	name = na;
		pieceIndex = pla;
		playerIndex=pla/NPIECETYPES;
		if(pla>=0) 
		{ pieceType = PieceType.values()[pla%NPIECETYPES]; 
		  forwardOneRow = (playerIndex==0)?-1:1;
		}
		image = im;
		randomv = rv;
		scale = scl;
	}
	public String toString()
	{	return("<"+ name+" #"+pieceIndex+">");
	}

	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private ShogiChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =	
    	{{0.578,0.554,1.0},		// (unusued)
    	{0.578,0.554,0.954},	// down bishop promoted
    	{0.578,0.554,1.0},		// (unused)
    	{0.578,0.488,1.0},		// down silver promoted
    	
    	{0.578,0.479,1.0},		// down knight promoted
    	{0.578,0.554,1.0},		// down lance promoted
    	{0.578,0.554,0.93},		// down rook promoted 
    	{0.578,0.554,0.763},	// down pawn promoted
    	
    	{0.578,0.554,1.0},	 // down general
    	{0.578,0.554,0.86},	 // down bishop
    	{0.578,0.554,0.875}, // down gold
    	{0.578,0.512,0.95},	 // down silver
    	
    	{0.578,0.554,0.902},	// down knight
    	{0.578,0.554,0.875},	// down lance
    	{0.578,0.554,0.902},// down rook
    	{0.578,0.554,0.888},// down pawn
    	
    	
    	{0.578,0.554,1.0},	// (unused)
    	{0.578,0.554,1.013},	// up bishop promoted
    	{0.578,0.554,1.0},	// (unused)
    	{0.578,0.554,0.875},	// up silver promoted
    	
    	{0.578,0.554,0.847},	// up knight promoted
    	{0.578,0.554,0.73},	// up lance promoted
    	{0.578,0.554,1.0},	// up rook promoted
    	{0.578,0.554,0.75},// up pawn promoted
    	
    	{0.578,0.554,1.0},	// up general
    	{0.578,0.554,1.04},	// up bishop
    	{0.578,0.554,0.94},	// up gold
    	{0.578,0.554,0.94},	// up silver
    	
    	{0.578,0.554,0.902},	// up knight
    	{0.578,0.554,0.93},	// up lance
    	{0.578,0.554,1.0},	// up rook
    	{0.578,0.554,0.847}};// up pawn
    	
    public static ShogiChip getChip(int chip)
	{	return(CANONICAL_PIECE[chip]);
	}

	public static ShogiChip getChip(int color,int chip)
	{	return(CANONICAL_PIECE[color*NPIECETYPES+chip]);
	}
	public static ShogiChip getChip(int color,PieceType chip)
	{	return(CANONICAL_PIECE[color*NPIECETYPES+chip.ordinal()]);
	}
	public ShogiChip getPromoted()	{
		ShogiChip ch = getChip(playerIndex,pieceType.promoted.ordinal());
		//G.Assert(ch.pieceType.demoted==pieceType.demoted,"promotion asymetric");
		return(ch);
	}
	public ShogiChip getDemoted()
	{	ShogiChip ch = getChip(playerIndex,pieceType.demoted.ordinal());
		//G.Assert(ch.pieceType.promoted==pieceType, "demotion asymtric");
		return(ch);
	}
	
	public ShogiChip getFlipped()
	{	if(pieceType.promoted!=null) { return(getPromoted()); }
		return(getDemoted()); 
	}
	/* pre load images and create the canonical pieces
   * 
   */
 
    static final String[] MaskNames = { "light-blank-mask" };
    static final String[] extraNames = {"check"};
    static ShogiChip check = null;
    
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		Image [] masks = forcan.load_images(ImageDir,e_ImageNames,"-mask");
        Image IM[]=forcan.load_images(ImageDir,w_ImageNames,masks);
        Image IM2[] = forcan.load_images(ImageDir,e_ImageNames,masks);
        int nPieces = IM.length;
        ShogiChip CC[] = new ShogiChip[nPieces];
        Random rv = new Random(35665);		// an arbitrary number, just change it
        for(int i=0;i<nPieces;i++) 
        	{
        	ShogiChip im2 = new ShogiChip(e_ImageNames[i],i,IM2[i],0,SCALES[i]);	// alternate chips for the other chip face
        	ShogiChip im1 = new ShogiChip(w_ImageNames[i],i,IM[i],rv.nextLong(),SCALES[i]);
        	CC[i]= im2.alt_image = im1; 
        	im1.alt_image = im2;
        	im2.isAlt = true;
      	
        	}
        check_digests(CC);
        
        // add the check sign
        Image extra[] = forcan.load_masked_images(ImageDir,extraNames);
        check = new ShogiChip(extraNames[0],-1,extra[0],rv.nextLong(),new double[]{0.5,0.5,1.0});
        Image.registerImages(CC);
        CANONICAL_PIECE = CC;
		}
	}

}
