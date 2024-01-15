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

import bridge.JCheckBoxMenuItem;
import bridge.WindowListener;
import bridge.JMenu;
import bridge.JMenuItem;
/**
 * services provided by the frame which encloses a game window
 * 
 * @author Ddyer
 *
 */
public interface LFrameProtocol extends MenuParentInterface
{   public void setInitialBounds(int theX, int theY, int theW, int theH);
	public void screenResized();
	/**
	 * query if sound is on.  You should call this before playing any sound.
	 * 
	 * @return true if ok to play sounds
	 */
    public boolean doSound();
    /** change the sound permission
     * 
     * @param enable
     */
    public void setDoSound(boolean enable);
    public void addWindowListener(WindowListener who);
    public JCheckBoxMenuItem addOption(String text, boolean initial,DeferredEventManager l);
    public JMenu addChoiceMenu(String item,DeferredEventManager l);
    public JMenuItem addAction(JMenuItem b,DeferredEventManager l);
    public JMenu addAction(JMenu b,DeferredEventManager l);
    public JMenuItem addAction(String text,DeferredEventManager l);
    public void addToMenuBar(JMenu m);
    public void addToMenuBar(JMenu m,DeferredEventManager l);
    public void removeAction(JMenu m);
    public void removeAction(JMenuItem m);

    public void addAction(JMenu m,JMenuItem mi,DeferredEventManager l);
    public JMenuItem addAction(JMenu m,String mname,DeferredEventManager e);
    public JCheckBoxMenuItem addOption(String text, boolean initial, JMenu m,DeferredEventManager l);
    public void killFrame();
    public boolean killed();
    public void dispose();
    public void setTitle(String str);
    public String getTitle();
    public void setDontKill(boolean v);
    public MenuParentInterface getMenuParent();
    public void setVisible(boolean v);
    public void setCanSavePanZoom(DeferredEventManager v);
    public void setHasSavePanZoom(boolean v);
	public void setIconAsImage(Image icon);
	public CanvasRotater getCanvasRotater();
	public TopFrameProtocol getFrame();
	public void moveToFront();
}
