package bridge;

import lib.G;

import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.List;
@SuppressWarnings("deprecation")
public class BSLookAndFeel extends DefaultLookAndFeel 
{	public BSLookAndFeel(UIManager m) { super(m); }
	
	public Dimension getListPreferredSize(@SuppressWarnings("rawtypes") List l)
	{
		Dimension dim = super.getListPreferredSize(l);
		dim.setWidth(dim.getWidth()+(int)(20*G.getDisplayScale()));
		return(dim);
	}

}
