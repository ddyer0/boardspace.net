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
package volcano;

import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.Random;
import lib.exCanvas;
import online.game.chip;

/* a generic icehouse piece, 3 sizes and n colors
 * no particular knowlege of any icehouse game, but 
 * provides one canonical instance of each piece and images and digests
 * 
 * all the colors except black use the same transparency 
 * all the images are actually based on the same raw image, so size and scale 
 * are identical
 */
public class Pyramid extends chip<Pyramid>
{
	public int colorIndex;
	public int sizeIndex;
	// colors
	public static final int BLACK_PYRAMID = 6;
	public static final int CLEAR_PYRAMID = 5;
	public static final int PURPLE_PYRAMID = 4;
	public static final int BLUE_PYRAMID = 3;
	public static final int GREEN_PYRAMID = 2;
	public static final int RED_PYRAMID = 1;
	public static final int YELLOW_PYRAMID = 0;
	// sizes
	public static final int SMALL_PYRAMID = 2;
	public static final int MEDIUM_PYRAMID = 1;
	public static final int LARGE_PYRAMID = 0;
	
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static Pyramid CANONICAL_PIECE[][] = null;	// created by preload_images

	
	static final String COLORNAMES[] = {"Yellow","Red","Green","Blue","Purple","Clear","Black"};
    public static final int nColors = COLORNAMES.length;
    static final String SIZENAMES[] = {"L","M","S"};
    public static final int nSizes=SIZENAMES.length;
     
    
	private Pyramid(int color,int size,String fil,Image im,double []sc,long rv)
	{	colorIndex = color;
		sizeIndex = size;
		file = fil;
		image = im;
		scale = sc;
		randomv = rv;
	}
	
	public int pieceNumber()
	{	return(colorIndex*nSizes+sizeIndex);
	}
	
	public static Pyramid getPyramid(int color,int size)
	{	return(CANONICAL_PIECE[color][size]);
	}
	public static Pyramid getPyramid(int pieceNumber)
	{	return(CANONICAL_PIECE[pieceNumber/nSizes][pieceNumber%nSizes]);
	}
	

  /* preload images and create the canonical pieces
   * 
   */
   private static final String[][] IceNames =
	    {   {"yellow-l-s","yellow-m-s","yellow-s-s"},
	    	{"red-l-s","red-m-s","red-s-s"},
	    	{"green-l-s","green-m-s","green-s-s"},
	    	{"blue-l-s","blue-m-s","blue-s-s"},
	    	{"purple-l-s","purple-m-s","purple-s-s"},
	    	{"clear-l-s","clear-m-s","clear-s-s"},
	    	{"black-l-s","black-m-s","black-s-s"}
	    	};
	private static final double ICE_SCALES[][] = 
    {{0.6050,0.606,1.0},	// large yellow
   	 {0.572,0.516,1.0},		// medium yellow
   	 {0.586,0.551,1.0}};	// small yellow
    private static final String Opaque_Masks[] = {"opaque-l-mask","opaque-m-mask","opaque-s-mask"};

    
	public double getChipRotation(exCanvas canvas)
	{	int chips = canvas.getAltChipset();
		double rotation = 0;
		switch(chips)
		{
		default:
				break;
		case 1:	rotation=Math.PI/2; 
				break;
		}
		return(rotation);
	}
	//
	// alternate chipsets for playtable.  The normal presentation is ok for side by side play
	// slightly disconcerting for face to face play.  This supports two alternates, one
	// with white pieces inverted, one with pieces facing left and right
	//
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{	
		drawRotatedChip(gc,canvas,getChipRotation(canvas),SQUARESIZE,xscale,cx,cy,label);
	}
	
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		Random r = new Random(20500305);
		Image [] opaque = forcan.load_images(ImageDir,Opaque_Masks);
        Image [] icemasks = forcan.load_images(ImageDir,IceNames[0],"-mask");
         Pyramid CC[][] = new Pyramid[nColors][nSizes];
        for(int i=0;i<nColors;i++)
        {Image IM[]=forcan.load_images(ImageDir,IceNames[i],(i==BLACK_PYRAMID)?opaque:icemasks);
         Pyramid row[] = new Pyramid[nSizes];
         for(int j=0;j<nSizes;j++) 
         	{ row[j]=new Pyramid(i,j,
         				IceNames[i][j],
         				IM[j],
         				ICE_SCALES[j],
         				r.nextLong()); 
         	}
         CC[i]=row;
        }
        CANONICAL_PIECE = CC;
		}
	}
}