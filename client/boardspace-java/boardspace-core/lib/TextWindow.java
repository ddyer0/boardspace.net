/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package lib;

import java.awt.Font;
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
/**
 * A window that displays text.  This is used as a console window
 * by textdisplayframe
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
public class TextWindow extends Canvas implements MouseListener,MouseMotionListener,MouseWheelListener,RepaintHelper,WindowListener,NullLayoutProtocol 
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
	public TextWindow(LFrameProtocol f)
	{	super(f);
		setLayout(new NullLayout(this));
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
	/** for mouseWheelListener */
	public void mouseWheelMoved(MouseWheelEvent e) {
		area.doMouseWheel(e.getX(),e.getY(),e.getWheelRotation());
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
	}
	
	public void startProcess() {
		// this is a dummy, we have no process.  TextMouseWindow has a process
	}

	public void doNullLayout()
	{
		area.setBounds(getX()+1,getY()+1,getWidth()-3,getHeight()-3);
	}
}
