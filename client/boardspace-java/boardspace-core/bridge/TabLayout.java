package bridge;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

import lib.G;
/**
 * note here on Tablayout, which is used for the activities and actions bars at the top
 * of boardspace screen.  Getting the dimensions of the icons right and consistent is
 * a nightmare.  The final, for now, disposition is to based the height on the default
 * font size, and the width proportionally scaled to the height, plus a fudge factor.
 * 
 * The containing panel takes its height from the size of this
 * 
 * @author ddyer
 *
 */
public class TabLayout implements LayoutManager
{	private int spacing = (int)(4*G.getDisplayScale());

	public void layoutContainer(Container parent) {
        int w = parent.getWidth();
        int h = parent.getHeight()-spacing*2;
        int nc = parent.getComponentCount();
		int sum = getFullWidth(parent);
		int squeeze = nc==0 ? 0 : (sum>w) ? (sum-w+nc)/nc : 0;
		int deficit = 0;
		for(int i=0,xpos=spacing;i<nc;i++) 
		{ Component p = parent.getComponent(i);
		  int ww2 = prefw(parent,i);
		  int desired = ww2-squeeze+deficit;
		  int actual = Math.max(20, desired);
		  p.setBounds(xpos,spacing,actual,h);
		  deficit = desired-actual;
		  xpos += actual+spacing;
		}
	}
	
	int getFullWidth(Container parent)
	{	int nc = parent.getComponentCount();
		int h = parent.getHeight()-spacing*2;
		int sum = h/2;
		for(int i=0;i<nc;i++) 
		{ 
		  sum += prefw(parent,i)+spacing; 
		}
		return(sum);
	}
	// this is an ad-hoc calculation to find the preferred width for an icon
	// assuming it will be scaled to the height of the parent.  The h/6 factor
	// accounts for wider horizontal margins than vertical, not sure where they
	// come from.
	int prefw(Container parent, int i)
	{	int h = parent.getHeight()-spacing*2;
		Dimension dim = parent.getComponent(i).getPreferredSize();
		int inc = (int)(h*((double)dim.getWidth()/dim.getHeight())+h/6);
		return inc;
	}
	int getFullHeight(Container parent)
	{	int nc = parent.getComponentCount();
		// use font height as the basic scale metric
		int max = (int)(G.getFontSize(G.getGlobalDefaultFont())*2.2);
		for(int i=0;i<nc;i++) { max = Math.max((int)parent.getComponent(i).getPreferredSize().getHeight(),max); }
		return(max);
	}
	public Dimension getPreferredSize(Container parent) 
	{	
		Dimension dim =new Dimension(getFullWidth(parent),getFullHeight(parent));
		return(dim);
	}

	public void addLayoutComponent(String name, Component comp) { }

	public void removeLayoutComponent(Component comp) {	}

	public Dimension preferredLayoutSize(Container parent) {
		Dimension dim = new Dimension(parent.getWidth(), parent.getHeight());
        return dim;
	}

	public Dimension minimumLayoutSize(Container parent) {
		Dimension dim = new Dimension(1, 1);
        return dim;
	}
}
