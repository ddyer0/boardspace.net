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
package raj;

import com.codename1.ui.Font;
import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.CompareTo;
import lib.G;
import lib.GC;
import lib.OStack;
import lib.Random;
import lib.exCanvas;
import online.game.chip;

class ChipStack extends OStack<RajChip>
{
	public RajChip[] newComponentArray(int n) { return(new RajChip[n]); }
}
/**
 * this is a specialization of {@link chip} to represent the pieces used by Raj
 * 
 * there are chips for each of 15 prizes, prize back, each of 5 card backs, and each of 5 card fronts.
 * plus, the actual CARDS for each player, 5 sets of 15 CARDS each.
 * 
 * @author ddyer
 *
 */
public class RajChip extends chip<RajChip> implements RajConstants,CompareTo<RajChip>
{
	private int index = 0;								// unique index
	private int cardValue = 0;							// value if this is a card
	private static final int CARDS_OFFSET = 1000;		// offset identifies cards vs everything else
	private static RajChip CANONICAL_PIECE[] = null;	// initialized when the chips are loaded	
	private static RajChip CARDS[] = null;				// initialized when the chips are loaded
	private RajColor color = null;
	private RajChip frontChip = null;

	static public int compareTo(RajChip c,RajChip d)
	{
		if(c.isCard()&&d.isCard())
		{
			return(c.cardValue()-d.cardValue());
		}
		return(0);
	}
	public int compareTo(RajChip o)
	{
		return(RajChip.compareTo(this,o));
	}
	public int altCompareTo(RajChip o)
	{
		return(RajChip.compareTo(o,this));
	}
	
	// constructor for everything except the player CARDS
	private RajChip(int i,Image im,String na,double[]sc,long ran)
	{	index = i;
		scale=sc;
		image=im;
		file = na;
		randomv = ran;
	}
	
	// constructor for the player CARDS. 
	private RajChip(RajColor color, int ind, int val,long rv,boolean front)
	{	index = ind;
		cardValue = val;
		randomv = rv;
		this.color = color;
		RajChip baseCard = front ? getCardFront(color) : getCardBack(color);
		frontChip = this;
		image = baseCard.image;
		scale = baseCard.scale;
		file = baseCard.file + "#"+val;
	}
	public boolean isCard()
	{	return((index>=CARDS_OFFSET) && index<(CARDS_OFFSET+CARDS.length));
	}
	public RajColor cardColor()
	{	G.Assert(isCard(),"not a card");
		return(color);
	}
	public int cardValue()
	{	G.Assert(isCard(),"not a card");
		return(cardValue);
	}
	public boolean isPrize()
	{	return((index>=PRIZE_INDEX) && (index<=PRIZE_INDEX+NUMBER_OF_PRIZES));
	}
	public int prizeValue()
	{	G.Assert(isPrize(),"not a prize");
		return(isPrize()?(index+MIN_PRIZE_VALUE):0);
	}
	public int chipNumber() { return(index); }
	public boolean isFront() 
	{
		return(isCard() && (frontChip==this));
	}

	// call from the viewer's preloadImages
    
    public static final RajChip getChip(int n) 
    	{ return((n<CARDS_OFFSET)?CANONICAL_PIECE[n]:CARDS[n-CARDS_OFFSET]); 
    	}
    static final double scales[][] = {
    	{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},
    	{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},
    	{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},
    	{0.5,0.5,1.0},
    	{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},
    	{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},
    	{0.5,0.5,1.0},{0.5,0.5,1.0}
    };
    private static final int PRIZE_INDEX = 0;
    private static final int CARD_FRONT_INDEX = 16;
    private static final int CARD_BACK_INDEX = 21;
    public static RajChip getPrize(int n)
    {	if((n>=MIN_PRIZE_VALUE) && (n<=MAX_PRIZE_VALUE))
    	{	return(CANONICAL_PIECE[PRIZE_INDEX+n-MIN_PRIZE_VALUE]);
    	}
    	return(null);
    }
    public static RajChip getCardFront(RajColor col)
    {	return(CANONICAL_PIECE[CARD_FRONT_INDEX+col.ordinal()]);
    }
    public static RajChip getCardBack(RajColor col)
    {	return(CANONICAL_PIECE[CARD_BACK_INDEX+col.ordinal()]);
    }  
    public RajChip getCardFront() { return(frontChip); }
    public RajChip getCardBack() { return(getCardBack(color)); }
   
    public static RajChip getCard(RajColor color, int val)
    {	return(getCard(NUMBER_OF_PRIZES*color.ordinal()+CARDS_OFFSET+val-1));
    }
    public static RajChip getCard(int ind)
    {	G.Assert(ind>=CARDS_OFFSET && ind<CARDS_OFFSET+CARDS.length,"is a card index");
    	return(CARDS[ind-CARDS_OFFSET]);
    }
    static final String[] chipNames = 
    {	"prize-m5","prize-m4","prize-m3","prize-m2","prize-m1",
    	"prize-back",
    	"prize-1","prize-2","prize-3","prize-4","prize-5",
    	"prize-6","prize-7","prize-8","prize-9","prize-10",
    	
    	"red-front","green-front","blue-front","brown-front","purple-front",
    	"red-back","green-back","blue-back","brown-back","purple-back"
    };
    
    private static boolean imagesLoaded = false;
    
   /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images (all two of them) into
     * a static array of RajChip which are used by all instances of the
     * game.
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{
		Random rv = new Random(5312324);
		int nColors = chipNames.length;
		// load the main images, their masks, and composite the mains with the masks
		// to make transparent images that are actually used.
        Image IM[]=forcan.load_masked_images(Dir,chipNames);
        RajChip CC[] = new RajChip[nColors];
        for(int i=0;i<nColors;i++) 
        	{CC[i]=new RajChip(i,IM[i],chipNames[i],scales[i],rv.nextLong()); 
        	}
        check_digests(CC);	// verify that the chips have different digests
		CANONICAL_PIECE = CC;
		Image.registerImages(CC);
		
		{
		// construct one set of cards for each color
        CARDS = new RajChip[RajColor.values().length*NUMBER_OF_PRIZES];
        
        for(RajColor color : RajColor.values())
        {	for(int i=0;i<NUMBER_OF_PRIZES;i++)
        	{	int ind = color.ordinal()*NUMBER_OF_PRIZES+i;
        		RajChip back = CARDS[ind] = new RajChip(color,CARDS_OFFSET+ind,i+1,rv.nextLong(),false);
        		back.frontChip = new RajChip(color,CARDS_OFFSET+ind,i+1,0,true);
        		 
        	}
        }}

		imagesLoaded = true;
		
		}
	}
	// draw the number info only
	public void drawChip(Graphics gc,Font font,int SQUARESIZE,int e_x,int e_y)
	{
		GC.setFont(gc,font);
		GC.Text(gc,true,e_x-(int)(SQUARESIZE*0.45),e_y-(int)(SQUARESIZE*0.66),
				SQUARESIZE,SQUARESIZE,color.color,null,""+cardValue());
	}
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,int cx,int cy,String label)
	{
		super.drawChip(gc, canvas, SQUARESIZE, cx, cy, label);
		if(isFront())
		{
			drawChip(gc,((RajViewer)canvas).cardDeckFont,SQUARESIZE,cx,cy);
		}
	}
	// draw the chip fron itself and the aux number info
	public void drawChipFront(Graphics gc,RajViewer drawOn,int squaresize,int e_x,int e_y)
	{	getCardFront().drawChip(gc,drawOn,squaresize,e_x,e_y,null);
	}
}
