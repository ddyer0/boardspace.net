package bridge;

import lib.G;
import lib.Graphics;

public class Canvas extends Component
{	

	public void paint(Graphics g) { G.Error("Should be overridden");}
	
	public void paint(com.codename1.ui.Graphics g)
	{	
		paint(Graphics.create(g));
	}
}
