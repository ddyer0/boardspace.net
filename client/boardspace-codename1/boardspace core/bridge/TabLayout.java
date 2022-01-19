package bridge;

import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;

public class TabLayout extends com.codename1.ui.layouts.Layout
{	private int spacing = 2;
	public void layoutContainer(Container parent) {
        int w = parent.getWidth();
        int nc = parent.getComponentCount();
		int sum = getFullWidth(parent);
		int squeeze = (sum>w) ? (sum-w+nc)/nc : 0;
		int deficit = 0;
		for(int i=0,xpos=0;i<nc;i++) 
		{ com.codename1.ui.Component p = parent.getComponentAt(i);
		  Dimension dim = p.getPreferredSize();
		  int ww = dim.getWidth();
		  p.setX(xpos);
		  p.setY(0);
		  p.setHeight(dim.getHeight());
		  int desired = ww-squeeze+deficit;
		  int actual = Math.max(20, desired);
		  p.setWidth(actual);
		  deficit = desired-actual;
		  xpos += actual+spacing;
		}
	}
	
	int getFullWidth(Container parent)
	{	int nc = parent.getComponentCount();
		int sum = -2;
		for(int i=0;i<nc;i++) { sum += parent.getComponentAt(i).getPreferredSize().getWidth()+spacing; }
		return(sum);
	}
	int getFullHeight(Container parent)
	{	int nc = parent.getComponentCount();
		int max = 0;
		for(int i=0;i<nc;i++) { max = Math.max(parent.getComponentAt(i).getPreferredSize().getHeight(),max); }
		return(max);
	}
	public Dimension getPreferredSize(Container parent) {
		return(new Dimension(getFullWidth(parent),getFullHeight(parent)));
	}

}
