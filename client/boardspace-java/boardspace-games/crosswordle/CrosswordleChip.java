/* copyright notice */package crosswordle;

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
import online.common.exCanvas;
import online.game.chip;

class ChipStack extends OStack<CrosswordleChip>
{
	public CrosswordleChip[] newComponentArray(int n) { return(new CrosswordleChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */

public class CrosswordleChip extends chip<CrosswordleChip> implements CrosswordleConstants
{
	private int index = 0;
	public static String BACK = "_back_";
	public static String YELLOW = "_yellow_";
	public static String GREEN = "_green_";
	public static String NEWGREEN = "_newgreen_";
	public static String NEWYELLOW = "_newyellow_";
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;

	String letter;
	char lcChar;
	String overlay;
	CrosswordleChip back = null;
	LetterColor id = null;
	public String contentsString() { return(letter); }
	
	private CrosswordleChip(CrosswordleChip from,char l)
	{	scale = from.scale;
		image = from.image;
		back = from;
		letter = ""+l;
		lcChar = (l>='A' && l<='Z') ? (char)(l+'a'-'A') : l;
		randomv = r.nextLong();
		index = allChips.size();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private CrosswordleChip(String na,double[]sc,String t,LetterColor v)
	{	scale=sc;
		overlay = t;
		file = na;
		if(v!=null) { v.chip = this; }
		id = v;
		index = allChips.size();
		allChips.push(this);
	}

	public int chipNumber() { return(index); }
	

    // indexes into the balls array, usually called the rack
    static final CrosswordleChip getChip(int n) { return((CrosswordleChip)allChips.elementAt(n)); }
    
    static CrosswordleChip letters[] = null;		// all the letters, including multiples and blanks
    static CrosswordleChip alphaLetters[] = null;	// just a-z
    static CrosswordleChip getLetter(char l)
    {
    	if(l>='a' && l<='z') { return(alphaLetters[l-'a']); }
    	if(l>='A' && l<='Z') { return(alphaLetters[l-'A']); }
    	return(null);
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public CrosswordleChip backgroundTile = new CrosswordleChip("background-tile-nomask",null,null,null);
    static public CrosswordleChip backgroundReviewTile = new CrosswordleChip("background-review-tile-nomask",null,null,null);
    static public CrosswordleChip stats = new CrosswordleChip("stats-nomask",new double[] {0.5,0.5,1.0},null,null);
    // + x moves to the left
    // + y moves up
    static private double letterScale[]  = {0.490,0.45,1.30 };	// 
	static private double letter2Scale[] = {0.51,0.45,1.35 };
   	static private double letter0Scale[] = {0.52,0.465,1.3 };	//
   	static private double letter1Scale[] = {0.52,0.46,1.3 };	// 

    
    static public CrosswordleChip Letter[] = {new CrosswordleChip("letter",letterScale,BACK,LetterColor.Blank),
    		new CrosswordleChip("letter0",letter0Scale,BACK,LetterColor.Blank),
    		new CrosswordleChip("letter1",letter1Scale,BACK,LetterColor.Blank),
    		new CrosswordleChip("letter2",letter2Scale,BACK,LetterColor.Blank),
    	};

    static CrosswordleChip yellowWash = new CrosswordleChip("yellowwash",new double[] {0.40,0.50,0.85},YELLOW,LetterColor.Yellow);
    static CrosswordleChip greenWash = new CrosswordleChip("greenwash",new double[] {0.40,0.50,0.85},GREEN,LetterColor.Green);
    static CrosswordleChip newyellowWash = new CrosswordleChip("newyellowwash",new double[] {0.40,0.50,0.85},NEWYELLOW,LetterColor.NewYellow);
    static CrosswordleChip newgreenWash = new CrosswordleChip("newgreenwash",new double[] {0.40,0.50,0.85},NEWGREEN,LetterColor.NewGreen);
    static CrosswordleChip logo = new CrosswordleChip("logo",new double[] {0.5,0.5,1},null,null);
    
    private static double dscale[] = {0.5,0.5,1};
    
	public static CrosswordleChip Icon = new CrosswordleChip("icon-nomask",dscale,null,null);


	public static CrosswordleChip LockRotation = new CrosswordleChip("lock-nomask",dscale,LockMessage,null);
	public static CrosswordleChip UnlockRotation = new CrosswordleChip("unlock-nomask",dscale,UnlockMessage,null);
	
	static public CrosswordleChip assignedBlanks[];
	public boolean isBlank() { return (this==Blank); }
	
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
    public static CrosswordleChip Blank; 
    public static CrosswordleChip A;
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
			CrosswordleChip ch = new CrosswordleChip(Letter[nletters%Letter.length],let);
			nletters++;
			for(int i=0;i<count;i++)
			{ pool.push(ch);
			}
		}
		letters = pool.toArray();  	
		alphaLetters = new CrosswordleChip[26];
		for(CrosswordleChip c : letters) { if(c.lcChar>='a') { alphaLetters[c.lcChar-'a'] = c; }}
    	Blank = letters[0];
    	A = letters[3];
    	
    	assignedBlanks = new CrosswordleChip[26];
		for(char ch='A'; ch<='Z'; ch = (char)(ch+1))
		{
			assignedBlanks[ch-'A']=new CrosswordleChip(Blank,ch); 
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
    {	
		super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, null);
    	if(BACK.equals(label)) {}
    	else if(YELLOW.equals(label)) 
    		{yellowWash.drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,null); }
       	else if(NEWYELLOW.equals(label)) 
		{newyellowWash.drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,null); }
    	else if(letter!=null)
    	{	// draw all letters with the same size
    		if(GREEN.equals(label)) 
    			{greenWash.drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,null); }
    		else if(NEWGREEN.equals(label)) 
			{newgreenWash.drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,null); }
    		Text ww = TextChunk.create("W");
    		int ss = (int)(SQUARESIZE*0.6);
    		if(ss>5)
    		{
    		// display the letter if the tile is not tiny
    		Font f = G.getFont(canvas.labelFont,ss);
    		GC.setFont(gc, f);
    		GC.setColor(gc,Color.black);
    		GC.setFont(gc, ww.selectFontSize(gc, ss,ss));
    		FontMetrics fm = G.getFontMetrics(f);
     		GC.Text(gc, letter, cx-fm.stringWidth(letter)/2+(int)(SQUARESIZE*0.1),cy+(int)(SQUARESIZE*0.27));
 
    		}
    	}
    }
}
