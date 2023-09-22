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
package exxit;
import lib.Random;
import online.game.chip;
import lib.G;
import lib.Image;
import lib.ImageLoader;

/**
 * this is not completely converted to the modern paradigm, only enough
 * so animations will work.
 * @author ddyer
 *
 */
public class ExxitPiece extends chip<ExxitPiece> implements ExxitConstants
{
	  private int imageIndex = 0;		// unique id for this piece
	  int typecode; 			// index into ImageFileNames etc.
	  int seq=0;				// sequenceNumber for this bug
	  ExxitCell location=null;	// where this piece currently resides
	  ExxitCell home_location=null;
	  public String prettyName = "";	// short bug name
	  public int colorIndex=-1;		// associated color
	  public ExxitPiece(int pIndex,int type,int pl,int seqn,long dig)
	  {	
	  	imageIndex = pIndex;
	  	typecode = type;
	  	colorIndex = pl;
	  	seq = seqn;
	  	randomv = dig;
	  	prettyName = (type==TILE_TYPE) ? TILE_NAMES[pl] : CHIP_NAMES[pl];
	  }
	  public void setColor(int cl)
	  {	G.Assert(typecode==TILE_TYPE,"can only change tiles");
	  	colorIndex = cl;
	  	imageIndex = typecode*2+cl;
	  	prettyName = TILE_NAMES[colorIndex];
	  }
	  public static long Digest(Random r,ExxitPiece c)
		{
			return(r.nextLong()*((c==null)?0:c.Digest()));
		}
	  public String toString() 
	  { String loc = (location==null)?"@nowhere":("@"+location.col+location.row);
	    return("<" + prettyName+"#"+seq+loc+">");
	  }

	  public int imageIndex(boolean flip) 
	  { return(flip?imageIndex^1:imageIndex);
	  }
	  static Image images[] = null;
	  static void preloadImages(ImageLoader can,String ImageDir)
	  {		if(images==null)
	  		{
	        images = can.load_masked_images(ImageDir, ImageFileNames); // load the main images
	  		}
	  }
	  public Image getImage() 
	  	{ return(images[imageIndex]); 
	  	}
	  public Image getImage(ImageLoader canvas)
		{   // canvas may be null
			return(images[imageIndex]); 
		}
}
