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
package gipf;

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
public class GipfChip extends chip<GipfChip> implements GipfConstants
{	
	public int drawingIndex;			// index for drawing
	public GColor color = null;			// index for owning player
    static final int FIRST_CHIP_INDEX = 0;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;
    static final int BLACK_CHIP_INDEX = WHITE_CHIP_INDEX+3;
	public int pieceNumber() { return(drawingIndex); }
	static final char WhiteChipName = 'W';
	static final char BlackChipName = 'B';
    static final Random rv = new Random(4953);
	private static DrawableImageStack chips = new DrawableImageStack();

	GipfChip altChip = null;
	GipfChip perspectiveChip = null;
	
	Potential potential = Potential.None;
	
	private GipfChip(GColor c,String na,double []sc,Potential pot,GipfChip pers,GipfChip alt)
	{	color = c;
		file = na;
		potential = pot;
		randomv = rv.nextLong();
		scale = sc;
		perspectiveChip = pers;
		altChip = alt;
		drawingIndex = chips.size();
		chips.push(this);
	}

	static public GipfChip getChip(int n) { return (GipfChip)chips.elementAt(n); }
	public GipfChip perspectiveChip() { return perspectiveChip!=null ? perspectiveChip : this; }
    public GipfChip getAltChip(int cc)
    { 	if(((cc & 2)==0) && (altChip!=null))
    		{
    		return ((cc & 1) !=0) ? altChip.perspectiveChip() : altChip;
    		}
    		else 
    		{
    		return ((cc & 1)!=0) ? perspectiveChip() : this;
    		}
    }
    
    private static double SCALES[][] =
    {	{0.57,0.41,1.63},
    	{0.54,0.43,1.65}   	
    };
    private static double ALTSCALES[][] =
    {	{0.5,0.5,1.3},
    	{0.5,0.5,1.4}   	
    };
	// matrx pieces
    static double bscale[] = new double[]{ 0.5,0.5,1.65};
    static double bscalep[] = new double[]{ 0.5,0.5,1.35};
    static double wscale[] = new double[]{ 0.5,0.5,1.3};
	   
	static GipfChip GipfBlackP = new GipfChip(GColor.B,"black",SCALES[1],Potential.None,null,null);
	static GipfChip GipfWhiteP = new GipfChip(GColor.W,"white",SCALES[0],Potential.None,null,null);

	static GipfChip GipfBlack = new GipfChip(GColor.B,"black-flat",ALTSCALES[1],Potential.None,GipfBlackP,null);
	static GipfChip GipfWhite = new GipfChip(GColor.W,"white-flat",ALTSCALES[0],Potential.None,GipfWhiteP,null);
	
	static GipfChip BlackP = new GipfChip(GColor.B,"matrx-black-perspective", new double[]{ 0.5,0.5,1.4},Potential.None,null,GipfBlack);
	static GipfChip WhiteP = new GipfChip(GColor.W,"matrx-white-perspective",new double[]{ 0.5,0.5,1.29},Potential.None,null,GipfWhite);
	static GipfChip Black = new GipfChip(GColor.B,"matrx-black-flat",bscale,Potential.None,BlackP,GipfBlack);
	static GipfChip White = new GipfChip(GColor.W,"matrx-white-flat",wscale,Potential.None,WhiteP,GipfWhite);
	
	static GipfChip BlackPunctP = new GipfChip(GColor.B,"matrx-black-punct-perspective",bscalep,Potential.Punct,null,null);
	static GipfChip WhitePunctP = new GipfChip(GColor.W,"matrx-white-punct-perspective",new double[]{ 0.5,0.5,1.23},Potential.Punct,null,null);
	static GipfChip BlackPunct = new GipfChip(GColor.B,"matrx-black-punct-flat",bscale,Potential.Punct,BlackPunctP,null);
	static GipfChip WhitePunct = new GipfChip(GColor.W,"matrx-white-punct-flat",wscale,Potential.Punct,WhitePunctP,null);
	
	static GipfChip BlackYinshP = new GipfChip(GColor.B,"matrx-black-yinsh-perspective",new double[]{ 0.5,0.5,1.33},Potential.Yinsh,null,null);
	static GipfChip WhiteYinshP = new GipfChip(GColor.W,"matrx-white-yinsh-perspective",new double[]{ 0.5,0.5,1.2},Potential.Yinsh,null,null);
	static GipfChip BlackYinsh = new GipfChip(GColor.B,"matrx-black-yinsh-flat",bscale,Potential.Yinsh,BlackYinshP,null);
	static GipfChip WhiteYinsh = new GipfChip(GColor.W,"matrx-white-yinsh-flat",wscale,Potential.Yinsh,WhiteYinshP,null);
	
	static GipfChip BlackDvonnP = new GipfChip(GColor.B,"matrx-black-dvonn-perspective",new double[]{ 0.5,0.5,1.45},Potential.Dvonn,null,null);
	static GipfChip WhiteDvonnP = new GipfChip(GColor.W,"matrx-white-dvonn-perspective",new double[]{ 0.5,0.5,1.27},Potential.Dvonn,null,null);
	static GipfChip BlackDvonn = new GipfChip(GColor.B,"matrx-black-dvonn-flat",bscale,Potential.Dvonn,BlackDvonnP,null);
	static GipfChip WhiteDvonn = new GipfChip(GColor.W,"matrx-white-dvonn-flat",wscale,Potential.Dvonn,WhiteDvonnP,null);
	
	static GipfChip BlackZertzP = new GipfChip(GColor.B,"matrx-black-zertz-perspective",bscalep,Potential.Zertz,null,null);
	static GipfChip WhiteZertzP = new GipfChip(GColor.W,"matrx-white-zertz-perspective",new double[]{ 0.5,0.5,1.2},Potential.Zertz,null,null);
	static GipfChip BlackZertz = new GipfChip(GColor.B,"matrx-black-zertz-flat",bscale,Potential.Zertz,BlackZertzP,null);
	static GipfChip WhiteZertz = new GipfChip(GColor.W,"matrx-white-zertz-flat",wscale,Potential.Zertz,WhiteZertzP,null);

	static GipfChip BlackTamskP = new GipfChip(GColor.B,"matrx-black-tamsk-perspective",new double[]{ 0.5,0.5,1.28},Potential.Tamsk,null,null);
	static GipfChip WhiteTamskP = new GipfChip(GColor.W,"matrx-white-tamsk-perspective",new double[]{ 0.5,0.5,1.41},Potential.Tamsk,null,null);
	static GipfChip BlackTamsk = new GipfChip(GColor.B,"matrx-black-tamsk-flat",bscale,Potential.Tamsk,BlackTamskP,null);
	static GipfChip WhiteTamsk = new GipfChip(GColor.W,"matrx-white-tamsk-flat",wscale,Potential.Tamsk,WhiteTamskP,null);

	static GipfChip Chips[][] =
		{	// these are in the ordinal order of the Potential enum
				{White,WhiteTamsk, WhiteZertz, WhiteDvonn, WhiteYinsh, WhitePunct}, 
				{Black,BlackTamsk, BlackZertz, BlackDvonn, BlackYinsh, BlackPunct}, 
		};
	static public GipfChip getChip(int player,Potential potential)
	{
		return Chips[player][potential.ordinal()];
	}
	// call from the viewer's preloadImages
	static boolean imagesLoaded = false;
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(ImageDir,chips);
 		}
	}


}