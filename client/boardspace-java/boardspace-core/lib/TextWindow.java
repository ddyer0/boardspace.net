package lib;

import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import bridge.Canvas;
import bridge.Utf8OutputStream;
import lib.RepaintManager.RepaintHelper;
import lib.RepaintManager.RepaintStrategy;

@SuppressWarnings("serial")
public class TextWindow extends Canvas implements MouseListener,MouseMotionListener,MouseWheelListener,RepaintHelper,WindowListener 
{
	TextContainer area = new TextContainer("");
	RepaintManager painter = new RepaintManager(this,
			G.isCodename1()
				? RepaintStrategy.Direct_Unbuffered
				: RepaintStrategy.Direct_SingleBuffer);
	public void drawBackground(Graphics gc) 
	{
		
	}
	public void resetLocalBoundsNow()
	{
		
	}
	public TextWindow(Frame f)
	{	
		painter.hasRunLoop = painter.drawLockRequired = false;
		addMouseMotionListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		f.addWindowListener(this);
		if(G.debug()) { G.print(painter.repaintStrategy); }
	}
	public void setReport(boolean v) { painter.setRecord(v); }
	
	public void setVisible(boolean v)
	{	area.setVisible(v);
		super.setVisible(v);
		// this shouldn't be necessary, but windows seems to lose
		// the contents of the initial drawing some percentage of 
		// the time.  This may be tied to volatile bitmaps.
		repaint(1000);
	}
	public void setBounds(int l,int t,int w,int h)
	{	super.setBounds(l, t, w, h);
		area.setBounds(l, t, w, h);
	}
	public void setFont(Font f)
	{	super.setFont(f);;
		area.setFont(f);
	}
	public void update(Graphics g)
	{	
		painter.update(g);
	}
	public void paint(Graphics g)
	{	painter.paint(g);
	}
	
	public void selectAll() { area.selectAll(); }

	public void setText(String s) {
		area.setText(s);
	}
	
	// for MouseListener
	public void mouseClicked(MouseEvent e) {
		//area.doMouseMove(e.getX(),e.getY(),MouseState.LAST_IS_DOWN);		
	}
	public void mousePressed(MouseEvent e) {
		area.doMouseMove(e.getX(),e.getY(),MouseState.LAST_IS_DOWN);	
		repaint(100);
	}
	public void mouseReleased(MouseEvent e) {
		area.doMouseMove(e.getX(),e.getY(),MouseState.LAST_IS_UP);
		repaint(100);
	}
	public void mouseEntered(MouseEvent e) {
		area.doMouseMove(e.getX(),e.getY(),MouseState.LAST_IS_ENTER);
	}
	public void mouseExited(MouseEvent e) {
		area.doMouseMove(e.getX(),e.getY(),MouseState.LAST_IS_EXIT);
		repaint(100);
	}
	//
	// for MouseMotionListener
	//
	public void mouseDragged(MouseEvent e) {
		area.doMouseMove(e.getX(),e.getY(),MouseState.LAST_IS_DRAG);
		repaint(100);
	}
	public void mouseMoved(MouseEvent e) {
		area.doMouseMove(e.getX(),e.getY(),MouseState.LAST_IS_MOVE);
		repaint(100);
	}
	public void mousePinched(PinchEvent e)
	{
	
	}
	//
	// for repaintHelper
	//
	public String getErrorReport() {
		return "";
	}

	public void actualPaint(Graphics g, HitPoint hp) {
		super.actualPaint(g);		
	}
	public boolean lowMemory() {
		return false;
	}
	public void setLowMemory(String s) {
		
	}

	public void handleError(String s, String context, Throwable r) {
		G.Error(s,r);
	}
	public void repaint(int n) { painter.repaint(n,"from textwindow"); }
	public void drawClientCanvas(Graphics g, boolean complete, HitPoint p) {
		//G.finishLog( );
		area.redrawBoard(g,p);
		if(area.activelyScrolling())
			{ area.doRepeat();
			 // G.startLog("Scrolling");
			  repaint(100); 
			}
	}
	public void ShowStats(Graphics gc, int i, int j) {
		
	}
	public void showRectangles(Graphics gc, int i) {
		
	}
	public Font getDefaultFont() {
		return getFont();
	}
	
	public void manageCanvasCache(int time) {
		
	}

	public boolean needsCacheManagement() {
		return false;
	}
	public void paintSprites(Graphics offGC, HitPoint pt) {		
	}
	
	public boolean globalPinchInProgress() {
		return false;
	}

	public void drawActivePinch(Graphics g, Image im, boolean useLast) {
		
	}
	public int getSX() {
		return 0;
	}

	public int getSY() {
		return 0;
	}

	public double getRotation() {
		return 0;
	}

	public double getGlobalZoom() {
		return 1;
	}
	/** for mouseWheelListener */
	public void mouseWheelMoved(MouseWheelEvent e) {
		int up = e.getWheelRotation();
		area.doMouseWheel(up);
		repaint(100);
	}
	public TextPrintStream getPrinter()
	{
		return TextPrintStream.getPrinter(new Utf8OutputStream(),area);

	}
	/* for window listener */
	public void windowOpened(WindowEvent e) {
		
	}
	public void windowClosing(WindowEvent e) {
		painter.shutdown();
	}
	public void windowClosed(WindowEvent e) {
		
	}
	public void windowIconified(WindowEvent e) {
		
	}
	public void windowDeiconified(WindowEvent e) {
		
	}
	public void windowActivated(WindowEvent e) {
		
	}
	public void windowDeactivated(WindowEvent e) {
		
	}


}