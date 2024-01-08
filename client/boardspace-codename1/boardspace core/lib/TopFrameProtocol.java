package lib;

import com.codename1.ui.Component;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;

import bridge.Container;
import bridge.JMenu;
import bridge.LayoutManager;
import bridge.ProxyWindow;
import bridge.WindowListener;

public interface TopFrameProtocol {

	void setOpaque(boolean b);

	void setBounds(int i, int j, int w, int h);

	void setVisible(boolean v);

	Image getIconAsImage();

	void dispose();

	void removeFromMenuBar(JMenu actions);

	Rectangle getBounds();

	void addWindowListener(WindowListener who);

	void revalidate();

	void changeImageIcon(Image icon);

	int getWidth();

	int getHeight();

	int getX();

	int getY();

	void addC(Component p);
	
	void addC(String where,Component p);
	
	void repaint();

	void addToMenuBar(JMenu m, DeferredEventManager l);

	void addToMenuBar(JMenu m);

	void setCloseable(boolean b);

	CanvasRotater getCanvasRotater();

	void setHasSavePanZoom(boolean v);

	void setEnableRotater(boolean v);

	void setCanSavePanZoom(DeferredEventManager v);

	void setLayout(LayoutManager x);

	void setSize(int x, int y);

	Container getContentPane();

	void addC(ProxyWindow w);

	String tabName();

	void setSize(Dimension newps);

	void packAndCenter();
	
	void setContentPane(Container p);

	Container getParentContainer();

}