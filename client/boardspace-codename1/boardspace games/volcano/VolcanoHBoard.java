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
package volcano;

import lib.G;
import online.game.*;
/*
 * This is a minimal, game specific extension to hexboard, structured
 * so the real board can use either a rectboard or hexboard extension
 * both the main "board" class and the main "viewer" class use this
 * object to locate board cells, and to translate board cells to geometry
 * 
 */


public class VolcanoHBoard extends hexBoard<VolcanoCell> implements VolcanoConstants
{ 	// factory method
	public VolcanoCell newcell(char c,int r)
	{	return(new VolcanoCell(c,r,cell.Geometry.Hex));
	}
	public BoardState getState() { throw G.Error("shouldn't be called"); }

    public VolcanoHBoard() // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_NOTHING or STYLE_CELL or STYLE_LINES
    	Grid_Style = VOLCANOGRIDSTYLE; //coordinates left and bottom
    	Init_Hex();
     }

    public void doInit(String var,long rv) { G.Error("shouldn't be called"); }
 
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    public void Init_Hex()
    { 
    	initBoard(ZfirstInCol, ZnInCol, null);
       // init the board with pyramids board code
     VolcanoCell center = null;
     for(VolcanoCell c = allCells; c!=null; c=c.next)
      {	int ir[] = starting_hex_colors[c.col-'A'];
        for(int idx=0;idx<Pyramid.nSizes;idx++)
     			{ int color = ir[c.row-1];
     			  if(color>=0) { c.addChip(Pyramid.getPyramid(color,Pyramid.nSizes-idx-1)); }
     			  else { center=c; }
     			}
     		}
     
		// finally add the caps
		Pyramid p = Pyramid.getPyramid(Pyramid.BLACK_PYRAMID,Pyramid.SMALL_PYRAMID);
		for(int direction = 0;direction<CELL_FULL_TURN; direction++)
			{ VolcanoCell nx = center.exitTo(direction);
			  nx.addChip(p); 
			}


    }


}
