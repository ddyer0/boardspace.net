package lib;

public class DrawnIcon implements Drawable
{	public int w = 0;
	public int h = 0;
	public Object parameter;
	public DrawnIcon(int ww,int hh,Object p)
	{
		w = ww;
		h = hh;
		parameter = p;
	}
	public void draw(Graphics gc, DrawingObject c, int size, int posx, int posy, String msg) {
		G.Error("drawChip must be overridden");
		
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
		return "icon";
	}

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		
		return h;
	}

}
