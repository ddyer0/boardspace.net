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
package jumbulaya;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

import lib.DrawableImageStack;
import lib.FontManager;
import lib.GC;
import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import lib.Text;
import lib.TextChunk;
import lib.exCanvas;
import online.game.chip;

class ChipStack extends OStack<JumbulayaChip>
{
	public JumbulayaChip[] newComponentArray(int n) { return(new JumbulayaChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class JumbulayaChip extends chip<JumbulayaChip> implements JumbulayaConstants
{
	 // we happen to know that the difference between lower and upper case
	 // is a single bit, so we can convert known alpha by twiddling the bit
	public static final char U2L = (char)('A'^'a');
	private int index = 0;
	public static String BACK = NotHelp + "_back_";
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public int extendedCharCode() { return(exChar); }
	String letter;
	int exChar;
	String tip;
	JumbulayaChip back = null;
	public String contentsString() { return(letter); }
	/**
	 * make an extended char code from ch and ch2
	 * the result will be one or two lower case characters coded as an integer
	 * @param ch
	 * @param ch2
	 * @return
	 */
	public static int makeExc(int ch,int ch2)
	{
		return ch|U2L|(((ch2==0)?0:(ch2|U2L)<<8));
	}
	private JumbulayaChip(JumbulayaChip from,char l,int sv)
	{	scale = from.scale;
		image = from.image;
		back = from;
		letter = ""+l+((sv==0) ? "" : (char)sv);
		exChar = makeExc(l,sv);		// both characters in lower case
		randomv = r.nextLong();
		index = allChips.size();
		allChips.push(this);
	}
	// used for color chips
	private JumbulayaChip(String na,double[]sc)
	{
		this(na,sc,null);
		randomv = r.nextLong();
	}
	// constructor for all the other random artwork.
	private JumbulayaChip(String na,double[]sc,String t)
	{	scale=sc;
		tip = t;
		file = na;
		randomv = r.nextLong();
		index = allChips.size();
		allChips.push(this);
	}
	public int chipNumber() { return(index); }
	

    // indexes into the balls array, usually called the rack
    static final JumbulayaChip getChip(int n) { return((JumbulayaChip)allChips.elementAt(n)); }
    
    static JumbulayaChip letters[] = null;		// all the letters, including multiples and blanks
    static JumbulayaChip alphaLetters[] = null;	// just a-z
    static JumbulayaChip getLetter(char l)
    {
    	if(l>='a' && l<='z') { return(alphaLetters[l-'a']); }
    	if(l>='A' && l<='Z') { return(alphaLetters[l-'A']); }
    	return(null);
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public JumbulayaChip backgroundTile = new JumbulayaChip("background-tile-nomask",null,null);
    static public JumbulayaChip backgroundReviewTile = new JumbulayaChip("background-review-tile-nomask",null,null);
   
    static private double letterScale[]  = {0.58,0.45,1.24 };
	static private double letter2Scale[] = {0.62,0.48,1.24 };
   	static private double letter0Scale[] = {0.62,0.48,1.24 };
   	static private double letter1Scale[] = {0.62,0.48,1.24 };

    static private double postScale[] = { 0.62,0.44,1.28};
    
    static public JumbulayaChip Letter[] = 
    	{	new JumbulayaChip("letter",letterScale,null),
    		new JumbulayaChip("letter0",letter0Scale,null),
    		new JumbulayaChip("letter1",letter1Scale,null),
    		new JumbulayaChip("letter2",letter2Scale,null),
    	};
    // greenpost double letter
    static public JumbulayaChip Blue = new JumbulayaChip("post-blue",new double[]{ 0.62,0.44,1.38},"blue");
    static public JumbulayaChip Yellow = new JumbulayaChip("post-yellow",new double[]{ 0.62,0.44,1.22},"yellow");
    static public JumbulayaChip Green = new JumbulayaChip("post-green",postScale,"green");
    static public JumbulayaChip Red = new JumbulayaChip("post-red",postScale,"red");
    static public JumbulayaChip Colors[] = {
    		 Red,
    		 Green,
    		 Blue,
    		 Yellow};
    
	public static JumbulayaChip findColor(String colorName) {
		for(JumbulayaChip co : Colors)
		{
			if(co.tip.equalsIgnoreCase(colorName)) { return(co); }
		}
		return(null);
	}
	
    private static double dscale[] = {0.5,0.5,1};
    
	public static JumbulayaChip Icon = new JumbulayaChip("icon-nomask",dscale,null);

	public static JumbulayaChip Board = new JumbulayaChip("board",dscale,null);
	
	public static JumbulayaChip OpenRacks = new JumbulayaChip("openracks-nomask",dscale,null);
	public static JumbulayaChip ClosedRacks = new JumbulayaChip("closedracks-nomask",dscale,null);

	public static JumbulayaChip LockRotation = new JumbulayaChip("lock-nomask",dscale,LockMessage);
	public static JumbulayaChip UnlockRotation = new JumbulayaChip("unlock-nomask",dscale,UnlockMessage);
	
	static {
		Option.OpenRacks.onIcon = OpenRacks;
		Option.OpenRacks.offIcon = ClosedRacks;
	}
    static private int letterSpecs[][] = {
    		{'A',6,0},
    		{'E',9,0},
    		{'I',6,0},
    		{'O',6,0},
    		{'N',5,0},
    		{'R',6,0},
    		{'T',5,0},
    		{'L',5,0},
    		{'S',5,0},
       		{'U',3,0},
       	    		
       		{'D',4,0},
       		{'G',3,0},
       		
       		{'B',3,0},
       		{'C',4,0},
       		{'M',4,0},
       		{'P',3,0},
       		
       		{'F',3,0},
       		{'H',3,0},
       		{'V',1,0},
       		{'W',2,0},
       		{'Y',2,0},
       		
       		{'K',2,0},
       		
       		{'J',1,0},
       		{'X',1,0},
       		
       		{'Z',1,0},
       		
      		{'Q',1,'U'},	// Qu
      		{'C',1,'H'},	// Ch
      		{'E',1,'D'},	// ed
      		{'E',1,'R'},	// Er
      		{'L',1,'Y'},	// Ly
      		{'S',1,'T'},	// St
      		{'T',1,'H'},	// Th
      		 
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
		forcan.load_masked_images(Dir,allChips);
    	ChipStack pool = new ChipStack();
    	int nletters = 0;
		for(int []ls : letterSpecs)
		{	char let = (char)ls[0];
			int count = ls[1];
			int second = ls[2];
			JumbulayaChip ch = new JumbulayaChip(Letter[nletters%Letter.length],let,second);
			nletters++;
			for(int i=0;i<count;i++)
			{ pool.push(ch);
			}
		}
		letters = pool.toArray();  	
		alphaLetters = new JumbulayaChip[26];
		for(JumbulayaChip c : letters) 
			{ int cc = c.extendedCharCode();
			  if((cc>='a') && cc<='z')
				{ alphaLetters[cc-'a'] = c; }}
		Image.registerImages(allChips);
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
    		int ss = (int)(SQUARESIZE*0.50);
    		if(ss>5)
    		{
    		// display the letter if the tile is not tiny
    		Font f = FontManager.getFont(canvas==null ? GC.getFont(gc) : canvas.labelFont,ss);
    		GC.setFont(gc, f);
    		FontMetrics fm = FontManager.getFontMetrics(f);
    		Text ww = TextChunk.create("W");
     		GC.setFont(gc, ww.selectFontSize(gc, ss,ss));
     		GC.Text(gc, letter, cx-fm.stringWidth(letter)/2-ss/10,cy+(int)(SQUARESIZE*0.2));
    		}	
    	}
    }

}
