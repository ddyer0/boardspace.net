package lib;

import bridge.Color;

public class Location implements LocationProvider {
	int x;
	int y;
	Color color = null;
	public int getX() { return x; }
	public int getY() { return y; }
	public Location(int xx,int yy) { x = xx; y=yy; }
	public Location(int xx,int yy,Color c) { this(xx,yy); color = c; }
	public Color getColor() { return color; }
}
