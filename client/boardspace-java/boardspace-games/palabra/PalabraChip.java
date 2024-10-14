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
package palabra;

import java.awt.Color;
import lib.Graphics;
import lib.Image;
import lib.ImageLoader;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import bridge.Platform.Style;
import lib.DrawableImageStack;
import lib.G;
import lib.GC;
import lib.OStack;
import lib.Random;
import lib.exCanvas;
import online.game.chip;

class ChipStack extends OStack<PalabraChip>
{
	public PalabraChip[] newComponentArray(int n) { return(new PalabraChip[n]); }
}

public class PalabraChip extends chip<PalabraChip> implements PalabraConstants
{
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public static ChipStack redDeck = new ChipStack();
	public static ChipStack blueDeck = new ChipStack();
	private String cardName = null;
	public String printableName() 
	{ 
		if(cardName!=null) { return(cardName); }
		else return(""+getCardId()+"-"+getCardColors()+getStars());
	}
   

	private int chipIndex;
	private int value;
	private boolean isCard = false;
	private PalabraChip cardBack;
	private char cardChar = 0;
	public char getCardId() { return(cardChar); }
	private int cardColors = 0;
	public String toString() 
	{
		if(!isCard) {return(super.toString());}
		return("<"+getBackColor()+" "+getCardId()+getStarString()+getCardColors()+">");
	}
	public String getCardColors() 
	{	String val = "";
		for(CardColors c : CardColors.values())
		{
			if((cardColors & (1<<c.ordinal()))!=0) { val += c.color; }
		}
		return(val);
	}
	private int stars = 0;
	private int getStars() { return(stars); }
	private String getBackColor()
	{
		if(cardBack==red_back) { return("Red"); }
		if(cardBack==blue_back) { return("Blue"); }
		return(cardBack.toString());
	}
	private String getStarString() { 
		switch(stars)
		{
		case 0:	return(" ");
		case 1: return(" * ");
		case 2: return(" ** ");
		case 3: return(" *** ");
		default: throw G.Error("not expected");
		}
	}
	private static final char WildcardId = '?';
	private static final char JokerId = '#';
	private static final char VowelId = '@';
	private static final char BlankId = '-';
	private static final char InfocardId = '!';
	public boolean isWildcard() { return(cardChar==WildcardId); }
	public boolean isJoker() { return(cardChar==JokerId); }
	public boolean isVowel() { return(cardChar==VowelId); }
	public boolean isBlank() { return(cardChar==BlankId); }
	public boolean isInfocard() { return(cardChar==InfocardId); }
	
	public boolean isCard() { return(isCard); }
	public int getValue() { return(value);}
	public int chipNumber() { return(chipIndex); }
	public static PalabraChip getChip(int id)
	{	return((PalabraChip)allChips.elementAt(id));
	}
	
	public PalabraId id = null;		// chips/images that are expected to be visible to the user interface should have an ID

	// constructor for chips not expected to be part of the UI
	private PalabraChip(String na,double scl[])
	{	file = na;
		chipIndex=allChips.size();
		randomv = r.nextLong();
		scale = scl;
		allChips.push(this);
	}

	private static int getCardColorMask(String str)
	{	int mask = 0;
		for(int i=0;i<str.length(); i++) 
		{
			mask |= (1 << CardColors.find(str.charAt(i)).ordinal());
		}
		return(mask);
	}
	private PalabraChip(String name,int ncopies,BackColors back,char id,String colors,int st,int v)
	{	int backOffset = 0;
		ChipStack deck =null;
		cardName = name;
		isCard = true;
		value = v;
		switch(back)
		{
		default: throw G.Error("Not expected");
		case Red:
			cardBack = red_back;
			backOffset = 1000;
			deck = redDeck;
			break;
		case Blue:
			cardBack = blue_back;
			backOffset = 2000;
			deck = blueDeck;
		}
		chipIndex = id+backOffset;
		cardChar = id;
		stars = st;
		cardColors = getCardColorMask(colors);
		for(int i=0;i<ncopies;i++) { deck.push(this); }
	}
	private PalabraChip(String name,int ncopies,char id,String colors,int st,int v)
	{
		this(name,ncopies,BackColors.Red,id,colors,st,v);
	}
	private PalabraChip(int ncopies,char id,String colors,int st,int v)
	{
		this(null,ncopies,BackColors.Red,id,colors,st,v);
	}
	
	static final PalabraChip circle = new PalabraChip("circle",new double[] {0.5,0.6,1.0});
	static final PalabraChip square = new PalabraChip("rectangle",new double[] {0.5,0.5,1.0});
	static final PalabraChip diamond = new PalabraChip("diamond",new double[] {0.5,0.5,1.0});
	static final PalabraChip triangle = new PalabraChip("triangle",new double[] {0.5,0.5,1.0});
	static final PalabraChip joker = new PalabraChip("joker",new double[] {0.5,0.5,1.0});
	static final PalabraChip star = new PalabraChip("star",new double[] {0.5,0.5,1.0});
	static final PalabraChip wild = new PalabraChip("blade-fan",new double[] {0.5,0.5,1.0});
	static final PalabraChip red_back = new PalabraChip("red-back",new double[] {0.5,0.5,1.0});
	static final PalabraChip blue_back = new PalabraChip("blue-back",new double[] {0.5,0.5,1.0});
	static final PalabraChip front = new PalabraChip("front",new double[] {0.5,0.5,1.0});
	static final PalabraChip red_back_borderless = new PalabraChip("red-back-borderless-nomask",new double[] {0.5,0.5,1.0});
	static final PalabraChip blue_back_borderless = new PalabraChip("blue-back-borderless-nomask",new double[] {0.5,0.5,1.0});
	static final PalabraChip infoCard = new PalabraChip("infocard-nomask.png",new double[] {0.5,0.5,1.0});
    public static Color palabraGreen = new Color(102,200,4);
    public static Color palabraBlue = new Color(61,89,186);
    public static Color palabraYellow = new Color(239,230,11);
    public static Color palabraRed = new Color(216,44,30);
 
    enum CardColors {
    	// red and blue go together, in this order
		Red('R',palabraRed,PalabraChip.circle),
		Blue('B',palabraBlue,PalabraChip.triangle),
		// green and yellow go together in this order
		Green('G',palabraGreen,PalabraChip.diamond),
		Yellow('Y',palabraYellow,PalabraChip.square),
		Black('X',Color.black,null),
		Clear((char)0,null,null);
		char color;
		Color fillColor = null;
		PalabraChip icon = null;
		CardColors(char n,Color c,PalabraChip card) { color = n; fillColor=c; icon = card; };
		public static CardColors find(char ch)
		{	for(CardColors c : values())
			{
			if(c.color==ch) { return(c); }
			}
			throw G.Error("Not expecting %s",ch); 
		}
	}
	enum BackColors { Red,Blue};

	static void copyDeck(ChipStack from,ChipStack to,BackColors color)
	{
		to.setSize(0);
		PalabraChip prev = null;
		PalabraChip prevCopy = null;
		for(int i=0;i<from.size();i++)
		{	PalabraChip fc = from.elementAt(i);
			if(fc != prev)
			{
			prev = fc;
			prevCopy = new PalabraChip(fc.cardName,0,color,fc.getCardId(),fc.getCardColors(),fc.getStars(),fc.getValue());
			}
			to.push(prevCopy);
		}
	}
	static {
		// template for a standard palabra deck
		new PalabraChip("Wild",3,WildcardId,"RGBY",0,0);
		new PalabraChip("Joker",2,JokerId,"X",0,0);
		new PalabraChip("Vowel",2,VowelId,"RGBY",0,0);
		new PalabraChip("Blank",3,(char)0,"",0,0);		// 3 blanks
		new PalabraChip("Info",1,InfocardId,"",0,0);	// 1 info card
		new PalabraChip(2,'A',"Y",0,1);
		new PalabraChip(1,'B',"Y",0,4);
		new PalabraChip(1,'C',"Y",2,3);
		new PalabraChip(1,'D',"Y",3,2);
		new PalabraChip(3,'E',"Y",0,1);	// 3 yellow e's with no stars
		new PalabraChip(1,'F',"Y",0,4);
		new PalabraChip(1,'G',"Y",0,5);
		new PalabraChip(1,'H',"Y",2,4);
		new PalabraChip(3,'I',"Y",0,1);
		new PalabraChip(1,'L',"Y",3,2);
		new PalabraChip(2,'N',"Y",0,1);
		new PalabraChip(2,'O',"Y",0,1);
		new PalabraChip(1,'S',"Y",2,1);
		new PalabraChip(1,'T',"Y",0,1);
		new PalabraChip(1,'Y',"Y",0,4);
		
		new PalabraChip(1,'C',"RB",0,3);
		new PalabraChip(1,'D',"RB",2,2);
		new PalabraChip(1,'J',"RB",0,9);
		new PalabraChip(1,'K',"RB",3,6);
		new PalabraChip(1,'L',"RB",0,2);
		new PalabraChip(1,'N',"RB",0,1);
		new PalabraChip(1,'R',"RB",0,1);
		new PalabraChip(1,'S',"RB",2,1);
		new PalabraChip(2,'T',"RB",0,1);
		new PalabraChip(1,'Q',"RB",0,10);
		new PalabraChip(1,'W',"RB",0,5);
		new PalabraChip(1,'X',"RB",0,8);
		new PalabraChip(1,'Z',"RB",0,10);
		
		new PalabraChip(2,'A',"G",0,1);
		new PalabraChip(1,'B',"G",3,4);
		new PalabraChip(1,'C',"G",0,3);
		new PalabraChip(1,'D',"G",0,2);
		new PalabraChip(3,'E',"G",0,1);
		new PalabraChip(1,'F',"G",2,4);
		new PalabraChip(1,'G',"G",2,5);
		new PalabraChip(1,'H',"G",0,4);
		new PalabraChip(3,'I',"G",0,1);
		new PalabraChip(1,'L',"G",0,2);
		new PalabraChip(1,'N',"G",0,1);
		new PalabraChip(1,'N',"G",3,1);
		new PalabraChip(2,'O',"G",0,1);
		new PalabraChip(1,'S',"G",0,1);
		new PalabraChip(1,'T',"G",0,1);
		new PalabraChip(1,'Y',"G",2,4);
		
		new PalabraChip(3,'A',"R",0,1);
		new PalabraChip(1,'B',"R",0,4);
		new PalabraChip(3,'E',"R",0,1);
		new PalabraChip(1,'G',"R",3,5);
		new PalabraChip(1,'F',"R",0,4);
		new PalabraChip(1,'H',"R",2,4);
		new PalabraChip(2,'I',"R",0,1);
		new PalabraChip(1,'M',"R",3,3);
		new PalabraChip(1,'N',"R",2,1);
		new PalabraChip(2,'O',"R",0,1);
		new PalabraChip(1,'P',"R",0,2);
		new PalabraChip(1,'R',"R",0,1);
		new PalabraChip(1,'S',"R",0,1);
		new PalabraChip(1,'U',"R",0,1);
		new PalabraChip(1,'V',"R",2,5);
		new PalabraChip(1,'Y',"R",0,4);
		
		new PalabraChip(1,'J',"GY",0,9);
		new PalabraChip(1,'K',"GY",2,6);
		new PalabraChip(1,'M',"GY",3,3);
		new PalabraChip(1,'P',"GY",0,2);
		new PalabraChip(1,'Q',"GY",0,10);
		new PalabraChip(2,'R',"GY",0,1);
		new PalabraChip(1,'T',"GY",0,1);
		new PalabraChip(1,'U',"GY",0,1);
		new PalabraChip(1,'V',"GY",0,5);
		new PalabraChip(1,'W',"GY",2,5);
		new PalabraChip(1,'X',"GY",0,8);
		new PalabraChip(1,'Z',"GY",0,10);
		
		new PalabraChip(3,'A',"B",0,1);
		new PalabraChip(1,'B',"B",0,4);
		new PalabraChip(3,'E',"B",0,1);
		new PalabraChip(1,'F',"B",2,4);
		new PalabraChip(1,'G',"B",0,5);
		new PalabraChip(1,'H',"B",0,4);
		new PalabraChip(2,'I',"B",0,1);
		new PalabraChip(1,'M',"B",0,3);
		new PalabraChip(1,'N',"B",0,1);
		new PalabraChip(2,'O',"B",0,1);
		new PalabraChip(1,'P',"B",3,2);
		new PalabraChip(1,'R',"B",2,1);
		new PalabraChip(1,'S',"B",0,1);
		new PalabraChip(1,'U',"B",2,1);
		new PalabraChip(1,'V',"B",0,5);
		new PalabraChip(1,'Y',"B",3,4);

		G.Assert(redDeck.size()==126,"should be 126 cards");
		copyDeck(redDeck,blueDeck,BackColors.Blue);
	}
    public void drawCard(Graphics gc,exCanvas drawOn,int SQUARESIZE,double xscale,int e_x0,int e_y0,String lab)
    {	G.Assert(isCard(),"should be a card");
    	if(gc!=null)
    	{GC.translate(gc,e_x0, e_y0);
    	 drawCardInternal(gc,drawOn,SQUARESIZE,xscale,e_x0,e_y0,true,!"-".equals(lab));
    	 GC.setRotation(gc,Math.PI,0,0);
    	 drawCardInternal(gc,drawOn,SQUARESIZE,xscale,e_x0,e_y0,false,false);
    	 GC.setRotation(gc,-Math.PI,0,0);
    	 GC.translate(gc,-e_x0,-e_y0);
    	}
    }
    public void drawCardInternal(Graphics gc,exCanvas drawOn,int SQUARESIZE,double xscale,int e_x0,int e_y0,
    		boolean includeCenter,boolean includeFront)
    {	
    	
    	if(includeFront) { front.drawChip(gc,drawOn,SQUARESIZE,xscale,0,0,null); }
    	
    	{
    		int letterx = (int)(- (SQUARESIZE*0.44));
    		int lettery = (int)(- (SQUARESIZE*0.7));
    		int letterw = (int)(SQUARESIZE*0.2);
    		int letterh = (int)(SQUARESIZE*0.16);
    		int letterw1 = (int)(SQUARESIZE*0.25);
    		int letterh1 = (int)(SQUARESIZE*0.2);
    		int letterx1 = letterx+letterw-letterw1;
    		int lettery1 = lettery+letterh-letterh1;
    		char id = getCardId();
    		switch(id)
    		{
    		case InfocardId:
    			{
    			infoCard.drawChip(gc, drawOn,SQUARESIZE, 0,0,null);
    			}
    			break;
    		case JokerId:
    			{
    			joker.drawChip(gc,drawOn,letterw,letterx+letterw/2,lettery+letterh/2,null);
    			int lx =  - letterw;
    			int ly = lettery - letterh/6;
    			GC.Text(gc,true,lx,ly,letterw*2,letterh,Color.black,null,"CANCEL");
 
    			String msg = "JOKER";
    			lettery += letterh/2;
    			for(int i=0;i<msg.length();i++)
    			{
    				char c = msg.charAt(i);
    				lettery += letterh;
    				GC.Text(gc,true,letterx,lettery,letterw,letterh,Color.black,null,""+c);
    			}
    			}
    			break;
    		case VowelId:
    			{
       			wild.drawChip(gc,drawOn,letterw,letterx+letterw/2,lettery+letterh/2,null);
       			String msg = "aeiouy";
    			int lx =  - 6*letterw/2;
    			int ly = lettery - letterh/4;
    			GC.Text(gc,true,lx,ly,letterw*6,letterh,Color.black,null,msg);
    			break;
    			}
    		case WildcardId:
    			{
    			wild.drawChip(gc,drawOn,letterw,letterx+letterw/2,lettery+letterh/2,null);
       			String msg = "A-Z";
    			int lx =  - 3*letterw/2;
    			int ly = lettery - letterh/4;
    			GC.Text(gc,true,lx,ly,letterw*3,letterh,Color.black,null,msg);
    			}
    			break;
    		default:
    			GC.Text(gc,true,letterx1,lettery1,letterw1,letterh1,Color.black,null,""+id);
    			int val = getValue();
    			if(val>0)
    			{
    				GC.Text(gc, true, letterx1+letterw1-letterw1/4, lettery1+2*letterh1/3, letterw1/2, letterh1/2,Color.black,null,""+val);
    			}
    		}
    		lettery += letterh1;
 
    	double ww = 0.21;
    	double hh = 0.45;
    	int cx = (int)(- SQUARESIZE*ww);
    	int cy = (int)(- SQUARESIZE*hh);
    	int cx0 = cx;
    	int cy0 = cy;
    	int cw = (int)(SQUARESIZE*ww*2);
    	int ch = (int)(SQUARESIZE*hh*2);
    	int border = Math.max(1,(int)(SQUARESIZE*0.02));
    	String colors = getCardColors();
    	int ncolors = colors.length();
    	int stepx = ncolors>2 ? (cw-border*2-1)/2 : cw-border*2-2;
    	int stepy = ncolors>1 ? (ch-border*2-1)/2 : ch-border*2-2;
    	int endy = cy0+stepy*2-2;
    	 
    	for(int i=0;i<ncolors;i++)
    	{	// if includeCenter is false, we're drawing the reversed card, 
    		// and we also want to draw the color icons in the reverse order.
    		// this puts the yellow diamond on top with the yellow half of
    		// the yelow-green cards etc.
    		CardColors color = CardColors.find(colors.charAt(includeCenter ? i : ncolors-i-1));
    		Color cl = color.fillColor;
    		PalabraChip icon = color.icon;

    		if(includeCenter && cl!=null) 
    			{ int ew = Math.min(stepx,cw-((cx>cx0)?stepx:0)-border*2-2);
    			  int eh = Math.min(stepy, ch-((cy>cy0)?stepy:0)-border*2-2);
    			  GC.fillRect(gc, cl, cx+border+1, cy+border+1,ew,eh); 
    			}
    		if(icon!=null)
    		{
    			icon.drawChip(gc,drawOn,letterw,letterx+letterw/2,lettery+letterw/2,null);
    			lettery += letterh;
    		}
    		cy += stepy;
    		if(cy>endy) { cy = cy0; cx+= stepx; }
     	}
    	if(ncolors>0) { GC.frameRect(gc, Color.black, cx0,cy0,cw,ch); }
    
    	// now the stars
    	for(int i=0;i<getStars();i++)
    		{
    		PalabraChip.star.drawChip(gc,drawOn,letterw,letterx+letterw/2,lettery+letterh/2,null);
    		lettery += letterh;
    		}
    	}
    
    }
    
// this incantation stolen from stackoverflow, to somehow set the dpi of png images
private static void saveGridImage(File output,RenderedImage gridImage,int dpi) throws IOException {
output.delete();

final String formatName = "png";

for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) {
   ImageWriter writer = iw.next();
   ImageWriteParam writeParam = writer.getDefaultWriteParam();
   ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
   IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
   if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
      continue;
   }

   setDPI(metadata,dpi);

   final ImageOutputStream stream = ImageIO.createImageOutputStream(output);
   try {
      writer.setOutput(stream);
      writer.write(metadata, new IIOImage(gridImage, null, metadata), writeParam);
   } finally {
      stream.close();
   }
   break;
}
 }

 private static void setDPI(IIOMetadata metadata,int dpi) throws IIOInvalidTreeException {

// for PMG, it's dots per millimeter
double dotsPerMilli = 1.0 * dpi / 10 / 2.54;

IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
horiz.setAttribute("value", Double.toString(dotsPerMilli));

IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
vert.setAttribute("value", Double.toString(dotsPerMilli));

IIOMetadataNode dim = new IIOMetadataNode("Dimension");
dim.appendChild(horiz);
dim.appendChild(vert);

IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
root.appendChild(dim);

metadata.mergeTree("javax_imageio_1.0", root);
 }
    private static void printCard(String name,Image im)
    {  	
		try {
			saveGridImage(new File(name),(RenderedImage)im,300);
			//FileOutputStream stream = new FileOutputStream(name);
			//ImageIO.write((BufferedImage)im, "png",stream);
			//stream.close();
		} catch (IOException e) {
			G.Error("failed ",e);
		}
    }
    public static int[] gamecrafterDims = {750,1125};
    public static int[] makeplayingcardsDims = { 1900,3000 };
    public static int[] printplaygamesDims = { 3600, 5400, 750,1125 };
    public static int[] printplaygamesInsideMargins = { 0,0,0,0};
    public static int[] superiorpodDims = { 3600, 5400,788,1087 };
    public static int[] superiorpodInsideMargins = { 0, 600, 37, 0};

    /**
     * print a full card of all the same.  Used for backs.
     * @param drawOn
     * @param master
     * @param card
     * @param name
     * @param firstx
     * @param firsty
     * @param stepx
     * @param stepy
     * @param masterWidth
     * @param masterHeight
     */
    private static void printMasterBack(exCanvas drawOn,Image master,Image card,String name,int firstx,int firsty,int stepx,int stepy,int masterWidth,int masterHeight)
    {	Graphics mgc = master.getGraphics();
    	GC.fillRect(mgc, Color.white,0,0,masterWidth,masterHeight);
    	for(int mx = firstx; mx<masterWidth-stepx; mx+=stepx)
    	{
    		for(int my = firsty; my<masterHeight-stepy; my+= stepy)
    		{
    		card.drawImage(mgc, mx,my);
    		}
    	}
    	printCard(name,master);
    }
 
    /**
     * arrange a deck of cards onto multiple master sheets.  This assumes that
     * the templates on the master sheet call for the cards to be centered on
     * the sheet with least possible waste.  PrintPlayGames for example, positions
     * 21 bridge size cards on a sheet, rotated 90 degrees and in 3x7 rows,columns.
     * 
     * @param drawOn
     * @param dest
     * @param dims
     * @param masterSheetDims
     */
    public static void printMasterDeck(exCanvas drawOn,String dest,boolean rotateMaster,int []dims,int masterSheetDims[],int masterSheetMargins[])
    {	int width = dims[0];
    	int height = dims[1];
    	int wstep = masterSheetDims[2];
    	int hstep = masterSheetDims[3];
    	int maxDim = Math.max(wstep, hstep);
    	int insideLeft = masterSheetMargins[0];
    	int insideTop = masterSheetMargins[1];
    	int insideRight = masterSheetMargins[2];
    	int insideBottom = masterSheetMargins[3];
    	int masterWidth = masterSheetDims[0];
    	int insideMasterWidth = (masterWidth-insideLeft-insideRight);
    	int masterHeight = masterSheetDims[1];
    	int insideMasterHeight = (masterHeight-insideTop-insideBottom);
    	
    	Image master = Image.createImage(masterWidth,masterHeight);
    	Image im = Image.createImage(width,height);
    	Image rota = rotateMaster ? Image.createImage(height,width) : im;
    	Image temp = rotateMaster ? Image.createTransparentImage(maxDim,maxDim) : null;
    	int stepx = rotateMaster? hstep : wstep;
    	int stepy = rotateMaster? wstep : hstep;
    	int firstx = insideLeft+(insideMasterWidth%stepx)/2;
    	int firsty = insideTop+(insideMasterHeight%stepy)/2;
    	int mx = firstx;
    	int my = firsty;
    	int txo = (maxDim-width)/2;
    	int tyo = (maxDim-height)/2;
    	Graphics mgc = master.getGraphics();
    	Graphics gc = im.getGraphics();
    	gc.setFont(G.getFont("Serif",Style.Bold,400));
    	int cx = width/2;
    	int cy = (int)(height*0.5);
    	int card_seq = 0;
    	ChipStack deck = redDeck;
    	PalabraChip prev = null;
    	int copy = 1;
    	for(int i=0;i<deck.size();i++)
    	{	PalabraChip  card = deck.elementAt(i);
    		if(card==prev) { copy++; } else { copy = 1; }
    		GC.fillRect(gc,Color.white,0,0,width,height);
    		card.drawCard(gc,drawOn, (int)(width*0.90),1.0,cx,cy+copy-1,"-");

    		if(rotateMaster)
    		{
    			Graphics rc = temp.getGraphics();
    			GC.fillRect(rc,Color.white,0,0,maxDim,maxDim);
    		 im.drawImage(rc, txo,tyo);
    			Image temp2 = temp.rotate(Math.PI/2,0xffffff);
    			Graphics rc2 = rota.getGraphics();

    		 temp2.drawImage(rc2, 0, 0,height,width,tyo,txo,tyo+height,txo+width);
    		rota.drawImage(mgc,mx,my);   			
    		}
    		else { im.drawImage(mgc,mx, my);}
 
    		mx += stepx;
    		if(mx>masterWidth-stepx) 
    			{ mx = firstx;
    			  my += stepy;
    			}
    		if(my>masterHeight-stepy) 
    			{ my = firsty;
    			  mx = firstx;
    			  card_seq++;
    			  String name = dest+"card-"+card_seq+".png";
    	    	  printCard(name,master);
    	    	  GC.fillRect(mgc, Color.white,0,0,masterWidth,masterHeight);
    			}
     		prev = card;    		
    	}
    	// if there's a partial card, print it.
    	if((mx!=firstx) || (my!=firsty))
	      {
	   	  card_seq++;
	   	  String name = dest+"card-"+card_seq+".png";
	   	  printCard(name,master);
	      }
 	    	  
     	GC.fillRect(gc,Color.white,0,0,width,height);
		red_back_borderless.drawChip(gc,drawOn, width,1.0,cx,cy,null);
		if(rotateMaster)
		{
		Graphics rc = temp.getGraphics();
		GC.fillRect(rc,Color.white,0,0,maxDim,maxDim);
		im.drawImage(rc, txo,tyo);
		Image temp2 = temp.rotate(Math.PI/2,0xffffff);
		Graphics rc2 = rota.getGraphics();

	 temp2.drawImage(rc2, 0, 0,height,width,tyo,txo,tyo+height,txo+width);
	   	printMasterBack(drawOn,master,rota,dest+"red-back.png",
	   			firstx,firsty,stepx,stepy,insideMasterWidth,insideMasterHeight);
    	}
		else {
		   	printMasterBack(drawOn,master,im,dest+"red-back.png",
		   			firstx,firsty,stepx,stepy,insideMasterWidth,insideMasterHeight);
			
		}
    	GC.fillRect(gc,Color.white,0,0,width,height);
		blue_back_borderless.drawChip(gc,drawOn, width,1.0,cx,cy,null);
		if(rotateMaster)
    	{
		Graphics rc = temp.getGraphics();
		GC.fillRect(rc,Color.white,0,0,maxDim,maxDim);
		im.drawImage(rc, txo,tyo);
		Image temp2 = temp.rotate(Math.PI/2,0xffffff);
		Graphics rc2 = rota.getGraphics();
		
	 temp2.drawImage(rc2, 0, 0,height,width,tyo,txo,tyo+height,txo+width);
		printMasterBack(drawOn,master,rota,dest+"blue-back.png",
	   			firstx,firsty,stepx,stepy,masterWidth,masterHeight);
    	}
    	else {
    		printMasterBack(drawOn,master,im,dest+"blue-back.png",
    	   			firstx,firsty,stepx,stepy,masterWidth,masterHeight);
    	}
		
    }

    public static void printDeck(exCanvas drawOn,String dest,int []dims)
    {	int width = dims[0];
    	int height = dims[1];
    	Image im = Image.createImage(width,height);
    	Graphics gc = im.getGraphics();
    	gc.setFont(G.getFont("Serif",Style.Bold,400));
    	int cx = width/2;
    	int cy = (int)(height*0.5);
    	ChipStack deck = redDeck;
    	PalabraChip prev = null;
    	int copy = 1;
    	for(int i=0;i<deck.size();i++)
    	{	PalabraChip  card = deck.elementAt(i);
    		if(card==prev) { copy++; } else { copy = 1; }
    		GC.fillRect(gc,Color.white,0,0,width,height);
    		card.drawCard(gc,drawOn, (int)(width*0.90),1.0,cx,cy+copy-1,"-");

     		String name = dest+card.printableName()+"-"+copy+".png";
    		printCard(name,im);
     		prev = card;
    		
    	}
    	GC.fillRect(gc,Color.white,0,0,width,height);
		red_back_borderless.drawChip(gc,drawOn, width,1.0,cx,cy,null);
		printCard(dest+"red-back.png",im);
		
    	GC.fillRect(gc,Color.white,0,0,width,height);
		blue_back_borderless.drawChip(gc,drawOn, width,1.0,cx,cy,null);
		printCard(dest+"blue-back.png",im);
		
    }

	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(ImageDir,allChips);
		if(imagesLoaded) { Image.registerImages(allChips); }
		}
	}

}
