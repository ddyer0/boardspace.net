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
package punct;
import lib.Random;
import lib.exCanvas;
import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.Drawable;
import lib.G;



/** a service class for the punct board/robot to keep track of the pieces
 * TODO: reorganize into an immutable PunctChip class and a mutable PunctPiece class
 * @author ddyer
 *
 */
public class PunctPiece implements PunctConstants,Drawable
{ // constants once the piece is created
  int id = 0;				// unique id for this piece
  int typecode; 			// index into ImageFileNames etc.
  int punct_index=0;		// index into rows
  int cr_offsets[][][];		// offsets relative to the punct for rotation 0-5
  public PunctColor color=null;		// natural color
  int nRotations = 6;		// number of significant rotations
  // changeable state
  int level=POOL_LEVEL;		// level above the board;
  boolean frozen;			// piece is under a stack
  int rotation=0;			// rotation steps
  char cols[]={'@','@','@'};// the columns this piece occupies first index is the punct
  punctCell cells[]=new punctCell[3];	//board cells occupied by this piece
  PunctGameBoard board=null;			//our associated board
  int rows[]=new int[3];	// the rows this piece occupies, first index is the punct
  public String getName() { return(toString()); }
  public int getWidth() { return(0); }
  public int getHeight() { return(0); }
  public PunctPiece copy()
  {
	  PunctPiece p = new PunctPiece(board,id,color,typecode,punct_index,cr_offsets);
	  p.copyFrom(this);
	  return(p);
  }
  public void copyFrom(PunctPiece other)
  {
	G.Assert(id==other.id,"same piece");
  	level = other.level;
  	rotation = other.rotation;
  	frozen = other.frozen;
  	for(int i=0;i<3;i++) 
  	{ cols[i]=other.cols[i]; rows[i]=other.rows[i]; 
  	  punctCell pc = other.cells[i];
  	  if(pc!=null) { cells[i]=board.getCell(pc.col,pc.row); }
  	}
  }
 
  public void rotateCurrentCenter(double amount,int x,int y,int cx,int cy) {};

  public void reInit() 
  { level=POOL_LEVEL; 
    rotation=0;
    frozen=false;
  	for(int i=0;i<3;i++) { cols[i]='@'; rows[i]=0; cells[i]=null;  }
  }
  
  public Image dotImage() { return(images[dotIndex[color.ordinal()]]); }
  
  public long Digest(Random r)
  {	int val=0;
  	long rr = r.nextLong();
  	switch(level)
  	{	
  	default:	// on board
  		for(int i=0;i<3;i++)
  		{	rr = (rr<<level) + rows[i]*100+cols[i];
  		}
  		val ^= rr;
  		break;
  	case POOL_LEVEL:
  		val ^= rr;
  		break;
  	case TRANSIT_LEVEL:
  		val ^= (rr>>1);
  	}
    return(val);
  }
  /* constructor */
  public PunctPiece(PunctGameBoard bd,int idn,PunctColor col,int type,int subtype,int offsets[][][]) 
  { id=idn;color=col; typecode=type; punct_index=subtype;
  	board=bd;
    nRotations = ((typecode==STRAIGHT_INDEX)&&(punct_index==MID_SUBINDEX)) ? 3 : 6; 
    if(offsets!=null)
    	{cr_offsets = offsets;
    	}
  }
  public boolean samePieceType(PunctPiece other)
  {	return((other==null) ? false : (typecode==other.typecode)&&(punct_index==other.punct_index));
  }
  
  /* image index for drawing the piece */
  public int effective_rotation()
  {	switch(typecode)
	  {
  	default: throw G.Error("doesn't rotate");
  	case PUNCT_INDEX: return(0);
  	case TRI_INDEX: 
  	case Y_INDEX:
  	case STRAIGHT_INDEX: return(rotation);
   }
  }
  private int imageIndex() 
  { 
  	switch(typecode)
	  {default: throw G.Error("Not expecting type %s",typecode);
	   case PUNCT_INDEX: return(typecode+color.ordinal());
  	   case TRI_INDEX: return(typecode+(rotation&1)*2+color.ordinal()) ;
  	   case Y_INDEX: return(typecode+rotation*2+color.ordinal());
  	   case STRAIGHT_INDEX: return(typecode+(rotation%3)*2+color.ordinal());
	  }
 } 
  // return the scaleset for drawing the punct dot
  public double[] dotScaleSet()
  {
   return(dot_scale[color.ordinal()][0]);
  }

  // return the scaleset for drawing this piece, as rotated
  public double[] scaleSet()
  {	int rot = effective_rotation();
   	switch(typecode)
	  {	default: throw G.Error("no scaleset");
	    case PUNCT_INDEX:
	    	//return(new double[]{0.55,0.16,1.3});
	    	return(punct_scale[color.ordinal()][0]);
	    case TRI_INDEX: return(tri_scale[color.ordinal()][rot]);
	    case STRAIGHT_INDEX:
	    	switch(punct_index)
	    	{
	    	default: throw G.Error("No scaleset for straight");
	    	case MID_SUBINDEX: return(straight_scale_c[color.ordinal()][rot]);
	    	case TOP_SUBINDEX: return(straight_scale_t[color.ordinal()][rot]);
	    	}
	    case Y_INDEX:
	    	switch(punct_index)
	    	{
	    	default: throw G.Error("No scaleset for straight");
	    	case TOP_SUBINDEX: return(y_scale_t[color.ordinal()][rot]);
	    	case MID_SUBINDEX: return(y_scale_c[color.ordinal()][rot]);
	    	case BOT_SUBINDEX: return(y_scale_b[color.ordinal()][rot]);
	    	}
	  }
  }
	static double defaultScale[] = { 1.5,0.89,-0.2,-0.8};	
	static double triScale[][] = 
  		{{ 1.5,0.9,-0.2,-1.2},
  	  	 { 1.5,0.9,0,-1},
  	  	 { 1.5,0.9,0,-0.5},
  	  	 { 1.5,0.9,-0.4,-0.5},
  	  	 { 1.5,0.9,-0.8,-1},
  	  	 { 1.5,0.9,-0.5,-1.4},
  		};
	static double straight_scale[][] =
    	{
    	{1.5,0.9,0,-1.4},		// white rotation 0
        {1.5,0.9,0,-1},		// white rotation 1
        {1.5,0.9,0,-0.5},		// white rotation 2
        {1.5,0.9,0,0},		// white rotation 3
        {1.5,0.9,-0.7,-0.5},		// white rotation 4
        {1.5,0.9,-0.8,-1}};	// white rotation 5
	static double y_scalea[][] =
        {{1.5,1,0,-1.4},	// white rotation 0
         {1.5,1,0,-1},		// white rotation 1
         {1.5,1,0,-0.4},		// white rotation 2
         {1.5,1,-0.3,-0.3},	// white rotation 3
         {1.5,1,-1,-1},	// white rotation 4
         {1.5,1,-0.7,-1.2}};	// white rotation 5
	static double y_scaleb[][] =
        {{1.5,1,-0.5,0},	// white rotation 0
         {1.5,1,-1.2,-1},		// white rotation 1
         {1.5,1,0,0},		// white rotation 2
         {1.5,1,0,-1.3},	// white rotation 3
         {1.5,1,0,-0.8},	// white rotation 4
         {1.5,1,0,0}};	// white rotation 5


   public double[] chipScale()
  {	int rot = effective_rotation();
   
   	switch(typecode)
	  {	default: throw G.Error("no scaleset");
	    case PUNCT_INDEX: 
	    case TRI_INDEX: return(triScale[rot]);
	    case STRAIGHT_INDEX:
	    	switch(punct_index)
	    	{
	    	default: 
	    	case MID_SUBINDEX: return(defaultScale);
	    	case TOP_SUBINDEX: return(straight_scale[rot]);
	    	}
	    case Y_INDEX:
	    	switch(punct_index)
	    	{
	    	default: throw G.Error("No scaleset for straight");
	    	case TOP_SUBINDEX: return(y_scalea[rot]);
	    	case MID_SUBINDEX: return(defaultScale);
	    	case BOT_SUBINDEX: return(y_scaleb[rot]);
	    	}
	  }
  }
  //pretty print
  public String toString()
    { String points = "";
      for(int i=0;i<3;i++) 
      { char col=cols[i];
        String ss = ""+col+rows[i];
        if(i==punct_index) 
        { ss = "["+ss+"]";
        }
        points += ss;
      }
      return("<Piece "+playerChar[color.ordinal()]+pieceID[id]+" #"+id+" "+" "+ImageFileNames[typecode]+"-"+punct_index+" "+level+points+">");
    }

public void drawChip(Graphics gc, exCanvas c, int size, int posx, int posy, String msg) 
{
	c.drawImage(gc, getImage(), scaleSet(),posx,posy,size,1.0,0.0,null,true);
	c.drawImage(gc, dotImage(),dotScaleSet(), posx, posy, size,1.0,0.0,msg,true);
}

static Image images[] = null;
public Image getImage() { return(images[imageIndex()]); }

public static void preloadImages(ImageLoader forcan,String ImageDir)
{	if(images==null)
	{
	images = forcan.load_images(ImageDir, ImageFileNames, 
			forcan.load_images(ImageDir, ImageFileNames,"-mask")); // load the main images
	}
}

public int animationHeight() {
	return(1);
}
public Drawable animationChip(int depth) 
{ return(this); 
}
	public double activeAnimationRotation() { return(0); }

}
