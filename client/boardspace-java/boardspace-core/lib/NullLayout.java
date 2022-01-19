package lib;
import java.awt.*;

public class NullLayout implements LayoutManager
{	
	NullLayoutProtocol expectedParent;
	public NullLayout(NullLayoutProtocol parent) 
	{ expectedParent = parent; 
	}
    /* Required by LayoutManager. */
    public void addLayoutComponent(String name, Component comp)    {  }
 
    /* Required by LayoutManager. */
    public void removeLayoutComponent(Component comp)    {  }
 
    /* Required by LayoutManager. */
    public Dimension preferredLayoutSize(Container parent) {
        Dimension dim = new Dimension(parent.getWidth(), parent.getHeight());
        return dim;
    }
 
    /* Required by LayoutManager. */
    public Dimension minimumLayoutSize(Container parent) {
        Dimension dim = new Dimension(1, 1);
        return dim;
    }

    public void layoutContainer( Container parent) 
    {	
     	expectedParent.doNullLayout(parent);
    }
	public Dimension getPreferredSize(Container parent) { return(new Dimension(parent.getWidth(),parent.getHeight())); }


}