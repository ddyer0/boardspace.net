package lib;

/**
 * this interface pairs with MouseManager, it defines the callbacks used by MouseManager
 * 
 * @author Ddyer
 *
 */
public interface MouseClient extends SizeProvider
{
	public void repaintForMouse(int n,String s);
	public void stopPinch();
	public boolean hasMovingObject(HitPoint pt);
	
	public void performStandardStartDragging(HitPoint pt);
	public void performStandardStopDragging(HitPoint pt);
	public HitPoint performStandardMouseMotion(int x,int y,MouseState pt);
	
	public void StartDragging(HitPoint pt);
	public void StopDragging(HitPoint pt);
	public HitPoint MouseMotion(int x,int y,MouseState st);
	
	public void MouseDown(HitPoint pt);
	public void Pinch(int x,int y,double amount,double twist);
	public void Wheel(int x,int y,int buttonm,double amount);
	
	public void wake();
	public MouseManager getMouse();
	public int rotateCanvasX(int xx, int yy);
	public int rotateCanvasY(int xx, int yy);
	
}