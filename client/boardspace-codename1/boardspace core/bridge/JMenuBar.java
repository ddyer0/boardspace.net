package bridge;

import com.codename1.ui.layouts.FlowLayout;

public class JMenuBar extends Container 
{	public JMenuBar() { setLayout(new FlowLayout()); setOpaque(true); setBackground(new Color(0.85f,0.85f,1.0f)); }
	public void add(Menu m) { 	add(m.getMenu()); }
	public void remove(Menu actions) { remove(actions.getMenu()); 	}
	
}
