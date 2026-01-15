package circle;

import lib.Digestable;
import lib.Drawable;
import lib.DrawingObject;
import lib.G;
import lib.Graphics;
import lib.OStack;
import lib.Random;
import lib.StackIterator;

class CritterStack extends OStack<Critter> implements Digestable
{
	public Critter[] newComponentArray(int sz) {
		return new Critter[sz];
	}

	public long Digest(Random r) {
		long v = 0;
		for(int lim=size()-1; lim>=0; lim--) { v ^= elementAt(lim).Digest(r);}
		return v;
	}
	
}
public class Critter extends OStack<CircleCell> implements CircleConstants,Digestable,Drawable
{
	CircleChip top = null;
	Critter next = null;
	public Critter(CircleChip t) { top = t; }
	
	public StackIterator<CircleCell> push(CircleCell ch)
	{
		G.Assert(size()<=3,"can't enlarge %s",ch);
		return super.push(ch);
	}
	public CircleCell[] newComponentArray(int sz) {
		return new CircleCell[sz];
	}
	public String toString() { return "<critter "+size()+" "+identity()+">"; }
	public CR identity = null;
	public CR identity()
	{	int sz = size();
		if(identity==null) 
		
		switch(sz)
		{
		default: 
			G.Error("illegal critter, size %s",sz);
			//$FALL-THROUGH$
		case 0:	return null;
		
		case 1: identity = CR.Single;
			break;
		case 2: identity = CR.Double;
			break;
		case 3:
			for(int i=0;i<sz && identity==null;i++)
			{
				CircleCell c = elementAt(i);
				CC cl = c.getConnectionClass();
				switch(cl)
				{
				case One: break;
				case TwoAdjacent:	
					identity = CR.Triplet;
					break;
				case TwoSkip1:
					identity = CR.ThreeAngle;
					break;
				case TwoSkip2:
					identity = CR.ThreeLine;
					break;
				default:
					G.Error("illegal config %s",cl);
				}			
			}
			G.Assert(identity!=null,"not classified");
			break;
		case 4:
			{
			int twoangles = 0;
			int ones = 0;
			int twostraights = 0;
			CircleCell oneA = null;
			CircleCell oneB = null;
			for(int i=0;i<sz && identity==null;i++)
			{
				CircleCell c = elementAt(i);
				CC cl = c.getConnectionClass();
				switch(cl)
				{
				case TwoAdjacent:
					break;
				case ThreeSkipSkip:
					identity = CR.FourStar;
					break;
				case TwoSkip2:
					twostraights++;
					break;
				case TwoSkip1:
					twoangles++;
					break;
				case One:
					oneA = oneB;
					oneB = c;
					ones++;
					break;
				case ThreeSkip1: 
					 identity = CR.FourClub; 
					break;
				case ThreeAdjacent:
					identity = CR.FourBlob; 
					break;
				default: G.Error("Not classified %s",cl);
				}
			}
			if(identity==null)
			{
				if(twoangles==1) { identity = CR.FourAngle; }
				else if(twostraights>0) { identity = CR.FourStraight; }
				else
				{	// we're left with the two cases, the U and the Z
					G.Assert(ones==2,"should be 2 ones");
					for(int dir=0;dir<6 && identity==null;dir++)
					{	// for the U shape, the first and last cells are type "one"
						// and there is a cell between them.
					CircleCell mid1 = oneA.exitTo(dir);
					CircleCell mid2 = oneB.exitTo(dir+CircleBoard.CELL_HALF_TURN);
					if(mid1 == mid2) { identity = CR.FourU; }
					}
					if(identity==null) { identity = CR.FourZZ;}
				}
			}
			G.Assert(identity!=null,"not classified");
			}
			break;
		}
		G.Assert(identity!=null,"should be id'd");
		return identity;
	}
	
	public void findCritter(CircleCell c)
	{	push(c);
		c.myCritter = this;
		for(int dir = 0; dir<6; dir++)
		{
			CircleCell next = c.exitTo(dir);
			if(next!=null && next.myCritter!=this && next.topChip()==top)
			{	findCritter(next);
			}
		}
	}
	
	public void forget()
	{
		for(int i=0;i<size();i++) { 
			CircleCell c = elementAt(i);
			if(c.myCritter==this) { c.myCritter = null; }
		}
	}
	public void remember()
	{
		for(int i=0;i<size();i++) { 
			CircleCell c = elementAt(i);
			c.critter();
		}
	}

	public long Digest(Random r) {
		long v = 0;
		for(int lim=size()-1; lim>=0; lim--) { v ^= elementAt(lim).Digest(r); }
		return v;
	}

	public void drawChip(Graphics gc, DrawingObject c, int size, int posx, int posy, String msg) 
	{
		 CR id = identity();
		 id.drawChip(gc,c,top,0,size,posx,posy,null);		
	}

	public void rotateCurrentCenter(double displayRotation, int x, int y, int px, int py) {
		
	}

	public double activeAnimationRotation() {
		return 0;
	}

	public int animationHeight() {
		return 0;
	}

	public String getName() {
		return toString();
	}

	public int getWidth() {
		return identity().getIconWidth();
	}

	public int getHeight() {
		return identity().getIconHeight();
	}
	
}
