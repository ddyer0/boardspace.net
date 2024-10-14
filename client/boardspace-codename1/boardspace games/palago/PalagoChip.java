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
package palago;

import lib.Image;
import lib.ImageLoader;
import bridge.Color;

import online.game.*;
import lib.Random;

public class PalagoChip extends chip<PalagoChip>
{
	public int index = 0;
	int lineExit[] = null;	// exits for the entries of lines
	int lineColor[] = null;	// color of the enclosed space
	int partialLineColor[]=null;
	public int nextInLine(int entered_from)	{ return(lineExit[entered_from]); 	}
	public int lineColorCode(int entry) { return(lineColor[entry]); }
	public int partialLineColorCode(int entry) { return(partialLineColor[entry]); }
	int mainLine = -1;		// entry direction for the outer line
	int blueLine = -1;		// entry direction for the line that encloses blue with the main line
	int yellowLine = -1;	// entry direction for the line that encloses yellow with the main line
	public String name = "";
	// constructor
	
	private PalagoChip(int i0,Image im,String na,double[]sc,long ran,String nam)
	{	index = i0;
		scale=sc;
		image=im;
		file = na;
		randomv = ran;
		name = nam;
		int idx = i0%nChips;
		lineExit = exits[idx];
		lineColor = colors[idx];
		partialLineColor = partialColors[idx];
		mainLine = mainlines[idx];
		yellowLine = yellowlines[idx];
		blueLine = bluelines[idx];
		
	}

	public PalagoChip getAltChip(int set)
	{	return( (this==logo)?this:(this==blank)?getChip(index+set):getChip(index+nChips*set));
	}
	public static int yellow_loop_code = 1;				// yellow corner is part of a loop
	public static int center_loop_code = 2;				// tile is part of a center loop
	public static int blue_loop_code = 4;				// blue corner is part of a loop
	public static int incomplete_center_code = 8;		// tile has been scanned from a center line
	public static int incomplete_blue_code = 16;		// blue corner was reached on a scan
	public static int incomplete_yellow_code = 32;		// yellow corner was reached on a scan
	public static int partial_blue_code = 64;		// blue corner scanned from a non-loop
	public static int partial_yellow_code = 128;	// yellow corner scanned from a non-loop
	public static int partial_center_code = 256;	// center line scanned from a non-loop (not used?)
	
	public int chipNumber() { return(index); }
	static private int exits[][] = {
		// tile 0
		 {	
			 3,	// 0->3
			 2,	// 1->2
			 1,	// 2->1
			 0,	// 3->0
			 5,	// 4->5
			 4	// 5->4		 
		 },
		// tile 1
		 {
			 1,	// 0->1
			 0, // 1->0
			 5,	// 2->5
			 4,	// 3->4
			 3,	// 4->3
			 2},// 5->2;
		 // tile 2
		 {	5,	// 0->5
			4,	// 1->4
			3,	// 2->3
			2,	// 3->2
			1,	// 4->1
			0	// 5->0
		 }
	};
	static private int colors[][] = {
		{
			center_loop_code,yellow_loop_code,yellow_loop_code,
			center_loop_code,blue_loop_code,blue_loop_code			
			},
			{
				blue_loop_code,blue_loop_code,
				center_loop_code,yellow_loop_code,
				yellow_loop_code,center_loop_code
				},
		{
		yellow_loop_code,center_loop_code,blue_loop_code,
		blue_loop_code,center_loop_code,yellow_loop_code
			
		}};
	static private int partialColors[][] = {
		{
			partial_center_code,partial_yellow_code,partial_yellow_code,
			partial_center_code,partial_blue_code,partial_blue_code			
			},
			{
				partial_blue_code,partial_blue_code,
				partial_center_code,partial_yellow_code,
				partial_yellow_code,partial_center_code
				},
		{
		partial_yellow_code,partial_center_code,partial_blue_code,
		partial_blue_code,partial_center_code,partial_yellow_code
			
		}};
	static private final int mainlines[] = { 0, 2, 1};
	static private final int yellowlines[] = { 1, 4, 0};
	static private final int bluelines[] = { 5, 0, 2};
	
	static final int blank_index = 9;
	static final double[][] SCALES=
    {   	// left
    	{0.52,0.5,1.77},	// left aqua
    	{0.469,0.50,2.0},	// right aqua
    	{0.5,0.5,1.73},		// down aqua
    	
    	{0.5,0.5,1.74},		// left red
    	{0.48,0.47,1.941},	// right red
    	{0.48,0.48,1.7},	// down red
  	
    	{0.52,0.5,1.666},	// left blue
    	{0.543,0.452,1.85},	// right blue
    	{0.559,0.476,1.73},	// down blue
    	
    	
      	{0.47,0.5,1.93},	// blank aqua
     	{0.50,0.50,1.84},	// blank red
      	{0.6,0.47,1.65},	// blank blue

    	
    	{0.5,0.5,1.8},		// logo
      };
    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
	static final int left_index = 0;
	static final int right_index = 1;
	static final int down_index = 2;
	static PalagoChip Left = null;
	static PalagoChip Right = null;
	static PalagoChip Down = null;
    static final String[] ImageNames = 
        {   "aqua-left", 
            "aqua-right",
            "aqua-down",
              
            "red-left", 
            "red-right",
            "red-down",
            
            "blue-left", 
            "blue-right",
            "blue-down",
            
            "aqua-blank",
            "red-blank",
            "blue-blank",
                      
            "logo",
        };
    public static Color ChipColor[][] = 
    	{	// light			dark
    	{new Color(226,237,236),new Color(91,242,255)},
    	{new Color(224,245,167),new Color(179,33,44)},
    	{new Color(214,220,61),new Color(22,77,237)}
};
    static final String names[]={"L","R","C","L","R","C","L","R","C","blank aqua","blank red","blank blue","logo"};
    
    public String chipName() { return(name); }
	// call from the viewer's preloadImages
    static PalagoChip CANONICAL_PIECE[] = null;
    static final PalagoChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    
    static final int nChips = 3;
    static PalagoChip logo = null;
    static PalagoChip blank = null;
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(CANONICAL_PIECE==null)
		{
		int nImages = ImageNames.length;
		Random rv = new Random(5312324);
        Image IM[]=forcan.load_masked_images(Dir,ImageNames);
        PalagoChip CC[] = new PalagoChip[nImages];
        for(int i=0;i<nImages;i++) 
        	{CC[i]=new PalagoChip(i,IM[i],ImageNames[i],SCALES[i],rv.nextLong(),names[i]); 
        	}
        Left = CC[0];
        Right = CC[1];
        Down = CC[2];
        blank = CC[blank_index];
        logo = CC[nImages-1];	// last is the logo
        check_digests(CC);
        Image.registerImages(CC);
        CANONICAL_PIECE = CC;
		}
	}   
	

}
