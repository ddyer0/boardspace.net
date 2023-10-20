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
package crosswords;

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

class ChipStack extends OStack<CrosswordsChip>
{
	public CrosswordsChip[] newComponentArray(int n) { return(new CrosswordsChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */

public class CrosswordsChip extends chip<CrosswordsChip> implements CrosswordsConstants
{
	private int index = 0;
	public static String BACK = "_back_";
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;

	String letter;
	char lcChar;
	int value;
	String tip;
	CrosswordsChip back = null;
	public String contentsString() { return(letter); }
	
	private CrosswordsChip(CrosswordsChip from,char l,int v)
	{	scale = from.scale;
		image = from.image;
		back = from;
		letter = ""+l;
		lcChar = (l>='A' && l<='Z') ? (char)(l+'a'-'A') : l;
		value = v;
		randomv = r.nextLong();
		index = allChips.size();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private CrosswordsChip(String na,double[]sc,String t,int v)
	{	scale=sc;
		tip = t;
		file = na;
		value = v;
		index = allChips.size();
		allChips.push(this);
	}
	public boolean isBlank() { return(value==0); }
	public int chipNumber() { return(index); }
	

    // indexes into the balls array, usually called the rack
    static final CrosswordsChip getChip(int n) { return((CrosswordsChip)allChips.elementAt(n)); }
    
    static CrosswordsChip letters[] = null;		// all the letters, including multiples and blanks
    static CrosswordsChip alphaLetters[] = null;	// just a-z
    static CrosswordsChip getLetter(char l)
    {
    	if(l>='a' && l<='z') { return(alphaLetters[l-'a']); }
    	if(l>='A' && l<='Z') { return(alphaLetters[l-'A']); }
    	return(null);
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public CrosswordsChip backgroundTile = new CrosswordsChip("background-tile-nomask",null,null,0);
    static public CrosswordsChip backgroundReviewTile = new CrosswordsChip("background-review-tile-nomask",null,null,0);
   
    static private double hexScale[] = {0.45,0.5,1.36};
    static private double letterScale[]  = {0.51,0.5,1.45 };
	static private double letter2Scale[] = {0.495,0.5,1.45 };
   	static private double letter0Scale[] = {0.51,0.5,1.45 };
   	static private double letter1Scale[] = {0.51,0.5,1.45 };

    static private double postScale[] = { 0.42,0.5,0.78};
    
    static public CrosswordsChip Tile = new CrosswordsChip("tile",hexScale,null,0);
    static public CrosswordsChip Letter[] = {new CrosswordsChip("letter",letterScale,null,0),
    		new CrosswordsChip("letter0",letter0Scale,null,0),
    		new CrosswordsChip("letter1",letter1Scale,null,0),
    		new CrosswordsChip("letter2",letter2Scale,null,0),
    	};
    static public CrosswordsChip Post = new CrosswordsChip("post",postScale,null,0);
    // greenpost double letter
    static public CrosswordsChip DoubleLetterGreen = new CrosswordsChip("post-green",postScale,DoubleLetter,2);
   
    // yellowpost triple letter
    static public CrosswordsChip TripleLetterYellow = new CrosswordsChip("post-yellow",postScale,TripleLetter,3);
    
    // bluepost double word
    static public CrosswordsChip DoubleWordBlue = new CrosswordsChip("post-blue",postScale,DoubleWord,10);
    
    // redpost triple letter
    static public CrosswordsChip TripleWordRed = new CrosswordsChip("post-red",postScale,TripleWord,15);
    
    private static double dscale[] = {0.5,0.5,1};
    
	public static CrosswordsChip Icon = new CrosswordsChip("icon-nomask",dscale,null,0);

	public static CrosswordsChip NoBackwards = new CrosswordsChip("nobackwards-nomask",dscale,null,0);
	public static CrosswordsChip Backwards = new CrosswordsChip("backwards-nomask",dscale,null,0);
	public static CrosswordsChip Diagonals = new CrosswordsChip("diagonals-nomask",dscale,null,0);
	public static CrosswordsChip NoDiagonals = new CrosswordsChip("nodiagonals-nomask",dscale,null,0);
	public static CrosswordsChip AllConnected = new CrosswordsChip("allconnected-nomask",dscale,null,0);
	public static CrosswordsChip NotConnected = new CrosswordsChip("noconnected-nomask",dscale,null,0);
	public static CrosswordsChip NoDuplicates = new CrosswordsChip("noduplicates-nomask",dscale,null,0);
	public static CrosswordsChip Duplicates = new CrosswordsChip("dups-nomask",dscale,null,0);
	public static CrosswordsChip OpenRacks = new CrosswordsChip("openracks-nomask",dscale,null,0);
	public static CrosswordsChip ClosedRacks = new CrosswordsChip("closedracks-nomask",dscale,null,0);

	public static CrosswordsChip LockRotation = new CrosswordsChip("lock-nomask",dscale,LockMessage,0);
	public static CrosswordsChip UnlockRotation = new CrosswordsChip("unlock-nomask",dscale,UnlockMessage,0);
	
	static {
		Option.Backwards.onIcon = Backwards;
		Option.Backwards.offIcon = NoBackwards;
		Option.Diagonals.onIcon = Diagonals;
		Option.Diagonals.offIcon = NoDiagonals;
		Option.NoDuplicate.onIcon = NoDuplicates;
		Option.NoDuplicate.offIcon = Duplicates;
		Option.Connected.onIcon = AllConnected;
		Option.Connected.offIcon = NotConnected;
		Option.OpenRacks.onIcon = OpenRacks;
		Option.OpenRacks.offIcon = ClosedRacks;
	}
	static public CrosswordsChip assignedBlanks[];
    static private int letterSpecs[][] = {
    		{' ',2,0},
    		
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
    public static CrosswordsChip Blank; 
    public static CrosswordsChip A;
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
		forcan.load_masked_images(Dir,allChips);
    	ChipStack pool = new ChipStack();
    	int nletters = 0;
		for(int []ls : letterSpecs)
		{	char let = (char)ls[0];
			int count = ls[1];
			int value = ls[2];
			CrosswordsChip ch = new CrosswordsChip(Letter[nletters%Letter.length],let,value);
			nletters++;
			for(int i=0;i<count;i++)
			{ pool.push(ch);
			}
		}
		letters = pool.toArray();  	
		alphaLetters = new CrosswordsChip[26];
		for(CrosswordsChip c : letters) { if(c.lcChar>='a') { alphaLetters[c.lcChar-'a'] = c; }}
    	Blank = letters[0];
    	A = letters[3];
    	
    	assignedBlanks = new CrosswordsChip[26];
		for(char ch='A'; ch<='Z'; ch = (char)(ch+1))
		{
			assignedBlanks[ch-'A']=new CrosswordsChip(Blank,ch,0); 
		}
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
    {	//if(lcChar=='o') { /* letter */scale = new double[] {0.51,0.365,1.45 };}
    	//if(lcChar=='i') { /* letter2 */scale = new double[] {0.495,0.365,1.45 };}
       	//if(lcChar=='a') { /* letter0 */scale = new double[] {0.51,0.365,1.45 };}
       	//if(lcChar=='c') { /* letter1 */scale = new double[] {0.51,0.37,1.45 };}
		super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, null);
    	if(BACK.equals(label)) {}
    	else if(letter!=null)
    	{	// draw all letters with the same size
    		
    		Text ww = TextChunk.create("W");
    		int ss = (int)(SQUARESIZE*0.5);
    		if(ss>5)
    		{
    		// display the letter if the tile is not tiny
    		Font f = G.getFont(canvas.labelFont,ss);
    		GC.setFont(gc, f);
    		GC.setFont(gc, ww.selectFontSize(gc, ss,ss));
    		FontMetrics fm = G.getFontMetrics(f);
     		GC.Text(gc, letter, cx-fm.stringWidth(letter)/2,cy+(int)(SQUARESIZE*0.15));
     		if(value!=0)
     		{
    		GC.Text(gc,true,cx+SQUARESIZE/5,cy,SQUARESIZE/4,SQUARESIZE/4,ltblue,null,""+value);
     		}
    		}
    	}
    }
}
