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

import java.awt.Color;
import java.awt.Rectangle;

import lib.SeatingChart.DefinedSeating;

public interface GameLayoutClient 
{  
	public enum BoxAlignment { Top,Center,Bottom,Edge,Left,Right }
	public enum Purpose
	{
		Chat,Done,DoneEdit,DoneEditRep,Edit,
		Log,Banner,Vcr,Draw,
		Other;
	}
	Rectangle createPlayerGroup(int player, int x, int y, double rotation,int unit);	
	DefinedSeating seatingChart();
	int standardFontSize();
	void SetupVcrRects(int left, int top, int width, int height);
	boolean isZoomed();
	void positionTheChat(Rectangle actual, Color back,Color fore);
	boolean canUseDone();
}
