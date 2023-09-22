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
package quinamid;


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
public class QuinamidChip extends chip<QuinamidChip> implements QuinamidConstants
{	
	public int colorIndex;
	private String name = "";
	
	public int chipNumber() { return(colorIndex); }

    static final int RED_CHIP_INDEX = 1;
    static final int BLUE_CHIP_INDEX = 0;
    static final int BOARD_6_INDEX = 2;
    static final int BOARD_5_INDEX = 3;
    static final int BOARD_4_INDEX = 4;
    static final int BOARD_3_INDEX = 5;
    static final int BOARD_2_INDEX = 6;
    static final int ARROW_INDEX = 7;
    static final int PAD_INDEX = 8;
    static final int HELP_INDEX = 9;
	private QuinamidChip(String na,int pla,Image im,long rv,double scl[])
	{	name = na;
		colorIndex=pla;
		image = im;
		randomv = rv;
		scale = scl;
	}
	public String toString()
	{	return("<"+ name+" #"+colorIndex+">");
	}
	
	
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private QuinamidChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	
    	{0.572,0.526,1.192},	// blue chip
    	{0.524,0.520,1.192},	// red chip
    	{0.5,0.5,1.025},	// board x 6
    	{0.48,0.50,1.07},	// board x 5
    	{0.47,0.52,1.1},	// board x 4
    	{0.43,0.56,1.2},	// board x 3
    	{0.38,0.6,1.3},	// board x 2
       	{0.5,0.5,1.0},		// right arrow (rotated 8 times for various directions)
       	{0.50,0.50,1.0},		// red mask to mark movable boards
       	{0.5,0.5,1.0}

    };
     
 
    public static QuinamidChip getBoard(int stack)
    {	return(CANONICAL_PIECE[BOARD_6_INDEX+stack]);
    }
	public static QuinamidChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}

  /* pre load images and create the canonical pieces
   * 
   */
 
   static final String[] ImageNames = 
       { "red-chip","blue-chip",
       	 "board-6",
       	 "board-5",
       	 "board-4",
       	 "board-3",
       	 "board-2",
       	 "right-arrow",
       	 "landingpad",
       	 "help-panel-nomask"
       };
   	public static QuinamidChip red = null;
   	public static QuinamidChip blue = null;
    public String contentsString() { return(name); }
    private static QuinamidChip arrows[] = new QuinamidChip[8];
    public static QuinamidChip getArrow(int n) { return(arrows[n]); }
    public static QuinamidChip getLandingPad() { return(CANONICAL_PIECE[PAD_INDEX]); }
    public static QuinamidChip HelpPanel;
	// call from the viewer's preloadImages
	public synchronized static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(HelpPanel==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        QuinamidChip CC[] = new QuinamidChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new QuinamidChip(ImageNames[i],i,IM[i],rv.nextLong(),SCALES[i]); 
        	}
        CANONICAL_PIECE = CC;
        arrows[0] = CANONICAL_PIECE[ARROW_INDEX];
        for(int idx=1; idx<arrows.length;idx++)
        {	Image aimage = arrows[0].image.rotate((idx*45/360.0)*(2*Math.PI),0x0);
        	arrows[idx] = new QuinamidChip(ImageNames[ARROW_INDEX],ARROW_INDEX,aimage,rv.nextLong(),SCALES[ARROW_INDEX]);
        }
        red = CC[0];
        blue = CC[1];
		MovementZone.Move_Right.arrow = QuinamidChip.getArrow(4);
		MovementZone.Move_Left.arrow = QuinamidChip.getArrow(0);
		MovementZone.Move_Up.arrow = QuinamidChip.getArrow(6);
		MovementZone.Move_Down.arrow = QuinamidChip.getArrow(2);
		
		MovementZone.Rotate_UpRight.arrow = QuinamidChip.getArrow(7);
		MovementZone.Rotate_UpRight.reverseArrow = QuinamidChip.getArrow(3);
		
		MovementZone.Rotate_UpLeft.arrow = QuinamidChip.getArrow(1);
		MovementZone.Rotate_UpLeft.reverseArrow = QuinamidChip.getArrow(5);
		
		MovementZone.Rotate_DownLeft.arrow = QuinamidChip.getArrow(3);
		MovementZone.Rotate_DownLeft.reverseArrow = QuinamidChip.getArrow(7);
		
		MovementZone.Rotate_DownRight.arrow = QuinamidChip.getArrow(5);
		MovementZone.Rotate_DownRight.reverseArrow = QuinamidChip.getArrow(1);
		HelpPanel = CC[HELP_INDEX];
        check_digests(CC);
		}
	}


}
