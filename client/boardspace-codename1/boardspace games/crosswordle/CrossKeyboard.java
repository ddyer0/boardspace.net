package crosswordle;

import com.codename1.ui.geom.Rectangle;

import lib.CalculatorButton;
import lib.Graphics;
import lib.HitPoint;
import lib.Keyboard;
import lib.KeyboardLayout;
import lib.TextContainer;

public class CrossKeyboard extends Keyboard {
	private CrosswordleViewer viewer = null;
	public CrossKeyboard(CrosswordleViewer see, TextContainer dis, Rectangle r, KeyboardLayout lay) 
	{	super(see, dis, r, lay);
		viewer = see;
	}
	public void drawButton(Graphics gc,CalculatorButton button,HitPoint highlight,Rectangle cr)
	{	button.textColor = viewer.keyboardTextColor(button);
		super.drawButton(gc,button,highlight,cr);
	}
	
}
