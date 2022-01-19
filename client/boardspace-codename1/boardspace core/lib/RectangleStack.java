package lib;

import com.codename1.ui.geom.Rectangle;


public class RectangleStack extends OStack<Rectangle> 
{
	public Rectangle[]newComponentArray(int n) 
		{ return(new Rectangle[n]); 
		}
}
