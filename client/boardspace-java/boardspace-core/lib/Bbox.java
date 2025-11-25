package lib;

public class Bbox 
{
	public int left=0;
	public int top=0;
	public int right=-1;
	public int bottom=-1;
	public int cellsize = 0;
	public String toString() { return "<Bbox "+left+","+top+" "+right+","+bottom+">";}
	public void addPoint(int x,int y)
	{
		if (left>right)
        {   left = x;
            top = y;
            right = left;
            bottom = top;
        }
        else
        {   if (x < left) {  left = x;  }
            if (x > right)  { right = x;  }
            if (y < top) {  top = y; }
            if (y > bottom) { bottom = y; }
        }
	}
}