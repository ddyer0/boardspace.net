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
package mbrane;

import java.awt.Rectangle;

import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.AR;
import lib.DrawableImageStack;
import lib.G;
import lib.GC;
import lib.OStack;
import lib.Random;
import lib.exCanvas;
import mbrane.MbraneConstants.MbraneColor;
import online.game.chip;

class ChipStack extends OStack<MbraneChip>
{
	public MbraneChip[] newComponentArray(int n) { return(new MbraneChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by Mbrane;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class MbraneChip extends chip<MbraneChip> 
{
	private int index = 0;
	static private final int COLOR_OFFSET = 100;
	static final String SPRITE = "-sprite-";
	
	MbraneColor color=null;
	MbraneChip altChip;
	int value = -1;
	int instance = -1;
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	
	// constructor for the chips on the board, which are the only things that are digestable.
	private MbraneChip(String na,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		randomv = r.nextLong();
		allChips.push(this);
	}
	private MbraneChip(MbraneChip from,MbraneColor col,int v)
	{
		scale = from.scale;
		image = from.image;
		file = ""+col+" "+v;
		color = col;
		value = v;
		index = (color.ordinal()+1)*COLOR_OFFSET+value;
		randomv = r.nextLong();
	}

	private MbraneChip(MbraneChip from,MbraneColor col,int v,double randomize)
	{
		scale = AR.copy(null,from.scale);
		image = from.image;
		file = ""+col+" "+v;
		color = col;
		value = v;
		index = color==null ? value : (color.ordinal()+1)*COLOR_OFFSET+value;
		randomv = r.nextLong();
		scale[0] += r.nextDouble()*randomize-randomize/2;
		scale[1] += r.nextDouble()*randomize*0.1-(randomize*0.1)/2;
		scale[2] += r.nextDouble()*randomize*0.1-(randomize*0.1)/2;
	}
	public int visibleNumber() { return(value/9); }

	public int chipNumber() { return(index); }
	
	private static double chipScale[] = {0.5,0.5,1};
	
	public static MbraneChip blank[] =
			{ new MbraneChip("chip",chipScale),
			  new MbraneChip("chip1",chipScale),
			  new MbraneChip("chip2",chipScale),
			  new MbraneChip("chip3",chipScale)
			};
	/*
	public static MbraneChip edge[] = 
			{ new MbraneChip("edge",chipScale),
			  new MbraneChip("edge1",chipScale),
			  new MbraneChip("edge2",chipScale),
			  new MbraneChip("edge3",chipScale)
			};
	*/
	public static MbraneChip smallBoard = new MbraneChip("smallboard-nomask",chipScale);
	
    // indexes into the balls array, usually called the rack
    public static final MbraneChip getChip(MbraneColor color,int n,int inst)
    { 	return(getChip(color,n*9+inst));
    }
    public static final MbraneChip getChip(MbraneColor color,int n)
    {
    	if(color==null) { return(blank[n]); }
    	return(color.instances[n]);
    }
    
    public static final MbraneChip getChip(int n)
    {
    	switch(n/COLOR_OFFSET)
    	{
    	case 0:
    		return((MbraneChip)allChips.elementAt(n));
    	case 1:
    		return(MbraneColor.Red.instances[n-COLOR_OFFSET]);
    	case 2:
    		return(MbraneColor.Black.instances[n-COLOR_OFFSET*2]);
    	default: break;
    	}
    	throw G.Error("not expecting %s",n);
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public MbraneChip backgroundTile = new MbraneChip("background-tile-nomask",null);
    static public MbraneChip backgroundReviewTile = new MbraneChip("background-review-tile-nomask",null);
   
	public static MbraneChip Icon = new MbraneChip("mbrane-icon-nomask",chipScale);

    public static MbraneChip board = new MbraneChip("board",chipScale);
    
	public void drawChipImage(Graphics gc,exCanvas canvas,int cx,int cy,double pscale[],
			int SQUARESIZE,double xscale,double jitter,String label,boolean artcenter)
	{	boolean sprite = SPRITE.equals(label);
		super.drawChipImage(gc, canvas, cx, cy, pscale, SQUARESIZE, xscale, jitter, sprite ? null : label, artcenter);
    	if(color!=null)
    	{	// the special string SPRITE tells us we're drawing from a tile sprite, and therefore
    		// always use the standard rotation for the text
    		double rota = sprite ? 0 : ((canvas.getAltChipset()>>1)&3)*Math.PI/2;
    		Rectangle ar = new Rectangle(cx-SQUARESIZE/2,cy-SQUARESIZE/2,(int)(SQUARESIZE*1.15),(int)(1*SQUARESIZE));
    		//G.frameRect(gc, Color.green, ar);
    		GC.Text(gc,rota, true, ar,color.textColor,null,""+(value/9));
    	}
    }
	
    public MbraneChip getAltChip(int set)
    {
    	return ((((set&1)!=0) && (altChip!=null)) ? altChip : this);
    }
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
		MbraneChip red[] = MbraneColor.Red.instances = new MbraneChip[81];
		MbraneChip black[] = MbraneColor.Black.instances = new MbraneChip[81];
		int idx=0;
		// construct the actual chips from 4 samples, with random variations
		// in size and position to give the overall effect a pleasing variety
		for(int i=0;i<81;i++)
			{
			red[i] = new MbraneChip(blank[idx],MbraneColor.Red,i,0.05);
			idx = (idx+1)&3;
			//red[i].altChip = new MbraneChip(edge[idx],null,i,0.15);
			//idx = (idx+1)&3;			
			black[i]=new MbraneChip(blank[idx],MbraneColor.Black,i,0.05);
			//idx = (idx+1)&3;
			//black[i].altChip = new MbraneChip(edge[idx],null,i,0.15);
			red[i].altChip  = black[i];
			black[i].altChip = red[i];
			}
		Image.registerImages(allChips);
		imagesLoaded = true;
		}
	}
}
