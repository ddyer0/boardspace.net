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
package ponte;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class PonteChip extends chip<PonteChip> implements PonteConstants
{	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;

	private int num = 0;
	public double logScales[] = null;
	public int chipNumber() { return(num); }
	private int colorIndex;
	public int colorIndex() { return(colorIndex); }
	public PonteId id;
	Bridge bridge = null;
	public boolean isBridge() { return(bridge!=null); }

	public String toString()
	{	return("<"+ file+" #"+colorIndex+">");
	}
	public String contentsString() 
	{ return(file); 
	} 
 
	private PonteChip(PonteId ch,String na,int pla,int nn,double logScl[],double scl[],Bridge br)
	{	id = ch;
		id.chip = this;
		file = na;
		num = nn;
		colorIndex=pla;
		bridge = br;
		scale = scl;				// scales for board presentation
		logScales = logScl;			// second set of scales to use in game logs
		randomv = r.nextLong();
		allChips.push(this);
	}

	private static int FIRST_TILE_INDEX = 0;

	public static PonteChip getTile(int color)
	{	return(CANONICAL_PIECE[FIRST_TILE_INDEX+color]);
	}

	public static PonteChip getPiece(int pl)
	{
		return(CANONICAL_PIECE[pl]);
	}
	static public PonteChip white = new PonteChip(PonteId.White_Chip_Pool,"white-tile",0,0,
			new double[] { 1.0,1.3,0,-0.4},
			new double[]{0.569,0.489,1.446},null);
	static public PonteChip red = new PonteChip(PonteId.Red_Chip_Pool,"red-tile",1,1,
			new double[] { 1.0,1.4,0,-0.4},
			new double[]{0.605,0.503,1.446},null);
	
 
	static public PonteChip Bridge_30 = new PonteChip(PonteId.Bridge_30,"bridge-30",-1,2,
			new double[] { 1.0, 0.8, 0.2, -1.0},
			new double[]{0.944,0.083,2.0},bridges[0]);
	static public PonteChip Bridge_45 = new PonteChip(PonteId.Bridge_45,"bridge-45",-1,3,
			new double[] { 1.0,0.8,0,-1.0},
			new double[]{0.888,0.138,2.05},bridges[1]);
	static public PonteChip Bridge_60 = new PonteChip(PonteId.Bridge_60,"bridge-60",-1,4,
			new double[] {1.0, 0.8 ,0 ,-1.1},
			new double[]{0.875,0.034,1.333},bridges[2]);
	static public PonteChip Bridge_90 = new PonteChip(PonteId.Bridge_90,"bridge-90",-1,5,
			new double[] { 1.0,0.8,0,-1.2},
			new double[]{0.555,0.157,0.703},bridges[3]);
	static public PonteChip Bridge_120 = new PonteChip(PonteId.Bridge_120,"bridge-120",-1,6,
			new double[]  { 1.0, 0.8, -0.5, -1.0},
			new double[]{0.25,0.006,1.236},bridges[4]);
	static public PonteChip Bridge_135 = new PonteChip(PonteId.Bridge_135,"bridge-135",-1,7,
			new double[]  {1.0, 0.8, -1.0, -1.0},
			new double[]{0.191,0.202,2.1},bridges[5]);
	static public PonteChip Bridge_150 = new PonteChip(PonteId.Bridge_150,"bridge-150",-1,8,
			new double[] {1.0 ,0.8 ,-1.2, -1.0},
			new double[]{0.166,0.138,2.0},bridges[6]);
	static public PonteChip Bridge_180 = new PonteChip(PonteId.Bridge_180,"bridge-180",-1,9,
			new double[] { 1.0,0.8,-0.0,-0.5},
			new double[]{0.925,0.372,2.0},bridges[7]);
	
	
	static private PonteChip CANONICAL_PIECE[] = {
		white,red,
		Bridge_30,Bridge_45,
		Bridge_60,Bridge_90,
		Bridge_120,Bridge_135,
		Bridge_150,Bridge_180
	};

	public static int N_STANDARD_PIECES = CANONICAL_PIECE.length-2;
	
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(ImageDir,allChips);
		}
	}


}
