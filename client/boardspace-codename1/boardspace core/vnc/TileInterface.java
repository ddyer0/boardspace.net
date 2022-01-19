package vnc;

public interface TileInterface
{	public boolean update(int[]newdata,int x,int y,int w,int h,int span);
	public boolean update(byte[]newdata);
	public boolean update(int[] newdata);

	public byte[] getData();

	public int getHeight();

	public int getWidth();
	

}
