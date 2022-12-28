package sprint;

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

class ChipStack extends OStack<SprintChip>
{
	public SprintChip[] newComponentArray(int n) { return(new SprintChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */

public class SprintChip extends chip<SprintChip> implements SprintConstants
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
	SprintChip back = null;
	public String contentsString() { return(letter); }
	
	private SprintChip(SprintChip from,char l,int v)
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
	private SprintChip(String na,double[]sc,String t,int v)
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
    static final SprintChip getChip(int n) { return((SprintChip)allChips.elementAt(n)); }
    
    static SprintChip letters[] = null;		// all the letters, including multiples and blanks
    static SprintChip alphaLetters[] = null;	// just a-z
    static SprintChip getLetter(char l)
    {
    	if(l>='a' && l<='z') { return(alphaLetters[l-'a']); }
    	if(l>='A' && l<='Z') { return(alphaLetters[l-'A']); }
    	return(null);
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public SprintChip backgroundTile = new SprintChip("background-tile-nomask",null,null,0);
    static public SprintChip backgroundReviewTile = new SprintChip("background-review-tile-nomask",null,null,0);
   
    static private double hexScale[] = {0.62,0.48,1.24};
    static private double letterScale[]  = {0.58,0.45,1.24 };
	static private double letter2Scale[] = {0.62,0.48,1.24 };
   	static private double letter0Scale[] = {0.62,0.48,1.24 };
   	static private double letter1Scale[] = {0.62,0.48,1.24 };
 
    static public SprintChip Tile = new SprintChip("tile",hexScale,null,0);
    static public SprintChip Letter[] = {new SprintChip("letter",letterScale,null,0),
    		new SprintChip("letter0",letter0Scale,null,0),
    		new SprintChip("letter1",letter1Scale,null,0),
    		new SprintChip("letter2",letter2Scale,null,0),
    	};
    
    private static double dscale[] = {0.5,0.5,1};
    
	public static SprintChip Icon = new SprintChip("icon-nomask",dscale,null,0);

	public static SprintChip NoBackwards = new SprintChip("nobackwards-nomask",dscale,null,0);
	public static SprintChip Backwards = new SprintChip("backwards-nomask",dscale,null,0);
	public static SprintChip Diagonals = new SprintChip("diagonals-nomask",dscale,null,0);
	public static SprintChip NoDiagonals = new SprintChip("nodiagonals-nomask",dscale,null,0);
	public static SprintChip AllConnected = new SprintChip("allconnected-nomask",dscale,null,0);
	public static SprintChip NotConnected = new SprintChip("noconnected-nomask",dscale,null,0);
	public static SprintChip NoDuplicates = new SprintChip("noduplicates-nomask",dscale,null,0);
	public static SprintChip Duplicates = new SprintChip("dups-nomask",dscale,null,0);
	public static SprintChip OpenRacks = new SprintChip("openracks-nomask",dscale,null,0);
	public static SprintChip ClosedRacks = new SprintChip("closedracks-nomask",dscale,null,0);

	public static SprintChip LockRotation = new SprintChip("lock-nomask",dscale,LockMessage,0);
	public static SprintChip UnlockRotation = new SprintChip("unlock-nomask",dscale,UnlockMessage,0);
	
	static {
		Option.NoDuplicate.onIcon = NoDuplicates;
		Option.NoDuplicate.offIcon = Duplicates;
	}
	static public SprintChip assignedBlanks[];
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
    public static SprintChip Blank; 
    public static SprintChip A;
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
			SprintChip ch = new SprintChip(Letter[nletters%Letter.length],let,value);
			nletters++;
			for(int i=0;i<count;i++)
			{ pool.push(ch);
			}
		}
		letters = pool.toArray();  	
		alphaLetters = new SprintChip[26];
		for(SprintChip c : letters) { if(c.lcChar>='a') { alphaLetters[c.lcChar-'a'] = c; }}
    	Blank = letters[0];
    	A = letters[3];
    	
    	assignedBlanks = new SprintChip[26];
		for(char ch='A'; ch<='Z'; ch = (char)(ch+1))
		{
			assignedBlanks[ch-'A']=new SprintChip(Blank,ch,0); 
		}}
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
		super.drawChip(gc, canvas, SQUARESIZE*6/5, xscale, cx, cy, null);
    	if(BACK.equals(label)) {}
    	else if(letter!=null)
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
     		GC.Text(gc, letter, (int)(cx-fm.stringWidth(letter)*0.75),cy+(int)(SQUARESIZE*0.2));
     		if(value!=0)
     		{
    		GC.Text(gc,true,cx+SQUARESIZE/10,cy+SQUARESIZE/6,SQUARESIZE/5,SQUARESIZE/5,ltblue,null,""+value);
     		}
    		}
    	}
    }
}
