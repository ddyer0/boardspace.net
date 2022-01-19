package bridge;


import com.codename1.ui.layouts.Layout;

public abstract class JContainer extends Container {
	public JContainer() { super(); }
	public JContainer(Layout flowLayout) 
	{
		super(flowLayout);
	}
}
