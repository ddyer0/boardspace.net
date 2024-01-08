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

import lib.AwtComponent;
import lib.G;
import lib.Image;
import lib.SizeProvider;

import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Font;
import com.codename1.ui.List;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

//
// this is a workaround to keep pop-ups from apprearing too close to the top
//
class ComboDialog extends Dialog implements SizeProvider
{
	ComboDialog(String uu,String lm)
	{ super();
	  setTitleComponent(new Label(Image.createImage(1,1)));
	}
	
	@SuppressWarnings("deprecation")
	public Command show(int top,int bot,int left,int right,boolean title,boolean modal)
	{	
		return super.show(Math.max(top, G.minimumFeatureSize()),bot,left,right,title,modal);
	}
	public void paint(com.codename1.ui.Graphics g0)
	{
		boolean rotated = MasterForm.rotateNativeCanvas(this,g0);
		super.paint(g0);
		if(rotated) { MasterForm.unrotateNativeCanvas(this, g0); }
	}
	public void pointerPressed(int x,int y)
	{	
		super.pointerPressed(MasterForm.translateX(this,x), MasterForm.translateY(this,y));;
	}
	public void pointerReleased(int x,int y)
	{	
		super.pointerReleased(MasterForm.translateX(this,x), MasterForm.translateY(this,y));;
	}

}

public class ComboBox<T> extends com.codename1.ui.ComboBox<T> implements AwtComponent
{
	public ComboBox(String s) 
		{ super(s); }
	public ComboBox() 
		{ super(); }
	public Font getFont()
	{ return(G.getFont(getStyle())); 
	}
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	
	// workaround to keep the pop-ups from appearing too close to the top
    protected Dialog createPopupDialog(List<T> l) 
    {
    	Dialog popupDialog = new ComboDialog(getUIID() + "Popup", getUIID() + "PopupTitle");
            popupDialog.setScrollable(false);
            popupDialog.getContentPane().setAlwaysTensile(false);
            popupDialog.setAlwaysTensile(false);
            popupDialog.getContentPane().setUIID("PopupContentPane");
            popupDialog.setDisposeWhenPointerOutOfBounds(true);
            popupDialog.setTransitionInAnimator(CommonTransitions.createEmpty());
            popupDialog.setTransitionOutAnimator(CommonTransitions.createEmpty());
            popupDialog.setLayout(new BorderLayout());
            popupDialog.add(BorderLayout.CENTER, l);
            return popupDialog;
     }
	@SuppressWarnings("rawtypes")
	protected Command showPopupDialog(Dialog popupDialog, List l) 
	{	Command result = super.showPopupDialog(popupDialog,l);
		
		return(result);
	}
	public void setWidth(int n) 
	{ super.setWidth(n); 
	}
	public Dimension calcPreferredSize()
	{
		Dimension sz = super.calcPreferredSize();
		//sz.setWidth(sz.getWidth()+100);
		return(sz);
	}
	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}
	public void paint(com.codename1.ui.Graphics g0)
	{	
		boolean rotated = MasterForm.rotateNativeCanvas(this, g0);
		super.paint(g0);
		if(rotated) { MasterForm.unrotateNativeCanvas(this, g0); }
	}
}
