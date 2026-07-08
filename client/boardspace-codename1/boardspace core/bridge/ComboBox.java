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

import lib.FontManager;
import lib.G;
import lib.SizeProvider;
import lib.Image;

import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.List;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.Container;


//
// this is a workaround to keep pop-ups from apprearing too close to the top
//
class ComboBoxDialog extends Dialog  implements SizeProvider
{
	ComboBoxDialog(String uu,String lm)
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
	public void pointerDragged(int x,int y)
	{	
		super.pointerDragged(MasterForm.translateX(this,x), MasterForm.translateY(this,y));
	}
}

public class ComboBox extends com.codename1.ui.ComboBox<JMenuItem> 
{	public boolean centerMenu = false;
	public ComboBox() 
		{ super(); 
		}
	public ComboBox(String title)
	{
		super(title);
	}
	public Font getFont()
	{ return(FontManager.getFont(getStyle())); 
	}
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	// workaround to keep the pop-ups from appearing too close to the top
    protected Dialog createPopupDialog(List<JMenuItem> l) 
    {
    	Dialog popupDialog = new ComboBoxDialog(getUIID() + "Popup", getUIID() + "PopupTitle");
            popupDialog.setScrollable(false);
            popupDialog.getContentPane().setAlwaysTensile(false);
            popupDialog.setAlwaysTensile(false);
            popupDialog.getContentPane().setUIID("PopupContentPane");
            popupDialog.setDisposeWhenPointerOutOfBounds(true);
            popupDialog.setTransitionInAnimator(CommonTransitions.createEmpty());
            popupDialog.setTransitionOutAnimator(CommonTransitions.createEmpty());
            popupDialog.setLayout(new BorderLayout());
            popupDialog.addComponent(BorderLayout.CENTER, l);
            return popupDialog;
     }

	public void paint(com.codename1.ui.Graphics g0)
	{	
		boolean rotated = MasterForm.rotateNativeCanvas(this, g0);
		super.paint(g0);
		if(rotated) { MasterForm.unrotateNativeCanvas(this, g0); }
	}
	boolean showingPopupDialog = false;
	
	public boolean isShowingPopupDialog() {
	        return showingPopupDialog || super.isShowingPopupDialog();
	}
	public JMenuItem getSelectedItem()
	{
		Object f = super.getSelectedItem();
		return f instanceof JMenuItem ? (JMenuItem) f : null;
	}
	
	public Command showPopupDialog(Dialog popupDialog, @SuppressWarnings("rawtypes") List l) {
		if(centerMenu) { return super.showPopupDialog(popupDialog,l); }
		else {
	            int top, bottom, left, right;
	            Form parentForm = getComponentForm();

	            int listW = Math.max(getWidth() , l.getPreferredW());
	            listW = Math.min(listW + l.getSideGap()*3/2, parentForm.getContentPane().getWidth());


	            Container content = popupDialog.getDialogComponent();
	            Style contentStyle = content.getStyle();

	            int listH = content.getPreferredH()
	                    + contentStyle.getVerticalMargins();

	            @SuppressWarnings("deprecation")
				Container title = popupDialog.getTitleArea();
	            listH += title.getPreferredH()+title.getStyle().getVerticalMargins() ;

	            bottom = 0;
	            top = getAbsoluteY();
	            int formHeight = parentForm.getHeight();
	            if(parentForm.getSoftButtonCount() > 1) {
	                Container c = parentForm.getSoftButton(0).getParent();
	                formHeight -= c.getHeight();
	                Style s = c.getStyle();
	                formHeight -= (s.getVerticalMargins());
	            }

	            if(listH < formHeight) {
	            	// pop up or down?
	            	// leave top at the top
	            	bottom = formHeight - top - listH;
	            	if(bottom<0) 
	            	{ top += bottom; bottom = 0;
	            	}
	            } else {
	                top = 0;
	            }

	            left = getAbsoluteX();
	            right = parentForm.getWidth() - left - listW;
	            if(right < 0) {
	                left += right;
	                right = 0;
	            }
	            showingPopupDialog = true;
	            // this is a complete kludge - we can't seem to get rid of the
	            // title and pulldown, so move the main part to cover them up.
	            Font c = getFont();
	            int lineh = FontManager.getFontSize(c);
	            top -= lineh;
	            bottom +=lineh;
	            @SuppressWarnings("deprecation")
				Command out =  popupDialog.show(Math.max(top, 0),
	                    Math.max(bottom, 0),
	                    Math.max(left, 0),
	                    Math.max(right, 0), false, true);
	            showingPopupDialog = false;
	            return out;
	        
	    }
	}
}
