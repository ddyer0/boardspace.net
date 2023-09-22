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
package bridge;

//
//this class is an attempt to integrate scrolling
//with selection.  If you click you get a selection
//if you click and drag you get scrolling, and no selection
//
public class ScrollableList<E> extends List<E> implements MouseListener
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;

	public ScrollableList(int i, boolean b) 
	{
		super(i, b);
		addMouseListener(this);
	}
	// with the standard codename1 api, it's impossible to revert to a "nothing selected"
	// state.  This is a workaround, to allow something to be selected only when the button
	// is pressed.
	boolean nothingSelected = true;

	public void pointerReleased(int x,int y)
	{
		super.pointerReleased(x,y);
	}
	public void removeAll()
	{
		super.removeAll();
		nothingSelected = true;
	}
	public E getSelectedItem()
	{
		if(nothingSelected) { return(null); }
		return(super.getSelectedItem());
	}
	public int getSelectedIndex()
	{
		if(nothingSelected) { return(-1); }
		else { return(super.getSelectedIndex());}
	}

	public void mouseClicked(MouseEvent e) {
		nothingSelected = false;
		
	}
	public void mousePressed(MouseEvent e) {
		nothingSelected = false;
		
	}
	public void mouseReleased(MouseEvent e) {
		nothingSelected = false;
		
	}
	public void mouseEntered(MouseEvent e) {
		
		
	}
	public void mouseExited(MouseEvent e) {
		
	}
}