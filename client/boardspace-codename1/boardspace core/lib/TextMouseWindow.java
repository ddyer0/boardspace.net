package lib;

import com.codename1.ui.Font;

import bridge.Utf8OutputStream;
import bridge.WindowEvent;
import bridge.WindowListener;
import lib.RepaintManager.RepaintHelper;
import lib.RepaintManager.RepaintStrategy;
/**
 * A window that displays text.  This is used as a console window
 * by textdisplayframe
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
public class TextMouseWindow extends MouseCanvas implements  RepaintHelper,WindowListener , Runnable
{
	TextContainer area = new TextContainer("");
	RepaintManager painter = new RepaintManager(this,
			G.isCodename1()
				? RepaintStrategy.Direct_Unbuffered
				: RepaintStrategy.Direct_SingleBuffer);
	public void drawBackground(Graphics gc, Image image) 
	{
		
	}
	public void resetLocalBoundsIfNeeded()
	{
		
	}
	public TextMouseWindow(LFrameProtocol f)
	{	super(f);
		painter.hasRunLoop = painter.drawLockRequired = false;
		f.addWindowListener(this);
		area.addObserver(this);
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
	
	public void MouseDown(HitPoint p)
	{
	}

	public HitPoint MouseMotion(int ex, int ey,MouseState upcode)
	{	
		area.doMouseMove(ex,ey,upcode);
		repaint(100);
		return null;
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
	public void ShowStats(Graphics gc, HitPoint hp,int i, int j) {
		
	}
	public void showRectangles(Graphics gc, HitPoint p,int i) {
		
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

	public TextPrintStream getPrinter()
	{
		return TextPrintStream.getPrinter(new Utf8OutputStream(),area);

	}
	
	/* for window listener */
	public void windowClosing(WindowEvent e) {
		painter.shutdown();
		shutdown();
	}

	public void ViewerRun(int waitTime)
	{
		mouse.performMouse();
  	  	//if(mouse.isDown()) { repaintSprites(); }	// keep the painter active if the mouse is down
  	  	painter.repaintAndOrSleep(waitTime); 
        mouse.performMouse();
        //repaint(100);
	}
	boolean exitRequest = false;
	public void run()
	{
		while(!exitRequest)
		{
			ViewerRun(100);
		}
	}
	public void shutdown()
	{	exitRequest = true;
	}
	public void startProcess()
	{	new Thread(this).start();
	}
}
