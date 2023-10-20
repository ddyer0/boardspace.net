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
package mogul;

import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.Random;
import lib.StockArt;
import lib.exCanvas;
import online.game.chip;
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class MogulChip extends chip<MogulChip>
{	
	private int cardIndex;
	private colors background = null;
	private colors border = null;
	private boolean starting = false;
	public int getBackgroundIndex() { return(background.ordinal()); }
	public int getBorderIndex() { return(border==null?-1:border.ordinal()); }
	public String getBorderColor() { return(border==null)?"none":border.toString(); }
	public String getBackgroundColor() { return(background==null)?"none":background.toString(); }

	private StockArt art = null;
	private static StockArt cards[] = null;
	private static StockArt centers[] = null;
	private static StockArt borders[] = null;
	private static StockArt chips[] = null;
	private static StockArt misc[] = null;
	public static MogulChip cardBack = null;
	public boolean isCard() { return((cardIndex>=firstCard) && cardIndex<(firstCard+nCards)); }
	public boolean isPokerChip() { return(this==pokerChip); }
	public boolean isStartingCard()
	{	return(starting);
	}
	public int chipNumber() { return(cardIndex); }
	public String contentsString() { return(toString()); }
	public void drawChip(Graphics gc, exCanvas canvas, int SQUARESIZE, double xscale, int cx, int cy, java.lang.String label)
	{
		if(art!=null) { art.drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,label); }
		else
		{	// cards are constructed from 3 pieces
			crashCard.drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,null);
			centers[background.ordinal()].drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,label);
			borders[border.ordinal()].drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,null);
		}
	}


	private MogulChip(int index,StockArt im,long rv,double sc[])
	{	cardIndex=index;
		art = im;
		randomv = rv;
		scale = sc;
	}
	private MogulChip(int index,colors[]color,long rv,double sc[])
	{	
		cardIndex=index;
		scale = sc;
		if(color!=null) 
		{	background = color[0];
			border = color[1];
		}
		randomv = rv;
	}
	public String toString()
	{	return(art==null ? "<"+background+" "+border+">" : "<"+art+">");
	}

		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private MogulChip CANONICAL_PIECE[] = null;	// created by preload_images

 
	public static MogulChip getCard(int ind)
	{	return((ind>=0 && ind<nCards) ? CANONICAL_PIECE[ind+firstCard] : null);
	}
	public static MogulChip getPlayerChip(int ind)
	{	return(CANONICAL_PIECE[playerChipOffset+ind]);
	}
	public boolean isPlayerChip()
		{ return((cardIndex>=playerChipOffset)&&(cardIndex<(playerChipOffset+chips.length))); 
		}

	public static MogulChip getChip(int ind)
	{	return(CANONICAL_PIECE[ind]);
	}
  /* pre load images and create the canonical pieces
   * 
   */
	static final String chip_names[] = {"yellow-chip","orange-chip","blue-chip","purple-chip","green-chip","red-chip"};
	static final String misc_names[] = {"poker-chip"};
	static final String card_names[] = {"back","crash"};
	static final String center_names[] = {"brown","green","yellow","blue","pink"};
	static final String border_names[] =
		{"brown-border",
		 "green-border",
		 "yellow-border",
		 "blue-border",
		 "pink-border"};
	
	enum colors  {brown,green,yellow,blue,pink};
	static int startingCards[] = {0,2,4,5,6,7};	// starting cards are brown, one with yellow,green, two with blue and pink
	static int nColors = colors.values().length;
	static MogulChip crashCard = null;
	static MogulChip pokerChip = null;
	static int playerChipOffset = 0;
	// the deck contains 8 brown,7 pink, 6 blue, 5 green, 5 yellow, cards
	// the deck borders are 5 brown, 6 pink, 7 blue, 6 green, 6 yellow
	static final colors[][] specs =
	{	{colors.brown,colors.green},{colors.brown,colors.green},
		{colors.brown,colors.yellow},{colors.brown,colors.yellow},
		{colors.brown,colors.blue},{colors.brown,colors.blue},
		{colors.brown,colors.pink},{colors.brown,colors.pink},
		
		{colors.green,colors.pink},{colors.green,colors.pink},
		{colors.green,colors.yellow},
		{colors.green,colors.blue},
		{colors.green,colors.brown},
		
		{colors.yellow,colors.blue},{colors.yellow,colors.blue},
		{colors.yellow,colors.pink},
		{colors.yellow,colors.green},
		{colors.yellow,colors.brown},
		
		{colors.blue,colors.green},{colors.blue,colors.green},
		{colors.blue,colors.pink},{colors.blue,colors.pink},
		{colors.blue,colors.yellow},
		{colors.blue,colors.brown},
		
		{colors.pink,colors.yellow},{colors.pink,colors.yellow},
		{colors.pink,colors.blue},{colors.pink,colors.blue},
		{colors.pink,colors.brown},{colors.pink,colors.brown},
		{colors.pink,colors.green}
	};
	static int nCards = specs.length;
	static int firstCard = 2;
	static double scales[][] = {{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},
								{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0}};
   
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(pokerChip==null)
		{
		misc = StockArt.preLoadArt(forcan,ImageDir,misc_names,scales);
		
		Image chip_mask = forcan.load_image(ImageDir,"chip-mask");
		Image[] chipImages = forcan.load_images(ImageDir,chip_names,chip_mask);
		chips = StockArt.preLoadArt(chip_names,chipImages,scales);
		
		Image cardMask = forcan.load_image(ImageDir,"card-mask");
		Image[] cardImages = forcan.load_images(ImageDir,card_names,cardMask);
		cards = StockArt.preLoadArt(card_names,cardImages,scales);
		
		Image centerMask = forcan.load_image(ImageDir,"center-mask");
		Image[] centerImages = forcan.load_images(ImageDir,center_names,centerMask);
		centers = StockArt.preLoadArt(center_names,centerImages,scales);
		
		Image borderMask = forcan.load_image(ImageDir,"border-mask");
		Image[] borderImages = forcan.load_images(ImageDir,border_names,borderMask);
		borders = StockArt.preLoadArt(border_names,borderImages,scales);
		
		Random r = new Random(63467345);
		int ind = 0;
        CANONICAL_PIECE = new MogulChip[specs.length+2+chip_names.length+misc_names.length];	// the deck plus back and crash
        cardBack = CANONICAL_PIECE[ind] = new MogulChip(ind,cards[0],r.nextLong(),scales[0]);	// card back
        ind++;
        CANONICAL_PIECE[ind] = crashCard = new MogulChip(ind,cards[1],r.nextLong(),scales[0]);	// crash card
        firstCard = ind+1;
        for(colors[] spec : specs)
        {	ind++;
        	CANONICAL_PIECE[ind] = new MogulChip(ind,spec,r.nextLong(),scales[0]);
        }
        
        for(StockArt m : misc) { ind++; CANONICAL_PIECE[ind] = new MogulChip(ind,m,r.nextLong(),scales[0]); }
        MogulChip pchip = CANONICAL_PIECE[ind];
        playerChipOffset = ind+1;
        for(StockArt c : chips) { ind++; CANONICAL_PIECE[ind] = new MogulChip(ind,c,r.nextLong(),scales[0]); }

        check_digests(CANONICAL_PIECE);
        for(int i : startingCards) { getCard(i).starting = true; }
        pokerChip = pchip;
		}
	}


}
