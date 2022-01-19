package vnc;

public class BitmapTile 
{	int data[];
	int width;
	int height;
	int to_x;
	int to_y;
	public BitmapTile(int d[],int w,int h,int tx,int ty)
	{
		data = d;
		width = w;
		height = h;
		to_x = tx;
		to_y = ty;
	}
}
