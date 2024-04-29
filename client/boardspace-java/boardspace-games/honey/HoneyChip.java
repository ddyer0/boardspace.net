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
package honey;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

import lib.DrawableImageStack;
import lib.G;
import lib.GC;
import lib.Graphics;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import lib.Text;
import lib.TextChunk;
import lib.exCanvas;
import online.game.chip;

class ChipStack extends OStack<HoneyChip>
{
	public HoneyChip[] newComponentArray(int n) { return(new HoneyChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */

public class HoneyChip extends chip<HoneyChip> implements HoneyConstants
{

	private int index = 0;
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;

	String letter;
	char lcChar;
	int value;
	String tip;
	HoneyChip back = null;
	public String contentsString() { return(letter); }
	
	private HoneyChip(HoneyChip from,char l,int v)
	{	scale = from.scale;
		image = from.image;
		back = from;
		letter = ""+l+((l=='Q') ? "U" : "");
		
		lcChar = (l>='A' && l<='Z') ? (char)(l+'a'-'A') : l;
		value = v;
		randomv = r.nextLong();
		index = allChips.size();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private HoneyChip(String na,double[]sc,String t,int v)
	{	scale=sc;
		tip = t;
		file = na;
		value = v;
		index = allChips.size();
		allChips.push(this);
	}
	public int chipNumber() { return(index); }
	

    // indexes into the balls array, usually called the rack
    static final HoneyChip getChip(int n) { return((HoneyChip)allChips.elementAt(n)); }
    
    static HoneyChip letters[] = null;		// all the letters, including multiples and blanks
    static HoneyChip alphaLetters[] = null;	// just a-z
    static HoneyChip getLetter(char l)
    {
    	if(l>='a' && l<='z') { return(alphaLetters[l-'a']); }
    	if(l>='A' && l<='Z') { return(alphaLetters[l-'A']); }
    	return(null);
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public HoneyChip backgroundTile = new HoneyChip("background-tile-nomask",null,null,0);
    static public HoneyChip backgroundReviewTile = new HoneyChip("background-review-tile-nomask",null,null,0);
    static public HoneyChip SummarySheet = new HoneyChip("words-nomask",new double[]{0.5,0.5,1.0},null,0);
    
   static private double letterScale[]  = new double[] {0.55,0.52,1.25 };

   static public HoneyChip Letter[] = {new HoneyChip("light-1",letterScale,null,0),
   		new HoneyChip("light-2",letterScale,null,0),
   		new HoneyChip("light-3",letterScale,null,0),
   		new HoneyChip("light-4",letterScale,null,0),
   	};
   
    private static double dscale[] = {0.5,0.5,1};
    
	public static HoneyChip Icon = new HoneyChip("honey-icon-nomask",dscale,null,0);

    static private int letterSpecs[][] = {   		
    		{'A',9,1},
    		{'E',12,1},
    		{'I',9,1},
    		{'O',8,1},
    		{'N',6,1},
    		{'R',6,1},
    		{'T',6,1},
    		{'L',4,1},
    		{'S',4,1},
       		{'U',4,1},
       	    		
       		{'D',4,2},
       		{'G',3,2},
       		
       		{'B',2,3},
       		{'C',2,3},
       		{'M',2,3},
       		{'P',2,3},
       		
       		{'F',2,4},
       		{'H',2,4},
       		{'V',2,4},
       		{'W',2,4},
       		{'Y',2,4},
       		
       		{'K',1,5},
       		
       		{'J',1,8},
       		{'X',1,8},
       		
       		{'Q',1,10},
       		{'Z',1,10},
    };
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
    	ChipStack pool = new ChipStack();
    	int nletters = 0;
		for(int []ls : letterSpecs)
		{	char let = (char)ls[0];
			int count = ls[1];
			int value = ls[2];
			HoneyChip ch = new HoneyChip(Letter[nletters%Letter.length],let,value);
			nletters++;
			for(int i=0;i<count;i++)
			{ pool.push(ch);
			}
		}
		letters = pool.toArray();  	
		alphaLetters = new HoneyChip[26];
		for(HoneyChip c : letters) { if(c.lcChar>='a') { alphaLetters[c.lcChar-'a'] = c; }}
 		}

	}   
	Color ltblue = new Color(100,100,250);
		
	public void drawChip(Graphics gc,
            exCanvas canvas,
            int SQUARESIZE,
            double xscale,
            int cx,
            int cy,
            java.lang.String label)
    {	
		super.drawChip(gc, canvas, SQUARESIZE*6/5, xscale, cx, cy, null);
		if(letter!=null)
    	{	// draw all letters with the same size
    		
    		Text ww = TextChunk.create("W");
    		int ss = (int)(SQUARESIZE*0.6);
    		if(ss>5)
    		{
    		// display the letter if the tile is not tiny
    		Font f = G.getFont(canvas.labelFont,ss);
    		GC.setFont(gc, f);
    		GC.setFont(gc, ww.selectFontSize(gc, ss,ss));
    		FontMetrics fm = G.getFontMetrics(f);
     		GC.Text(gc, letter, (int)(cx-fm.stringWidth(letter)*0.6),cy+(int)(SQUARESIZE*0.15));
     		if(value!=0)
     		{
    		GC.Text(gc,true,cx+SQUARESIZE/10,cy+SQUARESIZE/5,SQUARESIZE/5,SQUARESIZE/5,ltblue,null,""+value);
     		}
    		}
    	}
    }
}
