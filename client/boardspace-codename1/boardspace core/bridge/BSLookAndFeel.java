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

import lib.G;

import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.List;
@SuppressWarnings("deprecation")
public class BSLookAndFeel extends DefaultLookAndFeel 
{	public BSLookAndFeel(UIManager m) { super(m); }
	
	public Dimension getListPreferredSize(@SuppressWarnings("rawtypes") List l)
	{
		Dimension dim = super.getListPreferredSize(l);
		dim.setWidth(dim.getWidth()+(int)(20*G.getDisplayScale()));
		return(dim);
	}

}
