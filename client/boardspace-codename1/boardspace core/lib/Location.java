package lib;

public class Location implements LocationProvider {
	int x;
	int y;
	public int getX() { return x; }
	public int getY() { return y; }
	public Location(int xx,int yy) { x = xx; y=yy; }
}
