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
package breakingaway;

import java.awt.Color;
import lib.Image;
import lib.ImageLoader;
/* below here should be the same for codename1 and standard java */
import lib.Random;
import lib.StockArt;
import online.game.chip;

public class BreakingAwayPiece extends chip<BreakingAwayPiece>
{	  static final int NCOLORS = 6;
	  int pieceNumber = 0;		// unique id for this piece
	  enum ChipColor 
	  { blue(Color.blue,Color.white),
		aqua(new Color(0.6f,0.9f,1.0f),Color.black),
		purple(new Color(0.6f,0.2f,0.7f),Color.white),
		yellow(Color.yellow,Color.black),
		white(Color.white,Color.black),
		green(Color.green,Color.white);
	  Color realColor; 
	  Color dotColor;
	  ChipColor(Color rc,Color dc) { realColor = rc; dotColor = dc;}
	  public static Color[] dotColors() 
	  {	ChipColor vv[] = values();
	    Color v [] = new Color[vv.length];
	    for(int i=0;i<vv.length;i++) { v[i]=vv[i].dotColor; }
	    return(v);
	  }
	  public static Color[] mouseColors() 
	  {	ChipColor vv[] = values();
	    Color v [] = new Color[vv.length];
	    for(int i=0;i<vv.length;i++) { v[i]=vv[i].realColor; }
	    return(v);
	  }

	  };
	  public ChipColor color;		// associated color
	  public int chipIndex() { return(pieceNumber); }
	  
	  static final String[] ImageFileNames = {"blue","aqua","purple","yellow","white","green"};
	  static final String[] PrefixNames = { "r-0-","r-120-","r-180-","r-220-","r-300-","r-60-"};
	  static final String[] MaskNames = { "mask" };

	  static final double SCALES[][] = 
	    {
		 {0.5,0.5, 1.0},	// R0
		 {0.5,0.6, 0.8},	// R120
		 {0.5,0.5, 1.0},	// R180
		 {0.5,0.5, 1.0},	// R220
		 {0.5,0.5, 0.85}, 	// R300
		 {0.5,0.5, 1.0}		// R60
	    };
	  

	  static BreakingAwayPiece CYCLES[][] = new BreakingAwayPiece[PrefixNames.length][ImageFileNames.length];
	  static StockArt DISCS[][] = new StockArt[PrefixNames.length][4];
	  
	  static BreakingAwayPiece[]CANONICAL_PIECE = null;
	  static BreakingAwayPiece getChip(int i) { return(CANONICAL_PIECE[i]); }
	
	BreakingAwayPiece(int idx,Image im,double []scales,String nam,long rv)
	{	pieceNumber = idx;
		image = im;
		color = ChipColor.values()[idx%NCOLORS];
		scale = scales;
		file = nam;
		randomv = rv;
	}
	public static Image[] loadCycleGroup(ImageLoader forcan,String Dir,int preindex)
	{	int len = ImageFileNames.length;
		String prefix = PrefixNames[preindex];
		String ar[] = new String[len];
		String ma[] = new String[1];
		for(int i=0;i<len;i++) { ar[i]= prefix+ImageFileNames[i]; }
		ma[0] = prefix + MaskNames[0];
		Image M[] = forcan.load_images(Dir,ma);
		Image IM[]=forcan.load_images(Dir,ar,M[0]);
		return(IM);
	}

	public BreakingAwayPiece getAltAngle(int set)
	{	return(CYCLES[set][pieceNumber]);
	}
	public static Image Icon = null;
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(Icon==null)
		{
		Random rv = new Random(6723324);
		int nChips = ImageFileNames.length;
		// load the main images, their masks, and composite the mains with the masks
		// to make transparent images that are actually used.
		for(int i=0,lim=PrefixNames.length; i<lim; i++)
		{
			//DISCS[i] = loadDotGroup(forcan,Dir,i);
			Image cycleImages[] = loadCycleGroup(forcan,Dir,i);
			for(int j=0;j<nChips;j++)
	        {
	        CYCLES[i][j] = new BreakingAwayPiece(j,cycleImages[j],SCALES[i],ImageFileNames[j],rv.nextLong());
	        }
		}
        CANONICAL_PIECE = CYCLES[0];
        check_digests(CANONICAL_PIECE);	// verify that the chips have different digests
		Icon = CANONICAL_PIECE[0].image;
		}
	}  
}