package bridge;

public class BoxLayout extends com.codename1.ui.layouts.BoxLayout
{
	public BoxLayout(com.codename1.ui.Container c,int form)
		{ super(form);
		  c.setLayout(this);
		}
}
