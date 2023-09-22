/* copyright notice */package volo;

import lib.G;
import lib.OStack;

class BlobStack extends OStack<VoloBlob>
{
	public VoloBlob[] newComponentArray(int n) { return(new VoloBlob[n]); }
}
/** a service class for the volo board/robot to keep track of blobs of like-colored
 * markers or of their complement.  Blob membership is maintained as a bit set in
 * two longs, so the maximum size is 128.
 *
 */
public class VoloBlob implements VoloConstants
{ VoloChip color;					// The chip type of members
  int playerIndex;
  public boolean inverse = false;	// if true, the complement of the chip type is members.
  int size = 0;						// number of members
  int sumofX = 0;
  int sumofY = 0;
  private long members1 = 0;		// the set members
  private long members2 = 0;
  // constructor
  public VoloBlob(VoloChip col,int player,boolean no) { color = col; playerIndex=player; inverse=no; }
  public void reInit() { size = 0; members1 = 0; members2 = 2; }
  //pretty print
  public static String[] nameString = new String[129];		// max size of set is 128
  public String toString()
     {
       return("<blob:" 
     		  + size + " "
     		  + (inverse?"not-":"")+color+" "
     		  + G.format("%s %s ",members1,members2)
     		  + (nameString[lowestMemberOrdinal()])
     		  +">");
     }

  // combine two blobs
  public void union(VoloBlob other)
  {		members1 |= other.members1;
  		members2 |= other.members2;
  		size = G.bitCount(members1)+G.bitCount(members2);
  }
  public int lowestMemberOrdinal()
  {	if(size==0) { return(0); }
  	int bit1 = G.numberOfTrailingZeros(members1);
  	if(bit1<64) { return(bit1); }
  	return(bit1+G.numberOfTrailingZeros(members2));
  }

  // true if the blobs have no members in common
  public boolean emptyIntersection(VoloBlob other)
  {	return(((members1 & other.members1)==0) && ((members2 & other.members2)==0));
  }
  
  // true if this blob contains all of the other
  public boolean containsAllOf(VoloBlob other)
  {
	  return(((other.members1 & ~members1)==0)
	  	&& ((other.members2 & ~members2)==0));
  }
  // true of the blob contains this cell
  public boolean contains(VoloCell c) 
  {		return(containsOrdinal(c.cellNumber));
  }
  public final boolean containsOrdinal(int n)
  {		int row = n>>6;
  		int col = n&0x3f;
  		long mask = (1L<<col);
  		switch(row)
  		{	case 0:	return((mask&members1)!=0);
  			case 1: return((mask&members2)!=0);
		default:
			break;
  		}
  		return(false);
  }
  // add a member to the blob
  public void addCell(VoloCell c) 
  {		int row = c.cellNumber>>6;
  		int col = c.cellNumber&0x3f;
  		long mask = (1L<<col);
  		//if(mask==0) { G.Error("bit number out of range",c); }
  		switch(row)
  		{	case 0:	if((members1&mask)==0) { size++; members1|=mask; sumofX += c.row; sumofY += c.col-'A';}  return;
  			case 1: if((members2&mask)==0) { size++; members2|=mask; sumofX += c.row; sumofY += c.col-'A';}  return;
		default:
			break;
  		}
  		//G.Error("cell number out of range");
  }
  // remove a member from the blob
  public void removeCell(VoloCell c) 
  {	  removeOrdinal(c.cellNumber);
  }
  // remove an ordinal member from a blob
  public final void removeOrdinal(int cellNumber)
  {		int row = cellNumber>>6;
  		int col = cellNumber&0x3f;
  		long mask = (1L<<col);
  		//if(mask==0) { G.Error("bit number out of range",c); }
  		switch(row)
  		{	case 0:	if((members1&mask)!=0) { size--; members1&=~mask; } return;
  			case 1: if((members2&mask)!=0) { size--; members2&=~mask; } return;
		default:
			break;
  		}
  		//G.Error("cell number out of range");
  }
 



 }
