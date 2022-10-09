package lib;

import bridge.Component;

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
	public void drawClientCanvas(Graphics g,boolean complete,HitPoint p);
	public void StartDragging(HitPoint pt);
	public HitPoint MouseMotion(int x,int y,MouseState st);
	public void MouseDown(HitPoint pt);
	public void StopDragging(HitPoint pt);
	public void Pinch(int x,int y,double amount,double twist);
	public void wake();
	public MouseManager getMouse();
	/** get scroll X */
	public int getSX();
	/** get scroll Y */
	public int getSY();
	/** get the global zoom */
	public double getGlobalZoom();
	public Component getComponent();
	public Image getOffScreenImage();
	public int rotateCanvasX(int xx, int yy);
	public int rotateCanvasY(int xx, int yy);
	
}