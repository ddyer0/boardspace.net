package bridge;


import com.codename1.ui.layouts.Layout;

public abstract class JComponent extends Container {
	public JComponent() { super(); }
	public JComponent(Layout flowLayout) 
	{
		super(flowLayout);
	}
}
