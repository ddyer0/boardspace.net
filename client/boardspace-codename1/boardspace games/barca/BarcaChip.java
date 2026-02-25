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
package barca;

import lib.Image;
import lib.ImageLoader;
import barca.BarcaConstants.BarcaId;
import lib.DrawableImageStack;
import lib.Random;
import lib.exCanvas;
import online.game.chip;

/**
 * this is a specialization of {@link chip} to represent the stones used by barca;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class BarcaChip extends chip<BarcaChip>
{
	private int index = -1;
	int colorIndex=-1;
	public BarcaChip icon = null;
	public int colorIndex() { return(colorIndex); }
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	private BarcaChip altChip= null;
	public BarcaId id;
	
	// constructor for the chips on the board, which are the only things that are digestable.
	private BarcaChip(String na,int ind,double[]sc,BarcaId con,int pl,BarcaChip ic,BarcaChip alt)
	{	index = ind;
		scale=sc;
		altChip = alt;
		file = na;
		icon = ic;
		colorIndex = pl;
		id = con;
		randomv = r.nextLong();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private BarcaChip(String na,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		allChips.push(this);
	}
	
	public int chipNumber() { return(index); }
	static final int whiteplayer = 0;
	static final int blackplayer = 1;
	static final BarcaChip White_Mouse_Icon = new BarcaChip("white-mouse-icon",-1,new double[]{0.5,0.5,0.8},null,whiteplayer,null,null);
	static final BarcaChip White_Lion_Icon = new BarcaChip("white-lion-icon",-1,new double[]{0.5,0.5,0.9},null,whiteplayer,null,null);
	static final BarcaChip White_Elephant_Icon = new BarcaChip("white-elephant-icon",-1,new double[]{0.5,0.5,1.0},null,whiteplayer,null,null);

	static final BarcaChip Black_Mouse_Icon = new BarcaChip("black-mouse-icon",-1,new double[]{0.5,0.5,0.8},null,blackplayer,null,null);
	static final BarcaChip Black_Lion_Icon = new BarcaChip("black-lion-icon",-1,new double[]{0.5,0.5,0.9},null,blackplayer,null,null);
	static final BarcaChip Black_Elephant_Icon = new BarcaChip("black-elephant-icon",-1,new double[]{0.5,0.5,1.0},null,blackplayer,null,null);

	static final double altScale[]= {0.491,0.50,0.68};
	static final BarcaChip Black_Mouse_np = new BarcaChip("blue-mouse-np",3,altScale,BarcaId.Black_Mouse,blackplayer,Black_Mouse_Icon,null);
	static final BarcaChip Black_Lion_np = new BarcaChip("blue-lion-np",4,altScale,BarcaId.Black_Lion,blackplayer,Black_Lion_Icon,null);
	static final BarcaChip Black_Elephant_np = new BarcaChip("blue-elephant-np",5,altScale,BarcaId.Black_Elephant,blackplayer,Black_Elephant_Icon,null);
	static final BarcaChip White_Mouse_np = new BarcaChip("red-mouse-np",0,altScale,BarcaId.White_Mouse,whiteplayer,White_Mouse_Icon,null);
	static final BarcaChip White_Lion_np = new BarcaChip("red-lion-np",1,altScale,BarcaId.White_Lion,whiteplayer,White_Lion_Icon,null);
	static final BarcaChip White_Elephant_np = new BarcaChip("red-elephant-np",2,altScale,BarcaId.White_Elephant,whiteplayer,White_Elephant_Icon,null);

	
	static final BarcaChip White_Mouse = new BarcaChip("white-mouse",0,new double[]{0.56,0.56,1.0},BarcaId.White_Mouse,whiteplayer,White_Mouse_Icon,White_Mouse_np);
	static final BarcaChip White_Lion = new BarcaChip("white-lion",1,new double[]{0.679,0.631,1.055},BarcaId.White_Lion,whiteplayer,White_Lion_Icon,White_Lion_np);
	static final BarcaChip White_Elephant = new BarcaChip("white-elephant",2,new double[]{0.693,0.628,1.29},BarcaId.White_Elephant,whiteplayer,White_Elephant_Icon,White_Elephant_np);

	static final BarcaChip Black_Mouse = new BarcaChip("black-mouse",3,new double[]{0.626,0.599,1.0},BarcaId.Black_Mouse,blackplayer,Black_Mouse_Icon,Black_Mouse_np);
	static final BarcaChip Black_Lion = new BarcaChip("black-lion",4,new double[]{0.637,0.630,1.08},BarcaId.Black_Lion,blackplayer,Black_Lion_Icon,Black_Lion_np);
	static final BarcaChip Black_Elephant = new BarcaChip("black-elephant",5,new double[]{0.634,0.668,1.23},BarcaId.Black_Elephant,blackplayer,Black_Elephant_Icon,Black_Elephant_np);

	public double getChipRotation(exCanvas canvas)
	{	int chips = exCanvas.getAltChipset(canvas); // in rare circumstances, canvas is null
		double rotation = 0;
		// alternate chipsets for arimaa, 1= white upside down 2 = twist left and right
		switch(chips)
		{
		default:
				break;
		case 1:	if(colorIndex==0) { rotation=Math.PI; }
				break;
		case 2: rotation = (colorIndex!=0) ? Math.PI/2 : -Math.PI/2;
				break;
		case 3: if(colorIndex!=0) { rotation=Math.PI; }
				break;
		case 4:	rotation = (colorIndex==0) ? Math.PI/2 : -Math.PI/2;
				break;
		case 5: break;	// 5 is alternate+never rotate
		}
		return(rotation);
	}


	static final BarcaChip[] CANONICAL_PIECE = {
			White_Mouse,White_Lion,White_Elephant,
			Black_Mouse,Black_Lion,Black_Elephant,
	};
	static final BarcaChip elephants[] = { White_Elephant,Black_Elephant};
	static final BarcaChip lions[] = { White_Lion, Black_Lion };
	static final BarcaChip mice[] = { White_Mouse, Black_Mouse};
	
	// indexes into the balls array, usually called the rack
    static final BarcaChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    
    public BarcaChip getAltChip(int set) 
    { return(((set==0)||(altChip==null)) ? this : altChip); 
    }
   
    /* plain images with no mask can be noted by naming them -nomask */
    static final public BarcaChip backgroundTile = new BarcaChip("background-tile-nomask",null);
    static final public BarcaChip backgroundReviewTile = new BarcaChip("background-review-tile-nomask",null);
   
    
    static final public BarcaChip board = new BarcaChip("board",new double[]{0.5,0.5,1.0});
    static final public BarcaChip board_np = new BarcaChip("board-np",new double[]{0.5,0.5,1.0});
   
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{	
		imagesLoaded = forcan.load_masked_images(Dir,allChips);
		if(imagesLoaded) { Image.registerImages(allChips); }
		}
	}   
}
