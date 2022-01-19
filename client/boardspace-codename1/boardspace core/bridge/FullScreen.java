package bridge;

import com.codename1.ui.geom.Dimension;

public interface FullScreen {
	public Dimension getMinimumSize(); 
	public void setBounds(int l,int t,int w,int h);
}
