package lib;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

public class NullLayout extends Layout 
{	
	NullLayoutProtocol expectedParent;
public NullLayout(NullLayoutProtocol parent) { expectedParent = parent; }
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
     	expectedParent.doNullLayout((parent instanceof bridge.Container) ? (bridge.Container)parent  : null);
}
public Dimension getPreferredSize(Container parent) { return(new Dimension(parent.getWidth(),parent.getHeight())); }


}