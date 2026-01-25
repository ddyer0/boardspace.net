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

import com.codename1.ui.Font;

import lib.FontManager;

public class PopupMenu extends Menu {
	private boolean showing = false;
	public PopupMenu() 
		{ super(); }
	public PopupMenu(String m)
		{ super(m); }
	public PopupMenu(String m,Font f) { this(m); setFont(f==null?FontManager.menuFont():f); }
	
	// this "light weight popup" stuff relates to using native menus
	// rather than java widgets.
	private static boolean defaultLightWeightPopup = true; 
	@SuppressWarnings("unused")
	private boolean lightWeightPopup = defaultLightWeightPopup;
	public void setLightWeightPopupEnabled(boolean b) { lightWeightPopup = b; }
	public static void setDefaultLightWeightPopupEnabled(boolean b) { defaultLightWeightPopup = b; }

	public void actionPerformed(ActionEvent e)
	{	super.actionPerformed(e);
		Object target = e.getSource();
		com.codename1.ui.Container on = getShowingOn();
		if((target==getMenu()) && (on!=null))
			{ getMenu().setVisible(false);
			  showing=false; 
			} 
	}
	public boolean isVisible() {
		// this gets called during initialization for undetermined reasons.
		return(showing); 
		}
	public boolean getTopLevel() { return(true); }
	int ncols = 1;
	public void setNColumns(int n) {
		ncols = n;
	}
	public boolean useSimpleMenu() { return ncols>1; }

}
