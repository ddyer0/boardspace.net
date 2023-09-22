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