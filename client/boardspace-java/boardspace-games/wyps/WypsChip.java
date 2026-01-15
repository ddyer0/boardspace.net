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
package wyps;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

import bridge.SystemFont;
import common.CommonConfig;
import lib.DrawableImageStack;
import lib.GC;
import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;
import lib.exCanvas;
import online.game.chip;

class ChipStack extends OStack<WypsChip>
{
	public WypsChip[] newComponentArray(int n) { return(new WypsChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class WypsChip extends chip<WypsChip> implements WypsConstants,CommonConfig
{	
	public static String BACK = NotHelp + "_back_";
	public static String SELECT = "_select_";
	private int index = 0;
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	
	private static boolean imagesLoaded = false;
	
	String letter;
	WypsChip altChip = null;
	char lcChar;
	String tip;
	WypsChip back = null;
	WypsColor color = null;
	public WypsColor getColor() { return(color); }
	public WypsChip getAltChip() { return(altChip); }
	public WypsChip getAltChip(WypsColor c) { return((color==c) ? this : altChip); }
	public String contentsString() { return(letter); }
	
	private WypsChip(WypsChip from,char l)
	{	scale = from.scale;
		image = from.image;
		back = from;
		altChip = this;
		letter = ""+l;
		lcChar = (l>='A' && l<='Z') ? (char)(l+'a'-'A') : l;
		randomv = r.nextLong();
		index = otherChips.size();
		otherChips.push(this);
	}
	
	// constructor for all the other shared stone artwork.
	private WypsChip(String na, double []sc)
	{
		scale = sc;
		file = na;
		stoneChips.push(this);
	}
	private WypsChip(String na,double[]sc,String t)
	{	scale=sc;
		tip = t;
		file = na;
		altChip = this;
		index = otherChips.size();
		otherChips.push(this);
	}
	public int chipNumber() { return(index); }
	

    // indexes into the balls array, usually called the rack
    static final WypsChip getChip(int n) { return((WypsChip)otherChips.elementAt(n)); }
    
    static WypsChip letters[] = null;		// all the letters, including multiples and blanks
    static WypsChip alphaLetters[] = null;	// just a-z
    static WypsChip getLetter(char l)
    {
    	if(l>='a' && l<='z') { return(alphaLetters[l-'a']); }
    	if(l>='A' && l<='Z') { return(alphaLetters[l-'A']); }
    	return(null);
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public WypsChip backgroundTile = new WypsChip("background-tile-nomask",null,null);
    static public WypsChip backgroundReviewTile = new WypsChip("background-review-tile-nomask",null,null);
   
     static private double letterScale[]  = new double[] {0.5,0.52,1.1 };

  
     //  static private double hexScaleNR[] = {0.50,0.50,1.6};
     static private double hexScaleNR[] = {0.50,0.50,1.53};

    static public WypsChip Tile = new WypsChip("hextile-nr",hexScaleNR);
    static public WypsChip hexTileNR_1 = new WypsChip("hextile-nr-1",hexScaleNR);
    static public WypsChip hexTileNR_2 = new WypsChip("hextile-nr-2",hexScaleNR);
    static public WypsChip hexTileNR_3 = new WypsChip("hextile-nr-3",hexScaleNR);

    static {
       	Tile.alternates = new WypsChip[] {Tile,hexTileNR_1,hexTileNR_2,hexTileNR_3};
    }


    static public WypsChip lightLetter[] = {new WypsChip("light-1",letterScale,null),
    		new WypsChip("light-2",letterScale,null),
    		new WypsChip("light-3",letterScale,null),
    		new WypsChip("light-4",letterScale,null),
    	};
    static public WypsChip darkLetter[] = {new WypsChip("dark-1",letterScale,null),
    		new WypsChip("dark-2",letterScale,null),
    		new WypsChip("dark-3",letterScale,null),
    		new WypsChip("dark-4",letterScale,null),
    	};
  
    private static double dscale[] = {0.5,0.5,1};
    
	public static WypsChip Icon = new WypsChip("icon-nomask",dscale,null);

	public static WypsChip LockRotation = new WypsChip("lock-nomask",dscale,LockMessage);
	public static WypsChip UnlockRotation = new WypsChip("unlock-nomask",dscale,UnlockMessage);
	
 	
    static private int letterSpecs[][] = {
    		
       		{'E',12,1},
       		{'A',7,1},
    		{'R',7,1},
    		{'S',7,1},
    		
    		{'I',6,1},
    		{'T',6,1},

    		{'L',5,1},
    		{'N',5,1},
       		{'O',5,1},
     	    		
       		{'D',4,2},
       		
       		{'C',3,3},
      		{'G',3,2},
      		{'U',3,1},
      		
       		{'F',2,4},
       		{'H',2,4},
      		{'M',2,3},
    		{'P',2,3},
      		{'Y',2,4},
      		 
       		{'B',1,3},
     		{'J',1,8},
       		{'K',1,5},
      		{'Q',1,10},   		        		
       		{'V',1,4},
       		{'W',1,4},  		
       		{'X',1,8},
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
		forcan.load_masked_images(StonesDir, stoneChips);
		forcan.load_masked_images(Dir,otherChips);
    	ChipStack pool = new ChipStack();
    	int nletters = 0;
		for(int []ls : letterSpecs)
		{	char let = (char)ls[0];
			int count = ls[1];
			WypsChip light = new WypsChip(lightLetter[nletters%lightLetter.length],let);
			WypsChip dark = new WypsChip(darkLetter[nletters%darkLetter.length],let);
			light.altChip = dark;
			light.color = WypsColor.Light;
			dark.altChip = light;
			dark.color = WypsColor.Dark;
			nletters++;
			for(int i=0;i<count;i++)
			{ pool.push(light);
			}
		}
		letters = pool.toArray();  	
		alphaLetters = new WypsChip[26];
		for(WypsChip c : letters) { if(c.lcChar>='a') { alphaLetters[c.lcChar-'a'] = c; }}
		Image.registerImages(stoneChips);
		Image.registerImages(otherChips);
		imagesLoaded = true;
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
    {	//if(lcChar=='o') { /* letter */scale = new double[] {0.5,0.52,1.1 };}
    	//if(lcChar=='i') { /* letter2 */scale = new double[] {0.495,0.365,1.45 };}
       	//if(lcChar=='a') { /* letter0 */scale = new double[] {0.51,0.365,1.45 };}
       	//if(lcChar=='c') { /* letter1 */scale = new double[] {0.51,0.37,1.45 };}
		super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, null);
    	if(BACK.equals(label) || letter==null) {}
    	else if(letter!=null)
    	{	// draw all letters with the same size
    		Text ww = TextChunk.create("W");
    		int ss = (int)(SQUARESIZE*0.6);
    		if(ss>5)
    		{
    		// displau the letter if the tile is not tiny
    		if(SELECT.equals(label))
    		{
    			StockArt.SmallO.drawChip(gc, canvas, ss*5, cx+ss/14,cy-ss/12,null);
    		}

    		Font f = SystemFont.getFont(canvas.labelFont,ss);
    		GC.setFont(gc, f);
    		GC.setFont(gc, ww.selectFontSize(gc, ss,ss));
    		FontMetrics fm = lib.FontManager.getFontMetrics(f);
     		GC.Text(gc,letter, cx-fm.stringWidth(letter)/2,cy+(int)(SQUARESIZE*0.2));
    		}
     	}
    }
}
