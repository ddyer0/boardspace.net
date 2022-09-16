package bridge;

import com.codename1.ui.geom.Dimension;

public class Spacer extends com.codename1.ui.Component
{
	public Dimension getPreferredSize() { return new Dimension(getWidth(),getHeight()); }
	public void paint(com.codename1.ui.Graphics g) {}
	
}
