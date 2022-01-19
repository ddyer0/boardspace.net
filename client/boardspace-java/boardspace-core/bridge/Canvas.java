package bridge;

import java.awt.Component;
import java.awt.Container;
import java.awt.LayoutManager;

import lib.G;
import lib.Graphics;
import lib.SizeProvider;
import lib.TextContainer;

@SuppressWarnings("serial")
public abstract class Canvas extends java.awt.Canvas implements SizeProvider 
{
	public void paint(java.awt.Graphics g)
	{	//if(!isDoubleBuffered()) { setDoubleBuffered(true); }
		paint(Graphics.create(g));
	}
	public void update(java.awt.Graphics g)
	{	update(Graphics.create(g));
	}
	public void update(lib.Graphics g)
	{
		actualUpdate(g);
	}
	public void actualUpdate(lib.Graphics g)
	{
		super.update(g.getGraphics());
	}
	public void paint(lib.Graphics g)
	{ 
		actualPaint(g);
	}
	public void actualPaint(Graphics g)
	{	// in windows based on Container, this would paint the components
	}
	public void actualRepaint(int n)
	{	super.repaint(n);
	}
	public void actualRepaint() { super.repaint(); }
	public Component add(Component c) { throw G.Error("shouldn't be called"); }
	public Component getComponent() { return(this); }
	public void removeThis() 
	{	Container par = getParent();
		if(par!=null) { par.remove(this); }
	}
	public void setLayout(LayoutManager m) {};
	public Component getMediaComponent() { return(this); }
	public void requestFocus(TextContainer p) { requestFocus(); }
}
